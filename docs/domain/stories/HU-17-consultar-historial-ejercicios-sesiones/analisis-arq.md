## Análisis Arquitectónico

> HU-17 consolida HU-23 y HU-24 originales. Es 100% lectura: no modifica datos, no dispara reglas de negocio, no crea entidades.

**Patrón arquitectónico:** MVVM Read-Only con Room DAO Queries — 3 pantallas (F1, F2, F3), 3 ViewModels, 3 Use Cases.

### Componentes Afectados

**Componentes reutilizados (sin modificación):**

| # | Componente | Paquete | Uso |
|---|---|---|---|
| R1 | `ProgressionIndicator` | `ui.components` | F2 y F3 — clasificación ↑/=/↓ con color semántico (HU-13) |
| R2 | `TensionTopAppBar` | `ui.components` | F1, F2, F3 — barra superior con título y botón retorno |
| R3 | `ExerciseProgressionDao.getByExerciseId()` | `data.local.dao` | F3 — estado de progresión actual del ejercicio (`status`) |
| R4 | `SessionDao.getSessionSummaryInfo()` | `data.local.dao` | F2 — resumen de sesión (status, módulo, versión, tonelaje). Query de HU-13 |
| R5 | `ExerciseSetDao.getSetsForSessionExercise()` | `data.local.dao` | F2 — series de cada ejercicio (iterado por Repository) |
| R6 | `ExerciseDao.getByIdOnce()` | `data.local.dao` | F3 — nombre, `isBodyweight`, `isIsometric`. Existe desde HU-14 |

**Componentes NO tocados:**

- `domain/rules/` — ninguna regla nueva (clasificación ya existe en `session_exercise.progression_classification`)
- `data/local/entity/` — ninguna entidad se modifica (0 cambios de esquema, 0 migraciones)
- `data/local/database/TensionDatabase.kt` — versión se mantiene en 7
- `data/local/seed/` — no se toca ningún seeder
- `di/DatabaseModule.kt` / `di/RepositoryModule.kt` — no se modifican (no hay nuevos DAOs ni Repository)
- Flujos E, B, C, D, G — no se modifican sus pantallas

### Impacto en performance

- **F1:** Query con JOIN a `module_version` y subconsulta de tonelaje. Índices existentes `session.status`, `session.date`, `session_exercise.session_id` son suficientes para < 500 sesiones.
- **F2:** Query de ejercicios + iteración para series. Índices `session_exercise.session_id` y `exercise_set.session_exercise_id` cubren el caso (~10 ejercicios por sesión máximo).
- **F3:** Query cross-session filtrado por `exercise_id`. Índice existente `session_exercise.exercise_id`. Para un ejercicio con 100 sesiones, el query es O(n) con índice — rendimiento aceptable sin paginación.

### Notas Técnicas

**Nota 1 — Sustituciones en F2 (CA-17.10).**
`session_exercise` tiene `exercise_id` (ejercicio realmente ejecutado) y `original_exercise_id` (prescrito original, si hubo sustitución). En F2 se muestra el nombre del ejercicio ejecutado y si `original_exercise_id IS NOT NULL`, se agrega la nota "Sustituyó a: [nombre del ejercicio original]". Requiere LEFT JOIN adicional en query de F2.

**Nota 2 — Formato de fecha para display.**
Las sesiones almacenan `date` como `String` ISO (`yyyy-MM-dd`). Para F1 y F3, el ViewModel formatea a `"dd MMM yyyy"` (ej: "10 feb 2026") usando `LocalDate.parse()` + `DateTimeFormatter`. La locale es español (ADR-17). Este patrón ya se usa en `WeightHistoryScreen`.

**Nota 3 — Estado de progresión en F3 mapeado desde `exercise_progression.status`.**
Valores posibles: `NO_HISTORY` (⚪ Sin Historial), `IN_PROGRESSION` (🟢 En Progresión), `IN_PLATEAU` (🟡 En Meseta), `IN_DELOAD` (🔵 En Descarga), `MASTERED` (trata como `IN_PROGRESSION` visualmente).

**Nota 4 — Paginación no necesaria.**
Un ejecutante que entrena 3-6 sesiones por semana acumularía ~300-600 sesiones en 2 años. Room + `LazyColumn` manejan esta escala sin paginación.

**Nota 5 — F3 necesita `onNavigateToExerciseDetail`.**
El wireframe F3 incluye un Text Button "Ver técnica de ejecución →" que navega a D2 (Detalle de Ejercicio). El composable `ExerciseHistoryScreen` actual solo recibe `onNavigateBack`. Se debe agregar `onNavigateToExerciseDetail: (Long) -> Unit`.

**Nota 6 — Ejercicios isométricos en F3: `reps` representa segundos.**
Para ejercicios isométricos (`is_isometric = 1`), la columna `exercise_set.reps` almacena segundos sostenidos. El ViewModel de F3 discrimina por tipo: estándar → Kg, bodyweight → reps, isométrico → segundos (s).

---

### Decisiones Fundamentadas

**1. Los nuevos Use Cases son wrappers delegados al Repository, sin lógica adicional.**
No hay transformación de negocio: el UseCase invoca un solo método del Repository y retorna el resultado. Mantiene la convención ADR-05 (todas las pantallas usan UseCase → Repository) y permite agregar lógica de dominio futura sin cambiar el ViewModel.

**2. F2 reutiliza `getSessionSummaryInfo()` existente y agrega queries para detalle de ejercicios.**
El resumen de la sesión se obtiene reutilizando `SessionDao.getSessionSummaryInfo(sessionId)` ya creado por HU-13. La lista de ejercicios usa un nuevo query en `SessionExerciseDao` y las series usan `ExerciseSetDao.getSetsForSessionExercise(sessionExerciseId)` existente (iterado por ejercicio en el Repository).

**3. El historial de ejercicio (F3) se construye con un query que cruza `session_exercise` con `session`, no con `exercise_progression`.**
`exercise_progression` solo tiene el estado actual. Para el historial longitudinal (fecha, peso, reps, RIR, clasificación por cada sesión) se necesita `session_exercise` → `session` → `exercise_set`.

**4. La tendencia de carga se calcula en el ViewModel, no en el DAO.**
El gráfico muestra puntos (session_index, weightKg) o (session_index, totalReps) para bodyweight. Los datos ya se obtienen con el query de historial. El ViewModel transforma `ExerciseHistoryEntry` en puntos para el gráfico Canvas. Es una transformación de presentación, no de negocio.

**5. Bottom Navigation condicional en F3 requiere modificación de `showBottomBar` en `TensionNavHost`.**
La implementación actual del `showBottomBar` no maneja el caso `exercise-history` con origen `session-summary`. Cuando el ejecutante navega E5 → F3, el bottom bar no se oculta porque `exercise-history` no está en la lista de exclusión. Se agrega condición análoga a la de `exercise-detail` con origen `active-session`.

**6. La ruta `session-detail/{sessionId}` no existe aún en el código — se agrega.**
`NavigationRoutes` define `SESSION_HISTORY` pero NO define `SESSION_DETAIL`. La Arquitectura Técnica (§4.3) define la ruta como `session-detail/{sessionId}`. Se agrega la constante, la función factory y el composable en `TensionNavHost`.

**7. Se crea `TrendChartComposable` porque `TonnageChartComposable` no es reutilizable para F3.**
`TonnageChartComposable` pinta evolución de tonelaje con múltiples líneas y eje X = microciclos. F3 necesita una sola línea con eje X = sesiones y eje Y = Kg (o reps). Se crea `TrendChartComposable` como componente separado en `ui.components`.

**8. Las sesiones incompletas muestran solo los ejercicios con series registradas (CA-17.09).**
El query de F2 filtra con `HAVING setCount > 0` (mismo patrón que `getExercisesForSummary` existente). Los ejercicios sin series registradas no aparecen en el detalle.

**9. No se crea un `HistoryRepository` separado.**
Los datos provienen de `session`, `session_exercise` y `exercise_set` — tablas ya bajo la responsabilidad de `SessionRepository`. Agregar 3 métodos a la interface existente es menos intrusivo que crear un nuevo Repository con binding Hilt.

---

### Componentes Nuevos

| # | Componente | Tipo | Paquete | Responsabilidad |
|---|---|---|---|---|
| 1 | `SessionHistoryScreen` | Composable | `ui.history` | F1 — Listado de sesiones cerradas con fecha, módulo-versión, estado y tonelaje |
| 2 | `SessionHistoryViewModel` | HiltViewModel | `ui.history` | Estado reactivo para F1 |
| 3 | `SessionHistoryUiState` | data class | `ui.history` | Loading/Empty/Loaded con lista de `SessionHistoryItem` |
| 4 | `SessionDetailScreen` | Composable | `ui.history` | F2 — Detalle de sesión pasada. Solo lectura |
| 5 | `SessionDetailViewModel` | HiltViewModel | `ui.history` | Estado reactivo para F2 |
| 6 | `SessionDetailUiState` | data class | `ui.history` | Loading/Loaded con resumen + ejercicios con series |
| 7 | `ExerciseHistoryViewModel` | HiltViewModel | `ui.history` | Estado reactivo para F3 |
| 8 | `ExerciseHistoryUiState` | data class | `ui.history` | Loading/Empty/Loaded con estado progresión + tendencia + lista |
| 9 | `GetSessionHistoryUseCase` | UseCase | `domain.usecase.history` | Obtiene lista de sesiones cerradas ordenadas cronológicamente |
| 10 | `GetSessionDetailUseCase` | UseCase | `domain.usecase.history` | Obtiene detalle completo de una sesión |
| 11 | `GetExerciseHistoryUseCase` | UseCase | `domain.usecase.history` | Obtiene historial de un ejercicio a lo largo de todas las sesiones |
| 12 | `SessionHistoryItem` | domain model | `domain.model` | sessionId, date, moduleCode, versionNumber, status, totalTonnageKg |
| 13 | `SessionDetail` | domain model | `domain.model` | Resumen de sesión + lista de ejercicios con series |
| 14 | `SessionDetailExercise` | domain model | `domain.model` | nombre, clasificación, series, nota sustitución |
| 15 | `ExerciseHistoryEntry` | domain model | `domain.model` | date, moduleCode, versionNumber, avgWeightKg, totalReps, avgRir, classification |
| 16 | `ExerciseHistoryData` | domain model | `domain.model` | exerciseName, progressionStatus, isBodyweight, isIsometric, entries |
| 17 | `TrendChartComposable` | Composable | `ui.components` | Gráfico lineal Canvas reutilizable (línea única, eje X = sesiones, eje Y = Kg/reps/s) |

### Componentes Modificados

| # | Componente | Modificación | Nivel |
|---|---|---|---|
| 1 | `SessionDao` | Agregar DTO `ClosedSessionDto` + query `getClosedSessionsWithSummary()` para F1 | Menor |
| 2 | `SessionExerciseDao` | Agregar DTOs `SessionDetailExerciseDto` + `ExerciseHistoryEntryDto` + queries `getExercisesForSessionDetail()` + `getExerciseHistoryEntries()` | Medio |
| 3 | `SessionRepository` (interface) | Agregar 3 métodos: `getSessionHistory()`, `getSessionDetail()`, `getExerciseHistory()` | Menor |
| 4 | `SessionRepositoryImpl` | Implementar los 3 nuevos métodos mapeando DTOs → domain models | Medio |
| 5 | `ExerciseHistoryScreen` | Refactorizar de placeholder a implementación completa de F3 | Mayor |
| 6 | `NavigationRoutes` | Agregar `SESSION_DETAIL` + función factory `sessionDetailRoute()` | Menor |
| 7 | `TensionNavHost` | Reemplazar placeholder SESSION_HISTORY, agregar SESSION_DETAIL, actualizar EXERCISE_HISTORY, agregar condición `showBottomBar` E5→F3 | Mayor |
| 8 | `BottomNavigationBar` | Mover `exercise-history` al tab Historial, agregar `session-detail` como childRoutePrefix del tab Historial | Medio |
| 9 | `strings.xml` | Agregar ~20 strings para las 3 pantallas | Menor |

### Hitos de Implementación

| # | Componente(s) | Descripción | Dependencias |
|---|---|---|---|
| 1 | Domain Models | Crear `SessionHistoryItem`, `SessionDetail`, `SessionDetailExercise`, `ExerciseHistoryEntry`, `ExerciseHistoryData` | Ninguna |
| 2 | DAO Queries | Agregar queries en `SessionDao` (F1) y `SessionExerciseDao` (F2, F3) | Ninguna |
| 3 | Repository | Agregar 3 métodos a `SessionRepository` e implementarlos en `SessionRepositoryImpl` | Hitos 1, 2 |
| 4 | Use Cases | Crear 3 Use Cases delegados | Hito 3 |
| 5 | F1 — Historial de Sesiones | Crear Screen + ViewModel + UiState | Hito 4 |
| 6 | F2 — Detalle de Sesión Pasada | Crear Screen + ViewModel + UiState + ruta SESSION_DETAIL | Hito 4 |
| 7 | TrendChartComposable | Gráfico Canvas lineal reutilizable | Ninguna |
| 8 | F3 — Historial de Ejercicio | Refactorizar ExerciseHistoryScreen + ViewModel + UiState | Hitos 4, 7 |
| 9 | Navegación y Strings | NavigationRoutes, TensionNavHost, BottomNavigationBar, strings.xml | Hitos 5, 6, 8 |
| 10 | Tests unitarios | 3 Use Cases + 3 ViewModels | Hitos 4, 5, 6, 8 |

### Verificación Cruzada de CAs

| CA | Mecanismo | Componente |
|---|---|---|
| CA-17.01 | Query DAO con fecha, peso, reps, RIR, clasificación por sesión | `SessionExerciseDao.getExerciseHistoryEntries()` + `ExerciseHistoryScreen` |
| CA-17.02 | `ORDER BY s.date DESC, s.id DESC` en query F3 | DAO query |
| CA-17.03 | Query JOIN sin filtro por `module_version_id` — solo por `exercise_id` | DAO query |
| CA-17.04 | `TrendChartComposable` con puntos extraídos del historial | `ExerciseHistoryScreen` + `TrendChartComposable` |
| CA-17.05 | `ExerciseHistoryUiState.Empty` | `ExerciseHistoryScreen` empty state |
| CA-17.06 | Condicional en ViewModel: `isBodyweight` → reps, `isIsometric` → segundos, estándar → Kg | `ExerciseHistoryViewModel` |
| CA-17.07 | Query con `status IN ('COMPLETED', 'INCOMPLETE')` + JOIN `module_version` | `SessionDao.getClosedSessionsWithSummary()` + `SessionHistoryScreen` |
| CA-17.08 | Query ejercicios con series (peso, reps, RIR, clasificación) | `SessionDetailScreen` + DAO queries |
| CA-17.09 | `HAVING setCount > 0` en query F2 | DAO query |
| CA-17.10 | `LEFT JOIN exercise oe ON se.original_exercise_id = oe.id` → nota "Sustituyó a:" | `SessionDetailScreen` |
| CA-17.11 | Pantalla sin botones de edición/eliminación | `SessionDetailScreen` |
| CA-17.12 | `SessionHistoryUiState.Empty` | `SessionHistoryScreen` empty state |
