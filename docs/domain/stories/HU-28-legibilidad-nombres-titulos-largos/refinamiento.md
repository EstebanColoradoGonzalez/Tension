## Refinamiento Técnico (Developer)
**Autor**: Esteban Colorado González | **Fecha**: 2026-08-30

---

### Contexto

Problema de presentación puro. No toca `domain/`, `data/` ni Room. Todo el cambio vive en `ui/`.

**Estado actual (diagnóstico sobre código):**

| Síntoma | Causa en código |
|---------|-----------------|
| Nombre de ejercicio cortado | `Text(...)` sin `maxLines`/`overflow` en la mayoría de sitios → Compose corta la línea al ancho disponible sin indicador |
| `Serie X de Y` cortado | `RegisterSetScreen.kt:76-99` — nombre y contador apilados en un `Column` dentro de `CenterAlignedTopAppBar` (altura fija 64 dp). Un nombre largo desborda el alto y el contador queda recortado |
| Tratamiento inconsistente | Coexisten `maxLines = 1` (`SessionSummaryScreen`, `ExerciseDictionaryScreen`, `PlanVersionDetailScreen`) y ausencia total de regla (resto). Ninguna regla centralizada |
| Elemento de lista rígido | `ExerciseDictionaryScreen.kt:328` usa `Modifier.height(72.dp)` fijo → una segunda línea no cabe |

**Patrón análogo leído:** `domain/util/RepsDisplayMapper.kt` (objeto Kotlin puro con test JVM `RepsDisplayMapperTest.kt`) + `ui/components/ProgressionIndicator.kt` (composable reutilizable sin sufijo forzado en `ui/components/`). Se replica el par **regla pura testeable + composable que la consume**.

**Arquitectura:** MVVM + Domain Layer (ADR-05). El cambio es exclusivamente capa UI — no cruza a ViewModel, Use Case ni Repository. `ui/components/` es el paquete existente para composables reutilizables transversales (`TensionTopAppBar`, `ProgressionIndicator`, `AlertLevelIndicator`).

**Decisión de diseño — prioridad del contador (CA-28.03):**
En un `Row` de Compose los hijos **sin** `weight` se miden primero con las constraints completas; los hijos **con** `weight` reciben solo el espacio restante. Colocando el contador como hijo sin peso y el nombre con `Modifier.weight(1f)`, el contador nunca cede espacio — la prioridad queda garantizada por construcción del layout, no por un ancho estimado. Cubre también CA-28.06 (pantalla de 5").

**Decisión de diseño — altura de las top app bars:**
`CenterAlignedTopAppBar` tiene altura fija de 64 dp. Las barras que muestran título + subtítulo en `Column` (`RegisterSetScreen`, `SessionSummaryScreen`, `SessionDetailScreen`, `ExerciseHistoryScreen`) requieren `expandedHeight = 96.dp` (API disponible en Material3 1.3.x — Compose BOM 2024.12.01) para admitir 2 líneas de título sin recortar el subtítulo.

**Alcance de tests:** el proyecto no tiene tests de Compose UI (`androidTest/` solo contiene migraciones Room). La regla se extrae a un objeto Kotlin puro sin imports Android, testeable en JVM con JUnit 4 siguiendo el patrón Given/When/Then de `domain/util/*MapperTest.kt`.

---

### Tareas de Implementación

#### Fase 1 — Regla centralizada

- [ ] **T1: Crear la regla de texto largo y sus composables** — `ui/components/TensionText.kt` (Base: `ui/components/ProgressionIndicator.kt` + `domain/util/RepsDisplayMapper.kt`)

  | Elemento | Contenido |
  |----------|-----------|
  | `enum class TensionTextKind` | `ENTITY_NAME`, `COUNTER` |
  | `object TensionTextRules` | `ENTITY_NAME_MAX_LINES = 2`, `COUNTER_MAX_LINES = 1`, `fun maxLinesFor(kind): Int`, `fun isTruncatable(kind): Boolean` |
  | `@Composable EntityNameText(...)` | `maxLines = 2`, `overflow = TextOverflow.Ellipsis`. Parámetros: `text`, `style`, `modifier`, `color`, `textAlign` |
  | `@Composable CounterText(...)` | `maxLines = 1`, `softWrap = false`, `overflow = TextOverflow.Clip` — nunca elipsis, nunca cede espacio. Parámetros: `text`, `style`, `modifier`, `color` |

  KDoc en inglés (§2.1 coding-standards). Sin strings nuevos en `strings.xml`.

#### Fase 2 — Contador con prioridad absoluta (CA-28.03, CA-28.06)

- [ ] **T2: Reestructurar el título de la barra de registro de serie** — `ui/session/RegisterSetScreen.kt:76-99`
  - `Column` → `Row(verticalAlignment = Alignment.CenterVertically)`
  - `EntityNameText(uiState.exerciseName, titleMedium, Modifier.weight(1f))`
  - `Spacer(Modifier.width(8.dp))`
  - `CounterText(...)` con `titleSmall` y color `onSurfaceVariant` — sin `weight`, se mide primero
  - `expandedHeight = 96.dp` en el `CenterAlignedTopAppBar`

#### Fase 3 — Aplicación de la regla en las 7 ubicaciones

- [ ] **T3: Barra superior compartida** — `ui/components/TensionTopAppBar.kt:23`
  - `Text(title)` → `EntityNameText(title, MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)`
  - Cubre de una sola vez: detalle de ejercicio del Diccionario, Crear ejercicio, Perfil, Historial de peso, Ajustes, Backup export/import

- [ ] **T4: Sesión activa** — `ui/session/ActiveSessionScreen.kt`
  - L191 `SessionTopBar` — título rutina + versión → `EntityNameText` (`titleLarge`)
  - L331 `PendingExerciseRow` — `exercise.name` → `EntityNameText` (`titleMedium`)
  - L457 `InProgressExerciseRow` — `exercise.name` → `EntityNameText` (`titleMedium`)
  - L559 `CompletedExerciseRow` — `exercise.name` → `EntityNameText` (`titleMedium`)

- [ ] **T5: Pantalla de inicio** — `ui/home/HomeScreen.kt`
  - L227 `ResumeSessionCard` — título de sesión → `EntityNameText` (`bodyMedium`)
  - L276 `NextSessionCard` — título de sesión → `EntityNameText` (`titleMedium`)

- [ ] **T6: Preview de sesión** — `ui/preview/SessionPreviewScreen.kt`
  - L87 título de la barra → `EntityNameText` (`titleLarge`, centrado)
  - L186 `PreviewExerciseCard` — `exercise.name` → `EntityNameText` (`titleSmall`, conserva `Modifier.weight(1f)`)

- [ ] **T7: Resumen post-sesión** — `ui/session/SessionSummaryScreen.kt`
  - L70-79 subtítulo rutina + versión de la barra → `EntityNameText` (`titleSmall`) + `expandedHeight = 96.dp`
  - L256 `item.name` — `maxLines = 1` → `EntityNameText` (2 líneas), conserva `Modifier.weight(1f, fill = false)` cuando hay chip "Dominado"

- [ ] **T8: Historial y detalle de sesión** — `ui/history/`
  - `SessionHistoryScreen.kt:140` — título rutina + versión de la fila → `EntityNameText` (`titleMedium`)
  - `SessionDetailScreen.kt:82-90` — título de la barra → `EntityNameText` (`titleLarge`) + `expandedHeight = 96.dp`
  - `SessionDetailScreen.kt:246` — `exercise.exerciseName` → `EntityNameText` (`titleMedium`)
  - `ExerciseHistoryScreen.kt:66-70` — `exerciseName` de la barra → `EntityNameText` (`titleLarge`) + `expandedHeight = 96.dp`

- [ ] **T9: Diccionario de Ejercicios** — `ui/catalog/ExerciseDictionaryScreen.kt`
  - L290 `exercise.name` — `maxLines = 1` → `EntityNameText` (2 líneas), conserva `Modifier.weight(1f, fill = false)`
  - L328 `Modifier.height(72.dp)` → `Modifier.heightIn(min = 72.dp)` para admitir la segunda línea sin romper el layout
  - Nombre completo ya accesible en `ExerciseDetailScreen.kt` → `DetailField` (sin `maxLines`) — cubre CA-28.05

- [ ] **T10: Pestaña Plan** — `ui/catalog/`
  - `TrainingPlanScreen.kt:122` — `routine.routineName` → `EntityNameText` (`titleMedium`)
  - `RoutineListScreen.kt:175` — `routine.name` → `EntityNameText` (`bodyLarge`)
  - `RoutineVersionListScreen.kt:63` — `routineName` de la barra → `EntityNameText` (`titleLarge`, centrado)
  - `PlanVersionDetailScreen.kt:90` — título rutina + versión de la barra → `EntityNameText` (`titleLarge`, centrado)
  - `PlanVersionDetailScreen.kt:413` — `exercise.name` en hoja de asignación — `maxLines = 1` → `EntityNameText`
  - `PlanVersionDetailScreen.kt:448` — `exercise.name` seleccionado → `EntityNameText` (`titleMedium`)
  - `PlanVersionDetailScreen.kt:524` — `state.exerciseName` en diálogo de edición → `EntityNameText` (`titleMedium`)

#### Fase 4 — Tests

- [ ] **T11: Test unitario de la regla centralizada** — `test/.../ui/components/TensionTextRulesTest.kt` (Base: `test/.../domain/util/RepsDisplayMapperTest.kt`)
  - JUnit 4, JVM, nomenclatura Given/When/Then con backticks
  - Casos: nombre de entidad → 2 líneas; contador → 1 línea; contador no truncable; nombre truncable; cobertura exhaustiva de `TensionTextKind` (falla si se agrega un tipo sin regla)

#### Fase 5 — Verificación

- [ ] **T12: Compilar y ejecutar la suite completa** — `gradlew :app:testDebugUnitTest` y `gradlew :app:assembleDebug` — 100 % en verde, sin regresiones

---

### Fuera de alcance

- Sin cambios en `domain/`, `data/`, Room ni migraciones (beta sin migración).
- Sin strings nuevos, sin rutas nuevas, sin controles nuevos.
- Los colores semánticos y el tema (RNF23) no se tocan: la regla es independiente del esquema claro/oscuro.
