## Refinamiento Técnico (Developer)
**Autor**: Esteban Colorado González | **Fecha**: 2026-09-07

---

### Contexto

La historia invierte una relación que hoy es `1 : N` y la convierte en `N : M`, y mueve un atributo del catálogo al registro. El proyecto ya tiene **el mismo movimiento resuelto dos veces**, y de ahí sale todo el patrón.

**Feature análoga leída completa: `exercise` ↔ `muscle_zone` (HU-03, HU-29).** Es la única relación `N : M` del esquema y trae los seis artefactos que esta historia necesita replicar, uno a uno:

```kotlin
// ExerciseMuscleZoneEntity.kt — tabla de unión: PK compuesta, dos FK RESTRICT,
// índice sobre la columna que no encabeza la PK
@Entity(
    tableName = "exercise_muscle_zone",
    primaryKeys = ["exercise_id", "muscle_zone_id"],
    foreignKeys = [ /* exercise RESTRICT, muscle_zone RESTRICT */ ],
    indices = [Index(value = ["muscle_zone_id"])],
)
```

```kotlin
// ExerciseDao.kt:128 — la transacción que inserta el ejercicio y sus relaciones
@Transaction
suspend fun insertExerciseWithMuscleZones(exercise, muscleZones): Long {
    val exerciseId = insert(exercise)
    insertAllMuscleZones(muscleZones.map { it.copy(exerciseId = exerciseId) })
    return exerciseId
}
```

```kotlin
// MuscleZoneDao.kt:17 — el catálogo filtrado a lo que tiene uso, vía la tabla de unión
SELECT DISTINCT mz.* FROM muscle_zone mz
INNER JOIN exercise_muscle_zone emz ON mz.id = emz.muscle_zone_id
```

```kotlin
// ExerciseSeeder.kt:19 — el sembrado recorre la lista de relaciones del modelo semilla
exercise.muscleZoneIds.forEach { id -> insertExerciseMuscleZone(db, exercise.id, id) }
```

Y el segundo precedente, para el otro lado del cambio — **un dato de presentación que pasa a vivir en la serie: `capture_unit` (HU-30)**. Es literalmente la columna vecina de la que esta historia añade:

```kotlin
// ExerciseSetEntity.kt:47
/** Unit the executant typed the weight in. Presentation only — weightKg is canonical. */
@ColumnInfo(name = "capture_unit", defaultValue = "KG")
val captureUnit: String = WeightUnit.KG.name,
```

```kotlin
// ExerciseSetDao.kt:96 — la memoria del último valor usado en el ejercicio, que es
// exactamente la forma de la preselección que pide CA-39.04
SELECT es.capture_unit FROM exercise_set es
INNER JOIN session_exercise se ON es.session_exercise_id = se.id
WHERE se.exercise_id = :exerciseId ORDER BY es.id DESC LIMIT 1
```

De `capture_unit` se hereda además el recorrido completo del dato: `ExerciseSetEntity` → `ExerciseSetData` (DAO) → `SetData` (dominio) → `RegisterSetInfo` → `RegisterSetUiState` → pantalla. El equipamiento de la serie recorre el mismo camino, con dos diferencias: **no tiene default** (es obligatorio, CA-39.04) y **gobierna la captura del peso** (CA-39.05), lo que `capture_unit` no hacía.

**Tercer precedente, para el renombrado: HU-29 con `Jalón al Pecho`.** Fija la regla de que un ejercicio renombrado conserva id, `media_resource`, zonas e historial, y que el asset **no** se renombra:

```kotlin
// ExerciseCatalog.kt:224 — nombre nuevo, asset viejo, id intacto
SeedExercise(25, "Jalón al Pecho", POLEA, listOf(DORSAL_ANCHO), "tiron_de_dorsales_polea"),
```

Los ocho renombrados de CA-39.07 se resuelven igual, y `SeedAssetsTest` sigue pasando sin tocar un solo PNG.

#### Superficie real del cambio

`equipment_type_id` no vive solo en `exercise`. La búsqueda sobre `src/` da **nueve consultas SQL** que hacen `INNER JOIN equipment_type ON e.equipment_type_id = et.id` y **nueve modelos** que arrastran un `equipmentTypeName: String` singular:

| Capa | Artefactos afectados |
|---|---|
| SQL | `ExerciseDao` ×3, `PlanAssignmentDao` ×3, `SessionExerciseDao` ×1, `EquipmentTypeDao` ×1, `ExerciseSetDao` (nuevas) |
| Dominio | `Exercise`, `PlanExercise`, `SessionExerciseDetail`, `SessionPreviewExercise`, `SetData`, `ExerciseHistoryEntry`, `RegisterSetInfo` |
| UI State | `ExerciseItem`, `ExerciseDetailItem`, `PlanVersionDetail*`, `ActiveSessionUiState` ×2, `SessionPreviewUiState`, `RegisterSetUiState` |
| Pantallas | `ExerciseDictionaryScreen`, `ExerciseDetailScreen`, `CreateExerciseScreen`, `PlanVersionDetailScreen` ×3, `SessionPreviewScreen`, `ActiveSessionScreen`, `RegisterSetScreen`, `SessionDetailScreen`, `ExerciseHistoryScreen` |

Ninguna de esas pantallas **decide** nada con el equipamiento: todas lo pintan como sufijo de un subtítulo (`"${muscleZones} · ${equipmentTypeName}"`). Por eso el cambio en la mayoría es mecánico —el singular pasa a ser un `joinToString(" · ")`— y solo tres pantallas ganan comportamiento nuevo: el formulario de ejercicio, el de serie y el historial de ejercicio.

#### El hallazgo que obliga a decidir

**`la_cadena_de_migraciones_no_tiene_huecos` (`MigrationV16ToV19Test:189`) afirma que la última migración llega a la versión del esquema.** El commit `d156723` backfilleó `16→17`, `17→18` y `18→19` justo para cerrar el fallo que dejó la app incapaz de abrir su propia base, y esa prueba es la guarda que quedó puesta. CA-39.11 pide **no entregar migración** y subir a v20. Las dos cosas no caben juntas: subir la versión sin migración rompe esa prueba.

No se resuelve borrando la prueba. Ver **D8**.

#### Dos defectos preexistentes que este cambio atraviesa

1. **El separador de `GROUP_CONCAT` no es el que se parsea.** `ExerciseDao` agrega con `GROUP_CONCAT(DISTINCT mz.name)` —SQLite no admite separador junto a `DISTINCT`, así que emite `","`— y `ExerciseRepositoryImpl:121` parte con `split(", ")`. Para los 4 ejercicios de dos zonas el resultado es **un solo elemento** `"Cuádriceps,Glúteos"`, y el filtro por zona del Diccionario (`exercise.muscleZones.any { it == selected }`) **nunca los encuentra**. No hay test que lo cubra: los de catálogo son sobre el modelo semilla y los de use case usan mocks del dominio.
   Importa aquí porque CA-39.03 exige que el filtro por equipamiento acierte cuando **alguna** opción coincide, es decir el mismo código con el mismo bug. Ver **D3**.

2. **`exercise_progression` es 1:1 con `exercise` y esta historia no lo cambia.** Es correcto y está en la frontera declarada: la progresión sigue resolviéndose por ejercicio hasta `HU-40`. Queda anotado para que no se lea como olvido.

#### Lo que NO se toca

- **El motor de decisión completo.** `DoubleThresholdRule`, `ProgressionClassificationRule`, `PlateauThresholdRule`, `DeloadNeedRule`, `PrefilledLoadRule`, `LoadVelocityRule` y el cierre de sesión no cambian ni una línea. La unidad de comparación sigue siendo el ejercicio (`HU-40`).
- **Las zonas musculares y su jerarquía** (`HU-41`).
- **`plan_assignment`**: no gana columna de equipamiento sugerido (`HU-41`). Las tres consultas de `PlanAssignmentDao` solo cambian el `JOIN` por la agregación.
- **`session_exercise`**: sin cambios. `HU-34` ya dejó `exercise_id` como el ejercicio efectivamente ejecutado, y el equipamiento vive un nivel más abajo, en la serie.
- **El tonelaje y las métricas.** `getTonnageDataBySessionIds` y `getSetDistributionBySessionIds` suman por zona muscular; el equipamiento no entra en ninguna agregación de `G`.
- **Los 37 PNG de `assets/exercises/`.** Ni uno se renombra ni se añade.
- **`is_isometric` / `is_to_technical_failure`** y el cronómetro.
- **El árbol (`N`), las alertas (`H`), el perfil (`C`), la descarga (`I`).**

---

### Decisiones técnicas

#### D1 — El catálogo de equipamiento se renumera 1..15 en el orden de CA-39.01

Los 23 tipos actuales pasan a 15 y **cambian de identificador**: `Máquina`=1, `Máquina Smith`=2, `Polea`=3, `Barra`=4, `Barra Fija`=5, `Mancuerna`=6, `Pesa Rusa`=7, `Banda Elástica`=8, `Peso Corporal`=9, `Peso Añadido`=10, `Barra EZ`=11, `TRX/Suspensión`=12, `Balón Medicinal`=13, `Rodillo de Abdomen`=14, `Paralelas/Dip Station`=15.

Se puede porque **nada sobrevive al cambio de esquema**: la única columna que hoy referencia `equipment_type.id` es `exercise.equipment_type_id`, que desaparece, y los respaldos del formato anterior quedan rechazados por CA-39.12. Conservar los ids viejos con diez huecos sería arrastrar la basura de la disyunción sin ganar nada.

El beneficio no es cosmético: **el id encoda el orden declarado**, que es el que el preview pide para la lista de casillas («en el orden de CA-39.01»), y con eso el orden de presentación no necesita una columna nueva ni una lista paralela en la UI.

#### D2 — `EquipmentTypeDao` ordena por `id`, no por `name`

Consecuencia directa de D1. Las dos consultas del DAO pasan de `ORDER BY name ASC` a `ORDER BY id ASC`. No es un orden arbitrario disfrazado: **no existe interfaz para crear tipos de equipamiento** —el único CRUD de catálogo es el de ejercicios (`D5-T1`)— así que la tabla es cerrada y sembrada, y su id es su posición declarada. Afecta también al dropdown de filtro del Diccionario, que pasa de alfabético a orden de catálogo; el preview no fija orden para ese control y el de catálogo agrupa mejor (las tres máquinas juntas, los cinco sin ejercicio al final).

#### D3 — La agregación de equipamiento usa subconsulta anidada, y de paso se corrige el separador de zonas

Para el equipamiento **no** se usa `GROUP_CONCAT(DISTINCT ...)`: se usa la forma anidada, que fija el orden y el separador a la vez.

```sql
(SELECT GROUP_CONCAT(name, '|') FROM (
    SELECT et.name AS name FROM exercise_equipment ee
    INNER JOIN equipment_type et ON ee.equipment_type_id = et.id
    WHERE ee.exercise_id = e.id
    ORDER BY et.id
 )) AS equipmentTypes
```

Tres razones sobre la alternativa `LEFT JOIN` + `GROUP_CONCAT(DISTINCT)`:

1. **El orden queda fijado** por el `ORDER BY et.id` del `SELECT` interior, que es el de D1. Con `GROUP_CONCAT(DISTINCT)` el orden lo decide el plan de ejecución y no es contractual.
2. **El separador es explícito.** `'|'` no aparece en ningún nombre de equipamiento ni de zona, así que `split("|")` es exacto. Es la corrección del defecto 1 del contexto, aplicada al dato nuevo desde el principio en lugar de heredarla.
3. **No multiplica filas.** Las consultas de `ExerciseDao` ya hacen `LEFT JOIN exercise_muscle_zone`; añadir un segundo `LEFT JOIN` sobre `exercise_equipment` produce el producto cartesiano de zonas × equipamientos y obliga a confiar en el `DISTINCT` para deshacerlo. La subconsulta escalar no toca el conjunto de filas exterior.

**Y de paso, la agregación de zonas pasa al mismo patrón.** Es una línea por consulta y convierte un filtro que hoy no encuentra 4 de los 37 ejercicios en uno que los encuentra. Está en el mismo código, en el mismo commit, y dejarlo roto teniendo la corrección delante sería deliberado. Queda anotado como corrección al paso, no como alcance: si el PO prefiere aislarlo en su propia historia, se revierte esa línea y el filtro por zona sigue como estaba.

#### D4 — `exercise_set.equipment_type_id` es `NOT NULL` sin default

`capture_unit` entró con `defaultValue = "KG"` porque existían filas previas que había que rellenar. Aquí no: el esquema cambia por instalación fresca (CA-39.11) y `exercise_set` nace vacía. Un default sería una mentira útil para nadie — y peor, permitiría insertar una serie sin equipamiento, que es exactamente lo que CA-39.04 prohíbe. La obligatoriedad se declara en los tres sitios: columna `NOT NULL`, `require` en `RegisterSetUseCase`, y botón deshabilitado en la pantalla.

`ON DELETE RESTRICT`, igual que las dos FK de `exercise_muscle_zone`. Es el otro pie de CA-39.10: aunque la interfaz impida quitar un equipamiento con historial, la base lo impide también.

#### D5 — La regla de captura de peso es una función pura y **decide por equipamiento, no por la marca del ejercicio**

CA-39.05 se escribió sobre `Dominadas` y dice «aplica a **todos** los ejercicios de peso corporal». Aplicada literalmente sobre la marca `is_bodyweight` produce un absurdo: `Crunch Abdominal`, `Sentadilla Búlgara` y `Zancadas` admiten `Peso Corporal` (CA-39.06) y **no** están marcados como peso corporal, así que el formulario les pediría teclear un peso para una serie hecha con el propio cuerpo.

La regla se implementa como composición de las dos dimensiones, en `domain/rules/ExternalLoadRule.kt`:

```kotlin
fun isCaptureEnabled(isBodyweight: Boolean, isIsometric: Boolean, equipmentName: String): Boolean = when {
    isIsometric -> false                        // el cronómetro sustituye la captura
    equipmentName == PESO_CORPORAL -> false     // el propio cuerpo no es carga externa
    equipmentName == PESO_ANADIDO -> true       // el lastre sí, y es lo único que lo es
    isBodyweight -> false                       // Barra Fija y Máquina asistida sobre Dominadas
    else -> true
}

fun requiresPositiveLoad(equipmentName: String): Boolean = equipmentName == PESO_ANADIDO
```

Verifica las cuatro cláusulas de CA-39.05 sobre `Dominadas` —`Peso Corporal` 0, `Barra Fija` 0, `Máquina` 0, `Peso Añadido` > 0— y además resuelve el caso que la CA no contempla sin contradecirla. Comparar contra el nombre en español tiene precedente directo: `LoadIncrementResolver.resolve(muscleGroup)` decide el incremento de carga contra `"Pecho"`, `"Cuádriceps"` y compañía, que son datos semilla igual que estos.

**La cuarta rama es la que importa.** `Máquina` sobre `Dominadas` es la asistida, y su contrapeso resta esfuerzo: registrarlo como peso invertiría el signo del dato y contaminaría el tonelaje y la velocidad de carga. Sobre cualquier otro ejercicio `Máquina` sí es carga y la rama no se alcanza porque `isBodyweight` es falso.

#### D6 — El campo de peso queda **visible y no editable**, no oculto

CA-39.05 dice «el campo de peso y el selector de unidad permanecen **ocultos** y el peso se registra como 0, **como hasta ahora**». Las dos mitades de la frase se contradicen: hoy el selector de unidad sí está oculto (`RegisterSetUiState.isUnitSelectorVisible`), pero el campo de peso está **visible con valor 0 y bloqueado**, con su etiqueta propia (`register_set_weight_bodyweight_label`).

Se conserva el comportamiento actual, por tres razones: «como hasta ahora» lo pide explícitamente; el preview lo dibuja así, sin ambigüedad («`▒▒0▒▒` no editable», Gray200); y ocultarlo dejaría al ejercicio isométrico —que comparte exactamente este estado— como única pantalla con el campo visible, o forzaría a cambiarlo también, fuera de alcance.

Es el punto más discutible de la historia y **está anotado como pregunta para producto**. Si la intención era ocultarlo de verdad, es un cambio de dos líneas en `WeightSection` y afecta también al isométrico.

#### D7 — La ficha de ejercicio (`D2`) edita el equipamiento sin botón de guardar

El detalle ya persiste dos atributos al instante y sin confirmación —imagen y dificultad de progresión— y el contrato lo declara así en `D2-T2`: «persiste de inmediato, sin botón de guardado — mismo patrón que el cambio de imagen». El equipamiento entra por ahí:

- Marcar una casilla añade la fila en `exercise_equipment` y el `Flow` de Room repinta.
- Desmarcar **valida antes de escribir**: si es la última (CA-39.09) o si tiene series registradas (CA-39.10), no se escribe y la casilla vuelve a su sitio con el mensaje que nombra la causa.
- Las opciones con historial se marcan con un candado, como pide el preview, resuelto con una consulta de conteo por par `(exercise_id, equipment_type_id)`.

El formulario de **creación** (`D5`) sí tiene botón, porque ya lo tiene, y ahí CA-39.09 pide literalmente que quede deshabilitado con la selección vacía.

#### D8 — La versión sube a 20 sin migración, y la guarda de continuidad se hace explícita en vez de desaparecer

CA-39.11 hereda ADR-019: `version = 20`, **sin** `MIGRATION_19_20`, **sin** `fallbackToDestructiveMigration`, `20.json` exportado. Una base v19 no abre el build nuevo y el reinicio lo hace el ejecutante desinstalando.

El conflicto con `la_cadena_de_migraciones_no_tiene_huecos` se resuelve así:

```kotlin
// Migrations.kt
/**
 * Última versión de esquema alcanzable por migración. Por debajo de ella la cadena
 * debe ser continua; el salto de aquí a `@Database(version = ...)` es la excepción
 * de ADR-019 y sube de forma deliberada, historia por historia.
 */
const val LAST_MIGRATED_VERSION = 19
```

La prueba pasa a afirmar dos cosas en lugar de una: que la cadena **no tiene huecos** hasta `LAST_MIGRATED_VERSION` —que es la guarda que evitó el fallo original— y que `Migrations.ALL.last().endVersion == LAST_MIGRATED_VERSION`. Lo que se pierde es la afirmación «la cadena llega al esquema vigente», que ADR-019 hace falsa por decisión de producto. Lo que se conserva es que **nadie pueda dejar un hueco por olvido**: subir el esquema sin migración obliga a tocar una constante con nombre y KDoc, revisable en el diff.

#### D9 — El historial de ejercicio segmenta por implemento en el `GROUP BY`, no en Kotlin

`getExerciseHistoryEntries` pasa de `GROUP BY se.id` a `GROUP BY se.id, es.equipment_type_id` y devuelve el nombre del equipamiento en el DTO. Una sesión en la que se usaron mancuerna y polea produce **dos entradas** con la misma fecha, cada una con su promedio de peso.

Es lo que hace cumplir la cláusula fuerte de CA-39.08 —«una serie de polea **nunca** se presenta en la misma serie temporal de comparación de peso que una de mancuerna»— **en el origen del dato**. Promediar en SQL sin segmentar y separar después en el ViewModel no serviría: el promedio ya habría mezclado los dos implementos y el número estaría contaminado antes de salir de la base.

`ExerciseHistoryData` gana `equipmentOptions: List<String>` derivado de las entradas, y el ViewModel un `selectedEquipment`. Con una sola opción se pinta como etiqueta, sin selector, como pide el preview.

#### D10 — El respaldo rechaza todo formato anterior y los caminos heredados se retiran

`SCHEMA_VERSION` 12 → **13**, con `exercise_equipment` en `TABLE_ORDER_INSERT` justo después de `exercise` (la FK lo exige) y junto a `exercise_muscle_zone`, su gemela.

CA-39.12 es tajante: los respaldos del formato anterior «quedan **incompatibles**» y la restauración «los rechaza con un mensaje explícito, en lugar de importarlos parcialmente». Así que `ACCEPTED_SCHEMA_VERSIONS` queda en `setOf(13)` y **se retiran** `PREVIOUS_SCHEMA_VERSION`, `LEGACY_SCHEMA_VERSION`, `LEGACY_TABLE_ORDER`, `TREE_STATE_TABLE`, `MODULE_CODE_TO_ROUTINE_ID` y `transformV8ToV9`. No se dejan en pie: código de importación inalcanzable es una promesa que la app ya no cumple, y su presencia haría creer al siguiente lector que v8 sigue soportada.

La razón por la que aquí no aplica el argumento que salvó a v11 en HU-37 —«el árbol es derivable del historial, restaurar sin él nunca deja un estado inválido»— es que **el equipamiento de la serie no es derivable de nada**. Un respaldo v12 no dice con qué implemento se hizo cada serie, y `exercise_set.equipment_type_id` es `NOT NULL`: importarlo exigiría inventar un valor por serie. Eso es precisamente la importación parcial que la CA prohíbe.

El mensaje es nuevo y específico (`import_backup_no_equipment`), con el texto del preview, en lugar de reusar `import_backup_incompatible_version`, que habla de números de versión y no de la causa.

#### D11 — `SeedExercise.equipmentTypeId` pasa a `equipmentTypeIds: List<Long>` y las constantes se reescriben

El modelo semilla es Kotlin puro y verificable en JVM; ahí es donde CA-39.01, CA-39.06 y CA-39.07 se vuelven testeables sin emulador. Los 11 `private const val` de equipamiento de `ExerciseCatalog` se sustituyen por los 15 de D1, y las 37 entradas declaran su lista en el orden exacto de la tabla de CA-39.06 — **97 relaciones**, cifra que el test fija.

`ExerciseSeeder` recorre `equipmentTypeIds` igual que ya recorre `muscleZoneIds`. Un método nuevo, `insertExerciseEquipment`, calcado de `insertExerciseMuscleZone`.

---

### Tareas de Implementación

#### Fase 1 — Esquema y catálogo semilla (CA-39.01, CA-39.02, CA-39.06, CA-39.07, CA-39.11)

- [ ] **T1: Crear la tabla de unión `exercise_equipment`** — `data/local/entity/ExerciseEquipmentEntity.kt` (Base: `entity/ExerciseMuscleZoneEntity.kt`)
  PK compuesta `(exercise_id, equipment_type_id)`, dos FK con `onDelete = RESTRICT`, índice sobre `equipment_type_id`.
- [ ] **T2: Retirar `equipment_type_id` de `ExerciseEntity` y rehacer sus índices** — `data/local/entity/ExerciseEntity.kt`
  Fuera la FK a `equipment_type`, fuera `Index(["equipment_type_id"])`, y `Index(["name", "equipment_type_id"], unique)` pasa a `Index(["name"], unique)` (CA-39.02: el nombre solo es único).
- [ ] **T3: Añadir `equipment_type_id` a `ExerciseSetEntity`** — `data/local/entity/ExerciseSetEntity.kt` (Base: la columna `capture_unit`, misma clase)
  `NOT NULL`, **sin default** (D4), FK a `equipment_type` con `RESTRICT`, índice propio.
- [ ] **T4: Sembrar los 15 tipos atómicos renumerados** — `data/local/seed/BaseDataSeeder.kt`
  `seedEquipmentTypes` pasa de 23 a 15 inserciones en el orden y con los ids de D1. Los 10 compuestos/duplicados de CA-39.01 desaparecen del sembrado.
- [ ] **T5: Reescribir el modelo semilla del ejercicio** — `data/local/seed/model/SeedExercise.kt`
  `equipmentTypeId: Long` → `equipmentTypeIds: List<Long>`.
- [ ] **T6: Reescribir el catálogo — 15 constantes, 8 renombrados, 97 relaciones** — `data/local/seed/ExerciseCatalog.kt`
  Depende de T5. Constantes de equipamiento según D1; nombres de CA-39.07 conservando id, `mediaResource`, `muscleZoneIds` y `progressionDifficulty`; listas de equipamiento en el orden de CA-39.06.
- [ ] **T7: Sembrar las relaciones ejercicio–equipamiento** — `data/local/seed/ExerciseSeeder.kt` (Base: `insertExerciseMuscleZone`, mismo archivo)
  Depende de T6. `insertExercise` deja de escribir `equipment_type_id`; nuevo `insertExerciseEquipment` recorriendo `equipmentTypeIds`.
- [ ] **T8: Registrar la entidad y subir el esquema a 20** — `data/local/database/TensionDatabase.kt`
  `ExerciseEquipmentEntity::class` en `entities`, `version = 20`. **Sin migración** (D8, ADR-019).
- [ ] **T9: Declarar `LAST_MIGRATED_VERSION` con su KDoc** — `data/local/database/Migrations.kt`
  Depende de T8. Constante = 19. `Migrations.ALL` no cambia. `DatabaseModule` no cambia.
- [ ] **T10: Exportar `20.json`** — `app/schemas/…TensionDatabase/20.json`
  Depende de T1–T8. Lo genera el build (`exportSchema = true`); se versiona en Git.

#### Fase 2 — Capa de datos: consultas (CA-39.02, CA-39.03, CA-39.08, CA-39.10)

- [ ] **T11: Reescribir las tres consultas de `ExerciseDao` y su DTO** — `data/local/dao/ExerciseDao.kt`
  `ExerciseWithDetails.equipmentTypeName: String` → `equipmentTypes: String?`. `getAll`, `getById` y `getNotInVersion` sustituyen el `INNER JOIN equipment_type` por la subconsulta anidada de D3, y la agregación de zonas pasa al mismo patrón con separador `'|'`.
- [ ] **T12: Añadir a `ExerciseDao` el CRUD de equipamiento del ejercicio** — `data/local/dao/ExerciseDao.kt` (Base: `insertExerciseWithMuscleZones`, mismo archivo)
  `insertAllEquipment`, `deleteEquipment(exerciseId, equipmentTypeId)`, `countEquipmentByExercise(exerciseId)`, `getEquipmentIdsByExercise(exerciseId)`, y `insertExerciseWithMuscleZones` pasa a `insertExerciseWithRelations` insertando también las relaciones de equipamiento en la misma `@Transaction`. `countByNameAndEquipment` → `countByName` (CA-39.02).
- [ ] **T13: Reapuntar `EquipmentTypeDao` a la tabla de unión y reordenar por id** — `data/local/dao/EquipmentTypeDao.kt` (Base: `MuscleZoneDao.getWithExercises`)
  `getWithExercises` pasa a `INNER JOIN exercise_equipment`; ambas consultas a `ORDER BY id ASC` (D2).
- [ ] **T14: Consultas de equipamiento en la serie** — `data/local/dao/ExerciseSetDao.kt` (Base: `getLastCaptureUnitForExercise`, mismo archivo)
  `getLastEquipmentTypeIdForExercise(exerciseId)` para la preselección de CA-39.04; `countSetsByExerciseAndEquipment(exerciseId, equipmentTypeId)` para el candado y el bloqueo de CA-39.10. `ExerciseSetData` gana `equipmentTypeName`, y `getSetsForSessionExercise` y `getLastHistoricalSets` lo resuelven con un `INNER JOIN equipment_type`.
- [ ] **T15: Equipamiento en las consultas de sesión** — `data/local/dao/SessionExerciseDao.kt`
  `getBySessionIdWithDetails` cambia el `LEFT JOIN equipment_type` por la subconsulta de D3 (opciones admitidas del ejercicio). `getExerciseInfoForSet` deja de necesitar equipamiento —lo resuelven T14 y T12— pero el DTO `SetExerciseInfo` no cambia.
- [ ] **T16: Segmentar el historial de ejercicio por implemento** — `data/local/dao/SessionExerciseDao.kt`
  Depende de T3. `getExerciseHistoryEntries` pasa a `GROUP BY se.id, es.equipment_type_id` con `INNER JOIN equipment_type`; `ExerciseHistoryEntryDto` gana `equipmentTypeName` (D9).
- [ ] **T17: Reescribir las tres consultas de `PlanAssignmentDao`** — `data/local/dao/PlanAssignmentDao.kt`
  Mismo cambio mecánico que T11 sobre sus dos DTOs. El plan no gana equipamiento sugerido; solo deja de leerlo de una columna que no existe.

#### Fase 3 — Dominio (CA-39.04, CA-39.05, CA-39.09, CA-39.10)

- [ ] **T18: Regla pura de captura de carga externa** — `domain/rules/ExternalLoadRule.kt` (Base: `domain/rules/LoadIncrementResolver.kt`)
  `isCaptureEnabled` y `requiresPositiveLoad` según D5, con las constantes `PESO_CORPORAL` y `PESO_ANADIDO`.
- [ ] **T19: Pluralizar el equipamiento en los modelos de dominio** — `domain/model/Exercise.kt`, `PlanExercise.kt`, `SessionExerciseDetail.kt`, `SessionPreviewExercise.kt`
  `equipmentTypeName: String` → `equipmentTypes: List<String>`.
- [ ] **T20: El equipamiento entra en la serie y en el historial** — `domain/model/ExerciseSessionData.kt`, `ExerciseHistoryEntry.kt`, `ExerciseHistoryData.kt`
  `SetData` gana `equipmentTypeName: String`; `ExerciseHistoryEntry` gana `equipmentTypeName: String`; `ExerciseHistoryData` gana `equipmentOptions: List<String>` (D9).
- [ ] **T21: `RegisterSetInfo` transporta opciones y preselección** — `domain/model/RegisterSetInfo.kt`
  Gana `equipmentOptions: List<EquipmentType>` y `preselectedEquipmentTypeId: Long`.
- [ ] **T22: Ampliar `ExerciseRepository`** — `domain/repository/ExerciseRepository.kt`
  `createExercise(equipmentTypeId)` → `equipmentTypeIds: List<Long>`; `exerciseExistsByNameAndEquipment` → `exerciseExistsByName`; nuevos `addEquipmentToExercise`, `removeEquipmentFromExercise`, `getEquipmentIdsOfExercise`, `countSetsWithEquipment`, `countEquipmentOfExercise`.
- [ ] **T23: Implementar el repositorio de ejercicios** — `data/repository/ExerciseRepositoryImpl.kt`
  Depende de T11, T12, T19, T22. El mapeo a dominio parte por `"|"` (D3) para zonas y equipamientos.
- [ ] **T24: `registerSet` persiste el equipamiento y `getRegisterSetInfo` lo resuelve** — `domain/repository/SessionRepository.kt`, `data/repository/SessionRepositoryImpl.kt`
  Depende de T14, T18, T21. `registerSet` gana `equipmentTypeId`; el peso se fuerza a 0 cuando `ExternalLoadRule.isCaptureEnabled` es falso, replicando cómo hoy se fuerza `capture_unit` a KG. `getRegisterSetInfo` resuelve opciones (T12), preselección (T14) y cae en la primera opción admitida si no hay historial.
- [ ] **T25: Propagar el equipamiento en detalle e historial** — `data/repository/SessionRepositoryImpl.kt`
  Depende de T14, T16, T20. `getSessionDetail` puebla `SetData.equipmentTypeName`; `getExerciseHistory` puebla las entradas y deriva `equipmentOptions` preservando el orden de aparición.
- [ ] **T26: Validaciones de creación de ejercicio** — `domain/usecase/catalog/CreateExerciseUseCase.kt`
  Depende de T22. `require(equipmentTypeIds.isNotEmpty())` (CA-39.09) y unicidad por nombre solo (CA-39.02).
- [ ] **T27: Casos de uso de edición del equipamiento admitido** — `domain/usecase/catalog/AddExerciseEquipmentUseCase.kt`, `RemoveExerciseEquipmentUseCase.kt` (Base: `UpdateExerciseProgressionDifficultyUseCase.kt`)
  Depende de T22. El de retirada rechaza con mensaje si queda una sola opción (CA-39.09) o si hay series registradas con ella (CA-39.10).
- [ ] **T28: `RegisterSetUseCase` exige equipamiento y valida el lastre** — `domain/usecase/session/RegisterSetUseCase.kt`
  Depende de T18, T24. `equipmentTypeId` obligatorio; `require(weightKg > 0)` cuando `requiresPositiveLoad`.

#### Fase 4 — Interfaz: Diccionario (CA-39.03, CA-39.09, CA-39.10)

- [ ] **T29: Componente reutilizable de selección múltiple de equipamiento** — `ui/catalog/components/EquipmentMultiSelector.kt` (Base: `ui/catalog/components/ProgressionDifficultySelector.kt`)
  Lista vertical de casillas en orden de catálogo, filas de 48 dp, contador `«N de 15 seleccionados»`, candado en las opciones con historial, mensaje de error al pie. Lo consumen `D5` y `D2`.
- [ ] **T30: Formulario de creación con equipamiento múltiple** — `ui/catalog/CreateExerciseUiState.kt`, `CreateExerciseViewModel.kt`, `CreateExerciseScreen.kt`
  Depende de T26, T29. `selectedEquipmentTypeId: Long?` → `selectedEquipmentTypeIds: Set<Long>`; `canSave` exige no vacío; el `ExposedDropdownMenuBox` de equipamiento se sustituye por T29.
- [ ] **T31: Ficha de ejercicio: todas las opciones y su edición** — `ui/catalog/ExerciseDetailUiState.kt`, `ExerciseDetailViewModel.kt`, `ExerciseDetailScreen.kt`
  Depende de T27, T29. Muestra las opciones unidas por `" · "` y el selector editable con persistencia inmediata (D7); los mensajes de CA-39.09 y CA-39.10 se renderizan al pie del selector.
- [ ] **T32: Filtro y listado del Diccionario contra la lista** — `ui/catalog/ExerciseDictionaryUiState.kt`, `ExerciseDictionaryViewModel.kt`, `ExerciseDictionaryScreen.kt`
  Depende de T19, T23. `equipmentTypeName == selected` → `equipmentTypes.any { it == selected }` (CA-39.03); la fila del listado une con `" · "`.
- [ ] **T33: Pantallas del plan y del preview** — `ui/catalog/PlanVersionDetail*.kt`, `ui/preview/SessionPreview*.kt`, `ui/session/ActiveSessionUiState.kt`, `ActiveSessionViewModel.kt`, `ActiveSessionScreen.kt`
  Depende de T19. Cambio mecánico del subtítulo `"${zonas} · ${equipamiento}"`.

#### Fase 5 — Interfaz: Registro de serie (CA-39.04, CA-39.05)

- [ ] **T34: Selector de equipamiento de la serie** — `ui/session/components/SetEquipmentSelector.kt` (Base: `ui/session/components/WeightUnitSelector.kt` y el `ExposedDropdownMenuBox` de `CreateExerciseScreen.kt:169`)
  Desplegable limitado a las opciones del ejercicio; con una sola opción se pinta resuelto y sin interacción (CA-39.04).
- [ ] **T35: Estado del formulario de serie** — `ui/session/RegisterSetUiState.kt`
  Depende de T18, T21. Gana `equipmentOptions`, `selectedEquipmentTypeId`, `selectedEquipmentName`, `equipmentError`; `isWeightEditable` e `isUnitSelectorVisible` pasan a derivarse de `ExternalLoadRule`; `isConfirmEnabled` exige equipamiento y, con `Peso Añadido`, peso > 0.
- [ ] **T36: ViewModel del formulario de serie** — `ui/session/RegisterSetViewModel.kt`
  Depende de T24, T28, T35. Preselección desde `RegisterSetInfo`; `onEquipmentSelected` recalcula la habilitación del peso y pone el campo a `"0"` al pasar a un equipamiento sin carga externa, o lo limpia al pasar a `Peso Añadido`; `onConfirm` envía el equipamiento.
- [ ] **T37: Pantalla del formulario de serie** — `ui/session/RegisterSetScreen.kt`
  Depende de T34, T36. El selector se coloca **sobre** `WeightSection`, como en el preview. `WeightSection` conserva el campo visible y bloqueado (D6). Etiqueta y nota de ayuda propias para `Peso Añadido`.
- [ ] **T38: Cadenas de interfaz** — `res/values/strings.xml`
  `exercise_field_equipment` pasa a «Equipamiento admitido»; nuevas: contador de seleccionados, error de selección vacía, error de retirada con historial, etiqueta y ayuda de `Peso Añadido`, etiqueta del selector de la serie, selector de implemento del historial, nota de comparación por implemento, rechazo del respaldo sin equipamiento.

#### Fase 6 — Interfaz: Historial (CA-39.08)

- [ ] **T39: Detalle de sesión pasada muestra el equipamiento de cada serie** — `ui/history/SessionDetailUiState.kt`, `SessionDetailViewModel.kt`, `SessionDetailScreen.kt`
  Depende de T20, T25. Segunda línea por serie con su implemento, como en el preview. El tonelaje del ejercicio sigue sumando todas las series.
- [ ] **T40: Historial de ejercicio segmentado por implemento** — `ui/history/ExerciseHistoryUiState.kt`, `ExerciseHistoryViewModel.kt`, `ExerciseHistoryScreen.kt`
  Depende de T16, T25. Selector de implemento cuando hay más de uno y etiqueta cuando hay uno solo; `buildTrendData` construye los puntos **solo** con las entradas del implemento seleccionado (D9).

#### Fase 7 — Respaldo y restauración (CA-39.12)

- [ ] **T41: Ampliar el formato y rechazar los anteriores** — `data/repository/BackupRepositoryImpl.kt`
  Depende de T1. `SCHEMA_VERSION` 13, `exercise_equipment` en `TABLE_ORDER_INSERT`, `ACCEPTED_SCHEMA_VERSIONS = setOf(13)`, retirada de los caminos v8 y v11/v12 (D10). El mensaje de rechazo es el nuevo de T38.

#### Fase 8 — Tests unitarios (JVM, sin emulador)

- [ ] **T42: Catálogo semilla — las tres tablas cerradas** — `test/…/data/local/seed/ExerciseCatalogTest.kt` (ampliación)
  15 tipos con sus ids y nombres exactos y ninguno con `" o "` (CA-39.01); 37 ejercicios, ninguno con lista vacía, 97 relaciones, todas dentro de `1..15` y sin duplicados (CA-39.02, CA-39.06); las 37 listas de equipamiento comparadas contra la tabla de CA-39.06; los 8 nombres nuevos y los 29 intactos, cada renombrado conservando id, asset y zonas (CA-39.07); `Dominadas` con `Barra Fija`, `Máquina` y `Peso Añadido` y su marca de peso corporal; unicidad por nombre solo. Se corrigen los nombres en `expectedHighDifficulty` y en la lista de dos zonas, y `name and equipment pairs are unique` pasa a `names are unique`.
- [ ] **T43: `ExternalLoadRule`** — `test/…/domain/rules/ExternalLoadRuleTest.kt` (Base: `LoadIncrementResolverTest.kt`)
  Las cuatro opciones de `Dominadas` de CA-39.05; `Peso Corporal` sobre un ejercicio no marcado (el caso de D5); `Máquina` sobre un ejercicio normal; isométrico; `requiresPositiveLoad`.
- [ ] **T44: Casos de uso de catálogo** — `test/…/domain/usecase/catalog/CreateExerciseUseCaseTest.kt` (nuevo), `AddExerciseEquipmentUseCaseTest.kt` (nuevo), `RemoveExerciseEquipmentUseCaseTest.kt` (nuevo)
  Base: `UpdatePlanAssignmentUseCaseTest.kt`. Lista vacía rechazada, unicidad por nombre, retirada de la última opción rechazada (CA-39.09), retirada con series rechazada y sin series permitida (CA-39.10).
- [ ] **T45: `RegisterSetUseCase`** — `test/…/domain/usecase/session/RegisterSetUseCaseTest.kt` (ampliación)
  Equipamiento obligatorio; `Peso Añadido` con peso 0 rechazado; peso forzado a 0 sin carga externa.
- [ ] **T46: `RegisterSetViewModel`** — `test/…/ui/session/RegisterSetViewModelTest.kt` (ampliación)
  Preselección con el último implemento usado y con la primera opción cuando no hay historial; una sola opción resuelta sin interacción; el cambio de implemento habilita y deshabilita el campo de peso; confirmar bloqueado sin equipamiento.
- [ ] **T47: `ExerciseHistoryViewModel`** — `test/…/ui/history/ExerciseHistoryViewModelTest.kt` (ampliación)
  Los puntos de tendencia salen solo del implemento seleccionado; con un implemento no se ofrece selector; el cambio de selección recalcula la serie temporal.
- [ ] **T48: Respaldo** — `test/…/data/repository/BackupRepositoryImplTest.kt` (ampliación), `test/…/domain/usecase/backup/ValidateBackupUseCaseTest.kt` (ampliación)
  `exercise_equipment` presente en el orden de inserción y después de `exercise`; v13 aceptada; v12, v11 y v8 rechazadas con el mensaje de equipamiento.
- [ ] **T49: Continuidad de migraciones con la excepción declarada** — `androidTest/…/MigrationV16ToV19Test.kt` (ampliación)
  Depende de T9. `la_cadena_de_migraciones_no_tiene_huecos` afirma continuidad hasta `LAST_MIGRATED_VERSION` y que la última migración termina ahí (D8).
- [ ] **T50: Integridad del sembrado sobre base real** — `androidTest/…/CatalogoSembradoTest.kt` (ampliación)
  Ningún ejercicio sin equipamiento; `equipment_type` con 15 filas; ninguna relación colgando; `exercise_equipment` consultable.

#### Fase 9 — Documentación (DoD)

- [ ] **T51: Modelo de dominio** — `docs/architecture/domain_and_state_model.md`
  §2: `exercise` sin `equipment_type_id` y con `name` único; `exercise_equipment` con su diccionario inline; `exercise_set` con `equipment_type_id`; `equipment_type` con 15 valores. §3: la relación `equipment_type 1:N exercise` pasa a `exercise N:M equipment_type` y se añade `equipment_type 1:N exercise_set`. §6.1: los 15 tipos, la tabla de 37 ejercicios con nombres y equipamientos nuevos, y `exercise_equipment` con 97 filas. Encabezado: esquema **20**, con la nota de ADR-019 y `LAST_MIGRATED_VERSION`.
- [ ] **T52: Contrato de interfaces** — `docs/architecture/interfaces_contract.md`
  `D1-T1` (filtro contra lista, `equipment_types` array), `D2-T1` (array), **`D2-T3` nuevo** (editar equipamiento admitido, con sus dos rechazos), `D5-T1` (`equipment_type_ids`, unicidad por nombre), `E2-T1` (equipamiento obligatorio y su gobierno del campo de peso), `F2-T1` (equipamiento por serie), `F3-T1` (entradas segmentadas y opciones), `J2-T1`/`J3-T1` (formato 13 y rechazo de anteriores). §3.2: `ERR_EXERCISE_NAME_DUPLICATE` reescrito, y nuevos `ERR_EQUIPMENT_REQUIRED`, `ERR_EQUIPMENT_HAS_SETS`, `ERR_BACKUP_NO_EQUIPMENT`. §4: restricción «un tipo, un implemento» y «el equipamiento de la serie no se corrige».
- [ ] **T53: Blueprint** — `docs/architecture/architecture_blueprint.md`
  §2.1: esquema **20**. Componentes nuevos en las cuatro capas. **ADR-019 ampliado** con v20 y con la forma que toma la guarda de continuidad.
- [ ] **T54: Artefactos de la historia** — `docs/domain/stories/HU-39-…/index.md`, `cambios.md`, `dev-record.md`, `docs/domain/stories/story_mapping_index.md`
  Fases, métricas de tiempo, registro cronológico, `dev-record.md` completo y el índice de mapeo con el estado de la historia.

---

### Riesgos y observaciones

**El falso `REGRESSION` sigue ocurriendo al terminar esta historia, y es lo esperado.** Estrenar un implemento distinto cambia el peso manejado y el motor lo lee como regresión, porque la unidad de comparación sigue siendo el ejercicio. No es un defecto introducido aquí: es el defecto preexistente que motivó la partición, y `HU-40` es obligatoria a continuación. **A partir de esta historia el dato para arreglarlo ya existe** — antes no.

**El campo de peso visible-y-bloqueado (D6) es la desviación más discutible.** CA-39.05 dice «ocultos» y se implementa «no editable», siguiendo el «como hasta ahora» de la misma frase y el preview. Pregunta abierta para producto.

**La corrección del separador de zonas (D3) es alcance adyacente asumido.** Arregla un filtro que hoy no encuentra 4 de los 37 ejercicios. Está en el mismo bloque de SQL que la historia reescribe de todas formas. Si el PO prefiere aislarlo, se revierte esa línea sin tocar nada más.

**Debilitar `la_cadena_de_migraciones_no_tiene_huecos` (D8) reabre parcialmente la puerta al fallo de `d156723`.** La mitigación es que el hueco deja de ser silencioso: subir el esquema sin migración obliga a mover `LAST_MIGRATED_VERSION`, que es una línea con nombre y KDoc en el diff. Es menos guarda que antes; es la que ADR-019 permite.

**Renumerar `equipment_type` (D1) es irreversible para cualquier base v19 y para todo respaldo existente.** Ya lo eran por CA-39.11 y CA-39.12; la renumeración no añade pérdida, pero sí cierra la puerta a un cambio de opinión sobre el rechazo de respaldos: un importador de v12 tendría además que traducir ids de equipamiento.

**El historial de un ejercicio entrenado con varios implementos gana filas con fecha repetida (D9).** Una sesión con mancuerna y polea aparece dos veces en la lista, una por implemento. Es lo que CA-39.08 pide y el selector lo hace legible, pero es un cambio de forma en una pantalla que hasta ahora tenía una fila por sesión.

**`exercise_progression` sigue siendo 1:1 con `exercise`.** Declarado en la frontera de alcance de la historia. La prescripción de carga que el formulario precarga es la del ejercicio, no la del par ejercicio-implemento, y por eso puede llegar un peso de polea a una serie de mancuerna. `HU-40`.

**Cinco tipos del catálogo nacen sin un solo ejercicio.** `Barra EZ`, `TRX/Suspensión`, `Balón Medicinal`, `Rodillo de Abdomen` y `Paralelas/Dip Station` los declara CA-39.01 como disponibles a futuro. `getWithExercises` los excluye del filtro del Diccionario —correcto: filtrar por algo sin resultados es ruido— pero el selector del formulario los ofrece, que es su propósito.

**No hay tests de UI instrumentados y esta historia no los añade.** `androidTest/` tiene sembrado y migración, no Compose UI Test. Las tres pantallas con comportamiento nuevo se cubren por ViewModel (T46, T47) y por regla pura (T43); el ensamblado visual queda en validación manual.

---

### Validación manual (no automatizable)

1. **Instalación fresca.** Desinstalar e instalar el build nuevo: debe abrir sin excepción de Room. Sobre una base v19 **sin desinstalar** debe fallar al abrir — es ADR-019, no un defecto.
2. **Diccionario.** `Elevación Lateral` aparece filtrando por `Mancuerna`, por `Polea` y por `Máquina`. Su ficha muestra las tres.
3. **Creación.** Guardar sin marcar equipamiento: botón deshabilitado y error al pie. Marcar dos y guardar: el ejercicio aparece con ambas.
4. **Edición con historial.** Registrar una serie de `Elevación Lateral` con `Polea`, volver a la ficha e intentar desmarcar `Polea`: se impide con el mensaje y la casilla vuelve marcada. Desmarcar `Máquina`, sin series: se permite.
5. **Registro.** Primera serie del ejercicio: preseleccionada la primera opción. Segunda: preseleccionado el implemento de la primera. `Remo Unilateral Polea Baja`: selector resuelto sin interacción.
6. **Dominadas, las cuatro opciones.** `Peso Corporal`, `Barra Fija` y `Máquina`: campo bloqueado en 0, sin selector de unidad. `Peso Añadido`: campo y `Kg`/`Lb` habilitados, confirmar bloqueado con 0.
7. **Historial.** Una sesión con dos implementos del mismo ejercicio: el detalle muestra el implemento por serie; el historial del ejercicio ofrece el selector y la gráfica cambia al cambiarlo. Un ejercicio de un solo implemento: etiqueta, sin selector.
8. **Respaldo.** Exportar y comprobar `schemaVersion: 13` y la tabla `exercise_equipment` con datos. Restaurar ese archivo: estado completo, incluido el equipamiento de las series históricas. Restaurar un respaldo v12: rechazo con el mensaje de equipamiento, sin importar nada.
