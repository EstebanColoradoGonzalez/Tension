## Refinamiento Técnico (Developer)
**Autor**: esteban.colorado | **Fecha**: 2026-09-30

---

### Contexto

`HU-44` acaba de entregar la geometría: `tree.js` genera tronco, ramas, follaje, nudos, montículo y Semilla **vértice a vértice**, con `computeVertexNormals()` explícito en cada malla y 0 normales invertidas medidas. Esta historia pinta sobre eso. La frontera es todavía más estrecha que la de su hermana: **lo único que cambia son los materiales**, y la geometría solo se toca para darles los dos datos que necesitan (`uv` y tangente).

**Feature análoga leída completa: el propio `assets/tree/tree.js` (1 945 líneas) y el `refinamiento.md` + `dev-record.md` de `HU-44`.** No hay otra feature análoga en el proyecto: `tree.js` es el único JavaScript, el único WebView y la única geometría procedural. Referencias secundarias: `.claude/skills/tension-arbol-3d-visual/references/modelo.md` (cómo está construido el modelo y qué trampas costaron iteraciones) y `docs/architecture/architecture_blueprint.md` §1.2, §2.1 (`UI-01`), §3, §4 y ADR-020 / ADR-021 / ADR-022.

#### Qué de la historia ya está cumplido por el código de hoy

| Exigencia de `HU-45` | Estado | Dónde |
|---|---|---|
| PRNG con semilla fija, cero `Math.random()` | ✅ Ya existe | `seededRandom()` LCG — `tree.js:611`; `BRANCH_SEED = 20260902`. `grep -c "Math.random"` da 1 y es **prosa de un comentario** (`tree.js:609`) |
| Ruido determinista en CPU sobre ese PRNG | ✅ Ya existe | `makeNoise()` / `makeSurfaceNoise()` — `tree.js:640` y `:669`. Lo escribió `HU-44` para la corteza **geométrica** |
| Normales explícitas y orientadas hacia fuera | ✅ Ya existe y está medido | `finishGeometry()` — `tree.js:683`. Es el prerrequisito que `HU-44` dejó listo para esta historia |
| Transición continua de salud, sin saltos | ✅ Ya existe | `TRANSITION_MS = 900`, `easeInOut()`, `applyHealth()`; `HEALTH_STOPS_*` interpola verde → seco → marchito |
| Caída de ramas y reducción de follaje por salud | ✅ Ya existe | `applyDroop()` + `updateSkeletonMatrices(foliageT)` |
| Warm-up de la sonda ≥ 2–3 fotogramas | ✅ Ya existe, con margen | `PROBE_WARMUP_FRAMES = 5` — ver **D9** |
| Degradación por calidad de un escalón, una sola vez | ✅ Ya existe | `QUALITY_PRESETS` / `QUALITY_DOWNGRADE` / `probe()` |
| `webglcontextlost` → `TreeBridge.onFailure()` | ✅ Ya existe | `bindContextLoss()` — `tree.js:1789`. Lo entregó `HU-44` (CA-44.05). **Se verifica, no se reescribe** |
| Sin `.glb` / `.gltf`, sin red, sin texturas | ✅ Ya existe | `assets/tree/` solo tiene `three.min.js`, `tree.html`, `tree.js`. `grep -c "TextureLoader"` = 0 |
| Topes duros de recursión | ✅ Ya existe | `MAX_BRANCH_DEPTH_HARD`, `MAX_BRANCH_NODES`, `MAX_FOLIAGE_BLOBS` |

#### Qué falta de verdad — la superficie real del cambio

| # | Hueco | CA |
|---|---|---|
| 1 | **Los cuatro materiales del árbol son color plano.** `trunkMaterial`, `foliageMaterial`, el material del montículo y el `stemMaterial` de la Semilla son `MeshLambertMaterial({ color })` sin una sola línea de shader propia (`tree.js:1349`, `:1350`, `:1284`, `:1224`). El relieve de `HU-44` es **geométrico** —`BARK_RELIEF` mueve vértices del tubo—, y a 8 vértices por anillo eso da silueta, no superficie. La corteza no tiene grano y el follaje **entero comparte un único color**. | CA-45.01 |
| 2 | **Todas las hojas son exactamente el mismo color en todo momento.** `applyHealth` pone un `setHex` sobre un material compartido por las 60–96 instancias. Un árbol que se seca de golpe, en bloque y sin manchas, no se lee como un secado natural. | CA-45.01, CA-45.02 |
| 3 | **No hay nada que degradar por calidad en los materiales.** `QUALITY_PRESETS` gobierna sombras y geometría; no tiene ninguna columna de material, así que `low` no puede «apagar el bump» ni «volver a Lambert básico» porque hoy ya es Lambert básico en los tres escalones. | CA-45.03 |
| 4 | **El fallo de compilación de shaders no se intercepta.** Hoy no hay ningún shader propio que pueda fallar, así que la ausencia no molesta. En cuanto entren los materiales procedurales, un fallo de compilación **no lanza excepción**: pinta negro y `window.onerror` no se entera. Sin interceptación explícita el ejecutante vería un árbol negro en lugar del ícono nativo. | CA-45.04 |
| 5 | **Falta la medición del APK y la ADR.** | CA-45.08, CA-45.10 |

#### Lo que NO se toca

- **`DOM-01`, `DAT-01`, `DB-01`, `DI-01`: nada.** Ni `TreeHealthRule`, ni `TreeGrowthStageRule`, ni `TreeStateEntity`, ni `TreeRepositoryImpl`, ni el respaldo, ni el esquema. La verificación de ADR-020 es la misma de `HU-38` y `HU-44`: **ningún archivo de `domain/`, `data/` ni `di/` aparece en el diff**.
- **Kotlin, íntegro** — incluido `TreeRenderQuality.kt`. Ver **D10**: la historia autoriza tocar sus parámetros «de ser necesario», y no lo es.
- **El contrato del puente.** Sigue llevando exactamente salud y etapa. `interfaces_contract.md` no cambia.
- **La geometría de `HU-44`.** Ni una constante de forma, ni `STAGE_PRESETS`, ni `growBranch`, ni el esqueleto, ni el encuadre. Lo único que se añade a las mallas son dos atributos (`uv`, `aTangent`) que **no mueven un solo vértice** (T2).
- **El mecanismo de calidad.** `TreeRenderQuality.resolve` predice y la sonda confirma. Esta historia solo le añade qué dibujar en cada escalón (regla de negocio 7).
- **La tarjeta de Inicio, la navegación, `tree.html` y `three.min.js`.**
- **`system_definition_document.md`.**

---

### Decisiones técnicas

#### D1 — Los materiales se **inyectan** en los de Three.js con `onBeforeCompile`, no se escriben desde cero

`MeshLambertMaterial` + `onBeforeCompile`, reemplazando los *chunks* `<beginnormal_vertex>`, `<defaultnormal_vertex>`, `<normal_fragment_begin>` y `<color_fragment>`. Se conservan gratis la iluminación Lambert, el mapa de sombras, el `instanceMatrix` de las tres `InstancedMesh` y la niebla.

> **Alternativa descartada — `ShaderMaterial` / `RawShaderMaterial` propio.** Da control total, y cuesta reimplementar a mano la iluminación, el sombreado proyectado y el instanciado: cientos de líneas de GLSL para llegar a donde Three ya está, en un archivo que **no tiene pruebas automatizadas** (ADR-021) y cuya única verificación real es mirar capturas. Además rompería la vía de escape de D7: con el hook, `low` es *no instalar el hook*, y el material vuelve a ser literalmente el Lambert básico que pide CA-45.03; con un shader propio habría que escribir y mantener un segundo shader «básico».

#### D2 — Cero texturas: todo el acabado es ruido matemático en GLSL

CA-45.05 lo ordena en ese orden: **«se priorizan los shaders matemáticos»**, y cada textura tiene que justificar «un detalle que el ruido procedural no logra». Grano de corteza, variación tonal de follaje y textura de tierra son exactamente lo que el ruido hace mejor —y sin coste de carga, sin asincronía y sin descompresión.

Consecuencias, todas verificables:

- El presupuesto de < 100 KB de CA-45.05 se cumple con **0 bytes**.
- La prohibición de Base64 se cumple por vacío: no hay nada que embeber.
- **No hay promesas de `TextureLoader`**, así que la condición de CA-45.05 sobre `TreeBridge.onReady()` —esperar a que resuelvan— se cumple por vacío, y con ella desaparece el *texture popping* que la nota técnica describe. Se deja registrado explícitamente y se verifica con `grep -c "TextureLoader" tree.js` = 0 y `ls assets/tree/*.webp` vacío.
- La nota de cierre del PO sobre `texture.dispose()` queda **sin objeto** por la misma razón, y así se anota en `dev-record.md`.

> **ENMENDADA DURANTE EL DESARROLLO (2026-09-30), con aprobación del PO.** «Cero texturas» pasa a
> ser «cero texturas **de imagen**». El ruido evaluado por fragmento no cabía en el presupuesto
> —la sonda degradaba siempre a `low` en el dispositivo de referencia, y antes de eso llegó a
> tirar la etapa Maduro marchita al fallback nativo por el timeout de carga—, así que se
> **precalcula una vez en una tabla de 128×128 en memoria** y el shader la lee. Todo lo que la
> frontera del PO prohíbe sigue sin ocurrir: no hay archivo, no está en `assets/`, son **0 bytes
> de APK**, no hay Base64, no hay red y no hay carga asíncrona —así que la condición de CA-45.05
> sobre `TreeBridge.onReady()` sigue cumpliéndose por vacío—. Es el mismo ruido matemático con
> la misma semilla fija, memorizado. Detalle en ADR-023 y en `dev-record.md`.

> **Es la única CA donde el plan elige rama, y por eso se declara aquí en vez de darse por supuesta.** Si al ver las tiras el PO echa en falta un detalle que el ruido no da, la puerta sigue abierta: añadir una `.webp` después es aditivo y no invalida nada de lo anterior.

#### D3 — El relieve es **bump** por perturbación de la normal en espacio tangente, con la parametrización de la propia malla

CA-45.01 acepta *displacement* **o** *bump*. Se elige bump, y la elección no es de gusto:

- **El displacement no cabe.** El tubo unitario tiene `(branchRings+1) × (trunkRadialSegments+1)` = **27 vértices** en `high`. Desplazar 27 vértices no produce relieve; produce otra silueta. Subir la densidad hasta que se vea —digamos 13 anillos × 16 radiales— multiplica los triángulos por ~12 sobre **138 instancias**: unos 53 000 triángulos contra un presupuesto de ~16 000. Y encima chocaría con la restricción física que gobierna `HU-44`/D2: `updateSkeletonMatrices` escala cada instancia con `(radius, length, radius)`, así que cualquier desplazamiento horneado en la malla unitaria se estira distinto en cada rama.
- **Tampoco se usan derivadas de pantalla (`dFdx`/`dFdy`).** Es la vía de Three (`perturbNormalArb`) y la más corta, pero en WebGL 1 exige `GL_OES_standard_derivatives`, y la directiva `#extension` **no se puede inyectar**: `onBeforeCompile` entrega el cuerpo del shader *después* del prefijo que Three antepone, y ese prefijo ya contiene sentencias `precision`, que son tokens no-preprocesador. Un `#extension` detrás de ellas es GLSL inválido en los drivers estrictos — es decir, un fallo de compilación silencioso justo en los dispositivos antiguos que RNF20 obliga a cubrir.

La vía que queda es la barata: **las mallas generadas son rejillas**. `makeTubeGeometry` recorre `(anillo, radial)` y `makeBlobGeometry` recorre `(fila, columna)`; emitir `uv` y una tangente por vértice son unas pocas líneas en cada uno (T2) y **no mueve un solo vértice**. Con eso el fragmento perturba la normal con `T` y `B = cross(normal, T)` sin necesitar derivadas ni varyings extra más allá de la tangente en espacio de vista.

Ventaja de propina: las nervaduras corren **a lo largo** de la rama, que es lo que hace la corteza de verdad, en vez de flotar en un ruido de mundo ajeno a la pieza.

#### D4 — La variación entre instancias viaja como **un solo float instanciado**, sembrado por el PRNG que ya existe

`aSeed`, un `THREE.InstancedBufferAttribute` de 1 componente en las geometrías de rama, nudo y follaje, rellenado en `buildInstancedMeshes()` con `seededRandom(BRANCH_SEED + …)`. Desplaza el ruido de corteza por rama y fija el tono, el matiz y el desfase de secado de cada hoja. **Sin una malla nueva, sin una llamada de dibujo nueva y sin una geometría nueva**, que es la regla de `modelo.md`: el detalle entra dentro de las instancias que ya existen.

> **Alternativa descartada — `InstancedMesh.setColorAt()` (`instanceColor`).** Está soportado de serie por el bundle (`USE_INSTANCING_COLOR` aparece en `three.min.js`) y no requiere atributo propio. Pero solo **multiplica el color difuso**: da tono y no matiz, y no puede desincronizar el secado de cada hoja (D6), que es la mitad de CA-45.02. Y cuesta tres floats por instancia en vez de uno.

#### D5 — El ruido GLSL es una función pura de `(uv, aSeed, uNoiseSeed)`: el determinismo es por construcción

CA-45.01 exige el mismo PRNG con semilla fija que la geometría y **prohíbe** `Math.random()` o semillas variables. Un ruido de valor sobre `hash(floor(p))` con suavizado de Hermite —la traducción a GLSL de `makeNoise`/`makeSurfaceNoise`, que ya viven en el archivo— es determinista por definición: no hay estado, no hay reloj, no hay azar. Las dos únicas entradas de entropía son `uNoiseSeed`, derivado de `BRANCH_SEED`, y `aSeed`, que produce `seededRandom`.

Por tanto **misma etapa + misma salud ⇒ imagen idéntica**, en cada apertura de la pantalla y también tras una degradación de la sonda, que es el caso que de verdad lo pone a prueba. Verificación: `grep -c "Math.random"` sobre el archivo debe seguir dando **1 y solo en prosa**.

#### D6 — El marchitado orgánico es **desincronización por hoja + manchas**, sobre el degradado de salud que ya existe

Lo que falta para que el secado se lea natural no es otra rampa de color: la rampa continua de `HEALTH_STOPS_*` ya cumple CA-45.02 en su parte de continuidad, y la caída y el encogimiento de la copa son de `HU-38`/`HU-44`. Lo que falla es que **las 96 hojas cambian de color exactamente a la vez y exactamente al mismo tono**.

Se añade un uniform `uHealth` al material de follaje y, con `aSeed`:

- **desfase de secado por hoja** (±): unas hojas amarillean antes que otras y la copa se seca a manchas, no en bloque;
- **manchas dentro de cada hoja**, cuyo contraste **crece** al bajar la salud: verde vibrante casi uniforme arriba, moteado seco y quebradizo abajo;
- **desaturación progresiva** hacia el marrón oscuro en salud crítica.

Todo es función continua de `uHealth`, que `TRANSITION_MS` + `easeInOut` ya mueven suave: la continuidad de CA-45.02 se conserva por construcción. La consecuencia mecánica es que `applyHealth()` pasa a actualizar también un uniform, no solo un `setHex`.

#### D7 — La degradación de CA-45.03 es **una columna de la tabla**, no una rama nueva de código

`QUALITY_PRESETS` gana dos columnas:

| | `materialNoise` (octavas) | `barkBump` (amplitud) | sombras (ya existía) |
|---|---|---|---|
| `high` | 2 | completa | ✅ |
| `medium` | 1 | mitad | ❌ |
| `low` | **0** | **0** | ❌ |

Con `materialNoise = 0` la fábrica **no instala el hook** y devuelve un `MeshLambertMaterial` pelado: eso es, al pie de la letra, «los materiales usan shaders básicos (tipo Lambert)» y «se desactiva el bump/displacement mapping». Las sombras ya estaban apagadas en `low` desde `HU-38`. Y el árbol de `low` sigue siendo **el mismo árbol orgánico de `HU-44`** con materiales simples, que es lo que la CA exige y lo que impide que se lea como un render a medio compilar.

La sonda y `QUALITY_DOWNGRADE` **no cambian** (regla de negocio 7).

#### D8 — El fallo de compilación se intercepta por `renderer.debug.onShaderError`, con la auditoría de `renderer.info.programs` como segunda red

La nota técnica de CA-45.04 propone auditar `renderer.info.programs` tras el primer render. Se hace, y además se instala algo más preciso: **`renderer.debug.onShaderError(gl, program, glVertexShader, glFragmentShader)`**, el hook documentado que Three invoca exactamente cuando un programa no enlaza o no compila. Está presente en el bundle (`grep onShaderError three.min.js`).

- El hook emite `TreeBridge.onFailure('shader-compile: …')` **de inmediato**.
- El barrido de `renderer.info.programs` tras el primer render recoge lo que el hook no haya visto —la forma del campo `diagnostics` ha cambiado entre versiones de Three, así que se lee a la defensiva y nunca se confía solo en él.

Ambos caminos desembocan en el fallback nativo de `HU-37` **sin mensaje de error**, que es lo que pide la CA. Del lado nativo no hace falta nada: `Tree3DView` acepta `onFailure` después de `onReady` (`Tree3DView.kt:60`), la misma red que `HU-44` aprovechó para `webglcontextlost`.

`webglcontextlost` ya está enganchado desde `HU-44` (`bindContextLoss`, `tree.js:1789`). CA-45.04 lo vuelve a pedir: **se verifica, no se reescribe.**

#### D9 — Los shaders se compilan **antes** de medir, con `renderer.compile()`

`PROBE_WARMUP_FRAMES = 5` ya supera los «2 o 3 fotogramas» que pide la nota técnica de CA-45.03. Pero el objetivo real de esa nota es que la compilación de los shaders de ruido **no caiga dentro de la ventana medida**, y cinco fotogramas de margen es una apuesta, no una garantía. `renderer.compile(scene, camera)` antes del primer `render()` la saca del bucle por completo, y de paso le da al barrido de D8 algo que auditar **antes** de pintar nada.

El warm-up sube igualmente a 8: el driver puede diferir la compilación real (`KHR_parallel_shader_compile`), y el coste de ocho fotogramas de margen —unos 130 ms de medición que no se usan— es despreciable frente a degradar a `low` un dispositivo que corría a 60 FPS. Es el error que la nota técnica nombra.

#### D10 — Ni una línea de Kotlin, tampoco en `TreeRenderQuality.kt`

La historia autoriza tocar sus parámetros «de ser necesario». No lo es: los tres escalones ya existen y lo que esta historia añade son **columnas de la tabla de `tree.js`**, que es donde `HU-44` puso las otras cuatro. Cambiar los umbrales de memoria o de núcleos movería qué dispositivos entran en cada escalón —una decisión sobre el parque de dispositivos, no sobre materiales— y encima invalidaría los cuatro tests de `ui/tree/` sin que ninguna CA lo pida.

Consecuencia verificable, la misma de `HU-44`: `git diff --name-only -- Tension/app/src/main/java Tension/app/src/test` **vacío**.

#### D11 — El registro explícito de liberación se extiende a los materiales

`HU-44` aprendió (su D10) que recorrer el grafo no basta para las **geometrías**, porque se comparten entre mallas. Con esta historia pasa lo mismo con los **materiales**: `trunkMaterial` lo comparten `branchMesh`, `junctionMesh` y el pie del tronco, y `stemMaterial` los tres hijos de la Semilla, así que `disposeGroup()` los libera por duplicado o por triplicado en cada reconstrucción — que ocurre al cambiar de etapa **y cada vez que la sonda degrada la calidad**. Se añade `generatedMaterials[]` con vaciado explícito, espejo exacto de `generatedGeometries[]`, y `disposeGroup` deja de liberar materiales.

#### D12 — Cada material declara su `customProgramCacheKey`

Es el defecto silencioso que este diseño trae de serie y por eso va como decisión y no como detalle. Three cachea los programas compilados por una clave que, para un material con `onBeforeCompile`, sale por defecto de **`material.onBeforeCompile.toString()`**. Dos materiales de este archivo con hooks distintos pero texto parecido —o peor, con la misma función compartida— compartirían programa, y **el follaje se dibujaría con el shader de la corteza** sin que nada fallara ni avisara.

Se sobrescribe `customProgramCacheKey()` devolviendo `'tree:' + kind + ':' + octaves + ':' + bump`, que es lo que de verdad distingue dos programas aquí. Es también lo que hace que una degradación de calidad **reutilice** el programa ya compilado en vez de recompilarlo.

---

### Reutilización

| Componente | Decisión | Motivo |
|---|---|---|
| `seededRandom(seed)` — `tree.js:611` | **Se reutiliza** | Es el PRNG con semilla fija que CA-45.01 exige. De él sale `aSeed` (D4) y de él deriva `uNoiseSeed` (D5). No se escribe un segundo generador |
| `makeNoise()` / `makeSurfaceNoise()` — `tree.js:640`, `:669` | **Se reutilizan como referencia, se traducen a GLSL** | El ruido de CPU sigue sirviendo a la geometría de `HU-44` y no se toca. El de GPU es su equivalente en el shader: mismo esquema (tabla con semilla + suavizado de Hermite), distinto lenguaje. No se sustituye uno por otro |
| `makeTubeGeometry()` / `makeBlobGeometry()` — `tree.js:711`, `:775` | **Se reutilizan y se amplían** | T2 les añade `uv` y `aTangent`. **Ni un vértice se mueve**: la posición, el índice y las normales salen idénticos |
| `finishGeometry()` — `tree.js:683` | **Se reutiliza y se amplía** | Pasa a adjuntar los atributos nuevos y sigue siendo el único sitio con `computeVertexNormals()` y el registro de liberación |
| `generatedGeometries[]` + `disposeGeneratedGeometries()` | **Se reutiliza; se clona para materiales** | D11 |
| `HEALTH_STOPS_*` / `foliageColor()` / `lerpHex()` | **Se reutilizan sin tocar** | El degradado de salud es correcto y es el que iguala el render al ícono nativo en 0, 25, 50 y 100. D6 lo **modula**, no lo sustituye |
| `applyHealth()` / `applyDroop()` / `updateSkeletonMatrices()` | **Se reutilizan; `applyHealth` se amplía** | Añade la actualización de `uHealth` (D6). La caída y las matrices no cambian |
| `QUALITY_PRESETS` / `QUALITY_DOWNGRADE` / `probe()` | **Se reutilizan, se añaden dos columnas** | D7. El mecanismo de calidad no se reinventa (regla 7) |
| `buildInstancedMeshes()` — `tree.js:1041` | **Se reutiliza y se amplía** | Rellena `aSeed` (D4). Las tres mallas y su reparto de instancias no cambian |
| `reportFailure()` — `tree.js:59` | **Se reutiliza** | D8 lo usa para el fallo de compilación, igual que `HU-44` lo usó para `webglcontextlost` |
| `reportBudget()` — `tree.js:41` | **Se reutiliza, se amplía** | Añade las octavas de ruido y la amplitud de bump efectivas, para poder leer en logcat con qué material se está dibujando |
| `bindContextLoss()` — `tree.js:1789` | **Se reutiliza sin tocar** | CA-45.04 lo vuelve a pedir; ya lo entregó `HU-44`. Se verifica |
| `init()` / `resize()` / `renderFrame()` | **Se reutilizan; `init` se amplía** | D8 (hook de error) y D9 (`renderer.compile`) entran en `init`. El bucle de render a demanda no cambia |
| `Tree3DView.kt` / `TreeBridge.kt` / `TreeRenderQuality.kt` / `TreeWebViewSupport.kt` / `TreeScreen.kt` / `TreeIcon` | **Se reutilizan sin modificar** | D10 |
| `tools/capturar-arbol.ps1` / `tools/seed-arbol.ps1` / `TreeTestScenarios.kt` | **Se reutilizan sin modificar** | Son el instrumento de CA-45.07. El catálogo ya tiene los 23 escenarios que `HU-44` necesitó, incluidos el 22 y el 23 de banda media |
| Arnés de humo de `HU-44` (scratchpad, fuera del repositorio) | **Se reutiliza y se amplía** | Ya carga el `three.min.js` real y ejercita 4 etapas × 3 calidades. T13 le añade las comprobaciones de los atributos nuevos y del determinismo de `aSeed` |
| `makeBarkMaterial()` / `makeFoliageMaterial()` / ruido GLSL / `aSeed` | **Se crean** | No hay equivalente: el proyecto no tiene ni un shader propio ni ninguna utilidad de materiales fuera de `tree.js`, y Three.js no trae un material de corteza procedural. Viven dentro del mismo IIFE, **sin archivo nuevo** |

---

### Tareas de Implementación

#### Fase 0 — Línea base del presupuesto (CA-45.08)

- [ ] **T1: Medir el peso del APK antes de tocar nada** — `Tension/app/build/outputs/apk/release/`

  `./gradlew :app:assembleRelease` y registrar el tamaño exacto en bytes, más el tamaño en disco de `tree.js` (hoy **81 029 bytes**). **Debe correr antes de T2**: después no hay línea base, solo estimación. Se anota en `dev-record.md`. `JAVA_HOME` del sistema apunta a un JDK 7 y Gradle aborta: exportar `JAVA_HOME=C:/Program Files/Eclipse Adoptium/jdk-17.0.20.101-hotspot` para el build, **sin modificar ninguna configuración del proyecto** (mismo hallazgo que HU-36, 37, 38 y 44).

#### Fase 1 — Lo que las mallas tienen que entregarle a los materiales (CA-45.01)

- [ ] **T2: Emitir `uv` y `aTangent` en las dos fábricas de geometría** — `assets/tree/tree.js` (Base: `makeTubeGeometry()` `:711`, `makeBlobGeometry()` `:775`, `finishGeometry()` `:683`)

  Ambas recorren una rejilla, así que la `uv` sale del propio índice del bucle y la tangente es la dirección de avance a lo largo del anillo (tubo) o de la columna (grumo), normalizada. `finishGeometry` los adjunta y sigue llamando a `computeVertexNormals()`. **Invariante que hay que comprobar, no suponer: ni una posición ni un índice ni una normal cambian.** El arnés de T13 compara el hash de posiciones contra la corrida de `HU-44`.

- [ ] **T3: Atributo instanciado `aSeed`, sembrado por el PRNG existente** — `assets/tree/tree.js` (Base: `buildInstancedMeshes()` `:1041`, `seededRandom()` `:611`)

  Un `THREE.InstancedBufferAttribute` de un componente por cada una de las tres `InstancedMesh`, relleno con `seededRandom(BRANCH_SEED + …)` en el mismo orden en que se emiten las instancias (D4). Se registra para liberación con el resto. **Sin `Math.random()`** (CA-45.01, D5).

#### Fase 2 — La biblioteca de shaders (CA-45.01, CA-45.02, CA-45.03)

- [ ] **T4: Ruido determinista en GLSL** — `assets/tree/tree.js` (Base: `makeNoise()` `:640` / `makeSurfaceNoise()` `:669`)

  Cadenas GLSL compartidas: `hash`, ruido de valor 2D con suavizado de Hermite y una suma de 1–2 octavas gobernada por `materialNoise` (D7). Traducción del esquema de CPU que ya existe, no un generador distinto. Función pura: sin estado, sin reloj, sin azar (D5).

- [ ] **T5: `makeBarkMaterial(kind)` — relieve y grano de corteza** — `assets/tree/tree.js` (D1, D3)

  `MeshLambertMaterial` + `onBeforeCompile`. Inyecta: en el vértice, el paso de `uv`, `aSeed` y la tangente llevada a espacio de vista junto a la normal (sustituyendo `<defaultnormal_vertex>`); en el fragmento, la altura de corteza, sus derivadas en `u` y `v` por diferencias finitas, la perturbación de la normal con `T` y `B = cross(normal, T)` (amplitud `barkBump`) y la modulación del albedo —grietas oscuras, crestas claras—. Sirve al tronco, a las ramas, a los nudos, al pie del tronco y, con otros parámetros, al montículo y al tallo de la Semilla. **`kind` entra en la clave de caché de D12.**

- [ ] **T6: `makeFoliageMaterial()` — variación tonal y secado orgánico** — `assets/tree/tree.js` (D4, D6)

  Mismo mecanismo, sin bump. Variación de tono y matiz por instancia desde `aSeed`, manchas dentro de cada hoja desde la `uv`, y `uHealth` gobernando desfase de secado por hoja, contraste de las manchas y desaturación. `applyHealth()` pasa a actualizar el uniform además del color base. La rampa de `HEALTH_STOPS_*` sigue mandando sobre el tono medio: **este material la modula, no la sustituye** (CA-45.02).

- [ ] **T7: Clave de caché de programa y registro de liberación de materiales** — `assets/tree/tree.js` (D11, D12)

  `customProgramCacheKey()` en cada material con hook, devolviendo `'tree:' + kind + ':' + octaves + ':' + bump`. `generatedMaterials[]` con vaciado explícito en `rebuildTree()`, y `disposeGroup()` deja de liberar materiales para no hacerlo por duplicado. **T7 no es un remate: sin la clave, el follaje se dibuja con el shader de la corteza y nada falla de forma observable.**

#### Fase 3 — Enganche con el modelo (CA-45.02, CA-45.03)

- [ ] **T8: Sustituir los cuatro materiales planos por la fábrica** — `assets/tree/tree.js` (`buildTree()` `:1347`, `buildMound()` `:1275`, `buildSeed()` `:1218`)

  `trunkMaterial` y el material del montículo → T5 con parámetros distintos (madera / tierra). `foliageMaterial` → T6. El `stemMaterial` de la Semilla → T6 para los cotiledones y T5 para el tallo, porque CA-45.02 y la lección de `HU-44` prohíben que una etapa se quede con otro tratamiento. Ninguna llamada de dibujo nueva.

- [ ] **T9: Columnas `materialNoise` y `barkBump` en `QUALITY_PRESETS`** — `assets/tree/tree.js` (D7, CA-45.03)

  `high` 2 octavas + bump completo · `medium` 1 octava + medio bump · `low` **0 octavas ⇒ material Lambert pelado, sin hook, sin bump**. Sombras ya apagadas en `medium` y `low`. `reportBudget` informa las dos columnas efectivas para poder leer en logcat con qué material se dibujó.

#### Fase 4 — Robustez del render (CA-45.04)

- [ ] **T10: Interceptar el fallo de compilación de shaders** — `assets/tree/tree.js` (Base: `reportFailure()` `:59`, `init()` `:1866`) (D8)

  `renderer.debug.onShaderError = …` → `reportFailure('shader-compile: …')`, instalado **antes** de construir el árbol. Más el barrido defensivo de `renderer.info.programs` tras el primer render, leyendo `diagnostics` sin confiar en su forma. Ambos llevan al fallback nativo de `HU-37` **sin mensaje de error**. Sin una línea de Kotlin: `Tree3DView` ya acepta `onFailure` después de `onReady`.

- [ ] **T11: Compilar antes de medir y ampliar el warm-up** — `assets/tree/tree.js` (D9, CA-45.03)

  `renderer.compile(scene, camera)` antes del primer `render()`. `PROBE_WARMUP_FRAMES` de 5 a 8. El resto de la sonda —`PROBE_FRAMES`, `PROBE_BUDGET_MS`, `QUALITY_DOWNGRADE`— **no se toca** (regla 7).

- [ ] **T12: Verificar `webglcontextlost`, no reescribirlo** — `assets/tree/tree.js:1789` (CA-45.04)

  Ya lo entregó `HU-44`. Comprobar que sigue enganchado, que sigue **sin** `preventDefault()` y que la CA queda cubierta por el código existente. Si se comprueba y está, se anota como verificado y no se toca.

#### Fase 5 — Verificación sin emulador

- [ ] **T13: Sintaxis, arnés de humo y suite de regresión** — `node --check`, arnés en el scratchpad, `./gradlew :app:testDebugUnitTest`

  `node --check Tension/app/src/main/assets/tree/tree.js`: **ningún paso del build valida el JS** y un error de sintaxis se manifiesta solo como fallback silencioso en el dispositivo. El arnés de `HU-44` —fuera del repositorio, ADR-021— se amplía para comprobar: que `uv`, `aTangent` y `aSeed` están presentes y bien dimensionados en las tres mallas y las cuatro etapas; que **el hash de posiciones y de matrices de instancia sigue siendo el de `HU-44`** (T2 no debe mover nada); que `aSeed` es idéntico entre reconstrucciones (D5); y que el presupuesto de triángulos no se movió. La suite Kotlin corre como **no-regresión**: 952 tests, 0 fallos, **sin un test nuevo** — la historia no cambia una línea de Kotlin (D10). *Lo que el arnés no puede verificar es el GLSL; ver Riesgos.*

#### Fase 6 — Verificación visual (CA-45.03, CA-45.06, CA-45.07)

- [ ] **T14: Juego de capturas y aprobación del PO** — `tools/capturar-arbol.ps1` (CA-45.07)

  Lo ejecuta **la persona**, no el agente (skill `tension-arbol-3d-visual`). `-Instalar` es obligatorio en el primer comando: los assets del WebView viajan dentro del APK. Tres tiras, una por banda de salud, en orden de crecimiento:

  ```powershell
  .\tools\capturar-arbol.ps1 -Instalar -Escenarios 01,03,07,12 -Etiqueta v1-alta
  .\tools\capturar-arbol.ps1 -Escenarios 22,08,23 -Etiqueta v1-media
  .\tools\capturar-arbol.ps1 -Escenarios 20,21,14 -Etiqueta v1-marchito
  ```

  Son las **10 capturas alcanzables**, sin repetir ninguna — ver *Riesgos* sobre por qué no son 12. Lo que se mira, en orden de probabilidad de fallo: **(1)** que ningún recuadro salga negro —sería fallo de compilación, no del emulador—; **(2)** que la corteza tenga grano y no color plano; **(3)** que las hojas no compartan un único tono; **(4)** que la banda media se lea como secado a manchas y no como un cambio de tinte en bloque; **(5)** `MargenSup`/`MargenInf` > 0 y `AltoPx` creciente de `01` a `12`, que es la no-regresión de `HU-44`; **(6)** la línea `calidad=` antes de comparar medidas entre tiras.

- [ ] **T15: Verificar la degradación estricta en `low`** — query string + logcat (CA-45.03)

  Forzar `?quality=low` y comprobar que el árbol **sigue siendo el árbol orgánico de `HU-44`** con material plano: sin bump, sin sombras, silueta y bandas de salud legibles, y **nunca** un render a medio compilar. Comprobar en `adb logcat -s chromium` la línea `[tree] construido … materialNoise=0 bump=0`. Comprobar que `degradado` no aparece de forma **sistemática** en un dispositivo que antes no degradaba, que es la señal de que los shaders se comieron el presupuesto.

#### Fase 7 — Presupuesto, documentación y cierre (CA-45.05, CA-45.08, CA-45.09, CA-45.10)

- [ ] **T16: Medir el incremento del APK y auditar el presupuesto de assets** — `dev-record.md` (CA-45.08, CA-45.05)

  `assembleRelease` de nuevo y diferencia contra T1, en bytes y en porcentaje, más el tamaño en disco de `tree.js` antes y después. La auditoría de CA-45.05 es de una línea y tiene que quedar escrita: **0 bytes de textura** (D2), `ls assets/tree/*.webp` vacío, `grep -c "TextureLoader\|base64" tree.js` = 0, ningún `.glb`/`.gltf` en el APK. Es un entregable de dos CAs, no una nota al pie.

- [ ] **T17: ADR de la estrategia de materiales** — `docs/architecture/architecture_blueprint.md` (CA-45.10)

  **ADR-023**, complementaria de ADR-022 (que cubre la geometría) y con la nota correspondiente en ADR-021, cuya frase «y sin texturas» pasa a querer decir «sin texturas de imagen, con materiales procedurales». Alternativas descartadas que la CA exige registrar: modelos `.glb`/`.gltf` y texturas descargadas (prohibidos por la frontera del PO y por RNF09), `ShaderMaterial` propio (D1), texturas `.webp` empaquetadas (D2), *displacement* y derivadas de pantalla (D3), `instanceColor` (D4). Consecuencias: el reparto por calidad de D7 y el incremento de APK de T16. Actualizar también la ficha de `tree.js` en §2.1 (`UI-01`), las fronteras verificables de §3 y la trazabilidad de §4 con la entrada de `HU-45`.

- [ ] **T18: Verificar las fronteras congeladas** — `git diff --stat` (CA-45.09)

  `git diff --name-only -- Tension/app/src/main/java Tension/app/src/test` **vacío** (D10). `system_definition_document.md` e `interfaces_contract.md` **sin modificar** —el puente sigue llevando exactamente salud y etapa—. Ningún `.glb`/`.gltf` ni `.webp` en `assets/`. Y la comprobación de que `HU-44` no ha sufrido regresión: silueta, etapas, escala por etapa, órbita, topes de zoom y reinicio de cámara idénticos.

- [ ] **T19: Registrar el desarrollo** — `dev-record.md` (nuevo, patrón de `HU-44`), `index.md` (fases y métricas), `cambios.md`

---

### Riesgos y observaciones

**El GLSL no se puede verificar sin dispositivo, y ese es el riesgo número uno de esta historia.** `node --check` valida el JavaScript, no las cadenas de shader que viajan dentro de él: para el analizador son texto. El arnés de humo carga el `three.min.js` real pero **stubea el renderer**, así que nunca llega a compilar un programa. Un paréntesis mal cerrado en el fragmento, un `varying` declarado en un lado y no en el otro, o un `attribute` que Three no encuentra, se manifiestan **solo en el dispositivo** y —esto es lo caro— **no como una excepción**, sino como un árbol negro. Por eso T10 no es un remate defensivo sino parte del núcleo: es lo que convierte ese modo de fallo en el ícono nativo de `HU-37`. Y por eso la primera cosa que se mira en la tira de T14 es si algún recuadro salió negro.

**El coste de los shaders de ruido en gama baja es el riesgo que la historia declara, y tiene salida escrita.** Un ruido de 2 octavas por fragmento sobre una copa que cubre buena parte del cuadro es, con diferencia, lo más caro que ha tenido este proyecto — y a diferencia de la geometría, el coste va por **píxel**, no por instancia, así que no se ve en el presupuesto de triángulos. La salida es D7: `low` no tiene hook en absoluto, y la sonda sigue pudiendo bajar un escalón. La señal de alarma concreta está en T15: que `degradado` aparezca **siempre** en el emulador `Medium_Phone_API_35`, que hoy se queda en `medium` de forma intermitente.

**`high` no se puede verificar en el emulador de la máquina.** `Medium_Phone_API_35` resuelve `medium`, y `high` requiere un AVD de 8 núcleos (eje B3 del README de pruebas). Es el escalón donde vive el bump completo y las 2 octavas, es decir, exactamente donde CA-45.01 pone la vara. Mitigación: forzar `?quality=high` por query string en el emulador —que ejercita el shader aunque el dispositivo no sea el previsto— y dejar anotado en `dev-record.md` qué se verificó en hardware real y qué no. Es la misma limitación que `HU-44` documentó y no la resuelve esta historia.

**Las «12 capturas» de CA-45.07 no son alcanzables tal como están escritas, por la misma razón que en `HU-44`.** La etapa Semilla existe **si y solo si** hay 0 sesiones cerradas (`TreeGrowthStageRule.resolve`), y con 0 sesiones no hay última sesión, así que `TreeHealthRule.calculate(null)` devuelve **100 siempre**. La Semilla tiene **una sola banda de salud alcanzable en el dominio**, y falsear `tree_state` no sirve: `TreeRepositoryImpl.recalculate()` corre al abrir la pantalla y lo sobrescribe. Se entregan **10 capturas reales** —Semilla ×1 + Brote/Joven/Maduro ×3—. El PO ya aceptó este reparto al aprobar CA-44.08; se vuelve a anotar porque la CA de esta historia repite el número.

**La colisión de la caché de programas (D12) es el defecto que este diseño trae de fábrica.** Three deriva la clave de caché de `onBeforeCompile.toString()`, así que dos materiales con hooks parecidos comparten programa **sin que nada falle de forma observable**: el follaje se dibujaría con el shader de la corteza y el resultado sería «raro» en vez de «roto». Es exactamente la clase de defecto que `HU-44` atrapó con el arnés antes de gastar una ronda de capturas (sus normales invertidas), y aquí no hay arnés que lo atrape. Por eso T7 es tarea propia y no una línea dentro de T5.

**T2 tiene un invariante que hay que comprobar, no suponer.** Añadir `uv` y `aTangent` a las dos fábricas de geometría no debe mover ni un vértice ni cambiar una normal. Es fácil romperlo al reordenar los bucles, y el síntoma —una silueta ligeramente distinta— se confundiría con «así se ve con los materiales nuevos» durante varias rondas. Por eso T13 compara el hash de posiciones contra la corrida de `HU-44` en vez de fiarse de la vista.

**`tree.js` sigue sin pruebas automatizadas, y esta historia tampoco es la que debe cambiarlo.** ADR-021 lo dejó escrito como precio asumido: *«el proyecto no tiene ni tendrá cadena de build JavaScript»*. Lo que sí se hace es lo mismo que en `HU-44` y un poco más: `node --check`, el arnés de humo ampliado con los invariantes de T2 y el determinismo de `aSeed`, y las tiras de T14 como verificación formal. **La suite Kotlin no es cobertura de esta historia; es su prueba de no-regresión**, y su valor está en que debe salir idéntica.

**Cero texturas es una decisión, no un olvido (D2).** CA-45.05 está escrita como condicional —«cuando el acabado exigiera texturas»— y ordena priorizar los shaders matemáticos. El plan elige la rama sin texturas, con lo que el presupuesto de < 100 KB se cumple con 0 bytes, la prohibición de Base64 queda sin objeto, y la condición de `onReady()` sobre las promesas de `TextureLoader` se cumple por vacío junto con el *texture popping* que describe. **Se declara aquí para que el PO lo vea antes de la implementación**, no después: si al mirar las tiras echa en falta un detalle que el ruido no da, añadir una `.webp` más tarde es aditivo y no invalida nada.

**El árbol sigue sin decidir nada.** La excepción de alcance de ADR-020 se mantiene sin ampliarse. Esta historia es, otra vez, **un solo archivo de producto**: `assets/tree/tree.js`.

---

### Validación manual (no automatizable)

1. **CA-45.01 (materiales)** — Tira `v1-alta`, calidad forzada a `high`. La corteza tiene grano y grietas, no color plano; el follaje tiene tonos distintos entre hojas y dentro de cada hoja; el conjunto se lee como material con presencia física y no como pintura lisa.
2. **CA-45.01 (determinismo)** — Abrir y cerrar la pantalla del árbol cinco veces seguidas **sin cambiar de estado**: corteza y follaje idénticos. El caso que de verdad lo rompe es la degradación de la sonda, que reconstruye unos segundos después de abrir: comprobar que el árbol **no cambia de piel** al degradar.
3. **CA-45.02 (marchitado orgánico)** — Escenarios `12` → `22`/`23` → `14`: la copa se seca **a manchas y desincronizada**, de verde vibrante a amarillo y a marrón quebradizo, sin ningún salto de textura ni de color. Cruzar la transición entera con la app abierta (cambio de día) y comprobar que los 900 ms son continuos.
4. **CA-45.03 (degradación estricta)** — Forzar `?quality=low`: material plano, sin bump, sin sombras, silueta y bandas de salud legibles. Forzar `?quality=high` y comparar. Logcat: `[tree] construido … materialNoise=… bump=…`.
5. **CA-45.04 (fallo de shaders)** — Provocar un fallo de compilación deliberado en una copia local del asset y comprobar que aparece **el ícono nativo teñido**, con puntaje, días y mensaje intactos, **sin mensaje de error y sin árbol negro**. Es la comprobación que justifica T10 y la única forma de saber que la red funciona.
6. **CA-45.04 (pérdida de contexto)** — Abrir la pantalla del árbol, mandar la app a segundo plano, abrir varias apps pesadas y volver: fallback nativo, sin pantalla en blanco ni congelada.
7. **CA-45.06 (presupuesto)** — Cronometrar de la tarjeta de Inicio al árbol renderizado: **< 1 s** en gama media. Rotación fluida con el dedo. El arranque de la app y el resto de la navegación, sin cambio.
8. **CA-45.09 (sin regresión)** — Silueta, cuatro etapas, escala por etapa y estados por salud **iguales que en `HU-44`**. Órbita, topes de elevación, pinza a los dos extremos y reinicio de cámara al reentrar **iguales que en `HU-38`**. Fondo transparente correcto en claro y oscuro (RNF23). Tarjeta de Inicio nativa.
