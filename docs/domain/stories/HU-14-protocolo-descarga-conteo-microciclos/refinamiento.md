# Refinamiento Técnico (Developer)
**Autor**: Esteban Colorado González | **Fecha**: 2026-02-17

---

## Consideraciones Generales

**Basado en análisis arquitectónico:**
Análisis Arquitectónico de HU-14 con 14 decisiones, 11 componentes nuevos + 22 modificados, y 9 notas técnicas. Patrón MVVM con orquestación de dominio en múltiples capas. HU-14 es la historia más transversal del sistema.

**Nivel de complejidad:**
ALTA — HU-14 toca 26 archivos (11 nuevos + 22 modificados referenciando componentes existentes) con lógica distribuida en todas las capas. La transversalidad es crítica: modifica `startSession()`, `closeSession()`, `evaluateProgression()`, `advanceRotation()`, y `getNextModuleVersionId()` — los 5 flujos más importantes del sistema. `DeloadLoadRule` es simple (2 funciones puras), pero la orquestación en `SessionRepositoryImpl` es delicada: 2 guards en `evaluateProgression()`, bloque de finalización (~30 líneas) en `closeSession()`, 2 puntos de resolución de versiones congeladas. Tests unitarios: ~15 escenarios (`DeloadLoadRuleTest`) + 3 escenarios nuevos (`RotationResolverTest`).

**Riesgos técnicos conocidos:**

1. **Guard `prescribeLoad()` durante deload:** `DoubleThresholdRule.prescribeLoad()` en L393-402 de `evaluateProgression()` sobreescribe `prescribed_load_kg` durante deload. Sin guard, la carga de reinicio post-deload se pierde.
2. **Guard alertas PLATEAU durante deload:** L412-430 crea/resuelve alertas PLATEAU durante deload. Guard: `if (!isDeloadSession) { /* plateau alert logic */ }`.
3. **`getLastWeightForExercise()` sin filtro deload:** Post-deload, E2 precargaría la carga de descarga (60%) en vez de la pre-descarga. Fix: agregar `s.deload_id IS NULL`.
4. **Resolución de versiones congeladas en 2 puntos:** `getNextModuleVersionId()` (Flow) y `GetNextSessionInfoUseCase` (Flow) deben ser consistentes — ambos usan `Deload` domain model como fuente.
5. **Finalización de deload debe ser transaccional:** El bloque (complete + reset loads + transition IN_DELOAD→IN_PROGRESSION + resolver alertas) dentro del mismo `database.withTransaction {}`.

**Dependencias nuevas:** Ninguna.

**Estrategia de testing:** JUnit 4 para reglas puras. 2 archivos: `DeloadLoadRuleTest` (~15 escenarios) + `RotationResolverTest` (+3 escenarios al existente). DAOs, guards de `evaluateProgression()`, y pantalla I1 se validan manualmente o instrumentados en Fase N.

---

## Historias Relacionadas Consultadas

**Implementaciones similares analizadas:**
HU-09 (RotationResolver) — base para conteo de microciclos y modificación de `advanceRotation()`. HU-06 (exercise_set) — patrón `getLastWeightForExercise()` para derivar carga pre-descarga. HU-11 (prescribed_load_kg) — patrón de carga prescriptiva y `loadIncrementKg`.

**Patrones de código reutilizados:**
- Transacción atómica `database.withTransaction {}` (patrón HU-01 `createProfile`)
- Sealed interface para estados (patrón HU-01 `UiState`)
- Domain model mapeado desde Entity (patrón ADR-05 layer violation prevention)
- `flatMapLatest` para combinar Flows (patrón HU-05 `GetNextSessionInfoUseCase`)
- Guards condicionales en `evaluateProgression()` (patrón ADR-06 rule engine)

**Mejores prácticas aplicadas:**
- Reglas puras en `domain/rules/` (ADR-06)
- Domain model como capa de abstracción sobre Entity (ADR-05)
- FK lógica sin constraint cuando tabla destino no existe aún (patrón Room)
- `fallbackToDestructiveMigration()` aceptable en pre-release
- Redondeo protector hacia abajo para seguridad del ejecutante

---

## Tareas de Implementación

### Fase 1: Domain — DeloadLoadRule + tests

- [ ] **Crear `DeloadLoadRule`** (CA-14.02, 14.05, 14.06)
  - [ ] Crear archivo: `domain/rules/DeloadLoadRule.kt`
  - [ ] `object DeloadLoadRule`
  - [ ] `private const val DELOAD_PERCENTAGE = 0.60`, `private const val RESET_PERCENTAGE = 0.90`
  - [ ] `fun calculateDeloadLoad(lastWeightKg: Double, loadIncrementKg: Double): Double`
    - Guard: `if (loadIncrementKg <= 0.0 || lastWeightKg <= 0.0) return 0.0`
    - Fórmula: `floor(lastWeightKg * 0.60 / loadIncrementKg) * loadIncrementKg`
  - [ ] `fun calculateResetLoad(preDeloadWeightKg: Double, loadIncrementKg: Double): Double`
    - Guard: `if (loadIncrementKg <= 0.0 || preDeloadWeightKg <= 0.0) return 0.0`
    - Fórmula: `floor(preDeloadWeightKg * 0.90 / loadIncrementKg) * loadIncrementKg`
  - [ ] `import kotlin.math.floor`

- [ ] **Crear `DeloadLoadRuleTest`** (~15 escenarios)
  - [ ] Crear archivo: `test/.../domain/rules/DeloadLoadRuleTest.kt`
  - [ ] Escenario 1: 60% de 60 Kg, incr=2.5 → 35.0 Kg
  - [ ] Escenario 2: 60% de 55 Kg, incr=2.5 → 32.5 Kg
  - [ ] Escenario 3: 60% de 100 Kg, incr=5.0 → 60.0 Kg
  - [ ] Escenario 4: 60% de 80 Kg, incr=5.0 → 45.0 Kg (80×0.6=48 → floor(48/5)×5 = 45)
  - [ ] Escenario 5: 60% de 2.5 Kg, incr=2.5 → 0.0 Kg (2.5×0.6=1.5 → floor(1.5/2.5)×2.5 = 0.0)
  - [ ] Escenario 6: 60% de 0 Kg, incr=2.5 → 0.0 Kg (guard)
  - [ ] Escenario 7: 90% de 60 Kg, incr=2.5 → 52.5 Kg (60×0.9=54 → floor(54/2.5)×2.5 = 52.5)
  - [ ] Escenario 8: 90% de 55 Kg, incr=2.5 → 47.5 Kg (55×0.9=49.5 → floor(49.5/2.5)×2.5 = 47.5)
  - [ ] Escenario 9: 90% de 100 Kg, incr=5.0 → 90.0 Kg
  - [ ] Escenario 10: 90% de 80 Kg, incr=5.0 → 70.0 Kg (80×0.9=72 → floor(72/5)×5 = 70)
  - [ ] Escenario 11: 90% de 0 Kg, incr=2.5 → 0.0 Kg (guard)
  - [ ] Escenario 12: 60% de 62.5 Kg, incr=2.5 → 37.5 Kg (exacto)
  - [ ] Escenario 13: 90% de 62.5 Kg, incr=2.5 → 55.0 Kg (62.5×0.9=56.25 → floor(56.25/2.5)×2.5 = 55.0)
  - [ ] Escenario 14: incr=0.0 → 0.0 (guard)
  - [ ] Escenario 15: 90% incr=0.0 → 0.0 (guard)

### Fase 2: Data — DeloadEntity + DeloadDao + migration

- [ ] **Crear `DeloadEntity`** (CA-14.01, 14.03, 14.04)
  - [ ] Crear archivo: `data/local/entity/DeloadEntity.kt`
  - [ ] `@Entity(tableName = "deload", indices = [Index(value = ["status"])])`
  - [ ] Columnas (Modelo de Datos §3.15):
    - `@PrimaryKey(autoGenerate = true) val id: Long = 0`
    - `@ColumnInfo(name = "status") val status: String = "ACTIVE"`
    - `@ColumnInfo(name = "activation_date") val activationDate: String`
    - `@ColumnInfo(name = "completion_date") val completionDate: String? = null`
    - `@ColumnInfo(name = "frozen_version_module_a") val frozenVersionModuleA: Int`
    - `@ColumnInfo(name = "frozen_version_module_b") val frozenVersionModuleB: Int`
    - `@ColumnInfo(name = "frozen_version_module_c") val frozenVersionModuleC: Int`

- [ ] **Crear `DeloadDao`** (CA-14.01, 14.03, 14.07)
  - [ ] Crear archivo: `data/local/dao/DeloadDao.kt`
  - [ ] `@Dao interface DeloadDao`
  - [ ] `@Insert suspend fun insert(deload: DeloadEntity): Long`
  - [ ] `@Query("SELECT * FROM deload WHERE status = 'ACTIVE' LIMIT 1") fun getActiveDeload(): Flow<DeloadEntity?>`
  - [ ] `@Query("SELECT * FROM deload WHERE status = 'ACTIVE' LIMIT 1") suspend fun getActiveDeloadOnce(): DeloadEntity?`
  - [ ] `@Query("UPDATE deload SET status = 'COMPLETED', completion_date = :completionDate WHERE id = :deloadId") suspend fun complete(deloadId: Long, completionDate: String)`
  - [ ] `@Query("SELECT * FROM deload WHERE id = :deloadId") suspend fun getById(deloadId: Long): DeloadEntity?`
  - [ ] `@Query("SELECT * FROM deload WHERE status = 'COMPLETED' ORDER BY completion_date DESC LIMIT 1") suspend fun getLastCompletedDeload(): DeloadEntity?`

- [ ] **Modificar `TensionDatabase`** (CA-14.01)
  - [ ] Agregar `DeloadEntity::class` al array `entities` (16ª entidad)
  - [ ] Bump `version = 5` a `version = 6`
  - [ ] Agregar `abstract fun deloadDao(): DeloadDao`
  - [ ] **Nota:** `fallbackToDestructiveMigration()` activo en `DatabaseModule.kt` → migration automática destructiva. Aceptable en pre-release.

- [ ] **Modificar `DatabaseModule`** (CA-14.01)
  - [ ] Agregar:
    ```kotlin
    @Provides
    fun provideDeloadDao(database: TensionDatabase): DeloadDao = database.deloadDao()
    ```

### Fase 3: Domain — RotationResolver modificación + tests

- [ ] **Modificar `RotationResolver.advanceRotation()`** (CA-14.04, 14.13)
  - [ ] Cambiar firma a `fun advanceRotation(current: RotationState, isDeload: Boolean = false): RotationState`
  - [ ] Modificar bloque `else` (posición == 6):
    ```kotlin
    else {
        if (isDeload) {
            current.copy(
                microcyclePosition = 1,
                microcycleCount = current.microcycleCount + 1,
                // versiones NO avanzan durante deload
            )
        } else {
            current.copy(
                microcyclePosition = 1,
                currentVersionModuleA = (current.currentVersionModuleA % 3) + 1,
                currentVersionModuleB = (current.currentVersionModuleB % 3) + 1,
                currentVersionModuleC = (current.currentVersionModuleC % 3) + 1,
                microcycleCount = current.microcycleCount + 1,
            )
        }
    }
    ```
  - [ ] 6 tests existentes pasan sin cambio (default `isDeload = false`)

- [ ] **Modificar `RotationResolverTest`** (+3 escenarios) (CA-14.04, 14.13)
  - [ ] Agregar al archivo existente:
    1. `advanceRotation isDeload=true from position 3 increments to 4` — posición avanza normalmente
    2. `advanceRotation isDeload=true from position 6 wraps to 1 versions frozen microcycleCount increments`
    3. `advanceRotation isDeload=true from position 6 with mixed versions (A=2,B=3,C=1) preserves all versions, microcycleCount++`

### Fase 4: Data/Repository — Activación del deload

- [ ] **Modificar `ExerciseProgressionDao`** (CA-14.01, 14.08)
  - [ ] Agregar `transitionToDeload()`:
    ```sql
    UPDATE exercise_progression
    SET status = 'IN_DELOAD'
    WHERE status NOT IN ('NO_HISTORY', 'MASTERED')
    ```
  - [ ] Agregar `getAllInDeload()`:
    ```sql
    SELECT * FROM exercise_progression WHERE status = 'IN_DELOAD'
    ```
  - [ ] Agregar `getAllWithPrescribedLoad()`:
    ```sql
    SELECT * FROM exercise_progression WHERE prescribed_load_kg IS NOT NULL
    ```

- [ ] **Modificar `AlertDao`** (CA-14.01, 14.07)
  - [ ] Agregar `getActiveAlertsByType(type: String): List<AlertEntity>`:
    ```sql
    SELECT * FROM alert WHERE type = :type AND is_active = 1 ORDER BY created_at DESC
    ```
  - [ ] Agregar `resolveAllByType(type: String, resolvedAt: String)`:
    ```sql
    UPDATE alert SET is_active = 0, resolved_at = :resolvedAt WHERE type = :type AND is_active = 1
    ```

- [ ] **Crear `Deload` domain model** (evitar layer violation)
  - [ ] Crear archivo: `domain/model/Deload.kt`
  - [ ] `data class Deload(val id: Long, val status: String, val activationDate: String, val completionDate: String?, val frozenVersionModuleA: Int, val frozenVersionModuleB: Int, val frozenVersionModuleC: Int)`

- [ ] **Modificar `SessionRepository`** (interfaz)
  - [ ] Agregar: `suspend fun activateDeload()`
  - [ ] Agregar: `suspend fun getDeloadState(): DeloadState`
  - [ ] Agregar: `fun getActiveDeload(): Flow<Deload?>`
  - [ ] Agregar: `suspend fun getDeloadIdBySessionId(sessionId: Long): Long?`
  - [ ] Agregar: `suspend fun countDeloadSessions(deloadId: Long): Int`

- [ ] **Modificar `SessionRepositoryImpl`** — constructor y activación
  - [ ] Agregar 3 nuevos parámetros al constructor: `deloadDao: DeloadDao`, `exerciseDao: ExerciseDao`, `moduleDao: ModuleDao` (total 13 parámetros con database)
  - [ ] Implementar `activateDeload()`:
    ```kotlin
    override suspend fun activateDeload() {
        database.withTransaction {
            val existingDeload = deloadDao.getActiveDeloadOnce()
            if (existingDeload != null) throw IllegalStateException("A deload cycle is already active")
            val rotationEntity = rotationStateDao.getRotationState().first()
                ?: throw IllegalStateException("Rotation state not found")
            val today = LocalDate.now().toString()
            deloadDao.insert(DeloadEntity(
                activationDate = today,
                frozenVersionModuleA = rotationEntity.currentVersionModuleA,
                frozenVersionModuleB = rotationEntity.currentVersionModuleB,
                frozenVersionModuleC = rotationEntity.currentVersionModuleC,
            ))
            exerciseProgressionDao.transitionToDeload()
        }
    }
    ```
  - [ ] Implementar `getActiveDeload(): Flow<Deload?>`:
    ```kotlin
    override fun getActiveDeload(): Flow<Deload?> = deloadDao.getActiveDeload().map { entity ->
        entity?.let { Deload(it.id, it.status, it.activationDate, it.completionDate, it.frozenVersionModuleA, it.frozenVersionModuleB, it.frozenVersionModuleC) }
    }
    ```
  - [ ] Implementar `getDeloadIdBySessionId` y `countDeloadSessions` como delegados a sus DAOs

### Fase 5: Data/Repository — Inicio de sesión con deload

- [ ] **Agregar `SessionDao.countDeloadSessions()`** (CA-14.03)
  ```sql
  SELECT COUNT(*) FROM session WHERE deload_id = :deloadId AND status IN ('COMPLETED', 'INCOMPLETE')
  ```

- [ ] **Agregar `SessionDao.hasSessionAfterDeload()`** (CA-14.07, Estado C de I1)
  ```sql
  SELECT EXISTS(
      SELECT 1 FROM session
      WHERE deload_id IS NULL
        AND status IN ('COMPLETED', 'INCOMPLETE')
        AND id > (SELECT MAX(id) FROM session WHERE deload_id = :deloadId)
  )
  ```

- [ ] **Modificar `ExerciseSetDao.getLastWeightForExercise()`** (CA-14.05, 14.08, Nota 8)
  - [ ] Agregar `INNER JOIN session s ON se.session_id = s.id WHERE ... AND s.deload_id IS NULL`

- [ ] **Agregar `ExerciseSetDao.getPreDeloadAvgWeight()`** (CA-14.05, 14.06)
  ```sql
  SELECT AVG(es.weight_kg)
  FROM exercise_set es
  WHERE es.session_exercise_id = (
      SELECT se.id
      FROM session_exercise se
      INNER JOIN session s ON se.session_id = s.id
      WHERE se.exercise_id = :exerciseId
        AND s.deload_id IS NULL
        AND s.date < :activationDate
        AND s.status IN ('COMPLETED', 'INCOMPLETE')
      ORDER BY s.date DESC, s.id DESC
      LIMIT 1
  )
  ```

- [ ] **Agregar `ExerciseDao.getByIdOnce()`** (Fase 6 — finalización):
  ```sql
  SELECT * FROM exercise WHERE id = :exerciseId
  ```
  Como `suspend fun` (distinto de `getById()` que retorna `Flow<ExerciseWithDetails?>`)

- [ ] **Agregar `ModuleDao.getByCode()`** (NO existe actualmente — CRITICAL E1):
  ```sql
  SELECT * FROM module WHERE code = :code
  ```

- [ ] **Modificar `SessionRepositoryImpl.startSession()`** (CA-14.01, 14.04)
  - [ ] Después de validar sesión activa (L81-83), agregar:
    ```kotlin
    val activeDeload = deloadDao.getActiveDeloadOnce()
    ```
  - [ ] En `SessionEntity(...)`: agregar `deloadId = activeDeload?.id`

- [ ] **Modificar `SessionRepositoryImpl.getNextModuleVersionId()`** (CA-14.04, 14.11)
  ```kotlin
  return rotationStateDao.getRotationState().flatMapLatest { rotationState ->
      if (rotationState == null) flowOf(0L)
      else {
          val moduleCode = RotationResolver.resolveModuleCode(rotationState.microcyclePosition)
          deloadDao.getActiveDeload().flatMapLatest { deload ->
              val versionNumber = if (deload != null)
                  RotationResolver.resolveVersionNumber(moduleCode, deload.frozenVersionModuleA, deload.frozenVersionModuleB, deload.frozenVersionModuleC)
              else
                  RotationResolver.resolveVersionNumber(moduleCode, rotationState.currentVersionModuleA, rotationState.currentVersionModuleB, rotationState.currentVersionModuleC)
              moduleVersionDao.getByModuleCodeAndVersion(moduleCode, versionNumber).map { it?.id ?: 0L }
          }
      }
  }
  ```

- [ ] **Modificar `GetNextSessionInfoUseCase`** (CA-14.04)
  - [ ] Agregar `combine()` con `sessionRepository.getActiveDeload()` para usar versiones congeladas en resolución de `versionNumber`

### Fase 6: Data/Repository — Cierre de sesión con deload

- [ ] **Modificar `SessionRepositoryImpl.evaluateProgression()`** (CA-14.01, 14.08, 14.09, 14.12)
  - [ ] **Guard 1 — prescribeLoad (L393-402):** Envolver con:
    ```kotlin
    val prescribedLoadKg = if (isDeloadSession) {
        currentProgression.prescribedLoadKg  // preservar, NO recalcular
    } else if (isBodyweight || isIsometric) {
        null
    } else {
        val meetsThreshold = DoubleThresholdRule.meetsDoubleThreshold(currentData)
        DoubleThresholdRule.prescribeLoad(currentData.avgWeightKg, exercise.loadIncrementKg, meetsThreshold)
    }
    ```
  - [ ] **Guard 2 — alertas PLATEAU (L412-430):** Envolver con `if (!isDeloadSession) { /* plateau alert logic */ }`
  - [ ] Guard existente en L442 (module-level detection) se mantiene sin cambio

- [ ] **Modificar `SessionRepositoryImpl.closeSession()`** (CA-14.03, 14.05, 14.06, 14.08)
  - [ ] Línea ~L313: cambiar `advanceRotation(currentRotation)` → `advanceRotation(currentRotation, isDeloadSession)`
  - [ ] Después de `rotationStateDao.update(...)` y antes de cierre de `withTransaction`, agregar bloque de finalización:
    ```kotlin
    if (isDeloadSession && deloadId != null) {
        val deloadSessionCount = sessionDao.countDeloadSessions(deloadId)
        if (deloadSessionCount == 6) {
            val today = LocalDate.now().toString()
            val deload = deloadDao.getById(deloadId) ?: throw IllegalStateException("Deload $deloadId not found")
            deloadDao.complete(deloadId, today)
            val allInDeload = exerciseProgressionDao.getAllInDeload()
            for (progression in allInDeload) {
                val exercise = exerciseDao.getByIdOnce(progression.exerciseId) ?: continue
                val isBodyweight = exercise.isBodyweight == 1
                val isIsometric = exercise.isIsometric == 1
                if (isBodyweight || isIsometric) {
                    exerciseProgressionDao.update(progression.copy(status = "IN_PROGRESSION", sessionsWithoutProgression = 0))
                } else {
                    val preDeloadWeight = exerciseSetDao.getPreDeloadAvgWeight(progression.exerciseId, deload.activationDate)
                    val module = moduleDao.getByCode(exercise.moduleCode)
                    val loadIncrementKg = module?.loadIncrementKg ?: 2.5
                    val resetLoad = if (preDeloadWeight != null && preDeloadWeight > 0.0)
                        DeloadLoadRule.calculateResetLoad(preDeloadWeight, loadIncrementKg)
                    else null
                    exerciseProgressionDao.update(progression.copy(status = "IN_PROGRESSION", prescribedLoadKg = resetLoad, sessionsWithoutProgression = 0))
                }
            }
            alertDao.resolveAllByType("MODULE_REQUIRES_DELOAD", today)
        }
    }
    ```

### Fase 7: Domain — Models + UseCases

- [ ] **Crear `DeloadState`** (CA-14.07)
  - [ ] Crear archivo: `domain/model/DeloadState.kt`
  - [ ] `sealed interface DeloadState`:
    - `data object NoDeloadNeeded : DeloadState`
    - `data class DeloadRequired(val modules: List<String>) : DeloadState`
    - `data class DeloadActive(val progress: Int, val totalSessions: Int, val frozenVersionA: Int, val frozenVersionB: Int, val frozenVersionC: Int) : DeloadState`
    - `data class DeloadCompleted(val resetLoads: List<ExerciseResetLoad>) : DeloadState`
  - [ ] `data class ExerciseResetLoad(val exerciseName: String, val resetLoadKg: Double)` en el mismo archivo

- [ ] **Crear `DeloadHomeState`** (CA-14.07)
  - [ ] Crear archivo: `domain/model/DeloadHomeState.kt`
  - [ ] `sealed interface DeloadHomeState`:
    - `data class Active(val progress: Int, val moduleCode: String) : DeloadHomeState`
    - `data class Required(val moduleCode: String) : DeloadHomeState`

- [ ] **Crear `ActivateDeloadUseCase`** (CA-14.01)
  - [ ] Crear archivo: `domain/usecase/deload/ActivateDeloadUseCase.kt`
  - [ ] `class ActivateDeloadUseCase @Inject constructor(private val sessionRepository: SessionRepository)`
  - [ ] `suspend operator fun invoke() = sessionRepository.activateDeload()`

- [ ] **Crear `GetDeloadStateUseCase`** (CA-14.07)
  - [ ] Crear archivo: `domain/usecase/deload/GetDeloadStateUseCase.kt`
  - [ ] `suspend operator fun invoke(): DeloadState = sessionRepository.getDeloadState()`

- [ ] **Implementar `SessionRepositoryImpl.getDeloadState()`**
  ```kotlin
  override suspend fun getDeloadState(): DeloadState {
      val activeDeload = deloadDao.getActiveDeloadOnce()
      if (activeDeload != null) {
          val progress = sessionDao.countDeloadSessions(activeDeload.id)
          return DeloadState.DeloadActive(progress, 6, activeDeload.frozenVersionModuleA, activeDeload.frozenVersionModuleB, activeDeload.frozenVersionModuleC)
      }
      val lastCompleted = deloadDao.getLastCompletedDeload()
      if (lastCompleted != null && !sessionDao.hasSessionAfterDeload(lastCompleted.id)) {
          val resetLoads = getResetLoadsForCompletedDeload(lastCompleted)
          return DeloadState.DeloadCompleted(resetLoads)
      }
      val deloadAlerts = alertDao.getActiveAlertsByType("MODULE_REQUIRES_DELOAD")
      if (deloadAlerts.isNotEmpty()) {
          return DeloadState.DeloadRequired(deloadAlerts.mapNotNull { it.moduleCode }.distinct())
      }
      return DeloadState.NoDeloadNeeded
  }
  ```
  - [ ] Helper privado `getResetLoadsForCompletedDeload(deload: DeloadEntity)`: lee `exerciseProgressionDao.getAllWithPrescribedLoad()` → filtra bodyweight/isométrico → mapea a `ExerciseResetLoad`

### Fase 8: UI — Home Card Descarga

- [ ] **Modificar `HomeUiState`**: agregar `val deloadState: DeloadHomeState? = null`

- [ ] **Modificar `HomeViewModel`**: agregar `sessionRepository.getActiveDeload()` al `combine()` → construir `DeloadHomeState` si `activeDeload != null`

- [ ] **Modificar `HomeScreen`**: agregar `onNavigateToDeloadManagement: () -> Unit` + composable `DeloadStatusCard`:
  - Card Filled, Secondary Container, corner 12.dp
  - Active: "🔄 Descarga activa" TitleMedium On Secondary Container + "Sesión {progress} de 6" BodyMedium
  - Required: "⚠️ Módulo {code} requiere descarga" TitleMedium On Secondary Container
  - Text Button "Ver gestión de descarga →" → `onNavigateToDeloadManagement()`

### Fase 8.5: UI — ActiveSessionScreen deload badge + cargas 60%

- [ ] **Modificar `ActiveSessionUiState`** (CA-14.02, 14.07)
  - [ ] Agregar: `val isDeloadSession: Boolean = false`, `val deloadProgress: String = ""`

- [ ] **Modificar `ActiveSessionViewModel`** (CA-14.02, 14.07)
  - [ ] Detectar si sesión es deload: `val deloadId = sessionRepository.getDeloadIdBySessionId(sessionId)`
  - [ ] Si deload: `deloadProgress = "${sessionRepository.countDeloadSessions(deloadId)}/6"`
  - [ ] `loadText` con 3 variantes deload (Especificación Visual §E1):
    ```kotlin
    val loadText = when {
        isDeload && detail.isIsometric -> "Isométrico (30s)"
        isDeload && detail.isBodyweight -> "Peso corporal (8 reps objetivo)"
        isDeload && detail.prescribedLoadKg != null ->
            "🔄 %.1f Kg".format(DeloadLoadRule.calculateDeloadLoad(detail.prescribedLoadKg, detail.loadIncrementKg))
        // variantes normales existentes...
    }
    ```
  - [ ] **NOTA:** Agregar `loadIncrementKg: Double` a `ExerciseSummaryDetail` DTO y query (JOIN a `module.load_increment_kg`)

- [ ] **Modificar `ActiveSessionScreen`** (CA-14.07)
  - [ ] Badge AssistChip condicional: "Descarga · Sesión {deloadProgress}" con ícono 🔄 color azul descarga
  - [ ] `LoadText` con color `semanticColors.deloadActive` cuando `isDeloadSession`

### Fase 9: UI — DeloadManagementScreen + ViewModel

- [ ] **Crear `DeloadManagementUiState`**: `Loading`, `Content(deloadState)`, `Error(message)`

- [ ] **Crear `DeloadManagementViewModel`**:
  - [ ] `@HiltViewModel @Inject constructor(getDeloadStateUseCase, activateDeloadUseCase)`
  - [ ] `init { loadState() }` + `fun activateDeload() { launch { activateDeloadUseCase(); loadState() } }`

- [ ] **Crear `DeloadManagementScreen`** (CA-14.01, 14.02-14.09)
  - [ ] `CenterAlignedTopAppBar` con ArrowBack → `onNavigateBack()`
  - [ ] `when(deloadState)`:

    **Estado A — `DeloadRequired(modules)`:**
    - Outlined Card: "No hay descarga activa" + "⚠️ Módulo {X} requiere descarga" + descripción 50% meseta
    - Filled Card (Secondary Container): protocolo detallado: "Carga al 60%", "4 series", "8 reps", "RIR 4-5", "1 microciclo (6 sesiones)", "Versiones congeladas". Excepciones CA-14.09: "Peso corporal: 8 reps / Isométricos: 30 seg". Nota final: "Al finalizar: reinicio 90% pre-descarga"
    - Filled Button "Activar Descarga" → `viewModel.activateDeload()`

    **Estado B — `DeloadActive(progress, totalSessions, frozenVersionA/B/C)`:**
    - Filled Card (Secondary Container): 🔄 + "Descarga activa" azul descarga + "Progreso: {progress}/6" + `LinearProgressIndicator(progress/6f)` + "Sesiones restantes: {6-progress}"
    - Filled Card (SurfaceContainerHigh): parámetros "60% / 4 series / 8 reps / RIR 4-5" + "Versión congelada: A-V{a}, B-V{b}, C-V{c}"

    **Estado C — `DeloadCompleted(resetLoads)`:**
    - Filled Card (Tertiary Container `#E0EEDD`): ✅ + "Descarga completada" + lista "{nombre}: {loadKg} Kg" + "Las versiones retoman su avance normal." (italic)

    **Estado `NoDeloadNeeded`:**
    - "No hay descarga pendiente" centrado + TextButton "Volver al inicio"

  - [ ] Bottom Navigation presente (I1 SÍ tiene Bottom Navigation — Wireframe I1)

### Fase 10: Navegación + integración

- [ ] **Modificar `NavigationRoutes`**: agregar `const val DELOAD_MANAGEMENT = "deload"` (Arq. Técnica §4.3 L405)

- [ ] **Modificar `TensionNavHost`**:
  - [ ] Reemplazar TODO L119: agregar `onNavigateToDeloadManagement = { navController.navigate(NavigationRoutes.DELOAD_MANAGEMENT) }` en `HomeScreen()`
  - [ ] Registrar composable I1:
    ```kotlin
    composable(NavigationRoutes.DELOAD_MANAGEMENT) {
        DeloadManagementScreen(onNavigateBack = { navController.popBackStack() })
    }
    ```
  - [ ] **Nota:** `showBottomBar` NO necesita excluir `deload` — I1 SÍ tiene Bottom Navigation

### Fase N: QA y Deployment

- [ ] Ejecutar Agente Peer Review (MANUAL)
- [ ] Resolver incidentes del Peer Review (MANUAL, condicional)
- [ ] Crear Pull Request (MANUAL)
- [ ] Ejecutar pipeline deployment DEV (MANUAL)
- [ ] Diseñar y ejecutar pruebas manuales (MANUAL)

---

## Vinculación CAs → Fases

| CA | Fase(s) |
|---|---|
| CA-14.01 — Activación | Fase 4 (activateDeload() transacción) + Fase 9 (I1 Estado A botón) |
| CA-14.02 — 60% carga | Fase 1 (DeloadLoadRule) + Fase 8.5 (loadText variante deload en E1) |
| CA-14.03 — 1 microciclo | Fase 5 (countDeloadSessions) + Fase 6 (closeSession count==6) |
| CA-14.04 — Versiones congeladas | Fase 3 (RotationResolver isDeload) + Fase 5 (getNextModuleVersionId + GetNextSessionInfoUseCase) |
| CA-14.05 — 90% reinicio | Fase 1 (calculateResetLoad) + Fase 6 (finalización: getPreDeloadAvgWeight + update) |
| CA-14.06 — Redondeo down | Fase 1 (floor formula) |
| CA-14.07 — Indicación visual | Fase 8 (B1 Card) + Fase 8.5 (E1 badge + cargas 60%) + Fase 9 (I1 Estados A/B/C) |
| CA-14.08 — Transición post-descarga | Fase 6 (IN_DELOAD→IN_PROGRESSION, sessionsWithoutProgression=0, resetLoad) |
| CA-14.09 — Bodyweight/isométricos | Fase 6 (guard bodyweight/isometric en finalización) + Fase 9 (I1 muestra excepciones) |
| CA-14.10-14.12, 14.14 — Conteo microciclos | ✅ Ya implementado en HU-09 |
| CA-14.13 — Conteo durante deload | Fase 3 (RotationResolverTest isDeload=true posición 6 → microcycleCount++) |

---

> **Método Ceiba IDE** | Usuario: Esteban Colorado González | Fecha: 2026-02-17
