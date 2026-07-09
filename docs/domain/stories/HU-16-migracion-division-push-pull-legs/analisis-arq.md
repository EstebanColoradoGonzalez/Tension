## Análisis Arquitectónico

> Esta historia es una migración de datos y metadatos. No modifica la arquitectura del proyecto ni la interfaz de usuario.

**Patrón arquitectónico:** La migración se implementa como una `Migration(6, 7)` de Room que ejecuta SQL directo dentro de una transacción. Los seeders (ModuleSeeder, ExerciseSeeder, PlanSeeder) se actualizan para reflejar la nueva estructura.

### Componentes afectados

#### 1. Base de Datos — Migración SQL (Modificación)

- **`DatabaseModule`**: Registrar `Migration(6, 7)` que ejecuta los siguientes statements SQL en orden dentro de una transacción:
  1. UPDATE `module` (2 filas: A y B) — cambiar nombres y descripciones
  2. UPDATE `exercise` (13 filas) — reasignar `module_code`: 7 Pecho de A→B, 5 Bíceps de B→A, 1 Elevación hombros de B→A
  3. UPDATE `exercise_muscle_zone` (1 fila) — ejercicio 26 de zone 7→4
  4. DELETE `plan_assignment` WHERE `module_version_id` IN (1, 2, 3, 4, 5, 6)
  5. INSERT 66 filas nuevas en `plan_assignment` (33 para A, 33 para B)

#### 2. Data Layer — Seeders (Modificación)

- **`ModuleSeeder.kt`**: Actualizar inserciones de módulos:
  - `("A", "Módulo A — Superior (Pull + Abs)", "Espalda, Bíceps, Abdomen", 2.5)`
  - `("B", "Módulo B — Superior (Push)", "Pecho, Hombro, Tríceps", 2.5)`
  - `("C", "Módulo C — Inferior", ...)` sin cambios

- **`ExerciseSeeder.kt`**: Actualizar `moduleCode` de:
  - Ejercicios de Pecho (IDs 1-7): de "A" a **"B"**
  - Ejercicios de Bíceps (IDs 16-20): de "B" a **"A"**
  - Elevación de hombros (ID 26): de "B" a **"A"**
  - Actualizar zona muscular del ejercicio 26: de zone_id 7 (Hombro) a zone_id 4 (Espalda Media)

- **`PlanSeeder.kt`**: Actualizar asignaciones de versiones de Módulos A y B:
  - Eliminar 6 versiones antiguas, crear 6 nuevas (A-V1/V2/V3, B-V1/V2/V3)
  - Mantener 3 versiones de C inalteradas
  - Total: 93 asignaciones (33 + 33 + 27)

#### 3. Componentes NO tocados

- `RotationStateEntity` / `RotationStateDao` — no se modifica tabla ni datos
- `ProfileEntity` / `ProfileDao` — no contiene estado de rotación
- `SessionEntity` / `SessionExerciseEntity` / `ExerciseSetEntity` — datos transaccionales preservados
- `AlertEntity` — no se modifica
- UI Layer — ningún cambio en pantallas, ViewModels o navegación
- Domain Layer — modelos de dominio no se modifican (los datos se derivan de entidades, no de módulos)

---

### Integraciones

| Interfaz / Contrato | Productor | Consumidor | Descripción |
| --- | --- | --- | --- |
| `Migration(6, 7)` | `DatabaseModule` | `TensionDatabase` | Migración SQL secuencial en transacción |
| `ModuleSeeder` | `ModuleSeeder.kt` | `TensionDatabase` (onCreate) | Inserta módulos con nuevos nombres/descripciones |
| `ExerciseSeeder` | `ExerciseSeeder.kt` | `TensionDatabase` (onCreate) | Inserta ejercicios con nuevos module_codes y zona muscular corregida |
| `PlanSeeder` | `PlanSeeder.kt` | `TensionDatabase` (onCreate) | Inserta 93 asignaciones con nueva estructura |

---

### Riesgos

| Riesgo | Probabilidad | Impacto | Mitigación |
| --- | --- | --- | --- |
| Migración SQL elimina datos históricos si hay error en DELETE/INSERT | Baja | Alto | Ejecutar dentro de transacción Room (@Transaction). Si falla INSERT, se revierte DELETE. Verificar con tests unitarios (CA-16.27 a CA-16.30). |
| Conteo de filas incorrecto post-migración | Media | Medio | Tests unitarios CA-16.27 y CA-16.18 verifican conteos exactos. Validar con consultas SQL directas contra exported schema. |
| Seeders inconsistentes con migración | Media | Alto | Actualizar seeders y migración en el mismo commit. Los seeders deben reflejar el estado FINAL (post-migración), no el estado intermedio. |
| Integridad referencial rota en plan_assignment | Baja | Alto | CA-16.30 valida cruzada: todos los ejercicios asignados pertenecen al módulo correcto de su versión. |
| Estado de rotación afectado por cambio de versiones | Baja | Alto | La migración no toca `rotation_state`. La próxima sesión usa el módulo y versión existentes, que ahora apuntan a ejercicios diferentes pero con los mismos exercise_ids. |

---

### Hitos de implementación

| Hito | Contenido | Dependencias |
| --- | --- | --- |
| 1 | Incrementar versión de `TensionDatabase` de 6 a 7 | — |
| 2 | Registrar `Migration(6, 7)` en `DatabaseModule` | Hito 1 |
| 3 | Implementar SQL de migración: UPDATE module, UPDATE exercise, UPDATE exercise_muscle_zone | Hito 2 |
| 4 | Implementar SQL de migración: DELETE plan_assignment + INSERT 66 filas nuevas | Hito 3 |
| 5 | Actualizar `ModuleSeeder.kt` con nuevos nombres y descripciones | — |
| 6 | Actualizar `ExerciseSeeder.kt` con nuevos module_codes y muscle_zone | Hito 5 |
| 7 | Actualizar `PlanSeeder.kt` con nuevas 66 asignaciones de A y B | Hito 6 |
| 8 | Tests unitarios: conteo por módulo, conteo por versión, zona muscular, integridad referencial | Hito 4, Hito 7 |

---

### Notas de auditoría

1. **`rotation_state` no se modifica.** La migración no toca la tabla `rotation_state`. El estado de rotación se preserva porque los `exercise_id` no cambian, solo el `module_code`. La próxima sesión del ejecutante usará el módulo y versión que corresponda según la rotación existente.
2. **Los seeders reflejan el estado FINAL.** Los seeders deben insertar los datos como quedarían después de aplicar la migración, no el estado intermedio. Esto evita inconsistencias entre instalaciones nuevas y migraciones.
3. **Transacción única.** Todos los statements SQL de la migración se ejecutan dentro de una sola transacción. Si cualquier statement falla, Room revierte todos los cambios.
4. **No hay cambios en la UI.** Esta historia es 100% datos y metadatos. Los tests de UI no son necesarios.
5. **Nota de corrección (arquitecto):** CA original (HU-15.5) decía `profile` como tabla y nombres de columna `current_module_position` / `module_a_version_index`. Los nombres correctos según código real son `rotation_state` / `microcycle_position` / `current_version_module_a/b/c`.
