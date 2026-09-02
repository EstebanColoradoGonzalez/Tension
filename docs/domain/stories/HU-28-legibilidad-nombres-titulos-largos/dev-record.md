## Dev Agent Record — Dev-Rápido

### Debug Log

| # | Tipo | Descripción | Resolución |
|---|------|-------------|------------|
| 1 | Entorno | `JAVA_HOME` del sistema apunta a `C:\apps\java\jdk1.7.0_79`. Gradle 8.11.1 exige Java 8+ y aborta con `Gradle 8.11.1 requires Java 1.8 or later to run` | Se ejecutó Gradle con `JAVA_HOME` apuntando a `C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot` (el JDK ya presente en el `PATH`). No requiere cambios en el proyecto |
| 2 | Layout | `ExerciseDictionaryScreen` fijaba `Modifier.height(72.dp)` en el `ListItem`; con la regla de 2 líneas la segunda línea quedaba recortada | Cambiado a `Modifier.heightIn(min = 72.dp)`. El import `layout.height` quedó sin uso y se reemplazó por `layout.heightIn` |
| 3 | Layout | `CenterAlignedTopAppBar` tiene altura fija de 64 dp; en las barras con título + subtítulo un título de 2 líneas recortaba el subtítulo | Añadido `expandedHeight = 96.dp` en `RegisterSetScreen`, `SessionSummaryScreen`, `SessionDetailScreen` y `ExerciseHistoryScreen` (API de Material3 1.3.x, disponible con Compose BOM 2024.12.01) |

### Completion Notes

- ⚡ Dev-Rápido: regla única de tratamiento de texto largo (hasta 2 líneas + elipsis) centralizada en `ui/components/TensionText.kt` y aplicada de forma consistente en las 7 ubicaciones afectadas.
- **CA-28.01 / CA-28.02 / CA-28.04** — `EntityNameText` reemplaza los `Text` que renderizan nombres de ejercicio, rutina, versión y título de sesión en sesión activa, inicio, preview, resumen post-sesión, historial, detalle de sesión, Diccionario de Ejercicios y pestaña Plan. `TensionTopAppBar` cubre además detalle de ejercicio, crear ejercicio, perfil, historial de peso, ajustes y backup export/import.
- **CA-28.03 / CA-28.06** — La barra de `RegisterSetScreen` pasó de `Column` (nombre encima del contador) a `Row` con el contador como hijo **sin** `weight`. En Compose los hijos sin peso se miden primero con las constraints completas y los hijos con `weight` reciben el remanente, por lo que `Serie X de Y` nunca cede espacio: la prioridad queda garantizada por construcción del layout, no por un ancho estimado. Aplica igual en pantalla de 5" (RNF21).
- **CA-28.05** — Al aplicarse la elipsis, el nombre completo sigue accesible en `ExerciseDetailScreen` → `DetailField`, que no impone `maxLines`. No requirió cambios.
- Se eliminaron los `maxLines = 1` inconsistentes que existían en `SessionSummaryScreen`, `ExerciseDictionaryScreen` y `PlanVersionDetailScreen`; ahora todos consumen la misma regla.
- Sin cambios en `domain/`, `data/`, Room ni migraciones. Sin strings, rutas ni controles nuevos. La regla es independiente del esquema de color, por lo que se comporta igual en tema claro y oscuro (RNF23).
- Verificación: `lintDebug` sin errores (97 warnings preexistentes), `testDebugUnitTest` 378/378 en verde, `assembleDebug` correcto.

### File List

| Acción | Archivo | Descripción |
|--------|---------|-------------|
| Creado | `ui/components/TensionText.kt` | Regla centralizada: `TensionTextKind`, `TensionTextRules` (Kotlin puro, sin imports Android) y los composables `EntityNameText` (2 líneas + elipsis) y `CounterText` (1 línea, sin elipsis ni wrap) |
| Creado | `test/.../ui/components/TensionTextRulesTest.kt` | 6 tests JUnit 4 (JVM) sobre la regla, patrón Given/When/Then |
| Modificado | `ui/components/TensionTopAppBar.kt` | Título → `EntityNameText` centrado. Centraliza 7 pantallas que usan la barra compartida |
| Modificado | `ui/session/RegisterSetScreen.kt` | Barra reestructurada `Column` → `Row`: nombre con `weight(1f)`, contador sin peso (prioridad absoluta) + `expandedHeight = 96.dp` |
| Modificado | `ui/session/ActiveSessionScreen.kt` | Título rutina+versión y nombre de ejercicio en las 3 tarjetas (pendiente, en curso, completado) → `EntityNameText` |
| Modificado | `ui/home/HomeScreen.kt` | Título de sesión en `ResumeSessionCard` y `NextSessionCard` → `EntityNameText` |
| Modificado | `ui/preview/SessionPreviewScreen.kt` | Título de la barra y nombre de ejercicio de la tarjeta → `EntityNameText` |
| Modificado | `ui/session/SessionSummaryScreen.kt` | Subtítulo rutina+versión (+ `expandedHeight = 96.dp`) y nombre de ejercicio (`maxLines = 1` → regla de 2 líneas) |
| Modificado | `ui/history/SessionHistoryScreen.kt` | Título rutina+versión de cada fila → `EntityNameText` |
| Modificado | `ui/history/SessionDetailScreen.kt` | Título de la barra (+ `expandedHeight = 96.dp`) y nombre de ejercicio de la tarjeta → `EntityNameText` |
| Modificado | `ui/history/ExerciseHistoryScreen.kt` | Nombre de ejercicio en la barra → `EntityNameText` + `expandedHeight = 96.dp` |
| Modificado | `ui/catalog/ExerciseDictionaryScreen.kt` | Nombre en lista (`maxLines = 1` → 2 líneas) y `ListItem` de `height(72.dp)` fijo a `heightIn(min = 72.dp)` |
| Modificado | `ui/catalog/TrainingPlanScreen.kt` | Nombre de rutina en la cabecera de grupo → `EntityNameText` |
| Modificado | `ui/catalog/RoutineListScreen.kt` | Nombre de rutina en la lista → `EntityNameText` |
| Modificado | `ui/catalog/RoutineVersionListScreen.kt` | Nombre de rutina en la barra → `EntityNameText` centrado |
| Modificado | `ui/catalog/PlanVersionDetailScreen.kt` | Título rutina+versión de la barra, nombre en la hoja de asignación (`maxLines = 1` → 2 líneas), ejercicio seleccionado y diálogo de edición → `EntityNameText` |
| Modificado | `docs/domain/stories/HU-28-.../refinamiento.md` | Plan técnico de la historia |
| Modificado | `docs/domain/stories/HU-28-.../index.md` | Fases Refinamiento y Desarrollo actualizadas |
| Modificado | `docs/domain/stories/HU-28-.../cambios.md` | Registro cronológico de la fase de desarrollo |

### Métricas Dev-Rápido

- Tiempo sesión IA: 20 min
- Tareas manuales DoD: 0 min
- Tiempo total: 20 min
