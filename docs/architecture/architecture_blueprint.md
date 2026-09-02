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
  - **Responsabilidad Central:** Renderizar la interfaz de usuario en las 26 vistas del sistema (Flujos A-J del Mapa de Navegación) y capturar las interacciones táctiles del ejecutante. No contiene lógica de negocio ni acceso a datos. Recibe estado inmutable desde el ViewModel y delega acciones hacia arriba mediante lambdas o eventos.
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
  - **Mapeo de Persistencia:** Resguarda físicamente la totalidad del modelo de dominio definido en `domain_and_state_model.md`: las 22 entidades (routine, muscle_zone, equipment_type, exercise, exercise_muscle_zone, routine_version, plan_assignment, profile, weight_record, session, session_exercise, exercise_set, exercise_progression, rotation_state, routine_current_version, week_day, daily_routine_override, day_skip, tree_state, deload, deload_frozen_version, alert) y sus relaciones.

- **Contenedor `DB-01`: Base de Datos Local (SQLite via Room)**
  - **Naturaleza Técnica:** SQLite nativo de Android, accedido exclusivamente a través de Room ORM. Versión de esquema actual: **19** (migraciones registradas v1→v2…v15→v16; **v17, v18 y v19 no tienen migración** por ADR-019). Archivo único en el directorio interno de la app: `filesDir/databases/tension.db`. Sin cifrado (ADR-15).
  - **Responsabilidad Central:** Persistir de forma durable todos los datos del sistema: histórico de sesiones y series (inmutables una vez registradas), plan de entrenamiento del ejecutante, catálogo de ejercicios, estados de progresión, rotación cíclica, ciclos de descarga y alertas. Garantiza integridad referencial mediante ForeignKeys y consistencia mediante transacciones atómicas.
  - **Mapeo de Persistencia:** 22 tablas que mapean 1:1 el esquema definido en `domain_and_state_model.md` — las 18 originales más `week_day`, `daily_routine_override` y `day_skip` (HU-36) y `tree_state` (HU-37). Datos semilla en `muscle_zone` (20 filas), `equipment_type` (23 filas), `exercise` (37 filas) y `exercise_muscle_zone` (41 filas) — precargados en `onCreate()`.

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
  - **Responsabilidad (SRP):** Definir el grafo de navegación completo (26 destinos en 10 nested graphs), gestionar el back stack, mapear rutas a Screens y gestionar la visibilidad condicional del Bottom Navigation Bar.
  - **Interfaces Expuestas:** `TensionNavHost(navController, startDestination)` — el punto de entrada de navegación de la aplicación.
  - **Dependencias Internas:** Consume todos los Screen Composables de los 10 paquetes de feature. Depende de `NavController` de Navigation Compose.
  - **Estructuras Atómicas Clave:** `Screen` (sealed class con las 24 rutas tipadas), `TensionNavHost` (función Composable), `BottomNavItem` (data class para los 5 ítems del Bottom Nav).

- **Módulo / Componente:** Screens por flujo funcional (ui.onboarding, ui.home, ui.session, etc.)
  - **Responsabilidad (SRP):** Cada Screen renderiza exactamente una vista del sistema y captura las interacciones del ejecutante para esa vista. No contiene lógica de negocio.
  - **Interfaces Expuestas:** Función `@Composable` por Screen: `RegisterProfileScreen`, `HomeScreen`, `ActiveSessionScreen`, `RegisterSetScreen`, etc. Reciben estado inmutable (`UiState`) y lambdas de acción como parámetros.
  - **Dependencias Internas:** Consumen su ViewModel correspondiente (via `hiltViewModel()`), componentes reutilizables de `ui.components` y el tema de `ui.theme`.
  - **Estructuras Atómicas Clave:** `{Feature}Screen` (Composable de pantalla), `{Feature}Content` (Composable de contenido separado del scaffold para facilitar previews).

- **Módulo / Componente:** Componentes reutilizables (ui.components)
  - **Responsabilidad (SRP):** Proveer bloques de UI compartidos entre múltiples Screens para evitar duplicación.
  - **Interfaces Expuestas:** `ProgressionIndicator`, `RirSelector`, `ExerciseListItem`, `SessionExerciseCard`, `LoadingIndicator`, `EmptyStateMessage`, `TensionTopBar`, `TensionBottomBar`, `ReassignRoutineDialog`, `weekDayName` / `weekDayShortName`.
  - **Dependencias Internas:** Solo dependen de `TensionTheme` y de parámetros inmutables recibidos como argumentos Composable.
  - **Nota (HU-37):** `TreeIcon` es la única traducción de etapa → recurso vectorial y de salud → tinte, compartida por la tarjeta de Inicio y la pantalla dedicada, para que ambas no puedan divergir. Son **cuatro recursos y no una matriz de veinte** porque el color se resuelve en tiempo de ejecución desde `LocalTensionSemanticColors`. `TreeScreen` aísla la representación en un composable privado `TreeVisual` de tamaño fijo: ningún otro elemento del layout conoce su contenido, que es la costura por la que HU-38 podrá sustituirlo sin reorganizar la pantalla. La tarjeta de Inicio es **nativa de forma permanente** — nunca llevará WebView, tampoco después de HU-38 (RNF01).
  - **Nota (HU-36):** `ReassignRoutineDialog` es el selector de reasignación temporal de la rutina de hoy, compartido por Inicio y por el preview de sesión — la reasignación se resuelve sobre la pantalla actual y no añade ruta al `NavHost`. `WeekDayLabel` es la única traducción de `WeekDay` a etiqueta legible, para que Inicio, el selector y la pestaña Plan no puedan divergir.

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
  - **Nota (HU-37):** `TreeHealthRule.calculate(daysSinceLastSession)` y `TreeGrowthStageRule.resolve(sessionCount)` son las dos reglas del árbol, **separadas a propósito**: las dimensiones son ortogonales — la estatura depende del historial y la salud de la recencia — y una sola regla que devolviera un par obligaría a leer las dos ramas juntas. Ambas son Kotlin puro y no reciben ningún estado de rotación, progresión ni alerta: **no forman parte del motor de decisión** y ninguna otra regla las invoca.
  - **Nota (HU-36):** `DailyRoutineRule.resolve(today, permanentRoutineId, override)` resuelve qué rutina corresponde hoy: la relación permanente del día, sustituida por la reasignación temporal cuando su fecha es hoy. Su firma es el contrato de la frontera con la rotación — no recibe posición ni conteo de microciclo, de modo que ninguna reasignación puede alcanzar `rotation_state`. `WeekDayAssignmentRule.resolveRoutineFor` decide, al editar los días de una rutina, qué rutina queda en cada día: las tres transiciones posibles —tomar, liberar, no tocar— se derivan de que el día apunte a una sola rutina. `DayResolutionRule.resolve` decide si el día de hoy ya está resuelto —entrenado u omitido— y `NextTrainingDayRule.resolve` encuentra el siguiente día con rutina saltando los de descanso. Juntas son las que impiden ejecutar varias sesiones la misma jornada, y ninguna recibe estado de rotación.

- **Módulo / Componente:** Use Cases (domain.usecase)
  - **Responsabilidad (SRP):** Encapsular una operación de negocio atómica. Cada Use Case orquesta: acceso a datos (via Repository interfaces), invocación de reglas del motor, y retorno de resultado.
  - **Interfaces Expuestas:** `operator fun invoke(params)`: función de invocación única por Use Case. Ejemplos: `StartSessionUseCase`, `RegisterSetUseCase`, `CloseSessionUseCase`, `DetectPlateauUseCase`, `ActivateDeloadUseCase`, `GetExerciseHistoryUseCase`, `ExportBackupUseCase`. HU-36 añade `GetTodaySessionUseCase` (reemplaza `GetNextSessionInfoUseCase`), `GetReassignableRoutinesUseCase`, `SetTemporaryRoutineUseCase`, `ClearTemporaryRoutineUseCase`, `GetWeekDayPlanUseCase` y `UpdateRoutineWeekDaysUseCase` — este último es la edición **permanente** de la relación día → rutina, distinta de la reasignación temporal de una sesión. `SkipTodayUseCase` y `UndoSkipTodayUseCase` cancelan el día y lo revierten; el primero descarta de paso una sesión en curso sin series, y se rechaza en cuanto hay una registrada. `ResolveStaleSessionUseCase` resuelve al arrancar —y al cruzar la medianoche— la sesión que quedó abierta de un día anterior: la cierra si tiene series, la descarta si no. HU-37 añade `GetTreeStateUseCase` y `RecalculateTreeStateUseCase`; el segundo es invocado desde `CloseSessionUseCase`, desde `ImportBackupUseCase` y desde `MainViewModel` **inmediatamente después** de `ResolveStaleSessionUseCase`, nunca antes — el orden es la garantía de que el árbol no se marchite para quien sí entrenó.
  - **Dependencias Internas:** Consume interfaces de Repository (contratos definidos en `domain.repository`). Invoca reglas del Motor según necesidad. Sin dependencias de Android.
  - **Estructuras Atómicas Clave:** `{Accion}{Entidad}UseCase` — clase con `operator fun invoke` suspendido o que retorna `Flow<T>`. Retorna `Result<T>` (sealed class Success/Error) para que el ViewModel gestione el estado de UI.

- **Módulo / Componente:** Interfaces de Repository (domain.repository)
  - **Responsabilidad (SRP):** Definir los contratos de acceso a datos que la capa Domain necesita, sin acoplarse a la implementación concreta (Room). Inversión de dependencia.
  - **Interfaces Expuestas:** Interfaces Kotlin: `SessionRepository`, `ExerciseRepository`, `PlanRepository`, `ProfileRepository`, `ProgressionRepository`, `AlertRepository`, `DeloadRepository`, `RotationRepository`, `BackupRepository`, `WeekDayRepository`, `TreeRepository`.
  - **Dependencias Internas:** Solo dependen de modelos de dominio (`domain.model`). Sin imports de Room ni Android.

---

### Contenedor: `DAT-01` — Capa de Datos

- **Módulo / Componente:** Repository Implementations (data.repository)
  - **Responsabilidad (SRP):** Implementar las interfaces de Repository definidas en `DOM-01`. Traducir entre modelos de dominio y entities de Room. Gestionar transacciones atómicas para operaciones multi-tabla.
  - **Interfaces Expuestas:** Implementa las interfaces de `domain.repository`. Inyectadas por Hilt mediante `@Binds` en `RepositoryModule`.
  - **Dependencias Internas:** Inyecta DAOs de Room. Usa `Dispatchers.IO` para operaciones de IO.
  - **Estructuras Atómicas Clave:** `SessionRepositoryImpl`, `ExerciseRepositoryImpl`, `PlanRepositoryImpl`, `WeekDayRepositoryImpl`, etc. Cada implementación encapsula los DAOs que necesita.
  - **Nota (HU-37):** `TreeRepositoryImpl` deriva el árbol de `SessionDao` —`countClosedSessions` y `getLastClosedSessionDate`, la contraparte **global** de `getLastSessionDateByRoutine`— y lo persiste en `TreeStateDao`. Vive separado de `SessionRepositoryImpl` para que el aislamiento sea legible en el propio contrato: el árbol lee del historial y **nada del sistema lee del árbol**. Los días transcurridos se derivan al mapear, no al persistir, de modo que la pantalla no pueda mostrar un conteo rancio.
  - **Nota (HU-36):** `WeekDayRepositoryImpl` expone la relación permanente día → rutina (pestaña Plan), el listado de rutinas ejecutables hoy y la escritura de la reasignación temporal. `SessionRepositoryImpl.getTodaySession()` es quien resuelve la propuesta del día combinando `week_day`, `daily_routine_override` y `DailyRoutineRule`; su flujo de fecha reemite al cruzar la medianoche local, para que la reversión automática de la reasignación ocurra con la app abierta.

- **Módulo / Componente:** DAOs (data.local.dao)
  - **Responsabilidad (SRP):** Proveer operaciones de base de datos tipadas y verificadas en tiempo de compilación para cada entidad o grupo de entidades relacionadas.
  - **Interfaces Expuestas:** Interfaces `@Dao` de Room: `SessionDao`, `SessionExerciseDao`, `ExerciseSetDao`, `ExerciseDao`, `PlanAssignmentDao`, `ProfileDao`, `WeightRecordDao`, `ExerciseProgressionDao`, `RotationStateDao`, `DeloadDao`, `AlertDao`, `RoutineDao`, `RoutineVersionDao`, `MuscleZoneDao`, `EquipmentTypeDao`, `WeekDayDao`, `DailyRoutineOverrideDao`, `DaySkipDao`, `TreeStateDao`.
  - **Dependencias Internas:** Consumen Entities de Room. Generadas automáticamente por el compilador KSP de Room.
  - **Estructuras Atómicas Clave:** Métodos `suspend fun insert/update/delete` para escritura. Métodos `fun getAll/getById: Flow<T>` para lectura reactiva. `@Transaction` para operaciones multi-tabla atómicas.

- **Módulo / Componente:** Entities de Room (data.local.entity)
  - **Responsabilidad (SRP):** Representar la estructura de cada tabla de la base de datos como data class Kotlin verificable en compilación.
  - **Interfaces Expuestas:** 22 data classes con anotaciones Room: `@Entity`, `@PrimaryKey`, `@ForeignKey`, `@Index`, `@ColumnInfo`.
  - **Estructuras Atómicas Clave:** `RoutineEntity`, `MuscleZoneEntity`, `EquipmentTypeEntity`, `ExerciseEntity`, `ExerciseMuscleZoneEntity`, `RoutineVersionEntity`, `PlanAssignmentEntity`, `ProfileEntity`, `WeightRecordEntity`, `SessionEntity`, `SessionExerciseEntity`, `ExerciseSetEntity`, `ExerciseProgressionEntity`, `RotationStateEntity`, `RoutineCurrentVersionEntity`, `WeekDayEntity`, `DailyRoutineOverrideEntity`, `DaySkipEntity`, `TreeStateEntity`, `DeloadEntity`, `DeloadFrozenVersionEntity`, `AlertEntity`.

- **Módulo / Componente:** Seed Data (data.local.seed)
  - **Responsabilidad (SRP):** Prepoblar la base de datos con los datos de inicialización en el primer uso de la app mediante `RoomDatabase.Callback.onCreate()`.
  - **Interfaces Expuestas:** `PrepopulateFacade` — coordina la inserción de todos los datos semilla invocando Seeders temáticos.
  - **Dependencias Internas:** `ExerciseSeeder` (37 ejercicios base y 41 relaciones ejercicio-zona), `BaseDataSeeder` (20 zonas musculares y 23 tipos de equipamiento), `PlanSeeder` (6 rutinas, 6 versiones y 35 asignaciones del plan predeterminado), `WeekDaySeeder` (7 días y su relación con la rutina que les corresponde). `WeekDaySeeder` se ejecuta **después** de `PlanSeeder`: la clave foránea a `routine` exige que las rutinas existan.
  - **Estructuras Atómicas Clave:** Los datos residen en estructuras Kotlin puras — `ExerciseCatalog` (`List<SeedExercise>`), `DefaultPlan` (`List<SeedRoutine>` + `List<SeedAssignment>`) y `DefaultWeekDays` (`List<SeedWeekDay>`) — y cada Seeder se limita a mapearlas a `ContentValues`. Sin dependencia de Android, los datos semilla son verificables por tests JVM.

---

## 4. Trazabilidad Funcional

*Matriz que garantiza la ejecución del código frente a las necesidades del negocio. Mapea Requerimientos Funcionales a los componentes responsables de su implementación.*

- **`RF-01, RF-02, RF-03` (Perfil del Ejecutante):** Lógica de negocio en `CreateProfileUseCase` / `UpdateProfileUseCase`. Persistencia en `ProfileRepositoryImpl` → `ProfileDao` / `WeightRecordDao`. Presentación en `RegisterProfileScreen` / `ProfileScreen` con `ProfileViewModel`.
- **`RF-04, RF-07, RF-61, RF-62` (Diccionario de Ejercicios):** Catálogo base en `ExerciseSeeder` (seed). Creación personalizada en `CreateExerciseUseCase`. Filtros en `GetExercisesFilteredUseCase`. Presentación en `ExerciseDictionaryScreen` / `ExerciseDetailScreen` con `CatalogViewModel`.
- **`RF-05, RF-06, RF-08, RF-63, RF-64, RF-65` (Plan de Entrenamiento):** Gestión del plan en `PlanRepositoryImpl` → `PlanAssignmentDao` / `RoutineVersionDao`. Use Cases: `GetPlanVersionDetailUseCase`, `AssignExerciseToPlanUseCase`, `RemoveExerciseFromPlanUseCase`. Presentación en `TrainingPlanScreen` / `PlanVersionDetailScreen` con `CatalogViewModel`.
- **`RF-09, RF-10, RF-11` (Determinación de sesión y microciclo):** Desde HU-36 la rutina propuesta la determina el **día de la semana**: `GetTodaySessionUseCase` → `SessionRepositoryImpl.getTodaySession` → `WeekDayDao` + `DailyRoutineOverrideDao` + `DailyRoutineRule`. `rotation_state` conserva íntegro su rol de avanzar posición y contar microciclos al cerrar sesión (`CloseSessionUseCase` → `RotationResolver.advanceRotation`), pero deja de indexar la rutina; la posición sigue persistiendo indefinidamente e inmune a ausencias. Reasignación temporal en `SetTemporaryRoutineUseCase` / `ClearTemporaryRoutineUseCase` → `WeekDayRepositoryImpl`. Presentación en `HomeScreen` y `SessionPreviewScreen` con `ReassignRoutineDialog`.
- **`RF-12, RF-13, RF-14, RF-15, RF-16, RF-17` (Registro de Sesión):** Inicio en `StartSessionUseCase`. Registro de series en `RegisterSetUseCase`. Cambio de ejercicio en sesión mediante la alternativa declarada en el slot del plan (`RF-16`, HU-26): `SessionRepositoryImpl.switchAlternativeInSession` → `SessionExerciseDao.switchAlternativeExercise`. Persistencia en `SessionRepositoryImpl` → `SessionDao`, `SessionExerciseDao`, `ExerciseSetDao`. Presentación en `ActiveSessionScreen`, `RegisterSetScreen` con `ActiveSessionViewModel`.
- **`RF-18, RF-19, RF-20, RF-21, RF-59` (Cierre de Sesión):** Protocolo de cierre en `CloseSessionUseCase` — orquesta: `DoubleThresholdRule`, `ProgressionClassificationRule`, `PlateauDetectionRule`, `AccumulatedFatigueRule`, cálculo de tonelaje, actualización de `exercise_progression`, generación de alertas y avance de `rotation_state`. Presentación en `SessionSummaryScreen` con `SessionSummaryViewModel`.
- **`RF-23, RF-24, RF-25, RF-26, RF-27, RF-28, RF-29, RF-30, RF-31, RF-32, RF-33` (Motor de Progresión):** Implementado en `DOM-01 domain.rules`: `DoubleThresholdRule` (RF-25/26), `ProgressionClassificationRule` (RF-23/24), `RegressionDetectionRule` (RF-29), `AccumulatedFatigueRule` (RF-30), `BodyweightProgressionRule` (RF-31), `IsometricProgressionRule` (RF-32/33). Invocados por `CloseSessionUseCase`.
- **`RF-34, RF-35, RF-36, RF-37` (Detección de Mesetas y Descarga):** `PlateauDetectionRule` (RF-34) y `DeloadRecommendationRule` (RF-37) en `DOM-01`. Alertas generadas por `GenerateAlertsUseCase` → `AlertRepositoryImpl` → `AlertDao`. Presentación en `AlertCenterScreen`, `AlertDetailScreen` con `AlertViewModel`.
- **`RF-38, RF-39, RF-40` (Protocolo de Descarga):** `ActivateDeloadUseCase` → `DeloadRepositoryImpl` → `DeloadDao`, `DeloadFrozenVersionDao`. Carga de reinicio calculada en `CloseSessionUseCase` post-descarga. Presentación en `DeloadScreen` con `DeloadViewModel`.
- **`RF-41, RF-42, RF-44, RF-45, RF-46, RF-47, RF-48, RF-49` (KPIs y Métricas):** Use Cases de métricas: `GetProgressionRateUseCase`, `GetTonnageByMuscleGroupUseCase`, `GetRirAverageUseCase`, `GetAdherenceIndexUseCase`, `GetLoadVelocityUseCase`, `GetVolumeDistributionUseCase`. Presentación en `MetricsPanelScreen`, `MuscleVolumeScreen`, `ProgressionTrendScreen` con `MetricsViewModel`.
- **`RF-43` (Estado de Progresión Persistido):** `exercise_progression` actualizado por `CloseSessionUseCase` tras cada sesión. Leído por `GetExerciseHistoryUseCase` → `ExerciseProgressionDao`. Presentación en `ExerciseHistoryScreen` con `HistoryViewModel`.
- **`RF-50, RF-51, RF-52, RF-60` (Historial y Consultas):** `GetSessionHistoryUseCase`, `GetSessionDetailUseCase`, `GetExerciseHistoryUseCase`, `GetTonnageTrendUseCase`. Presentación en `SessionHistoryScreen`, `SessionDetailScreen`, `ExerciseHistoryScreen` con `HistoryViewModel`.
- **`RF-53, RF-54, RF-55, RF-56, RF-57, RF-58` (Sistema de Alertas):** Alertas generadas por `GenerateAlertsUseCase` (invocado tras cada cierre de sesión). Persistencia en `AlertRepositoryImpl` → `AlertDao`. Resolución automática evaluada en el mismo Use Case. Presentación en `AlertCenterScreen`, `AlertDetailScreen`.
- **`HU-37` (Árbol de progreso del entrenamiento) — excepción de alcance declarada:** Reglas puras `TreeHealthRule` y `TreeGrowthStageRule` en `DOM-01`. Estado derivado y persistido por `TreeRepositoryImpl` → `TreeStateDao` + `SessionDao`. Recálculo en los tres momentos por `RecalculateTreeStateUseCase` (cierre de sesión, cambio de día **tras** el barrido, apertura de la pantalla) y tras restaurar un respaldo. Presentación en `TreeScreen` con `TreeViewModel` y en la tarjeta de acceso de `HomeScreen`. **No mapea ningún RF**: es una funcionalidad puramente visual, y la exclusión de `system_definition_document.md` §2.1 —motivación, adherencia y días del calendario como insumos de decisión— **se mantiene intacta** (ver ADR-020).
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

- **Contexto:** 26 vistas con estados complejos requieren un framework declarativo que minimice boilerplate y facilite renderizado basado en estado.
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

- **Contexto:** 26 vistas, 10 flujos funcionales, navegación por tabs con preservación de estado, start destination dinámica (A1 vs B1), flujo contenido de sesión activa (E1-E5 sin escape al Bottom Nav).
- **Decisión:** `MainActivity` única con `NavHost` de Navigation Compose. 10 nested graphs (uno por flujo). Bottom Navigation con `saveState`/`restoreState`.
- **Consecuencias:** 24 rutas tipadas (E4 es `AlertDialog`, no ruta; E3 retirada en HU-34). Nested graphs aíslan back stack por tab. Transiciones especiales con `popUpTo` (A1→B1, E4→E5, J3→B1). Reglas de visibilidad del Bottom Nav evaluadas por ruta actual del `NavController`.

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

### ADR-019: Esquema sin migración durante la beta — reinicio a cargo del ejecutante

- **Contexto:** HU-36 añade dos tablas (`week_day`, `daily_routine_override`) y renombra las seis rutinas del seed para quitarles el día. El renombrado no es migrable sin pisar un posible rename manual del ejecutante, y la historia declara la excepción: *"Beta sin migración: la base de datos se reinicia; los cambios de esquema se validan sobre instalación fresca."* El producto precisó el mecanismo: el reinicio lo hace el ejecutante desinstalando y reinstalando, y la aplicación debe comportarse como si fuera su primera salida a producción.
- **Decisión:** `TensionDatabase.version` sube sin migración y **sin** `fallbackToDestructiveMigration()`. Aplicado a v17 (`week_day`, `daily_routine_override`), a v18 (`day_skip`) y a v19 (`tree_state`, HU-37). Las migraciones históricas `MIGRATION_6_7` … `MIGRATION_15_16` se conservan intactas: describen el esquema de su época. El formato de respaldo sube a `SCHEMA_VERSION = 12`: v10 incorporó `week_day` y `daily_routine_override` —omitirlas perdería la relación día → rutina en cada restauración—, v11 añadió `day_skip` y v12 añade `tree_state`. **A diferencia de los saltos anteriores, v12 acepta también el formato inmediatamente previo (v11)**: el árbol es enteramente derivable del historial, así que restaurar sin él nunca deja un estado inválido — `ImportBackupUseCase` lo reconstruye — y rechazarlo inutilizaría todo respaldo exportado hasta HU-37.
- **Consecuencias:** Excepción documentada a RNF19, limitada a la fase beta. Una base anterior falla al abrir con la excepción de Room, que es la señal de reinstalar; la app nunca borra datos por su cuenta. Los respaldos en formato 9 dejan de poder restaurarse — consistente con que no hay instalación previa que preservar. Ningún test ejerce el arranque en v17 porque no hay migración que probar; lo que se verifica es la semilla sobre instalación fresca (`DefaultWeekDaysTest` y validación manual).

---

### ADR-020: El árbol de entrenamiento como funcionalidad visual aislada

- **Contexto:** `system_definition_document.md` §2.1 mantiene **fuera del sistema** la motivación, el estado anímico y la adherencia del ejecutante, y los días del calendario o la frecuencia de asistencia, como insumos de decisión. HU-37 introduce un árbol que crece con las sesiones acumuladas y se marchita con la inactividad — exactamente el material que esa exclusión aparta. La historia se documenta como **excepción explícita y acotada**, no como ampliación del dominio.
- **Decisión:** El árbol es una funcionalidad **puramente visual** con dependencia **unidireccional**: lee del historial de sesiones y no escribe nada que ningún otro componente consuma. Se materializa en tres fronteras verificables:
  1. **Contrato propio.** `TreeRepository` vive separado de `SessionRepository` en lugar de añadirle dos métodos. No rompe el aislamiento colgarlo del contrato del motor de decisión — lo vuelve indistinguible.
  2. **Sin relaciones en el esquema.** `tree_state` no declara ninguna clave foránea. La dependencia con `session` es de cálculo, no de integridad.
  3. **Enganches fuera del repositorio de sesión.** El recálculo se invoca desde `CloseSessionUseCase`, `ImportBackupUseCase` y `MainViewModel`. **`SessionRepositoryImpl.closeSession` no se modificó.**
- **Consecuencias:** Ningún componente del motor de decisión —prescripción de carga, Doble Umbral, meseta, regresión, fatiga, protocolo de descarga, rotación cíclica— lee el puntaje ni la etapa; el árbol **no genera alertas** ni altera `ROUTINE_INACTIVITY` o `LOW_ADHERENCE`; la adherencia semanal y el resto de KPIs se calculan igual que antes. El corte de salud a los 14 días **coincide** con el umbral de crisis de `ROUTINE_INACTIVITY` por alineación semántica deliberada, no por acoplamiento: son medidas distintas — la alerta mide inactividad **por rutina** y el árbol la mide **global**. El recálculo es *best-effort* en el cierre de sesión y en el cambio de día: un árbol desactualizado es un defecto visual, mientras que una excepción propagada convertiría una sesión ya cerrada en un error. La verificación del aislamiento es que ninguno de esos archivos aparezca en el diff de la historia.

---

### Decisiones de Dominio con Impacto Técnico

*Decisiones de modelado de dominio que influyen directamente en la implementación.*

| ID | Decisión de Dominio | Impacto Técnico |
|----|---------------------|-----------------|
| D-01 | Rotación cíclica agnóstica al calendario | `rotation_state.microcycle_position` persiste indefinidamente e inmune a ausencias del ejecutante. **Revisada en HU-36:** la rotación deja de determinar *qué rutina* toca y conserva solo el avance de posición y el conteo de microciclos. La determinación pasa a depender del calendario (ver D-09). |
| D-09 | El día de la semana es una entidad relacionada con la rutina | Tabla `week_day` de 7 filas con `routine_id` anulable; el domingo queda registrado sin rutina. `LocalDate.now().dayOfWeek.value` indexa la fila directamente. El nombre de la rutina deja de llevar el día. |
| D-11 | La relación permanente día → rutina es editable, y una rutina puede ocupar varios días | El día es el dueño de la relación (`week_day.routine_id`): apunta a una rutina o a ninguna. De ahí que la edición sea multi-selección de días sobre la rutina, y que asignar un día ocupado lo **mueva** en lugar de duplicarlo. `WeekDayAssignmentRule` concentra esa decisión; `UpdateRoutineWeekDaysUseCase` la rechaza durante una descarga activa, porque dejar sin días a una rutina congelada impediría que su sesión se propusiera y el ciclo no cerraría nunca. Resuelve la consecuencia que HU-36 dejó abierta: una rutina creada por el ejecutante ya puede tomar un día y proponerse sola. |
| D-13 | Un día se resuelve una sola vez | Un día queda resuelto al cerrar una sesión con esa fecha o al registrarlo en `day_skip`. Resuelto, deja de proponerse: la interfaz informa qué toca el siguiente día con rutina (`NextTrainingDayRule`, saltando los de descanso) con el inicio deshabilitado hasta que ese día llegue. Es lo que impide ejecutar la misma sesión varias veces en la misma jornada, y la razón de que la reasignación temporal tampoco se ofrezca entonces — sería la puerta trasera para una segunda sesión. |
| D-14 | Cancelar el día y cerrar la sesión son actos distintos | **Cerrar** exige al menos una serie: da por terminado lo entrenado. **Cancelar** (`B1-T5`) resuelve el día sin dejar sesión alguna — descarta la que estuviera en curso sin series — y deja de estar disponible en cuanto hay una serie registrada, momento en que la única salida es cerrar como `INCOMPLETE`. El mecanismo anterior confundía ambos: iniciar y cerrar vacía dejaba una fila que aparecía en el historial, contaba en `countSessionsInWeek` (subiendo la adherencia) y actualizaba `getLastSessionDateByRoutine` (silenciando `ROUTINE_INACTIVITY`), premiando en las métricas lo contrario de lo ocurrido. La frontera es el primer `exercise_set`. |
| D-15 | El día anterior se cierra solo, y sin registro propio | Una sesión `IN_PROGRESS` con `date` anterior a hoy se resuelve automáticamente: cerrada como `INCOMPLETE` si tiene series, descartada si no. Corre desde `MainViewModel` sobre `CurrentDateProvider.dateFlow()`, cuya primera emisión cubre el arranque y las siguientes el cambio de día con la app abierta — **con la app cerrada no corre nada**, y no se añade un worker en segundo plano porque el resultado observable sería el mismo. El día no entrenado no genera fila alguna: la ausencia de sesión ya lo expresa para el historial, la adherencia y la alerta de inactividad. |
| D-16 | La estatura del árbol no retrocede; la salud sí | Las dos dimensiones son ortogonales: la etapa se deriva del **total de sesiones cerradas** y la salud de los **días desde la última**. Faltar marchita el árbol, no lo encoge. La invariante no exige memoria: al ser función monótona del conteo, la etapa solo puede subir mientras se entrene. **No se aplica un máximo pegajoso a la etapa persistida** — lo haría contradecir CA-37.09, que exige que tras restaurar un respaldo el árbol quede en el estado que corresponde al historial restaurado, también si es menor. `day_skip` **no protege al árbol**: omitir un día lo marchita igual que no abrir la app, coherente con que tampoco cuente como adherencia ni silencie `ROUTINE_INACTIVITY`. |
| D-12 | El ejercicio de una versión se cuenta por slot, no por asignación | Un slot dual son dos asignaciones y **un** ejercicio de la sesión: o se hace uno o se hace el otro. `RoutineVersionDao` cuenta `COUNT(DISTINCT pa.slot)`. Alinea las cifras del plan con lo que el resto del sistema ya hacía: el preview agrupa por slot, `startSession` crea un `session_exercise` por slot y el protocolo de descarga mide `COUNT(DISTINCT pa.slot)`. |
| D-10 | La reasignación de rutina es temporal y de un día | `daily_routine_override` de fila única con `date`. La reversión es semántica —la fila solo se honra cuando su fecha es hoy—, no un borrado programado: la reasignación caduca al cambiar el día aunque la app no se abra, y sobrevive a un segundo inicio de sesión el mismo día. La reasignación se agota en la determinación y nunca escribe en `rotation_state`. |
| D-02 | Cambio de ejercicio en sesión solo con ejercicio "No Iniciado" (0 series) | El control de intercambio de alternativa en E1 se muestra/oculta según `session_exercise` sin series (`COUNT(exercise_set) = 0`). La sustitución por grupo muscular que originó esta decisión fue **retirada en HU-34**; la restricción sobrevive aplicada al único mecanismo vigente, las alternativas por slot de HU-26. |
| D-03 | Sesiones cerradas son inmutables | Sin endpoints ni Use Cases de edición post-cierre. `session.status` = COMPLETED/INCOMPLETE congela todos los datos asociados. No se implementan funciones de update retroactivo en DAOs de sesión. |
| D-04 | E4 es un diálogo, no una pantalla de navegación | `AlertDialog` Compose gestionado por estado del ViewModel (`showCloseDialog: Boolean`). No es una ruta en el `NavHost`. No entra al back stack. |
| D-05 | Objetivo de frecuencia semanal con default 6 sesiones | `profile.weekly_frequency` inicializado en 6 (rango 4-6). No se solicita en onboarding (A1). Configurable en J1. Denominador del KPI de Adherencia (RF-47). |
| D-06 | Comparación de progresión contra último registro del mismo ejercicio, independiente de versión | El query de comparación en `GetLastExerciseSessionUseCase` busca la última `session_exercise` por `exercise_id` sin filtrar `routine_version_id`. |
| D-07 | Incremento diferenciado: +2.5 Kg tren superior, +5 Kg tren inferior | Constantes en `DoubleThresholdRule`: `LOAD_INCREMENT_UPPER = 2.5f`, `LOAD_INCREMENT_LOWER = 5.0f`. Determinadas por zona muscular del ejercicio via `exercise_muscle_zone`. |
| D-08 | Descarga dura 1 microciclo completo sin rotar versiones | `deload.status = 'ACTIVE'` durante N sesiones (N = total de rutinas). Las versiones de cada rutina se congelan en `deload_frozen_version`. No cambian hasta `deload.status = 'COMPLETED'`. |
