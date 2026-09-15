# Historia de Usuario

**Como** El Ejecutante,
**Quiero** que mi árbol 3D se vea orgánico y natural —tronco irregular, ramas que no son cilindros perfectos, copa que no son esferas—,
**Para** que deje de parecer un juguete geométrico hecho de formas básicas.

> **Historia resultante de partición.** Es la **primera de dos** historias en que se dividió *Evolución fotorrealista del árbol 3D*. La hija [`HU-45`](../HU-45-materiales-organicos-arbol-3d/historia.md) añade después los materiales procedurales (relieve de corteza y variación tonal de follaje). **Debe implementarse primero.**

> **Extensión de `HU-38`.** Esta historia modifica **únicamente** cómo se genera la geometría del modelo 3D que `HU-38` ya entrega. Depende de [`HU-38`](../HU-38-arbol-3d-interactivo/historia.md) (que a su vez depende de [`HU-37`](../HU-37-arbol-progreso-entrenamiento/historia.md)): de ellas hereda la entidad de persistencia, el cálculo de salud y etapa, la ruta de navegación, la pantalla dedicada, el WebView, el puente JavaScript, el sistema de calidad y el respaldo. Todo eso se conserva intacto.

## Descripción

`HU-38` cumplió su objetivo técnico: el árbol es tridimensional, rotable con el dedo y transiciona de forma fluida entre estados de salud. Pero lo hace con un estilo **Low-Poly** —tronco cilíndrico y copa de icosaedros superpuestos— que comunica el estado pero no lo *siente*: es un juguete geométrico, no un árbol.

Esta historia reemplaza la generación del modelo por un **generador de ramificación procedural**: algoritmos de ramificación (L-Systems, fractales o splines, o combinación de ellos) que producen un tronco irregular, ramas naturales y un volumen de follaje orgánico, en las cuatro etapas y en los tres niveles de calidad. Los materiales en esta rebanada son de **color plano/sencillo por etapa y salud** —sin relieve ni ruido—: el acabado orgánico llega en `HU-45`.

La vara de aceptación de esta historia es la **silueta**: que un observador describa el árbol como natural y no como una figura construida con formas básicas. El nombre *fotorrealista* de la historia original es aspiracional y se resuelve en su conjunto con `HU-45`; esta hija entrega la mitad estructural de ese salto.

### Frontera técnica declarada por el PO

- Esta historia modifica **únicamente** la lógica de generación geométrica dentro del asset local `assets/tree/tree.js` y, de ser necesario, los parámetros de `TreeRenderQuality.kt`.
- **No se toca** la base de datos, el cálculo de reglas, el puente JavaScript (sigue entregando exactamente salud y etapa), el fallback nativo (`TreeIcon` de `HU-37`), la tarjeta de Inicio, ni la navegación.
- **Restricción de red y peso:** la app sigue sin permiso `INTERNET` y 100% offline. Sigue **estrictamente prohibido** el uso de archivos `.glb` o `.gltf`.
- El estilo **Low-Poly no se conserva**: se retira por completo. Mantener dos generadores procedurales en `tree.js` inflaría el peso del script y crearía deuda técnica innecesaria. El escalón `LOW` de calidad será el **nuevo** árbol orgánico drásticamente simplificado, no el árbol antiguo.
- **El presupuesto de rendimiento manda sobre la fidelidad visual** (regla heredada de `HU-38`): antes rápido que bonito.

### Restricción heredada de HU-37 / HU-38

La excepción de alcance sigue vigente sin cambios: el árbol es una funcionalidad **puramente visual y aislada**. No alimenta al motor de decisión, no genera alertas, no altera KPIs y no modifica `system_definition_document.md`. Esta historia no amplía esa frontera — solo cambia cómo se dibuja el árbol.

---

## Criterios de Aceptación

### Escenario 1: Flujo Principal

#### CA-44.01 — Geometría orgánica por ramificación procedural

- **Dado** que `HU-38` está implementada y la pantalla del árbol muestra el modelo 3D
- **Cuando** el árbol se genera en cualquier etapa
- **Entonces** el tronco, las ramas y el volumen de follaje se generan mediante **algoritmos de ramificación procedural** (L-Systems, fractales o splines, o combinación de ellos) que producen una silueta **irregular y natural**, no prismas ni esferas perfectas
- **Y** la escala general del árbol sigue obedeciendo a la **etapa** de crecimiento —Semilla, Brote, Joven, Maduro—, de forma independiente de la salud, exactamente como en `HU-38`
- **Y** un observador describiría el árbol como natural, no como una figura construida con formas básicas
- **Y** la geometría procedural **calcula y expone explícitamente sus normales de vértice** (p. ej. `computeVertexNormals()`), prerrequisito matemático indispensable para que `HU-45` pueda aplicar iluminación y texturas de relieve (bump/displacement) correctamente

> **Nota técnica — disposal de mallas.** La ramificación procedural genera mallas nuevas al cambiar de etapa. El *garbage collector* de JavaScript **no libera la memoria de la GPU** por sí solo: `tree.js` **debe invocar explícitamente `.dispose()`** sobre las geometrías y materiales anteriores antes de montar los nuevos. Requisito estricto para evitar fugas de memoria (*memory leaks*) en WebGL que llenarían la memoria de video y crashearían el WebView.

> **Nota técnica — generación determinista (PRNG).** La ramificación procedural depende de funciones matemáticas aleatorias: si se usa `Math.random()` estándar o semillas de ruido variables, **el árbol cambiará de forma cada vez que el ejecutante abra la pantalla** — en vez de cuidar *su* árbol, verá un árbol distinto cada vez. Por ello `tree.js` tiene **estrictamente prohibido** el uso de `Math.random()` o semillas variables: debe implementarse un **PRNG (p. ej. Mulberry32 o LCG) inicializado con una semilla matemática fija**, de modo que para una misma etapa y salud los vértices y las ramas se generen **siempre en las mismas coordenadas exactas**, manteniendo la identidad del árbol del usuario.

#### CA-44.02 — El mismo nivel de fidelidad en las cuatro etapas

- **Dado** que no pueden mezclarse estilos de arte entre etapas —un árbol maduro orgánico junto a un brote Low-Poly rompería la ilusión—
- **Cuando** el sistema renderiza cualquiera de las cuatro etapas
- **Entonces** la **Semilla** se muestra como un **montículo de tierra con una semilla o brote orgánico**, en el mismo tratamiento
- **Y** el **Brote** se muestra como una **planta joven natural**
- **Y** **Joven** y **Maduro** se muestran como árboles ramificados en el mismo tratamiento
- **Y** las cuatro etapas comparten el mismo nivel de fidelidad: no existe ninguna etapa renderizada con el estilo anterior

#### CA-44.03 — Estados de salud conservados sobre la nueva silueta

- **Dado** que el aspecto del árbol sigue expresando la salud
- **Cuando** el árbol se genera en cualquier valor de salud (100 a 0)
- **Entonces** el follaje sigue reduciéndose y las ramas siguen cayendo conforme la salud baja, de forma **continua y sin saltos** entre estados fijos, igual que en `HU-38`
- **Y** con salud alta el follaje presenta su escala máxima y color verde; con salud baja o crítica el follaje desaparece o se torna marrón/oscuro con ramas caídas y aspecto marchito
- **Y** la transición de color entre estados se conserva fluida y continua
- **Y** la transición continua aplica a las **transformaciones** —escala del follaje, ángulo de caída de las ramas— **y al color**: es una transición continua de estado, no de geometría
- **Y** la estructura base de las ramas (**topología geométrica**) se **genera una sola vez por etapa** y se **mantiene fija** para cualquier valor de salud de esa etapa: la transición nunca muta la malla ni su número de vértices, porque hacer morphing entre mallas de distinta topología destruye la geometría y genera artefactos gráficos

### Escenario 2: Validaciones

#### CA-44.04 — Degradación estricta por calidad con el nuevo generador

- **Dado** que el sistema de calidad de `HU-38` —`TreeRenderQuality` en Kotlin (HIGH, MEDIUM, LOW) y la sonda de rendimiento en JavaScript— sigue vigente
- **Cuando** el árbol se renderiza en cada nivel de calidad
- **Entonces** en **LOW** el árbol usa la **misma silueta orgánica con geometría drásticamente simplificada** —menos ramificación, menos segmentos, menos volumen de follaje—
- **Y** en LOW el árbol mantiene reconocible su silueta natural y sus estados de salud: es el nuevo árbol orgánico simplificado, **no** el Low-Poly anterior
- **Y** la sonda de rendimiento puede seguir bajando un escalón de calidad si la medida real no cumple el presupuesto
- **Y** en dispositivos de gama baja el renderizado sostiene **30–60 FPS** en rotación: el presupuesto manda sobre la fidelidad
- **Y** el generador de ramificación tiene **topes duros de recursión definidos por código** (cap de iteraciones y de segmentos): la inicialización en CPU debe quedar con margen para **jamás superar el timeout de carga nativo (2,5 s)** de `HU-38`

> **Nota técnica — warm-up de shaders.** En Three.js el primer `render()` obliga a la GPU a compilar los shaders, lo que causa un micro-parón (*jank*) de varios milisegundos en ese primer fotograma. La sonda de rendimiento **debe implementar un periodo de warm-up, ignorando los primeros 2 o 3 fotogramas** antes de comenzar a promediar los FPS, para que el tiempo de compilación inicial de los shaders no penalice falsamente la medición de fluidez ni baje la calidad a `LOW` en un dispositivo que después corre a 60 FPS estables.

> **Nota técnica — límite de recursión (CPU vs timeout).** El generador de ramificación procedural corre en la CPU mediante JavaScript. Si el nivel de ramificación (recursividad) no tiene un límite duro, la rama se dispara combinatoriamente: en un celular de gama baja, calcular la geometría podría tardar **más de 2,5 segundos** —el timeout de carga nativo heredado de `HU-38`— y el WebView jamás emitiría `TreeBridge.onReady()`, activando el fallback nativo **permanentemente** en un dispositivo que sí tiene GPU capaz. Por ello el generador debe tener **topes máximos de recursión estrictamente definidos por código** (cap de iteraciones, de ramas y de segmentos por rama), y el nivel `LOW` debe calcular su geometría lo suficientemente rápido como para que la inicialización en CPU **jamás supere el timeout de 2,5 s**.

#### CA-44.05 — Fallback nativo ante incapacidad de render

- **Dado** que el dispositivo no soporta WebGL adecuado o no puede renderizar el contenido 3D
- **Cuando** El Ejecutante abre la pantalla del árbol
- **Entonces** el sistema presenta **silenciosamente la representación nativa de `HU-37`** —el ícono vectorial de la etapa teñido según la salud—
- **Y** el puntaje, los días desde el último entrenamiento y el mensaje contextual **se siguen mostrando**
- **Y** la pantalla **no queda en blanco, ni se cierra, ni presenta un error bloqueante**: no hay mensaje de error porque no hay nada que el ejecutante deba hacer
- **Y** el mecanismo de detección es el existente de `HU-38` (señal de fallo del render al lado nativo): esta historia no lo modifica
- **Y** si el contexto WebGL se pierde **durante la sesión** (p. ej. al volver de segundo plano), el sistema **también cae al fallback nativo**: la pantalla no queda congelada ni en blanco

> **Nota técnica — pérdida de contexto WebGL.** Para ahorrar batería, Android destruye agresivamente la memoria de la GPU cuando la app pasa a segundo plano: al volver, el canvas de Three.js puede haber perdido su conexión con la GPU y el render se queda **congelado o en blanco**. `tree.js` **debe escuchar el evento estándar de HTML5 `webglcontextlost` sobre el canvas** y, al capturar el evento, **emitir inmediatamente `TreeBridge.onFailure()`** para activar el fallback nativo y evitar dejar una pantalla inutilizable.

#### CA-44.06 — Presupuesto de rendimiento y assets

- **Dado** el rango de dispositivos soportado (Android 8.0+, API 26 — RNF20) y la app 100% offline (RNF09)
- **Cuando** El Ejecutante abre la pantalla del árbol en un dispositivo de gama media
- **Entonces** la carga y el renderizado inicial se completan en **menos de 1 segundo** (presupuesto heredado de `HU-38`)
- **Y** la rotación con el dedo se percibe fluida, sin trabas apreciables
- **Y** no se empaquetan **ningún** `.glb`, `.gltf` ni modelo 3D externo, ni se carga contenido desde red
- **Y** el resto de la aplicación **no sufre degradación** de arranque ni de navegación: la tarjeta de Inicio sigue nativa y sin WebView

### Escenario 3: Casos Extremos

#### CA-44.07 — El generador Low-Poly se retira por completo

- **Dado** que conservar dos generadores en el asset inflaría su peso y crearía deuda técnica
- **Cuando** se entrega esta historia
- **Entonces** el código del generador Low-Poly (icosaedros y cilindros con colores planos) **no existe** en `tree.js`
- **Y** un solo generador —el nuevo— cubre las cuatro etapas y los tres niveles de calidad
- **Y** el fallback ante incapacidad del dispositivo es el nativo de `HU-37` (CA-44.05), nunca el renderizado antiguo

#### CA-44.08 — Aprobación estética por capturas (silueta)

- **Dado** que la generación procedural puede arrojar resultados impredecibles matemáticamente
- **Cuando** la historia se considera implementada
- **Entonces** se ha generado el **juego completo de capturas: 4 etapas × 3 bandas de salud (12 capturas)** con el flujo de capturas del árbol (`tools/capturar-arbol.ps1`)
- **Y** el PO **aprueba las 12 capturas** como vara de la silueta: cada etapa se lee natural y cada banda de salud (alta, media, baja/marchita) se lee coherente con su estado
- **Y** ninguna captura muestra artefactos de render (barridos, cortes, bandas de aliasing)

#### CA-44.09 — Peso del APK medido

- **Dado** que la distribución es un APK firmado donde cada byte cuenta (RNF37)
- **Cuando** se entrega la historia
- **Entonces** el **incremento final en bytes del APK** respecto a la versión previa se **mide y registra** en la documentación de la historia
- **Y** el APK total permanece dentro del presupuesto de tamaño del proyecto

#### CA-44.10 — Comportamiento heredado sin regresión

- **Dado** que esta historia solo cambia la geometría del modelo
- **Cuando** se verifica la historia
- **Entonces** la rotación de cámara con el dedo, los límites de zoom/paneo y el reinicio de cámara al reentrar **se comportan igual** que en `HU-38`
- **Y** el fondo del WebView sigue transparente, correcto en modo claro y oscuro (RNF23)
- **Y** el puente JavaScript sigue entregando **exactamente** salud y etapa —sin nuevos parámetros—
- **Y** el cálculo de salud y etapa, la persistencia, el respaldo, la tarjeta de Inicio y la ruta de navegación **no se modifican**
- **Y** `system_definition_document.md` **no se modifica**

#### CA-44.11 — Documentación actualizada

- **Dado** que el enfoque de generación cambia de primitivas a ramificación procedural
- **Cuando** se actualiza la documentación
- **Entonces** `docs/architecture/architecture_blueprint.md` refleja la nueva estrategia de generación del árbol y registra la **ADR** (nueva o ampliación de ADR-021) con la elección, sus alternativas descartadas (modelos `.glb`/`.gltf`, conservar el Low-Poly como nivel de degradación) y la regla de degradación por calidad
- **Y** la excepción de alcance permanece registrada: la app sigue 100% offline y el árbol sigue siendo visual y aislado

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante — único usuario del sistema. La aplicación es single-user y no maneja roles.
- **Permisos requeridos:** Ninguno.
- **Valor de negocio:** `HU-38` dio al árbol vida interactiva; esta historia da la primera mitad del salto a algo orgánico. La vara mínima de la visión original —*dejar de parecer un juguete geométrico*— se cumple con esta hija y es visible desde la primera apertura.

### Reglas de Negocio

1. **Solo cambia la geometría del modelo.** Cálculo, persistencia, puente, navegación, tarjeta de Inicio y respaldo son de `HU-37`/`HU-38` y no se tocan.
2. **La vara de esta rebanada es la silueta.** Se exige naturalidad estructural —tronco irregular, ramas naturales, volumen de follaje orgánico—; el acabado de materiales (relieve, variación tonal) es de `HU-45` y no se evalúa aquí.
3. **Tamaño expresa etapa; salud expresa estado.** Regla heredada e inamovible: la escala sigue las cuatro etapas y el aspecto de hojas/ramas sigue la salud de 100 a 0, en transición continua.
4. **Un solo nivel de fidelidad para todo el ciclo de vida.** Semilla, Brote, Joven y Maduro comparten el mismo tratamiento —sin mezclar estilos—. Semilla es un montículo de tierra con semilla/brote orgánico; Brote, una planta joven natural.
5. **El presupuesto manda sobre la fidelidad.** En conflicto, se degrada la geometría —nunca la fluidez—. LOW debe sostener 30–60 FPS en gama baja.
6. **El Low-Poly no sobrevive.** Se retira por completo; no conviven dos generadores. LOW es el nuevo árbol orgánico simplificado.
7. **Si el dispositivo no puede renderizar —o pierde la capacidad a mitad de sesión—, cae al nativo.** La red de seguridad es el ícono de `HU-37`: incapacidad de render o pérdida del contexto WebGL (`webglcontextlost`, p. ej. al volver de segundo plano) → fallback nativo silencioso, sin mensaje de error (CA-44.05).
8. **Sin modelos 3D, sin red.** Ni `.glb` ni `.gltf`; todo viaja dentro del APK (RNF09).
9. **El árbol es siempre el mismo árbol.** La generación procedural es **determinista**: PRNG con semilla fija, nunca `Math.random()` — para una misma etapa y salud, la silueta se genera siempre idéntica (CA-44.01).
10. **La calidad la deciden los dos niveles existentes.** `TreeRenderQuality` (Kotlin) predice y la sonda de rendimiento (JS) confirma o baja escalón; esta historia no crea un nuevo mecanismo de calidad.
11. **La excepción de alcance de `HU-37` sigue vigente sin ampliarse.** El árbol no alimenta al motor de decisión, no genera alertas y no altera KPIs.

### Interfaz

**No se crea ninguna pantalla nueva ni ninguna ruta nueva.** Se cambia el aspecto del modelo dentro del área que `HU-38` ya estableció.

- **Inicio (B1):** sin cambios. La tarjeta permanece nativa tal como la dejaron `HU-37`/`HU-38`.
- **Pantalla del árbol:** el área del árbol muestra el nuevo modelo orgánico. Barra superior, puntaje, días y mensaje contextual permanecen idénticos.
- **Resto de la aplicación:** sin cambios.

#### Detalle de Interfaz de Usuario

- **Diseño general:** el WebView sigue ocupando el mismo área, con fondo transparente sobre el fondo nativo de la app, correcto en modo claro y oscuro (RNF23). El bloque de estado inferior no se reorganiza. Solo portrait (RNF07).
- **Campos y controles:** ningún control nuevo. La única interacción es el gesto de arrastre para rotar la cámara, con zoom y paneo acotados, idéntico al de `HU-38`.
- **Flujo de navegación visual:** sin rutas nuevas. Inicio → pantalla del árbol → retroceso, igual que hoy.
- **Mensajes y feedback:** **ningún texto nuevo.** Se conservan íntegras las bandas de salud y las tablas de textos de `HU-37`. Ante incapacidad de render **no se muestra mensaje de error**: se sustituye silenciosamente por el ícono nativo.

### Sistemas Externos

Ninguno nuevo. Los componentes de `HU-38` siguen siendo los únicos involucrados:

- **WebView nativo de Android** — sin cambios.
- **Three.js (empaquetado, sin CDN)** — la librería no cambia; cambia el código que la usa (`assets/tree/tree.js`).

El intercambio de datos es local e inalterado: la capa nativa entrega salud y etapa por el puente; la calidad y el tema entran por query string al cargar el asset. El código web no devuelve datos al sistema.

### Disparadores

- El Ejecutante abre la pantalla del árbol → el árbol se renderiza en su etapa y salud actuales con la calidad predicha para el dispositivo.
- El estado del árbol cambia (cierre de sesión o cambio de día) y El Ejecutante abre la pantalla → se renderiza con el nuevo estado.
- La sonda de rendimiento detecta que la calidad actual no cumple el presupuesto → baja un escalón de calidad y se vuelve a medir (puede repetirse hasta el nivel mínimo).
- El dispositivo no soporta WebGL adecuado o no puede renderizar el contenido 3D → se presenta el fallback nativo de `HU-37`.
- El contexto WebGL se pierde durante la sesión (`webglcontextlost`, p. ej. al volver de segundo plano) → `tree.js` emite `TreeBridge.onFailure()` y se presenta el fallback nativo de `HU-37`.

### Entidades del Dominio

- **Estado del árbol** — salud (0–100) y etapa de crecimiento (Semilla, Brote, Joven, Maduro); solo lectura para esta historia.

### Alcance y Continuidad

Esta historia **extiende `HU-38`** (El árbol en 3D), ya implementada: cambia únicamente la generación geométrica del modelo dentro del asset local —de primitivas Low-Poly a ramificación procedural— y, de ser necesario, los parámetros de la predicción de calidad en Kotlin. No introduce ninguna capacidad nueva: el árbol, su pantalla, su WebView, su puente y su sistema de calidad ya existen; lo que cambia es la estructura del dibujo.

Es la **primera de dos** rebanadas de *Evolución fotorrealista del árbol 3D* (partición aprobada por el PO, corte por capas de datos —fidelidad progresiva—). Su hermana [`HU-45`](../HU-45-materiales-organicos-arbol-3d/historia.md) añade los materiales procedurales sobre la geometría que esta historia deja lista.

Continúa la excepción declarada en `HU-37`/`HU-38` sin ampliarla: el árbol sigue siendo una funcionalidad **puramente visual y aislada** —lee del historial y nada del sistema lee del árbol—. No se incorporan sistemas nuevos: el WebView y Three.js ya son parte del proyecto y todo sigue siendo local, sin red.

### Preview de Interfaz

**Preview:** [`44.preview.txt`](./44.preview.txt) | **Formato:** ASCII (wireframe de texto)

Cubre el antes/después del área del árbol respecto a `HU-38`, las cuatro etapas en el mismo tratamiento orgánico, los estados de salud sobre la nueva silueta, la degradación en LOW como nuevo árbol simplificado, el fallback nativo y los presupuestos (12 capturas, peso del APK).

---

## Contexto y Referencias

**Arquitectura:** `docs/architecture/architecture_blueprint.md` (§1.2 WebView del Sistema, §1.3 Anti-Alcance, §2.1 contenedor `UI-01` — render 3D del árbol, ADR-020 y ADR-021), `docs/architecture/interfaces_contract.md` (puente nativo ↔ web de `HU-38`), `docs/domain/definition/system_definition_document.md` §2.1 (frontera de alcance, no se modifica).

**Historias relacionadas:**

- **[HU-45](../HU-45-materiales-organicos-arbol-3d/historia.md)** — Los materiales que hacen sentir vivo al árbol. **Hermana, se implementa después**: consume la geometría que esta historia deja lista.
- **[HU-38](../HU-38-arbol-3d-interactivo/historia.md)** — El árbol en 3D. **Dependencia dura.** Aporta el WebView, el puente, `TreeRenderQuality`, la sonda de rendimiento, el área del modelo y el estilo Low-Poly que esta historia reemplaza. **Ya está implementada.**
- **[HU-37](../HU-37-arbol-progreso-entrenamiento/historia.md)** — El árbol de mi entrenamiento. Aporta el cálculo, la persistencia, la pantalla y la representación nativa que permanece como fallback permanente.

**Restricciones transversales aplicables:**

- RNF01 — El renderizado no bloquea la interfaz, y el arranque de la app no se degrada
- RNF07 — Solo portrait
- RNF09 — 100% offline: todo se empaqueta, nada se descarga
- RNF20 — Compatible con Android 8.0+ (API 26): el fallback cubre los dispositivos incapaces
- RNF21 / RNF22 — Pantallas de 5" a 7", de 720p a 1440p
- RNF23 — Tema claro/oscuro: el fondo transparente debe respetar ambos
- RNF31 — El JS como recurso versionado en assets, no hardcodeado
- RNF37 — APK firmado sin Google Play Store: el peso añadido importa

**Lecciones aprendidas:** `HU-38` enseñó que el riesgo del WebView se aísla con fallback nativo y presupuesto medible; esta historia hereda esa lección y la aplica al nuevo riesgo, que no es de integración sino de **resultado estético impredecible** de un generador procedural. Por eso la aceptación exige capturas aprobadas por el PO (CA-44.08) y el fallback cae al nativo, no a un render a medio compilar (CA-44.05).

**Riesgo declarado:** el riesgo principal de esta rebanada es la **aprobación estética de la silueta**: un generador procedural puede cumplir el presupuesto de geometría y aun así no convencer. Tiene salida declarada: el juego de capturas como vara de aceptación explícita (CA-44.08). El riesgo de rendimiento de los shaders avanzados queda en `HU-45`.

---

## Definición de Terminado (Inicial)

- [ ] Tronco, ramas y follaje generados por ramificación procedural con silueta natural, sin primitivas básicas visibles
- [ ] Generación determinista (PRNG con semilla fija, sin `Math.random()`): el mismo árbol idéntico en cada apertura de la pantalla
- [ ] Normales de vértice calculadas y expuestas explícitamente en toda la geometría generada (prerrequisito de la iluminación/relieve de HU-45)
- [ ] `.dispose()` de geometrías y materiales al regenerar la malla de etapa, sin fugas de memoria en WebGL
- [ ] La transición de salud es de transformaciones y color, sin mutar la topología de la malla
- [ ] Las cuatro etapas (Semilla, Brote, Joven, Maduro) en el mismo tratamiento orgánico, sin mezclar estilos
- [ ] Semilla como montículo de tierra con semilla/brote orgánico; Brote como planta joven natural
- [ ] Escala por etapa y aspecto por salud (follaje/ramas, transición continua) conservados respecto a HU-38
- [ ] LOW = nuevo árbol orgánico con geometría drásticamente simplificada; 30–60 FPS en gama baja
- [ ] Topes duros de recursión en el generador: la inicialización en CPU jamás supera el timeout de carga nativo (2,5 s)
- [ ] Sonda de rendimiento conservada, capaz de bajar un escalón de calidad, con warm-up de 2–3 fotogramas antes de promediar
- [ ] Fallo de render o WebGL ausente → fallback nativo de HU-37 silencioso, sin error visible
- [ ] `webglcontextlost` escuchado sobre el canvas: pérdida del contexto GPU (p. ej. al volver de segundo plano) → `TreeBridge.onFailure()` inmediato y fallback nativo
- [ ] Cero `.glb`/`.gltf` en el APK, sin contenido remoto
- [ ] Carga y render inicial en menos de 1 segundo en gama media
- [ ] Código Low-Poly retirado por completo de `tree.js`
- [ ] 12 capturas (4 etapas × 3 bandas de salud) generadas y aprobadas por el PO como vara de silueta
- [ ] Incremento del APK medido en bytes y registrado
- [ ] Puente, calidad, fallback, tarjeta de Inicio, navegación, cálculo, persistencia y respaldo verificados sin regresión
- [ ] `architecture_blueprint.md` con la ADR de la nueva estrategia de generación; `system_definition_document.md` sin modificar
