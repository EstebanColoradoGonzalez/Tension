# Dev Agent Record — HU-23

## Tareas de Implementación

### Fase 1: Migración Room v8→v9 + Entidades (Hito 1)

#### Data — Entidades Nuevas

- [ ] **Crear `RoutineEntity`** (AC: CA-23.01, CA-23.02, CA-23.04, CA-23.05)
  - [ ] `@Entity(tableName = "routine")`: `id: Long` (PK autoincrement), `name: String` (UNIQUE), `sortOrder: Int` (CHECK ≥ 1), `createdAt: String` — Archivo: `data/local/entity/RoutineEntity.kt`

- [ ] **Crear `RoutineVersionEntity`** (AC: CA-23.07, CA-23.09, CA-23.14)
  - [ ] `@Entity(tableName = "routine_version")`: `id: Long` (PK autoincrement), `routineId: Long` (FK→routine ON DELETE CASCADE), `versionNumber: Int` (CHECK ≥ 1). Unique(routineId, versionNumber). Índice en `routineId` — Archivo: `data/local/entity/RoutineVersionEntity.kt`

- [ ] **Crear `RoutineCurrentVersionEntity`** (AC: CA-23.10, CA-23.18)
  - [ ] `@Entity(tableName = "routine_current_version")`: `routineId: Long` (PK, FK→routine ON DELETE CASCADE), `currentVersionNumber: Int` (DEFAULT 1, CHECK ≥ 1) — Archivo: `data/local/entity/RoutineCurrentVersionEntity.kt`

- [ ] **Crear `DeloadFrozenVersionEntity`** (AC: CA-23.26)
  - [ ] PK compuesta `(deloadId, routineId)`, `deloadId: Long` (FK→deload ON DELETE CASCADE), `routineId: Long` (FK→routine ON DELETE RESTRICT), `frozenVersionNumber: Int` (CHECK ≥ 1) — Archivo: `data/local/entity/DeloadFrozenVersionEntity.kt`

#### Data — Entidades Modificadas

- [ ] **Modificar `ExerciseEntity`** (AC: CA-23.15)
  - [ ] Eliminar campo `moduleCode: String` y su `@ForeignKey` a `module`. Eliminar `@Index` en `module_code`. Mantener `@Index` en `equipment_type_id` y unique index `(name, equipment_type_id)` — Archivo: `data/local/entity/ExerciseEntity.kt`

- [ ] **Modificar `SessionEntity`** (AC: CA-23.21)
  - [ ] Renombrar `moduleVersionId` → `routineVersionId`, FK apunta a `routine_version` — Archivo: `data/local/entity/SessionEntity.kt`

- [ ] **Modificar `PlanAssignmentEntity`** (AC: CA-23.12, CA-23.14)
  - [ ] Renombrar `moduleVersionId` → `routineVersionId` en PK compuesta y FK. PK `(exerciseId, routineVersionId)` permite mismo ejercicio en múltiples rutinas/versiones — Archivo: `data/local/entity/PlanAssignmentEntity.kt`

- [ ] **Modificar `AlertEntity`** (AC: CA-23.31)
  - [ ] Cambiar `moduleCode: String?` → `routineId: Long?` con FK→routine ON DELETE CASCADE — Archivo: `data/local/entity/AlertEntity.kt`

- [ ] **Modificar `DeloadEntity`** (AC: CA-23.26)
  - [ ] Eliminar `frozenVersionModuleA`, `frozenVersionModuleB`, `frozenVersionModuleC` — Archivo: `data/local/entity/DeloadEntity.kt`

- [ ] **Modificar `RotationStateEntity`** (AC: CA-23.18)
  - [ ] Eliminar `currentVersionModuleA`, `currentVersionModuleB`, `currentVersionModuleC`. Conservar solo `id`, `microcyclePosition`, `microcycleCount` — Archivo: `data/local/entity/RotationStateEntity.kt`
  - [ ] Nota: `ProfileRepositoryImpl.createProfile()` inserta `RotationStateEntity()` con constructor por defecto — verificar que defaults (microcyclePosition=1, microcycleCount=0) sean correctos para v9

#### Data — Migración SQL

- [ ] **Crear `MIGRATION_8_9`** (AC: CA-23.35, CA-23.36, CA-23.37)
  - [ ] Fase 1 SQL: CREATE TABLE `routine`, `routine_version`, `routine_current_version`, `deload_frozen_version` — Archivo: `data/local/database/Migrations.kt`
  - [ ] Fase 2 SQL: INSERT datos existentes (módulos→rutinas con nombres 'Pull + Abs', 'Push', 'Pierna'; IDs de `routine_version` preservados de `module_version`)
  - [ ] Fase 3 SQL: Recrear `session`, `plan_assignment`, `exercise`, `alert`, `deload`, `rotation_state` con nuevo esquema (patrón CREATE _new → INSERT → DROP → RENAME)
  - [ ] Fase 4 SQL: DROP TABLE `module_version`, DROP TABLE `module`
  - [ ] Crear índices en todas las tablas recreadas

#### Data — Database y Seeders

- [ ] **Modificar `TensionDatabase`** (AC: CA-23.35)
  - [ ] Incrementar version a 9, cambiar `exportSchema = false` → `exportSchema = true` — Archivo: `data/local/database/TensionDatabase.kt`
  - [ ] Actualizar entities array: reemplazar `ModuleEntity`, `ModuleVersionEntity` por `RoutineEntity`, `RoutineVersionEntity`, `RoutineCurrentVersionEntity`, `DeloadFrozenVersionEntity`
  - [ ] Actualizar entities modificadas: `ExerciseEntity`, `SessionEntity`, `PlanAssignmentEntity`, `AlertEntity`, `DeloadEntity`, `RotationStateEntity`
  - [ ] Reemplazar DAOs: `moduleDao()`, `moduleVersionDao()` → `routineDao()`, `routineVersionDao()`, `routineCurrentVersionDao()`, `deloadFrozenVersionDao()`
  - [ ] Agregar `MIGRATION_8_9` al builder `.addMigrations()`
  - [ ] Configurar `room.schemaLocation` en `app/build.gradle.kts` KSP args

- [ ] **Crear `PrepopulateCallbackV9`** (AC: CA-23.06)
  - [ ] Callback para nuevas instalaciones v9+: seeds de `muscle_zone`, `equipment_type`, `exercise` (sin `module_code`). NO invoca `ModuleSeeder` ni `PlanSeeder`. Plan vacío — usuario crea su propio plan — Archivo: `data/local/seed/PrepopulateCallbackV9.kt`
  - [ ] Condicional en `TensionDatabase.Builder`: usar `PrepopulateCallbackV9` para creación nueva, mantener `PrepopulateCallback` original para migraciones

#### Test

- [ ] **Test instrumentado de migración** (AC: CA-23.35, CA-23.36, CA-23.37) — MANUAL
  - [ ] Configurar `MigrationTestHelper` con schema v8
  - [ ] Datos representativos: 3 módulos, 9 versiones, 82 asignaciones, sesiones con/sin deload, alertas activas, ejercicios custom
  - [ ] Verificar: IDs preservados, FKs válidas, datos de deload migrados, alertas con `routineId` correcto

### Fase 2: DAOs Nuevos + Modificados (Hito 2)

#### Data — DAOs Nuevos

- [ ] **Crear `RoutineDao`** (AC: CA-23.01, CA-23.02, CA-23.03, CA-23.04, CA-23.05)
  - [ ] `getAll(): Flow<List<RoutineEntity>>` ORDER BY sort_order — Archivo: `data/local/dao/RoutineDao.kt`
  - [ ] `getById(id: Long): RoutineEntity?`
  - [ ] `insert(routine: RoutineEntity): Long`
  - [ ] `update(routine: RoutineEntity)`
  - [ ] `delete(id: Long)`
  - [ ] `getMaxSortOrder(): Int?`
  - [ ] `countRoutines(): Int`
  - [ ] `updateSortOrder(id: Long, sortOrder: Int)`

- [ ] **Crear `RoutineVersionDao`** (AC: CA-23.07, CA-23.08, CA-23.10, CA-23.11)
  - [ ] `getByRoutineId(routineId: Long): Flow<List<RoutineVersionEntity>>` — Archivo: `data/local/dao/RoutineVersionDao.kt`
  - [ ] `getByRoutineIdAndVersion(routineId: Long, versionNumber: Int): RoutineVersionEntity?`
  - [ ] `insert(version: RoutineVersionEntity): Long`
  - [ ] `delete(id: Long)`
  - [ ] `countByRoutineId(routineId: Long): Int`
  - [ ] `getMaxVersionNumber(routineId: Long): Int?`
  - [ ] `getVersionCountsByRoutine(): List<VersionCountDto>` — `SELECT routine_id, COUNT(*) AS count FROM routine_version GROUP BY routine_id` (evita N+1 en `closeSession`)

- [ ] **Crear `RoutineCurrentVersionDao`** (AC: CA-23.10, CA-23.11, CA-23.18)
  - [ ] `getAll(): Flow<List<RoutineCurrentVersionEntity>>` — Archivo: `data/local/dao/RoutineCurrentVersionDao.kt`
  - [ ] `getAllOnce(): List<RoutineCurrentVersionEntity>` (suspend)
  - [ ] `getByRoutineId(routineId: Long): RoutineCurrentVersionEntity?`
  - [ ] `insert(entity: RoutineCurrentVersionEntity)`
  - [ ] `update(entity: RoutineCurrentVersionEntity)`
  - [ ] `deleteByRoutineId(routineId: Long)`

- [ ] **Crear `DeloadFrozenVersionDao`** (AC: CA-23.26)
  - [ ] `getByDeloadId(deloadId: Long): List<DeloadFrozenVersionEntity>` — Archivo: `data/local/dao/DeloadFrozenVersionDao.kt`
  - [ ] `insertAll(entities: List<DeloadFrozenVersionEntity>)`
  - [ ] `deleteByDeloadId(deloadId: Long)`

#### Data — DAOs Modificados

- [ ] **Modificar `PlanAssignmentDao`** (AC: CA-23.12, CA-23.13)
  - [ ] Todas las queries: `module_version_id` → `routine_version_id` — Archivo: `data/local/dao/PlanAssignmentDao.kt`
  - [ ] `getPreviewByModuleVersionId` → `getPreviewByRoutineVersionId`: eliminar JOIN `module m ON e.module_code = m.code`, eliminar `loadIncrementKg` del DTO (se resuelve en Repository via `LoadIncrementResolver`)
  - [ ] DTO `SessionPreviewExerciseDto`: `loadIncrementKg` → `muscleGroup: String?` (del JOIN existente con `muscle_zone`)
  - [ ] DTO `PlanAssignmentWithExerciseDetails`: eliminar `moduleCode`, agregar `muscleGroup: String?`

- [ ] **Modificar `SessionDao`** (AC: CA-23.20, CA-23.32)
  - [ ] DTOs: `moduleCode: String` → `routineName: String` — Archivo: `data/local/dao/SessionDao.kt`
  - [ ] JOINs: `module_version` + `module` → `routine_version` + `routine`
  - [ ] `getSessionIdsByModuleInRange(moduleCode, limit)` → `getSessionIdsByRoutineInRange(routineId: Long, limit: Int)`
  - [ ] `getLastSessionDateByModule(moduleCode)` → `getLastSessionDateByRoutine(routineId: Long)`

- [ ] **Modificar `ExerciseDao`** (AC: CA-23.15, CA-23.22)
  - [ ] DTO `ExerciseWithDetails`: eliminar `moduleCode`, `moduleName` — Archivo: `data/local/dao/ExerciseDao.kt`
  - [ ] Eliminar `getByModuleCodeNotInVersion` → reemplazar por `getEligibleForVersion(routineVersionId: Long)` que no filtra por módulo
  - [ ] `getEligibleSubstitutesForSession(moduleCode, sessionId)` → `getEligibleSubstitutesForSession(muscleZoneIds: List<Long>, sessionId: Long)`: filtra por zona muscular, excluye ejercicios ya en sesión

- [ ] **Modificar `SessionExerciseDao`** (AC: CA-23.21, CA-23.33)
  - [ ] DTOs con `moduleCode` → `routineId: Long` o eliminarlo — Archivo: `data/local/dao/SessionExerciseDao.kt`
  - [ ] `SessionExerciseWithDetails`: eliminar JOIN `module` para `loadIncrementKg`; agregar `muscleGroup: String?` via JOIN `exercise_muscle_zone` + `muscle_zone`
  - [ ] `SessionExerciseForProgression`: eliminar `moduleCode` y `loadIncrementKg`; agregar `muscleGroup: String?`
  - [ ] `ExerciseHistoryEntryDto`: `moduleCode` → `routineName` via JOIN `routine_version` + `routine`

- [ ] **Modificar `AlertDao`** (AC: CA-23.31)
  - [ ] `existsActiveByModule(moduleCode)` → `existsActiveByRoutine(routineId: Long)` — Archivo: `data/local/dao/AlertDao.kt`
  - [ ] `resolveByModuleAndType(moduleCode)` → `resolveByRoutineAndType(routineId: Long)`

#### Data — DAOs y Entidades Eliminados

- [ ] **Eliminar `ModuleDao`** — Archivo: `data/local/dao/ModuleDao.kt`
- [ ] **Eliminar `ModuleVersionDao`** — Archivo: `data/local/dao/ModuleVersionDao.kt`
- [ ] **Eliminar `ModuleEntity`** — Archivo: `data/local/entity/ModuleEntity.kt`
- [ ] **Eliminar `ModuleVersionEntity`** — Archivo: `data/local/entity/ModuleVersionEntity.kt`

### Fase 3: Reglas de Negocio + Modelos de Dominio (Hito 3)

#### Domain — Reglas de Negocio

- [ ] **Crear `LoadIncrementResolver`** (AC: CA-23.23)
  - [ ] Object con `fun resolve(muscleGroup: String): Double` — tren inferior → 5.0, tren superior → 2.5, fallback → 2.5 — Archivo: `domain/rules/LoadIncrementResolver.kt`
  - [ ] `LOWER_BODY_GROUPS = setOf("Cuádriceps", "Isquiotibiales", "Glúteos", "Aductores", "Abductores", "Gemelos")`
  - [ ] Test unitario — Archivo: `src/test/.../domain/rules/LoadIncrementResolverTest.kt`
    - Caso: "Cuádriceps" → 5.0
    - Caso: "Pecho" → 2.5
    - Caso: "Bíceps" → 2.5
    - Caso: "Gemelos" → 5.0
    - Caso: string no reconocido → 2.5

- [ ] **Reescribir `RotationResolver`** (AC: CA-23.16, CA-23.17, CA-23.19, CA-23.11)
  - [ ] `resolveRoutineId(position: Int, routines: List<Routine>): Long` — Archivo: `domain/model/RotationResolver.kt`
  - [ ] `resolveVersionNumber(routineId: Long, currentVersions: Map<Long, Int>): Int`
  - [ ] `advanceRotation(position: Int, totalRoutines: Int, currentVersions: Map<Long, Int>, versionCounts: Map<Long, Int>, isDeload: Boolean): AdvanceResult`
  - [ ] `data class AdvanceResult(val newPosition: Int, val newMicrocycleCount: Int, val updatedVersions: Map<Long, Int>)`
  - [ ] Wrap-around por rutina: `(current % count) + 1` al completar microciclo
  - [ ] Test unitario — Archivo: `src/test/.../domain/model/RotationResolverTest.kt`
    - N=1 rutina, 1 versión → posición siempre 1, versión siempre 1
    - N=3 rutinas, 3 versiones cada una → rotación clásica
    - N=5 rutinas, versiones heterogéneas (1,2,3,5,10)
    - N=2 rutinas con deload activo → versiones no avanzan
    - Completar microciclo → incrementar `microcycleCount`

- [ ] **Eliminar `MUSCLE_GROUPS_BY_MODULE` de `AlertThresholdRule`** (AC: CA-23.24, CA-23.27, CA-23.31)
  - [ ] Eliminar mapa hardcoded — Archivo: `domain/rules/AlertThresholdRule.kt`
  - [ ] `CorrectiveActionRule.ROTATE_VERSION` opera con `routineId` (CA-23.27)
  - [ ] Modificar test — Archivo: `src/test/.../domain/rules/AlertThresholdRuleTest.kt`

#### Domain — Modelos Nuevos

- [ ] **Crear `Routine`** — `data class Routine(val id: Long, val name: String, val sortOrder: Int, val createdAt: String)` — Archivo: `domain/model/Routine.kt`
- [ ] **Crear `RoutineVersion`** — `data class RoutineVersion(val id: Long, val routineId: Long, val versionNumber: Int)` — Archivo: `domain/model/RoutineVersion.kt`

#### Domain — Modelos Modificados (15+ data classes)

- [ ] **Modificar `NextSession`** (AC: CA-23.20) — `moduleCode` → `routineId: Long` + `routineName: String`, `moduleVersionId` → `routineVersionId` — Archivo: `domain/model/NextSession.kt`
- [ ] **Modificar `ActiveSession`** (AC: CA-23.21) — `moduleCode` → `routineName: String` — Archivo: `domain/model/ActiveSession.kt`
- [ ] **Modificar `RotationState`** (AC: CA-23.18) — Eliminar `currentVersionModuleA/B/C`. Solo: `microcyclePosition: Int`, `microcycleCount: Int` — Archivo: `domain/model/RotationState.kt`
- [ ] **Modificar `Deload`** (AC: CA-23.26) — Eliminar `frozenVersionModuleA/B/C`. Agregar `frozenVersions: Map<Long, Int>` — Archivo: `domain/model/Deload.kt`
- [ ] **Modificar `DeloadState`** (AC: CA-23.25, CA-23.26) — `DeloadActive`: `frozenVersionA/B/C` → `frozenVersions: Map<Long, Int>`. `DeloadRequired`: `modules: List<String>` → `routineIds: List<Long>` — Archivo: `domain/model/DeloadState.kt`
- [ ] **Modificar `Exercise`** (AC: CA-23.15) — Eliminar `moduleCode: String` — Archivo: `domain/model/Exercise.kt`
- [ ] **Modificar `SubstituteExerciseInfo`** (AC: CA-23.22) — `moduleCode: String` → `muscleZoneIds: List<Long>` — Archivo: `domain/model/SubstituteExerciseInfo.kt`
- [ ] **Modificar `SessionPreviewExercise`** (AC: CA-23.20) — Eliminar `moduleCode`, `loadIncrementKg`. Agregar `muscleGroup: String?` — Archivo: `domain/model/SessionPreviewExercise.kt`
- [ ] **Modificar `SessionHistoryItem`** (AC: CA-23.32) — `moduleCode` → `routineName` — Archivo: `domain/model/SessionHistoryItem.kt`
- [ ] **Modificar `SessionDetail`** (AC: CA-23.32) — `moduleCode` → `routineName` — Archivo: `domain/model/SessionDetail.kt`
- [ ] **Modificar `PlanVersionDetail`** — `moduleCode` → `routineName` — Archivo: `domain/model/PlanVersionDetail.kt`
- [ ] **Modificar `DeloadHomeState`** (AC: CA-23.25) — `Active/Required`: `moduleCode` → `routineId`/`routineName` — Archivo: `domain/model/DeloadHomeState.kt`
- [ ] **Modificar `ExerciseHistoryEntry`** (AC: CA-23.33) — `moduleCode` → `routineName` — Archivo: `domain/model/ExerciseHistoryEntry.kt`
- [ ] **Modificar `SessionSummary`** — `moduleCode` → `routineName` — Archivo: `domain/model/SessionSummary.kt`
- [ ] **Renombrar `RirByModule` → `RirByRoutine`** — `moduleCode: String` → `routineId: Long` + `routineName: String` — Archivo: `domain/model/RirByRoutine.kt`
- [ ] **Modificar `SetDistributionData`** — `moduleCode` → `routineId: Long` — Archivo: `domain/model/SetDistributionData.kt`
- [ ] **Modificar `AlertTriggerData`** — `RirTrigger.moduleCode` → `routineId: Long`. `InactivityTrigger.moduleCode` → `routineId: Long` — Archivo: `domain/model/AlertTriggerData.kt`
- [ ] **Renombrar `ModuleInactivityData` → `RoutineInactivityData`** — `moduleCode` → `routineId: Long`, `moduleName` → `routineName: String` — Archivo: `domain/model/RoutineInactivityData.kt`

### Fase 4: Repositories (Hito 4)

#### Domain — RoutineRepository (Nuevo)

- [ ] **Crear interfaz `RoutineRepository`** (AC: CA-23.01 a CA-23.13)
  - [ ] `fun getRoutines(): Flow<List<Routine>>` — Archivo: `domain/repository/RoutineRepository.kt`
  - [ ] `suspend fun getRoutineById(id: Long): Routine?`
  - [ ] `suspend fun createRoutine(name: String): Long`
  - [ ] `suspend fun updateRoutineName(id: Long, name: String)`
  - [ ] `suspend fun deleteRoutine(id: Long)`
  - [ ] `suspend fun reorderRoutines(orderedIds: List<Long>)`
  - [ ] `fun getVersionsByRoutine(routineId: Long): Flow<List<RoutineVersion>>`
  - [ ] `suspend fun createVersion(routineId: Long): Long`
  - [ ] `suspend fun deleteVersion(versionId: Long)`
  - [ ] `suspend fun countRoutines(): Int`
  - [ ] `suspend fun hasActiveSessionForRoutine(routineId: Long): Boolean`

- [ ] **Crear `RoutineRepositoryImpl`** (AC: CA-23.01 a CA-23.13)
  - [ ] Inyectar: `routineDao`, `routineVersionDao`, `routineCurrentVersionDao`, `planAssignmentDao`, `sessionDao` — Archivo: `data/repository/RoutineRepositoryImpl.kt`
  - [ ] `createRoutine()`: validar nombre (no vacío, ≤50 chars, UNIQUE). Insertar rutina + V1 automática + `routine_current_version` con valor 1
  - [ ] `deleteRoutine()`: validar `countRoutines() > 1`, no sesión activa. Reordenar `sort_order` de restantes
  - [ ] `reorderRoutines()`: actualizar `sort_order` de cada rutina. Recalibrar `microcyclePosition` si necesario

#### Data — SessionRepositoryImpl (Rewrite parcial)

- [ ] **Modificar constructor de `SessionRepositoryImpl`** — Archivo: `data/repository/SessionRepositoryImpl.kt`
  - [ ] Quitar: `moduleDao: ModuleDao`, `moduleVersionDao: ModuleVersionDao`
  - [ ] Agregar: `routineDao: RoutineDao`, `routineVersionDao: RoutineVersionDao`, `routineCurrentVersionDao: RoutineCurrentVersionDao`, `deloadFrozenVersionDao: DeloadFrozenVersionDao`

- [ ] **`getNextModuleVersionId()` → `getNextRoutineVersionId()`** (AC: CA-23.20)
  - [ ] `routineDao.getAll()` → `RotationResolver.resolveRoutineId()` → `routineCurrentVersionDao.getByRoutineId()` → `routineVersionDao.getByRoutineIdAndVersion()` → ID

- [ ] **`closeSession()` — advanceRotation con N rutinas** (AC: CA-23.19, CA-23.11, CA-23.30)
  - [ ] `routineCurrentVersionDao.getAllOnce()` para versiones actuales
  - [ ] `routineVersionDao.getVersionCountsByRoutine()` para total (sin N+1)
  - [ ] `RotationResolver.advanceRotation()` con parámetros dinámicos
  - [ ] Si microciclo completó: actualizar `routine_current_version` por rutina

- [ ] **`activateDeload()` — escribir en `deload_frozen_version`** (AC: CA-23.26)
  - [ ] Copiar versiones actuales de `routine_current_version` a `deload_frozen_version`

- [ ] **Deload finalization** (AC: CA-23.26)
  - [ ] `countDeloadSessions(deloadId) == routineDao.countRoutines()` (era `== 6`)

- [ ] **`evaluateRirOutOfRange()` — iterar rutinas dinámicas** (AC: CA-23.24, CA-23.28)
  - [ ] Reemplazar `listOf("A", "B", "C")` por `routineDao.getAll().map { it.id }`
  - [ ] `getSessionIdsByRoutineInRange(routineId)` — query batch `getSessionIdsByRoutinesInRange(routineIds: List<Long>)` para evitar N+1

- [ ] **`evaluateModuleInactivity()` → `evaluateRoutineInactivity()`** (AC: CA-23.25, CA-23.31)
  - [ ] Iterar `routineDao.getAll()` en vez de `listOf("A", "B", "C")`
  - [ ] Zonas musculares dinámicas via query `plan_assignment` + `exercise_muscle_zone` + `muscle_zone` para la rutina
  - [ ] Query batch para obtener zonas de todas las rutinas en una consulta (evitar N+1)

- [ ] **`getSessionPreviewExercises()` — LoadIncrementResolver** (AC: CA-23.20)
  - [ ] `loadIncrementKg` se resuelve con `LoadIncrementResolver.resolve(muscleGroup)` en el mapper

- [ ] **Mapper global de `moduleCode` → `routineName`/`routineId`** — Actualizar todos los mappers de DTOs en los ~15 métodos restantes

#### Data — Otros Repositories

- [ ] **Modificar `AlertRepositoryImpl`** (AC: CA-23.24, CA-23.25, CA-23.31)
  - [ ] `moduleCode` → `routineId: Long` en ~10 puntos — Archivo: `data/repository/AlertRepositoryImpl.kt`
  - [ ] `MUSCLE_GROUPS_BY_MODULE[moduleCode]` → query dinámico de zonas musculares por rutina
  - [ ] Mensajes: "Módulo ${moduleCode}" → "${routineName}"

- [ ] **Modificar `MetricsRepositoryImpl`** (AC: CA-23.28, CA-23.29)
  - [ ] `getRirValuesByModule(moduleCode: String)` → `getRirValuesByRoutine(routineId: Long)` — Archivo: `data/repository/MetricsRepositoryImpl.kt`

- [ ] **Modificar `ExerciseRepositoryImpl`** (AC: CA-23.15, CA-23.22)
  - [ ] Eliminar `moduleCode` como parámetro — Archivo: `data/repository/ExerciseRepositoryImpl.kt`
  - [ ] `getEligibleSubstitutes(moduleCode, sessionId)` → `getEligibleSubstitutes(muscleZoneIds: List<Long>, sessionId: Long)`

- [ ] **Modificar `PlanRepositoryImpl`**
  - [ ] `moduleVersionId` → `routineVersionId` — Archivo: `data/repository/PlanRepositoryImpl.kt`
  - [ ] Eliminar agrupación por `moduleCode`
  - [ ] `getAvailableExercisesForVersion(moduleCode, moduleVersionId)` → `getAvailableExercisesForVersion(routineVersionId: Long)`

#### Domain — Interfaces de Repository

- [ ] **Modificar `SessionRepository`** — `getNextModuleVersionId()` → `getNextRoutineVersionId()`. `getSessionPreviewExercises(moduleVersionId)` → `getSessionPreviewExercises(routineVersionId)` — Archivo: `domain/repository/SessionRepository.kt`
- [ ] **Modificar `PlanRepository`** — `getAvailableExercisesForVersion(moduleCode, moduleVersionId)` → `getAvailableExercisesForVersion(routineVersionId: Long)` — Archivo: `domain/repository/PlanRepository.kt`
- [ ] **Modificar `ExerciseRepository`** — Firmas sin `moduleCode` — Archivo: `domain/repository/ExerciseRepository.kt`
- [ ] **Modificar `MetricsRepository`** — `moduleCode: String` → `routineId: Long` — Archivo: `domain/repository/MetricsRepository.kt`

#### DI

- [ ] **Agregar bindings para `RoutineRepository`** — `@Binds fun bindRoutineRepository(impl: RoutineRepositoryImpl): RoutineRepository` — Archivo: `di/RepositoryModule.kt`
- [ ] **Exponer nuevos DAOs desde `DatabaseModule`**: `routineDao()`, `routineVersionDao()`, `routineCurrentVersionDao()`, `deloadFrozenVersionDao()`

### Fase 5: UseCases de Gestión de Rutinas (Hito 5)

> Dependencia: Fase 4 (RoutineRepository interfaz + impl + bindings Hilt) debe estar completada.

- [ ] **Crear `CreateRoutineUseCase`** (AC: CA-23.01, CA-23.06) — Valida nombre (max 50 chars, no vacío, no duplicado). Delega a `routineRepository.createRoutine(name)` — Archivo: `domain/usecase/routine/CreateRoutineUseCase.kt`
  - [ ] Test unitario — Archivo: `src/test/.../domain/usecase/routine/CreateRoutineUseCaseTest.kt`

- [ ] **Crear `UpdateRoutineNameUseCase`** (AC: CA-23.02) — Valida nombre, delega a `routineRepository.updateRoutineName(id, name)` — Archivo: `domain/usecase/routine/UpdateRoutineNameUseCase.kt`
  - [ ] Test unitario — Archivo: `src/test/.../domain/usecase/routine/UpdateRoutineNameUseCaseTest.kt`

- [ ] **Crear `DeleteRoutineUseCase`** (AC: CA-23.03) — Valida `countRoutines() > 1`, no sesión activa — Archivo: `domain/usecase/routine/DeleteRoutineUseCase.kt`
  - [ ] Test unitario — Archivo: `src/test/.../domain/usecase/routine/DeleteRoutineUseCaseTest.kt`

- [ ] **Crear `ReorderRoutinesUseCase`** (AC: CA-23.04) — Recibe lista ordenada de IDs, delega a `routineRepository.reorderRoutines(orderedIds)` — Archivo: `domain/usecase/routine/ReorderRoutinesUseCase.kt`
  - [ ] Test unitario — Archivo: `src/test/.../domain/usecase/routine/ReorderRoutinesUseCaseTest.kt`

- [ ] **Crear `CreateRoutineVersionUseCase`** (AC: CA-23.07) — Delega a `routineRepository.createVersion(routineId)` — Archivo: `domain/usecase/routine/CreateRoutineVersionUseCase.kt`
  - [ ] Test unitario — Archivo: `src/test/.../domain/usecase/routine/CreateRoutineVersionUseCaseTest.kt`

- [ ] **Crear `DeleteRoutineVersionUseCase`** (AC: CA-23.08) — Valida `countByRoutineId() > 1` — Archivo: `domain/usecase/routine/DeleteRoutineVersionUseCase.kt`
  - [ ] Test unitario — Archivo: `src/test/.../domain/usecase/routine/DeleteRoutineVersionUseCaseTest.kt`

### Fase 6: UseCases Existentes Modificados (Hito 6)

- [ ] **Modificar `GetNextSessionInfoUseCase`** (AC: CA-23.16, CA-23.20) — Nuevo `RotationResolver` + `RoutineCurrentVersionDao`. Retorna `NextSession` con `routineId`, `routineName`, `routineVersionId` — Archivo: `domain/usecase/session/GetNextSessionInfoUseCase.kt`
  - [ ] Test: modificar — Archivo: `src/test/.../domain/usecase/session/GetNextSessionInfoUseCaseTest.kt`

- [ ] **Modificar `StartSessionUseCase`** (AC: CA-23.20) — `moduleVersionId` → `routineVersionId` — Archivo: `domain/usecase/session/StartSessionUseCase.kt`
  - [ ] Test: modificar

- [ ] **Modificar `GetSessionPreviewUseCase`** (AC: CA-23.20) — `moduleVersionId` → `routineVersionId` — Archivo: `domain/usecase/session/GetSessionPreviewUseCase.kt`
  - [ ] Test: modificar

- [ ] **Modificar `SubstituteExerciseUseCase`** (AC: CA-23.22) — Filtro por zona muscular — Archivo: `domain/usecase/session/SubstituteExerciseUseCase.kt`
  - [ ] Test: modificar

- [ ] **Modificar `GetPlanVersionDetailUseCase`** — `moduleVersionId` → `routineVersionId` — Archivo: `domain/usecase/plan/GetPlanVersionDetailUseCase.kt`
- [ ] **Modificar `AssignExerciseToVersionUseCase`** (AC: CA-23.12) — `moduleVersionId` → `routineVersionId` — Archivo: `domain/usecase/plan/AssignExerciseToVersionUseCase.kt`
- [ ] **Modificar `UnassignExerciseFromVersionUseCase`** (AC: CA-23.13) — `moduleVersionId` → `routineVersionId` — Archivo: `domain/usecase/plan/UnassignExerciseFromVersionUseCase.kt`
- [ ] **Modificar `GetSessionSummaryUseCase`** (AC: CA-23.32) — `moduleCode` → `routineName` — Archivo: `domain/usecase/session/GetSessionSummaryUseCase.kt`
- [ ] **Modificar `ExportBackupUseCase`** (AC: CA-23.34) — Esquema con tablas `routine`/`routine_version`/`routine_current_version`/`deload_frozen_version` — Archivo: `domain/usecase/backup/ExportBackupUseCase.kt`
  - [ ] Test: modificar
- [ ] **Modificar `ImportBackupUseCase`** (AC: CA-23.34) — Detectar versión backup (v8 vs v9). Transformar v8→v9 en importación — Archivo: `domain/usecase/backup/ImportBackupUseCase.kt`
  - [ ] Test: modificar
- [ ] **Modificar `ValidateBackupUseCase`** (AC: CA-23.34) — Validar esquema v9 (tablas routine) — Archivo: `domain/usecase/backup/ValidateBackupUseCase.kt`
  - [ ] Test: modificar

### Fase 7: UI — Gestión del Plan (Hito 7)

#### UI — Pantallas Nuevas (`ui/catalog/`)

- [ ] **Crear `RoutineListUiState`** (AC: CA-23.01, CA-23.04, CA-23.05)
  - [ ] `isLoading: Boolean`, `routines: List<RoutineItem>`, `showCreateDialog: Boolean`, `createDialogName: String`, `deleteTarget: RoutineItem?` — Archivo: `ui/catalog/RoutineListUiState.kt`
  - [ ] `data class RoutineItem(val id: Long, val name: String, val sortOrder: Int, val versionCount: Int)`

- [ ] **Crear `RoutineListViewModel`** (AC: CA-23.01, CA-23.02, CA-23.03, CA-23.04)
  - [ ] `@HiltViewModel` con `CreateRoutineUseCase`, `UpdateRoutineNameUseCase`, `DeleteRoutineUseCase`, `ReorderRoutinesUseCase`, `RoutineRepository` — Archivo: `ui/catalog/RoutineListViewModel.kt`
  - [ ] Acciones: `createRoutine(name)`, `updateName(id, name)`, `deleteRoutine(id)`, `reorderRoutines(orderedIds)`

- [ ] **Crear `RoutineListScreen`** (AC: CA-23.01, CA-23.02, CA-23.03, CA-23.04)
  - [ ] `CenterAlignedTopAppBar` con título "Mi Plan" — Archivo: `ui/catalog/RoutineListScreen.kt`
  - [ ] `LazyColumn` con rutinas en sort_order. Cada item: nombre, conteo de versiones, acciones (editar, eliminar)
  - [ ] Drag-and-drop para reordenar (CA-23.04)
  - [ ] FAB "Crear rutina" → dialog con input de nombre (max 50 chars)
  - [ ] Dialog de confirmación para eliminar (CA-23.03)
  - [ ] Tap rutina → navega a versiones. Bottom Nav visible

- [ ] **Crear `RoutineVersionListUiState`** (AC: CA-23.07, CA-23.08)
  - [ ] `isLoading: Boolean`, `routineName: String`, `versions: List<VersionItem>`, `deleteTarget: VersionItem?` — Archivo: `ui/catalog/RoutineVersionListUiState.kt`
  - [ ] `data class VersionItem(val id: Long, val versionNumber: Int, val exerciseCount: Int)`

- [ ] **Crear `RoutineVersionListViewModel`** (AC: CA-23.07, CA-23.08)
  - [ ] `@HiltViewModel` con `CreateRoutineVersionUseCase`, `DeleteRoutineVersionUseCase`, `RoutineRepository`, `SavedStateHandle` — Archivo: `ui/catalog/RoutineVersionListViewModel.kt`

- [ ] **Crear `RoutineVersionListScreen`** (AC: CA-23.07, CA-23.08)
  - [ ] `CenterAlignedTopAppBar` con `navigationIcon: ArrowBack`, título: nombre de rutina — Archivo: `ui/catalog/RoutineVersionListScreen.kt`
  - [ ] `LazyColumn` con versiones, conteo de ejercicios. FAB "Crear versión". Botón eliminar versión con confirmación (solo si >1 versión)
  - [ ] Tap versión → navega a `PlanVersionDetailScreen(routineVersionId)`

#### UI — Navegación

- [ ] **Agregar rutas en `NavigationRoutes`** (AC: CA-23.01, CA-23.07)
  - [ ] `ROUTINE_LIST = "routine-list"` — Archivo: `ui/navigation/NavigationRoutes.kt`
  - [ ] `ROUTINE_VERSIONS = "routine-versions/{routineId}"` + fun helper
  - [ ] Modificar rutas existentes: `moduleVersionId` → `routineVersionId`, `moduleCode` → `routineId` en params de sesión/preview

- [ ] **Agregar composables en `TensionNavHost`** (AC: CA-23.01, CA-23.07)
  - [ ] Composable para `RoutineListScreen` — Archivo: `ui/navigation/TensionNavHost.kt`
  - [ ] Composable para `RoutineVersionListScreen` con argumento `routineId: Long`
  - [ ] Bottom Nav visible para ambas pantallas

- [ ] **Modificar `BottomNavigationBar`**
  - [ ] Reemplazar tab o agregar tab "Plan" que navega a `ROUTINE_LIST` — Archivo: `ui/components/BottomNavigationBar.kt`
  - [ ] Alternativa: "Diccionario" y "Plan" como tabs horizontales dentro de `RoutineListScreen`

#### UI — PlanVersionDetailScreen

- [ ] **Modificar `PlanVersionDetailViewModel`** — `moduleVersionId` → `routineVersionId` en `SavedStateHandle` y UseCases — Archivo: `ui/catalog/PlanVersionDetailViewModel.kt`
- [ ] **Modificar `PlanVersionDetailScreen`** — Adaptar parámetros de navegación — Archivo: `ui/catalog/PlanVersionDetailScreen.kt`

#### Recursos

- [ ] **Agregar strings para gestión de rutinas** (AC: CA-23.01 a CA-23.08) — "Mi Plan", "Crear rutina", "Nombre de la rutina", "Eliminar rutina", "Crear versión", "Eliminar versión", confirmaciones — Archivo: `res/values/strings.xml`

### Fase 8: UI — Pantallas Existentes Modificadas (Hito 8)

#### UI — Home

- [ ] **Modificar `HomeUiState`** (AC: CA-23.06, CA-23.20) — `NextSession` con `routineName` en vez de `moduleCode`. Agregar `hasPlan: Boolean` para empty state — Archivo: `ui/home/HomeUiState.kt`
- [ ] **Modificar `HomeViewModel`** (AC: CA-23.06, CA-23.20) — `startSession(routineVersionId)`. Empty state si `routineRepository.countRoutines() == 0` — Archivo: `ui/home/HomeViewModel.kt`
- [ ] **Modificar `HomeScreen`** (AC: CA-23.06) — NextSessionCard: mostrar `routineName`. Si `!hasPlan`: empty state con CTA "Crear tu primer plan" → navega a `RoutineListScreen` — Archivo: `ui/home/HomeScreen.kt`

#### UI — Sesión Activa

- [ ] **Modificar `ActiveSessionUiState`** (AC: CA-23.21) — `moduleCode` → `routineName` — Archivo: `ui/session/ActiveSessionUiState.kt`
- [ ] **Modificar `ActiveSessionViewModel`** (AC: CA-23.21) — Header con `routineName`. `LoadDisplayMapper` recibe `LoadIncrementResolver.resolve(muscleGroup)` — Archivo: `ui/session/ActiveSessionViewModel.kt`
- [ ] **Modificar `SessionPreviewViewModel`** (AC: CA-23.20) — `moduleCode` → `routineName`. `loadIncrementKg` via `LoadIncrementResolver` — Archivo: `ui/preview/SessionPreviewViewModel.kt`
- [ ] **Modificar `SubstituteExerciseViewModel`** (AC: CA-23.22) — Filtrar sustitutos por zona muscular del ejercicio actual (no por módulo) — Archivo: `ui/session/SubstituteExerciseViewModel.kt`
  - [ ] Obtener `muscleZoneIds` del ejercicio, pasar a `exerciseRepository.getEligibleSubstitutes(muscleZoneIds, sessionId)`
  - [ ] Test: modificar — Archivo: `src/test/.../ui/session/SubstituteExerciseViewModelTest.kt`

#### UI — Historial

- [ ] **Modificar `SessionHistoryScreen`** (AC: CA-23.32, CA-23.33) — `moduleCode` → `routineName` — Archivo: `ui/history/SessionHistoryScreen.kt`
- [ ] **Modificar `SessionDetailScreen`** — `moduleCode` → `routineName` — Archivo: `ui/history/SessionDetailScreen.kt`
- [ ] **Modificar `ExerciseHistoryScreen`** — `moduleCode` → `routineName` — Archivo: `ui/history/ExerciseHistoryScreen.kt`
- [ ] Tests: modificar `SessionHistoryViewModelTest.kt`, `SessionDetailViewModelTest.kt`, `ExerciseHistoryViewModelTest.kt`

#### UI — Catálogo

- [ ] **Modificar `ExerciseDictionaryScreen`** (AC: CA-23.15) — Eliminar `moduleCode`/`moduleName` de display. Eliminar filtro por módulo — Archivo: `ui/catalog/ExerciseDictionaryScreen.kt`
- [ ] **Modificar `ExerciseDetailScreen`/`ExerciseDetailViewModel`/`ExerciseDetailUiState`** (AC: CA-23.15) — Eliminar `moduleCode` y `moduleName` del `ExerciseDetailItem` y del display — Archivos: `ui/catalog/ExerciseDetailUiState.kt`, `ExerciseDetailViewModel.kt`, `ExerciseDetailScreen.kt`
- [ ] **Modificar `CreateExerciseScreen`/`CreateExerciseViewModel`/`CreateExerciseUiState`** (AC: CA-23.15)
  - [ ] Eliminar selector de módulo (dropdown A/B/C) — Archivo: `ui/catalog/CreateExerciseScreen.kt`
  - [ ] Eliminar `selectedModuleCode`, `modules`, `onModuleSelected()` y validación de módulo — Archivo: `ui/catalog/CreateExerciseViewModel.kt`
  - [ ] Eliminar campos `selectedModuleCode` y `modules` — Archivo: `ui/catalog/CreateExerciseUiState.kt`
  - [ ] Actualizar `CreateExerciseUseCase` para no recibir `moduleCode` — Archivo: `domain/usecase/catalog/CreateExerciseUseCase.kt`

- [ ] **Eliminar `TrainingPlanScreen`/`TrainingPlanViewModel`/`TrainingPlanUiState`** (reemplazados por `RoutineListScreen`)
  - [ ] Eliminar `ui/catalog/TrainingPlanScreen.kt`, `TrainingPlanViewModel.kt`, `TrainingPlanUiState.kt`
  - [ ] Eliminar ruta `TRAINING_PLAN` de `NavigationRoutes.kt`. Eliminar composable de `TensionNavHost.kt`
  - [ ] Actualizar tests: `src/test/.../ui/catalog/TrainingPlanViewModelTest.kt`

#### UI — Alertas, Deload, Métricas, Backup

- [ ] **Modificar `AlertCenterScreen`/`AlertDetailScreen`** (AC: CA-23.31) — `moduleCode` → `routineName` en display — Archivos: `ui/alerts/AlertCenterScreen.kt`, `ui/alerts/AlertDetailScreen.kt`
  - [ ] Tests: modificar `AlertCenterViewModelTest.kt`, `AlertDetailViewModelTest.kt`
- [ ] **Modificar `DeloadManagementScreen`/ViewModel** (AC: CA-23.25, CA-23.26) — Adaptar `DeloadState`/`DeloadHomeState` a Map dinámico — Archivos: `ui/deload/DeloadManagementScreen.kt`, `ui/deload/DeloadManagementViewModel.kt`
- [ ] **Modificar `MetricsScreen`/ViewModel** (AC: CA-23.28, CA-23.29) — `moduleCode` → `routineId` — Archivos: `ui/metrics/MetricsScreen.kt`, `ui/metrics/MetricsViewModel.kt`
- [ ] **Modificar ViewModels de backup** (AC: CA-23.34) — Adaptar a nuevos UseCases de export/import — Archivos: `ui/settings/ExportBackupViewModel.kt`, `ui/settings/ImportBackupViewModel.kt`
  - [ ] Tests: modificar `ExportBackupViewModelTest.kt`, `ImportBackupViewModelTest.kt`

#### UI — Navegación Global

- [ ] **Modificar `NavigationRoutes`** — Parámetros: `moduleVersionId` → `routineVersionId`, `moduleCode` → `routineId` en rutas existentes — Archivo: `ui/navigation/NavigationRoutes.kt`
- [ ] **Modificar `TensionNavHost`** — Composables existentes con nuevos parámetros. `onNavigateToRoutineList` en HomeScreen. `onNavigateToRoutineVersions` en RoutineListScreen — Archivo: `ui/navigation/TensionNavHost.kt`

### Fase N: QA y Deployment

#### Code Quality

- [ ] **Ejecutar Agente Peer Review** (MANUAL)
- [ ] **Resolver incidentes del Peer Review** (MANUAL, condicional)

#### Deployment DEV

- [ ] **Crear Pull Request** (MANUAL)
- [ ] **Ejecutar pipeline deployment DEV** (MANUAL)

#### Testing Manual

- [ ] **Diseñar set de pruebas manuales** (MANUAL)
  - Migración: Instalar app v8 con datos. Actualizar a v9. Verificar que todo funciona idénticamente (CA-23.35, CA-23.36)
  - Gestión de rutinas: Crear (CA-23.01), editar nombre (CA-23.02), eliminar con confirmación (CA-23.03), reordenar (CA-23.04)
  - Gestión de versiones: Crear (CA-23.07), eliminar con confirmación (CA-23.08)
  - Asignación: Asignar ejercicio a versión (CA-23.12), desasignar (CA-23.13), mismo ejercicio en múltiples versiones (CA-23.14)
  - Rotación: Verificar rotación con N rutinas (CA-23.16, CA-23.17, CA-23.19)
  - Sesión: Iniciar sesión con rutina custom, verificar prescripción correcta (CA-23.20, CA-23.21)
  - Sustitución: Verificar que sustitutos se filtran por zona muscular (CA-23.22)
  - Deload: Activar deload con rutinas custom, verificar duración = N sesiones (CA-23.26)
  - Progresión: Incremento por zona muscular (CA-23.23), detección fatiga por rutina (CA-23.24, CA-23.25)
  - Historial: Nombre de rutina en sesiones pasadas (CA-23.32), datos previos preservados (CA-23.34)
  - Backup: Exportar v9, importar v9. Exportar con app v8, importar en v9 (CA-23.34)
  - Empty state: Nueva instalación v9 → plan vacío, CTA para crear plan (CA-23.06)
- [ ] **Ejecutar pruebas manuales** (MANUAL)
