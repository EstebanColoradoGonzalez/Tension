# Dev Agent Record — Dev-Rápido

### Debug Log

| # | Tipo | Descripción | Resolución |
|---|------|-------------|------------|
| 1 | Cosmético | `PlanRepositoryImpl.assignExercise()` usaba `(getMaxSortOrder() ?: -1) + 1` — primera asignación manual en versión vacía produciría `sort_order = 0` (0-based), inconsistente con seed data (1-based) | Cambiado `?: -1` a `?: 0` para que primera asignación manual obtenga `sort_order = 1` |

### Completion Notes

- ✅ Auditoría completada — Cruce exhaustivo contra documentación. 3 hallazgos corregidos. Sin issues pendientes.
- ✅ Desarrollo completado — BUILD SUCCESSFUL — todos los unit tests pasan, auditoría de código 11/11 PASS con 0 bugs.

### File List

| Acción | Archivo | Descripción |
|--------|---------|-------------|
| M | `data/local/entity/PlanAssignmentEntity.kt` | Agregado `sortOrder: Int = 0` con `@ColumnInfo(name = "sort_order")` |
| M | `data/local/database/TensionDatabase.kt` | Bumped `version = 7` → `version = 8` |
| M | `data/local/database/Migrations.kt` | Agregada `MIGRATION_7_8`: ALTER TABLE + DELETE + 82 INSERTs con sort_order |
| M | `di/DatabaseModule.kt` | Registrada `MIGRATION_7_8` en `.addMigrations()` |
| M | `data/local/seed/PlanSeeder.kt` | Reescrito con 82 asignaciones (era 93) + sort_order en helper `pa()` |
| M | `data/local/dao/PlanAssignmentDao.kt` | ORDER BY → `pa.sort_order ASC`, agregado `getMaxSortOrder()` |
| M | `data/local/dao/SessionExerciseDao.kt` | 3 queries: ORDER BY → COALESCE subconsulta, agregado `INNER JOIN session s` donde faltaba |
| M | `data/repository/PlanRepositoryImpl.kt` | `assignExercise()` computa `nextSortOrder` antes de insert |
| M | `ui/catalog/PlanVersionDetailUiState.kt` | Agregado `isBodyweight: Boolean = false` a `PlanExerciseItem` |
| M | `ui/catalog/PlanVersionDetailViewModel.kt` | Mapeado `isBodyweight = pe.isBodyweight` |
| M | `ui/catalog/PlanVersionDetailScreen.kt` | Badge "Fuera del gym" para bodyweight en Módulo A |
| M | `ui/session/ActiveSessionScreen.kt` | Badge "Fuera del gym" en 3 sub-composables + `moduleCode` propagado |
| M | `res/values/strings.xml` | Agregado `exercise_outside_gym` = "Fuera del gym" |
| C | `androidTest/.../MigrationV7ToV8Test.kt` | 7 tests instrumentados para MIGRATION_7_8 |

**Resultado:** BUILD SUCCESSFUL — todos los unit tests pasan, auditoría de código 11/11 PASS con 0 bugs.

---

## §7 Tareas de Implementación

### Fase 0: Infraestructura — Esquema y Migración

**ACs vinculados:** CA-21.24, CA-21.27

#### Data Layer — Esquema

- [x] **Agregar campo `sortOrder` a `PlanAssignmentEntity`** (CA-21.24)
  - [x] Agregar `@ColumnInfo(name = "sort_order") val sortOrder: Int = 0` al data class, después del campo `reps`
  - [x] Archivo: `data/local/entity/PlanAssignmentEntity.kt`
  - [x] Default Kotlin `= 0` permite compilación sin cambiar callers existentes

- [x] **Bump versión de base de datos a 8** (CA-21.24)
  - [x] Cambiar `version = 7` → `version = 8` en anotación `@Database`
  - [x] Archivo: `data/local/database/TensionDatabase.kt`

#### Data Layer — Migración

- [x] **Crear `MIGRATION_7_8` en `Migrations.kt`** (CA-21.01–CA-21.16, CA-21.24, CA-21.27)
  - [x] Agregar `val MIGRATION_7_8 = object : Migration(7, 8)` con override de `migrate(db)`
  - [x] Archivo: `data/local/database/Migrations.kt`
  - [x] Paso 1 — DDL: `ALTER TABLE plan_assignment ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0`
  - [x] Paso 2 — Limpiar: `DELETE FROM plan_assignment` (elimina las 93 asignaciones actuales)
  - [x] Paso 3 — Insertar 82 nuevas asignaciones con `sort_order`. Formato: `INSERT INTO plan_assignment (module_version_id, exercise_id, sets, reps, sort_order) VALUES (mv, eid, 4, 'reps', sortOrder)`. Mapeo completo según §5:
    - A-V1 (mv=1, 12 filas): 1→eid10, 2→eid8, 3→eid9, 4→eid26, 5→eid16, 6→eid18, 7→eid17, 8→eid19, 9→eid11, 10→eid12, 11→eid13, 12→eid14
    - A-V2 (mv=2, 11 filas): 1→eid10, 2→eid8, 3→eid9, 4→eid26, 5→eid16, 6→eid18, 7→eid17, 8→eid20, 9→eid11, 10→eid14, 11→eid15
    - A-V3 (mv=3, 11 filas): 1→eid10, 2→eid8, 3→eid9, 4→eid26, 5→eid16, 6→eid19, 7→eid17, 8→eid20, 9→eid11, 10→eid13, 11→eid14
    - B-V1 (mv=4, 8 filas): 1→eid1, 2→eid3, 3→eid6, 4→eid27, 5→eid25, 6→eid24, 7→eid22, 8→eid23
    - B-V2 (mv=5, 8 filas): 1→eid1, 2→eid7, 3→eid5, 4→eid27, 5→eid25, 6→eid29, 7→eid21, 8→eid23
    - B-V3 (mv=6, 8 filas): 1→eid1, 2→eid2, 3→eid4(TO_TECHNICAL_FAILURE), 4→eid27, 5→eid24, 6→eid28, 7→eid21, 8→eid22
    - C-V1 (mv=7, 8 filas): 1→eid39, 2→eid43, 3→eid30, 4→eid31, 5→eid35, 6→eid32, 7→eid33, 8→eid34
    - C-V2 (mv=8, 8 filas): 1→eid36, 2→eid38, 3→eid43, 4→eid30, 5→eid31, 6→eid41, 7→eid33, 8→eid34
    - C-V3 (mv=9, 8 filas): 1→eid38, 2→eid40, 3→eid37, 4→eid30, 5→eid31, 6→eid32, 7→eid33, 8→eid34

- [x] **Registrar `MIGRATION_7_8` en `DatabaseModule`** (CA-21.24)
  - [x] Cambiar `.addMigrations(Migrations.MIGRATION_6_7)` → `.addMigrations(Migrations.MIGRATION_6_7, Migrations.MIGRATION_7_8)`
  - [x] Archivo: `di/DatabaseModule.kt`

### Fase 1: Seed Data

**ACs vinculados:** CA-21.01–CA-21.16, CA-21.24

- [x] **Reescribir `PlanSeeder` con 82 asignaciones y sort_order**
  - [x] Actualizar helper `pa()`: firma nueva `pa(db, moduleVersionId, exerciseId, reps, sortOrder)`
  - [x] Agregar `put("sort_order", sortOrder)` al ContentValues dentro del helper
  - [x] Reescribir `seedPlanAssignments()` con las 82 nuevas asignaciones — mismos exercise_id, reps y sort_order que `MIGRATION_7_8` (coherencia total obligatoria)
  - [x] Actualizar comentarios de conteo: A-V1=12, A-V2=11, A-V3=11, B×3=8, C×3=8
  - [x] Archivo: `data/local/seed/PlanSeeder.kt`

### Fase 2: Queries de Orden y Repositorio

**ACs vinculados:** CA-21.24, CA-21.25

#### Data Layer — DAOs

- [x] **Cambiar ORDER BY en `PlanAssignmentDao.getDetailsByModuleVersionId()`**
  - [x] Reemplazar `ORDER BY e.name ASC` → `ORDER BY pa.sort_order ASC` (la query ya hace INNER JOIN a `plan_assignment pa`)
  - [x] Archivo: `data/local/dao/PlanAssignmentDao.kt`

- [x] **Agregar query `getMaxSortOrder()` a `PlanAssignmentDao`**
  - [x] `@Query("SELECT MAX(sort_order) FROM plan_assignment WHERE module_version_id = :moduleVersionId") suspend fun getMaxSortOrder(moduleVersionId: Long): Int?`
  - [x] Archivo: `data/local/dao/PlanAssignmentDao.kt`

- [x] **Cambiar ORDER BY en `SessionExerciseDao.getBySessionIdWithDetails()`**
  - [x] La query ya tiene `INNER JOIN session s ON se.session_id = s.id` (confirmado)
  - [x] Reemplazar `ORDER BY e.name ASC` → subconsulta COALESCE con fallback 9999:
    ```sql
    ORDER BY COALESCE(
      (SELECT pa2.sort_order FROM plan_assignment pa2
       WHERE pa2.module_version_id = s.module_version_id
       AND pa2.exercise_id = COALESCE(se.original_exercise_id, se.exercise_id)),
      9999
    ) ASC
    ```
  - [x] Archivo: `data/local/dao/SessionExerciseDao.kt`

- [x] **Cambiar ORDER BY en `SessionExerciseDao.getExercisesForSummary()`**
  - [x] Agregar `INNER JOIN session s ON se.session_id = s.id` al FROM (actualmente ausente — hallazgo auditoría)
  - [x] Reemplazar `ORDER BY e.name ASC` → subconsulta COALESCE idéntica a `getBySessionIdWithDetails()`
  - [x] Archivo: `data/local/dao/SessionExerciseDao.kt`

- [x] **Cambiar ORDER BY en `SessionExerciseDao.getExercisesForSessionDetail()`**
  - [x] Agregar `INNER JOIN session s ON se.session_id = s.id` al FROM (actualmente ausente — hallazgo auditoría)
  - [x] Reemplazar `ORDER BY e.name ASC` → subconsulta COALESCE idéntica a `getBySessionIdWithDetails()`
  - [x] Archivo: `data/local/dao/SessionExerciseDao.kt`

#### Data Layer — Repositorio

- [x] **Actualizar `PlanRepositoryImpl.assignExercise()` para computar sort_order**
  - [x] Antes del insert: `val nextSortOrder = (planAssignmentDao.getMaxSortOrder(moduleVersionId) ?: 0) + 1`
  - [x] Pasar `sortOrder = nextSortOrder` al constructor de `PlanAssignmentEntity`
  - [x] Nota: usar `?: 0` (no `?: -1`) para que primera asignación manual obtenga sort_order = 1 (1-based, consistente con seed data)
  - [x] Archivo: `data/repository/PlanRepositoryImpl.kt`

### Fase 3: Indicador Visual UI

**ACs vinculados:** CA-21.05, CA-21.26

#### UI Layer — Modelos y ViewModels

- [x] **Agregar `isBodyweight` a `PlanExerciseItem`**
  - [x] Agregar `val isBodyweight: Boolean = false` al data class `PlanExerciseItem`
  - [x] Archivo: `ui/catalog/PlanVersionDetailUiState.kt`

- [x] **Mapear `isBodyweight` en `PlanVersionDetailViewModel`**
  - [x] Agregar `isBodyweight = pe.isBodyweight` al constructor de `PlanExerciseItem` dentro del `.map {}`
  - [x] Archivo: `ui/catalog/PlanVersionDetailViewModel.kt`

- [x] **Agregar string resource**
  - [x] `<string name="exercise_outside_gym">Fuera del gym</string>`
  - [x] Archivo: `res/values/strings.xml`

#### UI Layer — Composables

- [x] **Agregar indicador "fuera del gym" en `PlanVersionDetailScreen`** (CA-21.05, CA-21.26)
  - [x] En composable `PlanExerciseList`, dentro del `ListItem`:
    - Si `moduleCode == "A" && exercise.isBodyweight` → mostrar badge Surface con `stringResource(R.string.exercise_outside_gym)`, color `secondaryContainer`, `RoundedCornerShape(4.dp)` (patrón badge `isCustom`)
  - [x] Propagar `moduleCode: String` como parámetro al composable `PlanExerciseList` desde el Scaffold
  - [x] Archivo: `ui/catalog/PlanVersionDetailScreen.kt`

- [x] **Agregar indicador "fuera del gym" en `ActiveSessionScreen`** (CA-21.05, CA-21.26)
  - [x] Propagar `moduleCode: String` como parámetro a `ExerciseRow` y sus 3 sub-composables (`NotStartedExerciseRow`, `InProgressExerciseRow`, `CompletedExerciseRow`)
  - [x] En cada sub-composable: si `moduleCode == "A" && exercise.isBodyweight` → mostrar badge `R.string.exercise_outside_gym` con estilo sutil (Surface + `secondaryContainer` o `FontStyle.Italic`)
  - [x] Archivo: `ui/session/ActiveSessionScreen.kt`

### Fase 4: Testing

**ACs vinculados:** CA-21.24, CA-21.27

- [x] **Crear `MigrationV7ToV8Test.kt`** (CA-21.24, CA-21.27)
  - [x] Crear clase siguiendo patrón de `MigrationV6ToV7Test.kt` — Room in-memory DB, `PrepopulateCallback`, `.addMigrations(Migrations.MIGRATION_6_7, Migrations.MIGRATION_7_8)`
  - [x] Archivo: `androidTest/.../MigrationV7ToV8Test.kt`
  - [x] Test 1: `assignmentCountByVersion_isCorrect` — conteos por module_version_id: 1→12, 2→11, 3→11, 4→8, 5→8, 6→8, 7→8, 8→8, 9→8, suma total = 82
  - [x] Test 2: `sortOrderIsSequential_perVersion` — para cada module_version_id (1..9), verificar que sort_order va de 1 a N sin gaps ni duplicados
  - [x] Test 3: `sortOrderColumn_exists` — `PRAGMA table_info(plan_assignment)` verifica que columna `sort_order` existe con tipo `INTEGER` y NOT NULL
  - [x] Test 4: `planAssignmentReferentialIntegrity_isValid` — todos los exercise_id asignados al module_version del módulo correcto
  - [x] Test 5: `historicalTablesExist_andAreAccessible` — `session`, `session_exercise`, `exercise_set` no fueron afectados por la migración
  - [x] Test 6: `exercise26InAllModuleAVersions` — exercise_id=26 aparece en module_version_id 1, 2 y 3 (validación específica de CA-21.01)
  - [x] Test 7: `exercise42RemovedFromAllModuleCVersions` — exercise_id=42 NO aparece en module_version_id 7, 8 ni 9 (validación de CA-21.16)

### Fase 5: QA y Deployment

- [ ] **Ejecutar Agente Peer Review** (MANUAL)
- [ ] **Resolver incidentes del Peer Review** (MANUAL, condicional)
- [ ] **Crear Pull Request** (MANUAL)
- [ ] **Ejecutar pipeline deployment DEV** (MANUAL)
- [ ] **Diseñar y ejecutar pruebas manuales** (MANUAL):
  - Verificar orden de ejercicios en vista de plan (pantalla D4) para cada módulo/versión
  - Verificar orden de ejercicios en sesión activa (pantalla E1) respeta orden sugerido
  - Verificar orden de ejercicios en resumen de sesión (E5) y detalle de sesión histórica (F2)
  - Verificar indicador "Fuera del gym" en ejercicios bodyweight (abdomen) del Módulo A — tanto en plan como en sesión
  - Verificar creación de nueva sesión post-migración con conteo correcto (12/11/11 para A, 8/8/8 para B, 8/8/8 para C)
  - Verificar que sesión existente en progreso se completa correctamente post-migración
  - Verificar que sesiones históricas completadas/incompletas no fueron alteradas
  - Verificar que ejercicios sustituidos heredan la posición del original en el orden

- [ ] **Actualizar `Modelo de Datos.md`** (MANUAL):
  - Agregar columna `sort_order INTEGER NOT NULL DEFAULT 0` a tabla `plan_assignment` en diagrama ER
  - Actualizar seed data de 93 → 82 filas con nueva distribución
  - Actualizar ejemplo A-V1 de 11 → 12 ejercicios
  - Actualizar cardinalidad en relación module_version → plan_assignment a "8-12 ejercicios"

---

### Vinculación CAs → Fases

| CA | Fase(s) | Mecanismo |
|----|---------|-----------|
| CA-21.01 | 0, 1, 4 | exercise_id=26 en A-V1 sort_order=4 (migración + seeder + test `exercise26InAllModuleAVersions`) |
| CA-21.02 | 0, 1 | A-V1 (mv=1): 12 INSERTs con sort_order 1-12 |
| CA-21.03 | 0, 1 | A-V2 (mv=2): 11 INSERTs con sort_order 1-11 |
| CA-21.04 | 0, 1 | A-V3 (mv=3): 11 INSERTs con sort_order 1-11 |
| CA-21.05 | 3 | Badge "Fuera del gym" derivado de `isBodyweight + moduleCode == "A"` |
| CA-21.06 | 0, 1 | Distribución verificada en mapeo 82 asignaciones |
| CA-21.07 | 0, 1 | B-V1/V2/V3 (mv=4/5/6): 8 INSERTs cada una |
| CA-21.08 | 0, 1 | B-V1 (mv=4): 8 INSERTs con sort_order 1-8 |
| CA-21.09 | 0, 1 | B-V2 (mv=5): 8 INSERTs con sort_order 1-8 |
| CA-21.10 | 0, 1 | B-V3 (mv=6): 8 INSERTs con sort_order 1-8 |
| CA-21.11 | 0, 1 | Cobertura completa del Diccionario B verificada |
| CA-21.12 | 0, 1 | C-V1/V2/V3 (mv=7/8/9): 8 INSERTs cada una |
| CA-21.13 | 0, 1 | C-V1 (mv=7): 8 INSERTs con sort_order 1-8 |
| CA-21.14 | 0, 1 | C-V2 (mv=8): 8 INSERTs con sort_order 1-8 |
| CA-21.15 | 0, 1 | C-V3 (mv=9): 8 INSERTs con sort_order 1-8 |
| CA-21.16 | 0, 1, 4 | exercise_id=42 NO en C×3 (test `exercise42RemovedFromAllModuleCVersions`). Avanzada permanece en Diccionario |
| CA-21.17 | 0, 2 | sort_order en seed data es informativo — no hay restricción de UI que fuerce el orden |
| CA-21.18 | 0, 2 | A: sort_order 1-4 espalda, 5-8 bíceps, 9-12 abdomen |
| CA-21.19 | 0, 2 | B: sort_order 1-3 pecho, 4-6 hombro, 7-8 tríceps |
| CA-21.20 | 0, 2 | C: sort_order según prioridad biomecánica |
| CA-21.21–23 | — | ✅ Completados por el PO — no requieren implementación |
| CA-21.24 | 0, 1, 4 | Migración + seeder + test instrumentado completo |
| CA-21.25 | 2 | ORDER BY `pa.sort_order ASC` en `PlanAssignmentDao`. COALESCE subconsulta en 3 queries de `SessionExerciseDao` |
| CA-21.26 | 3 | Badge "Fuera del gym" en `PlanVersionDetailScreen` y `ActiveSessionScreen` |
| CA-21.27 | 0, 4 | MIGRATION_7_8 no toca `session`/`session_exercise`/`exercise_set`. Test `historicalTablesExist_andAreAccessible` |
