# Análisis Arquitectónico

## Patrón Arquitectónico

**MVVM de solo lectura con derivación de señales en el dominio.**

HU-13 es una historia 100% de lectura — no escribe datos. Toda la información que necesita E5 ya fue persistida por las HUs predecesoras: HU-09 (cierre de sesión y tonelaje), HU-10 (clasificación de progresión en `session_exercise.progression_classification`), HU-11 (carga prescrita en `exercise_progression.prescribed_load_kg`), HU-12 (alertas de meseta y descarga en tabla `alert`). El flujo es lineal: queries agregadas en Data → enriquecimiento con lógica de señales en Domain → presentación visual en UI. La pieza arquitectónicamente más relevante es la regla pura `ActionSignalRule` que encapsula la derivación de señales de acción, incluyendo la ruta de "Considerar descarga" definida en HU-12 Decisión 13.

---

## Decisiones de Diseño

### Decisión 1 — HU-13 es una historia de lectura pura — no modifica ningún dato.

E5 se presenta después del cierre de sesión (`closeSession()` de HU-09). En ese punto, todos los datos ya fueron persistidos: `session.status` (HU-09), `session_exercise.progression_classification` (HU-10), `exercise_progression.prescribed_load_kg` (HU-11), alertas en tabla `alert` (HU-12). HU-13 solo los lee, enriquece con la regla `ActionSignalRule` y los presenta visualmente. No inserta, actualiza ni elimina registros.

### Decisión 2 — El tonelaje se calcula en la query, no se almacena.

El Modelo de Datos §3.10 establece explícitamente: "El tonelaje total de la sesión no se almacena — se calcula como `SUM(exercise_set.weight_kg * exercise_set.reps)` desde las series asociadas vía `session_exercise`." La query `getSessionSummaryInfo()` computa el tonelaje como agregación SQL, evitando una columna derivada en la tabla `session`.

### Decisión 3 — `ActionSignalRule` es una función pura en `domain/rules/` siguiendo ADR-06.

Encapsula siete variantes de señal (sealed interface `ActionSignal`) con lógica determinista, más un caso edge para ejercicios sin historial (`FirstSession`). La regla recibe `classification: ProgressionClassification?`, `prescribedLoadKg: Double?`, `avgWeightKg: Double`, `moduleRequiresDeload: Boolean`, `isBodyweight: Boolean`, `isIsometric: Boolean`, `totalReps: Int`, `previousTotalReps: Int?`, `setCount: Int`, `isMastered: Boolean`.

**Variantes para ejercicios estándar (con peso):**
- **`IncreaseLoad(targetKg)`** — DU cumplido: `prescribedLoadKg > avgWeightKg`. Texto: "Subir carga → X Kg".
- **`ProgressInReps`** — DU NO cumplido + clasificación `POSITIVE_PROGRESSION`. Texto: "Progresar en reps (misma carga)" (RF27). Distingue progresión de reps sin haber alcanzado DU.
- **`MaintainLoad`** — DU NO cumplido + clasificación `MAINTENANCE` o `REGRESSION` aislada (sin alerta de módulo). Texto: "Mantener carga — progresar en reps" (si MAINTENANCE) o "Mantener carga" (si REGRESSION aislada).
- **`ConsiderDeload`** — DU NO cumplido + clasificación `REGRESSION` **Y** alerta activa `MODULE_REQUIRES_DELOAD` para el módulo. Texto: "Considerar descarga". **Solo aplica a ejercicios estándar** — para bodyweight/isométricos con regresión, las señales tipo-específicas se mantienen y el alerta MODULE_REQUIRES_DELOAD se surfacea vía H1/H2.

**Variantes para ejercicios de peso corporal:**
- **`BodyweightSignal(totalReps, diff)`** — Texto: "N reps totales (+diferencia)" / "(=)" / "(−diferencia)" según clasificación.

**Variantes para ejercicios isométricos:**
- **`IsometricSignal(setCount, avgSeconds, mastered)`** — Texto: "{setCount}×{avgSeconds}s — Progresando/Manteniendo/Regresando". `avgSeconds = totalReps / setCount`.
- **`IsometricMastered`** — Texto: "4×45s — 🏆 Dominado". Cuando `isMastered == true` (CA-08.07: `exercise_progression.status == 'MASTERED'`).

La regla no depende de Android — es testeable con JUnit puro (RNF29, RNF30).

**Pseudocódigo de `ActionSignalRule.resolve()`:**

```
Si classification == null → FirstSession ("Primera sesión — sin referencia")

Si isIsometric:
  Si isMastered → IsometricMastered
  Else → IsometricSignal(setCount, avgSeconds = totalReps/setCount, mastered=false)

Si isBodyweight:
  → BodyweightSignal(totalReps, diff = totalReps - previousTotalReps)

Si estándar:
  Si prescribedLoadKg > avgWeightKg → IncreaseLoad(prescribedLoadKg)
  Si classification == REGRESSION && moduleRequiresDeload → ConsiderDeload
  Si classification == POSITIVE_PROGRESSION → ProgressInReps
  Si classification == MAINTENANCE → MaintainLoad (texto: "Mantener carga — progresar en reps")
  Si classification == REGRESSION (aislada) → MaintainLoad (texto: "Mantener carga")
```

### Decisión 4 — La derivación de "Considerar descarga" consume datos de HU-12 sin recalcular y solo aplica a ejercicios estándar.

HU-12 Decisión 13 establece la ruta: E5 muestra "Considerar descarga" cuando `session_exercise.progression_classification == 'REGRESSION'` **Y** `alertDao.existsActiveByModule(moduleCode, "MODULE_REQUIRES_DELOAD")` retorna `true`. HU-13 encapsula esta lógica en `ActionSignalRule.resolve()` que recibe un boolean `moduleRequiresDeload` ya resuelto por la query. **Esta señal solo aplica a ejercicios estándar (con peso).** Para ejercicios de peso corporal con regresión, E5 muestra "N reps totales (−diferencia)"; para isométricos con regresión, E5 muestra "N×Xs — Regresando" — en ambos casos, la señal tipo-específica se mantiene independientemente del estado MODULE_REQUIRES_DELOAD del módulo. El alerta de módulo se surfacea al ejecutante vía las pantallas de alertas (H1/H2, HU-17), no vía las señales individuales de bodyweight/isométricos en E5. Si solo hay regresión aislada sin alerta de módulo en un ejercicio estándar, E5 muestra "↓ Regresión" con señal "Mantener carga", no "Considerar descarga". Fuente de verdad: Especificación Visual §E5 tabla "Señales de acción por tipo".

### Decisión 5 — Solo se muestran ejercicios con al menos 1 serie registrada.

CA-13.07 especifica: "clasificación de progresión solo para los ejercicios que tienen registros". La query filtra con `HAVING completedSets > 0`. Los ejercicios sin series no aparecen en la lista — no hay datos para calcular progresión ni señales. El wireframe E5 confirma: "Los ejercicios sin ningún registro no aparecen en la lista".

### Decisión 6 — `ProgressionIndicator` es un composable reutilizable para E5, F2 y F3.

La combinación ícono (↑/=/↓) + color semántico + texto clasificación se repite en tres pantallas (E5, F2, F3). Se extrae como componente `ProgressionIndicator` en `ui/components/` que recibe la clasificación como parámetro y renderiza la tríada visual correcta. Los colores se toman de `LocalTensionSemanticColors.current` (ya definidos en `Color.kt` y expuestos en `Theme.kt`).

### Decisión 7 — La navegación E5 → F3 (historial de ejercicio) se condiciona a la existencia de la ruta.

F3 pertenece a HU-17 que aún no está implementada. La ruta `exercise-history/{exerciseId}` ya existe en `NavigationRoutes.kt`. El click en cada ejercicio intenta navegar a F3; si la pantalla aún no tiene composable registrado, se mantiene como placeholder. No se bloquea la funcionalidad de E5 por dependencia de F3.

### Decisión 8 — `SessionSummaryViewModel` recibe `sessionId` via `SavedStateHandle`.

El `sessionId` se pasa como argumento de navegación en la ruta `session-summary/{sessionId}`. El ViewModel lo extrae de `SavedStateHandle` (patrón estándar en el proyecto, usado en `ActiveSessionViewModel`). Esto evita pasar el ID manualmente y soporta process death/recreation.

### Decisión 9 — La sesión con 0 ejercicios registrados se maneja como edge case.

Si una sesión se cierra como "Incompleta" sin ninguna serie registrada (caso extremo pero posible), E5 muestra: Card de estado "Incompleta ⚠️", tonelaje 0 Kg, "0/N ejercicios", y lista vacía. No se muestra mensaje de error — es un resumen legítimo de una sesión vacía.

### Decisión 10 — E5 no tiene Bottom Navigation ni botón de retorno.

Wireframe E5 y Especificación Visual §E5 confirman: `CenterAlignedTopAppBar` sin `navigationIcon` (no hay retorno), sin Bottom Navigation. La única salida es el botón "Ir al Inicio" que navega a B1 con `popUpTo(HOME) { inclusive = true }`, limpiando el back stack del flujo de sesión.

---

## Componentes Afectados

### Componentes nuevos:

| # | Componente | Capa | Responsabilidad |
|---|---|---|---|
| 1 | `ActionSignal` | Domain (model) | Sealed interface con 8 variantes: `IncreaseLoad`, `ProgressInReps`, `MaintainLoad`, `ConsiderDeload`, `BodyweightSignal`, `IsometricSignal`, `IsometricMastered`, `FirstSession` |
| 2 | `ActionSignalRule` | Domain (rules) | Función pura `resolve()`: discrimina por tipo (bodyweight/isometric/standard) y clasificación + condición DU → retorna `ActionSignal` |
| 3 | `SessionSummary` | Domain (model) | Data class: metadata de sesión + lista de `ExerciseSummaryItem` |
| 4 | `ExerciseSummaryItem` | Domain (model) | Data class: nombre, clasificación, señal de acción, peso/reps, tipo, mastered |
| 5 | `GetSessionSummaryUseCase` | Domain (usecase/session) | Orquesta Repository + `ActionSignalRule` por ejercicio → `SessionSummary` |
| 6 | `SessionSummaryViewModel` | UI (session) | Carga datos vía UseCase, expone `StateFlow<SessionSummaryUiState>` |
| 7 | `SessionSummaryUiState` | UI (session) | Sealed interface: `Loading`, `Success(summary)`, `Error(message)` |
| 8 | `SessionSummaryScreen` | UI (session) | Composable E5 completo: Card estado/tonelaje + lista de progresión + botón "Ir al Inicio" |
| 9 | `ProgressionIndicator` | UI (components) | Composable reutilizable: ícono + color + texto de clasificación |

### Componentes modificados:

| # | Componente | Modificación | Nivel |
|---|---|---|---|
| 1 | `SessionRepository` | Agregar `getSessionSummaryData(sessionId): SessionSummaryData` | Menor |
| 2 | `SessionRepositoryImpl` | Implementar `getSessionSummaryData()`: combina queries de sesión, ejercicios, alerta MODULE_REQUIRES_DELOAD | Mayor |
| 3 | `SessionDao` | Agregar DTO `SessionSummaryInfo` + query `getSessionSummaryInfo(sessionId)` | Menor |
| 4 | `SessionExerciseDao` | Agregar DTO `ExerciseSummaryDto` + query `getExercisesForSummary(sessionId)` | Mayor |
| 5 | `TensionNavHost` | Registrar composable E5 en ruta `session-summary/{sessionId}`, reemplazar TODO con navegación real | Menor |

---

## Notas Técnicas

### Nota 1 — La query de `previousTotalReps` reutiliza el patrón de `getLastHistoricalSets()`.

`ExerciseSetDao.getLastHistoricalSets()` (HU-10) usa la misma subquery para encontrar la sesión anterior de un ejercicio: `SELECT se2.id FROM session_exercise se2 INNER JOIN session s2 ... WHERE se2.exercise_id = ? AND s2.id != ? AND s2.status IN ('COMPLETED', 'INCOMPLETE') ORDER BY s2.date DESC, s2.id DESC LIMIT 1`. La query del resumen reutiliza este patrón como subquery anidada para obtener `SUM(reps)` de la sesión anterior, evitando una llamada separada por ejercicio.

### Nota 2 — `SessionSummaryData` es un modelo intermedio entre Data y Domain.

El Repository retorna `SessionSummaryData` con DTOs crudos (strings para clasificación, nullable para prescribed_load). El `GetSessionSummaryUseCase` lo transforma a `SessionSummary` con domain models (enum para clasificación, sealed `ActionSignal`). Esta separación permite que el Repository permanezca agnóstico a la lógica de señales y que la regla sea testeable independientemente.

### Nota 3 — El ícono `↑`/`=`/`↓` no es un emoji — es un carácter Unicode literal.

La Especificación Visual §E5 define los íconos como caracteres de texto 24dp con color semántico, no como `Icon()` de Material. Se implementan como `Text("↑", fontSize = 24.sp, color = semanticColors.progressionPositive)` dentro de un `Box(modifier = Modifier.size(24.dp))` como `leadingContent` del `ListItem`.

### Nota 4 — El formato de tonelaje usa separador de miles (ej: "12,450 Kg").

La Especificación Visual §E5 muestra "12,450 Kg" con separador de miles. Se formatea con `NumberFormat.getIntegerInstance(Locale("es", "ES"))`. El tonelaje es `Double` (puede tener decimales si `weight_kg` tiene decimales), pero se muestra como entero redondeado para legibilidad.

### Nota 5 — Señales de acción para ejercicios sin historial previo (`classification == null`).

`session_exercise.progression_classification` es `NULL` cuando el ejercicio no tiene historial previo (primera sesión del ejercicio). CA-10.07 establece: "Sin Historial → no se emite clasificación". Para estos ejercicios, E5 muestra: sin ícono de clasificación, sin color semántico, señal de acción "Primera sesión — sin referencia". Esto cubre el edge case del primer uso del sistema.

### Nota 6 — Diferencia entre `avgWeightKg` y `prescribedLoadKg` para derivar "Subir carga".

La señal "Subir carga → X Kg" se muestra cuando `prescribedLoadKg > avgWeightKg` (la carga prescrita por el Doble Umbral es superior a la carga actual). Si `prescribedLoadKg == avgWeightKg` o `prescribedLoadKg` es `null`, la señal es "Mantener carga". La comparación se hace con tolerancia de 0.01 Kg (misma `WEIGHT_TOLERANCE` de `ProgressionClassificationRule`).

### Nota 7 — Señales para peso corporal e isométricos no incluyen carga prescrita.

Para ejercicios de peso corporal (`isBodyweight = true`), `exercise_progression.prescribed_load_kg` es `NULL` (nunca se prescribe carga — Regla 6 del MDS). La señal muestra "N reps totales (+diferencia)" donde `diferencia = totalReps - previousTotalReps`. Para isométricos (`isIsometric = true`), la señal muestra "{setCount}×{avgSeconds}s" (ej: "4×42s — Progresando") donde `avgSeconds = totalReps / setCount` (porque `exercise_set.reps` almacena segundos para isométricos — Modelo de Datos §3.12). Cuando `exercise_progression.status == 'MASTERED'` (CA-08.07 de HU-08, implementado en HU-10), E5 muestra el badge "🏆 Dominado" como un `AssistChip` M3 con `containerColor: Tertiary Container (#E0EEDD)` junto al nombre del ejercicio (Especificación Visual §E5). La señal "Considerar descarga" NO se aplica a bodyweight ni isométricos — estos tipos mantienen sus señales específicas incluso cuando MODULE_REQUIRES_DELOAD está activo para el módulo.

---

## Verificación Cruzada de CAs

| CA | Estado | Mecanismo | Implementado en |
|---|---|---|---|
| CA-13.01 | Done | `ActiveSessionViewModel.navigateToSessionSummary` → `TensionNavHost` → `SessionSummaryScreen` | HU-13 (Navegación + Screen) |
| CA-13.02 | Done | `SessionDao.getSessionSummaryInfo()` → `SUM(weight_kg * reps)` → `SessionSummary.totalTonnageKg` | HU-13 (DAO + Screen) |
| CA-13.03 | Done | `SessionDao.getSessionSummaryInfo()` → `completedExercises` / `totalExercises` | HU-13 (DAO + Screen) |
| CA-13.04 | Done | `SessionExerciseDao.getExercisesForSummary()` → `progression_classification` → `ProgressionIndicator` | HU-13 (DAO + Screen) |
| CA-13.05 | Done | `ActionSignalRule.resolve()` → sealed `ActionSignal` → texto de señal en `SessionSummaryScreen` | HU-13 (Rule + Screen) |
| CA-13.06 | Done | `ProgressionIndicator`: ↑ verde (#2E7D32/#81C784), = ámbar (#8D6E00/#FFD54F), ↓ rojo (#C62828/#EF9A9A) | HU-13 (Screen + Component) |
| CA-13.07 | Done | Query con `HAVING setCount > 0`, Card estado "Incompleta ⚠️", tonelaje parcial | HU-13 (DAO + Screen) |

---

## Análisis de Impacto y Performance

### Código real verificado:

- `SessionRepositoryImpl.kt`: Ya inyecta `AlertDao` (HU-12). Constructor L41-50 tiene todos los DAOs necesarios: `sessionDao`, `sessionExerciseDao`, `exerciseProgressionDao`, `alertDao`. NO requiere inyección adicional.
- `SessionDao.kt`: Tiene `getActiveSessionWithModuleVersion()` con DTO `ActiveSessionInfo`. Se necesita query similar para sesiones cerradas (por `sessionId`) con tonelaje agregado.
- `SessionExerciseDao.kt`: Tiene `getBySessionIdWithDetails()` que devuelve `SessionExerciseWithDetails` con `prescribedLoadKg`, `isBodyweight`, `isIsometric` — pero NO incluye `progression_classification` ni reps previas. Se necesita query nueva orientada al resumen.
- `AlertDao.kt`: Tiene `existsActiveByModule(moduleCode, type)` — exactamente lo que necesita la derivación de "Considerar descarga".
- `NavigationRoutes.kt`: `SESSION_SUMMARY = "session-summary/{sessionId}"` ya definida (L20). `sessionSummaryRoute(sessionId)` helper ya existe (L28).
- `TensionNavHost.kt`: L262-268 tiene el TODO con navegación temporal a HOME. Se reemplaza por la navegación real y el composable.
- `Color.kt`: Todos los colores semánticos necesarios ya definidos: `ProgressionPositiveLight/Dark`, `MaintenanceLight/Dark`, `RegressionLight/Dark`.
- `Theme.kt`: `TensionSemanticColors` expone `progressionPositive`, `maintenance`, `regression`, `sessionCompleted`, `sessionIncomplete` via `LocalTensionSemanticColors`.

### Cadena de invocación completa:

```
ActiveSessionViewModel.onCloseSessionConfirmed()
  → closeSessionUseCase(sessionId)                     [HU-09]
  → _navigateToSessionSummary.emit(id)                 [HU-09]
  → TensionNavHost: navController.navigate(sessionSummaryRoute(id))   [HU-13]
  → SessionSummaryScreen(sessionId)                                    [HU-13]
    → SessionSummaryViewModel.init { loadSummary(sessionId) }         [HU-13]
      → getSessionSummaryUseCase(sessionId)                           [HU-13]
        → sessionRepository.getSessionSummaryData(sessionId)          [HU-13]
          → sessionDao.getSessionSummaryInfo(sessionId)               [HU-13 query]
          → sessionExerciseDao.getExercisesForSummary(sessionId)      [HU-13 query]
          → alertDao.existsActiveByModule(moduleCode, "MODULE_...")    [HU-12]
        → ActionSignalRule.resolve(dto, moduleRequiresDeload)          [HU-13]
```

### Impacto en performance:
- `getSessionSummaryInfo()`: 1 SELECT con 2 subqueries de agregación sobre máximo 11 session_exercises y 44 exercise_sets. Despreciable.
- `getExercisesForSummary()`: JOIN triple con 2 subqueries anidadas por fila. Máximo 11 filas × 2 subqueries = 22 operaciones mínimas sobre tablas indexadas. Despreciable para SQLite.
- `existsActiveByModule()`: Ya indexado por `is_active` (HU-12). Operación O(1).
