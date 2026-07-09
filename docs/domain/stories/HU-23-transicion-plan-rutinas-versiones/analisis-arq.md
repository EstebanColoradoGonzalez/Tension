# Análisis Arquitectónico — HU-23

<!-- ============================================================================ -->
<!-- SECCIÓN AGREGADA POR: Workflow analizar-disenar-historia-usuario            -->
<!-- ETAPA: Análisis Arquitectónico                                              -->
<!-- RESPONSABLE: Arquitecto                                                     -->
<!-- FECHA: 2026-05-06                                                           -->
<!-- ESTADO: Refinado (Developer)                                               -->
<!-- ============================================================================ -->

## Decisiones de Diseño

**Patrón Arquitectónico:** MVVM + Domain Layer (4 capas), transformación completa del modelo de dominio — de "módulo fijo" (A, B, C hardcoded) a "rutina dinámica" (entidad definida por el usuario con N rutinas y M versiones por rutina).

**Justificación:** La arquitectura actual (ADR-05) define la separación UI → ViewModel → UseCase → Repository → DAO → Room. HU-23 no introduce un patrón arquitectónico nuevo — la misma separación de 4 capas se mantiene. Lo que cambia es el **modelo de dominio subyacente**: la entidad `module` (3 valores fijos A/B/C, PK tipo String) se reemplaza por `routine` (N valores dinámicos, PK autoincrement Long). Esto propaga cambios a todas las capas porque `module_code` es una FK referenciada por 5 tablas y hardcodeada en 5 clases de lógica de negocio (`RotationResolver`, `SessionRepositoryImpl`, `AlertRepositoryImpl`, `AlertThresholdRule`, evaluaciones de KPIs). La migración Room v8→v9 transforma datos existentes sin pérdida (CA-23.35, CA-23.36, CA-23.37).

---

## Bloque A — Migración Room v8→v9 (MIGRATION_8_9)

- **`MIGRATION_8_9` en `Migrations.kt` (Nuevo):** Migración SQL pura ejecutada en 4 fases transaccionales.
  - Nivel de cambio: Crítico

### Fase 1 — Crear nuevas tablas:

```sql
CREATE TABLE routine (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    sort_order INTEGER NOT NULL CHECK(sort_order >= 1),
    created_at TEXT NOT NULL
);

CREATE TABLE routine_version (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    routine_id INTEGER NOT NULL REFERENCES routine(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL CHECK(version_number >= 1),
    UNIQUE(routine_id, version_number)
);
CREATE INDEX idx_routine_version_routine_id ON routine_version(routine_id);

CREATE TABLE routine_current_version (
    routine_id INTEGER PRIMARY KEY REFERENCES routine(id) ON DELETE CASCADE,
    current_version_number INTEGER NOT NULL DEFAULT 1 CHECK(current_version_number >= 1)
);

CREATE TABLE deload_frozen_version (
    deload_id INTEGER NOT NULL REFERENCES deload(id) ON DELETE CASCADE,
    routine_id INTEGER NOT NULL REFERENCES routine(id) ON DELETE RESTRICT,
    frozen_version_number INTEGER NOT NULL CHECK(frozen_version_number >= 1),
    PRIMARY KEY(deload_id, routine_id)
);
```

### Fase 2 — Poblar desde datos existentes (CA-23.35):

```sql
-- Módulos → Rutinas (preservar semántica)
INSERT INTO routine (id, name, sort_order, created_at)
VALUES (1, 'Pull + Abs', 1, date('now')),
       (2, 'Push', 2, date('now')),
       (3, 'Pierna', 3, date('now'));

-- module_version → routine_version (preservar IDs exactos para mantener FKs)
INSERT INTO routine_version (id, routine_id, version_number)
SELECT mv.id,
       CASE mv.module_code WHEN 'A' THEN 1 WHEN 'B' THEN 2 WHEN 'C' THEN 3 END,
       mv.version_number
FROM module_version mv;

-- rotation_state → routine_current_version
INSERT INTO routine_current_version (routine_id, current_version_number)
SELECT 1, current_version_module_a FROM rotation_state WHERE id = 1
UNION ALL
SELECT 2, current_version_module_b FROM rotation_state WHERE id = 1
UNION ALL
SELECT 3, current_version_module_c FROM rotation_state WHERE id = 1;

-- deload → deload_frozen_version (si hay deloads)
INSERT INTO deload_frozen_version (deload_id, routine_id, frozen_version_number)
SELECT id, 1, frozen_version_module_a FROM deload
UNION ALL
SELECT id, 2, frozen_version_module_b FROM deload
UNION ALL
SELECT id, 3, frozen_version_module_c FROM deload;
```

### Fase 3 — Migrar tablas con FKs (recrear con nuevo esquema):

```sql
-- session: module_version_id → routine_version_id (IDs iguales, solo rename)
CREATE TABLE session_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    routine_version_id INTEGER NOT NULL REFERENCES routine_version(id) ON DELETE RESTRICT,
    deload_id INTEGER REFERENCES deload(id) ON DELETE RESTRICT,
    date TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'IN_PROGRESS'
        CHECK(status IN ('IN_PROGRESS', 'COMPLETED', 'INCOMPLETE'))
);
INSERT INTO session_new SELECT id, module_version_id, deload_id, date, status FROM session;
DROP TABLE session;
ALTER TABLE session_new RENAME TO session;
CREATE INDEX idx_session_date ON session(date);
CREATE INDEX idx_session_routine_version_id ON session(routine_version_id);
CREATE INDEX idx_session_status ON session(status);
CREATE INDEX idx_session_deload_id ON session(deload_id);

-- plan_assignment: module_version_id → routine_version_id
CREATE TABLE plan_assignment_new (
    routine_version_id INTEGER NOT NULL REFERENCES routine_version(id) ON DELETE CASCADE,
    exercise_id INTEGER NOT NULL REFERENCES exercise(id) ON DELETE RESTRICT,
    sets INTEGER NOT NULL CHECK(sets > 0),
    reps TEXT NOT NULL CHECK(reps IN ('8-12', 'TO_TECHNICAL_FAILURE', '30-45_SEC')),
    sort_order INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY(routine_version_id, exercise_id)
);
INSERT INTO plan_assignment_new
SELECT module_version_id, exercise_id, sets, reps, sort_order FROM plan_assignment;
DROP TABLE plan_assignment;
ALTER TABLE plan_assignment_new RENAME TO plan_assignment;
CREATE INDEX idx_plan_assignment_exercise_id ON plan_assignment(exercise_id);

-- exercise: eliminar module_code (ejercicios agnósticos CA-23.15)
CREATE TABLE exercise_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    equipment_type_id INTEGER NOT NULL REFERENCES equipment_type(id) ON DELETE RESTRICT,
    is_bodyweight INTEGER NOT NULL DEFAULT 0,
    is_isometric INTEGER NOT NULL DEFAULT 0,
    is_to_technical_failure INTEGER NOT NULL DEFAULT 0,
    is_custom INTEGER NOT NULL DEFAULT 0,
    media_resource TEXT
);
INSERT INTO exercise_new
SELECT id, name, equipment_type_id, is_bodyweight, is_isometric,
       is_to_technical_failure, is_custom, media_resource FROM exercise;
DROP TABLE exercise;
ALTER TABLE exercise_new RENAME TO exercise;
CREATE UNIQUE INDEX idx_exercise_name_equipment ON exercise(name, equipment_type_id);
CREATE INDEX idx_exercise_equipment_type_id ON exercise(equipment_type_id);

-- alert: module_code → routine_id
CREATE TABLE alert_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type TEXT NOT NULL,
    level TEXT NOT NULL,
    exercise_id INTEGER REFERENCES exercise(id) ON DELETE RESTRICT,
    routine_id INTEGER REFERENCES routine(id) ON DELETE CASCADE,
    muscle_group TEXT,
    message TEXT NOT NULL,
    is_active INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL,
    resolved_at TEXT
);
INSERT INTO alert_new (id, type, level, exercise_id, routine_id, muscle_group,
                       message, is_active, created_at, resolved_at)
SELECT a.id, a.type, a.level, a.exercise_id,
       CASE a.module_code WHEN 'A' THEN 1 WHEN 'B' THEN 2 WHEN 'C' THEN 3 ELSE NULL END,
       a.muscle_group, a.message, a.is_active, a.created_at, a.resolved_at
FROM alert a;
DROP TABLE alert;
ALTER TABLE alert_new RENAME TO alert;
CREATE INDEX idx_alert_is_active ON alert(is_active);
CREATE INDEX idx_alert_type ON alert(type);
CREATE INDEX idx_alert_exercise_id ON alert(exercise_id);
CREATE INDEX idx_alert_routine_id ON alert(routine_id);

-- deload: eliminar frozen_version_module_a/b/c
CREATE TABLE deload_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE', 'COMPLETED')),
    activation_date TEXT NOT NULL,
    completion_date TEXT
);
INSERT INTO deload_new SELECT id, status, activation_date, completion_date FROM deload;
DROP TABLE deload;
ALTER TABLE deload_new RENAME TO deload;
CREATE INDEX idx_deload_status ON deload(status);

-- rotation_state: eliminar current_version_module_a/b/c
CREATE TABLE rotation_state_new (
    id INTEGER PRIMARY KEY DEFAULT 1 CHECK(id = 1),
    microcycle_position INTEGER NOT NULL DEFAULT 1 CHECK(microcycle_position >= 1),
    microcycle_count INTEGER NOT NULL DEFAULT 0 CHECK(microcycle_count >= 0)
);
INSERT INTO rotation_state_new
SELECT id, microcycle_position, microcycle_count FROM rotation_state;
DROP TABLE rotation_state;
ALTER TABLE rotation_state_new RENAME TO rotation_state;
```

### Fase 4 — Limpiar tablas obsoletas:

```sql
DROP TABLE module_version;
DROP TABLE module;
```

Nota crítica: Los IDs de `routine_version` se insertan explícitamente con los mismos valores que `module_version.id` → las FKs de `session` y `plan_assignment` siguen válidas sin necesidad de UPDATE en cada fila.

---

## Bloque B — Entidades Room (4 nuevas, 6 modificadas, 2 eliminadas)

- **`RoutineEntity` (Nuevo):** Tabla `routine` — `id` (PK autoincrement), `name` (TEXT UNIQUE), `sortOrder` (INT CHECK ≥ 1), `createdAt` (TEXT). Ubicación: `data/local/entity/RoutineEntity.kt`
- **`RoutineVersionEntity` (Nuevo):** Tabla `routine_version` — `id` (PK autoincrement), `routineId` (FK→routine ON DELETE CASCADE), `versionNumber` (INT CHECK ≥ 1). Unique(routineId, versionNumber). Ubicación: `data/local/entity/RoutineVersionEntity.kt`
- **`RoutineCurrentVersionEntity` (Nuevo):** Tabla `routine_current_version` — `routineId` (PK, FK→routine ON DELETE CASCADE), `currentVersionNumber` (INT DEFAULT 1). Ubicación: `data/local/entity/RoutineCurrentVersionEntity.kt`
- **`DeloadFrozenVersionEntity` (Nuevo):** Tabla `deload_frozen_version` — PK compuesta `(deloadId, routineId)`, `deloadId` (FK→deload ON DELETE CASCADE), `routineId` (FK→routine ON DELETE RESTRICT), `frozenVersionNumber` (INT). Ubicación: `data/local/entity/DeloadFrozenVersionEntity.kt`
- **`ExerciseEntity` (Modificación):** Eliminar campo `moduleCode` y su FK a `module`. Eliminar índice en `module_code`. Nivel: Mayor.
- **`SessionEntity` (Modificación):** FK `moduleVersionId` → `routineVersionId`. Tabla destino: `routine_version`. Nivel: Menor.
- **`PlanAssignmentEntity` (Modificación):** FK y PK compuesta `moduleVersionId` → `routineVersionId`. Nivel: Menor.
- **`AlertEntity` (Modificación):** Campo `moduleCode: String?` (FK→module) → `routineId: Long?` (FK→routine ON DELETE CASCADE). Nivel: Mayor.
- **`DeloadEntity` (Modificación):** Eliminar `frozenVersionModuleA/B/C`. Versiones congeladas en `deload_frozen_version`. Nivel: Mayor.
- **`RotationStateEntity` (Modificación):** Eliminar `currentVersionModuleA/B/C`. Solo `id`, `microcyclePosition`, `microcycleCount`. Nivel: Mayor.
- **`ModuleEntity` (Eliminada)**, **`ModuleVersionEntity` (Eliminada)**

---

## Bloque C — DAOs (4 nuevos, 5 modificados, 2 eliminados)

- **`RoutineDao` (Nuevo):** `getAll(): Flow<List<RoutineEntity>>` (ORDER BY sort_order), `getById`, `insert`, `update`, `delete`, `getMaxSortOrder`, `countRoutines`, `updateSortOrder`. Ubicación: `data/local/dao/RoutineDao.kt`
- **`RoutineVersionDao` (Nuevo):** `getByRoutineId`, `getByRoutineIdAndVersion`, `insert`, `delete`, `countByRoutineId`, `getMaxVersionNumber`, `getVersionCountsByRoutine()` (query agregado — evita N+1 en closeSession). Ubicación: `data/local/dao/RoutineVersionDao.kt`
- **`RoutineCurrentVersionDao` (Nuevo):** `getAll(): Flow`, `getAllOnce(): List` (suspend), `getByRoutineId`, `insert`, `update`, `deleteByRoutineId`. Ubicación: `data/local/dao/RoutineCurrentVersionDao.kt`
- **`DeloadFrozenVersionDao` (Nuevo):** `getByDeloadId`, `insertAll`, `deleteByDeloadId`. Ubicación: `data/local/dao/DeloadFrozenVersionDao.kt`
- **`PlanAssignmentDao` (Modificación):** Todas las queries: `module_version_id` → `routine_version_id`. `getPreviewByModuleVersionId` → `getPreviewByRoutineVersionId`. JOIN `module m ON e.module_code = m.code` eliminado — `loadIncrementKg` resuelto en capa de aplicación por `LoadIncrementResolver`. `SessionPreviewExerciseDto` pierde `loadIncrementKg`, obtiene `muscleGroup: String?` del JOIN existente con `muscle_zone`.
- **`SessionDao` (Modificación):** DTOs: `moduleCode: String` → `routineName: String`. JOINs: `module_version` + `module` → `routine_version` + `routine`. `getSessionIdsByModuleInRange` → `getSessionIdsByRoutineInRange(routineId: Long, limit: Int)`. `getLastSessionDateByModule` → `getLastSessionDateByRoutine(routineId: Long)`. Nivel: Mayor.
- **`ExerciseDao` (Modificación):** DTO `ExerciseWithDetails` pierde `moduleCode`, `moduleName`. Eliminar `getByModuleCodeNotInVersion` → reemplazar por `getEligibleForVersion(routineVersionId: Long)`. `getEligibleSubstitutesForSession(moduleCode, sessionId)` → `getEligibleSubstitutesForSession(muscleZoneIds: List<Long>, sessionId: Long)`. Nivel: Mayor.
- **`SessionExerciseDao` (Modificación):** DTOs con `moduleCode` → `routineId` o eliminarlo. Eliminar JOIN `module` para `loadIncrementKg`; agregar `muscleGroup: String?` via JOIN `exercise_muscle_zone` + `muscle_zone`. `ExerciseHistoryEntryDto`: `moduleCode` → `routineName` via JOIN `routine_version` + `routine`. Nivel: Mayor.
- **`AlertDao` (Modificación):** `existsActiveByModule(moduleCode)` → `existsActiveByRoutine(routineId: Long)`. `resolveByModuleAndType` → `resolveByRoutineAndType`. Nivel: Menor.
- **`ModuleDao` (Eliminado)**, **`ModuleVersionDao` (Eliminado)**

---

## Bloque D — Modelos de Dominio y Reglas (5 modificados, 3 nuevos)

- **`Routine` — Modelo de dominio (Nuevo):** `data class Routine(val id: Long, val name: String, val sortOrder: Int, val createdAt: String)`. Ubicación: `domain/model/Routine.kt`
- **`RoutineVersion` — Modelo de dominio (Nuevo):** `data class RoutineVersion(val id: Long, val routineId: Long, val versionNumber: Int)`. Ubicación: `domain/model/RoutineVersion.kt`
- **`LoadIncrementResolver` — Regla de negocio (Nuevo):** Reemplaza `module.load_increment_kg`. Tren inferior → 5.0 Kg, tren superior → 2.5 Kg, fallback → 2.5. Ubicación: `domain/rules/LoadIncrementResolver.kt`

  ```kotlin
  object LoadIncrementResolver {
      private val LOWER_BODY_GROUPS = setOf(
          "Cuádriceps", "Isquiotibiales", "Glúteos",
          "Aductores", "Abductores", "Gemelos",
      )

      fun resolve(muscleGroup: String): Double =
          if (muscleGroup in LOWER_BODY_GROUPS) 5.0 else 2.5
  }
  ```

- **`RotationResolver` (Modificación — Rewrite completo):** Ya no usa A/B/C hardcoded. Opera con N rutinas dinámicas:
  - `resolveRoutineId(position: Int, routines: List<Routine>): Long` — indexa por sort_order
  - `resolveVersionNumber(routineId: Long, currentVersions: Map<Long, Int>): Int` — lookup directo
  - `advanceRotation(position: Int, totalRoutines: Int, currentVersions: Map<Long, Int>, versionCounts: Map<Long, Int>, isDeload: Boolean): AdvanceResult` — retorna `newPosition`, `newMicrocycleCount`, `updatedVersions: Map<Long, Int>` (con wrap-around `(current % count) + 1` por rutina)
  - Nivel de cambio: Crítico

- **Modelos de dominio adicionales con `moduleCode` (15 modelos — Modificación):** `Exercise`, `ExerciseHistoryEntry`, `SessionHistoryItem`, `SessionDetail`, `SessionSummary`, `SessionPreviewExercise`, `PlanVersionDetail`, `SubstituteExerciseInfo`, `AlertTriggerData.RirTrigger`, `AlertTriggerData.InactivityTrigger`, `ModuleInactivityData` (→ `RoutineInactivityData`), `DeloadHomeState.Active`, `DeloadHomeState.Required`, `RirByModule` (→ `RirByRoutine`), `SetDistributionData`. Cada uno pierde `moduleCode` y recibe `routineId: Long` o `routineName: String` según contexto.

- **`NextSession` (Modificación):** `moduleCode: String` → `routineId: Long` + `routineName: String`. `moduleVersionId: Long` → `routineVersionId: Long`. Nivel: Mayor.
- **`RotationState` (Modificación):** Eliminar `currentVersionModuleA/B/C`. Solo `microcyclePosition: Int`, `microcycleCount: Int`. Nivel: Mayor.
- **`Deload` (Modificación):** Eliminar `frozenVersionModuleA/B/C`. Agregar `frozenVersions: Map<Long, Int>`. Nivel: Mayor.
- **`DeloadState` (Modificación):** `DeloadActive.frozenVersionA/B/C` → `frozenVersions: Map<Long, Int>`. `DeloadRequired.modules: List<String>` → `routineIds: List<Long>`. Nivel: Mayor.
- **`AlertThresholdRule.MUSCLE_GROUPS_BY_MODULE` — Eliminado:** Mapa hardcoded A→[...], B→[...], C→[...] eliminado. Grupos musculares dinámicos desde `plan_assignment` + `exercise_muscle_zone` + `muscle_zone`. Incluye `CorrectiveActionRule.ROTATE_VERSION` que opera con `routineId` (CA-23.27).

---

## Bloque E — Repository (1 nuevo, 5 modificados)

- **`RoutineRepositoryImpl` (Nuevo):** Inyecta `routineDao`, `routineVersionDao`, `routineCurrentVersionDao`, `planAssignmentDao`, `sessionDao`. Métodos: `createRoutine()` (valida nombre ≤50 chars, UNIQUE; inserta rutina + V1 automática + `routine_current_version`), `deleteRoutine()` (valida `countRoutines() > 1`, no sesión activa, reordena `sort_order`), `reorderRoutines()` (actualiza `sort_order`, recalibra `microcyclePosition`).

- **`SessionRepositoryImpl` (Modificación — ~300 líneas cambian):** Constructor pasa de 13 a 15 dependencias (quita `moduleDao`, `moduleVersionDao`, agrega `routineDao`, `routineVersionDao`, `routineCurrentVersionDao`, `deloadFrozenVersionDao`). Cambios clave:
  - `getNextModuleVersionId()` → `getNextRoutineVersionId()` — usa `routineDao.getAll()`, `RoutineCurrentVersionDao`, nuevo `RotationResolver`
  - `closeSession()` — `advanceRotation` con N rutinas dinámicas; sin N+1 via `getVersionCountsByRoutine()`
  - `activateDeload()` — copia versiones actuales de `routine_current_version` a `deload_frozen_version`
  - Deload finalization: `countDeloadSessions(deloadId) == routineDao.countRoutines()` (era `== 6`)
  - `evaluateRirOutOfRange()` — itera `routineDao.getAll()` en vez de `listOf("A", "B", "C")`
  - `evaluateModuleInactivity()` → `evaluateRoutineInactivity()` — zonas musculares dinámicas via query
  - `getSessionPreviewExercises()` — `loadIncrementKg` via `LoadIncrementResolver`

- **`AlertRepositoryImpl` (Modificación):** `moduleCode` → `routineId: Long` en ~10 puntos. Grupos musculares dinámicos. Nivel: Mayor.
- **`MetricsRepositoryImpl` (Modificación):** `moduleCode: String` → `routineId: Long` en parámetros. Nivel: Mayor.
- **`ExerciseRepositoryImpl` (Modificación):** Eliminar `moduleCode` como parámetro. `getEligibleSubstitutes(moduleCode, sessionId)` → `getEligibleSubstitutes(muscleZoneIds: List<Long>, sessionId: Long)`. Nivel: Menor.
- **`PlanRepositoryImpl` (Modificación):** `moduleVersionId` → `routineVersionId`. Eliminar agrupación por `moduleCode`. `getAvailableExercisesForVersion` sin filtro por módulo. Nivel: Mayor.

---

## Bloque F — UseCases (6 nuevos, 6+ modificados)

**Nuevos (gestión del plan — CA-23.01 a CA-23.08):**

| UseCase | CA | Ubicación |
|---|---|---|
| `CreateRoutineUseCase` | CA-23.01, CA-23.06 | `domain/usecase/routine/CreateRoutineUseCase.kt` |
| `UpdateRoutineNameUseCase` | CA-23.02 | `domain/usecase/routine/UpdateRoutineNameUseCase.kt` |
| `DeleteRoutineUseCase` | CA-23.03 | `domain/usecase/routine/DeleteRoutineUseCase.kt` |
| `ReorderRoutinesUseCase` | CA-23.04 | `domain/usecase/routine/ReorderRoutinesUseCase.kt` |
| `CreateRoutineVersionUseCase` | CA-23.07 | `domain/usecase/routine/CreateRoutineVersionUseCase.kt` |
| `DeleteRoutineVersionUseCase` | CA-23.08 | `domain/usecase/routine/DeleteRoutineVersionUseCase.kt` |

**Modificados:**

| UseCase | Cambio |
|---|---|
| `GetNextSessionInfoUseCase` | Nuevo `RotationResolver` + `RoutineCurrentVersionDao`. Retorna `routineId`, `routineName`, `routineVersionId` |
| `StartSessionUseCase` | `moduleVersionId` → `routineVersionId` |
| `GetSessionPreviewUseCase` | `moduleVersionId` → `routineVersionId` |
| `SubstituteExerciseUseCase` | Filtro por zona muscular (CA-23.22) |
| `AssignExerciseToVersionUseCase` | `moduleVersionId` → `routineVersionId` |
| `UnassignExerciseFromVersionUseCase` | `moduleVersionId` → `routineVersionId` |
| `GetSessionSummaryUseCase` | `moduleCode` → `routineName` |
| `ExportBackupUseCase` | Esquema con tablas `routine`/`routine_version`/`routine_current_version`/`deload_frozen_version` |
| `ImportBackupUseCase` | Detectar versión backup (v8 vs v9), transformar v8→v9 en importación |
| `ValidateBackupUseCase` | Validar esquema v9 |

---

## Bloque G — UI: Pantallas nuevas de gestión del plan

- **`RoutineListScreen` + `RoutineListViewModel` (Nuevo):** Lista de rutinas en sort_order. Drag-and-drop para reordenar (CA-23.04). Acciones: editar nombre (inline/dialog), eliminar con confirmación (CA-23.03). FAB "Crear rutina" (CA-23.01). Tap rutina → versiones. CenterAlignedTopAppBar "Mi Plan". Bottom Nav visible.
- **`RoutineVersionListScreen` + `RoutineVersionListViewModel` (Nuevo):** Lista de versiones con conteo de ejercicios. Crear versión (CA-23.07), eliminar con confirmación (CA-23.08). Tap versión → `PlanVersionDetailScreen(routineVersionId)`.
- **`PlanVersionDetailScreen/ViewModel` (Modificación):** `moduleVersionId` → `routineVersionId`. Se agrega capacidad de asignar y remover ejercicios (CA-23.12, CA-23.13).
- **`ExerciseSelectorScreen/ViewModel` (Nuevo):** Bottom sheet para seleccionar ejercicio del Diccionario y asignarlo con sets (default 4) y reps (default '8-12').

---

## Bloque H — UI: Pantallas existentes modificadas

| Pantalla | Cambio |
|---|---|
| `HomeScreen/HomeViewModel` | `moduleCode` → `routineName` en NextSessionCard. Empty state si `countRoutines() == 0` |
| `ActiveSessionViewModel/Screen` | `moduleCode` → `routineName` en header. `loadIncrementKg` via `LoadIncrementResolver` |
| `SessionPreviewViewModel/Screen` | `moduleCode` → `routineName`. `loadIncrementKg` via `LoadIncrementResolver` |
| `SessionSummaryViewModel` | `moduleCode` → `routineName` |
| `SessionHistoryScreen` | `moduleCode` → `routineName` |
| `SessionDetailScreen` | `moduleCode` → `routineName` |
| `SubstituteExerciseViewModel` | Filtro por zona muscular (CA-23.22): `muscleZoneIds` del ejercicio |
| `ExerciseDictionaryScreen` | Sin `moduleCode`/`moduleName`. Grouping por zona muscular o alfabético |
| `ExerciseDetailScreen/ViewModel` | Eliminar `moduleCode`, `moduleName` del display (CA-23.15) |
| `CreateExerciseScreen/ViewModel` | Eliminar selector de módulo (dropdown A/B/C) (CA-23.15) |
| `TrainingPlanScreen/ViewModel` | **Eliminados** — reemplazados por `RoutineListScreen` + `RoutineVersionListScreen` |
| `AlertsScreen` | `moduleCode` → `routineName` |
| `DeloadManagementScreen/ViewModel` | `DeloadState` adaptar a Map dinámico |
| `MetricsScreen/ViewModel` | `moduleCode` → `routineId` en params |
| `TensionNavHost/NavigationRoutes` | `moduleVersionId`/`moduleCode` → `routineVersionId`/`routineId`. Nuevas rutas para gestión de rutinas |

---

## Hitos de Implementación

| Hito | Contenido | Dependencias |
|---|---|---|
| 1 | MIGRATION_8_9, Entidades nuevas/modificadas, TensionDatabase v9, PrepopulateCallbackV9 | Ninguna (bloqueante) |
| 2 | DAOs nuevos (Routine, RoutineVersion, RoutineCurrentVersion, DeloadFrozenVersion), DAOs modificados, DAOs eliminados | Hito 1 |
| 3 | `LoadIncrementResolver`, `RotationResolver` rewrite, Modelos de dominio nuevos y modificados (15+), `AlertThresholdRule` sin mapa hardcoded | Hito 2 |
| 4 | `SessionRepositoryImpl` rewrite parcial (~300 líneas), `RoutineRepositoryImpl` nuevo, otros Repos modificados | Hitos 2, 3 |
| 5 | UseCases de gestión de rutinas (6 nuevos), UseCases existentes modificados de asignación | Hito 2 |
| 6 | UseCases existentes modificados de sesión, deload, backup | Hitos 3, 4 |
| 7 | UI Gestión del plan: RoutineListScreen, RoutineVersionListScreen, ExerciseSelectorScreen, PlanVersionDetailScreen modificado, navegación | Hito 5 |
| 8 | UI Pantallas existentes: Home, ActiveSession, Preview, Summary, History, Substitute, Dictionary, Alerts, Deload, Metrics, Backup, Navigation global | Hitos 4, 6 |

---

## Validación de Impacto

**Código real verificado:**

| Componente | Estado Actual (v8) | Impacto para v9 |
|---|---|---|
| `ModuleEntity` | Tabla `module`, PK=code (String), 3 filas fijas (A, B, C) | **Eliminada** — reemplazada por `RoutineEntity` con PK Long autoincrement |
| `ModuleVersionEntity` | Tabla `module_version`, FK→module.code, 9 filas (3 módulos × 3 versiones) | **Eliminada** — reemplazada por `RoutineVersionEntity`. IDs preservados en migración |
| `ExerciseEntity.moduleCode` | FK→module.code, obliga a cada ejercicio a pertenecer a un módulo | **Columna eliminada** — ejercicios agnósticos (CA-23.15). 43 ejercicios pierden su module_code sin impacto funcional |
| `RotationStateEntity` | 5 campos: position, versionA, versionB, versionC, count. Fila única | **Reducida a 3 campos** — position, count, id. Versiones migran a `routine_current_version` |
| `DeloadEntity` | 6 campos incluyendo frozenVersionA/B/C | **Reducida a 4 campos** — id, status, activation_date, completion_date |
| `RotationResolver` | 47 líneas, hardcoded A/B/C con posiciones 1-6, versiones hardcoded %3 | **Rewrite completo** — parametrizado por `List<Routine>`, `Map<Long, Int>` |
| `SessionRepositoryImpl` | 1040 líneas, 13 dependencias inyectadas, lógica A/B/C en 8+ métodos | **~300 líneas cambian** — constructor: 13 → 15 dependencias (quita 2, agrega 4) |
| `GetNextSessionInfoUseCase` | 52 líneas, usa `RotationResolver.resolveModuleCode()` | **Rewrite** — usa `routineDao.getAll()`, `routineCurrentVersionDao`, nuevo `RotationResolver` |
| `SessionDao` | 120 líneas, 6 DTOs con `moduleCode` | **~80% queries modificadas** — JOINs con `routine`+`routine_version` |
| `ExerciseDao` | 120 líneas, 3 queries con `module_code` filter | **3 queries modificadas** — eliminar JOIN module, sustitución filtra por zona muscular |
| `PlanAssignmentDao` | 109 líneas, 2 DTOs, 8 queries con `module_version_id` | **Todas las queries cambian** `module_version_id` → `routine_version_id`. JOIN `module` eliminado |
| `AlertDao` | 74 líneas, `existsActiveByModule`, `resolveByModuleAndType` | **2 queries modificadas** — `module_code` → `routine_id` |
| `AlertThresholdRule.MUSCLE_GROUPS_BY_MODULE` | Mapa hardcoded: A→[Espalda,Bíceps,Abdomen], B→[...], C→[...] | **Eliminado** — grupos musculares dinámicos de `plan_assignment` + `exercise_muscle_zone` |
| `ExerciseSeeder` | Seeds 43 ejercicios con `module_code` | **Requiere actualización** — omitir `module_code` en `PrepopulateCallbackV9` |
| `AlertRepositoryImpl` | Usa `moduleCode` 10+ veces, `MUSCLE_GROUPS_BY_MODULE` | **Rewrite parcial** — `moduleCode` → `routineId`, grupos musculares dinámicos |
| `MetricsRepositoryImpl` | `moduleCode: String` param, `getSessionIdsByModuleInRange` | **Parámetro cambia** a `routineId: Long` |
| `PlanRepositoryImpl` | Agrupa por `moduleCode`, mapea nombres módulo | **Rewrite** — interfaz `PlanRepository` cambia |
| `TensionNavHost` | 22+ rutas con `moduleVersionId`/`moduleCode` | **~10 rutas modificadas**, ~4 rutas nuevas |
| Pantallas UI | 15+ pantallas con `moduleCode` en display | **Display textual cambia** — "Módulo A" → nombre de rutina ("Pull + Abs") |
| Modelos de dominio | 15 data classes con campo `moduleCode: String` | **Cambio mecánico masivo** — `moduleCode` → `routineId: Long` o `routineName: String` |

**Verificaciones críticas:**

- `routine_version.id` en migración se inserta con el mismo valor que `module_version.id` → FKs de `session` y `plan_assignment` permanecen válidas sin UPDATE masivo.
- `exercise.module_code` se elimina sin afectar `exercise_muscle_zone` — relación ejercicio↔zona muscular independiente del módulo.
- Lógica de deload cambia de "6 sesiones fijas" a "N sesiones" (N = número de rutinas). Correcto: un microciclo recorre todas las rutinas exactamente una vez (CA-23.26).
- `LoadIncrementResolver` usa `muscle_zone.muscle_group` que ya existe en la DB.
- Las migraciones Room son transaccionales por defecto.

**Cadenas de invocación:**

```text
// Creación de rutina (CA-23.01)
RoutineListScreen (tap FAB "Crear rutina")
  → Dialog: nombre (max 50 chars, validación unicidad)
    → RoutineListViewModel.createRoutine(name)
      → CreateRoutineUseCase(name)
        → routineDao.getMaxSortOrder() → sortOrder = MAX + 1
        → routineDao.insert(RoutineEntity(name, sortOrder, today)) → routineId
        → routineVersionDao.insert(RoutineVersionEntity(routineId, 1)) → versionId
        → routineCurrentVersionDao.insert(RoutineCurrentVersionEntity(routineId, 1))

// Rotación con N rutinas (CA-23.16, CA-23.19)
closeSession(sessionId)
  → sessionDao.updateStatus(sessionId, status)
  → evaluateProgression(sessionId, routineVersionId, isDeload)
  → routineDao.getAll() → routines (ordenadas por sort_order)
  → routineCurrentVersionDao.getAllOnce() → currentVersions: Map<Long, Int>
  → rotationStateDao.getRotationState() → RotationState(position, count)
  → RotationResolver.advanceRotation(position, routines.size, currentVersions,
      versionCounts = routineVersionDao.getVersionCountsByRoutine(), isDeload)
  → AdvanceResult(newPosition, newCount, updatedVersions)
  → rotationStateDao.update(newPosition, newCount)
  → Si microciclo completó: routineCurrentVersionDao.update(updatedVersion) por rutina

// Activar deload (CA-23.25, CA-23.26)
ActivateDeloadUseCase()
  → SessionRepositoryImpl.activateDeload()
    → deloadDao.insert(DeloadEntity(today)) → deloadId
    → routineCurrentVersionDao.getAllOnce() → currentVersions
    → deloadFrozenVersionDao.insertAll(
        currentVersions.map { DeloadFrozenVersionEntity(deloadId, it.routineId, it.currentVersionNumber) }
      )
```

**Riesgos identificados:**

| Riesgo | Impacto | Mitigación |
|---|---|---|
| Migración SQL falla o corrompe datos | Pérdida total del plan y sesiones del usuario | Migración atómica Room. Tests con `MigrationTestHelper` + datos reales de v8. IDs de `routine_version` preservados exactamente como `module_version` |
| Queries inconsistentes post-migración | Crash en pantallas con queries v8 en DB v9 | Todos los DAOs se modifican en el mismo hito que las entidades. Sin ventana de inconsistencia |
| Deload activo al momento de migración | Versiones congeladas se pierden | Fase 2 copia `frozen_version_module_a/b/c` a `deload_frozen_version` para cada deload existente |
| Rotación con N=1 rutina | Microciclo de 1 sesión, versión rota cada sesión | Comportamiento correcto por diseño (CA-23.17). Tests específicos para N=1 |
| Rotación con N rutinas y versiones heterogéneas | Rutina A 2 versiones, Rutina B 5 | Wrap-around independiente por rutina: `(current % count) + 1`. Tests para combinaciones heterogéneas |
| `loadIncrementKg` calculado en app vs en SQL | Valor incorrecto con ejercicios multi-zona | `LoadIncrementResolver` usa `muscle_group` principal. Ningún ejercicio actual cruza tren sup/inf |
| Sesión activa IN_PROGRESS durante upgrade APK | Migración corre con sesión IN_PROGRESS | Migración puramente esquemática. `routine_version.id == module_version.id` → sesión activa sigue funcionando |
| Backup v8 importado en app v9 | Esquema JSON de exportación cambia | `ImportBackupUseCase` detecta versión y transforma v8→v9 |
| Seeders para nuevas instalaciones v9+ | `ExerciseSeeder` actual inserta `module_code` | `PrepopulateCallbackV9` con seeders actualizados; callback original intacto para migraciones ≤v8 |

---

## Notas Técnicas

- **Scope de la migración:** MIGRATION_8_9 es la migración más extensa del proyecto (7 tablas recreadas, 4 tablas nuevas, 2 tablas eliminadas). Testear con `MigrationTestHelper` + datos representativos de v8 (3 módulos, 9 versiones, 82 asignaciones, sesiones con y sin deload, alertas activas).
- **Seeders obsoletos:** `ModuleSeeder` y `PlanSeeder` se mantienen en el código porque las migraciones v1→v7 y v7→v8 los referencian. En nuevas instalaciones v9+, `PrepopulateCallbackV9` inicializa la DB sin invocarlos.
- **`LoadIncrementResolver` vs columna en DB:** Se optó por calcular en capa de aplicación porque el valor se deriva 1:1 del grupo muscular (regla de negocio pura), evita duplicar datos y es más fácil de modificar.
- **Onboarding nuevas instalaciones:** Con módulos fijos eliminados, nuevas instalaciones v9+ inician con plan vacío. Flujo: empty state en Home con CTA "Crear tu primer plan" → `RoutineListScreen`.
- **Numeración de deload sessions:** `countDeloadSessions(deloadId)` sigue funcionando (basa en `session.deload_id`), pero la comparación `== 6` se reemplaza por `== routineDao.countRoutines()`.
- **Nombres en migración SQL (Fase 2):** `'Pull + Abs'`, `'Push'`, `'Pierna'` se derivan de `module.name` del `ModuleSeeder`. Son inmediatamente editables post-migración (CA-23.02).
- **`ExerciseSeeder` requiere actualización para v9+:** El seeder actual inserta `module_code` (L80: `put("module_code", moduleCode)`). En v9+ la columna no existe en `exercise`. Se mantiene versión actual para migraciones ≤v8.
- **Impacto en Backup/Restore (HU-19):** Tablas exportadas `module`/`module_version` desaparecen; se agregan `routine`, `routine_version`, `routine_current_version`, `deload_frozen_version`. Backups v8 importados en app v9 requieren transformación en capa de import.
- **Propagación de `muscleGroup` para `LoadIncrementResolver`:** `SessionExerciseForProgression` pierde `loadIncrementKg`. El query de `getSessionExercisesForProgression` debe agregar JOIN a `exercise_muscle_zone` + `muscle_zone` para obtener `muscle_group` principal.

---

## Referencias y Validación

**Historias relacionadas:**

- Historia #3: HU-03 — Registro de plan (base que HU-23 reemplaza con plan dinámico)
- Historia #4: HU-04 — Detalle de versión — se reparametriza para `routineVersionId`, agrega CRUD de asignaciones
- Historia #5: HU-05 — `startSession()`: `moduleVersionId` → `routineVersionId`
- Historia #6: HU-06 — Registro de serie: sin cambio funcional
- Historia #7: HU-07 — Sustitución: filtro cambia de `module_code` a zona muscular (CA-23.22)
- Historia #8: HU-08 — Cross-cutting isométrico: sin cambio funcional
- Historia #9: HU-09 — Cierre de sesión + rotación: rewrite para N rutinas
- Historia #10: HU-10 — Motor de progresión: `ProgressionClassificationRule` sin cambio. `SessionExerciseForProgression` pierde `moduleCode` y `loadIncrementKg`
- Historia #11: HU-11 — Regla Doble Umbral: `loadIncrementKg` → `LoadIncrementResolver`
- Historia #12: HU-12 — Detección fatiga: `moduleCode` → `routineId`. `CorrectiveActionRule.ROTATE_VERSION` → por rutina (CA-23.27)
- Historia #13: HU-13 — Resumen post-sesión: `SessionSummaryInfo.moduleCode` → `routineName`. `existsActiveByModule` → `existsActiveByRoutine`
- Historia #14: HU-14 — Protocolo de descarga: `frozenVersionModuleA/B/C` → `deload_frozen_version`. Duración cambia de 6 a N sesiones
- Historia #15: HU-15 — KPIs: `getSessionIdsByModuleInRange` → `getSessionIdsByRoutineInRange`. "RIR Promedio por Módulo" → "RIR Promedio por Rutina"
- Historia #17: HU-17 — Historial: `SessionHistoryItem`, `SessionDetail`, `ExerciseHistoryEntryDto` tienen `moduleCode` → `routineName`
- Historia #18: HU-18 — KPIs y alertas: `MODULE_REQUIRES_DELOAD` → `ROUTINE_REQUIRES_DELOAD`. `MODULE_INACTIVITY` → `ROUTINE_INACTIVITY`
- Historia #19: HU-19 — Backup/Restore: esquema JSON cambia. Importación detecta v8 vs v9
- Historia #21: HU-21 — `sort_order` en `plan_assignment`: preservado en v9
- Historia #22: HU-22 — Preview de sesión: `moduleVersionId` → `routineVersionId`. `LoadDisplayMapper` usa `LoadIncrementResolver`

**Validado por:** Esteban Colorado González | **Fecha:** 2026-05-06 | **Enfoque:** Exploratorio

**Métricas de Análisis Arquitectónico:**
- Inicio: 2026-05-06 11:11 | Fin: 2026-05-06 12:21 | Duración real: 70 minutos
