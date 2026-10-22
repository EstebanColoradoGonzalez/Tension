## Refinamiento Técnico (Developer)
**Autor**: esteban.colorado | **Fecha**: 2026-10-22

---

### Contexto

La historia toca tres frentes que comparten un solo cierre de sesión y una sola subida de esquema:

1. **Progresión por rango 8–12.** Sustituye la Regla de Doble Umbral (`DoubleThresholdRule`, `HU-11`) dentro de `SessionRepositoryImpl.evaluateProgression` y añade la señal de bajar, el aviso en directo y el avance «N de 4».
2. **Umbral base por defecto en 10.** Es una constante (`PlateauThresholdRule.DEFAULT_BASE_THRESHOLD`) y el `defaultValue` de `profile.plateau_base_threshold`. Ajustes ya deriva el desglose de la regla.
3. **Categoría Empuje / Jalón / Pierna** como columna de `exercise` y de `routine`. Filtra la sesión (`HU-43`) y el plan (`HU-26`) y restringe la edición.

**Arquitectura:** MVVM con capa de dominio explícita (ADR-05). UI → ViewModel → Use Case → Repository → DAO/Room. Las reglas son Kotlin puro en `domain/rules` (ADR-06).

**Feature análoga: `HU-43`, ajustar la sesión del día. Los archivos se agregaron el 2026-09-11.** Es la más reciente que toca dominio, datos y UI. `HU-44` y `HU-45` son posteriores, pero solo tocan `assets/tree/tree.js`. De `HU-43` se replica la forma completa:

- una regla pura (`SessionAdjustmentRule`) que devuelve un veredicto tipado y no un booleano;
- un esquema que sube sin migración (23 → 24) con el KDoc de `LAST_MIGRATED_VERSION`;
- un respaldo que sube de formato y rechaza el anterior con un mensaje propio;
- `AddExerciseSheet`, `ActiveSessionViewModel` y `CreateExerciseViewModel` con el argumento `sessionId`;
- tests de regla, de caso de uso y de ViewModel con `mockk`, más un test instrumentado sobre base real.

Candidatas descartadas por más antiguas:

- `HU-41` (`RemoveExerciseEquipmentUseCase`, 2026-09-07). Es el precedente del rechazo que nombra rutinas y se reutiliza su forma `Result.SuggestedByPlan(routineNames)`, pero no es la análoga del conjunto.
- `HU-40` (`ProgressionConsolidationRule`, 2026-09-07).

#### Superficie real del cambio

| Capa | Qué entra |
|---|---|
| `DB-01` | `exercise.movement_category`, `routine.movement_category`, `exercise_progression.top_range_streak`, `top_range_weight_kg` y `last_range_reading`, y `session_exercise_progression.range_reading` y `top_range_streak`. `profile.plateau_base_threshold` pasa a `DEFAULT 10`. Esquema 24 → **25 sin migración** (ADR-019). |
| `DAT-01` | Semillas con categoría (38 ejercicios y 6 rutinas), consultas filtradas por categoría, cierre de sesión con la nueva regla, respaldo 17 → **18**. |
| `DOM-01` | `MovementCategory`, `HypertrophyRangeRule` (nueva, pura), `MovementCategoryRule` (nueva, pura), dos casos de uso de cambio de categoría y cambios de firma en crear ejercicio, crear rutina, asignar y alternativa. **Se retira `DoubleThresholdRule`**. |
| `UI-02` / `UI-01` | Selector de categoría (componente nuevo, compartido), detalle de ejercicio, diálogos de rutina, etiqueta de categoría en el plan y en la sesión, aviso en directo, resumen post-sesión e indicación en la sesión activa. **Ninguna pantalla nueva.** |

#### Hallazgos que deciden el diseño

**1. La señal de subir está muerta en silencio.** `DoubleThresholdRule.meetsDoubleThreshold` exige `setCount >= 4`, y 27 de las 35 asignaciones del plan por defecto tienen 3 series. CA-46.06 pide que el número de series **no intervenga**: la regla nueva no recibe ni conoce las series prescritas.

**2. Desde `HU-40` la unidad de evaluación es el par `(ejercicio, implemento)`.** El conteo hacia la subida tiene que vivir en `exercise_progression`, que ya es por par. La lectura de cada sesión vive en `session_exercise_progression`, que ya es la fila que el resumen pinta por implemento. Así no hace falta ninguna tabla nueva.

**3. La serie de peso corporal no tiene carga externa.** Eso no lo decide la marca del ejercicio sino el implemento (`ExternalLoadRule`, `HU-39`). `Dominadas` no admite el implemento «Peso Corporal»: admite `Barra Fija`, `Máquina` y `Peso Añadido`. `qa.md` habla de «P3 Dominadas con Peso Corporal», y el comportamiento que describe es el de un par **sin carga externa**. Por eso CA-46.15 se resuelve con `ExternalLoadRule.isCaptureEnabled` y no con el nombre del implemento:

- un par sin carga externa nunca recibe «Bajar peso»;
- solo recibe «Añadir peso extra», y únicamente si el ejercicio admite `Peso Añadido`;
- el par `Peso Añadido` sigue la regla completa sobre el lastre.

**4. El registro de serie navega hacia atrás al guardar.** `RegisterSetViewModel` emite `navigateBack` en cuanto persiste. Por eso el aviso «en ese momento» (CA-46.05) se entrega a la sesión activa como resultado de navegación y se muestra en un `Snackbar`, que no bloquea. Además, el formulario de la serie siguiente lo vuelve a mostrar sobre el formulario sin taparlo, como dibuja el preview. Las dos vistas salen de **la misma función de la regla**.

**5. `getAvailableExercisesForVersion` alimenta los dos selectores del plan**, el de asignar y el de alternativa (`PlanVersionDetailViewModel`). Filtrar por categoría en `ExerciseDao.getNotInVersion` cubre ambos de una vez. La guarda de dominio va en los dos casos de uso.

---

### Decisiones técnicas

#### D1 — `HypertrophyRangeRule`: lectura, conteo, señal y aviso en un solo sitio

```kotlin
enum class RangeReading { TOP, WITHIN, BELOW }          // CA-46.01

data class TopRangeStreak(val count: Int, val referenceWeightKg: Double?)

object HypertrophyRangeRule {
    const val RANGE_MIN = 8; const val RANGE_MAX = 12; const val SESSIONS_TO_INCREASE = 4
    fun read(reps: List<Int>): RangeReading                       // mayoría estricta: n*2 > total
    fun referenceWeight(weightsKg: List<Double>): Double          // el mayor, nunca el promedio
    fun nextStreak(previous, reading, referenceWeightKg): TopRangeStreak   // CA-46.17
    fun sessionSignal(reading, streak, hasExternalLoad, admitsAddedWeight): RangeSignal  // CA-46.02/03/04/15
    fun setNotice(reps, weightKg, streak, hasExternalLoad, admitsAddedWeight): SetRangeNotice?  // CA-46.05
}
```

- **La sesión se lee por mayoría estricta.** Con 2 en el tope y 2 por debajo no hay mayoría y la lectura es `WITHIN` (QA-02).
- **El conteo** sube a `n + 1` si la sesión está en el tope con el mismo peso de referencia (tolerancia 0,01 kg). Vuelve a `1` si está en el tope con otro peso, y a `0` en `WITHIN` o `BELOW`.
- **La señal de la sesión** puede ser:
  - `Decrease`, en `BELOW` y solo con carga externa;
  - `Increase`, con un conteo de 4 o más y carga externa;
  - `AddExtraWeight`, con un conteo de 4 o más, sin carga externa y admitiendo `Peso Añadido`;
  - `Progressing(n)`, con un conteo de 1 a 3, salvo un par sin carga externa que no admita `Peso Añadido` (CA-46.15: «ninguna señal de peso»);
  - `InRange`, en el resto de casos.
- **El aviso en directo** puede ser:
  - `LowerNextSet`, con menos de 8 repeticiones y carga externa;
  - `IncreaseDue` o `AddWeightDue`, con 12 o más repeticiones, un conteo de 4 o más y el mismo peso;
  - `TopOfRange(n)`, con 12 o más repeticiones en cualquier otro caso. `n` es el conteo + 1 si el peso coincide, y 1 si no.
  - Un par sin carga externa que no admite `Peso Añadido` no recibe ningún aviso.
- **Isométricos (CA-46.16) y descarga (CA-46.18) quedan fuera por construcción.** No llegan a la regla: quien la invoca los excluye, igual que hoy la descarga se salta en `evaluateProgression`. El conteo queda congelado porque no se escribe.

Se devuelven tipos y no textos: las cadenas las pone la UI (ADR-17). El incremento sigue siendo el de `LoadIncrementResolver`: CA-46.03 y la sección «Fuera de alcance» lo conservan.

#### D2 — Qué carga se prescribe al cerrar

- `Increase` → `peso de referencia + incremento` (CA-46.03).
- **Cualquier otra señal → el peso de referencia.** Ni el de antes ni el promedio.

Con eso `PrefilledLoadRule` deja de proponer una subida pendiente después de un «Bajar peso» o de una sesión dentro del rango. Esa subida ya no está vigente, y el peso nunca se baja salvo por debajo de 8 (RN-5). Si El Ejecutante no sube y vuelve a cerrar en el tope, el conteo pasa a 5, la señal es `Increase` otra vez y la prescripción se mantiene (QA-05).

La carga externa se decide **por par** con `ExternalLoadRule.isCaptureEnabled`, y no con `exercise.is_bodyweight`. Así el par `Peso Añadido` de `Dominadas` recibe por fin una carga prescrita, que es la que pinta «Subir carga → X».

#### D3 — Persistencia del conteo y de la lectura

| Tabla | Columna | Para qué |
|---|---|---|
| `exercise_progression` | `top_range_streak INTEGER NOT NULL DEFAULT 0` | Conteo vigente del par (CA-46.03, CA-46.19). |
| `exercise_progression` | `top_range_weight_kg REAL NULL` | Peso de referencia que sostiene el conteo. «Mismo peso» se compara contra él. |
| `exercise_progression` | `last_range_reading TEXT NULL` | Última lectura del par. La sesión activa la necesita para «▼ bajar peso» (CA-46.02). |
| `session_exercise_progression` | `range_reading TEXT NULL` | Lectura de ese cierre: el resumen es un registro fiel, igual que `prescribed_load_kg`. |
| `session_exercise_progression` | `top_range_streak INTEGER NULL` | Conteo tras ese cierre: el «N de 4» del resumen. |

`NULL` en `range_reading` significa que la regla no aplicó en ese cierre (isométrico o descarga). El resumen cae entonces en la señal de siempre. El respaldo vuelca columnas por cursor, así que las cinco viajan solas (CA-46.19).

#### D4 — `MovementCategory` es un `enum` de dominio y una columna `TEXT NOT NULL` sin default

```kotlin
enum class MovementCategory { PUSH, PULL, LEGS; companion object { fun fromCode(code: String?): MovementCategory? } }
```

- **Sin default**, por el criterio de ADR-019 que siguieron `is_primary` y `suggested_equipment_type_id`: un default permitiría crear un ejercicio o una rutina sin el dato que la historia existe para capturar (CA-46.13).
- **`fromCode` devuelve `null` y no un valor por defecto**, a diferencia de `ProgressionDifficulty`: inventar una categoría para un código corrupto metería un ejercicio en una división que nadie eligió.
- **Las etiquetas viven en `strings.xml`** (Empuje / Jalón / Pierna), no en la base de datos.

#### D5 — `MovementCategoryRule`: compatibilidad de la asignación y conflictos de edición

Es una regla pura con dos funciones:

- `isAssignable(exerciseCategory, routineCategory)`, la igualdad estricta (RN-9, RN-12);
- `conflicts(newCategory, otherSide)`, que devuelve los nombres en conflicto.

La invocan:

- los casos de uso de asignar y de alternativa (CA-46.12);
- añadir a la sesión (CA-46.10);
- los dos casos de uso de cambio de categoría (CA-46.14).

Los conflictos se leen en SQL —qué rutinas tienen asignado el ejercicio, y qué ejercicios tiene asignados la rutina en cualquier versión— y la regla decide sobre esa lectura.

`UpdateExerciseMovementCategoryUseCase` y `UpdateRoutineMovementCategoryUseCase` **devuelven un `Result`** (`Updated` / `ConflictsWithPlan(names)`) y no lanzan. Es la forma de `RemoveExerciseEquipmentUseCase` (`HU-41`): la pantalla nombra la causa.

#### D6 — Filtro por categoría: en la consulta y defendido en el dominio

| Punto | Filtro (lo que se ve) | Guarda (lo que se rechaza) |
|---|---|---|
| Añadir a la sesión (CA-46.10) | `getAddableExercises` filtra por la categoría de la rutina de la sesión | `addExerciseToSession` rechaza otra categoría dentro de la transacción |
| Crear desde la sesión (CA-46.11) | El formulario llega con la categoría fijada y deshabilitada | `CreateAndAddExerciseToSessionUseCase` **toma la categoría de la sesión**, no la del formulario |
| Asignar al plan y alternativa (CA-46.12) | `ExerciseDao.getNotInVersion` filtra por la categoría de la rutina de la versión | `AssignExerciseToVersionUseCase` y `AddAlternativeToSlotUseCase` rechazan otra categoría |

**La categoría del día es la de la rutina de `session.routine_version_id`.** La reasignación temporal (`B1-T3`) ya se agotó al elegir esa versión, así que CA-46.09 se cumple sin código.

#### D7 — Aviso en directo: resultado de navegación y aviso sobre la serie siguiente

1. `SessionRepository.registerSet` pasa a devolver el `SetRangeNotice?` de la serie que acaba de persistir, calculado dentro de la misma transacción con el conteo vigente del par.
2. `RegisterSetUseCase` lo propaga.
3. `RegisterSetViewModel` lo emite con `navigateBack`.
4. `TensionNavHost` lo deja en el `savedStateHandle` de la entrada anterior, que es el patrón estándar de Navigation Compose para devolver un resultado.
5. `ActiveSessionScreen` lo muestra en el `Snackbar` que ya tiene y lo limpia.

`RegisterSetInfo` gana `previousSetNotice`, el aviso de la última serie de este ejercicio en la sesión. La pantalla lo pinta como tarjeta sobre el formulario, como en el preview. El registro no se bloquea ni se modifica en ningún caso.

#### D8 — Indicación junto al peso propuesto en la sesión activa (CA-46.02, CA-46.04)

`SessionExerciseDao.getBySessionIdWithDetails` gana el estado de rango del par que gobierna `prescribedLoadKg`. Se ordena por `prescribed_load_kg DESC` con el mismo criterio de la columna existente (CA-40.04). Llegan `topRangeStreak`, `topRangeWeightKg` y `lastRangeReading`. `ActiveSessionViewModel` llama a `HypertrophyRangeRule.sessionSignal` y lo traduce:

- «▼ bajar peso»;
- «▲ subida» o «▲ añadir peso extra»;
- «· N de 4 en el tope».

En una sesión de descarga no se pinta nada (CA-46.18).

#### D9 — Resumen post-sesión

`ActionSignal` gana estas variantes:

- `DecreaseLoad`;
- `IncreaseLoad(targetKg, streak)`, que suma el conteo a la existente;
- `AddExtraWeight(streak)`;
- `TopOfRange(streak)`;
- `WithinRange`.

`ActionSignalRule.resolve` recibe `rangeReading`, `topRangeStreak`, `hasExternalLoad` y `admitsAddedWeight`. Si hay lectura de rango, **manda la señal de rango**, también en la primera sesión, donde hoy sale `FirstSession`: QA-01 pide «1 de 4» sin sesiones previas. Si no hay lectura, por isométrico o por descarga, se conserva la lógica actual. Con más de un implemento, cada renglón de par pinta su propia señal (CA-40.02).

#### D10 — Esquema 25 sin migración y respaldo 18 que rechaza el 17

`LAST_MIGRATED_VERSION` sigue en 19; el KDoc suma `HU-46` como sexta historia que sube el esquema sin migrar. El formato 17 **no puede** restaurarse: `exercise.movement_category` y `routine.movement_category` son `NOT NULL` sin default y la categoría es una decisión, no un cálculo. CA-46.19 lo declara fuera de alcance. El rechazo nombra la causa con una cadena propia.

#### D11 — Se retira `DoubleThresholdRule`

CA-46.06 dice que **desaparecen** sus dos condiciones. Dejar la regla en el árbol sin invocador sería el mismo tipo de código muerto que retiró `HU-39` en el respaldo. Se borran `DoubleThresholdRule.kt` y `DoubleThresholdRuleTest.kt`, y su papel lo asume `HypertrophyRangeRule` con su propio archivo de tests (estándar §5.5).

`coding-standards.md` la usa como **ejemplo de nomenclatura** (§2.3, §5.2). No se toca: es un documento de estándares, no de inventario, y la convención que ilustra sigue vigente.

---

### Reutilización

| Componente | Decisión | Motivo |
|------------|----------|--------|
| `domain/rules/ExternalLoadRule.kt` | Se reutiliza | T5, T9, T10: decide por par si hay carga externa (CA-46.15) |
| `domain/rules/LoadIncrementResolver.kt` | Se reutiliza | T9: el incremento de la subida no cambia |
| `domain/rules/PrefilledLoadRule.kt` | Se reutiliza sin cambios | T9: la prescripción nueva entra por la misma precedencia |
| `domain/rules/PlateauThresholdRule.kt` | Se reutiliza, cambia la constante | T2: default 10 (CA-46.07) |
| `ui/catalog/components/ProgressionDifficultySelector.kt` | Se replica su forma | T17: el selector de categoría usa el mismo segmentado de 48 dp, pero con un estado «sin selección» que la dificultad no tiene |
| `RemoveExerciseEquipmentUseCase.Result.SuggestedByPlan` | Se replica la forma | T13: `Result` con nombres en lugar de excepción |
| `PlanAssignmentDao.getRoutineNamesSuggestingEquipment` | Se replica la forma | T7: consultas de conflicto que devuelven nombres |
| `session_exercise_progression` / `exercise_progression` | Se reutilizan, ganan columnas | T3, T9: el par ya es la unidad; no hace falta tabla nueva |
| `ActiveSessionScreen` `SnackbarHost` | Se reutiliza | T21: el aviso en directo entra por el mismo canal que los errores |
| `CreateExerciseViewModel.SESSION_ID_ARG` | Se reutiliza | T18: la sesión de origen ya llega; se usa para fijar la categoría |
| `MovementCategory` (nuevo) | Se crea | No hay clasificación de movimiento en el dominio, solo el nombre libre de la rutina |
| `HypertrophyRangeRule` (nueva) | Se crea una vez | Lectura, conteo, señal y aviso: la usan el cierre (T9), el registro de serie (T10), el resumen (T11) y la sesión activa (T20) |
| `MovementCategoryRule` (nueva) | Se crea una vez | La usan asignar y alternativa (T14), añadir a la sesión (T15) y los dos cambios de categoría (T13) |
| `MovementCategorySelector` (componente nuevo) | Se crea una vez | Lo usan crear ejercicio (T18), detalle de ejercicio (T19) y los diálogos de rutina (T22) |
| `MovementCategoryLabel` (`@Composable` y etiqueta) | Se crea una vez | Lo usan la lista de rutinas, el plan, la sesión activa y la hoja de añadir (T20 a T23), para que la traducción enum → texto no diverja |

### Estándares aplicables

| Regla | Sección del estándar | Aplica a |
|-------|----------------------|----------|
| Código e identificadores en inglés; strings de UI en español en `strings.xml`; datos semilla en español | §2.1, §3.6 | Todas las tareas; T6 (semillas) y T24 (cadenas) |
| Regla del motor como `{Nombre}Rule`, función pura en `domain/rules`, un archivo por regla, sin Android | §2.3, §4.5 | T5 (`HypertrophyRangeRule`), T12 (`MovementCategoryRule`) |
| Domain no importa `android.*`, `androidx.*` ni `com.google.*` | §3.2, §4.1 | `MovementCategory`, reglas y casos de uso |
| Casos de uso `{Acción}{Entidad}UseCase` con `operator fun invoke` | §2.3, §2.4 | T13 a T16 |
| ViewModel con `MutableStateFlow` privado → `StateFlow`, eventos con `SharedFlow`, sin `Context` de UI ni `mutableStateOf` | §3.5, §4.2 | T18 a T22 |
| El ViewModel no toca DAOs: todo pasa por casos de uso y repositorios | §4.1 | T22: los diálogos de rutina usan casos de uso |
| Las validaciones de dominio viven en el caso de uso, no en el Composable | §4.1 | T13 a T16 (categoría obligatoria y compatible) |
| `when` con 3 o más ramas; sin `!!` fuera de tests; `sealed interface` para estados finitos | §3.1, §3.3 | `RangeSignal`, `SetRangeNotice`, `Result` |
| Constantes `SCREAMING_SNAKE_CASE` en `object` | §2.5 | `RANGE_MIN`, `RANGE_MAX`, `SESSIONS_TO_INCREASE` |
| Touch target mínimo de 48×48 dp | §6.3 | T17 (selector de categoría) |
| Una señal nunca solo por color: siempre con ícono o símbolo | §6.4 | T11, T20, T21 (▲ ▼ ●) |
| Seed: los Seeders solo mapean; los datos viven en catálogos Kotlin puros verificables en JVM | §4.7 | T6 |
| JUnit 4 en `test/` para reglas y casos de uso; mocks de repositorio; un archivo de tests por regla | §5.1, §5.4, §5.5 | T25 a T29 |
| Nombre de test descriptivo entre backticks | §5.3 | Todos los tests. **Choque con la análoga:** `HU-43` y el resto del repo nombran los tests en español libre, no en `given/when/then`. Se sigue la convención real del repositorio en los archivos existentes y `given/when/then` en los tests nuevos de reglas. |
| Coroutines solo en `viewModelScope`; sin `GlobalScope` | §4.2, §4.3 | T18 a T22 |
| ADR-019: esquema sin migración durante la beta; `LAST_MIGRATED_VERSION` se mueve solo por decisión explícita | Blueprint ADR-019 (citado en §7) | T4 |

---

### Tareas de Implementación

#### Fase 1 — Dominio base y esquema (CA-46.07, CA-46.08, CA-46.09, CA-46.19)

- [x] **T1: `MovementCategory`** — `domain/model/MovementCategory.kt` (nuevo) (Base: `domain/model/ProgressionDifficulty.kt`)
  `PUSH`, `PULL` y `LEGS`, con `fromCode` que devuelve `null` ante un código desconocido (D4). Añadir `movementCategory` a `Exercise`, `Routine` y `AddableExercise`.
- [x] **T2: Umbral base por defecto en 10** — `domain/rules/PlateauThresholdRule.kt`, `data/local/entity/ProfileEntity.kt`
  `DEFAULT_BASE_THRESHOLD = 10` y la tabla del KDoc pasa a 10 / 15 / 20. `defaultValue = "10"`. El rango 3–15 no cambia.
- [x] **T3: Columnas nuevas** — `entity/ExerciseEntity.kt`, `entity/RoutineEntity.kt`, `entity/ExerciseProgressionEntity.kt`, `entity/SessionExerciseProgressionEntity.kt` (Base: `entity/SessionExerciseEntity.kt` de `HU-43`)
  Las dos `movement_category TEXT NOT NULL`, sin default. Las tres de rango en `exercise_progression` y las dos en `session_exercise_progression` (D3). KDoc del porqué de cada una.
- [x] **T4: Esquema 25** — `database/TensionDatabase.kt`, `database/Migrations.kt`, `app/schemas/.../25.json`
  `version = 25` **sin migración**. `LAST_MIGRATED_VERSION` sigue en 19 y su KDoc suma `HU-46`. El JSON lo exporta el build.

#### Fase 2 — Semillas (CA-46.08, CA-46.09)

- [x] **T6: Catálogo y plan con categoría** — `seed/model/SeedExercise.kt`, `seed/model/SeedRoutine.kt`, `seed/ExerciseCatalog.kt`, `seed/DefaultPlan.kt`, `seed/ExerciseSeeder.kt`, `seed/PlanSeeder.kt`
  `movementCategory` es obligatorio en los dos modelos semilla, sin valor por defecto, para que olvidar uno no compile. Los 38 ejercicios se clasifican según la tabla de CA-46.08 (9 / 18 / 11) y las 6 rutinas, Push → Empuje, Pull → Jalón y Lower → Pierna. Los seeders solo mapean.

#### Fase 3 — Reglas (CA-46.01 a CA-46.06, CA-46.10, CA-46.12, CA-46.14 a CA-46.17)

- [x] **T5: `HypertrophyRangeRule`** — `domain/rules/HypertrophyRangeRule.kt` (nuevo) (Base: `rules/SessionAdjustmentRule.kt`)
  `RangeReading`, `TopRangeStreak`, `RangeSignal` y `SetRangeNotice`, más las cinco funciones de D1. KDoc con el porqué de la mayoría estricta, del peso mayor y de la exclusión de isométricos y descarga por quien invoca.
- [x] **T5b: Retirar `DoubleThresholdRule`** — `domain/rules/DoubleThresholdRule.kt`, `test/.../DoubleThresholdRuleTest.kt` (se borran) (D11)
- [x] **T12: `MovementCategoryRule`** — `domain/rules/MovementCategoryRule.kt` (nuevo) (Base: `rules/SessionAdjustmentRule.kt`)
  `isAssignable` y `conflicts` (D5).

#### Fase 4 — Datos (CA-46.01 a CA-46.05, CA-46.10 a CA-46.15, CA-46.17, CA-46.18)

- [x] **T7: Consultas de categoría** — `dao/ExerciseDao.kt`, `dao/RoutineDao.kt`, `dao/PlanAssignmentDao.kt`, `dao/SessionDao.kt`
  `movementCategory` en `ExerciseWithDetails` y en sus tres consultas. `getNotInVersion` filtra por la categoría de la rutina de la versión (D6). Se añaden:
  - `updateMovementCategory` (ejercicio y rutina);
  - `getRoutineNamesAssigningExercise(exerciseId)`, con nombre y categoría;
  - `getExercisesAssignedToRoutine(routineId)`, con nombre y categoría;
  - `getMovementCategoryOfVersion(routineVersionId)`;
  - `getMovementCategoryOfSession(sessionId)`.
- [x] **T8: Repositorios de catálogo, plan y rutina** — `ExerciseRepository(+Impl)`, `PlanRepository(+Impl)`, `RoutineRepository(+Impl)`
  `createExercise` y `createRoutine` reciben la categoría. Se añaden `updateExerciseMovementCategory`, `updateRoutineMovementCategory`, las lecturas de conflicto y `getMovementCategoryOfVersion`. El mapeo a dominio lleva la categoría.
- [x] **T9: Cierre de sesión con la regla nueva** — `data/repository/SessionRepositoryImpl.kt` (`evaluateProgression`), `dao/ExerciseSetDao.kt` si hace falta el nombre del implemento
  Por cada par con series, fuera de descarga y si no es isométrico:
  - lectura, conteo nuevo, señal y prescripción de D2;
  - persistir el conteo, el peso y la lectura en `exercise_progression`;
  - persistir la lectura y el conteo en `session_exercise_progression`.

  Clasificación, meseta y alertas **no cambian**.
- [x] **T10: Aviso en directo** — `SessionRepository.registerSet` y `getRegisterSetInfo`, `domain/model/RegisterSetInfo.kt`
  `registerSet` devuelve el `SetRangeNotice?` de la serie persistida, que es `null` en descarga e isométricos (CA-46.16, CA-46.18). `RegisterSetInfo.previousSetNotice` se calcula desde la última serie del mismo `session_exercise` (D7).
- [x] **T11: Resumen con señal de rango** — `dao/SessionExerciseProgressionDao.kt` (la consulta del resumen lleva `rangeReading`, `topRangeStreak`, `hasExternalLoad` por implemento y `admitsAddedWeight`), `domain/model/ActionSignal.kt`, `domain/rules/ActionSignalRule.kt`, `domain/usecase/session/GetSessionSummaryUseCase.kt` (D9)
- [x] **T11b: Estado de rango en la sesión activa** — `dao/SessionExerciseDao.kt` (`getBySessionIdWithDetails` + DTO), `domain/model/SessionExerciseDetail.kt` (D8)
- [x] **T15b: Sesión acotada a la categoría** — `SessionRepositoryImpl.getAddableExercises` filtra y `addExerciseToSession` rechaza otra categoría. Se añade `getSessionMovementCategory(sessionId)` (D6).

#### Fase 5 — Casos de uso (CA-46.10 a CA-46.14)

- [x] **T13: Cambio de categoría con conflicto** — `domain/usecase/catalog/UpdateExerciseMovementCategoryUseCase.kt` y `domain/usecase/routine/UpdateRoutineMovementCategoryUseCase.kt` (nuevos) (Base: `catalog/RemoveExerciseEquipmentUseCase.kt`)
  Devuelven `Result.Updated` o `Result.ConflictsWithPlan(names)`, y no escriben si hay conflicto.
- [x] **T14: Plan restringido** — `plan/AssignExerciseToVersionUseCase.kt`, `plan/AddAlternativeToSlotUseCase.kt`
  `require(MovementCategoryRule.isAssignable(...))` con el mensaje de dominio.
- [x] **T15: Crear con categoría obligatoria** — `catalog/CreateExerciseUseCase.kt`, `routine/CreateRoutineUseCase.kt`, `catalog/CreateAndAddExerciseToSessionUseCase.kt`, `session/GetSessionMovementCategoryUseCase.kt` (nuevo)
  Los dos primeros reciben `MovementCategory` (no nulo); el ViewModel valida antes. El compuesto **ignora** la categoría del formulario y usa la de la sesión (D6).
- [x] **T16: Registrar serie devuelve el aviso** — `session/RegisterSetUseCase.kt`

#### Fase 6 — Interfaz (CA-46.02 a CA-46.05, CA-46.07 a CA-46.14)

- [x] **T17: Selector y etiqueta de categoría** — `ui/catalog/components/MovementCategorySelector.kt` (nuevo) (Base: `components/ProgressionDifficultySelector.kt`), `ui/components/MovementCategoryLabel.kt` (nuevo)
  Segmentado de 48 dp, sin preselección, con estado deshabilitado y línea de apoyo opcional. La etiqueta traduce el enum.
- [x] **T18: Crear ejercicio** — `CreateExerciseUiState`, `CreateExerciseViewModel`, `CreateExerciseScreen`
  Al guardar sin categoría aparece «Elige la categoría: Empuje, Jalón o Pierna» y no se guarda: el botón **no** se deshabilita por la categoría, para que el mensaje llegue (QA-21). Desde la sesión, la categoría llega fijada con «Categoría del día» (CA-46.11).
- [x] **T19: Detalle de ejercicio** — `ExerciseDetailUiState`, `ExerciseDetailViewModel`, `ExerciseDetailScreen`
  Selector de categoría con persistencia inmediata, igual que la dificultad. Un conflicto abre un diálogo que nombra las rutinas (preview §6).
- [x] **T20: Sesión activa** — `ActiveSessionUiState`, `ActiveSessionViewModel`, `ActiveSessionScreen`, `components/AddExerciseSheet.kt`
  - Etiqueta de la categoría del día en la cabecera.
  - Indicación de rango junto al peso propuesto (D8).
  - La hoja dice «Solo ejercicios de {categoría}», y su estado vacío, «Ningún ejercicio de {categoría} coincide.».
  - `Snackbar` con el aviso devuelto por navegación (D7).
- [x] **T21: Registrar serie y resumen** — `RegisterSetUiState`, `RegisterSetViewModel`, `RegisterSetScreen`, `SessionSummaryScreen`, `navigation/TensionNavHost.kt`
  La tarjeta del aviso de la serie anterior va sobre el formulario. `navigateBack` lleva el aviso y el NavHost lo devuelve a la entrada anterior. El resumen pinta las variantes nuevas con símbolo (▲ ▼ ●) y la segunda línea «Tú eliges cuánto según el implemento» o «N de 4 sesiones en el tope».
- [x] **T22: Rutinas y plan** — `RoutineListUiState`, `RoutineListViewModel`, `RoutineListScreen`, `TrainingPlanUiState`, `TrainingPlanViewModel`, `TrainingPlanScreen`
  Selector obligatorio en crear y editar rutina. El cambio de categoría pasa por `UpdateRoutineMovementCategoryUseCase` y un conflicto se muestra con los nombres de los ejercicios. Cada rutina lleva su etiqueta de categoría (QA-14).
- [x] **T23: Etiqueta en el detalle de versión** — `PlanVersionDetailUiState` y `PlanVersionDetailScreen`
  La cabecera muestra la categoría de la rutina; los selectores ya llegan filtrados por T7.
- [x] **T24: Cadenas** — `res/values/strings.xml`
  - Las tres categorías, el error de obligatoriedad y «Categoría del día».
  - El título y el cuerpo del conflicto del ejercicio y el de la rutina.
  - «Solo ejercicios de %s» y su estado vacío.
  - Los avisos en directo: «Baja el peso en la próxima serie», «Tope del rango — cuenta para la subida (sesión %d de 4)», «Ya toca subir el peso» y «Ya toca añadir peso extra».
  - Las señales del resumen y las indicaciones de la sesión activa.
  - El rechazo del respaldo 17.

#### Fase 7 — Respaldo (CA-46.19)

- [x] **T24b: Formato 18** — `data/repository/BackupRepositoryImpl.kt`
  `SCHEMA_VERSION = 18`. KDoc con el motivo del rechazo del 17. El mensaje del formato anterior pasa a la cadena nueva de categorías. Las columnas viajan por cursor sin tocar `TABLE_ORDER_INSERT`.

#### Fase 8 — Tests (QA-01 a QA-36)

- [x] **T25: `HypertrophyRangeRuleTest`** — `test/.../domain/rules/HypertrophyRangeRuleTest.kt` (nuevo) (Base: `rules/SessionAdjustmentRuleTest.kt`)
  - Lectura con las seis filas de QA-01 y el empate de QA-02.
  - Peso de referencia como el mayor (QA-34, QA-35).
  - Conteo en las secuencias de QA-04, QA-29, QA-30, QA-31 y QA-34.
  - Señal: la de bajar (QA-03), la de subir en la cuarta y que se mantiene en la quinta (QA-04, QA-05), con 3 series (QA-06/QA-10: la regla no recibe series ni RIR).
  - Peso corporal: con y sin `Peso Añadido`, y el par `Peso Añadido` (QA-25, QA-26, QA-27).
  - Avisos: 7 y 8 (QA-07), 11, 12 y 15 con «sesión 2 de 4» (QA-08), 8 a 11 (QA-09), «Ya toca subir» (QA-36).
- [x] **T26: `MovementCategoryRuleTest`** — `test/.../domain/rules/MovementCategoryRuleTest.kt` (nuevo)
  Igualdad estricta y conflictos con nombres (QA-22, QA-23, QA-24).
- [x] **T27: Tests de casos de uso** — `UpdateExerciseMovementCategoryUseCaseTest`, `UpdateRoutineMovementCategoryUseCaseTest` (nuevos). Se actualizan `CreateExerciseUseCaseTest`, `CreateAndAddExerciseToSessionUseCaseTest` (la categoría es la de la sesión aunque el formulario mande otra), `AssignExerciseToVersionUseCaseTest` (nuevo si no existe), `AddAlternativeToSlotUseCaseTest`, `CreateRoutineUseCaseTest` (nuevo si no existe), `RegisterSetUseCaseTest` y `GetSessionSummaryUseCaseTest` (la señal de rango manda sobre `FirstSession`; las variantes nuevas). Cubren QA-16, QA-18, QA-19, QA-20 y QA-21.
- [x] **T28: Semillas** — `test/.../data/local/ExerciseCatalogTest.kt`, `DefaultPlanTest.kt`
  Los 38 ejercicios contra la tabla de CA-46.08, **transcrita de la historia y no del código** (QA-13). Las 6 rutinas contra CA-46.09 (QA-14). Ninguna asignación del plan por defecto mezcla categorías.
- [x] **T29: Umbral y ViewModels** — `PlateauThresholdRuleTest` (default 10, desglose 10 / 15 / 20, 11 → 11 / 17 / 22: QA-11, QA-12), `SettingsViewModelTest`, `ActiveSessionViewModelTest` y `RegisterSetViewModelTest` (aviso propagado), `BackupRepositoryImplTest` (formato 18 y rechazo del 17: QA-33).
- [x] **T30: Test instrumentado** — `androidTest/.../ProgresionPorRangoYCategoriaTest.kt` (nuevo) (Base: `androidTest/AjustarSesionDelDiaTest.kt`)
  Sobre una base real recorre la secuencia de 4 sesiones de 3 series hasta la prescripción de subida y la mantiene en la quinta. Además comprueba que:
  - la descarga no suma ni reinicia (QA-32);
  - los añadibles de un día de Empuje no traen Jalón ni Pierna (QA-16);
  - `addExerciseToSession` rechaza otra categoría;
  - el perfil nuevo nace con umbral 10.

  **Compila en `assembleAndroidTest`**; su ejecución necesita emulador y queda para la validación manual.

#### Fase 9 — Documentación (CA-46.19, DoD)

- [x] **T31: Modelo de dominio** — `docs/architecture/domain_and_state_model.md`
  Esquema 25. Las columnas nuevas en §2. `MovementCategory` en §4. La tabla de categorías en §6.1. Umbral 10 en `profile` y en `exercise_progression`.
- [x] **T32: Contrato de interfaces** — `docs/architecture/interfaces_contract.md`
  `E1-T2`, `E2-T1`, `E4-T1`, `E5-T1`, `D2-T1`, `D5-T1`, `D6-T1`, `D4-T1`, `J1-T2`, `J2-T1` y `J3-T1`.
- [x] **T33: Blueprint** — `docs/architecture/architecture_blueprint.md`
  - Esquema 25 y ADR-019 con v25.
  - `HypertrophyRangeRule` sustituye a `DoubleThresholdRule` en el motor.
  - `MovementCategoryRule` entra en las notas.
  - D-07 actualizada y trazabilidad de `HU-46`.
- [x] **T34: Índice de historias y artefactos de la HU** — `docs/domain/stories/story_mapping_index.md`, `HU-46-…/index.md`, `cambios.md`, `dev-record.md`

---

### Riesgos y observaciones

- **El riesgo dominante es la prescripción (D2).** Si al cerrar se conserva una subida no alcanzada, `PrefilledLoadRule` seguirá precargando el peso mayor después de un «Bajar peso». Ninguna excepción lo delata. Lo cubren T25 (regla) y T30 (base real).
- **`qa.md` usa «Dominadas con Peso Corporal»**, y `Dominadas` no admite ese implemento (hallazgo 3). El comportamiento se implementa por carga externa, que cubre `Barra Fija` y `Máquina` asistida. Queda anotado para que la validación manual use `Barra Fija`.
- **Un ejercicio con varios implementos muestra en la sesión activa el estado del par que gobierna la carga** (D8), igual que hoy `prescribedLoadKg`. Es informativo: el aviso en directo y el resumen sí son por par.
- **Respaldo invalidado por sexta vez consecutiva.** Es el mismo planteamiento abierto de ADR-019; CA-46.19 lo acepta explícitamente.
- **Sin tests de UI instrumentados**, igual que `HU-40` a `HU-43`. El selector, los diálogos y los avisos se cubren por ViewModel; el ensamblado visual queda en validación manual.
- **La importación de respaldos anteriores queda fuera de alcance** (CA-46.19).

---

### Validación manual (no automatizable)

1. **Instalación fresca.** Desinstalar e instalar; debe abrir sin excepción de Room. En Ajustes, el umbral muestra 10 y el desglose Baja → 10 · Media → 15 · Alta → 20 (QA-11).
2. **Diccionario.** Cada ejercicio muestra su categoría: Elevación Lateral es Empuje; Remo al Mentón, Crunch Abdominal y Press Pallof son Jalón; Peso Muerto Rumano es Pierna (QA-13).
3. **Plan.** Cada rutina muestra su categoría (QA-14). El selector de asignar y el de alternativa solo listan ejercicios de esa categoría (QA-19, QA-20).
4. **Crear sin categoría.** Tanto el ejercicio como la rutina se rechazan con «Elige la categoría: Empuje, Jalón o Pierna» (QA-21).
5. **Conflictos.** Cambiar Aperturas a Jalón queda impedido y se nombran las dos rutinas Push. Cruce de Polea Alta, que está fuera del plan, cambia libremente (QA-22, QA-23). La rutina Lower no puede pasar a Empuje (QA-24).
6. **Sesión.** La cabecera muestra la categoría del día. «Añadir ejercicio» solo trae esa categoría y la búsqueda vacía lo dice (QA-16, QA-17). Con la rutina reasignada, la categoría la sigue (QA-15).
7. **Crear desde la sesión.** La categoría llega fijada con «Categoría del día» (QA-18).
8. **Aviso en directo.** Una serie de 7 muestra «Baja el peso en la próxima serie»; una de 12, el aviso de tope con N de 4; una de 10, nada (QA-07 a QA-09).
9. **Resumen.** Con 3 series: «Mantener — N de 4» en las sesiones 1 a 3 y «Subir carga → X» en la cuarta (QA-04). Una sesión por debajo muestra «Bajar peso» sin cantidad (QA-03).
10. **Dominadas con `Barra Fija`.** Nunca recibe «Bajar peso» y tras 4 sesiones en el tope recibe «Añadir peso extra» (QA-25, QA-26).
11. **Descarga.** Sin avisos ni señales; el conteo no se mueve (QA-32).
12. **Respaldo.** Exportar muestra `schemaVersion: 18`. Restaurado, conserva las categorías y el conteo (QA-33). Un respaldo 17 se rechaza con un mensaje explícito.
