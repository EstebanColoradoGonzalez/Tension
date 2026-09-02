## Refinamiento Técnico (Developer)
**Autor**: Esteban Colorado González | **Fecha**: 2026-08-31

---

### Contexto

La analítica de HU-15 vive en un solo paquete, `ui/metrics/`, con tres pantallas y tres ViewModels que consumen nueve Use Cases del paquete `domain/usecase/metrics/`. Cada Use Case delega el número a una regla pura de `domain/rules/` (`AdherenceRule`, `AvgRirRule`, `ProgressionRateRule`, `LoadVelocityRule`, `TonnageRule`, `VolumeDistributionRule`, `TrendClassificationRule`) y devuelve un modelo de dominio plano.

Esta historia **no toca ninguna de esas siete reglas, ni ningún Use Case en su cálculo** (CA-35.05). Interviene en la capa que hay por encima: los modelos que transportan el resultado, el estado de UI que lo expone y el Composable que lo pinta.

El defecto que corrige es estructural, no cosmético. Las siete reglas comparten un patrón: **cuando no tienen datos suficientes devuelven `0.0`**.

```kotlin
ProgressionRateRule.calculate(p, t)         // if (totalCount == 0) return 0.0
LoadVelocityRule.calculate(c, i, n)         // if (sessionCount <= 1) return 0.0
AdherenceRule.calculate(c, p)               // if (plannedSessions == 0) return 0.0
AvgRirRule.calculate(values)                // if (rirValues.isEmpty()) return 0.0
VolumeDistributionRule.calculate(m, 0)      // mapValues { 0.0 }
```

Ese cero viaja hasta la pantalla indistinguible de un cero calculado. Es exactamente el "cero engañoso" que CA-35.04 prohíbe y el que CA-35.07 obliga a separar del cero legítimo. `GetTonnageByMuscleGroupUseCase` añade una segunda fuente del mismo problema: cuando el microciclo seleccionado no tiene sesiones devuelve los doce grupos musculares con `0.0`, rellenando la pantalla de barras vacías que parecen medidas.

La corrección no puede consistir en cambiar el retorno de las reglas —eso sería modificar el cálculo—, sino en **transportar hasta la presentación la evidencia con la que cada regla decidió que no tenía datos**, y decidir allí si se muestra un valor o un mensaje.

#### Estado actual relevante

| Hecho verificado en código | Consecuencia para esta HU |
|---|---|
| Las tres pantallas viven en `ui/metrics/` con tres ViewModels y nueve Use Cases; ningún test cubre el paquete | Todo lo que deba probarse tiene que quedar en una pieza pura testeable en JVM, más tests de ViewModel nuevos |
| Cada regla de cálculo devuelve `0.0` ante datos insuficientes, con una guarda explícita | La condición de suficiencia **ya está escrita** en cada regla. No hay que calibrar umbrales nuevos: se replican los que ya existen (D3) |
| `ClassificationCount.totalCount` existe pero `ExerciseProgressionRate` no lo propaga | La pantalla no puede distinguir 0 % de "sin observaciones". Falta un campo, no un cálculo |
| `ExerciseSessionRange.sessionCount` existe pero `ExerciseLoadVelocity` no lo propaga | Idéntico: `LoadVelocityRule` corta en `sessionCount <= 1` y la UI no lo sabe |
| `RirByRoutine.averageRir` **sí** es nullable y la UI ya pinta `metrics_no_data` = "Sin datos" | Es el único indicador con estado de ausencia, pero el texto no dice qué falta (CA-35.04) |
| `GetTonnageByMuscleGroupUseCase` rellena `ALL_GROUPS` con `0.0` cuando `sessionIds.isEmpty()` | Doce ceros engañosos por microciclo vacío. Se resuelve en el ViewModel con `sessionIds.size`, sin tocar el Use Case |
| `MetricsScreen` guarda la selección de período en `remember { mutableStateOf(...) }` **dentro del Composable**, mientras `MetricsViewModel` la guarda en `progressionWeeks` / `rirSessionLimit` | El período existe pero no es observable desde el estado. CA-35.03 obliga a mostrarlo junto al valor: hay que elevarlo al `UiState` |
| `metrics_load_velocity_na` = "N/A" para peso corporal e isométricos | Tercer estado real: no aplicable. Distinto de insuficiente y de cero (CA-35.04, CA-35.07) |
| `AdherenceCard` fija `containerColor = Color(0xFFF0E0E0)`; `VolumeDistributionCard` fija la barra en `Color(0xFF6B4F4F)`; `RirRoutineRow` fija seis colores de badge | Colores claros literales que no responden a `isSystemInDarkTheme()`. Contradicen RNF23 y CA-35.06 |
| `TensionSemanticColors` ya expone `progressionPositive`, `maintenance`, `regression`, `trendAscending/Stable/Declining`, `alertMediumBg`, `exerciseRowCompletedBg`, `alertCrisisBg` en variante clara y oscura | Los literales anteriores tienen sustituto exacto en el tema. No hay que inventar colores, hay que dejar de esquivarlos |
| `TonnageChartComposable` tiene una paleta privada de 12 colores oscuros (`0xFF8B1A1A`…) y dibuja el eje Y sin unidad y el eje X sin título | Ilegible sobre `surfaceContainer` oscuro y sin significado de ejes. Es el objeto directo de CA-35.06 |
| `TrendChartComposable` (`ui/components/`) solo lo consume `ExerciseHistoryScreen` (F3) | **Fuera de alcance.** La historia cubre las tres pantallas de analítica, no el historial de ejercicio |
| El almacenamiento es kilogramo canónico: `WeightConverter` solo interviene en captura (HU-30) y `SessionDetailScreen` | CA-35.03 se cumple por construcción en analítica. Lo que falta no es convertir, es **rotular** |
| `"Kg/sesión"` está escrito en Kotlin dentro de `LoadVelocityRow`, no en `strings.xml` | Incumple §3.6 de los estándares. Se corrige al normalizar la unidad a `kg` |
| `ui/components/TensionText.kt` aloja un objeto puro (`TensionTextRules`) con test JVM en `test/.../ui/components/` | Precedente directo (HU-28) para ubicar la regla de presentación de esta historia y su test |
| `EntityNameText` ya resuelve el truncado de nombres largos a dos líneas | Los nombres de ejercicio y de grupo muscular de las tarjetas lo reutilizan (HU-28) |

---

### Decisiones técnicas

#### D1 — La anatomía de la tarjeta es una pieza reutilizable, no un patrón copiado tres veces

CA-35.01 y CA-35.02 describen una estructura idéntica para todo indicador: etiqueta, valor dominante con unidad, descripción y período. Si esa estructura se escribe a mano en cada pantalla, los tres sitios divergen a la primera modificación y el criterio deja de ser verificable como propiedad del sistema.

Se crea `ui/components/MetricCard.kt` con los Composables de la anatomía, y las tres pantallas se limitan a alimentarlos. El orden de composición y la dominancia tipográfica del valor quedan definidos en un único archivo. Es la misma resolución que HU-28 dio al tratamiento de nombres largos.

#### D2 — El estado de un indicador es un tipo cerrado, no un `Double` con convenciones

Un indicador puede estar en exactamente tres situaciones, y hoy las tres se representan con el mismo `Double`:

- tiene valor —incluido el cero legítimo de CA-35.07—,
- no tiene datos suficientes para calcularse (CA-35.04),
- no aplica al ejercicio (peso corporal, isométrico).

Se modela como `sealed interface MetricValue` con `Available`, `Insufficient` y `NotApplicable`. Con un tipo cerrado, mostrar un cero donde falta el dato deja de ser posible por construcción, que es lo que CA-35.04 pide; y el `when` exhaustivo obliga a cada pantalla a decidir explícitamente qué pinta en cada rama.

`Insufficient` no lleva texto: lleva un `MetricRequirement` con la naturaleza de lo que falta y cuánto falta. El texto se resuelve en el Composable contra `strings.xml`, porque la pieza es Kotlin puro y §3.6 reserva los textos visibles al recurso.

#### D3 — Los umbrales de suficiencia se transcriben de las reglas, no se calibran

Esta es la decisión que sostiene CA-35.05. Cada condición de "datos insuficientes" se toma **literalmente de la guarda que la regla de cálculo ya ejecuta**:

| Indicador | Guarda existente | Fuente | Constante |
|---|---|---|---|
| Tasa de progresión | `ProgressionRateRule`: `totalCount == 0` | `ClassificationCount.totalCount` | `MIN_PROGRESSION_OBSERVATIONS = 1` |
| Velocidad de carga | `LoadVelocityRule`: `sessionCount <= 1` | `ExerciseSessionRange.sessionCount` | `MIN_LOAD_VELOCITY_SESSIONS = 2` |
| RIR promedio | `AvgRirRule`: `rirValues.isEmpty()` | tamaño de `getRirValuesByRoutine` | `MIN_RIR_SETS = 1` |
| Adherencia | `AdherenceRule`: `plannedSessions == 0` | `AdherenceData.plannedSessions` | `MIN_WEEKLY_TARGET = 1` |
| Tonelaje por grupo | `GetTonnageByMuscleGroupUseCase`: `sessionIds.isEmpty()` | tamaño de `microcycleMap[n]` | `MIN_MICROCYCLE_SESSIONS = 1` |
| Distribución de volumen | `VolumeDistributionRule`: `totalSets == 0` | tamaño de `microcycleMap[n]` | `MIN_MICROCYCLE_SESSIONS = 1` |
| Evolución de tonelaje | `VolumeViewModel`: `totalMicrocycles < 2` | `microcycleMap.size` | `MIN_EVOLUTION_MICROCYCLES = 2` |
| Tendencia por grupo | `TrendViewModel` / `GetMuscleGroupTrendUseCase`: `< 4` completos | conteo de microciclos completos | `MIN_TREND_MICROCYCLES = 4` |

**Ningún indicador que hoy muestra un número deja de mostrarlo.** El único cambio observable es que donde antes salía `0` sin dato, ahora sale la explicación de qué falta. Cualquier otro criterio —por ejemplo exigir tres observaciones para que una tasa sea "fiable"— sería una calibración nueva y contradiría CA-35.05; se descarta explícitamente.

#### D4 — El indicador por entidad se agrupa en una tarjeta, no se replica por entidad

CA-35.02 dice que cada indicador se presenta como una tarjeta. Cuatro de los siete indicadores del alcance no producen un escalar sino una serie por entidad: RIR por módulo, tasa de progresión por ejercicio, velocidad de carga por ejercicio, tonelaje y tendencia por grupo muscular. Emitir una tarjeta completa por entidad repetiría la misma etiqueta, la misma descripción y el mismo período treinta veces en la pantalla de Métricas; el ruido resultante ataca precisamente la comprensión que la historia persigue.

Se resuelve con dos formas de tarjeta, ambas con la anatomía completa de CA-35.02:

- **`MetricCard`** — indicador de valor único. Etiqueta, valor dominante con unidad, descripción, período al pie. Se dispone en rejilla de dos columnas, como el prototipo. La usa Adherencia.
- **`MetricListCard`** — indicador cuyo valor es una serie. La tarjeta porta etiqueta, descripción y período; cada fila porta la entidad y **su** valor con unidad, que es el elemento dominante de la fila. La usan RIR, tasa de progresión, velocidad de carga, tonelaje por grupo, distribución y tendencia.

La lectura de "indicador" que se adopta es la del enunciado de CA-35.01 —"un indicador numérico" con su etiqueta, unidad y descripción—, no la de cada punto de dato. **Es una interpretación, y queda levantada como observación** para que el PO pueda corregirla: revertirla significaría sustituir `MetricListCard` por una rejilla de `MetricCard`, sin tocar nada más del plan.

#### D5 — El período sube al `UiState`

Hoy el desplegable de período de `MetricsScreen` vive en `remember { mutableStateOf(options.first()) }` dentro del Composable, mientras el valor efectivo del cálculo vive en `MetricsViewModel.progressionWeeks`. Las dos copias coinciden por casualidad, porque ambas arrancan en el mismo valor.

CA-35.03 exige que el período se muestre junto al valor. Mostrarlo desde la copia del Composable sería rotular el número con un período que no es necesariamente el que lo produjo. `progressionWeeks` y `rirSessionLimit` pasan a `MetricsUiState.Content` y el desplegable se alimenta del estado. Es además la corrección de la prohibición de §4.2 sobre estado de UI fuera del `StateFlow`.

Lo mismo aplica a `VolumeUiState.Content`, que gana `sessionsInSelectedMicrocycle` — el dato que permite distinguir el tonelaje cero legítimo del microciclo sin entrenar (CA-35.07).

#### D6 — Los colores literales se sustituyen por los del tema; no se crean colores nuevos salvo para la gráfica

CA-35.06 exige legibilidad en tema claro y oscuro. Los literales `Color(0xFFF0E0E0)`, `Color(0xFF6B4F4F)` y los seis del badge de RIR son colores claros fijos que no responden a `isSystemInDarkTheme()`. Todos tienen equivalente exacto en `TensionSemanticColors`, que ya define pareja clara/oscura para señales de progresión, tendencia y niveles de alerta. Se sustituyen; el esquema visual no cambia en tema claro.

La única incorporación al tema es la que la gráfica necesita y no existe: `chartSeries: List<Color>` (la paleta de doce series de `TonnageChartComposable`, hoy privada y solo válida sobre fondo claro) y `metricInsufficient: Color` (el gris neutro del marcador de dato ausente del prototipo). Ambas con variante clara y oscura, siguiendo el patrón de `Color.kt` + `Theme.kt` ya establecido.

#### D7 — El kilogramo se rotula, no se convierte

CA-35.03 se cumple ya en el dato: `ExerciseSetEntity.weightKg` almacena kilogramos y `WeightConverter` solo interviene en captura (HU-30) y en el detalle de sesión pasada. Ningún agregado de analítica pasa por conversión. Lo que falta es que la unidad aparezca.

Se normaliza el símbolo a `kg` en minúscula —el símbolo SI, y el que usa el enunciado de la historia— frente al `Kg` actual de `volume_tonnage_format`. El literal `"Kg/sesión"` incrustado en `LoadVelocityRow` desaparece: pasa a `MetricUnit.KILOGRAM_PER_SESSION` con su símbolo en `strings.xml`. **`SessionSummaryScreen` conserva su `Kg`**: no es pantalla de analítica y queda fuera del alcance declarado de la historia.

#### D8 — Ubicación de las piezas nuevas

| Pieza | Ruta | Motivo |
|---|---|---|
| `MetricUnit`, `MetricValue`, `MetricRequirement`, `MetricRequirementKind`, `MetricSufficiencyRules`, `MetricFormatRules` | `ui/components/MetricPresentation.kt` | Kotlin puro sin imports de Compose, testeable en JVM. Precedente exacto: `TensionTextRules` en `ui/components/TensionText.kt` (HU-28) |
| `MetricSectionHeader`, `MetricCard`, `MetricCardPair`, `MetricListCard`, `MetricEntityRow`, `MetricValueText`, `MetricInsufficientBlock` | `ui/components/MetricCard.kt` | Componentes compartidos por las tres pantallas (D1) |
| `chartSeries`, `metricInsufficient` | `ui/theme/Color.kt`, `ui/theme/Theme.kt` | Colores del tema, junto al resto de semánticos (D6) |

No se crea ningún Use Case, ningún Repository, ninguna entidad y ninguna ruta. **No hay cambio de esquema ni migración**: la versión de la base de datos no se toca.

---

### Tareas de Implementación

#### Fase 1 — Reglas de presentación (Kotlin puro, sin Compose)

- [x] **T1: Crear `MetricPresentation.kt`** — `ui/components/MetricPresentation.kt` (Base: `ui/components/TensionText.kt`) (D2, D3)

  Unidades de presentación, con el kilogramo como unidad canónica de carga (CA-35.03, D7):

  ```kotlin
  enum class MetricUnit { KILOGRAM, KILOGRAM_PER_SESSION, PERCENTAGE, RIR, COUNT }
  ```

  Estado del indicador (D2):

  ```kotlin
  sealed interface MetricValue {
      data class Available(val amount: Double, val unit: MetricUnit) : MetricValue
      data class Insufficient(val requirement: MetricRequirement) : MetricValue
      data object NotApplicable : MetricValue
  }

  data class MetricRequirement(
      val kind: MetricRequirementKind,
      val available: Int,
      val needed: Int,
  ) {
      val missing: Int get() = (needed - available).coerceAtLeast(0)
  }

  enum class MetricRequirementKind {
      EXERCISE_OBSERVATIONS,
      EXERCISE_SESSIONS,
      ROUTINE_SETS,
      WEEKLY_TARGET,
      MICROCYCLE_SESSIONS,
      COMPLETE_MICROCYCLES,
  }
  ```

  `MetricSufficiencyRules` — una función por indicador, cada una con las constantes transcritas de su regla de cálculo (D3). El KDoc de cada constante cita la guarda de la que procede:

  ```kotlin
  object MetricSufficiencyRules {
      const val MIN_PROGRESSION_OBSERVATIONS = 1
      const val MIN_LOAD_VELOCITY_SESSIONS = 2
      const val MIN_RIR_SETS = 1
      const val MIN_WEEKLY_TARGET = 1
      const val MIN_MICROCYCLE_SESSIONS = 1
      const val MIN_EVOLUTION_MICROCYCLES = 2
      const val MIN_TREND_MICROCYCLES = 4

      fun progressionRate(rate: Double, observations: Int): MetricValue
      fun loadVelocity(velocity: Double, sessionCount: Int, isBodyweight: Boolean): MetricValue
      fun averageRir(averageRir: Double?, recordedSets: Int): MetricValue
      fun adherence(percentage: Double, plannedSessions: Int): MetricValue
      fun tonnage(tonnageKg: Double, sessionsInMicrocycle: Int): MetricValue
      fun distribution(percentage: Double, sessionsInMicrocycle: Int): MetricValue
      fun evolution(totalMicrocycles: Int): MetricRequirement?
      fun trend(completeMicrocycles: Int): MetricRequirement?
  }
  ```

  `loadVelocity` devuelve `NotApplicable` cuando `isBodyweight`, **antes** de evaluar la suficiencia: no aplicable y sin datos son estados distintos y el orden importa.

  `MetricFormatRules` — formateo del valor, sin unidad concatenada (la unidad la compone el Composable):

  ```kotlin
  object MetricFormatRules {
      fun formatAmount(amount: Double, unit: MetricUnit): String
  }
  ```

  `KILOGRAM` → entero con separador de millar `es-ES` (patrón de `SessionSummaryScreen:137`); `PERCENTAGE` → entero; `RIR` → un decimal; `KILOGRAM_PER_SESSION` → un decimal **con signo explícito** (`+2,5` / `-1,0` / `0,0`), que es lo que hoy hace a mano `LoadVelocityRow`; `COUNT` → entero.

#### Fase 2 — Tema y componentes de tarjeta

- [x] **T2: Añadir los colores de gráfica y de dato ausente al tema** — `ui/theme/Color.kt`, `ui/theme/Theme.kt` (D6, CA-35.06)

  En `Color.kt`, bajo un bloque nuevo `// Semantic Domain Colors — Analytics Charts (G1, G2, G3)`: las doce series en variante clara (las actuales de `TonnageChartComposable`) y oscura (las mismas aclaradas para contraste sobre `surfaceContainer` oscuro), más `MetricInsufficientLight` / `MetricInsufficientDark` para el marcador neutro.

  En `Theme.kt`, `TensionSemanticColors` gana `chartSeries: List<Color> = emptyList()` y `metricInsufficient: Color = Color.Unspecified`, poblados en `LightSemanticColors` y `DarkSemanticColors`.

- [x] **T3: Crear los componentes de la anatomía de tarjeta** — `ui/components/MetricCard.kt` (Base: `ui/components/TensionText.kt`, `ui/alerts/AlertCard.kt`) (D1, D4, CA-35.01, CA-35.02)

  | Composable | Responsabilidad |
  |---|---|
  | `MetricSectionHeader(title)` | Encabezado temático: `titleSmall` en mayúsculas sobre `onSurfaceVariant` + `HorizontalDivider`. Separación de 24 dp respecto a la sección anterior (§6.3) |
  | `MetricCard(label, value, description, period)` | Anatomía completa de CA-35.02 en orden estricto: etiqueta (`labelLarge`), valor (`headlineMedium`, elemento dominante) con la unidad adyacente en `titleMedium`, descripción (`bodySmall`), separador punteado y período al pie (`labelSmall`, `onSurfaceVariant`). `RoundedCornerShape(12.dp)`, padding 16 dp, `surfaceContainer` |
  | `MetricCardPair(left, right)` | Fila de dos tarjetas con `weight(1f)` y `IntrinsicSize.Min`, para rejilla dentro de `LazyColumn` sin scroll anidado. `right` nulo → la izquierda ocupa media anchura y conserva la retícula |
  | `MetricListCard(label, description, period, content)` | Misma anatomía a nivel de tarjeta, con slot para las filas de entidad (D4) |
  | `MetricEntityRow(name, value, trailing)` | `EntityNameText` con `weight(1f)` + `MetricValueText` + slot opcional (badge de RIR, icono de tendencia). Altura mínima 48 dp (RNF06) |
  | `MetricValueText(value, style)` | `when` exhaustivo sobre `MetricValue`: `Available` → `MetricFormatRules.formatAmount` + símbolo de unidad; `Insufficient` → `MetricInsufficientBlock`; `NotApplicable` → `metric_not_applicable` en cursiva sobre `onSurfaceVariant` |
  | `MetricInsufficientBlock(requirement)` | Marcador neutro `──` en `metricInsufficient` + frase de qué falta, resuelta con `when (requirement.kind)` sobre `strings.xml` con `requirement.missing`. **Nunca** cero ni guion suelto (CA-35.04) |

  El cero legítimo entra por `Available(0.0, unit)` y se pinta con la tipografía dominante normal; el ausente nunca alcanza esa rama. La distinción de CA-35.07 queda garantizada por el tipo, no por una convención de formato.

  Todos los colores desde `MaterialTheme.colorScheme` o `TensionThemeExtended.semanticColors`. Ningún literal `Color(0x…)` en este archivo.

#### Fase 3 — Evidencia de suficiencia en los modelos (sin tocar cálculos)

- [x] **T4: Propagar las observaciones de la tasa de progresión** — `domain/model/ExerciseProgressionRate.kt`, `domain/usecase/metrics/GetProgressionRateUseCase.kt` (D3)

  `ExerciseProgressionRate` gana `observations: Int`, alimentado con `c.totalCount`, que la proyección `ClassificationCount` ya trae. `rate` se sigue calculando con `ProgressionRateRule.calculate(c.positiveCount, c.totalCount)` — **misma llamada, mismo resultado** (CA-35.05).

- [x] **T5: Propagar el número de sesiones de la velocidad de carga** — `domain/model/ExerciseLoadVelocity.kt`, `domain/usecase/metrics/GetLoadVelocityUseCase.kt` (D3)

  `ExerciseLoadVelocity` gana `sessionCount: Int`, alimentado con `r.sessionCount`. En la rama de peso corporal / isométrico se propaga igualmente, para que la UI pueda distinguir "no aplica" de "no aplica y además sin datos". `LoadVelocityRule.calculate` no se toca.

- [x] **T6: Propagar el número de series con RIR registrado** — `domain/model/RirByRoutine.kt`, `domain/usecase/metrics/GetAvgRirByRoutineUseCase.kt` (D3)

  `RirByRoutine` gana `recordedSets: Int` = `rirValues.size`. El retorno temprano con `averageRir = null` se conserva; ahora acompañado del conteo que permite redactar qué falta. `AvgRirRule` y la interpretación por `AlertThresholdRule` quedan intactas.

#### Fase 4 — Textos

- [x] **T7: Reescribir el bloque de analítica de `strings.xml`** — `res/values/strings.xml` (CA-35.01, CA-35.03, CA-35.04)

  **Unidades** (D7): `metric_unit_kg` = "kg", `metric_unit_kg_per_session` = "kg/sesión", `metric_unit_percentage` = "%", `metric_unit_rir` = "RIR".

  **Estados**: `metric_not_applicable` = "No aplica", `metric_insufficient_marker` = "──", y una frase por `MetricRequirementKind`, todas nombrando qué falta y cuánto (CA-35.04):

  | Clave | Texto |
  |---|---|
  | `metric_insufficient_exercise_observations` | "Necesitas registrar este ejercicio al menos una vez en el período" |
  | `metric_insufficient_exercise_sessions` | "Necesitas %1$d sesión más con este ejercicio para calcularla" |
  | `metric_insufficient_routine_sets` | "Necesitas registrar series de este módulo para calcularlo" |
  | `metric_insufficient_weekly_target` | "Define tu objetivo semanal de sesiones en Ajustes" |
  | `metric_insufficient_microcycle_sessions` | "Este microciclo no tiene sesiones registradas" |
  | `metric_insufficient_complete_microcycles` | "Necesitas %1$d microciclos completos más" |

  **Encabezados de sección** (CA-35.02): `metrics_section_adherence` = "ADHERENCIA", `metrics_section_intensity` = "INTENSIDAD", `metrics_section_progression` = "PROGRESIÓN", `volume_section_tonnage` = "TONELAJE", `volume_section_distribution` = "DISTRIBUCIÓN", `volume_section_evolution` = "EVOLUCIÓN", `trend_section_muscle_groups` = "GRUPOS MUSCULARES".

  **Descripciones breves** (CA-35.01), una por indicador, en segunda persona y sin terminología del motor: `metrics_adherence_description` = "Sesiones que completaste frente a tu objetivo semanal"; `metrics_rir_description` = "Repeticiones que te quedaban en reserva al terminar cada serie"; `metrics_progression_description` = "Sesiones en las que subiste carga"; `metrics_load_velocity_description` = "Carga que sumas de media en cada sesión"; `volume_tonnage_description` = "Peso total que levantaste en cada grupo"; `volume_distribution_description` = "Reparto de tus series entre las zonas de cada grupo"; `volume_evolution_description` = "Peso total levantado en cada microciclo"; `trend_description` = "Hacia dónde va tu tonelaje y tu progresión en cada grupo".

  **Períodos** (CA-35.03): `metrics_period_current_week` = "semana actual"; `metrics_period_last_sessions` = "últimas %1$d sesiones"; `metrics_period_last_weeks` = "últimas %1$d semanas"; `volume_period_microcycle` = "microciclo %1$d"; `volume_period_all_microcycles` = "todos los microciclos"; `trend_period_microcycles` = "últimos %1$d microciclos completos".

  **Ejes de gráfica** (CA-35.06): `chart_axis_y_tonnage` = "kg", `chart_axis_x_microcycle` = "microciclo", `chart_legend_note` = "Cada punto es un microciclo completo".

  Se eliminan por quedar sin referencias: `metrics_no_data`, `metrics_load_velocity_na`, `volume_insufficient`, `trend_insufficient`, `trend_evaluation_period`. `volume_tonnage_format` se retira: el formateo pasa a `MetricFormatRules` (T1) y la unidad a `metric_unit_kg`.

#### Fase 5 — Pantalla de Métricas (G1)

- [x] **T8: Elevar el período al estado** — `ui/metrics/MetricsUiState.kt`, `ui/metrics/MetricsViewModel.kt` (D5, CA-35.03)

  `MetricsUiState.Content` gana `progressionWeeks: Int` y `rirSessionLimit: Int`. `MetricsViewModel` los emite en cada `loadMetrics()`; `progressionWeeks` y `rirSessionLimit` dejan de ser campos privados sueltos y pasan a leerse del estado actual. Ningún Use Case cambia de firma ni de argumento: `getProgressionRateUseCase(progressionWeeks)` sigue recibiendo lo mismo (CA-35.05).

- [x] **T9: Recomponer la pantalla en secciones de tarjetas** — `ui/metrics/MetricsScreen.kt` (D1, D4, D6, CA-35.01, CA-35.02, CA-35.03)

  Tres secciones con `MetricSectionHeader`, sustituyendo los `HorizontalDivider` sueltos actuales:

  | Sección | Indicador | Componente | Unidad | Período |
  |---|---|---|---|---|
  | ADHERENCIA | Adherencia semanal | `MetricCard` en `MetricCardPair` | `%` | semana actual |
  | INTENSIDAD | RIR promedio por módulo | `MetricListCard` + fila por rutina | `RIR` | últimas N sesiones |
  | PROGRESIÓN | Tasa de progresión | `MetricListCard` + fila por ejercicio | `%` | últimas N semanas |
  | PROGRESIÓN | Velocidad de carga | `MetricListCard` + fila por ejercicio | `kg/sesión` | últimas N semanas |

  Cada valor se construye con la función correspondiente de `MetricSufficiencyRules` (T1) a partir del modelo ampliado en la Fase 3. `AdherenceCard`, `RirByRoutineCard`, `ProgressionRateSection`, `ProgressionRateRow` y `LoadVelocityRow` desaparecen como Composables privados; su contenido pasa a los componentes compartidos.

  Se conserva sin cambios: los dos desplegables de período —ahora alimentados desde el estado (T8)—, la navegación al historial de ejercicio al tocar una fila, los dos enlaces rápidos del pie y el badge de interpretación de RIR, que pasa al slot `trailing` de `MetricEntityRow` con `alertMediumBg` / `exerciseRowCompletedBg` / `alertCrisisBg` del tema en lugar de sus seis literales (D6). La flecha de tendencia de la tasa de progresión conserva su umbral 60/40 y sus tres colores, tomados ahora de `progressionPositive` / `maintenance` / `regression` — es color de señal, no cálculo, y el icono acompaña siempre al color (RNF05).

  `metrics_rir_reference` se conserva como nota de la tarjeta de RIR: es la leyenda que explica la escala, y CA-35.01 la refuerza en lugar de retirarla.

#### Fase 6 — Pantalla de Volumen (G2)

- [x] **T10: Exponer las sesiones del microciclo seleccionado** — `ui/metrics/VolumeUiState.kt`, `ui/metrics/VolumeViewModel.kt` (D5, CA-35.04, CA-35.07)

  `VolumeUiState.Content` gana `sessionsInSelectedMicrocycle: Int`, poblado con `microcycleMap[selectedMicrocycle]?.size ?: 0` tanto en `loadVolume()` como en `selectMicrocycle()`. Es el dato que separa el tonelaje cero de un grupo no entrenado (legítimo) del microciclo entero sin sesiones (insuficiente). `insufficientEvolution` se conserva; pasa a expresarse con `MetricSufficiencyRules.evolution(totalMicrocycles)`.

- [x] **T11: Recomponer la pantalla en secciones de tarjetas** — `ui/metrics/VolumeScreen.kt` (D1, D4, D6, D7)

  | Sección | Indicador | Componente | Unidad | Período |
  |---|---|---|---|---|
  | TONELAJE | Tonelaje por grupo muscular | `MetricListCard` + `TonnageBarRow` por grupo | `kg` | microciclo N |
  | DISTRIBUCIÓN | Distribución de volumen | `MetricListCard` + fila por zona, agrupada por grupo | `%` | microciclo N |
  | EVOLUCIÓN | Evolución del tonelaje | `MetricListCard` + `TonnageChartComposable` | `kg` | todos los microciclos |

  El selector de microciclo se conserva sobre las secciones. `TonnageBarRow` conserva su barra proporcional y pasa a rotular el valor con `MetricValueText`; la barra se pinta con `MaterialTheme.colorScheme.primary` (ya lo hace) y la de distribución sustituye su `Color(0xFF6B4F4F)` por `colorScheme.secondary` (D6). Con `sessionsInSelectedMicrocycle == 0` la tarjeta completa muestra `MetricInsufficientBlock` en lugar de doce barras a cero (CA-35.04).

- [x] **T12: Rotular y hacer legible la gráfica de tonelaje** — `ui/metrics/TonnageChartComposable.kt` (CA-35.06)

  - Título de eje Y con la unidad (`chart_axis_y_tonnage`) sobre la esquina superior izquierda del lienzo, patrón de `TrendChartComposable.drawTrendChart` (`yAxisLabel`).
  - Título de eje X centrado bajo las etiquetas (`chart_axis_x_microcycle`); las etiquetas de microciclo pasan de `"14"` a `"mc14"`, como el prototipo, para que el número no se lea como una fecha.
  - Nota al pie con `chart_legend_note`.
  - La paleta privada `chartColors` se sustituye por `TensionThemeExtended.semanticColors.chartSeries` (T2), resuelta en el Composable y pasada a `drawChart` — un `DrawScope` no puede leer el tema.
  - Los tamaños de fuente de eje suben de 10 sp a `labelSmall` para ser legibles en 5"; el `width` mínimo del lienzo se recalcula en consecuencia. La extensión privada `Int.sp` del final del archivo se elimina al dejar de usarse.

#### Fase 7 — Pantalla de Tendencia (G3)

- [x] **T13: Recomponer la pantalla y explicitar los datos insuficientes** — `ui/metrics/TrendUiState.kt`, `ui/metrics/TrendViewModel.kt`, `ui/metrics/TrendScreen.kt` (D1, D4, D6, CA-35.04)

  `TrendUiState.Content` gana `completeMicrocycles: Int` para rotular el período. `TrendUiState.InsufficientData` pasa a portar un `MetricRequirement` en lugar de un `Int` suelto, y `TrendViewModel` lo construye con `MetricSufficiencyRules.trend(completedCount)` — el conteo `< 4` es el mismo que hoy (D3), solo cambia el tipo que lo transporta.

  `TrendScreen` presenta una sección GRUPOS MUSCULARES con un `MetricListCard` y una fila por grupo: nombre, etiqueta de dirección e icono. Los tres colores literales de `TrendRow` pasan a `trendAscending` / `trendStable` / `trendDeclining` del tema (D6). La dirección no es un valor numérico: la fila usa el slot `trailing` y la tarjeta declara su unidad como "dirección de la tendencia" en la descripción, no como símbolo.

  El estado de datos insuficientes deja de ser un `Box` centrado con una frase y pasa a `MetricListCard` con `MetricInsufficientBlock`, para que la pantalla vacía tenga la misma anatomía que la poblada.

#### Fase 8 — Tests unitarios (JVM, sin emulador)

- [x] **T14: Crear `MetricPresentationTest`** — `test/.../ui/components/MetricPresentationTest.kt` (Base: `test/.../ui/components/TensionTextRulesTest.kt`)

  | Caso | Entrada | Esperado | CA |
  |---|---|---|---|
  | Cero legítimo de tasa | `progressionRate(0.0, observations = 5)` | `Available(0.0, PERCENTAGE)` | CA-35.07 |
  | Tasa sin observaciones | `progressionRate(0.0, 0)` | `Insufficient(EXERCISE_OBSERVATIONS)` | CA-35.04 |
  | Tasa positiva | `progressionRate(64.0, 5)` | `Available(64.0, …)` | CA-35.05 |
  | Velocidad con una sesión | `loadVelocity(0.0, 1, false)` | `Insufficient(EXERCISE_SESSIONS, missing = 1)` | CA-35.04 |
  | Cero legítimo de velocidad | `loadVelocity(0.0, 6, false)` | `Available(0.0, KILOGRAM_PER_SESSION)` | CA-35.07 |
  | Peso corporal | `loadVelocity(0.0, 1, true)` | `NotApplicable`, nunca `Insufficient` | CA-35.04 |
  | RIR sin series | `averageRir(null, 0)` | `Insufficient(ROUTINE_SETS)` | CA-35.04 |
  | RIR cero legítimo | `averageRir(0.0, 12)` | `Available(0.0, RIR)` | CA-35.07 |
  | Adherencia sin objetivo | `adherence(0.0, 0)` | `Insufficient(WEEKLY_TARGET)` | CA-35.04 |
  | Adherencia cero legítimo | `adherence(0.0, 6)` | `Available(0.0, PERCENTAGE)` | CA-35.07 |
  | Tonelaje de grupo no entrenado | `tonnage(0.0, sessionsInMicrocycle = 6)` | `Available(0.0, KILOGRAM)` | CA-35.07 |
  | Tonelaje de microciclo vacío | `tonnage(0.0, 0)` | `Insufficient(MICROCYCLE_SESSIONS)` | CA-35.04 |
  | Evolución con un microciclo | `evolution(1)` | requisito con `missing == 1` | CA-35.04 |
  | Evolución con dos | `evolution(2)` | `null` | CA-35.04 |
  | Tendencia con dos completos | `trend(2)` | requisito con `missing == 2` | CA-35.04 |
  | Tendencia con cuatro | `trend(4)` | `null` | CA-35.05 |
  | Umbrales transcritos | constantes | `1, 2, 1, 1, 1, 2, 4` | CA-35.05, D3 |
  | Ningún insuficiente formatea número | todas las ramas `Insufficient` | no son `Available` | CA-35.04 |
  | Tonelaje con millar | `formatAmount(12480.0, KILOGRAM)` | `"12.480"` | CA-35.01 |
  | Velocidad con signo | `formatAmount(2.5, KG_PER_SESSION)` y `(-1.0, …)` | `"+2,5"`, `"-1,0"` | CA-35.01 |
  | RIR con un decimal | `formatAmount(1.25, RIR)` | un decimal | CA-35.01 |

- [x] **T15: Crear `MetricsViewModelTest`** — `test/.../ui/metrics/MetricsViewModelTest.kt` (Base: `test/.../ui/history/ExerciseHistoryViewModelTest.kt`)

  Los cuatro Use Cases mockeados con MockK y `StandardTestDispatcher`. Casos: el estado `Content` expone `progressionWeeks` y `rirSessionLimit` iniciales (D5); `changeProgressionPeriod(8)` los refleja en el estado y **vuelve a invocar el Use Case con 8**; `changeRirPeriod` idem; los valores de `AdherenceData`, `RirByRoutine`, `ExerciseProgressionRate` y `ExerciseLoadVelocity` llegan al estado sin transformación numérica (CA-35.05); una excepción produce `Error`.

- [x] **T16: Crear `VolumeViewModelTest`** — `test/.../ui/metrics/VolumeViewModelTest.kt`

  Casos: `sessionsInSelectedMicrocycle` corresponde al microciclo seleccionado; `selectMicrocycle` lo recalcula junto al tonelaje y la distribución; con `microcycleMap` vacío el conteo es 0 y el tonelaje del Use Case sigue devolviendo los doce grupos a cero —**el Use Case no cambia**, quien decide es la pantalla (D3)—; `insufficientEvolution` es cierto con un microciclo y falso con dos.

- [x] **T17: Crear `TrendViewModelTest`** — `test/.../ui/metrics/TrendViewModelTest.kt`

  Casos: con menos de 4 microciclos completos el estado es `InsufficientData` con `missing` correcto; con 4 el estado es `Content` y expone `completeMicrocycles`; los `MuscleGroupTrend` del Use Case llegan sin alterar (CA-35.05); una excepción produce `Error`.

- [x] **T18: Ejecutar la suite completa** — `./gradlew testDebugUnitTest`

  Verde al 100 %. Los tests de las siete reglas de cálculo (`AdherenceRuleTest`, `AvgRirRuleTest`, `TonnageRuleTest`, `TrendClassificationRuleTest`, `VolumeDistributionRuleTest` y los de progresión) **no deben requerir ni un solo ajuste**: es la evidencia directa de CA-35.05. Si alguno cambia, se ha tocado un cálculo y hay que revertirlo.

#### Fase 9 — Documentación

- [x] **T19: Documentar la estructura de presentación de los indicadores** — `docs/architecture/interfaces_contract.md` (CA-35.08)

  En el Flujo G, los payloads de salida de `G1-T1`, `G2-T1` y `G3-T1` pasan a declarar por indicador: `label`, `value`, `unit`, `description`, `period` y `state` (`AVAILABLE` / `INSUFFICIENT` / `NOT_APPLICABLE`), con `requirement` (`kind`, `available`, `needed`) cuando el estado es `INSUFFICIENT`. Los campos de cálculo existentes (`rate_pct`, `kg_per_session`, `rir_avg`, `adherence_pct`, `tonnage_kg`, `classification`) se conservan **sin cambio de nombre ni de semántica**, porque el cálculo no cambia.

  Sección nueva al cierre del Flujo G con la tabla de suficiencia de D3: indicador, guarda de la regla, dato de evidencia y constante. Nota explícita de que el kilogramo es la unidad de presentación de todo agregado (CA-35.03) y de que el cero calculado y el dato ausente son estados distintos (CA-35.07).

- [x] **T20: Cerrar el registro de la historia** — `docs/domain/stories/HU-35-metricas-comprensibles/dev-record.md`, `cambios.md`, `index.md`

  Dev Agent Record con debug log, notas de completitud, file list y verificación; fases de Refinamiento y Desarrollo en `index.md`.

---

### Riesgos y observaciones

**La lectura de "indicador" de D4 es el punto que más conviene revisar.** CA-35.02 dice que cada indicador es una tarjeta; cuatro de los siete indicadores del alcance producen una serie por entidad y el plan los agrupa en una tarjeta con filas en lugar de emitir una tarjeta por ejercicio. La alternativa literal es viable —sustituir `MetricListCard` por una rejilla de `MetricCard`, sin tocar nada más— pero produce treinta tarjetas con la misma descripción repetida en la pantalla de Métricas. Queda levantada para decisión del PO.

**El prototipo muestra indicadores que el sistema no calcula.** `35.preview.txt` incluye "Tonelaje total semanal", "Por sesión", "En meseta" y "Microciclos" como tarjetas de la pantalla de Métricas. Ninguno de los cuatro existe hoy como Use Case: el tonelaje solo se agrega por grupo muscular y por microciclo, en la pantalla de Volumen. Crearlos sería añadir cálculos, que es lo que CA-35.05 excluye. **El plan aplica la anatomía del prototipo a los indicadores existentes y no incorpora ninguno nuevo.** El propio prototipo declara sus valores ilustrativos.

**El prototipo describe el volumen en "series efectivas · últimos 30 días"; el sistema lo calcula en kilogramos por microciclo.** Se conserva la métrica real y se rotula con su unidad y su período verdaderos. Cambiar la magnitud sería cambiar el cálculo.

**El cambio de `Kg` a `kg` alcanza solo a las tres pantallas de analítica** (D7). `SessionSummaryScreen`, `SessionDetailScreen`, `RegisterSetScreen` y el historial conservan su rotulación actual. Queda una inconsistencia visible entre pantallas hasta que otra historia la unifique; corregirla aquí excedería el alcance declarado.

**La paleta oscura de la gráfica es una calibración nueva.** Las doce series claras actuales no tienen contraparte oscura definida en el tema, así que T2 introduce doce colores que nadie ha validado contra WCAG AA sobre `surfaceContainer` oscuro. Es el punto a verificar primero si la gráfica se ve mal en tema oscuro.

**Los tests de UI instrumentados quedan fuera.** La anatomía de la tarjeta, la jerarquía tipográfica y la legibilidad en 5" no se verifican con JUnit: `androidTest/` no cubre hoy el paquete `ui/metrics/` y añadir esa cobertura desbordaría la historia. CA-35.02 y CA-35.06 se validan manualmente (abajo). Lo que sí queda con test es la decisión de qué se muestra —que es donde vive el defecto—, en `MetricPresentationTest`.

**Elevar el período al `UiState` corrige un desincronizado latente.** Hoy el desplegable y el ViewModel mantienen copias independientes del período; coinciden solo porque arrancan iguales. Tras T8 el desplegable refleja siempre el valor con el que se calculó. Es un cambio de comportamiento pequeño y deseado, no una regresión.

---

### Validación manual (no automatizable)

La decisión de qué se muestra se verifica con tests JVM; lo que sigue verifica la composición real sobre el dispositivo.

1. **CA-35.01** — Recorrer Métricas, Volumen y Tendencia: todo número visible debe tener encima una etiqueta, al lado una unidad, debajo una descripción y al pie un período. Ningún número debe quedar suelto.
2. **CA-35.02** — En cualquier tarjeta, el valor debe ser el texto de mayor tamaño. Las tarjetas deben aparecer bajo un encabezado de sección, nunca sueltas entre separadores.
3. **CA-35.03** — Todo tonelaje debe leerse en `kg` minúscula. Ningún indicador con período debe mostrarse sin él, incluido tras cambiar el desplegable de 4 a 12 semanas: el pie de la tarjeta debe pasar a "últimas 12 semanas" **y** el valor debe recalcularse.
4. **CA-35.04 (instalación fresca)** — Sobre base recién creada, abrir las tres pantallas: no debe aparecer ningún cero ni guion. Cada indicador debe declarar qué falta.
5. **CA-35.04 (microciclo vacío)** — En Volumen, retroceder a un microciclo sin sesiones: la tarjeta de tonelaje debe mostrar el mensaje, no doce barras a cero.
6. **CA-35.04 (una sola sesión con un ejercicio)** — La velocidad de carga de ese ejercicio debe decir cuántas sesiones faltan, no `0,0 kg/sesión`.
7. **CA-35.05** — Anotar los valores de adherencia, RIR por módulo, tasa de progresión y velocidad de carga **antes** de instalar el cambio, y compararlos después sobre la misma base de datos: deben ser idénticos dígito a dígito.
8. **CA-35.06 (gráfica)** — En Volumen, con al menos dos microciclos: la gráfica debe mostrar `kg` sobre el eje Y, `microciclo` bajo el eje X y las etiquetas `mc9`, `mc10`… Repetir en tema oscuro: las doce series y las etiquetas de eje deben distinguirse del fondo.
9. **CA-35.06 (5")** — En un dispositivo o emulador de 5": ninguna etiqueta de eje ni de tarjeta debe solaparse ni cortarse; las filas deben conservar 48 dp de alto.
10. **CA-35.07** — Localizar un grupo muscular no entrenado en un microciclo **con** sesiones: debe mostrar `0 kg` con tipografía de valor. Compararlo lado a lado con el microciclo vacío del punto 5: los dos estados deben ser visualmente inconfundibles.
11. **CA-35.07 (peso corporal)** — Un ejercicio de peso corporal en Velocidad de carga debe decir "No aplica", distinto tanto del cero como del mensaje de datos insuficientes.
12. **Tema** — Recorrer las tres pantallas en claro y en oscuro: ninguna tarjeta debe conservar fondo claro fijo, y los badges de RIR y los iconos de tendencia deben mantener contraste en ambos.
