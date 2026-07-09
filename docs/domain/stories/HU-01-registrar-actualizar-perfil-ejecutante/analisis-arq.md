## Análisis Arquitectónico

> Esta historia es la primera en implementarse. Además de resolver los requisitos funcionales del perfil, construye la infraestructura base del proyecto que todas las historias posteriores reutilizarán.

**Patrón arquitectónico:** MVVM con capa Domain explícita (4 capas: UI → ViewModel → Domain → Data), según ADR-05.

### Componentes afectados

#### 1. Infraestructura Base del Proyecto (Nuevo)

Configuración de dependencias, estructura de paquetes y framework base. Es el cimiento que toda historia posterior requiere.

- **`libs.versions.toml`**: Agregar versiones y dependencias para Room (runtime, KTX, compiler KSP), Hilt (android, compiler, navigation-compose), Navigation Compose, Lifecycle (viewmodel-compose, runtime-compose), Coroutines y KSP plugin.
- **`build.gradle.kts` (project)**: Agregar plugins de Hilt y KSP.
- **`build.gradle.kts` (app)**: Aplicar plugins `ksp` y `hilt`, agregar dependencias desde version catalog.
- **`TensionApplication`**: Clase Application con `@HiltAndroidApp`. Registrar en `AndroidManifest.xml` con `android:name=".TensionApplication"`.
- **`MainActivity`**: Agregar `@AndroidEntryPoint` para habilitar inyección Hilt en composables con `hiltViewModel()`.
- **`AndroidManifest.xml`**: Agregar `android:screenOrientation="portrait"` en la activity (RNF07, ADR-16).

#### 2. Tema Tension (Modificación)

Implementar el sistema de diseño visual definido en la Especificación Visual. Reemplaza los colores placeholder del template.

- **`Color.kt`**: Paleta completa Tension derivada del seed `#8B1A1A` — 30 roles de color para esquema claro y oscuro (§4.1, §4.2), más colores semánticos del dominio (§4.3: progresión, mantenimiento, regresión, estados de ejercicio, niveles de alerta, tendencias, descarga).
- **`Theme.kt`**: `TensionTheme` aplica `lightColorScheme()` o `darkColorScheme()` según `isSystemInDarkTheme()`. Sin `dynamicLightColorScheme`/`dynamicDarkColorScheme` (ADR-12). Expone colores semánticos extendidos como `CompositionLocal`.
- **`Type.kt`**: Escala tipográfica M3 completa con Roboto (15 estilos), según Especificación Visual §5.1.

#### 3. Data Layer — Entities (Nuevo)

Mapeo 1:1 de las tablas del Modelo de Datos necesarias para HU-01.

- **`ProfileEntity`**: Tabla `profile`, fila única (id=1). Columnas: `height_m` (REAL, >0), `experience_level` (TEXT: BEGINNER/INTERMEDIATE/ADVANCED), `weekly_frequency` (INTEGER, DEFAULT **6**, rango 4-6), `created_at` (TEXT, ISO 8601). El peso NO se almacena aquí — se obtiene del registro más reciente de `weight_record`.
- **`WeightRecordEntity`**: Tabla `weight_record`. Columnas: `id` (PK autoincrement), `weight_kg` (REAL, >0), `date` (TEXT, ISO 8601). Índice en `date` para ordenamiento cronológico.
- **`RotationStateEntity`**: Tabla `rotation_state`, fila única (id=1). Columnas: `microcycle_position` (INTEGER, DEFAULT 1, rango 1-6), `current_version_module_a` (INTEGER, DEFAULT 1, rango 1-3), `current_version_module_b` (INTEGER, DEFAULT 1, rango 1-3), `current_version_module_c` (INTEGER, DEFAULT 1, rango 1-3), `microcycle_count` (INTEGER, DEFAULT 0). Se inicializa al crear perfil (Modelo de Datos §3.14).

#### 4. Data Layer — DAOs (Nuevo)

- **`ProfileDao`**: `insert(profile)`, `getProfile(): Flow<ProfileEntity?>`, `update(profile)`.
- **`WeightRecordDao`**: `insert(record)`, `getLatestWeight(): Flow<WeightRecordEntity?>`, `getAllDescByDate(): Flow<List<WeightRecordEntity>>`.
- **`RotationStateDao`**: `insert(state)`, `getRotationState(): Flow<RotationStateEntity?>`.

#### 5. Data Layer — Database y Converters (Nuevo)

- **`Converters`**: TypeConverters `String ↔ LocalDate` para columnas `created_at` y `date` (ISO 8601).
- **`TensionDatabase`**: `@Database` con entities `[ProfileEntity, WeightRecordEntity, RotationStateEntity]`, versión 1. Expone los 3 DAOs. `fallbackToDestructiveMigration()` durante desarrollo. Sin seed data para estas tablas (se crean por input del usuario y al crear perfil).

#### 6. Data Layer — Repository (Nuevo)

- **`ProfileRepositoryImpl`**: Implementa `ProfileRepository`. Encapsula `ProfileDao`, `WeightRecordDao` y `RotationStateDao`. El método `createProfile()` ejecuta una transacción atómica `@Transaction` que inserta: (1) `ProfileEntity`, (2) primer `WeightRecordEntity` con el peso inicial, (3) `RotationStateEntity` con defaults. El método `updateProfile()` actualiza altura y experiencia. El método `updateWeight()` inserta un nuevo `WeightRecordEntity` (sin borrar anteriores — CA-02.05). Asigna `created_at` con `LocalDate.now().toString()` al momento del insert.

#### 7. Domain Layer — Models (Nuevo)

- **`ExperienceLevel`**: Enum con valores `BEGINNER`, `INTERMEDIATE`, `ADVANCED`. Kotlin puro.
- **`Profile`**: Modelo de dominio — `currentWeightKg: Double`, `heightM: Double`, `experienceLevel: ExperienceLevel`, `createdAt: LocalDate`. No es entity de Room.

#### 8. Domain Layer — Repository Interface (Nuevo)

- **`ProfileRepository`**: Interfaz en Kotlin puro. Contratos: `suspend fun createProfile(weightKg: Double, heightM: Double, experienceLevel: ExperienceLevel)`, `fun getProfile(): Flow<Profile?>`, `suspend fun updateProfile(heightM: Double, experienceLevel: ExperienceLevel)`, `suspend fun updateWeight(weightKg: Double)`, `fun getLatestWeight(): Flow<Double?>`, `fun getAllWeightRecords(): Flow<List<WeightRecord>>`.

#### 9. Domain Layer — Use Cases (Nuevo)

- **`CreateProfileUseCase`**: Valida peso > 0, altura > 0, experiencia válida. Invoca `ProfileRepository.createProfile()`. Usado por A1.
- **`GetProfileUseCase`**: Obtiene perfil actual combinando datos de `profile` y peso más reciente de `weight_record`. Usado por C1, B1 (verificar existencia).
- **`UpdateProfileUseCase`**: Actualiza altura y/o experiencia. Validaciones idénticas. Usado por C1.
- **`UpdateWeightUseCase`**: Registra nuevo peso en `weight_record`. Valida peso > 0. Usado por C1 cuando el peso cambia (CA-02.01).
- **`CheckProfileExistsUseCase`**: Verifica si existe un registro en la tabla `profile`. Retorna `Flow<Boolean>`. Usado por `MainViewModel` para determinar start destination.

#### 10. UI Layer — Onboarding / A1 (Nuevo)

Paquete: `ui.onboarding`.

- **`RegisterProfileScreen`**: Composable de nivel pantalla. Formulario con: `OutlinedTextField` numérico para peso (sufijo "Kg", `keyboardType = Number`), `OutlinedTextField` numérico para altura (sufijo "m"), `RadioButton` group con 3 opciones (Principiante, Intermedio, Avanzado), botón "Registrar" (Filled Button, habilitado solo si todos los campos son válidos). Sin Top Bar estándar — logo "Tension" centrado + subtítulo "Configura tu perfil". Sin Bottom Navigation. Colores y estilos según Especificación Visual §8 A1.
- **`RegisterProfileViewModel`**: `@HiltViewModel`. Estado: `StateFlow<RegisterProfileUiState>` con campos del formulario, errores de validación, estado de carga. Eventos: `SharedFlow<RegisterProfileEvent>` con `NavigateToHome` al éxito. Invoca `CreateProfileUseCase` al confirmar.

#### 11. UI Layer — Perfil / C1 (Nuevo)

Paquete: `ui.profile`.

- **`ProfileScreen`**: Composable. Formulario con datos precargados. Mismo estilo que A1 pero con: `CenterAlignedTopAppBar` con `←` retorno + título "Mi Perfil", botón "Guardar" (deshabilitado si no hay cambios — dirty state), enlace "Ver historial de peso →" que navega a C2. Bottom Navigation visible con Configuración activo. Colores según Especificación Visual §8 C1.
- **`ProfileViewModel`**: `@HiltViewModel`. Carga perfil con `GetProfileUseCase`. Detecta cambios (dirty state). Al guardar: si peso cambió, invoca `UpdateWeightUseCase` (CA-02.01) + `UpdateProfileUseCase`; si solo cambió altura o experiencia, invoca solo `UpdateProfileUseCase`.

#### 12. UI Layer — C2 Stub (Nuevo)

Paquete: `ui.profile`.

- **`WeightHistoryScreen`**: Composable stub mínimo. `CenterAlignedTopAppBar` con `←` retorno + título "Historial de Peso". Body con lista vacía placeholder. Bottom Navigation visible. La lógica completa de HU-02 se implementa cuando se aborde esa historia. Este stub garantiza que la navegación C1→C2 funcione desde el primer momento.

#### 13. UI Layer — B1 Placeholder (Nuevo)

Paquete: `ui.home`.

- **`HomeScreen`**: Composable placeholder funcional mínimo. `CenterAlignedTopAppBar` con "Tension" a la izquierda. Body con texto de bienvenida. Bottom Navigation con 5 ítems (Inicio activo, los demás navegan a stubs o a secciones que se implementarán en historias futuras). Suficiente para que la navegación A1→B1 funcione post-registro.

#### 14. UI Layer — J1 Stub (Nuevo)

Paquete: `ui.settings`.

- **`SettingsScreen`**: Composable stub mínimo. `CenterAlignedTopAppBar` con "Configuración". Body con `ListItem` "Editar perfil" que navega a C1. Bottom Navigation con Configuración activo. Garantiza el flujo J1→C1→J1 desde el primer momento (Mapa de Navegación §7.3 C1).

#### 15. UI Layer — Navegación (Nuevo)

Paquete: `ui.navigation`.

- **`MainViewModel`**: `@HiltViewModel`. Consulta `CheckProfileExistsUseCase`, expone `StateFlow<StartDestination>` (enum: `ONBOARDING` o `HOME`). El composable raíz usa este estado para configurar el `startDestination` del `NavHost` (Arquitectura Técnica §4.2).
- **`TensionNavHost`**: Composable con `NavHost`. Rutas: `register` (A1), `home` (B1), `profile` (C1), `weight-history` (C2), `settings` (J1). Start destination dinámica según `MainViewModel`. Transición A1→B1 con `popUpTo(register) { inclusive = true }` (limpia back stack — Arquitectura Técnica §4.8).
- **`BottomNavigationBar`**: Composable reutilizable. 5 ítems: Inicio (`Home`), Diccionario (`MenuBook`), Historial (`History`), Métricas (`BarChart`), Configuración (`Settings`). Ícono activo filled Primary, inactivo outlined On Surface Variant. Pill indicator Primary Container. Oculto en A1. Los ítems sin destino implementado navegan al Home por ahora.
- **`NavigationRoutes`**: Object con constantes de rutas en kebab-case.

#### 16. UI Layer — Componentes reutilizables (Nuevo)

Paquete: `ui.components`.

- **`TensionTopAppBar`**: Wrapper sobre `CenterAlignedTopAppBar` con las variantes definidas en Especificación Visual §7.1 (sin retorno, con retorno, con cierre).

#### 17. DI Layer — Módulos Hilt (Nuevo)

Paquete: `di`.

- **`DatabaseModule`**: `@Module @InstallIn(SingletonComponent)`. Provee `TensionDatabase` como `@Singleton`. Provee `ProfileDao`, `WeightRecordDao`, `RotationStateDao` desde la database.
- **`RepositoryModule`**: `@Module @InstallIn(SingletonComponent)`. Vincula `ProfileRepository` interfaz ↔ `ProfileRepositoryImpl` con `@Binds`.

#### 18. Recursos (Modificación)

- **`strings.xml`**: Strings en español para A1 ("Tension", "Configura tu perfil", "Peso corporal", "Altura", "Nivel de experiencia", "Principiante", "Intermedio", "Avanzado", "Registrar", "El peso debe ser un valor positivo", "La altura debe ser un valor positivo"), C1 ("Mi Perfil", "Guardar", "Ver historial de peso"), B1 ("Bienvenido a Tension"), J1 ("Configuración", "Editar perfil"), C2 ("Historial de Peso").

---

### Integraciones

| Interfaz / Contrato | Productor | Consumidor | Descripción |
| --- | --- | --- | --- |
| `ProfileRepository` | `ProfileRepositoryImpl` (Data) | Use Cases (Domain) | Contrato de acceso a datos de perfil, peso e inicialización de rotación. Definido en Domain, implementado en Data, inyectado por Hilt |
| `StateFlow<RegisterProfileUiState>` | `RegisterProfileViewModel` | `RegisterProfileScreen` | Estado del formulario A1: campos, errores de validación, estado de carga |
| `StateFlow<ProfileUiState>` | `ProfileViewModel` | `ProfileScreen` | Perfil cargado en C1 con datos precargados en campos editables y dirty state |
| `SharedFlow<RegisterProfileEvent>` | `RegisterProfileViewModel` | `TensionNavHost` (nivel navegación) | Evento one-shot "Registro exitoso" → navegar a B1 limpiando back stack |
| `StateFlow<StartDestination>` | `MainViewModel` | `TensionNavHost` | Decide start destination: A1 si no hay perfil, B1 si existe |

---

### Riesgos

| Riesgo | Probabilidad | Impacto | Mitigación |
| --- | --- | --- | --- |
| Configuración de Hilt + KSP genera errores de compilación en primera integración | Media | Medio | Verificar compatibilidad de versiones en `libs.versions.toml` antes de agregar. Seguir guía oficial Hilt para Compose |
| Validaciones duplicadas entre A1 (registro) y C1 (edición) | Baja | Bajo | Centralizar validaciones en Use Cases (`CreateProfileUseCase`, `UpdateProfileUseCase`), no en ViewModels ni UI |
| Transacción atómica `createProfile` (profile + weight_record + rotation_state) falla parcialmente | Baja | Alto | Usar `@Transaction` de Room para garantizar atomicidad. Si falla cualquier insert, se revierte todo |
| `weekly_frequency` DEFAULT — el Modelo de Datos §3.8 define DEFAULT 6, que es la fuente autoritativa del esquema | Baja | Bajo | Implementar con DEFAULT 6 (Modelo de Datos §3.8). No se encontró ningún ADR que contradiga este valor |

---

### Hitos de implementación

| Hito | Contenido | Dependencias |
| --- | --- | --- |
| 1 | Infraestructura: dependencias en `libs.versions.toml` y `build.gradle.kts`, `TensionApplication`, `@AndroidEntryPoint`, `screenOrientation="portrait"` | — |
| 2 | Tema Tension: `Color.kt`, `Theme.kt`, `Type.kt` con paleta seed `#8B1A1A` | Hito 1 |
| 3 | Data Layer: 3 entities (`ProfileEntity`, `WeightRecordEntity`, `RotationStateEntity`), 3 DAOs, `Converters`, `TensionDatabase` | Hito 1 |
| 4 | Domain Layer: modelos (`Profile`, `ExperienceLevel`), `ProfileRepository` interfaz, 5 Use Cases | — (Kotlin puro) |
| 5 | Data Layer — Repository: `ProfileRepositoryImpl` con transacción atómica | Hito 3, Hito 4 |
| 6 | DI: `DatabaseModule`, `RepositoryModule` | Hito 3, Hito 5 |
| 7 | UI — A1 Registro de Perfil: `RegisterProfileScreen` + `RegisterProfileViewModel` | Hito 2, Hito 4, Hito 6 |
| 8 | UI — C1 Perfil del Ejecutante: `ProfileScreen` + `ProfileViewModel` | Hito 2, Hito 4, Hito 6 |
| 9 | UI — Stubs: B1 placeholder, J1 stub, C2 stub, `BottomNavigationBar`, `TensionTopAppBar`, `strings.xml` | Hito 2 |
| 10 | Navegación: `MainViewModel`, `TensionNavHost` con start destination dinámica, rutas, transición A1→B1 | Hito 7, Hito 8, Hito 9 |

---

### Notas de auditoría

1. **`rotation_state` se inicializa al crear perfil** (Modelo de Datos §3.14). Sin esto, HU-05 (Iniciar sesión) fallaría al consultar qué módulo/versión toca. Incluido en la transacción atómica de `CreateProfileUseCase`.
2. **`weekly_frequency` DEFAULT = 6** (Modelo de Datos §3.8, fuente autoritativa del esquema). No se encontró ADR que contradiga este valor. Rango permitido: 4-6 (configurable desde J1 en HU-21 CA-21.05).
3. **C2, B1, J1 son stubs mínimos** — solo cáscaras de navegación para que los flujos A1→B1, J1→C1→J1 y C1→C2 funcionen. La lógica completa se implementa en sus respectivas historias (HU-02, HU-05, HU-18, HU-31/32).
4. **`TensionDatabase` declara solo 3 entidades** en esta historia. Las 13 restantes se agregan incrementalmente. Se usa `fallbackToDestructiveMigration()` durante desarrollo.
5. **Colores semánticos extendidos** (progresión, mantenimiento, regresión, etc.) se definen en `Color.kt` y se exponen via `CompositionLocal` en `Theme.kt` aunque HU-01 no los use directamente. Esto evita retocar el tema en cada historia posterior.
