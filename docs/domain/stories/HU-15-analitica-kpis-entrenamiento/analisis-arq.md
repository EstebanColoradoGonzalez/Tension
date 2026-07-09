## Análisis Arquitectónico

> HU-15 es la historia más amplia del sistema en superficie de datos: lee TODAS las tablas transaccionales para producir 6 KPIs distintos más 2 análisis temporales. Es una historia de **lectura pura** — no modifica ningún dato existente, no ejecuta lógica al cierre de sesión, y no produce efectos colaterales.

**Patrón arquitectónico:** MVVM con lectura analítica multicapa — repositorio nuevo dedicado + reglas de cálculo puras + 3 pantallas de presentación (G1/G2/G3).

### Componentes afectados

#### 1. Domain Layer — Reglas Puras (Nuevo)

7 reglas funcionales puras en `domain/rules/` (ADR-06):

- **`ProgressionRateRule`** (CA-15.01): `fun calculate(positiveCount: Int, totalCount: Int): Double`
- **`LoadVelocityRule`** (CA-15.03, 15.04): `fun calculate(currentWeightKg: Double, initialWeightKg: Double, sessionCount: Int): Double`
- **`TonnageRule`** (CA-15.07, 15.09): `fun calculateForMuscleGroup(sets: List<SetForTonnage>): Map<String, Double>` + DTO `SetForTonnage`
- **`VolumeDistributionRule`** (CA-15.10): `fun calculate(setsByZone: Map<String, Int>, totalSets: Int): Map<String, Double>`
- **`AvgRirRule`** (CA-15.13): `fun calculate(rirValues: List<Int>): Double`
- **`AdherenceRule`** (CA-15.16): `fun calculate(completedSessions: Int, plannedSessions: Int): Double`
- **`TrendClassificationRule`** (CA-15.21): `fun classify(values: List<Double>): TrendDirection`

#### 2. Domain Layer — Modelos (Nuevo)

- **`TrendDirection`**: Enum `ASCENDING, STABLE, DECLINING`
- **`RirInterpretation`**: Enum `OPTIMAL, RISK_TOO_CLOSE, INSUFFICIENT_STIMULUS`
- **DTOs analíticos** (12): `AdherenceData`, `RirByModule`, `ExerciseProgressionRate`, `ExerciseLoadVelocity`, `MuscleGroupTonnage`, `MuscleGroupTrend`, `TonnageSnapshot`, `ClassificationCount`, `ExerciseSessionRange`, `SetTonnageData`, `SetDistributionData`, `ClassificationCountByGroup`

#### 3. Domain Layer — Repository Interface (Nuevo)

- **`MetricsRepository`**: Interfaz en `domain/repository/` con 11 contratos analíticos. Responsabilidad ortogonal a los 4 repositorios existentes.

#### 4. Data Layer — Repository Implementation (Nuevo)

- **`MetricsRepositoryImpl`**: Implementación en `data/repository/`. Recibe 5 DAOs inyectados: `SessionDao`, `ExerciseSetDao`, `SessionExerciseDao`, `ProfileDao`, `RotationStateDao`. Sin `@Singleton` en clase (inconsistente con `SessionRepositoryImpl`).

#### 5. Data Layer — DAO Queries Nuevos

- **`SessionDao`**: Agregar `getClosedSessionsOrdered()`, `countSessionsInWeek()`, `getSessionIdsByModuleInRange()`
- **`ExerciseSetDao`**: Agregar `getTonnageDataBySessionIds()`, `getRirValuesBySessionIds()`, `getAvgWeightByExerciseInSession()`, `getSetDistributionBySessionIds()`
- **`SessionExerciseDao`**: Agregar `getClassificationCountsByPeriod()`, `getExerciseSessionRangeByPeriod()`, `getClassificationCountsBySessionIds()`

#### 6. Domain Layer — Use Cases (Nuevo)

9 Use Cases en `domain/usecase/metrics/`:

| Use Case | Bloque | Descripción |
|---|---|---|
| `GetMicrocycleMapUseCase` | Base | Map\<Int, List\<Long\>\> de sesiones agrupadas — ADR-05 |
| `GetAdherenceUseCase` | C | Sesiones semana + weekly_frequency → AdherenceRule |
| `GetAvgRirByModuleUseCase` | C | Sets de N últimas sesiones → AvgRirRule × módulo |
| `GetProgressionRateUseCase` | A | Clasificaciones por ejercicio en período → ProgressionRateRule |
| `GetLoadVelocityUseCase` | A | Peso inicial/actual por ejercicio → LoadVelocityRule |
| `GetTonnageByMuscleGroupUseCase` | B | Sets+zones del microciclo → TonnageRule + padding 12 grupos |
| `GetVolumeDistributionUseCase` | B | Sets por zona por módulo → VolumeDistributionRule |
| `GetTonnageEvolutionUseCase` | E | Tonelaje por grupo × microciclos → lista TonnageSnapshot |
| `GetMuscleGroupTrendUseCase` | D | Tonelaje + tasa por grupo × microciclos → TrendClassificationRule (doble métrica) |

#### 7. UI Layer — G1 MetricsScreen (Nuevo)

Paquete: `ui.metrics`.

- **`MetricsUiState`**: Sealed class `Loading`, `Content(adherence, rirByModule, progressionRates, loadVelocities)`, `Error`
- **`MetricsViewModel`**: `@HiltViewModel`. KPIs G1, `changeProgressionPeriod()`, `changeRirPeriod()`
- **`MetricsScreen`**: G1 — 4 secciones + 2 quick links. `CenterAlignedTopAppBar` sin navigationIcon, title "Métricas". LazyColumn con: Card Adherencia, Card RIR, Tasa de Progresión, Velocidad de Carga, 2 TextButton links.

#### 8. UI Layer — G2 VolumeScreen (Nuevo)

Paquete: `ui.metrics`.

- **`VolumeUiState`**: `Loading`, `Content(tonnageByGroup, distributionByModule, evolution, selectedMicrocycle, totalMicrocycles, insufficientEvolution)`, `Error`
- **`VolumeViewModel`**: `@HiltViewModel`. Selector microciclo, recálculo tonnage/distribution.
- **`VolumeScreen`** + **`TonnageChartComposable`**: Stepper microciclo, 12 barras horizontales tonelaje, distribución % por módulo (15 zonas), gráfico Canvas multilínea.

#### 9. UI Layer — G3 TrendScreen (Nuevo)

Paquete: `ui.metrics`.

- **`TrendUiState`**: `Loading`, `Content(trends)`, `InsufficientData(remaining)`, `Error`
- **`TrendViewModel`**: `@HiltViewModel`. Guard: `completedCount < 4` → `InsufficientData`
- **`TrendScreen`**: Lista 12 grupos con clasificación ASCENDING/STABLE/DECLINING + colores e íconos.

#### 10. Navegación (Modificación)

- **`NavigationRoutes`**: Agregar `MUSCLE_VOLUME = "muscle-volume"`, `PROGRESSION_TREND = "progression-trend"`
- **`TensionNavHost`**: Reemplazar PlaceholderScreen L244-245 con MetricsScreen. Registrar G2 y G3.
- **`BottomNavigationBar`**: Agregar `childRoutes = setOf(MUSCLE_VOLUME, PROGRESSION_TREND)` al tab METRICS.

#### 11. DI (Modificación)

- **`RepositoryModule`**: Agregar 5to binding: `@Binds @Singleton abstract fun bindMetricsRepository(impl: MetricsRepositoryImpl): MetricsRepository`

---

### Decisiones de Diseño

**Decisión 1 — `MetricsRepository` es el quinto repositorio del sistema.**

Los 4 repositorios existentes gestionan entidades transaccionales individuales. HU-15 requiere consultas de agregación complejas que cruzan múltiples tablas. Mezclar en `SessionRepository` (ya con 18+ métodos) violaría SRP. `MetricsRepository` recibe SessionDao, ExerciseSetDao, SessionExerciseDao, ProfileDao, RotationStateDao.

**Decisión 2 — Los KPIs se calculan on-demand (al abrir G1), no al cierre de sesión.**

Calcularlas al cierre añadiría latencia al flujo crítico `closeSession()`, persistiría valores derivados que se invalidan con cada nueva sesión, y duplicaría datos. El volumen de datos es limitado (~4800 filas de exercise_set en un año). ADR-06 valida este enfoque: reglas puras + queries derivadas.

**Decisión 3 — La asociación de sesiones a microciclos se deriva del orden de cierre, no de una columna persistida.**

La tabla `session` NO tiene columna `microcycle_number`. La agrupación: sesiones cerradas (COMPLETED/INCOMPLETE) ordenadas por (date ASC, id ASC) → cada grupo de 6 sesiones consecutivas forma un microciclo. El microciclo parcial actual se incluye como entry final. Se encapsula en `MetricsRepository.getSessionIdsGroupedByMicrocycle()` → `Map<Int, List<Long>>`.

**Decisión 4 — 7 reglas puras en `domain/rules/` (ADR-06).**

Funciones puras sin estado ni dependencias externas. Cada una con guards para casos edge.

**Decisión 5 — Tendencia clasificada con pendiente lineal simplificada.**

`TrendClassificationRule.classify(values)`: regresión lineal simple `slope = Σ(xi - x̄)(yi - ȳ) / Σ(xi - x̄)²`. Umbral ±5% del valor medio para evitar clasificar ruido como tendencia. CA-15.20: se invoca DOS veces por grupo (tonelaje + tasa de progresión). Combinación más conservadora: DECLINING > STABLE > ASCENDING.

**Decisión 6 — RIR Promedio usa semanas ISO 8601 (lunes a domingo) y N últimas sesiones del módulo, no fecha.**

CA-15.14: "últimas 2 ejecuciones del mismo módulo" = N sesiones más recientes del módulo X (no un rango de fechas). `getSessionIdsByModuleInRange(moduleCode, limit)` con `ORDER BY s.date DESC, s.id DESC LIMIT :limit`.

**Decisión 7 — Tasa de Progresión filtra por período configurable (4/8/12 semanas) usando `session.date`.**

CA-15.02: período 4 semanas por defecto. Query: contar sesiones donde `progression_classification = 'POSITIVE_PROGRESSION'` vs. total (IS NOT NULL) dentro de `date >= startDate AND s.deload_id IS NULL`.

**Decisión 8 — Velocidad de Carga: peso promedio de series de la primera/última sesión en el período.**

"Peso inicial" = `AVG(weight_kg)` de series de la primera sesión del ejercicio en el período. "Peso actual" = `AVG(weight_kg)` de la última sesión. Denominador: `sessionCount - 1` (sesiones intermedias). Si 1 sesión → velocidad = 0.

**Decisión 9 — Tonelaje multi-zona: contabilización al 100% en cada grupo.**

CA-15.09: el JOIN exercise_set → exercise_muscle_zone → muscle_zone produce DUPLICADOS intencionales — una fila por muscle_group del ejercicio. `TonnageRule` suma normalmente → el tonelaje completo aparece en cada grupo.

**Decisión 10 — Distribución de Volumen por zona muscular (15), no por grupo (12).**

RF49 y MDS KPI 6 dicen "porcentaje de series totales de cada **zona muscular**". Permite detectar imbalances intra-grupo (ej: Pecho Medio 60% vs Pecho Superior 15%). `getSetDistributionBySessionIds()` usa `GROUP BY mz.name` (15 zonas) por módulo.

**Decisión 11 — G1 reemplaza el `PlaceholderScreen` existente en la ruta `METRICS`.**

`NavigationRoutes.METRICS = "metrics"` ya existe en TensionNavHost L244-245 con placeholder. G1 tiene 4 secciones: Adherencia, RIR Promedio, Tasa de Progresión, Velocidad de Carga + 2 quick links → G2, G3.

**Decisión 12 — G2 y G3 son pantallas independientes con sus propios ViewModels.**

G2 (Volumen): selector microciclo stepper ◀▶ + barras tonelaje + distribución % + gráfico multilínea. G3 (Tendencia): lista 12 grupos con clasificación ASCENDING/STABLE/DECLINING.

**Decisión 13 — Gráfico de evolución temporal (G2) es un Canvas composable custom, sin librería externa.**

Composable `TonnageChartComposable`: ejes, líneas, puntos, leyenda. Máximo 12 líneas × 20 puntos = 240 draw calls. Sin interpolación spline. Si se decide usar Vico/YCharts en el futuro, solo cambia el composable.

**Decisión 14 — 9 Use Cases orquestan Repository + Rules por bloque de KPIs.**

Ya detallados en Componentes afectados §6.

**Decisión 15 — Estados vacíos y datos insuficientes como variantes explícitas del UiState.**

- G3 (Tendencia): `microcycleCount < 4` → `TrendUiState.InsufficientData(remaining)` (CA-15.23)
- G2 (Evolución): `totalMicrocycles < 2` → `VolumeUiState.Content.insufficientEvolution = true` (CA-15.28)

**Decisión 16 — Rutas G2 y G3 en NavigationRoutes.**

`MUSCLE_VOLUME = "muscle-volume"` → G2. `PROGRESSION_TREND = "progression-trend"` → G3. Bottom Navigation mantiene tab Métricas activo en G2/G3 vía `childRoutes` en `BottomNavigationBar`.

**Decisión 17 — Agrupación por microciclo implementada en Kotlin, no SQL.**

SQLite no soporta window functions robustas. Lógica: (1) `getClosedSessionsOrdered()` — ORDER BY date ASC, id ASC. (2) `chunked(6)` → Map<Int, List<Long>> (microcicle → sessionIds). (3) El bloque parcial final (< 6 sesiones) = microciclo en progreso. Las sesiones de deload participan normalmente (CA-14.13).

**Decisión 18 — Selectores de período en G1 implementados como `ExposedDropdownMenuBox` M3.**

- Tasa de Progresión: "4 semanas" (defecto), "8 semanas", "12 semanas"
- RIR Promedio: "2 últimas sesiones" (defecto), "4 últimas sesiones", "6 últimas sesiones"

Los cambios de período disparan recarga del KPI via `viewModelScope.launch`. No se persisten.

---

### Componentes Nuevos (Resumen)

| # | Componente | Capa | Responsabilidad |
|---|---|---|---|
| 1-7 | 7 Rules | Domain (rules) | Funciones puras — ADR-06 |
| 8 | `TrendDirection` | Domain (model) | Enum: ASCENDING, STABLE, DECLINING |
| 9 | `MetricsRepository` | Domain (repository) | Queries analíticas agregadas |
| 10 | `MetricsRepositoryImpl` | Data (repository) | Implementación — 5 DAOs inyectados |
| 11-19 | 9 Use Cases | Domain (usecase/metrics) | Orquestación Repository + Rules |
| 20-21 | `MetricsUiState`, `MetricsViewModel` | UI (metrics) | KPIs G1 |
| 22 | `MetricsScreen` | UI (metrics) | G1 — 4 secciones + 2 quick links |
| 23-24 | `VolumeUiState`, `VolumeViewModel` | UI (metrics) | KPIs G2 |
| 25 | `VolumeScreen` | UI (metrics) | G2 — selector + barras + distribución + gráfico |
| 26 | `TonnageChartComposable` | UI (metrics) | Canvas multilínea tonelaje temporal |
| 27-28 | `TrendUiState`, `TrendViewModel` | UI (metrics) | Tendencias G3 |
| 29 | `TrendScreen` | UI (metrics) | G3 — lista 12 grupos + clasificación |
| 30+ | DTOs (12) | Domain (model) | `AdherenceData`, `RirByModule`, `SetTonnageData`, etc. |

### Componentes Modificados

| # | Componente | Modificación | Nivel |
|---|---|---|---|
| 1 | `SessionDao` | Agregar 3 queries analíticos | Medio |
| 2 | `ExerciseSetDao` | Agregar 4 queries analíticos + DTOs | Medio |
| 3 | `SessionExerciseDao` | Agregar 3 queries analíticos + DTOs | Medio |
| 4 | `NavigationRoutes` | Agregar `MUSCLE_VOLUME`, `PROGRESSION_TREND` | Menor |
| 5 | `TensionNavHost` | Reemplazar PlaceholderScreen, registrar G2/G3 | Medio |
| 6 | `RepositoryModule` | Agregar 5to binding | Menor |
| 7 | `BottomNavigationBar` | Agregar `childRoutes` al tab METRICS | Menor |

---

### Notas Técnicas

**Nota 1 — Agrupación por microciclo asume sesiones consecutivas de 6.**

El sistema no permite eliminar sesiones y `advanceRotation()` siempre avanza secuencialmente. Las sesiones IN_PROGRESS se excluyen. Las sesiones INCOMPLETE sí cuentan (avanzan la rotación — CA-09.05).

**Nota 2 — Sesiones de deload en cálculos.**

Tonelaje G2 SÍ incluye deload (dip del 60% es informativo — HU-17 verificará si caída es por descarga planificada). Tasa de Progresión, Velocidad de Carga y RIR NO incluyen deload (las cargas reducidas artificialmente penalizarían métricas de rendimiento real).

**Nota 3 — Gráfico Canvas custom.**

Composable `TonnageChartComposable`: ejes (Label Small, On Surface Variant), líneas (Primary #8B1A1A, Secondary #6B4F4F, Tertiary #5C6B4F + variantes), grid dashed (Outline Variant), leyenda inferior. Edge case: todos grupos con tonelaje 0 → eje Y mínimo 1 Kg.

**Nota 4 — Sin nueva tabla ni migración.**

HU-15 es 100% lectura. No se crea ninguna entidad Room nueva, no se modifica ninguna tabla, no se requiere migration. `TensionDatabase` version=6 se mantiene.

**Nota 5 — `SetForTonnage` vs `SetTonnageData`.**

`SetTonnageData` es el DTO del DAO. `SetForTonnage` es el input de `TonnageRule`. El mapping `.map { SetForTonnage(it.weightKg, it.reps, it.muscleGroup) }` es intencional — separación semántica DAO/Domain.

**Nota 6 — Accesos F3 desde G1 ya existen.**

`NavigationRoutes.EXERCISE_HISTORY = "exercise-history/{exerciseId}"` ya existe. La navegación G1→F3 usa `navController.navigate("exercise-history/$exerciseId")`. `ExerciseHistoryScreen` ya registrado en TensionNavHost.

**Nota 7 — Distribución de Volumen por módulo (Nota sobre CA-15.10).**

CA-15.10 dice "porcentaje de series totales de cada zona muscular respecto al total de series **del módulo**". La distribución se calcula POR MÓDULO (A, B, C) sobre 15 zonas musculares. G2 muestra 3 sub-secciones una por módulo.

**Nota 8 — Query de Tasa de Progresión excluye deload y sin clasificación.**

Filtros: `AND s.deload_id IS NULL` (excluir deload) + `AND se.progression_classification IS NOT NULL` (excluir primera sesión sin historial).

---

### Verificación Cruzada de CAs (28/28)

| CA | Estado | Mecanismo |
|---|---|---|
| CA-15.01 | Done | `ProgressionRateRule.calculate()` + `getClassificationCountsByPeriod()` |
| CA-15.02 | Done | Dropdown G1 (4/8/12 semanas) → `MetricsViewModel.changeProgressionPeriod()` |
| CA-15.03 | Done | `LoadVelocityRule.calculate(current, initial, sessions)` → Kg/sesión |
| CA-15.04 | Done | `LoadVelocityRule`: si `current == initial` → 0.0 |
| CA-15.05 | Done | Guard `isBodyweight || isIsometric` en `GetLoadVelocityUseCase` → "N/A" en UI |
| CA-15.06 | Done | G1 Secciones 3+4: lista por ejercicio con % y Kg/sesión |
| CA-15.07 | Done | `TonnageRule.calculateForMuscleGroup(sets)` + `getTonnageDataBySessionIds()` |
| CA-15.08 | Done | JOIN exercise_muscle_zone → muscle_zone. `mz.muscle_group` como GROUP BY |
| CA-15.09 | Done | JOIN produce duplicados intencionales → 100% en cada grupo |
| CA-15.10 | Done | `VolumeDistributionRule` + `getSetDistributionBySessionIds()` GROUP BY `mz.name` (15 zonas) |
| CA-15.11 | Done | `getSessionIdsGroupedByMicrocycle()` → chunked(6) en Kotlin |
| CA-15.12 | Done | G2: barras tonelaje + distribución % + selector microciclo |
| CA-15.13 | Done | `AvgRirRule.calculate(rirValues)` → promedio 1 decimal |
| CA-15.14 | Done | Dropdown G1 (2/4/6 sesiones del módulo) → `changeRirPeriod()` |
| CA-15.15 | Done | `RirInterpretation` enum + badges coloreados + referencia textual |
| CA-15.16 | Done | `AdherenceRule.calculate(completed, planned)` capped 100% |
| CA-15.17 | Done | `profile.weekly_frequency` vía `ProfileDao.getProfile()` |
| CA-15.18 | Done | Todos los queries: `status IN ('COMPLETED', 'INCOMPLETE')` |
| CA-15.19 | Done | G1 Secciones 1+2: card adherencia + card RIR por módulo |
| CA-15.20 | Done | `GetMuscleGroupTrendUseCase` — doble métrica (tonelaje + tasa progresión) |
| CA-15.21 | Done | `TrendClassificationRule.classify()` pendiente lineal ±5% |
| CA-15.22 | Done | Últimos 4-6 microciclos completados |
| CA-15.23 | Done | Guard: `completedCount < 4` → `TrendUiState.InsufficientData(remaining)` |
| CA-15.24 | Done | G3: lista de 12 grupos con tendencia desglosada |
| CA-15.25 | Done | `GetTonnageEvolutionUseCase` → `TonnageChartComposable` Canvas G2 |
| CA-15.26 | Done | Gráfico multilínea permite visualizar trayectorias |
| CA-15.27 | Done | 12 grupos canónicos paddeados en `GetTonnageByMuscleGroupUseCase` |
| CA-15.28 | Done | Guard: `totalMicrocycles < 2` → mensaje en G2 |

---

### Referencias y Validación

**Documentación consultada:**
- MDS §7.A KPIs 1-6
- MDS §7.B Umbrales de Tolerancia
- Modelo de Datos §3.2, §3.5, §3.8, §3.10, §3.11, §3.12, §3.14
- ADR-06 (rules puras), ADR-05 (dirección de dependencias), ADR-18 (JUnit 4)
- Wireframes G1, G2, G3
- Especificación Visual §G1, §G2, §G3, §4.3
- Requerimientos RF42, RF44-49, RF52

**Validado por:** esteban.colorado | **Fecha:** 2026-02-18 | **Enfoque:** Exploratorio
