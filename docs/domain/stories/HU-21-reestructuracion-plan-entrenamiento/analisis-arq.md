# Análisis Arquitectónico — Historia #21

## §4 Dependencias Técnicas e Integración

### Componentes Afectados

| Componente | Tipo de cambio | Descripción |
|---|---|---|
| `PlanAssignmentEntity` | Modificación Menor | Agregar campo `sortOrder: Int = 0` con `@ColumnInfo(name = "sort_order")`. Default Kotlin = 0 para backward compatibility |
| `TensionDatabase` | Modificación Menor | Bump `version = 7` → `version = 8` |
| `Migrations.kt` | Modificación Mayor | Agregar `MIGRATION_7_8`: ALTER TABLE + DELETE 93 asignaciones + INSERT 82 nuevas con sort_order |
| `DatabaseModule` | Modificación Menor | Registrar `MIGRATION_7_8` en cadena `.addMigrations()` |
| `PlanSeeder` | Modificación Mayor | Reescribir `seedPlanAssignments()` con 82 asignaciones. Helper `pa()` extendido con parámetro `sortOrder` |
| `PlanAssignmentDao` | Modificación Menor | `ORDER BY e.name ASC` → `ORDER BY pa.sort_order ASC`. Agregar `getMaxSortOrder(moduleVersionId)` |
| `SessionExerciseDao` | Modificación Moderada | 3 queries con `ORDER BY e.name ASC` → subconsulta COALESCE con fallback 9999. Agregar `INNER JOIN session s` donde faltaba |
| `PlanRepositoryImpl` | Modificación Menor | `assignExercise()`: computar `nextSortOrder = (getMaxSortOrder() ?: 0) + 1` antes de insert |
| `PlanExerciseItem` | Modificación Menor | Agregar `isBodyweight: Boolean = false` |
| `PlanVersionDetailViewModel` | Modificación Menor | Mapear `isBodyweight = pe.isBodyweight` |
| `PlanVersionDetailScreen` | Modificación Menor | Badge "Fuera del gym" para bodyweight en Módulo A |
| `ActiveSessionScreen` | Modificación Menor | Badge "Fuera del gym" en 3 sub-composables + propagación de `moduleCode` |
| `strings.xml` | Modificación Menor | Agregar `exercise_outside_gym` = "Fuera del gym" |

### Verificación de código real (paso 1.5)

- **`PlanAssignmentEntity.kt`:** Confirmado 4 campos: `moduleVersionId`, `exerciseId`, `sets`, `reps` — sin `sort_order`. PK compuesto `(module_version_id, exercise_id)`.
- **`PlanAssignmentDao.kt`:** `getDetailsByModuleVersionId()` usa `ORDER BY e.name ASC`. Hace INNER JOIN a `plan_assignment pa` — `pa.sort_order` accesible tras migración. DTO `PlanAssignmentWithExerciseDetails` incluye `isBodyweight: Int` y `moduleCode: String`.
- **`SessionExerciseDao.kt`:** 3 queries con `ORDER BY e.name ASC` (líneas 127, 243, 324). `getBySessionIdWithDetails()` ya tiene LEFT JOIN a `plan_assignment pa` y `INNER JOIN session s`. `getExercisesForSummary()` y `getExercisesForSessionDetail()` **no tienen JOIN a `session`** — deben agregarlo.
- **`PlanSeeder.kt`:** 93 asignaciones actuales (A: 33, B: 33, C: 27). Helper `pa(db, moduleVersionId, exerciseId, reps)`. `seedModuleVersions()` inserta 9 module_versions (IDs 1-9) sin cambios.
- **`Migrations.kt`:** Solo contiene `MIGRATION_6_7` (HU-16). Patrón establecido: DELETE FROM plan_assignment + INSERT nuevos registros en `migrate()` override.
- **`TensionDatabase.kt`:** Confirmado `version = 7`. 16 entidades registradas, incluye `PlanAssignmentEntity`.
- **`DatabaseModule.kt`:** Builder chain con `.addMigrations(Migrations.MIGRATION_6_7).fallbackToDestructiveMigration()`. Se agrega `MIGRATION_7_8`.
- **`ExerciseSeeder.kt`:** "Elevación de hombros con mancuernas" ya existe con `id = 26`, `module_code = "A"`. No se requieren ejercicios nuevos.
- **`SessionRepositoryImpl.kt`:** `createSession()` (línea 131) usa `planAssignmentDao.getByModuleVersionId(moduleVersionId).first()`. La query `getByModuleVersionId()` no tiene ORDER BY — el orden no afecta la creación de la sesión. Nuevas sesiones post-migración usan automáticamente las 82 nuevas asignaciones.
- **`PlanVersionDetailUiState.kt`:** `PlanExerciseItem` tiene: `exerciseId`, `name`, `equipmentTypeName`, `muscleZonesSummary`, `sets`, `repsDisplay`, `isSpecialCondition`, `isCustom` — **no incluye** `isBodyweight`. Requiere agregar el campo.
- **`ActiveSessionUiState`:** `ExerciseUiItem` ya tiene `isBodyweight: Boolean` y `ActiveSessionUiState` tiene `moduleCode: String` — no requiere cambios de modelo.

### Análisis de performance

La subconsulta COALESCE en ORDER BY opera sobre una tabla de tamaño acotado (~82 filas, 9 module_versions × ~9 ejercicios promedio). El índice existente en `exercise_id` optimiza la subconsulta. No se espera crecimiento exponencial — la tabla `plan_assignment` no escala con el historial del ejecutante.

---

## §5 Análisis Arquitectónico

### Patrón y Justificación

**Patrón:** Migración DDL + Data con propagación de orden vía columna `sort_order` en `plan_assignment` — sin duplicación en `session_exercise`.

**Justificación:** La tabla `plan_assignment` actualmente tiene 4 columnas sin mecanismo para persistir el orden. Las queries de presentación ordenan `ORDER BY e.name ASC` (alfabético), incompatible con el orden biomecánico requerido. La solución es agregar `sort_order INTEGER NOT NULL DEFAULT 0` a `plan_assignment` mediante `MIGRATION_7_8`, reescribir las 93 asignaciones a 82 nuevas con composiciones y orden correctos, y cambiar las cláusulas ORDER BY en todos los DAOs de presentación.

Se mantiene `sort_order` únicamente en `plan_assignment` (no en `session_exercise`) porque: (1) el plan es la fuente de verdad del orden sugerido, (2) las queries de sesión ya hacen LEFT JOIN a `plan_assignment` — el `sort_order` es accesible sin esquema adicional, (3) ejercicios sustituidos pueden resolver su posición vía subconsulta sobre `original_exercise_id`, y (4) se evita redundancia de datos y una segunda migración DDL.

**Alternativa descartada — sort_order en `session_exercise`:** Requeriría ALTER TABLE adicional, lógica en `createSession()` para copiar sort_order, y generaría desincronización si el plan cambia en el futuro. Mayor complejidad sin beneficio funcional.

### Mapeo completo de las 82 asignaciones (sort_order : exercise_id, reps)

**Módulo A — Pull + Abs:**

```
A-V1 (mv=1, 12 ejercicios):
  1:10(8-12), 2:8(8-12), 3:9(8-12), 4:26(8-12),
  5:16(8-12), 6:18(8-12), 7:17(8-12), 8:19(8-12),
  9:11(8-12), 10:12(8-12), 11:13(8-12), 12:14(30-45_SEC)

A-V2 (mv=2, 11 ejercicios):
  1:10(8-12), 2:8(8-12), 3:9(8-12), 4:26(8-12),
  5:16(8-12), 6:18(8-12), 7:17(8-12), 8:20(8-12),
  9:11(8-12), 10:14(30-45_SEC), 11:15(30-45_SEC)

A-V3 (mv=3, 11 ejercicios):
  1:10(8-12), 2:8(8-12), 3:9(8-12), 4:26(8-12),
  5:16(8-12), 6:19(8-12), 7:17(8-12), 8:20(8-12),
  9:11(8-12), 10:13(8-12), 11:14(30-45_SEC)
```

**Módulo B — Push:**

```
B-V1 (mv=4, 8 ejercicios):
  1:1(8-12), 2:3(8-12), 3:6(8-12), 4:27(8-12),
  5:25(8-12), 6:24(8-12), 7:22(8-12), 8:23(8-12)

B-V2 (mv=5, 8 ejercicios):
  1:1(8-12), 2:7(8-12), 3:5(8-12), 4:27(8-12),
  5:25(8-12), 6:29(8-12), 7:21(8-12), 8:23(8-12)

B-V3 (mv=6, 8 ejercicios):
  1:1(8-12), 2:2(8-12), 3:4(TO_TECHNICAL_FAILURE), 4:27(8-12),
  5:24(8-12), 6:28(8-12), 7:21(8-12), 8:22(8-12)
```

**Módulo C — Pierna:**

```
C-V1 (mv=7, 8 ejercicios):
  1:39(8-12), 2:43(8-12), 3:30(8-12), 4:31(8-12),
  5:35(8-12), 6:32(8-12), 7:33(8-12), 8:34(8-12)

C-V2 (mv=8, 8 ejercicios):
  1:36(8-12), 2:38(8-12), 3:43(8-12), 4:30(8-12),
  5:31(8-12), 6:41(8-12), 7:33(8-12), 8:34(8-12)

C-V3 (mv=9, 8 ejercicios):
  1:38(8-12), 2:40(8-12), 3:37(8-12), 4:30(8-12),
  5:31(8-12), 6:32(8-12), 7:33(8-12), 8:34(8-12)
```

**Total: 82 asignaciones.** Antes: 93. Delta: A +1 (V1: 11→12), B −9 (3×11→3×8), C −3 (3×9→3×8).

**Reps especiales:** exercise_id 14 y 15 → `'30-45_SEC'`, exercise_id 4 en B-V3 → `'TO_TECHNICAL_FAILURE'`, todos los demás → `'8-12'`.

### Estrategia ORDER BY para sesiones con sustituciones

```sql
ORDER BY COALESCE(
  (SELECT pa2.sort_order FROM plan_assignment pa2
   WHERE pa2.module_version_id = s.module_version_id
   AND pa2.exercise_id = COALESCE(se.original_exercise_id, se.exercise_id)),
  9999
) ASC
```

- **Ejercicio no sustituido** (`original_exercise_id` IS NULL): `COALESCE(NULL, se.exercise_id) = se.exercise_id` → encuentra sort_order.
- **Ejercicio sustituido**: `COALESCE(original_id, se.exercise_id) = original_id` → hereda la posición del ejercicio original.
- **Ejercicio sin plan_assignment** (removido del plan o custom): subconsulta retorna NULL → COALESCE devuelve 9999 → aparece al final.

**Prerequisito por query:** `getBySessionIdWithDetails()` ya tiene `INNER JOIN session s`. `getExercisesForSummary()` y `getExercisesForSessionDetail()` necesitan agregar `INNER JOIN session s ON se.session_id = s.id` (hallazgo de auditoría del Arquitecto).

### Indicador "fuera del gym" — derivado sin backend adicional

La lógica `moduleCode == "A" && isBodyweight == true` identifica exactamente los ejercicios de abdomen del Módulo A (IDs 11-15). Estos son los únicos ejercicios con `is_bodyweight = true` en el Módulo A. No se requiere una nueva columna ni flag adicional en la entidad.

### Notas Técnicas

**Nota 1 — Default Kotlin en `PlanAssignmentEntity.sortOrder`.**

El campo debe declararse como `val sortOrder: Int = 0` para que el código existente en `PlanRepositoryImpl.assignExercise()` compile sin cambios. El default `0` es coherente: al actualizar el campo para computar `nextSortOrder = (getMaxSortOrder() ?: 0) + 1`, la primera asignación manual en una versión vacía obtendrá `sort_order = 1` (1-based, consistente con el seed data).

**Nota 2 — `PlanRepositoryImpl.assignExercise()`: fix del off-by-one.**

Usar `?: 0` (no `?: -1`) para que primera asignación manual en versión vacía produzca `sort_order = 1`, consistente con el seed data (1-based). Si se usara `?: -1`, la primera asignación manual obtendría `sort_order = 0` (0-based) — inconsistente con el seed data que arranca en 1.

**Nota 3 — Estrategia de manejo de sesión en progreso (CA-21.27).**

La migración `MIGRATION_7_8` solo modifica `plan_assignment`. No toca `session`, `session_exercise`, ni `exercise_set`. Los ejercicios removidos del plan (ej: Avanzada de Zancadas, exercise_id=42) obtienen sort_order NULL → fallback 9999 → se posicionan al final. Comportamiento aceptable: la sesión es completable, solo cambia el orden visual de esos ejercicios.

**Nota 4 — Auditoría post-análisis: 3 defectos identificados y corregidos.**

(1) Queries `getExercisesForSummary()` y `getExercisesForSessionDetail()` carecen de JOIN a `session` para la subconsulta de sort_order — agregado `INNER JOIN session s ON se.session_id = s.id`.
(2) Componente omitido `PlanRepositoryImpl.assignExercise()` necesita manejar sort_order — computar `nextSortOrder` antes de insert.
(3) `PlanAssignmentEntity.sortOrder` requiere default Kotlin `= 0` para compilación sin cambiar callers.

### Hallazgos del Análisis de Código

**Propagación de `isBodyweight` a UI del plan:** `PlanExerciseItem` en `PlanVersionDetailUiState.kt` **no incluye** `isBodyweight` — solo tiene: `exerciseId`, `name`, `equipmentTypeName`, `muscleZonesSummary`, `sets`, `repsDisplay`, `isSpecialCondition`, `isCustom`. La propagación requiere agregar `isBodyweight: Boolean` a `PlanExerciseItem` y mapearlo en `PlanVersionDetailViewModel`. Para `ActiveSessionScreen`, `ExerciseUiItem` ya tiene `isBodyweight: Boolean` — no requiere cambios de modelo.

**Verificación de las 82 asignaciones contra CAs:** Se validó cada assignment del mapeo del Arquitecto contra las tablas de composición definitiva de los CA-21.02 a CA-21.15. Resultado: ✅ 82/82 asignaciones correctas.

**Verificación del estado actual del seeder vs. HU-21:**
- A-V1 actual: 11 ejercicios (sin exercise_id=26) → HU-21: 12 ejercicios (+exercise_id=26). ✅
- A-V2/V3 actual: 11 ejercicios (con exercise_id=26) → HU-21: 11 ejercicios (sin cambio de composición, solo reordenamiento). ✅
- B-V1/V2/V3 actual: 11 ejercicios → HU-21: 8 ejercicios (-3 por versión). ✅
- C-V1/V2/V3 actual: 9 ejercicios (con exercise_id=42) → HU-21: 8 ejercicios (-1 por versión, removido exercise_id=42). ✅
- Delta total: 93 → 82 = -11 asignaciones. ✅
