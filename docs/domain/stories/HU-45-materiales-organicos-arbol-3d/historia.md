# Historia de Usuario

**Como** El Ejecutante,
**Quiero** que mi árbol tenga corteza con relieve y hojas con variaciones tonales reales, que secan y caen de forma natural cuando se marchita,
**Para** que se sienta como un organismo vivo que yo cuido y no una figura de plástico.

> **Historia resultante de partición.** Es la **segunda de dos** historias en que se dividió *Evolución fotorrealista del árbol 3D*. Depende de [`HU-44`](../HU-44-arbol-organico/historia.md), que debe estar implementada antes: de ella toma la geometría orgánica del árbol (ramificación procedural, cuatro etapas, tres niveles de calidad) y sobre ella añade los materiales procedurales.

> **Extensión de `HU-38`.** Como `HU-44`, modifica **únicamente** el asset local `assets/tree/tree.js` y, de ser necesario, los parámetros de `TreeRenderQuality.kt`. Hereda de [`HU-38`](../HU-38-arbol-3d-interactivo/historia.md) la entidad de persistencia, el cálculo de salud y etapa, la ruta de navegación, la pantalla dedicada, el WebView, el puente JavaScript, el sistema de calidad y el respaldo. Todo eso se conserva intacto.

## Descripción

`HU-44` dio al árbol estructura: se ve orgánico, pero con materiales de color plano/sencillo. Es un árbol dibujado bien, no un árbol que se *siente*: la corteza no tiene relieve, el follaje no tiene variación tonal y el marchitamiento, aunque continuo, no se percibe como un proceso natural de secado.

Esta historia completa el salto con **materiales procedurales**: shaders WebGL con funciones de ruido (Perlin/Simplex o equivalentes) que generan relieve en la corteza (displacement o bump mapping) y variaciones de color orgánicas en las hojas, con un marchitamiento que transiciona de forma continua de verde vibrante a un amarillo/marrón seco y quebradizo.

La **vara de aceptación** es la de la historia original, resuelta en su conjunto: que el árbol *deje de parecer un juguete geométrico y se sienta como un árbol natural vivo*. El nombre *fotorrealista* es aspiracional —el PO lo declara explícitamente, conocedores de los límites de WebGL móvil, procedural y offline—; no se exige indistinguibilidad fotográfica, se exige naturalidad percibida. La vara final la fija la aprobación del PO sobre el juego completo de capturas.

### Frontera técnica declarada por el PO

- Esta historia modifica **únicamente** la lógica de materiales dentro del asset local `assets/tree/tree.js` y, de ser necesario, los parámetros de `TreeRenderQuality.kt`. **No redefine la geometría** de `HU-44`; la consume.
- **No se toca** la base de datos, el cálculo de reglas, el puente JavaScript (sigue entregando exactamente salud y etapa), el fallback nativo (`TreeIcon` de `HU-37`), la tarjeta de Inicio, ni la navegación.
- **Restricción de red y peso:** la app sigue sin permiso `INTERNET` y 100% offline. Sigue **estrictamente prohibido** el uso de archivos `.glb` o `.gltf`.
- **Uso de assets:** si el acabado exigiera texturas de imagen, estas deben ser archivos **`.webp` altamente comprimidos (< 100 KB en total)** ubicados como **archivos físicos en `assets/tree/` y cargados de forma asíncrona**. **Queda estrictamente prohibido el embebido en Base64** en el JavaScript (infla el tamaño real en ~33 % y bloquea el hilo principal durante el parseo del JS). **Se priorizan los shaders matemáticos**: cada textura debe justificar un detalle que el ruido procedural no logra.
- **Generación determinista:** los shaders de ruido usan el **mismo PRNG con semilla matemática fija** que la geometría de `HU-44` (nota técnica de CA-45.01): con `Math.random()` o semillas variables, la textura cambiaría en cada apertura de la pantalla y el ejecutante vería un árbol distinto cada vez.
- **El presupuesto de rendimiento manda sobre la fidelidad visual** (regla heredada de `HU-38`): antes rápido que bonito.

### Restricción heredada de HU-37 / HU-38

La excepción de alcance sigue vigente sin cambios: el árbol es una funcionalidad **puramente visual y aislada**. No alimenta al motor de decisión, no genera alertas, no altera KPIs y no modifica `system_definition_document.md`. Esta historia no amplía esa frontera — solo cambia cómo se dibuja el árbol.

---

## Criterios de Aceptación

### Escenario 1: Flujo Principal

#### CA-45.01 — Materiales procedurales con ruido

- **Dado** que `HU-44` está implementada y el árbol muestra su silueta orgánica
- **Cuando** El Ejecutante observa el árbol en calidad HIGH
- **Entonces** la corteza presenta **relieve** generado por funciones de ruido (Perlin/Simplex o equivalentes) en shaders WebGL —displacement o bump mapping— y no un color plano
- **Y** el follaje presenta **variaciones de color orgánicas** —no un único tono plano— coherentes con un material natural
- **Y** el resultado se percibe como un material con presencia física, no como pintura lisa

> **Nota técnica — generación determinista (PRNG).** El ruido procedural de los materiales depende de funciones matemáticas aleatorias: si se usa `Math.random()` estándar o semillas de ruido variables, **la textura cambiará cada vez que el ejecutante abra la pantalla** — el árbol cambiaría de corteza y de follaje en cada apertura. Por ello `tree.js` tiene **estrictamente prohibido** el uso de `Math.random()` o semillas variables: debe implementarse un **PRNG (p. ej. Mulberry32 o LCG) inicializado con una semilla matemática fija** —la misma base que la geometría de `HU-44`—, de modo que para una misma etapa y salud los vértices, las ramas y las texturas se generen **siempre en las mismas coordenadas exactas**, manteniendo la identidad del árbol del usuario.

#### CA-45.02 — Marchitamiento orgánico a lo largo de la salud

- **Dado** que el aspecto del árbol sigue expresando la salud de 100 a 0
- **Cuando** la salud baja
- **Entonces** las hojas transicionan de forma **continua** de **verde vibrante** a un **amarillo/marrón seco y quebradizo**
- **Y** la caída de las ramas y la reducción del follaje conservan el comportamiento heredado de `HU-38` (continuo, sin saltos entre estados fijos)
- **Y** la transición de materiales entre cualquier par de valores de salud es **fluida y continua**, sin cambios bruscos de textura o color
- **Y** con salud alta (≈100) el follaje presenta su escala máxima y verde vibrante; con salud baja o crítica (≈0) el follaje desaparece o se torna completamente marrón/oscuro, seco y quebradizo, con ramas caídas y aspecto marchito

### Escenario 2: Validaciones

#### CA-45.03 — Degradación estricta de shaders por calidad

- **Dado** que el sistema de calidad de `HU-38` —`TreeRenderQuality` en Kotlin (HIGH, MEDIUM, LOW) y la sonda de rendimiento en JavaScript— sigue vigente
- **Cuando** el árbol se renderiza en cada nivel de calidad
- **Entonces** en **HIGH** se activan los **shaders procedurales completos** —bump/displacement en corteza, variación tonal en follaje— y las texturas de mayor resolución disponible
- **Y** en **LOW** se **desactiva el bump/displacement mapping**, los materiales usan **shaders básicos (tipo Lambert)** y se **apagan las sombras**
- **Y** en LOW el árbol conserva reconocibles su silueta orgánica y sus estados de salud — es el árbol de `HU-44` con materiales simples, **no** un render a medio compilar
- **Y** la sonda de rendimiento puede seguir bajando un escalón de calidad si la medida real no cumple el presupuesto
- **Y** en dispositivos de gama baja el renderizado sostiene **30–60 FPS** en rotación: el presupuesto manda sobre la fidelidad

> **Nota técnica — warm-up de shaders.** En Three.js el primer `render()` obliga a la GPU a compilar los shaders, y los shaders de ruido procedural de esta historia son especialmente costosos en ese primer fotograma (micro-parón de varios milisegundos). La sonda de rendimiento **debe implementar un periodo de warm-up, ignorando los primeros 2 o 3 fotogramas** antes de comenzar a promediar los FPS, para que el tiempo de compilación inicial no penalice falsamente la medición de fluidez ni baje la calidad a `LOW` en un dispositivo que después corre a 60 FPS estables.

#### CA-45.04 — Fallback nativo ante incapacidad de shaders avanzados

- **Dado** que el dispositivo no soporta WebGL adecuado o **no puede compilar los shaders avanzados** de esta historia
- **Cuando** El Ejecutante abre la pantalla del árbol
- **Entonces** el sistema presenta **silenciosamente la representación nativa de `HU-37`** —el ícono vectorial de la etapa teñido según la salud—
- **Y** el puntaje, los días desde el último entrenamiento y el mensaje contextual **se siguen mostrando**
- **Y** la pantalla **no queda en blanco, ni se cierra, ni presenta un error bloqueante**: no hay mensaje de error porque no hay nada que el ejecutante deba hacer
- **Y** el mecanismo de detección es el existente de `HU-38` (señal de fallo del render al lado nativo): esta historia no lo modifica
- **Y** si el contexto WebGL se pierde **durante la sesión** (p. ej. al volver de segundo plano), el sistema **también cae al fallback nativo**: la pantalla no queda congelada ni en blanco

> **Nota técnica — el fallo de WebGL es silencioso.** En Three.js la compilación de shaders ocurre de forma **asíncrona en la GPU durante el primer render**: un fallo de compilación **no lanza una excepción** que atrape el `try/catch` global (mecanismo heredado de `HU-38`); solo arroja un `console.warn` y pinta el material **negro**. Sin acción explícita, el ejecutante vería un árbol negro en lugar del fallback. Por tanto `tree.js` **debe interceptar explícitamente los errores de compilación de `WebGLProgram`** —por ejemplo, auditando `renderer.info.programs` tras el primer render— y, al detectar un shader fallido, **emitir `TreeBridge.onFailure()` de forma manual** para activar el fallback nativo.

> **Nota técnica — pérdida de contexto WebGL.** Para ahorrar batería, Android destruye agresivamente la memoria de la GPU cuando la app pasa a segundo plano: al volver, el canvas de Three.js puede haber perdido su conexión con la GPU y el render se queda **congelado o en blanco**. `tree.js` **debe escuchar el evento estándar de HTML5 `webglcontextlost` sobre el canvas** y, al capturar el evento, **emitir inmediatamente `TreeBridge.onFailure()`** para activar el fallback nativo y evitar dejar una pantalla inutilizable.

#### CA-45.05 — Presupuesto de assets

- **Dado** que la app sigue 100% offline y sin permiso `INTERNET` (RNF09)
- **Cuando** el acabado exigiera texturas de imagen
- **Entonces** estas son archivos **`.webp` altamente comprimidos, con un total menor a 100 KB, ubicados como archivos físicos en `assets/tree/`** y **cargados de forma asíncrona por Three.js**
- **Y** queda **estrictamente prohibido** embeberlas en **Base64** dentro del JavaScript: el Base64 infla el tamaño real en ~33 % y bloquea el hilo principal durante el parseo inicial del JS, rompiendo el presupuesto de carga de < 1 s (CA-45.06)
- **Y** **se priorizan los shaders matemáticos** sobre las texturas de imagen: cada textura debe justificar un detalle que el ruido procedural no logra
- **Y** no se empaquetan **ningún** `.glb`, `.gltf` ni modelo 3D externo
- **Y** no se carga ningún contenido desde red: todo viaja dentro del APK
- **Y** la emisión de `TreeBridge.onReady()` queda **condicionada a la resolución exitosa de todas las promesas de carga de texturas** (`TextureLoader`): el árbol no se reporta listo hasta que sus materiales tengan los assets `.webp` aplicados

> **Nota técnica — "texture popping" de texturas asíncronas.** Las texturas `.webp` se cargan de forma asíncrona desde el disco del APK. Si `tree.js` emitiera `TreeBridge.onReady()` en el primer fotograma sin esperar a que la carga termine, el lado nativo iniciaría el fundido (*crossfade*) sobre un material sin textura: el ejecutante vería un árbol **gris o negro** que repentinamente "parpadea" (*texture popping*) cuando la imagen termina de cargar. Por ello `onReady()` **solo se emite cuando todas las promesas de `TextureLoader` han resuelto** y las texturas están aplicadas a los materiales.

### Escenario 3: Casos Extremos

#### CA-45.06 — Presupuesto de rendimiento

- **Dado** el rango de dispositivos soportado (Android 8.0+, API 26 — RNF20)
- **Cuando** El Ejecutante abre la pantalla del árbol en un dispositivo de gama media
- **Entonces** la carga y el renderizado inicial se completan en **menos de 1 segundo** (presupuesto heredado de `HU-38`)
- **Y** la rotación con el dedo se percibe fluida, sin trabas apreciables
- **Y** el resto de la aplicación **no sufre degradación** de arranque ni de navegación: la tarjeta de Inicio sigue nativa y sin WebView

#### CA-45.07 — Aprobación estética por capturas (vara final)

- **Dado** que la generación procedural puede arrojar resultados impredecibles matemáticamente
- **Cuando** la historia se considera implementada
- **Entonces** se ha generado el **juego completo de capturas: 4 etapas × 3 bandas de salud (12 capturas)** con el flujo de capturas del árbol (`tools/capturar-arbol.ps1`)
- **Y** el PO **aprueba las 12 capturas** como vara final de la estética del árbol evolucionado: cada etapa se lee natural, cada banda de salud (alta, media, baja/marchita) se lee coherente con su estado y el conjunto se siente como un organismo vivo
- **Y** ninguna captura muestra artefactos de render (barridos, cortes, bandas de aliasing, materiales sin compilar)

#### CA-45.08 — Peso del APK medido y auditado

- **Dado** que la distribución es un APK firmado donde cada byte cuenta (RNF37)
- **Cuando** se entrega la historia
- **Entonces** el **incremento final en bytes del APK** respecto a la versión previa se **mide y registra** en la documentación de la historia
- **Y** el registro audita que cualquier textura comprimida (`.webp`) y código shader añadido **respetó el presupuesto de < 100 KB** declarado en CA-45.05
- **Y** el APK total permanece dentro del presupuesto de tamaño del proyecto

#### CA-45.09 — Comportamiento heredado sin regresión

- **Dado** que esta historia solo cambia los materiales del modelo
- **Cuando** se verifica la historia
- **Entonces** la silueta orgánica, las cuatro etapas, la escala por etapa y los estados por salud **se comportan igual** que en `HU-44`
- **Y** la rotación de cámara con el dedo, los límites de zoom/paneo y el reinicio de cámara al reentrar **se comportan igual** que en `HU-38`
- **Y** el fondo del WebView sigue transparente, correcto en modo claro y oscuro (RNF23)
- **Y** el puente JavaScript sigue entregando **exactamente** salud y etapa —sin nuevos parámetros—
- **Y** el cálculo de salud y etapa, la persistencia, el respaldo, la tarjeta de Inicio y la ruta de navegación **no se modifican**
- **Y** `system_definition_document.md` **no se modifica**

#### CA-45.10 — Documentación actualizada

- **Dado** que el enfoque de renderizado incorpora shaders de ruido y el manejo de texturas comprimidas
- **Cuando** se actualiza la documentación
- **Entonces** `docs/architecture/architecture_blueprint.md` refleja la nueva estrategia de materiales y **registra la ADR** (nueva o ampliación de la iniciada en `HU-44`, con alternativa de modelos `.glb`/`.gltf` y texturas descargadas descartadas)
- **Y** si el contrato del puente o la predicción de calidad cambian, `docs/architecture/interfaces_contract.md` se actualiza en consecuencia
- **Y** la excepción de alcance permanece registrada: la app sigue 100% offline y el árbol sigue siendo visual y aislado

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante — único usuario del sistema. La aplicación es single-user y no maneja roles.
- **Permisos requeridos:** Ninguno.
- **Valor de negocio:** `HU-38` dio al árbol vida interactiva, `HU-44` le dio estructura orgánica; esta historia le da la piel. La fidelidad visual es el último ingrediente de la recompensa emocional que justifica EPIC-09: la diferencia entre algo que la app dibujó y algo que el ejecutante siente que cuida.

### Reglas de Negocio

1. **Solo cambia la piel del modelo.** La geometría es de `HU-44`; cálculo, persistencia, puente, navegación, tarjeta de Inicio y respaldo son de `HU-37`/`HU-38` y no se tocan.
2. **El fotorrealismo es aspiracional; la vara es natural.** No se exige indistinguibilidad fotográfica —WebGL móvil procedural y offline no la garantiza—; se exige que el árbol se sienta como un árbol natural vivo. La vara final la fija la aprobación del PO sobre el juego de capturas (CA-45.07).
3. **Tamaño expresa etapa; salud expresa estado.** Regla heredada e inamovible: la escala sigue las cuatro etapas y el aspecto de hojas/ramas sigue la salud de 100 a 0, en transición continua —ahora con secado orgánico, no solo cambio de tinte—.
4. **El presupuesto manda sobre la fidelidad.** En conflicto, se degradan los gráficos —bump/displacement, texturas, sombras— y nunca la fluidez. Antes rápido que bonito: LOW debe sostener 30–60 FPS en gama baja sin bump mapping y sin sombras.
5. **Si el dispositivo no puede compilar los shaders avanzados —o pierde la GPU a mitad de sesión—, cae al nativo.** La red de seguridad es el ícono de `HU-37`, no un render degradado a medio compilar: incapacidad de render o pérdida del contexto WebGL (`webglcontextlost`, p. ej. al volver de segundo plano) → fallback nativo silencioso, sin mensaje de error (CA-45.04).
6. **Sin modelos 3D, sin red, con techo de assets.** Ni `.glb` ni `.gltf`; shaders matemáticos prioritarios; texturas de imagen solo si las justifica el resultado, `.webp` físicas en `assets/tree/` y cargadas asíncronamente, < 100 KB en total —**sin Base64**, que infla ~33 % y bloquea el parseo del JS. `TreeBridge.onReady()` solo se emite cuando **todas** las texturas están cargadas y aplicadas (sin *texture popping*; CA-45.05).
7. **La calidad la deciden los dos niveles existentes.** `TreeRenderQuality` (Kotlin) predice y la sonda de rendimiento (JS) confirma o baja escalón; esta historia no crea un nuevo mecanismo de calidad, solo le da qué dibujar en cada escalón.
8. **El árbol es siempre el mismo árbol.** Los materiales de ruido son **deterministas**: mismo PRNG con semilla fija que la geometría de `HU-44`, nunca `Math.random()` — para una misma etapa y salud, la textura se genera siempre idéntica (CA-45.01).
9. **La excepción de alcance de `HU-37` sigue vigente sin ampliarse.** El árbol no alimenta al motor de decisión, no genera alertas y no altera KPIs.

### Interfaz

**No se crea ninguna pantalla nueva ni ninguna ruta nueva.** Se cambia el aspecto del modelo dentro del área que `HU-38` ya estableció.

- **Inicio (B1):** sin cambios. La tarjeta permanece nativa tal como la dejaron `HU-37`/`HU-38`.
- **Pantalla del árbol:** el área del árbol muestra el modelo orgánico de `HU-44` con materiales procedurales. Barra superior, puntaje, días y mensaje contextual permanecen idénticos.
- **Resto de la aplicación:** sin cambios.

#### Detalle de Interfaz de Usuario

- **Diseño general:** el WebView sigue ocupando el mismo área, con fondo transparente sobre el fondo nativo de la app, correcto en modo claro y oscuro (RNF23). El bloque de estado inferior no se reorganiza. Solo portrait (RNF07).
- **Campos y controles:** ningún control nuevo. La única interacción es el gesto de arrastre para rotar la cámara, con zoom y paneo acotados, idéntico al de `HU-38`.
- **Flujo de navegación visual:** sin rutas nuevas. Inicio → pantalla del árbol → retroceso, igual que hoy.
- **Mensajes y feedback:** **ningún texto nuevo.** Se conservan íntegras las bandas de salud y las tablas de textos de `HU-37`. Ante incapacidad de shaders avanzados **no se muestra mensaje de error**: se sustituye silenciosamente por el ícono nativo.

### Sistemas Externos

Ninguno nuevo. Los componentes de `HU-38` siguen siendo los únicos involucrados:

- **WebView nativo de Android** — sin cambios.
- **Three.js (empaquetado, sin CDN)** — la librería no cambia; cambia el código que la usa (`assets/tree/tree.js`).
- **Archivos de textura `.webp` (solo si se justifican)** — nuevos assets locales opcionales, < 100 KB en total, dentro del APK.

El intercambio de datos es local e inalterado: la capa nativa entrega salud y etapa por el puente; la calidad y el tema entran por query string al cargar el asset. El código web no devuelve datos al sistema.

### Disparadores

- El Ejecutante abre la pantalla del árbol → el árbol se renderiza con sus materiales procedurales en su etapa y salud actuales, con la calidad predicha para el dispositivo.
- El estado del árbol cambia (cierre de sesión o cambio de día) y El Ejecutante abre la pantalla → se renderiza con el nuevo estado y sus materiales transicionan de forma continua.
- La sonda de rendimiento detecta que la calidad actual no cumple el presupuesto → baja un escalón de calidad (apagando bump mapping y sombras al llegar a LOW) y se vuelve a medir (puede repetirse hasta el nivel mínimo).
- El dispositivo no soporta WebGL adecuado o falla la compilación de los shaders avanzados → se presenta el fallback nativo de `HU-37`.
- El contexto WebGL se pierde durante la sesión (`webglcontextlost`, p. ej. al volver de segundo plano) → `tree.js` emite `TreeBridge.onFailure()` y se presenta el fallback nativo de `HU-37`.

### Entidades del Dominio

- **Estado del árbol** — salud (0–100) y etapa de crecimiento (Semilla, Brote, Joven, Maduro); solo lectura para esta historia.

### Alcance y Continuidad

Esta historia **extiende `HU-44`** (El árbol deja de ser geométrico), que a su vez extiende `HU-38`: añade únicamente los materiales procedurales —shaders de ruido para relieve de corteza y variación tonal de follaje— y su degradación estricta por calidad, sobre la geometría orgánica que `HU-44` deja lista. No introduce ninguna capacidad nueva: el árbol, su pantalla, su WebView, su puente, su sistema de calidad y su fallback ya existen; lo que cambia es el acabado del dibujo.

Es la **segunda de dos** rebanadas de *Evolución fotorrealista del árbol 3D* (partición aprobada por el PO, corte por capas de datos —fidelidad progresiva—). Con su entrega se resuelve en su conjunto la visión original de la historia pre-partición.

Continúa la excepción declarada en `HU-37`/`HU-38` sin ampliarla: el árbol sigue siendo una funcionalidad **puramente visual y aislada** —lee del historial y nada del sistema lee del árbol—. No se incorporan sistemas nuevos: el WebView y Three.js ya son parte del proyecto y todo sigue siendo local, sin red.

### Preview de Interfaz

**Preview:** [`45.preview.txt`](./45.preview.txt) | **Formato:** ASCII (wireframe de texto)

Cubre el antes/después respecto a `HU-44` (silueta igual, cambia la piel), los materiales procedurales de corteza y follaje, el marchitamiento orgánico continuo, la degradación estricta de shaders por calidad, el fallback nativo ante fallo de compilación y los presupuestos (12 capturas finales, auditoría de peso del APK).

---

## Contexto y Referencias

**Arquitectura:** `docs/architecture/architecture_blueprint.md` (§1.2 WebView del Sistema, §1.3 Anti-Alcance, §2.1 contenedor `UI-01` — render 3D del árbol, ADR-020 y ADR-021), `docs/architecture/interfaces_contract.md` (puente nativo ↔ web de `HU-38`), `docs/domain/definition/system_definition_document.md` §2.1 (frontera de alcance, no se modifica).

**Historias relacionadas:**

- **[HU-44](../HU-44-arbol-organico/historia.md)** — El árbol deja de ser geométrico. **Dependencia dura, hermana.** Aporta la geometría de ramificación procedural, las cuatro etapas orgánicas y los tres niveles de calidad sobre los que esta historia pinta. **Debe estar implementada antes.**
- **[HU-38](../HU-38-arbol-3d-interactivo/historia.md)** — El árbol en 3D. Aporta el WebView, el puente, `TreeRenderQuality`, la sonda de rendimiento, el área del modelo y el presupuesto de rendimiento. **Ya está implementada.**
- **[HU-37](../HU-37-arbol-progreso-entrenamiento/historia.md)** — El árbol de mi entrenamiento. Aporta el cálculo, la persistencia, la pantalla y la representación nativa que permanece como fallback permanente.

**Restricciones transversales aplicables:**

- RNF01 — El renderizado no bloquea la interfaz, y el arranque de la app no se degrada
- RNF07 — Solo portrait
- RNF09 — 100% offline: todo se empaqueta, nada se descarga
- RNF20 — Compatible con Android 8.0+ (API 26): el fallback cubre los dispositivos incapaces
- RNF21 / RNF22 — Pantallas de 5" a 7", de 720p a 1440p
- RNF23 — Tema claro/oscuro: el fondo transparente debe respetar ambos
- RNF31 — El JS y cualquier textura como recursos versionados en assets, no hardcodeados
- RNF37 — APK firmado sin Google Play Store: el peso añadido importa

**Lecciones aprendidas:** `HU-38` enseñó que el riesgo del WebView se aísla con fallback nativo y presupuesto medible; `HU-44` que el resultado de un generador procedural se acepta con capturas. Esta historia hereda ambas lecciones y las aplica a su riesgo propio, que es el **rendimiento de los shaders de ruido en gama baja** —el más costoso del proyecto—. Por eso la degradación en LOW es explícita y estricta (sin bump, sin sombras, Lambert) y el fallback cae al nativo ante fallo de compilación (CA-45.04), no a un render a medio compilar.

**Riesgo declarado:** el riesgo principal es el **presupuesto de rendimiento en gama baja** —shaders de ruido y bump/displacement son costosos—. Tiene salida declarada: degradación estricta por calidad con el presupuesto por encima de la fidelidad (CA-45.03) y el juego de capturas como vara de aceptación explícita (CA-45.07).

---

## Definición de Terminado (Inicial)

- [ ] Corteza con relieve por ruido (displacement/bump) y follaje con variación de color orgánica en calidad HIGH
- [ ] Generación determinista (PRNG con semilla fija, sin `Math.random()`): la misma textura idéntica en cada apertura de la pantalla
- [ ] Marchitamiento continuo de verde vibrante a amarillo/marrón seco y quebradizo a lo largo de la salud 100→0
- [ ] Degradación estricta: HIGH completo · LOW con bump/displacement off, shaders Lambert, sombras off, 30–60 FPS en gama baja
- [ ] Sonda de rendimiento conservada, capaz de bajar un escalón de calidad, con warm-up de 2–3 fotogramas antes de promediar
- [ ] Fallo de compilación de shaders o WebGL ausente → fallback nativo de HU-37 silencioso, sin error visible, con detección explícita del fallo de `WebGLProgram` y `TreeBridge.onFailure()` manual
- [ ] `webglcontextlost` escuchado sobre el canvas: pérdida del contexto GPU (p. ej. al volver de segundo plano) → `TreeBridge.onFailure()` inmediato y fallback nativo
- [ ] Cero `.glb`/`.gltf` en el APK; shaders matemáticos prioritarios; texturas `.webp` físicas en `assets/tree/` con carga asíncrona (si existen) < 100 KB en total — sin Base64
- [ ] `TreeBridge.onReady()` solo tras resolver todas las promesas de carga de texturas: sin "texture popping" (nunca un árbol gris/negro que parpadea)
- [ ] Carga y render inicial en menos de 1 segundo en gama media
- [ ] 12 capturas (4 etapas × 3 bandas de salud) generadas y aprobadas por el PO como vara final de la estética
- [ ] Incremento del APK medido en bytes y registrado, con auditoría del presupuesto de assets
- [ ] Silueta, etapas, estados de salud, puente, calidad, fallback, tarjeta de Inicio, navegación, cálculo, persistencia y respaldo verificados sin regresión
- [ ] `architecture_blueprint.md` con la ADR de la estrategia de materiales y, si aplica, `interfaces_contract.md` actualizado; `system_definition_document.md` sin modificar
