# Análisis Arquitectónico — Historia #5

## Dependencias Técnicas e Integración

### 4.1. Modelo de Dominio y Estado

- **Entidades Afectadas (nuevas):** `SessionEntity`, `SessionExerciseEntity`, `ExerciseProgressionEntity`, `ExerciseSetEntity` — 4 entidades nuevas que completan el schema de sesión (Referencia a `docs/architecture/domain_and_state_model.md`).
- **Entidades Afectadas (existentes):** `RotationStateEntity` (lectura), `ModuleVersionEntity` (lectura), `PlanAssignmentEntity` (lectura), `ExerciseEntity` (lectura).
- **Mutaciones de Estado:** `startSession()` — INSERT en `session` + INSERT batch en `session_exercise` en transacción atómica. La rotación se avanza en HU-09 (solo lectura en HU-05).

### 4.2. Contrato de Interfaces (Triggers / API)

- **Trigger / Endpoint consumido:** `B1 HomeScreen — Botón "Iniciar Sesión"` | `B1 HomeScreen — Botón "Reanudar Sesión"` (crash recovery) (Referencia a `docs/architecture/interfaces_contract.md`).
- **Payload requerido:** `moduleVersionId: Long` (derivado del `rotation_state`). Para crash recovery: `sessionId: Long` (de la sesión `IN_PROGRESS` existente).

### 4.3. UI / Assets

- **Componente Visual:** `B1 — HomeScreen` (reemplazo del stub — Card Próxima Sesión + Card Reanudar condicional + Sección Progreso), `E1 — ActiveSessionScreen` (nuevo, parcial — lista de ejercicios con prescripción y carga objetivo, sin registro de series aún).

---

## Análisis Arquitectónico

> HU-05 es la historia más compleja hasta ahora. Involucra tres capacidades distintas que se orquestan en un flujo único: (1) Determinación del módulo-versión por rotación cíclica, (2) Creación de la sesión en transacción atómica, (3) Presentación de la prescripción con carga objetivo. También implementa crash recovery (RNF10) y transforma B1 de stub a Home funcional.

**Patrón arquitectónico:** MVVM con capa Domain explícita (4 capas: UI → ViewModel → Domain → Data), según ADR-05.

### Componentes afectados

#### 1. Data Layer — Entities (Nuevas)

Paquete: `data.local.entity`.

- **`SessionEntity`**: Tabla `session`. PK autoincrement. Columnas: `id` (Long, PK), `moduleVersionId` (column = "module_version_id", Long, NOT NULL, FK → `module_version(id)` ON DELETE RESTRICT), `deloadId` (column = "deload_id", Long?, nullable — **sin `@ForeignKey` anotado** porque la tabla `deload` no existe aún, se agrega en HU-17), `date` (String, NOT NULL, ISO 8601 `"YYYY-MM-DD"`), `status` (String, NOT NULL, DEFAULT `"IN_PROGRESS"`). Valores: `"IN_PROGRESS"`, `"COMPLETED"`, `"INCOMPLETE"`. Índices: `date`, `module_version_id`, `status`, `deload_id`.

- **`SessionExerciseEntity`**: Tabla `session_exercise`. PK autoincrement. FKs: `sessionId` → `session(id)` ON DELETE CASCADE, `exerciseId` → `exercise(id)` ON DELETE RESTRICT. Columnas: `sessionId` (Long, NOT NULL), `exerciseId` (Long, NOT NULL), `originalExerciseId` (Long?, nullable — **En HU-05 siempre null**, sustitución se implementa en HU-07), `progressionClassification` (String?, nullable — **En HU-05 siempre null**, se implementa en HU-10). UNIQUE(`session_id`, `exercise_id`). Índices: `session_id`, `exercise_id`.

- **`ExerciseProgressionEntity`**: Tabla `exercise_progression`. PK natural `exerciseId` (FK → `exercise(id)` ON DELETE RESTRICT). Columnas: `status` (String, NOT NULL, DEFAULT `"NO_HISTORY"`). Valores: `"NO_HISTORY"`, `"IN_PROGRESSION"`, `"IN_PLATEAU"`, `"IN_DELOAD"`, `"MASTERED"`. `prescribedLoadKg` (column = "prescribed_load_kg", Double?, nullable — **En HU-05 la tabla está vacía**, filas se crean en HU-06 al registrar la primera serie. Se lee via LEFT JOIN para determinar carga objetivo: null → "Sin historial"). `sessionsWithoutProgression` (column = "sessions_without_progression", Int, NOT NULL, DEFAULT 0). **Justificación de creación en HU-05:** El LEFT JOIN de `SessionExerciseWithDetails` requiere que la tabla exista en el schema.

- **`ExerciseSetEntity`**: Tabla `exercise_set`. PK autoincrement. FK: `sessionExerciseId` → `session_exercise(id)` ON DELETE CASCADE. Columnas: `sessionExerciseId` (column = "session_exercise_id", Long, NOT NULL), `setNumber` (column = "set_number", Int, NOT NULL), `weightKg` (column = "weight_kg", Double, NOT NULL), `reps` (Int, NOT NULL), `rir` (Int, NOT NULL). Índice: `session_exercise_id`. UNIQUE(`session_exercise_id`, `set_number`). **Justificación de creación en HU-05:** (a) LEFT JOIN de `SessionExerciseWithDetails` necesita COUNT de series — la tabla debe existir, (b) evita incremento de versión de BD en HU-06 (BD pasa de 3 → 4 una sola vez), (c) los constraints quedan definidos desde el inicio.

#### 2. Data Layer — DAOs (Nuevos y Modificaciones)

Paquete: `data.local.dao`.

- **`SessionDao`** (Nuevo): `insert(session: SessionEntity): Long`, `getActiveSession(): Flow<SessionEntity?>` (`SELECT * FROM session WHERE status = 'IN_PROGRESS' LIMIT 1`), `getById(sessionId: Long): Flow<SessionEntity?>`, `getActiveSessionWithModuleVersion(): Flow<ActiveSessionInfo?>`.

  **`ActiveSessionInfo`** (data class intermedia no `@Entity`): `sessionId: Long`, `moduleCode: String`, `versionNumber: Int`, `totalExercises: Int`, `completedExercises: Int`.

  Query para `getActiveSessionWithModuleVersion()`:
  ```sql
  SELECT s.id AS sessionId, mv.module_code AS moduleCode, mv.version_number AS versionNumber,
      (SELECT COUNT(*) FROM session_exercise WHERE session_id = s.id) AS totalExercises,
      (SELECT COUNT(*) FROM session_exercise se2
       INNER JOIN (SELECT session_exercise_id, COUNT(*) AS cnt FROM exercise_set
                   GROUP BY session_exercise_id HAVING cnt >= 4) completed
       ON se2.id = completed.session_exercise_id WHERE se2.session_id = s.id) AS completedExercises
  FROM session s INNER JOIN module_version mv ON s.module_version_id = mv.id
  WHERE s.status = 'IN_PROGRESS' LIMIT 1
  ```

- **`SessionExerciseDao`** (Nuevo): `insertAll(exercises: List<SessionExerciseEntity>)`, `getBySessionId(sessionId: Long): Flow<List<SessionExerciseEntity>>`, `getBySessionIdWithDetails(sessionId: Long): Flow<List<SessionExerciseWithDetails>>`.

  **`SessionExerciseWithDetails`** (data class intermedia no `@Entity`): `sessionExerciseId: Long`, `exerciseId: Long`, `exerciseName: String`, `equipmentTypeName: String`, `muscleZones: String` (GROUP_CONCAT), `sets: Int`, `reps: String`, `isBodyweight: Int`, `isIsometric: Int`, `isToTechnicalFailure: Int`, `prescribedLoadKg: Double?`, `completedSets: Int`.

  Query JOIN completa (7 tablas):
  ```sql
  SELECT se.id AS sessionExerciseId, se.exercise_id AS exerciseId, e.name AS exerciseName,
      et.name AS equipmentTypeName, GROUP_CONCAT(DISTINCT mz.name) AS muscleZones,
      pa.sets, pa.reps, e.is_bodyweight AS isBodyweight, e.is_isometric AS isIsometric,
      e.is_to_technical_failure AS isToTechnicalFailure,
      ep.prescribed_load_kg AS prescribedLoadKg,
      (SELECT COUNT(*) FROM exercise_set es WHERE es.session_exercise_id = se.id) AS completedSets
  FROM session_exercise se
  INNER JOIN exercise e ON se.exercise_id = e.id
  INNER JOIN equipment_type et ON e.equipment_type_id = et.id
  INNER JOIN session s ON se.session_id = s.id
  INNER JOIN plan_assignment pa ON pa.module_version_id = s.module_version_id AND pa.exercise_id = se.exercise_id
  LEFT JOIN exercise_muscle_zone emz ON e.id = emz.exercise_id
  LEFT JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
  LEFT JOIN exercise_progression ep ON e.id = ep.exercise_id
  WHERE se.session_id = :sessionId
  GROUP BY se.id ORDER BY e.name ASC
  ```
  > Se usa subquery `(SELECT COUNT(*) FROM exercise_set WHERE session_exercise_id = se.id)` para `completedSets` en vez de `COUNT(DISTINCT es.id)` en el GROUP BY para evitar interacción con el GROUP_CONCAT de muscle_zones.

- **`ExerciseProgressionDao`** (Nuevo): `getByExerciseId(exerciseId: Long): Flow<ExerciseProgressionEntity?>`, `insert(progression: ExerciseProgressionEntity)`, `update(progression: ExerciseProgressionEntity)`.

- **`ExerciseSetDao`** (Nuevo, stub): Interface `@Dao` vacía — los métodos de inserción y consulta se implementan en HU-06. La interface se expone en `TensionDatabase` para que Room la valide.

- **`ModuleVersionDao`** (Modificación): Agregar `getByModuleCodeAndVersion(moduleCode: String, versionNumber: Int): Flow<ModuleVersionEntity?>` — `SELECT * FROM module_version WHERE module_code = :moduleCode AND version_number = :versionNumber LIMIT 1`. Para resolver `(moduleCode, versionNumber)` → `module_version_id`.

- **`RotationStateDao`** (Modificación): Agregar `@Update suspend fun update(state: RotationStateEntity)`. Para HU-09 (avanzar rotación al cerrar sesión).

#### 3. Data Layer — Repository (Nuevo)

Paquete: `data.repository`.

- **`SessionRepositoryImpl`**: Implementa `SessionRepository`. Inyecta `SessionDao`, `SessionExerciseDao`, `PlanAssignmentDao`, `RotationStateDao`, `ModuleVersionDao`, `TensionDatabase`. Métodos:

  - **`getNextModuleVersionId(): Flow<Long>`** — Lógica central de determinación (CA-05.01, 05.02, 05.03):
    1. Lee `rotation_state` via `RotationStateDao.getRotationState()`.
    2. Mapea `microcyclePosition` (1-6) a módulo: `when(pos) { 1,4 → "A"; 2,5 → "B"; 3,6 → "C" }`.
    3. Lee la versión actual del módulo: `currentVersionModuleA`, `_b`, o `_c`.
    4. Busca `module_version_id` via `ModuleVersionDao.getByModuleCodeAndVersion(moduleCode, versionNumber)`.
    5. Retorna como Flow (reactivo con `flatMapLatest`).

  - **`startSession(moduleVersionId: Long): Long`** — Transacción atómica `database.withTransaction`:
    1. Verifica que NO exista sesión `IN_PROGRESS` via `sessionDao.getActiveSession().first()`. Si existe, lanza `IllegalStateException`.
    2. INSERT `SessionEntity(moduleVersionId, date = LocalDate.now().toString(), status = "IN_PROGRESS", deloadId = null)`. Obtiene `sessionId`.
    3. Consulta `planAssignmentDao.getByModuleVersionId(moduleVersionId).first()` (lectura one-shot del Flow).
    4. **Valida que la lista no esté vacía.** Si vacía, lanza `IllegalStateException("No exercises assigned to this module version")`.
    5. Construye `List<SessionExerciseEntity>` (una fila por ejercicio: `exerciseId`, `sessionId`, `originalExerciseId = null`, `progressionClassification = null`).
    6. INSERT batch via `sessionExerciseDao.insertAll(exercises)`.
    7. Retorna `sessionId`.

  - **`getActiveSession(): Flow<ActiveSession?>`** — Mapea `ActiveSessionInfo` → `ActiveSession` (domain model).
  - **`getSessionExercises(sessionId: Long): Flow<List<SessionExerciseDetail>>`** — Mapea `SessionExerciseWithDetails` → `SessionExerciseDetail`. El mapeo: `muscleZones` split por `", "`, `isBodyweight/isIsometric/isToTechnicalFailure` `Int → Boolean`, `status` derivado del `completedSets`.
  - **`getRotationState(): Flow<RotationState?>`** — Mapea `RotationStateEntity` → `RotationState`.
  - **`getSessionModuleVersion(sessionId: Long): Flow<Pair<String, Int>?>`** — Encadena `sessionDao.getById()` con `moduleVersionDao.getById()` via `flatMapLatest`. Para el Top Bar de E1.

#### 4. Domain Layer — Models (Nuevos)

Paquete: `domain.model`.

- **`RotationState`**: Data class — `microcyclePosition: Int`, `currentVersionModuleA: Int`, `currentVersionModuleB: Int`, `currentVersionModuleC: Int`, `microcycleCount: Int`. Kotlin puro.
- **`ExerciseSessionStatus`**: Enum class — `NOT_STARTED`, `IN_PROGRESS`, `COMPLETED`. Derivado del conteo de series (0 = NOT_STARTED, 1-3 = IN_PROGRESS, 4 = COMPLETED). No se almacena en BD (Modelo de Datos §3.11: "se deriva en la capa de aplicación contando las series asociadas").
- **`NextSession`**: Data class para B1 Card "Próxima Sesión" — `moduleCode: String`, `versionNumber: Int`, `moduleVersionId: Long`.
- **`ActiveSession`**: Data class para B1 Card "Reanudar Sesión" — `sessionId: Long`, `moduleCode: String`, `versionNumber: Int`, `totalExercises: Int`, `completedExercises: Int`.
- **`SessionExerciseDetail`**: Data class para E1 — `sessionExerciseId: Long`, `exerciseId: Long`, `name: String`, `equipmentTypeName: String`, `muscleZones: List<String>`, `sets: Int`, `reps: String`, `isBodyweight: Boolean`, `isIsometric: Boolean`, `isToTechnicalFailure: Boolean`, `prescribedLoadKg: Double?` (null = sin historial CA-05.07, valor = carga objetivo CA-05.08), `completedSets: Int` (0-4, determina estado derivado), `status: ExerciseSessionStatus` (derivado en mapper).

#### 5. Domain Layer — Repository Interface (Nuevo)

Paquete: `domain.repository`.

- **`SessionRepository`**: Interface. Contratos: `getNextModuleVersionId(): Flow<Long>`, `startSession(moduleVersionId: Long): Long`, `getActiveSession(): Flow<ActiveSession?>`, `getSessionExercises(sessionId: Long): Flow<List<SessionExerciseDetail>>`, `getRotationState(): Flow<RotationState?>`, `getSessionModuleVersion(sessionId: Long): Flow<Pair<String, Int>?>`.

#### 6. Domain Layer — Use Cases (Nuevos)

Paquete: `domain.usecase.session`.

- **`GetNextSessionInfoUseCase`**: `operator fun invoke(): Flow<NextSession>`. Combina `getRotationState()` con resolución módulo/versión para producir `NextSession`. Lógica de resolución `microcyclePosition` → `moduleCode`:
  ```kotlin
  fun resolveModuleCode(position: Int): String = when (position) {
      1, 4 -> "A"; 2, 5 -> "B"; 3, 6 -> "C"
      else -> error("Invalid microcycle position: $position")
  }
  ```
  **Nota sobre CA-05.09:** La fecha NO se usa en ningún paso de esta lógica.

- **`GetActiveSessionUseCase`**: `operator fun invoke(): Flow<ActiveSession?>`. Delega a `getActiveSession()`. Para B1 crash recovery card.

- **`StartSessionUseCase`**: `suspend operator fun invoke(moduleVersionId: Long): Long`. Delega a `startSession()`. Si ya existe sesión activa, propaga `IllegalStateException`.

- **`GetSessionExercisesUseCase`**: `operator fun invoke(sessionId: Long): Flow<List<SessionExerciseDetail>>`. Delega a `getSessionExercises()`. Para E1.

- **`GetMicrocycleCountUseCase`**: `operator fun invoke(): Flow<Int>`. Delega a `getRotationState()` y mapea a `microcycleCount`. Para B1 indicador de progreso.

#### 7. Data Layer — Database (Modificación)

- **`TensionDatabase`**: Agregar las 4 nuevas entities al array. Versión incrementa a **4** (se mantiene `fallbackToDestructiveMigration()` durante desarrollo). Exponer los 4 nuevos DAOs: `sessionDao()`, `sessionExerciseDao()`, `exerciseProgressionDao()`, `exerciseSetDao()`.

#### 8. UI Layer — B1 Home (Reemplazo del stub)

Paquete: `ui.home`.

- **`HomeScreen`** (Reemplazo completo del stub): Estructura según Wireframes B1 y Especificación Visual §8 B1:
  - **Top Bar custom (NO usa `TensionTopAppBar` genérico)**: `Row` dentro de `Surface`. Izquierda: `Text("Tension")`, Title Large, color Primary (`#8B1A1A`), padding horizontal 16 dp. Derecha: `IconButton` con ícono `Icons.Outlined.Notifications`, tint On Surface Variant. `BadgedBox` superpuesto. **En HU-05, badge siempre punto vacío** (`Badge` sin content, `containerColor = Outline (#857370)`, size 6 dp). Al tocar → `onNavigateToAlerts()`.
  - **Body** (`LazyColumn`, padding horizontal 16 dp):
    - **Card "Reanudar Sesión"** (condicional: `showResumeCard`, visible si hay sesión `IN_PROGRESS`): `ElevatedCard`, containerColor Error Container (`#FFDAD6`), tonalElevation Level 2 (3 dp), shape RoundedCornerShape(12 dp). Ícono ⚠️ (tint Error) + "Tienes una sesión activa sin cerrar" (Title Medium, On Error Container `#410002`). Línea: "Módulo X — Versión N" + "N de M ejercicios completados". Botón "Reanudar Sesión" (Filled, Primary). **Cuando visible, la Card "Próxima Sesión" se OCULTA.**
    - **Card "Próxima Sesión"** (condicional: `showNextSessionCard`, visible si NO hay sesión activa): `FilledCard`, containerColor Primary Container (`#F5DDDD`), shape RoundedCornerShape(12 dp). "Módulo X — Versión N" (Title Medium, On Primary Container `#5C0E0E`). "Tu próxima sesión" (Body Medium, On Primary Container). Botón "Iniciar Sesión" (Filled Button, full width, Primary) → `viewModel.startSession()`.
    - **Sección "Progreso"**: `HorizontalDivider` + número `microcycleCount` (Headline Medium, Primary) + label "microciclos" (Body Small, On Surface Variant). Centrado horizontalmente.
    - **Card "Estado de Descarga"**: **En HU-05 siempre oculta** (se implementa en HU-17).

```kotlin
@Composable
fun HomeScreen(
    onNavigateToAlerts: () -> Unit,
    onNavigateToActiveSession: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
)
```

- **`HomeViewModel`**: `@HiltViewModel`. Inyecta los 4 Use Cases. Combina 3 Flows via `combine()` → `HomeUiState`. Método `startSession()`: ejecuta en `viewModelScope.launch`, emite `sessionId` via `MutableSharedFlow<Long>(replay = 0)`. Método `resumeSession(sessionId: Long)`: emite `sessionId` en el mismo SharedFlow.
- **`HomeUiState`**: Data class — `isLoading`, `nextSession: NextSession?`, `activeSession: ActiveSession?`, `microcycleCount: Int`, `alertCount: Int = 0`. Propiedades derivadas: `showNextSessionCard = activeSession == null`, `showResumeCard = activeSession != null`.

#### 9. UI Layer — E1 Sesión Activa (Nuevo, parcial)

Paquete: `ui.session`.

- **`ActiveSessionScreen`**: Composable de nivel pantalla. Recibe `sessionId` como argumento de navegación. Estructura según Wireframes E1 y Especificación Visual §8 E1:
  - **Top Bar custom (NO usa `CenterAlignedTopAppBar`)**: `Column` con padding horizontal 16 dp. `Text("Módulo X — Versión N")` (Title Large, On Surface, alineado izquierda). `Text("Sesión activa")` (Title Small, On Surface Variant). Badge descarga: **oculto en HU-05** (HU-17).
  - **Barra de progreso**: `Text("N de M ejercicios completados")` (Body Medium). `LinearProgressIndicator` determinate, trackColor Surface Variant, color Primary, height 8 dp, shape RoundedCornerShape(4 dp). Porcentaje (Label Small, On Surface Variant, textAlign End).
  - **Lista de ejercicios** (`LazyColumn`): Cada ejercicio renderizado según `ExerciseSessionStatus`:

    **No Iniciado** (`completedSets == 0`): `OutlinedCard`, borderColor Outline Variant. Leading ⚪ gris Outline (`#857370`), 24 dp. Línea 1: Title Medium, On Surface (nombre). Línea 2: statusDisplayText. Línea 3: carga objetivo (variantes):

    | Tipo | Condición | Texto | Estilo |
    |---|---|---|---|
    | Estándar con historial | `!isBodyweight && !isIsometric && prescribedLoadKg != null` | "60 Kg" | Body Medium, On Surface |
    | Estándar sin historial | `!isBodyweight && !isIsometric && prescribedLoadKg == null` | "Sin historial — establecer carga" | Body Medium, On Surface Variant, italic |
    | Peso corporal | `isBodyweight && !isIsometric` | "Peso corporal" | Body Medium, On Surface Variant |
    | Isométrico | `isIsometric` | "Isométrico (30-45s)" | Body Medium, On Surface Variant |

    Trailing `Row` spacing 8 dp: `FilledTonalButton("Registrar")` (48 dp touch, containerColor Primary Container, **En HU-05: TODO HU-06**) + `OutlinedButton("Sustituir")` (48 dp touch, **En HU-05: TODO HU-07**) + `IconButton(📷)` (48×48 dp, onClick → `onNavigateToExerciseDetail(exerciseId)`).

    **En Ejecución** (`completedSets` 1-3): `FilledCard`, containerColor `#E3F2FD` (light) / `#1A2733` (dark). Leading 🔵 `#1565C0` (light) / `#64B5F6` (dark). Botón "Registrar" → **`Button` (Filled, Primary)** (distinto de No Iniciado — la Especificación Visual §8 E1 escala la prominencia). **Sin botón "Sustituir"** (CA-07.06: solo si 0 series). **En HU-05 este estado nunca se alcanza.**

    **Completado** (`completedSets == 4`): `FilledCard`, containerColor `#E8F5E9` (light) / `#1A2E1A` (dark). Leading ✅ `#2E7D32` (light) / `#81C784` (dark). Texto `alpha = 0.7f`. Solo 📷. **En HU-05 nunca se alcanza.**

  - **Botón "Cerrar Sesión"**: `OutlinedButton` full width, borderColor Secondary (`#6B4F4F`). **En HU-05: TODO HU-09**.
  - **`BackHandler`**: En HU-05: `BackHandler { /* no-op */ }` — el usuario debe cerrar sesión formalmente. En HU-09 se reemplaza con diálogo de confirmación (E4).
  - **Sin Bottom Navigation** (Arquitectura Técnica §4.5.1: E1 siempre oculta).

```kotlin
@Composable
fun ActiveSessionScreen(
    onNavigateToRegisterSet: (Long) -> Unit,
    onNavigateToSubstitute: (Long) -> Unit,
    onNavigateToExerciseDetail: (Long) -> Unit,
    onNavigateToSessionSummary: (Long) -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: ActiveSessionViewModel = hiltViewModel(),
)
```

- **`ActiveSessionViewModel`**: `@HiltViewModel`. Inyecta `GetSessionExercisesUseCase`, `SessionRepository`. Recibe `sessionId` via `SavedStateHandle`. Combina `getSessionExercisesUseCase(sessionId)` con `sessionRepository.getSessionModuleVersion(sessionId)` via `combine()`. Mapea `SessionExerciseDetail` → `ExerciseUiItem` calculando `loadDisplayText` y `statusDisplayText`.
- **`ActiveSessionUiState`**: Data class — `isLoading`, `moduleCode: String`, `versionNumber: Int`, `exercises: List<ExerciseUiItem>`. Propiedades derivadas: `completedCount`, `totalCount`, `progress: Float`.
- **`ExerciseUiItem`**: Data class — `sessionExerciseId`, `exerciseId`, `name`, `equipmentTypeName`, `muscleZones: String`, `sets`, `reps`, `prescribedLoadKg`, `isBodyweight`, `isIsometric`, `isToTechnicalFailure`, `completedSets`, `status: ExerciseSessionStatus`, `loadDisplayText: String`, `statusDisplayText: String`.

#### 10. UI Layer — Navegación (Modificaciones)

- **`NavigationRoutes`**: Agregar `const val ACTIVE_SESSION = "active-session/{sessionId}"` y helper `fun activeSessionRoute(sessionId: Long) = "active-session/$sessionId"`.
- **`TensionNavHost`**: Actualizar composable HOME para pasar `onNavigateToActiveSession`. Agregar composable `ACTIVE_SESSION` con `navArgument("sessionId", NavType.LongType)`. Extender `showBottomBar` para excluir rutas de sesión:
  ```kotlin
  val showBottomBar = currentRoute != null &&
      currentRoute != NavigationRoutes.REGISTER &&
      !currentRoute.startsWith("active-session")
  ```
  **Caso especial D2 desde E1:** Cuando el ejecutante navega E1→D2 (📷), la Arquitectura Técnica §4.5.1 indica que Bottom Nav debe ocultarse (origen sesión activa). Implementación: verificar `navController.previousBackStackEntry?.destination?.route?.startsWith("active-session") == true`. Se prefiere la evaluación del back stack por ser más limpia que contaminar la ruta con query params.
- **`BottomNavigationBar` / visibilidad**: Ya se gestiona en `TensionNavHost` via `showBottomBar`. No requiere cambios directos.

#### 11. UI Layer — Recursos

- **`strings.xml`**: Eliminar strings obsoletos del stub B1 (`home_welcome`, `home_description`). Agregar strings para B1 funcional y E1:
  - B1: `home_next_session_label`, `home_next_session_format`, `home_start_session`, `home_resume_title`, `home_resume_progress`, `home_resume_session`, `home_microcycles_label`, `home_alert_badge_description`.
  - E1: `session_active_label`, `session_module_version_format`, `session_progress_format`, `session_status_not_started`, `session_status_in_progress`, `session_status_completed`, `session_load_kg_format`, `session_load_no_history`, `session_load_bodyweight`, `session_load_isometric`, `session_register_set`, `session_substitute`, `session_close`.

#### 12. DI Layer — Hilt Modules (Modificaciones)

- **`DatabaseModule`**: Agregar 4 `@Provides` para los nuevos DAOs: `provideSessionDao`, `provideSessionExerciseDao`, `provideExerciseProgressionDao`, `provideExerciseSetDao`.
- **`RepositoryModule`**: Agregar `@Binds @Singleton abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository`.

### Dependencias y precondiciones

| Precondición | Fuente | Estado |
|---|---|---|
| `RotationStateEntity` existe y se inicializa al crear perfil | HU-01 | Implementada |
| `RotationStateDao.getRotationState()` disponible | HU-01 | Implementada |
| `ModuleVersionEntity` con 9 filas seed (IDs 1-9) | HU-03 | Implementada |
| `PlanAssignmentEntity` con 93 filas seed | HU-03 | Implementada |
| `PlanAssignmentDao.getByModuleVersionId()` disponible | HU-03 | Implementada |
| `ModuleVersionDao.getById()` disponible | HU-04 | Implementada |
| Navegación B1→E1 definida en Mapa de Navegación | Arquitectura | Documentada |
| Bottom Nav oculta en E1 | Arquitectura Técnica §4.5.1 | Documentada |

### Hitos de implementación

| # | Entregable | Dependencia |
|---|---|---|
| 1 | Data Layer — Entities: `SessionEntity`, `SessionExerciseEntity`, `ExerciseProgressionEntity`, `ExerciseSetEntity` | — |
| 2 | Data Layer — DAOs: `SessionDao` (+ `ActiveSessionInfo`), `SessionExerciseDao` (+ `SessionExerciseWithDetails`), `ExerciseProgressionDao`, `ExerciseSetDao` (stub), `RotationStateDao` (+update), `ModuleVersionDao` (+getByModuleCodeAndVersion) | Hito 1 |
| 3 | Data Layer — `TensionDatabase` (version 4, +4 entities, +4 DAOs) | Hito 1, Hito 2 |
| 4 | Domain — Models: `RotationState`, `SessionExerciseDetail`, `ExerciseSessionStatus`, `NextSession`, `ActiveSession` | — |
| 5 | Domain — Repository interface `SessionRepository` + Use Cases (5) | Hito 4 |
| 6 | Data Layer — `SessionRepositoryImpl` | Hito 2, Hito 3, Hito 5 |
| 7 | DI — `DatabaseModule` (+4 DAOs), `RepositoryModule` (+SessionRepository) | Hito 3, Hito 6 |
| 8 | UI — `HomeScreen` reemplazo + `HomeViewModel` + `HomeUiState` | Hito 5, Hito 7 |
| 9 | UI — `ActiveSessionScreen` + `ActiveSessionViewModel` + `ActiveSessionUiState` + `ExerciseUiItem` | Hito 5, Hito 7 |
| 10 | Navegación — `NavigationRoutes` (+ACTIVE_SESSION), `TensionNavHost` (B1 actualizado, E1 nuevo), Bottom Nav (exclusión sesión) | Hito 8, Hito 9 |
| 11 | Recursos — `strings.xml` (+strings B1/E1) | — (independiente) |

### Notas de auditoría

1. **CA-05.01 (rotación cíclica) se resuelve leyendo `rotation_state.microcycle_position`.** El mapeo posición→módulo es determinístico: 1,4→A; 2,5→B; 3,6→C. No se consulta fecha ni calendario en ningún punto del flujo — la fecha solo se usa como metadato de `session.date` al persistir (CA-05.09). La rotación es agnóstica al calendario por diseño (MDS §5.A.1).
2. **CA-05.02 (versión) se resuelve leyendo `current_version_module_X` del `rotation_state`.** Cada módulo tiene su versión independiente (3 columnas separadas). Las versiones rotan V1→V2→V3→V1 al inicio de cada microciclo nuevo (HU-09 avance). En HU-05 solo se lee.
3. **CA-05.03 (primera sesión) se cumple por valores por defecto del `RotationStateEntity`.** `microcyclePosition = 1`, `currentVersionModuleA = 1` → Position 1 → Módulo A, Versión 1 → `module_version` id = 1 (seed data §3.6).
4. **CA-05.04 y CA-05.05 (persistencia/durabilidad) se cumplen por diseño de Room/SQLite.** Persiste tras cierre de app, reinicio de dispositivo y actualización de versión (RNF13).
5. **CA-05.06 (prescripción) se resuelve con la query JOIN en `SessionExerciseDao.getBySessionIdWithDetails()`.** La lista completa de ejercicios + carga objetivo se obtiene en una sola consulta.
6. **CA-05.07 (sin historial) se resuelve con NULL.** Si no existe fila en `exercise_progression`, el LEFT JOIN retorna `NULL` en `prescribedLoadKg`. La UI mapea `null` → "Sin historial — establecer carga" con fontStyle italic.
7. **CA-05.08 (carga del historial) se resuelve con `exercise_progression.prescribed_load_kg`.** Este campo se calcula y persiste al cierre de sesión por HU-10/HU-11. En HU-05, para la primera sesión, todas las cargas serán `null`.
8. **RNF10 (crash recovery) se implementa en B1.** `SessionDao.getActiveSessionWithModuleVersion()` detecta si hay sesión `IN_PROGRESS`. Si existe, B1 muestra Card "Reanudar Sesión" y oculta Card "Próxima Sesión". Al tocar "Reanudar" → navega a E1 con el `sessionId` existente.
9. **La restricción "solo una sesión activa a la vez" se garantiza en `startSession()`.** Antes de insertar, se verifica que no exista sesión `IN_PROGRESS`. Si existe, se lanza `IllegalStateException`. La UI no debería permitirlo porque B1 oculta el botón "Iniciar" cuando hay sesión activa y muestra "Reanudar".
10. **El estado del ejercicio NO se almacena en BD.** Se deriva del conteo de series: 0 = No Iniciado, 1-3 = En Ejecución, 4 = Completado (Modelo de Datos §3.11).
11. **`ExerciseSetEntity` se crea como entity completa en HU-05** aunque HU-06 insertará datos: (a) el LEFT JOIN de `SessionExerciseWithDetails` necesita COUNT de series, (b) la versión de BD (4) incluye el schema completo, evitando incremento adicional en HU-06.
12. **`ExerciseProgressionEntity` se crea vacía en HU-05.** Las filas se crean en HU-06 al registrar la primera serie (Modelo de Datos §3.13). En HU-05, el LEFT JOIN retorna `null` para todos los ejercicios → CA-05.07 correcto.
13. **E1 no tiene Bottom Navigation** (Wireframes E1, Arquitectura Técnica §4.5.1). La condición `showBottomBar` se amplía para excluir rutas con prefijo `"active-session"`.
14. **B1 deja de ser un stub.** Se reemplaza el placeholder completamente. Los strings `home_welcome` y `home_description` de la versión stub se eliminan.
15. **La Card "Estado de Descarga" en B1 se difiere a HU-17.** En HU-05, el body de B1 solo muestra: Card Reanudar (condicional) XOR Card Próxima Sesión + Sección Progreso.
16. **`deload_id` en `SessionEntity` se define como nullable sin FK anotada.** La FK declarativa se agrega en HU-17 cuando se cree `DeloadEntity`.
17. **`HomeViewModel.startSession()` no debe bloquear la UI.** La creación de sesión se ejecuta en coroutine. El resultado se emite como evento oneshot via `MutableSharedFlow<Long>(replay = 0)`. La UI recolecta el Flow en `LaunchedEffect` y navega a E1.
18. **Mapeo `microcycle_position` → `module_version_id` es un cálculo de 3 pasos.** (1) `position` → `moduleCode`, (2) `rotation_state` → `currentVersion` del módulo, (3) lookup en `module_version`. Este cálculo se encapsula en `SessionRepositoryImpl` y es determinístico/testeable.

### Riesgos y mitigaciones

| Riesgo | Probabilidad | Impacto | Mitigación |
|---|---|---|---|
| Transacción `startSession` falla parcialmente | Baja | Alto | Usar `database.withTransaction { }` de Room para atomicidad |
| `rotation_state` no existe al intentar iniciar sesión | Nula | Alto | B1 solo es accesible con perfil existente (start destination dinámica). `getRotationState()` retorna `Flow<RotationState?>`, el ViewModel verifica non-null |
| Sesión huérfana (creada pero app se cierra antes de navegar a E1) | Baja | Medio | La sesión queda `IN_PROGRESS`. El crash recovery de B1 la detecta y ofrece reanudarla |
| `exercise_set` tabla vacía causa error en LEFT JOIN | Nula | Alto | La tabla se crea vacía en HU-05. LEFT JOIN con tabla vacía retorna 0 en COUNT — comportamiento esperado |
| El ejecutante toca "Iniciar Sesión" múltiples veces rápido (double-tap) | Media | Bajo | `startSession()` valida que no exista sesión activa. La UI deshabilita el botón tras el primer toque (loading state) |

### Verificación cruzada de CAs

| CA | Mecanismo de cumplimiento | Verificado contra |
|---|---|---|
| CA-05.01 | `RotationStateEntity.microcyclePosition` mapea a módulo A/B/C. Sin referencia temporal | Modelo de Datos §3.14, MDS §5.A.1 |
| CA-05.02 | `current_version_module_X` del `rotation_state`. V1→V2→V3 wrap-around | Modelo de Datos §3.14 |
| CA-05.03 | Defaults: `microcyclePosition=1`, `currentVersionModuleA=1` → module_version(A,1) = id 1 | HU-01 `RotationStateEntity()` defaults, Modelo de Datos §3.6 seed |
| CA-05.04 | SQLite persiste indefinidamente. Sin timeout ni reinicio | RNF13, Modelo de Datos §3.14 |
| CA-05.05 | Room/SQLite es archivo local. Sobrevive reinicios y actualizaciones | RNF13 |
| CA-05.06 | `SessionExerciseWithDetails` JOIN retorna lista completa + carga | Wireframes E1, Especificación Visual §8 E1, RF12 |
| CA-05.07 | LEFT JOIN `exercise_progression` → NULL → "Sin historial" italic | Especificación Visual §8 E1 |
| CA-05.08 | `exercise_progression.prescribed_load_kg` calculado por HU-10/11 | Modelo de Datos §3.13 |
| CA-05.09 | `LocalDate.now()` solo se usa para `session.date`. La determinación A/B/C no usa fecha | MDS §5.A.1 |

### Auditoría profunda — 2026-02-13

**Hallazgos corregidos:**

| # | Hallazgo | Severidad | Corrección aplicada |
|---|---|---|---|
| A1 | Card "Próxima Sesión" descrita como `OutlinedCard` | Error | Corregido a `FilledCard` con fondo Primary Container (`#F5DDDD`), elevation tonal Level 1 |
| A2 | Card "Próxima Sesión" — tipografía "Módulo X" como `Title Large, On Surface` | Error | Corregido a Title Medium + On Primary Container (`#5C0E0E`) |
| A3 | Card "Próxima Sesión" — orden de textos invertido | Error | Corregido: módulo/versión primero (Title Medium), label "Tu próxima sesión" después (Body Medium) |
| A4 | Sección "Progreso" — número como `Display Medium` | Error | Corregido a Headline Medium |
| A5 | Sección "Progreso" — label como `Body Medium` | Error | Corregido a Body Small |
| A6 | Card "Reanudar Sesión" — subtítulo en una línea | Menor | Corregido a dos líneas separadas (decisión deliberada priorizando Wireframes) |
| A7 | `SessionRepository` interface faltaba `getSessionModuleVersion(sessionId)` | Gap | Agregado a la interface y a `SessionRepositoryImpl` |
| A8 | `startSession()` descrito con query directa al DAO, pero `getByModuleVersionId()` retorna `Flow` | Imprecisión | Corregido: especificado `.first()` para lectura one-shot del Flow |
| A9 | `startSession()` sin validación de plan vacío | Gap | Agregado validación: si lista vacía, lanza `IllegalStateException` |

**Hallazgos verificados como correctos** (V1-V17): defaults de `RotationStateEntity`, mapeo posición→módulo, V1→V2→V3 wrap-around, session lifecycle, crash recovery, estado ejercicio derivado, tabla `exercise_set` vacía, tabla `exercise_progression` vacía, `deload_id` nullable sin FK, E1 sin Bottom Navigation, `showBottomBar` ampliar exclusión, rotación agnóstica al calendario, DB version 3→4 con destructive migration, todos los DAOs/Entities existentes verificados.

**Inconsistencia externa detectada:** HU-01 dice `current_version_module_b` rango 1-2, pero Modelo de Datos §3.14 define rango 1-3 (correcto). Impacto en HU-05: ninguno. Es documental, no funcional.

**Cadena de dependencias verificada:**
```
HU-01 (rotation_state init) → HU-03 (module_version + plan_assignment seed)
  → HU-04 (ModuleVersionDao.getById, plan customization)
    → HU-05 (session creation, rotation read, E1/B1) — ESTA HU
      → HU-06 (exercise_set writes, exercise_progression creates)
      → HU-07 (session_exercise substitution)
      → HU-09 (session close, rotation advance)
        → HU-10 (progression classification)
        → HU-11 (prescribed_load_kg)
```
