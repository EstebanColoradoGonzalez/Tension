## Refinamiento Técnico (Developer)
**Autor**: Arquitecto | **Fecha**: 2026-07-09

---

### Consideraciones Generales

**Basado en análisis arquitectónico:**
Análisis Arquitectónico de HU-16 con 8 hitos, 4 componentes afectados, 4 integraciones y 5 riesgos identificados. Migración de datos y metadatos sin cambios en UI ni Domain Layer.

**Nivel de complejidad:**
MEDIA — La migración SQL es directa pero crítica: afecta 4 tablas y debe ejecutarse en transacción. Los seeders requieren atención para reflejar el estado final correcto. El riesgo principal es la integridad de datos.

**Riesgos técnicos conocidos:**
1. Migración SQL debe ejecutarse en transacción única — cualquier error revierte todo.
2. Conteo de filas debe ser exacto: 13 ejercicios reasignados, 66 eliminadas, 66 insertadas en plan_assignment.
3. Seeders deben reflejar estado FINAL (post-migración), no estado intermedio.
4. Integridad referencial: todos los ejercicios en plan_assignment deben pertenecer al módulo correcto de su versión.

**Patrones y convenciones del equipo:**
- Código fuente en inglés, UI y datos de dominio en español
- Naming: `Migration(from, to)` para migraciones Room
- Migraciones SQL dentro de `@Transaction` de Room
- Seeders como funciones separadas: `ModuleSeeder`, `ExerciseSeeder`, `PlanSeeder`
- Tests unitarios con JUnit 4 + MockK

**Dependencias nuevas:**
Ninguna. Esta historia solo modifica datos existentes.

**Estrategia de testing:**
JUnit 4 + MockK | Tests unitarios para validación post-migración (CA-16.27 a CA-16.30) | Consultas SQL directas contra base de datos en memoria

---

### Historias Relacionadas Consultadas

**Implementaciones similares analizadas:**
HU-14 (v5→v6) — patrón de migración Room ya establecido en el proyecto.

**Patrones de código reutilizados:**
- `Migration(6, 7)` siguiendo el patrón HU-14
- Seeders actualizados siguiendo el patrón existente
- Tests unitarios con consultas SQL directas

**Mejores prácticas aplicadas:**
- Migración en transacción única para atomicidad
- Seeders reflejan estado final, no intermedio
- Tests unitarios validan conteos exactos e integridad referencial
- Preservación de datos históricos: no tocar session, exercise_set, session_exercise, alert

---

### Tareas de Implementación

#### Fase 1: Migración de Base de Datos Room

> Basado en Hito #1-4 del Análisis Arquitectónico

##### Database Migration

- [ ] **Incrementar versión de TensionDatabase** (AC: 16.23)
  - [ ] Cambiar `version = 6` a `version = 7` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/database/TensionDatabase.kt`

- [ ] **Registrar Migration(6, 7) en DatabaseModule** (AC: 16.23)
  - [ ] Crear `Migration(6, 7)` con `addMigrations()` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/di/DatabaseModule.kt`

- [ ] **Implementar SQL: UPDATE module** (AC: 16.01, 16.02)
  - [ ] `UPDATE module SET name = 'Módulo A — Superior (Pull + Abs)', group_description = 'Espalda, Bíceps, Abdomen' WHERE module_code = 'A'` — Dentro de Migration(6, 7)
  - [ ] `UPDATE module SET name = 'Módulo B — Superior (Push)', group_description = 'Pecho, Hombro, Tríceps' WHERE module_code = 'B'` — Dentro de Migration(6, 7)

- [ ] **Implementar SQL: UPDATE exercise (module_code)** (AC: 16.04-16.10)
  - [ ] UPDATE 7 ejercicios de Pecho: `module_code = 'B'` WHERE exercise_id IN (1, 2, 3, 4, 5, 6, 7) — Dentro de Migration(6, 7)
  - [ ] UPDATE 5 ejercicios de Bíceps: `module_code = 'A'` WHERE exercise_id IN (16, 17, 18, 19, 20) — Dentro de Migration(6, 7)
  - [ ] UPDATE 1 ejercicio Elevación hombros: `module_code = 'A'` WHERE exercise_id = 26 — Dentro de Migration(6, 7)

- [ ] **Implementar SQL: UPDATE exercise_muscle_zone** (AC: 16.12)
  - [ ] `UPDATE exercise_muscle_zone SET muscle_zone_id = 4 WHERE exercise_id = 26` — Dentro de Migration(6, 7)

- [ ] **Implementar SQL: plan_assignment recomposición** (AC: 16.14-16.16)
  - [ ] `DELETE FROM plan_assignment WHERE module_version_id IN (1, 2, 3, 4, 5, 6)` — Dentro de Migration(6, 7)
  - [ ] INSERT 33 filas para Módulo A (A-V1, A-V2, A-V3) — Dentro de Migration(6, 7)
  - [ ] INSERT 33 filas para Módulo B (B-V1, B-V2, B-V3) — Dentro de Migration(6, 7)

#### Fase 2: Actualización de Seeders

> Basado en Hito #5-7 del Análisis Arquitectónico

##### ModuleSeeder

- [ ] **Actualizar ModuleSeeder.kt** (AC: 16.19)
  - [ ] Módulo A: `("A", "Módulo A — Superior (Pull + Abs)", "Espalda, Bíceps, Abdomen", 2.5)` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/seed/ModuleSeeder.kt`
  - [ ] Módulo B: `("B", "Módulo B — Superior (Push)", "Pecho, Hombro, Tríceps", 2.5)` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/seed/ModuleSeeder.kt`
  - [ ] Módulo C: sin cambios — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/seed/ModuleSeeder.kt`

##### ExerciseSeeder

- [ ] **Actualizar moduleCode de Pecho en ExerciseSeeder** (AC: 16.20)
  - [ ] IDs 1-7: cambiar `moduleCode = "A"` a `moduleCode = "B"` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/seed/ExerciseSeeder.kt`

- [ ] **Actualizar moduleCode de Bíceps en ExerciseSeeder** (AC: 16.20)
  - [ ] IDs 16-20: cambiar `moduleCode = "B"` a `moduleCode = "A"` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/seed/ExerciseSeeder.kt`

- [ ] **Actualizar moduleCode y muscle_zone de Elevación de hombros en ExerciseSeeder** (AC: 16.20, 16.21)
  - [ ] ID 26: cambiar `moduleCode = "B"` a `moduleCode = "A"` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/seed/ExerciseSeeder.kt`
  - [ ] ID 26: cambiar zone_id de 7 a 4 (`emz(db, 26, 4)`) — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/seed/ExerciseSeeder.kt`

##### PlanSeeder

- [ ] **Actualizar PlanSeeder.kt** (AC: 16.22)
  - [ ] Eliminar asignaciones antiguas de A y B (6 versiones) — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/seed/PlanSeeder.kt`
  - [ ] Insertar 33 nuevas filas para A (A-V1: 10,8,9,16,18,17,19,11,12,13,14; A-V2: 10,8,9,26,16,18,17,20,11,14,15; A-V3: 10,8,9,26,16,19,17,20,11,13,14) — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/seed/PlanSeeder.kt`
  - [ ] Insertar 33 nuevas filas para B (B-V1: 1,3,6,4,27,24,25,28,21,22,23; B-V2: 1,7,6,5,27,25,29,2,21,22,23; B-V3: 1,3,6,5,27,24,28,2,21,22,23) — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/seed/PlanSeeder.kt`
  - [ ] Mantener 27 filas de C inalteradas — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/seed/PlanSeeder.kt`

#### Fase 3: Tests Unitarios

> Basado en Hito #8 del Análisis Arquitectónico

##### Validación Post-Migración

- [ ] **Test: conteo de ejercicios por módulo** (AC: 16.27)
  - [ ] `SELECT module_code, COUNT(*) FROM exercise GROUP BY module_code` → A=14, B=15, C=14 — Archivo: `app/src/test/java/.../Migration6to7Test.kt`

- [ ] **Test: conteo de asignaciones por versión** (AC: 16.28)
  - [ ] `SELECT module_version_id, COUNT(*) FROM plan_assignment GROUP BY module_version_id` → 1-6: 11 cada uno, 7-9: 9 cada uno — Archivo: `app/src/test/java/.../Migration6to7Test.kt`

- [ ] **Test: zona muscular de Elevación de hombros** (AC: 16.29)
  - [ ] `SELECT mz.name FROM exercise_muscle_zone emz JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id WHERE emz.exercise_id = 26` → "Espalda Media" — Archivo: `app/src/test/java/.../Migration6to7Test.kt`

- [ ] **Test: integridad referencial de plan_assignment** (AC: 16.30)
  - [ ] `SELECT pa.* FROM plan_assignment pa JOIN module_version mv ON pa.module_version_id = mv.id JOIN exercise e ON pa.exercise_id = e.id WHERE mv.module_code != e.module_code` → 0 filas — Archivo: `app/src/test/java/.../Migration6to7Test.kt`
