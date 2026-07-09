# Historia de Usuario

**Como** ejecutante,
**Quiero** consultar el historial completo de cualquier ejercicio con su tendencia de carga y el historial de todas mis sesiones pasadas con sus datos completos,
**Para** evaluar mi progreso real en cada movimiento, revisar mi entrenamiento reciente, comparar sesiones y tener un registro completo, accesible y trazable de todo mi trabajo realizado.

## Descripción

Esta historia define la consulta del historial completo de ejercicios con tendencia de carga y el historial de sesiones pasadas. HU-17 es 100% de lectura: no modifica datos, no dispara reglas de negocio, no crea entidades. Se implementa como MVVM Read-Only con Room DAO Queries — 3 pantallas (F1, F2, F3), 3 ViewModels, 3 Use Cases.

---

## Criterios de Aceptación

### Bloque A — Historial y Tendencia de Carga por Ejercicio (RF50, RF51)

#### CA-17.01 — Historial completo de registros

**Dado que** el ejecutante selecciona un ejercicio para consultar su historial,
**cuando** el sistema presenta los datos,
**entonces** muestra el historial completo de todas las sesiones en las que se registró ese ejercicio, incluyendo para cada sesión: fecha, peso utilizado (Kg), repeticiones logradas (por serie o totales), RIR (por serie o promedio) y clasificación de progresión (Progresión positiva, Mantenimiento o Regresión).

#### CA-17.02 — Orden cronológico

**Dado que** el sistema presenta el historial de un ejercicio,
**cuando** ordena las entradas,
**entonces** las muestra en orden cronológico, de la sesión más reciente a la más antigua, permitiendo al ejecutante ver primero su estado actual.

#### CA-17.03 — Historial independiente del módulo-versión

**Dado que** un ejercicio puede aparecer en varias versiones del mismo módulo o puede haber sido ejecutado como sustitución,
**cuando** el sistema construye el historial,
**entonces** incluye todos los registros del ejercicio independientemente de la versión del módulo en que se haya ejecutado, consolidando toda la data del ejercicio en una sola vista.

#### CA-17.04 — Visualización de la tendencia de carga

**Dado que** el ejecutante consulta un ejercicio con múltiples sesiones de historial,
**cuando** visualiza la tendencia,
**entonces** el sistema muestra la evolución del peso utilizado a lo largo del tiempo, permitiendo identificar visualmente si la carga es ascendente, estable o descendente.

#### CA-17.05 — Ejercicios sin historial

**Dado que** el ejecutante consulta un ejercicio que nunca ha sido registrado,
**cuando** intenta ver el historial,
**entonces** el sistema indica que no hay registros disponibles para ese ejercicio.

#### CA-17.06 — Ejercicios de peso corporal: tendencia por repeticiones

**Dado que** el ejecutante consulta el historial de un ejercicio de peso corporal (Peso = 0),
**cuando** visualiza la tendencia,
**entonces** la tendencia se muestra por repeticiones totales en lugar de carga, ya que el peso no varía.

---

### Bloque B — Historial de Sesiones Pasadas (RF60)

#### CA-17.07 — Listado de sesiones pasadas

**Dado que** el ejecutante accede al historial de sesiones,
**cuando** el sistema presenta el listado,
**entonces** muestra todas las sesiones cerradas, cada una con: fecha, módulo (A, B o C), versión (V1, V2 o V3), estado (Completada o Incompleta) y tonelaje total, ordenadas de la más reciente a la más antigua.

#### CA-17.08 — Detalle de una sesión pasada

**Dado que** el ejecutante selecciona una sesión del historial,
**cuando** accede al detalle,
**entonces** el sistema muestra los ejercicios ejecutados en esa sesión con sus datos completos: para cada ejercicio, las series registradas con peso, repeticiones, RIR y la clasificación de progresión del ejercicio en esa sesión.

#### CA-17.09 — Sesiones incompletas en el historial

**Dado que** el ejecutante tiene sesiones cerradas como Incompletas,
**cuando** consulta el historial,
**entonces** las sesiones incompletas aparecen claramente marcadas como "Incompleta", mostrando solo los ejercicios y series que efectivamente fueron registrados, sin datos inventados para los ejercicios faltantes.

#### CA-17.10 — Sustituciones reflejadas en el historial

**Dado que** una sesión pasada incluyó sustituciones puntuales de ejercicios,
**cuando** el ejecutante consulta el detalle de esa sesión,
**entonces** el sistema muestra los ejercicios que realmente se ejecutaron (incluyendo los sustitutos), reflejando fielmente lo que ocurrió en la sesión.

#### CA-17.11 — Inmutabilidad de sesiones consultadas

**Dado que** el ejecutante consulta el detalle de una sesión pasada,
**cuando** visualiza los datos,
**entonces** toda la información es de solo lectura; no se ofrecen opciones para editar, eliminar o agregar datos a sesiones cerradas.

#### CA-17.12 — Historial vacío

**Dado que** el ejecutante no tiene sesiones cerradas registradas,
**cuando** accede al historial,
**entonces** el sistema indica que no hay sesiones pasadas disponibles.

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante (usuario principal de la app).
- **Permisos requeridos:** Ninguno — operación local sobre la base de datos del dispositivo.
- **Valor de negocio:** Permite al ejecutante evaluar su progreso real, comparar sesiones y tener un registro completo y trazable de todo su trabajo realizado.

### Reglas de Negocio

1. **Solo lectura:** HU-17 es 100% de lectura — no modifica datos, no dispara reglas de negocio, no crea entidades.
2. **Historial independiente del módulo-versión:** Los registros se consolidan por `exercise_id` sin filtro por `module_version_id`.
3. **Tendencia por tipo de ejercicio:** Estándar → Kg, bodyweight → repeticiones totales, isométrico → segundos.
4. **Sesiones incompletas:** Solo muestran ejercicios con series registradas (`HAVING setCount > 0`).
5. **Inmutabilidad:** Las sesiones consultadas son de solo lectura — no hay opciones de editar, eliminar o agregar datos.
6. **Paginación no necesaria:** Room + `LazyColumn` manejan ~300-600 sesiones sin paginación.

### Interfaz

- **F1 — `SessionHistoryScreen`:** Listado de sesiones cerradas con fecha, módulo-versión, estado y tonelaje. Bottom Navigation con Historial activo.
- **F2 — `SessionDetailScreen`:** Detalle de sesión pasada. Solo lectura. Muestra ejercicios con series (peso, reps, RIR, clasificación) y nota "Sustituyó a:" si aplica.
- **F3 — `ExerciseHistoryScreen`:** Historial de un ejercicio a lo largo de todas las sesiones. Incluye `TrendChartComposable` (gráfico Canvas lineal), estado de progresión actual, y lista de entradas cronológicas. Text Button "Ver técnica de ejecución →" que navega a D2.

### Sistemas Externos

Ninguno. Operación 100% local sobre SQLite (Room) en el dispositivo del ejecutante.

### Preview de Interfaz

Ver `Especificación Visual` para las pantallas F1, F2, F3. Los mockups definen colores, tipografía y layout de cada pantalla.

---

## Contexto y Referencias

**Arquitectura:** `docs/architecture/architecture_blueprint.md` | `docs/architecture/domain_and_state_model.md`

**Entidades consultadas:** `session`, `session_exercise`, `exercise_set`, `exercise_progression`, `module_version` (ver `docs/architecture/domain_and_state_model.md`)

**Interfaces de referencia:** `F1 — SessionHistoryScreen` | `F2 — SessionDetailScreen` | `F3 — ExerciseHistoryScreen` (ver `docs/architecture/interfaces_contract.md`)

**Requisitos cubiertos:** RF50, RF51, RF60

**Épica / Módulo:** `EPIC-04: Historial y Analítica`

**Prioridad:** Alta

**Historias relacionadas:**
- HU-06 (Registró series — `exercise_set` es la fuente primaria para F2 y F3)
- HU-07 (Registró sustituciones — `session_exercise.original_exercise_id` para nota "Sustituyó a:" en F2)
- HU-08 (Ejercicios bodyweight e isométricos — F3 discrimina tendencia por tipo de ejercicio)
- HU-09 (Cerró sesiones con estado COMPLETED/INCOMPLETE y tonelaje — F1 filtra por estos estados)
- HU-10 (Clasificó progresión — `session_exercise.progression_classification` se muestra en F2 y F3)
- HU-13 (Creó `getSessionSummaryInfo()`, `getExercisesForSummary()` — patrón más cercano a F2)
- HU-15 (Creó `TonnageChartComposable` con Canvas — técnica replicada en `TrendChartComposable`)
- HU-16 (Migración Pull/Push/Legs — datos históricos válidos post-migración)
- HU-18 (Alertas navegan a F3 desde H2)
- HU-19 (Backup incluirá historial visible)

**Historias originales consolidadas:**
- **HU-23 original** — Consultar historial y tendencia de carga de un ejercicio (RF50, RF51)
- **HU-24 original** — Consultar historial de sesiones pasadas (RF60)

---

## Definición de Terminado (Inicial)

- [x] Historial completo de registros por ejercicio con fecha, peso, reps, RIR, clasificación (CA-17.01)
- [x] Orden cronológico descendente por fecha (CA-17.02)
- [x] Historial independiente del módulo-versión (CA-17.03)
- [x] Tendencia de carga visual con gráfico Canvas (CA-17.04)
- [x] Ejercicios sin historial muestran estado Empty (CA-17.05)
- [x] Tendencia por repeticiones para bodyweight, segundos para isométrico (CA-17.06)
- [x] Listado de sesiones cerradas con módulo, versión, estado, tonelaje (CA-17.07)
- [x] Detalle de sesión con ejercicios, series y clasificación (CA-17.08)
- [x] Sesiones incompletas marcadas y con solo ejercicios con series (CA-17.09)
- [x] Sustituciones reflejadas con nota "Sustituyó a:" (CA-17.10)
- [x] Sesiones consultadas de solo lectura sin opciones de edición (CA-17.11)
- [x] Historial vacío indica "no hay sesiones pasadas disponibles" (CA-17.12)
- [x] 3 Use Cases con tests unitarios (delegación al Repository)
- [x] 3 ViewModels con tests unitarios (transformación de datos y estados)
- [x] `TrendChartComposable` reutilizable con Canvas lineal
- [x] Navegación F1→F2→F3 con Bottom Navigation condicional funcional
