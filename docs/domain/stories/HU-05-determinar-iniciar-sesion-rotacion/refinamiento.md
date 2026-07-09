# Refinamiento Técnico — Historia #5

<!-- SECCIÓN AGREGADA POR: Workflow refinamiento-tecnico -->
<!-- ETAPA: Refinamiento Técnico -->
<!-- RESPONSABLE: Developer -->
<!-- BASE: Análisis Arquitectónico (Arquitecto) - Ver archivo analisis-arq.md -->
<!-- FECHA: 2026-02-13 -->
<!-- ESTADO: Listo para Desarrollo -->
<!-- AUDITORÍA: Completada 2026-02-13 — Auditoría profunda: cruce exhaustivo contra Modelo de Datos §3.10-§3.14, Especificación Visual §8 B1/E1, Wireframes B1/E1, Arquitectura Técnica §4.3/§4.5/§4.5.1/§4.7/§5.1-§5.4, ADR-05/ADR-11/ADR-18, Mapa de Navegación §B1/E1, Requerimientos RF09/RF10/RF11/RF12/RNF06/RNF10/RNF13, Manifiesto de Dominio Sistémico §5.A.1/§5.C/§6.B.9, 32 HUs completas, y código HU-01/HU-02/HU-03/HU-04 implementado (9/9 claims de código verificados). 9 hallazgos corregidos por el arquitecto (A1-A9), 17 verificaciones positivas (V1-V17), 1 inconsistencia externa documentada. Auditoría post-refinamiento: 5 hallazgos. Correcciones aplicadas: (B-3) botón "Registrar" en estado "En Ejecución" corregido de FilledTonalButton/Primary Container a Filled Button/Primary per Especificación Visual §8 E1; (D-1) showBottomBar extendido con evaluación de back stack para ocultar Bottom Nav en D2 cuando origen es E1; (B-4) colores de estado de ejercicio ampliados con variantes light/dark. -->

## Consideraciones Generales

**Basado en análisis arquitectónico:**
HU-05 con 11 hitos, 12+ componentes nuevos, 6 riesgos identificados y 9 hallazgos corregidos. Quinta historia — más compleja hasta ahora. Infraestructura base completa gracias a HU-01 a HU-04 (10 entities, 9 DAOs, 3 repositories).

**Nivel de complejidad:**
ALTA — 4 entidades Room nuevas, 4 DAOs nuevos + 2 modificaciones, 5 modelos de dominio nuevos + 1 enum, 1 repositorio nuevo con lógica de negocio compleja (rotación + transacción atómica), 5 Use Cases, 2 pantallas funcionales (B1 reemplazo y E1 parcial), 2 ViewModels, navegación con argumentos tipados, ocultamiento de Bottom Nav para rutas de sesión, Custom Top Bar en B1 y E1, crash recovery con card condicional, y queries JOIN multi-tabla con LEFT JOIN para carga objetivo.

**Riesgos técnicos conocidos:**
1. Transacción `startSession` puede fallar parcialmente — usar `database.withTransaction {}`.
2. Double-tap en "Iniciar Sesión" — validación en `startSession()` + loading state en la UI.
3. Sesión huérfana (creada pero app se cierra antes de navegar a E1) — detectada y reanudable via crash recovery de B1.
4. La query JOIN de `SessionExerciseWithDetails` es compleja (7 tablas) — verificar con datos reales que GROUP_CONCAT y COUNT retornan valores correctos.
5. `deload_id` en `SessionEntity` sin FK anotada a tabla inexistente — aceptable, la FK declarativa se agrega en HU-17.

**Patrones y convenciones del equipo (establecidos en HU-01—HU-04):**
- Código fuente en inglés, UI y datos de dominio en español (Arquitectura Técnica §5.1)
- Naming: `{Feature}Screen`, `{Feature}ViewModel`, `{Acción}{Entidad}UseCase`, `{Entidad}Entity`, `{Entidad}Dao` (§5.2)
- Estructura Composable: `hiltViewModel()` + `collectAsStateWithLifecycle()` + `LaunchedEffect` para eventos oneshot (§5.3)
- Estructura ViewModel: `_uiState MutableStateFlow` / `uiState StateFlow` + `MutableSharedFlow` para eventos de navegación (§5.4)
- `operator fun invoke()` en Use Cases
- Callbacks en Composables con prefijo `on` (`onNavigateToActiveSession`)
- `childRoutes` y `childRoutePrefixes` en `BottomNavItem` para rutas hijas (patrón HU-02/HU-03)

**Dependencias nuevas a instalar:** Ninguna.

**Estrategia de testing:**
JUnit 4 (ADR-18) + MockK + kotlinx-coroutines-test | Tests unitarios para los 5 Use Cases | Cobertura: 100% Use Cases

---

## Historias Relacionadas Consultadas

**Patrones de código reutilizados:**
- `database.withTransaction { }` de `ProfileRepositoryImpl` → reutilizado en `SessionRepositoryImpl.startSession()`
- Query JOIN con GROUP_CONCAT de `ExerciseDao` → reutilizado/extendido en `SessionExerciseDao.getBySessionIdWithDetails()`
- `SavedStateHandle` de `ExerciseDetailViewModel` y `PlanVersionDetailViewModel` → reutilizado en `ActiveSessionViewModel`
- `MutableSharedFlow<T>(replay = 0)` para eventos oneshot de navegación → patrón nuevo en HU-05, reutilizable en HU-06+

**HUs futuras que dependen de artefactos de HU-05:**
- HU-06: INSERT en `exercise_set`, CREATE/UPDATE en `exercise_progression` (tablas creadas en HU-05)
- HU-07: UPDATE en `session_exercise.exercise_id` + SET `original_exercise_id`
- HU-09: UPDATE `session.status`, avance de rotación con `RotationStateDao.update()` (expuesto en HU-05)
- HU-10: UPDATE `session_exercise.progression_classification`
- HU-11: UPDATE `exercise_progression.prescribed_load_kg` (leído por HU-05)
- HU-17: Utiliza `session.deload_id` (columna creada nullable en HU-05)
- HU-18: Microciclos en B1 → lectura de `rotation_state.microcycle_count` ya expuesta en B1

---

## Código existente verificado (HU-01 + HU-02 + HU-03 + HU-04 implementados)

| Componente | Archivo | Estado |
| --- | --- | --- |
| `TensionDatabase` | `data/local/database/TensionDatabase.kt` | Existe — versión 3, 10 entities, 9 DAOs. Se modifica: +4 entities, +4 DAOs, versión → 4 |
| `DatabaseModule` | `di/DatabaseModule.kt` | Existe — provee DB + 9 DAOs. Se modifica: +4 DAOs |
| `RepositoryModule` | `di/RepositoryModule.kt` | Existe — binds 3 repos. Se modifica: +SessionRepository |
| `RotationStateEntity` | `data/local/entity/RotationStateEntity.kt` | Existe — PK id=1, defaults: position=1, versionA=1, versionB=1, versionC=1, count=0. No se modifica |
| `RotationStateDao` | `data/local/dao/RotationStateDao.kt` | Existe — `insert()`, `getRotationState()`. Se modifica: +`update()` |
| `ModuleVersionDao` | `data/local/dao/ModuleVersionDao.kt` | Existe — `getAll()`, `getAllWithExerciseCount()`, `getById()`, `insertAll()`. Se modifica: +`getByModuleCodeAndVersion()` |
| `PlanAssignmentDao` | `data/local/dao/PlanAssignmentDao.kt` | Existe — `getByModuleVersionId()`, etc. No se modifica |
| `NavigationRoutes` | `ui/navigation/NavigationRoutes.kt` | Existe — 13 constantes + helpers. Se modifica: +ACTIVE_SESSION + helper |
| `TensionNavHost` | `ui/navigation/TensionNavHost.kt` | Existe — 13 composables. Se modifica: actualizar HOME, agregar ACTIVE_SESSION, extender showBottomBar |
| `HomeScreen` | `ui/home/HomeScreen.kt` | Existe — stub con logo + bienvenida. Se REEMPLAZA completamente |
| `TensionTopAppBar` | `ui/components/TensionTopAppBar.kt` | Existe — **No se usa en B1 ni E1** (ambos tienen Top Bar custom inline) |
| `Converters` | `data/local/database/Converters.kt` | Existe — `LocalDate ↔ String`. Reutilizable para `session.date` |

---

## Tareas de Implementación

### Fase 1: Data Layer — Entities (4 nuevas)

> Basado en Hito #1 del Análisis Arquitectónico

- [x] **Crear SessionEntity** (AC: 05.06)
  - [x] `@Entity(tableName = "session")`. PK autoincrement `id: Long`. FK: `moduleVersionId` → `module_version(id)` ON DELETE RESTRICT. Columnas: `moduleVersionId` (column = "module_version_id", NOT NULL), `deloadId` (column = "deload_id", Long?, nullable — **sin `@ForeignKey` anotado** porque la tabla `deload` no existe aún), `date` (String, NOT NULL, ISO 8601), `status` (String, NOT NULL, defaultValue = `"IN_PROGRESS"`). Índices: `date`, `module_version_id`, `status`, `deload_id` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/entity/SessionEntity.kt`

- [x] **Crear SessionExerciseEntity** (AC: 05.06)
  - [x] `@Entity(tableName = "session_exercise")`. PK autoincrement `id: Long`. FKs: `sessionId` → `session(id)` ON DELETE CASCADE, `exerciseId` → `exercise(id)` ON DELETE RESTRICT. Columnas: `sessionId`, `exerciseId`, `originalExerciseId` (Long?, nullable — **En HU-05 siempre null**), `progressionClassification` (String?, nullable — **En HU-05 siempre null**). Índices: `session_id`, `exercise_id`. UNIQUE(`session_id`, `exercise_id`) — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/entity/SessionExerciseEntity.kt`

- [x] **Crear ExerciseProgressionEntity** (AC: 05.07, 05.08)
  - [x] `@Entity(tableName = "exercise_progression")`. PK natural `exerciseId: Long` (FK → `exercise(id)` ON DELETE RESTRICT). Columnas: `status` (String, NOT NULL, defaultValue = `"NO_HISTORY"`), `prescribedLoadKg` (column = "prescribed_load_kg", Double?, nullable — **En HU-05 la tabla está vacía**, filas se crean en HU-06), `sessionsWithoutProgression` (column = "sessions_without_progression", Int, NOT NULL, defaultValue = "0"). **Justificación:** LEFT JOIN de `SessionExerciseWithDetails` requiere que la tabla exista en el schema — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/entity/ExerciseProgressionEntity.kt`

- [x] **Crear ExerciseSetEntity** (AC: 05.06 indirect — schema completud)
  - [x] `@Entity(tableName = "exercise_set")`. PK autoincrement `id: Long`. FK: `sessionExerciseId` → `session_exercise(id)` ON DELETE CASCADE. Columnas: `sessionExerciseId` (column = "session_exercise_id", NOT NULL), `setNumber` (column = "set_number", Int, NOT NULL), `weightKg` (column = "weight_kg", Double, NOT NULL), `reps` (Int, NOT NULL), `rir` (Int, NOT NULL). Índice: `session_exercise_id`. UNIQUE(`session_exercise_id`, `set_number`). **Justificación:** (a) LEFT JOIN necesita COUNT de series, (b) evita incremento de versión de BD en HU-06, (c) constraints definidos desde el inicio — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/entity/ExerciseSetEntity.kt`

### Fase 2: Data Layer — DAOs (4 nuevos + 2 modificaciones)

> Basado en Hito #2 del Análisis Arquitectónico

- [x] **Crear SessionDao** (AC: 05.06, 05.01, RNF10)
  - [x] `@Dao`. Métodos: `insert(session): Long`, `getActiveSession(): Flow<SessionEntity?>`, `getById(sessionId): Flow<SessionEntity?>`, `getActiveSessionWithModuleVersion(): Flow<ActiveSessionInfo?>`.
  - [x] Definir `ActiveSessionInfo` como data class en `SessionDao.kt` (no `@Entity`): `sessionId: Long`, `moduleCode: String`, `versionNumber: Int`, `totalExercises: Int`, `completedExercises: Int`.
  - Query SQL para `getActiveSessionWithModuleVersion()`: (ver query completa en §5 Análisis Arquitectónico — §2 DAOs)
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/dao/SessionDao.kt`

- [x] **Crear SessionExerciseDao** (AC: 05.06, 05.07, 05.08)
  - [x] `@Dao`. Métodos: `insertAll(exercises: List<SessionExerciseEntity>)`, `getBySessionId(sessionId): Flow<List<SessionExerciseEntity>>`, `getBySessionIdWithDetails(sessionId): Flow<List<SessionExerciseWithDetails>>`.
  - [x] Definir `SessionExerciseWithDetails` como data class en `SessionExerciseDao.kt` (no `@Entity`): todos los campos de visualización de E1 (ver §5 §2 DAOs para detalle completo).
  - Query SQL JOIN completa (7 tablas): (ver query completa en §5 §2 DAOs para detalle completo)
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/dao/SessionExerciseDao.kt`

- [x] **Crear ExerciseProgressionDao** (AC: 05.07, 05.08)
  - [x] `@Dao`. Métodos: `getByExerciseId(exerciseId): Flow<ExerciseProgressionEntity?>`, `insert(progression)`, `update(progression)`.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/dao/ExerciseProgressionDao.kt`

- [x] **Crear ExerciseSetDao** (stub para HU-06)
  - [x] `@Dao` interface vacía. Los métodos se implementan en HU-06.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/dao/ExerciseSetDao.kt`

- [x] **Modificar RotationStateDao** (+1 método)
  - [x] Agregar: `@Update suspend fun update(state: RotationStateEntity)`. Para HU-09.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/dao/RotationStateDao.kt`

- [x] **Modificar ModuleVersionDao** (+1 método)
  - [x] Agregar: `@Query("SELECT * FROM module_version WHERE module_code = :moduleCode AND version_number = :versionNumber LIMIT 1") fun getByModuleCodeAndVersion(moduleCode: String, versionNumber: Int): Flow<ModuleVersionEntity?>`.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/dao/ModuleVersionDao.kt`

### Fase 3: Data Layer — Database y DI

> Basado en Hitos #3 y #7 del Análisis Arquitectónico

- [x] **Actualizar TensionDatabase** (AC: 05.06)
  - [x] Agregar 4 entities al array. Incrementar `version = 4`. Exponer 4 DAOs abstractos. Se mantiene `fallbackToDestructiveMigration()` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/database/TensionDatabase.kt`

- [x] **Actualizar DatabaseModule** (AC: 05.06)
  - [x] Agregar 4 `@Provides`: `provideSessionDao`, `provideSessionExerciseDao`, `provideExerciseProgressionDao`, `provideExerciseSetDao` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/di/DatabaseModule.kt`

- [x] **Actualizar RepositoryModule** (AC: 05.06)
  - [x] Agregar: `@Binds @Singleton abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/di/RepositoryModule.kt`

### Fase 4: Domain Layer — Models + Enum

> Basado en Hito #4 del Análisis Arquitectónico

- [x] **Crear ExerciseSessionStatus** (AC: 05.06) — Enum: `NOT_STARTED`, `IN_PROGRESS`, `COMPLETED`. Derivado del conteo de series. No se almacena en BD — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/model/ExerciseSessionStatus.kt`
- [x] **Crear RotationState** (AC: 05.01, 05.02, 05.04, 05.05) — Data class: `microcyclePosition`, `currentVersionModuleA`, `currentVersionModuleB`, `currentVersionModuleC`, `microcycleCount` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/model/RotationState.kt`
- [x] **Crear NextSession** (AC: 05.01, 05.02, 05.03) — Data class: `moduleCode`, `versionNumber`, `moduleVersionId` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/model/NextSession.kt`
- [x] **Crear ActiveSession** (RNF10) — Data class: `sessionId`, `moduleCode`, `versionNumber`, `totalExercises`, `completedExercises` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/model/ActiveSession.kt`
- [x] **Crear SessionExerciseDetail** (AC: 05.06, 05.07, 05.08) — Data class con todos los campos de visualización de E1, incluyendo `prescribedLoadKg: Double?` y `status: ExerciseSessionStatus` (derivado en mapper) — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/model/SessionExerciseDetail.kt`

### Fase 5: Domain Layer — Repository Interface + Use Cases

> Basado en Hito #5 del Análisis Arquitectónico

#### Repository Interface

- [x] **Crear SessionRepository** (AC: 05.01-05.09)
  - [x] Interface con 6 contratos: `getNextModuleVersionId()`, `startSession(moduleVersionId)`, `getActiveSession()`, `getSessionExercises(sessionId)`, `getRotationState()`, `getSessionModuleVersion(sessionId)` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/repository/SessionRepository.kt`

#### Use Cases

- [x] **Crear GetNextSessionInfoUseCase** (AC: 05.01, 05.02, 05.03, 05.09)
  - [x] `operator fun invoke(): Flow<NextSession>`. Combina `getRotationState()` con resolución módulo/versión. Lógica: `when(position) { 1,4 → "A"; 2,5 → "B"; 3,6 → "C" }`. Sin referencias temporales.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/usecase/session/GetNextSessionInfoUseCase.kt`
  - [x] Test unitario: mapeo posición→módulo para las 6 posiciones, primera sesión (position=1 → A-V1), caso con versiones avanzadas — Archivo: `...GetNextSessionInfoUseCaseTest.kt`

- [x] **Crear GetActiveSessionUseCase** (RNF10)
  - [x] `operator fun invoke(): Flow<ActiveSession?>`. Delega a `getActiveSession()`.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/usecase/session/GetActiveSessionUseCase.kt`
  - [x] Test unitario: sesión activa encontrada, caso null — Archivo: `...GetActiveSessionUseCaseTest.kt`

- [x] **Crear StartSessionUseCase** (AC: 05.06)
  - [x] `suspend operator fun invoke(moduleVersionId: Long): Long`. Delega a `startSession()`. Propaga `IllegalStateException` si ya hay sesión activa.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/usecase/session/StartSessionUseCase.kt`
  - [x] Test unitario: caso exitoso retorna ID, caso sesión activa existente lanza excepción — Archivo: `...StartSessionUseCaseTest.kt`

- [x] **Crear GetSessionExercisesUseCase** (AC: 05.06, 05.07, 05.08)
  - [x] `operator fun invoke(sessionId: Long): Flow<List<SessionExerciseDetail>>`. Delega a `getSessionExercises()`.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/usecase/session/GetSessionExercisesUseCase.kt`
  - [x] Test unitario: lista con ejercicios, lista vacía — Archivo: `...GetSessionExercisesUseCaseTest.kt`

- [x] **Crear GetMicrocycleCountUseCase** (B1 progreso)
  - [x] `operator fun invoke(): Flow<Int>`. Delega a `getRotationState()` y mapea a `microcycleCount`.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/usecase/session/GetMicrocycleCountUseCase.kt`
  - [x] Test unitario: mapeo correcto, caso null → 0 — Archivo: `...GetMicrocycleCountUseCaseTest.kt`

### Fase 6: Data Layer — Repository Implementation

> Basado en Hito #6 del Análisis Arquitectónico

- [x] **Crear SessionRepositoryImpl** (AC: 05.01-05.09)
  - [x] `@Inject constructor`. Inyecta: `SessionDao`, `SessionExerciseDao`, `PlanAssignmentDao`, `RotationStateDao`, `ModuleVersionDao`, `TensionDatabase`.
  - `getNextModuleVersionId()`: usa `flatMapLatest` encadenando Flow de `rotationState` → resolución módulo → Flow de `moduleVersion`.
  - `startSession()`: transacción atómica con `database.withTransaction {}`. Pasos detallados en §5 §3 Repository.
  - `getSessionExercises()`: mapeo `SessionExerciseWithDetails` → `SessionExerciseDetail` incluyendo derivación de `ExerciseSessionStatus`.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/repository/SessionRepositoryImpl.kt`

### Fase 7: UI Layer — B1 Home (Reemplazo del stub)

> Basado en Hito #8 del Análisis Arquitectónico

- [x] **Crear HomeUiState** (AC: 05.01, 05.02, 05.03, RNF10)
  - [x] Data class: `isLoading`, `nextSession: NextSession?`, `activeSession: ActiveSession?`, `microcycleCount: Int`, `alertCount: Int = 0`. Propiedades derivadas: `showNextSessionCard`, `showResumeCard` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/home/HomeUiState.kt`

- [x] **Crear HomeViewModel** (AC: 05.01, 05.02, 05.03, 05.06, RNF10)
  - [x] `@HiltViewModel`. Combina 3 Flows via `combine()`. Evento de navegación: `MutableSharedFlow<Long>(replay = 0)`. Método `startSession()` en `viewModelScope.launch`. Método `resumeSession(sessionId)` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/home/HomeViewModel.kt`

- [x] **Reemplazar HomeScreen** (AC: 05.01, 05.02, 05.03, 05.06, RNF10)
  - [x] Top Bar custom (NO `TensionTopAppBar`): `Row` con `Text("Tension")` Title Large Primary + `BadgedBox` con `IconButton` Notifications. `LazyColumn` con: Card Reanudar (`ElevatedCard`, Error Container, condicional), Card Próxima Sesión (`FilledCard`, Primary Container, condicional), Sección Progreso (Headline Medium + Body Small). `LaunchedEffect` recolecta `navigationEvent` → `onNavigateToActiveSession(sessionId)`. Ver especificaciones detalladas en §5 §8 — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/home/HomeScreen.kt`

### Fase 8: UI Layer — E1 Sesión Activa (Nuevo, parcial)

> Basado en Hito #9 del Análisis Arquitectónico

- [x] **Crear ActiveSessionUiState y ExerciseUiItem** (AC: 05.06, 05.07, 05.08)
  - [x] `ActiveSessionUiState`: `isLoading`, `moduleCode`, `versionNumber`, `exercises: List<ExerciseUiItem>`. Propiedades derivadas: `completedCount`, `totalCount`, `progress: Float`.
  - [x] `ExerciseUiItem`: `sessionExerciseId`, `exerciseId`, `name`, `equipmentTypeName`, `muscleZones: String`, `sets`, `reps`, `prescribedLoadKg`, `isBodyweight`, `isIsometric`, `isToTechnicalFailure`, `completedSets`, `status: ExerciseSessionStatus`, `loadDisplayText: String`, `statusDisplayText: String` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/session/ActiveSessionUiState.kt`

- [x] **Crear ActiveSessionViewModel** (AC: 05.06, 05.07, 05.08)
  - [x] `@HiltViewModel`. Inyecta `GetSessionExercisesUseCase`, `SessionRepository`. Recibe `sessionId` via `SavedStateHandle`. `combine()` de dos Flows. Mapea `SessionExerciseDetail` → `ExerciseUiItem` calculando `loadDisplayText`:
    ```kotlin
    when {
        detail.isIsometric -> "Isométrico (30-45s)"
        detail.isBodyweight -> "Peso corporal"
        detail.prescribedLoadKg != null -> "%.1f Kg".format(detail.prescribedLoadKg)
        else -> "Sin historial — establecer carga"
    }
    ```
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/session/ActiveSessionViewModel.kt`

- [x] **Crear ActiveSessionScreen** (AC: 05.06, 05.07, 05.08)
  - [x] Top Bar custom (NO `CenterAlignedTopAppBar`). Barra de progreso determinate. `LazyColumn` con `ExerciseRow` composable que renderiza 3 estados (No Iniciado / En Ejecución / Completado) según `status`. Botón "Cerrar Sesión" (OutlinedButton, Secondary, full width, TODO HU-09). `BackHandler` no-op. Sin Bottom Nav. Ver especificaciones detalladas de colores e interacciones en §5 §9 — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/session/ActiveSessionScreen.kt`

### Fase 9: Navigation + Strings

> Basado en Hitos #10 y #11 del Análisis Arquitectónico

- [x] **Agregar ruta ACTIVE_SESSION** (AC: 05.06)
  - [x] Agregar `const val ACTIVE_SESSION = "active-session/{sessionId}"` y `fun activeSessionRoute(sessionId: Long) = "active-session/$sessionId"` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/navigation/NavigationRoutes.kt`

- [x] **Actualizar NavHost** (AC: 05.06)
  - [x] Actualizar composable HOME para pasar `onNavigateToActiveSession`. Agregar composable `ACTIVE_SESSION` con `navArgument("sessionId", NavType.LongType)`. Extender `showBottomBar` para excluir rutas de sesión (`!currentRoute.startsWith("active-session")`). Implementar lógica de evaluación del back stack para ocultar Bottom Nav en D2 cuando el origen es E1 (§4.5.1) — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/navigation/TensionNavHost.kt`

- [x] **Actualizar strings.xml** (AC: 05.01, 05.06)
  - [x] Eliminar strings obsoletos del stub B1 (`home_welcome`, `home_description`). Agregar strings para B1 funcional (8 strings) y E1 (12 strings). Ver lista completa en §5 §11 Recursos — Archivo: `app/src/main/res/values/strings.xml`

### Fase 10: QA y Deployment

#### Code Quality

- [x] **Ejecutar Agente Peer Review** — MANUAL
- [x] **Resolver incidentes del Peer Review** (condicional) — MANUAL

#### Deployment DEV

- [x] **Crear Pull Request** — MANUAL
- [x] **Ejecutar pipeline deployment DEV** — MANUAL

#### Testing Manual

- [x] **Diseñar set de pruebas manuales** — MANUAL
- [x] **Ejecutar pruebas manuales** — MANUAL

---

**Vinculación CA → Fase de implementación:**
- CA-05.01 → Fases 4, 5, 6 (RotationState model + GetNextSessionInfoUseCase + SessionRepositoryImpl lógica posición→módulo). Mapeo determinístico: 1,4→A; 2,5→B; 3,6→C
- CA-05.02 → Fases 4, 5, 6 (RotationState model + Use Case + repo lectura `current_version_module_X`). V1→V2→V3 wrap-around
- CA-05.03 → Fase 6 (defaults de RotationStateEntity: position=1, versionA=1 → module_version A-V1 = id 1)
- CA-05.04 → Implícito (SQLite persiste indefinidamente. RNF13)
- CA-05.05 → Implícito (Room/SQLite es archivo local. RNF13)
- CA-05.06 → Fases 1, 2, 3, 6, 7, 8 (Entities + DAOs + Database + Repository startSession + B1 Card Próxima + E1 lista ejercicios)
- CA-05.07 → Fases 1, 2, 6, 8 (ExerciseProgressionEntity vacía + LEFT JOIN NULL + E1 "Sin historial" italic)
- CA-05.08 → Fases 1, 2, 6, 8 (ExerciseProgressionEntity.prescribedLoadKg + LEFT JOIN valor + E1 "60 Kg")
- CA-05.09 → Fases 5, 6 (GetNextSessionInfoUseCase: fecha solo en session.date como metadato, nunca como input de decisión)
