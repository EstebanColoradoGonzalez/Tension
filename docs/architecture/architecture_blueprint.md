# Blueprint de Arquitectura de Software

> Este documento es el mapa topológico integral del sistema, unificando desde el contexto exterior hasta las estructuras atómicas de código. Se utiliza para gobernar las dependencias, responsabilidades y el stack tecnológico. Mantener el formato jerárquico estricto.
>

---

## 1. Contexto y Fronteras del Ecosistema

*El sistema como caja negra, mapeando exclusivamente sus interacciones operativas con el mundo exterior.*

### 1.1. Actores (Agentes Operativos)

- **El Ejecutante:** Persona que realiza entrenamiento de fuerza orientado a hipertrofia. Es el único actor humano del sistema y cumple un doble rol simultáneo: operador (introduce datos de entrenamiento mediante la interfaz táctil) y beneficiario (consume las prescripciones, señales de progresión, alertas e historial analítico que el sistema produce). Su interacción principal es el ciclo registro → consulta → decisión que ocurre en cada sesión de entrenamiento.
- **El Sistema (agente automatizado):** El conjunto de reglas, cálculos y estructuras que procesan los datos del Ejecutante y producen decisiones. No es un actor humano, pero cumple un rol activo: determina la rutina y versión correspondiente por rotación cíclica, ejecuta el motor de progresión al cerrar cada sesión, genera alertas, prescribe cargas y preserva el historial. Inyecta resultados derivados que el Ejecutante consume.

### 1.2. Sistemas Externos (Dependencias)

- **Android OS (API 26 — API 35):** Plataforma de ejecución obligatoria. El sistema depende de Android OS para renderizado de UI (Jetpack Compose), gestión de ciclo de vida de componentes (Activity, ViewModel), sistema de archivos local (almacenamiento de base de datos y backup), permisos de almacenamiento externo (backup/restore) y la preferencia de tema claro/oscuro del dispositivo. No depende de servicios de Google (Play Services, Firebase, Maps) — solo de la plataforma Android nativa.
- **Sistema de Archivos del Dispositivo:** Almacenamiento local físico donde residen la base de datos SQLite (`tension.db`) y los archivos de backup exportados (`tension_backup_YYYYMMDD.json`). El sistema escribe y lee en el directorio interno de la app (`filesDir`) y opcionalmente en almacenamiento externo para los backups. Dependencia de existencia garantizada — toda instalación de Android tiene sistema de archivos.
- **Galería de Imágenes del Dispositivo:** Aplicación del sistema operativo que permite al ejecutante seleccionar imágenes para asociar a ejercicios personalizados (RF-62). El sistema interactúa con la galería mediante un Intent estándar de Android (`ACTION_PICK`). La imagen seleccionada se copia al almacenamiento interno de la app. Dependencia opcional — solo requerida al crear o editar ejercicios personalizados con imagen.

### 1.3. Exclusiones Explícitas (Anti-Alcance)

- **Backend remoto / API de red:** Se excluye formalmente cualquier comunicación con servidores externos. La app opera 100% offline (RNF-09). No existe endpoint, WebSocket, REST API ni GraphQL. La justificación arquitectónica es el contexto de uso: gimnasios con conectividad impredecible y el requisito de resiliencia total independiente de la red.
- **Google Play Services:** Se excluye toda dependencia de servicios propietarios de Google (Play Services, Firebase Analytics, Firebase Crashlytics, Google Sign-In, AdMob). El sistema no requiere ninguno de estos servicios para su funcionamiento y su inclusión añadiría dependencias de red innecesarias.
- **Material You (Color Dinámico):** Se excluye el esquema de color dinámico derivado del wallpaper del dispositivo (API 31+), aunque la app usa Material 3. La justificación es la necesidad de colores semánticos predecibles (progresión, regresión, alertas) y la compatibilidad con API 26+ (ADR-12).
- **Sincronización en la nube o multi-dispositivo:** Se excluye cualquier mecanismo de sincronización entre dispositivos. Los datos viven exclusivamente en el dispositivo del ejecutante. El mecanismo de backup/restore cubre el caso de migración de dispositivo de forma manual.
- **Funcionalidades sociales o de compartición de datos de entrenamiento:** Se excluyen perfiles públicos, comparativas entre usuarios, leaderboards y cualquier forma de intercambio de datos de entrenamiento con terceros.

---

## 2. Unidades Desplegables y Stack Tecnológico (Nivel Contenedores)

*Las piezas de software y almacenamiento que se ejecutan de forma independiente. Esta es la base tecnológica del proyecto.*

### 2.1. Inventario de Contenedores

- **Contenedor `UI-01`: Capa de Presentación (UI Layer)**
  - **Naturaleza Técnica:** Kotlin 2.0.21 + Jetpack Compose (vía BOM) + Compose Material 3 + Navigation Compose. Ejecutado en el hilo principal de Android (Main Dispatcher). Tecnología de compilación: AGP 8.9.1, Gradle con Kotlin DSL, Version Catalog (`libs.versions.toml`).
  - **Responsabilidad Central:** Renderizar la interfaz de usuario en las 27 vistas del sistema (Flujos A-J del Mapa de Navegación) y capturar las interacciones táctiles del ejecutante. No contiene lógica de negocio ni acceso a datos. Recibe estado inmutable desde el ViewModel y delega acciones hacia arriba mediante lambdas o eventos.
  - **Mapeo de Persistencia:** No aplica. Esta capa no persiste datos; solo renderiza el estado entregado por `UI-02`.
  - **Dependencias clave:** Jetpack Compose BOM, Compose Material 3, Material Symbols / Icons Extended, Navigation Compose, Lifecycle Runtime Compose (`collectAsStateWithLifecycle`).

- **Contenedor `UI-02`: Capa de ViewModel**
  - **Naturaleza Técnica:** Kotlin 2.0.21 + Lifecycle ViewModel Compose + Kotlin Coroutines + Kotlin Flow / StateFlow + Hilt Navigation Compose (`hiltViewModel()`). Los ViewModels sobreviven a cambios de configuración del dispositivo (rotación, cambio de idioma) y son destruidos cuando la pantalla se retira del back stack.
  - **Responsabilidad Central:** Gestionar el estado de la UI (expuesto como `StateFlow<UiState>`), orquestar operaciones delegando la lógica de negocio a los Use Cases del dominio, y emitir eventos one-shot (navegación, snackbar) mediante `SharedFlow`. Actúa como único punto de verdad para el estado de cada pantalla.
  - **Mapeo de Persistencia:** No aplica. El ViewModel no persiste datos; delega toda operación de persistencia a la capa de dominio y datos.

- **Contenedor `DOM-01`: Capa de Dominio (Use Cases + Motor de Reglas)**
  - **Naturaleza Técnica:** Kotlin 2.0.21 **puro** — sin ninguna dependencia de `android.*`, `androidx.*` ni `com.google.*` (RNF-29). Ejecutado en `Dispatchers.IO` cuando accede a repositorios; ejecutado de forma síncrona para cálculos del motor de reglas (funciones puras). Testeable con JUnit 4 en JVM local sin emulador (RNF-30).
  - **Responsabilidad Central:** Encapsular la totalidad de la lógica de negocio del sistema — las Reglas 1-7 del Manifiesto de Dominio Sistémico (Doble Umbral, detección de meseta, regresión, fatiga acumulada, protocolo de descarga, peso corporal, isométricos), los Use Cases que orquestan operaciones de negocio, y las interfaces de Repository que definen los contratos de acceso a datos (inversión de dependencia).
  - **Mapeo de Persistencia:** No aplica directamente. Define las interfaces de Repository (contratos); las implementaciones concretas viven en `DAT-01`.

- **Contenedor `DAT-01`: Capa de Datos (Repository + DAO)**
  - **Naturaleza Técnica:** Kotlin 2.0.21 + Room Runtime + Room KTX + Room Compiler (KSP) + Kotlin Coroutines. Las operaciones de escritura son funciones `suspend` ejecutadas en `Dispatchers.IO`. Las consultas de lectura exponen `Flow<T>` reactivo que se actualiza automáticamente cuando los datos cambian.
  - **Responsabilidad Central:** Implementar las interfaces de Repository definidas en `DOM-01`, abstraer los detalles de Room detrás de una API de dominio, y gestionar el acceso concurrente a la base de datos mediante transacciones atómicas. Incluye la prepopulación de datos semilla mediante `RoomDatabase.Callback.onCreate()` y la gestión de TypeConverters para tipos no nativos de SQLite.
  - **Mapeo de Persistencia:** Resguarda físicamente la totalidad del modelo de dominio definido en `domain_and_state_model.md`: las 18 entidades (routine, muscle_zone, equipment_type, exercise, exercise_muscle_zone, routine_version, plan_assignment, profile, weight_record, session, session_exercise, exercise_set, exercise_progression, rotation_state, routine_current_version, deload, deload_frozen_version, alert) y sus relaciones.

- **Contenedor `DB-01`: Base de Datos Local (SQLite via Room)**
  - **Naturaleza Técnica:** SQLite nativo de Android, accedido exclusivamente a través de Room ORM. Versión de esquema actual: 13 (migraciones registradas v1→v2…v12→v13). Archivo único en el directorio interno de la app: `filesDir/databases/tension.db`. Sin cifrado (ADR-15).
  - **Responsabilidad Central:** Persistir de forma durable todos los datos del sistema: histórico de sesiones y series (inmutables una vez registradas), plan de entrenamiento del ejecutante, catálogo de ejercicios, estados de progresión, rotación cíclica, ciclos de descarga y alertas. Garantiza integridad referencial mediante ForeignKeys y consistencia mediante transacciones atómicas.
  - **Mapeo de Persistencia:** 18 tablas que mapean 1:1 el esquema definido en `domain_and_state_model.md`. Datos semilla en `muscle_zone` (20 filas), `equipment_type` (23 filas), `exercise` (43 filas) y `exercise_muscle_zone` (48 filas) — precargados en `onCreate()`.

- **Contenedor `DI-01`: Módulo de Inyección de Dependencias (Hilt)**
  - **Naturaleza Técnica:** Hilt Android + Hilt Navigation Compose. Framework basado en Dagger con verificación del grafo de dependencias en tiempo de compilación. Anotaciones: `@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`, `@Inject`, `@Singleton`.
  - **Responsabilidad Central:** Gestionar el grafo de dependencias del sistema: instanciar y proveer `TensionDatabase` → DAOs → Repository Implementations → Use Cases → ViewModels. Asegurar que cada componente reciba sus dependencias automáticamente sin instanciación manual. Gestionar el ciclo de vida de las dependencias (Singleton para la DB, scoped para ViewModels).

### 2.2. Flujos de Red y Protocolos

- **De `UI-01` a `UI-02`:** El propósito es transmitir acciones del ejecutante (intenciones de usuario) al ViewModel para su procesamiento. Utiliza invocación de funciones Kotlin directas (llamadas a métodos del ViewModel desde lambdas de los Composables). La comunicación se ejecuta de forma síncrona en el hilo principal.
- **De `UI-02` a `UI-01`:** El propósito es entregar estado actualizado a la UI para su renderizado. Utiliza `StateFlow<UiState>` recolectado con `collectAsStateWithLifecycle()`. La comunicación es asíncrona basada en eventos (el ViewModel emite nuevo estado cuando cambia; la UI recompone automáticamente). Los eventos one-shot (navegación, snackbar) se emiten vía `SharedFlow` y se consumen en `LaunchedEffect`.
- **De `UI-02` a `DOM-01`:** El propósito es delegar lógica de negocio a los Use Cases. Utiliza invocación de funciones Kotlin `suspend` dentro de `viewModelScope.launch { }`. La comunicación es asíncrona no bloqueante.
- **De `DOM-01` a `DAT-01`:** El propósito es acceder a datos persistidos a través de las interfaces de Repository. Utiliza funciones `suspend` para escritura y `Flow<T>` para lectura reactiva. La comunicación es asíncrona no bloqueante, ejecutada en `Dispatchers.IO`.
- **De `DAT-01` a `DB-01`:** El propósito es ejecutar operaciones de lectura/escritura sobre SQLite. Utiliza el protocolo de Room: queries compiladas en tiempo de build, ejecutadas como SQL nativo sobre SQLite. Sin protocolo de red — comunicación local directa con el archivo de base de datos.

> **Nota:** No existen flujos de red externos. Toda comunicación es local entre contenedores del mismo proceso Android. El protocolo es Kotlin Coroutines + Flow para comunicación reactiva asíncrona entre capas.

---

## 3. Topología Lógica (Componentes y Clases)

*Acercamiento microscópico al interior de los contenedores para identificar los bloques de código y las estructuras de datos atómicas.*

---

### Contenedor: `UI-01` — Capa de Presentación

- **Módulo / Componente:** `TensionTheme` (ui.theme)
  - **Responsabilidad (SRP):** Aplicar el sistema de diseño visual completo: esquema de color (claro `#8B1A1A` seed / oscuro), escala tipográfica Material 3 (Roboto del sistema), formas y colores semánticos de dominio (progresión, regresión, alertas).
  - **Interfaces Expuestas:** Composable envolvente `TensionTheme { }` que proporciona el tema a todo el árbol Compose.
  - **Dependencias Internas:** Consume Material 3 (`MaterialTheme`) y `isSystemInDarkTheme()` de Android. Sin dependencias de negocio.
  - **Estructuras Atómicas Clave:** `Color.kt` (30 roles de color claro + oscuro + colores semánticos), `Type.kt` (15 estilos tipográficos M3), `Theme.kt` (composable `TensionTheme`).

- **Módulo / Componente:** `NavHost + Routes` (ui.navigation)
  - **Responsabilidad (SRP):** Definir el grafo de navegación completo (27 destinos en 10 nested graphs), gestionar el back stack, mapear rutas a Screens y gestionar la visibilidad condicional del Bottom Navigation Bar.
  - **Interfaces Expuestas:** `TensionNavHost(navController, startDestination)` — el punto de entrada de navegación de la aplicación.
  - **Dependencias Internas:** Consume todos los Screen Composables de los 10 paquetes de feature. Depende de `NavController` de Navigation Compose.
  - **Estructuras Atómicas Clave:** `Screen` (sealed class con las 25 rutas tipadas), `TensionNavHost` (función Composable), `BottomNavItem` (data class para los 5 ítems del Bottom Nav).

- **Módulo / Componente:** Screens por flujo funcional (ui.onboarding, ui.home, ui.session, etc.)
  - **Responsabilidad (SRP):** Cada Screen renderiza exactamente una vista del sistema y captura las interacciones del ejecutante para esa vista. No contiene lógica de negocio.
  - **Interfaces Expuestas:** Función `@Composable` por Screen: `RegisterProfileScreen`, `HomeScreen`, `ActiveSessionScreen`, `RegisterSetScreen`, etc. Reciben estado inmutable (`UiState`) y lambdas de acción como parámetros.
  - **Dependencias Internas:** Consumen su ViewModel correspondiente (via `hiltViewModel()`), componentes reutilizables de `ui.components` y el tema de `ui.theme`.
  - **Estructuras Atómicas Clave:** `{Feature}Screen` (Composable de pantalla), `{Feature}Content` (Composable de contenido separado del scaffold para facilitar previews).

- **Módulo / Componente:** Componentes reutilizables (ui.components)
  - **Responsabilidad (SRP):** Proveer bloques de UI compartidos entre múltiples Screens para evitar duplicación.
  - **Interfaces Expuestas:** `ProgressionIndicator`, `RirSelector`, `ExerciseListItem`, `SessionExerciseCard`, `LoadingIndicator`, `EmptyStateMessage`, `TensionTopBar`, `TensionBottomBar`.
  - **Dependencias Internas:** Solo dependen de `TensionTheme` y de parámetros inmutables recibidos como argumentos Composable.

---

### Contenedor: `UI-02` — Capa de ViewModel

- **Módulo / Componente:** ViewModels por flujo funcional
  - **Responsabilidad (SRP):** Un ViewModel por Screen o por flujo funcional cohesivo. Gestiona exactamente el estado de su pantalla y orquesta las operaciones delegando a Use Cases.
  - **Interfaces Expuestas:** `val uiState: StateFlow<{Feature}UiState>` (estado observable), `val events: SharedFlow<{Feature}Event>` (eventos one-shot), funciones públicas de acción: `startSession()`, `registerSet(weight, reps, rir)`, `closeSession()`, `activateDeload()`, etc.
  - **Dependencias Internas:** Inyecta Use Cases del Dominio mediante `@HiltViewModel`. No depende de DAOs, Repositories ni Room directamente.
  - **Estructuras Atómicas Clave:** `{Feature}ViewModel : ViewModel()`, `{Feature}UiState` (sealed class: Loading, Success, Error, Empty), `{Feature}Event` (sealed class: NavigateTo, ShowSnackbar), `{Feature}Action` (sealed class de intenciones del usuario).

---

### Contenedor: `DOM-01` — Capa de Dominio

- **Módulo / Componente:** Motor de Reglas (domain.rules)
  - **Responsabilidad (SRP):** Implementar las 7 reglas de negocio del Manifiesto de Dominio Sistémico como funciones puras testeables con JUnit 4 sin emulador.
  - **Interfaces Expuestas:** Funciones puras invocables por los Use Cases: `DoubleThresholdRule`, `ProgressionClassificationRule`, `PlateauDetectionRule`, `RegressionDetectionRule`, `AccumulatedFatigueRule`, `BodyweightProgressionRule`, `IsometricProgressionRule`.
  - **Dependencias Internas:** Cero dependencias externas — solo reciben tipos primitivos y modelos de dominio (data classes Kotlin puras) como entrada.
  - **Estructuras Atómicas Clave:** Una clase por regla (ej: `DoubleThresholdRule`) con `operator fun invoke(sets: List<SetData>): ProgressionSignal`. Los datos de entrada son modelos de dominio definidos en `domain.model`.

- **Módulo / Componente:** Use Cases (domain.usecase)
  - **Responsabilidad (SRP):** Encapsular una operación de negocio atómica. Cada Use Case orquesta: acceso a datos (via Repository interfaces), invocación de reglas del motor, y retorno de resultado.
  - **Interfaces Expuestas:** `operator fun invoke(params)`: función de invocación única por Use Case. Ejemplos: `StartSessionUseCase`, `RegisterSetUseCase`, `CloseSessionUseCase`, `DetectPlateauUseCase`, `ActivateDeloadUseCase`, `GetExerciseHistoryUseCase`, `ExportBackupUseCase`.
  - **Dependencias Internas:** Consume interfaces de Repository (contratos definidos en `domain.repository`). Invoca reglas del Motor según necesidad. Sin dependencias de Android.
  - **Estructuras Atómicas Clave:** `{Accion}{Entidad}UseCase` — clase con `operator fun invoke` suspendido o que retorna `Flow<T>`. Retorna `Result<T>` (sealed class Success/Error) para que el ViewModel gestione el estado de UI.

- **Módulo / Componente:** Interfaces de Repository (domain.repository)
  - **Responsabilidad (SRP):** Definir los contratos de acceso a datos que la capa Domain necesita, sin acoplarse a la implementación concreta (Room). Inversión de dependencia.
  - **Interfaces Expuestas:** Interfaces Kotlin: `SessionRepository`, `ExerciseRepository`, `PlanRepository`, `ProfileRepository`, `ProgressionRepository`, `AlertRepository`, `DeloadRepository`, `RotationRepository`, `BackupRepository`.
  - **Dependencias Internas:** Solo dependen de modelos de dominio (`domain.model`). Sin imports de Room ni Android.

---

### Contenedor: `DAT-01` — Capa de Datos

- **Módulo / Componente:** Repository Implementations (data.repository)
  - **Responsabilidad (SRP):** Implementar las interfaces de Repository definidas en `DOM-01`. Traducir entre modelos de dominio y entities de Room. Gestionar transacciones atómicas para operaciones multi-tabla.
  - **Interfaces Expuestas:** Implementa las interfaces de `domain.repository`. Inyectadas por Hilt mediante `@Binds` en `RepositoryModule`.
  - **Dependencias Internas:** Inyecta DAOs de Room. Usa `Dispatchers.IO` para operaciones de IO.
  - **Estructuras Atómicas Clave:** `SessionRepositoryImpl`, `ExerciseRepositoryImpl`, `PlanRepositoryImpl`, etc. Cada implementación encapsula los DAOs que necesita.

- **Módulo / Componente:** DAOs (data.local.dao)
  - **Responsabilidad (SRP):** Proveer operaciones de base de datos tipadas y verificadas en tiempo de compilación para cada entidad o grupo de entidades relacionadas.
  - **Interfaces Expuestas:** Interfaces `@Dao` de Room: `SessionDao`, `SessionExerciseDao`, `ExerciseSetDao`, `ExerciseDao`, `PlanAssignmentDao`, `ProfileDao`, `WeightRecordDao`, `ExerciseProgressionDao`, `RotationStateDao`, `DeloadDao`, `AlertDao`, `RoutineDao`, `RoutineVersionDao`, `MuscleZoneDao`, `EquipmentTypeDao`.
  - **Dependencias Internas:** Consumen Entities de Room. Generadas automáticamente por el compilador KSP de Room.
  - **Estructuras Atómicas Clave:** Métodos `suspend fun insert/update/delete` para escritura. Métodos `fun getAll/getById: Flow<T>` para lectura reactiva. `@Transaction` para operaciones multi-tabla atómicas.

- **Módulo / Componente:** Entities de Room (data.local.entity)
  - **Responsabilidad (SRP):** Representar la estructura de cada tabla de la base de datos como data class Kotlin verificable en compilación.
  - **Interfaces Expuestas:** 18 data classes con anotaciones Room: `@Entity`, `@PrimaryKey`, `@ForeignKey`, `@Index`, `@ColumnInfo`.
  - **Estructuras Atómicas Clave:** `RoutineEntity`, `MuscleZoneEntity`, `EquipmentTypeEntity`, `ExerciseEntity`, `ExerciseMuscleZoneEntity`, `RoutineVersionEntity`, `PlanAssignmentEntity`, `ProfileEntity`, `WeightRecordEntity`, `SessionEntity`, `SessionExerciseEntity`, `ExerciseSetEntity`, `ExerciseProgressionEntity`, `RotationStateEntity`, `RoutineCurrentVersionEntity`, `DeloadEntity`, `DeloadFrozenVersionEntity`, `AlertEntity`.

- **Módulo / Componente:** Seed Data (data.local.seed)
  - **Responsabilidad (SRP):** Prepoblar la base de datos con los datos de inicialización en el primer uso de la app mediante `RoomDatabase.Callback.onCreate()`.
  - **Interfaces Expuestas:** `PrepopulateFacade` — coordina la inserción de todos los datos semilla invocando Seeders temáticos.
  - **Dependencias Internas:** `ExerciseSeeder` (43 ejercicios base), `MuscleZoneSeeder` (20 zonas), `EquipmentTypeSeeder` (23 tipos), `ExerciseMuscleZoneSeeder` (48 relaciones ejercicio-zona).
  - **Estructuras Atómicas Clave:** Cada Seeder encapsula inserciones de sus entidades con datos literales en español.

---

## 4. Trazabilidad Funcional

*Matriz que garantiza la ejecución del código frente a las necesidades del negocio. Mapea Requerimientos Funcionales a los componentes responsables de su implementación.*

- **`RF-01, RF-02, RF-03` (Perfil del Ejecutante):** Lógica de negocio en `CreateProfileUseCase` / `UpdateProfileUseCase`. Persistencia en `ProfileRepositoryImpl` → `ProfileDao` / `WeightRecordDao`. Presentación en `RegisterProfileScreen` / `ProfileScreen` con `ProfileViewModel`.
- **`RF-04, RF-07, RF-61, RF-62` (Diccionario de Ejercicios):** Catálogo base en `ExerciseSeeder` (seed). Creación personalizada en `CreateExerciseUseCase`. Filtros en `GetExercisesFilteredUseCase`. Presentación en `ExerciseDictionaryScreen` / `ExerciseDetailScreen` con `CatalogViewModel`.
- **`RF-05, RF-06, RF-08, RF-63, RF-64, RF-65` (Plan de Entrenamiento):** Gestión del plan en `PlanRepositoryImpl` → `PlanAssignmentDao` / `RoutineVersionDao`. Use Cases: `GetPlanVersionDetailUseCase`, `AssignExerciseToPlanUseCase`, `RemoveExerciseFromPlanUseCase`. Presentación en `TrainingPlanScreen` / `PlanVersionDetailScreen` con `CatalogViewModel`.
- **`RF-09, RF-10, RF-11` (Rotación Cíclica):** Motor de rotación en `GetNextSessionUseCase` → `RotationRepositoryImpl` → `RotationStateDao`. La posición persiste indefinidamente en `rotation_state`. Presentación en `HomeScreen` con `HomeViewModel`.
- **`RF-12, RF-13, RF-14, RF-15, RF-16, RF-17` (Registro de Sesión):** Inicio en `StartSessionUseCase`. Registro de series en `RegisterSetUseCase`. Sustituciones en `SubstituteExerciseUseCase`. Persistencia en `SessionRepositoryImpl` → `SessionDao`, `SessionExerciseDao`, `ExerciseSetDao`. Presentación en `ActiveSessionScreen`, `RegisterSetScreen`, `SubstituteExerciseScreen` con `ActiveSessionViewModel`.
- **`RF-18, RF-19, RF-20, RF-21, RF-59` (Cierre de Sesión):** Protocolo de cierre en `CloseSessionUseCase` — orquesta: `DoubleThresholdRule`, `ProgressionClassificationRule`, `PlateauDetectionRule`, `AccumulatedFatigueRule`, cálculo de tonelaje, actualización de `exercise_progression`, generación de alertas y avance de `rotation_state`. Presentación en `SessionSummaryScreen` con `SessionSummaryViewModel`.
- **`RF-23, RF-24, RF-25, RF-26, RF-27, RF-28, RF-29, RF-30, RF-31, RF-32, RF-33` (Motor de Progresión):** Implementado en `DOM-01 domain.rules`: `DoubleThresholdRule` (RF-25/26), `ProgressionClassificationRule` (RF-23/24), `RegressionDetectionRule` (RF-29), `AccumulatedFatigueRule` (RF-30), `BodyweightProgressionRule` (RF-31), `IsometricProgressionRule` (RF-32/33). Invocados por `CloseSessionUseCase`.
- **`RF-34, RF-35, RF-36, RF-37` (Detección de Mesetas y Descarga):** `PlateauDetectionRule` (RF-34) y `DeloadRecommendationRule` (RF-37) en `DOM-01`. Alertas generadas por `GenerateAlertsUseCase` → `AlertRepositoryImpl` → `AlertDao`. Presentación en `AlertCenterScreen`, `AlertDetailScreen` con `AlertViewModel`.
- **`RF-38, RF-39, RF-40` (Protocolo de Descarga):** `ActivateDeloadUseCase` → `DeloadRepositoryImpl` → `DeloadDao`, `DeloadFrozenVersionDao`. Carga de reinicio calculada en `CloseSessionUseCase` post-descarga. Presentación en `DeloadScreen` con `DeloadViewModel`.
- **`RF-41, RF-42, RF-44, RF-45, RF-46, RF-47, RF-48, RF-49` (KPIs y Métricas):** Use Cases de métricas: `GetProgressionRateUseCase`, `GetTonnageByMuscleGroupUseCase`, `GetRirAverageUseCase`, `GetAdherenceIndexUseCase`, `GetLoadVelocityUseCase`, `GetVolumeDistributionUseCase`. Presentación en `MetricsPanelScreen`, `MuscleVolumeScreen`, `ProgressionTrendScreen` con `MetricsViewModel`.
- **`RF-43` (Estado de Progresión Persistido):** `exercise_progression` actualizado por `CloseSessionUseCase` tras cada sesión. Leído por `GetExerciseHistoryUseCase` → `ExerciseProgressionDao`. Presentación en `ExerciseHistoryScreen` con `HistoryViewModel`.
- **`RF-50, RF-51, RF-52, RF-60` (Historial y Consultas):** `GetSessionHistoryUseCase`, `GetSessionDetailUseCase`, `GetExerciseHistoryUseCase`, `GetTonnageTrendUseCase`. Presentación en `SessionHistoryScreen`, `SessionDetailScreen`, `ExerciseHistoryScreen` con `HistoryViewModel`.
- **`RF-53, RF-54, RF-55, RF-56, RF-57, RF-58` (Sistema de Alertas):** Alertas generadas por `GenerateAlertsUseCase` (invocado tras cada cierre de sesión). Persistencia en `AlertRepositoryImpl` → `AlertDao`. Resolución automática evaluada en el mismo Use Case. Presentación en `AlertCenterScreen`, `AlertDetailScreen`.
- **`RF-15, RF-31` (Exportar / Importar Backup):** `ExportBackupUseCase` serializa BD a JSON en `Dispatchers.IO`. `ImportBackupUseCase` valida, deserializa y restaura en transacción atómica. Presentación en `ExportBackupScreen`, `ImportBackupScreen` con `SettingsViewModel`.

---

## 5. Registros de Decisiones Arquitectónicas (ADR)

*El registro formal que justifica las elecciones tecnológicas y de diseño del sistema.*

---

### ADR-001: Kotlin como lenguaje principal

- **Contexto:** El proyecto requiere soporte de primera clase para coroutines (IO asíncrono sobre Room), null safety en compilación y compatibilidad nativa con Jetpack Compose.
- **Decisión:** Kotlin 2.0.21 como lenguaje único. JVM target 11. El nuevo compilador K2 habilita mejor rendimiento de compilación de Compose.
- **Consecuencias:** Acceso a todas las APIs modernas de Android y Jetpack. El motor de reglas se escribe en Kotlin puro, testeable sin emulador. Compose requiere Kotlin — no hay alternativa en este stack.

---

### ADR-002: Jetpack Compose para la UI

- **Contexto:** 27 vistas con estados complejos requieren un framework declarativo que minimice boilerplate y facilite renderizado basado en estado.
- **Decisión:** Jetpack Compose (vía BOM) + Compose Material 3 para toda la interfaz. No hay layouts XML, Fragments ni Views.
- **Consecuencias:** UI como funciones `@Composable`. Estado fluye unidireccionalmente: `StateFlow<UiState>` → recomposición automática. Navigation Compose reemplaza Navigation Component XML. Testing con Compose UI Test JUnit4.

---

### ADR-003: Room como ORM de persistencia local

- **Contexto:** App 100% offline con 18 entidades relacionadas, queries multi-tabla (tonelaje por grupo muscular, historial de ejercicio) y datos que crecen indefinidamente. Se requieren transacciones atómicas y migraciones automáticas.
- **Decisión:** Room (Runtime + KTX + Compiler KSP). Queries verificadas en compilación. Migraciones con objetos `Migration`.
- **Consecuencias:** 18 `@Entity`, `@Dao` por entidad/grupo, `@Transaction` para operaciones de cierre de sesión. `Flow<T>` reactivo para UI. TypeConverters para fechas ISO 8601. Validación de queries SQL en build time — errores detectados antes de runtime.

---

### ADR-004: Hilt como framework de inyección de dependencias

- **Contexto:** Grafo de dependencias profundo: `TensionDatabase` → DAOs → Repositories → Use Cases → ViewModels. El testing requiere sustituir implementaciones por mocks.
- **Decisión:** Hilt Android + Hilt Navigation Compose. Verificación del grafo en compilación. `@HiltViewModel` + `hiltViewModel()` para ViewModels.
- **Consecuencias:** 2 módulos Hilt: `DatabaseModule` (DB y DAOs como `@Singleton`) y `RepositoryModule` (interfaces ↔ implementaciones). Si falta un binding, el build falla en compilación. Eliminación de Manual DI para un grafo de 17+ DAOs.

---

### ADR-005: MVVM con capa Domain explícita

- **Contexto:** Lógica de negocio densa (7 reglas, KPIs, protocolo de descarga, detección de mesetas) requiere una capa intermedia testeable sin dependencias de Android.
- **Decisión:** MVVM con 4 capas (UI → ViewModel → Domain → Data) y regla de dependencia unidireccional estricta. La capa Domain no importa ninguna clase de `android.*` ni `androidx.*`.
- **Consecuencias:** Use Cases encapsulan operaciones con `operator fun invoke()`. Flujo reactivo: Data `Flow<T>` → Domain → ViewModel `StateFlow<UiState>` → UI `collectAsStateWithLifecycle()`. Inversión de dependencia via interfaces de Repository.

---

### ADR-006: Motor de reglas como Kotlin puro sin dependencias Android

- **Contexto:** Las Reglas 1-7 son el diferenciador del producto. Deben tener cobertura alta de tests unitarios ejecutables sin emulador (RNF-29, RNF-30).
- **Decisión:** Funciones puras en el paquete `domain.rules`. Un archivo por regla. Invocadas por los Use Cases de cierre de sesión.
- **Consecuencias:** 7 archivos de reglas, cada uno testeable con JUnit 4 en JVM local. Las reglas reciben datos primitivos/modelos de dominio y retornan resultados — sin acceso a Room, SharedPreferences ni componentes Android. RNF-30 satisfecho directamente.

---

### ADR-007: Single Activity con Navigation Compose

- **Contexto:** 27 vistas, 10 flujos funcionales, 58 conexiones de navegación, navegación por tabs con preservación de estado, start destination dinámica (A1 vs B1), flujo contenido de sesión activa (E1-E5 sin escape al Bottom Nav).
- **Decisión:** `MainActivity` única con `NavHost` de Navigation Compose. 10 nested graphs (uno por flujo). Bottom Navigation con `saveState`/`restoreState`.
- **Consecuencias:** 25 rutas tipadas (E4 es `AlertDialog`, no ruta). Nested graphs aíslan back stack por tab. Transiciones especiales con `popUpTo` (A1→B1, E4→E5, J3→B1). Reglas de visibilidad del Bottom Nav evaluadas por ruta actual del `NavController`.

---

### ADR-008: Estructura de paquetes layer-first con agrupación por feature

- **Contexto:** Se necesita una estructura que haga explícita la regla de dependencia entre capas y que agrupe código que cambia junto.
- **Decisión:** Raíz organizada por capa (`ui/`, `domain/`, `data/`, `di/`). Dentro de `ui/` y `domain/usecase/`, sub-paquetes por flujo funcional.
- **Consecuencias:** 4 paquetes raíz. `ui/` con 10 paquetes de feature + 3 transversales (navigation, components, theme). `domain/` con 4 sub-paquetes (model, repository, usecase, rules). `data/` con local (database, dao, entity, seed, storage) y repository. La estructura refleja directamente la regla de dependencia arquitectónica.

---

### ADR-009: StateFlow + SharedFlow para gestión de estado reactivo

- **Contexto:** Los ViewModels necesitan exponer estado reactivo a la UI y emitir eventos one-shot (navegación, snackbar) consumidos exactamente una vez.
- **Decisión:** `MutableStateFlow` (privado) → `StateFlow` (público) para estado. `MutableSharedFlow` (privado) → `SharedFlow` (público) para eventos one-shot.
- **Consecuencias:** Cada ViewModel expone `val uiState: StateFlow<FeatureUiState>` con sealed class (Loading, Success, Error, Empty). Eventos recolectados en `LaunchedEffect`. UI recolecta con `collectAsStateWithLifecycle()` — se detiene cuando el lifecycle no está en STARTED.

---

### ADR-010: JSON como formato de backup

- **Contexto:** RNF-17 permite JSON o SQLite exportado. El archivo debe ser autodescriptivo, incluir metadatos de versión y completarse en < 10 segundos para 2 años de datos.
- **Decisión:** JSON con estructura autodescriptiva. Nombre: `tension_backup_YYYYMMDD.json`. Header con metadatos de versión de app y esquema.
- **Consecuencias:** Serialización/deserialización en `Dispatchers.IO`. Validación de metadatos al importar (migraciones de datos si versión anterior). Sin cifrado (advertencia al usuario). Exportación/importación en transacción atómica. Manejable en < 10s para ~30,000 registros de series.

---

### ADR-011: Prepopulación de datos con RoomDatabase.Callback

- **Contexto:** El Diccionario de Ejercicios (43 ejercicios), zonas musculares (20) y tipos de equipo (23) deben existir desde el primer uso. El ejecutante configura su plan después (HU-22).
- **Decisión:** `RoomDatabase.Callback.onCreate()` con patrón Facade que delega en Seeders temáticos (`ExerciseSeeder`, `MuscleZoneSeeder`, `EquipmentTypeSeeder`, `ExerciseMuscleZoneSeeder`).
- **Consecuencias:** Seed data en código Kotlin versionado en Git. Toda inserción en una transacción — atomicidad garantizada. `rotation_state` se inicializa en `ProfileRepositoryImpl.createProfile()` (no como seed). Ejercicios custom (RF-62) y asignaciones al plan (RF-63) persisten via CRUD en runtime, no como seed.

---

### ADR-012: Esquema de color propio derivado de seed — sin Material You dinámico

- **Contexto:** Los colores semánticos (progresión verde, regresión rojo, alertas) deben mantener contraste predecible. Material You (API 31+) es incompatible con la cobertura API 26+.
- **Decisión:** Esquema propio derivado del seed `#8B1A1A` (Rojo Imperio Romano). La preferencia claro/oscuro del sistema SÍ se respeta; el wallpaper NO influye.
- **Consecuencias:** `TensionTheme` aplica `lightColorScheme()` o `darkColorScheme()` según `isSystemInDarkTheme()`. Colores semánticos definidos como extensiones del tema M3. Contraste verificado WCAG AA en ambos esquemas. Sin `dynamicLightColorScheme()` — eliminando dependencia de API 31+.

---

### ADR-013: Tipografía del sistema (Roboto) sin fuentes custom

- **Contexto:** El APK tiene presupuesto de tamaño limitado (< 150 MB, RNF-24) por los 43 assets PNG de ejercicios. La app opera 100% offline (sin descarga de fuentes en runtime).
- **Decisión:** Fuente del sistema (Roboto) para todos los estilos tipográficos M3. Sin fuentes empaquetadas en assets ni descargadas desde red.
- **Consecuencias:** `Type.kt` define la escala de 15 estilos M3 sin cambiar la familia de fuentes. Display Large/Medium/Small no se usan en ninguna vista. El presupuesto de APK se reserva íntegramente para los assets multimedia de ejercicios.

---

### ADR-014: Distribución como APK firmado sin Google Play Store

- **Contexto:** La app es de uso personal. La publicación en Play Store introduce requisitos (revisión, metadata, políticas) sin beneficio para un usuario único.
- **Decisión:** APK release firmado con keystore del desarrollador. Instalación directa habilitando "fuentes desconocidas". Sin Google Play Console.
- **Consecuencias:** Actualizaciones distribuidas como nuevo APK instalado manualmente. Sin Play App Signing. Tamaño del APK debe mantenerse < 150 MB (RNF-24) incluyendo assets multimedia.

---

### ADR-015: Base de datos sin cifrado

- **Contexto:** Los datos son registros de entrenamiento personal (peso, repeticiones, RIR). No hay información financiera, médica protegida, credenciales ni datos de terceros. El cifrado añadiría SQLCipher (~3 MB) y overhead de rendimiento.
- **Decisión:** Room opera directamente sobre SQLite estándar sin capa de cifrado. El archivo de backup tampoco se cifra (RNF-26, advertencia al usuario).
- **Consecuencias:** Queries marginalmente más rápidas. APK sin librería SQLCipher (~3 MB ahorrados). Advertencia de contenido no cifrado al exportar backup.

---

### ADR-016: Solo modo portrait — sin soporte landscape ni tablets

- **Contexto:** La app se usa en el gimnasio frecuentemente con una mano entre series. Los formularios y listas están diseñados para scroll vertical en pantallas 5"-7". El soporte landscape duplicaría esfuerzo sin beneficio.
- **Decisión:** `android:screenOrientation="portrait"` en `AndroidManifest.xml`. Sin layouts alternativos `res/layout-land/` ni `res/layout-sw600dp/`.
- **Consecuencias:** Todos los Composables diseñados para portrait. Previews configuradas solo en portrait. Los diseños son responsivos dentro del rango portrait 5"-7" (720p-1440p).

---

### ADR-017: Interfaz monoidioma en español

- **Contexto:** App de uso personal para un ejecutante hispanohablante. Sin requerimiento de internacionalización ni plan de distribución a otros idiomas.
- **Decisión:** Toda la UI en español. Un único `res/values/strings.xml`. Sin carpetas `values-en/`, `values-pt/`, etc.
- **Consecuencias:** Strings de UI en español en `strings.xml`. Mensajes de validación de dominio en inglés (uso interno, no visibles). Datos seed en español. Código fuente en inglés. Si en el futuro se requiere otro idioma, los strings ya están en `strings.xml` — extraíbles, pero no es objetivo actual.

---

### ADR-018: JUnit 4 para testing del motor de reglas

- **Contexto:** Las reglas R1-R7 son la parte más crítica del sistema. Deben tener cobertura alta y ejecutarse sin emulador (RNF-30). JUnit 5 requiere plugin Gradle adicional no soportado nativamente por AGP.
- **Decisión:** JUnit 4 para tests unitarios del motor de reglas y Use Cases (en `test/`, JVM local). Compose UI Test JUnit4 para tests de UI instrumentados. Espresso para integración.
- **Consecuencias:** Tests del motor de reglas en `test/` — ejecución rápida sin emulador. Un archivo de tests por regla (R1-R7) cubriendo: caso exitoso, caso de frontera, datos insuficientes. Use Cases testeados con mocks de Repository. ViewModels testeados con `TestCoroutineDispatcher`.

---

### Decisiones de Dominio con Impacto Técnico

*Decisiones de modelado de dominio que influyen directamente en la implementación.*

| ID | Decisión de Dominio | Impacto Técnico |
|----|---------------------|-----------------|
| D-01 | Rotación cíclica agnóstica al calendario | `rotation_state.microcycle_position` persiste indefinidamente. Sin lógica de fecha/calendario en la determinación de rutina. Posición inmune a ausencias del ejecutante. |
| D-02 | Sustitución puntual solo con ejercicio "No Iniciado" (0 series) | El botón "Sustituir" en E1 se muestra/oculta según `session_exercise` sin series (`COUNT(exercise_set) = 0`). Validación en `SubstituteExerciseUseCase`. |
| D-03 | Sesiones cerradas son inmutables | Sin endpoints ni Use Cases de edición post-cierre. `session.status` = COMPLETED/INCOMPLETE congela todos los datos asociados. No se implementan funciones de update retroactivo en DAOs de sesión. |
| D-04 | E4 es un diálogo, no una pantalla de navegación | `AlertDialog` Compose gestionado por estado del ViewModel (`showCloseDialog: Boolean`). No es una ruta en el `NavHost`. No entra al back stack. |
| D-05 | Objetivo de frecuencia semanal con default 6 sesiones | `profile.weekly_frequency` inicializado en 6 (rango 4-6). No se solicita en onboarding (A1). Configurable en J1. Denominador del KPI de Adherencia (RF-47). |
| D-06 | Comparación de progresión contra último registro del mismo ejercicio, independiente de versión | El query de comparación en `GetLastExerciseSessionUseCase` busca la última `session_exercise` por `exercise_id` sin filtrar `routine_version_id`. |
| D-07 | Incremento diferenciado: +2.5 Kg tren superior, +5 Kg tren inferior | Constantes en `DoubleThresholdRule`: `LOAD_INCREMENT_UPPER = 2.5f`, `LOAD_INCREMENT_LOWER = 5.0f`. Determinadas por zona muscular del ejercicio via `exercise_muscle_zone`. |
| D-08 | Descarga dura 1 microciclo completo sin rotar versiones | `deload.status = 'ACTIVE'` durante N sesiones (N = total de rutinas). Las versiones de cada rutina se congelan en `deload_frozen_version`. No cambian hasta `deload.status = 'COMPLETED'`. |
