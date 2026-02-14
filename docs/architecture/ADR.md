# Decisiones Arquitectónicas (ADR) — Tension

---

## 1. Propósito

Este documento registra las decisiones técnicas clave del proyecto Tension. Cada decisión incluye contexto, alternativas evaluadas, decisión tomada y consecuencias. El formato es compacto y orientado a consulta rápida durante la implementación.

**Convención de estado:**

- **Adoptada** — Decisión vigente y aplicable.
- **Sustituida por ADR-XX** — Decisión que fue reemplazada (se mantiene como registro histórico).

---

## 2. Índice de Decisiones

| ADR | Título | Estado |
|-----|--------|--------|
| ADR-01 | Kotlin como lenguaje principal | Adoptada |
| ADR-02 | Jetpack Compose para la UI | Adoptada |
| ADR-03 | Room como ORM de persistencia local | Adoptada |
| ADR-04 | Hilt como framework de inyección de dependencias | Adoptada |
| ADR-05 | MVVM con capa Domain explícita | Adoptada |
| ADR-06 | Motor de reglas como Kotlin puro sin dependencias Android | Adoptada |
| ADR-07 | Single Activity con Navigation Compose | Adoptada |
| ADR-08 | Estructura de paquetes layer-first con agrupación por feature | Adoptada |
| ADR-09 | StateFlow + SharedFlow para gestión de estado reactivo | Adoptada |
| ADR-10 | JSON como formato de backup | Adoptada |
| ADR-11 | Prepopulación de datos con RoomDatabase.Callback | Adoptada |
| ADR-12 | Esquema de color propio derivado de seed — sin Material You dinámico | Adoptada |
| ADR-13 | Tipografía del sistema (Roboto) sin fuentes custom | Adoptada |
| ADR-14 | Distribución como APK firmado sin Google Play Store | Adoptada |
| ADR-15 | Base de datos sin cifrado | Adoptada |
| ADR-16 | Solo modo portrait — sin soporte landscape ni tablets | Adoptada |
| ADR-17 | Interfaz monoidioma en español | Adoptada |
| ADR-18 | JUnit 4 para testing del motor de reglas | Adoptada |

---

## 3. Registro de Decisiones

---

### ADR-01 — Kotlin como lenguaje principal

**Fecha:** 2026-02-11
**Estado:** Adoptada
**RNF:** RNF33

**Contexto:**
El proyecto requiere un lenguaje para desarrollo Android nativo con soporte de primera clase para coroutines (operaciones asíncronas de IO sobre Room), null safety (evitar NPE en datos de sesión), y extension functions para mantener código conciso.

**Alternativas consideradas:**

| Alternativa | Pros | Contras |
|-------------|------|---------|
| **Kotlin** | Lenguaje oficial de Android, null safety en el compilador, coroutines nativas, interoperabilidad Compose obligatoria | — |
| Java | Amplia base de conocimiento | Sin soporte de Compose, verboso, null safety solo via anotaciones, coroutines requieren librería externa |
| Kotlin Multiplatform | Código compartido entre plataformas | Complejidad innecesaria — la app es exclusivamente Android |

**Decisión:** Kotlin 2.0.21 como lenguaje único del proyecto. JVM target 11.

**Consecuencias:**
- Acceso a todas las APIs modernas de Android y Jetpack.
- Compose requiere Kotlin obligatoriamente — no hay alternativa.
- El motor de reglas se escribe en Kotlin puro, testeable sin emulador.
- Kotlin 2.0+ habilita el nuevo compilador de Compose (K2) para mejor rendimiento de compilación.

---

### ADR-02 — Jetpack Compose para la UI

**Fecha:** 2026-02-11
**Estado:** Adoptada
**RNF:** RNF34

**Contexto:**
La app tiene 26 vistas con estados complejos (sesión activa con progreso por ejercicio, indicadores de progresión, alertas condicionales). Se necesita un framework de UI que facilite la renderización basada en estado y reduzca el boilerplate.

**Alternativas consideradas:**

| Alternativa | Pros | Contras |
|-------------|------|---------|
| **Jetpack Compose** | Declarativo, recomposición automática por estado, integración nativa con Material 3, previews en IDE, menos código que XML | Curva de aprendizaje diferente al sistema de vistas |
| XML + ViewBinding | Maduro, amplia documentación | Verboso, gestión manual de estado, Material 3 limitado, sin recomposición automática |
| XML + Data Binding | Binding declarativo en XML | Complejidad de expresiones en XML, debugging difícil, deprecación progresiva |

**Decisión:** Jetpack Compose vía BOM + Compose Material 3 para toda la interfaz de usuario.

**Consecuencias:**
- Toda la UI es funciones `@Composable` — no hay layouts XML, Fragments ni Views.
- Navigation Compose reemplaza Navigation Component XML.
- Material 3 provee los componentes del sistema de diseño directamente (NavigationBar, TopAppBar, Card, TextField, etc.).
- El estado fluye unidireccionalmente: `StateFlow<UiState>` → Composable → recomposición automática.
- Testing de UI con Compose UI Test JUnit4 en lugar de Espresso puro.

---

### ADR-03 — Room como ORM de persistencia local

**Fecha:** 2026-02-11
**Estado:** Adoptada
**RNF:** RNF14, RNF35

**Contexto:**
La app es 100% offline (RNF09) con 16 entidades en el Modelo de Datos, relaciones complejas (ForeignKey entre sesiones, ejercicios, series, progresión) y queries que requieren joins multi-tabla (tonelaje por grupo muscular, historial de ejercicio con clasificación de progresión). Los datos crecen indefinidamente con cada sesión registrada.

**Alternativas consideradas:**

| Alternativa | Pros | Contras |
|-------------|------|---------|
| **Room** | ORM oficial de Android, compile-time query verification, integración nativa con Flow y coroutines, migraciones automáticas (RNF19), transacciones atómicas (RNF11) | Requiere procesador KSP en tiempo de compilación |
| SQLDelight | Queries SQL-first, verificación en compilación | Menos integración con el ecosistema Jetpack, comunidad más pequeña |
| Realm | No-SQL, API reactiva | Schema diferente al modelo relacional requerido, migración compleja, licencia |
| SharedPreferences / DataStore | Simple para key-value | No soporta 16 entidades relacionadas ni queries complejas |
| SQLite directo (sin ORM) | Control total | Sin verificación de queries en compilación, boilerplate masivo, gestión manual de cursores |

**Decisión:** Room (Runtime + KTX + Compiler vía KSP) como única capa de persistencia.

**Consecuencias:**
- Las 16 entidades del Modelo de Datos mapean directamente a `@Entity` con `@PrimaryKey`, `@ForeignKey`, `@Index`.
- Queries reactivas con `Flow<T>` para actualización automática de UI.
- Funciones `suspend` para escritura (insert, update, delete).
- TypeConverters para tipos no nativos (String ↔ LocalDate).
- Migraciones de esquema automáticas con `Migration` objects (RNF19).
- Transacciones atómicas con `@Transaction` para operaciones multi-tabla como cierre de sesión.
- El compilador KSP valida todas las queries SQL en tiempo de compilación — errores se detectan antes de runtime.

---

### ADR-04 — Hilt como framework de inyección de dependencias

**Fecha:** 2026-02-11
**Estado:** Adoptada
**RNF:** RNF32

**Contexto:**
La app tiene un grafo de dependencias profundo: `TensionDatabase` → DAOs → Repository Implementations → Use Cases → ViewModels. Sin inyección de dependencias, cada ViewModel requeriría instanciar manualmente toda la cadena. Además, el testing requiere poder sustituir implementaciones reales por mocks.

**Alternativas consideradas:**

| Alternativa | Pros | Contras |
|-------------|------|---------|
| **Hilt** | Basado en Dagger (compile-time), integración oficial con ViewModel y Navigation Compose (`hiltViewModel()`), scopes automáticos por componente | Anotaciones generan código — incrementa tiempo de compilación |
| Dagger puro | Máximo control, compile-time DI | Configuración manual extensa (Components, Modules, Subcomponents), no tiene `hiltViewModel()` |
| Koin | Simplicidad, DSL de Kotlin | Runtime DI (errores en runtime, no en compilación), sin verificación de grafo en build |
| Manual DI | Sin librería externa | Inviable para un grafo de 16 DAOs + 9 Repositories + N Use Cases + N ViewModels |

**Decisión:** Hilt Android + Hilt Navigation Compose.

**Consecuencias:**
- `@HiltAndroidApp` en `TensionApplication`.
- `@AndroidEntryPoint` en `MainActivity`.
- `@HiltViewModel` + `@Inject constructor` en cada ViewModel — dependencias resueltas automáticamente.
- `hiltViewModel()` en cada Composable Screen para obtener el ViewModel scoped al destino de navegación.
- 2 módulos Hilt: `DatabaseModule` (provee `TensionDatabase` y DAOs como `@Singleton`) y `RepositoryModule` (vincula interfaces con implementaciones).
- El grafo completo se verifica en tiempo de compilación — si falta un binding, la build falla.

---

### ADR-05 — MVVM con capa Domain explícita

**Fecha:** 2026-02-11
**Estado:** Adoptada
**RNF:** RNF28, RNF29

**Contexto:**
La app tiene lógica de negocio densa: 7 reglas del motor de progresión, cálculos de KPIs, protocolos de descarga, detección de mesetas y alertas. Esta lógica no pertenece ni a la UI ni al acceso a datos — necesita una capa intermedia que sea testeable de forma independiente, sin dependencias de Android.

**Alternativas consideradas:**

| Alternativa | Pros | Contras |
|-------------|------|---------|
| **MVVM + Domain Layer** | Lógica de negocio aislada y testeable, regla de dependencia explícita, motor de reglas en Kotlin puro | Una capa adicional respecto a MVVM puro |
| MVVM puro (sin Domain) | Menos capas | ViewModel absorbe lógica de negocio, difícil de testear sin Android, ViewModels inflados |
| MVI (Model-View-Intent) | Eventos unidireccionales, estado inmutable | Complejidad innecesaria para esta app, boilerplate de reducers |
| Clean Architecture completa | Máxima separación (Entities, Use Cases, Interface Adapters, Frameworks) | Sobre-ingeniería para una app single-module |

**Decisión:** MVVM con 4 capas (UI → ViewModel → Domain → Data), regla de dependencia unidireccional estricta.

**Consecuencias:**
- La capa Domain contiene Use Cases, Motor de Reglas (R1-R7) e interfaces de Repository.
- Domain no importa ninguna clase de `android.*`, `androidx.*` ni `com.google.*`.
- Los Use Cases encapsulan operaciones de negocio con un único método `operator fun invoke()`.
- El flujo de datos reactivo: Data `Flow<T>` → Domain → ViewModel `StateFlow<UiState>` → UI `collectAsStateWithLifecycle()`.
- La inversión de dependencia (Repository interface en Domain, implementación en Data) permite testear Use Cases con mocks puros.

---

### ADR-06 — Motor de reglas como Kotlin puro sin dependencias Android

**Fecha:** 2026-02-11
**Estado:** Adoptada
**RNF:** RNF29, RNF30

**Contexto:**
Las Reglas 1-7 del Manifiesto de Dominio Sistémico son el diferenciador del producto: Doble Umbral, detección de meseta, regresión, fatiga, protocolo de descarga, progresión de peso corporal e isométricos. Estas reglas deben ser testeables unitariamente con alta cobertura, sin necesidad de emulador, base de datos ni framework Android.

**Alternativas consideradas:**

| Alternativa | Pros | Contras |
|-------------|------|---------|
| **Funciones puras en Kotlin (paquete `domain.rules`)** | Testeable con JUnit estándar, sin dependencias, cada regla es una unidad independiente | Requiere disciplina para no filtrar dependencias Android |
| Lógica embebida en ViewModels | Menos archivos | No testeable sin instrumentación, acoplamiento alto, duplicación si múltiples ViewModels la usan |
| Módulo Gradle separado (`:domain`) | Máxima garantía de aislamiento | Complejidad de configuración multi-módulo para un solo desarrollador |
| Rules engine externo (Drools, etc.) | Flexibilidad de configuración | Sobre-ingeniería extrema, dependencia innecesaria |

**Decisión:** Las reglas R1-R7 se implementan como funciones puras en el paquete `domain.rules`, invocadas por los Use Cases. Cada regla es un archivo independiente.

**Consecuencias:**
- 7 archivos de reglas, cada uno testeable con JUnit 4 sin emulador.
- Los Use Cases del cierre de sesión orquestan la invocación de reglas.
- Las reglas reciben datos primitivos o modelos de dominio y retornan resultados — no acceden a Room, SharedPreferences ni ningún componente Android.
- RNF30 se satisface directamente: todas las reglas críticas tienen tests unitarios.

---

### ADR-07 — Single Activity con Navigation Compose

**Fecha:** 2026-02-11
**Estado:** Adoptada
**RNF:** RNF28

**Contexto:**
La app tiene 26 vistas organizadas en 10 flujos funcionales, con 58 conexiones de navegación. Necesita: navegación por tabs (Bottom Navigation con 5 secciones), preservación de estado por tab, argumentos tipados entre pantallas, vistas reutilizables con múltiple origen (D2, F3), un flujo contenido de sesión activa (E1-E5 sin escape al Bottom Nav), y start destination dinámica (A1 vs B1).

**Alternativas consideradas:**

| Alternativa | Pros | Contras |
|-------------|------|---------|
| **Single Activity + NavHost (Navigation Compose)** | Declarativo, rutas tipadas, nested graphs para aislamiento de back stack, integración con Hilt (`hiltViewModel()`), gestión automática de back stack | Toda la complejidad de navegación en un solo grafo |
| Multiple Activities | Aislamiento natural | No comparte estado, transiciones lentas, sin Bottom Nav persistente |
| Single Activity + Fragments (Navigation Component XML) | Maduro, amplia documentación | Requiere XML para navigation graph, Fragments añaden lifecycle complejo, no se integra con Compose |
| Compose-only (sin Navigation library) | Control total | Gestión manual del back stack, argumentos, deep links y state restoration |

**Decisión:** Single Activity (`MainActivity`) con `NavHost` de Navigation Compose. 10 nested graphs (uno por flujo funcional). Bottom Navigation con `saveState`/`restoreState`.

**Consecuencias:**
- `MainActivity` solo contiene `setContent` → `TensionTheme` → `Scaffold` → `NavHost`.
- 26 destinos Composable, 25 con ruta string (E4 es un dialog, no una ruta).
- Nested graphs aíslan el back stack por tab — al cambiar de tab y volver, el estado se preserva.
- Argumentos tipados (`Long`) para IDs de entidad embebidos en la ruta.
- Transiciones especiales con `popUpTo` para limpiar back stack (A1→B1, E4→E5, J3→B1).
- Reglas de visibilidad del Bottom Nav evaluadas por ruta actual del `NavController`.

---

### ADR-08 — Estructura de paquetes layer-first con agrupación por feature

**Fecha:** 2026-02-11
**Estado:** Adoptada

**Contexto:**
Se necesita una estructura de paquetes que haga explícita la regla de dependencia entre capas (UI → ViewModel → Domain → Data) y que al mismo tiempo agrupe código que cambia junto (los archivos de un mismo flujo funcional).

**Alternativas consideradas:**

| Alternativa | Pros | Contras |
|-------------|------|---------|
| **Layer-first con feature dentro** | Regla de dependencia explícita en la jerarquía de paquetes, Hilt modules naturales por capa | Features dispersos entre capas (el Screen y el UseCase del mismo feature están en paquetes distintos) |
| Feature-first | Todo lo de un feature junto | La regla de dependencia no es explícita en la estructura, módulos Hilt más complejos |
| Feature-first con capas dentro | Cada feature tiene su ui/, domain/, data/ | Duplicación de estructura, excesivo para una app single-module |

**Decisión:** Raíz organizada por capa (`ui/`, `domain/`, `data/`, `di/`). Dentro de `ui/` y `domain/usecase/`, sub-paquetes por flujo funcional (onboarding, home, session, etc.).

**Consecuencias:**
- 4 paquetes raíz + `MainActivity` y `TensionApplication` en la raíz.
- `ui/` tiene 10 paquetes de feature + 3 transversales (navigation, components, theme).
- `domain/` tiene 4 sub-paquetes: model, repository, usecase (con sub-features), rules.
- `data/` tiene 3 sub-paquetes: local (database, dao, entity), repository.
- La estructura refleja directamente la regla de dependencia arquitectónica.

---

### ADR-09 — StateFlow + SharedFlow para gestión de estado reactivo

**Fecha:** 2026-02-11
**Estado:** Adoptada

**Contexto:**
Los ViewModels necesitan exponer estado a la UI de forma reactiva (la UI se recompone cuando el estado cambia) y emitir eventos one-shot (navegación, snackbar) que se consumen exactamente una vez. Se necesita un mecanismo que respete el lifecycle de Android y sea compatible con Compose.

**Alternativas consideradas:**

| Alternativa | Pros | Contras |
|-------------|------|---------|
| **StateFlow + SharedFlow** | Kotlin nativo, lifecycle-aware con `collectAsStateWithLifecycle()`, hot stream (siempre tiene valor actual), SharedFlow para eventos one-shot | Requiere patrón `_mutable` / `exposed immutable` |
| LiveData | Lifecycle-aware nativo de Android | No es de Kotlin puro (depende de `androidx.lifecycle`), no funciona en Domain Layer, transformaciones limitadas |
| Compose State (`mutableStateOf`) | Integración directa con Compose | No funciona fuera de Compose, difícil de testear en ViewModel, no apto para Domain Layer |
| RxJava | Potente para transformaciones complejas | Añade una dependencia pesada, curva de aprendizaje, Kotlin Coroutines lo reemplaza nativamente |
| Channel | Consumo single-collector | No admite múltiples colectores, pérdida de eventos si no hay collector activo |

**Decisión:** `MutableStateFlow` (privado) → `StateFlow` (público) para estado de UI. `MutableSharedFlow` (privado) → `SharedFlow` (público) para eventos one-shot.

**Consecuencias:**
- Cada ViewModel expone `val uiState: StateFlow<FeatureUiState>` con una sealed class (Loading, Success, Error, Empty).
- Los eventos (navegación, snackbar) se emiten via `SharedFlow` y se recolectan en `LaunchedEffect` del Composable.
- La UI recolecta con `collectAsStateWithLifecycle()` — se detiene cuando el lifecycle no está en STARTED.
- Patrón estándar: `private val _uiState = MutableStateFlow(...)` / `val uiState = _uiState.asStateFlow()`.
- El Domain Layer puede usar `Flow<T>` plano (Kotlin stdlib) sin dependencia de Android.

---

### ADR-10 — JSON como formato de backup

**Fecha:** 2026-02-11
**Estado:** Adoptada
**RNF:** RNF15, RNF16, RNF17, RNF18

**Contexto:**
RNF17 permite JSON o SQLite exportado como formato de backup. El archivo debe ser autodescriptivo, incluir metadatos de versión para migraciones futuras, completarse en < 10 segundos para 2 años de datos (RNF18), y ser compartible vía apps del sistema (RNF15).

**Alternativas consideradas:**

| Alternativa | Pros | Contras |
|-------------|------|---------|
| **JSON** | Legible por humanos, autodescriptivo, fácil de agregar metadatos de versión, compartible como archivo de texto, parseable por herramientas externas | Más pesado que binario, requiere serialización/deserialización manual de entidades |
| SQLite exportado (copia de .db) | Rápido (copia de archivo), restauración directa | No es legible, requiere que la versión del schema coincida exactamente, migración manual entre versiones de schema, difícil agregar metadatos |
| Protobuf | Compacto, tipado | Requiere librería adicional, no legible, complejidad innecesaria |
| ZIP comprimido con JSON | Menor tamaño | Complejidad adicional sin beneficio claro para el volumen de datos esperado |

**Decisión:** JSON con estructura autodescriptiva. Nombre de archivo: `tension_backup_YYYYMMDD.json`. Incluye metadatos de versión de app y esquema de BD en el header.

**Consecuencias:**
- El archivo contiene un objeto JSON raíz con: metadatos (versión de app, versión de esquema, fecha de exportación) y un objeto por cada tabla del modelo de datos.
- La serialización/deserialización se implementa en la Data Layer usando `kotlinx.serialization` o JSON manual.
- La importación valida los metadatos de versión antes de procesar — si la versión del esquema es anterior, se puede aplicar una migración de datos.
- El archivo no se cifra (RNF25, RNF26) pero se muestra una advertencia al usuario antes de exportar.
- La exportación e importación se ejecutan en `Dispatchers.IO` dentro de una transacción atómica para garantizar consistencia.
- Rendimiento: para 2 años de datos (~730 sesiones × 9-11 ejercicios × 4 series = ~30,000 registros de series), el JSON es manejable dentro del umbral de 10 segundos.

---

### ADR-11 — Prepopulación de datos con RoomDatabase.Callback

**Fecha:** 2026-02-11
**Estado:** Adoptada
**RNF:** RNF31

**Contexto:**
El Diccionario de Ejercicios (43 ejercicios base), las zonas musculares (15), los tipos de equipo (9), los módulos (3), las versiones de módulo (9) y las asignaciones plan-ejercicio (93) deben existir en la base de datos desde el primer uso de la app (RF04, RF05). Estos datos representan el catálogo base del sistema. Adicionalmente, el ejecutante puede ampliar el diccionario con ejercicios propios (RF62) y asignarlos a versiones del plan (RF63) — estas operaciones son en runtime y no afectan el mecanismo de seed.

**Alternativas consideradas:**

| Alternativa | Pros | Contras |
|-------------|------|---------|
| **RoomDatabase.Callback.onCreate()** | Se ejecuta una sola vez al crear la BD, código Kotlin versionado y auditable, patrón Facade para organizar seeders | Requiere escribir código de inserción manual |
| Pre-packaged database (`createFromAsset`) | BD .db pre-llenada en assets | Difícil de auditar/versionar, requiere herramienta externa para generar el .db, migraciones complicadas |
| JSON en assets + lectura en onCreate | Datos separados del código | Parseo adicional, posibles errores de formato, más lento |
| SQL scripts en assets | Declarativo | Sin verificación de tipos en compilación, propenso a errores de sintaxis |

**Decisión:** `RoomDatabase.Callback.onCreate()` con patrón Facade que delega en servicios temáticos (Seeders).

**Consecuencias:**
- `PrepopulateFacade` orquesta la prepopulación invocando: `ModuleSeeder`, `ExerciseSeeder`, `PlanSeeder`.
- Cada Seeder encapsula las inserciones de sus entidades con datos literales en español.
- El seed data es código Kotlin versionado en Git — cualquier cambio en el catálogo base o Plan se rastrea.
- La prepopulación se ejecuta dentro de una transacción para garantizar atomicidad (o todo se inserta o nada).
- El estado de rotación (`rotation_state`) se inicializa en `ProfileRepositoryImpl.createProfile()` (junto con el perfil, no como seed data), tal como documenta el Modelo de Datos §3.14.
- Los ejercicios creados por el ejecutante (RF62) y sus asignaciones al plan (RF63) se persisten en las mismas tablas (`exercise`, `plan_assignment`) vía operaciones CRUD en runtime, no como seed data.

---

### ADR-12 — Esquema de color propio derivado de seed — sin Material You dinámico

**Fecha:** 2026-02-11
**Estado:** Adoptada
**RNF:** RNF23

**Contexto:**
Material 3 ofrece dos enfoques de color: esquema estático derivado de un color seed, o Material You dinámico (derivado del wallpaper del dispositivo). La app necesita identidad visual consistente (señales de progresión verde/amarillo/rojo que deben contrastar correctamente contra los fondos de la app) y debe respetar la preferencia de tema claro/oscuro del sistema operativo (RNF23).

**Alternativas consideradas:**

| Alternativa | Pros | Contras |
|-------------|------|---------|
| **Esquema propio con seed `#8B1A1A`** | Identidad visual consistente, colores semánticos predecibles, contraste WCAG AA verificable | No se adapta al wallpaper del usuario |
| Material You dinámico | Se adapta al dispositivo, sensación nativa | Los colores semánticos (progresión, regresión, alertas) podrían perder contraste dependiendo del wallpaper; no disponible en API < 31 (la app soporta API 26) |
| Esquema propio + fallback dinámico en API 31+ | Lo mejor de ambos mundos | Complejidad de mantener dos esquemas, testing doble, riesgo de colores semánticos inconsistentes |

**Decisión:** Esquema de color propio derivado del seed `#8B1A1A` (Rojo Imperio Romano). Se genera la paleta Material 3 completa (30 roles, claro y oscuro). La preferencia claro/oscuro del sistema SÍ se respeta; el wallpaper NO influye en los colores.

**Consecuencias:**
- `TensionTheme` aplica `lightColorScheme()` o `darkColorScheme()` según `isSystemInDarkTheme()`.
- Los colores semánticos de dominio (progresión `#2E7D32`/`#81C784`, mantenimiento `#8D6E00`/`#FFD54F`, regresión `#C62828`/`#EF9A9A`) se definen como extensiones del tema, fuera del esquema M3 estándar.
- El contraste está verificado para WCAG AA en ambos esquemas. El ámbar de mantenimiento usa `#8D6E00` (ratio ~7:1) en lugar de `#F9A825` (insuficiente).
- No se usa `dynamicLightColorScheme()` / `dynamicDarkColorScheme()` — eliminando la dependencia de API 31+.

---

### ADR-13 — Tipografía del sistema (Roboto) sin fuentes custom

**Fecha:** 2026-02-11
**Estado:** Adoptada

**Contexto:**
La escala tipográfica de Material 3 define 15 estilos. Se necesita una fuente legible en tamaños pequeños (Body Small 12sp para metadata de ejercicio) y grandes (Headline Large 32sp para tonelaje) en pantallas de 5" a 7" (RNF21).

**Alternativas consideradas:**

| Alternativa | Pros | Contras |
|-------------|------|---------|
| **Roboto (tipografía del sistema)** | Optimizada para pantallas Android, no incrementa tamaño del APK, ya escalada por M3, renderizado nativo eficiente | Sin diferenciación tipográfica |
| Google Fonts (descarga en runtime) | Variedad, no aumenta APK | Requiere red (viola RNF09 — 100% offline), latencia de carga |
| Fuente custom empaquetada en assets | Identidad visual única | Incrementa APK (RNF24 < 150 MB ya presionado por 43 imágenes PNG de ejercicios en `assets/exercises/`), posibles problemas de renderizado |

**Decisión:** Fuente del sistema (Roboto en Android) para todas las escalas tipográficas. Sin fuentes custom.

**Consecuencias:**
- La escala tipográfica se define en `Type.kt` usando los pesos y tamaños de M3 sin cambiar la familia de fuentes.
- Display Large, Display Medium y Display Small no se usan en ninguna vista — se define explícitamente para evitar uso accidental.
- El APK no incluye archivos de fuentes — todo el presupuesto de tamaño se reserva para las imágenes PNG de los 43 ejercicios (en `assets/exercises/`).

---

### ADR-14 — Distribución como APK firmado sin Google Play Store

**Fecha:** 2026-02-11
**Estado:** Adoptada
**RNF:** RNF37

**Contexto:**
La aplicación es de uso personal y no requiere distribución masiva. Publicar en Play Store introduce requisitos adicionales (políticas de contenido, revisión, metadata) sin beneficio para un usuario único.

**Alternativas consideradas:**

| Alternativa | Pros | Contras |
|-------------|------|---------|
| **APK firmado (instalación directa)** | Control total, sin revisión externa, instalación inmediata | Requiere habilitar "fuentes desconocidas" en el dispositivo |
| Google Play Store | Distribución automática, actualizaciones | Proceso de revisión, metadata obligatoria, políticas de contenido, tiempo de publicación |
| App Bundle (AAB) + sideload | Optimización por dispositivo | Requiere bundletool para generar APK desde AAB, complejidad innecesaria |

**Decisión:** Distribución como APK firmado para instalación directa. Sin Play Store.

**Consecuencias:**
- El build genera un APK release firmado con keystore del desarrollador.
- Las actualizaciones se distribuyen como nuevo APK — el usuario instala manualmente.
- No se configura Play App Signing ni Google Play Console.
- El tamaño del APK debe mantenerse < 150 MB (RNF24) incluyendo los assets multimedia.

---

### ADR-15 — Base de datos sin cifrado

**Fecha:** 2026-02-11
**Estado:** Adoptada
**RNF:** RNF25

**Contexto:**
Los datos almacenados son registros de entrenamiento personal (peso, repeticiones, RIR). No hay información financiera, médica protegida, credenciales ni datos de terceros. El cifrado añadiría una dependencia (SQLCipher) y overhead de rendimiento en cada operación de IO.

**Alternativas consideradas:**

| Alternativa | Pros | Contras |
|-------------|------|---------|
| **Sin cifrado** | Sin dependencia adicional, rendimiento máximo de Room, simplicidad | Si el dispositivo se compromete, los datos son legibles |
| SQLCipher | Cifrado transparente de la BD | Añade ~3 MB al APK, overhead de rendimiento en cada query, gestión de clave |
| Cifrado a nivel Android (EncryptedSharedPreferences) | Nativo de Jetpack | Solo para key-value, no aplica a Room completo |

**Decisión:** La base de datos Room NO se cifra. El archivo de backup tampoco (RNF26).

**Consecuencias:**
- Room opera directamente sobre SQLite estándar sin capa de cifrado intermedia.
- Las queries son marginalmente más rápidas (sin decrypt/encrypt por operación).
- El APK no incluye la librería SQLCipher (~3 MB ahorrados).
- Se muestra una advertencia al exportar backup informando que el archivo contiene datos de entrenamiento sin cifrar.

---

### ADR-16 — Solo modo portrait — sin soporte landscape ni tablets

**Fecha:** 2026-02-11
**Estado:** Adoptada
**RNF:** RNF07, RNF21

**Contexto:**
La app se usa en el gimnasio, frecuentemente con una mano mientras se descansa entre series. Los formularios de registro (peso, reps, RIR) y las listas de ejercicios están diseñados para scroll vertical en pantallas de 5"-7". El soporte landscape duplicaría el esfuerzo de diseño y testing sin beneficio para el caso de uso.

**Alternativas consideradas:**

| Alternativa | Pros | Contras |
|-------------|------|---------|
| **Solo portrait** | Una sola configuración de layout, testing simplificado, experiencia optimizada para uso con una mano | No se puede usar en horizontal |
| Portrait + landscape | Flexibilidad | Doble diseño, doble testing, layouts adaptativos complejos, sin beneficio real en el gimnasio |
| Portrait + tablet | Alcance mayor | RNF21 excluye tablets explícitamente, layouts tablet requieren diseño separado |

**Decisión:** `android:screenOrientation="portrait"` en `AndroidManifest.xml`. Sin soporte landscape ni tablets.

**Consecuencias:**
- Todos los wireframes y la Especificación Visual asumen layout vertical.
- No se definen layouts alternativos en `res/layout-land/` ni `res/layout-sw600dp/`.
- Los Composables usan anchos responsivos dentro del rango portrait 5"-7".
- Las previews en Android Studio solo necesitan configurarse en portrait.

---

### ADR-17 — Interfaz monoidioma en español

**Fecha:** 2026-02-11
**Estado:** Adoptada
**RNF:** RNF08

**Contexto:**
La app es de uso personal para un ejecutante hispanohablante. No hay requerimiento de internacionalización ni plan de distribuir la app a usuarios de otros idiomas.

**Alternativas consideradas:**

| Alternativa | Pros | Contras |
|-------------|------|---------|
| **Español único** | Simplicidad, sin overhead de i18n, strings directos | Si en el futuro se requiere otro idioma, hay que refactorizar |
| Multi-idioma (i18n desde el inicio) | Preparado para futuro | Overhead innecesario, archivos de strings duplicados, complejidad de mantenimiento |

**Decisión:** Toda la UI en español. Un único `res/values/strings.xml` sin carpetas `values-en/`, `values-pt/`, etc.

**Consecuencias:**
- Los strings de UI (`"Iniciar sesión"`, `"Exportar datos"`) van en `strings.xml` en español.
- Los mensajes de validación de dominio están en inglés (uso interno, no visibles al usuario).
- Los datos de seed (nombres de ejercicios, zonas musculares) están en español.
- El código fuente (clases, funciones, variables, KDoc) está en inglés.
- Si en el futuro se necesita otro idioma, las constantes ya están en `strings.xml` — serían extraíbles, pero no es un objetivo actual.

---

### ADR-18 — JUnit 4 para testing del motor de reglas

**Fecha:** 2026-02-11
**Estado:** Adoptada
**RNF:** RNF30

**Contexto:**
Las reglas de negocio (R1-R7) son la parte más crítica del sistema. Cualquier error en la Regla de Doble Umbral, detección de meseta o protocolo de descarga produce prescripciones incorrectas. Estas reglas deben tener cobertura de tests unitarios alta y ejecutarse rápidamente sin emulador.

**Alternativas consideradas:**

| Alternativa | Pros | Contras |
|-------------|------|---------|
| **JUnit 4** | Estándar en el ecosistema Android, ejecución local rápida, soporte nativo en Gradle | API ligeramente más antigua que JUnit 5 |
| JUnit 5 | API moderna (nested tests, display names, extensions) | Requiere Gradle plugin adicional para Android, no soportado nativamente por AGP |
| Kotest | DSL de Kotlin idiomático, property-based testing | Dependencia extra, curva de aprendizaje, menor adopción en Android |
| TestNG | Flexible con data providers | No es estándar en Android, requiere configuración adicional |

**Decisión:** JUnit 4 para tests unitarios del motor de reglas y Use Cases. Compose UI Test JUnit4 para tests de UI instrumentados. Espresso para integración Android.

**Consecuencias:**
- Los tests del motor de reglas residen en `test/` (no `androidTest/`) — se ejecutan en JVM local sin emulador.
- Cada regla (R1-R7) tiene su propio archivo de tests con casos que cubren: caso exitoso, caso de frontera, datos insuficientes.
- Los Use Cases se testean mockando las interfaces de Repository.
- Los ViewModels se testean con `TestCoroutineDispatcher` y mocks de Use Cases.
- Compose UI Tests residen en `androidTest/` para verificar comportamiento de pantallas completas.

---

## 4. Decisiones Derivadas del Dominio

Las siguientes decisiones no son de arquitectura de software sino de modelado de dominio. Se registran aquí porque influyen directamente en la implementación técnica.

| # | Decisión de dominio | Impacto técnico | Fuente |
|---|--------------------|----------------|--------|
| D-01 | Rotación cíclica agnóstica al calendario (A→B→C) | La columna `current_module_position` y `current_version_index` del estado de rotación se persisten indefinidamente. No hay lógica de fecha/calendario en la determinación de qué módulo toca | MDS §4, §6-B.9 |
| D-02 | Sustitución puntual solo con ejercicio "No Iniciado" (0 series) | El botón "Sustituir" se habilita/deshabilita según `session_exercise.status`. No se puede sustituir un ejercicio con series registradas | HU-07, CA-07.05, CA-07.06 |
| D-03 | Sesiones cerradas son inmutables | No existe endpoint/función de edición post-cierre. La tabla `session` pasa a estado COMPLETED/INCOMPLETE y los datos quedan congelados | MDS §4 "Integridad del dato", HU-09 |
| D-04 | E4 es un diálogo, no una pantalla de navegación | `AlertDialog` gestionado por estado del ViewModel (`showCloseDialog: Boolean`). No es una ruta en el `NavHost` — no entra al back stack | Wireframes E4, Arquitectura §4.3 |
| D-05 | Objetivo de frecuencia semanal con default 4 sesiones | `settings.weekly_goal` se inicializa en 4 (rango 4-6). No se pide en onboarding — el ejecutante lo configura en J1 cuando quiera | Mapa de Navegación §6.1, HU-21 |
| D-06 | Comparación de progresión contra último registro del mismo ejercicio (independiente de versión) | El query de comparación busca la última `session_exercise` del mismo `exercise_id`, sin filtrar por `module_version_id` | HU-10, RF23 |
| D-07 | Incremento diferenciado: +2.5 Kg (A/B) y +5 Kg (C) | Constantes del motor de reglas: `LOAD_INCREMENT_UPPER = 2.5`, `LOAD_INCREMENT_LOWER = 5.0`. Se determinan por el módulo del ejercicio | MDS §6-A R1, HU-11 |
| D-08 | Descarga dura 1 microciclo completo (6 sesiones A-B-C-A-B-C) | El estado de descarga se mantiene activo durante 6 sesiones consecutivas, no por tiempo calendario. La versión del módulo NO cambia durante la descarga | MDS §6-A R5, HU-17 |
