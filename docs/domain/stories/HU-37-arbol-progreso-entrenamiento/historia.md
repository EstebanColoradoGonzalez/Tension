# Historia de Usuario

**Como** El Ejecutante,
**Quiero** ver un árbol que crece con mis entrenamientos acumulados y cuya salud refleja qué tan reciente fue el último,
**Para** reconocer de un vistazo el estado real de mi entrenamiento sin tener que interpretar métricas.

> **Historia resultante de partición.** Es la **primera de dos** historias en que se dividió la historia original *Árbol de progreso del entrenamiento* (16 criterios de aceptación). Esta hija construye el árbol completo con representación **nativa**; [`HU-38`](../HU-38-arbol-3d-interactivo/historia.md) sustituye después esa representación por un modelo 3D interactivo. **Debe implementarse primero.**

## Descripción

El sistema ya sabe todo lo necesario para decir si El Ejecutante viene entrenando o no: la fecha de la última sesión y cuántas lleva registradas. Esa información hoy solo existe en forma de números —adherencia semanal, alertas de inactividad— y exige leerla e interpretarla.

Esta historia añade una **representación visual de ese mismo dato**: un árbol con dos dimensiones independientes.

- **La estatura del árbol** expresa el historial acumulado: cuántas sesiones se han registrado desde el principio. Empieza como semilla y avanza por etapas hasta florecer. **Nunca retrocede.**
- **La salud del árbol** expresa la constancia reciente: cuántos días han pasado desde el último entrenamiento. Va de 100 (verde vivo) a 0 (marrón, marchito).

Ambas son ortogonales: un árbol maduro puede estar completamente marchito, y un brote recién nacido puede estar perfectamente sano. La ausencia se castiga en la salud, nunca en la estatura, para que faltar no borre lo ya construido.

**El árbol de esta historia es nativo.** Un ícono vectorial por etapa, teñido dinámicamente según la salud. Sin WebView, sin dependencias JavaScript, sin riesgo técnico nuevo: es Jetpack Compose, como el resto de la aplicación. Al terminar esta historia el ejecutante ya tiene su árbol funcionando en el 100% de los dispositivos soportados.

Esta historia también construye **toda la infraestructura** de la funcionalidad: la entidad de persistencia, el cálculo, los momentos de recálculo, la ruta de navegación, la pantalla dedicada y la integración con el respaldo. `HU-38` no toca nada de eso — se limita a sustituir el bloque visual dentro de una pantalla que ya existe.

### Frontera de alcance — excepción declarada

`docs/domain/definition/system_definition_document.md` §2.1 mantiene **fuera del sistema** la *"motivación, estado anímico y adherencia del ejecutante"* y los *"días del calendario o frecuencia de asistencia"* como insumos de decisión. **Esa exclusión no se levanta.**

Esta historia se documenta como **excepción explícita y acotada**: el árbol es una funcionalidad **puramente visual y aislada**. Lee datos ya existentes y no escribe nada que ningún otro componente consuma. En concreto:

- **No alimenta al motor de decisión.** Ni la prescripción de carga, ni el Doble Umbral, ni la detección de meseta, regresión o fatiga, ni el protocolo de descarga leen el estado del árbol.
- **No altera la determinación de sesión ni la rotación cíclica.**
- **No genera alertas** ni modifica las existentes (`ROUTINE_INACTIVITY`, `LOW_ADHERENCE`).
- **No cambia ningún KPI.** La adherencia semanal se sigue calculando exactamente igual.

La dependencia es unidireccional: el árbol lee del historial; nada del sistema lee del árbol.

---

## Criterios de Aceptación

### Escenario 1: Flujo Principal

#### CA-37.01 — Acceso desde Inicio

- **Dado** que El Ejecutante está en la pantalla de Inicio
- **Cuando** observa la pantalla
- **Entonces** ve una tarjeta de acceso al árbol ubicada **debajo** de la tarjeta de sesión del día
- **Y** la tarjeta refleja el **estado actual** del árbol mediante el ícono de su etapa y un texto contextual, no un contenido estático
- **Y** la tarjeta es **nativa** y lo seguirá siendo: nunca renderiza contenido web, para no penalizar el arranque de Inicio (RNF01)
- **Y** al tocarla, el sistema navega a la pantalla dedicada del árbol
- **Y** el área táctil de la tarjeta es de al menos 48×48 dp (RNF06)

#### CA-37.02 — Pantalla dedicada del árbol

- **Dado** que El Ejecutante toca la tarjeta de acceso
- **Cuando** se abre la pantalla del árbol
- **Entonces** se muestra el árbol ocupando el área principal
- **Y** se muestra el **puntaje de salud** en su escala de 0 a 100
- **Y** se muestran los **días transcurridos desde el último entrenamiento**
- **Y** se muestra un **mensaje contextual** acorde al estado del árbol
- **Y** la única acción de navegación disponible es el retroceso nativo de la barra superior
- **Y** la pantalla es una **ruta nueva** en la navegación, sin pestaña propia en la barra inferior

> **Frontera con HU-38:** esta historia deja lista la ruta, el layout, la barra superior y el bloque de estado. `HU-38` sustituye **únicamente** el área del árbol.

#### CA-37.03 — Puntaje de salud calculado y persistido

- **Dado** que existe al menos una sesión registrada
- **Cuando** el sistema calcula la salud del árbol
- **Entonces** obtiene `d` = días naturales entre la fecha de la última sesión registrada y la fecha actual
- **Y** aplica: `d ≤ 2` → **100** · `2 < d < 14` → **descenso lineal** de 100 a 0 · `d ≥ 14` → **0**
- **Y** el resultado es siempre un entero dentro del rango **0–100**
- **Y** el puntaje se **persiste** en la base de datos local

> **Verificación del descenso lineal:** `d = 3` → 92 · `d = 5` → 75 · `d = 8` → **50** · `d = 11` → 25 · `d = 14` → 0.
> El corte en 14 días se alinea deliberadamente con el umbral de crisis de `ROUTINE_INACTIVITY`.

#### CA-37.04 — Etapa de crecimiento por sesiones acumuladas

- **Dado** que el sistema conoce el número total de sesiones registradas
- **Cuando** determina la etapa de crecimiento del árbol
- **Entonces** aplica: **Semilla** = 0 sesiones · **Brote** = 1 a 9 · **Joven** = 10 a 29 · **Maduro/Florecido** = 30 o más
- **Y** la etapa alcanzada **nunca retrocede**, cualquiera que sea la salud
- **Y** la etapa se persiste junto con el puntaje de salud

#### CA-37.05 — Representación visual nativa

- **Dado** que el árbol tiene una etapa y un puntaje de salud
- **Cuando** se renderiza en la tarjeta de Inicio o en la pantalla dedicada
- **Entonces** se muestra **un ícono vectorial correspondiente a su etapa** — cuatro en total: semilla, brote, joven y maduro
- **Y** el ícono se **tiñe dinámicamente según la salud**: verde vivo con salud alta, amarillento o seco con salud media, marrón u oscuro con salud baja
- **Y** el color se deriva del tema de la aplicación y respeta modo claro y oscuro (RNF23)
- **Y** las dos dimensiones se leen por separado: la **forma** comunica la etapa, el **color** comunica la salud

### Escenario 2: Validaciones

#### CA-37.06 — Momentos de recálculo

- **Dado** que el estado del árbol depende de la fecha actual y del historial
- **Cuando** ocurre cualquiera de estos tres eventos
- **Entonces** el sistema recalcula y persiste salud y etapa:
  1. **Al cerrar una sesión** — el árbol reacciona de inmediato al entrenamiento recién registrado
  2. **En cada emisión del cambio de día** — cubre el arranque de la app y el cruce de medianoche con la app abierta
  3. **Al abrir la pantalla del árbol** — garantiza que lo mostrado nunca sea un valor rancio
- **Y** en el caso 2 el recálculo se ejecuta **después** del barrido de cierre automático de sesiones del día anterior (`B1-T7`), nunca antes
- **Y** el cálculo no bloquea la interfaz (RNF01)

> **Por qué el orden importa (alineación con HU-36):** el barrido cierra como `INCOMPLETE` la sesión abierta de ayer **conservando su `date` original**. Si el árbol se recalculara antes, leería una fecha de último entrenamiento desactualizada y marchitaría el árbol de alguien que sí entrenó.

#### CA-37.07 — Qué cuenta como entrenamiento

- **Dado** que el árbol debe reflejar esfuerzo real y no intención
- **Cuando** el sistema determina la fecha del último entrenamiento y el total acumulado
- **Entonces** considera las sesiones con estado **`COMPLETED`** e **`INCOMPLETE`**
- **Y** **no** considera las sesiones `IN_PROGRESS`
- **Y** los días marcados con **«Hoy no entreno»** (`day_skip`) **no** protegen al árbol: omitir un día lo marchita igual que no abrir la app
- **Y** una sesión reasignada temporalmente (`daily_routine_override`) cuenta como cualquier otra: al árbol le da igual **qué** rutina se entrenó

> **Alineación con HU-36:** cerrar una sesión **sin ninguna serie registrada** la **descarta** en lugar de guardarla como `INCOMPLETE`. Por tanto toda sesión persistida implica al menos una serie, y este criterio no necesita condición adicional sobre `exercise_set`.

#### CA-37.08 — Aislamiento respecto al motor de decisión

- **Dado** que la funcionalidad se declara como excepción visual y aislada
- **Cuando** se implementa la historia
- **Entonces** ningún componente del motor de decisión —prescripción de carga, Doble Umbral, meseta, regresión, fatiga, protocolo de descarga, rotación cíclica— lee el puntaje ni la etapa del árbol
- **Y** el árbol **no genera alertas** ni modifica el comportamiento de `ROUTINE_INACTIVITY` ni de `LOW_ADHERENCE`
- **Y** el cálculo de la **adherencia semanal y del resto de KPIs permanece idéntico**
- **Y** la exclusión de `system_definition_document.md` §2.1 **se mantiene intacta**: esta historia no la modifica

#### CA-37.09 — Respaldo y restauración

- **Dado** que la etapa de crecimiento acumulada representa progreso del ejecutante
- **Cuando** se exporta un respaldo y luego se restaura
- **Entonces** salud y etapa del árbol se **preservan**
- **Y** la versión del formato de respaldo se incrementa en consecuencia
- **Y** un respaldo de la versión anterior se restaura sin error, dejando el árbol en un estado válido derivado del historial restaurado

### Escenario 3: Casos Extremos

#### CA-37.10 — Ejecutante sin historial

- **Dado** que El Ejecutante acaba de instalar la aplicación y **no tiene ninguna sesión registrada**
- **Cuando** abre la pantalla del árbol
- **Entonces** el árbol se muestra en etapa **Semilla**
- **Y** el estado se presenta como punto de partida, **sin castigar** a quien todavía no ha tenido oportunidad de entrenar
- **Y** el texto acompañante invita a registrar la primera sesión, sin mostrar un conteo de días de inactividad que no tendría referencia

#### CA-37.11 — Ausencia prolongada con árbol maduro

- **Dado** que El Ejecutante alcanzó la etapa **Maduro/Florecido** y lleva 30 días sin entrenar
- **Cuando** abre la pantalla del árbol
- **Entonces** el árbol conserva su **etapa de maduro** y el ícono correspondiente
- **Y** se muestra **completamente marchito**: salud 0 y color marrón u oscuro
- **Y** al registrar una sesión, **recupera la salud** manteniendo la etapa ya alcanzada

#### CA-37.12 — Documentación actualizada

- **Dado** que se incorpora una entidad nueva al dominio
- **Cuando** se actualiza la documentación
- **Entonces** `docs/architecture/domain_and_state_model.md` refleja la entidad de estado del árbol, sus campos y el dominio cerrado de las etapas de crecimiento
- **Y** `docs/architecture/architecture_blueprint.md` refleja el nuevo componente y su aislamiento respecto al motor de decisión
- **Y** `docs/architecture/interfaces_contract.md` refleja el nuevo acceso desde Inicio y la pantalla dedicada
- **Y** la excepción de alcance queda registrada, **sin modificar** `system_definition_document.md`

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante — único usuario del sistema. La aplicación es single-user y no maneja roles.
- **Permisos requeridos:** Ninguno.
- **Valor de negocio:** El sistema ya mide la constancia, pero la expresa en porcentajes y alertas que exigen ser leídos e interpretados. Un árbol que crece y se marchita comunica el mismo hecho sin intermediarios y funciona como recompensa por el historial acumulado. La estatura hace visible el esfuerzo de meses; la salud hace visible el de la última semana.

### Reglas de Negocio

1. **Dos dimensiones ortogonales.** La **estatura** depende del historial acumulado; la **salud** depende de la recencia. Son independientes: un árbol maduro puede estar marchito y un brote puede estar sano.
2. **La salud se calcula por días transcurridos.** `d ≤ 2` → 100. Entre 3 y 13 días, descenso lineal. A los 14 días o más, 0. El margen de 48 horas reconoce que descansar un día es parte del entrenamiento, no una falta.
3. **El corte de 14 días no es arbitrario.** Coincide con el umbral de crisis de `ROUTINE_INACTIVITY`, de modo que el árbol termina de marchitarse justo cuando el sistema ya considera crítica la inactividad.
4. **La estatura se mide en sesiones acumuladas.** Cuatro etapas: Semilla (0), Brote (1–9), Joven (10–29), Maduro/Florecido (30+). El crecimiento es lento y meritorio a propósito.
5. **La etapa nunca retrocede.** Faltar marchita el árbol, no lo encoge. Nadie vuelve a empezar de cero en tamaño.
6. **Solo el esfuerzo registrado cuenta.** Sesiones `COMPLETED` e `INCOMPLETE`. Las `IN_PROGRESS` no. Como HU-36 descarta las sesiones cerradas sin ninguna serie, toda sesión persistida ya implica esfuerzo real.
7. **Omitir el día no protege al árbol.** «Hoy no entreno» resuelve el día para la determinación de sesión, pero no crea sesión, no cuenta como adherencia y no silencia la inactividad. El árbol se comporta igual: se marchita.
8. **Al árbol le da igual qué rutina se entrenó.** Una sesión reasignada temporalmente cuenta como cualquier otra.
9. **El recálculo ocurre en tres momentos** —cierre de sesión, cambio de día y apertura de la pantalla— y en el cambio de día **siempre después** del barrido de cierre automático.
10. **El árbol lee, nunca escribe hacia el sistema.** Dependencia estrictamente unidireccional. Ningún componente de decisión, alerta o KPI consume su estado.
11. **La exclusión de alcance se mantiene.** La motivación y la adherencia siguen fuera del sistema como insumos de decisión. Esta historia es una excepción visual acotada, no una ampliación del dominio.
12. **La forma comunica la etapa, el color comunica la salud.** Cuatro íconos, no doce ilustraciones. Evita una matriz de assets que `HU-38` dejaría obsoleta.
13. **La tarjeta de Inicio es nativa para siempre.** No es una decisión provisional en espera de `HU-38`: es una restricción de rendimiento permanente (RNF01).

### Interfaz

Se añade **una pantalla nueva** y **un acceso** en la pantalla existente de Inicio.

- **Inicio (B1):** nueva tarjeta de acceso, ubicada **debajo** de la tarjeta de sesión del día, para no competir con la acción primaria de la jornada. Muestra el ícono de la etapa teñido según la salud, y un texto que cambia con el estado.
- **Pantalla del árbol (nueva):** dedicada exclusivamente a visualizar el árbol. Barra superior con retroceso nativo, árbol, puntaje, días desde el último entrenamiento y mensaje contextual.
- **Resto de la aplicación:** sin cambios. Ninguna otra pantalla se modifica.

#### Detalle de Interfaz de Usuario

- **Diseño general:** La pantalla del árbol dedica el área principal a la representación del árbol sobre el fondo nativo de la app. Debajo, un bloque compacto de estado. Solo portrait (RNF07). El layout se diseña previendo que el área del árbol será sustituida por `HU-38` sin reorganizar el resto.
- **Campos y controles:** Tarjeta de acceso en Inicio con área táctil mínima de 48×48 dp (RNF06). **Reutiliza el componente de tarjeta ya existente en Inicio** — misma elevación, radio de esquinas, padding y colores base que *"Hoy te toca"* y *"Día de descanso"*. No se diseña un estilo propio: su posición debajo de la tarjeta de sesión ya le da la jerarquía subordinada correcta. En la pantalla del árbol no hay controles de formulario: la única acción es el retroceso de la barra superior.
- **Flujo de navegación visual:** Inicio → tarjeta del árbol → pantalla del árbol → retroceso a Inicio. Ruta nueva, sin pestaña propia en la navegación inferior.
- **Mensajes y feedback:** Registro sobrio, segunda persona, frases cortas, sin signos de admiración ni emojis — el mismo de `strings.xml`. En Semilla **no** se muestra conteo de días.

##### Bandas de salud para los textos

| Banda | Rango |
|---|---|
| Alta | ≥ 67 |
| Media | 34 – 66 |
| Baja | 1 – 33 |
| Marchito | 0 |

##### Textos de la tarjeta de Inicio

Título fijo: **Tu árbol**. Línea dinámica según el estado:

| Estado | Texto |
|---|---|
| Semilla | Aún no ha germinado |
| Alta | Está en su mejor momento |
| Media | Empieza a secarse |
| Baja | Se está marchitando |
| Marchito | Está marchito |

##### Textos de la pantalla dedicada

La etapa se muestra con su nombre: `Semilla` · `Brote` · `Joven` · `Maduro`.

| Estado | Días | Mensaje contextual |
|---|---|---|
| Semilla | *(no se muestra)* | Registra tu primera sesión y tu árbol germinará. |
| Alta | Último entrenamiento: hoy / ayer / hace N días | Vienes entrenando. Tu árbol lo refleja. |
| Media | hace N días | Han pasado N días. Tu árbol empieza a secarse. |
| Baja | hace N días | Han pasado N días. Tu árbol se está marchitando. |
| Marchito | hace N días | Han pasado N días. Tu árbol está marchito. Una sesión basta para revivirlo. |

### Sistemas Externos

Ninguno. La aplicación opera 100% offline sobre almacenamiento local (RNF09, RNF14). No hay consumo de APIs, ni dependencias nuevas de terceros: esta historia se resuelve íntegramente con Jetpack Compose y Room, el stack ya presente en el proyecto.

### Preview de Interfaz

**Preview:** [`37.preview.txt`](./37.preview.txt) | **Formato:** ASCII (wireframe de texto)

Cubre la tarjeta de acceso en Inicio con sus cinco estados, la pantalla dedicada con salud alta, media y baja, los dos casos extremos —Semilla sin historial y Maduro marchito— y el flujo de navegación.

---

## Contexto y Referencias

**Arquitectura:** `docs/architecture/domain_and_state_model.md` (entidad `session` y su ciclo de vida en §5.1; `day_skip`, `daily_routine_override`; §4 Dominios Cerrados), `docs/architecture/architecture_blueprint.md` (§1.3 Anti-Alcance, contenedores `UI-01`, `DAT-01`, `DB-01`, decisiones D-14 y D-15), `docs/architecture/interfaces_contract.md` (Flujo B — Inicio, triggers `B1-T1` a `B1-T7`), `docs/domain/definition/system_definition_document.md` §2.1 (frontera de alcance).

**Historia hermana:**

- **[HU-38](../HU-38-arbol-3d-interactivo/historia.md)** — El árbol en 3D. Sustituye la representación nativa de esta historia por un modelo tridimensional interactivo, conservando la nativa como fallback. **Se implementa después.**

**Historias relacionadas:**

- **HU-36** — Días de la semana como entidad reasignable. **Referencia obligatoria.** Redefinió por completo el manejo de sesiones: descarte de sesiones sin series, `day_skip` como acto distinto de cerrar, cierre automático al cambiar el día y `CurrentDateProvider` como ticker compartido. Toda la lógica de validación de sesiones de esta historia se alinea con ese mecanismo.
- **HU-09** — Cerrar sesión y avanzar rotación. Momento de recálculo número 1.
- **HU-18** — Sistema de Alertas. Su `ROUTINE_INACTIVITY` mide inactividad **por rutina**; el árbol mide inactividad **global**. Son complementarias y no deben acoplarse.
- **HU-15** — Analítica y KPIs. La adherencia semanal es la lectura numérica de lo que el árbol expresa visualmente. No se modifica.
- **HU-19** — Backup y Restauración. Debe incorporar el estado del árbol.
- **HU-22** — Preview de sesión sin iniciar. Precedente de pantalla dedicada colgada de Inicio.
- **HU-33** — Alertas comprensibles y accionables. Precedente del criterio de comunicar estado sin exigir interpretación.

**Restricciones transversales aplicables:**

- RNF01 — El cálculo no bloquea la interfaz
- RNF06 — Área táctil mínima 48×48 dp en la tarjeta de acceso
- RNF07 — Solo portrait
- RNF09 — 100% offline
- RNF14 / RNF35 — Persistencia en SQLite mediante Room
- RNF20 — Compatible con Android 8.0+ (API 26)
- RNF21 / RNF22 — Pantallas de 5" a 7", de 720p a 1440p
- RNF23 — Tema claro/oscuro: el tinte del ícono debe funcionar en ambos
- RNF28 — Arquitectura MVVM
- RNF29 — El cálculo de salud y etapa es una regla pura, sin dependencias de Android
- RNF30 — Pruebas unitarias sobre el cálculo de salud, la determinación de etapa y los casos límite
- RNF31 — Los íconos de etapa como recursos versionados, no hardcodeados
- **Beta sin migración:** la base de datos se reinicia desinstalando y reinstalando; los cambios de esquema se validan sobre instalación fresca. Excepción documentada a RNF19 (ADR-019), limitada a esta historia.

**Lecciones aprendidas:** HU-36 dejó una advertencia directamente aplicable aquí: un mecanismo que cuenta sesiones mal —guardando como válidas las que no tuvieron ninguna serie— termina **premiando en las métricas justo lo contrario de lo ocurrido**. Un árbol que florece porque el ejecutante abrió y cerró la app sin entrenar sería exactamente ese mismo defecto en versión visual, y más dañino, porque el refuerzo es inmediato. Por eso el criterio de qué cuenta como entrenamiento se ancla al mecanismo ya corregido en HU-36 y no define uno propio.

**Riesgo declarado:** bajo. Esta historia no introduce ninguna tecnología nueva. Su único punto de atención es el **orden de ejecución dentro del cambio de día** (CA-37.06): recalcular antes del barrido de cierre automático produciría un árbol marchito para alguien que sí entrenó.

---

## Definición de Terminado (Inicial)

- [ ] Tarjeta de acceso en Inicio, debajo de la tarjeta de sesión, reflejando el estado actual del árbol
- [ ] Pantalla dedicada con árbol, puntaje, días desde el último entrenamiento y mensaje contextual
- [ ] Ruta de navegación nueva, sin pestaña propia en la barra inferior
- [ ] Cuatro íconos vectoriales de etapa, teñidos dinámicamente según la salud, correctos en modo claro y oscuro
- [ ] Puntaje de salud 0–100 calculado según la regla de días y persistido en base de datos local
- [ ] Etapa de crecimiento en cuatro niveles por sesiones acumuladas, irreversible
- [ ] Recálculo en los tres momentos definidos, y en el cambio de día siempre después del barrido de cierre automático
- [ ] Solo sesiones `COMPLETED` e `INCOMPLETE` cuentan; `day_skip` no protege al árbol
- [ ] Estado del árbol incorporado al respaldo y la restauración, con versión de formato incrementada
- [ ] Ejecutante sin historial mostrado como Semilla, sin conteo de días de inactividad
- [ ] Árbol maduro y ausente mostrado marchito conservando su etapa, y recuperando salud al entrenar
- [ ] Motor de decisión, alertas y KPIs verificados sin cambios de comportamiento
- [ ] Reglas de salud y de etapa cubiertas por pruebas unitarias, incluidos los límites 2, 3, 13 y 14 días y los cortes 0, 1, 9, 10, 29 y 30 sesiones
- [ ] `domain_and_state_model.md`, `architecture_blueprint.md` e `interfaces_contract.md` actualizados, y `system_definition_document.md` sin modificar
- [ ] Layout de la pantalla verificado como sustituible: el área del árbol puede cambiarse sin reorganizar el resto
