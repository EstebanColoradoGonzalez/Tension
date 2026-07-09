# Análisis Arquitectónico — HU-04

> HU-04 es la cuarta historia en implementarse. Transforma el stub `TrainingPlanScreen` (D3) creado en HU-03 en una vista funcional y construye la nueva vista D4 (Detalle de Versión del Plan) con las funcionalidades de asignación (RF63) y desasignación (RF64) de ejercicios. Esta historia reutiliza la infraestructura de datos creada en HU-03 — las entidades `ModuleVersionEntity`, `PlanAssignmentEntity`, `ModuleEntity` y `ExerciseEntity` ya existen con su seed data (9 module_versions, 93 plan_assignments). El volumen de entidades nuevas es mínimo (0 tablas nuevas); el esfuerzo se concentra en nuevos DAOs con JOINs, un repositorio nuevo (`PlanRepository`), 4 Use Cases, 2 ViewModels y 2 pantallas funcionales.

**Patrón arquitectónico:** MVVM con capa Domain explícita (4 capas: UI → ViewModel → Domain → Data), según ADR-05. Mismo patrón establecido en HU-01/HU-02/HU-03.

---

## Componentes afectados

### 1. Data Layer — DAO Modificaciones (Existentes)

Paquete: `data.local.dao`.

- **`PlanAssignmentDao`** (Modificación): Actualmente tiene `getByModuleVersionId(Long): Flow<List<PlanAssignmentEntity>>` y `insertAll()`. Se necesitan queries adicionales:
  - `getDetailsByModuleVersionId(moduleVersionId: Long): Flow<List<PlanAssignmentWithExerciseDetails>>` — Query JOIN entre `plan_assignment`, `exercise`, `equipment_type`, `exercise_muscle_zone` y `muscle_zone` para obtener toda la información visual de D4 en una sola consulta.
  - `insert(assignment: PlanAssignmentEntity)` — Insert individual para CA-04.07.
  - `delete(moduleVersionId: Long, exerciseId: Long)` — Delete por PK compuesta para CA-04.08. `@Query("DELETE FROM plan_assignment WHERE module_version_id = :moduleVersionId AND exercise_id = :exerciseId")`.

- **`PlanAssignmentWithExerciseDetails`**: Data class intermedia (no `@Entity`). Campos: `exerciseId: Long`, `exerciseName: String`, `moduleCode: String`, `equipmentTypeName: String`, `muscleZones: String` (GROUP_CONCAT), `sets: Int`, `reps: String`, `isBodyweight: Int`, `isIsometric: Int`, `isToTechnicalFailure: Int`, `isCustom: Int`.

- **`ModuleVersionWithCount`**: Data class intermedia (no `@Entity`), definida en `ModuleVersionDao.kt`. Campos: `id: Long`, `moduleCode: String`, `versionNumber: Int`, `exerciseCount: Int`. Obtenida mediante query: `SELECT mv.id, mv.module_code AS moduleCode, mv.version_number AS versionNumber, COUNT(pa.exercise_id) AS exerciseCount FROM module_version mv LEFT JOIN plan_assignment pa ON mv.id = pa.module_version_id GROUP BY mv.id ORDER BY mv.module_code ASC, mv.version_number ASC`. Esta query única reemplaza la necesidad de 9 Flows individuales de conteo.

- **`ModuleVersionDao`** (Modificación): Se necesita:
  - `getAllWithExerciseCount(): Flow<List<ModuleVersionWithCount>>` — Query combinada con LEFT JOIN a `plan_assignment` + COUNT + GROUP BY. Usado para D3.
  - `getById(moduleVersionId: Long): Flow<ModuleVersionEntity?>` — Para obtener el `module_code` y `version_number` de una versión específica (usado en D4 para construir el título "Módulo X — Versión N").

- **`ExerciseDao`** (Modificación): Se necesita un query adicional para CA-04.07:
  - `getByModuleCodeNotInVersion(moduleCode: String, moduleVersionId: Long): Flow<List<ExerciseWithDetails>>` — Ejercicios del mismo módulo que NO están asignados a la versión dada. `WHERE e.module_code = :moduleCode AND e.id NOT IN (SELECT exercise_id FROM plan_assignment WHERE module_version_id = :moduleVersionId)`. Reutiliza `ExerciseWithDetails` existente.

### 2. Data Layer — Repository (Nuevo)

Paquete: `data.repository`.

- **`PlanRepositoryImpl`**: Implementa `PlanRepository`. Inyecta `PlanAssignmentDao`, `ModuleVersionDao`, `ModuleDao`, `ExerciseDao`. Métodos:
  - `getModulesWithVersionCounts(): Flow<List<ModuleWithVersions>>` — Combina `ModuleDao.getAll()` con `ModuleVersionDao.getAllWithExerciseCount()` usando `combine()`. Agrupa las versiones por `moduleCode` y las asocia al módulo correspondiente.
  - `getVersionDetail(moduleVersionId: Long): Flow<PlanVersionDetail?>` — Combina `ModuleVersionDao.getById()` con `PlanAssignmentDao.getDetailsByModuleVersionId()` para producir el detalle completo de D4.
  - `getAvailableExercisesForVersion(moduleCode: String, moduleVersionId: Long): Flow<List<Exercise>>` — Delega a `ExerciseDao.getByModuleCodeNotInVersion()` y mapea a dominio.
  - `assignExercise(moduleVersionId: Long, exerciseId: Long, sets: Int, reps: String)` — Insert en `plan_assignment`.
  - `unassignExercise(moduleVersionId: Long, exerciseId: Long)` — Delete de `plan_assignment`.

### 3. Domain Layer — Models del plan (Nuevo)

Paquete: `domain.model`.

- **`ModuleWithVersions`**: Data class — `module: Module`, `versions: List<VersionSummary>`. Representa un módulo con sus versiones para D3.
- **`VersionSummary`**: Data class — `moduleVersionId: Long`, `versionNumber: Int`, `exerciseCount: Int`. Una fila dentro de la sección de un módulo en D3.
- **`PlanVersionDetail`**: Data class — `moduleVersionId: Long`, `moduleCode: String`, `moduleName: String`, `versionNumber: Int`, `exercises: List<PlanExercise>`. El detalle completo para D4.
- **`PlanExercise`**: Data class — `exerciseId: Long`, `name: String`, `equipmentTypeName: String`, `muscleZones: List<String>`, `sets: Int`, `reps: String`, `isBodyweight: Boolean`, `isIsometric: Boolean`, `isToTechnicalFailure: Boolean`, `isCustom: Boolean`. El campo `reps` se mapea en la UI: `"8-12"` → "8-12 reps", `"TO_TECHNICAL_FAILURE"` → "Al fallo técnico", `"30-45_SEC"` → "30-45 seg".

### 4. Domain Layer — Repository Interface (Nuevo)

Paquete: `domain.repository`.

- **`PlanRepository`**: Interfaz Kotlin puro. Contratos:
  - `fun getModulesWithVersionCounts(): Flow<List<ModuleWithVersions>>`
  - `fun getVersionDetail(moduleVersionId: Long): Flow<PlanVersionDetail?>`
  - `fun getAvailableExercisesForVersion(moduleCode: String, moduleVersionId: Long): Flow<List<Exercise>>`
  - `suspend fun assignExercise(moduleVersionId: Long, exerciseId: Long, sets: Int, reps: String)`
  - `suspend fun unassignExercise(moduleVersionId: Long, exerciseId: Long)`

### 5. Domain Layer — Use Cases (Nuevo)

Paquete: `domain.usecase.plan`.

- **`GetTrainingPlanUseCase`**: Invoca `PlanRepository.getModulesWithVersionCounts()`. Retorna `Flow<List<ModuleWithVersions>>`. Lectura pura para D3. Produce los 3 módulos con sus 3 versiones y conteos de ejercicios.
- **`GetPlanVersionDetailUseCase`**: Invoca `PlanRepository.getVersionDetail(moduleVersionId)`. Retorna `Flow<PlanVersionDetail?>`. Lectura pura para D4.
- **`AssignExerciseToVersionUseCase`**: Invoca `PlanRepository.assignExercise(moduleVersionId, exerciseId, sets, reps)`. Valida que `sets > 0` y `reps` sea un valor válido (`"8-12"`, `"TO_TECHNICAL_FAILURE"`, `"30-45_SEC"`).
- **`UnassignExerciseFromVersionUseCase`**: Invoca `PlanRepository.unassignExercise(moduleVersionId, exerciseId)`. No elimina el ejercicio del Diccionario ni afecta el historial de sesiones.

> **Nota sobre `GetAvailableExercisesForVersion`:** No se crea un Use Case separado — el `PlanVersionDetailViewModel` puede invocar directamente `PlanRepository.getAvailableExercisesForVersion()` ya que es una consulta de soporte para la UI de asignación.

### 6. Data Layer — Repository Implementation (Nuevo)

Paquete: `data.repository`.

- **`PlanRepositoryImpl`**: `@Inject constructor`. Inyecta `PlanAssignmentDao`, `ModuleVersionDao`, `ModuleDao`, `ExerciseDao`. Implementa `PlanRepository`.
  - `getModulesWithVersionCounts()`: Usa `combine()` de `moduleDao.getAll()` y `moduleVersionDao.getAllWithExerciseCount()`. Agrupa versiones por `moduleCode`, asocia con el módulo correspondiente. Mapea a `ModuleWithVersions`.
  - `getVersionDetail(moduleVersionId)`: Combina `moduleVersionDao.getById()` con `planAssignmentDao.getDetailsByModuleVersionId()` + lookup de `moduleName` desde `moduleDao`. Mapea a `PlanVersionDetail`.
  - `getAvailableExercisesForVersion(moduleCode, moduleVersionId)`: Delega a `exerciseDao.getByModuleCodeNotInVersion()` y mapea `ExerciseWithDetails` → `Exercise`.
  - `assignExercise(moduleVersionId, exerciseId, sets, reps)`: Inserta `PlanAssignmentEntity(moduleVersionId, exerciseId, sets, reps)` via `planAssignmentDao.insert()`.
  - `unassignExercise(moduleVersionId, exerciseId)`: Delega a `planAssignmentDao.delete(moduleVersionId, exerciseId)`.

### 7. Data Layer — Database (Modificación menor)

- **`TensionDatabase`**: No necesita cambios de schema — las entities `ModuleVersionEntity` y `PlanAssignmentEntity` ya existen (creadas en HU-03). La versión de BD no incrementa.

### 8. UI Layer — D3 Plan de Entrenamiento (Reemplazo del stub)

Paquete: `ui.catalog`.

- **`TrainingPlanScreen`** (Reemplazo del stub): Se mantiene la firma existente agregando `viewModel: TrainingPlanViewModel = hiltViewModel()`. Estructura según Wireframes D3 y Especificación Visual §8 D3:
  - **Top Bar**: Reutiliza la estructura del stub actual — `CenterAlignedTopAppBar` con título "Diccionario" + `TabRow` con "Ejercicios" (inactivo, navega a D1) / "Plan" (activo). Se preserva tal cual.
  - **Body**: `LazyColumn` con 3 secciones de módulo. Cada sección:
    - Encabezado: `Title Medium, On Surface` con nombre del módulo ("Módulo A — Superior (Pull + Abs)"). Subtítulo: `Body Small, On Surface Variant` con grupo muscular.
    - 3 `ListItem` M3 de 56 dp por versión: `headlineContent = "Versión N"`, `trailingContent = "(X ej.)"` con conteo dinámico. Clickable → `onNavigateToPlanVersionDetail(moduleVersionId)`.
    - Separación entre módulos: `Spacer 12 dp + Divider Outline Variant + Spacer 12 dp`.
  - **No tiene Bottom Navigation propia** — ya se maneja en el `Scaffold` del `TensionNavHost`.

```kotlin
@Composable
fun TrainingPlanScreen(
    onNavigateToExerciseDictionary: () -> Unit,
    onNavigateToPlanVersionDetail: (Long) -> Unit,
    viewModel: TrainingPlanViewModel = hiltViewModel(),
)
```

- **`TrainingPlanViewModel`**: `@HiltViewModel`. Inyecta `GetTrainingPlanUseCase`. Estado: `StateFlow<TrainingPlanUiState>`. En `init`, recolecta el Flow del Use Case y mapea a `TrainingPlanUiState`.
- **`TrainingPlanUiState`**: Data class con `isLoading: Boolean = true`, `modules: List<ModuleSectionItem> = emptyList()`.
- **`ModuleSectionItem`**: Data class — `moduleCode: String`, `moduleName: String`, `groupDescription: String`, `versions: List<VersionItem>`.
- **`VersionItem`**: Data class — `moduleVersionId: Long`, `versionNumber: Int`, `exerciseCount: Int`.

> **Nota sobre `groupDescription` de Módulo C:** El campo `ModuleEntity.groupDescription` almacena "Cuádriceps, Isquiotibiales, Glúteos, Aductores, Abductores, Gemelos" (Modelo de Datos §3.1). Los Wireframes D3 muestran "(Pierna)" como subtítulo simplificado. Se usa `groupDescription` directamente porque es informativamente más preciso. Los subtítulos de Módulos A y B coinciden exactamente con sus `groupDescription`.

### 9. UI Layer — D4 Detalle de Versión del Plan (Nuevo)

Paquete: `ui.catalog`.

- **`PlanVersionDetailScreen`**: Composable de nivel pantalla. Recibe `moduleVersionId` como argumento de navegación. Estructura según Wireframes D4 y Especificación Visual §8 D4:
  - **Top Bar**: `TensionTopAppBar` con retorno + título dinámico "Módulo X — Versión N" (string resource `plan_version_title_format`: "Módulo %1$s — Versión %2$d").
  - **Subtítulo informativo**: `Text Body Small, On Surface Variant`: "N ejercicios · Sin orden obligatorio" (CA-04.03, RF06). Padding bottom 16 dp.
  - **Lista de ejercicios**: `LazyColumn` con `ListItem` M3 80 dp por ejercicio. Cada fila:
    - Línea 1 (`headlineContent`): Title Medium, On Surface — nombre del ejercicio.
    - Línea 2 (`supportingContent`): Body Medium, On Surface Variant — "Pecho Medio · Máquina" (zona muscular + tipo de equipo, separados por " · ").
    - Línea 3 (`supportingContent`): prescripción. Según tipo:
      - Estándar: "4 series · 8-12 reps"
      - Al fallo técnico (Flexiones): "4 series · Al fallo técnico" — **fontStyle: italic**
      - Isométrico (Plancha, Plancha Lateral): "4 series · 30-45 seg" — **fontStyle: italic**
      - Peso corporal otros: "4 series · 8-12 reps" — sin distinción visual especial
    - `trailingContent`: `IconButton` Delete con tint `Error` (48 dp touch target) para desasignar (CA-04.08). Al tocar → diálogo de confirmación.
    - Clickable → navega a D2 con `exerciseId`.
    - `HorizontalDivider` 1 dp Outline Variant entre filas.
  - **FAB asignar ejercicio** (CA-04.07): `FloatingActionButton` M3, ícono Add (24 dp), containerColor Primary Container. Al tocar → Bottom Sheet con lista de ejercicios del mismo módulo no asignados a esta versión.
  - **Diálogo de confirmación desasignación**: `AlertDialog` M3 con título "Desasignar ejercicio", texto con nombre del ejercicio, botones "Cancelar" / "Desasignar" (color Error).
  - **Bottom Sheet de asignación**: `ModalBottomSheet` M3 con `LazyColumn` de ejercicios disponibles. Al seleccionar uno: sub-formulario para confirmar sets (campo numérico, default "4") y reps (selector con las 3 opciones válidas). Botón "Asignar" confirma y cierra el sheet.
  - **Estado vacío**: Si no hay ejercicios asignados, mostrar "No hay ejercicios asignados a esta versión." Body Large, On Surface Variant.

```kotlin
@Composable
fun PlanVersionDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToExerciseDetail: (Long) -> Unit,
    viewModel: PlanVersionDetailViewModel = hiltViewModel(),
)
```

- **`PlanVersionDetailViewModel`**: `@HiltViewModel`. Recibe `moduleVersionId: Long` via `SavedStateHandle`. Inyecta `GetPlanVersionDetailUseCase`, `AssignExerciseToVersionUseCase`, `UnassignExerciseFromVersionUseCase`, `PlanRepository`. Estado principal: `StateFlow<PlanVersionDetailUiState>`. Estado secundario: `StateFlow<AssignExerciseSheetState>`. Funciones: `onDeleteExercise(exerciseId)`, `onConfirmDelete()`, `onDismissDeleteDialog()`, `onFabClick()`, `onExerciseSelected(exerciseId)`, `onSetsChanged(sets)`, `onRepsSelected(reps)`, `onConfirmAssign()`, `onDismissSheet()`.
- **`PlanVersionDetailUiState`**: Data class — `isLoading: Boolean = true`, `moduleCode: String = ""`, `moduleName: String = ""`, `versionNumber: Int = 0`, `exercises: List<PlanExerciseItem> = emptyList()`, `showDeleteDialog: Boolean = false`, `exerciseToDelete: PlanExerciseItem? = null`.
- **`PlanExerciseItem`**: Data class para UI — `exerciseId: Long`, `name: String`, `equipmentTypeName: String`, `muscleZonesSummary: String`, `sets: Int`, `repsDisplay: String` (ya mapeado a español), `isSpecialCondition: Boolean` (true si es fallo técnico o isométrico — para aplicar italic), `isCustom: Boolean`.
- **`AssignExerciseSheetState`**: Data class — `isVisible: Boolean = false`, `availableExercises: List<ExerciseItem> = emptyList()`, `selectedExerciseId: Long? = null`, `sets: String = "4"`, `reps: String = "8-12"`, `isAssigning: Boolean = false`.

### 10. UI Layer — Mapeo de `reps` a texto español

El campo `reps` en `PlanAssignmentEntity` almacena valores codificados. La UI D4 los mapea:

| Valor en DB | Texto en D4 | Estilo visual |
| --- | --- | --- |
| `"8-12"` | "8-12 reps" | Normal |
| `"TO_TECHNICAL_FAILURE"` | "Al fallo técnico" | *Italic* |
| `"30-45_SEC"` | "30-45 seg" | *Italic* |

```kotlin
fun mapRepsToDisplay(reps: String): Pair<String, Boolean> = when (reps) {
    "TO_TECHNICAL_FAILURE" -> "Al fallo técnico" to true
    "30-45_SEC" -> "30-45 seg" to true
    else -> "$reps reps" to false
}
```

### 11. UI Layer — Navegación (Modificación)

- **`NavigationRoutes`**: Agregar constante: `PLAN_VERSION_DETAIL = "plan-version-detail/{moduleVersionId}"`. Helper function: `fun planVersionDetailRoute(moduleVersionId: Long) = "plan-version-detail/$moduleVersionId"`.
- **`TensionNavHost`**: Actualizar composable `TRAINING_PLAN` para pasar `onNavigateToPlanVersionDetail` real. Agregar composable para `PLAN_VERSION_DETAIL` con `navArgument("moduleVersionId", NavType.LongType)` → `PlanVersionDetailScreen`.
- **`BottomNavigationBar`**: Agregar `"plan-version-detail"` al `childRoutePrefixes` del ítem Diccionario.

### 12. DI Layer — Módulos (Modificación)

- **`RepositoryModule`**: Agregar binding `PlanRepository` ↔ `PlanRepositoryImpl` con `@Binds @Singleton`.

### 13. Recursos (Modificación)

- **`strings.xml`**: Agregar strings para D3: `version_label_format` = "Versión %d", `exercise_count_format_short` = "(%d ej.)". Para D4: `plan_version_title_format` = "Módulo %1$s — Versión %2$d", `plan_exercise_count_subtitle` = "%d ejercicios · Sin orden obligatorio", `reps_technical_failure` = "Al fallo técnico", `reps_isometric_format` = "30-45 seg", `prescription_format` = "%1$d series · %2$s", `unassign_dialog_title` = "Desasignar ejercicio", `unassign_dialog_confirm` = "Desasignar", `assign_exercise_title` = "Asignar ejercicio", `assign_button` = "Asignar", `no_exercises_assigned` = "No hay ejercicios asignados a esta versión.", `fab_assign_exercise` = "Asignar ejercicio".

---

## Integraciones

| Interfaz / Contrato | Productor | Consumidor | Descripción |
| --- | --- | --- | --- |
| `PlanRepository` | `PlanRepositoryImpl` (Data) | Use Cases de plan (Domain) | Contrato de acceso al plan de entrenamiento: lectura de módulos/versiones, detalle de versión, asignación/desasignación |
| `StateFlow<TrainingPlanUiState>` | `TrainingPlanViewModel` | `TrainingPlanScreen` | Estructura jerárquica de 3 módulos × 3 versiones con conteos de ejercicios para D3 |
| `StateFlow<PlanVersionDetailUiState>` | `PlanVersionDetailViewModel` | `PlanVersionDetailScreen` | Lista de ejercicios de una versión con prescripción, estado de diálogo de desasignación |
| `StateFlow<AssignExerciseSheetState>` | `PlanVersionDetailViewModel` | `PlanVersionDetailScreen` | Estado del Bottom Sheet de asignación: ejercicios disponibles, selección, sets/reps |
| `SavedStateHandle` | Navigation Compose | `PlanVersionDetailViewModel` | `moduleVersionId: Long` extraído de la ruta `plan-version-detail/{moduleVersionId}` |
| `ExerciseDao.getByModuleCodeNotInVersion()` | `ExerciseDao` | `PlanRepositoryImpl` | Ejercicios del mismo módulo no asignados a una versión — alimenta el selector de CA-04.07 |
| `PlanAssignmentDao.insert()` / `.delete()` | `PlanAssignmentDao` | `PlanRepositoryImpl` | Operaciones de escritura en `plan_assignment` — primera mutación del catálogo del plan |
| D4 → D2 navegación | `PlanVersionDetailScreen` | `ExerciseDetailScreen` | Al tocar un ejercicio en D4, navega a D2 con `exerciseId`. Retorno via `popBackStack()` vuelve a D4 |

---

## Riesgos

| Riesgo | Probabilidad | Impacto | Mitigación |
| --- | --- | --- | --- |
| Bottom Sheet de asignación con muchos ejercicios disponibles si el módulo tiene muchos custom | Baja | Bajo | Inicialmente el pool es limitado (máx ~15 ejercicios por módulo seed). Si crecen los custom, agregar búsqueda o scroll con índices |
| Desasignación de último ejercicio de una versión deja versión vacía | Baja | Medio | No se impone mínimo de ejercicios — el plan es personalizable (RF64). El estado vacío se maneja con UI placeholder |
| Conteo de ejercicios en D3 se desincroniza si se asigna/desasigna en D4 | Certeza | Bajo | Los conteos se obtienen via `Flow` reactivo. Room invalida automáticamente las queries que dependen de `plan_assignment` |
| El selector de reps en el Bottom Sheet no cubre todos los valores válidos | Media | Bajo | Limitar el selector a los 3 valores del CHECK constraint. No es campo de texto libre |
| La ruta `plan-version-detail/{moduleVersionId}` no marca Diccionario como activo en Bottom Nav | Certeza | Bajo | Agregar `"plan-version-detail"` a `childRoutePrefixes` del ítem Diccionario. Mismo patrón resuelto en HU-03 |
| `PlanAssignmentEntity` usa PK compuesta — Room previene duplicados automáticamente | Muy baja | Bajo | La UI filtra ejercicios ya asignados (query `NOT IN`), previniendo el caso en condiciones normales |

---

## Hitos de implementación

| Hito | Contenido | Dependencias |
| --- | --- | --- |
| 1 | Data Layer — DAO Modificaciones: `PlanAssignmentDao` (+getDetails/insert/delete), `PlanAssignmentWithExerciseDetails`, `ModuleVersionDao` (+getAllWithExerciseCount/+getById), `ModuleVersionWithCount`, `ExerciseDao` (+getByModuleCodeNotInVersion) | HU-03 completada (entities y seed data existentes) |
| 2 | Domain Layer: 4 modelos (`ModuleWithVersions`, `VersionSummary`, `PlanVersionDetail`, `PlanExercise`), `PlanRepository` interfaz, 4 Use Cases | — (Kotlin puro) |
| 3 | Data Layer — Repository: `PlanRepositoryImpl` | Hito 1, Hito 2 |
| 4 | DI: actualizar `RepositoryModule` (+PlanRepository binding) | Hito 3 |
| 5 | UI — D3: `TrainingPlanScreen` (reemplazo del stub), `TrainingPlanViewModel`, `TrainingPlanUiState` | Hito 2, Hito 3 |
| 6 | UI — D4: `PlanVersionDetailScreen`, `PlanVersionDetailViewModel`, `PlanVersionDetailUiState`, `AssignExerciseSheetState`, diálogos de asignación/desasignación | Hito 2, Hito 3 |
| 7 | Navegación: `NavigationRoutes` (+PLAN_VERSION_DETAIL), `TensionNavHost` (actualizar D3, agregar D4), `BottomNavigationBar` (+childRoutePrefix) | Hito 5, Hito 6 |
| 8 | Recursos: `strings.xml` (+strings D3/D4) | — (independiente) |

---

## Notas de auditoría

1. **CA-04.01 (plan precargado) se cumple con el seed data de HU-03.** Las 93 filas de `plan_assignment` y 9 filas de `module_version` ya existen en la BD. HU-04 solo las lee — no necesita seed adicional. Sin conexión a internet (RNF09).
2. **CA-04.02 (detalle de ejercicios) requiere un JOIN multi-tabla.** `PlanAssignmentDao.getDetailsByModuleVersionId()` debe resolver nombre del ejercicio, tipo de equipo, zonas musculares (via GROUP_CONCAT), sets y reps en una sola consulta. Patrón idéntico al `ExerciseDao.getAll()` de HU-03 pero filtrado por `module_version_id`.
3. **CA-04.03 (sin orden obligatorio) se cumple por diseño UI.** El subtítulo "N ejercicios · Sin orden obligatorio" comunica explícitamente que el listado no implica secuencia. RF06 queda satisfecho.
4. **CA-04.04 (condiciones especiales) se resuelve en el mapeo `reps` → display.** Los ejercicios con `reps = "TO_TECHNICAL_FAILURE"` muestran "Al fallo técnico" en italic; los con `reps = "30-45_SEC"` muestran "30-45 seg" en italic. Los demás muestran "8-12 reps" sin distinción visual.
5. **CA-04.05 (todos los módulos y versiones) se cumple con D3 → D4.** D3 muestra las 9 combinaciones (3 módulos × 3 versiones). IDs seed: A-V1=1, A-V2=2, A-V3=3, B-V1=4, B-V2=5, B-V3=6, C-V1=7, C-V2=8, C-V3=9 (Modelo de Datos §3.6).
6. **CA-04.06 (cantidad de ejercicios) se obtiene reactivamente.** D3 muestra el conteo por versión — inicialmente A×3=11, B×3=11, C×3=9. Las asignaciones/desasignaciones actualizan el conteo via Flow reactivo de Room.
7. **CA-04.07 (asignar ejercicio) requiere filtrar ejercicios del mismo módulo no asignados.** La query `ExerciseDao.getByModuleCodeNotInVersion()` usa `WHERE e.module_code = :moduleCode AND e.id NOT IN (SELECT exercise_id FROM plan_assignment WHERE module_version_id = :moduleVersionId)`. El pool incluye ejercicios seed y custom del mismo módulo.
8. **CA-04.08 (desasignar) elimina todo el slot con sus alternativas.** La operación busca el `slot` del ejercicio via `getSlotForExercise()` y elimina todas las filas de `plan_assignment` de ese slot. Los ejercicios permanecen en el Diccionario. **No se permite desasignar mientras haya una sesión activa de esa versión** (`hasActiveSessionForVersion` guard en `UnassignExerciseFromVersionUseCase`).
9. **D3 reutiliza la estructura de tabs de HU-03.** El stub `TrainingPlanScreen` ya tiene la `CenterAlignedTopAppBar` con título "Diccionario" y `TabRow`. Se preserva el TopBar y se reemplaza solo el body.
10. **D4 navega a D2 (Detalle de Ejercicio).** D2 ya es reutilizable desde HU-03. El back stack es: D3 → D4 → D2. El retorno de D2 via `popBackStack()` vuelve a D4. La Bottom Nav es visible con Diccionario activo.
11. **`PlanRepository` es un repositorio separado de `ExerciseRepository`.** Aunque ambos acceden a tablas del catálogo, sus responsabilidades son distintas: `ExerciseRepository` gestiona el Diccionario (lectura de ejercicios y filtros), `PlanRepository` gestiona el Plan de Entrenamiento (lectura de versiones con prescripción, asignación/desasignación).
12. **No se incrementa la versión de la BD.** HU-04 no agrega ni modifica tablas — solo agrega queries y operaciones DML (INSERT/DELETE) sobre tablas existentes.
13. **Compatibilidad con HU-05 (sesión) verificada.** HU-05 necesita obtener los ejercicios de una combinación módulo-versión para armar la sesión. La query `PlanAssignmentDao.getDetailsByModuleVersionId()` creada en HU-04 será reutilizable o extensible para HU-05.
14. **El Bottom Sheet de asignación usa defaults conservadores.** Sets = 4 (siempre 4 en MVP, Modelo de Datos §3.7), reps = "8-12" por defecto. El selector de reps SOLO muestra las 3 opciones válidas del CHECK constraint — no es campo de texto libre.
15. **Tabs D1 ↔ D3 ya implementados en HU-03.** La navegación lateral entre tabs usa `launchSingleTop = true` y `restoreState = true` sin apilamiento. El `TensionNavHost` ya tiene ambos composables conectados.
16. **`ExerciseRepositoryImpl.toDomainModel()` se puede reutilizar.** La conversión de `ExerciseWithDetails` → `Exercise` ya existe en `ExerciseRepositoryImpl`. `PlanRepositoryImpl` necesita el mismo mapeo para `getAvailableExercisesForVersion()`. Se puede extraer a un objeto `ExerciseMapper` en `data.repository` o duplicar el mapper (<15 líneas) para mantener independencia de repositorios.
17. **Validación de integridad cruzada módulo-ejercicio.** El Modelo de Datos §3.7 documenta: "El `exercise_id` referenciado debe pertenecer al mismo módulo que el `module_version_id`." Esta restricción no es expresable como CHECK en SQLite. La query `getByModuleCodeNotInVersion(moduleCode, ...)` ya filtra por módulo, previniendo la asignación cruzada.
18. **Conteo en D3 — implementación resuelta.** Se adopta el enfoque de query única con COUNT: `ModuleVersionDao.getAllWithExerciseCount()` retorna `Flow<List<ModuleVersionWithCount>>`. Este enfoque es más eficiente que 9 Flows individuales y se propaga reactivamente: al insertar/eliminar en `plan_assignment`, Room invalida la query y emite los conteos actualizados.
19. **Compatibilidad con HU-19 (Backup/Restore) verificada.** HU-19 lista `plan_assignment` y `module_version` como datos incluidos en backup/restore. El diseño de tablas y repositorios de HU-04 es compatible.
