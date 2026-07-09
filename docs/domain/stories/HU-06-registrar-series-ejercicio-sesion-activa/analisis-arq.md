# Análisis Arquitectónico — HU-06

## Patrones y Decisiones Arquitectónicas

**Patrón Arquitectónico:** Clean Architecture (MVVM + Use Cases + Repository) — consistente con HU-01 a HU-05.

**Justificación:** HU-06 sigue el mismo flujo de datos del resto de la app: E2 → ViewModel → UseCase → Repository → DAOs → Room. La complejidad reside en la lógica de validación (3 campos, 3 reglas) y la transacción atómica de registro con asignación secuencial del número de serie.

## Componentes afectados

### 1. Data Layer — DAOs (Modificaciones)

Paquete: `data.local.dao`.

- **`ExerciseSetDao`** (Modificación Mayor): Actualmente stub vacío (creado en HU-05). Se convierte en DAO funcional:

  - `insert(set: ExerciseSetEntity): Long` — `@Insert`. Retorna el `id` generado.

  - `getNextSetNumber(sessionExerciseId: Long): Int` — `@Query` suspend. Calcula `COUNT(*) + 1` para asignar el número secuencial (CA-06.09). Si ya hay 4 series, retorna 5 — el repository lo valida:
    ```sql
    SELECT COUNT(*) + 1 FROM exercise_set
    WHERE session_exercise_id = :sessionExerciseId
    ```

  - `getLastWeightForExercise(exerciseId: Long): Double?` — `@Query` suspend. Query cross-session para precarga del último peso (CA-06.04, RNF04). Retorna `null` si sin historial:
    ```sql
    SELECT es.weight_kg
    FROM exercise_set es
    INNER JOIN session_exercise se ON es.session_exercise_id = se.id
    WHERE se.exercise_id = :exerciseId
    ORDER BY es.id DESC
    LIMIT 1
    ```
    > `ORDER BY es.id DESC` usa el autoincrement monotónicamente creciente — la última serie insertada para un ejercicio dado será siempre la de mayor `id`. Más eficiente que un JOIN adicional a `session.date`.

- **`SessionExerciseDao`** (Modificación Menor): Agregar query one-shot para poblar el formulario E2.

  - `getExerciseInfoForSet(sessionExerciseId: Long): SetExerciseInfo?` — `@Query` suspend. Query JOIN multi-tabla. Usa LEFT JOIN con `COALESCE(pa.sets, 4)` como protección ante ejercicios sustituidos (HU-07 futuro):
    ```sql
    SELECT
        se.exercise_id AS exerciseId,
        e.name AS exerciseName,
        e.is_bodyweight AS isBodyweight,
        e.is_isometric AS isIsometric,
        e.is_to_technical_failure AS isToTechnicalFailure,
        COALESCE(pa.sets, 4) AS totalSets
    FROM session_exercise se
    INNER JOIN exercise e ON se.exercise_id = e.id
    INNER JOIN session s ON se.session_id = s.id
    LEFT JOIN plan_assignment pa ON pa.module_version_id = s.module_version_id
        AND pa.exercise_id = se.exercise_id
    WHERE se.id = :sessionExerciseId
    ```

  - **`SetExerciseInfo`**: Data class intermedia (no `@Entity`), definida en `SessionExerciseDao.kt`. Campos: `exerciseId: Long`, `exerciseName: String`, `isBodyweight: Int`, `isIsometric: Int`, `isToTechnicalFailure: Int`, `totalSets: Int`.

- **`ExerciseProgressionDao`** (Modificación Menor): Agregar método para creación idempotente.

  - `insertIfNotExists(progression: ExerciseProgressionEntity)` — `@Insert(onConflict = OnConflictStrategy.IGNORE)`. Crea la fila `exercise_progression` con defaults al registrar la primera serie. La PK es `exercise_id` — el IGNORE garantiza idempotencia (Modelo de Datos §3.13: "Se crea una fila por ejercicio al registrar la primera serie del ejercicio").

### 2. Domain Layer — Modelo Nuevo

Paquete: `domain.model`.

- **`RegisterSetInfo`**: Data class que encapsula la información necesaria para poblar el formulario E2:
  - `sessionExerciseId: Long` — ID del ejercicio-en-sesión. Usado para el INSERT.
  - `exerciseId: Long` — ID del ejercicio base. Usado para consultar último peso.
  - `exerciseName: String` — Nombre mostrado en el Top Bar de E2.
  - `currentSetNumber: Int` — Próximo número de serie a registrar (1-4).
  - `totalSets: Int` — Siempre 4 en MVP. Viene del `COALESCE(pa.sets, 4)`.
  - `lastWeightKg: Double?` — Último peso utilizado para precarga (CA-06.04). `null` = sin historial → campo vacío. `0.0` = peso corporal/isométrico → campo fijo no editable.
  - `isBodyweight: Boolean` — Flag de peso corporal (CA-08.01).
  - `isIsometric: Boolean` — Flag de isométrico (CA-08.04, CA-08.05).
  - `isToTechnicalFailure: Boolean` — Flag de fallo técnico (informativo).

### 3. Domain Layer — Repository Interface (Modificación)

Paquete: `domain.repository`.

- **`SessionRepository`** (Modificación): Agregar 2 métodos al contrato existente:
  - `suspend fun getRegisterSetInfo(sessionExerciseId: Long): RegisterSetInfo?` — Consulta one-shot (no Flow) al abrir E2. Retorna `null` si `sessionExerciseId` no existe o ejercicio ya tiene 4 series.
  - `suspend fun registerSet(sessionExerciseId: Long, weightKg: Double, reps: Int, rir: Int)` — Transacción atómica: calcula número de serie, INSERT en `exercise_set`, INSERT idempotente en `exercise_progression`. Lanza `IllegalStateException` si el ejercicio ya tiene 4 series.

### 4. Domain Layer — Use Cases Nuevos

Paquete: `domain.usecase.session`.

- **`GetRegisterSetInfoUseCase`** (Nuevo): Wrapper puro de `SessionRepository.getRegisterSetInfo()`.
  ```kotlin
  class GetRegisterSetInfoUseCase @Inject constructor(
      private val sessionRepository: SessionRepository,
  ) {
      suspend operator fun invoke(sessionExerciseId: Long): RegisterSetInfo? =
          sessionRepository.getRegisterSetInfo(sessionExerciseId)
  }
  ```

- **`RegisterSetUseCase`** (Nuevo): Valida reglas de negocio y delega persistencia.
  ```kotlin
  class RegisterSetUseCase @Inject constructor(
      private val sessionRepository: SessionRepository,
  ) {
      suspend operator fun invoke(sessionExerciseId: Long, weightKg: Double, reps: Int, rir: Int) {
          require(weightKg >= 0) { "Weight must be >= 0" }
          require(reps >= 1) { "Reps must be >= 1" }
          require(rir in 0..5) { "RIR must be between 0 and 5" }
          sessionRepository.registerSet(sessionExerciseId, weightKg, reps, rir)
      }
  }
  ```
  > **Mensajes en inglés** (Arquitectura Técnica §5.7: mensajes de dominio son uso interno). El ViewModel mapea `IllegalArgumentException` a strings localizados en español para la UI. La validación en el Use Case es la última línea de defensa — la UI también valida en tiempo real (redundancia intencional: Clean Architecture principio).

### 5. Data Layer — Repository Implementation (Modificación Mayor)

Paquete: `data.repository`.

- **`SessionRepositoryImpl`** (Modificación Mayor): Implementar los 2 nuevos métodos. Agregar `exerciseSetDao: ExerciseSetDao` y `exerciseProgressionDao: ExerciseProgressionDao` al constructor (ambos providers ya existen en `DatabaseModule` desde HU-05).

  - **`getRegisterSetInfo(sessionExerciseId: Long): RegisterSetInfo?`:**
    1. `val info = sessionExerciseDao.getExerciseInfoForSet(sessionExerciseId)`. Si `null` → retorna `null`.
    2. `val nextSetNumber = exerciseSetDao.getNextSetNumber(sessionExerciseId)`. Si `> info.totalSets` → retorna `null` (ejercicio ya completado).
    3. Determina `lastWeightKg`: si `isBodyweight == 1` o `isIsometric == 1` → `0.0`. Si no → `exerciseSetDao.getLastWeightForExercise(info.exerciseId)`.
    4. Mapea a `RegisterSetInfo` convirtiendo flags `Int → Boolean`.

  - **`registerSet(sessionExerciseId: Long, weightKg: Double, reps: Int, rir: Int)`:**
    Ejecuta en `database.withTransaction {}`:
    1. `val nextSetNumber = exerciseSetDao.getNextSetNumber(sessionExerciseId)`.
    2. `val info = sessionExerciseDao.getExerciseInfoForSet(sessionExerciseId)`. Si `null` → lanza `IllegalStateException`.
    3. Valida `nextSetNumber <= info.totalSets`. Si no, lanza `IllegalStateException("Exercise already has maximum sets registered")`.
    4. INSERT: `exerciseSetDao.insert(ExerciseSetEntity(sessionExerciseId, nextSetNumber, weightKg, reps, rir))`.
    5. INSERT idempotente: `exerciseProgressionDao.insertIfNotExists(ExerciseProgressionEntity(exerciseId = info.exerciseId))` con defaults.

    > **¿Por qué la transacción es necesaria?** Los pasos 1-4 deben ser atómicos para evitar condiciones de carrera en la asignación de `nextSetNumber`. Sin transacción, dos llamadas rápidas podrían obtener el mismo número y violar el UNIQUE constraint `(session_exercise_id, set_number)`.

### 6. UI Layer — Estado y ViewModel

Paquete: `ui.session`.

- **`RegisterSetUiState`** (Nuevo):
  ```kotlin
  data class RegisterSetUiState(
      val isLoading: Boolean = true,
      val exerciseName: String = "",
      val currentSetNumber: Int = 1,
      val totalSets: Int = 4,
      val weightKg: String = "",
      val reps: String = "",
      val selectedRir: Int? = null,
      val isWeightEditable: Boolean = true,
      val isIsometric: Boolean = false,
      val isBodyweight: Boolean = false,
      val weightError: String? = null,
      val repsError: String? = null,
      val isSaving: Boolean = false,
  ) {
      val isConfirmEnabled: Boolean
          get() = selectedRir != null &&
              weightKg.isNotBlank() &&
              reps.isNotBlank() &&
              weightError == null &&
              repsError == null &&
              !isSaving
  }
  ```
  > `weightKg` y `reps` son `String` (no `Double`/`Int`) porque representan el texto del `OutlinedTextField`. El parseo a tipo numérico se realiza en `onConfirm()`, permitiendo al usuario ver exactamente lo que escribió.

- **`RegisterSetViewModel`** (Nuevo): `@HiltViewModel`. Inyecta `GetRegisterSetInfoUseCase`, `RegisterSetUseCase`, `SavedStateHandle`.
  - `sessionExerciseId` extraído via `savedStateHandle.get<Long>("sessionExerciseId")`.
  - `_uiState: MutableStateFlow<RegisterSetUiState>` / `uiState: StateFlow<RegisterSetUiState>`.
  - `_navigateBack: MutableSharedFlow<Boolean>(replay = 0)` / `navigateBack: SharedFlow<Boolean>` — la UI recolecta con `LaunchedEffect` y navega back al emitir `true`.
  - **`init`:** Llama a `getRegisterSetInfoUseCase(sessionExerciseId)`. Si válido:
    - `exerciseName`, `currentSetNumber`, `totalSets`, `isWeightEditable = !info.isBodyweight && !info.isIsometric`.
    - `weightKg`: si bodyweight/isometric → `"0"` (fijo); si `lastWeightKg != null` → `String.format("%.1f", lastWeightKg)`; si `null` → `""`.
  - **`onWeightChanged(value: String)`:** Actualiza `weightKg`. Valida: si parsea a `Double` y `< 0` → `weightError`. Si vacío o parseo intermedio (`"1."`) → `weightError = null`.
  - **`onRepsChanged(value: String)`:** Actualiza `reps`. Valida: si parsea a `Int` y `< 1` → `repsError` (mensaje distinto para isométrico).
  - **`onRirSelected(rir: Int)`:** Actualiza `selectedRir`. Sin validación — chips 0-5 garantizan rango por construcción.
  - **`onConfirm()`:** En `viewModelScope.launch`: parsea campos, marca `isSaving = true`, ejecuta `registerSetUseCase()`, emite `true` en `_navigateBack`. Si `IllegalStateException` (ejercicio ya completado) → también emite `true` (graceful: retorna a E1). `finally { isSaving = false }`.

### 7. UI Layer — Pantalla E2

Paquete: `ui.session`.

- **`RegisterSetScreen`** (Nuevo): Composable. Firma:
  ```kotlin
  @Composable
  fun RegisterSetScreen(
      onNavigateBack: () -> Unit,
      viewModel: RegisterSetViewModel = hiltViewModel(),
  )
  ```

  **Top Bar:** `CenterAlignedTopAppBar` (M3) (Arquitectura Técnica §4.6: "Center Aligned con cierre" para E2).
  - `navigationIcon`: `IconButton` con `Icons.Default.Close` (✕), tint On Surface. `onClick = onNavigateBack`.
  - `title`: `Column(horizontalAlignment = CenterHorizontally)`:
    - `Text(uiState.exerciseName)` — Title Large, On Surface.
    - `Text("Serie N de 4")` — Title Small, On Surface Variant. Formato: `register_set_title_format`.

  **Body:** `Column` con padding 16 dp, spacing vertical 16 dp.

  - **Campo "Peso (Kg)"** — `OutlinedTextField` con 3 variantes:
    - **Estándar** (`isWeightEditable == true`): `keyboardType = Decimal` (**`Decimal`** no `Number` — `weight_kg` es `REAL (Double)`, las cargas usan incrementos de 2.5 Kg). Label "Peso (Kg)", trailing "Kg" (Body Small), precargado con último peso. `isError = weightError != null`.
    - **Peso corporal** (`isBodyweight == true`): `enabled = false`, valor `"0"`, label "Peso (Kg) (Peso corporal)", fondo `Surface Container Highest (#EDE0D5)`, opacity 0.5.
    - **Isométrico** (`isIsometric == true`): `enabled = false`, valor `"0"`, label "Peso (Kg) (Isométrico)", mismos estilos disabled.

  - **Campo "Repeticiones" / "Segundos sostenidos"** — `OutlinedTextField` con 2 variantes:
    - **Estándar y peso corporal** (`isIsometric == false`): label "Repeticiones", `keyboardType = Number`, trailing "reps".
    - **Isométrico** (`isIsometric == true`): label "Segundos sostenidos", trailing "seg", `supportingText = "(Referencia: 30–45 seg)"` siempre visible (CA-08.05).

  - **Selector RIR** — `Row` horizontal con spacing 8 dp, 6 chips circulares `Box` 48×48 dp (`CircleShape`, RNF06):
    - Seleccionado: fondo `Primary (#8B1A1A)`, texto `On Primary (#FFFFFF)`.
    - No seleccionado: fondo `Surface Container (#F8EBE0)`, borde `Outline`, texto `On Surface`.
    - Single select. Sin precarga. `semantics { contentDescription = "RIR $rir" }` para accesibilidad.

  - **Botón "Confirmar":** `Button` full width, `containerColor = Primary`, height min 48 dp (RNF06). `enabled = isConfirmEnabled`. Margin top 24 dp.
  - **"Cancelar":** `TextButton` centrado, color Primary. `onClick = onNavigateBack`.
  - **Sin Bottom Navigation** (Arquitectura Técnica §4.5.1).

  **Navegación de retorno:** `LaunchedEffect(Unit) { viewModel.navigateBack.collect { if (it) onNavigateBack() } }`.

  > **RNF02 (máximo 3 toques):** Con peso precargado: (1) reps, (2) chip RIR, (3) Confirmar. Para primera sesión (sin precarga): 4 toques — aceptable dado la ausencia de historial.

### 8. Navegación (Modificaciones)

- **`NavigationRoutes`**: Agregar `const val REGISTER_SET = "register-set/{sessionExerciseId}"` y `fun registerSetRoute(sessionExerciseId: Long) = "register-set/$sessionExerciseId"`.
- **`TensionNavHost`**: Wiring del callback en `ACTIVE_SESSION` (reemplazar TODO HU-06). Nuevo composable entry `REGISTER_SET`. Extender `showBottomBar`:
  ```kotlin
  val showBottomBar = currentRoute != null &&
      currentRoute != NavigationRoutes.REGISTER &&
      !currentRoute.startsWith("active-session") &&
      !currentRoute.startsWith("register-set") &&
      !(currentRoute.startsWith("exercise-detail") &&
          navController.previousBackStackEntry?.destination?.route
              ?.startsWith("active-session") == true)
  ```

### 9. Recursos

- **`strings.xml`**: Agregar ~16 strings para E2:
  ```xml
  <string name="register_set_title_format">Serie %1$d de %2$d</string>
  <string name="register_set_weight_label">Peso (Kg)</string>
  <string name="register_set_weight_bodyweight_label">Peso (Kg) (Peso corporal)</string>
  <string name="register_set_weight_isometric_label">Peso (Kg) (Isométrico)</string>
  <string name="register_set_weight_suffix">Kg</string>
  <string name="register_set_reps_label">Repeticiones</string>
  <string name="register_set_reps_suffix">reps</string>
  <string name="register_set_seconds_label">Segundos sostenidos</string>
  <string name="register_set_seconds_suffix">seg</string>
  <string name="register_set_seconds_reference">(Referencia: 30–45 seg)</string>
  <string name="register_set_rir_label">RIR</string>
  <string name="register_set_confirm">Confirmar</string>
  <string name="register_set_cancel">Cancelar</string>
  <string name="error_weight_negative">El peso debe ser ≥ 0 Kg</string>
  <string name="error_reps_min">Las repeticiones deben ser ≥ 1</string>
  <string name="error_seconds_min">La duración debe ser ≥ 1 segundo</string>
  ```

## Validación de Impacto

**Código verificado antes de proponer:**

| Archivo | Estado verificado | Hallazgo |
|---|---|---|
| `ExerciseSetDao.kt` | Stub vacío — solo `@Dao interface ExerciseSetDao` | Confirma necesidad de 3 métodos nuevos |
| `ExerciseSetEntity.kt` | Completa con 6 columnas. UNIQUE(session_exercise_id, set_number). FK CASCADE. | Columnas coinciden con INSERT propuesto |
| `SessionExerciseDao.kt` | `getBySessionId()` y `getBySessionIdWithDetails()` existen | No hay método para info individual. Se necesita `getExerciseInfoForSet()` |
| `ExerciseProgressionDao.kt` | Tiene `insert()` y `update()`. `insert()` usa `@Insert` default (ABORT on conflict) | No sirve para idempotencia. Se necesita `insertIfNotExists()` con IGNORE |
| `SessionRepositoryImpl.kt` | 164 líneas, inyecta 6 parámetros (5 DAOs + TensionDatabase) | No inyecta `ExerciseSetDao` ni `ExerciseProgressionDao`. Se agregan al constructor |
| `SessionRepository.kt` | 6 métodos actuales | Ninguno cubre registro de series. Se agregan 2 |
| `NavigationRoutes.kt` | 8 rutas, sin `REGISTER_SET` | Se agrega |
| `TensionNavHost.kt` | `onNavigateToRegisterSet = { /* TODO: HU-06 */ }` | Stub confirmado. Se reemplaza |
| `showBottomBar` | Excluye `REGISTER` y `active-session` | No excluye `register-set`. Se agrega |
| `ActiveSessionScreen.kt` | "Registrar" button pasa `exercise.sessionExerciseId` via `onNavigateToRegisterSet` | Ya pasa `sessionExerciseId` correcto |
| `DatabaseModule.kt` | `provideExerciseSetDao()` y `provideExerciseProgressionDao()` ya existen | No se necesitan nuevos providers |

**Impacto en el Flow reactivo de E1:** Al insertar una serie en `exercise_set`, Room invalida automáticamente el query `getBySessionIdWithDetails()` en E1 (que hace `COUNT(exercise_set)` como subquery). El Flow re-emite la lista actualizada y `completedSets` refleja la nueva serie. El estado del ejercicio cambia reactivamente: 0 → `NOT_STARTED`, 1-3 → `IN_PROGRESS`, 4 → `COMPLETED`. No se requiere ningún refresh manual.

## Notas Técnicas

1. **CA-06.08 (metadatos automáticos) se cumple por contexto relacional.** La cadena `exercise_set → session_exercise → session → module_version` proporciona: Fecha (`session.date`), Módulo (`module_version.module_code`), Versión (`module_version.version_number`), Ejercicio (`session_exercise.exercise_id`), Número de serie (`exercise_set.set_number`). El ejecutante solo ingresa: peso, reps, RIR.
2. **CA-06.10 (orden libre) se cumple por diseño de E1.** Cada ejercicio tiene su propio botón "Registrar" independiente. El `set_number` es secuencial *dentro de cada ejercicio*, no a nivel de sesión.
3. **CA-06.11 (vinculación al ejercicio sustituto) se cumple automáticamente.** La serie se inserta con referencia al `session_exercise_id`, que apunta al `exercise_id` que realmente se ejecutó. Si hubo sustitución (HU-07), `session_exercise.exercise_id` ya contiene el sustituto.
4. **CA-06.12 (estado visual) ya fue implementado en HU-05.** Los 3 estados se derivan del `completedSets` count. Al insertar una serie, Room invalida el Flow y E1 re-renderiza. No se requiere trabajo adicional en HU-06.
5. **CA-06.13 (preservación ante cierre) se cumple por Room.** Cada INSERT se persiste en SQLite de forma inmediata tras el `withTransaction` commit. Si la app se cierra durante la transacción, Room hace rollback — no hay pérdida de datos parciales.
6. **Relación con HU-08:** Los CAs de HU-08 referenciados en E2 (CA-08.01, CA-08.04, CA-08.05, CA-08.08) se cubren en este diseño. CA-08.02, CA-08.03, CA-08.06, CA-08.07 (progresión/doble umbral) son responsabilidad de HU-10/HU-11.
7. **Impacto en HU-07 (sustitución).** El LEFT JOIN en `getExerciseInfoForSet()` con `COALESCE(pa.sets, 4)` prepara E2 para ejercicios sustituidos donde `exercise_id` no existe en `plan_assignment`.
8. **`getLastWeightForExercise()` es cross-session.** Busca el último peso en TODAS las sesiones anteriores. Permite que si un ejercicio tuvo 60 Kg en la sesión anterior, al iniciar una nueva sesión el peso se precargue correctamente.
9. **Decisión diferida: precarga vs. carga prescrita post-descarga (HU-17).** La query retorna el "último peso utilizado" sin filtrar sesiones de descarga. Tras un ciclo de descarga, E2 precargaría el peso de descarga mientras E1 muestra la carga prescrita de reinicio. Esta discrepancia es **intencionada** según CA-06.04. Si HU-17 requiere cambio, se modifica la query sin cambiar la interfaz.
10. **`keyboardType` para campo Peso: `Decimal` (no `Number`).** La Especificación Visual §8 E2 usa "keyboardType: Number" genéricamente. Sin embargo, `weight_kg` es `REAL (Double)` y las cargas usan incrementos de 2.5 Kg, requiriendo entrada decimal. Se implementa con `KeyboardType.Decimal`. Esta es una refinación técnica del término genérico de la especificación.

## Hitos de implementación

| # | Entregable | Dependencia |
|---|---|---|
| 1 | Data Layer — DAOs: `ExerciseSetDao` (+3 métodos), `SessionExerciseDao` (+`getExerciseInfoForSet` + `SetExerciseInfo`), `ExerciseProgressionDao` (+`insertIfNotExists`) | — |
| 2 | Domain — Model: `RegisterSetInfo` | — |
| 3 | Domain — Repository Interface: `SessionRepository` (+2 métodos) | Hito 2 |
| 4 | Domain — Use Cases: `GetRegisterSetInfoUseCase`, `RegisterSetUseCase` | Hito 3 |
| 5 | Data — Repository Impl: `SessionRepositoryImpl` (+2 métodos, +2 DAOs inyectados) | Hito 1, 3 |
| 6 | UI — State + ViewModel: `RegisterSetUiState`, `RegisterSetViewModel` | Hito 4 |
| 7 | UI — Screen: `RegisterSetScreen` (E2 completo con variantes estándar/bodyweight/isométrico) | Hito 6 |
| 8 | Navegación: `NavigationRoutes` (+REGISTER_SET), `TensionNavHost` (wiring E1→E2 + showBottomBar exclusión) | Hito 7 |
| 9 | Recursos: `strings.xml` (~16 strings E2) | — (independiente) |

## Notas de auditoría

1. **CA-06.01 (captura de 3 datos) se resuelve con 3 campos en E2.** Los 3 son obligatorios — el botón "Confirmar" se deshabilita hasta que todos tengan valor válido.
2. **CA-06.02 (máximo 3 toques) se cumple con precarga.** Con peso precargado: (1) reps, (2) RIR chip, (3) Confirmar. Sin precarga (primera sesión): 4 toques, aceptable.
3. **CA-06.03 (teclado numérico):** Peso: `KeyboardType.Decimal`. Repeticiones/Segundos: `KeyboardType.Number`. RIR: chips de selección directa.
4. **CA-06.04 (precarga último peso):** `getLastWeightForExercise()` cross-session. `null` si sin historial → campo vacío. Para bodyweight/isometric → valor fijo `"0"`.
5. **CA-06.05 a CA-06.07 (validaciones) cubiertos con doble capa.** UI (feedback inmediato en ViewModel) + UseCase `require()` (última línea de defensa). Room no soporta CHECK constraints nativamente — UNIQUE y FK constraints proporcionan integridad estructural.
6. **CA-06.08 (metadatos automáticos) por estructura relacional.** Ver Nota Técnica 1.
7. **CA-06.09 (secuencia 1-2-3-4) con `COUNT(*) + 1`.** Dentro de transacción para evitar race conditions. UNIQUE constraint como safety net.
8. **CA-06.10 (orden libre) ya implementado en HU-05.** No requiere trabajo adicional.
9. **CA-06.11 (vinculación al sustituto) cumplido automáticamente.** Ver Nota Técnica 3.
10. **CA-06.12 (estado visual) ya implementado en HU-05.** Room invalida Flow reactivamente al INSERT en `exercise_set`. No requiere trabajo adicional.
11. **CA-06.13 (preservación ante cierre) por Room.** INSERT atómico en `withTransaction`. Crash recovery de B1 (HU-05) detecta sesión `IN_PROGRESS`.
12. **RNF02 (3 toques) via precarga + chips.**
13. **RNF03 (teclado numérico) via `keyboardType`.**
14. **RNF04 (precarga peso) via `getLastWeightForExercise()`.** Cross-session, por `exercise_id`.
15. **RNF10 (crash recovery) cubierto por Room + HU-05 B1 Card "Reanudar".**
16. **RNF12 (validación rangos) en 2 capas:** UI (validación RT en ViewModel) → UseCase (`require()`).

## Riesgos y mitigaciones

| Riesgo | Probabilidad | Impacto | Mitigación |
|---|---|---|---|
| Condición de carrera en asignación de `set_number` (doble-tap rápido) | Baja | Alto | `database.withTransaction {}` serializa accesos. `UNIQUE(session_exercise_id, set_number)` como safety net. UI deshabilita botón via `isSaving = true` |
| LEFT JOIN `plan_assignment` falla con ejercicio sustituido (HU-07 futuro) | Nula en HU-06 | Alto en HU-07 | LEFT JOIN + `COALESCE(pa.sets, 4)` protege |
| Registro de serie 5+ por error de lógica | Baja | Alto | Validación en repository (`nextSetNumber <= totalSets`). UI no muestra "Registrar" en ejercicios COMPLETED |
| Pérdida de datos entre confirm y persist (crash) | Baja | Medio | Room transacción atómica: o se persiste todo o nada |

## Verificación cruzada de CAs

| CA | Mecanismo de cumplimiento | Verificado contra |
|---|---|---|
| CA-06.01 | 3 campos obligatorios en E2: Peso, Reps/Segundos, RIR | Wireframes E2, Especificación Visual §8 E2, RF13 |
| CA-06.02 | Precarga peso + chips RIR = 3 toques (reps, chip, confirmar) | RNF02, Wireframes E2 §Comportamiento #7 |
| CA-06.03 | `keyboardType = Decimal/Number` en OutlinedTextField | RNF03, Especificación Visual §8 E2 |
| CA-06.04 | `getLastWeightForExercise()` cross-session | RNF04, Wireframes E2 #4 "Precargado con último peso" |
| CA-06.05 | `require(weightKg >= 0)` + validación RT ViewModel | RNF12, Modelo de Datos §3.12 |
| CA-06.06 | `require(reps >= 1)` + validación RT ViewModel | RNF12, Modelo de Datos §3.12 |
| CA-06.07 | Chips 0-5 (imposible seleccionar fuera de rango) + `require(rir in 0..5)` | RNF12, Modelo de Datos §3.12 |
| CA-06.08 | Relación `exercise_set → session_exercise → session → module_version` | MDS §5.A.5, RF14 |
| CA-06.09 | `COUNT(*) + 1` en transacción + UNIQUE constraint | Modelo de Datos §3.12, RF15 |
| CA-06.10 | Botón "Registrar" independiente por ejercicio en E1 (HU-05) | Wireframes E1 #7, MDS §5.A.3 |
| CA-06.11 | `session_exercise.exercise_id` = ejercicio efectivo tras sustitución (HU-07) | Modelo de Datos §3.11, MDS §5.A.4 |
| CA-06.12 | `completedSets` derivado via COUNT subquery en E1 (HU-05 existente) | Modelo de Datos §3.11, RF17 |
| CA-06.13 | Room INSERT atómico + crash recovery B1 | RNF10, Modelo de Datos §3.12 |
