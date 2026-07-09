## Análisis Arquitectónico

<!-- ============================================================================ -->
<!-- SECCIÓN AGREGADA POR: Workflow analizar-disenar-historia-usuario            -->
<!-- ETAPA: Análisis Arquitectónico                                              -->
<!-- RESPONSABLE: Arquitecto                                                     -->
<!-- FECHA: 2026-05-08                                                           -->
<!-- ============================================================================ -->

### Componentes Afectados

#### Bloque A — MIGRATION_10_11 en `Migrations.kt` (Nuevo)

- Nivel de cambio: Crítico
- Ubicación: `data/local/database/Migrations.kt`

**Fase 1 — ALTER TABLE session_exercise (soporte CA-25.06/07):**

```sql
ALTER TABLE session_exercise ADD COLUMN is_finalized INTEGER NOT NULL DEFAULT 0;
```

Los ejercicios de sesiones cerradas (status COMPLETED o INCOMPLETE) deben tener is_finalized = 1 para mantener coherencia:

```sql
UPDATE session_exercise SET is_finalized = 1
WHERE session_id IN (
  SELECT id FROM session WHERE status IN ('COMPLETED', 'INCOMPLETE')
);
```

Para la sesión activa (si existe): los ejercicios cuyo `completedSets >= prescribedSets` también se marcan finalizados:

```sql
UPDATE session_exercise SET is_finalized = 1
WHERE session_id IN (SELECT id FROM session WHERE status = 'IN_PROGRESS')
  AND id IN (
    SELECT se.id FROM session_exercise se
    INNER JOIN session s ON se.session_id = s.id
    LEFT JOIN plan_assignment pa ON pa.routine_version_id = s.routine_version_id
      AND pa.exercise_id = COALESCE(se.original_exercise_id, se.exercise_id)
    WHERE (SELECT COUNT(*) FROM exercise_set es WHERE es.session_exercise_id = se.id)
          >= COALESCE(pa.sets, 4)
  );
```

**Fase 2 — Nuevas zonas musculares (CA-25.31/33):**

```sql
INSERT INTO muscle_zone (id, name, muscle_group) VALUES (16, 'Espalda Alta', 'Espalda');
INSERT INTO muscle_zone (id, name, muscle_group) VALUES (17, 'Trapecio', 'Espalda');
INSERT INTO muscle_zone (id, name, muscle_group) VALUES (18, 'Espalda Baja', 'Espalda');
INSERT INTO muscle_zone (id, name, muscle_group) VALUES (19, 'Antebrazo', 'Antebrazo');
INSERT INTO muscle_zone (id, name, muscle_group) VALUES (20, 'Cuello', 'Cuello');
```

**Fase 3 — Nuevos tipos de equipamiento (CA-25.32/33):**

```sql
INSERT INTO equipment_type (id, name) VALUES (16, 'Banda Elástica');
INSERT INTO equipment_type (id, name) VALUES (17, 'Kettlebell');
INSERT INTO equipment_type (id, name) VALUES (18, 'Barra EZ');
INSERT INTO equipment_type (id, name) VALUES (19, 'TRX / Suspensión');
INSERT INTO equipment_type (id, name) VALUES (20, 'Balón Medicinal');
INSERT INTO equipment_type (id, name) VALUES (21, 'Rodillo de Abdomen');
INSERT INTO equipment_type (id, name) VALUES (22, 'Paralelas / Dip Station');
INSERT INTO equipment_type (id, name) VALUES (23, 'Barra Fija');
```

**Fase 4 — Reclasificación Face Pull (CA-25.25):**

El Face Pull se identifica por nombre + equipment_type_id (no por ID fijo, ya que post-HU-23 los IDs son dinámicos):

```sql
UPDATE exercise_muscle_zone
SET muscle_zone_id = 16
WHERE muscle_zone_id = 7
  AND exercise_id IN (
    SELECT id FROM exercise WHERE name = 'Face Pull'
  );
```

Esto cambia todas las instancias de Face Pull de Hombro (7) a Espalda Alta (16). El uso de `name = 'Face Pull'` es seguro porque UNIQUE(name, equipment_type_id) garantiza unicidad.

- **`TensionDatabase.kt` (Modificación):** Cambiar `version = 10` a `version = 11`. Nivel: Menor. Ubicación: `data/local/database/TensionDatabase.kt`

- **`DatabaseModule.kt` (Modificación):** Registrar `MIGRATION_10_11` en la cadena de migraciones. Nivel: Menor. Ubicación: `di/DatabaseModule.kt`

  ```kotlin
  .addMigrations(
      Migrations.MIGRATION_6_7, Migrations.MIGRATION_7_8,
      Migrations.MIGRATION_8_9, Migrations.MIGRATION_9_10, Migrations.MIGRATION_10_11
  )
  ```

#### Bloque B — Edición de series/reps en plan oficial (CA-25.01–05)

- **`PlanAssignmentDao.kt` (Modificación):** Agregar query UPDATE para modificar sets y reps.

  ```kotlin
  @Query("""
      UPDATE plan_assignment SET sets = :sets, reps = :reps
      WHERE routine_version_id = :routineVersionId AND exercise_id = :exerciseId
  """)
  suspend fun updateSetsAndReps(routineVersionId: Long, exerciseId: Long, sets: Int, reps: String)
  ```

- **`PlanRepository.kt` / `PlanRepositoryImpl.kt` (Modificación):** Exponer método `updatePlanAssignment(routineVersionId, exerciseId, sets, reps)`. Nivel: Menor.

- **`UpdatePlanAssignmentUseCase.kt` (Nuevo):** Valida restricciones de negocio y delega al repositorio. Ubicación: `domain/usecase/plan/UpdatePlanAssignmentUseCase.kt`

  ```kotlin
  class UpdatePlanAssignmentUseCase @Inject constructor(
      private val planRepository: PlanRepository,
  ) {
      suspend operator fun invoke(routineVersionId: Long, exerciseId: Long, sets: Int, reps: String) {
          require(sets in 1..10) { "Sets must be between 1 and 10" }
          require(isValidReps(reps)) { "Invalid reps format" }
          planRepository.updatePlanAssignment(routineVersionId, exerciseId, sets, reps)
      }

      private fun isValidReps(reps: String): Boolean {
          if (reps == "TO_TECHNICAL_FAILURE" || reps == "30-45_SEC") return true
          val range = reps.split("-")
          return range.size == 2
              && range[0].toIntOrNull() != null
              && range[1].toIntOrNull() != null
              && range[0].toInt() > 0
              && range[0].toInt() < range[1].toInt()
      }
  }
  ```

- **`PlanVersionDetailViewModel.kt` (Modificación):** Agregar `_editDialogState: MutableStateFlow<EditPlanAssignmentState?>`, métodos `onEditExercise(exercise)`, `onEditSetsChanged(sets)`, `onEditRepsSelected(reps)`, `onConfirmEdit()`, `onDismissEditDialog()`. Nivel: Mayor.

- **`PlanVersionDetailScreen.kt` (Modificación):** Agregar composable para diálogo de edición con NumberPicker para sets (1-10) y selector/input para reps. Trigger: long-press o tap en ejercicio asignado. Nivel: Mayor.

- **`PlanVersionDetailUiState.kt` (Modificación):** Agregar data class `EditPlanAssignmentState` con campos `exerciseId`, `exerciseName`, `currentSets`, `currentReps`, `newSets`, `newReps`. Nivel: Menor.

#### Bloque C — Series extra y finalización anticipada en sesión (CA-25.06–10)

- **`SessionExerciseEntity.kt` (Modificación):** Agregar `@ColumnInfo(name = "is_finalized", defaultValue = "0") val isFinalized: Int = 0`. Nivel: Crítico.

- **`SessionExerciseDao.kt` (Modificación):** Agregar query finalizeExercise y actualizar proyecciones.

  ```kotlin
  @Query("UPDATE session_exercise SET is_finalized = 1 WHERE id = :sessionExerciseId")
  suspend fun finalizeExercise(sessionExerciseId: Long)
  ```

  Actualizar query `getSessionExerciseDetails` para incluir `se.is_finalized AS isFinalized` en la proyección. Agregar campo `isFinalized: Int` al DTO `SessionExerciseWithDetails`.

- **`SessionDao.kt` (Modificación):** Actualizar 3 queries que usan `completedSets >= COALESCE(pa.sets, 4)` para usar `se2.is_finalized = 1`. Nivel: Mayor.

  **Query 1 — `getActiveSessionWithRoutineVersion()` (línea 53):** Subquery de conteo de completados:

  ```sql
  -- ANTES:
  (SELECT COUNT(*) FROM session_exercise se2
   WHERE se2.session_id = s.id
     AND (SELECT COUNT(*) FROM exercise_set WHERE session_exercise_id = se2.id)
         >= COALESCE((...pa.sets...), 4)
  ) AS completedExercises

  -- DESPUÉS:
  (SELECT COUNT(*) FROM session_exercise se2
   WHERE se2.session_id = s.id
     AND se2.is_finalized = 1
  ) AS completedExercises
  ```

  **Query 2 — `getSessionSummaryInfo()` (línea 105):** Mismo cambio.

- **`SessionRepositoryImpl.kt` (Modificación):** Nivel: Mayor.

  ```kotlin
  // En registerSet(): ELIMINAR líneas 355-357 (guard de max sets)
  // if (nextSetNumber > info.totalSets) {
  //     throw IllegalStateException("Exercise already has maximum sets registered")
  // }

  // En el mapeo de status (líneas 243-245):
  val status = when {
      detail.isFinalized == 1 -> ExerciseSessionStatus.COMPLETED
      detail.completedSets == 0 -> ExerciseSessionStatus.NOT_STARTED
      else -> ExerciseSessionStatus.IN_PROGRESS
  }

  // Nuevo método:
  override suspend fun finalizeExercise(sessionExerciseId: Long) {
      sessionExerciseDao.finalizeExercise(sessionExerciseId)
  }
  ```

  En `closeSession()`: finalizar automáticamente todos los ejercicios no finalizados antes de evaluar completedExercises:

  ```sql
  UPDATE session_exercise SET is_finalized = 1 WHERE session_id = :sessionId AND is_finalized = 0
  ```

- **`SessionRepository.kt` (Modificación):** Agregar `suspend fun finalizeExercise(sessionExerciseId: Long)`. Nivel: Menor.

- **`FinalizeExerciseUseCase.kt` (Nuevo):** Delegación simple al repositorio. Ubicación: `domain/usecase/session/FinalizeExerciseUseCase.kt`

- **`ActiveSessionViewModel.kt` (Modificación):** Agregar acción `onFinalizeExercise(sessionExerciseId)`. La UI del botón "Registrar" cambia: cuando `completedSets >= prescribedSets && !isFinalized`, mostrar opciones "Agregar serie extra" (navega a RegisterSet) y "Finalizar ejercicio" (invoca finalize). Nivel: Mayor.

- **`ActiveSessionScreen.kt` (Modificación):** Card de ejercicio con botón contextual: "Registrar" cuando hay series pendientes → "Agregar serie extra" / "Finalizar ejercicio" cuando se alcanzaron las prescritas → ocultar botones cuando `isFinalized`. Nivel: Mayor.

- **`ActiveSessionUiState.kt` (Modificación):** Agregar `isFinalized: Boolean` a `ExerciseUiItem`. Actualizar `statusDisplayText` para mostrar "5/4 series" cuando `completedSets > prescribedSets`. Nivel: Menor.

- **`GetRegisterSetInfoUseCase.kt` / `SessionRepositoryImpl.getRegisterSetInfo()` (Modificación):** Eliminar el guard `if (nextSetNumber > info.totalSets) return null`. El título en RegisterSetScreen mostrará "Serie 5 de 4" para series extra. Nivel: Menor.

- **`RegisterSetViewModel.kt` (Modificación):** Manejar `currentSetNumber > totalSets` (serie extra) en el string del título. Nivel: Menor.

- **`ExerciseSummaryItem.kt` (Modificación):** Agregar `completedSets: Int` y `prescribedSets: Int` para CA-25.09. Ubicación: `domain/model/ExerciseSummaryItem.kt`

- **`GetSessionSummaryUseCase.kt` (Modificación):** Incluir `completedSets` y `prescribedSets` al construir cada `ExerciseSummaryItem`. Nivel: Menor.

- **`SessionSummaryScreen.kt` (Modificación):** Actualizar `ExerciseSummaryRow` para mostrar "X/Y series" como supporting text. Nivel: Menor.

#### Bloque D — Reducción del rango de RIR de 0-5 a 0-2 (CA-25.11–16)

- **`RegisterSetScreen.kt` — `RirSelector` (Modificación):**

  ```kotlin
  // ANTES: for (rir in 0..5) {
  // DESPUÉS:
  for (rir in 0..2) {
  ```
  Ubicación: `ui/session/RegisterSetScreen.kt` línea 282

- **`RegisterSetUseCase.kt` (Modificación):**

  ```kotlin
  // ANTES: require(rir in 0..5) { "RIR must be between 0 and 5" }
  // DESPUÉS:
  require(rir in 0..2) { "RIR must be between 0 and 2" }
  ```
  Ubicación: `domain/usecase/session/RegisterSetUseCase.kt` línea 17

- **`AlertThresholdRule.kt` (Modificación):**

  ```kotlin
  // ANTES:
  const val RIR_LOW_THRESHOLD = 1.5
  const val RIR_HIGH_THRESHOLD = 3.5
  // DESPUÉS:
  const val RIR_LOW_THRESHOLD = 0.5
  const val RIR_HIGH_THRESHOLD = 1.8
  ```
  Ubicación: `domain/rules/AlertThresholdRule.kt`

- **`strings.xml` — strings de RIR (Modificación):**

  ```xml
  <!-- CA-25.14: Métricas RIR reference -->
  <string name="metrics_rir_reference">1 = óptimo · &lt; 0.5 = riesgo · > 1.8 = insuficiente</string>

  <!-- CA-25.15: Deload protocol RIR -->
  <string name="deload_protocol_rir">· RIR 2</string>
  <string name="deload_params_rir">RIR objetivo: 2</string>

  <!-- CA-25.16: Deload bodyweight/isometric RIR -->
  <string name="deload_protocol_bodyweight">· Peso corporal: 8 reps, RIR 2 (sin ajuste de carga)</string>
  <string name="deload_protocol_isometric">· Isométricos: 30 seg, RIR 2</string>
  ```

  **Nota:** La prescripción de RIR durante deload es **solo informativa** (strings en UI). No existe lógica de negocio que fuerce un valor de RIR durante sesiones de deload. Solo se actualizan los strings descriptivos.

#### Bloque E — Filtros dinámicos en diccionario (CA-25.17–21)

- **DAOs (Modificación):** Agregar queries con JOIN para obtener solo zonas/equipamientos que tienen ejercicios.

  ```kotlin
  @Query("""
      SELECT DISTINCT mz.* FROM muscle_zone mz
      INNER JOIN exercise_muscle_zone emz ON mz.id = emz.muscle_zone_id
      ORDER BY mz.name ASC
  """)
  fun getMuscleZonesWithExercises(): Flow<List<MuscleZoneEntity>>

  @Query("""
      SELECT DISTINCT et.* FROM equipment_type et
      INNER JOIN exercise e ON et.id = e.equipment_type_id
      ORDER BY et.name ASC
  """)
  fun getEquipmentTypesWithExercises(): Flow<List<EquipmentTypeEntity>>
  ```
  Ubicación: `data/local/dao/MuscleZoneDao.kt` o `ExerciseDao.kt` / `EquipmentTypeDao.kt`

- **`ExerciseRepository.kt` / `ExerciseRepositoryImpl.kt` (Modificación):** Agregar métodos `getEquipmentTypesWithExercises()` y `getMuscleZonesWithExercises(): Flow<List<T>>`. Nivel: Menor.

- **`GetFilterOptionsUseCase.kt` (Modificación):** Cambiar de `getAllEquipmentTypes()` + `getAllMuscleZones()` a `getEquipmentTypesWithExercises()` + `getMuscleZonesWithExercises()`. Nivel: Mayor.

  ```kotlin
  // ANTES:
  combine(exerciseRepository.getAllEquipmentTypes(), exerciseRepository.getAllMuscleZones()) { ... }
  // DESPUÉS:
  combine(exerciseRepository.getEquipmentTypesWithExercises(), exerciseRepository.getMuscleZonesWithExercises()) { ... }
  ```

- **`GetAllFilterOptionsUseCase.kt` (Nuevo):** Exactamente el mismo código actual de `GetFilterOptionsUseCase` (usa `getAllEquipmentTypes()` y `getAllMuscleZones()`). Para uso exclusivo en pantalla de creación de ejercicio (D5). Ubicación: `domain/usecase/catalog/GetAllFilterOptionsUseCase.kt`

- **`CreateExerciseViewModel.kt` (Modificación):** Cambiar inyección de `GetFilterOptionsUseCase` a `GetAllFilterOptionsUseCase` para mantener comportamiento de mostrar TODAS las opciones al crear ejercicio (CA-25.21). Nivel: Menor.

#### Bloque F — Reclasificación Face Pull + nuevas zonas/equipamiento en seeders (CA-25.22–26, CA-25.31–34)

- **`BaseDataSeeder.kt` (Modificación):** Agregar 5 zonas musculares (IDs 16-20) y 8 tipos de equipamiento (IDs 16-23).

  ```kotlin
  // Zonas musculares:
  MuscleZoneEntity(id = 16, name = "Espalda Alta", muscleGroup = "Espalda"),
  MuscleZoneEntity(id = 17, name = "Trapecio", muscleGroup = "Espalda"),
  MuscleZoneEntity(id = 18, name = "Espalda Baja", muscleGroup = "Espalda"),
  MuscleZoneEntity(id = 19, name = "Antebrazo", muscleGroup = "Antebrazo"),
  MuscleZoneEntity(id = 20, name = "Cuello", muscleGroup = "Cuello"),

  // Tipos de equipamiento:
  EquipmentTypeEntity(id = 16, name = "Banda Elástica"),
  EquipmentTypeEntity(id = 17, name = "Kettlebell"),
  EquipmentTypeEntity(id = 18, name = "Barra EZ"),
  EquipmentTypeEntity(id = 19, name = "TRX / Suspensión"),
  EquipmentTypeEntity(id = 20, name = "Balón Medicinal"),
  EquipmentTypeEntity(id = 21, name = "Rodillo de Abdomen"),
  EquipmentTypeEntity(id = 22, name = "Paralelas / Dip Station"),
  EquipmentTypeEntity(id = 23, name = "Barra Fija"),
  ```
  Ubicación: `data/local/database/BaseDataSeeder.kt`

- **`ExerciseSeeder.kt` (Modificación):** Cambiar `emz(db, 14, 7)` a `emz(db, 14, 16)` (Hombro ID 7 → Espalda Alta ID 16). Ubicación: `data/local/seed/ExerciseSeeder.kt`

#### Bloque G — Renombrar "Configuración" a "Ajustes" (CA-25.27–30)

- **`strings.xml` (Modificación):**

  ```xml
  <!-- CA-25.27 -->
  <string name="nav_settings">Ajustes</string>
  <!-- CA-25.28 -->
  <string name="settings_title">Ajustes</string>
  <!-- CA-25.29 -->
  <string name="import_backup_back_to_settings">Volver a Ajustes</string>
  ```

  CA-25.30 explícitamente indica que `NavigationRoutes.SETTINGS` y el nombre interno no cambian — solo texto visible.

### Hitos de Implementación

| Hito | Contenido | Dependencias |
|---|---|---|
| 1 | MIGRATION_10_11 + SessionExerciseEntity.is_finalized + TensionDatabase v11 + DatabaseModule | Ninguna (base para todo) |
| 2 | Seeders: BaseDataSeeder + ExerciseSeeder (nuevas zonas, equipamiento, Face Pull) | Hito 1 |
| 3 | Bloque B: Edición series/reps en plan (DAO, UseCase, ViewModel, Screen) | Ninguna directa |
| 4 | Bloque C: Series extra + finalización anticipada (Repository, UseCase, ViewModel, Screen) | Hito 1 (requiere is_finalized) |
| 5 | Bloque D: RIR 0–2 (RirSelector, UseCase, AlertThresholdRule, strings) | Ninguna |
| 6 | Bloque E: Filtros dinámicos (DAO queries, Repository, UseCases, ViewModel) | Ninguna |
| 7 | Bloque G: Strings "Ajustes" (solo strings.xml) | Ninguna |
| 8 | Documentación (CA-25.35–42) | Todos los bloques completados |

### Validación de Impacto

**Cadena de impacto — Bloque C (`is_finalized`):**

- `SessionExerciseDao.getSessionExerciseDetails()` → `SessionRepositoryImpl.getSessionExercises()` → `GetSessionExercisesUseCase` → `ActiveSessionViewModel` — incluir `is_finalized` en proyección DTO
- `SessionDao` queries de conteo (`completedExercises`) — usadas en Home (B1), history (F1/F2), summary (E5), close dialog (E4) — todas cambian de `completedSets >= sets` a `is_finalized = 1`
- `SessionRepositoryImpl.registerSet()` — eliminar guard de max sets para series extra
- `SessionRepositoryImpl.getRegisterSetInfo()` — eliminar guard `nextSetNumber > totalSets` que retorna null
- `CloseSessionUseCase` / `closeSession()` — finalizar todos los ejercicios al cerrar sesión

**Registros históricos con RIR 3-5:** Los umbrales nuevos (0.5/1.8) se aplicarán retroactivamente a promedios que incluyen valores 3-5. Comportamiento esperado según CA-25.12/13: el usuario verá alertas que le indican ajustar su esfuerzo al nuevo rango.

**Filtros dinámicos:** `ExerciseDictionaryViewModel` usa `GetFilterOptionsUseCase` (dinámico). `CreateExerciseViewModel` cambia a `GetAllFilterOptionsUseCase` (completo). Ambos son `Flow` reactivos — si se crea un ejercicio con nueva zona, el filtro del diccionario se actualiza automáticamente (CA-25.19).

**ADR consultados:**
- ADR-03 (Room): `Migration(10, 11)`, `ALTER TABLE`, `exportSchema = true`
- ADR-04 (Hilt): Nuevo UseCase inyectado automáticamente
- ADR-07 (Navigation): No se agregan rutas — `NavigationRoutes.SETTINGS` no cambia (CA-25.30)
- ADR-09 (StateFlow): Nuevos estados en PlanVersionDetailViewModel siguen el mismo patrón
- ADR-11 (Migraciones): En `Migrations.kt`, registrado en `DatabaseModule`, versión incrementada en `@Database`

### Notas Técnicas

- **Nota 1 — Face Pull: identificación por nombre (no por ID):** Post-HU-23, los IDs de ejercicio son dinámicos. El Face Pull no tiene ID fijo en la base de datos migrada. La migración usa `WHERE name = 'Face Pull'` — seguro porque UNIQUE(name, equipment_type_id) garantiza unicidad.

- **Nota 2 — `is_finalized` vs eliminación del guard:** Se eligió `is_finalized` sobre contar series > prescritas como "completado" porque: (1) permite finalización anticipada (2/4 → completado), (2) permite series extra sin auto-completar (5/4 → sigue IN_PROGRESS), (3) es un cambio de estado explícito que el usuario controla, (4) compatible hacia atrás: ejercicios en sesiones cerradas migran con `is_finalized = 1`.

- **Nota 3 — Strings de deload:** No existe lógica de negocio que fuerce un valor de RIR durante deload. Los strings son puramente informativos. Con el nuevo rango 0-2, el valor máximo (RIR 2) coincide con lo prescrito en el protocolo de descarga actualizado.

- **Nota 4 — Validación de reps en `UpdatePlanAssignmentUseCase`:** Acepta: rangos numéricos (`"6-8"`, `"8-12"`, `"3-5"`), `"TO_TECHNICAL_FAILURE"`, `"30-45_SEC"`. El `RepsRangeParser` existente ya maneja estos formatos. Sin CHECK constraint en DB — consistente con el estado actual del esquema.

- **Nota 5 — `Modelo de Datos.md` corrección de CHECK constraint:** El documento línea 282 documenta `CHECK(reps IN ('8-12', 'TO_TECHNICAL_FAILURE', '30-45_SEC'))` pero el esquema real nunca tuvo esta restricción. La documentación debe actualizarse para indicar que reps es `TEXT NOT NULL` con validación en capa Domain. Esta corrección es parte de CA-25.38.

### Referencias y Validación

**Código fuente verificado:**
- `PlanAssignmentEntity.kt` — reps: String sin annotation CHECK
- `SessionExerciseEntity.kt` — No tiene campo is_finalized (será agregado)
- `ExerciseSetEntity.kt` — rir: Int sin CHECK constraint
- `SessionRepositoryImpl.kt` — registerSet() con guard en línea 355, status mapping líneas 243-245
- `RegisterSetUseCase.kt` — `require(rir in 0..5)`
- `AlertThresholdRule.kt` — RIR_LOW=1.5, RIR_HIGH=3.5
- `GetFilterOptionsUseCase.kt` — combine(getAllEquipmentTypes, getAllMuscleZones)
- `RegisterSetScreen.kt` — RirSelector `for(rir in 0..5)`
- `BaseDataSeeder.kt` — 15 zonas, 15 equipamientos
- `ExerciseSeeder.kt` — Face Pull `emz(db, 14, 7)` → Hombro
- `strings.xml` — nav_settings="Configuración", deload_protocol_rir="RIR 4–5"
- `SessionDao.kt` — completedExercises con `completedSets >= COALESCE(pa.sets, 4)`

**Historias relacionadas:**

- Historia #3: HU-03 — Seed data original (BaseDataSeeder, ExerciseSeeder, PlanSeeder)
- Historia #5: HU-05 — Sesión activa (SessionExerciseEntity, SessionExerciseDao — infraestructura base que HU-25 extiende)
- Historia #6: HU-06 — Registro de serie (RIR range 0-5 en RegisterSetUseCase, RirSelector — HU-25 cambia a 0-2)
- Historia #10: HU-10 — Evaluación de progresión post-sesión
- Historia #13: HU-13 — Alertas y métricas (AlertThresholdRule, umbrales 1.5/3.5 — HU-25 cambia a 0.5/1.8)
- Historia #16: HU-16 — Primer patrón de migración (MIGRATION_6_7)
- Historia #17: HU-17 — Deload (protocolo de descarga, strings deload_protocol_*)
- Historia #21: HU-21 — MIGRATION_7_8, sort_order en plan_assignment
- Historia #23: HU-23 — MIGRATION_8_9, módulos → rutinas (migración de esquema)
- Historia #24: HU-24 — MIGRATION_9_10, actualización diccionario/plan. Define el estado actual (v10) sobre el que trabaja HU-25

**Validado por:** Esteban Colorado González | **Fecha:** 2026-05-08 | **Enfoque:** Exploratorio

**Métricas de Análisis Arquitectónico:**
- PERT: O=30, M=45, P=70 → E ≈ 47 min
- Inicio: 2026-05-08 22:13 | Fin: 2026-05-08 22:31 | Duración real: 18 minutos
