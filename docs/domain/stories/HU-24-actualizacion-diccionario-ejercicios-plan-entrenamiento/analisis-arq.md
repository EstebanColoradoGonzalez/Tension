## Análisis Arquitectónico

> Esta historia es una historia de **datos puros** — no cambia lógica de negocio, UI (salvo 1 método de carga de imágenes), navegación ni reglas de dominio. El esquema de tablas no cambia (las 18 entidades de v9 se preservan con exactamente las mismas columnas y constraints).

**Patrón arquitectónico:** Migración DDL + Data (MIGRATION_9_10) + Rewrite completo de Seeders + Asset cleanup. No introduce patrón arquitectónico nuevo — la misma separación de 4 capas MVVM + Domain Layer (ADR-05) se mantiene intacta. Los cambios se concentran exclusivamente en la capa Data: migración SQL/Kotlin, seeders de datos precargados y assets de imágenes.

### Componentes afectados

#### Bloque A — MIGRATION_9_10 en `Migrations.kt` (Nuevo)

- Nivel de cambio: Crítico
- Ubicación: `data/local/database/Migrations.kt`

**Fase 1 — Equipment types nuevos (CA-24.14):**

```sql
INSERT INTO equipment_type (id, name) VALUES (10, 'Mancuernas o Polea');
INSERT INTO equipment_type (id, name) VALUES (11, 'Polea con Cuerda');
INSERT INTO equipment_type (id, name) VALUES (12, 'Polea con Cuerda o Polea con Barra en V');
INSERT INTO equipment_type (id, name) VALUES (13, 'Barra');
INSERT INTO equipment_type (id, name) VALUES (14, 'Mancuerna o Polea o Barra');
INSERT INTO equipment_type (id, name) VALUES (15, 'Barra o Mancuernas');
```

**Fase 2 — Completar deload activo si existe (CA-24.28):**

```sql
UPDATE deload SET status = 'COMPLETED', completion_date = date('now')
WHERE status = 'ACTIVE';
```

Se ejecuta ANTES de tocar rutinas/versiones para evitar inconsistencias en `deload_frozen_version` que referencia `routine_id`.

**Fase 3 — Cerrar sesión activa si referencia versión que será eliminada (CA-24.22):**

```sql
UPDATE session SET status = 'COMPLETED'
WHERE status = 'IN_PROGRESS'
  AND routine_version_id IN (
    SELECT id FROM routine_version WHERE routine_id IN (1, 2, 3)
  );
```

Si la sesión activa pertenece a una rutina del usuario (ID > 3), no se toca. Si pertenece a una rutina seed (1, 2, 3) cuya versión será eliminada, se cierra automáticamente.

**Fase 4 — Limpieza de plan y rutinas originales (CA-24.17, CA-24.18, CA-24.19, CA-24.27):**

```sql
-- Eliminar asignaciones de versiones de rutinas seed
DELETE FROM plan_assignment
WHERE routine_version_id IN (
  SELECT id FROM routine_version WHERE routine_id IN (1, 2, 3)
);

-- Eliminar deload_frozen_version de rutinas seed
DELETE FROM deload_frozen_version WHERE routine_id IN (1, 2, 3);

-- Eliminar routine_current_version de rutinas seed
DELETE FROM routine_current_version WHERE routine_id IN (1, 2, 3);

-- Eliminar alertas de rutinas seed (CA-24.27)
DELETE FROM alert WHERE routine_id IN (1, 2, 3);

-- Eliminar routine_versions que NO tienen sesiones asociadas
DELETE FROM routine_version
WHERE routine_id IN (1, 2, 3)
  AND id NOT IN (SELECT DISTINCT routine_version_id FROM session);

-- Eliminar rutinas seed SOLO si no retienen versiones por historial
DELETE FROM routine WHERE id IN (1, 2, 3)
  AND id NOT IN (SELECT DISTINCT routine_id FROM routine_version);
```

**Diseño clave — Preservación de versiones con historial (CA-24.25):** Si una `routine_version` con `routine_id IN (1, 2, 3)` tiene sesiones históricas, NO se elimina. La rutina padre tampoco se elimina (FK constraint). Las nuevas rutinas se crean con IDs dinámicos (MAX+1), evitando colisiones. Las rutinas legacy quedan como "huérfanas" — sin `routine_current_version`, sin `plan_assignment`, sin alertas — pero sus versiones preservan la integridad referencial de `session.routine_version_id`.

**Fase 5 — Ejercicios: eliminar obsoletos, preservar con historial (CA-24.15, CA-24.23, CA-24.26):**

```sql
-- Marcar como custom los ejercicios precargados que tienen historial
-- (incluye tanto exercise_id como original_exercise_id para cubrir
-- ejercicios que fueron sustituidos en sesión — FK RESTRICT en ambos)
UPDATE exercise SET is_custom = 1
WHERE is_custom = 0
  AND id IN (
    SELECT DISTINCT se.exercise_id FROM session_exercise se
    UNION
    SELECT DISTINCT se.original_exercise_id FROM session_exercise se
    WHERE se.original_exercise_id IS NOT NULL
  );

-- Eliminar alertas vinculadas a ejercicios sin historial (CA-24.27)
-- (FK RESTRICT en alert.exercise_id → exercise.id impide eliminar
-- ejercicios que tengan alertas sin limpiarlas primero)
DELETE FROM alert
WHERE exercise_id IS NOT NULL
  AND exercise_id IN (
    SELECT id FROM exercise
    WHERE is_custom = 0
      AND id NOT IN (SELECT DISTINCT exercise_id FROM session_exercise)
      AND id NOT IN (
        SELECT DISTINCT original_exercise_id FROM session_exercise
        WHERE original_exercise_id IS NOT NULL
      )
  );

-- Eliminar exercise_progression de ejercicios sin historial (CA-24.26)
DELETE FROM exercise_progression
WHERE exercise_id IN (
  SELECT id FROM exercise
  WHERE is_custom = 0
    AND id NOT IN (SELECT DISTINCT exercise_id FROM session_exercise)
    AND id NOT IN (
      SELECT DISTINCT original_exercise_id FROM session_exercise
      WHERE original_exercise_id IS NOT NULL
    )
);

-- Eliminar exercise_muscle_zone de ejercicios sin historial
DELETE FROM exercise_muscle_zone
WHERE exercise_id IN (
  SELECT id FROM exercise
  WHERE is_custom = 0
    AND id NOT IN (SELECT DISTINCT exercise_id FROM session_exercise)
    AND id NOT IN (
      SELECT DISTINCT original_exercise_id FROM session_exercise
      WHERE original_exercise_id IS NOT NULL
    )
);

-- Eliminar plan_assignment de ejercicios sin historial
-- (versiones seed ya limpiadas en Fase 4; esto cubre asignaciones
-- en rutinas del usuario que referencien ejercicios precargados)
DELETE FROM plan_assignment
WHERE exercise_id IN (
  SELECT id FROM exercise
  WHERE is_custom = 0
    AND id NOT IN (SELECT DISTINCT exercise_id FROM session_exercise)
    AND id NOT IN (
      SELECT DISTINCT original_exercise_id FROM session_exercise
      WHERE original_exercise_id IS NOT NULL
    )
);

-- Eliminar ejercicios precargados sin historial
DELETE FROM exercise
WHERE is_custom = 0
  AND id NOT IN (SELECT DISTINCT exercise_id FROM session_exercise)
  AND id NOT IN (
    SELECT DISTINCT original_exercise_id FROM session_exercise
    WHERE original_exercise_id IS NOT NULL
  );
```

**Fase 6 — Insertar nuevos ejercicios + plan (CA-24.03, CA-24.08–CA-24.11, CA-24.16, CA-24.20, CA-24.21):**

Esta fase se implementa en **Kotlin** (no SQL puro) dentro del método `migrate()` porque los IDs son dinámicos (MAX+1):

```kotlin
// Obtener MAX exercise ID para evitar colisiones (CA-24.16)
val maxExerciseId = db.query("SELECT COALESCE(MAX(id), 0) FROM exercise")
    .use { if (it.moveToFirst()) it.getLong(0) else 0L }
val baseExId = maxExerciseId + 1

// Insertar 26 ejercicios con IDs baseExId..baseExId+25
// Insertar 29 registros en exercise_muscle_zone (26 ejercicios, 3 con doble zona)

// Obtener MAX routine ID para nuevas rutinas
val maxRoutineId = db.query("SELECT COALESCE(MAX(id), 0) FROM routine")
    .use { if (it.moveToFirst()) it.getLong(0) else 0L }
val piernaId = maxRoutineId + 1
val pushId = maxRoutineId + 2
val pullId = maxRoutineId + 3

// Insertar 3 rutinas (CA-24.08 adaptado a IDs dinámicos)
// Insertar 4 routine_versions con IDs MAX+1..+4 (CA-24.09)
// Insertar 3 routine_current_version con version_number = 1 (CA-24.20)
// Insertar 27 plan_assignments (CA-24.11 con exercise IDs mapeados)

// Reiniciar rotación (CA-24.21)
db.execSQL("UPDATE rotation_state SET microcycle_position = 1 WHERE id = 1")
```

**Mapeo de IDs lógicos a dinámicos:** Los ejercicios se definen en CA-24.03 con IDs lógicos 1-26. En migración, se insertan con IDs `baseExId + (idLógico - 1)`. Las asignaciones del plan (CA-24.11) referencian IDs lógicos que deben traducirse a IDs reales usando este mismo offset. Ejemplo: CA-24.11 dice `exercise_id = 1` (Aductores) → en migración es `baseExId + 0`.

- **`TensionDatabase` (Modificación):** Incrementar `version` de 9 a 10. Entities, DAOs y TypeConverters no cambian. Nivel: Menor. Ubicación: `data/local/database/TensionDatabase.kt`

- **`DatabaseModule` (Modificación):** Agregar `Migrations.MIGRATION_9_10` a `.addMigrations(...)`. Nivel: Menor. Ubicación: `di/DatabaseModule.kt`

  ```kotlin
  // Antes:
  .addMigrations(Migrations.MIGRATION_6_7, Migrations.MIGRATION_7_8, Migrations.MIGRATION_8_9)
  // Después:
  .addMigrations(Migrations.MIGRATION_6_7, Migrations.MIGRATION_7_8, Migrations.MIGRATION_8_9, Migrations.MIGRATION_9_10)
  ```

#### Bloque B — `BaseDataSeeder` (Modificación)

- **`BaseDataSeeder.seedEquipmentTypes()` (Modificación):** Agregar 6 equipment types nuevos (IDs 10-15). Nivel: Menor. Ubicación: `data/local/seed/BaseDataSeeder.kt`

  ```kotlin
  insertEquipmentType(db, 10, "Mancuernas o Polea")
  insertEquipmentType(db, 11, "Polea con Cuerda")
  insertEquipmentType(db, 12, "Polea con Cuerda o Polea con Barra en V")
  insertEquipmentType(db, 13, "Barra")
  insertEquipmentType(db, 14, "Mancuerna o Polea o Barra")
  insertEquipmentType(db, 15, "Barra o Mancuernas")
  ```

#### Bloque C — `ExerciseSeeder` (Rewrite completo)

- **`ExerciseSeeder` (Modificación — Rewrite):** Eliminar 43 ejercicios actuales y reemplazar por 26 nuevos (IDs 1-26) según CA-24.03. Nivel: Crítico. Ubicación: `data/local/seed/ExerciseSeeder.kt`
  - `seedExercises()` inserta 26 ejercicios con IDs fijos 1-26 (para instalaciones nuevas no hay colisiones)
  - Todos con `is_custom = 0`, `is_bodyweight = 0`, `is_isometric = 0`, `is_to_technical_failure = 0` (CA-24.05, CA-24.06)
  - `media_resource` según convención CA-24.07 (snake_case del nombre + primer equipment type antes de "o")
  - Método helper `ex()` se mantiene igual (misma firma)
  - `seedExerciseMuscleZones()` reescrito: 26 entradas + 3 adicionales para ejercicios multi-zona (Peso Muerto Rumano → 11+15, Sentadilla Búlgara → 10+15, Sentadilla de Zumo → 10+12) (CA-24.04)

#### Bloque D — `PlanSeeder` (Rewrite completo)

- **`PlanSeeder` (Modificación — Rewrite):** Nivel: Crítico. Ubicación: `data/local/seed/PlanSeeder.kt`
  - `seedRoutines()`: 3 rutinas — Pierna (Leg) sort=1, Pecho/Hombro/Tríceps (Push) sort=2, Espalda/Bíceps/Abdomen (Pull) sort=3 (CA-24.08)
  - `seedRoutineVersions()`: 4 versiones — Pierna V1(id=1), Pierna V2(id=2), Push V1(id=3), Pull V1(id=4) (CA-24.09)
  - `seedRoutineCurrentVersions()`: 3 entradas con `current_version_number = 1` (CA-24.10)
  - `seedPlanAssignments()`: 27 asignaciones todas con `sets = 4`, `reps = '8-12'` (CA-24.11)
  - Eliminar constantes `FAILURE` y `R30_45` (ningún ejercicio del nuevo diccionario las usa)

#### Bloque E — `ExerciseDetailScreen.kt` — Carga de imágenes (Modificación)

- **`rememberExerciseBitmap()` (Modificación):** Nivel: Menor. Ubicación: `ui/catalog/ExerciseDetailScreen.kt`

  Cambio de:
  ```kotlin
  val assetDirs = listOf("exercises/module-a", "exercises/module-b", "exercises/module-c")
  for (dir in assetDirs) {
      try {
          val path = "$dir/$mediaResource.png"
          return@remember context.assets.open(path).use { stream ->
              BitmapFactory.decodeStream(stream)?.asImageBitmap()
          }
      } catch (_: Exception) { /* try next */ }
  }
  ```
  A:
  ```kotlin
  try {
      val path = "exercises/$mediaResource.png"
      return@remember context.assets.open(path).use { stream ->
          BitmapFactory.decodeStream(stream)?.asImageBitmap()
      }
  } catch (_: Exception) { /* no asset found */ }
  ```
  Se preserva el fallback a `File(mediaResource)` para imágenes de ejercicios custom.

#### Bloque F — Assets `assets/exercises/` (Operación de archivos)

- **Imágenes a eliminar (CA-24.07d):** 29 archivos `.png` correspondientes a ejercicios eliminados del diccionario.
- **Imágenes a renombrar (CA-24.07e):** 14 archivos `.png`. Se incluye `extension_de_cuadriceps_maquina.png` que no cambia de nombre.
- **Imágenes nuevas (CA-24.07f):** 12 archivos ya presentes en `assets/exercises/` con nombres correctos.
- **Riesgo identificado:** Los archivos `remo_t_inclinado.png` y `sentadilla_bulgara.png` están presentes pero sus nombres no coinciden con los esperados (`remo_t_inclinado_maquina.png` y `sentadilla_bulgara_mancuernas.png`). El Developer debe verificar si son las imágenes correctas que solo necesitan rename.
- **Estado final (CA-24.07g):** 26 archivos `.png` en `assets/exercises/`, uno por ejercicio precargado.

### Hitos de implementación

| Hito | Contenido | Dependencias |
| --- | --- | --- |
| 1 | MIGRATION_9_10 (6 fases SQL/Kotlin) + test instrumentado | Ninguna (bloqueante) |
| 2 | Seeders actualizados (BaseDataSeeder, ExerciseSeeder, PlanSeeder) + TensionDatabase v10 + DatabaseModule | Hito 1 |
| 3 | Assets (eliminar 29, renombrar 16, verificar 12 nuevas) | Ninguna (independiente) |
| 4 | UI — Corrección de `rememberExerciseBitmap()` | Hito 3 |

### Notas de auditoría

1. **FK `exercise.equipment_type_id → equipment_type(id)` ON DELETE RESTRICT:** Fase 1 inserta los 6 nuevos equipment types ANTES de que Fase 6 inserte ejercicios que los referencien. Orden correcto.
2. **FK `exercise_muscle_zone.exercise_id → exercise(id)` ON DELETE RESTRICT:** Fase 5 elimina `exercise_muscle_zone` ANTES de eliminar el ejercicio padre. Orden correcto.
3. **FK `plan_assignment.exercise_id → exercise(id)` ON DELETE RESTRICT:** Fase 4 limpia asignaciones de versiones seed. Fase 5 limpia asignaciones restantes de ejercicios sin historial. Ambas ANTES de eliminar ejercicios.
4. **FK `session_exercise.exercise_id` y `original_exercise_id → exercise(id)` ON DELETE RESTRICT:** Los ejercicios referenciados por cualquiera de las dos columnas NO se eliminan (se marcan `is_custom = 1`).
5. **FK `alert.exercise_id → exercise(id)` ON DELETE RESTRICT:** Fase 5 elimina alertas de ejercicios sin historial explícitamente ANTES de eliminar el ejercicio padre.
6. **`RotationState` singleton (id=1):** Fase 6 reinicia `microcycle_position = 1` pero preserva `microcycle_count` (CA-24.21).
7. **Las migraciones Room son transaccionales por defecto.**
8. **Colisión de nombres:** Algunos ejercicios del nuevo diccionario tienen nombres similares a los anteriores (ej: "Extensión de Cuádriceps" ID 30 viejo → "Extensión de Cuádriceps" nuevo ID 11). Si el ejercicio viejo fue marcado `is_custom = 1`, ambos coexisten con IDs distintos. El UNIQUE (name, equipment_type_id) puede colisionar si comparten `equipment_type_id`. Mitigación: en Fase 5, al marcar como custom, renombrar si el nombre colisiona con el nuevo diccionario.
9. **IDs dinámicos en migración vs IDs fijos en seeders:** En `ExerciseSeeder` (nuevas instalaciones) los IDs son fijos 1-26. En MIGRATION_9_10, los IDs parten de MAX+1 (puede haber ejercicios custom con IDs > 43). Las `plan_assignment` de la migración deben mapear IDs lógicos (1-26) a IDs reales usando offset.
10. **Orden de fases en migración es crítico:** La secuencia Fase 1→2→3→4→5→6 respeta todas las dependencias de FK. Alterar el orden puede causar FK violations.
