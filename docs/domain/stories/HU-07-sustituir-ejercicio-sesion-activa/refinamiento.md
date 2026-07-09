## Refinamiento Técnico (Developer)
**Autor**: Esteban Colorado González | **Fecha**: 2026-02-14

---

### Consideraciones Generales

**Basado en análisis arquitectónico:**
Análisis Arquitectónico de HU-07 con 9 hitos, corrección de bug crítico en E1 (INNER JOIN → LEFT JOIN), lógica de sustitución puntual con preservación de `original_exercise_id`. Patrón Clean Architecture (MVVM + Use Cases + Repository).

**Nivel de complejidad:**
BAJA-MEDIA — No introduce entidades nuevas (`session_exercise` ya tiene `original_exercise_id` desde HU-05). Modifica 2 DAOs existentes (+4 métodos), extiende 2 Repository interfaces (+4 métodos), crea 1 modelo de dominio, 1 Use Case (wrapper fino), 1 pantalla E3 con ViewModel y UiState, 1 ruta de navegación, y ~8 strings. Complejidad reside en: (a) corrección del bug crítico en E1, (b) lógica de preservación de `original_exercise_id` ante sustituciones dobles, (c) transacción atómica con validación de `completedSets`.

**Riesgos técnicos conocidos:**
1. Ejercicio sustituido desaparece de E1 (bug INNER JOIN) — Fix obligatorio. Probabilidad: segura sin fix. Impacto: bloqueante.
2. Race condition: registro de serie entre validación y UPDATE — `database.withTransaction {}` + UI condicional.
3. Sustitución doble pierde `original_exercise_id` — Lógica: `info.originalExerciseId ?: info.exerciseId`.

**Patrones y convenciones del equipo:**
- Código fuente en inglés, UI y datos de dominio en español (Arquitectura Técnica §5.1)
- Naming: `{Feature}Screen`, `{Feature}ViewModel`, `{Acción}{Entidad}UseCase`, `{Entidad}Repository`/`{Entidad}RepositoryImpl`, `{Entidad}Entity`, `{Entidad}Dao` (§5.2)
- Estructura Composable: hiltViewModel() + collectAsStateWithLifecycle() + LaunchedEffect para eventos (§5.3)
- Estructura ViewModel: `_uiState MutableStateFlow` / `uiState StateFlow` + `_events MutableSharedFlow` / `events SharedFlow` (§5.4)
- Sealed classes para UiState (Loading, Success, Error) y Events
- `operator fun invoke()` en Use Cases
- Callbacks en Composables con prefijo `on` (onNavigateBack, onRegisterProfile)

**Dependencias nuevas:** Ninguna.

**Estrategia de testing:**
JUnit 4 (ADR-18) + MockK + kotlinx-coroutines-test | Test unitario para `SubstituteExerciseUseCase` | Cobertura: todos los CAs

---

### Historias Relacionadas Consultadas

**Implementaciones similares analizadas:**
- HU-05: `startSession()` crea desde `plan_assignment`, `NotStartedExerciseRow` con botón "Sustituir", `original_exercise_id` introducido.
- HU-06: LEFT JOIN + COALESCE en queries de sesión, manejo de ejercicios no prescritos.
- HU-03: `getByModuleCodeNotInVersion()` como patrón de referencia para `getByModuleCodeNotInIds()`.

**Patrones de código reutilizados:**
- Query pattern de `ExerciseDao.getByModuleCodeNotInVersion()` adaptado para exclusión por IDs de sesión.
- Data class intermedia de query en archivo DAO (patrón establecido en HU-05 con `SessionExerciseForSubstitution`).
- `MutableSharedFlow<Boolean>(replay = 0)` para evento oneshot de navegación back (patrón HU-01).
- `database.withTransaction {}` para operaciones atómicas (patrón HU-01 `createProfile`).

**Mejores prácticas aplicadas:**
- Transacción atómica para validación + UPDATE en `substituteExercise()`.
- Preservación de `original_exercise_id` con Elvis operator para sustituciones dobles.
- Graceful exit con `null` check en `getSubstituteExerciseInfo()` → `navigateBack(true)`.
- 3 capas de validación para CA-07.06: UI (botón condicional), Repository (completedSets), Transacción (validación atómica).

---

### Tareas de Implementación

#### Fase 1: Data Layer — DAOs (2 modificaciones + 1 bug fix)

> Basado en Hito #1 del Análisis Arquitectónico

- [ ] **Fix query `getBySessionIdWithDetails()` en SessionExerciseDao** (AC: 07.05, 07.03) — **BUG FIX CRÍTICO**
  - [ ] Cambiar `INNER JOIN plan_assignment pa` → `LEFT JOIN plan_assignment pa`.
  - [ ] Cambiar `pa.sets,` → `COALESCE(pa.sets, 4) AS sets,`.
  - [ ] Cambiar `pa.reps,` → `COALESCE(pa.reps, '8-12') AS reps,`.
  - [ ] Sin este fix, el ejercicio sustituido desaparece de E1 tras la sustitución.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/dao/SessionExerciseDao.kt`

- [ ] **Agregar `SessionExerciseForSubstitution` + 3 nuevos métodos a SessionExerciseDao** (AC: 07.01, 07.05, 07.06)
  - [ ] Definir `SessionExerciseForSubstitution` como data class (no `@Entity`): `id: Long`, `sessionId: Long`, `exerciseId: Long`, `originalExerciseId: Long?`, `exerciseName: String`, `moduleCode: String`, `completedSets: Int`.
  - [ ] `@Query suspend fun getSessionExerciseForSubstitution(sessionExerciseId: Long): SessionExerciseForSubstitution?` — ver query completa en §5 §1 DAOs. `completedSets` determina si es sustituible (CA-07.06).
  - [ ] `@Query suspend fun getExerciseIdsForSession(sessionId: Long): List<Long>` — `SELECT exercise_id FROM session_exercise WHERE session_id = :sessionId`.
  - [ ] `@Query UPDATE suspend fun updateExerciseId(sessionExerciseId: Long, newExerciseId: Long, originalExerciseId: Long)` — UPDATE selectivo (no `@Update` de Room) para evitar sobrescribir `progression_classification`. Ver query en §5 §1 DAOs.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/dao/SessionExerciseDao.kt`

- [ ] **Agregar `getByModuleCodeNotInIds()` a ExerciseDao** (AC: 07.01, 07.02)
  - [ ] `@Query fun getByModuleCodeNotInIds(moduleCode: String, excludedExerciseIds: List<Long>): Flow<List<ExerciseWithDetails>>` — ver query completa en §5 §1 DAOs. `Flow` por consistencia con el patrón del DAO. Incluye ejercicios custom.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/dao/ExerciseDao.kt`

#### Fase 2: Domain Layer — Model y Use Case

> Basado en Hitos #2, #3 y #4 del Análisis Arquitectónico

- [ ] **Crear `SubstituteExerciseInfo`** (AC: 07.01, 07.02, 07.05)
  - [ ] Data class: `sessionExerciseId: Long`, `currentExerciseId: Long`, `currentExerciseName: String`, `moduleCode: String`, `sessionId: Long`.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/model/SubstituteExerciseInfo.kt`

- [ ] **Agregar 3 métodos a `SessionRepository`** (AC: 07.05, 07.06)
  - [ ] `suspend fun getSubstituteExerciseInfo(sessionExerciseId: Long): SubstituteExerciseInfo?`
  - [ ] `suspend fun getExerciseIdsForSession(sessionId: Long): List<Long>`
  - [ ] `suspend fun substituteExercise(sessionExerciseId: Long, newExerciseId: Long)`
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/repository/SessionRepository.kt`

- [ ] **Agregar 1 método a `ExerciseRepository`** (AC: 07.01)
  - [ ] `fun getEligibleSubstitutes(moduleCode: String, excludedExerciseIds: List<Long>): Flow<List<Exercise>>`
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/repository/ExerciseRepository.kt`

- [ ] **Crear `SubstituteExerciseUseCase`** (AC: 07.05, 07.06)
  - [ ] Wrapper fino: `suspend operator fun invoke(sessionExerciseId: Long, newExerciseId: Long)` delegando a `sessionRepository.substituteExercise()`.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/usecase/session/SubstituteExerciseUseCase.kt`
  - [ ] Test unitario: delegación caso exitoso, propaga `IllegalStateException`. Archivo: `...SubstituteExerciseUseCaseTest.kt`

#### Fase 3: Data Layer — Repository Implementations

> Basado en Hito #5 del Análisis Arquitectónico

- [ ] **Agregar 3 métodos a `SessionRepositoryImpl`** (AC: 07.05, 07.06)
  - [ ] `getSubstituteExerciseInfo()`: 3 pasos (getSessionExerciseForSubstitution → validar completedSets == 0 → mapear a SubstituteExerciseInfo). Ver lógica completa en §5 §5.
  - [ ] `getExerciseIdsForSession()`: delega a `sessionExerciseDao.getExerciseIdsForSession()`.
  - [ ] `substituteExercise()`: en `database.withTransaction {}`. Ver lógica completa en §5 §5 — incluye lógica de preservación `originalExerciseId = info.originalExerciseId ?: info.exerciseId`.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/repository/SessionRepositoryImpl.kt`

- [ ] **Agregar 1 método a `ExerciseRepositoryImpl`** (AC: 07.01)
  - [ ] `getEligibleSubstitutes()`: delega a `exerciseDao.getByModuleCodeNotInIds()` y mapea con `toDomainModel()` existente.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/repository/ExerciseRepositoryImpl.kt`

#### Fase 4: UI Layer — ViewModel y UiState

> Basado en Hito #6 del Análisis Arquitectónico

- [ ] **Crear `SubstituteExerciseViewModel`** (AC: 07.01, 07.02, 07.05, 07.06)
  - [ ] Definir `SubstituteExerciseUiState` y `SubstituteExerciseUiItem` dentro del mismo archivo (ver definiciones completas en §5 §6).
  - [ ] `@HiltViewModel`. Inyecta `SubstituteExerciseUseCase`, `SessionRepository`, `ExerciseRepository`, `SavedStateHandle`. `init` carga info + excluidos + observa Flow de sustitutos. Métodos: `onExerciseSelected()`, `onDismissDialog()`, `onConfirmSubstitution()` con protección `isSubstituting`.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/session/SubstituteExerciseViewModel.kt`

#### Fase 5: UI Layer — Pantalla E3

> Basado en Hito #7 del Análisis Arquitectónico

- [ ] **Crear `SubstituteExerciseScreen`** (AC: 07.01, 07.02, 07.05, 07.06)
  - [ ] `CenterAlignedTopAppBar` con ✕ cierre + título dual. Body con texto informativo (2 líneas), `LazyColumn` con `ListItem` 64dp (headlineContent + supportingContent "zona · equipo"), `TextButton` Cancelar. Diálogo `AlertDialog` condicional con corner 28dp, botón Confirmar disabled durante `isSubstituting`. Sin Bottom Nav. `LaunchedEffect` recolecta `navigateBack`. Ver especificaciones completas en §5 §7.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/session/SubstituteExerciseScreen.kt`

#### Fase 6: Navegación

> Basado en Hito #8 del Análisis Arquitectónico

- [ ] **Modificar `NavigationRoutes`** (AC: 07.05) — `+SUBSTITUTE_EXERCISE + helper` — Archivo: `...NavigationRoutes.kt`
- [ ] **Modificar `TensionNavHost`** — Wiring callback E1→E3, nuevo composable entry, extensión `showBottomBar` — Archivo: `...TensionNavHost.kt`

#### Fase 7: Recursos

> Basado en Hito #9 del Análisis Arquitectónico

- [ ] **Agregar ~8 strings para E3 a `strings.xml`** — ver lista completa en §5 §9 Recursos — Archivo: `app/src/main/res/values/strings.xml`

#### Fase N: QA y Deployment

- [ ] **Ejecutar Agente Peer Review** — MANUAL
- [ ] **Resolver incidentes del Peer Review** (condicional) — MANUAL
- [ ] **Crear Pull Request** — MANUAL
- [ ] **Ejecutar pipeline deployment DEV** — MANUAL
- [ ] **Diseñar set de pruebas manuales** — MANUAL
- [ ] **Ejecutar pruebas manuales** — MANUAL

---

**Vinculación CA → Fase de implementación:**
- CA-07.01 → Fases 1, 2, 3, 4, 5 (`getByModuleCodeNotInIds()` filtra módulo + excluye IDs sesión → ViewModel carga lista → E3 LazyColumn)
- CA-07.02 → Fase 1 (`WHERE module_code = :moduleCode` en query de sustitutos)
- CA-07.03 → Fase 3 (`substituteExercise()` solo hace UPDATE en `session_exercise`, `plan_assignment` intacto)
- CA-07.04 → Sin trabajo en HU-07 (ya implementado en HU-05: `startSession()` crea desde `plan_assignment`)
- CA-07.05 → Fases 1, 3, 4, 5, 6 (`updateExerciseId()` + `SubstituteExerciseUseCase` + ViewModel + E3 Screen + NavigationRoutes wiring)
- CA-07.06 → Fases 1, 3, 4, 5 (3 capas: UI `NotStartedExerciseRow` HU-05 + Repository `completedSets > 0 → null` + Transacción `completedSets == 0` validación atómica)
