## Refinamiento Técnico (Developer)
**Autor**: Esteban Colorado González | **Fecha**: 2026-02-13

---

### Consideraciones Generales

**Basado en análisis arquitectónico:**
Análisis Arquitectónico de HU-03 con 10 hitos, 17 componentes, 5 integraciones y 6 riesgos identificados. Patrón MVVM con capa Domain explícita (ADR-05). Tercera historia — mayor volumen de entidades nuevas (7 tablas) y seed data (220 filas). Infraestructura base ya existe gracias a HU-01/HU-02.

**Nivel de complejidad:**
ALTA — Introduce 7 entidades Room nuevas, 6 DAOs nuevos, sistema de prepopulación con 220 filas (ADR-11), 4 modelos de dominio, 1 repositorio nuevo, 5 Use Cases, 5 pantallas (D1, D2, D3 stub, D5, F3 stub), 2 ViewModels, navegación con argumentos tipados, sistema de tabs compartido, sistema de filtrado en memoria, carga de imágenes PNG desde `assets/exercises/`, y extensión del `BottomNavigationBar` para rutas con argumentos.

**Riesgos técnicos conocidos:**
1. Tamaño del APK puede exceder 150 MB con 43 PNGs (ya existentes en `assets/exercises/`) — medir APK actual, optimizar con WebP estático si necesario.
2. Seed data con 220 inserciones en transacción atómica — `fallbackToDestructiveMigration()` en desarrollo.
3. Bottom Navigation no marca Diccionario como activo en D2 (ruta con argumentos) — extender lógica con evaluación por prefijo.
4. Label Bottom Nav "Ejercicios" debería ser "Diccionario" — corregir `nav_dictionary` en strings.xml.

**Patrones y convenciones del equipo (establecidos en HU-01/HU-02):**
- Código fuente en inglés, UI y datos de dominio en español (Arquitectura Técnica §5.1)
- Naming: `{Feature}Screen`, `{Feature}ViewModel`, `{Acción}{Entidad}UseCase`, `{Entidad}Entity`, `{Entidad}Dao`, `{Entidad}Seeder` (§5.2)
- Estructura Composable: `hiltViewModel()` + `collectAsStateWithLifecycle()` + `LaunchedEffect` para eventos (§5.3)
- Estructura ViewModel: `_uiState MutableStateFlow` / `uiState StateFlow` (§5.4)
- `operator fun invoke()` en Use Cases
- Callbacks en Composables con prefijo `on` (`onNavigateBack`)
- `childRoutes` en `BottomNavItem` para rutas hijas (patrón establecido en HU-02)

**Dependencias nuevas a instalar:**
Ninguna. Las imágenes PNG se cargan desde `assets/` via `context.assets.open()` + `BitmapFactory.decodeStream()` + `asImageBitmap()` — APIs nativas de Android. Se creará una función helper `@Composable rememberAssetBitmapPainter(path: String)` para encapsular la carga.

**Estrategia de testing:**
JUnit 4 (ADR-18) + MockK + kotlinx-coroutines-test | Tests unitarios para los 3 Use Cases (domain): `GetExercisesUseCase`, `GetExerciseDetailUseCase`, `GetFilterOptionsUseCase` | Cobertura: 100% Use Cases

---

### Historias Relacionadas Consultadas

**Implementaciones similares analizadas:**
HU-02 — Se reutiliza patrón `BottomNavItem.childRoutes` para marcar tab activo en rutas hijas. `WeightHistoryViewModel` sirve como referencia para `ExerciseDictionaryViewModel`.

**Patrones de código reutilizados:**
- `TensionTopAppBar` con variante sin retorno (D1/D3) y con retorno (D2/F3)
- `BottomNavigationBar` con `childRoutes` (HU-02) — se extiende para soportar rutas con argumentos via prefijo
- `PlaceholderScreen` pattern del NavHost para stubs (D3, F3)

**HUs futuras que dependen del seed data de HU-03:**
- HU-04: Consultar Plan de Entrenamiento → consume `module_version`, `plan_assignment`, D3/D4
- HU-05: Iniciar sesión de entrenamiento → consume `module_version`, `exercise`, `plan_assignment`
- HU-06: Registrar series → consume `exercise`
- HU-07: Sustitución de ejercicios → consume `exercise` filtrado por `module_code`
- HU-08: Ejercicios peso corporal/isométricos → consume flags `is_bodyweight`, `is_isometric`, `is_to_technical_failure`
- HU-20: Tonelaje por grupo muscular → consume `exercise_muscle_zone` + `muscle_zone.muscle_group`
- HU-23: Historial de ejercicio → consume `exercise` para F3
- HU-19: Backup/Restore → incluyen todas las tablas del catálogo

---

### Código existente verificado (HU-01 + HU-02 implementados)

| Componente | Archivo | Estado |
| --- | --- | --- |
| `TensionDatabase` | `data/local/database/TensionDatabase.kt` | Existe — versión 1, 3 entities, 3 DAOs. Se modifica: +7 entities, +6 DAOs, +callback, versión → 2 |
| `DatabaseModule` | `di/DatabaseModule.kt` | Existe — provee DB + 3 DAOs. Se modifica: +6 DAOs, +PrepopulateCallback |
| `RepositoryModule` | `di/RepositoryModule.kt` | Existe — binds ProfileRepository. Se modifica: +ExerciseRepository |
| `NavigationRoutes` | `ui/navigation/NavigationRoutes.kt` | Existe — 8 rutas. Se modifica: +EXERCISE_DETAIL, +TRAINING_PLAN, +EXERCISE_HISTORY, +CREATE_EXERCISE |
| `TensionNavHost` | `ui/navigation/TensionNavHost.kt` | Existe — placeholder en EXERCISE_DICTIONARY. Se modifica: reemplazar placeholder + agregar D2, D3, D5, F3 |
| `BottomNavigationBar` | `ui/components/BottomNavigationBar.kt` | Existe — `childRoutes` en Settings. Se modifica: +childRoutes en Diccionario + lógica prefijo para rutas con args |
| `TensionTopAppBar` | `ui/components/TensionTopAppBar.kt` | Existe — variantes retorno y cierre. No se modifica |
| `strings.xml` | `res/values/strings.xml` | Existe — `nav_dictionary = "Ejercicios"`. Se modifica: corregir a "Diccionario" + agregar strings D1/D2/D3/D5/F3 |
| `assets/exercises/` | `app/src/main/assets/exercises/` | Existe — 3 subdirectorios (`module-a/`, `module-b/`, `module-c/`) con 43 PNGs ya presentes. Verificar naming y optimización |

---

### Tareas de Implementación

#### Fase 1: Data Layer — Entities (7 nuevas)

> Basado en Hito #1 del Análisis Arquitectónico

##### Entities del catálogo

- [ ] **Crear ModuleEntity** (AC: 03.01)
  - [ ] `@Entity(tableName = "module")`. PK natural `code: String` (TEXT). Columnas: `name` (TEXT, NOT NULL), `groupDescription` (TEXT, NOT NULL, columnInfo name = "group_description"), `loadIncrementKg` (REAL, NOT NULL, columnInfo name = "load_increment_kg"). 3 filas inmutables — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/entity/ModuleEntity.kt`

- [ ] **Crear MuscleZoneEntity** (AC: 03.01, 03.05)
  - [ ] `@Entity(tableName = "muscle_zone")`. PK autoincrement `id: Long`. Columnas: `name` (TEXT, NOT NULL, UNIQUE), `muscleGroup` (TEXT, NOT NULL, columnInfo name = "muscle_group"). Index en `muscle_group`. 15 filas inmutables — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/entity/MuscleZoneEntity.kt`

- [ ] **Crear EquipmentTypeEntity** (AC: 03.01, 03.04)
  - [ ] `@Entity(tableName = "equipment_type")`. PK autoincrement `id: Long`. Columnas: `name` (TEXT, NOT NULL, UNIQUE). 9 filas inmutables — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/entity/EquipmentTypeEntity.kt`

- [ ] **Crear ExerciseEntity** (AC: 03.01, 03.02, 03.10)
  - [ ] `@Entity(tableName = "exercise")`. PK autoincrement `id: Long`. FKs: `moduleCode` → `module(code)` ON DELETE RESTRICT, `equipmentTypeId` → `equipment_type(id)` ON DELETE RESTRICT. Columnas: `name`, `moduleCode` (column = "module_code"), `equipmentTypeId` (column = "equipment_type_id"), `isBodyweight` (column = "is_bodyweight", default 0), `isIsometric` (column = "is_isometric", default 0), `isToTechnicalFailure` (column = "is_to_technical_failure", default 0), `isCustom` (column = "is_custom", default 0), `mediaResource` (column = "media_resource", nullable). UNIQUE(name, equipmentTypeId). Indices en `module_code`, `equipment_type_id`. 43 filas seed + filas dinámicas (RF62) — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/entity/ExerciseEntity.kt`

- [ ] **Crear ExerciseMuscleZoneEntity** (AC: 03.01, 03.05)
  - [ ] `@Entity(tableName = "exercise_muscle_zone", primaryKeys = ["exercise_id", "muscle_zone_id"])`. FKs: `exerciseId` → `exercise(id)`, `muscleZoneId` → `muscle_zone(id)`, ambas ON DELETE RESTRICT. 48 filas (38 × 1 zona + 5 × 2 zonas) — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/entity/ExerciseMuscleZoneEntity.kt`

##### Entities auxiliares del plan

- [ ] **Crear ModuleVersionEntity** (AC: 03.01 indirect — seed atomicidad)
  - [ ] `@Entity(tableName = "module_version")`. PK autoincrement `id: Long`. FK `moduleCode` → `module(code)` ON DELETE RESTRICT. Columnas: `moduleCode` (column = "module_code"), `versionNumber` (column = "version_number", INTEGER NOT NULL). UNIQUE(module_code, version_number). 9 filas — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/entity/ModuleVersionEntity.kt`

- [ ] **Crear PlanAssignmentEntity** (AC: 03.01 indirect — seed atomicidad)
  - [ ] `@Entity(tableName = "plan_assignment", primaryKeys = ["module_version_id", "exercise_id"])`. FKs: `moduleVersionId` → `module_version(id)`, `exerciseId` → `exercise(id)`, ambas ON DELETE RESTRICT. Columnas: `moduleVersionId` (column = "module_version_id"), `exerciseId` (column = "exercise_id"), `sets` (INTEGER NOT NULL), `reps` (TEXT NOT NULL). 93 filas — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/entity/PlanAssignmentEntity.kt`

#### Fase 2: Data Layer — DAOs (6 nuevos)

> Basado en Hito #2 del Análisis Arquitectónico

- [ ] **Crear ExerciseDao** (AC: 03.02, 03.07)
  - [ ] `@Dao`. Queries: `getAll(): Flow<List<ExerciseWithDetails>>` — query con JOIN a `module`, `equipment_type` y GROUP_CONCAT para zonas musculares vía `exercise_muscle_zone` + `muscle_zone`. `getById(exerciseId: Long): Flow<ExerciseWithDetails?>` para D2. Inserts: `insertAll(exercises: List<ExerciseEntity>)`, `insertAllMuscleZones(zones: List<ExerciseMuscleZoneEntity>)`, `insert(exercise: ExerciseEntity): Long`, `insertExerciseWithMuscleZones(exercise, muscleZones): Long` (`@Transaction`), `updateMediaResource(exerciseId, mediaResource)`, `countByNameAndEquipment(name, equipmentTypeId): Int`. Definir `ExerciseWithDetails` como data class intermedia (no @Entity) con campos: `id`, `name`, `moduleCode`, `moduleName`, `equipmentTypeName`, `isBodyweight`, `isIsometric`, `isToTechnicalFailure`, `isCustom`, `mediaResource` (nullable), `muscleZones` (String con GROUP_CONCAT) — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/dao/ExerciseDao.kt`

- [ ] **Crear ModuleDao** (AC: 03.03)
  - [ ] `@Dao`. Queries: `getAll(): Flow<List<ModuleEntity>>`. Insert: `insertAll(modules: List<ModuleEntity>)` para seed — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/dao/ModuleDao.kt`

- [ ] **Crear EquipmentTypeDao** (AC: 03.04)
  - [ ] `@Dao`. Query: `getAll(): Flow<List<EquipmentTypeEntity>>`. Insert: `insertAll(types: List<EquipmentTypeEntity>)` para seed — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/dao/EquipmentTypeDao.kt`

- [ ] **Crear MuscleZoneDao** (AC: 03.05)
  - [ ] `@Dao`. Query: `getAll(): Flow<List<MuscleZoneEntity>>`. Insert: `insertAll(zones: List<MuscleZoneEntity>)` para seed — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/dao/MuscleZoneDao.kt`

- [ ] **Crear ModuleVersionDao** (provisorio para HU-04+)
  - [ ] `@Dao`. Query: `getAll(): Flow<List<ModuleVersionEntity>>`. Insert: `insertAll(versions: List<ModuleVersionEntity>)` para seed — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/dao/ModuleVersionDao.kt`

- [ ] **Crear PlanAssignmentDao** (provisorio para HU-04+)
  - [ ] `@Dao`. Query: `getByModuleVersionId(id: Long): Flow<List<PlanAssignmentEntity>>`. Insert: `insertAll(assignments: List<PlanAssignmentEntity>)` para seed. HU-04 extenderá este DAO con query JOIN — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/dao/PlanAssignmentDao.kt`

#### Fase 3: Data Layer — Seed Data (ADR-11)

> Basado en Hito #3 del Análisis Arquitectónico

- [ ] **Crear ModuleSeeder** (AC: 03.01)
  - [ ] Inserta en orden: (a) 3 filas en `module` (Modelo de Datos §3.1 seed data), (b) 15 filas en `muscle_zone` (§3.2 seed data), (c) 9 filas en `equipment_type` (§3.3 seed data). Todos los datos literales en español con acentos — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/seed/ModuleSeeder.kt`

- [ ] **Crear ExerciseSeeder** (AC: 03.01, 03.02)
  - [ ] Inserta: (a) 43 filas en `exercise` (Modelo de Datos §3.4 seed data) con `media_resource` = nombre normalizado del ejercicio + equipo (ver tabla completa en §16). Implementar función de normalización: lowercase → strip acentos → espacios a underscores → eliminar paréntesis. Ejercicios bodyweight: Flexiones (`is_bodyweight=1`, `is_to_technical_failure=1`), Abdominales/Escalador/Giro Ruso/Sentadilla Cuerpo (`is_bodyweight=1`), Plancha/Plancha Lateral (`is_bodyweight=1`, `is_isometric=1`); (b) 48 filas en `exercise_muscle_zone` (§3.5). Los 5 ejercicios multi-zona del Módulo C generan 2 filas cada uno. Nombres con acentos normalizados del Modelo de Datos: "Curl de bíceps", "Dominada de tríceps banco", "Elevación frontal", "Sentadilla Búlgara Dividida", "Zancada hacia atrás" — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/seed/ExerciseSeeder.kt`

- [ ] **Crear PlanSeeder** (AC: 03.01 indirect — atomicidad ADR-11)
  - [ ] Inserta: (a) 9 filas en `module_version` (§3.6), (b) 93 filas en `plan_assignment` (Plan de Entrenamiento completo). Mapea `reps`: `"8-12"` estándar, `"TO_TECHNICAL_FAILURE"` para Flexiones, `"30-45_SEC"` para Plancha/Plancha Lateral. `sets` siempre = 4 — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/seed/PlanSeeder.kt`

- [ ] **Crear PrepopulateFacade** (AC: 03.01)
  - [ ] Orquesta invocación secuencial: (1) `ModuleSeeder`, (2) `ExerciseSeeder`, (3) `PlanSeeder`. Respeta dependencias FK. No incluye `RotationSeeder` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/seed/PrepopulateFacade.kt`

- [ ] **Crear PrepopulateCallback** (AC: 03.01)
  - [ ] Implementa `RoomDatabase.Callback`. En `onCreate(db)` ejecuta `PrepopulateFacade.populate()` dentro de una transacción atómica. Si falla, se revierte todo — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/seed/PrepopulateCallback.kt`

#### Fase 4: Data Layer — Database y DI

> Basado en Hito #4 del Análisis Arquitectónico

- [ ] **Actualizar TensionDatabase** (AC: 03.01)
  - [ ] Agregar 7 entities al array. Incrementar `version = 2`. Exponer 6 DAOs abstractos. Se mantiene `fallbackToDestructiveMigration()` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/database/TensionDatabase.kt`

- [ ] **Actualizar DatabaseModule** (AC: 03.01)
  - [ ] Agregar `@Provides` para los 6 nuevos DAOs. Agregar `@Provides @Singleton` para `PrepopulateCallback`. Modificar `provideTensionDatabase()` para registrar el callback — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/di/DatabaseModule.kt`

- [ ] **Actualizar RepositoryModule** (AC: 03.02)
  - [ ] Agregar `@Binds @Singleton abstract fun bindExerciseRepository(impl: ExerciseRepositoryImpl): ExerciseRepository` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/di/RepositoryModule.kt`

#### Fase 5: Domain Layer — Models, Repo Interface, Use Cases

> Basado en Hito #5 del Análisis Arquitectónico

##### Models

- [ ] **Crear Module** (AC: 03.03) — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/model/Module.kt`
- [ ] **Crear MuscleZone** (AC: 03.05) — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/model/MuscleZone.kt`
- [ ] **Crear EquipmentType** (AC: 03.04) — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/model/EquipmentType.kt`
- [ ] **Crear Exercise** (AC: 03.02, 03.07) — Data class Kotlin puro con `mediaResource: String?` nullable — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/model/Exercise.kt`

##### Repository Interface

- [ ] **Crear ExerciseRepository** (AC: 03.02, 03.03, 03.04, 03.05, 03.07, 03.10, 03.11)
  - [ ] Interfaz Kotlin puro con 5 contratos de lectura + 3 de escritura: `getAllExercises()`, `getExerciseById(id)`, `getAllModules()`, `getAllEquipmentTypes()`, `getAllMuscleZones()`, `suspend fun createExercise(...)`, `suspend fun updateExerciseImage(...)`, `suspend fun exerciseExistsByNameAndEquipment(...)` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/repository/ExerciseRepository.kt`

##### Use Cases

- [ ] **Crear GetExercisesUseCase** (AC: 03.02-03.06)
  - [ ] Delega a `exerciseRepository.getAllExercises()`. Lectura pura — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/usecase/catalog/GetExercisesUseCase.kt`
  - [ ] Test unitario: delegación al repositorio, lista completa, lista vacía — Archivo: `app/src/test/java/com/estebancoloradogonzalez/tension/domain/usecase/catalog/GetExercisesUseCaseTest.kt`

- [ ] **Crear GetExerciseDetailUseCase** (AC: 03.07)
  - [ ] Delega a `exerciseRepository.getExerciseById(exerciseId)`. Lectura pura — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/usecase/catalog/GetExerciseDetailUseCase.kt`
  - [ ] Test unitario: ejercicio encontrado, null — Archivo: `app/src/test/java/com/estebancoloradogonzalez/tension/domain/usecase/catalog/GetExerciseDetailUseCaseTest.kt`

- [ ] **Crear GetFilterOptionsUseCase** (AC: 03.03, 03.04, 03.05)
  - [ ] Usa `combine()` de `getAllModules()`, `getAllEquipmentTypes()`, `getAllMuscleZones()` para producir `Flow<FilterOptions>`. `FilterOptions` data class en `domain.model` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/usecase/catalog/GetFilterOptionsUseCase.kt`
  - [ ] Test unitario: verifica combine de los 3 Flows — Archivo: `app/src/test/java/com/estebancoloradogonzalez/tension/domain/usecase/catalog/GetFilterOptionsUseCaseTest.kt`

- [ ] **Crear CreateExerciseUseCase** (AC: 03.10)
  - [ ] Valida nombre no vacío, al menos una zona muscular, unicidad (nombre, equipmentTypeId) — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/usecase/catalog/CreateExerciseUseCase.kt`

- [ ] **Crear UpdateExerciseImageUseCase** (AC: 03.11)
  - [ ] Invoca `ExerciseRepository.updateExerciseImage(exerciseId, mediaResource)` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/usecase/catalog/UpdateExerciseImageUseCase.kt`

#### Fase 6: Data Layer — Repository Implementation

> Basado en Hito #6 del Análisis Arquitectónico

- [ ] **Crear ExerciseRepositoryImpl** (AC: 03.02-03.07)
  - [ ] `@Inject constructor`. Inyecta `ExerciseDao`, `ModuleDao`, `EquipmentTypeDao`, `MuscleZoneDao`. Implementa `ExerciseRepository`. Mapea `ExerciseWithDetails` → `Exercise`. Para `muscleZones`: si DAO retorna GROUP_CONCAT, split por ", " — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/repository/ExerciseRepositoryImpl.kt`

- [ ] **Crear ImageStorageHelper** (AC: 03.10, 03.11)
  - [ ] `@Singleton`. Inyecta `@ApplicationContext context: Context`. Métodos: `saveImageToInternal(uri: Uri): String?` (copia imagen a `filesDir/exercise_images/exercise_{UUID}.jpg`), `deleteImageIfInternal(mediaResource: String?)` (limpieza best-effort) — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/storage/ImageStorageHelper.kt`

#### Fase 7: Assets Multimedia (43 PNGs)

> Basado en Hito #7 del Análisis Arquitectónico

- [ ] **Verificar assets existentes en assets/exercises/** (AC: 03.07, 03.09)
  - [ ] Los 43 archivos PNG ya existen en disco en `app/src/main/assets/exercises/`. **No es necesario crear archivos.** Verificar: (a) que los 43 archivos coincidan exacto con la tabla de §16 (nombres y ubicación por módulo), (b) que la resolución sea ≤ 720px de ancho, (c) que la compresión PNG esté optimizada para móvil. Medir tamaño del APK total — debe ser ≤ 150 MB (RNF24, CA-03.09). Si supera ~100 MB, considerar conversión a WebP estático — Directorio: `app/src/main/assets/exercises/`

#### Fase 8: UI Layer — D1 Diccionario de Ejercicios + D3 Stub + D5 Crear Ejercicio

> Basado en Hito #8 del Análisis Arquitectónico

##### UiState D1

- [ ] **Crear ExerciseDictionaryUiState, ExerciseItem** (AC: 03.02-03.06)
  - [ ] `ExerciseDictionaryUiState`: data class con `isLoading`, `exercises: List<ExerciseItem>`, `totalCount: Int`, `moduleOptions: List<String>`, `equipmentOptions: List<String>`, `muscleZoneOptions: List<String>`, `selectedModule: String?`, `selectedEquipment: String?`, `selectedMuscleZone: String?`. `ExerciseItem`: data class con `id: Long`, `name: String`, `moduleCode: String`, `equipmentTypeName: String`, `muscleZonesSummary: String`, `isCustom: Boolean` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/catalog/ExerciseDictionaryUiState.kt`

##### ViewModel D1

- [ ] **Crear ExerciseDictionaryViewModel** (AC: 03.02-03.06)
  - [ ] `@HiltViewModel`. Inyecta `GetExercisesUseCase`, `GetFilterOptionsUseCase`. En `init`, usa `combine()` entre el Flow de ejercicios, Flow de opciones de filtro y 3 `MutableStateFlow<String?>` internos para los filtros seleccionados. Filtrado en memoria: `moduleCode == selectedModule` (si no null), `equipmentTypeName == selectedEquipment`, `muscleZones.any { it == selectedMuscleZone }`. CA-03.06: los 3 filtros son AND. Funciones: `onModuleFilterSelected(code: String?)`, `onEquipmentFilterSelected(name: String?)`, `onMuscleZoneFilterSelected(name: String?)` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/catalog/ExerciseDictionaryViewModel.kt`

##### Screen D1

- [ ] **Crear ExerciseDictionaryScreen** (AC: 03.01-03.06)
  - [ ] Composable de nivel pantalla con Scaffold, TabRow (Ejercicios activo / Plan → D3), 3 dropdowns de filtro en fila, contador "Mostrando N de T ejercicios", LazyColumn con ListItem 72dp (nombre + supporting + badge "Personalizado" para isCustom), FAB Add → D5, estado sin resultados. Sin Bottom Nav dentro del Screen (ya en NavHost) — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/catalog/ExerciseDictionaryScreen.kt`

##### D3 Stub

- [ ] **Crear TrainingPlanScreen stub** (para navegación tabs D1 ↔ D3)
  - [ ] Composable stub con misma TopBar estructura que D1 pero "Plan" tab activo. Body: placeholder text "Próximamente" — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/catalog/TrainingPlanScreen.kt`

##### D5 — Crear Ejercicio

- [ ] **Crear CreateExerciseUiState** (AC: 03.10)
  - [ ] Data class con todos los campos del formulario, errores de validación, `imageUri: String?`, `canSave` computed — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/catalog/CreateExerciseUiState.kt`

- [ ] **Crear CreateExerciseViewModel** (AC: 03.10, 03.11)
  - [ ] `@HiltViewModel`. Inyecta `GetFilterOptionsUseCase`, `CreateExerciseUseCase`, `ImageStorageHelper`. Carga opciones via `getFilterOptionsUseCase().first()`. Gestiona imagen con `ImageStorageHelper` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/catalog/CreateExerciseViewModel.kt`

- [ ] **Crear CreateExerciseScreen** (AC: 03.10, 03.11)
  - [ ] Composable con formulario completo: imagen opcional (galería, placeholder), nombre, módulo dropdown, equipo dropdown, zonas musculares FlowRow de FilterChip, condiciones especiales (3 checkboxes), botón Crear. Al éxito → `onNavigateBack()` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/catalog/CreateExerciseScreen.kt`

#### Fase 9: UI Layer — D2 Detalle de Ejercicio + F3 Stub

> Basado en Hito #9 del Análisis Arquitectónico

##### UiState D2

- [ ] **Crear ExerciseDetailUiState** (AC: 03.07, 03.11)
  - [ ] Sealed interface: `Loading`, `Success(exercise: ExerciseDetailItem)`, `Error(message: String)`. `ExerciseDetailItem`: data class con `id`, `name`, `moduleCode`, `moduleName`, `equipmentTypeName`, `muscleZones: String`, `isCustom`, `mediaResource: String?` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/catalog/ExerciseDetailUiState.kt`

##### ViewModel D2

- [ ] **Crear ExerciseDetailViewModel** (AC: 03.07, 03.11)
  - [ ] `@HiltViewModel`. Recibe `exerciseId` via `SavedStateHandle`. Inyecta `GetExerciseDetailUseCase`, `UpdateExerciseImageUseCase`, `ImageStorageHelper`. Función `onImageSelected(uri: Uri?)`: usa `ImageStorageHelper.saveImageToInternal()`, elimina imagen anterior vía `deleteImageIfInternal()`, invoca `updateExerciseImageUseCase()` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/catalog/ExerciseDetailViewModel.kt`

##### Screen D2

- [ ] **Crear ExerciseDetailScreen** (AC: 03.07, 03.08, 03.11)
  - [ ] Composable. Crea `imagePickerLauncher` via `rememberLauncherForActivityResult(GetContent) { viewModel.onImageSelected(it) }`. TopBar con retorno + título dinámico. Box clickable 240dp con lógica de carga doble (ruta absoluta → asset → placeholder logo+AddAPhoto). 4 campos informativos. Enlace "Ver historial de este ejercicio →" → F3 — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/catalog/ExerciseDetailScreen.kt`

##### F3 Stub

- [ ] **Crear ExerciseHistoryScreen stub** (AC: 03.08 — preparar ruta para HU-23)
  - [ ] Composable stub con TensionTopAppBar retorno + título "Historial de Ejercicio". Body placeholder "Próximamente" — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/history/ExerciseHistoryScreen.kt`

#### Fase 10: Navigation + BottomNavigationBar + Strings

> Basado en Hito #10 del Análisis Arquitectónico

##### NavigationRoutes

- [ ] **Agregar rutas nuevas** (AC: 03.02, 03.07)
  - [ ] Agregar constantes: `EXERCISE_DETAIL = "exercise-detail/{exerciseId}"`, `TRAINING_PLAN = "training-plan"`, `EXERCISE_HISTORY = "exercise-history/{exerciseId}"`, `CREATE_EXERCISE = "create-exercise"`. Helper functions: `fun exerciseDetailRoute(id: Long) = "exercise-detail/$id"`, `fun exerciseHistoryRoute(id: Long) = "exercise-history/$id"` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/navigation/NavigationRoutes.kt`

##### TensionNavHost

- [ ] **Actualizar NavHost** (AC: 03.02, 03.07, 03.10)
  - [ ] Reemplazar `PlaceholderScreen` de `EXERCISE_DICTIONARY` con `ExerciseDictionaryScreen`. Agregar composable para `EXERCISE_DETAIL` con argumento `NavType.LongType`, `CREATE_EXERCISE`, `TRAINING_PLAN`, `EXERCISE_HISTORY` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/navigation/TensionNavHost.kt`

##### BottomNavigationBar

- [ ] **Extender lógica de selección para rutas con argumentos** (AC: 03.02, 03.10)
  - [ ] Para el ítem Diccionario: `childRoutes = setOf("training-plan", "create-exercise")` + `childRoutePrefixes = setOf("exercise-detail", "exercise-history", "plan-version-detail")`. Evaluación `selected`: `currentRoute == item.route || currentRoute in item.childRoutes || item.childRoutePrefixes.any { prefix -> currentRoute?.startsWith(prefix) == true }` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/components/BottomNavigationBar.kt`

##### Strings

- [ ] **Actualizar strings.xml** (AC: 03.01, 03.02, 03.07, 03.10, 03.11)
  - [ ] **Corregir**: `nav_dictionary` de "Ejercicios" a "Diccionario". **Corregir**: `nav_settings` de "Ajustes" a "Configuración".
  - [ ] **Agregar sección D1**: `dictionary_title`, `tab_exercises`, `tab_plan`, `filter_all`, `filter_module`, `filter_equipment`, `filter_muscle_zone`, `exercise_count_format`, `no_exercises_match`, `badge_custom`, `fab_create_exercise`
  - [ ] **Agregar sección D2**: `exercise_field_name`, `exercise_field_module`, `exercise_field_equipment`, `exercise_field_muscle_zone`, `exercise_history_link`, `tap_to_add_image`
  - [ ] **Agregar sección D5**: `create_exercise_title`, `create_exercise_button`, `image_optional_hint`, `special_conditions_label`, `bodyweight_label`, `isometric_label`, `technical_failure_label`
  - [ ] **Agregar sección F3 stub**: `exercise_history_title`
  - Archivo: `app/src/main/res/values/strings.xml`

#### Fase 11: QA y Deployment

##### Code Quality

- [ ] **Ejecutar Agente Peer Review** — MANUAL
- [ ] **Resolver incidentes del Peer Review** (condicional) — MANUAL

##### Deployment DEV

- [ ] **Crear Pull Request** — MANUAL
- [ ] **Ejecutar pipeline deployment DEV** — MANUAL

##### Testing Manual

- [ ] **Diseñar set de pruebas manuales** — MANUAL
- [ ] **Ejecutar pruebas manuales** — MANUAL

---

**Vinculación CA → Fase de implementación:**
- CA-03.01 → Fases 1, 2, 3, 4 (Entities + DAOs + Seed 220 filas + Database/DI)
- CA-03.02 → Fases 5, 6, 8, 10 (Models + Repo + D1 Screen con exercise info + NavHost)
- CA-03.03 → Fase 8 (ViewModel filtro por módulo con opciones "Todos"/"A"/"B"/"C")
- CA-03.04 → Fase 8 (ViewModel filtro por equipo con opciones "Todos" + 9 tipos)
- CA-03.05 → Fase 8 (ViewModel filtro por zona con opciones "Todos" + 15 zonas)
- CA-03.06 → Fase 8 (ViewModel `combine` de 3 filtros = AND lógico)
- CA-03.07 → Fases 7, 9 (43 PNGs en assets/exercises/ + D2 Screen con media visual)
- CA-03.08 → Fase 9 (D2 es reutilizable; integración desde E1 se completa en HU-05/HU-06)
- CA-03.09 → Fase 7 (verificación de PNGs existentes, APK ≤ 150 MB)
- CA-03.10 → Fases 2, 5, 6, 8, 10 (DAO insert/countByNameAndEquipment + CreateExerciseUseCase + ExerciseRepository.createExercise + D5 CreateExerciseScreen + Navigation CREATE_EXERCISE)
- CA-03.11 → Fases 5, 6, 9 (UpdateExerciseImageUseCase + ExerciseRepository.updateExerciseImage + D2 image picker con logo placeholder + D5 imagen opcional)
