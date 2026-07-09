## Dev Agent Record — Dev-Rápido

### Debug Log

| # | Tipo | Descripción | Resolución |
|---|------|-------------|------------|
| 1 | Error compilación test | `SubstituteExerciseViewModelTest` referenciaba `getExerciseIdsForSession()` ya eliminado de la interfaz `SessionRepository` | Eliminar la verificación `coVerify(exactly = 0) { sessionRepository.getExerciseIdsForSession(any()) }` del test |
| 2 | Error test runtime | `UncompletedCoroutinesError` en test `when getSubstituteExerciseInfo returns null emits navigateBack` — `MutableSharedFlow(replay=0)` suspende `emit()` sin collector activo | Usar `backgroundScope.launch(UnconfinedTestDispatcher(testScheduler))` para iniciar el collector antes de `advanceUntilIdle()` |
| 3 | Decisión técnica | Auditoría de código encontró 14 hallazgos — 12 preexistentes al cambio de HU-20, 1 dentro del scope | Corregido hallazgo #12 (test `onConfirmSubstitution` no verificaba emisión de `navigateBack`). Los 12 hallazgos preexistentes se documentan pero no se corrigen en esta historia |

### Historial de Código Real Verificado

- **`SubstituteExerciseViewModel.kt`:** El bloque `init` computa `excludedIds` como `sessionRepository.getExerciseIdsForSession(info.sessionId)` (línea ~61) — una invocación `suspend` que retorna `List<Long>` estática. Luego pasa esta lista como parámetro fijo a `exerciseRepository.getEligibleSubstitutes(info.moduleCode, excludedIds)` (línea ~63). El `Flow` resultante nunca se re-emite porque Room no detecta cambios en `session_exercise` a través de un parámetro externo.
- **`ExerciseDao.kt`:** Confirmado que `getByModuleCodeNotInIds()` usa `AND e.id NOT IN (:excludedExerciseIds)`. Room solo infiere dependencia de la tabla `exercise`, no de `session_exercise`.
- **`SessionExerciseDao.kt`:** Confirmado que `getExerciseIdsForSession()` retorna `List<Long>` (no `Flow`) — no hay reactividad en la cadena.
- **`ExerciseRepositoryImpl.kt`:** Delega directamente al DAO con mapeo `toDomainModel()`. No agrega lógica de filtrado.
- **`SessionRepositoryImpl.kt`:** `getExerciseIdsForSession()` (línea ~293) simplemente delega al DAO. `substituteExercise()` y `getSubstituteExerciseInfo()` no se ven afectados.

### Completion Notes

- ✅ Auditoría completada 2026-03-04 — Corrección quirúrgica de cadena de invocación defectuosa. Subconsulta SQL embebida habilitando Room invalidation tracking. Eliminación de código muerto verificada con grep.
- ✅ Desarrollo completado 2026-03-04 — 7 tests unitarios para `SubstituteExerciseViewModel`. BUILD SUCCESSFUL — 318 tests pasan (0 fallos), 0 errores de compilación.
- HU-20 corrige la implementación de HU-07 sin redefinir la funcionalidad. La especificación original de HU-07 es correcta.

### File List

| Acción | Archivo | Descripción |
|--------|---------|-------------|
| Modificado | `data/local/dao/ExerciseDao.kt` | Nuevo `getEligibleSubstitutesForSession()`, eliminado `getByModuleCodeNotInIds()` |
| Modificado | `domain/repository/ExerciseRepository.kt` | Firma `getEligibleSubstitutes()` cambiada a `(moduleCode, sessionId: Long)` |
| Modificado | `data/repository/ExerciseRepositoryImpl.kt` | Delegación al nuevo método DAO |
| Modificado | `ui/session/SubstituteExerciseViewModel.kt` | Eliminado snapshot estático `excludedIds` en `init` |
| Modificado | `data/local/dao/SessionExerciseDao.kt` | Eliminado `getExerciseIdsForSession()` |
| Modificado | `domain/repository/SessionRepository.kt` | Eliminada declaración `getExerciseIdsForSession()` |
| Modificado | `data/repository/SessionRepositoryImpl.kt` | Eliminada implementación `getExerciseIdsForSession()` (líneas ~293-295) |
| Creado | `test/.../ui/session/SubstituteExerciseViewModelTest.kt` | 7 tests unitarios para `SubstituteExerciseViewModel` |

### Métricas Dev-Rápido

- Tests unitarios: 7 (SubstituteExerciseViewModel)
- CAs validados: 13 (CA-20.01 a CA-20.13)
- Archivos modificados: 7
- Archivos creados: 1
- BUILD: SUCCESSFUL — 318 tests pasan (0 fallos), 0 errores de compilación
