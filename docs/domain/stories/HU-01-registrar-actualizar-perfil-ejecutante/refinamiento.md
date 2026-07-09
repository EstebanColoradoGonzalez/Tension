## Refinamiento Técnico (Developer)
**Autor**: Esteban Colorado González | **Fecha**: 2026-02-12

---

### Consideraciones Generales

**Basado en análisis arquitectónico:**
Análisis Arquitectónico de HU-01 con 10 hitos, 18 componentes, 5 integraciones y 4 riesgos identificados. Patrón MVVM con capa Domain explícita (ADR-05). Primera historia que construye infraestructura base del proyecto.

**Nivel de complejidad:**
ALTA — Esta historia no solo implementa el perfil del ejecutante sino que construye toda la infraestructura base del proyecto: configuración Gradle (Room, Hilt, KSP, Navigation), estructura de 4 capas, sistema de diseño visual completo, navegación con start destination dinámica y 5 pantallas (A1, B1, C1, C2 stub, J1 stub). Es el cimiento de las 31 historias restantes.

**Riesgos técnicos conocidos:**
1. Configuración Hilt + KSP puede generar errores de compilación en primera integración — verificar compatibilidad de versiones.
2. Transacción atómica `createProfile` (profile + weight_record + rotation_state) — usar `@Transaction` de Room.
3. `weekly_frequency` DEFAULT = 6 (Modelo de Datos §3.8, fuente autoritativa del esquema).
4. Start destination dinámica requiere estado de carga antes de renderizar NavHost — evitar flicker.

**Patrones y convenciones del equipo:**
- Código fuente en inglés, UI y datos de dominio en español (Arquitectura Técnica §5.1)
- Naming: `{Feature}Screen`, `{Feature}ViewModel`, `{Acción}{Entidad}UseCase`, `{Entidad}Repository`/`{Entidad}RepositoryImpl`, `{Entidad}Entity`, `{Entidad}Dao` (§5.2)
- Estructura Composable: hiltViewModel() + collectAsStateWithLifecycle() + LaunchedEffect para eventos (§5.3)
- Estructura ViewModel: `_uiState MutableStateFlow` / `uiState StateFlow` + `_events MutableSharedFlow` / `events SharedFlow` (§5.4)
- Sealed classes para UiState (Loading, Success, Error) y Events
- `operator fun invoke()` en Use Cases
- Callbacks en Composables con prefijo `on` (onNavigateBack, onRegisterProfile)

**Dependencias nuevas a instalar:**
Room Runtime + KTX + Compiler (KSP), Hilt Android + Compiler + Navigation Compose, Navigation Compose, Lifecycle ViewModel Compose + Runtime Compose, Coroutines Core + Android, Material Icons Extended, MockK (test), kotlinx-coroutines-test (test), KSP plugin, Hilt plugin

**Estrategia de testing:**
JUnit 4 (ADR-18) + MockK + kotlinx-coroutines-test | Tests unitarios para 5 Use Cases (domain) | Cobertura: 100% Use Cases, validaciones de dominio

---

### Historias Relacionadas Consultadas

**Implementaciones similares analizadas:**
No existen historias previas refinadas — HU-01 es la primera historia del proyecto.

**Patrones de código reutilizados:**
Ninguno previo. Esta historia ESTABLECE los patrones base que reutilizarán todas las historias futuras.

**Mejores prácticas aplicadas:**
- Transacción atómica para creación de perfil (profile + weight_record + rotation_state)
- Dirty state en formulario C1 para habilitar/deshabilitar botón Guardar
- Start destination dinámica con estado Loading para evitar flicker
- Validaciones centralizadas en Use Cases (no en ViewModels ni UI)
- CompositionLocal para colores semánticos extendidos del dominio

---

### Tareas de Implementación

#### Fase 1: Infraestructura y Configuración Gradle

> Basado en Hito #1 del Análisis Arquitectónico

##### Configuración Gradle y Application

- [ ] **Agregar versiones y dependencias al Version Catalog** (AC: 01.06)
  - [ ] Agregar versiones de Room, Hilt, KSP, Navigation Compose, Lifecycle, Coroutines, MockK, coroutines-test, Material Icons Extended — Archivo: `Tension/gradle/libs.versions.toml`
  - [ ] Agregar libraries para cada dependencia — Archivo: `Tension/gradle/libs.versions.toml`
  - [ ] Agregar plugins de Hilt y KSP — Archivo: `Tension/gradle/libs.versions.toml`

- [ ] **Configurar plugins a nivel proyecto** (AC: 01.06)
  - [ ] Agregar `alias(libs.plugins.hilt)` y `alias(libs.plugins.ksp)` con `apply false` — Archivo: `Tension/build.gradle.kts`

- [ ] **Configurar plugins y dependencias a nivel app** (AC: 01.06)
  - [ ] Aplicar plugins `ksp` y `hilt` — Archivo: `Tension/app/build.gradle.kts`
  - [ ] Agregar todas las `implementation`, `ksp`, `testImplementation` y `androidTestImplementation` desde version catalog — Archivo: `Tension/app/build.gradle.kts`

- [ ] **Crear TensionApplication con @HiltAndroidApp** (AC: 01.06)
  - [ ] Crear clase `TensionApplication` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/TensionApplication.kt`

- [ ] **Configurar AndroidManifest** (AC: 01.06)
  - [ ] Registrar `android:name=".TensionApplication"` en `<application>` — Archivo: `app/src/main/AndroidManifest.xml`
  - [ ] Agregar `android:screenOrientation="portrait"` en `<activity>` (RNF07, ADR-16) — Archivo: `app/src/main/AndroidManifest.xml`

- [ ] **Agregar @AndroidEntryPoint a MainActivity** (AC: 01.06)
  - [ ] Agregar anotación `@AndroidEntryPoint` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/MainActivity.kt`

#### Fase 2: Sistema de Diseño Visual (Tema Tension)

> Basado en Hito #2 del Análisis Arquitectónico

##### Theme

- [ ] **Implementar paleta de colores completa** (AC: 01.01, 01.07)
  - [ ] Reemplazar colores placeholder con paleta Tension derivada de seed `#8B1A1A` — 30 roles M3 para esquema claro (§4.1) y oscuro (§4.2) + colores semánticos de dominio (§4.3: progresión, mantenimiento, regresión, estados ejercicio, alertas, tendencias, descarga) — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/theme/Color.kt`

- [ ] **Implementar TensionTheme con esquemas claro/oscuro** (AC: 01.01, 01.07)
  - [ ] Reemplazar tema placeholder: `lightColorScheme()` y `darkColorScheme()` con paleta Tension, selección por `isSystemInDarkTheme()`, SIN `dynamicLightColorScheme`/`dynamicDarkColorScheme` (ADR-12). Exponer colores semánticos extendidos via `CompositionLocal` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/theme/Theme.kt`

- [ ] **Implementar escala tipográfica M3 completa** (AC: 01.01, 01.07)
  - [ ] Reemplazar tipografía placeholder con 15 estilos M3 (Especificación Visual §5.1), Roboto (FontFamily.Default), sin fuentes custom (ADR-13) — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/theme/Type.kt`

#### Fase 3: Data Layer — Entities, DAOs y Database

> Basado en Hito #3 del Análisis Arquitectónico

##### Entities

- [ ] **Crear ProfileEntity** (AC: 01.06, 01.09)
  - [ ] `@Entity(tableName = "profile")` con columnas: `id` (PK, default 1), `height_m` (REAL, >0), `experience_level` (TEXT: BEGINNER/INTERMEDIATE/ADVANCED), `weekly_frequency` (INTEGER, DEFAULT 6, rango 4-6), `created_at` (TEXT, ISO 8601). CHECK constraints — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/entity/ProfileEntity.kt`

- [ ] **Crear WeightRecordEntity** (AC: 01.06, 01.09)
  - [ ] `@Entity(tableName = "weight_record")` con columnas: `id` (PK autoincrement), `weight_kg` (REAL, >0), `date` (TEXT, ISO 8601). Índice en `date` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/entity/WeightRecordEntity.kt`

- [ ] **Crear RotationStateEntity** (AC: 01.06)
  - [ ] `@Entity(tableName = "rotation_state")` con columnas: `id` (PK, default 1), `microcycle_position` (INTEGER, DEFAULT 1, 1-6), `current_version_module_a` (INTEGER, DEFAULT 1, 1-3), `current_version_module_b` (INTEGER, DEFAULT 1, 1-3), `current_version_module_c` (INTEGER, DEFAULT 1, 1-3), `microcycle_count` (INTEGER, DEFAULT 0) — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/entity/RotationStateEntity.kt`

##### TypeConverters

- [ ] **Crear Converters** (AC: 01.06)
  - [ ] TypeConverters `String ↔ LocalDate` para columnas de fecha ISO 8601 — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/database/Converters.kt`

##### DAOs

- [ ] **Crear ProfileDao** (AC: 01.06, 01.09)
  - [ ] `@Dao` con `insert(profile)`, `getProfile(): Flow<ProfileEntity?>`, `update(profile)` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/dao/ProfileDao.kt`

- [ ] **Crear WeightRecordDao** (AC: 01.06, 01.09)
  - [ ] `@Dao` con `insert(record)`, `getLatestWeight(): Flow<WeightRecordEntity?>`, `getAllDescByDate(): Flow<List<WeightRecordEntity>>` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/dao/WeightRecordDao.kt`

- [ ] **Crear RotationStateDao** (AC: 01.06)
  - [ ] `@Dao` con `insert(state)`, `getRotationState(): Flow<RotationStateEntity?>` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/dao/RotationStateDao.kt`

##### Database

- [ ] **Crear TensionDatabase** (AC: 01.06)
  - [ ] `@Database` con entities `[ProfileEntity, WeightRecordEntity, RotationStateEntity]`, version 1. `@TypeConverters(Converters::class)`. Expone 3 DAOs. `fallbackToDestructiveMigration()` durante desarrollo — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/database/TensionDatabase.kt`

#### Fase 4: Domain Layer — Modelos, Interfaz Repository y Use Cases

> Basado en Hito #4 del Análisis Arquitectónico

##### Modelos de Dominio

- [ ] **Crear enum ExperienceLevel** (AC: 01.02)
  - [ ] Enum class Kotlin puro con valores `BEGINNER`, `INTERMEDIATE`, `ADVANCED` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/model/ExperienceLevel.kt`

- [ ] **Crear modelo Profile** (AC: 01.01, 01.07)
  - [ ] Data class Kotlin puro: `currentWeightKg: Double`, `heightM: Double`, `experienceLevel: ExperienceLevel`, `weeklyFrequency: Int`, `createdAt: LocalDate` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/model/Profile.kt`

- [ ] **Crear modelo WeightRecord** (AC: 01.06)
  - [ ] Data class Kotlin puro: `id: Long`, `weightKg: Double`, `date: LocalDate` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/model/WeightRecord.kt`

##### Interfaz Repository

- [ ] **Crear interfaz ProfileRepository** (AC: 01.06, 01.09)
  - [ ] Interfaz Kotlin puro con contratos: `suspend fun createProfile(...)`, `fun getProfile(): Flow<Profile?>`, `suspend fun updateProfile(...)`, `suspend fun updateWeight(...)`, `fun getLatestWeight(): Flow<Double?>`, `fun getAllWeightRecords(): Flow<List<WeightRecord>>`, `fun profileExists(): Flow<Boolean>` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/repository/ProfileRepository.kt`

##### Use Cases

- [ ] **Crear CreateProfileUseCase** (AC: 01.04, 01.05, 01.06)
  - [ ] Valida peso > 0, altura > 0, experiencia válida. Invoca `ProfileRepository.createProfile()` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/usecase/profile/CreateProfileUseCase.kt`
  - [ ] Test unitario: caso éxito, peso inválido, altura inválida — Archivo: `app/src/test/java/com/estebancoloradogonzalez/tension/domain/usecase/profile/CreateProfileUseCaseTest.kt`

- [ ] **Crear GetProfileUseCase** (AC: 01.07)
  - [ ] Obtiene perfil actual combinando profile + peso más reciente — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/usecase/profile/GetProfileUseCase.kt`
  - [ ] Test unitario: perfil existe, perfil no existe — Archivo: `app/src/test/java/com/estebancoloradogonzalez/tension/domain/usecase/profile/GetProfileUseCaseTest.kt`

- [ ] **Crear UpdateProfileUseCase** (AC: 01.08, 01.09)
  - [ ] Actualiza altura y/o experiencia. Validaciones idénticas a Create — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/usecase/profile/UpdateProfileUseCase.kt`
  - [ ] Test unitario: caso éxito, altura inválida — Archivo: `app/src/test/java/com/estebancoloradogonzalez/tension/domain/usecase/profile/UpdateProfileUseCaseTest.kt`

- [ ] **Crear UpdateWeightUseCase** (AC: 01.04, 01.09)
  - [ ] Registra nuevo peso en weight_record. Valida peso > 0 — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/usecase/profile/UpdateWeightUseCase.kt`
  - [ ] Test unitario: caso éxito, peso inválido — Archivo: `app/src/test/java/com/estebancoloradogonzalez/tension/domain/usecase/profile/UpdateWeightUseCaseTest.kt`

- [ ] **Crear CheckProfileExistsUseCase** (AC: 01.01)
  - [ ] Retorna `Flow<Boolean>` verificando existencia de registro en profile — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/usecase/profile/CheckProfileExistsUseCase.kt`
  - [ ] Test unitario: existe, no existe — Archivo: `app/src/test/java/com/estebancoloradogonzalez/tension/domain/usecase/profile/CheckProfileExistsUseCaseTest.kt`

#### Fase 5: Data Layer — Repository Implementation

> Basado en Hito #5 del Análisis Arquitectónico

##### Repository

- [ ] **Crear ProfileRepositoryImpl** (AC: 01.06, 01.09)
  - [ ] Implementa `ProfileRepository`. Encapsula `ProfileDao`, `WeightRecordDao`, `RotationStateDao`. `createProfile()` con `@Transaction` atómica (inserta profile + weight_record + rotation_state). `updateProfile()` actualiza altura y experiencia. `updateWeight()` inserta nuevo `WeightRecordEntity`. Mapeo entity ↔ domain model — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/repository/ProfileRepositoryImpl.kt`

#### Fase 6: Inyección de Dependencias (Hilt)

> Basado en Hito #6 del Análisis Arquitectónico

##### Módulos Hilt

- [ ] **Crear DatabaseModule** (AC: 01.06)
  - [ ] `@Module @InstallIn(SingletonComponent)`. Provee `TensionDatabase` como `@Singleton`. Provee `ProfileDao`, `WeightRecordDao`, `RotationStateDao` desde database — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/di/DatabaseModule.kt`

- [ ] **Crear RepositoryModule** (AC: 01.06)
  - [ ] `@Module @InstallIn(SingletonComponent)`. Vincula `ProfileRepository` ↔ `ProfileRepositoryImpl` con `@Binds` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/di/RepositoryModule.kt`

#### Fase 7: UI — A1 Registro de Perfil

> Basado en Hito #7 del Análisis Arquitectónico

##### Onboarding (A1)

- [ ] **Crear RegisterProfileUiState y RegisterProfileEvent** (AC: 01.01)
  - [ ] Data class con campos formulario, errores, isLoading. Sealed class Event con `NavigateToHome` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/onboarding/RegisterProfileUiState.kt`

- [ ] **Crear RegisterProfileViewModel** (AC: 01.01, 01.04, 01.05, 01.06)
  - [ ] `@HiltViewModel`. StateFlow<RegisterProfileUiState>. SharedFlow<RegisterProfileEvent>. Invoca CreateProfileUseCase al confirmar. Validaciones reactivas de campos — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/onboarding/RegisterProfileViewModel.kt`

- [ ] **Crear RegisterProfileScreen** (AC: 01.01, 01.02, 01.03, 01.04, 01.05)
  - [ ] Composable con: logo "Tension" + subtítulo, OutlinedTextField numérico peso (sufijo Kg, keyboardType Number), OutlinedTextField numérico altura (sufijo m), RadioButton group (Principiante/Intermedio/Avanzado), botón "Registrar" (habilitado solo si válido). Sin Bottom Nav. Errores inline. Colores Tension — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/onboarding/RegisterProfileScreen.kt`

#### Fase 8: UI — C1 Perfil del Ejecutante

> Basado en Hito #8 del Análisis Arquitectónico

##### Perfil (C1)

- [ ] **Crear ProfileUiState y ProfileEvent** (AC: 01.07, 01.09)
  - [ ] Data class `ProfileUiState` con campos precargados, dirty state, errores de validación, isLoading. Sealed class `ProfileEvent` con `SaveSuccess` (feedback de guardado exitoso), `SaveError(message)` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/profile/ProfileUiState.kt`

- [ ] **Crear ProfileViewModel** (AC: 01.07, 01.08, 01.09)
  - [ ] `@HiltViewModel`. Carga perfil con GetProfileUseCase. Detecta dirty state. Al guardar: si peso cambió → UpdateWeightUseCase + UpdateProfileUseCase; si no → solo UpdateProfileUseCase — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/profile/ProfileViewModel.kt`

- [ ] **Crear ProfileScreen** (AC: 01.07, 01.08, 01.09)
  - [ ] Composable con: CenterAlignedTopAppBar con ← + "Mi Perfil", campos precargados (peso, altura, experiencia), botón "Guardar" (deshabilitado si no hay cambios), enlace "Ver historial de peso →" navega a C2. Bottom Nav visible, Configuración activo — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/profile/ProfileScreen.kt`

#### Fase 9: UI — Stubs, Componentes y Recursos

> Basado en Hito #9 del Análisis Arquitectónico

##### Stubs de Navegación

- [ ] **Crear HomeScreen (B1 placeholder)** (AC: 01.06)
  - [ ] Composable placeholder con TopAppBar "Tension", texto bienvenida, Bottom Nav con Inicio activo — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/home/HomeScreen.kt`

- [ ] **Crear SettingsScreen (J1 stub)** (AC: 01.07)
  - [ ] Composable stub con TopAppBar "Configuración", ListItem "Editar perfil" → navega a C1. Bottom Nav con Configuración activo — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/settings/SettingsScreen.kt`

- [ ] **Crear WeightHistoryScreen (C2 stub)** (AC: 01.07)
  - [ ] Composable stub con TopAppBar ← + "Historial de Peso", lista vacía placeholder. Bottom Nav visible — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/profile/WeightHistoryScreen.kt`

##### Componentes Reutilizables

- [ ] **Crear TensionTopAppBar** (AC: 01.01, 01.07)
  - [ ] Wrapper sobre CenterAlignedTopAppBar con variantes: sin retorno, con retorno (←), con cierre (✕) — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/components/TensionTopAppBar.kt`

- [ ] **Crear BottomNavigationBar** (AC: 01.01, 01.07)
  - [ ] 5 ítems: Inicio (Home), Diccionario (MenuBook), Historial (History), Métricas (BarChart), Configuración (Settings). Ícono activo filled Primary, inactivo outlined OnSurfaceVariant. Pill indicator PrimaryContainer. Oculto en A1 — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/components/BottomNavigationBar.kt`

##### Recursos

- [ ] **Actualizar strings.xml** (AC: 01.01, 01.02, 01.04, 01.05, 01.07)
  - [ ] Agregar todos los strings en español para A1, B1, C1, C2, J1 — Archivo: `app/src/main/res/values/strings.xml`

#### Fase 10: Navegación

> Basado en Hito #10 del Análisis Arquitectónico

##### Navegación

- [ ] **Crear NavigationRoutes** (AC: 01.01, 01.06, 01.07)
  - [ ] Object con constantes de rutas kebab-case: `register`, `home`, `profile`, `weight-history`, `settings` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/navigation/NavigationRoutes.kt`

- [ ] **Crear MainViewModel** (AC: 01.01, 01.06)
  - [ ] `@HiltViewModel`. Consulta CheckProfileExistsUseCase, expone `StateFlow<StartDestination>` (enum LOADING/ONBOARDING/HOME). Estado LOADING inicial para evitar flicker — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/navigation/MainViewModel.kt`

- [ ] **Crear TensionNavHost** (AC: 01.01, 01.06, 01.07)
  - [ ] Composable con NavHost. Rutas: register (A1), home (B1), profile (C1), weight-history (C2), settings (J1). Start destination dinámica. Transición A1→B1 con `popUpTo(register) { inclusive = true }`. Scaffold con TopAppBar y BottomNav condicionales — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/navigation/TensionNavHost.kt`

- [ ] **Actualizar MainActivity con TensionNavHost** (AC: 01.01, 01.06)
  - [ ] Reemplazar Greeting placeholder con TensionTheme + TensionNavHost — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/MainActivity.kt`

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
- CA-01.01, 01.02, 01.03 → Fase 7 (A1 Screen)
- CA-01.04, 01.05 → Fase 4 (Use Cases con validaciones) + Fase 7 (errores inline en A1)
- CA-01.06 → Fases 1, 3, 4, 5, 6, 10 (infraestructura + persistencia)
- CA-01.07 → Fase 8 (C1 Screen con datos precargados)
- CA-01.08 → Fase 4 (validaciones en UpdateProfileUseCase) + Fase 8 (errores inline en C1)
- CA-01.09 → Fase 5 (ProfileRepositoryImpl persistencia)
