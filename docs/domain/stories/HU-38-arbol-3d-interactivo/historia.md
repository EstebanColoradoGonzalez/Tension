# Historia de Usuario

**Como** El Ejecutante,
**Quiero** que mi árbol sea un modelo tridimensional que puedo rotar con el dedo y que se transforma de forma fluida según su salud,
**Para** que la recompensa visual de mi constancia sea algo que dé ganas de volver a abrir.

> **Historia resultante de partición.** Es la **segunda de dos** historias en que se dividió la historia original *Árbol de progreso del entrenamiento*. Depende de [`HU-37`](../HU-37-arbol-progreso-entrenamiento/historia.md), que debe estar implementada antes: de ella hereda la entidad de persistencia, el cálculo de salud y etapa, la ruta de navegación, la pantalla dedicada y la integración con el respaldo.

## Descripción

`HU-37` entregó el árbol funcionando: crece con las sesiones acumuladas, se marchita con la ausencia, y se muestra como un ícono vectorial teñido según la salud. Cumple su propósito informativo, pero no el emocional — un ícono no se siente como algo vivo que uno cuida.

Esta historia sustituye **únicamente el área del árbol** dentro de la pantalla dedicada por un **modelo tridimensional generado por código**, rotable con el dedo y con transiciones continuas entre estados de salud. Todo lo demás —la tarjeta de Inicio, la ruta, el layout, el bloque de estado, el cálculo, la persistencia y el respaldo— permanece exactamente como `HU-37` lo dejó.

**El ícono nativo de `HU-37` no se elimina.** Pasa a ser el **fallback** que se muestra cuando el WebView no está disponible o no puede renderizar. Es la red de seguridad de esta historia, no un extra: garantiza que ningún ejecutante se quede sin árbol por culpa de su dispositivo.

### Frontera técnica declarada por el PO

- El renderizado 3D se resuelve con **generación procedural por código** dentro de un archivo HTML/JS local cargado en un **WebView** nativo, usando **Three.js**.
- **No se empaquetan modelos 3D externos** (`.glb`, `.gltf`) para no engrosar el peso del APK.
- Three.js y el HTML viajan **dentro del APK**. No hay CDN ni descarga en tiempo de ejecución (RNF09).
- Este es el **primer WebView del proyecto**: hoy no existe ninguno. La aplicación es Compose puro.
- **El presupuesto de rendimiento manda sobre la fidelidad visual.** Si un dispositivo no alcanza el objetivo, se degradan los gráficos por código antes que entregar una experiencia lenta.

### Restricción heredada de HU-37

La excepción de alcance sigue vigente sin cambios: el árbol es una funcionalidad **puramente visual y aislada**. No alimenta al motor de decisión, no genera alertas, no altera KPIs y no modifica `system_definition_document.md`. Esta historia no amplía esa frontera — solo cambia cómo se dibuja el árbol.

---

## Criterios de Aceptación

### Escenario 1: Flujo Principal

#### CA-38.01 — El árbol 3D sustituye la representación nativa

- **Dado** que `HU-37` está implementada y la pantalla dedicada existe
- **Cuando** El Ejecutante abre la pantalla del árbol en un dispositivo con WebView disponible
- **Entonces** el área del árbol muestra el **modelo tridimensional** en lugar del ícono vectorial
- **Y** el puntaje, los días desde el último entrenamiento, el mensaje contextual y la barra superior **permanecen sin cambios**
- **Y** la **tarjeta de Inicio sigue siendo nativa**: esta historia no la modifica en absoluto (RNF01)
- **Y** no se altera la ruta de navegación, ni el cálculo, ni la persistencia, ni el respaldo introducidos por `HU-37`

#### CA-38.02 — Estilo visual y estados dinámicos

- **Dado** que el árbol se renderiza con un puntaje de salud determinado
- **Cuando** El Ejecutante lo observa
- **Entonces** el estilo es **Low-Poly**: tronco cilíndrico y copa formada por esferas o icosaedros superpuestos, generados por código
- **Y** con **salud alta (≈100)** el follaje presenta su escala máxima y colores verdes vivos y brillantes
- **Y** con **salud media (≈50)** el follaje se reduce en tamaño y el color vira a un verde amarillento o seco
- **Y** con **salud baja o crítica (≈0)** el follaje desaparece o se torna completamente marrón/oscuro, con ramas caídas y aspecto marchito
- **Y** la transición entre estados es **fluida y continua**, no un salto entre tres modelos fijos
- **Y** el **tamaño del árbol corresponde a su etapa** de crecimiento —semilla, brote, joven, maduro—, de forma independiente de la salud

#### CA-38.03 — Interactividad de cámara

- **Dado** que El Ejecutante está en la pantalla del árbol
- **Cuando** arrastra el dedo sobre él
- **Entonces** la cámara **rota suavemente** alrededor del árbol
- **Y** el **zoom y el paneo están acotados** dentro de límites que impiden perder el árbol de vista o atravesarlo
- **Y** el gesto de rotación **no dispara** el scroll de la página web ni el de la pantalla nativa
- **Y** al salir y volver a entrar, la cámara **regresa a su posición inicial**

### Escenario 2: Validaciones

#### CA-38.04 — Integración nativa del WebView

- **Dado** que el árbol se renderiza dentro de un WebView
- **Cuando** se muestra la pantalla
- **Entonces** el **fondo del WebView es transparente**, dejando ver el fondo nativo de la app en modo claro y oscuro (RNF23)
- **Y** las **barras de desplazamiento están ocultas** y el scroll web bloqueado
- **Y** el WebView **no permite navegación a contenido remoto**: carga exclusivamente el archivo local empaquetado (RNF09)
- **Y** el **ciclo de vida del WebView se gestiona explícitamente** — se destruye y se desregistra su puente JavaScript al salir de la pantalla, sin dejar fugas de memoria ni referencias retenidas al contexto
- **Y** el estado se entrega al código web mediante una función expuesta que recibe **salud y etapa de crecimiento**, invocable desde el lado nativo a través del puente JavaScript del WebView
- **Y** rotar el dispositivo, mandar la app a segundo plano y volver **no duplica** instancias del WebView ni deja renders huérfanos

#### CA-38.05 — Fallback nativo

- **Dado** que el WebView del sistema no está disponible, está desactualizado o el dispositivo no puede renderizar el contenido 3D
- **Cuando** El Ejecutante abre la pantalla del árbol
- **Entonces** el sistema presenta la **representación nativa de `HU-37`**: el ícono vectorial de la etapa teñido según la salud
- **Y** el puntaje, los días desde el último entrenamiento y el mensaje contextual **se siguen mostrando**
- **Y** la pantalla **no queda en blanco, ni se cierra, ni presenta un error bloqueante**
- **Y** la representación nativa **se conserva en el código** de forma permanente: esta historia no la elimina

#### CA-38.06 — Presupuesto de rendimiento y degradación

- **Dado** el rango de dispositivos soportado (Android 8.0+, API 26 — RNF20)
- **Cuando** El Ejecutante abre la pantalla del árbol en un dispositivo de gama media
- **Entonces** la carga y el renderizado inicial se completan en **menos de 1 segundo**
- **Y** la rotación con el dedo se percibe fluida, sin trabas apreciables
- **Y** si un dispositivo no alcanza el presupuesto, **se degrada la fidelidad visual por código** —menos polígonos, menos esferas en el follaje, sin sombras— hasta cumplirlo: **el presupuesto manda sobre la fidelidad**
- **Y** el resto de la aplicación **no sufre degradación** de arranque ni de navegación por la incorporación del WebView
- **Y** el incremento de peso del APK que aporta Three.js queda **medido y registrado** en la documentación de la historia

### Escenario 3: Casos Extremos

#### CA-38.07 — Documentación actualizada

- **Dado** que se incorpora el primer WebView del proyecto y una dependencia JavaScript
- **Cuando** se actualiza la documentación
- **Entonces** `docs/architecture/architecture_blueprint.md` refleja el nuevo componente de renderizado y **una decisión arquitectónica (ADR)** que registre la elección de resolver el 3D por generación procedural en WebView sin assets externos, con sus alternativas descartadas
- **Y** `docs/architecture/interfaces_contract.md` refleja el **contrato del puente nativo ↔ web** y su comportamiento ante fallo
- **Y** la excepción del anti-alcance queda registrada: la app sigue siendo 100% offline y Three.js viaja empaquetado
- **Y** `system_definition_document.md` **no se modifica**

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante — único usuario del sistema. La aplicación es single-user y no maneja roles.
- **Permisos requeridos:** Ninguno.
- **Valor de negocio:** Un ícono informa; un ser vivo que se puede girar con el dedo genera apego. El salto de `HU-37` a esta historia es exactamente el salto de *saber* el estado del entrenamiento a *querer* cuidarlo. Es la parte de la funcionalidad que justifica llamarla recompensa y no indicador.

### Reglas de Negocio

1. **Esta historia solo cambia cómo se dibuja el árbol.** El cálculo, la persistencia, la navegación y el respaldo son de `HU-37` y no se tocan.
2. **La tarjeta de Inicio nunca lleva WebView.** Restricción permanente de rendimiento (RNF01), no una limitación provisional.
3. **La representación nativa no se elimina.** Pasa a fallback y se conserva en el código de forma permanente.
4. **El presupuesto manda sobre la fidelidad.** Ante conflicto, se degradan los gráficos, no la fluidez.
5. **Sin assets 3D externos.** El árbol se genera por código. No se empaquetan `.glb` ni `.gltf`.
6. **Sin red.** Three.js y el HTML viajan dentro del APK. El WebView no navega a contenido remoto.
7. **Tamaño y color son independientes.** El tamaño expresa la etapa; el color y el follaje expresan la salud. Un árbol maduro puede estar marchito.
8. **La excepción de alcance de `HU-37` sigue vigente sin ampliarse.** El árbol no alimenta al motor de decisión, no genera alertas y no altera KPIs.

### Interfaz

**No se crea ninguna pantalla nueva ni ninguna ruta nueva.** Se sustituye un bloque visual dentro de una pantalla que ya existe.

- **Inicio (B1):** sin cambios. La tarjeta permanece nativa tal como la dejó `HU-37`.
- **Pantalla del árbol:** el área del árbol pasa de ícono vectorial a WebView con el modelo 3D. Barra superior, puntaje, días y mensaje contextual permanecen idénticos.
- **Resto de la aplicación:** sin cambios.

#### Detalle de Interfaz de Usuario

- **Diseño general:** El WebView ocupa el área que `HU-37` reservó para el árbol, con fondo transparente sobre el fondo nativo de la app, correcto en modo claro y oscuro (RNF23). El bloque de estado inferior no se reorganiza. Solo portrait (RNF07).
- **Campos y controles:** Ningún control de formulario. La única interacción nueva es el gesto de arrastre para rotar la cámara, con zoom y paneo acotados.
- **Flujo de navegación visual:** Sin rutas nuevas. Inicio → pantalla del árbol → retroceso, igual que en `HU-37`.
- **Mensajes y feedback:** **Ningún texto nuevo.** Se conservan íntegras las bandas de salud y las tablas de textos definidas en `HU-37` — tarjeta de Inicio y pantalla dedicada — sin añadir, quitar ni reformular ninguna cadena. Ante fallo del WebView **no se muestra mensaje de error**: se sustituye silenciosamente por el ícono nativo y la pantalla sigue siendo útil.

### Sistemas Externos

Ninguno de red. La aplicación mantiene su operación 100% offline (RNF09). No hay consumo de APIs.

Se incorporan **dos dependencias locales nuevas**, ambas empaquetadas en el APK:

- **WebView nativo de Android** — componente de la plataforma. **Primer uso en el proyecto**: no existe ningún WebView en el código actual, verificado sobre el repositorio.
- **Three.js** — librería JavaScript de renderizado 3D, servida desde los assets locales. Sin CDN ni descarga en tiempo de ejecución.

El intercambio de datos es local: la capa nativa entrega salud y etapa al código web a través del puente JavaScript del WebView. El código web no devuelve datos al sistema.

### Preview de Interfaz

**Preview:** [`38.preview.txt`](./38.preview.txt) | **Formato:** ASCII (wireframe de texto)

Cubre el antes/después respecto a `HU-37` delimitando exactamente qué cambia, el render 3D sobre fondo transparente, la transición continua entre estados de salud, los límites de la cámara, el fallback nativo y el orden de degradación por rendimiento.

---

## Contexto y Referencias

**Arquitectura:** `docs/architecture/architecture_blueprint.md` (§1.3 Anti-Alcance, §2.1 inventario de contenedores, §5 ADR), `docs/architecture/interfaces_contract.md` (Flujo B — Inicio), `docs/architecture/coding-standards.md`, `docs/domain/definition/system_definition_document.md` §2.1 (frontera de alcance, no se modifica).

**Historia hermana:**

- **[HU-37](../HU-37-arbol-progreso-entrenamiento/historia.md)** — El árbol de mi entrenamiento. **Dependencia dura.** Aporta la entidad de persistencia, el cálculo de salud y etapa, la ruta, la pantalla dedicada, el bloque de estado, el respaldo y la representación nativa que aquí pasa a fallback. **Debe estar implementada antes.**

**Historias relacionadas:**

- **HU-03** — Diccionario de Ejercicios con media visual. Único precedente de assets visuales empaquetados y de gestión de recursos multimedia en el proyecto.
- **HU-22** — Preview de sesión sin iniciar. Precedente de pantalla dedicada colgada de Inicio.
- **HU-19** — Backup y Restauración. No se modifica en esta historia; su integración se resolvió en `HU-37`.

**Restricciones transversales aplicables:**

- RNF01 — El renderizado no bloquea la interfaz, y el arranque de la app no se degrada
- RNF07 — Solo portrait
- RNF09 — 100% offline: Three.js y el HTML se empaquetan, no se descargan
- RNF20 — Compatible con Android 8.0+ (API 26): el fallback cubre los WebView antiguos
- RNF21 / RNF22 — Pantallas de 5" a 7", de 720p a 1440p
- RNF23 — Tema claro/oscuro: el fondo transparente debe respetar ambos
- RNF28 — Arquitectura MVVM
- RNF31 — El HTML, el JS y Three.js como recursos versionados en assets, no hardcodeados
- RNF37 — APK firmado sin Google Play Store: el peso añadido importa

**Lecciones aprendidas:** el proyecto ha sido deliberadamente nativo y sin dependencias de terceros más allá del stack de Jetpack. Introducir un WebView rompe esa homogeneidad por primera vez, y la lección de `HU-36` aplica en su forma general: los mecanismos nuevos deben quedar acotados por una frontera explícita. Aquí la frontera es doble — el WebView vive en una sola pantalla y no puede navegar fuera de su archivo local, y el fallback nativo garantiza que su ausencia nunca degrade la funcionalidad.

**Riesgo declarado:** **esta es la historia donde se concentra todo el riesgo técnico** de la funcionalidad del árbol. Los puntos a resolver en el análisis arquitectónico son el ciclo de vida y las fugas de memoria del WebView, el peso que Three.js añade al APK, la fidelidad del fondo transparente sobre distintos fabricantes, y el cumplimiento del presupuesto de 1 segundo en gama baja. La partición existe precisamente para que este riesgo no bloquee la entrega del árbol: si esta historia se complica, `HU-37` ya está en producción y el ejecutante no se queda sin funcionalidad.

---

## Definición de Terminado (Inicial)

- [ ] Árbol 3D generado por código en estilo Low-Poly, sin archivos `.glb` ni `.gltf` en el APK
- [ ] Tamaño del árbol correspondiente a la etapa, independiente de la salud
- [ ] Transición fluida y continua entre estados de salud, sin saltos entre modelos fijos
- [ ] Rotación de cámara suave con límites de zoom y paneo, sin scroll web ni nativo
- [ ] Cámara reiniciada a su posición inicial al reentrar a la pantalla
- [ ] WebView con fondo transparente correcto en modo claro y oscuro
- [ ] Barras de desplazamiento ocultas y scroll web bloqueado
- [ ] WebView sin navegación remota, cargando exclusivamente el archivo local empaquetado
- [ ] Ciclo de vida del WebView gestionado, sin fugas de memoria ni instancias duplicadas tras segundo plano
- [ ] Puente JavaScript recibiendo salud y etapa desde el lado nativo
- [ ] Fallback nativo funcionando cuando el WebView no está disponible, conservado permanentemente en el código
- [ ] Carga y render inicial en menos de 1 segundo en gama media
- [ ] Degradación de fidelidad por código implementada y verificada en gama baja
- [ ] Arranque y navegación de la app medidos sin degradación
- [ ] Incremento de peso del APK medido y registrado
- [ ] Tarjeta de Inicio verificada sin cambios y sin WebView
- [ ] Cálculo, persistencia, navegación y respaldo de `HU-37` verificados sin regresión
- [ ] `architecture_blueprint.md` con ADR nuevo e `interfaces_contract.md` con el contrato del puente, actualizados; `system_definition_document.md` sin modificar
