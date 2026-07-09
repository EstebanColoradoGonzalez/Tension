# Historia de Usuario

**Como** ejecutante,
**Quiero** que el sistema reorganice internamente la distribución de grupos musculares por módulo — pasando de la división anterior (A = Pecho, Espalda, Abdomen; B = Hombro, Tríceps, Bíceps) a la división fisiológicamente optimizada para hipertrofia **Pull / Push / Legs** (A = Espalda, Bíceps, Abdomen; B = Pecho, Hombro, Tríceps) — y que corrija la clasificación del ejercicio "Elevación de hombros con mancuernas" de Hombro a Espalda Media,
**Para** que mi plan de entrenamiento refleje correctamente los patrones biomecánicos de jalón y empuje, y los datos históricos ya registrados se preserven íntegros y correctamente asociados bajo la nueva estructura.

## Descripción

Esta historia define la migración de la división de entrenamiento de la estructura anterior (A = Pecho, Espalda, Abdomen; B = Hombro, Tríceps, Bíceps) a la división Pull / Push / Legs (A = Espalda, Bíceps, Abdomen; B = Pecho, Hombro, Tríceps; C = Inferior inalterado). Además, corrige la clasificación del ejercicio "Elevación de hombros con mancuernas" de Hombro a Espalda Media. La migración actualiza las tablas `module`, `exercise`, `exercise_muscle_zone` y `plan_assignment`, preservando íntegramente los datos históricos de sesiones, series y alertas.

---

## Criterios de Aceptación

### Bloque A — Migración de la tabla `module` (metadatos)

#### CA-16.01 — Actualización de nombre y grupo muscular del Módulo A

**Dado que** el Módulo A tenía `name = "Módulo A — Superior"` y `group_description = "Pecho, Espalda, Abdomen"`,
**cuando** se ejecuta la migración,
**entonces** el Módulo A queda con `name = "Módulo A — Superior (Pull + Abs)"` y `group_description = "Espalda, Bíceps, Abdomen"`.

#### CA-16.02 — Actualización de nombre y grupo muscular del Módulo B

**Dado que** el Módulo B tenía `name = "Módulo B — Superior"` y `group_description = "Hombro, Tríceps, Bíceps"`,
**cuando** se ejecuta la migración,
**entonces** el Módulo B queda con `name = "Módulo B — Superior (Push)"` y `group_description = "Pecho, Hombro, Tríceps"`.

#### CA-16.03 — Módulo C inalterado

**Dado que** el Módulo C ya estaba correctamente asignado,
**cuando** se ejecuta la migración,
**entonces** el Módulo C conserva `name = "Módulo C — Inferior"` y `group_description = "Cuádriceps, Isquiotibiales, Glúteos, Aductores, Abductores, Gemelos"` sin cambios.

---

### Bloque B — Migración de la tabla `exercise` (reasignación de módulo)

#### CA-16.04 — Ejercicios de Pecho migran de A a B

**Dado que** los ejercicios de Pecho (IDs 1-7: Press de banca, Press de mancuerna, Press de banca inclinada, Flexiones, Cruce en polea alta, Apertura de pecho sentado, Apertura de pecho inclinado) pertenecían al Módulo A,
**cuando** se ejecuta la migración,
**entonces** los 7 ejercicios de Pecho tienen `module_code = "B"`.

#### CA-16.05 — Ejercicios de Bíceps migran de B a A

**Dado que** los ejercicios de Bíceps (IDs 16-20: Curl de bíceps Mancuerna, Curl de bíceps Polea, Curl de martillo cruzado, Curl de martillo, Curl de Contracción) pertenecían al Módulo B,
**cuando** se ejecuta la migración,
**entonces** los 5 ejercicios de Bíceps tienen `module_code = "A"`.

#### CA-16.06 — Elevación de hombros con mancuernas migra de B a A

**Dado que** el ejercicio "Elevación de hombros con mancuernas" (ID 26) pertenecía al Módulo B,
**cuando** se ejecuta la migración,
**entonces** el ejercicio tiene `module_code = "A"`.

#### CA-16.07 — Ejercicios de Hombro (excepto Elevación de hombros) permanecen en B

**Dado que** los ejercicios de Hombro restantes (IDs 24, 25, 27, 28, 29: Elevación frontal, Elevación lateral, Press de elevación sentado, Remo vertical, Remo vertical con cable) pertenecían al Módulo B,
**cuando** se ejecuta la migración,
**entonces** los 5 ejercicios de Hombro conservan `module_code = "B"`.

#### CA-16.08 — Ejercicios de Tríceps permanecen en B

**Dado que** los ejercicios de Tríceps (IDs 21-23: Dominada de tríceps banco, Extensión de tríceps por encima de la cabeza, Flexión de tríceps con cuerda) pertenecían al Módulo B,
**cuando** se ejecuta la migración,
**entonces** los 3 ejercicios de Tríceps conservan `module_code = "B"`.

#### CA-16.09 — Ejercicios de Espalda y Abdomen permanecen en A

**Dado que** los ejercicios de Espalda (IDs 8-10: Remo con Inclinación, Remo con un solo brazo doblado, Tiro de dorsales) y Abdomen (IDs 11-15: Abdominales, Escalador, Giro Ruso, Plancha, Plancha Lateral) pertenecían al Módulo A,
**cuando** se ejecuta la migración,
**entonces** los 8 ejercicios conservan `module_code = "A"`.

#### CA-16.10 — Ejercicios del Módulo C inalterados

**Dado que** los 14 ejercicios del Módulo C (IDs 30-43) ya estaban correctamente asignados,
**cuando** se ejecuta la migración,
**entonces** los 14 ejercicios conservan `module_code = "C"`.

#### CA-16.11 — Conteo final de ejercicios por módulo

**Dado que** la migración ha completado las reasignaciones,
**cuando** se verifica el conteo,
**entonces** el Módulo A tiene 14 ejercicios (4 Espalda + 5 Bíceps + 5 Abdomen), el Módulo B tiene 15 ejercicios (7 Pecho + 5 Hombro + 3 Tríceps) y el Módulo C tiene 14 ejercicios (inalterado). Total: 43 ejercicios.

---

### Bloque C — Corrección de zona muscular de "Elevación de hombros con mancuernas"

#### CA-16.12 — Cambio de zona muscular de Hombro a Espalda Media

**Dado que** el ejercicio "Elevación de hombros con mancuernas" (ID 26) tenía asignada la zona muscular Hombro (muscle_zone_id = 7),
**cuando** se ejecuta la migración,
**entonces** la fila correspondiente en `exercise_muscle_zone` se actualiza a `muscle_zone_id = 4` (Espalda Media).

#### CA-16.13 — Recálculo de KPIs post-migración

**Dado que** el cambio de zona muscular afecta la distribución de volumen por grupo muscular,
**cuando** el ejecutante consulta las métricas de analítica después de la migración,
**entonces** el tonelaje y distribución de volumen del grupo muscular "Hombro" disminuye al perder las series de Elevación de hombros, y el grupo muscular "Espalda" aumenta al ganarlas. Los cálculos reflejan la corrección retroactivamente.

---

### Bloque D — Migración de la tabla `plan_assignment` (recomposición de versiones)

#### CA-16.14 — Eliminación de asignaciones antiguas de Módulos A y B

**Dado que** las asignaciones del plan de entrenamiento para los Módulos A y B ya no corresponden a la nueva estructura,
**cuando** se ejecuta la migración,
**entonces** se eliminan las 66 filas de `plan_assignment` correspondientes a `module_version_id` IN (1, 2, 3, 4, 5, 6).

#### CA-16.15 — Inserción de nuevas asignaciones del Módulo A (Pull + Abs)

**Dado que** el nuevo Módulo A agrupa Espalda, Bíceps y Abdomen,
**cuando** se ejecuta la migración,
**entonces** se insertan las siguientes 33 filas de `plan_assignment` (3 versiones × 11 ejercicios):

**A-V1:** Tiro de dorsales (10), Remo con Inclinación (8), Remo con un solo brazo doblado (9), Curl de bíceps Mancuerna (16), Curl de martillo cruzado (18), Curl de bíceps Polea (17), Curl de martillo (19), Abdominales (11), Escalador (12), Giro Ruso (13), Plancha (14).

**A-V2:** Tiro de dorsales (10), Remo con Inclinación (8), Remo con un solo brazo doblado (9), Elevación de hombros (26), Curl de bíceps Mancuerna (16), Curl de martillo cruzado (18), Curl de bíceps Polea (17), Curl de Contracción (20), Abdominales (11), Plancha (14), Plancha Lateral (15).

**A-V3:** Tiro de dorsales (10), Remo con Inclinación (8), Remo con un solo brazo doblado (9), Elevación de hombros (26), Curl de bíceps Mancuerna (16), Curl de bíceps Polea (17), Curl de Contracción (20), Abdominales (11), Giro Ruso (13), Plancha (14).

#### CA-16.16 — Inserción de nuevas asignaciones del Módulo B (Push)

**Dado que** el nuevo Módulo B agrupa Pecho, Hombro y Tríceps,
**cuando** se ejecuta la migración,
**entonces** se insertan las siguientes 33 filas de `plan_assignment` (3 versiones × 11 ejercicios):

**B-V1:** Press de banca (1), Press de banca inclinada (3), Apertura de pecho sentado (6), Flexiones (4, fallo técnico), Press de elevación sentado (27), Elevación frontal (24), Elevación lateral (25), Remo vertical (28), Dominada de tríceps banco (21), Extensión de tríceps cabeza (22), Flexión de tríceps cuerda (23).

**B-V2:** Press de banca (1), Apertura de pecho inclinado (7), Apertura de pecho sentado (6), Cruce en polea alta (5), Press de elevación sentado (27), Elevación lateral (25), Remo vertical con cable (29), Press de mancuerna (2), Dominada de tríceps banco (21), Extensión de tríceps cabeza (22), Flexión de tríceps cuerda (23).

**B-V3:** Press de banca (1), Press de banca inclinada (3), Apertura de pecho sentado (6), Cruce en polea alta (5), Press de elevación sentado (27), Elevación frontal (24), Remo vertical (28), Press de mancuerna (2), Dominada de tríceps banco (21), Extensión de tríceps cabeza (22), Flexión de tríceps cuerda (23).

#### CA-16.17 — Asignaciones del Módulo C inalteradas

**Dado que** el Módulo C no cambió su composición,
**cuando** se ejecuta la migración,
**entonces** las 27 filas de `plan_assignment` de `module_version_id` IN (7, 8, 9) permanecen sin cambios.

#### CA-16.18 — Conteo total de asignaciones post-migración

**Dado que** la migración ha completado la recomposición,
**cuando** se verifica el conteo,
**entonces** `plan_assignment` tiene exactamente 93 filas: 33 (A) + 33 (B) + 27 (C).

---

### Bloque E — Actualización de seeders para instalaciones nuevas

#### CA-16.19 — ModuleSeeder actualizado

**Dado que** el seeder de módulos contenía los nombres y descripciones anteriores,
**cuando** se actualiza el seeder,
**entonces** `ModuleSeeder.kt` inserta:
- `("A", "Módulo A — Superior (Pull + Abs)", "Espalda, Bíceps, Abdomen", 2.5)`
- `("B", "Módulo B — Superior (Push)", "Pecho, Hombro, Tríceps", 2.5)`
- `("C", "Módulo C — Inferior", ...)` sin cambios.

#### CA-16.20 — ExerciseSeeder actualizado (module_code)

**Dado que** el seeder de ejercicios asignaba los ejercicios a los módulos anteriores,
**cuando** se actualiza el seeder,
**entonces** los ejercicios de Pecho (IDs 1-7) se insertan con `moduleCode = "B"`, los ejercicios de Bíceps (IDs 16-20) y Elevación de hombros (ID 26) se insertan con `moduleCode = "A"`, y los demás ejercicios conservan su módulo original.

#### CA-16.21 — ExerciseSeeder actualizado (muscle_zone)

**Dado que** el seeder asignaba "Elevación de hombros con mancuernas" (ID 26) a la zona muscular Hombro (zone_id = 7),
**cuando** se actualiza el seeder,
**entonces** la asignación se cambia a Espalda Media (zone_id = 4): `emz(db, 26, 4)`.

#### CA-16.22 — PlanSeeder actualizado

**Dado que** el seeder del plan contenía las asignaciones de versiones anteriores para Módulos A y B,
**cuando** se actualiza el seeder,
**entonces** `PlanSeeder.kt` refleja las nuevas 33 + 33 asignaciones de A y B según los CA-16.15 y CA-16.16, y las 27 asignaciones de C permanecen iguales.

---

### Bloque F — Migración de base de datos Room

#### CA-16.23 — Incremento de versión de la base de datos

**Dado que** la base de datos actual está en versión 6 (v5→v6 introducida en HU-14),
**cuando** se implementa la migración,
**entonces** la versión de la base de datos se incrementa a 7 y se registra una `Migration(6, 7)` en el `DatabaseModule`.

#### CA-16.24 — Migración SQL ejecutada secuencialmente

**Dado que** la migración requiere cambios en 4 tablas (`module`, `exercise`, `exercise_muscle_zone`, `plan_assignment`),
**cuando** se ejecuta la `Migration(6, 7)`,
**entonces** los statements SQL se ejecutan en el siguiente orden dentro de una transacción:
1. UPDATE `module` (2 filas: A y B)
2. UPDATE `exercise` (13 filas: 7 Pecho de A→B, 5 Bíceps de B→A, 1 Elevación hombros de B→A)
3. UPDATE `exercise_muscle_zone` (1 fila: ejercicio 26 de zone 7→4)
4. DELETE `plan_assignment` WHERE `module_version_id` IN (1, 2, 3, 4, 5, 6)
5. INSERT 66 filas nuevas en `plan_assignment`

#### CA-16.25 — Preservación de datos históricos

**Dado que** el ejecutante tiene sesiones, series y alertas registradas bajo la estructura anterior,
**cuando** se ejecuta la migración,
**entonces** ninguna fila de las tablas `session`, `exercise_set`, `session_exercise` ni `alert` es modificada ni eliminada. Los datos históricos se preservan íntegros.

#### CA-16.26 — Estado de rotación preservado

**Dado que** el estado de rotación está almacenado en la tabla `rotation_state` (entidad separada con fila única `id = 1`) con columnas `microcycle_position`, `current_version_module_a`, `current_version_module_b`, `current_version_module_c`, `microcycle_count`,

> **Nota de corrección (arquitecto):** CA original (HU-15.5) decía `profile` como tabla y nombres de columna `current_module_position` / `module_a_version_index`. Los nombres correctos según código real son `rotation_state` / `microcycle_position` / `current_version_module_a/b/c`. El intent del CA es correcto: el estado se preserva porque la migration no toca `rotation_state`.

**cuando** se ejecuta la migración,
**entonces** el estado de rotación se preserva sin cambios. La próxima sesión del ejecutante usará el módulo y versión que corresponda según la rotación existente.

---

### Bloque G — Validación de integridad post-migración

#### CA-16.27 — Test unitario: conteo de ejercicios por módulo

**Dado que** la migración ha sido aplicada,
**cuando** se ejecuta `SELECT module_code, COUNT(*) FROM exercise GROUP BY module_code`,
**entonces** retorna: A = 14, B = 15, C = 14.

#### CA-16.28 — Test unitario: conteo de asignaciones por versión

**Dado que** la migración ha sido aplicada,
**cuando** se ejecuta `SELECT module_version_id, COUNT(*) FROM plan_assignment GROUP BY module_version_id`,
**entonces** retorna: 1 = 11, 2 = 11, 3 = 11, 4 = 11, 5 = 11, 6 = 11, 7 = 9, 8 = 9, 9 = 9.

#### CA-16.29 — Test unitario: zona muscular de Elevación de hombros

**Dado que** la migración ha sido aplicada,
**cuando** se consulta `SELECT mz.name FROM exercise_muscle_zone emz JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id WHERE emz.exercise_id = 26`,
**entonces** retorna "Espalda Media".

#### CA-16.30 — Test unitario: integridad referencial de plan_assignment

**Dado que** la migración ha recompuesto las asignaciones del plan,
**cuando** se ejecuta la validación cruzada `SELECT pa.* FROM plan_assignment pa JOIN module_version mv ON pa.module_version_id = mv.id JOIN exercise e ON pa.exercise_id = e.id WHERE mv.module_code != e.module_code`,
**entonces** retorna 0 filas (todos los ejercicios asignados pertenecen al módulo correcto de su versión).

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante (usuario principal de la app).
- **Permisos requeridos:** Ninguno — operación local sobre la base de datos del dispositivo.
- **Valor de negocio:** Permite que el plan de entrenamiento refleje correctamente los patrones biomecánicos de jalón y empuje, optimizando la hipertrofia.

### Reglas de Negocio

1. **División Pull / Push / Legs:** La estructura cambia de (A = Pecho, Espalda, Abdomen; B = Hombro, Tríceps, Bíceps) a (A = Espalda, Bíceps, Abdomen; B = Pecho, Hombro, Tríceps; C = Inferior inalterado).
2. **Jalones (Pull):** Los movimientos de tracción — como remos y jalones dorsales — reclutan sinérgicamente el bíceps. Entrenar espalda y bíceps juntos aprovecha esta sinergia.
3. **Empujones (Push):** Los movimientos de empuje — como press de banca y press de hombro — reclutan sinérgicamente el tríceps. Entrenar pecho, hombro y tríceps juntos aprovecha esta sinergia.
4. **Abdomen con Pull:** El módulo de jalones (Espalda + Bíceps) trabaja 2 grupos musculares principales frente a los 3 del módulo de empujones (Pecho + Hombro + Tríceps). Agregar Abdomen al módulo A equilibra la carga de trabajo.
5. **Corrección de zona muscular:** El ejercicio "Elevación de hombros con mancuernas" trabaja el trapecio, que anatómicamente pertenece a la espalda, no al hombro. La zona muscular correcta es **Espalda Media**, no Hombro.
6. **Preservación de datos históricos:** Cada registro de serie está vinculado al `exercise_id` (no al módulo) y al `session_id`. La reasignación del `module_code` actualiza la pertenencia del ejercicio, pero no altera los datos crudos de series ya registradas.
7. **Recálculo de métricas:** Las métricas históricas (tonelaje, tasas de progresión) se recalculan automáticamente porque se derivan del ejercicio, no del módulo.
8. **Migración atómica:** La migración SQL se ejecuta dentro de una transacción para garantizar que todos los cambios se apliquen o ninguno.

### Interfaz

Ninguna. Esta historia no modifica la interfaz de usuario — es una migración de datos y metadatos en la base de datos local.

### Sistemas Externos

Ninguno. Operación 100% local sobre SQLite (Room) en el dispositivo del ejecutante.

---

## Contexto y Referencias

**Arquitectura:** `docs/architecture/architecture_blueprint.md` | `docs/architecture/domain_and_state_model.md`

**Entidades afectadas:** `ModuleEntity`, `ExerciseEntity`, `ExerciseMuscleZoneEntity`, `ModuleVersionEntity`, `PlanAssignmentEntity`, `RotationStateEntity` (ver `docs/architecture/domain_and_state_model.md`)

**Interfaces de referencia:** Ninguna. No hay cambios en la UI.

**Requisitos cubiertos:** RF04, RF05, RF08

**Épica / Módulo:** `EPIC-01: Configuración inicial y estructura del plan`

**Prioridad:** Alta

**Historias relacionadas:**
- HU-01 (BD + seeders)
- HU-03 (ExerciseSeeder + PlanSeeder + assets)
- HU-04 (estructura original del plan)
- HU-15 (métricas — recálculo retroactivo)
- HU-17, HU-18, HU-19 (operan sobre la estructura Pull/Push/Legs correcta)

**Nota de corrección (arquitecto):** CA original (HU-15.5) decía `profile` como tabla y nombres de columna `current_module_position` / `module_a_version_index`. Los nombres correctos según código real son `rotation_state` / `microcycle_position` / `current_version_module_a/b/c`.

---

## Definición de Terminado (Inicial)

- [x] Tabla `module` actualizada: A = "Módulo A — Superior (Pull + Abs)" / "Espalda, Bíceps, Abdomen", B = "Módulo B — Superior (Push)" / "Pecho, Hombro, Tríceps", C inalterado
- [x] 13 ejercicios reasignados de módulo: 7 Pecho A→B, 5 Bíceps B→A, 1 Elevación hombros B→A
- [x] Ejercicio 26 (Elevación de hombros) actualizado de zona muscular Hombro (7) a Espalda Media (4)
- [x] 66 filas antiguas de `plan_assignment` eliminadas (module_version_id 1-6)
- [x] 66 filas nuevas de `plan_assignment` insertadas: 33 para A (3 versiones × 11 ejercicios), 33 para B (3 versiones × 11 ejercicios)
- [x] 27 filas de `plan_assignment` de C (module_version_id 7-9) inalteradas
- [x] `plan_assignment` tiene exactamente 93 filas post-migración
- [x] Versión de base de datos incrementada a 7 con `Migration(6, 7)`
- [x] Migración SQL ejecutada secuencialmente en transacción
- [x] Datos históricos preservados: ninguna fila de `session`, `exercise_set`, `session_exercise` ni `alert` modificada
- [x] Estado de rotación preservado en `rotation_state`
- [x] ModuleSeeder actualizado con nuevos nombres y module_codes
- [x] ExerciseSeeder actualizado con nuevos module_codes y muscle_zone para ejercicio 26
- [x] PlanSeeder actualizado con nuevas 33 + 33 asignaciones de A y B
- [x] Tests unitarios pasando: conteo por módulo, conteo por versión, zona muscular, integridad referencial

---

## Tabla de Reasignación de Ejercicios

| exercise_id | Nombre | Módulo anterior | Módulo nuevo | Zona muscular anterior | Zona muscular nueva |
|---|---|---|---|---|---|
| 1 | Press de banca | A | **B** | Pecho Medio | Pecho Medio |
| 2 | Press de mancuerna | A | **B** | Pecho Medio | Pecho Medio |
| 3 | Press de banca inclinada | A | **B** | Pecho Superior | Pecho Superior |
| 4 | Flexiones | A | **B** | Pecho Inferior | Pecho Inferior |
| 5 | Cruce en polea alta | A | **B** | Pecho Inferior | Pecho Inferior |
| 6 | Apertura de pecho sentado | A | **B** | Pecho Medio | Pecho Medio |
| 7 | Apertura de pecho inclinado | A | **B** | Pecho Superior | Pecho Superior |
| 16 | Curl de bíceps (Mancuerna) | B | **A** | Bíceps | Bíceps |
| 17 | Curl de bíceps (Polea) | B | **A** | Bíceps | Bíceps |
| 18 | Curl de martillo cruzado | B | **A** | Bíceps | Bíceps |
| 19 | Curl de martillo | B | **A** | Bíceps | Bíceps |
| 20 | Curl de Contracción | B | **A** | Bíceps | Bíceps |
| 26 | Elevación de hombros con mancuernas | B | **A** | **Hombro** | **Espalda Media** |

---

## Componentes NO Tocados

- `RotationStateEntity` / `RotationStateDao` — no se modifica tabla ni datos
- `ProfileEntity` / `ProfileDao` — no contiene estado de rotación
- `SessionEntity` / `SessionExerciseEntity` / `ExerciseSetEntity` — datos transaccionales preservados
- `AlertEntity` — no se modifica
- UI Layer — ningún cambio en pantallas, ViewModels o navegación
- Domain Layer — modelos de dominio no se modifican (los datos se derivan de entidades, no de módulos)
