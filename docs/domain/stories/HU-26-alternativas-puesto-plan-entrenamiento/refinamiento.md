## Refinamiento Técnico (Developer)
**Autor**: Esteban Colorado González | **Fecha**: 2026-05-08

---

### Notas de invariante de datos

- `original_exercise_id` en `session_exercise` distingue entre **sustitución** (ejercicio fuera del plan, `original_exercise_id != NULL`) y **intercambio de alternativa** (ejercicio del plan, `original_exercise_id = NULL`). Al ejecutar `switchAlternativeExercise`, el DAO limpia `original_exercise_id = NULL` explícitamente.
- La pantalla de progresión y el cálculo de KPIs solo consideran `session_exercise` con `exercise_id IS NOT NULL` y series registradas — no se ven afectados por el intercambio de alternativas mientras `completedSets == 0`.
- El índice único `(session_id, exercise_id)` en `session_exercise` garantiza que no hay colisión al intercambiar: el ejercicio anterior se reemplaza en la misma fila, no se inserta una nueva.

### Escenario de instalación fresca y MIGRATION_12_13

La razón de la migración 12→13 es que el seeder de `BaseDataSeeder` (ejecutado en instalaciones frescas) insertó los registros de `plan_assignment` sin establecer el campo `slot` (valor DEFAULT 0 para todos). Esto rompería el agrupamiento por slot. MIGRATION_12_13 corrige los datos existentes en instalaciones frescas alineando `slot = sort_order`, de modo que cada ejercicio queda en un slot único inicial (sin alternativas agrupadas accidentalmente).

### Bottom sheet de selección de alternativa (sesión)

El bottom sheet debe filtrar del catálogo los ejercicios que:
1. Ya están asignados al slot en `plan_assignment` para la versión de rutina activa.
2. No son el ejercicio actualmente seleccionado en el `session_exercise`.

```kotlin
// Query DAO sugerida:
@Query("""
    SELECT e.* FROM exercise e
    INNER JOIN plan_assignment pa ON pa.exercise_id = e.id
    INNER JOIN session s ON s.routine_version_id = pa.routine_version_id
    WHERE s.id = :sessionId AND pa.slot = :slot
      AND pa.exercise_id != :currentExerciseId
    ORDER BY pa.sort_order ASC
""")
fun getAlternativesForSlotInSession(sessionId: Long, slot: Int, currentExerciseId: Long): Flow<List<ExerciseEntity>>
```

### Validación de unicidad en AddAlternativeToSlotUseCase

```kotlin
// Antes de insertar, verificar que el ejercicio no existe ya en la versión:
val existing = planAssignmentDao.findByVersionAndExercise(routineVersionId, exerciseId)
if (existing != null) {
    throw DuplicateExerciseException("El ejercicio ya está asignado a esta versión de rutina")
}
```

---

### Consideraciones Generales

**Basado en análisis arquitectónico:**
HU-26 introduce un concepto nuevo de dominio: slots en el plan de entrenamiento. Requiere 2 migraciones de base de datos, nuevas columnas en 2 tablas, agrupación UI por slot, y un mecanismo de intercambio en sesión activa con guard de concurrencia.

**Nivel de complejidad:**
ALTA — HU-26 introduce un concepto de dominio nuevo (slots), requiere 2 migraciones DDL (MIGRATION_11_12 + MIGRATION_12_13), modifica 2 tablas existentes, crea nuevos UseCases, reestructura la UI de D4 (agrupación por slot), añade interacción en E1 (intercambio), y requiere guard de concurrencia para evitar modificaciones del plan durante sesiones activas.

**Riesgos técnicos conocidos:**
1. **MIGRATION_12_13 corrección de datos:** La subconsulta para actualizar `session_exercise.slot` debe ser eficiente y correcta. Si hay sesiones con `routine_version_id` diferente al plan actual, la correlación puede fallar.
2. **Concurrencia en `hasActiveSessionForVersion`:** El guard debe ejecutarse como operación atómica para evitar race conditions entre la validación y la modificación.
3. **Agrupación UI en D4:** El `groupBy` debe ser estable y consistente con el orden de sort_order para que la UI muestre los slots en el orden correcto.
4. **Bottom sheet de intercambio:** Debe filtrar correctamente los ejercicios ya asignados al slot y el ejercicio actual. Si el catálogo cambia durante la sesión, la lista debe actualizarse.

**Patrones y convenciones del equipo:**
- `object : Migration(N, N+1)` dentro de `Migrations.kt` (patrón establecido en HU-16, replicado en HU-21/23/24/25)
- UseCase con `@Inject constructor` + `suspend operator fun invoke()`
- DAO queries con Room `@Query`, DTOs como `data class` dentro del DAO
- StateFlow + SharedFlow en ViewModels (ADR-09)
- Strings en `res/values/strings.xml` para todo texto visible
- Guard de sesión activa como patrón compartido (similar a HU-20, HU-21)

**Dependencias nuevas:** Ninguna.

**Estrategia de testing:** JUnit 4 + MockK + StandardTestDispatcher + runTest | Unitarios (UseCases, guards, unicidad) + Instrumentado (2 migraciones) + Integración (flujo completo agregar → iniciar → intercambiar → bloquear)

### Historias Relacionadas Consultadas

- HU-23: Rutinas dinámicas — tablas base `routine`, `routine_version`, `plan_assignment`, `session`, `session_exercise`. HU-26 extiende `plan_assignment` con slot.
- HU-24: Diccionario de ejercicios — catálogo completo, MIGRATION_9_10. HU-26 usa el catálogo para selección de alternativas.
- HU-25: Ajustes de usabilidad — `is_finalized`, filtros dinámicos, MIGRATION_10_11. HU-26 depende de DB v11.

**Patrones de código reutilizados:**
- Patrón `object : Migration(N, N+1)` con `db.execSQL()` (Migrations.kt)
- Patrón guard de sesión activa `hasActiveSessionForVersion` (compartido entre UseCases)
- Patrón `groupBy` para agrupación UI (similar a agrupación por slot existente en D4)
- Patrón UseCase con validación `require()` / excepciones personalizadas

**Mejores prácticas aplicadas:**
- Separación de responsabilidades: DAO (query agrupada), Repository (lógica de datos), UseCase (guards de negocio), ViewModel (estado UI)
- Migración en 2 pasos: 11→12 para esquema nuevo, 12→13 para corrección de datos — evita romper instalaciones existentes
- `original_exercise_id = NULL` distingue intercambio de sustitución — mantiene semántica clara en el dominio

**Métricas de Refinamiento:**
- PERT: O=55, M=75, P=120 → E ≈ 80 min
- Inicio: 2026-05-08 | Fin: 2026-05-08

---

## Tareas de Implementación

### Fase 0 — Migración de base de datos

- [ ] Implementar `MIGRATION_11_12` en `Migrations.kt`:
  - [ ] `ALTER TABLE plan_assignment ADD COLUMN slot INTEGER NOT NULL DEFAULT 0`
  - [ ] `ALTER TABLE session_exercise ADD COLUMN pending_selection INTEGER NOT NULL DEFAULT 0`
  - [ ] `ALTER TABLE session_exercise ADD COLUMN slot INTEGER NOT NULL DEFAULT 0`
- [ ] Implementar `MIGRATION_12_13` en `Migrations.kt`:
  - [ ] `UPDATE plan_assignment SET slot = sort_order`
  - [ ] `UPDATE session_exercise SET slot = (subconsulta al plan)`
- [ ] Actualizar `AppDatabase.VERSION` a 13
- [ ] Registrar ambas migraciones en `AppDatabase.addMigrations(...)`
- [ ] Actualizar entidades Room: `PlanAssignmentEntity` (+slot), `SessionExerciseEntity` (+pending_selection, +slot)

### Fase 1 — DAO y Repository — Gestión de slots en el plan

- [ ] `PlanAssignmentDao`:
  - [ ] `insertWithSlot(entity: PlanAssignmentEntity)` — insertar con slot explícito
  - [ ] `deleteByVersionAndSlot(routineVersionId, slot)` — eliminar todos los ejercicios del slot
  - [ ] `updateByVersionAndSlot(routineVersionId, slot, sets, reps)` — propagar sets/reps
  - [ ] `findByVersionAndExercise(routineVersionId, exerciseId)` — guard de unicidad
  - [ ] `getByVersionGroupedBySlot(routineVersionId)` — query agrupada para D4
- [ ] `PlanAssignmentRepository`:
  - [ ] `addAlternativeToSlot(routineVersionId, slot, exerciseId, sets, reps)`
  - [ ] `deleteSlot(routineVersionId, slot)`
  - [ ] `updateSlotSetsAndReps(routineVersionId, slot, sets, reps)`
  - [ ] `hasActiveSessionForVersion(routineVersionId)` — guard compartido

### Fase 2 — DAO y Repository — Intercambio en sesión

- [ ] `SessionDao`:
  - [ ] `switchAlternativeExercise(sessionId, currentExerciseId, newExerciseId)` — UPDATE con `original_exercise_id = NULL`
  - [ ] Actualizar `getSessionExercisesWithDetails()` para incluir `alternatives_in_slot` (subconsulta)
  - [ ] `getAlternativesForSlotInSession(sessionId, slot, currentExerciseId)` — lista de alternativas disponibles
- [ ] `SessionRepository`:
  - [ ] `switchAlternativeInSession(sessionId, currentExerciseId, newExerciseId)`
- [ ] `ExerciseSessionUiModel`: añadir campos `slot`, `alternativesInSlot`, `pendingSelection`

### Fase 3 — Use Cases

- [ ] `AddAlternativeToSlotUseCase`:
  - [ ] Guard: `hasActiveSessionForVersion` → error si activa (CA-26.18)
  - [ ] Guard: unicidad de ejercicio en la versión (CA-26.07)
  - [ ] Herencia de sets/reps del ejercicio principal del slot (CA-26.02)
- [ ] `UnassignExerciseFromVersionUseCase` — extender con modo slot completo:
  - [ ] Guard: `hasActiveSessionForVersion` (CA-26.18)
  - [ ] Eliminar todos los ejercicios del slot (CA-26.05)
- [ ] `AssignExerciseToVersionUseCase`:
  - [ ] Guard: `hasActiveSessionForVersion` (CA-26.18)
- [ ] `UpdateSlotSetsRepsUseCase` (o extender existente):
  - [ ] Propagación a todas las alternativas del slot (CA-26.04)

### Fase 4 — ViewModel y UI — Pantalla D4 (Plan)

- [ ] `RoutineVersionDetailViewModel`:
  - [ ] Exponer `slotGroups: Map<Int, List<PlanAssignmentUiModel>>` (agrupado por slot)
  - [ ] `onAddAlternative(slot: Int)` → abre bottom sheet con catálogo filtrado
  - [ ] `onDeleteSlot(slot: Int)` → `UnassignExerciseFromVersionUseCase` modo slot
  - [ ] `onEditSlotSetsReps(slot: Int, sets: Int, reps: String)` → propagación a todas las alternativas
  - [ ] Manejo de `ActiveSessionException` → Snackbar (CA-26.18)
- [ ] `RoutineVersionDetailScreen` (D4):
  - [ ] Refactorizar lista de ejercicios: iterar por slot, no por ejercicio
  - [ ] `SlotRow` composable: título concatenado con " ó ", botones +/✏️/🗑️
  - [ ] Bottom sheet de selección de ejercicio alternativo (catálogo + filtro de ya asignados)

### Fase 5 — ViewModel y UI — Pantalla E1 (Sesión activa)

- [ ] `ActiveSessionViewModel`:
  - [ ] `openAlternativeSelector(exercise: ExerciseSessionUiModel)` → carga alternativas del slot
  - [ ] `confirmAlternativeSwitch(sessionId, currentExerciseId, newExerciseId)` → `switchAlternativeInSession`
  - [ ] Estado: `alternativeSelectorState: AlternativeSelectorState` (idle/open con lista de alternativas)
- [ ] `ActiveSessionScreen` (E1):
  - [ ] Icono `SwapHoriz`: visible si `alternativesInSlot > 1 && completedSets == 0 && isFinalized == 0`
  - [ ] Bottom sheet de intercambio: lista de alternativas con nombre, equipamiento, zonas musculares
  - [ ] Al confirmar: actualizar fila en pantalla con datos del nuevo ejercicio

### Fase 6 — Seeder y datos iniciales

- [ ] Actualizar `BaseDataSeeder` para establecer `slot` correctamente al insertar en `plan_assignment` (prevenir el escenario que MIGRATION_12_13 corrige en retrocompatibilidad)
- [ ] Verificar que las 4 versiones de rutina (Pierna V1, Pierna V2, Push V1, Pull V1) asignan slots únicos por ejercicio (sin agrupaciones iniciales en el plan por defecto)

### Fase 7 — QA / Despliegue

- [ ] Tests unitarios: `AddAlternativeToSlotUseCase` (guards, herencia sets/reps, unicidad)
- [ ] Tests unitarios: `UnassignExerciseFromVersionUseCase` en modo slot completo
- [ ] Tests de migración: MIGRATION_11_12 y MIGRATION_12_13 con datos existentes
- [ ] Tests de integración: flujo completo agregar alternativa → iniciar sesión → intercambiar → registrar serie → bloqueo de intercambio
- [ ] Prueba manual en dispositivo: instalación fresca + actualización desde v11
- [ ] Prueba manual: sesión activa bloquea modificación del plan (Snackbar)
- [ ] Prueba manual: eliminación de slot elimina todas las alternativas
