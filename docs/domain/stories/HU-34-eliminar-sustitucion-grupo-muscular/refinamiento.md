## Refinamiento Técnico (Developer)
**Autor**: Esteban Colorado González | **Fecha**: 2026-08-31

---

### Contexto

La sustitución por grupo muscular (HU-07) es una feature vertical completa: atraviesa las cinco capas del sistema con artefactos propios en cada una, y además dejó una huella en el esquema que el resto del sistema arrastra desde entonces.

**Artefactos exclusivos** (existen solo para esta funcionalidad):

| Capa | Artefacto |
|---|---|
| `UI-01` | `SubstituteExerciseScreen`, ruta `substitute-exercise/{sessionExerciseId}`, botón "Sustituir" en `ActiveSessionScreen`, indicador "Sustituyó a:" en `SessionDetailScreen`, 10 strings |
| `UI-02` | `SubstituteExerciseViewModel` + `SubstituteExerciseUiState` + `SubstituteExerciseUiItem` |
| `DOM-01` | `SubstituteExerciseUseCase`, `SubstituteExerciseInfo`, `SessionRepository.getSubstituteExerciseInfo`, `SessionRepository.substituteExercise`, `ExerciseRepository.getEligibleSubstitutes` |
| `DAT-01` | `SessionExerciseDao.getSessionExerciseForSubstitution`, `SessionExerciseDao.updateExerciseId`, DTO `SessionExerciseForSubstitution`, `ExerciseDao.getEligibleSubstitutesForSession`, `ExerciseDao.getMuscleZoneIdsByExerciseId` |
| `DB-01` | Columna `session_exercise.original_exercise_id` + su FK + su índice |

#### El hallazgo que define el alcance

`original_exercise_id` tiene **un único escritor de valor no nulo** en todo el proyecto: `SessionExerciseDao.updateExerciseId`, invocado exclusivamente desde `SessionRepositoryImpl.substituteExercise`. El intercambio de alternativas de HU-26 (`switchAlternativeExercise`) la pone en `NULL` de forma explícita — y así lo documenta `interfaces_contract.md` §`F2-T1`: *"`original_exercise_id` se mantiene `NULL` (no es sustitución, es selección entre equivalentes)"*.

Consecuencia: al retirar la sustitución la columna queda **inescribible**, permanentemente `NULL`, y los 30 `COALESCE(original_exercise_id, exercise_id)` repartidos por el sistema pasan a ser idénticos a `exercise_id`.

| Archivo | Sitios `COALESCE` / referencia |
|---|---|
| `data/local/dao/SessionExerciseDao.kt` | 23 |
| `data/local/dao/ExerciseSetDao.kt` | 5 |
| `data/local/dao/PlanAssignmentDao.kt` | 1 (`OR se.original_exercise_id = pa.exercise_id`) |
| `data/repository/BackupRepositoryImpl.kt` | 1 (reparación de `slot` post-restauración) |

Dejar la columna sería exactamente la *funcionalidad latente* que la regla de negocio 4 de la historia prohíbe ("Retirar es retirar"), y dejaría 30 expresiones SQL que mienten sobre la semántica del sistema. **Decisión del PO en esta sesión: retiro total, esquema incluido.**

#### Lo que NO se toca

- **Intercambio de alternativa por slot (HU-26).** `switchAlternativeInSession`, `switchAlternativeExercise`, `alternativesInSlot`, `hasAlternatives`, el control `⇄` y `AddAlternativeToSlotUseCase`: intactos (CA-34.02).
- **Personalización del plan (HU-04, HU-21, HU-23).** `PlanRepositoryImpl`, `UpdatePlanAssignmentUseCase`, `TrainingPlanScreen`, `PlanVersionDetailScreen`: no se abren (CA-34.04).
- **Consultas por zona muscular de métricas y alertas.** Los `JOIN exercise_muscle_zone` de `ExerciseSetDao` (volumen por grupo, tonelaje), `getPrimaryMuscleGroupByExercise` y `muscle_zone` completa: se conservan. Lo que desaparece es únicamente `getMuscleZoneIdsByExerciseId`, cuyos tres llamadores son los tres métodos de sustitución (CA-34.07).
- **Migraciones históricas.** `MIGRATION_6_7` … `MIGRATION_14_15` son registro histórico y deben seguir ejecutando contra el esquema de su época: no se editan aunque nombren `original_exercise_id`.

---

### Decisiones técnicas

#### D1 — Retiro por eliminación, no por desactivación

Sin *feature flag*, sin código comentado, sin pantalla inalcanzable. Cada artefacto de la tabla anterior se borra del árbol o se borra del archivo que lo contiene. Es la lectura literal de CA-34.01 y de la regla de negocio 4.

#### D2 — La columna desaparece del esquema: v15 → v16

Justificación en el §Contexto. El efecto colateral valioso es que Room verifica SQL en tiempo de compilación: cualquier `COALESCE` olvidado que siga nombrando la columna **rompe el build**, lo que convierte al compilador en la red de seguridad de las 30 simplificaciones.

#### D3 — La migración recrea la tabla; no usa `DROP COLUMN`

`minSdk = 26` trae SQLite 3.19, y `ALTER TABLE ... DROP COLUMN` existe desde 3.35. Además la columna sostiene una `ForeignKey` y un índice, que SQLite no permite desmontar en caliente. Se replica el patrón ya usado en el proyecto: `MIGRATION_11_12` fase 3 (crear tabla nueva → copiar → `DROP` → `RENAME` → recrear los cuatro índices).

La copia **no pierde información**: el único dato que la columna podría contener se descarta a propósito, y la historia lo autoriza de forma expresa ("Beta sin migración: la base de datos se reinicia; no se requiere migración de datos ni tratamiento de sesiones históricas con sustituciones previas"). El índice `index_session_exercise_original_exercise_id` no se recrea.

#### D4 — La prescripción se resuelve sobre el ejercicio ejecutado, y eso no cambia ningún comportamiento

`exercise_progression` es una tabla por slot y hoy se lee con `COALESCE(original_exercise_id, exercise_id)`: en una sustitución la progresión se seguía contra el ejercicio **original**. Con la sustitución retirada solo quedan dos formas de que `exercise_id` cambie respecto del plan — ninguna, o el intercambio de alternativa, que ya anulaba la columna. Por tanto `COALESCE(...) ≡ exercise_id` para toda fila existente y futura: la simplificación es semánticamente neutra, no una recalibración. Se transcribe a `interfaces_contract.md` §`F3-T1` para que un lector futuro no lea la frase actual como una regla perdida.

#### D5 — El indicador "Sustituyó a:" se retira del modelo, no solo de la pantalla

CA-34.03 exige que el detalle de sesión no muestre indicadores del mecanismo. Se retira en los cuatro niveles a la vez —`strings.xml`, `SessionDetailScreen`, `SessionDetailExercise`, `SessionDetailExerciseDto` y el `LEFT JOIN exercise oe`— porque un campo que nadie puede poblar es la misma clase de residuo que la columna.

#### D6 — `getMuscleZoneIdsByExerciseId` también se va

Es la consulta que decidía si dos ejercicios comparten zona. Sus tres llamadores son `ExerciseRepositoryImpl.getEligibleSubstitutes`, `SessionRepositoryImpl.getSubstituteExerciseInfo` y `SessionRepositoryImpl.substituteExercise`. Retirados los tres, queda huérfana (CA-34.07). Es la única consulta por zona muscular que se elimina: las de métricas y alertas resuelven el grupo por otra vía y no la usan.

#### D7 — La tarjeta de ejercicio no iniciado consolida sus controles en una sola fila

Hoy `NotStartedExerciseRow` reparte los controles en dos filas: `[Registrar] [Sustituir] [📷]` cuando el slot es simple, y `[Registrar] [Sustituir]` + `[⇄] [📷]` cuando tiene alternativa. La segunda fila existe porque "Sustituir" no dejaba ancho para el resto. Al retirarlo, los controles caben en una: `[Registrar]` + `[⇄]` si hay alternativa + `[📷]`.

Esto cumple el requisito de interfaz de la historia ("no debe dejar espacios vacíos ni desalinear la tarjeta") sin rediseñar la tarjeta: no se mueve la imagen, no cambia la tipografía, no se toca el `Spacer` de 8 dp ni los 48 dp de área táctil. El wireframe de `34.preview.txt` dibuja el `⇄` en la línea del título, pero es un prototipo declarado "pendiente de validación con Diseño" y su disposición no coincide con la implementación actual en ninguna de las dos columnas del ANTES/DESPUÉS; se respeta la implementación existente y se limita el cambio al retiro del control.

#### D8 — El formato de backup no cambia de versión; el importador aprende a ignorar columnas ajenas

`BackupRepositoryImpl.exportToJson` serializa con `SELECT *` y `importFromJson` reconstruye un `ContentValues` con **todas** las claves del JSON. Un backup producido por cualquier build ≤ v15 contiene `original_exercise_id`, de modo que tras eliminar la columna `db.insert` lanzaría *"table session_exercise has no column named original_exercise_id"*: **todo backup existente dejaría de restaurarse**. Es una regresión de HU-19 provocada por esta historia y hay que cerrarla aquí.

Se descarta bumpear `SCHEMA_VERSION` de 9 a 10 con un `transformV9ToV10`: obligaría a mover `LEGACY_SCHEMA_VERSION` a 9 y perdería la compatibilidad con los backups v8 que hoy sí se importan. En su lugar, `importFromJson` consulta las columnas reales de cada tabla (`PRAGMA table_info`) y descarta las claves que no existen. Descartar es correcto y no pierde datos: el valor descartado es siempre `NULL`. Como efecto lateral, cualquier columna que se retire en el futuro deja de romper la restauración.

#### D9 — El plan de tests es, en su mayor parte, borrado

No hay reglas nuevas ni comportamiento nuevo que cubrir: la historia retira. Se eliminan las dos suites propias de la funcionalidad (23 casos) y se ajustan las dos que fijan el modelo de detalle de sesión. La única lógica realmente nueva —el filtro de claves de D8— sí recibe casos propios en `BackupRepositoryImplTest`, que ya está montado con `mockk` sobre `SupportSQLiteDatabase` y admite el escenario sin infraestructura adicional.

**No se añade test instrumentado de migración.** `MigrationV6ToV7Test` y `MigrationV7ToV8Test` no usan `MigrationTestHelper`: abren una base en memoria con `PrepopulateCallback` y verifican la **semilla**, no el camino de migración. Replicar ese patrón para v15→v16 daría una prueba que no ejerce `MIGRATION_15_16`. Se declara como verificación manual en su lugar.

#### D10 — Correcciones del mapa de historias: cuatro de las seis ya están aplicadas

CA-34.05 enumera seis correcciones. La auditoría de `story_mapping_index.md` encuentra que **HU-27 sí figura** (§2, línea 58) y que **HU-28 a HU-36 también** (§2) — se incorporaron al crear esas historias. Lo que falta es distinto de lo que el criterio supone:

| Ítem del criterio | Estado real del índice | Trabajo |
|---|---|---|
| HU-27 ausente | Presente en §2 | Falta en §10 (Cobertura por Historia) → añadir |
| HU-28 a HU-36 sin mapear | Presentes en §2 | Faltan en §10 → añadir; sus estados en §2 obsoletos → actualizar |
| HU-18 marcada `Todo` | Confirmado | → `Done` |
| HU-20 sin cancelar | `Todo` | → `Cancelada` + nota |
| HU-07 sin anotar | `Done` | → `Retirada (HU-34)` |
| RF16 asignado a HU-07 | Confirmado en §7 y §10 | → HU-26 |

Se ejecuta el **espíritu** del criterio (el índice queda consistente) y se documenta la diferencia. Además, §4.6 "Inconsistencias Declaradas" existe justamente para estas cuatro filas y se cierra: pasa a *"Corregidas en HU-34"*. §9 (Resumen de Cobertura) arrastra dos cifras derivadas de los estados que cambian y se recalcula.

---

### Tareas de Implementación

#### Fase 1 — Interfaz (`UI-01`)

- [x] **T1: Eliminar la pantalla de sustitución** — `ui/session/SubstituteExerciseScreen.kt` (190 líneas, archivo completo)

- [x] **T2: Eliminar el ViewModel de sustitución** — `ui/session/SubstituteExerciseViewModel.kt` (107 líneas: `SubstituteExerciseUiState`, `SubstituteExerciseUiItem` y `SubstituteExerciseViewModel`)

- [x] **T3: Retirar el punto de entrada de la sesión activa** — `ui/session/ActiveSessionScreen.kt`

  Quitar el parámetro `onNavigateToSubstitute: (Long) -> Unit` de `ActiveSessionScreen` y el `onSubstitute: () -> Unit` de `ExerciseRow` y `NotStartedExerciseRow`, con sus dos sitios de propagación. Eliminar el `OutlinedButton` que renderiza `R.string.session_substitute`.

  Consolidar los controles en una sola `Row` (D7):

  ```kotlin
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      FilledTonalButton(onClick = onRegister, /* … sin cambios … */)
      if (exercise.hasAlternatives) {
          IconButton(onClick = onSelectAlternative, modifier = Modifier.size(48.dp)) {
              Icon(Icons.Filled.SwapHoriz, stringResource(R.string.select_exercise_for_slot), …)
          }
      }
      IconButton(onClick = onViewDetail, modifier = Modifier.size(48.dp)) {
          Icon(Icons.Outlined.PhotoCamera, contentDescription = null, …)
      }
  }
  ```

  Desaparecen el `if (exercise.hasAlternatives)` que abría la segunda fila, su `Spacer(4.dp)` y la duplicación del `IconButton` de cámara. `hasAlternatives` conserva su único uso.

- [x] **T4: Retirar el indicador de sustitución del detalle de sesión** — `ui/history/SessionDetailScreen.kt` (D5)

  Eliminar el bloque `if (exercise.originalExerciseName != null) { … }` con su `Spacer` y su `Text` de `R.string.session_detail_substituted`. Retirar el `import androidx.compose.ui.text.font.FontStyle` si queda sin uso.

- [x] **T5: Eliminar la ruta** — `ui/navigation/NavigationRoutes.kt`

  Quitar `const val SUBSTITUTE_EXERCISE` y `fun substituteExerciseRoute(...)`.

- [x] **T6: Desmontar el cableado de navegación** — `ui/navigation/TensionNavHost.kt`

  Cuatro puntos: el `import …ui.session.SubstituteExerciseScreen`; la condición `!currentRoute.startsWith("substitute-exercise") &&` de la visibilidad del bottom bar; el `onNavigateToSubstitute = { … }` que pasa a `ActiveSessionScreen`; y el bloque `composable(route = NavigationRoutes.SUBSTITUTE_EXERCISE) { SubstituteExerciseScreen(…) }` completo, con su `navArgument`.

- [x] **T7: Retirar los textos** — `res/values/strings.xml`

  Diez strings: `session_substitute`, el comentario `<!-- Substitute Exercise E3 -->`, `substitute_exercise_title`, `substitute_exercise_subtitle_format`, `substitute_exercise_info_line1`, `substitute_exercise_info_line2`, `substitute_exercise_dialog_title_format`, `substitute_exercise_dialog_text`, `substitute_exercise_confirm`, `substitute_exercise_cancel` y `session_detail_substituted`. Verificar que ningún otro `stringResource` los referencia antes de borrar.

#### Fase 2 — Dominio (`DOM-01`)

- [x] **T8: Eliminar el caso de uso** — `domain/usecase/session/SubstituteExerciseUseCase.kt` (archivo completo)

- [x] **T9: Eliminar el modelo de dominio** — `domain/model/SubstituteExerciseInfo.kt` (archivo completo)

- [x] **T10: Limpiar el contrato de sesión** — `domain/repository/SessionRepository.kt`

  Quitar `getSubstituteExerciseInfo` y `substituteExercise`, más el `import …domain.model.SubstituteExerciseInfo`. `switchAlternativeInSession` y `finalizeExercise` quedan tal cual.

- [x] **T11: Limpiar el contrato de ejercicios** — `domain/repository/ExerciseRepository.kt`

  Quitar `getEligibleSubstitutes(sessionId, muscleZoneIds): Flow<List<Exercise>>`. Revisar si el `import kotlinx.coroutines.flow.Flow` sigue en uso (lo está, por otros métodos).

- [x] **T12: Retirar el campo de sustitución del detalle** — `domain/model/SessionDetailExercise.kt` (D5)

  Eliminar `val originalExerciseName: String?`.

#### Fase 3 — Datos (`DAT-01`)

- [x] **T13: Retirar la sustitución del repositorio de sesión** — `data/repository/SessionRepositoryImpl.kt`

  1. Eliminar `getSubstituteExerciseInfo` (16 líneas) y `substituteExercise` (38 líneas, incluida la `withTransaction` con las cuatro validaciones).
  2. Simplificar los tres `val progressionExerciseId = info.originalExerciseId ?: info.exerciseId` (ramas de descarga, de prescripción normal y de `registerSet`) a `info.exerciseId` (D4).
  3. Quitar `originalExerciseName = dto.originalExerciseName` del mapeo a `SessionDetailExercise`.
  4. Retirar el `import …domain.model.SubstituteExerciseInfo`. Revisar si `exerciseDao` sigue inyectándose para algo (sí: `insertIfNotExists` y otras rutas) antes de tocar el constructor.

- [x] **T14: Retirar los elegibles del repositorio de ejercicios** — `data/repository/ExerciseRepositoryImpl.kt`

  Eliminar `getEligibleSubstitutes` completo (15 líneas, incluido el filtrado por intersección de zonas). Verificar que `import kotlinx.coroutines.flow.map` conserva otros usos.

- [x] **T15: Limpiar el DAO de ejercicios de sesión** — `data/local/dao/SessionExerciseDao.kt` (el archivo de mayor superficie: 23 sitios)

  - Eliminar el DTO `SessionExerciseForSubstitution` y la consulta `getSessionExerciseForSubstitution`.
  - Eliminar la consulta `updateExerciseId` (el único escritor de la columna).
  - `switchAlternativeExercise`: quitar `original_exercise_id = NULL` del `SET`; queda `SET exercise_id = :exerciseId` (CA-34.02, sin cambio de comportamiento).
  - `SetExerciseInfo`: quitar el campo `originalExerciseId` y su proyección `se.original_exercise_id AS originalExerciseId`.
  - `SessionDetailExerciseDto`: quitar `originalExerciseName`; en `getExercisesForSessionDetail` quitar la proyección `oe.name AS originalExerciseName` y el `LEFT JOIN exercise oe ON se.original_exercise_id = oe.id` (D5).
  - Sustituir cada `COALESCE(se.original_exercise_id, se.exercise_id)` por `se.exercise_id` (y sus variantes con alias `se2`, `se3`) en las proyecciones de progresión, clasificación, resumen de ejercicios, historial, rangos por sesión, `muscleGroup`, los `LEFT JOIN exercise_progression ep`, los `GROUP BY` y los `ORDER BY` con `plan_assignment`.
  - `getDistinctExerciseIdsBySession`: `SELECT DISTINCT COALESCE(original_exercise_id, exercise_id)` → `SELECT DISTINCT exercise_id`.

- [x] **T16: Limpiar el DAO de ejercicios** — `data/local/dao/ExerciseDao.kt`

  Eliminar `getEligibleSubstitutesForSession` (consulta de 25 líneas con su `NOT IN` sobre la sesión) y `getMuscleZoneIdsByExerciseId` (D6). `ExerciseWithDetails` se conserva: lo usan el diccionario y el plan.

- [x] **T17: Simplificar el DAO de series** — `data/local/dao/ExerciseSetDao.kt`

  Cinco sitios: los dos `WHERE COALESCE(se.original_exercise_id, se.exercise_id) = :exerciseId`, el `WHERE COALESCE(se2…)`, y los dos `INNER JOIN exercise_muscle_zone emz ON COALESCE(se.original_exercise_id, se.exercise_id) = emz.exercise_id`. Los `JOIN` a `exercise_muscle_zone` **se conservan** — solo cambia el lado izquierdo de la igualdad (CA-34.07).

- [x] **T18: Simplificar el DAO de asignaciones de plan** — `data/local/dao/PlanAssignmentDao.kt`

  `AND (se.exercise_id = pa.exercise_id OR se.original_exercise_id = pa.exercise_id)` → `AND se.exercise_id = pa.exercise_id`. Es la entrada de `hasSlotAlternative` que HU-33 añadió; el `OR` solo cubría filas sustituidas.

#### Fase 4 — Esquema (`DB-01`)

- [x] **T19: Retirar la columna de la entidad** — `data/local/entity/SessionExerciseEntity.kt`

  Tres bloques: la tercera `ForeignKey` (`childColumns = ["original_exercise_id"]`), el `Index(value = ["original_exercise_id"])` y el campo `originalExerciseId`. Los otros tres índices y las dos FK restantes se conservan, incluido el único `Index(..., unique = true)` sobre `(session_id, exercise_id)`.

- [x] **T20: Escribir la migración v15 → v16** — `data/local/database/Migrations.kt` (Base: `MIGRATION_11_12` fase 3) (D3)

  ```kotlin
  val MIGRATION_15_16 = object : Migration(15, 16) {
      override fun migrate(db: SupportSQLiteDatabase) {
          // Muscle-group substitution removed (HU-34). original_exercise_id had a
          // single writer — the substitution itself — and the slot-alternative switch
          // always nulled it, so the column is unwritable once the feature is gone.
          //
          // SQLite before 3.35 has no DROP COLUMN, and minSdk 26 ships 3.19; the
          // column also backs a foreign key and an index. The table is recreated.
          db.execSQL(
              """
              CREATE TABLE session_exercise_new (
                  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                  session_id INTEGER NOT NULL,
                  exercise_id INTEGER,
                  progression_classification TEXT,
                  is_finalized INTEGER NOT NULL DEFAULT 0,
                  pending_selection INTEGER NOT NULL DEFAULT 0,
                  slot INTEGER NOT NULL DEFAULT 0,
                  FOREIGN KEY(session_id) REFERENCES session(id) ON DELETE CASCADE,
                  FOREIGN KEY(exercise_id) REFERENCES exercise(id) ON DELETE RESTRICT
              )
              """,
          )
          db.execSQL(
              """
              INSERT INTO session_exercise_new (id, session_id, exercise_id,
                  progression_classification, is_finalized, pending_selection, slot)
              SELECT id, session_id, exercise_id,
                  progression_classification, is_finalized, pending_selection, slot
              FROM session_exercise
              """,
          )
          db.execSQL("DROP TABLE session_exercise")
          db.execSQL("ALTER TABLE session_exercise_new RENAME TO session_exercise")
          db.execSQL("CREATE INDEX index_session_exercise_session_id ON session_exercise(session_id)")
          db.execSQL("CREATE INDEX index_session_exercise_exercise_id ON session_exercise(exercise_id)")
          db.execSQL("CREATE UNIQUE INDEX index_session_exercise_session_id_exercise_id ON session_exercise(session_id, exercise_id)")
      }
  }
  ```

  El `SELECT` copia `exercise_id` tal cual: la fila conserva el ejercicio **efectivamente ejecutado**, que es el que sus series registran. `index_session_exercise_original_exercise_id` no se recrea.

- [x] **T21: Registrar la migración** — `di/DatabaseModule.kt`

  Añadir `Migrations.MIGRATION_15_16` al final de la cadena de `addMigrations(...)`.

- [x] **T22: Subir la versión de esquema** — `data/local/database/TensionDatabase.kt`

  `version = 15` → `version = 16`.

- [x] **T23: Generar el schema JSON** — `app/schemas/…/16.json`

  Lo emite KSP al compilar. Verificar que `16.json` aparece y que su `session_exercise` no declara `original_exercise_id` ni su índice.

#### Fase 5 — Backup y restauración (`HU-19` sin regresión)

- [x] **T24: Blindar la importación frente a columnas retiradas** — `data/repository/BackupRepositoryImpl.kt` (D8)

  1. Añadir un cache de columnas reales por tabla, consultado una vez por importación:

     ```kotlin
     private fun columnsOf(db: SupportSQLiteDatabase, table: String): Set<String> {
         val columns = mutableSetOf<String>()
         db.query("PRAGMA table_info($table)").use { cursor ->
             val nameIndex = cursor.getColumnIndex("name")
             while (cursor.moveToNext()) columns.add(cursor.getString(nameIndex))
         }
         return columns
     }
     ```

  2. En el bucle de `importFromJson`, descartar toda clave ausente del esquema vigente antes de poblar el `ContentValues`. Un backup previo a v16 trae `original_exercise_id` y hoy provocaría *"table session_exercise has no column named original_exercise_id"*.
  3. Simplificar el `COALESCE(session_exercise.original_exercise_id, session_exercise.exercise_id)` de la consulta de reparación de `slot` a `session_exercise.exercise_id`.
  4. `SCHEMA_VERSION` permanece en 9 y `LEGACY_SCHEMA_VERSION` en 8: el formato del archivo no cambia y la compatibilidad con v8 se conserva.

#### Fase 6 — Tests unitarios (JVM, sin emulador)

- [x] **T25: Eliminar la suite del caso de uso** — `test/…/domain/usecase/session/SubstituteExerciseUseCaseTest.kt` (archivo completo)

- [x] **T26: Eliminar la suite del ViewModel** — `test/…/ui/session/SubstituteExerciseViewModelTest.kt` (archivo completo)

- [x] **T27: Ajustar la suite del detalle de sesión** — `test/…/domain/usecase/history/GetSessionDetailUseCaseTest.kt`

  Quitar `originalExerciseName` de las tres fixtures y **eliminar el caso** que verificaba el indicador (`assertNotNull` + `assertEquals("Curl Bíceps", …)`): comprueba una salida que la historia retira. Los demás casos —clasificación, series, descarga— se conservan.

- [x] **T28: Ajustar la fixture del ViewModel de detalle** — `test/…/ui/history/SessionDetailViewModelTest.kt`

  Quitar `originalExerciseName = null` de la fixture. Sin casos nuevos ni eliminados.

- [x] **T29: Cubrir el filtro de columnas de la importación** — `test/…/data/repository/BackupRepositoryImplTest.kt` (Base: el caso `importFromJson inserts tables in parents-first order`) (D9)

  Tres casos sobre el `mockk` de `SupportSQLiteDatabase` ya montado, interceptando `PRAGMA table_info` y capturando el `ContentValues` de `db.insert`:

  1. Una fila con `original_exercise_id` se inserta sin esa clave y con las demás intactas.
  2. Una fila sin claves ajenas se inserta idéntica (el filtro no descarta nada legítimo).
  3. La importación de un backup con la columna retirada **no lanza excepción** — el caso de regresión de HU-19 que D8 cierra.

#### Fase 7 — Documentación arquitectónica (CA-34.06)

- [x] **T30: Retirar el componente del blueprint** — `docs/architecture/architecture_blueprint.md`

  Tres puntos: §3 `UI-01` línea de trazabilidad `RF-12…RF-17` (quitar "Sustituciones en `SubstituteExerciseUseCase`" y `SubstituteExerciseScreen`, y reasignar `RF-16` a las alternativas por slot de HU-26); la fila `D-02` de la tabla de decisiones de dominio (la restricción "sustitución puntual solo con 0 series" deja de existir; la restricción equivalente de HU-26 la sustituye); y el recuento de vistas del sistema, que pasa de 27 a 26.

- [x] **T31: Retirar la pantalla y la ruta del contrato de interfaces** — `docs/architecture/interfaces_contract.md`

  - Eliminar `E3-T1` "Seleccionar Ejercicio Sustituto" completo (trigger, descripción, payload, estado de éxito) y la vista `E3` del mapa de navegación.
  - Eliminar `ERR_SUBSTITUTION_INVALID_ZONE` y `ERR_SUBSTITUTION_HAS_SETS` de la tabla de errores.
  - `F3-T1` (detalle de sesión): quitar `was_substituted` y `original_exercise_name` del payload de respuesta y la frase "(incluyendo sustituciones)".
  - `F4-T1` (historial de ejercicio): quitar "incluyendo sesiones donde fue ejecutado como sustituto".
  - `F2-T1` (intercambio de alternativa): la frase que explica que `original_exercise_id` se mantiene `NULL` pierde su objeto; se reescribe sin la columna.
  - Precedencia de carga precargada: `COALESCE(original_exercise_id, exercise_id)` → `session_exercise.exercise_id`, con la nota de D4 sobre por qué la simplificación es neutra.

- [x] **T32: Retirar la columna del modelo de dominio** — `docs/architecture/domain_and_state_model.md`

  Quitar el campo `original_exercise_id` de `session_exercise`, sus dos líneas de `// INTEGRIDAD:` (zona muscular compartida y 0 series), la mención "puede ser sustituto o alternativa" de `exercise_id`, el comentario de cabecera de la tabla y la fila `exercise 1:N session_exercise (original_exercise_id)` de la matriz de relaciones. Subir la versión de esquema documentada a 16.

#### Fase 8 — Mapa de historias (CA-34.05)

- [x] **T33: Corregir estados en §2** — `docs/domain/stories/story_mapping_index.md`

  HU-18 `Todo` → `Done`. HU-20 `Todo` → `Cancelada` con nota "superada por HU-34". HU-07 `Done` → `Retirada (HU-34)`. (D10)

- [x] **T34: Actualizar estados de HU-28 a HU-36 en §2**

  HU-28 a HU-33 pasan de `Borrador (PO)` a `Lista para Revisión` —su estado real según sus `index.md`— y HU-34 a `En Desarrollo`.

- [x] **T35: Reasignar RF16 en §7**

  `| RF16 | HU-07 |` → `| RF16 | HU-26 |` en la matriz de trazabilidad RF → HU.

- [x] **T36: Completar y corregir §10**

  Reasignar `RF16` de HU-07 a HU-26 (que hoy declara solo `RF65`); anotar HU-07 como retirada y HU-20 como cancelada; añadir las filas ausentes de **HU-27** y de **HU-28 a HU-36** con sus RF cubiertos. (D10)

- [x] **T37: Cerrar §4.6 "Inconsistencias Declaradas"**

  Las cuatro filas pasan de "pendientes de corrección" a corregidas, con referencia a HU-34 y fecha.

- [x] **T38: Recalcular §9 "Resumen de Cobertura"**

  Las cifras de "Historias Done", "Historias Todo" y "Historias en Borrador (PO)" derivan de los estados que T33 y T34 cambian; añadir la categoría "Historias canceladas". Verificar de paso el recuento "Done 20 (HU-01 a HU-15, HU-22 a HU-27)", que hoy enumera 21 historias.

#### Fase 9 — Cierre de la historia

- [x] **T39: Registrar el desarrollo** — `dev-record.md` (nuevo, patrón de HU-33)

- [x] **T40: Actualizar fases y métricas** — `index.md` (Refinamiento y Desarrollo a ✅) y `cambios.md` (entradas de refinamiento y desarrollo)

---

### Riesgos y observaciones

**El riesgo real de esta historia no está en lo que se elimina, sino en las 30 consultas que se simplifican.** Toca las rutas de progresión (HU-10, HU-11), métricas (HU-15), alertas (HU-33) y backup (HU-19), ninguna de las cuales es objeto de la historia. La mitigación es estructural, no de disciplina: al retirar la columna del esquema (D2), Room verifica en compilación cada `@Query` contra la tabla real y **el build falla** ante cualquier referencia superviviente. Un `COALESCE` olvidado no puede llegar a ejecución.

**La regresión de backup de D8 es el hallazgo más consecuente del refinamiento y no lo cubre ninguna CA.** Sin el filtro de columnas, cada backup exportado antes de esta historia dejaría de restaurarse con una excepción de SQLite. Se corrige aquí porque esta historia la causa; el criterio que lo respalda es CA-34.04 leído en su intención (no romper lo que no es objeto de la historia), no su letra.

**`SessionRepositoryImpl` no tiene tests unitarios** —depende de Room— y concentra 3 de las simplificaciones más semánticas (`progressionExerciseId`). El argumento de que son neutras es el de D4 y está verificado en código, no probado. Si algo se degrada en silencio, la prescripción de carga es el sitio donde mirar primero.

**El índice unique `(session_id, exercise_id)` sobrevive a la migración y conviene notarlo.** Era la restricción que impedía sustituir un ejercicio por otro ya presente en la sesión —la validación `existsInSession` de `substituteExercise` la duplicaba en Kotlin—. Sigue protegiendo al intercambio de alternativas de HU-26, que puede colisionar del mismo modo. Ninguna CA de esta historia lo cubre; se levanta como cobertura no verificada de HU-26.

**CA-34.05 describe un índice distinto del que existe.** Cuatro de sus seis correcciones ya estaban aplicadas y dos apuntaban a la sección equivocada (§10 en lugar de §2). Se ejecuta la intención completa —el índice queda consistente— y la diferencia queda transcrita en D10 para que no se lea como omisión.

**El wireframe no coincide con la implementación en ninguna de sus dos columnas.** `34.preview.txt` dibuja los controles en la línea del título de la tarjeta; la implementación los tiene en filas de botones bajo el nombre. El prototipo está declarado "pendiente de validación con Diseño", de modo que se respeta la implementación existente (D7) y no se rediseña la tarjeta. Si Diseño valida el wireframe, el rediseño es una historia aparte.

**No se toca ninguna migración histórica.** `MIGRATION_8_9`, `MIGRATION_9_10` y `MIGRATION_11_12` nombran `original_exercise_id` y deben seguir haciéndolo: describen el esquema de su época y una base antigua debe poder recorrer la cadena completa hasta v16.

---

### Validación manual (no automatizable)

Los tests JVM cubren el filtro de importación y las fixtures de modelo; Room cubre el SQL en compilación. Lo que sigue verifica el cableado real sobre la base de datos y la pantalla — en particular la migración, que ningún test del proyecto ejerce (D9).

1. **CA-34.01** — Iniciar sesión y recorrer los ejercicios no iniciados: ningún control debe abrir una pantalla de sustitución. Intentar navegar a mano a `substitute-exercise/1`: la ruta no debe existir.
2. **CA-34.02** — Sobre un slot con alternativa declarada y 0 series, pulsar `⇄`: el ejercicio debe intercambiarse en el sitio, sin pantalla intermedia. Registrar una serie y volver a mirar: el control debe haber desaparecido (CA-26.10).
3. **CA-34.03 (sesión activa)** — Comparar una tarjeta de slot simple contra una de slot dual: la simple no debe mostrar ningún control de cambio, y ambas deben conservar alineación, altura de imagen y 48 dp de área táctil, sin hueco donde estaba "Sustituir".
4. **CA-34.03 (historial y detalle)** — Abrir el historial de sesiones y el detalle de una sesión cerrada: ninguna línea "Sustituyó a:", ninguna etiqueta ni indicador del mecanismo.
5. **CA-34.04** — Entrar a la gestión del plan: asignar un ejercicio a un slot, removerlo y sustituirlo por otro en una versión de rutina. Las tres operaciones deben seguir funcionando.
6. **Migración v15 → v16 (D3)** — Sobre una base v15 **con datos reales** (sesiones cerradas, series, progresiones, alertas activas), instalar el build nuevo sin borrar datos. La app debe abrir sin `IllegalStateException` de Room, y deben conservarse: número de sesiones, número de series por sesión, clasificaciones de progresión, cargas prescritas y alertas.
7. **Migración con una sustitución previa** — Sobre una base v15 donde exista al menos una fila con `original_exercise_id` no nulo, migrar y abrir el detalle de esa sesión: debe mostrar el ejercicio **ejecutado** con sus series completas y sin indicador de sustitución.
8. **Backup — ida y vuelta en v16** — Exportar un backup desde el build nuevo y restaurarlo: el JSON no debe contener `original_exercise_id` y la restauración debe completarse sin error.
9. **Backup — restauración de un archivo previo (D8)** — Restaurar un backup exportado **antes** de esta historia, que sí contiene `original_exercise_id`: debe completarse sin error, y las sesiones, series y `slot` deben quedar correctos. Es el escenario que la historia rompería sin T24.
10. **CA-34.07 (zona muscular conservada)** — Abrir Analítica y verificar el volumen por grupo muscular; provocar y abrir una alerta de caída de tonelaje por grupo muscular. Ambas dependen de `exercise_muscle_zone` y deben seguir calculando igual que antes de la historia.
11. **Progresión post-sesión (D4)** — Cerrar una sesión completa y verificar que la clasificación de progresión y la carga prescrita del siguiente microciclo se calculan sobre el ejercicio ejecutado, con el mismo resultado que antes del cambio.
