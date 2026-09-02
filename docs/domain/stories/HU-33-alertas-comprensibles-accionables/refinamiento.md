## Refinamiento Técnico (Developer)
**Autor**: Esteban Colorado González | **Fecha**: 2026-08-31

---

### Contexto

El sistema de alertas de HU-18 está repartido en tres puntos y la historia toca los tres:

1. **Emisión** — `SessionRepositoryImpl.evaluatePostSession` invoca cinco evaluadores privados (`evaluateLowProgressionRate`, `evaluateRirOutOfRange`, `evaluateLowAdherence`, `evaluateTonnageDrop`, `evaluateRoutineInactivity`) más la detección de meseta embebida en `evaluateProgression`. Cada uno decide el nivel consultando `AlertThresholdRule` y persiste un `AlertEntity` con un `message` congelado en el momento de la inserción.
2. **Umbrales** — `AlertThresholdRule` es un `object` de once constantes y diez predicados. Es el punto único donde viven los números de las cinco familias.
3. **Lectura** — `AlertRepositoryImpl` recompone en cada apertura del detalle los datos que dispararon la alerta (`buildTriggerData`), su explicación (`buildCausalAnalysis`) y una lista de recomendaciones (`buildRecommendations`), que la UI pinta como viñetas sueltas más dos `TextButton` de navegación condicionados por dos booleanos (`showExerciseHistoryLink`, `showDeloadLink`).

Lo que la historia cambia no es la arquitectura de emisión ni el ciclo de vida de la alerta — que se conservan intactos, CA-33.05 y CA-33.07 — sino **tres capas de contenido superpuestas al mismo mecanismo**: los números contra los que se compara, el idioma en que se redacta el hallazgo y la existencia obligatoria de una acción con su acceso directo.

#### Estado actual relevante

| Hecho verificado en código | Consecuencia para esta HU |
|---|---|
| `AlertThresholdRule` concentra los once umbrales de las cinco familias | Punto único de cambio para CA-33.03. Ninguna familia lee números fuera de este objeto |
| `AlertThresholdRule.progressionLevel(rate)` no recibe el ejercicio | CA-33.04 obliga a añadir la dificultad como parámetro. La firma cambia y con ella su único llamador |
| `ClassificationCount` (`domain/model/`) ya trae `exerciseName`, no `progressionDifficulty` | La ponderación necesita ampliar la proyección `getClassificationCountsByPeriod`, patrón idéntico a T8 de HU-32 |
| `evaluateLowProgressionRate` usa `LocalDate.now().minusWeeks(4)` y emite con **una sola** clasificación en la ventana | CA-33.03 pide 6 semanas; CA-33.09 obliga además a una guarda de observaciones mínimas que hoy no existe |
| `evaluateLowAdherence` emite con **una** semana baja y escala a crisis con dos | CA-33.03 desplaza ambos: alerta con 2, crisis con 3+. Requiere mirar tres semanas hacia atrás, no dos |
| `evaluateRirOutOfRange` pide `getSessionIdsByRoutineInRange(routineId, 2)` y exige `bothLow`/`bothHigh` | La ventana pasa a 3: la consulta y la condición de sostenimiento cambian de aridad |
| `evaluateTonnageDrop` compara el último microciclo completo contra el anterior sobre `closedSessions.filter { deloadId == null }` | La ventana de 2 microciclos consecutivos **ya es la vigente**: CA-33.03 solo mueve los porcentajes (10/20 → 15/25) |
| `AlertThresholdRule.tonnageLevel` tiene una rama `isDeloadSession && isTonnageAlert -> MEDIUM_ALERT` | Contradice CA-33.08. Hoy es inocua porque los dos llamadores pasan `false` y las sesiones de descarga ya están filtradas, pero la regla miente sobre su propio contrato |
| `evaluateRoutineInactivity` compara contra `INACTIVITY_ALERT_DAYS`/`CRISIS_DAYS` sobre días naturales | CA-33.03 solo mueve los números (10/14 → 14/21). Mecanismo intacto |
| `AlertEntity.message` se persiste en la inserción; `AlertItem.message` lo devuelve sin transformar | La frase del Centro de Alertas se compone en emisión. Los textos actuales (`"Tasa: 18%"`, `"RIR <0.5 sostenido"`, `"Caída de tonelaje −18%"`) son el defecto que CA-33.01 corrige |
| `AlertDetail` expone `recommendations: List<String>` + `showExerciseHistoryLink` + `showDeloadLink` | CA-33.02 pide **una** acción con **un** acceso directo opcional. Los tres campos se colapsan en uno |
| `alertTypeDisplayName` está duplicado literal en `AlertCard.kt:82` y `AlertDetailScreen.kt:351`, pese a existir ya `alert_type_*` en `strings.xml:319-325` | La reescritura de títulos se hace una sola vez si antes se elimina la duplicación |
| `PlanAssignmentDao.getAlternativesForSlot` y `getSlotByExercise` existen (HU-26) | CA-33.10 es verificable en consulta: se puede saber si un ejercicio tiene alternativa antes de proponerla |
| `CorrectiveActionRule` escalona en `MICRO_INCREMENT_THRESHOLD = 4` y `ROTATE_VERSION_THRESHOLD = 6`, absolutos | Descalibrado frente a los umbrales efectivos 5/8/10 de HU-32, que lo dejó levantado para esta historia |
| `RegisterSetUseCase:22` → `require(rir in 0..2)`; `RegisterSetScreen:425` → `for (rir in 0..2)` | **El RIR de este sistema vive en escala 0–2.** Los umbrales `< 1.5` y `> 3.5` de CA-33.03 pertenecen a una escala 0–5. Ver D3 |
| No existe `AlertRepositoryImplTest`; los tests de alerta cubren reglas, casos de uso y ViewModels | Todo texto y toda decisión de acción que deba probarse tiene que vivir en `domain/rules/`, no en el repositorio |

---

### Decisiones técnicas

#### D1 — Los umbrales siguen siendo un solo objeto; la ponderación entra como parámetro, no como estado

`AlertThresholdRule` conserva su forma de `object` de constantes y predicados puros. CA-33.04 no introduce estado: la dificultad ya está persistida en `exercise.progression_difficulty` (HU-32) y viaja hasta la regla como parámetro. `progressionLevel(rate)` pasa a `progressionLevel(rate, difficulty)` y los dos predicados de progresión reciben la dificultad. La tabla de CA-33.04 se resuelve con un `when (difficulty)` dentro de la regla, no con un campo nuevo en `ProgressionDifficulty`: el multiplicador que ese enum ya expone pertenece al umbral de meseta (HU-32) y mezclar ambas escalas en el mismo enum acoplaría dos calibraciones independientes.

#### D2 — Cada familia declara su ventana como constante nombrada

Hoy las ventanas son literales dispersos: `minusWeeks(4)` en el repositorio, el `2` de `getSessionIdsByRoutineInRange`, el `chunked(cycleSize)` implícito. CA-33.03 exige que cada familia declare su ventana explícitamente. Todas pasan a constantes de `AlertThresholdRule` (`PROGRESSION_WINDOW_WEEKS`, `RIR_SUSTAINED_SESSIONS`, `ADHERENCE_ALERT_WEEKS`, `ADHERENCE_CRISIS_WEEKS`, `TONNAGE_MICROCYCLES`, `INACTIVITY_ALERT_DAYS`, `INACTIVITY_CRISIS_DAYS`) y el repositorio las consume. El objeto queda como la declaración única y auditable que CA-33.03 pide, y la documentación se limita a transcribirlo.

#### D3 — La familia RIR conserva 0.5 / 1.8 y solo amplía su ventana — desviación declarada de CA-33.03

CA-33.03 fija la condición de alerta en `RIR promedio < 1.5 o > 3.5`. Esos números presuponen una escala de RIR 0–5. **En este sistema el RIR se captura en 0–2**: `RegisterSetUseCase` lo valida con `require(rir in 0..2)` y `RegisterSetScreen` solo ofrece tres botones. Aplicar el criterio literalmente produce dos defectos, ambos contrarios al propósito de la historia:

- `> 3.5` es inalcanzable por construcción — la mitad de "estímulo insuficiente" de la familia dejaría de emitir para siempre, en silencio.
- `< 1.5` cubre el rango de trabajo normal en una escala 0–2 — dispararía en casi toda rutina, que es exactamente el falso positivo crónico que la historia existe para eliminar.

Los valores vigentes `RIR_LOW_THRESHOLD = 0.5` y `RIR_HIGH_THRESHOLD = 1.8` sí están calibrados contra la escala real y no hay evidencia de que produzcan ruido. **Decisión, confirmada con el PO: se conservan.** De CA-33.03 se aplica el único cambio coherente con el dominio, que además es el que el criterio persigue: la ventana pasa de 2 a 3 sesiones consecutivas. La desviación y su motivo quedan escritos en `interfaces_contract.md` junto a la tabla de umbrales, para que el criterio no se relea como incumplido.

#### D4 — Los datos insuficientes se cortan en el evaluador, no en la regla

CA-33.09 exige que una ventana incompleta no emita nada, ni siquiera un aviso de datos insuficientes. Tres familias ya lo cumplen por construcción (`evaluateRirOutOfRange` con su `continue`, `evaluateTonnageDrop` con `completeMicrocycles.size < 2`, `evaluateRoutineInactivity` porque sin sesión previa resuelve en lugar de emitir). Las dos que no:

- **Progresión**: hoy emite con una sola clasificación en las 6 semanas. Se añade `PROGRESSION_MIN_OBSERVATIONS = 3`. Justificación: una tasa calculada sobre una o dos sesiones no es una tasa, es el resultado de un lanzamiento de moneda; tres es el mínimo con el que el cociente distingue tendencia de fluctuación.
- **Adherencia**: la guarda actual (`< 7` días desde la primera sesión) corresponde a una ventana de una semana. Con alerta a 2 semanas consecutivas, pasa a `< 14`.

La guarda vive en el evaluador y no en `AlertThresholdRule` porque es una condición sobre la disponibilidad del dato, no sobre su valor: la regla sigue siendo una función total de números a nivel.

#### D5 — La narración es una regla pura, compartida por emisión y lectura

La frase que ve El Ejecutante se necesita en dos momentos distintos: al insertar la alerta (`AlertEntity.message`, que es lo que pinta el Centro de Alertas) y al abrir su detalle (la explicación ampliada). Duplicarla garantiza que ambas divergan.

Se crea `domain/rules/AlertNarrativeRule.kt`, objeto puro que recibe primitivos (nombre del ejercicio, porcentaje, días, semanas, dificultad) y devuelve la frase. Lo consumen `SessionRepositoryImpl` en emisión y `AlertRepositoryImpl` en lectura.

Vive en `domain/rules/` y no en `strings.xml` por dos razones concretas: las capas Domain y Data no pueden acceder a recursos Android (§4.1 de los estándares) y el `message` se **persiste**, de modo que tiene que existir antes de que haya ninguna UI que lo formatee. Es además la única ubicación que hace verificable CA-33.01 con tests JVM (RNF29, RNF30): se puede afirmar sobre el texto producido que no contiene identificadores ni nombres de reglas. La excepción a §3.6 de los estándares queda acotada a los cuerpos de alerta, que ya la ejercían de facto en `buildCausalAnalysis`; los títulos de tipo sí bajan a `strings.xml`, donde ya estaban declarados sin usarse.

#### D6 — La acción sugerida se parte en decisión pura y redacción

`SuggestedActionRule` (`domain/rules/`) decide **qué** acción corresponde y si es navegable, a partir de primitivos: tipo de alerta, nivel, si el RIR es bajo o alto, si el ejercicio tiene alternativa en su puesto, sesiones sin progresión y umbral efectivo. `AlertNarrativeRule` redacta el texto de esa decisión. La partición mantiene la regla testeable sin mocks y concentra CA-33.10 —la comprobación de ejecutabilidad— en un único punto con entrada explícita.

`SuggestedActionKind` es un enum en `domain/model/`. La navegabilidad no es un booleano suelto sino un `SuggestedActionTarget?` nulo: cuando la acción no es navegable (revisar técnica, dejar repeticiones en reserva) el objetivo simplemente no existe, y la UI no tiene que consultar dos campos para decidir si pinta el botón.

#### D7 — `AlertDetail` colapsa tres campos en uno

`recommendations: List<String>`, `showExerciseHistoryLink: Boolean` y `showDeloadLink: Boolean` desaparecen y se sustituyen por `suggestedAction: SuggestedAction`. CA-33.02 pide una acción concreta con un acceso directo, no una lista de sugerencias con dos botones sueltos al pie: mantener los tres campos permitiría representar estados que el criterio prohíbe (una alerta sin ninguna acción, o una acción sin destino pero con botón). El modelo nuevo hace inexpresable la alerta sin acción.

#### D8 — La exclusión de descarga se cierra en la regla, no solo en el llamador

`tonnageLevel(dropPercentage, isDeloadSession = true)` devuelve hoy `MEDIUM_ALERT` cuando la caída supera el umbral de alerta. Que no se manifieste es un accidente de los llamadores, que pasan `false`. CA-33.08 dice que en descarga planificada no se emite: la rama pasa a devolver `null`. Es la corrección de un contrato que miente, no un cambio de comportamiento observable, y queda cubierta con test.

#### D9 — `CorrectiveActionRule` se recalibra a desplazamientos relativos

Sus umbrales absolutos (4 y 6) fueron calibrados contra un umbral de meseta de 3, y HU-32 los dejó explícitamente levantados para esta historia. Con umbrales efectivos de 5, 8 o 10, todo ejercicio en meseta supera ambos y las dos acciones aparecen juntas en la primera alerta — una alerta que propone dos cosas a la vez no propone ninguna, que es el defecto que CA-33.02 ataca. `recommend` pasa a recibir el umbral efectivo y a escalonar de forma relativa: la primera acción desde el propio umbral, la segunda a partir de `umbral + ceil(umbral / 2)`. Con umbral 3 reproduce 3 y 5, próximo a la gradación original; con umbral 10 escalona en 10 y 15.

#### D10 — Sin cambio de esquema

Ninguna decisión introduce columnas. La ponderación consume `exercise.progression_difficulty` (v15, HU-32); la acción sugerida se deriva en lectura; la narración se compone en emisión sobre el `message` que ya existe. La versión de la base de datos no se toca y no hay migración. Las alertas ya persistidas conservan su texto antiguo hasta resolverse; en beta la base se reinicia, de modo que el punto es teórico.

#### D11 — Ubicación de las piezas nuevas

| Pieza | Ruta | Motivo |
|---|---|---|
| `SuggestedAction`, `SuggestedActionTarget`, `SuggestedActionKind` | `domain/model/` | Modelos de dominio, junto a `AlertDetail` y `ActionSignal` |
| `AlertNarrativeRule` | `domain/rules/` | Función pura de primitivos a texto (D5) |
| `SuggestedActionRule` | `domain/rules/` | Función pura de contexto a decisión (D6) |
| `SuggestedActionCard` | `ui/alerts/` | Componente propio de la pantalla de detalle, como `AlertCard` |

---

### Tareas de Implementación

#### Fase 1 — Umbrales y ventanas (dominio puro)

- [x] **T1: Reescribir `AlertThresholdRule`** — `domain/rules/AlertThresholdRule.kt` (Base: `domain/rules/PlateauThresholdRule.kt`)

  Constantes por familia, con la ventana declarada junto al umbral (D2):

  ```kotlin
  const val PROGRESSION_WINDOW_WEEKS = 6L
  const val PROGRESSION_MIN_OBSERVATIONS = 3
  const val RIR_LOW_THRESHOLD = 0.5
  const val RIR_HIGH_THRESHOLD = 1.8
  const val RIR_SUSTAINED_SESSIONS = 3
  const val ADHERENCE_THRESHOLD = 60.0
  const val ADHERENCE_ALERT_WEEKS = 2
  const val ADHERENCE_CRISIS_WEEKS = 3
  const val ADHERENCE_LOOKBACK_WEEKS = 4
  const val TONNAGE_ALERT_THRESHOLD = 15.0
  const val TONNAGE_CRISIS_THRESHOLD = 25.0
  const val TONNAGE_MICROCYCLES = 2
  const val INACTIVITY_ALERT_DAYS = 14L
  const val INACTIVITY_CRISIS_DAYS = 21L
  ```

  Progresión ponderada (CA-33.04, D1):

  ```kotlin
  fun progressionAlertThreshold(difficulty: ProgressionDifficulty): Double
  fun progressionCrisisThreshold(difficulty: ProgressionDifficulty): Double
  fun isProgressionAlert(rate: Double, difficulty: ProgressionDifficulty): Boolean
  fun isProgressionCrisis(rate: Double, difficulty: ProgressionDifficulty): Boolean
  fun progressionLevel(rate: Double, difficulty: ProgressionDifficulty): String?
  ```

  con `LOW → 40/20`, `MEDIUM → 35/15`, `HIGH → 25/10`. `PROGRESSION_ALERT_THRESHOLD` y `PROGRESSION_CRISIS_THRESHOLD` desaparecen como constantes sueltas.

  Adherencia por semanas consecutivas (CA-33.03): `fun adherenceLevel(consecutiveLowWeeks: Int): String?` → `null` bajo 2, `MEDIUM_ALERT` en 2, `CRISIS` desde 3. `ADHERENCE_CRISIS_WEEKS` cambia de significado (era 2, ahora 3) y se le suma `ADHERENCE_ALERT_WEEKS`.

  Tonelaje: `tonnageLevel(dropPercentage, isDeloadSession)` devuelve `null` cuando `isDeloadSession` (D8, CA-33.08).

  `isRirLow`, `isRirHigh`, `isRirOutOfRange`, `isAdherenceLow`, `isTonnageAlert`, `isTonnageCrisis`, `isInactivityAlert`, `isInactivityCrisis` e `inactivityLevel` conservan firma. KDoc en inglés por familia con umbral, ventana y justificación — es la fuente de la que se transcribe la documentación de CA-33.03.

- [x] **T2: Recalibrar `CorrectiveActionRule`** — `domain/rules/CorrectiveActionRule.kt` (D9)

  `recommend(sessionsWithoutProgression: Int, plateauThreshold: Int): List<CorrectiveAction>`. Primera acción desde `plateauThreshold`; segunda desde `plateauThreshold + ceil(plateauThreshold / 2.0)`. `MICRO_INCREMENT_THRESHOLD` y `ROTATE_VERSION_THRESHOLD` se eliminan. KDoc con la tabla de equivalencia 3 → 3/5, 5 → 5/8, 10 → 10/15.

- [x] **T3: Ajustar el consumo de umbrales en métricas** — `domain/usecase/metrics/GetAvgRirByRoutineUseCase.kt`

  Lee `RIR_LOW_THRESHOLD` y `RIR_HIGH_THRESHOLD`, que no cambian de valor ni de nombre (D3): **verificar que compila sin tocarlo**. Se declara como tarea para dejar constancia de que la interpretación de RIR en Analítica (HU-15) no sufre desplazamiento.

#### Fase 2 — Narración y acción sugerida (dominio puro)

- [x] **T4: Crear `SuggestedActionKind`** — `domain/model/SuggestedActionKind.kt` (Base: `domain/model/CorrectiveAction.kt`)

  ```kotlin
  enum class SuggestedActionKind {
      INCREASE_LOAD_SLIGHTLY,
      EXTEND_REPS_BEFORE_LOAD,
      SWITCH_TO_SLOT_ALTERNATIVE,
      ROTATE_ROUTINE_VERSION,
      START_DELOAD,
      REDUCE_VOLUME,
      LEAVE_REPS_IN_RESERVE,
      INCREASE_LOAD_FOR_STIMULUS,
      RESUME_MODULE,
      INCREASE_WEEKLY_FREQUENCY,
      REVIEW_TECHNIQUE,
  }
  ```

- [x] **T5: Crear `SuggestedAction` y `SuggestedActionTarget`** — `domain/model/SuggestedAction.kt` (Base: `domain/model/ActionSignal.kt`)

  ```kotlin
  data class SuggestedAction(
      val kind: SuggestedActionKind,
      val text: String,
      val target: SuggestedActionTarget?,
  )

  sealed interface SuggestedActionTarget {
      data class ExerciseHistory(val exerciseId: Long) : SuggestedActionTarget
      data object DeloadManagement : SuggestedActionTarget
      data object TrainingPlan : SuggestedActionTarget
  }
  ```

  Los tres destinos son rutas ya existentes (`EXERCISE_HISTORY`, `DELOAD_MANAGEMENT`, `TRAINING_PLAN`): CA-33.02 exige que el acceso directo lleve a ejecutar la acción, no que se creen rutas nuevas. `target = null` es el caso no navegable.

- [x] **T6: Crear `SuggestedActionRule`** — `domain/rules/SuggestedActionRule.kt` (Base: `domain/rules/CorrectiveActionRule.kt`) (D6)

  ```kotlin
  data class SuggestedActionContext(
      val alertType: String,
      val level: String,
      val exerciseId: Long?,
      val hasSlotAlternative: Boolean,
      val sessionsWithoutProgression: Int,
      val plateauThreshold: Int,
      val isRirLow: Boolean,
  )

  fun resolve(context: SuggestedActionContext): Pair<SuggestedActionKind, SuggestedActionTarget?>
  ```

  Mapa por familia:

  | Tipo | Condición | Acción | Destino |
  |---|---|---|---|
  | `PLATEAU` | `CorrectiveActionRule` no propone rotación | `EXTEND_REPS_BEFORE_LOAD` | historial del ejercicio |
  | `PLATEAU` | propone rotación y hay alternativa en el puesto | `SWITCH_TO_SLOT_ALTERNATIVE` | plan de entrenamiento |
  | `PLATEAU` | propone rotación y **no** hay alternativa | `ROTATE_ROUTINE_VERSION` | plan de entrenamiento |
  | `LOW_PROGRESSION_RATE` | nivel `CRISIS` y hay alternativa | `SWITCH_TO_SLOT_ALTERNATIVE` | plan de entrenamiento |
  | `LOW_PROGRESSION_RATE` | resto | `INCREASE_LOAD_SLIGHTLY` | historial del ejercicio |
  | `RIR_OUT_OF_RANGE` | RIR bajo | `LEAVE_REPS_IN_RESERVE` | — (no navegable) |
  | `RIR_OUT_OF_RANGE` | RIR alto | `INCREASE_LOAD_FOR_STIMULUS` | plan de entrenamiento |
  | `LOW_ADHERENCE` | — | `INCREASE_WEEKLY_FREQUENCY` | — (no navegable) |
  | `TONNAGE_DROP` | — | `REDUCE_VOLUME` | plan de entrenamiento |
  | `ROUTINE_INACTIVITY` | — | `RESUME_MODULE` | plan de entrenamiento |
  | `ROUTINE_REQUIRES_DELOAD` | — | `START_DELOAD` | gestión de descarga |
  | desconocido | — | `REVIEW_TECHNIQUE` | — (no navegable) |

  **CA-33.10 vive en la tercera y cuarta fila**: `SWITCH_TO_SLOT_ALTERNATIVE` solo se propone con `hasSlotAlternative = true`; sin alternativa cae a una acción aplicable. La rama por defecto garantiza que ninguna alerta quede sin acción (CA-33.02).

- [x] **T7: Crear `AlertNarrativeRule`** — `domain/rules/AlertNarrativeRule.kt` (Base: `domain/rules/PlateauCausalAnalysisRule.kt`) (D5)

  Tres grupos de funciones puras, todas devolviendo español natural sin identificadores, sin nombres de tipo de alerta y sin nombres de regla (CA-33.01):

  **Titulares** (se persisten en `AlertEntity.message`, se leen en el Centro de Alertas):

  ```kotlin
  fun plateauHeadline(exerciseName: String, sessions: Int): String
  fun progressionRateHeadline(exerciseName: String, rate: Int, weeks: Long): String
  fun rirHeadline(routineName: String, avgRir: Double, isLow: Boolean, sessions: Int): String
  fun adherenceHeadline(percentage: Int, consecutiveWeeks: Int): String
  fun tonnageHeadline(muscleGroup: String, dropPercentage: Int, microcycles: Int): String
  fun inactivityHeadline(routineName: String, days: Long): String
  fun deloadHeadline(routineName: String): String
  ```

  Registro objetivo, tomado del preview: *"Elevación Lateral lleva 10 sesiones sin subir carga"*, *"No has entrenado Pull desde hace 16 días"*, *"Tu tonelaje de Espalda bajó 18% en los últimos 2 microciclos"*.

  **Explicaciones** (sustituyen a `buildCausalAnalysis`), que además justifican el umbral cuando la ponderación interviene, según el preview: *"Es un ejercicio de progresión difícil, por eso el sistema esperó 10 sesiones antes de avisarte."*

  ```kotlin
  fun plateauExplanation(exerciseName: String, sessions: Int, difficulty: ProgressionDifficulty, cause: PlateauCause): String
  fun progressionRateExplanation(exerciseName: String, rate: Int, difficulty: ProgressionDifficulty, isCrisis: Boolean): String
  fun rirExplanation(routineName: String, avgRir: Double, isLow: Boolean): String
  fun adherenceExplanation(percentage: Int, consecutiveWeeks: Int): String
  fun tonnageExplanation(muscleGroup: String, dropPercentage: Int, isDeload: Boolean): String
  fun inactivityExplanation(routineName: String, days: Long, muscleGroups: List<String>): String
  fun deloadExplanation(routineName: String, regressionPercentage: Int): String
  ```

  **Acción sugerida**: `fun suggestedActionText(kind: SuggestedActionKind, exerciseName: String, routineName: String, incrementKg: Double): String`, con el registro de segunda persona del preview (*"Prueba subir a 13.5 kg aunque bajes un par de repeticiones"*, *"Deja 2 repeticiones en reserva en las primeras series"*).

#### Fase 3 — Persistencia: propagar los datos que la narración necesita

- [x] **T8: Añadir la dificultad a la proyección de tasa de progresión** — `data/local/dao/SessionExerciseDao.kt`, `domain/model/ClassificationCount.kt` (Base: T8 de HU-32)

  `ClassificationCount` gana `progressionDifficulty: String`; `getClassificationCountsByPeriod` añade `e.progression_difficulty AS progressionDifficulty` — el `INNER JOIN exercise e` ya existe. Es lo que hace posible CA-33.04 en el evaluador.

- [x] **T9: Añadir el nombre del ejercicio a la proyección de progresión** — `data/local/dao/SessionExerciseDao.kt`

  `SessionExerciseForProgression` gana `exerciseName: String`; `getSessionExercisesForProgression` añade `e.name AS exerciseName`. Sin él, el titular de la alerta de meseta no puede nombrar el ejercicio en emisión (CA-33.01) y habría que resolverlo con una consulta adicional dentro del bucle.

- [x] **T10: Exponer si un ejercicio tiene alternativa en su puesto** — `data/local/dao/PlanAssignmentDao.kt` (Base: `getAlternativesForSlot`)

  ```kotlin
  @Query("""
      SELECT EXISTS(
          SELECT 1 FROM plan_assignment pa2
          WHERE pa2.routine_version_id = (
              SELECT pa1.routine_version_id FROM plan_assignment pa1
              WHERE pa1.exercise_id = :exerciseId LIMIT 1
          )
          AND pa2.slot = (
              SELECT pa1.slot FROM plan_assignment pa1
              WHERE pa1.exercise_id = :exerciseId LIMIT 1
          )
          AND pa2.exercise_id != :exerciseId
      )
  """)
  suspend fun hasSlotAlternative(exerciseId: Long): Boolean
  ```

  Entrada de CA-33.10. Consulta de solo lectura, sin cambio de esquema.

#### Fase 4 — Emisión: umbrales, ventanas y titulares

- [x] **T11: Ponderar y ampliar la ventana de la tasa de progresión** — `data/repository/SessionRepositoryImpl.kt` → `evaluateLowProgressionRate`

  - `minusWeeks(4)` → `minusWeeks(AlertThresholdRule.PROGRESSION_WINDOW_WEEKS)` (CA-33.03).
  - Guarda de CA-33.09: `if (exerciseCount.totalCount < AlertThresholdRule.PROGRESSION_MIN_OBSERVATIONS) continue` **antes** de calcular, sin resolver ni emitir (D4).
  - `progressionLevel(rate, ProgressionDifficulty.fromCode(exerciseCount.progressionDifficulty))` (CA-33.04).
  - `message = AlertNarrativeRule.progressionRateHeadline(exerciseCount.exerciseName, rate.toInt(), PROGRESSION_WINDOW_WEEKS)` en lugar de `"Tasa: ${rate.toInt()}%"` (CA-33.01).

- [x] **T12: Ampliar la ventana de RIR a tres sesiones** — `data/repository/SessionRepositoryImpl.kt` → `evaluateRirOutOfRange` (CA-33.03, D3)

  `getSessionIdsByRoutineInRange(routine.id, RIR_SUSTAINED_SESSIONS)`; `if (sessionIds.size < RIR_SUSTAINED_SESSIONS) continue` (CA-33.09). La condición pasa de dos variables a `List<Double>`: `allLow = averages.all { isRirLow(it) }`, `allHigh = averages.all { isRirHigh(it) }`, `allOptimal = averages.none { isRirOutOfRange(it) }` — el retiro automático de CA-33.07 conserva su semántica. `message` desde `AlertNarrativeRule.rirHeadline(...)`.

- [x] **T13: Exigir dos semanas consecutivas de adherencia baja** — `data/repository/SessionRepositoryImpl.kt` → `evaluateLowAdherence` (CA-33.03)

  Guarda de historial a `< 14` días (D4, CA-33.09). Se extrae un contador de semanas bajas consecutivas hacia atrás hasta `ADHERENCE_LOOKBACK_WEEKS`, empezando en la semana anterior; el nivel sale de `AlertThresholdRule.adherenceLevel(consecutiveLowWeeks)`. Con `null` se resuelve la alerta activa y no se emite. `message` desde `AlertNarrativeRule.adherenceHeadline(...)`, con el número de semanas consecutivas en la frase. El `resolveAllByType` previo a la inserción se conserva: es el mecanismo de reemplazo de HU-18, no se toca (CA-33.07).

- [x] **T14: Mover los umbrales de tonelaje** — `data/repository/SessionRepositoryImpl.kt` → `evaluateTonnageDrop` (CA-33.03)

  Los porcentajes ya viven en `AlertThresholdRule` (T1): el evaluador no cambia de forma. Se sustituye el `message` por `AlertNarrativeRule.tonnageHeadline(muscleGroup, dropPercentage.toInt(), TONNAGE_MICROCYCLES)`. El filtro `deloadId == null` sobre los microciclos se conserva intacto — es lo que sostiene CA-33.08 en emisión.

- [x] **T15: Mover los umbrales de inactividad** — `data/repository/SessionRepositoryImpl.kt` → `evaluateRoutineInactivity` (CA-33.03)

  Sin cambio estructural. `message` desde `AlertNarrativeRule.inactivityHeadline(routine.name, daysSince)`; las zonas musculares dejan de concatenarse entre paréntesis en el titular y pasan a la explicación del detalle (CA-33.01).

- [x] **T16: Narrar la meseta y la necesidad de descarga en emisión** — `data/repository/SessionRepositoryImpl.kt` → `evaluateProgression`

  `message` de `PLATEAU` desde `AlertNarrativeRule.plateauHeadline(exercise.exerciseName, effectivePlateauThreshold)` en lugar de `"$effectivePlateauThreshold sesiones sin progresión"`. `message` de `ROUTINE_REQUIRES_DELOAD` desde `AlertNarrativeRule.deloadHeadline(routineName)` en lugar de `"≥50% ejercicios en meseta/regresión"` — hoy es la expresión literal de la regla que disparó, el caso de manual de CA-33.01. Requiere resolver el nombre de la rutina en ese punto (`routineDao.getById(routineId)?.name`). Ninguna guarda de descarga ni orden de invocación cambia (CA-33.05, CA-33.07).

#### Fase 5 — Lectura: acción sugerida y explicaciones

- [x] **T17: Sustituir recomendaciones por acción sugerida en el modelo** — `domain/model/AlertDetail.kt` (D7)

  Se eliminan `recommendations`, `showExerciseHistoryLink` y `showDeloadLink`; se añade `suggestedAction: SuggestedAction`. `exerciseId` se conserva: lo usa `AlertDetailViewModel` con independencia del destino de la acción.

- [x] **T18: Construir la acción sugerida en el repositorio** — `data/repository/AlertRepositoryImpl.kt`

  `buildRecommendations` se sustituye por `buildSuggestedAction(alert): SuggestedAction`:

  1. Reúne el contexto: `hasSlotAlternative` (T10), `sessionsWithoutProgression` y el umbral efectivo del ejercicio (`PlateauThresholdRule.effectiveThreshold` con el perfil y la dificultad, patrón de `SessionRepositoryImpl.evaluateProgression`), `isRirLow` (ya existe como `isRirLowAlert`).
  2. `SuggestedActionRule.resolve(context)` → `kind` + `target` (CA-33.02, CA-33.10).
  3. `AlertNarrativeRule.suggestedActionText(kind, ...)` → texto, con el incremento de carga resuelto por `LoadIncrementResolver` como ya hace hoy la rama de `PLATEAU`.

  `CorrectiveActionRule.recommend` pasa a invocarse con el umbral efectivo (T2, D9) y su resultado alimenta la decisión de meseta en lugar de producir viñetas.

- [x] **T19: Redirigir el análisis causal a la narración** — `data/repository/AlertRepositoryImpl.kt`

  `buildCausalAnalysis` conserva su papel de orquestador (recompone datos, resuelve nombres, invoca `PlateauCausalAnalysisRule`) pero deja de contener literales: cada rama delega en la función `…Explanation` correspondiente de `AlertNarrativeRule` (D5, CA-33.01). `buildPlateauCausalAnalysis` y `buildDeloadCausalAnalysis` se conservan como recolectores de datos y devuelven el texto de la regla. La ventana de `getSessionIdsByRoutineInRange(routineId, 4)` de `buildPlateauCausalAnalysis` se mantiene: es contexto causal, no ventana de emisión.

- [x] **T20: Alinear la ventana de la tasa de progresión en lectura** — `data/repository/AlertRepositoryImpl.kt` → `buildProgressionRateTrigger`

  `minusWeeks(4)` → `minusWeeks(AlertThresholdRule.PROGRESSION_WINDOW_WEEKS)`. Sin este ajuste el detalle mostraría una tasa calculada sobre una ventana distinta a la que originó la alerta.

#### Fase 6 — Interfaz

- [x] **T21: Crear `SuggestedActionCard`** — `ui/alerts/SuggestedActionCard.kt` (Base: `ui/alerts/AlertCard.kt`) (CA-33.02)

  Card diferenciada (`RoundedCornerShape(12.dp)`, padding 16 dp, `surfaceContainerHigh`) con el título "Qué puedes hacer" desde `strings.xml`, el texto de la acción y, si `target != null`, un `Button` de altura mínima 48 dp cuya etiqueta depende del destino ("Ver ejercicio →", "Ir al plan →", "Gestionar descarga →"). Sin destino, la card queda solo con texto. Se coloca inmediatamente después de la explicación causal, antes de cualquier `HorizontalDivider` final, para que sea visible sin desplazamiento adicional.

- [x] **T22: Rehacer la sección inferior del detalle** — `ui/alerts/AlertDetailScreen.kt` (D7)

  Se eliminan el bloque `alert_detail_recommendations_title` con sus viñetas y los dos `TextButton` condicionados. Se inserta `SuggestedActionCard`. `AlertDetailScreen` gana `onNavigateToTrainingPlan: () -> Unit` y despacha sobre `SuggestedActionTarget`: `ExerciseHistory` → `onNavigateToExerciseHistory(exerciseId)`, `DeloadManagement` → `onNavigateToDeloadManagement()`, `TrainingPlan` → `onNavigateToTrainingPlan()`.

- [x] **T23: Cablear el destino nuevo** — `ui/navigation/TensionNavHost.kt`

  En el `composable(NavigationRoutes.ALERT_DETAIL)`, añadir `onNavigateToTrainingPlan = { navController.navigate(NavigationRoutes.TRAINING_PLAN) }`. Ruta existente, sin entradas nuevas en `NavigationRoutes` (CA-33.02: "reutiliza rutas ya existentes").

- [x] **T24: Eliminar la duplicación de títulos y reescribirlos** — `ui/alerts/AlertCard.kt`, `ui/alerts/AlertDetailScreen.kt`, `res/values/strings.xml` (CA-33.01)

  La función privada `alertTypeDisplayName`, hoy duplicada literal en ambos archivos, se sustituye por `stringResource` sobre los `alert_type_*` que ya existen en `strings.xml` sin uso. Los textos se reescriben al registro del ejecutante:

  | Clave | Antes | Después |
  |---|---|---|
  | `alert_type_plateau` | Meseta detectada | Llevas tiempo sin avanzar |
  | `alert_type_low_progression` | Tasa de progresión baja | Progresión muy lenta |
  | `alert_type_rir_out_of_range` | RIR fuera de rango | Intensidad fuera de tu rango |
  | `alert_type_low_adherence` | Adherencia baja | Estás entrenando poco |
  | `alert_type_tonnage_drop` | Caída de tonelaje | Tu volumen bajó |
  | `alert_type_routine_inactivity` | Inactividad por rutina | Módulo sin entrenar |
  | `alert_type_routine_requires_deload` | Rutina requiere descarga | Toca bajar el ritmo |

  Nuevas: `alert_detail_suggested_action_title` ("Qué puedes hacer"), `alert_action_go_to_exercise`, `alert_action_go_to_plan`, `alert_action_manage_deload`. `alert_detail_recommendations_title`, `alert_detail_view_history` y `alert_detail_manage_deload` se eliminan al quedar sin referencias.

  **La diferenciación visual de severidad no se toca** (CA-33.06): los `when (alert.level)` de color, la elevación de `CRISIS` y `AlertLevelIndicator` quedan exactamente como están.

- [x] **T25: Alinear el registro del resumen post-sesión** — `ui/session/SessionSummaryScreen.kt` → `formatActionSignal`

  Las señales de acción de HU-13 heredan el criterio de redacción (sección *Interfaz* de la historia): segunda persona y sin terminología del motor. `"Subir carga → %.1f Kg"` pasa a `"Sube a %.1f Kg la próxima vez"` y equivalentes. **No se cambia `ActionSignal` ni su cálculo** — es reescritura de presentación, sin efecto sobre HU-10 ni HU-13.

#### Fase 7 — Tests unitarios (JVM, sin emulador)

- [x] **T26: Reescribir `AlertThresholdRuleTest`** — `test/.../domain/rules/AlertThresholdRuleTest.kt`

  | Caso | Entrada | Esperado | CA |
  |---|---|---|---|
  | Progresión baja: alerta | `39.9, LOW` | `MEDIUM_ALERT` | CA-33.04 |
  | Progresión baja: frontera | `40.0, LOW` | `null` | CA-33.04 |
  | Progresión baja: crisis | `19.9, LOW` | `CRISIS` | CA-33.04 |
  | Progresión media: alerta | `34.9, MEDIUM` | `MEDIUM_ALERT` | CA-33.04 |
  | Progresión media: 38% no alerta | `38.0, MEDIUM` | `null` | CA-33.04 |
  | Progresión media: crisis | `14.9, MEDIUM` | `CRISIS` | CA-33.04 |
  | Progresión alta: alerta | `24.9, HIGH` | `MEDIUM_ALERT` | CA-33.04 |
  | Progresión alta: 30% no alerta | `30.0, HIGH` | `null` | CA-33.04 |
  | Progresión alta: crisis | `9.9, HIGH` | `CRISIS` | CA-33.04 |
  | Difícil no penalizado | `30.0` con `LOW` alerta y con `HIGH` no | — | CA-33.04 |
  | Ventana de progresión | — | `PROGRESSION_WINDOW_WEEKS == 6L` | CA-33.03 |
  | Observaciones mínimas | — | `PROGRESSION_MIN_OBSERVATIONS == 3` | CA-33.09 |
  | RIR: ventana ampliada | — | `RIR_SUSTAINED_SESSIONS == 3` | CA-33.03, D3 |
  | RIR: umbrales conservados | — | `0.5` y `1.8` | D3 |
  | Adherencia: una semana no alerta | `1` | `null` | CA-33.03 |
  | Adherencia: dos semanas alerta | `2` | `MEDIUM_ALERT` | CA-33.03 |
  | Adherencia: tres semanas crisis | `3`, `4` | `CRISIS` | CA-33.03 |
  | Tonelaje: 12% ya no alerta | `12.0, false` | `null` | CA-33.03 |
  | Tonelaje: 16% alerta | `16.0, false` | `MEDIUM_ALERT` | CA-33.03 |
  | Tonelaje: 22% no es crisis | `22.0, false` | `MEDIUM_ALERT` | CA-33.03 |
  | Tonelaje: 26% crisis | `26.0, false` | `CRISIS` | CA-33.03 |
  | Tonelaje en descarga | `30.0, true` | `null` | CA-33.08, D8 |
  | Inactividad: 12 días no alerta | `12` | `null` | CA-33.03 |
  | Inactividad: 15 días alerta | `15` | `MEDIUM_ALERT` | CA-33.03 |
  | Inactividad: 20 días no es crisis | `20` | `MEDIUM_ALERT` | CA-33.03 |
  | Inactividad: 22 días crisis | `22` | `CRISIS` | CA-33.03 |

- [x] **T27: Crear `SuggestedActionRuleTest`** — `test/.../domain/rules/SuggestedActionRuleTest.kt` (Base: `test/.../domain/rules/CorrectiveActionRuleTest.kt`)

  | Caso | Escenario | CA |
  |---|---|---|
  | Toda familia produce acción | las siete familias devuelven `kind` no nulo | CA-33.02 |
  | Tipo desconocido produce acción | tipo inexistente → `REVIEW_TECHNIQUE`, sin destino | CA-33.02 |
  | Meseta temprana | bajo el escalón de rotación → `EXTEND_REPS_BEFORE_LOAD`, destino historial | CA-33.02 |
  | Meseta con alternativa | sobre el escalón y `hasSlotAlternative = true` → `SWITCH_TO_SLOT_ALTERNATIVE` | CA-33.10 |
  | **Meseta sin alternativa** | sobre el escalón y `hasSlotAlternative = false` → `ROTATE_ROUTINE_VERSION`, nunca `SWITCH_TO_SLOT_ALTERNATIVE` | CA-33.10 |
  | Crisis de progresión sin alternativa | `CRISIS` sin alternativa → `INCREASE_LOAD_SLIGHTLY` | CA-33.10 |
  | RIR bajo no navegable | `isRirLow = true` → `LEAVE_REPS_IN_RESERVE`, `target == null` | CA-33.02 |
  | RIR alto navegable | `isRirLow = false` → `INCREASE_LOAD_FOR_STIMULUS`, destino plan | CA-33.02 |
  | Adherencia no navegable | → `INCREASE_WEEKLY_FREQUENCY`, `target == null` | CA-33.02 |
  | Descarga navegable | → `START_DELOAD`, destino gestión de descarga | CA-33.02 |
  | Ningún destino inventado | todo `target` no nulo pertenece a los tres definidos | CA-33.02 |

- [x] **T28: Crear `AlertNarrativeRuleTest`** — `test/.../domain/rules/AlertNarrativeRuleTest.kt` (Base: `test/.../domain/rules/PlateauCausalAnalysisRuleTest.kt`)

  | Caso | Aserción | CA |
  |---|---|---|
  | Sin identificadores internos | ningún titular ni explicación contiene `_id`, `id=`, `exercise_id`, `routine_id` | CA-33.01 |
  | Sin nombres de tipo interno | ningún texto contiene `PLATEAU`, `LOW_PROGRESSION_RATE`, `RIR_OUT_OF_RANGE`, `MEDIUM_ALERT`, `CRISIS` | CA-33.01 |
  | Sin nombres de regla | ningún texto contiene `Rule`, `Threshold`, `umbral` | CA-33.01 |
  | Nombra el elemento | cada titular contiene el nombre del ejercicio, la rutina o el grupo recibido | CA-33.01 |
  | Declara el dato | cada titular contiene el número que lo originó (sesiones, %, días) | CA-33.01 |
  | Justifica el umbral ponderado | la explicación de meseta con `HIGH` menciona que el ejercicio progresa despacio | CA-33.04 |
  | Descarga no se narra como regresión | `tonnageExplanation(isDeload = true)` la describe como esperada | CA-33.08 |
  | Toda acción tiene texto | `suggestedActionText` devuelve no vacío para los once `SuggestedActionKind` | CA-33.02 |
  | Sin cadenas vacías | ninguna función devuelve `""` con entradas válidas | CA-33.02 |

- [x] **T29: Actualizar `CorrectiveActionRuleTest`** — `test/.../domain/rules/CorrectiveActionRuleTest.kt` (D9)

  Los casos existentes se reexpresan con `plateauThreshold = 3`, que reproduce la gradación original (3 y 5) y documenta que el mecanismo no cambió. Casos nuevos: con umbral 5 la rotación no aparece en 5 pero sí en 8; con umbral 10 no aparece en 12 pero sí en 15; bajo el umbral la lista es vacía.

- [x] **T30: Actualizar las fixtures de `AlertDetail`** — `test/.../domain/usecase/alerts/GetAlertDetailUseCaseTest.kt`, `test/.../ui/alerts/AlertDetailViewModelTest.kt` (D7)

  Ambos construyen `AlertDetail` posicionalmente y dejan de compilar al colapsarse los tres campos. Se sustituyen `recommendations`/`showExerciseHistoryLink`/`showDeloadLink` por una `SuggestedAction`, y la aserción sobre `showExerciseHistoryLink` pasa a afirmar sobre el `target`.

- [x] **T31: Ejecutar la suite completa** — `./gradlew testDebugUnitTest`

  Verde al 100 %. Atención a `GetActiveAlertsUseCaseTest`, `GetActiveAlertCountUseCaseTest` y `AlertCenterViewModelTest`: usan `AlertItem`, cuya forma **no** cambia, por lo que no deberían requerir ajuste — si lo requieren, algo se propagó fuera de lo previsto. Que ningún test de HU-10, HU-11, HU-12 ni HU-14 necesite cambios es la evidencia de no regresión de CA-33.05 y CA-33.07.

#### Fase 8 — Documentación

- [x] **T32: Documentar umbrales, ventanas y contenido de la alerta** — `docs/architecture/interfaces_contract.md` (CA-33.03, CA-33.11)

  - `H1-T1`: el `message` del payload se describe como frase en lenguaje natural, no como dato crudo.
  - `H2-T1`: `recommendations` se sustituye por `suggested_action` con `text` y `target` opcional; se retiran `has_deload_link` y `has_exercise_link`.
  - Sección nueva en el Flujo H con la tabla de las cinco familias: umbral de alerta, umbral de crisis, ventana y **justificación** de cada uno, incluida la tabla de ponderación por dificultad de CA-33.04.
  - Nota explícita de la desviación de D3: por qué la familia RIR conserva 0.5 / 1.8 sobre una escala 0–2.

- [x] **T33: Actualizar el modelo de dominio** — `docs/architecture/domain_and_state_model.md`

  En `model alert`, el comentario de `message` deja de describirlo como resumen técnico y pasa a declararlo frase en lenguaje natural compuesta en emisión. Se deja constancia de que no hay cambio de versión de esquema (D10).

- [x] **T34: Cerrar el registro de la historia** — `docs/domain/stories/HU-33-alertas-comprensibles-accionables/dev-record.md`, `cambios.md`, `index.md`

  Dev Agent Record con debug log, notas de completitud, file list y verificación; fases de Refinamiento y Desarrollo en `index.md`.

---

### Riesgos y observaciones

**La desviación de D3 es el punto que más conviene revisar.** Es la única parte del plan que no ejecuta CA-33.03 al pie de la letra, y lo hace por una incompatibilidad de escala verificada en código, no por criterio propio. Queda documentada en el contrato de interfaces precisamente para que un lector futuro del criterio no la interprete como un olvido. Si el PO prefiere el criterio literal, el cambio es de una línea en `AlertThresholdRule` — con el efecto descrito.

**El umbral de observaciones mínimas de la progresión es una calibración nueva, no una transcripción.** CA-33.09 exige la guarda pero no fija su valor; el `3` de D4 es una decisión de desarrollo. Es el número que hay que revisar primero si la familia de progresión resulta demasiado silenciosa o demasiado ruidosa en uso real.

**Las alertas ya activas conservan su texto antiguo.** Los `message` persistidos antes de esta historia no se reescriben: se recomponen solo los emitidos a partir de ahora. En beta la base se reinicia (restricción declarada en la historia), de modo que no se programa migración de textos. La explicación y la acción sugerida del detalle sí se recomponen en lectura, por lo que incluso una alerta antigua abre con contenido nuevo.

**`buildTonnageDropTrigger` y `buildCausalAnalysis` recalculan microciclos con `chunked` sobre listas distintas.** El primero filtra `deloadId == null` antes de trocear y el segundo no lo hace de forma idéntica, lo que puede producir un `isDeload` discrepante entre el dato mostrado y su explicación. Es un defecto preexistente de HU-18 que ninguna CA de esta historia cubre; **no se corrige aquí**, se levanta. Su dueño natural es la historia que revise la analítica de tonelaje.

**La reescritura de títulos afecta a una pantalla que no es la del alcance.** `alert_type_*` se consume desde el Centro de Alertas y desde el detalle. El cambio es intencional (CA-33.01 aplica a "cualquier alerta ... en el Centro de Alertas o en su detalle") y no altera colores, iconos ni orden (CA-33.06).

---

### Validación manual (no automatizable)

Las reglas y los textos se verifican con tests JVM; lo que sigue verifica el cableado real sobre la base de datos y la pantalla.

1. **CA-33.01** — Provocar una alerta de cada familia y recorrer el Centro de Alertas: ningún texto debe contener números de identificador, nombres en mayúsculas del tipo de alerta ni la palabra "umbral". Cada tarjeta debe nombrar el ejercicio, la rutina o el grupo muscular afectado.
2. **CA-33.02** — Abrir el detalle de cada una de las siete alertas posibles: todas deben mostrar el bloque "Qué puedes hacer" con texto, visible sin desplazamiento adicional. Ninguna debe quedarse en la descripción del problema.
3. **CA-33.02 (navegable)** — En una alerta de meseta, tocar el acceso directo: debe abrir el historial del ejercicio. En una de necesidad de descarga, debe abrir la gestión de descarga. En una de tonelaje, el plan de entrenamiento. El área táctil debe cubrir 48 dp de alto.
4. **CA-33.02 (no navegable)** — En una alerta de RIR bajo o de adherencia, el bloque debe aparecer solo con texto, sin botón.
5. **CA-33.03 (progresión)** — Con un ejercicio en dificultad `Baja` y tasa del 38 % sobre seis semanas, debe emitirse alerta. El mismo 38 % en un ejercicio `Alta` no debe emitir ninguna.
6. **CA-33.03 (adherencia)** — Con una única semana por debajo del 60 %, no debe emitirse alerta. Con dos consecutivas, alerta. Con tres, crisis.
7. **CA-33.03 (inactividad)** — Con 12 días sin entrenar un módulo, sin alerta. A los 15, alerta. A los 22, crisis.
8. **CA-33.05** — Con al menos tres alertas activas, una de ellas de crisis: iniciar sesión, registrar una serie y cerrar sesión. Ninguna operación debe bloquearse ni mostrar modal, interstitial o confirmación.
9. **CA-33.06** — Comparar una alerta de crisis contra una de nivel medio en el Centro de Alertas: deben diferir en color y en iconografía, no solo en el texto.
10. **CA-33.07** — Sobre una alerta de tonelaje activa, cerrar una sesión que recupere el volumen: debe resolverse sola y desaparecer de las activas, conservándose en el histórico.
11. **CA-33.08** — Con un protocolo de descarga activo, cerrar el microciclo de descarga: pese a la caída de tonelaje, no debe emitirse alerta de esa familia.
12. **CA-33.09** — Sobre instalación fresca, cerrar una sola sesión: ninguna familia debe emitir alerta, y en particular no debe aparecer ninguna alerta que informe de datos insuficientes.
13. **CA-33.10** — Abrir una alerta de meseta de un ejercicio cuyo puesto **no** tiene alternativa configurada: la acción sugerida no debe proponer cambiar por la alternativa. Añadir una alternativa al puesto, reabrir: entonces sí debe proponerla.
