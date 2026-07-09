## Análisis Arquitectónico

> Esta historia completa el ciclo de entrenamiento iniciado en HU-05. Construye la transacción de cierre que consolida datos y avanza la rotación cíclica.

**Patrón arquitectónico:** MVVM con capa Domain explícita (4 capas: UI → ViewModel → Domain → Data), según ADR-05. Transacciones atómicas Room para consistencia.

### Componentes afectados

#### 1. Domain — Lógica de rotación pura (Nuevo)

Función pura para avance de rotación cíclica, testeable unitariamente sin emulador.

- **`RotationResolver.kt`** (`domain/model/RotationResolver.kt`): Agregar `advanceRotation(current: RotationState): RotationState`. Lógica: si `microcyclePosition < 6` → `copy(microcyclePosition = pos + 1)`; si `== 6` → `copy(pos=1, vA=(vA%3)+1, vB=(vB%3)+1, vC=(vC%3)+1, count+1)`. Fórmula wrap-around: `(v % 3) + 1` (1→2, 2→3, 3→1).
- **`RotationResolverTest.kt`** (`domain/model/RotationResolverTest.kt`): 6 tests: posiciones 1→2, 5→6, 6→1 con V1→V2, 6→1 con V3→V1, 6→1 con versiones mixtas, 6→1 con incremento de count.

#### 2. Data Layer — DAO (Modificación)

- **`SessionDao.kt`** (`data/local/dao/SessionDao.kt`): Agregar `@Query("UPDATE session SET status = :status WHERE id = :sessionId") suspend fun updateStatus(sessionId: Long, status: String)`.

#### 3. Data Layer — Repository (Modificación)

- **`SessionRepository.kt`** (`domain/repository/SessionRepository.kt`): Agregar `suspend fun closeSession(sessionId: Long)`.
- **`SessionRepositoryImpl.kt`** (`data/repository/SessionRepositoryImpl.kt`): Implementar `closeSession()` con `database.withTransaction { ... }`. Pasos: (1) `sessionDao.getActiveSessionWithModuleVersion().first()`, (2) validar sessionId, (3) determinar status COMPLETED/INCOMPLETE, (4) `sessionDao.updateStatus()`, (5) `rotationStateDao.getRotationState().first()`, (6) mapear a `RotationState`, (7) `RotationResolver.advanceRotation()`, (8) `rotationStateDao.update()`.

#### 4. Domain — Use Case (Nuevo)

- **`CloseSessionUseCase.kt`** (`domain/usecase/session/CloseSessionUseCase.kt`):
```kotlin
class CloseSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(sessionId: Long) {
        sessionRepository.closeSession(sessionId)
    }
}
```
- **`CloseSessionUseCaseTest.kt`** (`domain/usecase/session/CloseSessionUseCaseTest.kt`): Test de delegación al repositorio + test de propagación de excepción.

#### 5. UI — UiState (Modificación)

- **`ActiveSessionUiState.kt`** (`ui/session/ActiveSessionUiState.kt`): Agregar campos `showCloseDialog: Boolean = false`, `isClosing: Boolean = false`. Agregar propiedades derivadas: `val incompleteCount: Int get() = totalCount - completedCount`, `val isAllCompleted: Boolean get() = completedCount == totalCount && totalCount > 0`.

#### 6. UI — ViewModel (Modificación)

- **`ActiveSessionViewModel.kt`** (`ui/session/ActiveSessionViewModel.kt`): Agregar `CloseSessionUseCase` al constructor. Agregar `_navigateToSessionSummary: MutableSharedFlow<Long>(replay = 0)` + `navigateToSessionSummary: SharedFlow<Long>`. Implementar `onCloseSessionRequested()`, `onCloseDialogDismissed()`, `onCloseSessionConfirmed()`. Ajustar `combine` collector para preservar `showCloseDialog` e `isClosing`.

#### 7. UI — Screen + Dialog E4 (Modificación)

- **`ActiveSessionScreen.kt`** (`ui/session/ActiveSessionScreen.kt`):
  1. `BackHandler { viewModel.onCloseSessionRequested() }` (reemplaza no-op).
  2. `onClick = { viewModel.onCloseSessionRequested() }` en botón "Cerrar Sesión".
  3. `LaunchedEffect` que observa `navigateToSessionSummary` → `onNavigateToSessionSummary(sessionId)`.
  4. AlertDialog condicional `if (uiState.showCloseDialog) { CloseSessionDialog(...) }`.
- **`CloseSessionDialog`** composable privado con 2 variantes:
  - **Caso A (completa):** título "Cerrar sesión", texto "Todos los ejercicios están completados. La sesión se cerrará como Completada.", botón "Cerrar ✓" (Primary).
  - **Caso B (parcial):** título "Cerrar sesión", ícono `Icons.Default.Warning` (Error), texto "Hay N ejercicios sin completar. La sesión se cerrará como Incompleta. Los datos parciales se conservarán.", botón "Cerrar ⚠️" (Error).

#### 8. Navigation — Ruta E5 stub (Modificación)

- **`NavigationRoutes.kt`** (`ui/navigation/NavigationRoutes.kt`): Agregar `const val SESSION_SUMMARY = "session-summary/{sessionId}"` + `fun sessionSummaryRoute(sessionId: Long) = "session-summary/$sessionId"`.
- **`TensionNavHost.kt`** (`ui/navigation/TensionNavHost.kt`): Wiring temporal a Home con `popUpTo(HOME) { inclusive = true }`.

#### 9. Strings (Modificación)

- **`strings.xml`** (`res/values/strings.xml`): Agregar 6 strings de E4:
  - `session_close_title`: "Cerrar sesión"
  - `session_close_complete_message`: "Todos los ejercicios están completados. La sesión se cerrará como Completada."
  - `session_close_incomplete_message`: "Hay %1$d ejercicios sin completar. La sesión se cerrará como Incompleta. Los datos parciales se conservarán."
  - `session_close_confirm_complete`: "Cerrar ✓"
  - `session_close_confirm_incomplete`: "Cerrar ⚠️"
  - `session_close_cancel`: "Cancelar"

### Verificación Exhaustiva CA por CA

**CA-09.01 — Cierre como Completada:**

| Capa | Implementación | Evidencia |
|------|----------------|-----------|
| Repository | `if (completedExercises == totalExercises) "COMPLETED"` | Transacción atómica en `closeSession()` |
| DAO | `SessionDao.updateStatus(sessionId, "COMPLETED")` | UPDATE query |
| UI (E4) | Caso A: texto confirmatorio + botón "Cerrar ✓" (Primary) | AlertDialog variante A |

**CA-09.02 — Cierre como Incompleta:**

| Capa | Implementación | Evidencia |
|------|----------------|-----------|
| Repository | `else "INCOMPLETE"` | Cualquier estado parcial → INCOMPLETE |
| Data | Series existentes no se eliminan | `updateStatus` solo cambia status, no toca `exercise_set` |

**CA-09.03 — Confirmación antes de cierre incompleto:**

| Capa | Implementación | Evidencia |
|------|----------------|-----------|
| UI (E4) | Caso B: ícono ⚠️ Error + texto con N ejercicios + botón Error | AlertDialog variante B |
| UiState | `incompleteCount: Int` = `totalCount - completedCount` | Propiedad derivada |

> El diálogo se muestra en AMBOS casos (completo e incompleto). La diferencia es visual: Caso A es confirmatorio (Primary), Caso B es de advertencia (Error).

**CA-09.04 — Cálculo automático de tonelaje:**

| Capa | Implementación | Evidencia |
|------|----------------|-----------|
| Datos | Tonelaje = `SUM(weight_kg * reps)` — derivado, NO almacenado | Modelo de Datos §2 y §3.10 |
| HU-09 | No calcula tonelaje directamente | El tonelaje se calcula cuando E5 (HU-13) lo consume |
| Query futura | `SELECT COALESCE(SUM(es.weight_kg * es.reps), 0.0) FROM exercise_set es INNER JOIN session_exercise se ON es.session_exercise_id = se.id WHERE se.session_id = :sessionId` | Será implementada en HU-13 |

> CA-09.04 dice "almacena". El Modelo de Datos contradice el "almacena" explícitamente. La resolución es que "almacena" significa "el dato queda disponible" — las series están persistidas, el tonelaje se puede derivar en cualquier momento. No se agrega columna `tonnage` a `session`.

**CA-09.05 — Avance de rotación al cerrar:**

| Capa | Implementación | Evidencia |
|------|----------------|-----------|
| Domain | `RotationResolver.advanceRotation()` — función pura | `(v % 3) + 1` para versiones |
| Repository | `rotationStateDao.update(newState)` dentro de transacción | Atómico con update de status |
| Lógica pos. < 6 | Solo incrementa posición | `copy(microcyclePosition = pos + 1)` |
| Lógica pos. == 6 | Reset a 1 + advance versions + increment count | `copy(pos=1, vA=(vA%3)+1, vB=(vB%3)+1, vC=(vC%3)+1, count+1)` |

**CA-09.06 — Avance solo al cerrar:** Garantía estructural — `advanceRotation()` solo se invoca desde `closeSession()`.

**CA-09.07 — Inmutabilidad:** Garantía estructural — ADR D-03. No existe endpoint de edición/eliminación post-cierre.

**CA-09.08 — Preservación ante cierre inesperado:** Ya implementado — `database.withTransaction` garantiza rollback + B1 crash recovery card (HU-05).

### Componentes Nuevos

**1. `CloseSessionUseCase`** — `domain/usecase/session/CloseSessionUseCase.kt`

```kotlin
class CloseSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(sessionId: Long) {
        sessionRepository.closeSession(sessionId)
    }
}
```

**2. `RotationResolver.advanceRotation()`** — extensión del object existente

```kotlin
fun advanceRotation(current: RotationState): RotationState {
    return if (current.microcyclePosition < 6) {
        current.copy(microcyclePosition = current.microcyclePosition + 1)
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

Función pura, testeable unitariamente. Opera sobre el modelo de dominio `RotationState`, no sobre la entity.

### Componentes Modificados

**3. `SessionRepository`** — agregar método:
```kotlin
suspend fun closeSession(sessionId: Long)
```

**4. `SessionRepositoryImpl.closeSession()`** — implementación transaccional:

```kotlin
override suspend fun closeSession(sessionId: Long) {
    database.withTransaction {
        val sessionInfo = sessionDao.getActiveSessionWithModuleVersion().first()
            ?: throw IllegalStateException("No active session found")
        if (sessionInfo.sessionId != sessionId)
            throw IllegalStateException("Session $sessionId is not the active session")

        val status = if (sessionInfo.completedExercises == sessionInfo.totalExercises)
            "COMPLETED" else "INCOMPLETE"
        sessionDao.updateStatus(sessionId, status)

        val rotationEntity = rotationStateDao.getRotationState().first()
            ?: throw IllegalStateException("Rotation state not found")
        val currentRotation = RotationState(
            microcyclePosition = rotationEntity.microcyclePosition,
            currentVersionModuleA = rotationEntity.currentVersionModuleA,
            currentVersionModuleB = rotationEntity.currentVersionModuleB,
            currentVersionModuleC = rotationEntity.currentVersionModuleC,
            microcycleCount = rotationEntity.microcycleCount,
        )
        val newRotation = RotationResolver.advanceRotation(currentRotation)
        rotationStateDao.update(rotationEntity.copy(
            microcyclePosition = newRotation.microcyclePosition,
            currentVersionModuleA = newRotation.currentVersionModuleA,
            currentVersionModuleB = newRotation.currentVersionModuleB,
            currentVersionModuleC = newRotation.currentVersionModuleC,
            microcycleCount = newRotation.microcycleCount,
        ))
    }
}
```

**5. `SessionDao`** — agregar query:
```kotlin
@Query("UPDATE session SET status = :status WHERE id = :sessionId")
suspend fun updateStatus(sessionId: Long, status: String)
```

**6. `ActiveSessionViewModel`** — agregar lógica de diálogo y cierre:

Campos nuevos en UiState: `showCloseDialog: Boolean = false`, `isClosing: Boolean = false`.

Nuevo SharedFlow: `_navigateToSessionSummary: MutableSharedFlow<Long>` + `navigateToSessionSummary: SharedFlow<Long>`.

```kotlin
fun onCloseSessionRequested() {
    _uiState.update { it.copy(showCloseDialog = true) }
}

fun onCloseDialogDismissed() {
    _uiState.update { it.copy(showCloseDialog = false) }
}

fun onCloseSessionConfirmed() {
    viewModelScope.launch {
        _uiState.update { it.copy(isClosing = true, showCloseDialog = false) }
        try {
            closeSessionUseCase(sessionId)
            _navigateToSessionSummary.emit(sessionId)
        } catch (_: Exception) {
            _uiState.update { it.copy(isClosing = false) }
        }
    }
}
```

**7. `ActiveSessionUiState`** — agregar campos:

```kotlin
data class ActiveSessionUiState(
    val isLoading: Boolean = true,
    val moduleCode: String = "",
    val versionNumber: Int = 0,
    val exercises: List<ExerciseUiItem> = emptyList(),
    val showCloseDialog: Boolean = false,
    val isClosing: Boolean = false,
) {
    val completedCount: Int get() = exercises.count { it.status == ExerciseSessionStatus.COMPLETED }
    val totalCount: Int get() = exercises.size
    val progress: Float get() = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    val incompleteCount: Int get() = totalCount - completedCount
    val isAllCompleted: Boolean get() = completedCount == totalCount && totalCount > 0
}
```

**8. `ActiveSessionScreen`** — wiring del botón, BackHandler, y AlertDialog:

1. `BackHandler { viewModel.onCloseSessionRequested() }` (reemplaza no-op).
2. `onClick = { viewModel.onCloseSessionRequested() }` en botón "Cerrar Sesión" (reemplaza TODO).
3. `LaunchedEffect` que observa `navigateToSessionSummary` y llama `onNavigateToSessionSummary(sessionId)`.
4. `AlertDialog` condicional `if (uiState.showCloseDialog) { CloseSessionDialog(...) }`.

AlertDialog tiene 2 variantes (Especificación Visual §8 E4):
- **Caso A (completa):** título "Cerrar sesión", texto "Todos los ejercicios están completados. La sesión se cerrará como Completada.", botón "Cerrar ✓" (Primary).
- **Caso B (parcial):** título "Cerrar sesión", ícono `Icons.Default.Warning` (Error), texto "Hay N ejercicios sin completar. La sesión se cerrará como Incompleta. Los datos parciales se conservarán.", botón "Cerrar ⚠️" (Error).

```kotlin
@Composable
private fun CloseSessionDialog(
    isAllCompleted: Boolean,
    incompleteCount: Int,
    isClosing: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = if (!isAllCompleted) {
            { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
        } else null,
        title = { Text(stringResource(R.string.session_close_title)) },
        text = {
            Text(
                if (isAllCompleted) stringResource(R.string.session_close_complete_message)
                else stringResource(R.string.session_close_incomplete_message, incompleteCount)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isClosing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAllCompleted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                ),
            ) {
                if (isClosing) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(if (isAllCompleted) stringResource(R.string.session_close_confirm_complete)
                    else stringResource(R.string.session_close_confirm_incomplete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isClosing) {
                Text(stringResource(R.string.session_close_cancel))
            }
        },
    )
}
```

**9. `NavigationRoutes`** — agregar ruta E5 (preparación para HU-13):
```kotlin
const val SESSION_SUMMARY = "session-summary/{sessionId}"
fun sessionSummaryRoute(sessionId: Long) = "session-summary/$sessionId"
```

**10. `TensionNavHost`** — wiring temporal a Home:
```kotlin
onNavigateToSessionSummary = { _ ->
    // TODO: HU-13 — navigate to session-summary/$sessionId
    navController.navigate(NavigationRoutes.HOME) {
        popUpTo(NavigationRoutes.HOME) { inclusive = true }
    }
},
```

**11. `strings.xml`** — agregar strings de E4:
```xml
<!-- Close Session Dialog E4 -->
<string name="session_close_title">Cerrar sesión</string>
<string name="session_close_complete_message">Todos los ejercicios están completados. La sesión se cerrará como Completada.</string>
<string name="session_close_incomplete_message">Hay %1$d ejercicios sin completar. La sesión se cerrará como Incompleta. Los datos parciales se conservarán.</string>
<string name="session_close_confirm_complete">Cerrar ✓</string>
<string name="session_close_confirm_incomplete">Cerrar ⚠️</string>
<string name="session_close_cancel">Cancelar</string>
```

### Decisiones de Diseño

**1. E4 es un AlertDialog, no una ruta de navegación.**
ADR D-04 y Arquitectura Técnica §4.3 fila 12 establecen que E4 es un diálogo modal superpuesto sobre E1, gestionado por estado del ViewModel (`showCloseDialog: Boolean`). No entra al back stack.

**2. El tonelaje NO se almacena en base de datos.**
CA-09.04 dice "almacena el resultado como dato derivado". El Modelo de Datos §2 establece: *"Datos calculados: No se almacenan en base de datos"*. §3.10 (`session`) confirma: *"El tonelaje total de la sesión no se almacena — se calcula como `SUM(exercise_set.weight_kg * exercise_set.reps)`"*. El tonelaje se calcula on-demand cuando E5 (HU-13), F1 (HU-24) o G1 (HU-20) lo necesiten. **En HU-09 no se calcula ni se almacena** — solo se cierra la sesión y se avanza la rotación.

**3. La operación de cierre es una transacción atómica con 2 operaciones.**
`database.withTransaction` que: (a) actualiza `session.status` a COMPLETED o INCOMPLETE, y (b) avanza `rotation_state`. Si cualquier operación falla, todo se revierte (CA-09.08).

**4. Determinación del status: COMPLETED vs INCOMPLETE.**
Una sesión es COMPLETED si y solo si **todos** sus ejercicios tienen exactamente 4 series registradas (`completedExercises == totalExercises`). En cualquier otro caso es INCOMPLETE. Se reutiliza `getActiveSessionWithModuleVersion().first()` dentro de la transacción para obtener los conteos sincrónicamente.

**5. Avance de rotación: posición + versiones.**
Según Modelo de Datos §3.14:
- Si `microcyclePosition < 6`: `microcyclePosition += 1`. Solo se avanza la posición.
- Si `microcyclePosition == 6`: `microcyclePosition = 1`, `microcycleCount += 1`, y todas las versiones avanzan: `(currentVersion % 3) + 1` (wrap-around V1→V2→V3→V1).

Fórmula de wrap-around: `(v % 3) + 1` — produce: 1→2, 2→3, 3→1. Aplica a los 3 módulos simultáneamente.

**6. HU-17 futuro: descarga congela versiones.**
Cuando HU-17 se implemente, el avance de rotación durante descarga deberá saltar el avance de versiones (CA-17.04: versiones se congelan). El código se diseña para ser extensible: `advanceRotation` recibirá `isDeload: Boolean` adicional.

**7. Navegación post-cierre: a Home (E5 diferido a HU-13).**
E5 aún no existe. Tras confirmar el cierre exitoso se navega a Home (B1) con `popUpTo(HOME) { inclusive = true }`. La ruta `session-summary/{sessionId}` se registra proactivamente en `NavigationRoutes` como preparación para HU-13 (costo marginal cero).

**8. BackHandler muestra el diálogo de cierre.**
HU-05 documentó el stub `BackHandler { /* no-op */ }`. Se reemplaza con `BackHandler { viewModel.onCloseSessionRequested() }`.

**9. CA-09.06 y CA-09.07 son garantías estructurales, no funcionalidades.**
CA-09.06: la única invocación de `advanceRotation` está dentro de `closeSession`. CA-09.07: no existe funcionalidad de edición/eliminación de sesiones cerradas (ADR D-03).

**10. CA-09.08 (crash recovery) ya está implementado.**
B1 ya detecta sesiones `IN_PROGRESS` y muestra la card "Reanudar Sesión" (HU-05). Si la app crasha durante el cierre, la transacción se revierte — la sesión permanece `IN_PROGRESS`. No se requiere lógica adicional.

### Validación de Impacto

| Archivo | Acción |
|---|---|
| `SessionRepository.kt` | +`closeSession(sessionId: Long)` |
| `SessionRepositoryImpl.kt` | Implementar `closeSession()` con transacción |
| `SessionDao.kt` | +`updateStatus()` |
| `RotationResolver.kt` | +`advanceRotation()` |
| `ActiveSessionViewModel.kt` | +diálogo state + cierre + navegación |
| `ActiveSessionUiState.kt` | +`showCloseDialog`, `isClosing`, `incompleteCount`, `isAllCompleted` |
| `ActiveSessionScreen.kt` | Wiring botón + BackHandler + AlertDialog E4 |
| `NavigationRoutes.kt` | +`SESSION_SUMMARY` |
| `TensionNavHost.kt` | Wiring `onNavigateToSessionSummary` temporal a Home |
| `strings.xml` | +6 strings de E4 |
| `CloseSessionUseCase.kt` | Nuevo archivo |

**Archivos no tocados:**
- `ExerciseSetDao.kt` — tonelaje diferido a HU-13.
- `ExerciseProgressionDao.kt` — HU-10 lo usará.
- `HomeScreen.kt` — crash recovery ya funciona.

### Notas Técnicas

1. **Patrón `.first()` dentro de transacción.** Ya establecido en `startSession()`. Se reutiliza para `getActiveSessionWithModuleVersion().first()` y `getRotationState().first()` en `closeSession()`.
2. **El ViewModel usa `onNavigateToSessionSummary` desde el principio.** El SharedFlow emite el `sessionId`, la Screen observa y llama al callback. TensionNavHost redirige temporalmente a Home. Cuando HU-13 implemente E5, solo cambia la lambda en TensionNavHost.
3. **La ruta `SESSION_SUMMARY` se agrega proactivamente.** Costo marginal cero — prepara HU-13.
4. **Progresión (HU-10/HU-11/HU-12) no se invoca en HU-09.** El punto de extensión será dentro de la transacción de `closeSession()`, después de actualizar el status y antes de avanzar la rotación.
5. **`advanceRotation()` como función pura.** Operable y testeable unitariamente sin emulador ni mocks (ADR-06: reglas de dominio como Kotlin puro).

### Hitos de implementación

| # | Entregable | Archivos | CAs |
|---|---|---|---|
| 1 | `RotationResolver.advanceRotation()` — función pura + test unitario | `RotationResolver.kt`, `RotationResolverTest.kt` | CA-09.05 |
| 2 | `SessionDao.updateStatus()` — query de update de status | `SessionDao.kt` | CA-09.01, CA-09.02 |
| 3 | `CloseSessionUseCase` + `SessionRepository.closeSession()` + implementación transaccional | `CloseSessionUseCase.kt`, `SessionRepository.kt`, `SessionRepositoryImpl.kt` | CA-09.01, CA-09.02, CA-09.05, CA-09.08 |
| 4 | `ActiveSessionUiState` + `ActiveSessionViewModel` — diálogo state + cierre + navegación | `ActiveSessionUiState.kt`, `ActiveSessionViewModel.kt` | CA-09.01, CA-09.02, CA-09.03 |
| 5 | `ActiveSessionScreen` — BackHandler + botón + AlertDialog E4 | `ActiveSessionScreen.kt`, `strings.xml` | CA-09.03 |
| 6 | `NavigationRoutes` + `TensionNavHost` — ruta E5 stub + wiring navegación post-cierre | `NavigationRoutes.kt`, `TensionNavHost.kt` | — (preparación HU-13) |

### Historias Relacionadas

**Predecesoras:**
- HU-05: Proporcionó stubs de `BackHandler { no-op }`, botón "Cerrar Sesión", `onNavigateToSessionSummary`, y la card de crash recovery en B1. También estableció el patrón `database.withTransaction` + `.first()`.
- HU-06: Proporcionó `exercise_set` data que determina conteo de sets por ejercicio → status COMPLETED/INCOMPLETE.
- HU-07: Sustituciones funcionan transparentemente — `session_exercise.exercise_id` apunta al ejercicio real sin lógica condicional.

**Sucesoras:**
- HU-10: Motor de progresión — se integrará dentro de la transacción de `closeSession()`.
- HU-11: Doble Umbral — también en el cierre.
- HU-12: Detección de regresión/fatiga — también post-cierre.
- HU-13: E5 (Resumen Post-Sesión) — `sessionId` se prepara. Tonelaje se calcula on-demand.
- HU-17: Protocolo de descarga — modificará `advanceRotation` para congelar versiones durante deload.
- HU-18: Usará `microcycleCount` incrementado por HU-09 en B1.

### Verificación Cruzada de CAs

| CA | Estado | Mecanismo | Implementado en |
|---|---|---|---|
| CA-09.01 | Por implementar | `closeSession()` → `updateStatus("COMPLETED")` si todos completos | HU-09 (transacción) |
| CA-09.02 | Por implementar | `closeSession()` → `updateStatus("INCOMPLETE")` si parcial | HU-09 (transacción) |
| CA-09.03 | Por implementar | AlertDialog E4 con 2 variantes (completa/parcial) + conteo | HU-09 (UI) |
| CA-09.04 | Parcial | Tonelaje = dato derivado, no almacenado. Query será en HU-13 | HU-09 persiste datos; HU-13 calcula |
| CA-09.05 | Por implementar | `RotationResolver.advanceRotation()` + `rotationStateDao.update()` | HU-09 (transacción) |
| CA-09.06 | Estructural | `advanceRotation` solo se invoca desde `closeSession` | Garantía por diseño |
| CA-09.07 | Estructural | No existe endpoint de edición post-cierre (ADR D-03) | Garantía por diseño |
| CA-09.08 | Ya implementado | `withTransaction` rollback + B1 crash recovery card | HU-05 (B1 recovery) + Room |
