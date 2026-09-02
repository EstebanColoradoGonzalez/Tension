## Dev Agent Record — Dev-Rápido

**Autor**: Esteban Colorado González | **Fecha**: 2026-08-31

### Debug Log

| # | Tipo | Descripción | Resolución |
|---|------|-------------|------------|
| 1 | Hallazgo | Las siete reglas de cálculo de HU-15 comparten un patrón no documentado: ante datos insuficientes **devuelven `0.0`** (`ProgressionRateRule` con `totalCount == 0`, `LoadVelocityRule` con `sessionCount <= 1`, `AdherenceRule` con `plannedSessions == 0`, `AvgRirRule` con lista vacía, `VolumeDistributionRule` con `totalSets == 0`). Ese cero llega a la pantalla indistinguible de un cero calculado — es el defecto exacto que CA-35.04 prohíbe | No se tocó ninguna regla (CA-35.05). Se propagó hasta la presentación **la evidencia con la que cada regla ya decide** (`totalCount`, `sessionCount`, `rirValues.size`, `plannedSessions`, tamaño del microciclo) y la decisión de mostrar valor o mensaje se tomó allí. Documentado como D2 y D3 |
| 2 | Alcance | El prototipo `35.preview.txt` presenta como tarjetas de la pantalla de Métricas cuatro indicadores que el sistema **no calcula**: "Tonelaje total semanal", "Por sesión", "En meseta" y "Microciclos". El tonelaje solo se agrega por grupo muscular y por microciclo, en la pantalla de Volumen | Crearlos habría sido añadir cálculos, que es lo que CA-35.05 excluye. Se aplicó la anatomía del prototipo a los indicadores existentes y no se incorporó ninguno nuevo. El propio prototipo declara sus valores ilustrativos. Levantado en `refinamiento.md` § *Riesgos* |
| 3 | Criterio | CA-35.02 pide "una tarjeta por indicador", pero cuatro de los siete indicadores del alcance producen una serie por entidad. Una tarjeta completa por ejercicio repetiría la misma etiqueta, descripción y período treinta veces en la pantalla de Métricas | Se introdujeron dos formas de tarjeta con la misma anatomía: `MetricCard` para el escalar y `MetricListCard` para la serie. La interpretación queda levantada como observación para el PO; revertirla es sustituir un componente por el otro, sin tocar el resto (D4) |
| 4 | Defecto latente | El desplegable de período de `MetricsScreen` guardaba su selección en `remember { mutableStateOf(...) }` **dentro del Composable**, mientras el valor efectivo del cálculo vivía en `MetricsViewModel.progressionWeeks`. Dos copias que coinciden solo porque arrancan iguales. Rotular el valor con la copia del Composable habría mostrado un período que no es necesariamente el que lo produjo | El período subió al `UiState` (`progressionWeeks`, `rirSessionLimit`) y el desplegable pasó a alimentarse del estado. Corrige además la prohibición de §4.2 de los estándares sobre estado de UI fuera del `StateFlow`. Cubierto con dos casos en `MetricsViewModelTest` (D5) |
| 5 | Entorno | `./gradlew` aborta con *"Gradle 8.11.1 requires Java 1.8 or later. You are currently using Java 1.7"* — el `JAVA_HOME` del sistema apunta a `C:\apps\java\jdk1.7.0_79` | Se ejecutó el build exportando `JAVA_HOME=C:/apps/java/JDK_17.0.5`, igual que en HU-32, HU-33 y HU-34. No se modificó ninguna configuración del proyecto |
| 6 | Incidente | Un script de edición en Python abrió `TrendScreen.kt` en modo `'w'` y falló al escribir por un par de sustitutos Unicode en el emoji de tendencia. La apertura ya había truncado el archivo a 0 bytes | El archivo se reescribió completo desde el contenido en contexto. Los tres emojis pasaron a constantes privadas (`ICON_ASCENDING`, `ICON_STABLE`, `ICON_DECLINING`) escritas en UTF-8 directo, verificado byte a byte. Ninguna otra pieza resultó afectada |

### Completion Notes

- ⚡ **Dev-Rápido:** las tres pantallas de analítica pasan a presentar cada indicador con **etiqueta, valor dominante con su unidad, descripción y período**, agrupados en secciones temáticas, y a declarar explícitamente qué falta cuando el dato no alcanza. **Ningún cálculo cambió.**
- **La corrección es estructural, no cosmética (D2).** El estado de un indicador dejó de ser un `Double` con convenciones y pasó a ser `MetricValue`, un tipo cerrado de tres ramas: `Available` —donde el cero es legítimo—, `Insufficient` y `NotApplicable`. Con `when` exhaustivo, mostrar un cero donde falta el dato deja de ser posible por construcción. Es lo que hace verificable CA-35.07 sin depender de una convención de formato.
- **Ningún umbral de suficiencia es una calibración nueva (D3).** Cada uno transcribe la guarda que la regla correspondiente ya ejecutaba antes de devolver `0.0`, y su KDoc cita la guarda de la que procede. Consecuencia buscada: **ningún indicador que hoy muestra un número deja de mostrarlo**. El único cambio observable es que donde salía `0` sin dato ahora sale la explicación.
- **La evidencia de suficiencia viaja en los modelos, no en los cálculos.** `ExerciseProgressionRate` gana `observations`, `ExerciseLoadVelocity` gana `sessionCount` y `RirByRoutine` gana `recordedSets`. Los tres campos se alimentan de proyecciones que **ya existían** (`ClassificationCount.totalCount`, `ExerciseSessionRange.sessionCount`, el tamaño de `getRirValuesByRoutine`): ni una consulta nueva, ni un `JOIN` añadido, ni una regla tocada.
- **`GetTonnageByMuscleGroupUseCase` era la segunda fuente del cero engañoso** y tampoco se tocó: sigue rellenando los doce grupos con `0.0` cuando el microciclo no tiene sesiones. Quien decide es la pantalla, con `sessionsInSelectedMicrocycle` que `VolumeViewModel` calcula desde el mapa de microciclos. Eso permite el corte que pide CA-35.07: grupo sin entrenar en microciclo con sesiones → `0 kg` legítimo; microciclo sin ninguna sesión → mensaje.
- **La regla de presentación es Kotlin puro en `ui/`, con test JVM.** `MetricPresentation.kt` no importa Compose y `MetricPresentationTest` lo cubre con 26 casos sin emulador. Es el precedente de HU-28 (`TensionTextRules` + `TensionTextRulesTest`) aplicado al mismo tipo de problema: una decisión de presentación que merece prueba.
- **Once colores literales salieron del código de pantalla (D6).** `Color(0xFFF0E0E0)` de la tarjeta de adherencia, `Color(0xFF6B4F4F)` de la barra de distribución, los seis del badge de RIR y los tres de la fila de tendencia eran claros fijos que no respondían a `isSystemInDarkTheme()` — contradecían RNF23. Todos tenían equivalente exacto en `TensionSemanticColors`. El esquema visual en tema claro no cambia.
- **La gráfica de tonelaje era el punto más lejos de CA-35.06:** paleta privada de doce colores oscuros sobre `surfaceContainer`, eje Y sin unidad, eje X sin significado y etiquetas de microciclo que se leían como fechas. Ahora declara `kg` sobre el eje Y, `microciclo` bajo el eje X, etiqueta cada punto como `mcN`, cierra con una nota de qué representa cada punto y toma sus doce series de `chartSeries`, con variante clara y oscura.
- **El kilogramo se rotula, no se convierte (D7).** `ExerciseSetEntity.weightKg` ya almacena kilogramos y `WeightConverter` solo interviene en captura (HU-30) y en el detalle de sesión pasada; ningún agregado de analítica pasa por conversión. Lo que faltaba era mostrar la unidad. Se normalizó el símbolo a `kg` y el literal `"Kg/sesión"` incrustado en Kotlin —que además incumplía §3.6— pasó a `strings.xml`.
- **Tests: 41 casos nuevos, ninguno existente modificado.** 26 en `MetricPresentationTest`, 5 en `MetricsViewModelTest`, 6 en `VolumeViewModelTest`, 4 en `TrendViewModelTest`. Los tres ViewModels de analítica no tenían cobertura antes de esta historia. **Que ningún test de `AdherenceRule`, `AvgRirRule`, `TonnageRule`, `TrendClassificationRule` ni `VolumeDistributionRule` necesitara un solo ajuste es la evidencia directa de CA-35.05.**
- **Sin cambio de esquema y sin migración.** Ninguna decisión introduce columnas ni toca la versión de la base de datos. Los tres campos añadidos son de modelos de dominio en memoria, derivados de datos que las consultas ya devolvían.
- **Los tests instrumentados quedan fuera, declarado en el plan.** La anatomía de la tarjeta, la jerarquía tipográfica y la legibilidad en 5" no se verifican con JUnit: `androidTest/` no cubre el paquete `ui/metrics/`. CA-35.02 y CA-35.06 se validan manualmente. Lo que sí quedó con test es la decisión de **qué** se muestra, que es donde vivía el defecto.
- **Observación levantada, no corregida:** la paleta oscura de doce series de la gráfica es una calibración nueva y **no está verificada contra WCAG AA** sobre `surfaceContainer` oscuro. Es el primer punto a revisar si la gráfica se ve mal en tema oscuro.
- **Inconsistencia declarada, no unificada:** el cambio de `Kg` a `kg` alcanza solo a las tres pantallas de analítica. `SessionSummaryScreen`, `SessionDetailScreen`, `RegisterSetScreen` y el historial conservan su rotulación. Unificarlas excedía el alcance de la historia.

### File List

| Acción | Archivo | Descripción |
|--------|---------|-------------|
| Creado | `ui/components/MetricPresentation.kt` | `MetricUnit`, `MetricValue`, `MetricRequirement`, `MetricRequirementKind`, `MetricSufficiencyRules`, `MetricFormatRules` — Kotlin puro, sin Compose (D2, D3) |
| Creado | `ui/components/MetricCard.kt` | Anatomía de tarjeta compartida: `MetricSectionHeader`, `MetricCard`, `MetricCardPair`, `MetricListCard`, `MetricEntityRow`, `MetricValueText`, `MetricInsufficientBlock`, `metricRequirementText`, `metricUnitSymbol` (D1, D4) |
| Creado | `test/…/ui/components/MetricPresentationTest.kt` | 26 casos: cero legítimo vs. dato ausente por indicador, no aplicable, umbrales transcritos y formateo |
| Creado | `test/…/ui/metrics/MetricsViewModelTest.kt` | 5 casos: ventanas en el estado, período efectivo tras cambiarlo, valores sin transformar, error |
| Creado | `test/…/ui/metrics/VolumeViewModelTest.kt` | 6 casos: sesiones del microciclo seleccionado, recálculo al cambiar, historial vacío, suficiencia de la evolución, error |
| Creado | `test/…/ui/metrics/TrendViewModelTest.kt` | 4 casos: requisito con lo que falta, ventana evaluada, tendencias sin transformar, error |
| Modificado | `ui/theme/Color.kt` | Bloque `Analytics Charts`: `ChartSeriesLight` / `ChartSeriesDark` (12 series) y `MetricInsufficientLight` / `Dark` (D6) |
| Modificado | `ui/theme/Theme.kt` | `TensionSemanticColors` gana `chartSeries` y `metricInsufficient`, poblados en ambos esquemas |
| Modificado | `domain/model/ExerciseProgressionRate.kt` | Campo `observations` — evidencia de suficiencia, no entra en el cálculo |
| Modificado | `domain/model/ExerciseLoadVelocity.kt` | Campo `sessionCount` — ídem |
| Modificado | `domain/model/RirByRoutine.kt` | Campo `recordedSets` — ídem |
| Modificado | `domain/usecase/metrics/GetProgressionRateUseCase.kt` | Propaga `c.totalCount`; la llamada a `ProgressionRateRule` no cambia |
| Modificado | `domain/usecase/metrics/GetLoadVelocityUseCase.kt` | Propaga `r.sessionCount` en las dos ramas; `LoadVelocityRule` no cambia |
| Modificado | `domain/usecase/metrics/GetAvgRirByRoutineUseCase.kt` | Propaga `rirValues.size`; `AvgRirRule` y la interpretación no cambian |
| Modificado | `ui/metrics/MetricsUiState.kt` | `Content` gana `progressionWeeks` y `rirSessionLimit` (D5) |
| Modificado | `ui/metrics/MetricsViewModel.kt` | Emite ambas ventanas en el estado; ventanas por defecto como constantes públicas |
| Modificado | `ui/metrics/MetricsScreen.kt` | Tres secciones (`ADHERENCIA`, `INTENSIDAD`, `PROGRESIÓN`) sobre los componentes compartidos; desplegables alimentados del estado; badge de RIR e icono de tendencia con colores del tema; cinco Composables privados retirados |
| Modificado | `ui/metrics/VolumeUiState.kt` | `Content` gana `sessionsInSelectedMicrocycle` (D5) |
| Modificado | `ui/metrics/VolumeViewModel.kt` | Calcula el conteo de sesiones en carga y en selección; `insufficientEvolution` desde `MetricSufficiencyRules.evolution` |
| Modificado | `ui/metrics/VolumeScreen.kt` | Tres secciones (`TONELAJE`, `DISTRIBUCIÓN`, `EVOLUCIÓN`); `MetricBarRow` unifica las dos barras; microciclo vacío muestra el mensaje en lugar de doce ceros; barra de distribución con `colorScheme.secondary` |
| Modificado | `ui/metrics/TonnageChartComposable.kt` | Título de eje Y con unidad, título de eje X, etiquetas `mcN`, nota al pie, series desde `chartSeries`, tipografía de eje a `labelSmall`; extensión privada `Int.sp` eliminada |
| Modificado | `ui/metrics/TrendUiState.kt` | `Content` gana `evaluatedMicrocycles`; `InsufficientData` porta un `MetricRequirement` en vez de un `Int` |
| Modificado | `ui/metrics/TrendViewModel.kt` | Suficiencia desde `MetricSufficiencyRules.trend`; ventana efectiva acotada a la que la regla evalúa |
| Modificado | `ui/metrics/TrendScreen.kt` | Sección `GRUPOS MUSCULARES` con `MetricListCard`; el estado vacío tiene la misma anatomía que el poblado; tres colores literales al tema |
| Modificado | `res/values/strings.xml` | Bloque de analítica reescrito: unidades, estados, seis frases de dato ausente, siete encabezados de sección, ocho descripciones, seis períodos y cuatro claves de gráfica. Retiradas `metrics_no_data`, `metrics_load_velocity_na`, `metrics_adherence_detail`, `volume_tonnage_format`, `volume_insufficient`, `trend_insufficient`, `trend_evaluation_period` |
| Modificado | `docs/architecture/interfaces_contract.md` | Flujo G reescrito: `G1-T1`, `G2-T1` y `G3-T1` con `label`, `unit`, `description`, `period`, `state` y `requirement`; sección nueva de estados y suficiencia con la tabla de transcripción; nota de unidad de presentación (CA-35.08) |
| Creado | `docs/domain/stories/HU-35-…/refinamiento.md` | Plan técnico: 20 tareas en 9 fases, 8 decisiones |
| Creado | `docs/domain/stories/HU-35-…/dev-record.md` | Este registro |
| Modificado | `docs/domain/stories/HU-35-…/index.md` | Fases de Refinamiento y Desarrollo |
| Modificado | `docs/domain/stories/HU-35-…/cambios.md` | Entradas de refinamiento y desarrollo |

### Verificación

| Comando | Resultado |
|---|---|
| `./gradlew testDebugUnitTest` | **579 tests · 0 fallos · 0 errores · 0 omitidos** |
| `./gradlew build` (debug + release + lint + tests) | **BUILD SUCCESSFUL** — release también 579/579 |
| Android Lint | **0 errores** · 95 warnings, todos preexistentes. Ningún string nuevo quedó sin consumir y ningún retirado quedó huérfano |
| Versión de esquema | **Sin cambio.** Ninguna migración; los tres campos añadidos son de modelos en memoria |
| Reglas de cálculo | `AdherenceRule`, `AvgRirRule`, `ProgressionRateRule`, `LoadVelocityRule`, `TonnageRule`, `VolumeDistributionRule` y `TrendClassificationRule` **sin modificar**, y sus tests sin un solo ajuste (CA-35.05) |

Balance de la suite: **+41** — 26 de la regla de presentación y 15 de los tres ViewModels de analítica, que no tenían cobertura. Ningún caso existente se eliminó ni se reescribió.

La validación manual de los 12 escenarios —incluidos la comparación de valores antes/después sobre la misma base, el microciclo vacío frente al grupo no entrenado y el recorrido en tema oscuro— queda registrada en `refinamiento.md` § *Validación manual (no automatizable)* y **no se ejecutó en esta sesión**.

### Métricas Dev-Rápido

- Tiempo sesión IA: 40 min
- Tareas manuales DoD: 0 min
- Tiempo total: 40 min
