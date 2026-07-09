## Refinamiento Técnico (Developer)
**Autor**: Esteban Colorado González | **Fecha**: 2026-05-08

---

### Consideraciones Generales

**Basado en análisis arquitectónico:**
Análisis Arquitectónico de HU-24 como historia de datos puros — no cambia lógica de negocio, UI (salvo 1 método de carga de imágenes), navegación ni reglas de dominio. El esquema de tablas no cambia (las 18 entidades de v9 se preservan con exactamente las mismas columnas y constraints). La migración MIGRATION_9_10 es más simple que MIGRATION_8_9 en que no cambia esquema de tablas, solo manipula datos. Sin embargo, la lógica condicional (preservar ejercicios con historial, IDs dinámicos, versiones con sesiones) es compleja.

**Nivel de complejidad:**
ALTA — La migración es crítica: requiere preservar historial, rutinas y ejercicios custom del usuario, evitar colisiones UNIQUE en ejercicios preservados, y manejar casos de borde (sesión activa, deload activo, ejercicios con historial). El test instrumentado es obligatorio.

**Riesgos técnicos conocidos:**
1. Ejercicio precargado con historial se elimina si no se verifica `session_exercise` correctamente — Fase 5 verifica TANTO `exercise_id` COMO `original_exercise_id`.
2. Rutina seed (ID 1/2/3) tiene versiones con sesiones históricas — FK violation si se intenta DELETE. Fase 4 elimina solo `routine_version` sin sesiones.
3. Sesión activa al momento de migración — FK rota con versión eliminada. Fase 3 cierra sesiones activas en versiones de rutinas seed ANTES de que Fase 4 las elimine.
4. Deload activo referencia rutinas eliminadas — FK violation en `deload_frozen_version`. Fase 2 completa deload activo ANTES de tocar rutinas.
5. IDs de ejercicios nuevos colisionan — UNIQUE constraint violation. CA-24.16: IDs parten de MAX(id)+1.
6. Ejercicio precargado referenciado por `original_exercise_id` — FK RESTRICT violation. Fase 5 verifica TANTO `exercise_id` COMO `original_exercise_id`.
7. Alerta referencia ejercicio precargado sin historial — FK RESTRICT violation en `alert.exercise_id`. Fase 5 incluye `DELETE FROM alert WHERE exercise_id IN (...)` explícito ANTES de eliminar ejercicios.
8. Imágenes con nombre incorrecto en assets — Validar que los 26 `media_resource` mapean 1:1 a archivos existentes después de aplicar renames y deletes.
9. Colisión UNIQUE(name, equipment_type_id) en ejercicios preservados — En Fase 5, al marcar como custom, renombrar si hay colisión: `UPDATE exercise SET name = name || ' (anterior)' WHERE ...`.

**Patrones y convenciones del equipo:**
- Código fuente en inglés, UI y datos de dominio en español (Arquitectura Técnica §5.1)
- Naming: `{Feature}Screen`, `{Feature}ViewModel`, `{Acción}{Entidad}UseCase`, `{Entidad}Repository`/`{Entidad}RepositoryImpl`, `{Entidad}Entity`, `{Entidad}Dao` (§5.2)
- Migraciones Room en `Migrations.kt` con patrón `object MIGRATION_X_Y : Migration(X, Y)`
- Seeders en `data/local/seed/` con métodos `seedXxx()` y helpers `ex()`, `insertEquipmentType()`, etc.
- Test instrumentado para migraciones Room con `InstrumentedTest` y `Room.inMemoryDatabaseBuilder`

**Dependencias nuevas a instalar:**
Ninguna — la historia usa APIs Room estándar + `SupportSQLiteDatabase` ya disponibles.

**Estrategia de testing:**
Test instrumentado para MIGRATION_9_10 con escenarios: (1) usuario limpio sin datos, (2) usuario con historial en ejercicios seed, (3) usuario con sesión activa en versión de rutina seed, (4) usuario con deload activo, (5) usuario con ejercicios custom y rutinas propias, (6) colisión UNIQUE (nombre + equipment_type_id).

---

### Historias Relacionadas Consultadas

**Implementaciones similares analizadas:**
- HU-16: Patrón de migración DDL+data (MIGRATION_6_7)
- HU-21: Migración de plan y rutinas (MIGRATION_7_8)
- HU-23: Refactor de entidades y migración (MIGRATION_8_9)
- HU-19: Backup/Restore (para validar preservación de datos custom)

**Patrones de código reutilizados:**
- Patrón de migración Room con `SupportSQLiteDatabase` — mismo patrón que MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9
- Helpers de seeders: `ex()`, `insertEquipmentType()`, `insertExercise()`, `insertRoutine()`, etc.
- Mapeo de IDs lógicos a dinámicos usando `MAX(id) + 1`

**Mejores prácticas aplicadas:**
- Orden de fases en migración respeta todas las dependencias de FK
- Preservación de datos del usuario: ejercicios custom, rutinas propias, historial de sesiones
- IDs dinámicos en migración vs IDs fijos en seeders para nuevas instalaciones
- Test instrumentado obligatorio con múltiples escenarios de borde

---

### Tareas de Implementación

#### Fase 1 — MIGRATION_9_10

- [ ] **Crear `MIGRATION_9_10` en `Migrations.kt`** (AC: CA-24.13–CA-24.22, CA-24.25–CA-24.28)
  - [ ] Fase 1 SQL: INSERT 6 equipment types (IDs 10-15) — Archivo: `data/local/database/Migrations.kt`
  - [ ] Fase 2 SQL: `UPDATE deload SET status = 'COMPLETED'` donde `status = 'ACTIVE'` (CA-24.28)
  - [ ] Fase 3 SQL: `UPDATE session SET status = 'COMPLETED'` para sesiones activas en versiones seed (CA-24.22)
  - [ ] Fase 4 SQL: DELETE plan_assignment, deload_frozen_version, routine_current_version, alertas, routine_version (sin sesiones históricas), routine (sin versiones) de IDs 1/2/3 (CA-24.17, CA-24.18, CA-24.19, CA-24.27)
  - [ ] Fase 5 SQL/Kotlin:
    - [ ] `UPDATE exercise SET is_custom = 1` para ejercicios con historial en `session_exercise` (exercise_id y original_exercise_id) (CA-24.15, CA-24.23)
    - [ ] Renombrar ejercicios preservados con colisión UNIQUE: `SET name = name || ' (anterior)'` (CA-24.36)
    - [ ] `DELETE FROM alert WHERE exercise_id` sin historial (CA-24.27)
    - [ ] `DELETE FROM exercise_progression` sin historial (CA-24.26)
    - [ ] `DELETE FROM exercise_muscle_zone` sin historial
    - [ ] `DELETE FROM plan_assignment WHERE exercise_id` sin historial
    - [ ] `DELETE FROM exercise WHERE is_custom = 0` sin historial (CA-24.15)
  - [ ] Fase 6 Kotlin: Insertar 26 ejercicios con IDs dinámicos (baseExId+offset) + 29 zonas musculares (CA-24.03, CA-24.04, CA-24.16)
  - [ ] Fase 6 Kotlin: Insertar 3 rutinas nuevas (piernaId, pushId, pullId = maxRoutineId+1/+2/+3) (CA-24.08, CA-24.17)
  - [ ] Fase 6 Kotlin: Insertar 4 routine_versions con IDs dinámicos (CA-24.09, CA-24.18)
  - [ ] Fase 6 Kotlin: Insertar 3 routine_current_version con `current_version_number = 1` (CA-24.10, CA-24.20)
  - [ ] Fase 6 Kotlin: Insertar 27 plan_assignments mapeando IDs lógicos a dinámicos (CA-24.11, CA-24.19)
  - [ ] Fase 6 SQL: `UPDATE rotation_state SET microcycle_position = 1 WHERE id = 1` (CA-24.21)

- [ ] **Test instrumentado de migración** (AC: CA-24.13–CA-24.28, CA-24.32–CA-24.36) — MANUAL
  - [ ] Escenario 1: Usuario limpio sin datos — verificar 26 ejercicios precargados, 3 rutinas, 4 versiones, 27 asignaciones
  - [ ] Escenario 2: Usuario con historial en ejercicios seed — verificar que ejercicios con historial se marcan custom
  - [ ] Escenario 3: Usuario con sesión activa en versión de rutina seed — verificar cierre automático (CA-24.22)
  - [ ] Escenario 4: Usuario con deload activo — verificar completado automático (CA-24.28)
  - [ ] Escenario 5: Usuario con ejercicios custom y rutinas propias — verificar preservación intacta (CA-24.23, CA-24.24)
  - [ ] Escenario 6: Colisión UNIQUE (nombre + equipment_type_id) — verificar renombrado con sufijo " (anterior)" (CA-24.36)

#### Fase 2 — Seeders Actualizados

- [ ] **Modificar `BaseDataSeeder`** (AC: CA-24.01, CA-24.02)
  - [ ] Agregar 6 llamadas `insertEquipmentType(db, id, name)` para IDs 10-15 — Archivo: `data/local/seed/BaseDataSeeder.kt`
  - [ ] Verificar que los 9 equipment types existentes (IDs 1-9) no se modifican ni eliminan

- [ ] **Reescribir `ExerciseSeeder`** (AC: CA-24.03–CA-24.07)
  - [ ] Eliminar los 43 ejercicios actuales y reemplazar con 26 ejercicios (IDs 1-26) — Archivo: `data/local/seed/ExerciseSeeder.kt`
  - [ ] `seedExercises()`: 26 inserts con equipment_type_ids incluyendo 10-15 y media_resource según CA-24.07
  - [ ] `seedExerciseMuscleZones()`: 29 inserts (26 + 3 multi-zona: Peso Muerto Rumano, Sentadilla Búlgara, Sentadilla de Zumo)
  - [ ] Todos con `is_custom = 0`, `is_bodyweight = 0`, `is_isometric = 0`, `is_to_technical_failure = 0` (CA-24.05, CA-24.06)

- [ ] **Reescribir `PlanSeeder`** (AC: CA-24.08–CA-24.12)
  - [ ] `seedRoutines()`: 3 rutinas — Pierna (sort=1), Push (sort=2), Pull (sort=3) — Archivo: `data/local/seed/PlanSeeder.kt`
  - [ ] `seedRoutineVersions()`: 4 versiones (Pierna V1 id=1, Pierna V2 id=2, Push V1 id=3, Pull V1 id=4)
  - [ ] `seedRoutineCurrentVersions()`: 3 entradas con `current_version_number = 1`
  - [ ] `seedPlanAssignments()`: 27 asignaciones con `sets = 4`, `reps = '8-12'` (ver tablas en CA-24.11)
  - [ ] Eliminar constantes `FAILURE` y `R30_45`

- [ ] **Modificar `TensionDatabase`** (AC: CA-24.13)
  - [ ] Incrementar `version` de 9 a 10 — Archivo: `data/local/database/TensionDatabase.kt`

- [ ] **Modificar `DatabaseModule`** (AC: CA-24.29)
  - [ ] Agregar `Migrations.MIGRATION_9_10` a `.addMigrations(...)` — Archivo: `di/DatabaseModule.kt`

#### Fase 3 — Assets

- [ ] **Eliminar 29 imágenes obsoletas** (AC: CA-24.07d)
  - [ ] Eliminar los 29 archivos `.png` listados en CA-24.07d de `assets/exercises/` — Archivo: `app/src/main/assets/exercises/`

- [ ] **Renombrar 14 imágenes** (AC: CA-24.07e)
  - [ ] Renombrar según la tabla de correspondencia en CA-24.07e — Archivo: `app/src/main/assets/exercises/`

- [ ] **Verificar 12 imágenes nuevas** (AC: CA-24.07f)
  - [ ] Verificar que las 12 imágenes nuevas están presentes con sus nombres correctos — Archivo: `app/src/main/assets/exercises/`

- [ ] **Verificar estado final** (AC: CA-24.07g)
  - [ ] Contar exactamente 26 archivos `.png` en `assets/exercises/` — Archivo: `app/src/main/assets/exercises/`

#### Fase 4 — UI

- [ ] **Modificar `rememberExerciseBitmap()` en `ExerciseDetailScreen.kt`** (AC: CA-24.07b)
  - [ ] Cambiar búsqueda de subdirectorios `module-a/b/c` a carpeta plana `exercises/$mediaResource.png` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/catalog/ExerciseDetailScreen.kt`

---

### Métricas y Validación

- Integridad de datos post-migración: conteo exacto de ejercicios, rutinas, versiones y asignaciones.
- No existen registros huérfanos ni colisiones UNIQUE.
- Carga correcta de imágenes en UI para todos los ejercicios precargados.
- Test instrumentado obligatorio con 6 escenarios de borde.
