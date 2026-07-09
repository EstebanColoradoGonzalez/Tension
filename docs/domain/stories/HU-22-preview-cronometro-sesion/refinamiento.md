# Refinamiento Técnico (Developer)

**Autor**: Esteban Colorado González | **Fecha**: 2026-03-05

---

## Consideraciones Generales

**Basado en análisis arquitectónico:**
Sí — Análisis Arquitectónico (Arquitecto) del 2026-03-05 con exploración verificada del código existente y auditoría de 15 defectos corregidos. 6 hitos de implementación, 2 bloques funcionales (Preview de Sesión + Cronómetro Isométrico), 14 componentes afectados.

**Nivel de complejidad:**
ALTA — Dos mejoras funcionales independientes que tocan múltiples capas (DAO, Repository, UseCase, ViewModel, Screen, Navigation). El Preview requiere un componente vertical completo nuevo (7 componentes: DTO, query, modelo, repo, UseCase, ViewModel, Screen). El Cronómetro requiere lógica de timer con `SystemClock.elapsedRealtime()`, manejo de estados (IDLE/RUNNING/STOPPED), señalización visual accesible con colores semánticos, y compatibilidad con background. Ambas mejoras interactúan con deload (HU-14), sustitución (HU-07), ejercicios custom (RF62) y reps especiales (`_SEC`, `TO_TECHNICAL_FAILURE`). 19 CAs a cubrir.

**Riesgos técnicos conocidos:**
1. **Inconsistencia de cargas preview ↔ sesión (CA-22.07):** Si otra sesión se cierra entre preview y start, `exercise_progression.prescribed_load_kg` puede cambiar. Mitigación: ambos leen del mismo `exercise_progression` via LEFT JOIN; CA-22.07 contempla el escenario explícitamente.
2. **Timer en background prolongado:** Si la app permanece >5 min en background, el OS puede suspender la corrutina. Mitigación: `SystemClock.elapsedRealtime()` calcula delta real al despertar, no depende del tick de la corrutina.
3. **Fallback isométricos sin rango `_SEC` (CA-22.16):** Ejercicio custom isométrico asignado con reps `"8-12"` por error. Mitigación: condición `isIsometric && !range.isSeconds` → `minSeconds=30, maxSeconds=60` con indicación de rango por defecto.
4. **RNF02 (max 3 toques) con detención manual:** Flujo con stop manual = 4 interacciones (Start + Stop + RIR + Confirm). Solo auto-stop = 3. Desviación justificada: elimina cambio de contexto a app externa (6+ interacciones). Beneficio neto significativo.

**Patrones y convenciones del equipo:**
- MVVM + Domain Layer con `StateFlow` + `SharedFlow` (`_uiState` privado / `uiState` público).
- Hilt `@HiltViewModel` + `SavedStateHandle` para argumentos de navegación.
- Room queries con `LEFT JOIN` + `COALESCE` para datos opcionales (patrón de `SessionExerciseDao`).
- `viewModelScope.launch` para coroutines. Domain layer Kotlin puro sin dependencias Android.
- `collectAsStateWithLifecycle()` en Composables. Lambdas `onNavigateTo*` para navegación.
- Convención de nombres: `{Feature}Screen`, `{Feature}ViewModel`, `{Feature}UiState`, `{Acción}{Entidad}UseCase`.

**Dependencias nuevas a instalar:**
Ninguna. `SystemClock.elapsedRealtime()` pertenece al SDK de Android (paquete `android.os`).

**Estrategia de testing:**
JUnit 4 + MockK | Tests unitarios (domain/util, UseCase, ViewModel) | Cobertura: 100% utilidades + UseCase, estados clave en ViewModels | Builders: no requeridos — datos inline (patrón existente)

---

## Historias Relacionadas Consultadas

**Implementaciones similares analizadas:**
- HU-04: `getDetailsByModuleVersionId()` en `PlanAssignmentDao`, `mapRepsToDisplay()` con mapeo de formatos especiales.
- HU-05: `startSession()` / `StartSessionUseCase`, crash recovery, carga objetivo (`prescribedLoadKg`).
- HU-06: Registro de serie (`RegisterSetScreen`/E2), campo `reps` almacena segundos para isométricos.
- HU-07: Sustitución de ejercicios — sustitutos no tienen `plan_assignment`, afecta fallback del cronómetro.
- HU-08: Cross-cutting isométrico (peso=0, "Segundos sostenidos", referencia "30-45 seg").
- HU-14: Protocolo de descarga — 7 variantes de `loadDisplayText`, `DeloadLoadRule`, indicador visual deload.
- HU-16: Migración con formato `'30-45_SEC'` en PlanSeeder.
- HU-21: `sort_order` en `plan_assignment`, ORDER BY COALESCE, badge "Fuera del gym".

**Patrones de código reutilizados:**
- 7 variantes de `loadDisplayText` de `ActiveSessionViewModel` (líneas 70-87) → extraídas a `LoadDisplayMapper`.
- `mapRepsToDisplay()` de `PlanVersionDetailViewModel.Companion` (línea 145) → extraído a `RepsDisplayMapper`.
- LEFT JOIN `exercise_progression` de `SessionExerciseDao.getBySessionIdWithDetails()` → mismo patrón para query del preview.
- ORDER BY `pa.sort_order ASC` de `PlanAssignmentDao.getDetailsByModuleVersionId()` → reutilizado en query del preview.
- Timer con `SystemClock.elapsedRealtime()` — patrón estándar Android para timers resilientes a background.
- Patrón de ruta con argumentos múltiples: `"session-preview/{moduleVersionId}/{moduleCode}/{versionNumber}"` siguiendo `"active-session/{sessionId}"`.

**Mejores prácticas aplicadas:**
- Extracción de utilidades compartidas a `domain/util/` para evitar duplicación (DRY) entre 3+ consumidores.
- Preview como query read-only sin side effects en BD (CA-22.01, CA-22.03).
- Composable condicional (`IsometricChronometer` vs `RepsField`) basado en tipo de ejercicio, sin modificar flujo de ejercicios no isométricos (CA-22.18).
- Accesibilidad dual color + ícono + texto para señalización del cronómetro (RNF05).
- `SystemClock.elapsedRealtime()` en lugar de `System.currentTimeMillis()` para resiliencia a background (CA-22.17).

---

## Debug Log

| # | Tipo | Descripción | Resolución |
|---|------|-------------|------------|
| 1 | Error compilación | Test `GetRegisterSetInfoUseCaseTest` falló por falta del nuevo parámetro `prescribedReps` en `RegisterSetInfo` | Agregado `prescribedReps = "8-12"` al objeto test |
| 2 | Bug potencial | Timer auto-stop podía mostrar `maxSeconds+1` antes de detenerse | Reordenada lógica: verificar límite ANTES de actualizar estado, clamping a `max` |
| 3 | Tipo incorrecto | DTO `SessionPreviewExerciseDto.muscleZones` era `String` no-null pero SQL `GROUP_CONCAT` puede retornar NULL | Cambiado a `String?` para reflejar semántica SQL correcta |

---

## Completion Notes

- **Fase 1:** 3 utilidades compartidas en `domain/util/` (RepsRangeParser, LoadDisplayMapper, RepsDisplayMapper) + 3 archivos de test (18 test cases). Refactorización de ActiveSessionViewModel y PlanVersionDetailViewModel para usar utilidades compartidas.
- **Fase 2:** DTO `SessionPreviewExerciseDto` + query `getPreviewByModuleVersionId()` con 7 JOINs en PlanAssignmentDao. Método `getSessionPreviewExercises()` en SessionRepository + implementación en SessionRepositoryImpl.
- **Fase 3:** `GetSessionPreviewUseCase` como delegación simple + 2 test cases.
- **Fase 4:** UiState + ViewModel deload-aware + SessionPreviewScreen completa con DeloadBanner, LazyColumn, badge "Fuera del gym", CTA. Ruta SESSION_PREVIEW + navegación desde HomeScreen (card clickable para preview, botón para inicio directo).
- **Fase 5:** Extensión de SetExerciseInfo (campo `reps`), query `getExerciseInfoForSet()` actualizada, RegisterSetInfo extendido con `prescribedReps`, mapeo en SessionRepositoryImpl.
- **Fase 6:** TimerState enum + 5 campos cronómetro en RegisterSetUiState + isConfirmEnabled condicional. Timer con SystemClock.elapsedRealtime() resiliente a background, auto-stop al máximo, RepsRangeParser + fallback isométrico. IsometricChronometer composable con CircularProgressIndicator, colores semánticos (error/progressionPositive), señalización dual color+ícono+texto, 3 botones contextuales. Condicional Cronómetro vs RepsField en RegisterSetScreen.
- **Auditoría:** 108 tareas marcadas completadas en §7. Audit conformance report: 12 items PASS, 2 issues corregidos (timer precision + DTO nullability), 3 issues pre-existentes no atribuibles a HU-22.

---

## Tareas de Implementación

### Fase 1: Utilidades de Parsing y Display (Hito 1)

#### Domain — Utilidades (`domain/util/`)

- [x] **Crear `RepsRangeParser`** (AC: CA-22.08, CA-22.09, CA-22.10, CA-22.15, CA-22.16)
  - [x] Crear data class `RepsRange(val min: Int, val max: Int, val isSeconds: Boolean)` — Archivo: `domain/util/RepsRangeParser.kt`
  - [x] Crear object `RepsRangeParser` con `fun parse(reps: String): RepsRange` — Archivo: `domain/util/RepsRangeParser.kt`
  - [x] Implementar lógica: detectar sufijo `_SEC`, split por `-`, defaults min=30 / max=60 para fallback
  - [x] Test unitario — Archivo: `src/test/.../domain/util/RepsRangeParserTest.kt`
    - Caso `"30-45_SEC"` → `RepsRange(30, 45, true)`
    - Caso `"8-12"` → `RepsRange(8, 12, false)`
    - Caso `"TO_TECHNICAL_FAILURE"` → `isSeconds=false`
    - Caso `"30-60_SEC"` → `RepsRange(30, 60, true)`
    - Caso input malformado → defaults seguros

- [x] **Crear `LoadDisplayMapper`** (AC: CA-22.01, CA-22.04, CA-22.05, CA-22.07)
  - [x] Crear object `LoadDisplayMapper` con `fun mapLoadDisplay(isDeload: Boolean, isIsometric: Boolean, isBodyweight: Boolean, prescribedLoadKg: Double?, loadIncrementKg: Double): String` — Archivo: `domain/util/LoadDisplayMapper.kt`
  - [x] Implementar 7 variantes en orden de precedencia (extraer de `ActiveSessionViewModel` líneas 70-87):
    1. `isDeload && isIsometric` → `"Isométrico (30s)"`
    2. `isDeload && isBodyweight` → `"Peso corporal (8 reps objetivo)"`
    3. `isDeload && prescribedLoadKg != null` → `"🔄 {deloadLoad} Kg"` (via `DeloadLoadRule.calculateDeloadLoad()`)
    4. `isIsometric` → `"Isométrico (30–45s)"`
    5. `isBodyweight` → `"Peso corporal"`
    6. `prescribedLoadKg != null` → `"{prescribedLoadKg} Kg"`
    7. `else` → `"Sin historial — establecer carga"`
  - [x] Test unitario — Archivo: `src/test/.../domain/util/LoadDisplayMapperTest.kt`
    - 7 variantes + edge cases (null prescribedLoadKg, loadIncrementKg = 0)

- [x] **Crear `RepsDisplayMapper`** (AC: CA-22.04)
  - [x] Crear object `RepsDisplayMapper` con `fun mapRepsToDisplay(reps: String): Pair<String, Boolean>` — Archivo: `domain/util/RepsDisplayMapper.kt`
  - [x] Implementar 3 variantes:
    - `"TO_TECHNICAL_FAILURE"` → `"Al fallo técnico" to true`
    - `"*_SEC"` (ej: `"30-45_SEC"`) → `"30–45 seg" to true`
    - Cualquier otro → `"$reps reps" to false`
  - [x] Test unitario — Archivo: `src/test/.../domain/util/RepsDisplayMapperTest.kt`

#### UI — Refactorización de Consumidores Existentes

- [x] **Refactorizar `PlanVersionDetailViewModel`**
  - [x] Reemplazar `companion object { mapRepsToDisplay() }` por llamada a `RepsDisplayMapper.mapRepsToDisplay()` — Archivo: `ui/catalog/PlanVersionDetailViewModel.kt`
  - [x] Eliminar el companion object con `mapRepsToDisplay()`
  - [x] Agregar import de `RepsDisplayMapper`

- [x] **Refactorizar `ActiveSessionViewModel`**
  - [x] Reemplazar bloque inline `val loadText = when { ... }` (líneas 70-87) por llamada a `LoadDisplayMapper.mapLoadDisplay(isDeload, detail.isIsometric, detail.isBodyweight, detail.prescribedLoadKg, detail.loadIncrementKg)` — Archivo: `ui/session/ActiveSessionViewModel.kt`
  - [x] Agregar import de `LoadDisplayMapper`

### Fase 2: Capa de Datos del Preview (Hito 2)

#### Domain — Modelo

- [x] **Crear modelo `SessionPreviewExercise`** (AC: CA-22.01)
  - [x] Data class con 12 campos: `exerciseId: Long`, `exerciseName: String`, `moduleCode: String`, `equipmentTypeName: String`, `muscleZones: String`, `sets: Int`, `reps: String`, `isBodyweight: Boolean`, `isIsometric: Boolean`, `isToTechnicalFailure: Boolean`, `prescribedLoadKg: Double?`, `loadIncrementKg: Double` — Archivo: `domain/model/SessionPreviewExercise.kt`

#### Data — DAO y Repository

- [x] **Crear DTO `SessionPreviewExerciseDto`** (AC: CA-22.01)
  - [x] Data class con 12 campos mapeados a columnas SQL (Int para booleans: `isBodyweight`, `isIsometric`, `isToTechnicalFailure`) — Archivo: `data/local/dao/PlanAssignmentDao.kt`

- [x] **Crear query `getPreviewByModuleVersionId()`** (AC: CA-22.01, CA-22.04, CA-22.07)
  - [x] SELECT 12 campos con JOINs: `exercise`, `equipment_type`, `module`, `exercise_muscle_zone`, `muscle_zone`, `exercise_progression` — Archivo: `data/local/dao/PlanAssignmentDao.kt`
  - [x] ORDER BY `pa.sort_order ASC`
  - [x] Retorna `Flow<List<SessionPreviewExerciseDto>>`
  - [x] SQL verificado:
    ```sql
    SELECT e.id AS exerciseId, e.name AS exerciseName, e.module_code AS moduleCode,
      et.name AS equipmentTypeName, GROUP_CONCAT(mz.name, ', ') AS muscleZones,
      pa.sets, pa.reps, e.is_bodyweight AS isBodyweight, e.is_isometric AS isIsometric,
      e.is_to_technical_failure AS isToTechnicalFailure,
      ep.prescribed_load_kg AS prescribedLoadKg, m.load_increment_kg AS loadIncrementKg
    FROM plan_assignment pa
    INNER JOIN exercise e ON pa.exercise_id = e.id
    INNER JOIN equipment_type et ON e.equipment_type_id = et.id
    INNER JOIN module m ON e.module_code = m.code
    LEFT JOIN exercise_muscle_zone emz ON e.id = emz.exercise_id
    LEFT JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
    LEFT JOIN exercise_progression ep ON e.id = ep.exercise_id
    WHERE pa.module_version_id = :moduleVersionId
    GROUP BY e.id
    ORDER BY pa.sort_order ASC
    ```

- [x] **Agregar método a `SessionRepository`** (AC: CA-22.01)
  - [x] `fun getSessionPreviewExercises(moduleVersionId: Long): Flow<List<SessionPreviewExercise>>` — Archivo: `domain/repository/SessionRepository.kt`

- [x] **Implementar en `SessionRepositoryImpl`** (AC: CA-22.01)
  - [x] Mapeo DTO → modelo de dominio (Int → Boolean para flags `isBodyweight`, `isIsometric`, `isToTechnicalFailure`) — Archivo: `data/repository/SessionRepositoryImpl.kt`

### Fase 3: Capa de Dominio del Preview (Hito 3)

#### Domain — Use Case

- [x] **Crear `GetSessionPreviewUseCase`** (AC: CA-22.01)
  - [x] Clase con `@Inject constructor(private val sessionRepository: SessionRepository)` — Archivo: `domain/usecase/session/GetSessionPreviewUseCase.kt`
  - [x] `operator fun invoke(moduleVersionId: Long): Flow<List<SessionPreviewExercise>>`
  - [x] Test unitario con Repository mockeado — Archivo: `src/test/.../domain/usecase/session/GetSessionPreviewUseCaseTest.kt`

### Fase 4: UI del Preview (Hito 4)

#### UI — Preview (`ui/preview/`)

- [x] **Crear `SessionPreviewUiState`** (AC: CA-22.01, CA-22.06)
  - [x] Data class: `isLoading: Boolean`, `moduleCode: String`, `versionNumber: Int`, `moduleVersionId: Long`, `exercises: List<PreviewExerciseItem>`, `isDeloadActive: Boolean`, `deloadSessionsRemaining: Int` — Archivo: `ui/preview/SessionPreviewUiState.kt`
  - [x] Data class interna `PreviewExerciseItem`: `exerciseId: Long`, `name: String`, `equipmentTypeName: String`, `muscleZones: String`, `setsDisplay: String`, `repsDisplay: String`, `isRepsSpecial: Boolean`, `loadDisplayText: String`, `isBodyweight: Boolean`, `showOutOfGymBadge: Boolean`

- [x] **Crear `SessionPreviewViewModel`** (AC: CA-22.01, CA-22.02, CA-22.04, CA-22.05, CA-22.06, CA-22.07)
  - [x] `@HiltViewModel` con inyección de `GetSessionPreviewUseCase`, `StartSessionUseCase`, `SessionRepository` (para `getActiveDeload()`), `SavedStateHandle` — Archivo: `ui/preview/SessionPreviewViewModel.kt`
  - [x] `moduleVersionId`, `moduleCode`, `versionNumber` desde `SavedStateHandle`
  - [x] `init`: cargar preview exercises + deload state via `combine()`
  - [x] Para cada ejercicio: aplicar `LoadDisplayMapper.mapLoadDisplay()` con awareness de deload
  - [x] Para cada ejercicio: aplicar `RepsDisplayMapper.mapRepsToDisplay()` para formato legible
  - [x] Badge "Fuera del gym" cuando `isBodyweight && moduleCode == "A"`
  - [x] Acción `startSession()` → `StartSessionUseCase(moduleVersionId)` → emit sessionId via `SharedFlow`
  - [x] Test unitario — Archivo: `src/test/.../ui/preview/SessionPreviewViewModelTest.kt`

- [x] **Crear `SessionPreviewScreen`** (AC: CA-22.01, CA-22.02, CA-22.03, CA-22.04, CA-22.05)
  - [x] `CenterAlignedTopAppBar` con `navigationIcon: ArrowBack` → Home, título: "Módulo {code} — V{n}" — Archivo: `ui/preview/SessionPreviewScreen.kt`
  - [x] Card deload condicional bajo el header con color `semanticColors.deloadActive` (#1565C0 / #64B5F6) y texto "Modo Descarga — {n}/6 sesiones" (CA-14.07)
  - [x] `LazyColumn` con ejercicios en sort_order: nombre, zona muscular, equipamiento, sets × reps (con mapeo), carga objetivo (7 variantes)
  - [x] Badge "Fuera del gym" para ejercicios bodyweight del módulo A (CA-22.05)
  - [x] Botón CTA "Iniciar Sesión" al final

#### UI — Navegación

- [x] **Agregar ruta `SESSION_PREVIEW`** (AC: CA-22.02, CA-22.03)
  - [x] `const val SESSION_PREVIEW = "session-preview/{moduleVersionId}/{moduleCode}/{versionNumber}"` — Archivo: `ui/navigation/NavigationRoutes.kt`
  - [x] `fun sessionPreviewRoute(moduleVersionId: Long, moduleCode: String, versionNumber: Int) = "session-preview/$moduleVersionId/$moduleCode/$versionNumber"` — Archivo: `ui/navigation/NavigationRoutes.kt`

- [x] **Agregar composable en `TensionNavHost`** (AC: CA-22.02, CA-22.03)
  - [x] Nuevo `composable(route = NavigationRoutes.SESSION_PREVIEW, arguments = listOf(navArgument("moduleVersionId") { type = NavType.LongType }, navArgument("moduleCode") { type = NavType.StringType }, navArgument("versionNumber") { type = NavType.IntType }))` con `SessionPreviewScreen` — Archivo: `ui/navigation/TensionNavHost.kt`
  - [x] Bottom Navigation **visible** para esta ruta (NO agregar a lista de exclusión de `showBottomBar`)
  - [x] Callback `onNavigateToActiveSession` para inicio desde preview, con `popUpTo(HOME)` para limpiar preview del back stack

#### UI — Home

- [x] **Modificar `HomeScreen`** (AC: CA-22.01, CA-22.02, CA-22.06)
  - [x] Agregar callback `onNavigateToPreview: (Long, String, Int) -> Unit` a parámetros de `HomeScreen` — Archivo: `ui/home/HomeScreen.kt`
  - [x] Hacer `NextSessionCard` clickable: tap en el card → navega al preview (pasar `moduleVersionId`, `moduleCode`, `versionNumber` desde `uiState.nextSession`)
  - [x] Mantener botón "Iniciar Sesión" como acción alternativa directa (usuario elige: tap card → preview, tap botón → inicio directo)

- [x] **Actualizar `TensionNavHost`** para pasar callback de preview a `HomeScreen`
  - [x] Agregar `onNavigateToPreview` en el composable de HOME que invoca `navController.navigate(NavigationRoutes.sessionPreviewRoute(...))` — Archivo: `ui/navigation/TensionNavHost.kt`

- [x] **Agregar strings para preview** (AC: CA-22.01)
  - [x] Strings: título de pantalla, label "Iniciar Sesión" del CTA, textos de deload, badge "Fuera del gym" — Archivo: `res/values/strings.xml`

### Fase 5: Extensión del Modelo de Datos para Cronómetro (Hito 5)

#### Data — DAO

- [x] **Modificar DTO `SetExerciseInfo`** (AC: CA-22.08)
  - [x] Agregar campo `val reps: String` (6 → 7 campos) — Archivo: `data/local/dao/SessionExerciseDao.kt`

- [x] **Modificar query `getExerciseInfoForSet()`** (AC: CA-22.08)
  - [x] Agregar `COALESCE(pa.reps, '8-12') AS reps` al SELECT — Archivo: `data/local/dao/SessionExerciseDao.kt`

#### Domain — Modelo

- [x] **Modificar `RegisterSetInfo`** (AC: CA-22.08)
  - [x] Agregar campo `val prescribedReps: String` (9 → 10 campos) — Archivo: `domain/model/RegisterSetInfo.kt`

#### Data — Repository

- [x] **Modificar `SessionRepositoryImpl.getRegisterSetInfo()`** (AC: CA-22.08)
  - [x] Mapear `info.reps` → `prescribedReps` en la construcción de `RegisterSetInfo` — Archivo: `data/repository/SessionRepositoryImpl.kt`

### Fase 6: Cronómetro — ViewModel + Composable (Hito 6)

#### UI — Estado y ViewModel

- [x] **Crear enum `TimerState`** (AC: CA-22.08)
  - [x] Valores: `IDLE`, `RUNNING`, `STOPPED` — Archivo: `ui/session/RegisterSetUiState.kt`

- [x] **Modificar `RegisterSetUiState`** (AC: CA-22.08, CA-22.10, CA-22.11, CA-22.12)
  - [x] Agregar campos: `timerState: TimerState = TimerState.IDLE`, `timerSeconds: Int = 0`, `minSeconds: Int? = null`, `maxSeconds: Int? = null`, `showChronometer: Boolean = false` — Archivo: `ui/session/RegisterSetUiState.kt`
  - [x] Actualizar `isConfirmEnabled`: cuando `showChronometer`, habilitar solo si `timerState == TimerState.STOPPED && selectedRir != null`

- [x] **Modificar `RegisterSetViewModel`** (AC: CA-22.08, CA-22.09, CA-22.10, CA-22.11, CA-22.12, CA-22.16, CA-22.17, CA-22.19)
  - [x] En bloque `init`: parsear `info.prescribedReps` con `RepsRangeParser.parse()` — Archivo: `ui/session/RegisterSetViewModel.kt`
  - [x] Si `range.isSeconds` → `showChronometer=true`, `minSeconds=range.min`, `maxSeconds=range.max`
  - [x] Si `info.isIsometric && !range.isSeconds` → fallback `showChronometer=true`, `minSeconds=30`, `maxSeconds=60` (CA-22.16)
  - [x] Implementar `onStartTimer()`: lanzar corrutina con `SystemClock.elapsedRealtime()` en loop ~100ms, actualizar `timerSeconds` como delta en segundos
  - [x] Implementar `onStopTimer()`: cancelar corrutina, asignar `reps = timerSeconds.toString()`, `timerState = STOPPED`
  - [x] Implementar `onResetTimer()`: `STOPPED → IDLE`, limpiar `reps` y `timerSeconds` para permitir reintento
  - [x] Auto-stop: cuando `timerSeconds >= maxSeconds` → llamar `onStopTimer()` automáticamente (CA-22.09)
  - [x] Test unitario para estados del timer — Archivo: `src/test/.../ui/session/RegisterSetViewModelTest.kt`
    - Caso: `init` con reps `"30-45_SEC"` → `showChronometer=true, minSeconds=30, maxSeconds=45`
    - Caso: `init` con reps `"8-12"` + `isIsometric=true` → fallback `showChronometer=true, minSeconds=30, maxSeconds=60`
    - Caso: `init` con reps `"8-12"` + `isIsometric=false` → `showChronometer=false`
    - Caso: `onStartTimer` → `timerState=RUNNING`
    - Caso: `onStopTimer` a 33s → `timerState=STOPPED, reps="33"`
    - Caso: `onResetTimer` → `timerState=IDLE, timerSeconds=0`

#### UI — Composable del Cronómetro

- [x] **Crear `IsometricChronometer`** (AC: CA-22.08, CA-22.09, CA-22.10, CA-22.11, CA-22.13, CA-22.14)
  - [x] Display grande de segundos en tipografía `headlineLarge` — Archivo: `ui/session/components/IsometricChronometer.kt`
  - [x] `CircularProgressIndicator` determinado (`progress = timerSeconds.toFloat() / maxSeconds.toFloat()`)
  - [x] Colores semánticos (ADR-12): `MaterialTheme.colorScheme.error` (#BA1A1A) pre-mínimo → `semanticColors.progressionPositive` (#2E7D32 / #81C784) dentro del rango. NO usar `Primary` (#8B1A1A)
  - [x] Texto de estado accesible (RNF05): "⚠️ Bajo el mínimo" (zona roja) → "✅ En rango" (zona verde) — señalización dual color + ícono + texto
  - [x] Texto soporte estático: "Rango: {min}–{max} seg"
  - [x] Botones: "Iniciar" cuando `IDLE` → "Detener" cuando `RUNNING` → "Reiniciar" cuando `STOPPED`
  - [x] Indicación visual si se detiene antes del mínimo (CA-22.11): texto "Tiempo por debajo del rango prescrito" en color `error`

- [x] **Modificar `RegisterSetScreen`** (AC: CA-22.08, CA-22.14, CA-22.18)
  - [x] Condicional: cuando `uiState.showChronometer == true`, reemplazar `RepsField(...)` por `IsometricChronometer(...)` — Archivo: `ui/session/RegisterSetScreen.kt`
  - [x] `WeightField` y `RirSelector` permanecen inalterados
  - [x] Pasar callbacks `onStartTimer`, `onStopTimer`, `onResetTimer` del ViewModel

#### Recursos

- [x] **Agregar strings para cronómetro** (AC: CA-22.08, CA-22.13)
  - [x] Labels de botones: "Iniciar", "Detener", "Reiniciar" — Archivo: `res/values/strings.xml`
  - [x] Textos de estado: "Bajo el mínimo", "En rango", "Tiempo por debajo del rango prescrito"
  - [x] Texto soporte: "Rango: %1$d–%2$d seg"

### Fase N: QA y Deployment

#### Code Quality

- [ ] **Ejecutar Agente Peer Review** (MANUAL)
- [ ] **Resolver incidentes del Peer Review** (MANUAL, condicional)

#### Deployment DEV

- [ ] **Crear Pull Request** (MANUAL)
- [ ] **Ejecutar pipeline deployment DEV** (MANUAL)

#### Testing Manual

- [ ] **Diseñar set de pruebas manuales** (MANUAL)
  - Preview: Tap en NextSessionCard → pantalla preview con lista de ejercicios, cargas objetivo, orden sugerido, badge "Fuera del gym" (módulo A)
  - Preview: Verificar que NO se crean registros en BD durante visualización (CA-22.01)
  - Preview: Botón "Iniciar Sesión" → transición a E1 con cargas consistentes (CA-22.02, CA-22.07)
  - Preview: Retorno con botón atrás sin consecuencias, misma prescripción al re-entrar (CA-22.03)
  - Preview: Con sesión activa en progreso → preview NO visible, solo ResumeCard (CA-22.06)
  - Preview: Con deload activo → card informativo azul + cargas al 60%
  - Cronómetro: Registrar serie de Plancha (30-45s) → cronómetro visual en lugar de input numérico (CA-22.08)
  - Cronómetro: Dejar correr hasta 45s → auto-stop, valor capturado automáticamente (CA-22.09)
  - Cronómetro: Detener a 33s (dentro del rango) → zona verde, valor "33" registrado (CA-22.10)
  - Cronómetro: Detener a 20s (antes del mínimo) → indicación "por debajo del rango", dato se registra (CA-22.11)
  - Cronómetro: Ejercicio no isométrico (Sentadilla) → flujo estándar sin cronómetro (CA-22.18)
  - Cronómetro: App a background durante timer → al regresar, tiempo real correcto (CA-22.17)
  - Cronómetro: Peso fijo en 0 Kg durante registro isométrico (CA-22.14)
- [ ] **Ejecutar pruebas manuales** (MANUAL)

### File List

| Acción | Archivo | Descripción |
|--------|---------|-------------|
| C | app/src/main/java/.../domain/util/RepsRangeParser.kt | Parser de formato reps "30-45_SEC" |
| C | app/src/main/java/.../domain/util/LoadDisplayMapper.kt | Mapper shared de 7 variantes de carga |
| C | app/src/main/java/.../domain/util/RepsDisplayMapper.kt | Mapper shared de reps a display |
| C | app/src/main/java/.../domain/model/SessionPreviewExercise.kt | Modelo dominio preview (12 campos) |
| C | app/src/main/java/.../domain/usecase/session/GetSessionPreviewUseCase.kt | UseCase delegación preview |
| C | app/src/main/java/.../ui/preview/SessionPreviewUiState.kt | Estado UI preview + PreviewExerciseItem |
| C | app/src/main/java/.../ui/preview/SessionPreviewViewModel.kt | ViewModel deload-aware con SharedFlow |
| C | app/src/main/java/.../ui/preview/SessionPreviewScreen.kt | Pantalla preview completa con cards |
| C | app/src/main/java/.../ui/session/components/IsometricChronometer.kt | Cronómetro isométrico composable |
| C | app/src/test/java/.../domain/util/RepsRangeParserTest.kt | 6 test cases parser |
| C | app/src/test/java/.../domain/util/LoadDisplayMapperTest.kt | 8 test cases mapper carga |
| C | app/src/test/java/.../domain/util/RepsDisplayMapperTest.kt | 4 test cases mapper reps |
| C | app/src/test/java/.../domain/usecase/session/GetSessionPreviewUseCaseTest.kt | 2 test cases UseCase |
| M | app/src/main/java/.../ui/session/ActiveSessionViewModel.kt | Refactored → LoadDisplayMapper |
| M | app/src/main/java/.../ui/catalog/PlanVersionDetailViewModel.kt | Refactored → RepsDisplayMapper |
| M | app/src/main/java/.../data/local/dao/PlanAssignmentDao.kt | DTO + query preview exercises |
| M | app/src/main/java/.../data/local/dao/SessionExerciseDao.kt | Campo reps en SetExerciseInfo |
| M | app/src/main/java/.../domain/repository/SessionRepository.kt | Método getSessionPreviewExercises |
| M | app/src/main/java/.../data/repository/SessionRepositoryImpl.kt | Impl preview + prescribedReps |
| M | app/src/main/java/.../domain/model/RegisterSetInfo.kt | Campo prescribedReps añadido |
| M | app/src/main/java/.../ui/session/RegisterSetUiState.kt | TimerState enum + 5 campos timer |
| M | app/src/main/java/.../ui/session/RegisterSetViewModel.kt | Timer logic + RepsRangeParser |
| M | app/src/main/java/.../ui/session/RegisterSetScreen.kt | Condicional Chronometer vs RepsField |
| M | app/src/main/java/.../ui/navigation/NavigationRoutes.kt | Ruta SESSION_PREVIEW + helper |
| M | app/src/main/java/.../ui/navigation/TensionNavHost.kt | Composable preview + callbacks |
| M | app/src/main/java/.../ui/home/HomeScreen.kt | onNavigateToPreview + card clickable |
| M | app/src/main/res/values/strings.xml | Strings preview + cronómetro |
| M | app/src/test/java/.../domain/usecase/session/GetRegisterSetInfoUseCaseTest.kt | Fijado prescribedReps |
