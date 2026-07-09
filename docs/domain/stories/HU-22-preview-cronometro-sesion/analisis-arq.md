# Análisis Arquitectónico

<!-- ============================================================================ -->
<!-- SECCIÓN AGREGADA POR: Workflow analizar-disenar-historia-usuario            -->
<!-- ETAPA: Análisis Arquitectónico                                              -->
<!-- RESPONSABLE: Arquitecto                                                     -->
<!-- FECHA: 2026-03-05                                                           -->
<!-- ESTADO: Analizado (Arquitecto)                                              -->
<!-- AUDITORÍA: Completada (2026-03-05) — 15 defectos corregidos                -->
<!-- ============================================================================ -->

## Decisiones de Diseño

**Patrón Arquitectónico:** MVVM + Domain Layer (4 capas), extendiendo el stack existente con nuevos componentes verticales por cada mejora.

**Justificación:** La arquitectura actual (ADR-05) define la separación UI → ViewModel → UseCase → Repository → DAO → Room. Ambas mejoras (Preview y Cronómetro) se resuelven extendiendo este patrón sin introducir ninguna tecnología o patrón arquitectónico nuevo. El Preview agrega un componente vertical completo (Screen → ViewModel → UseCase → Repository → DAO query) con awareness de deload (HU-14). El Cronómetro modifica un componente vertical existente (RegisterSet) reemplazando una vista y enriqueciendo el modelo de datos. El enfoque exploratorio confirmó que toda la información necesaria ya existe en la base de datos y puede resolverse con queries de solo lectura (Preview) y extensiones menores de modelos existentes (Cronómetro).

**Componentes Afectados:**

### Bloque A — Preview de Sesión desde Home

- **`PlanAssignmentDao` — query `getPreviewByModuleVersionId()` (Modificación):** Nuevo query que extiende el existente `getDetailsByModuleVersionId()` (HU-04) añadiendo `LEFT JOIN exercise_progression` para obtener `prescribed_load_kg` y `LEFT JOIN module` para obtener `load_increment_kg` (necesario para calcular cargas de deload al 60%). Nuevo DTO: `SessionPreviewExerciseDto` con 12 campos.
  - Nivel de cambio: Menor
  - Especificaciones — Query SQL:

    ```sql
    SELECT 
        e.id AS exerciseId,
        e.name AS exerciseName,
        e.module_code AS moduleCode,
        et.name AS equipmentTypeName,
        GROUP_CONCAT(mz.name, ', ') AS muscleZones,
        pa.sets,
        pa.reps,
        e.is_bodyweight AS isBodyweight,
        e.is_isometric AS isIsometric,
        e.is_to_technical_failure AS isToTechnicalFailure,
        ep.prescribed_load_kg AS prescribedLoadKg,
        m.load_increment_kg AS loadIncrementKg
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

    Patrón idéntico al que `getBySessionIdWithDetails()` en `SessionExerciseDao` ya usa con `LEFT JOIN exercise_progression` e `INNER JOIN module`. Incluye `isToTechnicalFailure` y `loadIncrementKg` que la versión anterior omitía.

- **`SessionPreviewExercise` — Modelo de dominio (Nuevo):** Modelo read-only que transporta los datos del preview desde la capa de datos hasta la UI. Campos: `exerciseId`, `exerciseName`, `moduleCode`, `equipmentTypeName`, `muscleZones`, `sets`, `reps`, `isBodyweight`, `isIsometric`, `isToTechnicalFailure`, `prescribedLoadKg`, `loadIncrementKg`.
  - Ubicación: `domain/model/SessionPreviewExercise.kt`

- **`SessionRepository` / `SessionRepositoryImpl` (Modificación):** Nuevo método `getSessionPreviewExercises(moduleVersionId: Long): Flow<List<SessionPreviewExercise>>`. Mapea de DTO a modelo de dominio (Int → Boolean para flags). La convención existente ubica toda la lógica de sesión en `SessionRepository` (20 métodos actuales).
  - Nivel de cambio: Menor

- **`GetSessionPreviewUseCase` (Nuevo):** Caso de uso que invoca `sessionRepository.getSessionPreviewExercises(moduleVersionId)`. Mantiene la convención de 1 UseCase por operación de negocio (patrón establecido en `GetNextSessionInfoUseCase`, `GetActiveSessionUseCase`, etc.).
  - Ubicación: `domain/usecase/session/GetSessionPreviewUseCase.kt`

- **`SessionPreviewViewModel` (Nuevo):** Inyecta `GetSessionPreviewUseCase`, `StartSessionUseCase`, `SessionRepository` (para `getActiveDeload()`). Estado via `StateFlow<SessionPreviewUiState>` con `moduleCode`, `versionNumber`, `moduleVersionId`, `exercises`, `isLoading`, `isDeloadActive`, `deloadSessionsRemaining`. El ViewModel debe ser **deload-aware** (HU-14):
  - En `init`: consultar `deloadRepository.getActiveDeload()`. Si hay deload activo para el módulo del preview, marcar `isDeloadActive = true` y calcular `deloadSessionsRemaining`.
  - Para cada ejercicio: aplicar la misma lógica de 7 variantes de `loadDisplayText` que usa `ActiveSessionViewModel` (ver sección "Lógica de display de carga" más abajo).
  - Acción `startSession()` → llama `StartSessionUseCase(moduleVersionId)` → emite sessionId via `SharedFlow`.
  - `moduleVersionId`, `moduleCode`, `versionNumber` recibidos desde `SavedStateHandle`.
  - Ubicación: `ui/preview/SessionPreviewViewModel.kt`

- **Lógica de display de carga — 7 variantes (replicar de `ActiveSessionViewModel`):** El preview debe mostrar las cargas exactamente como las muestra E1. Las 7 variantes verificadas en código son:
  1. `isDeload && isIsometric` → "Isométrico (30s)"
  2. `isDeload && isBodyweight` → "Peso corporal (8 reps objetivo)"
  3. `isDeload && prescribedLoadKg != null` → "🔄 {deloadLoad} Kg" (calculado con `DeloadLoadRule.calculateDeloadLoad(prescribedLoadKg, loadIncrementKg)` al 60%)
  4. `isIsometric` → "Isométrico (30–45s)"
  5. `isBodyweight` → "Peso corporal"
  6. `prescribedLoadKg != null` → "{prescribedLoadKg} Kg"
  7. `else` → "Sin historial — establecer carga"

  Esta lógica debe extraerse como utilidad compartida (ej. `LoadDisplayMapper`) o replicarse en `SessionPreviewViewModel` con el mismo orden de precedencia.

- **Lógica de display de reps — mapeo de formatos especiales:** El campo `reps` crudo puede contener `'8-12'`, `'30-45_SEC'`, o `'TO_TECHNICAL_FAILURE'`. El preview debe aplicar el mismo mapeo que `PlanVersionDetailViewModel.mapRepsToDisplay()` (HU-04):
  - `"TO_TECHNICAL_FAILURE"` → "Al fallo técnico" (italic)
  - `"30-45_SEC"` → "30–45 seg" (italic)
  - Cualquier otro → "{reps} reps"

- **`SessionPreviewScreen` (Nuevo):** Pantalla Compose read-only con Bottom Navigation visible (es pre-sesión, no intra-sesión). Registrada en el `home-graph` del NavHost.
  - Header: `CenterAlignedTopAppBar` con `navigationIcon: ArrowBack` → B1, título: Title Large, On Surface, "Módulo {code} — V{n}".
  - Si deload activo: card informativo bajo el header con color `semanticColors.deloadActive` (#1565C0 / #64B5F6) y texto "Modo Descarga — {n}/6 sesiones" (CA-14.07).
  - `LazyColumn` con ejercicios en sort_order. Cada item: nombre, zona muscular, equipamiento, sets × reps (con mapeo de formato), carga objetivo (con las 7 variantes).
  - Ejercicios bodyweight del módulo A: badge "Fuera del gym" (CA-22.05, patrón HU-21).
  - Botón CTA "Iniciar Sesión" → crea sesión y navega a ActiveSession.
  - Pantalla separada (no bottom sheet) porque la lista de 8-12 ejercicios con cargas excede el espacio de un bottom sheet, y permite ruta propia (ADR-07).
  - Ubicación: `ui/preview/SessionPreviewScreen.kt`

- **`NavigationRoutes` / `TensionNavHost` (Modificación):** Nueva ruta `SESSION_PREVIEW = "session-preview/{moduleVersionId}/{moduleCode}/{versionNumber}"`. Nuevo `composable` en NavHost con `SessionPreviewScreen`. Bottom Navigation **visible** para esta ruta (NO agregar a la lista de exclusión de `showBottomBar`). HomeScreen: nueva callback `onNavigateToPreview(moduleVersionId, moduleCode, versionNumber)`. Modificación de `NextSessionCard`: el card se hace clickable → navega al preview. Botón "Iniciar Sesión" se mantiene como acción rápida alternativa (usuario elige: tap card → preview, o tap botón → inicio directo). Inventario de vistas del proyecto pasa de 26 a 27, con 3 nuevas conexiones (B1 → Preview, Preview → E1, Preview → B1 retorno).
  - Nivel de cambio: Menor

### Bloque B — Cronómetro Integrado para Ejercicios por Tiempo

- **`RepsRangeParser` — Utilidad de parsing (Nuevo):** Parsea el formato `'30-45_SEC'` almacenado en `plan_assignment.reps`. Retorna `RepsRange(min, max, isSeconds)`. Centraliza la lógica de parsing que se usa tanto en el cronómetro como potencialmente en el preview.
  - Ubicación: `domain/util/RepsRangeParser.kt`
  - Especificaciones:

    ```kotlin
    data class RepsRange(val min: Int, val max: Int, val isSeconds: Boolean)
    
    object RepsRangeParser {
        fun parse(reps: String): RepsRange {
            val isSeconds = reps.endsWith("_SEC")
            val cleaned = reps.removeSuffix("_SEC")
            val parts = cleaned.split("-")
            return RepsRange(
                min = parts[0].toIntOrNull() ?: 30,
                max = parts.getOrNull(1)?.toIntOrNull() ?: 60,
                isSeconds = isSeconds,
            )
        }
    }
    ```

- **`SetExerciseInfo` DTO en `SessionExerciseDao` (Modificación):** Agregar campo `reps: String` al DTO existente (actualmente 6 campos → 7). Modificar query `getExerciseInfoForSet`: añadir `COALESCE(pa.reps, '8-12') AS reps` al SELECT. La cláusula `LEFT JOIN plan_assignment` ya existe en la query.
  - Nivel de cambio: Menor

- **`RegisterSetInfo` — Modelo de dominio (Modificación):** Agregar campo `prescribedReps: String` al modelo existente (actualmente 9 campos → 10). Transporta el rango prescrito desde la capa de datos hasta el ViewModel. Modificar `getRegisterSetInfo()` en `SessionRepositoryImpl`: mapear `info.reps` → `prescribedReps`.
  - Nivel de cambio: Menor

- **`RegisterSetUiState` (Modificación):** Nuevos campos para el estado del timer: `timerState: TimerState` (enum: IDLE, RUNNING, STOPPED), `timerSeconds: Int`, `minSeconds: Int?`, `maxSeconds: Int?`, `showChronometer: Boolean` (derivado: `true` cuando el rango prescrito tiene `isSeconds = true`, independientemente del flag `isIsometric` — esto desacopla el cronómetro del flag de ejercicio para permitir futuros ejercicios con rango en segundos que no sean isométricos). Actualizar `isConfirmEnabled`: cuando `showChronometer`, se habilita solo si `timerState == STOPPED && selectedRir != null`.
  - Nivel de cambio: Mayor
  - Nota: El flag `isIsometric` sigue controlando el peso fijo a 0 Kg (CA-22.14), pero el cronómetro se activa por `range.isSeconds` (formato del reps). Fallback: si `isIsometric && !range.isSeconds` → `showChronometer = true` con rango por defecto 30-60s (CA-22.16).

- **`RegisterSetViewModel` (Modificación):** En el bloque `init` (no existe método `loadInfo()` — la carga ocurre en el `viewModelScope.launch` del `init`): parsear `prescribedReps` con `RepsRangeParser`. Si `range.isSeconds` → activar cronómetro con `minSeconds=range.min`, `maxSeconds=range.max`. Si `isIsometric && !range.isSeconds` → fallback `minSeconds=30`, `maxSeconds=60` (CA-22.16). Nuevos métodos `onStartTimer()`, `onStopTimer()` y `onResetTimer()` (STOPPED → IDLE para permitir reintento). Timer implementado con `viewModelScope.launch` + `SystemClock.elapsedRealtime()` en loop de ~100ms para conteo preciso. `elapsedRealtime()` no se pausa cuando la app va a background → resuelve CA-22.17. Auto-stop cuando `timerSeconds >= maxSeconds` (CA-22.09). Al detener: asigna `reps = elapsed.toString()`.
  - Nivel de cambio: Mayor
  - Nota sobre RNF02 (max 3 toques): El flujo del cronómetro con detención manual requiere 4 interacciones (Start + Stop + RIR + Confirm). Solo el auto-stop (CA-22.09) cumple con 3 toques (Start + RIR + Confirm). Esta desviación se justifica porque el cronómetro **elimina el cambio de contexto a app externa** (que representaba 6+ interacciones: abrir cronómetro externo + iniciar + detener + memorizar + regresar a Tension + transcribir). El beneficio neto es significativo. Para optimizar: considerar combinar Stop + selección de RIR como un paso (el selector de RIR se muestra inline al detener).

- **`IsometricChronometer` — Composable (Nuevo):** Componente visual con:
  - Display grande de segundos (tipografía `headlineLarge`)
  - `CircularProgressIndicator` determinado (`progress = seconds / maxSeconds`)
  - Colores semánticos (ADR-12): `MaterialTheme.colorScheme.error` (#BA1A1A) pre-mínimo → `semanticColors.progressionPositive` (#2E7D32 claro / #81C784 oscuro) en rango → auto-stop al máximo. **NO usar `Primary`** que es #8B1A1A (rojo imperio), visualmente indistinguible de `Error`.
  - Señalización accesible (RNF05 — color + ícono + texto): Texto de estado dinámico bajo el contador: "⚠️ Bajo el mínimo" (zona roja) → "✅ En rango" (zona verde), además del cambio de color. El icono y texto cambian sincronizados con la zona.
  - Texto de soporte estático: "Rango: {min}–{max} seg"
  - Botones: "Iniciar" cuando IDLE → "Detener" cuando RUNNING → "Reiniciar" cuando STOPPED (permite reintento sin salir del formulario)
  - Indicación visual si se detiene antes del mínimo (CA-22.11): texto "Tiempo por debajo del rango prescrito" en color `error`
  - Ubicación: `ui/session/components/IsometricChronometer.kt`

- **`RegisterSetScreen` (Modificación):** Cambio condicional: cuando `uiState.showChronometer == true`, reemplazar `RepsField` por `IsometricChronometer`. El `RirSelector` y `WeightField` permanecen inalterados (CA-22.14). RIR se mantiene como input numérico manual (CA-22.08).
  - Nivel de cambio: Mayor

## Hitos de Implementación

1. **`RepsRangeParser` + `LoadDisplayMapper` + tests unitarios** — Utilidades de parsing del formato `'30-45_SEC'` y mapeo de display de carga (7 variantes) y reps (3 variantes). Sin dependencias externas. Base para cronómetro y preview.
   - Dependencias: Ninguna

2. **Capa de datos del Preview** — `SessionPreviewExerciseDto` (12 campos), query `getPreviewByModuleVersionId()` en `PlanAssignmentDao`, método `getSessionPreviewExercises()` en `SessionRepository`/`SessionRepositoryImpl`.
   - Dependencias: HU-21 implementada (sort_order) ✅

3. **Capa de dominio del Preview** — `SessionPreviewExercise` (modelo), `GetSessionPreviewUseCase`.
   - Dependencias: Hito 2

4. **UI del Preview** — `SessionPreviewViewModel` (deload-aware), `SessionPreviewUiState`, `SessionPreviewScreen`, ruta `session-preview/{moduleVersionId}/{moduleCode}/{versionNumber}` en `TensionNavHost` (Bottom Nav visible), modificación de `NextSessionCard` en `HomeScreen`.
   - Dependencias: Hito 1 (LoadDisplayMapper), Hito 3, reutiliza `StartSessionUseCase` y `DeloadRepository` existentes

5. **Extensión del modelo de datos para cronómetro** — `SetExerciseInfo.reps` (DTO → 7 campos), `RegisterSetInfo.prescribedReps` (modelo → 10 campos), query `getExerciseInfoForSet` modificada, mapping en `SessionRepositoryImpl.getRegisterSetInfo()`.
   - Dependencias: Ninguna nueva (componentes existentes)

6. **Cronómetro — ViewModel + Composable** — `TimerState` enum, campos de timer en `RegisterSetUiState`, lógica de timer en `RegisterSetViewModel` (bloque `init`), componente `IsometricChronometer` (con accesibilidad RNF05), integración condicional en `RegisterSetScreen`.
   - Dependencias: Hito 1 (RepsRangeParser), Hito 5

## Validación de Impacto

**Código real verificado (Paso 1.5):**

| Componente | Estado Actual | Impacto |
|---|---|---|
| `NextSession` (modelo) | 3 campos (moduleCode, versionNumber, moduleVersionId) | Sin cambio — el preview usa su propia ruta con estos 3 valores como parámetros |
| `GetNextSessionInfoUseCase` | Resuelve rotación con deload awareness, retorna `Flow<NextSession?>` | Sin cambio — el preview recibe `moduleVersionId` de `HomeUiState.nextSession.moduleVersionId` |
| `HomeUiState` | `showNextSessionCard = activeSession == null && nextSession != null` | Sin cambio — CA-22.06 ya se cumple: cuando hay sesión activa, no se muestra el card |
| `PlanAssignmentDao.getDetailsByModuleVersionId()` | 11 campos, ORDER BY sort_order, sin prescribedLoadKg | No se modifica — se crea nuevo query `getPreviewByModuleVersionId()` |
| `ActiveSessionViewModel` | 7 variantes de `loadDisplayText` incluyendo 3 de deload | Referencia para la lógica de display del preview (mismas variantes) |
| `PlanVersionDetailViewModel.mapRepsToDisplay()` | Mapea `_SEC` → "seg", `TO_TECHNICAL_FAILURE` → "Al fallo técnico" | Referencia para el mapeo de display de reps del preview |
| `SessionRepository` | `getActiveDeload()`, `countDeloadSessions()` — métodos existentes en `SessionRepository` | Inyectado en `SessionPreviewViewModel` para deload awareness |
| `SessionExerciseDao.getExerciseInfoForSet()` | 6 campos, LEFT JOIN plan_assignment, no incluye `reps` | Modificación menor — agregar campo `reps` al SELECT y al DTO (→ 7 campos) |
| `RegisterSetInfo` (modelo) | 9 campos, no incluye prescribedReps | Modificación menor — agregar `prescribedReps: String` (→ 10 campos) |
| `RegisterSetViewModel` | Carga en bloque `init`, maneja isIsometric (weight=0, reps manual) | Modificación mayor — timer state, parseo de reps en `init`, start/stop/reset |
| `RegisterSetScreen` | `RepsField` como `OutlinedTextField` para isométricos | Modificación mayor — condicional entre `RepsField` e `IsometricChronometer` |
| `RegisterSetUiState` | 13 campos declarados + 1 computed (`isConfirmEnabled`) | Modificación mayor — 5 nuevos campos, `isConfirmEnabled` actualizado |
| `startSession()` en `SessionRepositoryImpl` | INSERT session + session_exercise en transacción | Sin cambio — preview es puramente read-only |
| `TensionNavHost` | 25 composable routes, bottom bar hidden for session routes | Modificación menor — +1 ruta (Bottom Nav visible en preview) |

**Verificaciones críticas:**

- `exercise_progression.prescribed_load_kg` ya se usa con LEFT JOIN en queries de `SessionExerciseDao` → mismo patrón aplicable al preview.
- `exercise_progression.prescribed_load_kg` es **siempre NULL** para bodyweight/isométricos (no usan peso) → el preview debe usar los flags `isBodyweight`/`isIsometric` para display, no depender solo de que `prescribedLoadKg != null`.
- `plan_assignment.reps` almacena `'30-45_SEC'` para isométricos (verificado en SQL de migración HU-16, `PlanSeeder`).
- `exercise_set.reps` almacena el valor entero de segundos para isométricos (patrón HU-06/HU-08) → compatible con captura automática del cronómetro (CA-22.19).
- `SystemClock.elapsedRealtime()` no se pausa cuando la app va a background → resuelve CA-22.17.
- `DeloadLoadRule.calculateDeloadLoad(lastWeightKg, loadIncrementKg)` requiere ambos parámetros → el query del preview debe incluir `module.load_increment_kg`.
- Ejercicios sustituidos (HU-07) no tienen `plan_assignment` → para estos, `COALESCE(pa.reps, '8-12')` en el cronómetro retorna `'8-12'` → `RepsRangeParser` retorna `isSeconds=false` → si el sustituto es isométrico, se activa fallback 30-60s (CA-22.16). Cadena verificada.

**Cadena de invocación — Preview:**

```text
HomeScreen (tap card) → navigate("session-preview/$moduleVersionId/$moduleCode/$versionNumber")
  → SessionPreviewViewModel.init(moduleVersionId)
    → GetSessionPreviewUseCase(moduleVersionId) [READ-ONLY query]
    → DeloadRepository.getActiveDeload() [consulta estado deload]
      → Si deload activo: isDeloadActive=true, calcular deloadSessionsRemaining
      → Para cada ejercicio: aplicar LoadDisplayMapper con 7 variantes de carga
      → Para cada ejercicio: aplicar mapRepsToDisplay para formato legible
  → SessionPreviewScreen renders exercises (Bottom Nav visible)
    → User taps "Iniciar Sesión"
      → SessionPreviewViewModel.startSession()
        → StartSessionUseCase(moduleVersionId) [EXISTENTE — crea registros DB]
          → navigate("active-session/$sessionId")
```

**Cadena de invocación — Cronómetro:**

```text
RegisterSetScreen (ejercicio con reps en segundos)
  → RegisterSetViewModel.init { getRegisterSetInfoUseCase(sessionExerciseId) }
    → RegisterSetInfo (incluye prescribedReps="30-45_SEC")
      → RepsRangeParser.parse("30-45_SEC") → RepsRange(30, 45, true)
        → UiState: showChronometer=true, minSeconds=30, maxSeconds=45
  → IsometricChronometer renders
    → User: "Iniciar" → onStartTimer() → coroutine con elapsedRealtime
      → Color: error (#BA1A1A, 0-29s) → progressionPositive (#2E7D32, 30-45s)
      → Texto: "⚠️ Bajo el mínimo" → "✅ En rango"
        → At 45s: auto-stop → reps = "45" (3 toques: Start + RIR + Confirm)
        → OR user stops at 33s → reps = "33" (4 toques: Start + Stop + RIR + Confirm)
    → User: "Confirmar" → onConfirm(reps=33, rir=selectedRir, weight=0.0)
      → registerSetUseCase → exercise_set.reps = 33 [COMPATIBLE]
```

**Riesgos identificados:**

| Riesgo | Impacto | Mitigación |
|--------|---------|------------|
| Inconsistencia de cargas entre preview y sesión real (CA-22.07) | Si otra sesión se cierra entre preview y start, `prescribed_load_kg` puede cambiar | Ambos leen del mismo `exercise_progression` via LEFT JOIN. Se actualiza solo en `evaluateProgression()` al cerrar sesión (HU-09). CA-22.07 contempla el escenario explícitamente |
| Timer imprecision en background prolongado | Si la app permanece >5 min en background, el OS puede suspender la corrutina | `SystemClock.elapsedRealtime()` calcula delta real al despertar. No depende del tick de la corrutina para el valor final |
| Ejercicio isométrico custom sin rango `_SEC` (CA-22.16) | Ejercicio custom marcado isométrico pero con reps `"8-12"` | Fallback: `isIsometric && !range.isSeconds` → `minSeconds=30, maxSeconds=60` con indicación de rango por defecto |
| Ejercicio sustituido isométrico (HU-07) sin `plan_assignment` | LEFT JOIN retorna NULL → `COALESCE(pa.reps, '8-12')` → `isSeconds=false` | Si sustituto `isIsometric`, se activa fallback 30-60s — cadena verificada en código |
| Post-deload: cargas de reinicio al 90% (CA-14.05) | Tras completar 6 sesiones de deload, `prescribed_load_kg` se actualiza con `calculateResetLoad()` (90%) | El preview lee de `exercise_progression.prescribed_load_kg` que ya refleja el valor actualizado. Verificar en tests que la transición deload → normal muestra cargas correctas en preview |
| RNF02 (max 3 toques) con detención manual | Flujo con stop manual = 4 toques. Solo auto-stop = 3 toques | Desviación justificada: cronómetro elimina cambio de contexto a app externa (6+ interacciones). Beneficio neto significativo. Optimización futura: combinar Stop + RIR en un paso |

## Notas Técnicas

- **No se requiere nueva migración de base de datos.** Todo el Preview opera con un query de solo lectura sobre tablas existentes (`plan_assignment`, `exercise`, `exercise_progression`, `module`). El cronómetro almacena datos en el mismo campo `exercise_set.reps` con el mismo formato entero.
- **`moduleVersionId`, `moduleCode` y `versionNumber` como parámetros de navegación** para el Preview. Pasar los 3 valores evita un query extra para resolver el header de la pantalla.
- **Extraer `LoadDisplayMapper` como utilidad compartida** entre `ActiveSessionViewModel` y `SessionPreviewViewModel`. Las 7 variantes de display de carga deben ser idénticas en ambos contextos para garantizar CA-22.07.
- **Extraer `mapRepsToDisplay()` como utilidad compartida** entre `PlanVersionDetailViewModel` y `SessionPreviewScreen`. Actualmente reside como companion object en `PlanVersionDetailViewModel` — moverlo a `domain/util/` o `ui/util/`.
- **Fallback del cronómetro** (CA-22.16): Se activa cuando `isIsometric == true` pero `reps` no contiene `_SEC`. El rango por defecto 30-60 es conservador y cubre los rangos actuales (30-45) con margen. El texto de soporte muestra "Rango por defecto: 30–60 seg" para transparencia.
- **Impacto documental:** El Mapa de Navegación y la Especificación Visual mapean erróneamente HU-22 a G1 (Panel de Métricas) y J1 (Configuración). Las pantallas realmente afectadas son B1 (NextSessionCard clickable), nueva pantalla Preview, y E2 (RegisterSetScreen con cronómetro). Esta corrección debe aplicarse a ambos documentos durante o después del refinamiento técnico.

## Referencias y Validación

**Documentación consultada:**

- Visión del Producto — §4.5 (sesión pertenece al usuario), §4.8 (fricción → cero)
- Requerimientos — RF12 (preview sesión), RF32 (isométricos 30-45s), RF38 (parámetros de descarga), RNF02 (max 3 toques), RNF05 (señales por color + ícono + texto), RNF10 (preservación datos confirmados)
- Arquitectura Técnica — MVVM + Domain Layer (4 capas), Material 3, §4.3 (inventario rutas), §4.5 (reglas Bottom Nav)
- ADR-03 (Room/SQLite), ADR-05 (MVVM + Domain), ADR-07 (Navigation Compose), ADR-09 (StateFlow + SharedFlow), ADR-12 (esquema cromático seed #8B1A1A, colores semánticos de progresión)
- Modelo de Datos — `plan_assignment`, `exercise_progression`, `exercise_set`, `session_exercise`, `deload`, `module`
- Mapa de Navegación — B1 (Home), E1-E5 (session flow), §3 (26 vistas → 27 con preview)
- Especificación Visual — §4.3 (colores semánticos: progressionPositive #2E7D32, error #BA1A1A, deloadActive #1565C0), §7.1 (CenterAlignedTopAppBar), §10 (accesibilidad RNF05)
- Plan de Entrenamiento — Composiciones por módulo (A: 11-12 ej, B: 8 ej, C: 8 ej), rangos isométricos `'30-45_SEC'`
- Diccionario de Ejercicios — Plancha, Plancha Lateral (isométricos), Flexiones (TO_TECHNICAL_FAILURE)

**Historias relacionadas:**

- Historia #4: HU-04 — `getDetailsByModuleVersionId()` en `PlanAssignmentDao`, `mapRepsToDisplay()` con mapeo de formatos especiales
- Historia #5: HU-05 — Creación de sesión (`startSession()`), crash recovery, carga objetivo (`prescribedLoadKg`)
- Historia #6: HU-06 — Registro de serie (RegisterSetScreen/E2), campo `reps` almacena segundos para isométricos
- Historia #7: HU-07 — Sustitución de ejercicios — sustitutos no tienen `plan_assignment`, afecta fallback del cronómetro (COALESCE → '8-12')
- Historia #8: HU-08 — Cross-cutting isométrico (peso=0, "Segundos sostenidos", referencia "30-45 seg")
- Historia #9: HU-09 — Cierre de sesión → `evaluateProgression()` actualiza `prescribed_load_kg`, relevante para consistencia CA-22.07
- Historia #10: HU-10 — Motor de progresión (opera sobre `exercise_set.reps` — compatible con cronómetro)
- Historia #11: HU-11 — Regla Doble Umbral produce `prescribed_load_kg`; isométricos excluidos (NULL permanente)
- Historia #14: HU-14 — Protocolo de descarga: versiones congeladas, cargas al 60% (`DeloadLoadRule`), indicador visual, 6 sesiones, reinicio al 90%. **Crítico para el preview.**
- Historia #16: HU-16 — Migración con formato `'30-45_SEC'` en PlanSeeder
- Historia #21: HU-21 — `sort_order` en `plan_assignment`, ORDER BY COALESCE, "fuera del gym" — ya implementada

**Validado por:** Esteban | **Fecha:** 2026-03-05 | **Enfoque:** Exploratorio | **Auditoría:** Completada (2026-03-05)
