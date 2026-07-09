## Análisis Arquitectónico

### Modelo de datos — nuevas columnas

```sql
-- MIGRATION_11_12: plan_assignment
ALTER TABLE plan_assignment ADD COLUMN slot INTEGER NOT NULL DEFAULT 0;

-- MIGRATION_11_12: session_exercise
ALTER TABLE session_exercise ADD COLUMN pending_selection INTEGER NOT NULL DEFAULT 0;
ALTER TABLE session_exercise ADD COLUMN slot INTEGER NOT NULL DEFAULT 0;

-- MIGRATION_12_13: corrección de datos (plan_assignment)
UPDATE plan_assignment SET slot = sort_order;

-- MIGRATION_12_13: corrección de datos (session_exercise)
UPDATE session_exercise AS se
SET slot = (
    SELECT pa.slot
    FROM plan_assignment pa
    INNER JOIN session s ON s.routine_version_id = pa.routine_version_id
    WHERE s.id = se.session_id
      AND pa.exercise_id = se.exercise_id
);
```

### Invariante de PK y unicidad

- PK de `plan_assignment`: `(routine_version_id, exercise_id)` — sin cambios. Garantiza que un ejercicio no puede asignarse dos veces a la misma versión (CA-26.07).
- `slot` en `plan_assignment` es un entero que agrupa ejercicios del mismo puesto; no es único. Múltiples ejercicios pueden compartir el mismo valor de `slot` dentro de la misma `routine_version_id`.
- Índice único de `session_exercise`: `(session_id, exercise_id)` — sin cambios. Dado que un mismo ejercicio solo puede pertenecer a un slot por versión, no puede haber colisiones al intercambiar alternativas.

### Cadena de invocación — Agregar alternativa al plan

```
D4 ViewModel
  └── AddAlternativeToSlotUseCase.invoke(routineVersionId, targetSlot, exerciseId)
        ├── [Guard] hasActiveSessionForVersion(routineVersionId) → error si activa
        ├── [Guard] ejercicio ya asignado en la versión → error si existe
        └── PlanAssignmentRepository.addAlternativeToSlot(routineVersionId, targetSlot, exerciseId, sets, reps)
              └── PlanAssignmentDao.insertWithSlot(PlanAssignmentEntity)
```

### Cadena de invocación — Intercambiar alternativa en sesión

```
E1 ViewModel
  └── SessionRepository.switchAlternativeInSession(sessionId, currentExerciseId, newExerciseId)
        └── SessionDao.switchAlternativeExercise(sessionId, currentExerciseId, newExerciseId)
              -- UPDATE session_exercise
              -- SET exercise_id = :newExerciseId,
              --     original_exercise_id = NULL
              -- WHERE session_id = :sessionId AND exercise_id = :currentExerciseId
```

### Cadena de invocación — Eliminar slot completo

```
D4 ViewModel
  └── UnassignExerciseFromVersionUseCase.invoke(routineVersionId, slot)  [modo slot completo]
        ├── [Guard] hasActiveSessionForVersion(routineVersionId) → error si activa
        └── PlanAssignmentRepository.deleteSlot(routineVersionId, slot)
              └── PlanAssignmentDao.deleteByVersionAndSlot(routineVersionId, slot)
                    -- DELETE FROM plan_assignment
                    -- WHERE routine_version_id = :routineVersionId AND slot = :slot
```

### Modelo UI — ExerciseSessionUiModel

```kotlin
data class ExerciseSessionUiModel(
    val sessionExerciseId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val slot: Int,
    val alternativesInSlot: Int,  // cantidad de alternativas disponibles en el slot del plan
    val pendingSelection: Int,
    val completedSets: Int,
    val isFinalized: Int,
    // ... demás campos
)
```

El campo `alternativesInSlot` se calcula en el DAO con una subconsulta al plan:

```sql
SELECT se.*,
       e.name AS exercise_name,
       (
           SELECT COUNT(*) FROM plan_assignment pa
           WHERE pa.routine_version_id = s.routine_version_id
             AND pa.slot = se.slot
       ) AS alternatives_in_slot
FROM session_exercise se
INNER JOIN exercise e ON e.id = se.exercise_id
INNER JOIN session s ON s.id = se.session_id
WHERE se.session_id = :sessionId
ORDER BY se.slot ASC, se.sort_order ASC
```

### Propagación de sets/reps a alternativas del slot

```kotlin
// PlanAssignmentRepository
suspend fun updateSlotSetsAndReps(routineVersionId: Long, slot: Int, sets: Int, reps: String) {
    dao.updateByVersionAndSlot(routineVersionId, slot, sets, reps)
}

// PlanAssignmentDao
@Query("""
    UPDATE plan_assignment
    SET sets = :sets, reps = :reps
    WHERE routine_version_id = :routineVersionId AND slot = :slot
""")
suspend fun updateByVersionAndSlot(routineVersionId: Long, slot: Int, sets: Int, reps: String)
```

### Pantalla D4 — Agrupación de filas por slot

```kotlin
// En RoutineVersionDetailViewModel o UseCase de presentación:
val slotGroups: Map<Int, List<PlanAssignmentUiModel>> = assignments.groupBy { it.slot }

// En Compose:
slotGroups.forEach { (slot, exercises) ->
    val title = exercises.joinToString(" ó ") { it.exerciseName }
    SlotRow(
        title = title,
        sets = exercises.first().sets,
        reps = exercises.first().reps,
        onAddAlternative = { /* abre bottom sheet */ },
        onEditSetsReps = { /* edita todas las alternativas del slot */ },
        onDelete = { /* elimina todo el slot */ }
    )
}
```

### Pantalla E1 — Visibilidad del icono SwapHoriz

```kotlin
// En ActiveSessionScreen:
if (exercise.alternativesInSlot > 1 && exercise.completedSets == 0 && exercise.isFinalized == 0) {
    IconButton(onClick = { viewModel.openAlternativeSelector(exercise) }) {
        Icon(
            imageVector = Icons.Default.SwapHoriz,
            contentDescription = "Intercambiar alternativa"
        )
    }
}
```

### Guard de sesión activa — patrón compartido

```kotlin
// PlanAssignmentRepository (o SessionRepository según arquitectura)
suspend fun hasActiveSessionForVersion(routineVersionId: Long): Boolean {
    return sessionDao.countActiveSessionsForVersion(routineVersionId) > 0
}

// En use cases que modifican el plan:
if (repository.hasActiveSessionForVersion(routineVersionId)) {
    throw ActiveSessionException("No se puede modificar el plan mientras hay una sesión activa")
}
```

---

## Componentes Afectados

### Tablas afectadas

| Tabla | Cambio |
|---|---|
| `plan_assignment` | +`slot INTEGER NOT NULL DEFAULT 0` (MIGRATION_11_12); datos corregidos en MIGRATION_12_13 |
| `session_exercise` | +`pending_selection INTEGER NOT NULL DEFAULT 0`, +`slot INTEGER NOT NULL DEFAULT 0` (MIGRATION_11_12); datos corregidos en MIGRATION_12_13 |

### Pantallas afectadas

| Pantalla | Cambio |
|---|---|
| D4 — RoutineVersionDetailScreen | Agrupar filas por slot, mostrar nombres concatenados con "ó", botón "+" por slot, edición/eliminación por slot |
| E1 — ActiveSessionScreen | Icono SwapHoriz cuando alternativesInSlot > 1 y NOT_STARTED; bottom sheet de selección de alternativa |

### Versiones de DB

| Migración | Descripción | Dirección |
|---|---|---|
| MIGRATION_11_12 | ALTER TABLE plan_assignment ADD COLUMN slot; ALTER TABLE session_exercise ADD COLUMN pending_selection, slot | v11 → v12 |
| MIGRATION_12_13 | UPDATE plan_assignment SET slot = sort_order; UPDATE session_exercise SET slot = (plan slot correspondiente) | v12 → v13 |

### Dependencias de historias previas

| Historia | Aporte a HU-26 |
|---|---|
| HU-23 (Transición a rutinas dinámicas) | Tablas `routine`, `routine_version`, `plan_assignment`, `session`, `session_exercise`; use cases de asignación y creación de sesión |
| HU-24 (Actualización del Diccionario) | Catálogo de ejercicios completo (26+ ejercicios, 4 versiones de rutina), MIGRATION_9_10 |
| HU-25 (Ajustes de usabilidad) | `is_finalized` en `session_exercise`, mapeo de status `COMPLETED/NOT_STARTED/IN_PROGRESS`, MIGRATION_10_11, DB en v11 |

### ADR consultados

- ADR-03 (Room): `Migration(11, 12)`, `Migration(12, 13)`, `ALTER TABLE`, `exportSchema = true`
- ADR-04 (Hilt): Nuevos UseCases inyectados automáticamente
- ADR-09 (StateFlow): Nuevos estados en RoutineVersionDetailViewModel y ActiveSessionViewModel
- ADR-11 (Migraciones): En `Migrations.kt`, registrado en `DatabaseModule`, versión incrementada en `@Database`

### Notas Técnicas

- **Nota 1 — `original_exercise_id` distingue sustitución de intercambio:** En `session_exercise`, `original_exercise_id != NULL` indica sustitución (ejercicio fuera del plan), mientras que `original_exercise_id = NULL` indica intercambio de alternativa (ejercicio del plan). Al ejecutar `switchAlternativeExercise`, el DAO limpia `original_exercise_id = NULL` explícitamente.
- **Nota 2 — MIGRATION_12_13 necesaria por seeder:** El seeder de `BaseDataSeeder` insertó `plan_assignment` sin establecer el campo `slot` (valor DEFAULT 0 para todos). Esto rompería el agrupamiento por slot en instalaciones frescas. MIGRATION_12_13 corrige alineando `slot = sort_order`.
- **Nota 3 — `alternativesInSlot` calculado por subconsulta:** No se almacena como campo; se calcula en el DAO con subconsulta al `plan_assignment` para mantener consistencia.
- **Nota 4 — Guard de concurrencia compartido:** `hasActiveSessionForVersion` se reutiliza en múltiples UseCases (AddAlternativeToSlot, AssignExerciseToVersion, UnassignExerciseFromVersion) para evitar modificaciones del plan durante sesiones activas.

### Métricas de Análisis Arquitectónico

- PERT: O=40, M=55, P=80 → E ≈ 58 min
- Inicio: 2026-05-08 | Fin: 2026-05-08 | Duración: 18 min
