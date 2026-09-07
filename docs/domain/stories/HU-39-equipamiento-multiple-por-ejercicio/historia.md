# Historia de Usuario

**Como** El Ejecutante,
**Quiero** que cada ejercicio declare todos los implementos con los que puedo hacerlo y poder elegir cuál uso al registrar cada serie,
**Para** que mi historial diga con qué entrené de verdad y dejen de existir ejercicios duplicados por implemento.

> **Hija 1 de 5** de la partición de *Equipamiento por ejercicio, 1RM estimado y sesión editable* (40 CAs). Hermanas: [`HU-40`](../HU-40-progresion-por-equipamiento/index.md), [`HU-41`](../HU-41-catalogo-anatomico-plan-alineado/index.md), [`HU-42`](../HU-42-1rm-estimado/index.md), [`HU-43`](../HU-43-ajustar-sesion-del-dia/index.md). Se implementa **primero**: las cuatro hermanas dependen de ella.

## Descripción

El sistema modela hoy el equipamiento como un **atributo fijo del ejercicio**: `exercise` tiene una única `equipment_type_id` obligatoria y la clave natural del catálogo es `(nombre, equipamiento)`. Bajo ese modelo, *Elevación Lateral con mancuerna* y *Elevación Lateral en polea* solo pueden existir como **dos ejercicios distintos**, con dos historiales que nunca se relacionan. Y para no duplicar el catálogo, el equipamiento acabó llenándose de valores compuestos —`Mancuerna o Polea`, `Barra o Mancuernas`, `Mancuerna o Polea o Barra`— que describen una disyunción en lugar de un implemento: el ejercicio dice *«se hace con una cosa o con otra»* pero la serie **no registra cuál**.

Esta historia invierte esa relación. El **ejercicio declara qué implementos admite** —una lista, siempre de al menos uno— y **la serie declara cuál se usó**. El equipamiento deja de ser identidad del catálogo y pasa a ser un dato del registro.

Es la hija que concentra **toda la infraestructura** de la funcionalidad: el catálogo de equipamiento normalizado a valores atómicos, la relación de un ejercicio con varios implementos, el equipamiento persistido en la serie, los formularios del Diccionario, el historial que lo expone, el cambio de esquema y la ampliación del formato de respaldo. Sus cuatro hermanas construyen encima sin volver a tocar nada de esto.

Su valor propio es inmediato y verificable sin las hermanas: el registro y el historial dejan de ser ambiguos sobre el implemento, y el catálogo deja de necesitar un ejercicio por cada combinación.

### Frontera de alcance

- **La progresión sigue resolviéndose por ejercicio.** Esta historia registra el implemento con fidelidad, pero **no** cambia todavía la unidad de comparación del motor de decisión. Eso es [`HU-40`](../HU-40-progresion-por-equipamiento/historia.md).
- **Riesgo conocido y transitorio:** mientras `HU-40` no esté implementada, el falso `REGRESSION` por cambio de implemento **sigue ocurriendo**. No es un defecto introducido aquí —es el defecto preexistente que motiva la partición— y el orden 1 → 2 es obligatorio precisamente por eso.
- **Las zonas musculares no se tocan.** Los 37 ejercicios conservan su clasificación muscular actual hasta [`HU-41`](../HU-41-catalogo-anatomico-plan-alineado/historia.md).
- **El plan no gana equipamiento sugerido aquí.** Hasta `HU-41`, el selector se preselecciona con el último implemento usado en ese ejercicio.
- **Los agarres no son equipamiento.** Cuerda, barra en V y accesorios equivalentes de polea son todos `Polea`.

---

## Criterios de Aceptación

### Escenario 1: Flujo Principal

#### CA-39.01 — Catálogo de equipamiento normalizado

- **Dado** que la aplicación se instala por primera vez
- **Cuando** se ejecuta el sembrado del catálogo de equipamiento
- **Entonces** el catálogo contiene exactamente **15 tipos atómicos**:

| # | Tipo | Origen |
|---|---|---|
| 1 | Máquina | existente |
| 2 | Máquina Smith | **nuevo** |
| 3 | Polea | existente |
| 4 | Barra | renombrado desde *Barra de Pesas* |
| 5 | Barra Fija | existente |
| 6 | Mancuerna | existente (singular) |
| 7 | Pesa Rusa | renombrado desde *Kettlebell* |
| 8 | Banda Elástica | existente |
| 9 | Peso Corporal | renombrado desde *Cuerpo* |
| 10 | Peso Añadido | **nuevo** |
| 11 | Barra EZ | existente — sin ejercicio seed, disponible a futuro |
| 12 | TRX/Suspensión | existente — sin ejercicio seed, disponible a futuro |
| 13 | Balón Medicinal | existente — sin ejercicio seed, disponible a futuro |
| 14 | Rodillo de Abdomen | existente — sin ejercicio seed, disponible a futuro |
| 15 | Paralelas/Dip Station | existente — sin ejercicio seed, disponible a futuro |

- **Y** se retiran los **10 tipos** siguientes, por ser compuestos, duplicados o no constituir maquinaria independiente: `Mancuernas` (plural, colapsado en *Mancuerna*), `Pesa`, `Máquina Multiestación` (subsumido en *Máquina*), `Polea con Cuerda` y `Polea con Barra en V` (son agarres, no equipamiento), `Mancuerna o Polea`, `Mancuerna o Polea o Barra`, `Barra o Mancuernas`, `Mancuernas o Polea` y `Mancuerna o Pesa Rusa`
- **Y** ningún tipo del catálogo expresa una disyunción: **un tipo, un implemento**
- **Y** `Máquina de Remo` y `Máquina Contractor` **no** son tipos propios — ambos son `Máquina`

#### CA-39.02 — Un ejercicio declara una o más opciones de equipamiento

- **Dado** un ejercicio del Diccionario
- **Cuando** se consulta su definición
- **Entonces** el ejercicio tiene asociada una **lista de equipamientos admitidos**, de uno o más elementos
- **Y** el atributo es **obligatorio**: ningún ejercicio puede existir con la lista vacía
- **Y** el nombre del ejercicio **ya no forma clave compuesta con el equipamiento**: el nombre por sí solo es único en el catálogo
- **Y** el mismo movimiento con distintos implementos es **un solo ejercicio** con varias opciones, no varios ejercicios

#### CA-39.03 — Gestión del equipamiento desde el Diccionario

- **Dado** que El Ejecutante está en el Diccionario de Ejercicios
- **Cuando** crea un ejercicio (`D5-T1`) o edita uno existente (`D2-T1`)
- **Entonces** el formulario permite seleccionar **varias** opciones de equipamiento
- **Y** exige **al menos una** para poder guardar
- **Y** el detalle del ejercicio (`D2-T1`) muestra **todas** sus opciones, no una sola
- **Y** el filtro por equipamiento del Diccionario (`D1-T1`) devuelve el ejercicio si **alguna** de sus opciones coincide con el filtro
- **Y** la capacidad aplica igual a los 37 ejercicios seed y a los creados por El Ejecutante

#### CA-39.04 — Selector de equipamiento al registrar la serie

- **Dado** que El Ejecutante registra una serie en la sesión activa (`E2-T1`)
- **Cuando** abre el formulario de registro
- **Entonces** ve un **selector de equipamiento** limitado a las opciones que el ejercicio admite
- **Y** el equipamiento elegido queda **persistido en la serie**, junto al peso, las repeticiones, el RIR y la unidad de captura
- **Y** el equipamiento es **obligatorio**: no se puede confirmar la serie sin él
- **Y** el selector se **preselecciona** con el equipamiento de la última serie registrada del mismo ejercicio, y si no hay ninguna, con la primera opción admitida
- **Y** si el ejercicio admite **una sola** opción, el selector se muestra resuelto y no exige interacción
- **Y** dos series del mismo ejercicio en la misma sesión **pueden** llevar equipamiento distinto
- **Y** la serie sigue siendo **inmutable** tras su creación: el equipamiento registrado no se corrige después

#### CA-39.05 — Peso corporal con carga externa

- **Dado** un ejercicio de peso corporal (hoy `Dominadas`, y cualquiera que El Ejecutante marque como tal)
- **Cuando** selecciona `Peso Corporal`
- **Entonces** el campo de peso y el selector de unidad permanecen **ocultos** y el peso se registra como **0**, como hasta ahora
- **Y cuando** selecciona `Barra Fija` — la dominada estricta con el propio peso —
- **Entonces** el comportamiento es el mismo: peso **0** y campo oculto
- **Y cuando** selecciona `Máquina` — la máquina asistida, donde la carga puesta es un **contrapeso que resta** esfuerzo en lugar de sumarlo —
- **Entonces** el peso también se registra como **0** y el campo permanece oculto: un contrapeso registrado como peso invertiría el significado del dato y contaminaría el tonelaje y la progresión
- **Pero cuando** selecciona `Peso Añadido` — la barra fija **con lastre** —
- **Entonces** el campo de peso y el selector `Kg` / `Lb` **se habilitan**
- **Y** el valor capturado representa **exclusivamente la carga externa**, no el peso corporal
- **Y** debe ser **estrictamente mayor que 0** para ese equipamiento
- **Y** la regla —solo `Peso Añadido` habilita la captura de carga— aplica a **todos** los ejercicios de peso corporal, no solo a `Dominadas`

#### CA-39.06 — Los 37 ejercicios y sus equipamientos admitidos

- **Dado** que la aplicación se instala por primera vez
- **Cuando** se siembra el catálogo de ejercicios
- **Entonces** cada uno de los 37 ejercicios declara exactamente estas opciones de equipamiento:

| # | Ejercicio | Equipamientos admitidos |
|---|---|---|
| 1 | Aductores | Máquina, Polea, Banda Elástica |
| 2 | Cruce de Polea Alta | Polea |
| 3 | Crunch Abdominal | Peso Corporal, Polea, Máquina |
| 4 | Curl Bayesian en Banco Inclinado | Polea, Mancuerna |
| 5 | Curl de Concentración | Mancuerna, Polea |
| 6 | Curl de Isquiotibiales Sentado | Máquina |
| 7 | Curl de Martillo Cruzado | Mancuerna, Polea |
| 8 | Curl de Predicador | Barra, Mancuerna, Máquina, Polea |
| 9 | Elevación de Pantorrilla de Pie | Máquina, Máquina Smith |
| 10 | Elevación Lateral | Mancuerna, Polea, Máquina |
| 11 | Extensión de Cuádriceps | Máquina |
| 12 | Extensión de Tríceps (Pushdown) | Polea |
| 13 | Extensión de Tríceps sobre Cabeza | Mancuerna, Barra, Polea |
| 14 | Face Pull | Polea, Banda Elástica |
| 15 | Hip Thrust | Barra, Máquina, Mancuerna, Máquina Smith |
| 16 | Peso Muerto Rumano | Barra, Mancuerna, Máquina Smith |
| 17 | Prensa Inclinada | Máquina |
| 18 | Press de Banca Inclinado | Barra, Mancuerna, Máquina, Máquina Smith |
| 19 | Press de Banca Plano | Barra, Mancuerna, Máquina, Máquina Smith |
| 20 | Press Pallof | Polea, Banda Elástica |
| 21 | Remo T Inclinado | Barra, Máquina |
| 22 | Sentadilla Búlgara | Peso Corporal, Mancuerna, Barra, Máquina Smith |
| 23 | Sentadilla Sumo | Mancuerna, Pesa Rusa, Barra, Polea |
| 24 | Sentadilla Hack | Máquina, Barra |
| 25 | Jalón al Pecho | Polea, Máquina |
| 26 | Vuelos Posteriores (Pájaros) | Mancuerna, Polea, Máquina |
| 27 | Remo al Mentón | Barra, Polea, Mancuerna |
| 28 | Aperturas | Mancuerna, Polea, Máquina |
| 29 | Pull-Over | Mancuerna, Polea, Barra, Máquina |
| 30 | Curl Martillo | Mancuerna, Polea |
| 31 | Rompecráneos | Barra, Mancuerna, Polea |
| 32 | Remo Horizontal | Barra, Mancuerna, Polea, Máquina |
| 33 | Zancadas (Lunges) | Peso Corporal, Mancuerna, Barra, Máquina Smith |
| 34 | Press Militar | Barra, Mancuerna, Máquina, Máquina Smith |
| 35 | Dominadas | Barra Fija, Máquina, Peso Añadido |
| 36 | Remo Unilateral Polea Baja | Polea |
| 37 | Remo Unilateral Polea Alta | Polea |

- **Y** ningún ejercicio se elimina del catálogo
- **Y** ningún ejercicio queda con lista vacía
- **Y** `Dominadas` conserva su marca de peso corporal, y su opción `Peso Añadido` es la única que habilita el registro de carga externa (CA-39.05)
- **Y** la **dificultad de progresión** de cada ejercicio **no se modifica**
- **Y** las **zonas musculares** de cada ejercicio **no se modifican** en esta historia — es alcance de `HU-41`

#### CA-39.07 — Renombrado de ocho ejercicios

- **Dado** el catálogo de ejercicios
- **Cuando** se compara con el nombre vigente
- **Entonces** ocho ejercicios pasan a llamarse:

| Nombre actual | Nombre nuevo |
|---|---|
| Elevación de Pantorrilla en Máquina de Pie | **Elevación de Pantorrilla de Pie** |
| Extensión de Tríceps en Polea (Pushdown) | **Extensión de Tríceps (Pushdown)** |
| Extensión de Tríceps por encima de la Cabeza | **Extensión de Tríceps sobre Cabeza** |
| Sentadilla de Zumo | **Sentadilla Sumo** |
| Vuelos Posteriores | **Vuelos Posteriores (Pájaros)** |
| Zancadas | **Zancadas (Lunges)** |
| Remo Unilateral en Polea Baja | **Remo Unilateral Polea Baja** |
| Remo Unilateral en Polea Alta | **Remo Unilateral Polea Alta** |

- **Y** los nombres pierden la referencia al implemento, porque el implemento ya no es parte de la identidad del ejercicio: *Elevación de Pantorrilla de Pie* se hace en `Máquina` **o** en `Máquina Smith`, y *Extensión de Tríceps (Pushdown)* no necesita decir «en Polea»
- **Y** los 29 ejercicios restantes **conservan su nombre**
- **Y** cada ejercicio renombrado **conserva su identificador, su recurso visual, su clasificación muscular y su historial**

#### CA-39.08 — El historial expone el equipamiento

- **Dado** que El Ejecutante consulta el detalle de una sesión pasada (`F2-T1`) o el historial de un ejercicio (`F3-T1`)
- **Cuando** revisa las series registradas
- **Entonces** cada serie muestra **con qué equipamiento** se ejecutó, junto al peso, las repeticiones, el RIR y la unidad de captura
- **Y** el historial del ejercicio permite **distinguir** las series por equipamiento, de modo que El Ejecutante pueda leer la evolución de cada implemento por separado
- **Y** una serie de polea **nunca** se presenta en la misma serie temporal de comparación de peso que una de mancuerna
- **Y** un ejercicio que admite varios implementos pero que se ha entrenado **con uno solo** muestra ese único implemento, sin selectores vacíos ni agrupaciones sin datos

---

### Escenario 2: Validaciones

#### CA-39.09 — Ejercicio sin equipamiento

- **Dado** el formulario de creación o edición de ejercicio
- **Cuando** El Ejecutante intenta guardar sin seleccionar ningún equipamiento
- **Entonces** el sistema **no persiste** el ejercicio
- **Y** muestra error en el campo de equipamiento
- **Y** el botón de guardar permanece **deshabilitado** mientras la selección esté vacía
- **Y** la misma validación impide **quitar la última** opción de un ejercicio que ya existe

#### CA-39.10 — Retirar un equipamiento con historial

- **Dado** un ejercicio con series registradas en un equipamiento concreto
- **Cuando** El Ejecutante intenta **quitar** ese equipamiento de las opciones del ejercicio
- **Entonces** el sistema **lo impide** y explica que existen series registradas con él
- **Y** la razón es la inmutabilidad de la serie: una serie ya escrita no puede quedar apuntando a un equipamiento que el ejercicio dejó de admitir
- **Y** quitar un equipamiento **sin** historial sí está permitido, siempre que quede al menos uno (CA-39.09)

---

### Escenario 3: Casos Extremos

#### CA-39.11 — Cambio de esquema por instalación fresca

- **Dado** que el esquema vigente es la versión 19 y que **ADR-019** resuelve el cambio de esquema durante la beta por instalación fresca
- **Cuando** se despliega esta historia
- **Entonces** **no** se entrega script de migración
- **Y** una base de datos anterior **no puede** abrir el build nuevo
- **Y** el reinicio lo ejecuta **El Ejecutante**, desinstalando y reinstalando la aplicación — la aplicación no lo hace por sí sola
- **Y** el historial de sesiones anterior **se pierde**, y esto queda declarado como consecuencia aceptada
- **Y** el criterio **lo heredan las cuatro historias hermanas**: cada una extiende el esquema y se resuelve del mismo modo

#### CA-39.12 — Respaldo y restauración

- **Dado** el mecanismo de respaldo y restauración (`J2-T1`, `J3-T1`)
- **Cuando** se exporta un respaldo tras esta historia
- **Entonces** el respaldo incluye el **equipamiento de cada serie** y las **opciones de equipamiento de cada ejercicio**
- **Y** los respaldos generados con el **formato anterior** quedan **incompatibles**: la restauración los rechaza con un mensaje explícito, en lugar de importarlos parcialmente
- **Y** un respaldo del formato nuevo restaura el estado completo, incluido el equipamiento de las series históricas

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante — actor único del sistema. Aplicación *single-user* sin roles.
- **Permisos requeridos:** ninguno. No existe modelo de permisos.
- **Valor de negocio:** el registro deja de ser ambiguo sobre el implemento y el catálogo deja de necesitar un ejercicio por combinación. Es también la infraestructura que permite, en las hermanas, que el motor de decisión compare pesos comparables.

### Reglas de Negocio

1. **El equipamiento es un dato de la serie, no del ejercicio.** El ejercicio declara qué implementos admite; la serie declara cuál se usó.
2. **Al menos un equipamiento por ejercicio.** El atributo es obligatorio y no admite lista vacía.
3. **Un tipo, un implemento.** El catálogo no admite valores compuestos ni disyunciones.
4. **Los agarres no son equipamiento.** Cuerda, barra en V y accesorios equivalentes son `Polea`.
5. **El nombre del ejercicio es único por sí solo.** El implemento ya no forma parte de su identidad.
6. **Sin carga externa no hay peso.** En ejercicios de peso corporal, solo `Peso Añadido` habilita la captura; `Peso Corporal`, `Barra Fija` y `Máquina` registran 0.
7. **Un contrapeso no es carga.** La máquina asistida no captura peso, porque el número significaría lo contrario que en el resto del sistema.
8. **Lo que ya tiene series no se toca.** No se retira un equipamiento con el que se registró una serie.

### Interfaz

| Punto | Ubicación | Naturaleza del cambio |
|---|---|---|
| Diccionario — filtro | `D1-T1` | El filtro por equipamiento coincide contra una lista |
| Diccionario — detalle | `D2-T1` | Muestra todas las opciones de equipamiento |
| Diccionario — crear | `D5-T1` | Selección múltiple de equipamiento |
| Registro de serie | `E2-T1` | Nuevo selector de equipamiento; peso y unidad condicionados en peso corporal |
| Historial de sesión | `F2-T1` | Cada serie muestra su equipamiento |
| Historial de ejercicio | `F3-T1` | Series distinguibles por equipamiento |
| Respaldo / Restauración | `J2-T1`, `J3-T1` | Formato ampliado; rechazo del formato anterior |

#### Detalle de Interfaz de Usuario

- **Diseño general:** ninguna pantalla nueva. Son modificaciones sobre pantallas existentes de los flujos D (Catálogo), E (Sesión Activa), F (Historial) y J (Ajustes y Respaldo).
- **Campos y controles:**
  - **Formulario de ejercicio (`D5-T1`, `D2-T1`) — equipamiento admitido:** **lista vertical de casillas de verificación** con los 15 tipos del catálogo, en el orden de CA-39.01, y un **contador de seleccionados** al pie (`3 de 15 seleccionados`). Se eligió sobre chips y sobre selector desplegable por ser inequívoca: la casilla no deja duda de qué está elegido y qué no, y el campo es obligatorio y multivalor. El coste asumido es que ocupa alto de pantalla.
  - **Formulario de serie (`E2-T1`) — equipamiento usado:** selector limitado a las opciones del ejercicio, preseleccionado con el último uso y resuelto sin interacción cuando el ejercicio admite una sola opción.
  - **Formulario de serie — peso:** el campo de peso y el selector `Kg`/`Lb` solo se habilitan, en ejercicios de peso corporal, cuando el equipamiento es `Peso Añadido`.
- **Flujo de navegación visual:** sin rutas nuevas. `Sesión activa → Registrar serie → (selector de equipamiento + peso + reps + RIR) → vuelta a la sesión`.
- **Mensajes y feedback:** error inline al intentar guardar un ejercicio sin equipamiento; mensaje que nombra la causa al impedir retirar un equipamiento con series registradas; mensaje explícito de incompatibilidad al rechazar un respaldo del formato anterior.

### Sistemas Externos

Ninguno. El sistema es local y sin integraciones. La persistencia es SQLite en el dispositivo y el respaldo es un archivo que El Ejecutante gestiona manualmente.

### Preview de Interfaz

**Preview:** [`39.preview.txt`](./39.preview.txt) | **Formato:** ASCII (wireframe de texto)

---

## Contexto y Referencias

**Arquitectura:**
- `docs/architecture/domain_and_state_model.md` — entidades `exercise`, `equipment_type`, `exercise_set`; datos semilla §6.1.
- `docs/architecture/interfaces_contract.md` — triggers `D1-T1`, `D2-T1`, `D5-T1`, `E2-T1`, `F2-T1`, `F3-T1`, `J2-T1`, `J3-T1`.
- **ADR-019** — excepción a RNF19: cambio de esquema resuelto por instalación fresca durante la beta.

**Historias hermanas:**
- [`HU-40`](../HU-40-progresion-por-equipamiento/historia.md) — progresión por par. **Depende de esta.** Orden 2, obligatorio.
- [`HU-41`](../HU-41-catalogo-anatomico-plan-alineado/historia.md) — catálogo anatómico y plan alineado. **Depende de esta.** Orden 3.
- [`HU-42`](../HU-42-1rm-estimado/historia.md) — 1RM estimado. **Depende de esta.** Orden 4, paralelizable.
- [`HU-43`](../HU-43-ajustar-sesion-del-dia/historia.md) — sesión editable. **Depende de esta y de `HU-41`.** Orden 5.

**Historias relacionadas:**
- `HU-03` — Diccionario de Ejercicios. Sus tres pantallas cambian.
- `HU-06` — registro de series. Punto de entrada del selector.
- `HU-17` — historial de ejercicios y sesiones. Debe exponer el equipamiento.
- `HU-19` — respaldo y restauración. Formato ampliado.
- `HU-29` — precedente de recategorización de catálogo con tabla cerrada.
- `HU-30` — captura en Kg/Lb y `capture_unit`.
- `HU-34` — dejó `session_exercise.exercise_id` como el ejercicio efectivamente ejecutado, lo que simplifica la resolución por par en `HU-40`.

**Lecciones aprendidas:**
- `HU-29` demostró que una recategorización conviene declararla como **tabla completa y cerrada** en el criterio de aceptación, no como delta narrado: evita ambigüedad en el sembrado.
- `HU-34` mostró el coste de mantener dos identidades para el mismo ejercicio dentro de la sesión. Esta historia lo evita: el equipamiento es un atributo de la serie, no una segunda identidad del ejercicio.

---

## Definición de Terminado (Inicial)

- [ ] Funcionalidad implementada según criterios de aceptación
- [ ] Validaciones funcionando correctamente
- [ ] Mensajes implementados
- [ ] Catálogo de equipamiento sembrado exactamente como declara CA-39.01 (15 tipos, 10 retirados)
- [ ] Los 37 ejercicios sembrados con las opciones de CA-39.06 y los nombres de CA-39.07
- [ ] Verificado que ningún ejercicio queda con lista de equipamiento vacía
- [ ] Verificado el comportamiento de peso en las cuatro opciones de `Dominadas` (CA-39.05)
- [ ] Documentación de arquitectura actualizada: modelo de dominio, contrato de interfaces y versión de esquema
- [ ] `story_mapping_index.md` actualizado con la historia, sus hermanas y sus dependencias
