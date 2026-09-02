## Refinamiento Técnico (Developer)
**Autor**: Esteban Colorado González | **Fecha**: 2026-09-02

---

### Contexto

`HU-37` dejó la historia servida: el árbol funciona completo con representación nativa y su última tarea (D12) fue **aislar el área del árbol en un composable privado de tamaño fijo**. Esa costura es literalmente el alcance de esta historia.

```kotlin
// TreeScreen.kt:158 — el único punto que esta historia sustituye
@Composable
private fun TreeVisual(stage: TreeGrowthStage, healthScore: Int, hasHistory: Boolean) {
    Box(modifier = Modifier.size(TREE_AREA_SIZE), contentAlignment = Alignment.Center) {
        TreeIcon(stage = stage, healthScore = healthScore, hasHistory = hasHistory, size = TREE_AREA_SIZE)
    }
}
```

Todo lo que la historia pide fuera de ese bloque **ya existe y no se toca**: `TreeState` con sus dos dimensiones, `TreeViewModel` recalculando antes de observar, la ruta `tree`, el bloque de estado, el respaldo v12 y los cinco colores semánticos del tema.

**Feature análoga leída completa: HU-03 — media visual del diccionario de ejercicios.** Es el **único** precedente de assets empaquetados y de consumo desde código:

```kotlin
// ExerciseDetailScreen.kt:245 — el patrón de lectura de assets del proyecto
context.assets.open("exercises/$mediaResource.png").use { stream -> ... }
```

De ahí se hereda la convención de carpeta plana bajo `assets/` y el `try/catch` que degrada en silencio cuando el recurso no está. **HU-37** aporta la segunda mitad del patrón: el composable de fallback ya escrito y probado.

#### El hallazgo que define el alcance

**No existe ningún `WebView` en el proyecto, ni ninguna dependencia JavaScript, ni el permiso `INTERNET`.** Verificado sobre `AndroidManifest.xml`, `app/build.gradle.kts` y una búsqueda de `WebView` en todo `src/main`. Las tres ausencias tienen consecuencias distintas:

1. **Sin `WebView`**: es la primera integración de una vista Android nativa (`AndroidView`) con ciclo de vida propio dentro de una app que hasta hoy es Compose puro. El riesgo declarado por la historia vive aquí.
2. **Sin dependencia JS**: Three.js entra como **asset versionado en Git**, no como artefacto de un pipeline npm que el proyecto no tiene y que esta historia no va a introducir.
3. **Sin permiso `INTERNET`**: RNF09 deja de ser una promesa y pasa a ser una imposibilidad del sistema operativo. Un `WebView` sin ese permiso **no puede** cargar contenido remoto aunque alguien escriba la URL. Es la garantía más fuerte de CA-38.04 y no cuesta una línea de código: cuesta **no** añadir el permiso.

#### La consecuencia que ninguna CA cubre

**El área del árbol mide 180 × 180 dp** (`TREE_AREA_SIZE`, HU-37) y CA-38.01 prohíbe reorganizar el layout. Es un lienzo pequeño para un gesto de órbita: el wireframe del preview sugiere un área visualmente mayor, cerca de la mitad de la pantalla. **Se conserva 180 dp** porque la CA es explícita — «el puntaje, los días, el mensaje contextual y la barra superior permanecen sin cambios», «el bloque de estado inferior no se reorganiza» — y ampliar el área desplazaría todo lo de abajo. Queda anotado como observación para producto: si el gesto resulta incómodo en dispositivo, agrandar el área es una decisión de la historia siguiente, no de esta.

#### Lo que NO se toca

Esta historia **solo** cambia cómo se dibuja el árbol. La lista es la frontera verificable sobre el diff:

- **Dominio y datos del árbol**: `TreeHealthRule`, `TreeGrowthStageRule`, `TreeState`, `TreeGrowthStage`, `TreeRepository`, `TreeRepositoryImpl`, `TreeStateEntity`, `TreeStateDao`, `GetTreeStateUseCase`, `RecalculateTreeStateUseCase`.
- **Esquema y respaldo**: `TensionDatabase` (sigue en v19), `BackupRepositoryImpl` (sigue en v12), `ImportBackupUseCase`, `DatabaseModule`, `RepositoryModule`. **Ninguna tabla nueva, ninguna columna nueva, ninguna versión nueva.**
- **Momentos de recálculo**: `CloseSessionUseCase`, `MainViewModel`. El árbol se recalcula exactamente cuando ya se recalculaba.
- **Inicio**: `HomeScreen`, `HomeViewModel`, `HomeUiState`. La tarjeta sigue siendo nativa **de forma permanente** (RNF01, regla de negocio 2).
- **Navegación**: `NavigationRoutes`, `TensionNavHost`. Sin rutas nuevas.
- **`TreeIcon`**: no se modifica ni se elimina. Pasa a fallback y se **reutiliza tal cual** desde el mismo composable.
- **Textos**: `strings.xml` no gana ni pierde ninguna cadena (regla de negocio de la sección *Interfaz*).
- **Motor de decisión**: la excepción de ADR-020 sigue vigente sin ampliarse.
- **`system_definition_document.md`**: no se modifica (CA-38.07).

| Capa | Artefacto | Responsabilidad | Dependencias | Artefactos asociados |
|---|---|---|---|---|
| `UI-01` | `TreeVisual` (privado en `TreeScreen.kt`) | Elegir representación: 3D si se puede, ícono si no | `Tree3DView`, `TreeIcon` | Único punto de sustitución (D12 de HU-37) |
| `UI-01` | `TreeIcon` (`ui/components`) | Etapa → recurso, salud → tinte | `LocalTensionSemanticColors` | 4 drawables vectoriales. **Sin cambios, pasa a fallback** |
| `UI-01` | `ExerciseDetailScreen:230-249` | Lectura de asset con degradación silenciosa | `context.assets` | `assets/exercises/*.png` (37 PNG) |
| `UI-01` | `TensionSemanticColors` | 5 roles de árbol con par claro/oscuro | `Color.kt:153-165` | Los hexadecimales que el render 3D debe espejar |
| `UI-02` | `TreeViewModel` | Recalcular antes de observar, exponer `TreeUiState` | `GetTreeStateUseCase`, `RecalculateTreeStateUseCase` | **Sin cambios** |
| — | `AndroidManifest.xml` | Sin permiso `INTERNET`, `screenOrientation="portrait"` | — | La ausencia del permiso **es** el cumplimiento de RNF09 |
| — | `app/build.gradle.kts` | minSdk 26, Compose BOM 2024.12.01 | — | **Sin dependencias nuevas**: `WebView` es plataforma |

---

### Decisiones técnicas

#### D1 — Three.js entra como asset, en su build UMD, y su versión queda fijada en Git

`three@0.160.0/build/three.min.js` — **la última release con build UMD**. A partir de r161 solo hay ES Modules, y un módulo ES cargado desde `file:///android_asset/` choca con el origen opaco de los `file://` en varios WebView: exigiría servir los assets por `WebViewAssetLoader` y, con él, la dependencia `androidx.webkit`. El UMD se carga con un `<script src>` clásico y funciona desde `file://` sin nada más. **Cero dependencias Gradle nuevas.**

El archivo se versiona en Git, como el seed data y los 37 PNG del diccionario: es el mismo principio de RNF31 y de ADR-011 — lo que la app necesita para funcionar viaja en el repositorio, auditable y diffeable, no se resuelve en tiempo de build ni en tiempo de ejecución.

#### D2 — Tres archivos en `assets/tree/`, no un HTML con todo dentro

RNF31 pide el HTML, el JS y Three.js como **recursos versionados**, no hardcodeados. Un `String` de Kotlin con el HTML inline —o un `loadDataWithBaseURL`— convertiría el render en código Kotlin y haría imposible diffear un cambio visual. Quedan `tree.html` (el shell), `tree.js` (la generación procedural) y `three.min.js` (la librería), en una carpeta plana igual que `assets/exercises/`.

#### D3 — El contrato del puente es asimétrico a propósito

| Dirección | Mecanismo | Contenido |
|---|---|---|
| Nativo → Web | `evaluateJavascript("window.tensionTree.setState(h, 'STAGE')")` | **Salud y etapa. Exactamente los dos parámetros de CA-38.04.** |
| Web → Nativo | `@JavascriptInterface` `TreeBridge` con `onReady()` y `onFailure(reason)` | Solo señales de estado del render. **Ningún dato de dominio.** |

La calidad de render **no viaja por `setState`**: entra como *query string* en la URL de carga (`tree.html?quality=low&dark=true`), porque es una propiedad del dispositivo y del tema que se fija una vez y no cambia mientras la pantalla vive. Mantener `setState(salud, etapa)` con exactamente dos parámetros es lo que hace que el contrato documentado y el código digan lo mismo.

`onFailure` no devuelve datos al sistema: la historia declara que «el código web no devuelve datos al sistema» y eso se respeta — una señal de que el render no está disponible no es un dato de dominio, es la condición del fallback.

#### D4 — El fallback no es una pantalla alternativa: es la capa de abajo

La tentación es un `if (webViewDisponible) 3D else ícono`. Deja dos agujeros: el área en blanco mientras el `WebView` arranca, y el parpadeo si falla a los 800 ms. El diseño real tiene tres estados y **el ícono cubre dos de ellos**:

```
┌─ TreeVisual (180×180 dp) ────────────────────────┐
│  fallo o soporte ausente  →  TreeIcon            │  ← HU-37, sin cambios
│  soportado, aún no listo  →  TreeIcon            │  ← nunca hay área vacía
│  soportado y listo        →  Tree3DView (fade)   │  ← el WebView, ya renderizando
└──────────────────────────────────────────────────┘
```

El `WebView` se compone desde el primer instante —tiene que hacerlo para poder inicializarse— pero con `alpha = 0`, y aparece con un `crossfade` corto cuando `onReady` llega. Consecuencias: **la pantalla nunca queda en blanco** (CA-38.05), no hay mensaje de error porque no hay nada que anunciar, y el fallo tardío se ve como un árbol que simplemente no cambió de forma. Un `alpha` de 0 no impide la inicialización de WebGL: el `WebView` está adjunto y midiendo.

#### D5 — Tres detecciones de fallo, no una

«El WebView no está disponible, está desactualizado o el dispositivo no puede renderizar» (CA-38.05) son tres cosas distintas y ninguna se detecta con el mismo mecanismo:

| Qué falla | Cómo se detecta | Cuándo |
|---|---|---|
| No hay WebView instalado o está deshabilitado | `WebView.getCurrentWebViewPackage() == null` (API 26+, sin `androidx.webkit`) y `try/catch` al construirlo | Antes de componer |
| Versión demasiado antigua para el JS de r160 | Major del `versionName` del paquete < `MIN_WEBVIEW_MAJOR` | Antes de componer |
| Sin WebGL, o error de parseo/ejecución del JS | `tensionTree` reporta `onFailure` desde su `try/catch` y desde `window.onerror` | Al inicializar |
| Proceso de render muerto | `onRenderProcessGone` → `true` (no propaga el crash) | En cualquier momento |
| Cualquier otra cosa que nadie previó | **Timeout**: sin `onReady` en `READY_TIMEOUT_MS` → fallback | Al inicializar |

El timeout es la red que cubre lo que las cuatro anteriores no vieron, y por eso existe aunque cada una de ellas tenga su propio camino.

#### D6 — La calidad de render se decide en Kotlin (testeable) y se corrige en JS (medida)

CA-38.06 exige degradar por código, y el presupuesto manda sobre la fidelidad. Se resuelve en dos tiempos:

1. **Predicción, en Kotlin puro y unitariamente testeable.** `TreeRenderQuality.resolve(memoryClassMb, isLowRamDevice, processorCount)` → `HIGH` / `MEDIUM` / `LOW`. Es una función pura sobre tres enteros: el `ActivityManager` se lee en el composable y no entra en la función. Es la única parte de esta historia con lógica de decisión, y por eso es la única con test.
2. **Corrección, en JS y con medida real.** Se promedia el tiempo de los primeros `PROBE_FRAMES` fotogramas; si supera el presupuesto, se baja un escalón de calidad **una sola vez**. La predicción puede equivocarse; la medida no.

El orden de degradación es el del preview, y cada escalón lo materializa:

| | `HIGH` | `MEDIUM` | `LOW` |
|---|---|---|---|
| 1. Sombras | sí | **no** | no |
| 2. Esferas de la copa | 7 | **5** | **3** |
| 3. Segmentos del tronco | 12 | **8** | **6** |
| 4. Detalle por primitiva (icosaedro) | 1 | 1 | **0** |
| `devicePixelRatio` máximo | 2.0 | 1.5 | 1.0 |

#### D7 — El color 3D espeja los cinco colores del tema, interpolados en vez de escalonados

`TreeIcon` tiñe por **bandas** (0 / 1–33 / 34–66 / ≥67). CA-38.02 pide lo contrario: transición **continua**. La contradicción es aparente — se conservan los mismos hexadecimales de `Color.kt:153-165` y se usan como **paradas de un degradado**:

```
salud   0 ──────── 25 ──────── 50 ──────── 100
     TreeWithered  TreeWithering  TreeDry   TreeHealthy
     #5D4037       #8D5524        #8D6E00   #2E7D32   (claro)
     #A1887F       #D2A679        #FFD54F   #81C784   (oscuro)
```

Entre paradas se interpola en RGB. El resultado en 0, 25, 50 y 100 es **idéntico** al del ícono nativo, y entre esos puntos hay degradado en lugar de salto. Así el 3D y el fallback no pueden divergir de paleta, que es el mismo motivo por el que HU-37 puso una sola traducción en `TreeIcon`.

Los hexadecimales quedan duplicados en `tree.js` porque el puente lleva **dos** parámetros y no cinco colores. La duplicación se declara en un comentario que apunta a `Color.kt` en ambos sentidos: es el precio de mantener el contrato de CA-38.04 literal, y es una constante visual, no una regla.

El par claro/oscuro se resuelve con `?dark=true` en la URL: los marrones invertidos de D10 de HU-37 son exactamente el caso límite de RNF23 que el wireframe señalaba, y el render 3D lo hereda entero.

#### D8 — Tamaño por etapa, follaje y color por salud: dos transformaciones independientes

Regla de negocio 7 y CA-38.02. En el grafo de la escena son dos escalas distintas aplicadas a dos nodos distintos, y esa separación es lo que hace imposible que se contaminen:

| Dimensión | Qué controla | Rango |
|---|---|---|
| **Etapa** (forma) | Escala del árbol completo | `SEED` 0.30 · `SPROUT` 0.55 · `YOUNG` 0.80 · `MATURE` 1.00 |
| **Salud** (color y follaje) | Escala del grupo de follaje, su color, y la caída de las ramas | follaje `0.0 → 1.0`; caída `0° → 35°` |

Un maduro marchito es escala 1.00 con follaje 0.0: un tronco grande y pelado. Un brote sano es escala 0.55 con follaje 1.0. El caso `SEED` no dibuja tronco ni copa — es un montículo con un brote mínimo, coherente con el drawable `ic_tree_seed` que representa una semilla enterrada.

**`hasHistory` no cruza el puente.** Sin historial la salud es 100 (`TreeHealthRule.calculate(null)`) y la etapa es `SEED`: el 3D dibuja una semilla, y una semilla se lee como semilla cualquiera que sea su tinte. El ícono nativo la pinta en gris neutro porque un ícono plano con forma de semilla en verde vivo sí resultaba confuso; un montículo tridimensional no. Es la razón de que CA-38.04 pida dos parámetros y no tres, y se respeta.

#### D9 — La cámara se acota por construcción, no por validación

CA-38.03 pide límites que impidan perder el árbol de vista o atravesarlo. No se usa `OrbitControls` —vive en `examples/jsm`, es un módulo ES y arrastraría el problema de D1—: se implementan los tres gestos a mano sobre `pointerdown/move/up`, que además es lo que permite acotar sin pelear con los defaults de nadie.

- **Órbita horizontal**: libre, sin límite. Girar en redondo no pierde nada de vista.
- **Elevación vertical**: `clamp` en `[-10°, +55°]`. Ni por debajo del suelo ni cenital.
- **Zoom por pinza**: `clamp` en `[MIN_DISTANCE, MAX_DISTANCE]` sobre el radio de la órbita. El mínimo es mayor que el radio de la copa, así que **la cámara no puede atravesar el árbol**.
- **Paneo**: **no se implementa.** El objetivo de la cámara es fijo en el eje del árbol. Un paneo acotado y un paneo ausente son indistinguibles para el ejecutante, y el ausente no puede tener un bug de límites. El preview solo pide órbita, elevación y zoom.

`touch-action: none` en el canvas y `preventDefault()` en los gestos impiden que el toque se convierta en scroll web; `overflow: hidden` y `overScrollMode = OVER_SCROLL_NEVER` cierran el resto. La pantalla nativa no tiene scroll —la `Column` de `TreeScreen` no lleva `verticalScroll`—, así que del lado nativo no hay nada que propagar; se pide `requestDisallowInterceptTouchEvent` de todos modos, porque si alguien añade scroll a la pantalla mañana el gesto seguirá funcionando.

**La cámara vuelve al inicio al reentrar sin escribir código para ello**: su estado vive en el JS del `WebView`, el `WebView` se destruye al salir de la pantalla y se crea uno nuevo al volver. No hay nada que restaurar porque no hay nada que sobreviva.

#### D10 — El ciclo de vida del WebView es explícito en las tres salidas

CA-38.04 pide destrucción, desregistro del puente y ausencia de instancias duplicadas. Tres mecanismos, uno por salida:

1. **Salir de la pantalla** → `AndroidView(onRelease = ...)`: `removeJavascriptInterface`, `bridge.release()` (anula el listener), `loadUrl("about:blank")`, `stopLoading()`, desanclar del padre y `destroy()`. En ese orden: destruir con el puente todavía registrado deja una referencia viva al listener, y el listener conoce el `Composable`.
2. **Segundo plano** → `LifecycleEventObserver`: `onPause()` / `onResume()` del `WebView`, que detienen los timers y el `requestAnimationFrame`. Sin esto, el bucle de render sigue consumiendo GPU con la app invisible.
3. **Recomposición** → el `WebView` se crea **una sola vez** dentro de `remember`, no en el `factory` de `AndroidView` en cada paso. Recomponer no puede duplicarlo. La app es portrait-only (RNF07, ADR-016), así que la rotación no recrea nada; el ciclo de segundo plano sí, y lo cubre el punto 2.

El puente se diseña **testeable en JVM** para poder fijar el desregistro con un test: recibe el `post` como lambda —en producción, el `Handler` del hilo principal; en test, ejecución directa— y su `release()` anula el listener. Que un `onReady` posterior al `release` no llame a nadie es exactamente «sin referencias retenidas al contexto», y es verificable sin emulador.

#### D11 — El primer render no se anima; los siguientes sí

CA-38.02 pide transición fluida y CA-38.06 pide render inicial en menos de 1 segundo. Una animación de entrada las pone en conflicto: el árbol tardaría en *llegar* a su estado aunque el render estuviera listo. Se resuelve por prioridad declarada — el presupuesto manda:

- **Primer `setState`**: se aplica **instantáneo**. El árbol aparece ya en su estado.
- **`setState` posteriores**: `tween` de `TRANSITION_MS` sobre salud, interpolando color, escala de follaje y caída de ramas.

En esta pantalla el estado no cambia mientras se mira —`TreeViewModel` recalcula una vez, al abrir—, así que el camino animado es el que se ejerce a mano adelantando la fecha. La continuidad que CA-38.02 exige es sobre todo la del **mapeo**: no hay tres modelos, hay una función de la salud, y eso se cumple en el primer fotograma.

#### D12 — Sin `proguard` que lo proteja hoy, con regla que lo proteja mañana

`isMinifyEnabled = false` en `release`, así que R8 no está renombrando nada y el puente funcionaría sin regla. Se añade igual el `-keepclassmembers` con `@JavascriptInterface` a `proguard-rules.pro`: el día que alguien active la minificación, el síntoma sería un árbol que se queda en el fallback **solo en release**, y nadie relacionaría eso con una regla ausente. Cuatro líneas contra un diagnóstico de horas.

---

### Tareas de Implementación

#### Fase 1 — Línea base del presupuesto (CA-38.06)

- [ ] **T1: Medir el peso del APK antes de tocar nada** — `Tension/app/build/outputs/apk/release/`

  `JAVA_HOME=<jdk-17> ./gradlew :app:assembleRelease` y registrar el tamaño exacto en bytes del APK resultante. **Debe correr antes de T2**: una vez añadido el asset no hay forma de medir el incremento. El valor se anota en `dev-record.md`.

#### Fase 2 — Assets empaquetados (RNF09, RNF31)

- [ ] **T2: Añadir Three.js como asset versionado** — `Tension/app/src/main/assets/tree/three.min.js` (Base: `assets/exercises/*.png`)

  Build UMD de `three@0.160.0` (`build/three.min.js`, ~654 KB). Se descarga **una vez** y se versiona en Git. Sin CDN, sin npm, sin descarga en tiempo de ejecución. Registrar la versión exacta en `dev-record.md` para que sea trazable. (D1)

- [ ] **T3: Crear el shell HTML** — `Tension/app/src/main/assets/tree/tree.html`

  Documento mínimo: `<meta viewport>`, `html/body/canvas` con `margin: 0`, `background: transparent`, `overflow: hidden`, `touch-action: none`, canvas al 100 % del viewport. Los dos `<script>` clásicos en orden: `three.min.js` y luego `tree.js`. **Ningún texto visible** — ni el hint «arrastra para rotar» del wireframe: la historia declara «ningún texto nuevo» y la carpeta de strings es la única fuente de texto de la app (ADR-017). (D2)

- [ ] **T4: Crear la generación procedural y la cámara** — `Tension/app/src/main/assets/tree/tree.js`

  Un único IIFE, sin dependencias más allá de `THREE`. Responsabilidades, en este orden:

  1. Leer `quality` y `dark` de `location.search`; resolver la tabla de calidad de D6.
  2. `WebGLRenderer({ alpha: true, antialias: quality !== 'low' })` con `setClearAlpha(0)` — el fondo transparente de CA-38.04 nace aquí.
  3. Generar el árbol por código: tronco `CylinderGeometry` (segmentos radiales por calidad), copa de N `IcosahedronGeometry` (N y detalle por calidad) distribuidas en la copa, ramas como cilindros finos con pivote propio para poder caerse. `MeshLambertMaterial` con `AmbientLight` + `DirectionalLight`. **Sin texturas, sin `.glb`, sin `.gltf`.**
  4. Exponer `window.tensionTree.setState(healthScore, stageCode)` — **exactamente los dos parámetros de CA-38.04**. Mapear con las curvas de D7 y D8. Primer llamado instantáneo, siguientes con `tween` (D11).
  5. Órbita, elevación acotada y zoom acotado sobre `pointerdown/move/up` + `preventDefault()`. Sin paneo. (D9)
  6. Sonda de rendimiento: promediar `PROBE_FRAMES` fotogramas y bajar un escalón de calidad si excede el presupuesto, una sola vez. (D6)
  7. `try/catch` alrededor de toda la inicialización y `window.onerror`: cualquier fallo llama a `TreeBridge.onFailure(reason)`; el éxito llama a `TreeBridge.onReady()`. Si `typeof THREE === 'undefined'` → `onFailure` inmediato. (D5)

  Comentarios en español, como el resto de la documentación del proyecto; identificadores en inglés (§2.1 de los estándares).

#### Fase 3 — Decisión de calidad y de soporte, en Kotlin puro (`UI-01`)

- [ ] **T5: Crear `TreeRenderQuality`** — `ui/tree/TreeRenderQuality.kt` (Base: `TreeGrowthStage.kt`)

  `enum class TreeRenderQuality(val code: String)` con `HIGH`, `MEDIUM`, `LOW` y un `companion object` con `resolve(memoryClassMb: Int, isLowRamDevice: Boolean, processorCount: Int): TreeRenderQuality`. Cortes en constantes con nombre. Función **pura**: no recibe `Context` ni `ActivityManager`, para que sea testeable en JVM. KDoc con el orden de degradación de D6 y con la razón de que la medida en JS pueda corregirla. (D6, CA-38.06)

- [ ] **T6: Crear `TreeWebViewSupport`** — `ui/tree/TreeWebViewSupport.kt`

  `object` con `MIN_WEBVIEW_MAJOR` y la función **pura** `isSupportedVersion(versionName: String?): Boolean` — `null`, cadena vacía o major ilegible → `false`; major ≥ mínimo → `true`. KDoc anotando que un `versionName` que no se puede leer se trata como no soportado: el fallback nativo siempre es una salida válida y presumir soporte no lo es. (D5, CA-38.05)

#### Fase 4 — Puente e integración nativa (CA-38.01, CA-38.04, CA-38.05)

- [ ] **T7: Crear el puente JavaScript** — `ui/tree/TreeBridge.kt`

  `class TreeBridge(private val post: (() -> Unit) -> Unit)` con `interface Listener { fun onReady(); fun onFailure(reason: String) }`, `var listener: Listener?`, los dos métodos `@JavascriptInterface` y `fun release() { listener = null }`. Los métodos anotados llegan desde un hilo del `WebView`: **todo** se reenvía por `post`. `NAME` como constante (`"TreeBridge"`) para que el registro nativo y el JS no puedan divergir. KDoc declarando que el puente **no transporta datos de dominio** en dirección web → nativo. (D3, D10)

- [ ] **T8: Crear el composable del render 3D** — `ui/tree/Tree3DView.kt` (Base: `ExerciseDetailScreen.kt:230-249` para la lectura de assets)

  `@Composable fun Tree3DView(healthScore: Int, stage: TreeGrowthStage, isDarkTheme: Boolean, onReady: () -> Unit, onFailure: () -> Unit, modifier: Modifier)`.

  - `remember` del `WebView` y del `TreeBridge` — **una sola instancia por composición** (D10.3).
  - Ajustes del `WebSettings`: `javaScriptEnabled = true`; `allowFileAccessFromFileURLs`, `allowUniversalAccessFromFileURLs`, `allowContentAccess`, `domStorageEnabled` en `false`; `blockNetworkLoads = true`; `cacheMode = LOAD_NO_CACHE`.
  - `setBackgroundColor(TRANSPARENT)`, barras de scroll desactivadas, `overScrollMode = OVER_SCROLL_NEVER`, `isFocusable = false`. (CA-38.04)
  - `WebViewClient` que **bloquea toda navegación**: `shouldOverrideUrlLoading` → `true` siempre; `onReceivedError` del frame principal → `onFailure`; `onRenderProcessGone` → `onFailure` y `return true`. (CA-38.04, D5)
  - Carga `file:///android_asset/tree/tree.html?quality=…&dark=…` con la calidad de T5 leída del `ActivityManager` y el tema del parámetro.
  - `LaunchedEffect` de timeout: sin `onReady` en `READY_TIMEOUT_MS` → `onFailure`. (D5)
  - `LaunchedEffect(ready, healthScore, stage)`: cuando está listo, `evaluateJavascript("window.tensionTree.setState(h, 'CODE')")`. (D3)
  - `LifecycleEventObserver` → `onPause()` / `onResume()`. (D10.2)
  - `AndroidView(factory = { webView }, onRelease = { … })` con la secuencia de destrucción completa de D10.1.

  Además: exponer `fun isTree3DSupported(context: Context): Boolean` que combine `WebView.getCurrentWebViewPackage()` con `TreeWebViewSupport.isSupportedVersion(...)` dentro de `runCatching` — un `PackageManager` que lanza no debe reventar la pantalla del árbol.

- [ ] **T9: Sustituir la representación en `TreeVisual`** — `ui/tree/TreeScreen.kt`

  Único cambio en el archivo: el cuerpo de `TreeVisual`. Tres estados de D4:

  ```
  soportado = remember { isTree3DSupported(context) }
  failed / ready por remember { mutableStateOf(false) }
  Box(180.dp) {
      if (!soportado || failed || !ready) TreeIcon(...)          // fallback y espera
      if (soportado && !failed) Tree3DView(... alpha animado)    // el 3D
  }
  ```

  `graphicsLayer { alpha = crossfade }` con `animateFloatAsState`. El `contentDescription` del área reutiliza `R.string.tree_icon_description` — **ninguna cadena nueva**. `TREE_AREA_SIZE` no cambia. El resto de la pantalla —barra, etapa, puntaje, días, mensaje— **no se toca** (CA-38.01). KDoc del composable actualizado: deja de ser «la costura que HU-38 sustituye» y pasa a ser «quien elige entre el render 3D y el fallback nativo».

- [ ] **T10: Proteger el puente ante una minificación futura** — `Tension/app/proguard-rules.pro`

  `-keepclassmembers class * { @android.webkit.JavascriptInterface <methods>; }` con comentario explicando que hoy `isMinifyEnabled = false` y que el síntoma de su ausencia sería un fallback silencioso solo en release. (D12)

#### Fase 5 — Tests unitarios (JVM, sin emulador)

- [ ] **T11: Test de la resolución de calidad** — `test/ui/tree/TreeRenderQualityTest.kt` (Base: `TreeGrowthStageRuleTest.kt`)

  Los cortes uno por uno y sus fronteras: dispositivo `isLowRamDevice` → `LOW` cualquiera que sea el resto; memoria y núcleos justo por debajo y justo por encima de cada corte; el caso holgado → `HIGH`. Nomenclatura `given/when/then` (§5.3).

- [ ] **T12: Test del soporte de WebView** — `test/ui/tree/TreeWebViewSupportTest.kt`

  `null` → no soportado; cadena vacía → no soportado; `"no-es-una-version"` → no soportado; major justo por debajo del mínimo → no soportado; major exacto y muy superior → soportado; `versionName` con sufijos reales del tipo `"120.0.6099.230"` → soportado.

- [ ] **T13: Test del puente y de su desregistro** — `test/ui/tree/TreeBridgeTest.kt`

  Con un `post` síncrono: `onReady` desde JS llega al listener; `onFailure` llega con su razón; **tras `release()` ninguno de los dos llama a nadie**, que es la verificación en JVM de «no deja referencias retenidas al contexto» (CA-38.04). Un `onReady` sin listener asignado no lanza.

#### Fase 6 — Documentación arquitectónica (CA-38.07)

- [ ] **T14: Documentar el componente y la decisión** — `docs/architecture/architecture_blueprint.md`

  §1.2: `WebView` del sistema como dependencia de plataforma **opcional** —su ausencia degrada, no rompe—. §1.3: la excepción al anti-alcance, anotando que la app sigue 100 % offline y **que no se añade el permiso `INTERNET`**. §2.1 `UI-01`: el render 3D como componente con sus tres assets. §2.2: el flujo nativo ↔ web con sus dos direcciones asimétricas (D3). §3 `UI-01`: `Tree3DView`, `TreeBridge`, `TreeRenderQuality`, `TreeWebViewSupport` y la nota de que `TreeIcon` pasa a fallback permanente. §4: trazabilidad de HU-38. §5: **ADR-021** — 3D por generación procedural en WebView sin assets externos, con las alternativas descartadas (Compose Canvas/OpenGL nativo, `.glb` empaquetado, `SceneView`/Filament, `WebViewAssetLoader` + ES Modules) y sus consecuencias.

- [ ] **T15: Documentar el contrato del puente** — `docs/architecture/interfaces_contract.md`

  §1: el canal nuevo `nativo ↔ web` en la tabla de protocolos. §2.11 `Flujo N`: `N1-T1` gana la nota de que la presentación tiene ahora **dos representaciones con el mismo estado**; trigger nuevo `N1-T3: Rotar la cámara del árbol` con sus límites y el hecho de que no produce efecto de estado; y el **contrato del puente** con las dos direcciones, sus firmas y el comportamiento ante fallo —fallback silencioso, sin código de error, porque no hay nada que el ejecutante deba hacer—. §4: dos restricciones nuevas — el `WebView` no navega fuera de su asset local, y la tarjeta de Inicio no lleva `WebView` de forma permanente.

- [ ] **T16: Verificar que `system_definition_document.md` no cambió** — `docs/domain/definition/system_definition_document.md`

  Comprobación explícita con `git status`, no edición (CA-38.07).

#### Fase 7 — Medición de cierre y registro

- [ ] **T17: Medir el incremento de peso del APK** — `dev-record.md`

  `assembleRelease` de nuevo y diferencia contra T1, en bytes y en porcentaje. Anotar también el tamaño en disco de los tres assets y el efecto de la compresión del APK sobre ellos. **Es un entregable de CA-38.06**, no una nota al pie.

- [ ] **T18: Verificar la suite y el aislamiento** — `./gradlew :app:testDebugUnitTest`, `./gradlew build`

  701 → 701 + nuevos, **0 fallos**. Revisar el diff contra la lista de «Lo que NO se toca»: ningún archivo de dominio, datos, respaldo, navegación o Inicio debe aparecer.

- [ ] **T19: Registrar el desarrollo** — `docs/domain/stories/HU-38-arbol-3d-interactivo/dev-record.md` (nuevo, patrón de HU-37)

- [ ] **T20: Actualizar fases y métricas** — `index.md` (Refinamiento y Desarrollo a ✅, métricas de tiempo) y `cambios.md` (entradas de refinamiento y desarrollo)

---

### Riesgos y observaciones

**El riesgo declarado por la historia no se puede cerrar en esta sesión, y el diseño lo asume.** «La fidelidad del fondo transparente sobre distintos fabricantes» y «el presupuesto de 1 segundo en gama baja» son afirmaciones sobre dispositivos físicos. Ningún test JVM las toca y no hay emulador en el pipeline. Lo que sí hace el diseño es que **el fallo de cualquiera de las dos sea barato**: el fondo opaco es un defecto visual sobre una pantalla que sigue siendo útil, y el presupuesto incumplido lo corrige la sonda de D6 bajando calidad sola. Los puntos 5, 6 y 7 de la validación manual son la verificación real.

**Three.js pesa ~654 KB en disco y esa cifra no es el incremento del APK.** Los assets `.js` se almacenan comprimidos en el APK, así que el incremento real rondará una fracción de eso. La cifra que CA-38.06 pide registrar es la del APK, no la del archivo, y por eso T1 existe antes que T2: sin línea base no hay medida, solo estimación.

**Un `WebView` viejo puede no parsear el JS de r160 y eso está previsto, no cubierto.** El build minificado de r160 usa sintaxis moderna que un WebView anterior a Chrome ~90 podría rechazar. `MIN_WEBVIEW_MAJOR` intenta atajarlo por versión, pero la relación versión → sintaxis soportada no es exacta y no se va a adivinar: si el parseo falla igual, `onFailure` o el timeout llevan al fallback. **La consecuencia es que en un Android 8 con WebView de fábrica el ejecutante verá el ícono nativo, y eso es el comportamiento correcto** — RNF20 se cumple por el fallback, que es exactamente para lo que la historia lo pidió.

**El área de 180 × 180 dp es la restricción más discutible de esta historia.** Es lo que CA-38.01 permite y es pequeña para un gesto de órbita en una pantalla de 5". Se implementa así y se anota: si en dispositivo resulta incómodo, ampliar el área toca el layout que la CA congela, y por tanto es material para una historia nueva y no una desviación de esta.

**El hint «( ↺ arrastra para rotar )» del preview no se implementa.** El wireframe lo dibuja dentro del área del `WebView`, pero la sección *Interfaz* de la historia es explícita: «**Ningún texto nuevo.** … sin añadir, quitar ni reformular ninguna cadena». Entre el prototipo —declarado «pendiente de validación con Diseño»— y la regla escrita, manda la regla. Además el texto tendría que vivir en el JS, fuera de `strings.xml`, rompiendo ADR-017 y RNF31 a la vez. Se anota como pregunta para producto, no como omisión.

**`tree.js` no tiene tests y el proyecto no va a ganar infraestructura JS para dárselos.** No hay Node, ni Jest, ni pipeline npm, y añadirlo por esta historia sería introducir una segunda cadena de build para probar un archivo. Lo que se hace en su lugar: **sacar de JS toda la lógica que se puede decidir en Kotlin** —la calidad (T5), el soporte (T6), el enrutado del puente (T7)—, que son las tres piezas con ramas, y dejar en JS lo que es geometría e interpolación, verificable solo mirándolo. Los puntos 1 a 4 de la validación manual lo ejercen.

**El fallback tiene un caso que se ve raro y es el correcto.** Si el `WebView` falla **después** de `onReady` —proceso de render muerto—, el árbol 3D desaparece y reaparece el ícono. Es un cambio visual sin explicación para el ejecutante. La alternativa —un mensaje— la prohíbe la historia: «Ante fallo del WebView **no se muestra mensaje de error**». Se acepta el parpadeo: es un caso raro y la pantalla sigue diciendo lo mismo.

**El árbol sigue sin decidir nada, y esta historia lo tiene más fácil que HU-37 para no romperlo.** No añade tablas, ni consultas, ni use cases, ni enganches. Todo lo nuevo vive en `ui/tree/` y en `assets/tree/`. La verificación de ADR-020 es más fuerte que en HU-37: **ningún archivo de `domain/`, `data/` ni `di/` aparece en el diff**.

---

### Validación manual (no automatizable)

Los tests JVM cubren las tres decisiones en Kotlin. Todo lo demás de esta historia —el render, la transparencia, el gesto, el presupuesto— es juicio sobre dispositivo. Esta lista es la verificación real de la historia, no un apéndice.

1. **CA-38.01 (sustitución y no-regresión)** — Abrir la pantalla del árbol con historial. El área debe mostrar el modelo 3D. Comprobar que el puntaje, los días, el mensaje y la barra superior son **idénticos** a los de antes del build. Volver a Inicio: la tarjeta sigue siendo el ícono vectorial nativo.
2. **CA-38.02 (continuidad y ortogonalidad)** — Adelantando la fecha del dispositivo, recorrer salud 100 → 75 → 50 → 25 → 0 abriendo la pantalla en cada salto. El follaje debe reducirse y virar de verde vivo a amarillento y a marrón **sin saltos entre los tramos**, y en 0 desaparecer con las ramas caídas. Con 30 sesiones acumuladas y 30 días sin entrenar: **árbol grande y pelado** — el tamaño es de la etapa, el follaje de la salud.
3. **CA-38.03 (cámara)** — Arrastrar horizontal: órbita. Arrastrar vertical hasta el extremo: se detiene sin pasar por debajo del suelo ni ponerse cenital. Pinza a los dos extremos: no atraviesa el árbol ni lo pierde de vista. Durante el arrastre, **nada más de la pantalla se mueve**. Salir y volver: la cámara está en su posición inicial.
4. **CA-38.04 (integración y ciclo de vida)** — En modo claro y en modo oscuro, comprobar que **el fondo nativo se ve a través** del área del árbol y que los marrones se leen en ambos (RNF23). Sin barras de desplazamiento. Mandar la app a segundo plano y volver varias veces: un solo árbol, sin renders huérfanos, sin crecimiento sostenido de memoria en el profiler. Entrar y salir de la pantalla diez veces seguidas y comprobar en el profiler que la memoria vuelve a su nivel: es la verificación de que el `WebView` se destruye de verdad.
5. **CA-38.05 (fallback)** — Deshabilitar el WebView del sistema desde los ajustes del dispositivo (o forzar `onFailure` con un `three.min.js` renombrado en un build de prueba). La pantalla debe presentar el ícono vectorial teñido, con puntaje, días y mensaje intactos, **sin mensaje de error, sin blanco y sin cierre**.
6. **CA-38.06 (presupuesto)** — Cronometrar desde el toque en la tarjeta de Inicio hasta el árbol renderizado en un dispositivo de gama media: **< 1 s**. Rotar con el dedo y comprobar fluidez. En el dispositivo más modesto disponible, verificar que la sonda baja la calidad y que el resultado sigue siendo fluido — **antes lento que bonito**. Comprobar además que el arranque de la app y la navegación por el resto de las pantallas no cambian: el `WebView` solo existe dentro de `N1`.
7. **RNF09 (offline)** — Con el dispositivo en modo avión, todo lo anterior debe funcionar igual. Revisar `AndroidManifest.xml`: **sin permiso `INTERNET`**.
8. **CA-38.07 y ADR-020 (aislamiento)** — Con el árbol marchito, comprobar que la prescripción de carga, el resumen post-sesión, las alertas y la adherencia se comportan igual. Revisar el diff: ningún archivo de `domain/`, `data/` ni `di/`.
