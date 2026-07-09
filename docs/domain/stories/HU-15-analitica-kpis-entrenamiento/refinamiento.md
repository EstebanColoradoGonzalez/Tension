## Refinamiento Técnico (Developer)
**Autor**: Esteban Colorado González | **Fecha**: 2026-02-18

---

### Consideraciones Generales

**Basado en análisis arquitectónico:**
HU-15 con 30 componentes nuevos, 7 reglas puras, 9 use cases, 3 pantallas, 10 queries SQL de agregación cruzando 6 tablas. Historia de lectura pura — no modifica datos, no produce efectos colaterales.

**Nivel de complejidad:**
ALTA en superficie (30 componentes nuevos), MEDIA en lógica individual (cada regla es simple: 1-3 funciones puras). Complejidad real: (1) 8+ queries SQL de agregación cruzando 6 tablas, (2) agrupación por microciclo derivada en Kotlin (chunked(6)), (3) Canvas composable para gráfico multilínea G2.

**Riesgos técnicos conocidos:**

1. **Queries de agregación complejos:** Cruzan 4-6 tablas (exercise_set → session_exercise → session → module_version + exercise → exercise_muscle_zone → muscle_zone). Room no soporta window functions → agrupación en Kotlin.
2. **Agrupación por microciclo:** Asume sesiones consecutivas de 6 — el sistema no permite eliminar sesiones y `advanceRotation()` es secuencial.
3. **Canvas composable custom:** Edge case: todos grupos con tonelaje 0 → eje Y mínimo 1 Kg.
4. **Sesiones de deload en cálculos:** EXCLUIR de tasa/velocidad/RIR. INCLUIR en tonelaje/adherencia.

**Dependencias nuevas:** Ninguna.

**Estrategia de testing:** JUnit 4 para 7 rules (sin MockK). 3 archivos de test instrumentado para DAOs. UI: validación manual en Fase N.

### Patrones y convenciones del equipo

- Código fuente en inglés, UI y datos de dominio en español
- Naming: `{Feature}Screen`, `{Feature}ViewModel`, `{Acción}{Entidad}UseCase`, `{Entidad}Repository`/`{Entidad}RepositoryImpl`
- Estructura Composable: hiltViewModel() + collectAsStateWithLifecycle() + LaunchedEffect para eventos
- Estructura ViewModel: `_uiState MutableStateFlow` / `uiState StateFlow` + `_events MutableSharedFlow` / `events SharedFlow`
- Sealed classes para UiState y Events
- `operator fun invoke()` en Use Cases
- Callbacks en Composables con prefijo `on`

---

### Tareas de Implementación

#### Fase 1: Reglas puras (7 rules) + TrendDirection + tests

- [ ] **Crear `ProgressionRateRule`** (CA-15.01)
  - [ ] Crear archivo: `domain/rules/ProgressionRateRule.kt`
  - [ ] `object ProgressionRateRule`
  - [ ] `fun calculate(positiveCount: Int, totalCount: Int): Double = if (totalCount == 0) 0.0 else (positiveCount.toDouble() / totalCount) * 100.0`

- [ ] **Crear `LoadVelocityRule`** (CA-15.03, 15.04)
  - [ ] Crear archivo: `domain/rules/LoadVelocityRule.kt`
  - [ ] `fun calculate(currentWeightKg: Double, initialWeightKg: Double, sessionCount: Int): Double`
    - `if (sessionCount <= 1) return 0.0`
    - `return (currentWeightKg - initialWeightKg) / (sessionCount - 1)`

- [ ] **Crear `TonnageRule`** (CA-15.07, 15.09)
  - [ ] Crear archivo: `domain/rules/TonnageRule.kt`
  - [ ] DTO `SetForTonnage(val weightKg: Double, val reps: Int, val muscleGroup: String)` en mismo archivo
  - [ ] `fun calculateForMuscleGroup(sets: List<SetForTonnage>): Map<String, Double>`
    - `sets.groupBy { it.muscleGroup }.mapValues { (_, g) -> g.sumOf { it.weightKg * it.reps } }`

- [ ] **Crear `VolumeDistributionRule`** (CA-15.10)
  - [ ] Crear archivo: `domain/rules/VolumeDistributionRule.kt`
  - [ ] `fun calculate(setsByZone: Map<String, Int>, totalSets: Int): Map<String, Double>`
    - Guard: `if (totalSets == 0) return setsByZone.mapValues { 0.0 }`
    - `setsByZone.mapValues { (_, count) -> (count.toDouble() / totalSets) * 100.0 }`

- [ ] **Crear `AvgRirRule`** (CA-15.13)
  - [ ] Crear archivo: `domain/rules/AvgRirRule.kt`
  - [ ] `fun calculate(rirValues: List<Int>): Double`
    - Guard: `if (rirValues.isEmpty()) return 0.0`
    - `(rirValues.sum().toDouble() / rirValues.size).let { (it * 10).roundToInt() / 10.0 }`
  - [ ] `import kotlin.math.roundToInt`

- [ ] **Crear `AdherenceRule`** (CA-15.16)
  - [ ] Crear archivo: `domain/rules/AdherenceRule.kt`
  - [ ] `fun calculate(completedSessions: Int, plannedSessions: Int): Double`
    - Guard: `if (plannedSessions == 0) return 0.0`
    - `((completedSessions.toDouble() / plannedSessions) * 100.0).coerceAtMost(100.0)`

- [ ] **Crear `TrendClassificationRule`** (CA-15.21)
  - [ ] Crear archivo: `domain/rules/TrendClassificationRule.kt`
  - [ ] `fun classify(values: List<Double>): TrendDirection`
    - Guard: `if (values.size < 2) return TrendDirection.STABLE`
    - Regresión lineal simple: `slope = Σ(xi - x̄)(yi - ȳ) / Σ(xi - x̄)²`
    - Umbral: `yMean * 0.05` (±5% del valor medio)
    - `if (slope > threshold) ASCENDING; if (slope < -threshold) DECLINING; else STABLE`
    - Guard: `if (denominator == 0.0) return STABLE`

- [ ] **Crear `TrendDirection`** (CA-15.21)
  - [ ] Crear archivo: `domain/model/TrendDirection.kt`
  - [ ] `enum class TrendDirection { ASCENDING, STABLE, DECLINING }`

- [ ] **Crear tests unitarios (7 archivos):**
  - [ ] `ProgressionRateRuleTest`: (3/10)→30%, (0/0)→0%, (10/10)→100%, (1/2)→50%
  - [ ] `LoadVelocityRuleTest`: (60,40,5)→5.0, (50,50,3)→0.0, (60,40,1)→0.0, (0,0,0)→0.0
  - [ ] `TonnageRuleTest`: 2 muscle groups, sets vacíos, ejercicio multi-zona (CA-15.09)
  - [ ] `VolumeDistributionRuleTest`: {"Pecho":10, "Espalda":6} total 16 → {62.5%, 37.5%}; totalSets=0
  - [ ] `AvgRirRuleTest`: [2,3,2,3]→2.5, [1]→1.0, vacío→0.0, [2,3,2,4]→2.8
  - [ ] `AdherenceRuleTest`: (5,6)→83.3, (6,6)→100.0, (7,6)→100.0 (capped), (0,0)→0.0
  - [ ] `TrendClassificationRuleTest`: ascendente, estable, declinante, < 2 puntos, dentro umbral, vacía

#### Fase 2: Domain models (DTOs analíticos)

Todos los archivos se crean en `domain/model/` (ADR-05 — Room mapea por alias de columna independientemente del paquete):

- [ ] `AdherenceData(completedSessions: Int, plannedSessions: Int, percentage: Double)`
- [ ] `RirByModule(moduleCode: String, averageRir: Double, interpretation: RirInterpretation)` + `enum RirInterpretation { OPTIMAL, RISK_TOO_CLOSE, INSUFFICIENT_STIMULUS }`
- [ ] `ExerciseProgressionRate(exerciseId: Long, exerciseName: String, rate: Double, isBodyweight: Boolean)`
- [ ] `ExerciseLoadVelocity(exerciseId: Long, exerciseName: String, velocity: Double, isBodyweight: Boolean)`
- [ ] `MuscleGroupTonnage(muscleGroup: String, tonnageKg: Double)`
- [ ] `MuscleGroupTrend(muscleGroup: String, direction: TrendDirection)`
- [ ] `TonnageSnapshot(microcycleNumber: Int, tonnageByGroup: Map<String, Double>)`
- [ ] `ClassificationCount(exerciseId: Long, exerciseName: String, isBodyweight: Int, positiveCount: Int, totalCount: Int)`
- [ ] `ExerciseSessionRange(exerciseId: Long, exerciseName: String, isBodyweight: Int, isIsometric: Int, firstSessionId: Long, lastSessionId: Long, sessionCount: Int)`
- [ ] `SetTonnageData(weightKg: Double, reps: Int, muscleGroup: String)`
- [ ] `SetDistributionData(muscleZoneName: String, moduleCode: String, setCount: Int)`
- [ ] `ClassificationCountByGroup(muscleGroup: String, positiveCount: Int, totalCount: Int)`

#### Fase 3: DAO queries nuevos

##### SessionDao

- [ ] `getClosedSessionsOrdered(): List<SessionEntity>`:
  ```sql
  SELECT id, module_version_id, deload_id, date, status FROM session
  WHERE status IN ('COMPLETED', 'INCOMPLETE') ORDER BY date ASC, id ASC
  ```
- [ ] `countSessionsInWeek(weekStartDate: String, weekEndDate: String): Int`:
  ```sql
  SELECT COUNT(*) FROM session WHERE status IN ('COMPLETED', 'INCOMPLETE')
  AND date >= :weekStartDate AND date <= :weekEndDate
  ```
- [ ] `getSessionIdsByModuleInRange(moduleCode: String, limit: Int): List<Long>`:
  ```sql
  SELECT s.id FROM session s
  INNER JOIN module_version mv ON s.module_version_id = mv.id
  WHERE mv.module_code = :moduleCode
    AND s.status IN ('COMPLETED', 'INCOMPLETE')
    AND s.deload_id IS NULL
  ORDER BY s.date DESC, s.id DESC LIMIT :limit
  ```

##### ExerciseSetDao

- [ ] `getTonnageDataBySessionIds(sessionIds: List<Long>): List<SetTonnageData>`:
  ```sql
  SELECT es.weight_kg AS weightKg, es.reps, mz.muscle_group AS muscleGroup
  FROM exercise_set es
  INNER JOIN session_exercise se ON es.session_exercise_id = se.id
  INNER JOIN exercise_muscle_zone emz ON se.exercise_id = emz.exercise_id
  INNER JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
  WHERE se.session_id IN (:sessionIds)
  ```
  **Nota:** Produce duplicados intencionales para ejercicios multi-zona (CA-15.09)

- [ ] `getRirValuesBySessionIds(sessionIds: List<Long>): List<Int>`:
  ```sql
  SELECT es.rir FROM exercise_set es
  INNER JOIN session_exercise se ON es.session_exercise_id = se.id
  WHERE se.session_id IN (:sessionIds)
  ```

- [ ] `getAvgWeightByExerciseInSession(exerciseId: Long, sessionId: Long): Double?`:
  ```sql
  SELECT AVG(es.weight_kg) FROM exercise_set es
  INNER JOIN session_exercise se ON es.session_exercise_id = se.id
  WHERE se.exercise_id = :exerciseId AND se.session_id = :sessionId
  ```

- [ ] `getSetDistributionBySessionIds(sessionIds: List<Long>): List<SetDistributionData>`:
  ```sql
  SELECT mz.name AS muscleZoneName, mv.module_code AS moduleCode, COUNT(*) AS setCount
  FROM exercise_set es
  INNER JOIN session_exercise se ON es.session_exercise_id = se.id
  INNER JOIN session s ON se.session_id = s.id
  INNER JOIN module_version mv ON s.module_version_id = mv.id
  INNER JOIN exercise_muscle_zone emz ON se.exercise_id = emz.exercise_id
  INNER JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
  WHERE se.session_id IN (:sessionIds)
  GROUP BY mz.name, mv.module_code
  ```
  **Nota CA-15.10 + RF49:** GROUP BY `mz.name` (15 zonas musculares), no `mz.muscle_group` (12 grupos)

##### SessionExerciseDao

- [ ] `getClassificationCountsByPeriod(startDate: String): List<ClassificationCount>`:
  ```sql
  SELECT se.exercise_id AS exerciseId, e.name AS exerciseName, e.is_bodyweight AS isBodyweight,
      SUM(CASE WHEN se.progression_classification = 'POSITIVE_PROGRESSION' THEN 1 ELSE 0 END) AS positiveCount,
      COUNT(se.progression_classification) AS totalCount
  FROM session_exercise se
  INNER JOIN session s ON se.session_id = s.id
  INNER JOIN exercise e ON se.exercise_id = e.id
  WHERE s.status IN ('COMPLETED', 'INCOMPLETE')
    AND s.deload_id IS NULL
    AND s.date >= :startDate
    AND se.progression_classification IS NOT NULL
  GROUP BY se.exercise_id
  ```

- [ ] `getExerciseSessionRangeByPeriod(startDate: String): List<ExerciseSessionRange>`:
  ```sql
  SELECT se.exercise_id AS exerciseId, e.name AS exerciseName,
      e.is_bodyweight AS isBodyweight, e.is_isometric AS isIsometric,
      MIN(se.session_id) AS firstSessionId, MAX(se.session_id) AS lastSessionId,
      COUNT(DISTINCT se.session_id) AS sessionCount
  FROM session_exercise se
  INNER JOIN session s ON se.session_id = s.id
  INNER JOIN exercise e ON se.exercise_id = e.id
  WHERE s.status IN ('COMPLETED', 'INCOMPLETE')
    AND s.deload_id IS NULL
    AND s.date >= :startDate
  GROUP BY se.exercise_id
  ```

- [ ] `getClassificationCountsBySessionIds(sessionIds: List<Long>): List<ClassificationCountByGroup>`:
  ```sql
  SELECT mz.muscle_group AS muscleGroup,
      SUM(CASE WHEN se.progression_classification = 'POSITIVE_PROGRESSION' THEN 1 ELSE 0 END) AS positiveCount,
      COUNT(se.progression_classification) AS totalCount
  FROM session_exercise se
  INNER JOIN session s ON se.session_id = s.id
  INNER JOIN exercise_muscle_zone emz ON se.exercise_id = emz.exercise_id
  INNER JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
  WHERE se.session_id IN (:sessionIds)
    AND s.deload_id IS NULL
    AND se.progression_classification IS NOT NULL
  GROUP BY mz.muscle_group
  ```
  **Nota:** Soporta doble métrica CA-15.20 (tasa de progresión por muscle_group)

- [ ] **Crear tests instrumentados:**
  - [ ] `androidTest/SessionDaoMetricsTest.kt` — DB en memoria, 12+ sesiones COMPLETED/INCOMPLETE, mix módulos
  - [ ] `androidTest/ExerciseSetDaoMetricsTest.kt` — poblar exercise_set, session_exercise, exercise_muscle_zone
  - [ ] `androidTest/SessionExerciseDaoMetricsTest.kt` — verificar exclusión deload y NULL classifications

#### Fase 4: MetricsRepository (interfaz + implementación)

- [ ] **Crear `MetricsRepository`** (`domain/repository/MetricsRepository.kt`):
  ```kotlin
  interface MetricsRepository {
      suspend fun getSessionsCompletedInWeek(weekStartDate: String, weekEndDate: String): Int
      suspend fun getWeeklyFrequency(): Int
      suspend fun getRirValuesByModule(moduleCode: String, sessionLimit: Int): List<Int>
      suspend fun getClassificationCounts(startDate: String): List<ClassificationCount>
      suspend fun getClassificationCountsBySessionIds(sessionIds: List<Long>): List<ClassificationCountByGroup>
      suspend fun getExerciseSessionRanges(startDate: String): List<ExerciseSessionRange>
      suspend fun getAvgWeightForExerciseInSession(exerciseId: Long, sessionId: Long): Double?
      suspend fun getSessionIdsGroupedByMicrocycle(): Map<Int, List<Long>>
      suspend fun getTonnageDataBySessionIds(sessionIds: List<Long>): List<SetTonnageData>
      suspend fun getSetDistributionBySessionIds(sessionIds: List<Long>): List<SetDistributionData>
      suspend fun getMicrocycleCount(): Int
  }
  ```

- [ ] **Crear `MetricsRepositoryImpl`** (`data/repository/MetricsRepositoryImpl.kt`):
  - [ ] `class MetricsRepositoryImpl @Inject constructor(sessionDao, exerciseSetDao, sessionExerciseDao, profileDao, rotationStateDao)` — sin `@Singleton` en clase
  - [ ] `getSessionIdsGroupedByMicrocycle()`:
    ```kotlin
    val sessions = sessionDao.getClosedSessionsOrdered()
    return sessions.chunked(6).mapIndexed { index, chunk ->
        (index + 1) to chunk.map { it.id }
    }.toMap()
    ```
  - [ ] `getWeeklyFrequency()`: `profileDao.getProfile().first()?.weeklyFrequency ?: 6`
  - [ ] `getRirValuesByModule()`: `sessionDao.getSessionIdsByModuleInRange(moduleCode, sessionLimit)` → `exerciseSetDao.getRirValuesBySessionIds(sessionIds)`
  - [ ] Los demás métodos delegan directamente al DAO correspondiente

#### Fase 5: Use Cases (9)

Todos en `domain/usecase/metrics/`. Patrón: `@Inject constructor` + `suspend operator fun invoke()`.

- [ ] **`GetMicrocycleMapUseCase`**: `invoke(): Map<Int, List<Long>>` = `metricsRepository.getSessionIdsGroupedByMicrocycle()`

- [ ] **`GetAdherenceUseCase`**: semana ISO (lunes=`DayOfWeek.MONDAY`, domingo=`DayOfWeek.SUNDAY`) → `countSessionsInWeek` + `getWeeklyFrequency` → `AdherenceRule.calculate()` → `AdherenceData`

- [ ] **`GetAvgRirByModuleUseCase`**: `invoke(sessionLimit: Int = 2): List<RirByModule>` — para cada módulo A/B/C: `getRirValuesByModule()` → `AvgRirRule.calculate()` → `RirInterpretation`: `< 1.5 = RISK_TOO_CLOSE`, `> 3.5 = INSUFFICIENT_STIMULUS`, else `OPTIMAL`

- [ ] **`GetProgressionRateUseCase`**: `invoke(weeksBack: Int = 4): List<ExerciseProgressionRate>` — `startDate = LocalDate.now().minusWeeks(weeksBack).toString()` → `getClassificationCounts(startDate)` → `ProgressionRateRule.calculate()`

- [ ] **`GetLoadVelocityUseCase`**: `invoke(weeksBack: Int = 4): List<ExerciseLoadVelocity>` — por ejercicio: si bodyweight/isometric → marcar N/A; else → `getAvgWeightForExerciseInSession(firstSessionId)` + `getAvgWeightForExerciseInSession(lastSessionId)` → `LoadVelocityRule.calculate()`

- [ ] **`GetTonnageByMuscleGroupUseCase`**: `invoke(sessionIds: List<Long>): List<MuscleGroupTonnage>` — `getTonnageDataBySessionIds()` → `TonnageRule.calculateForMuscleGroup()` → padding 12 grupos canónicos a 0.0:
  ```kotlin
  val allGroups = listOf("Pecho", "Espalda", "Abdomen", "Hombro", "Tríceps", "Bíceps",
      "Cuádriceps", "Isquiotibiales", "Glúteos", "Aductores", "Abductores", "Gemelos")
  return allGroups.map { group -> MuscleGroupTonnage(group, tonnageMap[group] ?: 0.0) }
  ```

- [ ] **`GetVolumeDistributionUseCase`**: `invoke(sessionIds: List<Long>): Map<String, Map<String, Double>>` — `getSetDistributionBySessionIds()` → `groupBy { it.moduleCode }` → `VolumeDistributionRule.calculate()` por módulo

- [ ] **`GetTonnageEvolutionUseCase`**: `invoke(microcycleMap: Map<Int, List<Long>>): List<TonnageSnapshot>` — por cada microciclo: `getTonnageDataBySessionIds()` → `TonnageRule` → `TonnageSnapshot`

- [ ] **`GetMuscleGroupTrendUseCase`**: `invoke(microcycleMap: Map<Int, List<Long>>): List<MuscleGroupTrend>`
  - `filterCompletedOnly()` = entries donde `value.size == 6`. Si `< 4` → `return emptyList()`
  - Últimos 4-6 microciclos completados ordenados cronológicamente
  - Por cada microciclo: calcular tonelaje por grupo + tasa de progresión por grupo
  - `progressionSnapshots` usa `mapNotNull`: si `counts.isEmpty()` (deload) → excluir punto
  - Combinación más conservadora: DECLINING > STABLE > ASCENDING

#### Fase 6: DI binding

- [ ] **Modificar `RepositoryModule`**:
  ```kotlin
  @Binds @Singleton
  abstract fun bindMetricsRepository(impl: MetricsRepositoryImpl): MetricsRepository
  ```

#### Fase 7: G1 — MetricsScreen + ViewModel

- [ ] **Crear `MetricsUiState`** (`ui/metrics/MetricsUiState.kt`): `Loading`, `Content(adherence, rirByModule, progressionRates, loadVelocities)`, `Error`

- [ ] **Crear `MetricsViewModel`** (`ui/metrics/MetricsViewModel.kt`):
  - [ ] `@HiltViewModel @Inject constructor(getAdherenceUseCase, getAvgRirByModuleUseCase, getProgressionRateUseCase, getLoadVelocityUseCase)`
  - [ ] `private var progressionWeeks = 4`, `private var rirSessionLimit = 2`
  - [ ] `init { loadMetrics() }`, `fun changeProgressionPeriod(weeks: Int)`, `fun changeRirPeriod(sessionLimit: Int)`

- [ ] **Crear `MetricsScreen`** (`ui/metrics/MetricsScreen.kt`):
  - [ ] Params: `onNavigateToVolume: () -> Unit`, `onNavigateToTrend: () -> Unit`, `onNavigateToExerciseHistory: (Long) -> Unit`
  - [ ] `CenterAlignedTopAppBar` sin navigationIcon, title "Métricas"
  - [ ] `LazyColumn` con 6 items:
    1. **Card Adherencia** (Secondary Container): porcentaje HeadlineMedium Primary + "{n} de {m} sesiones"
    2. **Card RIR** (Surface Container High): `ExposedDropdownMenuBox` (2/4/6 sesiones) + 3 filas A/B/C + badges 🟢🔴🟡 + referencia textual
    3. **Divider** M3 (Outline Variant, margin 16dp) + `ExposedDropdownMenuBox` (4/8/12 semanas) + lista Tasa de Progresión (ListItem 56dp, clickable → F3, trailing: % + color ≥60%=verde/40-59%=ámbar/<40%=rojo)
    4. **Divider** M3 + lista Velocidad de Carga (trailing: "+X.X Kg/sesión" o "N/A" italic para bodyweight)
    5. **Divider** M3 + TextButton "Volumen por Grupo Muscular →" → `onNavigateToVolume()`
    6. TextButton "Tendencia de Progresión →" → `onNavigateToTrend()`

#### Fase 8: G2 — VolumeScreen + ViewModel

- [ ] **Crear `VolumeUiState`**: `Loading`, `Content(tonnageByGroup, distributionByModule, evolution, selectedMicrocycle, totalMicrocycles, insufficientEvolution)`, `Error`

- [ ] **Crear `VolumeViewModel`**:
  - [ ] `@HiltViewModel @Inject constructor(getMicrocycleMapUseCase, getTonnageByMuscleGroupUseCase, getVolumeDistributionUseCase, getTonnageEvolutionUseCase)`
  - [ ] `init { loadVolume() }` — obtiene mapa, carga último microciclo, evolution. `insufficientEvolution = totalMicrocycles < 2`
  - [ ] `fun selectMicrocycle(n: Int)` — recalcula tonnage y distribution para el microciclo n

- [ ] **Crear `VolumeScreen`** + **`TonnageChartComposable`**:
  - [ ] `CenterAlignedTopAppBar` con ArrowBack, title "Volumen por Grupo Muscular"
  - [ ] `LazyColumn` con 4 secciones: (1) stepper ◀▶ microciclo, (2) 12 barras horizontales tonelaje (Primary fill), (3) distribución % por módulo con 15 zonas por sub-sección, (4) gráfico `TonnageChartComposable` o mensaje insuficiente
  - [ ] Canvas: ejes, 12 líneas colores diferenciados, puntos, leyenda, grid dashed

#### Fase 9: G3 — TrendScreen + ViewModel

- [ ] **Crear `TrendUiState`**: `Loading`, `Content(trends)`, `InsufficientData(remaining)`, `Error`

- [ ] **Crear `TrendViewModel`**: `@HiltViewModel @Inject constructor(getMicrocycleMapUseCase, getMuscleGroupTrendUseCase)`. Guard: `completedCount < 4` → `InsufficientData`

- [ ] **Crear `TrendScreen`**:
  - [ ] `CenterAlignedTopAppBar` con ArrowBack, title "Tendencia de Progresión"
  - [ ] `Content` → "Evaluación: últimos 4-6 microciclos" + `LazyColumn` 12 filas
  - [ ] `InsufficientData` → "Se necesitan al menos 4 microciclos completados. Faltan {remaining}."
  - [ ] Cada fila: ListItem 56dp, nombre grupo, trailing: texto español + ícono:
    - `ASCENDING` → "Ascendente" verde #2E7D32/#81C784, 📈
    - `STABLE` → "Estable" ámbar #8D6E00/#FFD54F, 📊
    - `DECLINING` → "En declive" rojo #C62828/#EF9A9A, 📉

#### Fase 10: Navegación + integración

- [ ] **Modificar `NavigationRoutes`**: `const val MUSCLE_VOLUME = "muscle-volume"`, `const val PROGRESSION_TREND = "progression-trend"`

- [ ] **Modificar `BottomNavigationBar`**: agregar `childRoutes = setOf(MUSCLE_VOLUME, PROGRESSION_TREND)` al tab METRICS

- [ ] **Modificar `TensionNavHost`** (L244-245):
  - [ ] Reemplazar `PlaceholderScreen` con `MetricsScreen(onNavigateToVolume, onNavigateToTrend, onNavigateToExerciseHistory)`
  - [ ] Registrar G2: `composable(MUSCLE_VOLUME) { VolumeScreen(onNavigateBack = { navController.popBackStack() }) }`
  - [ ] Registrar G3: `composable(PROGRESSION_TREND) { TrendScreen(onNavigateBack = { navController.popBackStack() }) }`
  - [ ] `showBottomBar` NO necesita excluir G2/G3 — ambas tienen Bottom Navigation

#### Fase N: QA y Deployment

- [ ] Ejecutar Agente Peer Review (MANUAL)
- [ ] Resolver incidentes (MANUAL, condicional)
- [ ] Crear Pull Request (MANUAL)
- [ ] Ejecutar pipeline deployment DEV (MANUAL)
- [ ] Diseñar y ejecutar pruebas manuales (MANUAL)

### Vinculación CAs → Fases (resumen)

| Bloque | CAs | Fases clave |
|---|---|---|
| A — Rendimiento | CA-15.01 a 15.06 | Fase 1 (rules) + Fase 3 (DAOs) + Fase 5 (use cases) + Fase 7 (G1 Secciones 3-4) |
| B — Volumen | CA-15.07 a 15.12 | Fase 1 (TonnageRule/VolumeDistributionRule) + Fase 3 (DAOs) + Fase 4 (MetricsRepo) + Fase 5 + Fase 8 (G2) |
| C — Intensidad | CA-15.13 a 15.19 | Fase 1 (AvgRirRule/AdherenceRule) + Fase 3 (DAOs) + Fase 5 + Fase 7 (G1 Secciones 1-2) |
| D — Tendencia | CA-15.20 a 15.24 | Fase 1 (TrendClassificationRule) + Fase 5 (GetMuscleGroupTrendUseCase) + Fase 9 (G3) |
| E — Evolución | CA-15.25 a 15.28 | Fase 5 (GetTonnageEvolutionUseCase) + Fase 8 (TonnageChartComposable) |
