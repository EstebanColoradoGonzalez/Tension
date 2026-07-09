## Análisis Arquitectónico

**Patrón Arquitectónico:** Clean Architecture (MVVM + Use Cases + Repository) — consistente con HU-01 a HU-06.

**Hallazgo Crítico — Bug en query de E1:**

La query `getBySessionIdWithDetails()` actualmente usa:
```sql
INNER JOIN plan_assignment pa ON pa.module_version_id = s.module_version_id
    AND pa.exercise_id = se.exercise_id
```
Un ejercicio sustituido (cuyo `exercise_id` es el sustituto, no el original del plan) **no existe** en `plan_assignment` para ese `module_version_id`. Resultado: el ejercicio sustituido **desaparece de E1** tras la sustitución — bug bloqueante que se corrige obligatoriamente en HU-07 con LEFT JOIN + COALESCE.

**Archivos nuevos (4):** `SubstituteExerciseInfo.kt`, `SubstituteExerciseUseCase.kt`, `SubstituteExerciseViewModel.kt`, `SubstituteExerciseScreen.kt`.

**Archivos modificados (9):** `SessionExerciseDao.kt`, `ExerciseDao.kt`, `SessionRepository.kt`, `SessionRepositoryImpl.kt`, `ExerciseRepository.kt`, `ExerciseRepositoryImpl.kt`, `NavigationRoutes.kt`, `TensionNavHost.kt`, `strings.xml`.

### Componentes afectados

#### 1. Data Layer — DAOs (Modificaciones)

Paquete: `data.local.dao`.

- **`SessionExerciseDao`** (Modificación Mayor):

  - **Corrección de `getBySessionIdWithDetails()`** (Bug fix crítico): Cambiar INNER JOIN → LEFT JOIN en `plan_assignment` + COALESCE para `sets` y `reps`:
    ```sql
    -- ANTES (INNER JOIN — ROMPE con sustitución):
    INNER JOIN plan_assignment pa ON pa.module_version_id = s.module_version_id
        AND pa.exercise_id = se.exercise_id

    -- DESPUÉS (LEFT JOIN + COALESCE — CORRECTO):
    LEFT JOIN plan_assignment pa ON pa.module_version_id = s.module_version_id
        AND pa.exercise_id = se.exercise_id
    ```
    Y en los campos SELECT:
    ```sql
    -- ANTES:          pa.sets, pa.reps,
    -- DESPUÉS: COALESCE(pa.sets, 4) AS sets, COALESCE(pa.reps, '8-12') AS reps,
    ```
    > Defaults `4` y `'8-12'` son los valores estándar del Plan de Entrenamiento. El COALESCE solo activa cuando `plan_assignment` no tiene fila para el ejercicio sustituido — un sustituto no prescrito hereda los parámetros estándar.

  - **`updateExerciseId(sessionExerciseId: Long, newExerciseId: Long, originalExerciseId: Long)`** (Nuevo): `@Query` UPDATE suspend. No usa `@Update` de Room (evita sobrescribir `progression_classification`):
    ```sql
    UPDATE session_exercise
    SET exercise_id = :newExerciseId,
        original_exercise_id = :originalExerciseId
    WHERE id = :sessionExerciseId
    ```

  - **`getExerciseIdsForSession(sessionId: Long): List<Long>`** (Nuevo): `@Query` suspend. Retorna IDs de ejercicios actualmente en la sesión, para excluirlos de la lista de sustitutos (CA-07.01):
    ```sql
    SELECT exercise_id FROM session_exercise WHERE session_id = :sessionId
    ```

  - **`getSessionExerciseForSubstitution(sessionExerciseId: Long): SessionExerciseForSubstitution?`** (Nuevo): `@Query` suspend. Obtiene datos necesarios para validar y ejecutar la sustitución:
    ```sql
    SELECT
        se.id,
        se.session_id AS sessionId,
        se.exercise_id AS exerciseId,
        se.original_exercise_id AS originalExerciseId,
        e.name AS exerciseName,
        e.module_code AS moduleCode,
        (SELECT COUNT(*) FROM exercise_set es WHERE es.session_exercise_id = se.id) AS completedSets
    FROM session_exercise se
    INNER JOIN exercise e ON se.exercise_id = e.id
    WHERE se.id = :sessionExerciseId
    ```

  - **`SessionExerciseForSubstitution`**: Data class intermedia (no `@Entity`), definida en `SessionExerciseDao.kt`. Campos: `id: Long`, `sessionId: Long`, `exerciseId: Long`, `originalExerciseId: Long?`, `exerciseName: String`, `moduleCode: String`, `completedSets: Int`.

- **`ExerciseDao`** (Modificación Menor): Agregar query para sustitutos elegibles (CA-07.01):

  - **`getByModuleCodeNotInIds(moduleCode: String, excludedExerciseIds: List<Long>): Flow<List<ExerciseWithDetails>>`** (Nuevo): Filtra por módulo y excluye los ejercicios ya en la sesión activa. `Flow` por consistencia con el patrón del DAO:
    ```sql
    SELECT e.id, e.name, e.module_code AS moduleCode, m.name AS moduleName,
        et.name AS equipmentTypeName, e.is_bodyweight AS isBodyweight,
        e.is_isometric AS isIsometric, e.is_to_technical_failure AS isToTechnicalFailure,
        e.is_custom AS isCustom, e.media_resource AS mediaResource,
        GROUP_CONCAT(mz.name, ', ') AS muscleZones
    FROM exercise e
    INNER JOIN module m ON e.module_code = m.code
    INNER JOIN equipment_type et ON e.equipment_type_id = et.id
    LEFT JOIN exercise_muscle_zone emz ON e.id = emz.exercise_id
    LEFT JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
    WHERE e.module_code = :moduleCode
      AND e.id NOT IN (:excludedExerciseIds)
    GROUP BY e.id ORDER BY e.name ASC
    ```
    > Diferencia vs `getByModuleCodeNotInVersion()`: excluye por IDs de sesión (runtime), no por plan (`plan_assignment`). Incluye ejercicios custom del mismo módulo (CA-03.10, CA-07.01).

#### 2. Domain Layer — Modelo Nuevo

Paquete: `domain.model`.

- **`SubstituteExerciseInfo`**: Data class — `sessionExerciseId: Long`, `currentExerciseId: Long`, `currentExerciseName: String`, `moduleCode: String`, `sessionId: Long`. Encapsula info necesaria para inicializar E3.

#### 3. Domain Layer — Repository Interfaces (Modificaciones)

Paquete: `domain.repository`.

- **`SessionRepository`** (Modificación): Agregar 3 métodos:
  - `suspend fun getSubstituteExerciseInfo(sessionExerciseId: Long): SubstituteExerciseInfo?` — One-shot (no Flow). Retorna `null` si el ejercicio no existe o tiene series registradas.
  - `suspend fun getExerciseIdsForSession(sessionId: Long): List<Long>` — Lista de IDs a excluir de E3.
  - `suspend fun substituteExercise(sessionExerciseId: Long, newExerciseId: Long)` — Transacción atómica. Lanza `IllegalStateException` si el ejercicio tiene series registradas.

- **`ExerciseRepository`** (Modificación): Agregar 1 método:
  - `fun getEligibleSubstitutes(moduleCode: String, excludedExerciseIds: List<Long>): Flow<List<Exercise>>` — Lista reactiva de sustitutos elegibles.

#### 4. Domain Layer — Use Case Nuevo

Paquete: `domain.usecase.session`.

- **`SubstituteExerciseUseCase`** (Nuevo): Wrapper fino que delega al repository.
  ```kotlin
  class SubstituteExerciseUseCase @Inject constructor(
      private val sessionRepository: SessionRepository,
  ) {
      suspend operator fun invoke(sessionExerciseId: Long, newExerciseId: Long) {
          sessionRepository.substituteExercise(sessionExerciseId, newExerciseId)
      }
  }
  ```
  > No se crea `GetSubstituteExerciseInfoUseCase` — el ViewModel inyecta directamente los 2 repositories para las operaciones de lectura (consistente con `ActiveSessionViewModel`), y usa `SubstituteExerciseUseCase` solo para la operación de escritura.

#### 5. Data Layer — Repository Implementations (Modificaciones)

Paquete: `data.repository`.

- **`SessionRepositoryImpl`** (Modificación): 3 nuevos métodos. No requiere DAOs adicionales — `sessionExerciseDao`, `exerciseSetDao` y `database` ya están inyectados.

  - **`getSubstituteExerciseInfo(sessionExerciseId: Long): SubstituteExerciseInfo?`:**
    1. `val info = sessionExerciseDao.getSessionExerciseForSubstitution(sessionExerciseId)`. Si `null` → retorna `null`.
    2. Si `info.completedSets > 0` → retorna `null` (CA-07.06).
    3. Mapea a `SubstituteExerciseInfo(sessionExerciseId, info.exerciseId, info.exerciseName, info.moduleCode, info.sessionId)`.

  - **`getExerciseIdsForSession(sessionId: Long): List<Long>`:** Delega a `sessionExerciseDao.getExerciseIdsForSession(sessionId)`.

  - **`substituteExercise(sessionExerciseId: Long, newExerciseId: Long)`:**
    En `database.withTransaction {}`:
    1. `val info = sessionExerciseDao.getSessionExerciseForSubstitution(sessionExerciseId)`. Si `null` → `throw IllegalStateException("Session exercise not found")`.
    2. Valida `info.completedSets == 0`. Si no → `throw IllegalStateException("Cannot substitute exercise with registered sets")`.
    3. `val originalExerciseId = info.originalExerciseId ?: info.exerciseId` — Preserva el original del plan ante **sustituciones dobles** (A→B→C mantiene A como `original_exercise_id`).
    4. `sessionExerciseDao.updateExerciseId(sessionExerciseId, newExerciseId, originalExerciseId)`.

    > La transacción es necesaria para evitar race conditions entre la validación de `completedSets` y el UPDATE.

- **`ExerciseRepositoryImpl`** (Modificación): 1 nuevo método.
  - **`getEligibleSubstitutes(moduleCode, excludedExerciseIds): Flow<List<Exercise>>`:** Delega a `exerciseDao.getByModuleCodeNotInIds()` y mapea con el `toDomainModel()` existente.

#### 6. UI Layer — Estado y ViewModel

Paquete: `ui.session`.

- **`SubstituteExerciseUiState`** + **`SubstituteExerciseUiItem`** (Nuevas — definidas dentro de `SubstituteExerciseViewModel.kt`):
  ```kotlin
  data class SubstituteExerciseUiState(
      val isLoading: Boolean = true,
      val originalExerciseName: String = "",
      val eligibleExercises: List<SubstituteExerciseUiItem> = emptyList(),
      val selectedExercise: SubstituteExerciseUiItem? = null,
      val showConfirmDialog: Boolean = false,
      val isSubstituting: Boolean = false,
  )

  data class SubstituteExerciseUiItem(
      val exerciseId: Long,
      val name: String,
      val muscleZones: String,
      val equipmentTypeName: String,
  )
  ```

- **`SubstituteExerciseViewModel`** (Nuevo): `@HiltViewModel`. Inyecta `SubstituteExerciseUseCase`, `SessionRepository`, `ExerciseRepository`, `SavedStateHandle`.
  - `sessionExerciseId` via `savedStateHandle.get<Long>("sessionExerciseId")`.
  - `_uiState: MutableStateFlow<SubstituteExerciseUiState>`.
  - `navigateBack: MutableSharedFlow<Boolean>(replay = 0)`.
  - **`init`:** (1) `getSubstituteExerciseInfo(sessionExerciseId)`. Si `null` → emite `navigateBack(true)` (graceful exit). (2) `getExerciseIdsForSession(info.sessionId)`. (3) Observa `exerciseRepository.getEligibleSubstitutes(info.moduleCode, excludedIds).collect { ... }`. Mapea `Exercise → SubstituteExerciseUiItem`. Actualiza `isLoading = false`, `originalExerciseName`, `eligibleExercises`.
  - **`onExerciseSelected(exercise)`:** Actualiza `selectedExercise = exercise`, `showConfirmDialog = true`.
  - **`onDismissDialog()`:** `showConfirmDialog = false`, `selectedExercise = null`.
  - **`onConfirmSubstitution()`:** Marca `isSubstituting = true`, ejecuta `substituteExerciseUseCase(sessionExerciseId, selectedExercise.exerciseId)`. Si éxito → emite `navigateBack(true)`. Si `IllegalStateException` → emite `navigateBack(true)` (graceful). `finally { isSubstituting = false }`.

#### 7. UI Layer — Pantalla E3

Paquete: `ui.session`.

- **`SubstituteExerciseScreen`** (Nuevo): Composable. Firma:
  ```kotlin
  @Composable
  fun SubstituteExerciseScreen(
      onNavigateBack: () -> Unit,
      viewModel: SubstituteExerciseViewModel = hiltViewModel(),
  )
  ```

  **Top Bar:** `CenterAlignedTopAppBar` (M3). `navigationIcon`: ✕ Close, `onClick = onNavigateBack`. `title`: `Column` → "Sustituir ejercicio" (Title Large) + "Reemplazar: [nombre]" (Title Small, On Surface Variant).

  **Body:** `Column` con padding 16 dp.
  - Texto informativo (2 líneas, Body Medium, On Surface Variant): "Selecciona un ejercicio del mismo módulo como reemplazo." + "La sustitución es puntual y no modifica el Plan." + `Spacer(16.dp)`.
  - **`LazyColumn`:** Lista de ejercicios elegibles. Cada fila: `ListItem` M3 (min height 64 dp). `headlineContent`: nombre (Title Medium). `supportingContent`: "zona(s) · equipo" (Body Medium, On Surface Variant — separador ` · ` entre zonas y equipo). `Modifier.clickable { viewModel.onExerciseSelected(exercise) }`. `HorizontalDivider` 1 dp Outline Variant entre filas.
  - **`TextButton`** "Cancelar": color Primary, margin top 16 dp. `onClick = onNavigateBack`.

  **Diálogo de confirmación:** `AlertDialog` M3, condicional (`showConfirmDialog == true`). `title`: "¿Sustituir [original] por [sustituto]?" (Title Medium). `text`: "Esta sustitución es solo para esta sesión." (Body Medium). `confirmButton`: `Button` "Confirmar", containerColor Primary, `enabled = !uiState.isSubstituting`, corner 28 dp. `dismissButton`: `TextButton` "Cancelar".

  **Sin Bottom Navigation** (Arquitectura Técnica §4.5.1). `LaunchedEffect` recolecta `navigateBack` → `onNavigateBack()`.

#### 8. Navegación (Modificaciones)

- **`NavigationRoutes`**: Agregar `const val SUBSTITUTE_EXERCISE = "substitute-exercise/{sessionExerciseId}"` y `fun substituteExerciseRoute(sessionExerciseId: Long) = "substitute-exercise/$sessionExerciseId"`.
- **`TensionNavHost`**: Wiring del callback E1→E3 (reemplazar TODO HU-07). Nuevo composable entry `SUBSTITUTE_EXERCISE`. Extender `showBottomBar`: agregar `&& !currentRoute.startsWith("substitute-exercise")`.

#### 9. Recursos

- **`strings.xml`**: Agregar ~8 strings para E3:
  ```xml
  <string name="substitute_exercise_title">Sustituir ejercicio</string>
  <string name="substitute_exercise_subtitle_format">Reemplazar: %1$s</string>
  <string name="substitute_exercise_info_line1">Selecciona un ejercicio del mismo módulo como reemplazo.</string>
  <string name="substitute_exercise_info_line2">La sustitución es puntual y no modifica el Plan.</string>
  <string name="substitute_exercise_dialog_title_format">¿Sustituir %1$s por %2$s?</string>
  <string name="substitute_exercise_dialog_text">Esta sustitución es solo para esta sesión.</string>
  <string name="substitute_exercise_confirm">Confirmar</string>
  <string name="substitute_exercise_cancel">Cancelar</string>
  ```

### Validación de Impacto

| Archivo | Estado verificado | Hallazgo |
|---|---|---|
| `SessionExerciseEntity.kt` | Completa con `original_exercise_id: Long? = null` (FK nullable) | Columna ya existe desde HU-05. No se necesita migración |
| `SessionExerciseDao.kt` | `getBySessionIdWithDetails()` usa INNER JOIN a `plan_assignment` | Bug: ejercicio sustituido desaparecería de E1. Se corrige a LEFT JOIN + COALESCE |
| `ExerciseDao.kt` | `getByModuleCodeNotInVersion()` existe como patrón de referencia | `getByModuleCodeNotInIds()` sigue el mismo patrón pero semántica de exclusión diferente |
| `SessionRepositoryImpl.kt` | 225 líneas, 8 parámetros inyectados (7 DAOs + TensionDatabase) | No requiere DAOs adicionales |
| `ExerciseRepositoryImpl.kt` | 106 líneas, `toDomainModel()` existente | Reutilizable para `getEligibleSubstitutes()` |
| `TensionNavHost.kt` | `onNavigateToSubstitute = { /* TODO: HU-07 */ }` | Stub confirmado. Se reemplaza |
| `ActiveSessionScreen.kt` | Botón "Sustituir" solo en `NotStartedExerciseRow`, pasa `exercise.sessionExerciseId` | CA-07.06 ya cumplido por diseño de E1. No se modifica |

**Impacto en el Flow reactivo de E1:** Al hacer UPDATE en `session_exercise`, Room invalida automáticamente `getBySessionIdWithDetails()`. El Flow re-emite la lista y el ejercicio sustituido aparece con su nuevo nombre en estado "No Iniciado". No se requiere refresh manual.

**Impacto en E2 (RegisterSetScreen):** E2 usa `getExerciseInfoForSet()` que busca por `session_exercise_id` (no cambia). Tras la sustitución, el JOIN a `exercise` retorna datos del sustituto. El LEFT JOIN + COALESCE ya implementado en HU-06 maneja el caso.

### Notas Técnicas

1. **CA-07.01 (lista de sustitutos elegibles) se resuelve con query por módulo + exclusión.** `WHERE module_code = :moduleCode AND id NOT IN (sessionExerciseIds)`. Incluye ejercicios de cualquier versión y custom.
2. **CA-07.03 y CA-07.04 (sustitución puntual) se cumplen por arquitectura.** `session_exercise` = runtime. `plan_assignment` = plan estable. `startSession()` (HU-05) siempre crea desde `plan_assignment` — no consulta sustituciones de sesiones anteriores.
3. **CA-07.05 (reemplazo efectivo) se cumple con UPDATE atómico.** `exercise_id = newExerciseId`. Series futuras se vincularán al nuevo ejercicio via `session_exercise_id` (que no cambió).
4. **CA-07.06 (bloqueo con series registradas) en 3 capas.** UI: botón solo en `NotStartedExerciseRow` (HU-05). Repository: `completedSets > 0 → null`. Transacción: validación atómica final.
5. **Sustitución doble (A→B→C).** `original_exercise_id` debe preservar A. Lógica: `val originalExerciseId = info.originalExerciseId ?: info.exerciseId`.
6. **UNIQUE constraint `(session_id, exercise_id)`.** La exclusión en E3 previene seleccionar un ejercicio ya presente. UNIQUE como safety net adicional.
7. **Relación con HU-24 (historial de sesiones).** `session_exercise.original_exercise_id` permite mostrar "Sustituyó a: [original]" (CA-24.04). Los datos están disponibles desde HU-07.

### Hitos de implementación

| # | Entregable | Dependencia |
|---|---|---|
| 1 | Data Layer — DAOs: `SessionExerciseDao` (fix query E1 + 3 nuevos métodos + `SessionExerciseForSubstitution`), `ExerciseDao` (+`getByModuleCodeNotInIds`) | — |
| 2 | Domain — Model: `SubstituteExerciseInfo` | — |
| 3 | Domain — Repository Interfaces: `SessionRepository` (+3 métodos), `ExerciseRepository` (+1 método) | Hito 2 |
| 4 | Domain — Use Case: `SubstituteExerciseUseCase` | Hito 3 |
| 5 | Data — Repository Impls: `SessionRepositoryImpl` (+3 métodos), `ExerciseRepositoryImpl` (+1 método) | Hito 1, 3 |
| 6 | UI — ViewModel: `SubstituteExerciseViewModel` (+ `SubstituteExerciseUiState`, `SubstituteExerciseUiItem`) | Hito 4 |
| 7 | UI — Screen: `SubstituteExerciseScreen` (E3 completo con lista + diálogo) | Hito 6 |
| 8 | Navegación: `NavigationRoutes` (+SUBSTITUTE_EXERCISE), `TensionNavHost` (wiring E1→E3 + showBottomBar exclusión) | Hito 7 |
| 9 | Recursos: `strings.xml` (~8 strings E3) | — (independiente) |

### Notas de auditoría

1. **CA-07.01 (lista de sustitutos) validado contra Wireframes E3 #5 y Especificación Visual §8 E3.** Query `getByModuleCodeNotInIds()`: filtra por módulo, excluye IDs de la sesión.
2. **CA-07.02 (restricción al mismo módulo) por `WHERE module_code = :moduleCode`.**
3. **CA-07.03 y CA-07.04 (sustitución puntual) por arquitectura.** UPDATE solo en `session_exercise`. `plan_assignment` intacto.
4. **CA-07.05 (reemplazo efectivo) con UPDATE atómico.** `exercise_id = newExerciseId` + registra `original_exercise_id`.
5. **CA-07.06 (bloqueo con series registradas) en 3 capas.** ADR D-02, Modelo de Datos §3.11 validación.
6. **RF16 (sustitución puntual sin modificar plan) cubierto integralmente.**

### Riesgos y mitigaciones

| Riesgo | Probabilidad | Impacto | Mitigación |
|---|---|---|---|
| Ejercicio sustituido desaparece de E1 (INNER JOIN bug) | Segura sin fix | Bloqueante | Fix obligatorio: INNER JOIN → LEFT JOIN + COALESCE en `getBySessionIdWithDetails()` |
| Race condition: registro de serie entre validación y UPDATE | Muy baja | Alto | `database.withTransaction {}` serializa. UI: botón "Sustituir" solo en estado "No Iniciado" |
| UNIQUE constraint violation al sustituir por ejercicio ya en sesión | Nula (por diseño) | Alto | Lista de E3 excluye `exerciseIds` de la sesión. UNIQUE constraint como safety net |
| Sustitución doble pierde `original_exercise_id` | Media sin lógica | Medio | `val originalExerciseId = info.originalExerciseId ?: info.exerciseId` preserva el original del plan |
| Lista de sustitutos vacía | Baja | Bajo | E3 muestra lista vacía. El ejecutante cancela y vuelve a E1. No requiere manejo especial |

### Verificación cruzada de CAs

| CA | Mecanismo de cumplimiento | Verificado contra |
|---|---|---|
| CA-07.01 | Query `getByModuleCodeNotInIds()`: filtra por módulo, excluye IDs de la sesión | Wireframes E3 #5, Especificación Visual §8 E3, RF16 |
| CA-07.02 | `WHERE module_code = :moduleCode` en la query de sustitutos | MDS §4 "mismo módulo" |
| CA-07.03 | UPDATE solo en `session_exercise` (runtime). `plan_assignment` intacto | MDS §4 "no modifica el Plan de Entrenamiento original" |
| CA-07.04 | `startSession()` (HU-05) crea desde `plan_assignment`, no consulta sustituciones | Modelo de Datos §3.11 |
| CA-07.05 | `updateExerciseId()` cambia `exercise_id` + registra `original_exercise_id` | Modelo de Datos §3.11 columnas |
| CA-07.06 | 3 capas: UI (botón condicional), Repository (`completedSets == 0`), Transacción (validación atómica) | ADR D-02, Modelo de Datos §3.11 |

## 5. Refinamiento Técnico

### Consideraciones Generales

**Nivel de complejidad:**
BAJA-MEDIA — No introduce entidades nuevas (`session_exercise` ya tiene `original_exercise_id` desde HU-05). Modifica 2 DAOs existentes (+4 métodos), extiende 2 Repository interfaces (+4 métodos), crea 1 modelo de dominio, 1 Use Case (wrapper fino), 1 pantalla E3 con ViewModel y UiState, 1 ruta de navegación, y ~8 strings. Complejidad reside en: (a) corrección del bug crítico en E1, (b) lógica de preservación de `original_exercise_id` ante sustituciones dobles, (c) transacción atómica con validación de `completedSets`.

**Riesgos técnicos conocidos:**
1. Ejercicio sustituido desaparece de E1 (bug INNER JOIN) — Fix obligatorio. Probabilidad: segura sin fix. Impacto: bloqueante.
2. Race condition: registro de serie entre validación y UPDATE — `database.withTransaction {}` + UI condicional.
3. Sustitución doble pierde `original_exercise_id` — Lógica: `info.originalExerciseId ?: info.exerciseId`.

**Patrones y convenciones del equipo (establecidos en HU-01—HU-06):**
- Mensajes de validación de dominio en inglés (§5.7)
- Data classes intermedias de query en archivos DAO (patrón establecido)
- Separador de zonas musculares: `, ` internamente, ` · ` en UI display
- `MutableSharedFlow<Boolean>(replay = 0)` para evento oneshot de navegación back

**Dependencias nuevas:** Ninguna.

**Estrategia de testing:** JUnit 4 + MockK + kotlinx-coroutines-test | Test para `SubstituteExerciseUseCase` | Cobertura: todos los CAs

### Código existente verificado (HU-01 a HU-06 implementados)

| Componente | Archivo | Estado |
| --- | --- | --- |
| `SessionExerciseEntity` | `data/local/entity/SessionExerciseEntity.kt` | Existe — `original_exercise_id: Long? = null`, UNIQUE `(session_id, exercise_id)`, FK RESTRICT. No se modifica |
| `SessionExerciseDao` | `data/local/dao/SessionExerciseDao.kt` | Existe — bug INNER JOIN confirmado. Se modifica: fix + 3 nuevos métodos + `SessionExerciseForSubstitution` |
| `ExerciseDao` | `data/local/dao/ExerciseDao.kt` | Existe — `getByModuleCodeNotInVersion()` como patrón. Se modifica: +`getByModuleCodeNotInIds()` |
| `SessionRepositoryImpl` | `data/repository/SessionRepositoryImpl.kt` | Existe — 225 líneas, 8 params (7 DAOs + TensionDatabase). No requiere DAOs adicionales |
| `ExerciseRepositoryImpl` | `data/repository/ExerciseRepositoryImpl.kt` | Existe — 106 líneas. Se modifica: +1 método, reutiliza `toDomainModel()` |
| `NavigationRoutes` | `ui/navigation/NavigationRoutes.kt` | Existe — 15 constantes + 5 helpers (post-HU-06). Se modifica: +SUBSTITUTE_EXERCISE + helper |
| `TensionNavHost` | `ui/navigation/TensionNavHost.kt` | Existe — TODO HU-07 stub confirmado. Se modifica: wiring + composable + showBottomBar |
| `ActiveSessionScreen` | `ui/session/ActiveSessionScreen.kt` | Existe — botón "Sustituir" solo en `NotStartedExerciseRow`. No se modifica |

**HUs futuras que dependen de artefactos de HU-07:**
- HU-09: Cerrar sesión → `session_exercise.exercise_id` apunta al sustituto. Opera sobre datos reales sin lógica condicional.
- HU-10: Clasificación progresión → compara datos del sustituto con historial del sustituto. Sin historial → "Sin Historial" (CA-10.07).
- HU-23: Historial de ejercicio → CA-23.03: consolida registros del ejercicio independientemente de si fue prescrito o sustituto.
- HU-24: Historial de sesiones → `session_exercise.original_exercise_id` permite mostrar "Sustituyó a: [original]" (CA-24.04).
