## Refinamiento Técnico (Developer)
**Autor**: esteban.colorado | **Fecha**: 2026-09-30

---

### Contexto

`HU-38` entregó el árbol 3D y, después de ella, el modelo **ya evolucionó dentro de su propia frontera**: `tree.js` no es hoy el tronco-cilindro-con-copa-de-icosaedros que describe la narrativa de esta historia. Tiene esqueleto de ramificación recursiva (`growBranch`), PRNG con semilla fija (`seededRandom(BRANCH_SEED)`), forma por etapa (`STAGE_PRESETS`), encuadre derivado de la geometría real y dibujo en tres `InstancedMesh`. **Ese hallazgo mueve el alcance real de esta historia**, y conviene decirlo antes del plan: lo que falta no es «poner ramificación», es **quitar las primitivas**.

**Feature análoga leída completa: el propio `assets/tree/tree.js` (1 465 líneas) y su refinamiento `HU-38`.** No hay otra feature análoga en el proyecto: `tree.js` es la única geometría procedural, el único JavaScript y el único WebView. La referencia secundaria es `.claude/skills/tension-arbol-3d-visual/references/modelo.md`, que documenta las decisiones de forma tomadas después de `HU-38` y las trampas que costaron iteraciones.

#### Qué de la historia ya está cumplido por el código de hoy

| Exigencia de `HU-44` | Estado | Dónde |
|---|---|---|
| Ramificación procedural recursiva | ✅ Ya existe | `growBranch()` — `tree.js:549` |
| PRNG con semilla fija, cero `Math.random()` | ✅ Ya existe | `seededRandom()` LCG — `tree.js:515`; `BRANCH_SEED = 20260902` |
| Topología fija por etapa; salud solo transforma y colorea | ✅ Ya existe | `applyDroop()` + `updateSkeletonMatrices(foliageT)` — la malla nunca se remalla por salud |
| Escala por etapa, aspecto por salud, transición continua | ✅ Ya existe | `STAGE_PRESETS.frameFill`, `TRANSITION_MS`, `easeInOut` |
| Warm-up de la sonda ≥ 2–3 fotogramas | ✅ Ya existe, con margen | `PROBE_WARMUP_FRAMES = 5` |
| `.dispose()` al regenerar la malla de etapa | ⚠️ Existe pero implícito | `disposeGroup()` recorre el grafo — ver **D10** |
| Sin `.glb` / `.gltf`, sin red | ✅ Ya existe | `assets/tree/` solo tiene `three.min.js`, `tree.html`, `tree.js` |
| Semilla como montículo con brote | ✅ Ya existe | `buildSeed()` + `buildMound()` |

#### Qué falta de verdad — la superficie real del cambio

| # | Hueco | CA |
|---|---|---|
| 1 | **Las primitivas siguen ahí.** 7 usos de `CylinderGeometry` / `IcosahedronGeometry` construyen todo el árbol: segmentos rectos de sección circular perfecta, uniones esféricas, follaje esférico, montículo esférico, tallo y cotiledones de la Semilla, pie del tronco. La silueta es *ramificada* pero está hecha de formas básicas. | CA-44.01, CA-44.07 |
| 2 | **Ninguna geometría calcula sus normales explícitamente.** `computeVertexNormals()` no aparece en el archivo: hoy las normales vienen regaladas por las primitivas de Three. En cuanto la geometría se genere a mano dejan de venir solas, y `HU-45` no puede iluminar ni desplazar sin ellas. | CA-44.01 |
| 3 | **`webglcontextlost` no se escucha.** El evento no aparece en `tree.js`. Al volver de segundo plano con el contexto GPU destruido la pantalla se queda congelada y nadie avisa al lado nativo. | CA-44.05 |
| 4 | **Los topes de recursión son de la tabla de calidad, no del código.** `maxBranchDepth` vive en `QUALITY_PRESETS`; no hay tope de nudos ni de hojas. Un preset mal tocado dispara la recursión y con ella el timeout de 2,5 s. | CA-44.04 |
| 5 | **Falta la medición del APK y la ADR.** | CA-44.09, CA-44.11 |

#### Lo que NO se toca

- **`DOM-01`, `DAT-01`, `DB-01`, `DI-01`: nada.** Ni `TreeHealthRule`, ni `TreeGrowthStageRule`, ni `TreeStateEntity`, ni `TreeRepositoryImpl`, ni el respaldo, ni el esquema. La verificación de ADR-020 es la misma de `HU-38`: **ningún archivo de `domain/`, `data/` ni `di/` aparece en el diff**.
- **Kotlin, íntegro.** `Tree3DView.kt`, `TreeBridge.kt`, `TreeRenderQuality.kt`, `TreeWebViewSupport.kt`, `TreeScreen.kt`, `TreeIcon`. Ver **D9**: la caída por pérdida de contexto no necesita una sola línea nativa nueva.
- **El contrato del puente.** Sigue llevando exactamente salud y etapa. `interfaces_contract.md` no cambia.
- **La tarjeta de Inicio, la navegación, `tree.html` y `three.min.js`.**
- **El mecanismo de calidad.** `TreeRenderQuality.resolve` predice y la sonda confirma; esta historia no crea uno nuevo (regla de negocio 10). Ver **D11**.
- **`system_definition_document.md`.**

---

### Decisiones técnicas

#### D1 — Un generador de mallas propio: ninguna primitiva de Three.js sobrevive en el árbol

Se escriben tres constructores de `THREE.BufferGeometry` a mano —`makeBranchGeometry`, `makeBlobGeometry`, `makeMoundGeometry`— y se retiran los 7 usos de `CylinderGeometry` / `IcosahedronGeometry` del árbol. Cada constructor termina con **`geometry.computeVertexNormals()` explícito**, que es literalmente lo que pide CA-44.01 y el prerrequisito matemático de `HU-45`.

La `PlaneGeometry` de `shadowPlane` **se queda**: es el receptor de sombra, no geometría del árbol, y no tiene silueta que leer.

> **Alternativa descartada — deformar los vértices de las primitivas.** Es menos código y da una silueta parecida, pero deja `new THREE.IcosahedronGeometry(...)` escrito en el archivo, y CA-44.07 no dice «que no se vea»: dice que el generador Low-Poly **no existe**. Generar desde cero además da control sobre el orden de los vértices, que es lo que `HU-45` necesitará para las UV.

#### D2 — La irregularidad estructural va al esqueleto, no a la malla unitaria

Esta es la decisión que gobierna el resto, y tiene una razón dura. `updateSkeletonMatrices` escala cada instancia con `(radius, length, radius)`:

```js
tmpScale.set(entry.radius, entry.length, entry.radius);   // tree.js:770
```

Cualquier **deriva lateral horneada en la geometría unitaria se multiplica por el radio** —centímetros en el tronco, milímetros en las puntas—, así que una rama «curvada» en la malla unitaria sale recta en pantalla. La curvatura tiene que vivir en el esqueleto.

Por eso cada rama se parte en `subSegments` nudos encadenados, cada uno con una desviación angular pequeña tomada del PRNG. El tronco se curva, las ramas dejan de ser segmentos rectos, y el precio es **cero llamadas de dibujo nuevas**: son más instancias dentro de las mallas que ya existen, que es exactamente lo que `modelo.md` prescribe («añadir detalle es más barato que añadir mallas»).

Tres propiedades se conservan gratis por construirlo así:
- La caída por salud sigue siendo **una rotación por nudo** (`applyDroop` recorre `branchPivot`), no un recálculo de forma.
- La topología sigue **fija por etapa**: los sub-segmentos se crean en `buildSkeleton`, no en `applyHealth` (CA-44.03).
- `collectFitSamples` itera `branchNodes`, así que el encuadre se recoloca solo (`modelo.md` §3).

#### D3 — La sección transversal deja de ser un círculo

`makeBranchGeometry(rings, radial, seed, profile)` construye un tubo a lo largo de +Y con el radio de **cada vértice** modulado por ruido determinista en las dos direcciones: a lo largo del eje (el fuste engorda y adelgaza) y alrededor (nervaduras de corteza, perfil no circular). Se genera **una sola geometría unitaria**: la variación entre ramas la da el `rotation.y` que cada nudo ya tiene, que hace caer las mismas nervaduras en sitios distintos, más la escala no uniforme de cada instancia.

> **Alternativa descartada — un pool de K geometrías variantes.** Da más variedad real, pero cuesta K `InstancedMesh` por familia: a K=2 el maduro pasa de 5 a 7 llamadas de dibujo. En gráficos móviles la llamada cuesta más que el triángulo (`modelo.md`), y el presupuesto manda sobre la fidelidad. Si las capturas de T14 muestran repetición evidente, **entonces** se sube a K=2 solo en `high`, donde sobra margen.

#### D4 — El pie del tronco pasa de cilindro a tubo acampanado

`buildRootFlare` se conserva como malla dedicada —mismo número de llamadas de dibujo que hoy— pero su cilindro se sustituye por `makeBranchGeometry` con un perfil que se abre en la base y ruido más grueso. Es el remate del sellado de la base que `modelo.md` §5 señala como el hueco que siempre se olvida.

#### D5 — El follaje deja de ser una esfera: es un grumo

`makeBlobGeometry(rows, cols, seed, roughness)` construye una malla lat/long con el radio por vértice desplazado por ruido determinista. Con `foliageFlatten` y la rotación de instancia que ya existen, la copa se lee como masa de hojas y no como bolas apiladas. **La misma geometría sirve para las uniones** con `roughness` bajo: son pequeñas y su función es tapar, no leerse.

El umbral `FOLIAGE_TIP_SCALE` / `FOLIAGE_TIP_SPREAD` es el parámetro que `modelo.md` §5 marca como «el que más se afina y el que más se equivoca». Un grumo irregular tiene **radio efectivo menor que la esfera del mismo radio nominal**, así que abrirá huecos en la copa si se deja igual. Se recalibra en T12 con las capturas delante, no a ojo.

#### D6 — El montículo es terreno, no un icosaedro aplastado

`makeMoundGeometry` genera una cúpula con el borde irregular y la superficie ondulada. Es la referencia de tamaño constante entre etapas y en Semilla es casi lo único que hay que mirar: si sigue siendo una esfera aplastada, la Semilla se sigue leyendo como formas básicas y CA-44.02 no se cumple.

#### D7 — La Semilla entra en el mismo tratamiento

`buildSeed()` deja de usar `CylinderGeometry` + dos `IcosahedronGeometry`: tallo con `makeBranchGeometry` y dos cotiledones con `makeBlobGeometry` aplanados. CA-44.02 prohíbe mezclar estilos entre etapas y la Semilla es la que más fácil se queda atrás, porque vive en su propio grupo fuera del esqueleto.

#### D8 — Topes duros de recursión por código, por encima de la etapa y de la calidad

Tres constantes nuevas y una guarda en `growBranch`:

```js
var MAX_BRANCH_DEPTH_HARD = 4;    // ningún preset puede pedir más
var MAX_BRANCH_NODES      = 260;  // incluye sub-segmentos (D2)
var MAX_FOLIAGE_BLOBS     = 180;
```

Hoy el único tope es `quality.maxBranchDepth`, que es una **tabla de configuración**: tocarla mal dispara la recursión combinatoria y con ella el timeout de carga nativo de 2,5 s (`READY_TIMEOUT_MS`, `Tree3DView.kt:45`), que deja el fallback activado **permanentemente** en un dispositivo con GPU capaz. CA-44.04 pide topes «definidos por código» precisamente porque sobreviven a un cambio de preset. Las cuentas reales entran en `reportBudget`.

#### D9 — `webglcontextlost` → `TreeBridge.onFailure()`, y ni una línea de Kotlin

```js
canvas.addEventListener('webglcontextlost', function (event) {
    reportFailure('webglcontextlost');
}, false);
```

**No se llama a `event.preventDefault()`**: prevenirlo es lo que pide la restauración del contexto, y aquí no se quiere restaurar — se quiere caer al ícono nativo, que ya existe y ya dice lo mismo.

Del lado nativo no hay nada que hacer: `Tree3DView` documenta en su KDoc que `onFailure` **puede llegar después de `onReady`** (`Tree3DView.kt:60`) y `notifyFailure` no está condicionado a `reportedReady`. La red de seguridad de `HU-38` ya cubría este caso; lo que faltaba era que el JS la usara.

#### D10 — El `.dispose()` deja de ser implícito

`disposeGroup()` recorre el grafo y libera `node.geometry` / `node.material` de lo que cuelgue de él. Con geometrías generadas y **compartidas entre mallas** (D5: blob para follaje y para uniones) eso las libera por duplicado y, peor, deja fuera cualquier geometría que no acabe colgada de una malla. Se añade un registro explícito:

```js
var generatedGeometries = [];   // lo llena cada make*Geometry
```

que `rebuildTree()` vacía llamando `.dispose()` una vez por geometría antes de construir la nueva etapa. CA-44.01 lo pide como requisito estricto y la ramificación genera mallas nuevas en cada cambio de etapa **y en cada degradación de calidad**, que es donde la fuga se acumula sin que nadie la vea.

#### D11 — La sonda de rendimiento no cambia

`PROBE_WARMUP_FRAMES = 5` ya supera con margen los 2–3 fotogramas que exige la nota técnica de CA-44.04, y `QUALITY_DOWNGRADE` ya baja un escalón una sola vez. Se conserva **tal cual**. La regla de negocio 10 es explícita: esta historia no crea un mecanismo de calidad nuevo.

Lo único que se toca de la tabla de calidad son las columnas que la geometría nueva necesita (`branchRings`, `subSegments`, `blobRows`, `blobCols`) y el `LOW` sigue siendo **el mismo árbol orgánico, drásticamente simplificado** — nunca el generador anterior, que ya no existe (CA-44.04, CA-44.07).

---

### Reutilización

| Componente | Decisión | Motivo |
|---|---|---|
| `seededRandom(seed)` — `tree.js:515` | **Se reutiliza** | Es el PRNG con semilla fija que CA-44.01 exige. Todo el ruido de D1–D7 sale de él; no se escribe un segundo generador |
| `growBranch()` / esqueleto de `Object3D` | **Se reutiliza y se extiende** | D2 le añade sub-segmentos. No se reescribe la recursión |
| `applyDroop()` / `updateSkeletonMatrices()` | **Se reutiliza** | La caída y el volcado de matrices valen igual con más nudos |
| `collectFitSamples()` / `fitRadiusForSamples()` / `frameStage()` | **Se reutiliza sin tocar** | Iteran `branchNodes` y `foliageSpecs`: el encuadre se recoloca solo (`modelo.md` §3) |
| `foliageColor()` / `lerpHex()` / `HEALTH_STOPS_*` | **Se reutiliza sin tocar** | El color por salud no cambia; CA-44.03 lo conserva |
| `QUALITY_PRESETS` / `QUALITY_DOWNGRADE` / `probe()` | **Se reutiliza, se añaden columnas** | D11 |
| `disposeGroup()` | **Se reutiliza y se refuerza** | D10 le añade el registro explícito |
| `reportFailure()` | **Se reutiliza** | D9 lo usa para `webglcontextlost` |
| `reportBudget()` | **Se reutiliza, se amplía** | Añade nudos y sub-segmentos (D8) |
| `Tree3DView.kt` / `TreeBridge.kt` / `TreeRenderQuality.kt` / `TreeWebViewSupport.kt` / `TreeIcon` | **Se reutilizan sin modificar** | D9 y la lista de «lo que NO se toca» |
| `tools/capturar-arbol.ps1`, `tools/seed-arbol.ps1` | **Se reutilizan sin modificar** | Son el instrumento de CA-44.08 |
| `TreeTestScenarios.kt` | **Se amplía** | El catálogo cubría las etapas y los extremos de salud, pero no la banda media de Brote ni la de Maduro: dos escenarios nuevos, no un catálogo nuevo |
| `makeBranchGeometry` / `makeBlobGeometry` / `makeMoundGeometry` / `noise*` | **Se crean** | No hay equivalente: el proyecto no tiene ninguna utilidad de geometría fuera de `tree.js`, y Three.js no trae un generador de mallas irregulares. Viven dentro del mismo IIFE, sin archivo nuevo |

---

### Tareas de Implementación

#### Fase 0 — Línea base del presupuesto (CA-44.09)

- [ ] **T1: Medir el peso del APK antes de tocar nada** — `Tension/app/build/outputs/apk/release/`

  `./gradlew :app:assembleRelease` y registrar el tamaño exacto en bytes. **Debe correr antes de T2**: después no hay línea base, solo estimación. El valor se anota en `dev-record.md`. Referencia de `HU-38`: el árbol entero costó 192 570 bytes, de los cuales 199 KiB son Three.js — esta historia **no añade assets**, así que el incremento esperado es el del propio `tree.js` y debería ser de unos pocos KiB.

#### Fase 1 — Generador de mallas orgánicas (CA-44.01, CA-44.07)

- [ ] **T2: Ruido determinista sobre el PRNG existente** — `assets/tree/tree.js` (Base: `seededRandom()`, `tree.js:515`)

  Una función de ruido suave (valor interpolado, no ruido blanco) construida **sobre `seededRandom`**, no sobre un generador nuevo. El ruido blanco da una malla con púas; hace falta continuidad entre vértices vecinos para que se lea como madera y no como erizo. Determinista por construcción: misma semilla → mismos vértices (CA-44.01, regla 9).

- [ ] **T3: `makeBranchGeometry(rings, radial, seed, profile)`** — `assets/tree/tree.js`

  Tubo unitario a lo largo de +Y, altura 1, radio 1 en la base. `profile(t)` da el radio normalizado a lo alto (estrechamiento del segmento, campana del pie de tronco en D4). El radio de cada vértice se modula con el ruido de T2 en las dos direcciones (D3). Tapa superior e inferior cerradas. **Termina en `computeVertexNormals()`** y se registra en `generatedGeometries` (D10).

- [ ] **T4: `makeBlobGeometry(rows, cols, seed, roughness)`** — `assets/tree/tree.js`

  Malla lat/long de radio 1 con desplazamiento radial por el ruido de T2. Sirve al follaje (`roughness` alto, D5) y a las uniones (`roughness` bajo). **`computeVertexNormals()` explícito** + registro.

- [ ] **T5: `makeMoundGeometry(radius, rows, cols, seed)`** — `assets/tree/tree.js`

  Cúpula de terreno con borde irregular y superficie ondulada (D6). **`computeVertexNormals()` explícito** + registro.

- [ ] **T6: Retirar las primitivas y hacer explícita la disposición** — `assets/tree/tree.js`

  Sustituir los **7 usos** (`tree.js:689, 700, 713, 826, 833, 853, 887`) por T3/T4/T5. Al terminar, `grep -c "IcosahedronGeometry\|CylinderGeometry" tree.js` debe dar **0**; la única primitiva restante es la `PlaneGeometry` de `shadowPlane`, que no es geometría del árbol (D1). Añadir `generatedGeometries[]` y vaciarlo con `.dispose()` en `rebuildTree()` (D10). Retirar `SEGMENT_TAPER` si `profile` lo absorbe.

#### Fase 2 — Esqueleto orgánico (CA-44.01, CA-44.02, CA-44.03)

- [ ] **T7: Sub-segmentos encadenados con desviación angular** — `assets/tree/tree.js` (Base: `growBranch()`, `tree.js:549`)

  Cada rama se emite como `subSegments` nudos en cadena, con un giro pequeño del PRNG entre uno y el siguiente (D2). El pivote de caída (`branchPivot`, `userData.baseRotZ`, `userData.droopFactor`) sigue estando **solo en el primer nudo de cada rama**: la caída no debe acumularse por sub-segmento o la punta se enrolla sobre sí misma.

- [ ] **T8: Uniones solo en bifurcaciones reales** — `assets/tree/tree.js` (Base: `buildInstancedMeshes()` / `updateSkeletonMatrices()`)

  Hoy hay una esfera de unión por nudo. Con D2 los nudos se multiplican por `subSegments` y las uniones con ellos, sin tapar nada nuevo: dentro de una rama los sub-segmentos son colineales y no hay costura que cubrir. Marcar cada entrada con `isFork` y contar solo ésas en `junctionMesh.count`. Es lo que evita que D2 se coma el presupuesto.

- [ ] **T9: Topes duros de recursión** — `assets/tree/tree.js`

  `MAX_BRANCH_DEPTH_HARD`, `MAX_BRANCH_NODES`, `MAX_FOLIAGE_BLOBS` con guarda de salida en `growBranch` (D8). `resolveForm()` acota además `branchDepth` contra el tope duro, no solo contra la calidad. `reportBudget` pasa a informar nudos, sub-segmentos, bifurcaciones y hojas.

- [ ] **T10: Semilla y pie del tronco en el mismo tratamiento** — `assets/tree/tree.js` (D4, D7)

  `buildSeed()` → tallo con T3 y dos cotiledones aplanados con T4. `buildRootFlare()` → T3 con perfil acampanado. Ninguna etapa queda con el tratamiento anterior (CA-44.02, CA-44.07).

#### Fase 3 — Robustez del render (CA-44.05)

- [ ] **T11: Escuchar `webglcontextlost` sobre el canvas** — `assets/tree/tree.js` (Base: `bindGestures()` / `reportFailure()`)

  Registrar en `init()`, junto al enganche de gestos. Sin `preventDefault()` (D9). **No se toca Kotlin**: `Tree3DView` ya acepta `onFailure` después de `onReady`.

#### Fase 4 — Calibración por calidad y presupuesto (CA-44.04, CA-44.06)

- [ ] **T12: Columnas nuevas en `QUALITY_PRESETS` y recalibrado del follaje** — `assets/tree/tree.js`

  `branchRings`, `subSegments`, `blobRows`, `blobCols` por escalón, degradando en el orden ya establecido. Presupuesto objetivo para Maduro, contra las cuentas de referencia de `modelo.md`: **`high` ≲ 16 000 triángulos · `medium` ≲ 7 000 · `low` ≲ 1 200**, y **5 llamadas de dibujo en todos los escalones** (las mismas de hoy: rama, unión, follaje, montículo, pie). `LOW` es el mismo árbol orgánico simplificado, nunca el anterior. Recalibrar `FOLIAGE_TIP_SPREAD` / `foliageScale` contra los huecos que D5 puede abrir — **con las capturas de T14 delante, no a ojo**.

#### Fase 5 — Verificación

- [ ] **T13: Sintaxis y suite de regresión** — `node --check`, `./gradlew :app:testDebugUnitTest`

  `node --check Tension/app/src/main/assets/tree/tree.js`: **ningún paso del build valida el JS** y un error de sintaxis se manifiesta solo como fallback silencioso en el dispositivo. La suite Kotlin corre como **regresión**, no como cobertura nueva: esta historia no cambia una línea de Kotlin, así que los 4 tests de `ui/tree/` y el resto de la suite deben quedar **exactamente igual, 0 fallos**. Ver *Riesgos* sobre por qué no se añaden tests de JS.

- [ ] **T14: Juego de capturas y aprobación del PO** — `tools/capturar-arbol.ps1` (CA-44.08)

  Lo ejecuta **la persona**, no el agente (skill `tension-arbol-3d-visual`). `-Instalar` es obligatorio: los assets viajan dentro del APK. Tres tiras, una por banda de salud, cada una en orden de crecimiento:

  ```powershell
  .\tools\capturar-arbol.ps1 -Instalar -Escenarios 01,03,07,12 -Etiqueta hu44-alta
  .\tools\capturar-arbol.ps1 -Escenarios 22,08,23 -Etiqueta hu44-media
  .\tools\capturar-arbol.ps1 -Escenarios 20,21,14 -Etiqueta hu44-marchito
  ```

  Son **exactamente las 10 capturas**, sin repetir ninguna. Los escenarios **22** (Brote 50) y **23** (Maduro 50) se añadieron al catálogo en esta historia: no existía banda media ni para Brote ni para Maduro —lo más cercano era el 05, con salud 92, y el 13, con salud 8—, así que dos de las diez capturas no habrían mostrado la banda intermedia.

  Se verifica: nada recortado (`MargenSup`/`MargenInf` > 0), `AltoPx` creciente de `01` a `12` en la tira alta, copa sin huecos, base sellada, maduro marchito ramificado y no un poste, color por salud. Antes de comparar medidas **entre tiras**, comprobar que la línea `calidad=` coincide. **Son 10 y no 12: la Semilla solo tiene una banda de salud alcanzable.**

- [ ] **T15: Medir el incremento del APK** — `dev-record.md` (CA-44.09)

  `assembleRelease` de nuevo y diferencia contra T1, en bytes y en porcentaje, más el tamaño en disco de `tree.js` antes y después. **Es un entregable de la CA**, no una nota al pie.

#### Fase 6 — Documentación y cierre

- [ ] **T16: ADR de la nueva estrategia de generación** — `docs/architecture/architecture_blueprint.md` (CA-44.11)

  **ADR-022**, con `ADR-021` anotada: su frase «se construye entero por código con primitivas (`CylinderGeometry` para tronco y ramas, `IcosahedronGeometry` para la copa)» queda **superada** por ésta. Alternativas descartadas que la CA exige registrar: modelos `.glb`/`.gltf` (prohibidos por la frontera del PO y por RNF09), conservar el Low-Poly como escalón de degradación (dos generadores en el mismo asset = peso y deuda), y el pool de geometrías variantes de D3. Consecuencias: la regla de degradación por calidad y el incremento de APK de T15. Actualizar también la ficha de `tree.js` en §2.1 (`UI-01`) y la línea 217/218 con la entrada de `HU-44`.

- [ ] **T17: Verificar las fronteras congeladas** — `git diff --stat` (CA-44.10)

  `system_definition_document.md` sin modificar. `interfaces_contract.md` sin modificar (el puente no cambia). Ningún archivo de `domain/`, `data/` ni `di/` en el diff. Ningún `.glb`/`.gltf` en `assets/`.

- [ ] **T18: Registrar el desarrollo** — `dev-record.md` (nuevo, patrón de `HU-38`), `index.md` (fases Refinamiento/Desarrollo a ✅ y métricas de tiempo), `cambios.md`

---

### Riesgos y observaciones

**Las «12 capturas» de CA-44.08 no son alcanzables tal como están escritas, y no por culpa del render.** La etapa Semilla existe **si y solo si** hay 0 sesiones cerradas (`TreeGrowthStageRule.resolve`), y con 0 sesiones no hay última sesión, así que `TreeHealthRule.calculate(null)` devuelve **100 siempre**. La Semilla tiene **una sola banda de salud alcanzable en el dominio**; sus otras dos no se pueden capturar sin falsear el estado, y falsearlo tampoco serviría: `TreeRepositoryImpl.recalculate()` corre al abrir la pantalla del árbol y sobrescribe cualquier `tree_state` escrito a mano (`modelo.md` §1). **Se entregan 10 capturas reales** —Semilla ×1 + Brote/Joven/Maduro ×3— y se anota la razón. Es una decisión que corresponde al PO: si quiere las 12, hay que cambiar la regla de salud, y eso es otra historia.

**La aprobación estética es el riesgo que la historia declara, y el plan lo asume sin poder cerrarlo.** Un generador procedural puede cumplir todos los presupuestos y aun así no convencer. La salida declarada es el bucle de T12 ↔ T14: las capturas mandan sobre los parámetros, no al revés. Por eso T12 dice explícitamente «con las capturas delante, no a ojo».

**`tree.js` sigue sin tests automatizados, y esta historia no es la que debe cambiarlo.** ADR-021 lo dejó escrito como precio asumido: *«el proyecto no tiene ni tendrá cadena de build JavaScript»*, y por eso las tres decisiones con ramas se sacaron a Kotlin, donde sí están cubiertas. Introducir Node + un runner + un doble de Three.js para probar geometría —que es justamente lo que solo se valida mirándolo— sería una segunda cadena de build contra una ADR adoptada, y contra la frontera técnica que el PO declaró para esta historia. Lo que sí se hace: `node --check` en T13 (que hoy no se ejecutaba nunca) y la tira de capturas de T14 como verificación formal. **La suite Kotlin no es cobertura de esta historia; es su prueba de no-regresión**, y su valor está justamente en que debe salir idéntica.

**D2 puede comerse el presupuesto si T8 no entra con él.** Multiplicar los nudos por `subSegments` multiplica también las esferas de unión, que son lo más caro por instancia. T8 no es una optimización opcional: sin ella, `medium` se va por encima del presupuesto y la sonda degradará siempre — que es exactamente la señal de alarma que `modelo.md` describe.

**El grumo irregular abre huecos donde la esfera no los abría.** Un blob con ruido tiene radio efectivo menor que la esfera de su radio nominal. `modelo.md` §5 marca el umbral del follaje como el parámetro que más se afina y más se equivoca: **por debajo, la copa se lee como floretes sueltos**. Está previsto en T12 y es la primera cosa que hay que mirar en la tira de T14.

**La narrativa de la historia describe un `tree.js` que ya no existe.** Habla de «tronco cilíndrico y copa de icosaedros superpuestos» y de «reemplazar la generación del modelo por un generador de ramificación procedural»; la ramificación, el PRNG con semilla y la forma por etapa **ya están implementados** desde después de `HU-38`. Eso no reduce el valor de la historia —el hueco real, las primitivas, es el que hace que siga leyéndose como formas básicas— pero sí cambia el reparto del esfuerzo: casi todo el trabajo está en las Fases 1 y 2, y los criterios CA-44.03 y la parte de PRNG de CA-44.01 se verifican en vez de implementarse.

**El árbol sigue sin decidir nada.** La excepción de alcance de ADR-020 se mantiene sin ampliarse: esta historia es la de menor superficie de las tres del árbol — **un solo archivo de producto**, `assets/tree/tree.js`.

---

### Validación manual (no automatizable)

1. **CA-44.01 / CA-44.02 (silueta)** — Tira `hu44-crecimiento`. Ninguna etapa se lee como prisma ni esfera; el tronco tiene perfil irregular y las ramas no son rectas. Semilla y Brote comparten tratamiento con Joven y Maduro.
2. **CA-44.01 (determinismo)** — Abrir y cerrar la pantalla del árbol cinco veces seguidas **sin cambiar de estado**: el árbol debe ser idéntico, vértice a vértice. Es la comprobación del PRNG con semilla fija, y el caso que la rompe es la degradación de la sonda, que reconstruye unos segundos después de abrir.
3. **CA-44.03 (transición continua)** — Escenarios `12` → `13` → `14`: el follaje encoge y las ramas caen de forma continua, sin salto de malla ni parpadeo de geometría.
4. **CA-44.04 (degradación)** — Comprobar en logcat la línea `[tree] construido … calidad=…`. Forzar `low` por query string y verificar que la silueta sigue siendo el árbol orgánico y que se leen etapa y salud. Ninguna aparición **sistemática** de `degradado` en un dispositivo que antes no degradaba.
5. **CA-44.05 (pérdida de contexto)** — Abrir la pantalla del árbol, mandar la app a segundo plano, abrir varias apps pesadas y volver. Si el contexto se perdió, debe aparecer **el ícono nativo teñido**, con puntaje, días y mensaje intactos, sin mensaje de error y sin pantalla en blanco.
6. **CA-44.06 (presupuesto)** — Cronometrar de la tarjeta de Inicio al árbol renderizado: **< 1 s** en gama media. Rotación fluida. El arranque de la app y el resto de la navegación, sin cambio.
7. **CA-44.10 (sin regresión)** — Órbita, topes de elevación, pinza a los dos extremos y reinicio de cámara al reentrar, **idénticos** a `HU-38`. Fondo transparente correcto en claro y oscuro (RNF23). Tarjeta de Inicio nativa.
8. **CA-44.07 (retirada del Low-Poly)** — `grep -c "IcosahedronGeometry\|CylinderGeometry" tree.js` = **0**.
