## Refinamiento Técnico (Developer)
**Autor**: Esteban Colorado González | **Fecha**: 2026-09-07

---

### Contexto

`HU-39` dejó el dato y declaró la frontera: *"`exercise_progression` sigue siendo 1:1 con `exercise`, y es la frontera declarada […] el falso `REGRESSION` por cambio de implemento sigue ocurriendo"*. Esta historia cruza esa frontera. No añade una dimensión al registro —eso ya está hecho—: **reescribe la unidad de comparación de un motor de decisión que ya funciona y está probado**.

**Feature análoga leída completa: `HU-39` (commit `94aa64f`), la historia inmediatamente anterior.** Aporta los cuatro artefactos que esta replica uno a uno:

```kotlin
// ExerciseSetEntity.kt — la columna que crea el par, con su FK y su índice
ForeignKey(entity = EquipmentTypeEntity::class, parentColumns = ["id"],
           childColumns = ["equipment_type_id"], onDelete = ForeignKey.RESTRICT)
indices = [ …, Index(value = ["equipment_type_id"]) ]
```

```kotlin
// ExerciseSetDao.kt:120 — la consulta "por par" ya existe, y es la forma exacta
// que las cinco consultas de memoria de esta historia van a adoptar
SELECT COUNT(*) FROM exercise_set es
INNER JOIN session_exercise se ON es.session_exercise_id = se.id
WHERE se.exercise_id = :exerciseId AND es.equipment_type_id = :equipmentTypeId
```

```kotlin
// SessionExerciseDao.kt:444 — la segmentación por implemento va en el GROUP BY,
// no en Kotlin. El historial ya lo hace; el motor lo va a hacer igual
GROUP BY se.id, es.equipment_type_id
```

```kotlin
// Migrations.kt:1245 — el esquema sube sin migración y la guarda es una constante
const val LAST_MIGRATED_VERSION = 19   // ADR-019
```

**Segunda análoga: `PlateauThresholdRule` + el contador (HU-32).** Aporta la separación exacta que esta historia necesita — *el umbral se compone de dos realidades distintas y el contador es agnóstico del umbral*. `resolveNewProgressionState(currentStatus, currentCounter, …, plateauThreshold)` **ya** recibe estado y contador como parámetros y el umbral como un tercero: la dificultad puede quedarse en el ejercicio mientras estado y contador se mudan al par, **sin cambiar una sola línea de la regla**. Es la propiedad que HU-32 pagó y que aquí se cobra.

**Tercera análoga: `DeloadNeedRule` y `RoutineFatigueRule`.** Son el precedente de forma de la consolidación: hechos por ejercicio agregados en una decisión de rutina, como función pura en `domain/rules/`, sin estado persistido. `ProgressionConsolidationRule` nace con esa misma forma.

#### Superficie real del cambio

El motor de decisión vive concentrado en **un solo método**: `SessionRepositoryImpl.evaluateProgression` (líneas 751-945). Alrededor hay tres anillos:

| Anillo | Qué contiene | Tamaño |
|---|---|---|
| **Núcleo** | `evaluateProgression`, `getRegisterSetInfo`, el bloque de reinicio de descarga en `closeSession` | 3 bloques en 1 archivo |
| **Consultas** | Las 5 consultas de "último valor del ejercicio" de `ExerciseSetDao` + los 4 `LEFT JOIN exercise_progression` | 2 DAOs |
| **Lectores** | Resumen post-sesión, KPIs, alertas, descarga, historial, respaldo | 8 archivos |

Las **reglas puras no cambian**: `DoubleThresholdRule`, `ProgressionClassificationRule`, `PlateauThresholdRule`, `DeloadLoadRule`, `DeloadNeedRule`, `ProgressionRateRule` y `PrefilledLoadRule` reciben datos y devuelven decisiones. Cambia **con qué datos se las invoca**, no lo que calculan. Es exactamente lo que la historia promete: *"ningún indicador cambia de fórmula"*.

#### El hallazgo que decide el diseño

`session_exercise.progression_classification` es **el único lector de la clasificación**, y lo consumen cinco consultas: la tasa de progresión (`G1`), la tendencia por grupo muscular (`G3`), la alerta `LOW_PROGRESSION_RATE`, el análisis causal de `PLATEAU` y el conteo de slots afectados para la descarga.

Si esa columna pasa a guardar **la lectura consolidada** en lugar de la del ejercicio, las cinco quedan intactas y consumen la consolidación de CA-40.04 **sin tocarse**. Es el punto de apalancamiento de toda la historia: un cambio de significado en una columna sustituye cinco reescrituras de SQL.

Lo que la columna deja de poder expresar es el renglón por par que pide el preview del resumen post-sesión. Eso vive en una tabla nueva (D3).

#### Dos defectos que el cambio de PK destapa

1. **Cuatro `LEFT JOIN exercise_progression ep ON … = ep.exercise_id`** (`PlanAssignmentDao:109` y `:187`, `SessionExerciseDao:153` y `:296`). Con PK compuesta ese join produce **una fila por par**, y los tres primeros lo colapsan con `GROUP BY se.id` / `GROUP BY e.id`: SQLite elegiría un `prescribed_load_kg` arbitrario entre los pares. Silencioso y no determinista.
2. **`countAffectedSlotsForDeload`** cuenta un slot como afectado si `ep.status = 'IN_PLATEAU'`. Con pares, **cualquier** par en meseta marcaría el slot — justo la disyunción que CA-40.05 prohíbe para la meseta.

Ninguno de los dos es alcance adyacente: los dos los **crea** el cambio de PK, y los dos van resueltos en la historia.

#### Lo que NO se toca

- **El tonelaje y el volumen por grupo muscular.** `getTonnageDataBySessionIds` y `getSetDistributionBySessionIds` no miran el implemento y siguen sin mirarlo (CA-40.07). El volumen es volumen.
- **`exercise.progression_difficulty`.** Sigue siendo del ejercicio (CA-40.01). `PlateauThresholdRule` no cambia.
- **Las siete reglas puras del motor.** Cambia el dato de entrada, no la fórmula.
- **`ExternalLoadRule`.** Gobierna la captura, no la progresión.
- **El árbol (`HU-37`, `HU-38`).** Lee tonelaje y adherencia, no comparación de pesos.
- **La reasignación de día, la rotación y el versionado de plan.** Ajenos.

---

### Decisiones técnicas

#### D1 — `exercise_progression` pasa a PK compuesta `(exercise_id, equipment_type_id)`

Es el corazón de CA-40.01. La tabla replica exactamente lo que `exercise_set` hizo en HU-39 con las mismas dos columnas: FK a `equipment_type` con `RESTRICT` e índice sobre `equipment_type_id`, que no encabeza la PK.

**Sin `defaultValue` en la columna nueva**, por el mismo argumento de HU-39 (D4): el esquema cambia por instalación fresca (ADR-019), la tabla nace vacía y no hay fila previa que rellenar. Un default permitiría escribir un estado de progresión sin el implemento que lo identifica.

**Se descartó la FK compuesta a `exercise_equipment`.** Declararía en el esquema la invariante "un par solo existe entre los implementos admitidos", que es cierta —la progresión se crea en `registerSet`, y ahí `HU-39` ya exige que el implemento esté admitido—. Se descarta porque `RESTRICT` sobre una tabla que la ficha de ejercicio edita en caliente convierte un rechazo ya cubierto por `RemoveExerciseEquipmentUseCase` en una excepción de SQLite con peor mensaje. Dos FK simples, como `exercise_set`.

#### D2 — La consolidación es una regla pura nueva y **no se persiste**

`ProgressionConsolidationRule`, en `domain/rules/`, con la forma de `DeloadNeedRule`: recibe los hechos de los pares y devuelve la lectura del ejercicio. Tres funciones:

```kotlin
/** CA-40.04 — disyunción: el ejercicio progresa si ALGUNO de sus pares progresó. */
fun isProgressing(pairStatuses: List<String>): Boolean

/** CA-40.05 — conjunción: meseta solo si TODOS los pares alcanzaron su umbral. */
fun isInPlateau(pairCounters: List<Int>, effectiveThreshold: Int): Boolean

/** CA-40.02/04 — la clasificación consolidada de la sesión. */
fun consolidate(pairClassifications: List<ProgressionClassification?>): ProgressionClassification?
```

La tercera necesita una decisión que la CA no escribe: la consolidación de CA-40.04 está enunciada como **booleana** (progresa / no progresa) y `progression_classification` tiene **tres** valores y un nulo. Se resuelve con el orden `POSITIVE_PROGRESSION > MAINTENANCE > REGRESSION > null`, quedándose con el mejor par clasificado:

| Pares de la sesión | Consolidado | Por qué |
|---|---|---|
| Mancuerna `POSITIVE`, Polea `null` | `POSITIVE` | "progresa si alguno progresó" |
| Mancuerna `MAINTENANCE`, Polea `REGRESSION` | `MAINTENANCE` | ninguno progresó, pero no todos retrocedieron |
| Mancuerna `REGRESSION`, Polea `REGRESSION` | `REGRESSION` | todos retrocedieron |
| Polea `null` (estreno, único par de la sesión) | `null` | **el caso de CA-40.02** |

Los pares sin clasificar se ignoran salvo que **todos** lo estén, y entonces el resultado es `null` — que es literalmente el bache inexistente que la historia existe para no diagnosticar.

**No se persiste ninguna fila consolidada** (CA-40.04, regla de negocio 8). El estado consolidado del ejercicio no existe en la base: se deriva en cada lectura. Lo que sí se persiste es el **resultado** de consolidar la clasificación de una sesión concreta, que no es un estado sino un hecho fechado e inmutable — la distinción es la misma que ya separa `exercise_progression.status` de `session_exercise.progression_classification`.

#### D3 — `session_exercise.progression_classification` guarda la **consolidada**, y la del par vive en una tabla nueva

Es el apalancamiento descrito arriba: las cinco consultas que leen esa columna pasan a leer la consolidación de CA-40.04 **sin una línea de SQL nueva**, que es exactamente lo que la CA pide (*"esa lectura consolidada es la que consumen la tasa de progresión, los KPIs comparativos y las alertas"*).

El renglón por par del resumen post-sesión necesita el otro dato. Tabla nueva:

```kotlin
@Entity(
    tableName = "session_exercise_progression",
    primaryKeys = ["session_exercise_id", "equipment_type_id"],
    foreignKeys = [ /* session_exercise CASCADE, equipment_type RESTRICT */ ],
    indices = [Index(value = ["equipment_type_id"])],
)
```

`CASCADE` sobre `session_exercise` porque una sesión descartada (`skipToday`, `discardSession`) ya arrastra sus `exercise_set` por la misma vía: la clasificación por par es del mismo orden de dato que la serie, no del orden del estado de progresión.

**Se consideró derivar la clasificación por par al leer el resumen** en vez de persistirla, y se descartó. El proyecto persiste la clasificación deliberadamente para que E5 y los KPIs lean **el mismo valor estable**; derivar una de las dos lecturas crearía dos fuentes para el mismo hecho, con la garantía de que divergen el día que `HU-43` permita editar la sesión del día.

#### D4 — Las cinco consultas de memoria pasan del ejercicio al par, y dos cambian de forma

| Consulta | Hoy | Con la historia | CA |
|---|---|---|---|
| `getLastWeightForSessionExercise` | último peso del slot en la sesión | `+ AND es.equipment_type_id = :eq` | 40.03 (2) |
| `getLastWeightInPreviousSession` | último peso en la sesión anterior **del ejercicio** | última sesión **en que se entrenó el par** | 40.03 (3) |
| `getLastCaptureUnitForExercise` | unidad de la última serie del ejercicio | `+ AND es.equipment_type_id = :eq` | 40.03 |
| `getPreDeloadAvgWeight` | media pre-descarga del ejercicio | del par | 40.06 |
| `getLastHistoricalSets` | series de la última sesión del ejercicio | series **del par** en la última sesión del par | 40.02 |

Las dos que cambian de forma —no basta con añadir un `AND`— son la segunda y la quinta: hoy resuelven *"la última sesión del ejercicio, y de ahí las series"*, y la subconsulta que elige la sesión **no filtra por implemento**. Una sesión en la que el ejercicio se hizo solo con polea sería "la última sesión" también para el par mancuerna, y devolvería cero filas — que el motor interpretaría como *sin historial*. El filtro tiene que entrar **dentro** de la subconsulta que elige la sesión, no fuera.

Es el mismo error de forma que HU-39 evitó en el historial (`D9`: *"agrupar solo por sesión mezclaría el peso de los dos implementos antes de que el dato saliera de la base"*), con el signo cambiado.

#### D5 — Los cuatro `LEFT JOIN exercise_progression` pasan a subconsulta consolidada

Con PK compuesta el join deja de ser `1:1`. Cada uno resuelve lo que su lector necesita:

- **`getBySessionIdWithDetails` (E1) y `getPreviewByRoutineVersionId`** exponen `prescribedLoadKg` como referencia informativa del ejercicio en una pantalla que no conoce implemento. Pasan a `MAX(prescribed_load_kg)` sobre los pares — el objetivo más alto entre implementos, coherente con la disyunción de CA-40.04.
- **`getExercisesForSummary`** deja de leer `ep` para la carga: la prescripción es del par y viaja en el renglón por par (D3). Conserva `isMastered` como *algún par `MASTERED`* — `MASTERED` es del isométrico, que no tiene carga externa y en la práctica tiene un solo par.
- **`countAffectedSlotsForDeload`** pasa a la conjunción de CA-40.05: un slot cuenta como estancado solo si **todos** los pares del ejercicio están en meseta. Se resuelve con `NOT EXISTS (… AND ep.status <> 'IN_PLATEAU')` más la exigencia de que exista al menos un par.

#### D6 — La velocidad de carga se calcula por par y se consolida por **máximo**

CA-40.07 dice que los indicadores que comparan peso *"se calculan por par y se consolidan según CA-40.04"*, pero CA-40.04 enuncia una consolidación **booleana** y la velocidad de carga es un número en kg/sesión. La traducción fiel del principio —*el ejercicio progresa si alguno de sus pares progresó*— es quedarse con **la mayor** velocidad entre los pares: un implemento que avanza no puede quedar promediado hasta desaparecer por otro que no.

Es la interpretación de una CA que no cierra el caso numérico. **Queda registrada como desviación** y la alternativa —media ponderada por sesiones— se descarta porque diluye exactamente lo que la historia existe para no diluir.

`sessionCount` del KPI pasa a ser el del par que ganó, que es la evidencia de suficiencia que le corresponde al número mostrado.

#### D7 — La alerta `PLATEAU` no gana ninguna columna

CA-40.05 pide que la alerta *"identifique qué pares están estancados"*. `alert.exercise_id` ya identifica el ejercicio, y los pares estancados se derivan en el detalle: los `exercise_progression` de ese ejercicio cuyo contador alcanza el umbral efectivo. Persistirlos duplicaría un dato que la tabla ya tiene y que puede cambiar entre la emisión y la lectura de la alerta — y en ese caso lo correcto es mostrar el estado de hoy, no el congelado.

`AlertTriggerData.PlateauTrigger` gana `stalledPairs: List<StalledPair>`; el titular y la explicación de `AlertNarrativeRule` los nombran.

#### D8 — El respaldo sube a 14 y rechaza el 13

El mecanismo es genérico —`exportToJson` recorre `TABLE_ORDER_INSERT` y vuelca columnas por cursor—, así que la tabla nueva y la columna nueva viajan solas. Lo que **no** puede viajar solo es el rechazo: un respaldo v13 tiene `exercise_progression` sin `equipment_type_id`, y la columna es `NOT NULL` y forma parte de la PK. Aceptarlo exigiría inventar un implemento por fila, que es justo lo que CA-40.09 prohíbe al decir que la restauración reproduce el estado *"sin recalcularlo"*.

`SCHEMA_VERSION` 13 → 14 y `session_exercise_progression` en `TABLE_ORDER_INSERT` después de `session_exercise` y antes de `exercise_set`. Sin caminos de compatibilidad: HU-39 ya los retiró y esta historia no los reabre.

#### D9 — `ExerciseResetLoad` gana el implemento; el panel de descarga **activa** del preview queda fuera

CA-40.06 exige que la reducción y el reinicio se calculen por par, y que el listado de reinicio los muestre así: `ExerciseResetLoad` gana `equipmentTypeName` y `I1` pinta un renglón por par.

El preview dibuja además una tabla de *"Descarga activa"* con la reducción `12.5 → 7.5` por par, que **hoy no existe en ninguna pantalla**: `DeloadState.DeloadActive` solo transporta progreso y versiones congeladas. Ninguna CA la pide y el preview está marcado *"PROTOTIPO — PENDIENTE DE VALIDACIÓN"*. **Queda fuera de alcance y anotada como pregunta para producto**, no como omisión.

#### D10 — Esquema 20 → 21 sin migración

ADR-019, igual que HU-39. `LAST_MIGRATED_VERSION` se queda en **19**: no se añade migración, así que la constante no se mueve. La guarda que HU-39 dejó (`la_cadena_de_migraciones_no_tiene_huecos` + la que impide que el esquema quede por detrás) sigue pasando sin tocarse, y eso es precisamente lo que demuestra que la guarda funciona: subir el esquema sin migración es una decisión que el diff enseña.

---

### Tareas de Implementación

#### Fase 1 — Esquema (CA-40.01, CA-40.09)

- [x] **T1: `exercise_progression` pasa a PK compuesta por el par** — `data/local/entity/ExerciseProgressionEntity.kt` (Base: `entity/ExerciseSetEntity.kt`, misma pareja de columnas)
  `@PrimaryKey` simple → `primaryKeys = ["exercise_id", "equipment_type_id"]`; nueva columna `equipment_type_id` `NOT NULL` sin default; FK a `equipment_type` con `RESTRICT`; `Index(["equipment_type_id"])` (D1).
- [x] **T2: Crear `session_exercise_progression`** — `data/local/entity/SessionExerciseProgressionEntity.kt` (Base: `entity/ExerciseEquipmentEntity.kt`)
  PK compuesta `(session_exercise_id, equipment_type_id)`, columna `progression_classification` nullable, FK `session_exercise` `CASCADE` + `equipment_type` `RESTRICT`, índice sobre `equipment_type_id` (D3).
- [x] **T3: Reescribir `ExerciseProgressionDao` por par** — `data/local/dao/ExerciseProgressionDao.kt`
  Depende de T1. `getByExerciseId` → `getByPair(exerciseId, equipmentTypeId)` y `getAllByExercise(exerciseId)`; `transitionToDeload` opera sobre los pares de los ejercicios dados; `getAllInDeload` y `getAllWithPrescribedLoad` devuelven pares. Nuevas: `getCountersByExercise(exerciseId)` para la consolidación y `getStalledPairs(exerciseId, threshold)` para D7.
- [x] **T4: Crear `SessionExerciseProgressionDao`** — `data/local/dao/SessionExerciseProgressionDao.kt` (Base: `dao/ExerciseProgressionDao.kt`)
  Depende de T2. `upsert`, `deleteBySessionExerciseId` y `getBySessionId(sessionId)` con el nombre del implemento resuelto para el resumen.
- [x] **T5: Registrar entidades y subir el esquema a 21** — `data/local/database/TensionDatabase.kt`, `di/DatabaseModule.kt`
  Depende de T1, T2, T4. `SessionExerciseProgressionEntity::class` en `entities`, `version = 21`, DAO abstracto y su `@Provides`. **Sin migración** (D10, ADR-019).
- [x] **T6: Exportar `21.json`** — `app/schemas/…TensionDatabase/21.json`
  Depende de T1-T5. Lo genera el build (`exportSchema = true`); se versiona en Git.

#### Fase 2 — Reglas puras (CA-40.02, CA-40.04, CA-40.05)

- [x] **T7: Regla de consolidación por ejercicio** — `domain/rules/ProgressionConsolidationRule.kt` (Base: `domain/rules/DeloadNeedRule.kt`)
  `isProgressing` (∨), `isInPlateau` (∧) y `consolidate` con el orden `POSITIVE > MAINTENANCE > REGRESSION > null` (D2). Kotlin puro, sin dependencias.
- [x] **T8: Documentar que el contador y el estado son del par** — `domain/rules/ProgressionClassificationRule.kt`
  Depende de T7. **Solo KDoc**: la firma de `resolveNewProgressionState` ya separa contador y umbral, y no cambia (HU-32). Se hace explícito que el llamador pasa los del par y el umbral del ejercicio (CA-40.01).

#### Fase 3 — Consultas por par (CA-40.02, CA-40.03, CA-40.05, CA-40.06)

- [x] **T9: Las cinco consultas de memoria pasan al par** — `data/local/dao/ExerciseSetDao.kt` (Base: `countSetsByExerciseAndEquipment`, mismo archivo)
  `getLastWeightForSessionExercise`, `getLastCaptureUnitForExercise` y `getPreDeloadAvgWeight` ganan `equipmentTypeId`; `getLastWeightInPreviousSession` y `getLastHistoricalSets` mueven el filtro **dentro** de la subconsulta que elige la sesión (D4). `ExerciseSetData` gana `equipmentTypeId: Long`.
- [x] **T10: Series de la sesión agrupables por par** — `data/local/dao/ExerciseSetDao.kt`
  Depende de T9. `getSetsForSessionExercise` expone `equipment_type_id` para que el motor agrupe en Kotlin; los implementos distintos de un `session_exercise` se resuelven con `getEquipmentIdsInSessionExercise(sessionExerciseId)`.
- [x] **T11: `getExercisesForSummary` deja de leer la carga del ejercicio** — `data/local/dao/SessionExerciseDao.kt`
  Depende de T1. Fuera `ep.prescribed_load_kg`; `isMastered` pasa a `EXISTS(… status = 'MASTERED')` (D5).
- [x] **T12: Resumen por par** — `data/local/dao/SessionExerciseProgressionDao.kt`
  Depende de T2, T4. Consulta que devuelve, por `session_exercise` de la sesión, el implemento, su clasificación, su media de peso, sus series y la carga prescrita **de su par**.
- [x] **T13: `getBySessionIdWithDetails` consolida la carga prescrita** — `data/local/dao/SessionExerciseDao.kt`
  Depende de T1. `LEFT JOIN exercise_progression` → subconsulta `MAX(prescribed_load_kg)` (D5).
- [x] **T14: Las dos consultas de `PlanAssignmentDao`** — `data/local/dao/PlanAssignmentDao.kt`
  Depende de T1. `getPreviewByRoutineVersionId` al mismo patrón de T13. `countAffectedSlotsForDeload` pasa a la conjunción de CA-40.05 con `NOT EXISTS` (D5).
- [x] **T15: Rangos de sesión y media de peso, por par** — `data/local/dao/SessionExerciseDao.kt`, `data/local/dao/ExerciseSetDao.kt`
  Depende de T9. `getExerciseSessionRangeByPeriod` gana variante por par (`GROUP BY se.exercise_id, es.equipment_type_id`) y `getAvgWeightByExerciseInSession` gana `equipmentTypeId`, para D6.

#### Fase 4 — Motor de decisión (CA-40.01, CA-40.02, CA-40.04, CA-40.05, CA-40.06)

- [x] **T16: `evaluateProgression` evalúa por par y consolida** — `data/repository/SessionRepositoryImpl.kt`
  Depende de T3, T4, T7, T9-T12. Por cada `session_exercise`: partir sus series por implemento; por cada par, clasificar contra el histórico **del par**, resolver estado y contador **del par** con el umbral efectivo **del ejercicio**, y calcular la carga del Doble Umbral **del par**. Escribir un `session_exercise_progression` por par y la **consolidada** en `session_exercise.progression_classification` (D2, D3). La meseta y su alerta pasan a evaluarse con `isInPlateau` sobre **todos** los pares del ejercicio (CA-40.05).
- [x] **T17: Descarga: transición y reinicio por par** — `data/repository/SessionRepositoryImpl.kt`
  Depende de T3, T9. `transitionToDeload` marca los pares; el bloque de cierre de descarga en `closeSession` calcula `calculateResetLoad` sobre la media pre-descarga **de cada par**, y un par sin historial no recibe carga (CA-40.06). El `continue` de sesión de descarga se resuelve por par.
- [x] **T18: La precarga se resuelve sobre el par** — `data/repository/SessionRepositoryImpl.kt`
  Depende de T3, T9. `getRegisterSetInfo` resuelve prescripción, memoria y unidad sobre `(exerciseId, preselectedEquipmentTypeId)`; la carga de descarga del par conserva su prioridad. Nuevo `getPrefilledLoadForPair(sessionExerciseId, equipmentTypeId)` para el recálculo al cambiar de implemento (CA-40.03).
- [x] **T19: `registerSet` crea el estado del par** — `data/repository/SessionRepositoryImpl.kt`
  Depende de T1, T3. `insertIfNotExists(ExerciseProgressionEntity(exerciseId, equipmentTypeId))` (CA-40.01).
- [x] **T20: Reinicio de descarga y estado de historial, por par** — `data/repository/SessionRepositoryImpl.kt`
  Depende de T3, T17. `getResetLoadsForCompletedDeload` devuelve un elemento por par con su implemento (D9); `getExerciseHistory` expone el estado de progresión **de cada par** en vez de uno solo.

#### Fase 5 — Dominio (CA-40.03, CA-40.04, CA-40.06, CA-40.07, CA-40.08)

- [x] **T21: Modelos de la precarga y del resumen por par** — `domain/model/RegisterSetInfo.kt`, `domain/model/PrefilledLoad.kt` (nuevo), `domain/model/ExerciseSummaryItem.kt`
  `PrefilledLoad(weightKg: Double?, captureUnit: WeightUnit)` para T18. `ExerciseSummaryItem` gana `pairs: List<ExercisePairSummary>`; con un solo par la interfaz lo colapsa (CA-40.08).
- [x] **T22: `ExerciseResetLoad` y `ExerciseHistoryData` ganan el implemento** — `domain/model/DeloadState.kt`, `domain/model/ExerciseHistoryData.kt`
  Depende de T20. `ExerciseResetLoad` gana `equipmentTypeName`; `progressionStatus: String` → `progressionStatusByEquipment: Map<String, String>` (CA-40.06, CA-40.01).
- [x] **T23: Contrato del repositorio de sesión** — `domain/repository/SessionRepository.kt`
  Depende de T18, T21. Añade `getPrefilledLoadForPair`.
- [x] **T24: Caso de uso de recálculo de precarga** — `domain/usecase/session/GetPrefilledLoadForEquipmentUseCase.kt` (Base: `usecase/session/GetRegisterSetInfoUseCase.kt`)
  Depende de T23. Resuelve peso y unidad del par al cambiar el selector (CA-40.03).
- [x] **T25: El resumen post-sesión construye los renglones por par** — `domain/usecase/session/GetSessionSummaryUseCase.kt`
  Depende de T12, T21. `ActionSignalRule` se invoca **por par** con la carga prescrita del par; la clasificación del ejercicio sigue siendo la consolidada (CA-40.02, CA-40.04).
- [x] **T26: Velocidad de carga por par, consolidada por máximo** — `domain/usecase/metrics/GetLoadVelocityUseCase.kt`, `domain/repository/MetricsRepository.kt`, `data/repository/MetricsRepositoryImpl.kt`
  Depende de T15. Un cálculo por par y se conserva el mayor, con su `sessionCount` (D6, CA-40.07). `LoadVelocityRule` no cambia.
- [x] **T27: Detalle de sesión con la clasificación del par** — `data/repository/SessionRepositoryImpl.kt`, `domain/usecase/history/GetSessionDetailUseCase.kt`
  Depende de T12, T22. El detalle de una sesión pasada muestra la clasificación del par junto a sus series, no la consolidada sobre todas.

#### Fase 6 — Alertas (CA-40.04, CA-40.05)

- [x] **T28: `PlateauTrigger` transporta los pares estancados** — `domain/model/AlertTriggerData.kt`
  `stalledPairs: List<StalledPair>` con implemento y contador (D7).
- [x] **T29: El titular y la explicación nombran los implementos** — `domain/rules/AlertNarrativeRule.kt`
  Depende de T28. `plateauHeadline` y `plateauExplanation` reciben los pares. Con un solo par el texto es **idéntico** al actual (CA-40.08).
- [x] **T30: El detalle de alerta deriva los pares** — `data/repository/AlertRepositoryImpl.kt`
  Depende de T3, T28, T29. `buildPlateauTrigger` deriva los pares por encima del umbral efectivo; `buildSuggestedAction` pasa a usar el contador **máximo** entre los pares del ejercicio, que es el que sostiene la meseta declarada.

#### Fase 7 — Interfaz (CA-40.03, CA-40.05, CA-40.06, CA-40.08)

- [x] **T31: Al cambiar de implemento se recalculan peso y unidad** — `ui/session/RegisterSetViewModel.kt`
  Depende de T24. `onEquipmentSelected` pasa a `viewModelScope.launch` e invoca T24; el campo queda vacío si el par no tiene historial y las reglas de `ExternalLoadRule` (peso 0, lastre limpio) se conservan (CA-40.03).
- [x] **T32: Resumen post-sesión con renglón por par** — `ui/session/SessionSummaryScreen.kt`, `SessionSummaryUiState.kt`
  Depende de T21, T25. Con un par se colapsa al renglón único de hoy (CA-40.08); con varios, un renglón por implemento con su señal.
- [x] **T33: Bloque "Implementos estancados" en el detalle de alerta** — `ui/alerts/AlertDetailScreen.kt`
  Depende de T28. Solo cuando hay más de un par estancado.
- [x] **T34: Estado de descarga por par** — `ui/deload/DeloadScreen.kt`
  Depende de T22. Un renglón por par en las cargas de reinicio.
- [x] **T35: El historial muestra el estado del implemento seleccionado** — `ui/history/ExerciseHistoryScreen.kt`, `ExerciseHistoryViewModel.kt`
  Depende de T22. El selector de implemento que HU-39 dejó pasa a gobernar también el estado de progresión mostrado.
- [x] **T36: Cadenas nuevas** — `res/values/strings.xml`
  Depende de T32-T35. Encabezado de implementos estancados y etiquetas de par. Español.

#### Fase 8 — Respaldo (CA-40.09)

- [x] **T37: Formato 14 con la tabla nueva y rechazo del 13** — `data/repository/BackupRepositoryImpl.kt`
  Depende de T2. `SCHEMA_VERSION` 13 → 14; `session_exercise_progression` en `TABLE_ORDER_INSERT` tras `session_exercise`; `ACCEPTED_SCHEMA_VERSIONS` sigue siendo el conjunto de uno (D8).

#### Fase 9 — Tests unitarios (JVM, sin emulador)

- [x] **T38: La regla de consolidación** — `test/…/domain/rules/ProgressionConsolidationRuleTest.kt` (Base: `DeloadNeedRuleTest.kt`)
  Disyunción con un par progresando y otro no; conjunción con uno estancado y otro no; los cuatro casos de la tabla de D2; par único (CA-40.08); lista vacía.
- [x] **T39: El caso central de CA-40.02** — `test/…/domain/usecase/session/CloseSessionUseCaseTest.kt`
  Estrenar polea con peso menor: clasificación `null`, contador del par nuevo en 0, estado y carga del par mancuerna **intactos**. Es la prueba que da nombre a la historia.
- [x] **T40: Precarga y unidad por par** — `test/…/domain/usecase/session/GetRegisterSetInfoUseCaseTest.kt`, nuevo `GetPrefilledLoadForEquipmentUseCaseTest.kt`
  Los cuatro niveles de precedencia resueltos sobre el par; par sin historial → vacío; descarga con prioridad; unidad del par.
- [x] **T41: Meseta consolidada** — ampliar `test/…/domain/rules/PlateauThresholdRuleTest.kt` más la cobertura de T38
  Un par por encima del umbral y otro por debajo: no hay meseta ni alerta. Todos por encima: meseta y alerta que nombra los pares.
- [x] **T42: Resumen y señal por par** — `test/…/domain/usecase/session/GetSessionSummaryUseCaseTest.kt` (nuevo)
  Depende de T25. Dos pares en una sesión → dos renglones; un par → uno; la carga prescrita mostrada es la del par.
- [x] **T43: Velocidad de carga consolidada** — `test/…/domain/usecase/metrics/GetLoadVelocityUseCaseTest.kt` (nuevo)
  D6: dos pares, gana el de mayor velocidad; par único idéntico al comportamiento previo.
- [x] **T44: Detalle de alerta con pares** — ampliar `test/…/domain/usecase/alerts/GetAlertDetailUseCaseTest.kt`, `ui/alerts/AlertDetailViewModelTest.kt`
  Depende de T30. Los pares estancados llegan al detalle; con un solo par el texto no cambia.
- [x] **T45: Descarga por par** — ampliar `test/…/domain/rules/DeloadLoadRuleTest.kt` y la cobertura de reinicio
  Reducción al 60% y reinicio al 90% independientes por par; par sin historial sin carga.
- [x] **T46: Recálculo al cambiar de implemento** — ampliar `test/…/ui/session/RegisterSetViewModelTest.kt`
  Depende de T31. Cambio de implemento con historial → peso del par; sin historial → vacío; sin carga externa → `"0"`.
- [x] **T47: Respaldo formato 14** — ampliar `test/…/data/repository/BackupRepositoryImplTest.kt`
  Depende de T37. `schemaVersion: 14`, la tabla nueva presente, rechazo del 13.
- [x] **T48: Adaptar los tests que cambian de firma** — `GetExerciseHistoryUseCaseTest.kt`, `GetSessionDetailUseCaseTest.kt`, `MetricsViewModelTest.kt`, `ui/history/ExerciseHistoryViewModelTest.kt`
  Mecánico: modelos que ganan implemento.
- [x] **T49: Sembrado y migración instrumentados** — `androidTest/…/CatalogoSembradoTest.kt`, `MigrationV16ToV19Test.kt`
  Depende de T5. La tabla nueva entra en la lista de tablas esperadas; la guarda de continuidad **no cambia** (D10) y debe seguir pasando.

#### Fase 10 — Documentación (DoD)

- [x] **T50: Modelo de dominio** — `docs/architecture/domain_and_state_model.md`
  Esquema 21; `exercise_progression` con PK compuesta; `session_exercise_progression`; matriz de relaciones (`exercise` `1 : N` `exercise_progression`); §5.3 el ciclo de vida pasa a ser del par y se añade la lectura consolidada.
- [x] **T51: Contrato de interfaces** — `docs/architecture/interfaces_contract.md`
  `E2-T1` (precedencia por par y recálculo), `E4-T1` (motor por par más consolidación), `E5-T1` (renglón por par), `G1-T1` (tasa y velocidad consolidadas), `G3-T1`, `H1-T1`/`H2-T1` (pares estancados), `I1-T2` (reinicio por par), `J2-T1`/`J3-T1` (formato 14).
- [x] **T52: Blueprint** — `docs/architecture/architecture_blueprint.md`
  Esquema 21; ADR-019 ampliado con v21; `ProgressionConsolidationRule` en el motor de reglas.
- [x] **T53: Índice de historias y artefactos de la HU** — `docs/domain/stories/story_mapping_index.md`, `HU-40-…/index.md`, `cambios.md`, `dev-record.md`
  `HU-40` a `Lista para Revisión`; fases, métricas de tiempo y registro cronológico.

---

### Riesgos y observaciones

**Este es el cambio más invasivo de EPIC-10, y lo es por naturaleza.** `HU-39` añadió una dimensión a un registro; esta reescribe reglas de decisión ya implementadas y probadas. El riesgo no está en el esquema —eso es mecánico— sino en que **cada consulta que hoy dice `exercise_id` y debería decir el par falla en silencio**: no lanza, devuelve el dato de otro implemento. Por eso D4 enumera las cinco y D5 los cuatro joins: la exhaustividad de esas dos listas es la mitigación.

**El caso más peligroso es el de `getLastHistoricalSets`.** Si el filtro por implemento se pone fuera de la subconsulta que elige la sesión en lugar de dentro (D4), el par mancuerna leerá *cero series* cada vez que la última sesión del ejercicio fuera solo de polea, y el motor lo clasificará como `NULL — Sin Historial` para siempre. Es un fallo que **se parece al comportamiento correcto de CA-40.02** y por eso no salta a la vista. Tiene su test en T39.

**Cambiar el significado de `session_exercise.progression_classification` (D3) es la decisión con más apalancamiento y también la más fácil de malinterpretar más adelante.** La columna se sigue llamando igual y ahora guarda otra cosa. Se mitiga con KDoc en la entidad y con la sección de `E4-T1` del contrato, pero es deuda de nombre asumida a conciencia: renombrarla exigiría tocar las cinco consultas que la historia existe para no tocar.

**La consolidación de la clasificación de tres valores (D2) es una decisión propia, no una CA.** CA-40.04 enuncia la disyunción en términos booleanos y `progression_classification` tiene tres valores. El orden elegido hace verdadera la CA literalmente; cualquier otro orden también podría, pero este es el único que además nunca produce `REGRESSION` si algún par no retrocedió.

**La velocidad de carga por máximo (D6) es la desviación más discutible.** La alternativa razonable —media ponderada— es más "justa" estadísticamente y diluye exactamente lo que la historia existe para no diluir. Pregunta abierta para producto.

**El panel de descarga activa del preview (D9) queda fuera.** Ninguna CA lo pide, `DeloadState.DeloadActive` no transporta cargas hoy y el preview está marcado como prototipo. Es la única parte del wireframe que no se implementa, y está declarada, no omitida.

**Un ejercicio con dos pares gana renglones en el resumen post-sesión.** Es lo que el preview pide, pero cambia la forma de una pantalla que hasta ahora tenía una fila por ejercicio. Para quien no alterna implementos —el caso de CA-40.08— la pantalla es **idéntica**, y eso tiene test (T42).

**`prescribed_load_kg` consolidado por `MAX` en E1 y en la vista previa de sesión (D5) es informativo, no prescriptivo.** La prescripción que gobierna la precarga es la del par y se resuelve en `getRegisterSetInfo`. Si más adelante esas pantallas quisieran mostrar la del implemento con el que se va a entrenar, tendrían que conocer el implemento, que hoy no conocen.

**Todo respaldo existente queda inservible, incluidos los generados con HU-39.** Es coherente con ADR-019 y con lo que la propia HU-39 hizo, pero conviene decirlo en voz alta: quien exportó ayer no puede restaurar mañana.

**No hay tests de UI instrumentados y esta historia no los añade.** Las cinco pantallas con comportamiento nuevo se cubren por ViewModel y por regla pura; el ensamblado visual queda en validación manual.

---

### Validación manual (no automatizable)

1. **Instalación fresca.** Desinstalar e instalar: debe abrir sin excepción de Room. Sobre una base v20 sin desinstalar debe fallar — es ADR-019, no un defecto.
2. **El caso de CA-40.02, de extremo a extremo.** `Elevación Lateral` con mancuerna en dos sesiones (11.5 → 12.0 kg), y una tercera con polea a 8.0 kg. Al cerrar: el resumen muestra *Sin historial* para polea, **no** `REGRESSION`; el contador de polea queda en 0; el estado y la carga prescrita de mancuerna no cambian.
3. **Precarga por par.** En el formulario de serie, alternar el selector entre mancuerna, polea y un implemento nunca entrenado: el peso y la unidad se recalculan en cada cambio y el tercero queda **vacío**.
4. **Meseta que no se declara (CA-40.05).** Con un par por encima del umbral efectivo y otro por debajo: no aparece alerta `PLATEAU`. Al cruzar el segundo, aparece y su detalle **nombra los dos implementos** con su contador.
5. **Ejercicio de un solo implemento (CA-40.08).** `Press de Banca Plano` con barra: resumen, métricas, historial y alertas **idénticos** a antes de la historia. Sin renglones extra ni estados vacíos.
6. **Descarga.** Activar una descarga sobre un ejercicio con dos pares: cada uno reduce al 60% de **su** carga; un par sin historial no recibe ninguna. Al cerrar la descarga, cada par reinicia al 90% de la suya, y el listado de reinicio muestra un renglón por par.
7. **Métricas.** Con dos implementos alternados, la tasa de progresión **no** baja respecto a entrenar solo con uno (CA-40.04). El tonelaje total suma las series de los dos sin distinguir (CA-40.07).
8. **Respaldo.** Exportar y comprobar `schemaVersion: 14`, `exercise_progression` con `equipment_type_id` y la tabla `session_exercise_progression` con datos. Restaurar: el estado del motor se reproduce par por par. Restaurar un respaldo v13: rechazo con mensaje, sin importar nada.
