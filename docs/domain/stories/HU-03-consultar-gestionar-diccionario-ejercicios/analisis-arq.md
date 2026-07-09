## Análisis Arquitectónico

> HU-03 es la tercera historia en implementarse. Introduce el catálogo de datos estáticos del dominio (módulos, zonas musculares, tipos de equipo, ejercicios y su relación N:M con zonas) y el sistema de seed data con `RoomDatabase.Callback.onCreate()`. Construye las vistas D1 (Diccionario) y D2 (Detalle de Ejercicio) con el sistema de tabs compartido con D3, que queda como stub para HU-04. Esta es la historia con mayor volumen de entidades nuevas (5 tablas + 1 join table) y seed data (43 + 15 + 9 + 3 + 9 + 93 + 48 = 220 filas).

**Patrón arquitectónico:** MVVM con capa Domain explícita (4 capas: UI → ViewModel → Domain → Data), según ADR-05. Mismo patrón establecido en HU-01/HU-02.

### Componentes afectados

#### 1. Data Layer — Entities del catálogo (Nuevo)

Mapeo 1:1 de las tablas del Modelo de Datos §3.1 a §3.5. Paquete: `data.local.entity`.

- **`ModuleEntity`**: Tabla `module`. PK natural `code: TEXT` ("A", "B", "C"). Columnas: `name` (TEXT, NOT NULL), `group_description` (TEXT, NOT NULL), `load_increment_kg` (REAL, NOT NULL). `CHECK(code IN ('A', 'B', 'C'))`. Solo 3 filas, inmutables.
- **`MuscleZoneEntity`**: Tabla `muscle_zone`. PK autoincrement. Columnas: `name` (TEXT, NOT NULL, UNIQUE), `muscle_group` (TEXT, NOT NULL). Índice en `muscle_group`. 15 filas inmutables.
- **`EquipmentTypeEntity`**: Tabla `equipment_type`. PK autoincrement. Columnas: `name` (TEXT, NOT NULL, UNIQUE). 9 filas inmutables.
- **`ExerciseEntity`**: Tabla `exercise`. PK autoincrement. Columnas: `name` (TEXT, NOT NULL), `module_code` (TEXT, FK → `module(code)`, ON DELETE RESTRICT), `equipment_type_id` (INTEGER, FK → `equipment_type(id)`, ON DELETE RESTRICT), `is_bodyweight` (INTEGER, NOT NULL, DEFAULT 0), `is_isometric` (INTEGER, NOT NULL, DEFAULT 0), `is_to_technical_failure` (INTEGER, NOT NULL, DEFAULT 0), `is_custom` (INTEGER, NOT NULL, DEFAULT 0), `media_resource` (TEXT, NULL). UNIQUE(`name`, `equipment_type_id`). Índices en `module_code` y `equipment_type_id`. 43 filas seed + filas dinámicas creadas por el ejecutante (RF62).
- **`ExerciseMuscleZoneEntity`**: Tabla `exercise_muscle_zone`. PK compuesta (`exercise_id`, `muscle_zone_id`). FKs a `exercise(id)` y `muscle_zone(id)`, ambas ON DELETE RESTRICT. 48 filas (38 ejercicios × 1 zona + 5 ejercicios × 2 zonas).

#### 2. Data Layer — Entities auxiliares del plan (Nuevo)

Aunque HU-04 implementará las vistas D3/D4, las entidades `module_version` y `plan_assignment` se crean en HU-03 porque: (a) el seed data debe ir completo en una única transacción atómica (ADR-11), y (b) `module_version` es referenciada por `session` y `rotation_state` en historias posteriores (HU-05+). Paquete: `data.local.entity`.

- **`ModuleVersionEntity`**: Tabla `module_version`. PK autoincrement. Columnas: `module_code` (TEXT, FK → `module(code)`), `version_number` (INTEGER, NOT NULL). UNIQUE(`module_code`, `version_number`). `CHECK(version_number >= 1 AND version_number <= 3)`. 9 filas.
- **`PlanAssignmentEntity`**: Tabla `plan_assignment`. PK compuesta (`module_version_id`, `exercise_id`). FKs a `module_version(id)` y `exercise(id)`. Columnas: `sets` (INTEGER, NOT NULL), `reps` (TEXT, NOT NULL). `CHECK(sets > 0)`, `CHECK(reps IN ('8-12', 'TO_TECHNICAL_FAILURE', '30-45_SEC'))`. 93 filas.

#### 3. Data Layer — DAOs del catálogo (Nuevo)

Paquete: `data.local.dao`.

- **`ExerciseDao`**: Queries para D1, D2 y D5.
  - `getAll(): Flow<List<ExerciseWithDetails>>` — query con JOIN a `equipment_type`, `module` y a `exercise_muscle_zone`+`muscle_zone` para obtener nombre de equipo, nombre de módulo y lista de zonas musculares en una sola consulta. Devuelve la lista completa para filtrado en memoria (43 ejercicios es un dataset trivial).
  - `getById(exerciseId: Long): Flow<ExerciseWithDetails?>` — mismo JOIN pero filtrado por ID. Usado por D2.
  - `insert(exercise: ExerciseEntity): Long` — inserta un ejercicio custom y retorna el ID generado.
  - `insertMuscleZone(zone: ExerciseMuscleZoneEntity)` — inserta una relación ejercicio-zona muscular.
  - `insertAllMuscleZones(zones: List<ExerciseMuscleZoneEntity>)` — inserta múltiples relaciones en lote.
  - `insertExerciseWithMuscleZones(exercise: ExerciseEntity, muscleZones: List<ExerciseMuscleZoneEntity>): Long` — método `@Transaction` atómico, retorna ID generado.
  - `updateMediaResource(exerciseId: Long, mediaResource: String?)` — actualiza la imagen de un ejercicio.
  - `countByNameAndEquipment(name: String, equipmentTypeId: Long): Int` — verifica unicidad nombre+equipo.
- **`ModuleDao`**: `getAll(): Flow<List<ModuleEntity>>` — los 3 módulos. Usado para opciones del dropdown de filtro por módulo en D1.
- **`EquipmentTypeDao`**: `getAll(): Flow<List<EquipmentTypeEntity>>` — los 9 tipos de equipo. Usado para opciones del dropdown de filtro por equipo.
- **`MuscleZoneDao`**: `getAll(): Flow<List<MuscleZoneEntity>>` — las 15 zonas musculares. Usado para opciones del dropdown de filtro por zona.
- **`ModuleVersionDao`**: `getAll(): Flow<List<ModuleVersionEntity>>` — las 9 versiones. Provisorio para HU-04.
- **`PlanAssignmentDao`**: `getByModuleVersionId(id: Long): Flow<List<PlanAssignmentWithExercise>>` — ejercicios asignados a una versión. Provisorio para HU-04.

**Nota sobre `ExerciseWithDetails`:** Data class intermedia (no `@Entity`) usada como resultado de query manual con JOIN. Contiene: `id: Long`, `name: String`, `moduleCode: String`, `moduleName: String`, `equipmentTypeName: String`, `isBodyweight: Int`, `isIsometric: Int`, `isToTechnicalFailure: Int`, `isCustom: Int`, `mediaResource: String?`, `muscleZones: String?` (resultado de `GROUP_CONCAT`, nullable cuando el ejercicio no tiene zonas asignadas). El repository transforma `muscleZones` a `List<String>` mediante `split(", ")` y filtra blancos.

#### 4. Data Layer — Seed Data (Nuevo)

Paquete: `data.local.seed`. Según ADR-11, se usa `RoomDatabase.Callback.onCreate()` con patrón Facade.

- **`PrepopulateCallback`**: Implementa `RoomDatabase.Callback`. En `onCreate()` ejecuta `PrepopulateFacade.populate(db)` dentro de una transacción.
- **`PrepopulateFacade`**: Orquesta la inserción secuencial delegando en 3 seeders temáticos alineados con ADR-11: (1) `ModuleSeeder`, (2) `ExerciseSeeder`, (3) `PlanSeeder`. El orden respeta las dependencias FK. **No incluye `RotationSeeder`** — el estado de rotación se inicializa en `ProfileRepositoryImpl.createProfile()` (HU-01), porque es estado de usuario que solo existe cuando hay perfil (Modelo de Datos §3.14).
- **`ModuleSeeder`**: Inserta en orden: (a) 3 filas en `module` (Modelo de Datos §3.1), (b) 15 filas en `muscle_zone` (§3.2), (c) 9 filas en `equipment_type` (§3.3). Agrupa entidades de referencia sin dependencias cruzadas.
- **`ExerciseSeeder`**: Inserta: (a) 43 filas en `exercise` — cada ejercicio incluye `media_resource` con el nombre normalizado del ejercicio + tipo de equipo (ver convención §16); (b) 48 filas en `exercise_muscle_zone`. Los 5 ejercicios multi-zona del Módulo C generan 2 filas cada uno.
- **`PlanSeeder`**: Inserta: (a) 9 filas en `module_version` (Modelo de Datos §3.6), (b) 93 filas en `plan_assignment`. Mapea `reps`: `"8-12"` estándar, `"TO_TECHNICAL_FAILURE"` para Flexiones, `"30-45_SEC"` para Plancha/Plancha Lateral.

#### 5. Data Layer — Database (Modificación)

- **`TensionDatabase`**: Agregar las 7 nuevas entidades al array `entities`. Versión incrementa a 2 (se mantiene `fallbackToDestructiveMigration()` durante desarrollo). Exponer los 6 nuevos DAOs. Registrar `PrepopulateCallback` en el builder con `.addCallback(PrepopulateCallback(...))`.

#### 6. Data Layer — Repository (Nuevo)

Paquete: `data.repository`.

- **`ExerciseRepositoryImpl`**: Implementa `ExerciseRepository`. Inyecta `ExerciseDao`, `ModuleDao`, `EquipmentTypeDao`, `MuscleZoneDao`. Métodos: `getAllExercises()`, `getExerciseById(id)`, `getAllModules()`, `getAllEquipmentTypes()`, `getAllMuscleZones()`, `createExercise(...)` (`@Transaction` atómico), `updateExerciseImage(exerciseId, mediaResource)`, `exerciseExistsByNameAndEquipment(name, equipmentTypeId)`.

#### 6b. Data Layer — ImageStorageHelper (Nuevo)

Paquete: `data.local.storage`.

- **`ImageStorageHelper`**: `@Singleton`. Inyecta `@ApplicationContext context: Context`. Centraliza la gestión de imágenes de ejercicios en almacenamiento interno. Métodos:
  - `saveImageToInternal(uri: Uri): String?` — copia la imagen del URI (galería) al directorio `filesDir/exercise_images/exercise_{UUID}.jpg`. Usa `use {}` para cerrar streams correctamente. Retorna la ruta absoluta del archivo o `null` en caso de error.
  - `deleteImageIfInternal(mediaResource: String?)` — elimina el archivo de imagen anterior si existe y está dentro del directorio `exercise_images/`. Operación best-effort (ignora errores silenciosamente). Previene acumulación de archivos huérfanos cuando el usuario cambia la imagen de un ejercicio.

#### 7. Domain Layer — Models del catálogo (Nuevo)

Paquete: `domain.model`. Kotlin puro, sin dependencias de Room.

- **`Module`**: Data class — `code: String`, `name: String`, `groupDescription: String`, `loadIncrementKg: Double`.
- **`MuscleZone`**: Data class — `id: Long`, `name: String`, `muscleGroup: String`.
- **`EquipmentType`**: Data class — `id: Long`, `name: String`.
- **`Exercise`**: Data class — `id: Long`, `name: String`, `moduleCode: String`, `moduleName: String`, `equipmentTypeName: String`, `muscleZones: List<String>`, `isBodyweight: Boolean`, `isIsometric: Boolean`, `isToTechnicalFailure: Boolean`, `isCustom: Boolean`, `mediaResource: String?`. El campo `isCustom` indica si el ejercicio fue creado por el ejecutante (RF62). El campo `mediaResource` es nullable: para ejercicios seed contiene el nombre normalizado, para ejercicios custom puede contener la ruta absoluta de una imagen de galería o `null`.

#### 8. Domain Layer — Repository Interface (Nuevo)

Paquete: `domain.repository`.

- **`ExerciseRepository`**: Interfaz Kotlin puro. 5 contratos de lectura: `getAllExercises()`, `getExerciseById(id)`, `getAllModules()`, `getAllEquipmentTypes()`, `getAllMuscleZones()`. 3 contratos de escritura: `createExercise(...)`, `updateExerciseImage(exerciseId, mediaResource)`, `exerciseExistsByNameAndEquipment(name, equipmentTypeId)`.

#### 9. Domain Layer — Use Cases (Nuevo)

Paquete: `domain.usecase.catalog`.

- **`GetExercisesUseCase`**: Invoca `ExerciseRepository.getAllExercises()`. Retorna `Flow<List<Exercise>>`. Lectura pura, sin validación.
- **`GetExerciseDetailUseCase`**: Invoca `ExerciseRepository.getExerciseById(id)`. Retorna `Flow<Exercise?>`. Lectura pura.
- **`GetFilterOptionsUseCase`**: Invoca `getAllModules()`, `getAllEquipmentTypes()`, `getAllMuscleZones()` en paralelo usando `combine()`. Retorna `Flow<FilterOptions>` con las 3 listas para los dropdowns de filtro.
- **`CreateExerciseUseCase`**: Valida nombre no vacío, al menos una zona muscular, unicidad (nombre, equipmentTypeId) vía `exerciseExistsByNameAndEquipment()`. Retorna `Long` (ID del ejercicio creado). Lanza `IllegalArgumentException` si la validación falla.
- **`UpdateExerciseImageUseCase`**: Invoca `ExerciseRepository.updateExerciseImage(exerciseId, mediaResource)`. Permite actualizar la imagen de cualquier ejercicio (seed o custom).

#### 10. UI Layer — D1 Diccionario de Ejercicios (Nuevo)

Paquete: `ui.catalog`.

- **`ExerciseDictionaryScreen`**: Composable de nivel pantalla. Estructura según Wireframes D1 y Especificación Visual §8 D1:
  - **Top Bar**: `CenterAlignedTopAppBar` sin retorno. Título "Diccionario". `TabRow` M3 (Primary Tabs) con 2 tabs: "Ejercicios" (activo) y "Plan" (navega a D3). Tab activo: texto Primary, indicador inferior Primary (3 dp). Tab inactivo: texto On Surface Variant.
  - **Filtros**: 3 `ExposedDropdownMenuBox` M3 compactos en una fila horizontal. Dropdown 1: Módulo (label "Módulo", opciones: "Todos", "A", "B", "C"). Dropdown 2: Equipo (label "Equipo", opciones: "Todos" + 9 tipos). Dropdown 3: Zona muscular (label "Zona", opciones: "Todos" + zonas). Cada dropdown con `weight(1f)`, spacing 8 dp, padding horizontal 16 dp, vertical 8 dp.
  - **Contador**: `Text` Body Small, On Surface Variant. "Mostrando N de T ejercicios" (T = total dinámico: precargados + creados). Padding top 12 dp, bottom 8 dp.
  - **Lista**: `LazyColumn` con `ListItem` M3 72 dp por ejercicio. `headlineContent`: Title Medium, On Surface (nombre). `supportingContent`: Body Medium, On Surface Variant ("Módulo A · Máquina · Pecho Medio", separados por " · "). Para `isCustom = true`: badge "Personalizado" (Label Small, On Tertiary Container, corner 4 dp). Divider 1 dp Outline Variant entre filas. Clickable → navega a D2 con `exerciseId`.
  - **FAB crear ejercicio** (CA-03.10): `FloatingActionButton` M3, ícono Add (24 dp), containerColor Primary Container, contentColor On Primary Container, posición bottom-end margin 16 dp. Al tocar → navega a D5.
  - **Estado sin resultados**: "No hay ejercicios que coincidan con los filtros seleccionados." Padding vertical 48 dp.
  - **Bottom Navigation**: Visible, Diccionario activo.
  - **Filtrado en memoria** — Los 43 ejercicios se filtran localmente en el ViewModel, sin queries adicionales a Room. CA-03.03 a CA-03.06 se cumplen filtrando por `moduleCode`, `equipmentTypeName` y `muscleZones` (si alguna zona coincide — ejercicios multi-zona).

```kotlin
@Composable
fun ExerciseDictionaryScreen(
    onNavigateToExerciseDetail: (Long) -> Unit,
    onNavigateToTrainingPlan: () -> Unit,
    onNavigateToCreateExercise: () -> Unit,
    viewModel: ExerciseDictionaryViewModel = hiltViewModel(),
)
```

- **`ExerciseDictionaryViewModel`**: `@HiltViewModel`. Inyecta `GetExercisesUseCase` y `GetFilterOptionsUseCase`. Usa `combine()` entre el Flow de ejercicios, el Flow de opciones de filtro y los filtros seleccionados (StateFlow internos). Funciones: `onModuleFilterSelected(code: String?)`, `onEquipmentFilterSelected(name: String?)`, `onMuscleZoneFilterSelected(name: String?)`.
- **`ExerciseDictionaryUiState`**: Data class — `isLoading`, `exercises: List<ExerciseItem>` (lista filtrada), `totalCount: Int`, `filterOptions: FilterOptions`, `selectedModule: String?`, `selectedEquipment: String?`, `selectedMuscleZone: String?`.
- **`ExerciseItem`**: Data class — `id: Long`, `name: String`, `moduleCode: String`, `equipmentTypeName: String`, `muscleZonesSummary: String`, `isCustom: Boolean`.
- **`FilterOptions`**: Data class — `modules: List<String>`, `equipmentTypes: List<String>`, `muscleZones: List<String>`.

#### 11. UI Layer — D2 Detalle de Ejercicio (Nuevo)

Paquete: `ui.catalog`.

- **`ExerciseDetailScreen`**: Composable de nivel pantalla. Recibe `exerciseId` como argumento de navegación. Estructura según Wireframes D2 y Especificación Visual §8 D2:
  - **Top Bar**: `CenterAlignedTopAppBar` con `←` retorno + título dinámico (nombre del ejercicio).
  - **Media visual**: `Image` (PNG) en `Box` clickable. Height 240 dp, full width, `ContentScale.Crop`, corner radius top 12 dp. **Lógica de carga:** Si `mediaResource` no es null, primero intenta como ruta de archivo absoluta (imágenes custom), si falla intenta como ruta `assets/exercises/module-{code}/{mediaResource}.png`. Si null o fallo: muestra logo de la app como placeholder con ícono `AddAPhoto` y texto "Toca para agregar imagen" (CA-03.11). **La imagen es clickable** en todos los casos: al tocar, abre el selector de galería. La imagen seleccionada se copia al almacenamiento interno y se actualiza `media_resource` vía `UpdateExerciseImageUseCase`.
  - **Información textual**: 4 campos con `overlineContent` (Label Medium, On Surface Variant) + `headlineContent` (Body Large, On Surface): "Nombre" → nombre, "Módulo" → "A — Superior", "Tipo de equipo" → valor, "Zona muscular" → zonas concatenadas.
  - **Enlace**: Text Button "Ver historial de este ejercicio →", color Primary, margin top 24 dp. Navega a F3.
  - **Bottom Navigation**: Condicional según origen. Para HU-03, D2 solo se accede desde D1 — Bottom Nav visible con Diccionario activo. Integración desde E1 (sesión activa) se implementa en HU-05/HU-06.

```kotlin
@Composable
fun ExerciseDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToExerciseHistory: (Long) -> Unit,
    viewModel: ExerciseDetailViewModel = hiltViewModel(),
)
```

- **`ExerciseDetailViewModel`**: `@HiltViewModel`. Recibe `exerciseId` via `SavedStateHandle`. Inyecta `GetExerciseDetailUseCase`, `UpdateExerciseImageUseCase`, `ImageStorageHelper`. Función `onImageSelected(uri: Uri?)`: usa `ImageStorageHelper.saveImageToInternal(uri)` para copiar imagen, elimina imagen anterior vía `deleteImageIfInternal()`, invoca `updateExerciseImageUseCase(exerciseId, savedPath)`. El Flow reactivo actualiza el UI automáticamente.
- **`ExerciseDetailUiState`**: Sealed interface — `Loading`, `Success(exercise: ExerciseDetail)`, `Error(message: String)`.
- **`ExerciseDetail`**: Data class — `id: Long`, `name: String`, `moduleCode: String`, `moduleName: String`, `equipmentTypeName: String`, `muscleZones: String` (concatenadas con ", "), `isCustom: Boolean`, `mediaResource: String?`.

#### 12. UI Layer — D5 Crear Ejercicio (Nuevo)

Paquete: `ui.catalog`.

- **`CreateExerciseScreen`**: Composable de nivel pantalla. Scaffold con `TensionTopAppBar` (retorno + título "Crear ejercicio"), Snackbar para errores, body scrollable con:
  - **Imagen** (opcional): `Box` 240 dp clickable que abre selector de galería. Si hay imagen seleccionada, la muestra con `ContentScale.Crop`. Si no, muestra `ExerciseImagePlaceholder` (composable compartido: logo de la app + ícono `AddAPhoto` + texto "Toca para agregar imagen"). La imagen se copia al almacenamiento interno vía `ImageStorageHelper`.
  - **Texto** "La imagen es opcional. Puedes agregarla después."
  - **Nombre**: `OutlinedTextField` obligatorio con validación.
  - **Módulo**: `ExposedDropdownMenuBox` con los 3 módulos (A, B, C).
  - **Tipo de equipo**: `ExposedDropdownMenuBox` con los 9 tipos de equipo.
  - **Zonas musculares**: `FlowRow` de `FilterChip` multi-select con las 15 zonas.
  - **Condiciones especiales**: 3 `Checkbox` (Peso corporal, Isométrico, Al fallo técnico).
  - **Botón Crear**: `Button` full-width, disabled hasta que todos los campos obligatorios estén completos. Muestra `CircularProgressIndicator` durante guardado.
  - Al éxito, navega automáticamente de vuelta al Diccionario (D1).

```kotlin
@Composable
fun CreateExerciseScreen(
    onNavigateBack: () -> Unit,
    viewModel: CreateExerciseViewModel = hiltViewModel(),
)
```

- **`CreateExerciseViewModel`**: `@HiltViewModel`. Inyecta `GetFilterOptionsUseCase`, `CreateExerciseUseCase`, `ImageStorageHelper`. Funciones: `onNameChanged`, `onModuleSelected`, `onEquipmentTypeSelected`, `onMuscleZoneToggled`, `onBodyweightChanged`, `onIsometricChanged`, `onToTechnicalFailureChanged`, `onImageSelected(uri: Uri?)`, `onSave`, `onDismissSaveError`.
- **`CreateExerciseUiState`**: Data class con: `isLoading`, `name`, `modules: List<Module>`, `selectedModuleCode`, `equipmentTypes: List<EquipmentType>`, `selectedEquipmentTypeId`, `muscleZones: List<MuscleZone>`, `selectedMuscleZoneIds: Set<Long>`, `isBodyweight`, `isIsometric`, `isToTechnicalFailure`, `imageUri: String?`, `isSaving`, `nameError`, `moduleError`, `equipmentError`, `muscleZoneError`, `saveSuccess`, `saveError`.

#### 13. UI Layer — D3 Stub (Nuevo)

Paquete: `ui.catalog`.

- **`TrainingPlanScreen`**: Composable stub mínimo. `CenterAlignedTopAppBar` con "Diccionario" + `TabRow` (tab "Plan" activo). Body con texto placeholder "Próximamente". La lógica completa se implementa en HU-04.

#### 14. UI Layer — F3 Stub (Nuevo)

Paquete: `ui.history`.

- **`ExerciseHistoryScreen`**: Composable stub mínimo. `TensionTopAppBar` con `←` retorno + título "Historial de Ejercicio". Body con texto placeholder. La lógica completa se implementa en HU-23.

#### 15. UI Layer — Navegación (Modificación)

- **`NavigationRoutes`**: Agregar constantes: `EXERCISE_DETAIL = "exercise-detail/{exerciseId}"`, `TRAINING_PLAN = "training-plan"`, `CREATE_EXERCISE = "create-exercise"`, `EXERCISE_HISTORY = "exercise-history/{exerciseId}"`.
- **`TensionNavHost`**: Reemplazar el `PlaceholderScreen` de `exercise-dictionary` con `ExerciseDictionaryScreen`. Agregar composables para `exercise-detail/{exerciseId}` (D2, con argumento `NavType.LongType`), `create-exercise` (D5), `training-plan` (D3 stub) y `exercise-history/{exerciseId}` (F3 stub).
- **`BottomNavigationBar`**: Agregar `childRoutes = setOf("training-plan", "create-exercise")` y `childRoutePrefixes = setOf("exercise-detail", "exercise-history", "plan-version-detail")` al ítem Diccionario.

#### 16. Assets Multimedia

- **43 archivos PNG** en `assets/exercises/`, organizados en 3 subdirectorios por módulo: `module-a/` (15 imágenes), `module-b/` (14 imágenes), `module-c/` (14 imágenes). Imágenes 3D minimalistas con fondo blanco. Ubicación en `assets/` (no `res/drawable`) porque Android no soporta subdirectorios dentro de `res/drawable/` (AAPT2 los rechaza).

**Convención de naming:** Nombre del ejercicio + tipo de equipo, normalizado: lowercase → strip acentos (á→a, é→e, í→i, ó→o, ú→u) → espacios a underscores → eliminar paréntesis y caracteres especiales. Extensión `.png`. Ejemplo: "Press de banca" + "Máquina" → `press_de_banca_maquina.png`.

**Tabla completa de 43 archivos:**

| # | Ejercicio | Equipo | `media_resource` |
|---|---|---|---|
| **module-a/** (15 archivos) | | | |
| 1 | Press de banca | Máquina | `press_de_banca_maquina` |
| 2 | Press de mancuerna | Mancuernas | `press_de_mancuerna_mancuernas` |
| 3 | Press de banca inclinada | Máquina | `press_de_banca_inclinada_maquina` |
| 4 | Flexiones | Cuerpo | `flexiones_cuerpo` |
| 5 | Cruce en polea alta | Máquina | `cruce_en_polea_alta_maquina` |
| 6 | Apertura de pecho sentado | Máquina | `apertura_de_pecho_sentado_maquina` |
| 7 | Apertura de pecho inclinado | Máquina | `apertura_de_pecho_inclinado_maquina` |
| 8 | Remo con Inclinación | Barra de Pesas | `remo_con_inclinacion_barra_de_pesas` |
| 9 | Remo con un solo brazo doblado | Mancuerna | `remo_con_un_solo_brazo_doblado_mancuerna` |
| 10 | Tiro de dorsales (Agarre ancho) | Máquina | `tiro_de_dorsales_agarre_ancho_maquina` |
| 11 | Abdominales | Cuerpo | `abdominales_cuerpo` |
| 12 | Escalador | Cuerpo | `escalador_cuerpo` |
| 13 | Giro Ruso | Cuerpo | `giro_ruso_cuerpo` |
| 14 | Plancha | Cuerpo | `plancha_cuerpo` |
| 15 | Plancha Lateral | Cuerpo | `plancha_lateral_cuerpo` |
| **module-b/** (14 archivos) | | | |
| 16 | Curl de bíceps | Mancuerna | `curl_de_biceps_mancuerna` |
| 17 | Curl de bíceps | Polea | `curl_de_biceps_polea` |
| 18 | Curl de martillo cruzado | Mancuerna | `curl_de_martillo_cruzado_mancuerna` |
| 19 | Curl de martillo | Mancuerna | `curl_de_martillo_mancuerna` |
| 20 | Curl de Contracción | Mancuerna | `curl_de_contraccion_mancuerna` |
| 21 | Dominada de tríceps banco | Pesa | `dominada_de_triceps_banco_pesa` |
| 22 | Extensión de tríceps por encima de la cabeza | Mancuerna | `extension_de_triceps_por_encima_de_la_cabeza_mancuerna` |
| 23 | Flexión de tríceps con cuerda | Máquina | `flexion_de_triceps_con_cuerda_maquina` |
| 24 | Elevación frontal | Mancuerna | `elevacion_frontal_mancuerna` |
| 25 | Elevación lateral | Mancuerna | `elevacion_lateral_mancuerna` |
| 26 | Elevación de hombros con mancuernas | Mancuerna | `elevacion_de_hombros_con_mancuernas_mancuerna` |
| 27 | Press de elevación sentado | Mancuerna | `press_de_elevacion_sentado_mancuerna` |
| 28 | Remo vertical | Barra de Pesas | `remo_vertical_barra_de_pesas` |
| 29 | Remo vertical con cable | Máquina | `remo_vertical_con_cable_maquina` |
| **module-c/** (14 archivos) | | | |
| 30 | Extensión de Cuádriceps | Máquina | `extension_de_cuadriceps_maquina` |
| 31 | Curl Femoral Tumbado | Máquina | `curl_femoral_tumbado_maquina` |
| 32 | Aductor de Cadera | Máquina | `aductor_de_cadera_maquina` |
| 33 | Abductor de Cadera | Máquina | `abductor_de_cadera_maquina` |
| 34 | Elevación de Gemelos Sentado | Máquina | `elevacion_de_gemelos_sentado_maquina` |
| 35 | Empuje de Cadera | Máquina | `empuje_de_cadera_maquina` |
| 36 | Sentadilla de Sumo | Mancuerna o Pesa Rusa | `sentadilla_de_sumo_mancuerna_o_pesa_rusa` |
| 37 | Sentadilla | Cuerpo | `sentadilla_cuerpo` |
| 38 | Sentadilla Búlgara Dividida | Mancuernas | `sentadilla_bulgara_dividida_mancuernas` |
| 39 | Sentadilla | Máquina Multiestación | `sentadilla_maquina_multiestacion` |
| 40 | Subir Escalones | Máquina | `subir_escalones_maquina` |
| 41 | Zancada hacia atrás | Mancuernas | `zancada_hacia_atras_mancuernas` |
| 42 | Avanzada de Zancadas | Mancuernas | `avanzada_de_zancadas_mancuernas` |
| 43 | Press de Pierna | Máquina | `press_de_pierna_maquina` |

**Reglas de normalización (implementar en `ExerciseSeeder`):**
1. Tomar `name` + `"_"` + `equipmentType.name` del Modelo de Datos §3.4
2. Lowercase → strip acentos (á→a, é→e, í→i, ó→o, ú→u)
3. Espacios a underscores → eliminar paréntesis y caracteres especiales
4. Resultado para DB: `media_resource = "press_de_banca_maquina"` (sin extensión)
5. Ruta en assets: `assets/exercises/module-{code}/{media_resource}.png`

#### 17. DI Layer — Módulos (Modificación + Nuevo)

- **`DatabaseModule`**: Agregar providers para los 6 nuevos DAOs y para `PrepopulateCallback`. Modificar `provideTensionDatabase()` para registrar el callback.
- **`RepositoryModule`**: Agregar binding `ExerciseRepository` ↔ `ExerciseRepositoryImpl` con `@Binds @Singleton`.

#### 18. Recursos (Modificación)

- **`strings.xml`**: Agregar strings para D1, D2, D3 stub, D5, F3 stub. **Corrección:** Cambiar `nav_dictionary` de "Ejercicios" a "Diccionario" para alinear con Especificación Visual §7.2 y Arquitectura Técnica §4.5.

---

### Integraciones

| Interfaz / Contrato | Productor | Consumidor | Descripción |
| --- | --- | --- | --- |
| `ExerciseRepository` | `ExerciseRepositoryImpl` (Data) | Use Cases de catálogo (Domain) | Contrato de acceso al catálogo de ejercicios, módulos, equipos y zonas. Definido en Domain, implementado en Data, inyectado por Hilt |
| `PrepopulateCallback` | `data.local.seed` | `TensionDatabase` builder | Callback que ejecuta seed data al crear la BD por primera vez. 220 inserciones en transacción atómica vía 3 Seeders (`ModuleSeeder`, `ExerciseSeeder`, `PlanSeeder`) alineados con ADR-11 |
| `StateFlow<ExerciseDictionaryUiState>` | `ExerciseDictionaryViewModel` | `ExerciseDictionaryScreen` | Lista de ejercicios filtrada + opciones de filtro + filtros seleccionados |
| `StateFlow<ExerciseDetailUiState>` | `ExerciseDetailViewModel` | `ExerciseDetailScreen` | Datos completos del ejercicio incluyendo media resource |
| `SavedStateHandle` | Navigation Compose | `ExerciseDetailViewModel` | `exerciseId: Long` extraído de la ruta `exercise-detail/{exerciseId}` |

---

### Riesgos

| Riesgo | Probabilidad | Impacto | Mitigación |
| --- | --- | --- | --- |
| Tamaño del APK excede 150 MB por assets PNG de 43 ejercicios | Baja | Medio | Imágenes PNG estáticas son significativamente más livianas que GIFs. Medir APK actual. Optimizar: resolución ≤ 720px, compresión PNG. Si excede, considerar WebP estático (20-30% más liviano) |
| Seed data con 220 inserciones falla parcialmente | Baja | Alto | Transacción atómica en `onCreate()` — si falla, se revierte todo. `fallbackToDestructiveMigration()` permite recrear la BD limpia |
| Filtrado en memoria con 43 ejercicios causa frame drops | Muy baja | Bajo | 43 elementos es trivial. El filtrado se hace en el ViewModel, no en recomposición |
| Versión de BD necesita migración de 1 a 2 al agregar entities | Baja | Medio | `fallbackToDestructiveMigration()` activo durante desarrollo. Datos de perfil/peso se pierden al cambiar versión, aceptable en fase de desarrollo |
| Bottom Navigation no marca Diccionario como activo en D2/D4 (rutas con argumentos) | Certeza | Bajo | Extender lógica de `selected` en `BottomNavigationBar` para evaluar prefijo de ruta |
| Label Bottom Nav "Ejercicios" confunde con tab "Ejercicios" de D1 | Certeza | Medio | Corregir `nav_dictionary` de "Ejercicios" a "Diccionario" en `strings.xml` |

---

### Hitos de implementación

| Hito | Contenido | Dependencias |
| --- | --- | --- |
| 1 | Data Layer — Entities: 7 nuevas entidades | — |
| 2 | Data Layer — DAOs: `ExerciseDao`, `ModuleDao`, `EquipmentTypeDao`, `MuscleZoneDao`, `ModuleVersionDao`, `PlanAssignmentDao` | Hito 1 |
| 3 | Data Layer — Seed: `PrepopulateCallback`, `PrepopulateFacade`, 3 Seeders — 220 filas totales | Hito 1, Hito 2 |
| 4 | Data Layer — Database: actualizar `TensionDatabase` (7 entities, 6 DAOs, callback). DI: actualizar `DatabaseModule` y `RepositoryModule` | Hito 1, Hito 2, Hito 3 |
| 5 | Domain Layer: modelos, `ExerciseRepository` interfaz, 5 Use Cases | — (Kotlin puro) |
| 6 | Data Layer — Repository: `ExerciseRepositoryImpl` | Hito 2, Hito 5 |
| 7 | Assets: verificar 43 PNGs existentes en `assets/exercises/`, naming y optimización | — (independiente) |
| 8 | UI — D1: `ExerciseDictionaryScreen`, `ExerciseDictionaryViewModel`, `ExerciseDictionaryUiState` + D3 stub + D5: `CreateExerciseScreen`, `CreateExerciseViewModel`, `CreateExerciseUiState` | Hito 5, Hito 6 |
| 9 | UI — D2: `ExerciseDetailScreen`, `ExerciseDetailViewModel`, `ExerciseDetailUiState` + F3 stub | Hito 5, Hito 6, Hito 7 |
| 10 | Navegación: actualizar `NavigationRoutes`, `TensionNavHost`, `BottomNavigationBar` + `strings.xml` | Hito 8, Hito 9 |

---

### Notas de auditoría

1. **CA-03.01 (precargado completo) se cumple mediante ADR-11.** `PrepopulateCallback.onCreate()` inserta 220 filas en transacción atómica usando el patrón Facade con 3 Seeders. Los 43 ejercicios están disponibles desde la primera apertura de la app, sin conexión a internet.
2. **Seeders alineados con ADR-11.** ADR-11 nombra 4 seeders: `ModuleSeeder`, `ExerciseSeeder`, `PlanSeeder`, `RotationSeeder`. El `RotationSeeder` no se incluye en el `PrepopulateFacade` porque el estado de rotación se inicializa en `ProfileRepositoryImpl.createProfile()` (HU-01, Modelo de Datos §3.14).
3. **CA-03.02 (información visible por ejercicio) requiere JOIN multi-tabla.** El `ExerciseDao.getAll()` resuelve nombre de módulo, nombre de equipo y lista de zonas musculares en una sola consulta usando `ExerciseWithDetails` como clase intermedia.
4. **CA-03.03 a CA-03.06 (filtros) se implementan en memoria.** Los 43 ejercicios se cargan completos y el ViewModel filtra reactivamente. El filtro de zona muscular verifica si ALGUNA zona del ejercicio coincide (ejercicios multi-zona como Sentadilla Búlgara tienen 2 zonas: Cuádriceps + Glúteos).
5. **CA-03.07 (media visual) depende de `media_resource` en la entity.** El campo almacena: para ejercicios seed, el nombre normalizado (ej: `"press_de_banca_maquina"`); para ejercicios custom, la ruta absoluta del archivo en almacenamiento interno. El composable usa doble estrategia: primero intenta cargar como ruta de archivo, si no existe construye la ruta de asset. Si `mediaResource` es null, se muestra `ExerciseImagePlaceholder`.
6. **CA-03.08 (media accesible durante sesión activa) es responsabilidad de E1 (HU-05/HU-06).** D2 es una vista reutilizable accesible desde E1. Esta historia prepara D2 como reutilizable; la integración desde E1 se completa en su historia correspondiente.
7. **CA-03.09 (APK ≤ 150 MB) es un riesgo bajo con PNG.** 43 imágenes PNG estáticas son significativamente más livianas que GIFs. Cumple RNF24 y RNF31 (seed data versionado en `assets/exercises/`).
8. **Tabs D1 ↔ D3 comparten TopAppBar.** Ambas pantallas usan `CenterAlignedTopAppBar` con título "Diccionario" + `TabRow`. La navegación lateral entre tabs usa `launchSingleTop = true` y `restoreState = true` sin apilamiento de back stack.
9. **Las 7 entities de seed data se incluyen todas en HU-03** para garantizar atomicidad del seed (ADR-11: una única transacción en `onCreate()`) y evitar migraciones intermedias.
10. **`BottomNavigationBar` requiere extensión para rutas con argumentos.** Las rutas `exercise-detail/{exerciseId}` no coinciden con `currentRoute == item.route`. Se necesita evaluación por prefijo.
11. **Corrección de label Bottom Nav: "Ejercicios" → "Diccionario".** El string actual `nav_dictionary = "Ejercicios"` contradice Especificación Visual §7.2 y Arquitectura Técnica §4.5, que definen el label como "Diccionario".
12. **Tipos de equipo corregidos a 9.** La Especificación Visual §8 D1 originalmente decía "Todos + 10 tipos de equipo". Corregido a "Todos + 9 tipos de equipo" alineado con Modelo de Datos §3.3.
13. **CA-03.10 (crear ejercicio) y CA-03.11 (imagen/placeholder) están IMPLEMENTADOS.** Componentes implementados: `CreateExerciseUseCase`, `UpdateExerciseImageUseCase`, `ExerciseRepository.createExercise()`, `ExerciseRepository.updateExerciseImage()`, `ExerciseDao.insertExerciseWithMuscleZones()` (`@Transaction` atómico), `ImageStorageHelper` (singleton para copia y eliminación de imágenes), pantalla D5 `CreateExerciseScreen` completa, `ExerciseImagePlaceholder` (composable compartido en `ui.components`).
14. **Seeders usan tildes del Modelo de Datos §3.4.** Los nombres de ejercicios en el seed se toman del Modelo de Datos con acentos normalizados: "Extensión de Cuádriceps", "Sentadilla Búlgara Dividida".
15. **Assets multimedia ya existen en disco.** Los 43 archivos PNG están en `app/src/main/assets/exercises/`, nombrados según la convención. No es necesario crear archivos durante el desarrollo de HU-03 — solo verificar naming y optimización.
