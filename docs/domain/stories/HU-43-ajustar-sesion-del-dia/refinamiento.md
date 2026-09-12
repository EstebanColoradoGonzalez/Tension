## Refinamiento Técnico (Developer)
**Autor**: Esteban Colorado González | **Fecha**: 2026-09-11

---

### Contexto

`HU-39` volvió multivalor el equipamiento del ejercicio y `HU-41` le dio jerarquía de zonas; entre las dos dejaron el formulario `D5-T1` completo. Esta historia **no toca ninguno de los dos frentes**: abre la sesión activa a dos operaciones temporales —añadir y retirar— y reutiliza ese formulario tal cual. Es la hija 5 de 5 y la única prescindible, pero también la que más superficie de la sesión activa mueve.

**Feature análoga leída completa: `HU-26` — alternativas por puesto (`E1-T1`).** Es el precedente exacto porque resuelve el mismo problema de forma: *una operación que cambia la composición de la sesión en curso sin tocar el plan, acotada a los ejercicios con 0 series, con su hoja de selección y su escritura sobre `session_exercise`*.

```kotlin
// SessionExerciseDao.kt — el intercambio de alternativa: una sola identidad del ejercicio
// en la fila, sin ejercicio de origen. La lección de HU-34, ya aplicada.
@Query("UPDATE session_exercise SET exercise_id = :exerciseId WHERE id = :sessionExerciseId")
suspend fun switchAlternativeExercise(sessionExerciseId: Long, exerciseId: Long)
```

```kotlin
// ActiveSessionViewModel.kt:163 — la frontera de 0 series, hoy en el ViewModel.
fun onSelectAlternative(exercise: ExerciseUiItem) {
    if (!exercise.hasAlternatives) return
    if (exercise.completedSets > 0) return
    if (exercise.isFinalized) return
    …
}
```

```kotlin
// ActiveSessionScreen.kt:695 — la hoja de selección sobre la sesión activa, con su
// LazyColumn de opciones y su confirmación. La forma que reutiliza «+ Añadir ejercicio».
@Composable private fun AlternativeSelectionSheet(state, onDismiss, onAlternativeSelected, onConfirm)
```

Y la feature análoga **del lado de la composición de la sesión** es `startSession`, que es hoy el único punto que escribe filas en `session_exercise`:

```kotlin
// SessionRepositoryImpl.kt:358 — una fila por puesto, con el ejercicio primario del puesto.
val sessionExercises = bySlot.entries.sortedBy { it.key }.map { (slot, alternatives) ->
    SessionExerciseEntity(sessionId = sessionId, exerciseId = alternatives.first().exerciseId, slot = slot)
}
sessionExerciseDao.insertAll(sessionExercises)
```

Y del lado del **formulario reutilizado**, `CreateExerciseScreen` con su `CreateExerciseViewModel`, que ya valida las dos reglas que `HU-39` y `HU-41` aportaron y que esta historia no vuelve a escribir.

#### Superficie real del cambio

| Capa | Qué entra |
|---|---|
| `DB-01` | Tres columnas en `session_exercise` y una tabla nueva, `session_withdrawal`. Esquema 23 → 24, **sin migración** (ADR-019). |
| `DAT-01` | `SessionWithdrawalEntity`, `SessionWithdrawalDao`, cuatro consultas retocadas en `SessionExerciseDao`, tres métodos nuevos en `SessionRepositoryImpl`. Respaldo 16 → 17. |
| `DOM-01` | `SessionAdjustmentRule` (pura), cinco casos de uso, tres modelos de dominio, cuatro campos en modelos existentes. |
| `UI-02` | `ActiveSessionUiState`, `ActiveSessionViewModel`, `CreateExerciseViewModel`. |
| `UI-01` | `ActiveSessionScreen` (botón, menú contextual, diálogo, marca), `AddExerciseSheet` (nuevo), `CreateExerciseScreen` (un botón), `SessionDetailScreen` (marca y nota). Ninguna pantalla nueva. |

#### Los tres hallazgos que deciden el diseño

**1. CA-43.07 ya está implementado — por accidente afortunado.** `SessionDao.getActiveSessionWithRoutineVersion` cuenta `totalExercises` como *toda* fila de `session_exercise` con `pending_selection = 0`, y `completedExercises` como las `is_finalized = 1`. `closeSession` resuelve `COMPLETED`/`INCOMPLETE` con esa comparación **antes** de `finalizeAllInSession`. Un ejercicio añadido es una fila más de `session_exercise`: entra en el total, no está finalizado, y la sesión cierra `INCOMPLETE` sin escribir una sola línea. No hay que implementar CA-43.07; hay que **probar que se cumple** y no romperlo.

**2. La unicidad de CA-43.01 ya es del esquema.** `session_exercise` lleva `Index(value = ["session_id", "exercise_id"], unique = true)` desde antes de esta historia. El «no puede añadirse otra vez» está garantizado por la base; lo que falta es el rechazo con causa antes de llegar a ella, y el marcado *ya está* en la hoja de selección.

**3. `slot` no sirve para distinguir al añadido.** Los puestos del plan empiezan en `0` (`PlanAssignmentEntity.slot` con `defaultValue = "0"`), que es también el valor por defecto de la columna. Un añadido con `slot = 0` heredaría por el `LEFT JOIN` las series y repeticiones del puesto 0 del plan, y el subconsulta `alternativesInSlot` le daría las alternativas de ese puesto — justo las dos cosas que CA-43.01 y CA-43.03 prohíben. **Todo lo que hoy depende de `slot` debe quedar guardado por `is_extra = 0`.** Es el defecto más probable de esta historia y es silencioso: no falla, muestra `4 series` donde deben ir `3` y un botón de intercambio donde no debe haber ninguno.

#### Lo que NO se toca

- **El plan, íntegro.** `plan_assignment`, `routine_version`, `routine_current_version`: ni una escritura. Es la frontera principal de la historia (CA-43.08) y se sostiene por construcción, no por disciplina: ningún camino nuevo alcanza `PlanRepository`.
- **La rotación y los microciclos.** `closeSession` avanza la rotación exactamente igual; la composición ajustada no entra en ese cálculo.
- **El motor de progresión, el tonelaje, los KPIs y el 1RM.** Leen `session_exercise` y `exercise_set` sin distinguir procedencia, que es precisamente lo que CA-43.08 pide. La única condición es **no filtrar por `is_extra` en ninguna de esas consultas**, y no se filtra.
- **`HU-26`.** El intercambio de alternativa de puesto sigue funcionando igual, sobre los ejercicios del plan. Esta historia no lo sustituye ni lo modifica.
- **`ExerciseSetEntity`.** La serie no sabe ni tiene por qué saber si su ejercicio vino del plan.
- **El formulario `D5-T1`.** Se invoca, no se modifica: los mismos campos, las mismas validaciones. Solo cambia la etiqueta del botón de guardar y qué ocurre después de guardar.
- **El intercambio `original_exercise_id`.** `HU-34` lo eliminó. No vuelve.

---

### Decisiones técnicas

#### D1 — El añadido es un `session_exercise` más, con una marca y su propia prescripción

Tres columnas en `session_exercise`:

| Columna | Tipo | Para qué |
|---|---|---|
| `is_extra` | `INTEGER NOT NULL DEFAULT 0` | La marca. La leen el orden, la marca ⊕, el conteo del presupuesto y las cuatro guardas de `slot`. |
| `prescribed_sets` | `INTEGER NULL` | Series propias del añadido. `NULL` en las filas del plan: las suyas vienen de `plan_assignment`. |
| `prescribed_reps` | `TEXT NULL` | Ídem con el rango. |

Sin `original_exercise_id`, sin segunda identidad, sin ejercicio de origen: la lección explícita de `HU-34`, que la propia historia recoge.

**Las dos columnas de prescripción existen para que CA-43.03 tenga un solo sitio donde decidirse.** La alternativa era resolverla en SQL con un `CASE WHEN se.is_extra = 1 THEN 3 …` repetido en las dos consultas que la necesitan —`getBySessionIdWithDetails`, para la lista, y `getExerciseInfoForSet`, para la pantalla de registro— con los literales `'8-12'`, `'30-45_SEC'` y `'TO_TECHNICAL_FAILURE'` escritos dos veces y sin test que los ate. Persistiéndola, la regla decide una vez al insertar y el SQL solo antepone lo que la fila ya trae:

```sql
COALESCE(se.prescribed_sets, pa.sets, 4)   AS sets,
COALESCE(se.prescribed_reps, pa.reps, '8-12') AS reps,
```

De paso la fila queda autodescriptiva: leyéndola se sabe con qué se comprometió el ejecutante, sin reconstruirlo.

`is_extra` **no** se deduce de `prescribed_sets IS NOT NULL`. Serían dos hechos distintos colapsados en una columna, y el día que una fila del plan quiera prescripción propia —una descarga, un ajuste de serie— el añadido dejaría de distinguirse.

#### D2 — El retiro borra la fila y deja constancia en `session_withdrawal`

El ejercicio retirado tiene 0 series por CA-43.06, así que borrar su fila de `session_exercise` no destruye nada inmutable y lo saca de golpe de todas las consultas del sistema: cierre, clasificación, tonelaje, historial. Es lo que CA-43.04 pide literalmente —«desaparece de la sesión y no se considera en su cierre ni en su clasificación»— sin tocar ninguna de esas consultas.

Pero el retiro **no puede olvidarse**, por dos razones distintas:

1. **La invariante de CA-43.05** necesita el número de retirados en todo momento.
2. **CA-43.08 y el preview** piden que el detalle de la sesión pasada diga *«Curl Bayesian se retiró de esta sesión»* — con nombre.

De ahí una tabla mínima:

```
session_withdrawal(id, session_id → session CASCADE, exercise_id → exercise RESTRICT)
    Index(session_id), Index(session_id, exercise_id) unique
```

Solo se escribe al retirar un ejercicio **del plan**. Quitar un añadido es *deshacer*, no retirar: baja el conteo de añadidos y no deja fila. Por eso el índice único se sostiene — un ejercicio del plan solo puede retirarse una vez por sesión, porque si vuelve, vuelve como añadido.

**Alternativa descartada: borrado lógico con `is_withdrawn` en `session_exercise`.** Obligaría a añadir `AND is_withdrawn = 0` a las diez consultas que hoy leen la tabla. Olvidar una deja un ejercicio retirado contando en la progresión, el tonelaje o el cierre, y el defecto no se ve hasta que alguien compara dos cifras. El borrado real no tiene esa clase de fallo.

**Alternativa descartada: derivar los retirados de `plan_assignment`.** `retirados = puestos del plan − filas con is_extra = 0` es aritméticamente cierto hoy, pero se apoya en que la versión del plan no cambie durante la sesión, y no da el nombre que el historial necesita. Una invariante no debe depender de que otra tabla no se mueva.

#### D3 — `SessionAdjustmentRule`: las cuatro decisiones, puras y en un solo sitio

```kotlin
object SessionAdjustmentRule {
    const val DEFAULT_SETS = 3
    const val DEFAULT_REPS = "8-12"
    const val ISOMETRIC_REPS = "30-45_SEC"
    const val TECHNICAL_FAILURE_REPS = "TO_TECHNICAL_FAILURE"

    fun defaultReps(isIsometric: Boolean, isToTechnicalFailure: Boolean): String
    fun budget(addedCount: Int, withdrawnCount: Int): Int          // added − withdrawn
    fun verdictForPlanExercise(addedCount, withdrawnCount, completedSets): WithdrawalVerdict
    fun verdictForAddedExercise(addedCount, withdrawnCount, completedSets): WithdrawalVerdict
}

sealed interface WithdrawalVerdict {
    data object Allowed : WithdrawalVerdict
    data class HasSets(val count: Int) : WithdrawalVerdict
    data object BudgetExhausted : WithdrawalVerdict
    data object WouldBreakInvariant : WithdrawalVerdict
}
```

Las tres causas de rechazo del preview —*«Ya tiene 2 series registradas»*, *«Añade un ejercicio primero»*, *«Repón «Curl Bayesian» primero»*— son exactamente las tres ramas distintas de `Allowed`. Devolver un veredicto y no un booleano es lo que permite que el menú muestre **la** causa y no *una* causa: el orden de evaluación es series → presupuesto/invariante, y queda fijado en la regla, probado en JVM, no repartido entre el ViewModel y el repositorio como hoy ocurre con la frontera de 0 series de `HU-26`.

Los dos veredictos son distintos porque la aritmética lo es:

- Retirar **del plan**: exige `budget(added, withdrawn) ≥ 1`. Consume presupuesto.
- Quitar un **añadido**: exige `added − 1 ≥ withdrawn`. No genera retiro.

Con `añadidos 1 · retirados 1` los dos dan negativo por motivos distintos, y el mensaje lo refleja.

`defaultReps` traduce CA-43.03 —«los isométricos en segundos y los de fallo técnico sin límite superior»— a los tres valores que `AssignExerciseToVersionUseCase.VALID_REPS` ya admite. El añadido nace con un rango del mismo vocabulario que el plan, no con uno propio.

#### D4 — El nombre a reponer siempre existe, y se puede demostrar

El mensaje de `WouldBreakInvariant` nombra un ejercicio: el primer retiro **no repuesto**. ¿Puede el veredicto salir y la lista estar vacía? No:

> Retirar `W` ejercicios del plan exige `W` añadidos; reponer `R` de ellos deja `añadidos = A + R` con `A ≥ W`. Quitar un añadido deja `A + R − 1 ≥ W + R − 1`, que es `≥ W` en cuanto `R ≥ 1`. Luego `WouldBreakInvariant` implica `R = 0`, es decir, al menos un retiro sin reponer.

Por eso la UI puede escribir el nombre sin rama defensiva. «No repuesto» se resuelve en SQL: retiros de la sesión cuyo `exercise_id` no está hoy en `session_exercise`. La misma consulta alimenta la nota del historial (CA-43.08), que por la misma razón no debe anunciar el retiro de un ejercicio que acabó estando en la sesión.

#### D5 — Reponer no es una acción: es añadir

CA-43.05 lo dice y el preview lo subraya. No hay `RestoreWithdrawnExerciseUseCase`, no hay botón de reponer, no hay camino que devuelva el ejercicio a su puesto. Se elige del Diccionario con la misma hoja, entra al final con `is_extra = 1` y con su prescripción `3 × 8-12` —no la que el plan le daba—, y el conteo de añadidos sube. La fila de `session_withdrawal` **se conserva**: el retiro ocurrió, y la aritmética de D4 depende de que siga contado.

Que el repuesto no recupere ni su puesto ni su prescripción original es consecuencia, no descuido: *«Vuelve al final de la lista, no a su puesto original»*.

#### D6 — El ejercicio creado se guarda aunque el añadido falle, y eso es deliberado

CA-43.02 pide dos efectos en un gesto —queda en el Diccionario **y** queda en la sesión— y además exige que *permanezca en el Diccionario aunque la sesión se cierre, se abandone o el ejercicio se retire de ella*. La asimetría es del negocio: el descubrimiento del día no se pierde.

`CreateAndAddExerciseToSessionUseCase` compone los dos casos de uso **sin transacción conjunta**. Si la creación falla, no hay ejercicio ni añadido. Si la creación va y el añadido no —sesión ya cerrada entre medias, carrera improbable pero representable—, el ejercicio queda en el Diccionario y el ejecutante lo añade a mano. Envolver ambos en una transacción produciría el único resultado que la CA prohíbe: un ejercicio creado que se desvanece.

#### D7 — Las cuatro guardas de `slot`, enumeradas

Consecuencia directa del hallazgo 3. En `SessionExerciseDao.getBySessionIdWithDetails`:

1. `LEFT JOIN plan_assignment … AND se.is_extra = 0` — el añadido no hereda series ni repeticiones de ningún puesto.
2. `alternativesInSlot` envuelto en `CASE WHEN se.is_extra = 1 THEN 0 ELSE (…) END` — sin puesto no hay alternativas (CA-43.01).
3. `ORDER BY se.is_extra ASC, se.slot ASC, se.id ASC` — los añadidos al final, en orden de llegada.
4. La misma cláusula de orden en `getExercisesForSummary` y `getExercisesForSessionDetail`, para que el resumen y el historial los muestren donde el preview los dibuja.

La 2 es la más fácil de olvidar y la única que produce un botón que no debería existir.

#### D8 — El presupuesto se calcula en el ViewModel, la invariante se defiende en el repositorio

El menú contextual necesita el veredicto de **cada** ejercicio para pintarse; recalcularlo por fila en la base sería una consulta por ítem. El ViewModel tiene los tres datos —`addedCount`, `withdrawnCount` y `completedSets` de cada fila— y llama a la regla, que es pura.

Pero la UI no es la guarda. `withdrawFromSession` vuelve a evaluar el veredicto **dentro de la transacción**, con los conteos releídos, y lanza si no es `Allowed`. Es el mismo reparto que `closeSession` ya aplica a la sesión sin series: *«La interfaz deshabilita el cierre en ese estado; esta guarda cubre la ruta de datos.»*

#### D9 — El menú contextual `⋮`, y por qué no un modo de edición

El preview lo argumenta y el código lo respalda: las filas ya llevan botones de acción en línea —registrar, intercambiar, ver imagen— y la acción de retirar vive junto al ejercicio que va a desaparecer. Un `IconButton` con `DropdownMenu` se suma a esa fila sin reestructurarla y sin introducir un modo con entrada y salida.

El ítem deshabilitado **muestra su causa** como línea de apoyo dentro del propio ítem, no como un `Snackbar` posterior: el preview dibuja la causa en el menú, y decirlo antes de tocar es mejor que decirlo después.

Las tres filas —`NotStartedExerciseRow`, `InProgressExerciseRow`, `CompletedExerciseRow`— reciben el mismo menú. En las dos últimas el ítem siempre sale deshabilitado por `HasSets`, y eso es exactamente lo que el preview dibuja con el candado 🔒: la acción se ve, y se ve por qué no se puede.

#### D10 — `create-exercise?sessionId={id}`: el mismo destino, un argumento opcional

CA-43.02 exige el mismo formulario «sin recortes». Duplicar la pantalla para cambiar qué ocurre al guardar sería la peor lectura posible de esa exigencia. La ruta gana un argumento opcional; `CreateExerciseViewModel` lo lee del `SavedStateHandle` y elige el caso de uso; el botón cambia de `Guardar` a `Guardar y añadir`. Nada más.

`TensionNavHost` oculta además la barra inferior cuando `create-exercise` se alcanza desde `active-session`, con la misma guarda de `previousBackStackEntry` que ya usa `exercise-detail`. Sin ella, la sesión activa —que nunca muestra barra— la haría aparecer a mitad del flujo.

#### D11 — El detalle de la sesión sigue mostrando lo **ejecutado**

El preview dibuja `Press Pallof 0 series ⊕` en el historial, pero `getExercisesForSessionDetail` tiene `HAVING setCount > 0` desde antes de esta historia, y por eso un ejercicio del plan sin series tampoco aparece hoy. La CA manda sobre el prototipo: *«muestra los ejercicios que realmente se ejecutaron»*. **Se conserva el `HAVING`**, y con él la coherencia: si el añadido sin series no apareciera y el del plan sin series sí, o al revés, el historial mentiría sobre uno de los dos.

Lo que sí entra es la marca ⊕ en los añadidos **que sí tienen series** y la nota que nombra los retiros no repuestos. Queda anotado como divergencia consciente del preview, que es un prototipo declarado pendiente de validación.

#### D12 — Esquema 24 sin migración, respaldo 17 rechazando el 16

`LAST_MIGRATED_VERSION` sigue en 19; el KDoc suma la quinta historia que sube el esquema sin migrar (ADR-019, heredado por CA-39.11).

El respaldo **no tenía más remedio** que subir: `validateBackup` recorre `TABLE_ORDER_INSERT` y rechaza por incompleto cualquier respaldo al que le falte una tabla de la lista, así que en cuanto `session_withdrawal` entra en el orden, ningún v16 pasa la validación. Aceptarlos exigiría un orden por versión, que es infraestructura nueva para restaurar respaldos de una beta.

El motivo de fondo también se sostiene solo: **el ajuste no se deriva de nada.** Un respaldo v16 no dice qué ejercicio se añadió ni cuál se retiró, y el historial restaurado presentaría una sesión cuya composición no coincide con la de su plan sin explicar por qué. `session_withdrawal` entra detrás de `session` y de `exercise`, sus dos padres. Las tres columnas de `session_exercise` viajan solas: el mecanismo vuelca columnas por cursor.

#### D13 — `E1` gana dos triggers, y ninguna pantalla nueva

`E1-T2` (añadir ejercicio a la sesión) y `E1-T3` (retirar de la sesión), más la nota en `D5-T1` sobre su invocación desde la sesión y en `F2-T1` sobre la marca y el aviso de retiro. `E1-T1` —el intercambio de alternativa— se documenta explícitamente como **no sustituido**, porque la historia nombra esa frontera y el contrato debe nombrarla también.

---

### Tareas de Implementación

#### Fase 1 — Esquema (CA-43.01, CA-43.04, CA-43.05)

- [x] **T1: `session_exercise` gana la marca y su prescripción** — `data/local/entity/SessionExerciseEntity.kt` (Base: `entity/ExerciseSetEntity.kt`)
  `is_extra INTEGER NOT NULL DEFAULT 0`, `prescribed_sets INTEGER NULL`, `prescribed_reps TEXT NULL` (D1). KDoc que declara por qué el añadido **no** lleva ejercicio de origen —la lección de `HU-34`— y por qué la prescripción se persiste en vez de resolverse en SQL.
- [x] **T2: `SessionWithdrawalEntity`** — `data/local/entity/SessionWithdrawalEntity.kt` (nuevo) (Base: `entity/SessionExerciseEntity.kt`, `entity/DeloadFrozenVersionEntity.kt`)
  Tabla `session_withdrawal`. FK `CASCADE` a `session`, FK `RESTRICT` a `exercise`, `Index(["session_id"])` e índice único `(session_id, exercise_id)`. KDoc con el argumento de D2: por qué se borra la fila y se guarda el retiro, y por qué el índice único se sostiene.
- [x] **T3: `SessionWithdrawalDao`** — `data/local/dao/SessionWithdrawalDao.kt` (nuevo) (Base: `dao/DeloadFrozenVersionDao.kt`, `dao/SessionExerciseDao.kt`)
  Depende de T2. `insert`; `countBySession(sessionId): Int`; `observeBySession(sessionId): Flow<List<WithdrawnExerciseDto>>` y `getPendingBySession(sessionId): List<WithdrawnExerciseDto>`, ambas con `INNER JOIN exercise` y el filtro de «no repuesto» —`exercise_id NOT IN (SELECT exercise_id FROM session_exercise WHERE session_id = :sessionId)`— resuelto en SQL (D4). El DTO acompaña al DAO.
- [x] **T4: Registrar entidad y DAO, esquema a 24** — `data/local/database/TensionDatabase.kt`
  Depende de T1, T2, T3. `version = 24`. **Sin migración**.
- [x] **T5: KDoc de `LAST_MIGRATED_VERSION`** — `data/local/database/Migrations.kt`
  Depende de T4. Una frase: `HU-43` lleva el esquema a la 24, también sin migración (ADR-019).
- [x] **T6: Proveer el DAO** — `di/DatabaseModule.kt`
  Depende de T3, T4. Un `@Provides` más, igual que `provideDeloadFrozenVersionDao`.
- [x] **T7: Exportar `24.json`** — `app/schemas/…TensionDatabase/24.json`
  Depende de T4. Lo genera el build (`exportSchema = true`); se versiona en Git.

#### Fase 2 — La regla (CA-43.03, CA-43.04, CA-43.05, CA-43.06)

- [x] **T8: `SessionAdjustmentRule` y `WithdrawalVerdict`** — `domain/rules/SessionAdjustmentRule.kt` (nuevo) (Base: `rules/OneRmRule.kt`, `rules/ExternalLoadRule.kt`)
  Las cuatro constantes, `defaultReps`, `budget` y los dos veredictos (D3). Kotlin puro, sin Android, sin acceso a datos. KDoc con el orden de evaluación —series antes que presupuesto— y con la demostración de D4 sobre por qué el nombre a reponer siempre existe.

#### Fase 3 — Consultas (CA-43.01, CA-43.03, CA-43.08)

- [x] **T9: Las cuatro guardas de `slot` y la prescripción propia** — `data/local/dao/SessionExerciseDao.kt`
  Depende de T1. En `getBySessionIdWithDetails`: `COALESCE(se.prescribed_sets, pa.sets, 4)` y su gemela de repeticiones; `AND se.is_extra = 0` en el `LEFT JOIN plan_assignment`; `alternativesInSlot` anulado para los añadidos; `ORDER BY se.is_extra ASC, se.slot ASC, se.id ASC`. `isExtra` se suma al DTO. En `getExerciseInfoForSet`, el mismo `COALESCE` para `totalSets` y `reps`. En `getExercisesForSummary` y `getExercisesForSessionDetail`, el `is_extra` en el `ORDER BY` y en el DTO del segundo (D7, D11). **Ninguna consulta filtra por `is_extra`**: lo añadido cuenta como lo demás (CA-43.08).
- [x] **T10: Conteo de añadidos** — `data/local/dao/SessionExerciseDao.kt`
  Depende de T1. `countExtrasInSession(sessionId): Int` y `observeExtraCount(sessionId): Flow<Int>`; `getById(sessionExerciseId): SessionExerciseEntity?` para que el retiro lea la fila que va a borrar; `deleteById(sessionExerciseId)`.

#### Fase 4 — Dominio y datos (CA-43.01, CA-43.02, CA-43.04, CA-43.05, CA-43.06)

- [x] **T11: Modelos de dominio** — `domain/model/AddableExercise.kt` (nuevo), `domain/model/SessionAdjustment.kt` (nuevo), `domain/model/SessionExerciseDetail.kt`, `domain/model/SessionDetail.kt`, `domain/model/SessionDetailExercise.kt`
  Depende de T8. `AddableExercise` con `isAlreadyInSession` (CA-43.01); `SessionAdjustment(addedCount, withdrawnCount, pendingWithdrawals)` con `budget` derivado de la regla; `isExtra` en `SessionExerciseDetail` y en `SessionDetailExercise`; `withdrawnExerciseNames` en `SessionDetail`.
- [x] **T12: Contrato del repositorio** — `domain/repository/SessionRepository.kt`
  Depende de T11. `getAddableExercises(sessionId): Flow<List<AddableExercise>>`, `addExerciseToSession(sessionId, exerciseId): Long`, `withdrawFromSession(sessionExerciseId)`, `getSessionAdjustment(sessionId): Flow<SessionAdjustment>`. KDoc de cada uno con la CA que lo obliga.
- [x] **T13: `addExerciseToSession`** — `data/repository/SessionRepositoryImpl.kt`
  Depende de T1, T8, T12. Dentro de `database.withTransaction`: sesión `IN_PROGRESS` —y es la sesión indicada—, ejercicio no presente ya (CA-43.01), lectura de `is_isometric` / `is_to_technical_failure`, prescripción por `SessionAdjustmentRule`, inserción con `isExtra = 1` y `slot = 0`. Comentario que fija por qué `slot` es indiferente aquí y peligroso fuera (D7).
- [x] **T14: `withdrawFromSession`** — `data/repository/SessionRepositoryImpl.kt`
  Depende de T3, T8, T10, T12. Dentro de `database.withTransaction`: fila y sesión, 0 series (CA-43.06), conteos releídos, veredicto según sea del plan o añadido, `IllegalStateException` si no es `Allowed`. Si es del plan: `session_withdrawal` primero, borrado después. Si es añadido: solo borrado (D2, D8).
- [x] **T15: Lecturas del ajuste** — `data/repository/SessionRepositoryImpl.kt`
  Depende de T3, T10, T12. `getAddableExercises` combinando el catálogo con los ids ya presentes; `getSessionAdjustment` combinando el conteo de añadidos con los retiros; `getSessionDetail` sumando `withdrawnExerciseNames` (CA-43.08) e `isExtra`.
- [x] **T16: Casos de uso** — `domain/usecase/session/AddExerciseToSessionUseCase.kt`, `WithdrawFromSessionUseCase.kt`, `GetAddableExercisesUseCase.kt`, `GetSessionAdjustmentUseCase.kt` (nuevos) y `domain/usecase/catalog/CreateAndAddExerciseToSessionUseCase.kt` (nuevo) (Base: `usecase/session/RegisterSetUseCase.kt`, `usecase/plan/AssignExerciseToVersionUseCase.kt`)
  Depende de T12. El quinto compone `CreateExerciseUseCase` y `AddExerciseToSessionUseCase` **sin transacción conjunta**, con el KDoc que argumenta la asimetría (D6). Ninguno reimplementa las validaciones del formulario: ya viven en `CreateExerciseUseCase`.

#### Fase 5 — Interfaz de la sesión activa (CA-43.01, CA-43.02, CA-43.04, CA-43.05, CA-43.06)

- [x] **T17: `ActiveSessionUiState`** — `ui/session/ActiveSessionUiState.kt`
  Depende de T11. `ExerciseUiItem` gana `isExtra` y `withdrawalBlockReason: String?` —`null` habilita el ítem—. Estado nuevo `AddExerciseSheetState(isVisible, query, exercises, isAdding)` y `WithdrawDialogState(isVisible, sessionExerciseId, exerciseName)`. `addedCount`, `withdrawnCount` y `budget` en el estado, para la hoja y el diálogo.
- [x] **T18: `ActiveSessionViewModel`** — `ui/session/ActiveSessionViewModel.kt`
  Depende de T16, T17. El `combine` existente suma `getSessionAdjustmentUseCase`; cada ítem resuelve su veredicto con la regla y lo traduce a cadena (D8). `onAddExerciseRequested`, `onQueryChanged`, `onExerciseChosen`, `onWithdrawRequested`, `onWithdrawConfirmed`, `onDismiss…`. Los fallos del retiro llegan por `errorMessage`, que la pantalla ya muestra en `Snackbar`.
- [x] **T19: `AddExerciseSheet`** — `ui/session/components/AddExerciseSheet.kt` (nuevo) (Base: `ActiveSessionScreen.AlternativeSelectionSheet`, `PlanVersionDetailScreen.AssignExerciseSheet`)
  Depende de T17. Campo de búsqueda, `LazyColumn` del Diccionario con los ya presentes **visibles y no seleccionables**, etiquetados «ya está» (CA-43.01), nota fija de que el ajuste es temporal y el plan no cambia, y al pie `+ Crear ejercicio nuevo` (CA-43.02).
- [x] **T20: `ActiveSessionScreen`** — `ui/session/ActiveSessionScreen.kt`
  Depende de T18, T19. Botón `+ Añadir ejercicio` como `item` del `LazyColumn`, **antes** de `Cerrar sesión`; menú `⋮` con `DropdownMenu` en las tres filas, con la causa como línea de apoyo del ítem deshabilitado (D9); diálogo de confirmación que enuncia que el plan no cambia; marca ⊕ y línea «añadido a esta sesión» en las filas con `isExtra`, y **sin** número de puesto ni botón de intercambio.
- [x] **T21: Crear ejercicio desde la sesión** — `ui/navigation/NavigationRoutes.kt`, `ui/navigation/TensionNavHost.kt`, `ui/catalog/CreateExerciseViewModel.kt`, `ui/catalog/CreateExerciseUiState.kt`, `ui/catalog/CreateExerciseScreen.kt`
  Depende de T16. Ruta `create-exercise?sessionId={sessionId}` con argumento opcional y `createExerciseRoute(sessionId: Long?)`; el ViewModel lo lee del `SavedStateHandle` y elige el caso de uso; `isFromSession` en el estado cambia la etiqueta del botón a «Guardar y añadir»; guarda de `previousBackStackEntry` para ocultar la barra inferior (D10). **El formulario no cambia**.
- [x] **T22: Cadenas** — `res/values/strings.xml`
  Depende de T19, T20, T21. Botón de añadir, título y nota de la hoja, etiqueta «ya está», crear nuevo, ítem de retirar, las tres causas —con el nombre del ejercicio a reponer como parámetro—, diálogo de retiro, marca de añadido, «Guardar y añadir», nota de retiro del historial. Todas en español.

#### Fase 6 — Historial (CA-43.08)

- [x] **T23: Marca y nota en el detalle de la sesión** — `ui/history/SessionDetailScreen.kt`
  Depende de T15. ⊕ junto a los añadidos **con series**; línea informativa que nombra los retiros no repuestos. Se conserva el `HAVING setCount > 0` (D11).

#### Fase 7 — Respaldo

- [x] **T24: Formato 17** — `data/repository/BackupRepositoryImpl.kt`
  Depende de T2. `session_withdrawal` en `TABLE_ORDER_INSERT` detrás de `session_exercise`; `SCHEMA_VERSION = 17`; `ACCEPTED_SCHEMA_VERSIONS` solo el vigente. KDoc con el motivo del rechazo del 16 (D12).
- [x] **T25: Mensaje de rechazo del formato 16** — `res/values/strings.xml`
  Depende de T24. Cadena nueva: el respaldo anterior no registra el ajuste de la sesión, y el ajuste no se deriva del plan.

#### Fase 8 — Tests

- [x] **T26: `SessionAdjustmentRuleTest`** — `test/…/domain/rules/SessionAdjustmentRuleTest.kt` (nuevo) (Base: `rules/OneRmRuleTest.kt`)
  Depende de T8. `defaultReps` en sus tres ramas. `budget` en la secuencia completa del preview: `0·0 → 0`, `1·0 → 1`, `1·1 → 0`, `2·1 → 1`. Veredicto del plan: sin añadidos → `BudgetExhausted`; con presupuesto → `Allowed`; con series → `HasSets` **aunque haya presupuesto** —el orden de evaluación—. Veredicto del añadido: `1·1` → `WouldBreakInvariant`; `2·1` → `Allowed`; con series → `HasSets`.
- [x] **T27: Tests de casos de uso** — `test/…/domain/usecase/session/AddExerciseToSessionUseCaseTest.kt`, `WithdrawFromSessionUseCaseTest.kt`, `GetAddableExercisesUseCaseTest.kt`, `GetSessionAdjustmentUseCaseTest.kt`, `test/…/domain/usecase/catalog/CreateAndAddExerciseToSessionUseCaseTest.kt` (nuevos) (Base: `usecase/session/RegisterSetUseCaseTest.kt`)
  Depende de T16. Añadir delega y propaga; el duplicado se rechaza (CA-43.01); retirar propaga el fallo del veredicto; los ya presentes salen marcados y **no filtrados**; el compuesto crea y añade, y **si el añadido falla el ejercicio sigue creado** (D6).
- [x] **T28: `ActiveSessionViewModelTest`** — `test/…/ui/session/ActiveSessionViewModelTest.kt` (nuevo) (Base: `ui/session/RegisterSetViewModelTest.kt`)
  Depende de T18. Es el test que cubre lo que el usuario ve: sin añadidos, **todos** los ítems del plan salen con causa «añade un ejercicio primero»; con uno añadido, los de 0 series quedan habilitados y los de series conservan su causa; con `1·1`, el añadido sale con la causa que **nombra** el retirado; tras reponer, queda habilitado. El orden de la lista deja los añadidos al final.
- [x] **T29: `BackupRepositoryImplTest` acompaña al formato 17** — `test/…/data/repository/BackupRepositoryImplTest.kt`
  Depende de T24. `PREVIOUS_SCHEMA_VERSION` pasa a 16; el vigente es 17; `session_withdrawal` viaja y se inserta **después** de `session` y `exercise`; un v16 se rechaza; un respaldo vigente sin la tabla se rechaza por incompleto.
- [x] **T30: `AjustarSesionDelDiaTest` (instrumentado)** — `androidTest/…/AjustarSesionDelDiaTest.kt` (nuevo) (Base: `androidTest/MiUnRmEstimadoTest.kt`)
  Depende de T13, T14, T9. Es el único test que ejercita la aritmética **sobre una base real**, que es donde la invariante vive: la secuencia completa añadir → retirar → intentar deshacer → reponer → deshacer, comprobando el conteo en cada paso; el añadido entra al final con `3 × 8-12`; un isométrico añadido entra en segundos; **el añadido no hereda las alternativas del puesto 0** (D7, hallazgo 3); un ejercicio con una serie no se retira ni siendo del plan ni siendo añadido; un añadido sin series cierra la sesión como `INCOMPLETE` (CA-43.07); tras el cierre, `plan_assignment` es **byte a byte la misma** y la siguiente sesión de esa rutina reproduce la composición original (CA-43.08); el ejercicio creado desde la sesión sigue en el Diccionario y **no** en el plan.

#### Fase 9 — Documentación (DoD)

- [x] **T31: Modelo de dominio** — `docs/architecture/domain_and_state_model.md`
  Esquema 24; `session_withdrawal` en §2 y en la matriz de relaciones §3; las tres columnas de `session_exercise`; §6.1 con *0 filas, no se siembra*; nota sobre por qué el retiro se persiste y el ejercicio retirado se borra (D2).
- [x] **T32: Contrato de interfaces** — `docs/architecture/interfaces_contract.md`
  `E1-T2` y `E1-T3` con sus precondiciones y sus tres causas de rechazo; nota en `E1-T1` de que **no** queda sustituido; nota en `D5-T1` sobre su invocación desde la sesión y el destino asimétrico; `E4-T1` con la completitud que cuenta los añadidos; `F2-T1` con la marca y el aviso; `J2-T1`/`J3-T1` con el formato 17 (D12, D13).
- [x] **T33: Blueprint** — `docs/architecture/architecture_blueprint.md`
  Esquema 24; ADR-019 ampliado con v24; `SessionAdjustmentRule` entre las notas del motor de reglas; `SessionWithdrawalDao` en `DAT-01`; `SessionWithdrawalEntity` en el inventario; entrada de trazabilidad funcional.
- [x] **T34: Índice de historias y artefactos de la HU** — `docs/domain/stories/story_mapping_index.md`, `HU-43-…/index.md`, `cambios.md`, `dev-record.md`
  `HU-43` a `Lista para Revisión` con sus hermanas y dependencias; cierre de la partición de cinco hijas; fases, métricas de tiempo y registro cronológico.

---

### Riesgos y observaciones

**El riesgo dominante es el hallazgo 3, y es silencioso.** Un añadido con `slot = 0` que herede el `LEFT JOIN` del puesto 0 no lanza ninguna excepción: muestra `4 series` donde el criterio pide `3` y ofrece un botón de intercambio que CA-43.01 prohíbe. Las cuatro guardas de D7 son la mitigación, y T30 las ejercita sobre una base real precisamente porque ninguna regla pura las alcanza — viven en SQL.

**El segundo riesgo es que algo empiece a filtrar por `is_extra`.** CA-43.08 exige lo contrario: las series del añadido cuentan para progresión, tonelaje, KPIs y 1RM *exactamente como cualquier otra*. La columna existe para ordenar, marcar y contar el presupuesto, nunca para excluir. Conviene que la revisión busque explícitamente `is_extra` fuera de las consultas que esta historia toca: si aparece en una de agregación, es un defecto.

**CA-43.07 se cumple hoy, sin código, y podría dejar de cumplirse sin que nadie lo note.** Depende de dos detalles de `closeSession`: que el estado se decida **antes** de `finalizaAllInSession` y que `totalExercises` cuente toda fila con `pending_selection = 0`. Cualquiera de los dos es un refactor plausible y ninguno tiene hoy test. T30 lo ancla.

**El respaldo de ayer no sirve mañana, por quinta vez consecutiva.** `HU-39` invalidó el 12, `HU-40` el 13, `HU-41` el 14, `HU-42` el 15 y esta el 16. Con la partición ya cerrada, este es el momento de decidir si el formato vuelve a ser compatible hacia atrás o si la beta asume que un respaldo solo vale dentro de una misma versión. La historia no lo resuelve; lo deja planteado en el punto natural.

**D11 diverge del preview y conviene que se lea antes de la revisión visual.** El prototipo dibuja un añadido con 0 series en el historial; el código no lo mostrará, porque un ejercicio del plan con 0 series tampoco se muestra hoy y la CA habla de lo *ejecutado*. Si el PO prefiere la lectura del preview, el cambio es un `HAVING` — pero entonces hay que quitarlo también para los del plan, y eso ya no es esta historia.

**El diálogo de cierre no nombra el ejercicio incompleto.** El preview lo dibuja nombrándolo; `CloseSessionDialog` hoy dice cuántos quedan, no cuáles. El comportamiento que CA-43.07 exige —cerrar como `INCOMPLETE`— se cumple igual, así que se deja como está para no reescribir un diálogo que la historia no pide cambiar. Queda dicho por si la revisión lo quiere dentro.

**`session_exercise` gana tres columnas y sigue sin tener un modelo propio de dominio.** El `SessionExerciseDetail` que viaja hacia la UI se arma en el repositorio desde un DTO plano de dieciocho campos, que ahora serán veinte. Es legible todavía, pero es la tercera historia consecutiva que lo engorda. Deuda anotada que esta historia no paga.

**Ningún test de UI instrumentado, igual que en `HU-40`, `HU-41` y `HU-42`.** La hoja de añadir, el menú contextual y la marca ⊕ se cubren por ViewModel; el ensamblado visual queda en validación manual.

---

### Validación manual (no automatizable)

1. **Instalación fresca.** Desinstalar e instalar: debe abrir sin excepción de Room. Sobre una base v23 sin desinstalar debe fallar — es ADR-019, no un defecto.
2. **Sin añadidos no se retira nada.** Empezar una sesión y abrir el `⋮` de cualquier ejercicio: «Retirar de la sesión» aparece **deshabilitado** con la causa *«Añade un ejercicio primero»*. Ningún ejercicio del plan escapa a esa regla, ni el primero ni el último.
3. **Añadir del Diccionario.** `+ Añadir ejercicio` → la lista muestra todo el Diccionario; los que ya están en la sesión se ven **atenuados y con la etiqueta «ya está»**, no ocultos. Elegir uno: entra **al final**, con `3 series · 8-12`, **sin número de puesto** y **sin botón de intercambio**.
4. **El puesto 0 no contamina.** Comprobar expresamente que el añadido no muestra las series del primer ejercicio del plan ni su icono de alternativa, incluso cuando ese primer puesto tiene dos alternativas definidas. Es el defecto que D7 previene.
5. **Modo del ejercicio.** Añadir un isométrico: la prescripción sale en **segundos**. Añadir uno de fallo técnico: **sin límite superior**. Ambos con 3 series.
6. **Añadir más de uno.** Dos añadidos en la misma sesión, ambos al final y en orden de llegada.
7. **El duplicado no pasa.** Intentar añadir un ejercicio que ya está: no es seleccionable. Es la vía que CA-43.01 cierra a favor del selector de equipamiento.
8. **Crear desde la sesión.** `+ Crear ejercicio nuevo` → el **mismo formulario** del Diccionario, con equipamiento múltiple y jerarquía de zonas. Guardar sin equipamiento o sin zona principal debe rechazarse con los mensajes de `HU-39` y `HU-41`. Guardar válido: vuelve a la sesión con el ejercicio **ya añadido**, sin segundo paso.
9. **El destino asimétrico.** Ese ejercicio aparece en el Diccionario como personalizado y **no** en el plan. Retirarlo de la sesión, cerrar la sesión y volver al Diccionario: **sigue ahí**.
10. **Retirar, con el presupuesto.** Con 1 añadido, el `⋮` de un ejercicio del plan con 0 series queda habilitado. Retirarlo: el diálogo dice que el plan no cambia. Tras confirmar, desaparece de la lista y el `⋮` de cualquier otro vuelve a estar deshabilitado con *«Añade otro ejercicio primero»*.
11. **La invariante.** En ese estado —`añadidos 1 · retirados 1`— abrir el `⋮` del añadido: deshabilitado, y el mensaje **nombra** el ejercicio retirado. Añadir ese mismo ejercicio desde el Diccionario: vuelve **al final**, no a su puesto. Ahora sí se puede quitar el añadido original.
12. **Las series bloquean.** Registrar una serie en un ejercicio y abrir su `⋮`: deshabilitado con el número de series. Igual para un añadido con una serie. Comprobarlo en los tres estados de fila —no iniciado, en ejecución, completado—.
13. **Cierre con añadido vacío.** Completar todos los del plan, dejar el añadido en 0 series y cerrar: la sesión queda **INCOMPLETA**. Repetir retirando el añadido antes de cerrar: queda **COMPLETA**.
14. **El historial registra lo ejecutado.** Detalle de esa sesión: los ejercicios con series, el añadido con su marca ⊕ y la línea que nombra el retirado. Tonelaje incluye las series del añadido.
15. **El plan intacto.** Próxima sesión de esa misma rutina: la composición original, con su orden, sus puestos y sus alternativas. El retirado volvió; el añadido no está.
16. **Rotación y microciclos.** Comparar posición y conteo antes y después de una sesión ajustada: idénticos a los de una sesión sin ajustar.
17. **Lo añadido cuenta.** Las series del añadido aparecen en el resumen de la sesión, en la progresión del ejercicio, en el tonelaje por grupo muscular y —si califican— en el 1RM.
18. **Después de cerrar no se ajusta.** En una sesión ya cerrada no hay botón de añadir ni menú de retirar.
19. **Respaldo.** Exportar y comprobar `schemaVersion: 17` y la presencia de `session_withdrawal`. Restaurar: los añadidos conservan su marca y su prescripción, y los retiros su nota. Restaurar un respaldo v16: rechazo con mensaje explícito.
