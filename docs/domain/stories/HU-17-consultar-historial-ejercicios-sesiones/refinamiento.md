## Refinamiento Técnico (Developer)
**Autor**: Esteban Colorado González | **Fecha**: 2026-02-12

---

### Consideraciones Generales

**Basado en análisis arquitectónico:**
Análisis Arquitectónico de HU-17 con 10 hitos, 17 componentes nuevos, 9 modificados, sin lógica de negocio nueva (100% lectura). Patrón MVVM Read-Only con Room DAO Queries.

**Nivel de complejidad:**
MEDIA — 17 componentes nuevos en 4 capas (domain models, use cases, ViewModels, UI) + 9 modificados, pero sin lógica de negocio nueva (100% lectura de datos ya persistidos). La complejidad real reside en: (1) queries SQL con JOINs múltiples y subconsultas para F1 y F3, (2) el gráfico Canvas `TrendChartComposable` con lógica de escalado, (3) la orquestación de navegación F1→F2→F3 con Bottom Navigation condicional, (4) la correcta interpretación del tipo de ejercicio (estándar/bodyweight/isométrico) para la tendencia de F3.

**Riesgos técnicos conocidos:**
1. **Performance de queries F3 con historial extenso:** El query `getExerciseHistoryEntries()` hace un cross-session JOIN por `exercise_id` con subconsultas de peso promedio, reps totales y RIR promedio. Los índices `session_exercise.exercise_id` y `session.status` ya existen. Rendimiento aceptable para < 500 sesiones sin paginación.
2. **`BottomNavigationBar` — `exercise-history` en dos tabs:** Mover `exercise-history` al tab Historial significa que cuando se navega D2→F3, el tab activo será Historial — aceptable porque F3 ES una pantalla de historial funcionalmente (Arquitectura Técnica §4.4 `history-graph`).
3. **`showBottomBar` — omisión del caso E5→F3:** Se agrega condición análoga a la de `exercise-detail` con origen `active-session`.

**Patrones y convenciones del equipo:**
- ViewModels: `@HiltViewModel` + `SavedStateHandle` para args de navegación, `MutableStateFlow<UiState>` + `asStateFlow()`, carga en `init {}` via `viewModelScope.launch`
- UiState: `sealed interface` con variantes `Loading`, `Empty`, `Loaded`
- Use Cases: `class XxxUseCase @Inject constructor(private val repo)` con `suspend operator fun invoke()` delegando al Repository
- Use Cases en paquete: `domain.usecase.{feature}/` (aquí `history/`)
- DAO DTOs: `data class` en el mismo archivo del DAO — usados como retorno de `@Query`
- Screens: `val uiState by viewModel.uiState.collectAsStateWithLifecycle()`
- Formato fecha: `LocalDate.parse()` + `DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es"))` — patrón de `WeightHistoryScreen`

**Dependencias nuevas a instalar:** Ninguna.

**Estrategia de testing:** JUnit 4 + MockK + Kotlin Coroutines Test | 3 Use Cases (delegación) + 3 ViewModels (transformación de datos y manejo de estados) | Sin builders — los domain models son `data class` con constructores explícitos.

---

### Historias Relacionadas Consultadas

**Implementaciones similares analizadas:**
- **HU-13:** Creó `SessionSummaryScreen`, `ProgressionIndicator`, `getSessionSummaryInfo()`, `getExercisesForSummary()` — patrón más cercano a F2. Se reutilizan directamente.
- **HU-15:** Creó `TonnageChartComposable` con Canvas multilínea — técnica de Canvas Compose replicada en `TrendChartComposable` (línea única).
- **HU-02:** `WeightHistoryScreen` — patrón de formato de fecha `dd MMM yyyy` con locale español reutilizado en F1 y F3.

**Patrones de código reutilizados:**
- `getSessionSummaryInfo()` existente de HU-13 para F2
- `getSetsForSessionExercise()` existente de HU-06 para F2
- `ProgressionIndicator` existente de HU-13 para F2 y F3
- Patrón de fecha de `WeightHistoryScreen` para F1 y F3

**Mejores prácticas aplicadas:**
- Use Cases como wrappers delegados al Repository sin lógica adicional
- HAVING setCount > 0 para filtrar ejercicios sin series registradas (CA-17.09)
- LEFT JOIN para sustituciones con nota "Sustituyó a:" (CA-17.10)
- sealed interface para UiState con Loading/Empty/Loaded
- Tendencia calculada en ViewModel (transformación de presentación, no de negocio)

---

### Tareas de Implementación

#### Fase 1: Domain Models

> Basado en Hito #1 del Análisis Arquitectónico

- [ ] **Crear `SessionHistoryItem`** (AC: 17.07)
  - [ ] Data class: `sessionId`, `date`, `moduleCode`, `versionNumber`, `status`, `totalTonnageKg` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/model/SessionHistoryItem.kt`

- [ ] **Crear `SessionDetail`** (AC: 17.08)
  - [ ] Data class: resumen de sesión + lista de `SessionDetailExercise` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/model/SessionDetail.kt`

- [ ] **Crear `SessionDetailExercise`** (AC: 17.08, 17.10)
  - [ ] Data class: nombre, clasificación, series, nota sustitución — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/model/SessionDetailExercise.kt`

- [ ] **Crear `ExerciseHistoryEntry`** (AC: 17.01, 17.04, 17.06)
  - [ ] Data class: date, moduleCode, versionNumber, avgWeightKg, totalReps, avgRir, classification — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/model/ExerciseHistoryEntry.kt`

- [ ] **Crear `ExerciseHistoryData`** (AC: 17.04, 17.06)
  - [ ] Data class: exerciseName, progressionStatus, isBodyweight, isIsometric, entries — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/model/ExerciseHistoryData.kt`

#### Fase 2: DAO Queries

> Basado en Hito #2 del Análisis Arquitectónico

##### SessionDao

- [ ] **Crear DTO `ClosedSessionDto`** (AC: 17.07)
  - [ ] Data class para query de sesiones cerradas con resumen — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/dao/SessionDao.kt`

- [ ] **Crear query `getClosedSessionsWithSummary()`** (AC: 17.07)
  - [ ] Query con `status IN ('COMPLETED', 'INCOMPLETE')` + JOIN `module_version` + subconsulta de tonelaje — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/dao/SessionDao.kt`

##### SessionExerciseDao

- [ ] **Crear DTO `SessionDetailExerciseDto`** (AC: 17.08, 17.10)
  - [ ] Data class para query de ejercicios en detalle de sesión — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/dao/SessionExerciseDao.kt`

- [ ] **Crear DTO `ExerciseHistoryEntryDto`** (AC: 17.01, 17.04)
  - [ ] Data class para query de historial de ejercicio cross-session — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/dao/SessionExerciseDao.kt`

- [ ] **Crear query `getExercisesForSessionDetail()`** (AC: 17.08, 17.09, 17.10)
  - [ ] Query con LEFT JOIN para sustituciones + HAVING setCount > 0 — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/dao/SessionExerciseDao.kt`

- [ ] **Crear query `getExerciseHistoryEntries()`** (AC: 17.01, 17.02, 17.03, 17.04)
  - [ ] Query JOIN `session_exercise` → `session` → `exercise_set` con subconsultas de avgWeightKg, totalReps, avgRir, ORDER BY date DESC — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/dao/SessionExerciseDao.kt`

#### Fase 3: Repository

> Basado en Hito #3 del Análisis Arquitectónico

##### SessionRepository (interface)

- [ ] **Agregar 3 métodos** (AC: 17.07, 17.08, 17.01)
  - [ ] `getSessionHistory()`, `getSessionDetail()`, `getExerciseHistory()` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/repository/SessionRepository.kt`

##### SessionRepositoryImpl

- [ ] **Implementar los 3 nuevos métodos** (AC: 17.07, 17.08, 17.01)
  - [ ] Mapear DTOs → domain models — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/repository/SessionRepositoryImpl.kt`

#### Fase 4: Use Cases

> Basado en Hito #4 del Análisis Arquitectónico

- [ ] **Crear `GetSessionHistoryUseCase`** (AC: 17.07, 17.12)
  - [ ] Wrapper delegado a `SessionRepository.getSessionHistory()` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/usecase/history/GetSessionHistoryUseCase.kt`
  - [ ] Test unitario: delegación al Repository — Archivo: `app/src/test/java/com/estebancoloradogonzalez/tension/domain/usecase/history/GetSessionHistoryUseCaseTest.kt`

- [ ] **Crear `GetSessionDetailUseCase`** (AC: 17.08, 17.09, 17.10)
  - [ ] Wrapper delegado a `SessionRepository.getSessionDetail()` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/usecase/history/GetSessionDetailUseCase.kt`
  - [ ] Test unitario: delegación al Repository — Archivo: `app/src/test/java/com/estebancoloradogonzalez/tension/domain/usecase/history/GetSessionDetailUseCaseTest.kt`

- [ ] **Crear `GetExerciseHistoryUseCase`** (AC: 17.01, 17.04, 17.05, 17.06)
  - [ ] Wrapper delegado a `SessionRepository.getExerciseHistory()` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/usecase/history/GetExerciseHistoryUseCase.kt`
  - [ ] Test unitario: delegación al Repository — Archivo: `app/src/test/java/com/estebancoloradogonzalez/tension/domain/usecase/history/GetExerciseHistoryUseCaseTest.kt`

#### Fase 5: F1 — Historial de Sesiones

> Basado en Hito #5 del Análisis Arquitectónico

- [ ] **Crear `SessionHistoryUiState`** (AC: 17.07, 17.12)
  - [ ] Sealed interface Loading/Empty/Loaded con lista de `SessionHistoryItem` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/history/SessionHistoryUiState.kt`

- [ ] **Crear `SessionHistoryViewModel`** (AC: 17.07, 17.12)
  - [ ] `@HiltViewModel`. Carga historial en `init {}`. Formatea fechas a `"dd MMM yyyy"` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/history/SessionHistoryViewModel.kt`

- [ ] **Crear `SessionHistoryScreen`** (AC: 17.07, 17.12)
  - [ ] Composable con lista de sesiones (fecha, módulo-versión, estado, tonelaje). Empty state para CA-17.12 — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/history/SessionHistoryScreen.kt`

#### Fase 6: F2 — Detalle de Sesión Pasada

> Basado en Hito #6 del Análisis Arquitectónico

- [ ] **Crear `SessionDetailUiState`** (AC: 17.08, 17.09, 17.10)
  - [ ] Sealed interface Loading/Loaded con resumen + ejercicios con series — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/history/SessionDetailUiState.kt`

- [ ] **Crear `SessionDetailViewModel`** (AC: 17.08, 17.09, 17.10)
  - [ ] `@HiltViewModel`. Carga detalle con `SavedStateHandle` para sessionId — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/history/SessionDetailViewModel.kt`

- [ ] **Crear `SessionDetailScreen`** (AC: 17.08, 17.09, 17.10, 17.11)
  - [ ] Composable solo lectura con ejercicios, series, clasificación y nota "Sustituyó a:" — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/history/SessionDetailScreen.kt`

- [ ] **Agregar ruta `SESSION_DETAIL` en `NavigationRoutes`** (AC: 17.08)
  - [ ] Constante `session-detail/{sessionId}` + función factory — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/navigation/NavigationRoutes.kt`

- [ ] **Agregar composable SESSION_DETAIL en `TensionNavHost`** (AC: 17.08)
  - [ ] Ruta `session-detail/{sessionId}` con `SessionDetailScreen` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/navigation/TensionNavHost.kt`

#### Fase 7: TrendChartComposable

> Basado en Hito #7 del Análisis Arquitectónico

- [ ] **Crear `TrendChartComposable`** (AC: 17.04, 17.06)
  - [ ] Gráfico Canvas lineal reutilizable: línea única, eje X = sesiones, eje Y = Kg/reps/s — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/components/TrendChartComposable.kt`

#### Fase 8: F3 — Historial de Ejercicio

> Basado en Hito #8 del Análisis Arquitectónico

- [ ] **Crear `ExerciseHistoryUiState`** (AC: 17.01, 17.04, 17.05, 17.06)
  - [ ] Sealed interface Loading/Empty/Loaded con estado progresión + tendencia + lista — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/history/ExerciseHistoryUiState.kt`

- [ ] **Crear `ExerciseHistoryViewModel`** (AC: 17.01, 17.04, 17.05, 17.06)
  - [ ] `@HiltViewModel`. Carga historial con `SavedStateHandle` para exerciseId. Transforma entries en puntos para gráfico. Discrimina tipo de ejercicio para tendencia — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/history/ExerciseHistoryViewModel.kt`

- [ ] **Refactorizar `ExerciseHistoryScreen`** (AC: 17.01, 17.04, 17.05, 17.06)
  - [ ] Implementación completa de F3 con `TrendChartComposable`, estado progresión, lista cronológica, botón "Ver técnica de ejecución →" — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/history/ExerciseHistoryScreen.kt`

#### Fase 9: Navegación y Strings

> Basado en Hito #9 del Análisis Arquitectónico

- [ ] **Actualizar `TensionNavHost`** (AC: 17.07, 17.08, 17.01)
  - [ ] Reemplazar placeholder SESSION_HISTORY, agregar SESSION_DETAIL, actualizar EXERCISE_HISTORY, agregar condición `showBottomBar` E5→F3 — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/navigation/TensionNavHost.kt`

- [ ] **Actualizar `BottomNavigationBar`** (AC: 17.07, 17.01)
  - [ ] Mover `exercise-history` al tab Historial, agregar `session-detail` como childRoutePrefix — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/components/BottomNavigationBar.kt`

- [ ] **Actualizar `strings.xml`** (AC: 17.01-17.12)
  - [ ] Agregar ~20 strings para las 3 pantallas — Archivo: `app/src/main/res/values/strings.xml`

#### Fase 10: Tests Unitarios

> Basado en Hito #10 del Análisis Arquitectónico

- [ ] **Tests Use Cases** (3 Use Cases, delegación)
  - [ ] `GetSessionHistoryUseCaseTest` — Archivo: `app/src/test/java/com/estebancoloradogonzalez/tension/domain/usecase/history/GetSessionHistoryUseCaseTest.kt`
  - [ ] `GetSessionDetailUseCaseTest` — Archivo: `app/src/test/java/com/estebancoloradogonzalez/tension/domain/usecase/history/GetSessionDetailUseCaseTest.kt`
  - [ ] `GetExerciseHistoryUseCaseTest` — Archivo: `app/src/test/java/com/estebancoloradogonzalez/tension/domain/usecase/history/GetExerciseHistoryUseCaseTest.kt`

- [ ] **Tests ViewModels** (3 ViewModels, transformación de datos)
  - [ ] `SessionHistoryViewModelTest` — Archivo: `app/src/test/java/com/estebancoloradogonzalez/tension/ui/history/SessionHistoryViewModelTest.kt`
  - [ ] `SessionDetailViewModelTest` — Archivo: `app/src/test/java/com/estebancoloradogonzalez/tension/ui/history/SessionDetailViewModelTest.kt`
  - [ ] `ExerciseHistoryViewModelTest` — Archivo: `app/src/test/java/com/estebancoloradogonzalez/tension/ui/history/ExerciseHistoryViewModelTest.kt`

---

### Vinculación CA → Fase de implementación:
- CA-17.01, 17.02, 17.03, 17.04 → Fase 8 (F3 Screen) + Fase 2 (DAO query)
- CA-17.05 → Fase 8 (Empty state)
- CA-17.06 → Fase 8 (ViewModel discriminación tipo ejercicio)
- CA-17.07, 17.12 → Fase 5 (F1 Screen) + Fase 2 (DAO query)
- CA-17.08, 17.09, 17.10, 17.11 → Fase 6 (F2 Screen) + Fase 2 (DAO queries)
- CA-17.09 → Fase 2 (HAVING setCount > 0)
- CA-17.10 → Fase 2 (LEFT JOIN sustituciones)
