# Estándares y Convenciones de Código — Tension

## 1. Propósito y Alcance General

Este documento establece las reglas obligatorias y las prácticas recomendadas para la escritura de código fuente en el proyecto Tension (app Android de registro de entrenamiento, Kotlin + Jetpack Compose). Su propósito es garantizar la legibilidad, la mantenibilidad y la uniformidad del código, haciendo explícita la regla de dependencia arquitectónica MVVM + Domain Layer. Este manual es la referencia principal durante las revisiones de código.

**Stack:** Kotlin 2.0.21 · Android minSdk 26 · Jetpack Compose (Material 3) · Room ORM · Hilt DI · 100 % offline

**Fuentes:** `Arquitectura Técnica.md §5`, `ADR.md`, `Especificación Visual.md`

---

## 2. Estándares de Nomenclatura (Naming Conventions)

### 2.1. Idioma por elemento

| Elemento | Idioma | Ejemplo |
|----------|--------|---------|
| Código fuente (clases, funciones, variables, paquetes) | Inglés | `ActiveSessionScreen`, `registerSet()`, `exerciseId` |
| Comentarios y KDoc | Inglés | `/** Calculates the prescribed load ... */` |
| Datos de dominio y seed data (valores en BD) | Español | `"Sentadilla Búlgara"`, `"Kilogramo"`, `"Pecho"` |
| Strings visibles al usuario (UI) | Español | `"Iniciar sesión"`, `"Ingrese un valor válido"` |
| Rutas de navegación | Inglés, kebab-case | `exercise-detail`, `session-history` |
| Documentación del proyecto (Markdown) | Español | Archivos en `docs/` |
| Mensajes de validación de dominio | Inglés (uso interno, no visible al usuario) | `"Weight must be >= 0"` |

### 2.2. Paquetes

- Minúsculas, una sola palabra cuando sea posible.
- Sin guiones bajos ni camelCase.
- Ejemplos: `ui.session`, `domain.usecase`, `data.local.dao`.

### 2.3. Clases y objetos

| Tipo | Convención | Ejemplo |
|------|-----------|---------|
| Screen (Composable de nivel pantalla) | `{Feature}Screen` | `HomeScreen`, `RegisterSetScreen` |
| ViewModel | `{Feature}ViewModel` | `HomeViewModel`, `ActiveSessionViewModel` |
| Use Case | `{Acción}{Entidad}UseCase` | `RegisterSetUseCase`, `DetectPlateauUseCase` |
| Repository (interfaz) | `{Entidad}Repository` | `SessionRepository`, `AlertRepository` |
| Repository (implementación) | `{Entidad}RepositoryImpl` | `SessionRepositoryImpl` |
| Entity (Room) | `{Entidad}Entity` | `SessionEntity`, `ExerciseSetEntity` |
| DAO | `{Entidad}Dao` | `SessionDao`, `ExerciseSetDao` |
| UiState | `{Feature}UiState` | `HomeUiState`, `ActiveSessionUiState` |
| Componente reutilizable | Nombre descriptivo sin sufijo forzado | `ProgressionIndicator`, `RirSelector` |
| Módulo Hilt | `{Ámbito}Module` | `DatabaseModule`, `RepositoryModule` |
| Regla del motor de reglas | `{Nombre}Rule` | `DoubleThresholdRule`, `PlateauDetectionRule` |
| Seeder (prepopulación) | `{Entidad}Seeder` | `ExerciseSeeder`, `MuscleZoneSeeder` |
| TypeConverter | `Converters` (singular por archivo) | `Converters` |

### 2.4. Funciones

| Tipo | Convención | Ejemplo |
|------|-----------|---------|
| Composable de pantalla | `PascalCase` (estándar Compose) | `ActiveSessionScreen()` |
| Composable de componente | `PascalCase` | `ProgressionIndicator()` |
| Use Case invocable | `operator fun invoke()` | `operator fun invoke(sessionId: Long): Flow<Session>` |
| Funciones de ViewModel | `camelCase`, verbo en imperativo | `startSession()`, `registerSet()`, `closeSession()` |
| Funciones de DAO | `camelCase`, prefijo por operación | `insert()`, `getById()`, `getAllByRoutine()`, `updateStatus()`, `deleteById()` |
| Funciones suspend | Marcadas con `suspend` — no usar sufijo "Async" | `suspend fun getById(id: Long): Entity?` |
| Callbacks / lambdas en Composables | Prefijo `on` | `onStartSession`, `onRegisterSet`, `onNavigateBack` |

### 2.5. Variables y propiedades

| Tipo | Convención | Ejemplo |
|------|-----------|---------|
| StateFlow interno (ViewModel) | Prefijo `_`, tipo `MutableStateFlow` | `private val _uiState = MutableStateFlow(UiState())` |
| StateFlow expuesto (ViewModel) | Sin prefijo, tipo `StateFlow` | `val uiState: StateFlow<UiState> = _uiState.asStateFlow()` |
| SharedFlow para eventos | Prefijo `_` interno, `SharedFlow` expuesto | `val events: SharedFlow<Event>` |
| Argumentos de navegación | camelCase | `exerciseId`, `sessionId`, `routineVersionId` |
| Constantes | `SCREAMING_SNAKE_CASE` en `companion object` u `object` | `LOAD_INCREMENT_UPPER = 2.5` |
| Parámetros de Composable | camelCase, descriptivos | `exerciseName: String`, `isLoading: Boolean`, `onConfirm: () -> Unit` |

---

## 3. Formato y Sintaxis del Código

### 3.1. Indentación y espaciado

| Aspecto | Estándar |
|---------|---------|
| Formato base | Kotlin Official Style Guide + ktlint |
| Indentación | 4 espacios |
| Longitud máxima de línea | 120 caracteres |
| Llaves | Estilo K&R (apertura en la misma línea) |
| Trailing commas | Habilitadas — cada parámetro o elemento en su propia línea si hay más de 2 |
| Expresiones `when` | Preferidas sobre cadenas `if-else` cuando hay 3+ ramas |

### 3.2. Imports y dependencias

| Regla | Detalle |
|-------|---------|
| Sin wildcards | Nunca `import com.example.*` |
| Ordenación | Automática por ktlint |
| Imports de Android en Domain | **Prohibido** — la capa Domain no importa `android.*`, `androidx.*` ni `com.google.*` |

### 3.3. Null safety

| Regla | Aplicación |
|-------|-----------|
| Usar operadores de safe-call | `?.`, `?:`, `let`, `require`, `checkNotNull` |
| Operador `!!` | Solo permitido en tests |
| Modelos de datos | `data class` preferida para UiState, DTOs, entities |
| Estados finitos | `sealed class` / `sealed interface` para `UiState`, `Event`, `Result`, `Action` |

### 3.4. Estructura de un Screen Composable

Cada Screen Composable sigue esta estructura obligatoria:

```kotlin
@Composable
fun FeatureScreen(
    viewModel: FeatureViewModel = hiltViewModel(),
    onNavigateToX: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Manejo de eventos one-shot (navegación, snackbar)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) { /* ... */ }
        }
    }

    // Contenido según estado
    when (val state = uiState) {
        is Loading -> LoadingIndicator()
        is Success -> FeatureContent(state.data, onAction = viewModel::onAction)
        is Error -> ErrorMessage(state.message)
    }
}
```

**Principios obligatorios:**
- El ViewModel se obtiene con `hiltViewModel()` — nunca se instancia manualmente.
- Las lambdas de navegación (`onNavigateToX`, `onNavigateBack`) son parámetros — el Screen no conoce el `NavController`.
- El estado se recolecta con `collectAsStateWithLifecycle()` para respetar el ciclo de vida.
- El contenido visual se extrae a una función `@Composable` interna (`FeatureContent`) que recibe datos inmutables y lambdas de acción.

### 3.5. Estructura de un ViewModel

```kotlin
@HiltViewModel
class FeatureViewModel @Inject constructor(
    private val someUseCase: SomeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<FeatureUiState>(FeatureUiState.Loading)
    val uiState: StateFlow<FeatureUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<FeatureEvent>()
    val events: SharedFlow<FeatureEvent> = _events.asSharedFlow()

    init {
        loadInitialData()
    }

    fun onAction(action: FeatureAction) {
        viewModelScope.launch {
            // Delegar al Use Case correspondiente
        }
    }
}
```

**Principios obligatorios:**
- `@HiltViewModel` + `@Inject constructor` — dependencias inyectadas automáticamente.
- Estado como `MutableStateFlow` privado, expuesto como `StateFlow` inmutable.
- Eventos one-shot como `MutableSharedFlow` privado, expuesto como `SharedFlow`.
- No referencia `Context`, `Activity` ni ninguna clase de la UI.

### 3.6. Strings y recursos

| Tipo | Ubicación | Ejemplo |
|------|-----------|---------|
| Textos visibles al usuario | `res/values/strings.xml` (español) | `<string name="start_session">Iniciar sesión</string>` |
| Mensajes de error de UI | `res/values/strings.xml` | `<string name="error_invalid_weight">Ingrese un peso válido</string>` |
| Mensajes de validación de dominio | Constantes en capa Domain (Kotlin puro) | `"Weight must be >= 0"` |
| Nombres de tabla/columna Room | `companion object` o `object` de cada Entity | `const val TABLE_NAME = "session"` |
| Datos de seed | En código dentro de los Seeders | `"Sentadilla Búlgara"`, `"Pecho"` |

---

## 4. Reglas de Implementación Arquitectónica

### 4.1. Dirección de dependencias (ADR-05)

```
UI → ViewModel → Domain → Data → Room/SQLite
```

| Regla | Consecuencia |
|-------|-------------|
| Cada capa solo depende de la capa inmediatamente inferior | Ninguna capa inferior referencia a una superior |
| Domain **no importa nada de Android** | Ningún `android.*`, `androidx.*` ni `com.google.*` en `domain/` |
| Repository interface en Domain, implementación en Data | Inversión de dependencia — los Use Cases son testeables sin Room |
| ViewModel no accede directamente a DAOs | Toda operación de datos pasa por Use Cases → Repositories |
| Screen no ejecuta lógica de negocio | Las validaciones de dominio viven en Use Cases, no en Composables |

**Estructura de paquetes (ADR-08):**

```
com.estebancoloradogonzalez.tension/
├── ui/           ← Screens (@Composable) + ViewModels (@HiltViewModel) por feature
├── domain/       ← Use Cases · Motor de Reglas (R1-R7) · Repository interfaces
├── data/         ← Repository impls · DAOs · Entities · Seed
└── di/           ← Módulos Hilt (DatabaseModule, RepositoryModule)
```

### 4.2. Gestión de estados y mutabilidad (ADR-09)

| Patrón | Uso | Ejemplo |
|--------|-----|---------|
| `MutableStateFlow` (privado) → `StateFlow` (público) | Estado de UI persistente | `val uiState: StateFlow<FeatureUiState>` |
| `MutableSharedFlow` (privado) → `SharedFlow` (público) | Eventos one-shot (navegación, snackbar) | `val events: SharedFlow<FeatureEvent>` |
| `collectAsStateWithLifecycle()` | Recolección lifecycle-aware en Composables | Se detiene cuando el lifecycle no está en `STARTED` |
| `Flow<T>` en Domain/Data | Streams reactivos en las capas inferiores | Room emite `Flow<T>` → ViewModel transforma en `StateFlow` |

**Prohibido:**
- `GlobalScope` o `CoroutineScope` manuales — toda coroutine en un scope con lifecycle definido.
- `LiveData` en la capa Domain — no es Kotlin puro.
- `mutableStateOf` en ViewModels — no testeable fuera de Compose.

### 4.3. Coroutines y threading

| Regla | Detalle |
|-------|---------|
| Scope del ViewModel | `viewModelScope` — se cancela automáticamente al destruirse |
| Dispatcher de IO | Repositories/DAOs usan `Dispatchers.IO` internamente. El ViewModel no selecciona dispatcher explícitamente |
| Main safety | Las funciones suspend de Use Cases y Repositories son main-safe: `withContext(Dispatchers.IO)` cuando acceden a Room |
| Paralelismo en ViewModel | `async { } + awaitAll()` dentro de `viewModelScope.launch` |
| Cancelación estructurada | Sin `GlobalScope` ni coroutines sin scope — siempre dentro de un scope con lifecycle |

### 4.4. Gestión de errores por capa

| Capa | Estrategia |
|------|-----------|
| **DAO** | Room lanza excepciones SQLite — se propagan al Repository sin capturar |
| **Repository** | Captura excepciones de IO, las transforma en `Result<T>` (`Success` / `Error`). No lanza al Use Case |
| **Use Case** | Recibe `Result<T>`, aplica lógica de negocio, retorna `Result<T>` al ViewModel. Validaciones producen `Result.Error` con mensaje descriptivo |
| **ViewModel** | Mapea `Result<T>` a estados de UI (`UiState.Error(message)`). Todo error se refleja como estado observable — nunca se lanza excepción no capturada |
| **UI** | Renderiza el estado de error. Muestra mensajes via Snackbar o `ErrorMessage` component. La lógica de retry se delega al ViewModel |

### 4.5. Motor de reglas (ADR-06)

Las Reglas 1-7 del Manifiesto de Dominio Sistémico se implementan como **funciones puras** en `domain/rules/`:
- Cada regla es un archivo independiente.
- Reciben datos primitivos o modelos de dominio — no acceden a Room ni a ningún componente Android.
- Invocadas por los Use Cases de cierre de sesión.
- Testeables con JUnit 4 sin emulador.

### 4.6. Inyección de dependencias (ADR-04)

| Componente | Anotación | Scope |
|-----------|-----------|-------|
| `TensionApplication` | `@HiltAndroidApp` | — |
| `MainActivity` | `@AndroidEntryPoint` | — |
| ViewModel | `@HiltViewModel` + `@Inject constructor` | `ViewModelComponent` |
| `TensionDatabase` y DAOs | `@Singleton` en `DatabaseModule` | `SingletonComponent` |
| Repository implementations | binding en `RepositoryModule` | `SingletonComponent` |
| Composable Screen | `hiltViewModel()` | scoped al destino de navegación |

**2 módulos Hilt obligatorios:** `DatabaseModule` (provee la BD y los DAOs) y `RepositoryModule` (vincula interfaces con implementaciones).

### 4.7. Prepopulación de datos (ADR-11)

- Usar `RoomDatabase.Callback.onCreate()` con patrón Facade (`PrepopulateFacade`).
- Cada Seeder encapsula las inserciones de sus entidades.
- La prepopulación se ejecuta en una transacción atómica.
- El seed data es código Kotlin versionado en Git — auditable y trazable.
- Los ejercicios creados por el ejecutante y sus asignaciones al plan se persisten via CRUD en runtime — no son seed data.

---

## 5. Estándares de Pruebas Automatizadas

### 5.1. Frameworks (ADR-18)

| Framework | Uso | Ubicación |
|-----------|-----|-----------|
| JUnit 4 | Tests unitarios del motor de reglas (R1-R7) y Use Cases | `test/` — JVM local, sin emulador |
| Compose UI Test JUnit4 | Tests de UI instrumentados para pantallas Compose | `androidTest/` |
| Espresso Core | Tests de integración Android | `androidTest/` |

### 5.2. Estructura de pruebas

Patrón **Given / When / Then** (separación explícita de preparación, acción y validación):

```kotlin
@Test
fun `given sufficient sets above threshold, when rule is evaluated, then progression is prescribed`() {
    // Given
    val history = buildSetHistory(setsAbove = 3, threshold = 2)

    // When
    val result = DoubleThresholdRule.evaluate(history)

    // Then
    assertEquals(Prescription.INCREASE_LOAD, result)
}
```

### 5.3. Nomenclatura de casos de prueba

Formato: `` `given <condición>, when <acción>, then <resultado esperado>` ``

- Describe el escenario en el nombre — sin ambigüedad.
- Usa backticks para permitir espacios en el nombre de la función de test.

### 5.4. Cobertura mínima por capa

| Capa | Cobertura requerida | Estrategia de mock |
|------|--------------------|--------------------|
| Motor de Reglas (R1-R7) | Alta — casos exitoso, frontera, datos insuficientes por regla | Sin mocks — funciones puras |
| Use Cases | Media-alta | Repository interfaces mockeadas |
| ViewModels | Media | `TestCoroutineDispatcher` + Use Cases mockeados |
| UI | Flows críticos | Compose UI Test instrumentado |

### 5.5. Reglas adicionales

- Los tests del motor de reglas residen en `test/` (no `androidTest/`) — se ejecutan en JVM sin emulador (RNF30).
- Cada regla (R1-R7) tiene su propio archivo de tests.
- Los ViewModels se testean con `TestCoroutineDispatcher` y mocks de Use Cases.
- `!!` está permitido únicamente en código de test.

---

## 6. Estándares Visuales de Implementación

### 6.1. Tema y colores (ADR-12)

- `TensionTheme` aplica `lightColorScheme()` o `darkColorScheme()` según `isSystemInDarkTheme()`.
- Color seed: `#8B1A1A` (Rojo Imperio Romano). **No usar** `dynamicLightColorScheme()` / `dynamicDarkColorScheme()`.
- Los colores semánticos de dominio (progresión, mantenimiento, regresión, alertas, descarga) se definen como extensiones del tema fuera del esquema M3 estándar.

| Señal | Color claro | Color oscuro |
|-------|-------------|--------------|
| Progresión positiva | `#2E7D32` | `#81C784` |
| Mantenimiento | `#8D6E00` | `#FFD54F` |
| Regresión | `#C62828` | `#EF9A9A` |
| Descarga activa | `#1565C0` | `#64B5F6` |

### 6.2. Tipografía (ADR-13)

- Fuente del sistema (Roboto) para todas las escalas — sin fuentes custom.
- `Display Large`, `Display Medium`, `Display Small` **no se usan** — definir explícitamente para evitar uso accidental.
- La escala tipográfica se define en `Type.kt` usando pesos y tamaños de M3 sin cambiar la familia de fuentes.

### 6.3. Dimensiones y touch targets (RNF06)

| Regla | Valor |
|-------|-------|
| Tamaño mínimo interactivo | 48 × 48 dp |
| Margen horizontal de pantalla | 16 dp |
| Separación entre secciones | 24 dp |
| Separación entre elementos | 16 dp |
| Padding interno de cards | 16 dp |
| Corner radius de cards | 12 dp |
| Corner radius de botones | 24 dp (Filled / Outlined Button M3) |
| Corner radius de OutlinedTextField | 4 dp |

### 6.4. Restricciones de plataforma (ADR-16, ADR-17)

- Solo modo portrait — `android:screenOrientation="portrait"` en `AndroidManifest.xml`.
- Sin layouts en `res/layout-land/` ni `res/layout-sw600dp/`.
- Toda la UI en español — un único `res/values/strings.xml` sin carpetas `values-en/`.
- Señales de progresión/estado nunca solo por color — siempre acompañadas de ícono (RNF05).

---

## 7. Decisiones Arquitectónicas de Referencia

| ADR | Decisión | Estado |
|-----|----------|--------|
| ADR-01 | Kotlin 2.0.21 como lenguaje único. JVM target 11 | Adoptada |
| ADR-02 | Jetpack Compose + Material 3 para toda la UI | Adoptada |
| ADR-03 | Room (Runtime + KTX + KSP) como única capa de persistencia | Adoptada |
| ADR-04 | Hilt Android + Hilt Navigation Compose | Adoptada |
| ADR-05 | MVVM con 4 capas, regla de dependencia unidireccional | Adoptada |
| ADR-06 | Motor de reglas en Kotlin puro sin dependencias Android | Adoptada |
| ADR-07 | Single Activity + Navigation Compose (10 nested graphs) | Adoptada |
| ADR-08 | Layer-first con agrupación por feature dentro de cada capa | Adoptada |
| ADR-09 | `StateFlow` (estado UI) + `SharedFlow` (eventos one-shot) | Adoptada |
| ADR-10 | JSON con metadatos de versión como formato de backup | Adoptada |
| ADR-11 | Prepopulación con `RoomDatabase.Callback.onCreate()` + Facade | Adoptada |
| ADR-12 | Esquema de color propio seed `#8B1A1A` — sin Material You dinámico | Adoptada |
| ADR-13 | Tipografía del sistema (Roboto) — sin fuentes custom | Adoptada |
| ADR-14 | Distribución APK firmado — sin Google Play Store | Adoptada |
| ADR-15 | Base de datos sin cifrado | Adoptada |
| ADR-16 | Solo modo portrait — sin landscape ni tablets | Adoptada |
| ADR-17 | Interfaz monoidioma en español | Adoptada |
| ADR-18 | JUnit 4 para testing del motor de reglas y Use Cases | Adoptada |
