## Refinamiento Técnico (Developer)
**Autor**: Esteban Colorado González | **Fecha**: 2026-02-12

---

### Consideraciones Generales

**Basado en análisis arquitectónico:**
Análisis Arquitectónico de HU-02 con 4 hitos, 6 componentes, 2 integraciones y 2 riesgos identificados. Patrón MVVM con capa Domain explícita (ADR-05). Segunda historia — toda la infraestructura base ya existe gracias a HU-01.

**Nivel de complejidad:**
BAJA — Esta historia reemplaza un stub existente (`WeightHistoryScreen`) con una lista de solo lectura. No crea entidades, DAOs, repositorios ni rutinas Hilt nuevas. El volumen de código nuevo es mínimo: 1 Use Case, 1 ViewModel, 1 UiState, reescritura de 1 Screen y ajuste menor en 1 componente reutilizable.

**Riesgos técnicos conocidos:**
1. Formato de fecha `LocalDate` → "dd MMM yyyy" en español puede variar según locale del dispositivo — usar `DateTimeFormatter` con `Locale("es")` explícito (RNF08).
2. Bottom Navigation no marca Configuración como activo en C1 ni C2 — extender lógica de selección del `BottomNavigationBar`.

**Patrones y convenciones del equipo (establecidos en HU-01):**
- Código fuente en inglés, UI y datos de dominio en español (Arquitectura Técnica §5.1)
- Naming: `{Feature}Screen`, `{Feature}ViewModel`, `{Acción}{Entidad}UseCase` (§5.2)
- Estructura Composable: `hiltViewModel()` + `collectAsStateWithLifecycle()` + `LaunchedEffect` para eventos (§5.3)
- Estructura ViewModel: `_uiState MutableStateFlow` / `uiState StateFlow` (§5.4)
- `operator fun invoke()` en Use Cases
- Callbacks en Composables con prefijo `on` (`onNavigateBack`)

**Dependencias nuevas a instalar:**
Ninguna. Todas las dependencias necesarias ya están configuradas en HU-01.

**Estrategia de testing:**
JUnit 4 (ADR-18) + MockK + kotlinx-coroutines-test | Test unitario para `GetWeightHistoryUseCase` (domain) | Cobertura: 100% Use Case

---

### Historias Relacionadas Consultadas

**Implementaciones similares analizadas:**
HU-01 — Se reutiliza exactamente el mismo patrón ViewModel + StateFlow + Screen establecido en `ProfileViewModel`/`ProfileScreen`. `GetProfileUseCase` sirve como referencia directa para `GetWeightHistoryUseCase`.

**Patrones de código reutilizados:**
- Patrón `Flow<List<T>>` → `StateFlow<UiState>` del ViewModel (igual que `ProfileViewModel.loadProfile()`)
- `TensionTopAppBar` con variante retorno (ya usado en `ProfileScreen`, `WeightHistoryScreen` stub)
- `BottomNavigationBar` ya montado en el `Scaffold` de `TensionNavHost` — no se re-monta dentro del Screen

**Mejores prácticas aplicadas:**
- La lista siempre tiene ≥1 entrada (registro inicial del onboarding) — no hay estado vacío real
- "Registro inicial" se identifica por ser el último ítem de la lista DESC (`MIN(date)`), sin flag en BD
- Guard defensivo para estado de carga mientras el Flow emite su primer valor
- Formato de fecha con `Locale("es")` explícito para independencia del locale del dispositivo

---

### Código existente verificado (HU-01 ya implementado)

| Componente | Archivo | Estado |
| --- | --- | --- |
| `WeightRecord` (domain model) | `domain/model/WeightRecord.kt` | Existe — `data class WeightRecord(id: Long, weightKg: Double, date: LocalDate)` |
| `ProfileRepository.getAllWeightRecords()` | `domain/repository/ProfileRepository.kt` | Existe — `fun getAllWeightRecords(): Flow<List<WeightRecord>>` |
| `ProfileRepositoryImpl.getAllWeightRecords()` | `data/repository/ProfileRepositoryImpl.kt` | Existe — mapea `WeightRecordEntity` → `WeightRecord`, ordena DESC por fecha |
| `WeightRecordDao.getAllDescByDate()` | `data/local/dao/WeightRecordDao.kt` | Existe — `@Query("SELECT * FROM weight_record ORDER BY date DESC")` |
| `WeightHistoryScreen` (stub) | `ui/profile/WeightHistoryScreen.kt` | Existe como stub — Box con texto centrado, se reemplazará |
| Ruta `weight-history` en NavHost | `ui/navigation/TensionNavHost.kt` | Existe — `composable(NavigationRoutes.WEIGHT_HISTORY) { WeightHistoryScreen(...) }` |
| `BottomNavigationBar` | `ui/components/BottomNavigationBar.kt` | Existe — requiere ajuste en lógica de `selected` para rutas hijas |
| `strings.xml` — strings de C2 | `res/values/strings.xml` | Parcial — existen `weight_history_title` y `weight_history_empty`, faltan `weight_history_initial_record` y `weight_history_kg_suffix` |
| `DatabaseModule` / `RepositoryModule` | `di/` | Existe — no requiere cambios |

---

### Tareas de Implementación

#### Fase 1: Domain Layer — Use Case

> Basado en Hito #1 del Análisis Arquitectónico

##### Use Case

- [ ] **Crear GetWeightHistoryUseCase** (AC: 02.02, 02.04, 02.05)
  - [ ] Clase en Kotlin puro. Inyecta `ProfileRepository`. `operator fun invoke(): Flow<List<WeightRecord>>`. Delega directamente a `profileRepository.getAllWeightRecords()` — es lectura pura sin lógica adicional. La lista retorna ≥1 entrada (registro inicial siempre existe), ordenada DESC por fecha — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/usecase/profile/GetWeightHistoryUseCase.kt`
  - [ ] Test unitario: verifica que invoca `profileRepository.getAllWeightRecords()` y retorna el Flow sin transformación. Caso con múltiples registros, caso con solo registro inicial (1 entrada) — Archivo: `app/src/test/java/com/estebancoloradogonzalez/tension/domain/usecase/profile/GetWeightHistoryUseCaseTest.kt`

#### Fase 2: UI Layer — UiState y ViewModel

> Basado en Hito #2 del Análisis Arquitectónico

##### UiState

- [ ] **Crear WeightHistoryUiState y WeightEntryItem** (AC: 02.02, 02.04)
  - [ ] `WeightHistoryUiState`: data class con `isLoading: Boolean = true` y `weightEntries: List<WeightEntryItem> = emptyList()`. `WeightEntryItem`: data class con `weightKg: Double`, `date: LocalDate`, `isInitialRecord: Boolean`. El flag `isInitialRecord` se calcula en el ViewModel — solo es `true` para la entrada con la fecha más antigua de la lista — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/profile/WeightHistoryUiState.kt`

##### ViewModel

- [ ] **Crear WeightHistoryViewModel** (AC: 02.02, 02.03, 02.04, 02.05)
  - [ ] `@HiltViewModel`. Inyecta `GetWeightHistoryUseCase`. En `init`, recolecta el `Flow<List<WeightRecord>>` via `viewModelScope.launch` y lo transforma a `WeightHistoryUiState`: para cada `WeightRecord`, crea `WeightEntryItem` con `isInitialRecord = (record == list.last())` (último de la lista DESC = más antiguo = registro inicial). Expone `val uiState: StateFlow<WeightHistoryUiState>`. Estado inicial: `isLoading = true` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/profile/WeightHistoryViewModel.kt`

#### Fase 3: UI Layer — Reemplazar stub de WeightHistoryScreen

> Basado en Hito #3 del Análisis Arquitectónico

##### Screen

- [ ] **Reemplazar WeightHistoryScreen stub con pantalla funcional** (AC: 02.02, 02.03, 02.04, 02.05)
  - [ ] Reescribir el composable `WeightHistoryScreen`. Firma: `fun WeightHistoryScreen(onNavigateBack: () -> Unit, viewModel: WeightHistoryViewModel = hiltViewModel())`. Recolecta `uiState` con `collectAsStateWithLifecycle()`. Top Bar: `TensionTopAppBar` con retorno + "Historial de Peso" (ya existe en el stub, se conserva). Body: si `isLoading` → indicador de carga centrado; si no → `LazyColumn` con las entradas. Cada fila usa `ListItem` M3 de 56 dp mínimo: `headlineContent` = fecha formateada "dd MMM yyyy" con `Locale("es")` en `Body Medium`, `On Surface`; `trailingContent` = peso con sufijo "Kg" (ej: "78.5 Kg") en `Body Medium`, `On Surface`, `fontWeight = Medium`; si `isInitialRecord` → `supportingContent` con `Text("Registro inicial")` en `Body Small`, `On Surface Variant`, con `Modifier.padding(top = 2.dp)`. `HorizontalDivider` M3 entre filas: 1 dp, `Outline Variant`. Guard defensivo: si la lista está vacía (teóricamente imposible), mostrar texto `weight_history_empty` existente — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/profile/WeightHistoryScreen.kt`

#### Fase 4: UI Layer — Ajustar BottomNavigationBar + Strings + NavHost

> Basado en Hito #4 del Análisis Arquitectónico

##### BottomNavigationBar

- [ ] **Extender lógica de selección del ítem activo** (AC: 02.02)
  - [ ] En `BottomNavigationBar.kt`, cambiar `val selected = currentRoute == item.route` por una lógica que agrupe rutas hijas: para el ítem Configuración (`settings`), marcar como activo cuando `currentRoute` esté en `setOf("settings", "profile", "weight-history")`. Los demás ítems mantienen `currentRoute == item.route`. Implementar con un map de `route → Set<String>` o una función `isRouteSelected(itemRoute, currentRoute)` para que sea extensible cuando futuras HUs añadan rutas hijas a otros tabs — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/components/BottomNavigationBar.kt`

##### NavHost

- [ ] **Verificar compatibilidad de firma del WeightHistoryScreen** (AC: 02.02)
  - [ ] En `TensionNavHost.kt`, verificar que la firma del `WeightHistoryScreen` actual (`onNavigateBack: () -> Unit`) sigue siendo compatible con el ViewModel inyectado via `hiltViewModel()`. No se requiere cambio estructural — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/navigation/TensionNavHost.kt`

##### Recursos

- [ ] **Agregar strings faltantes a strings.xml** (AC: 02.02, 02.04)
  - [ ] Agregar `weight_history_initial_record` = "Registro inicial" y `weight_history_kg_suffix` = "Kg". Los strings `weight_history_title` y `weight_history_empty` ya existen y se conservan — Archivo: `app/src/main/res/values/strings.xml`

#### Fase 5: QA y Deployment

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
- CA-02.01 → Ya cubierto por HU-01 (`ProfileViewModel.onSaveClick()` invoca `UpdateWeightUseCase` que inserta en `weight_record`). No requiere trabajo en HU-02.
- CA-02.02 → Fases 1, 2, 3 (Use Case + ViewModel + Screen con lista DESC)
- CA-02.03 → Fase 3 (lista cronológica descendente permite identificar tendencia visualmente)
- CA-02.04 → Fases 2, 3 (flag `isInitialRecord` calculado en ViewModel, etiqueta "Registro inicial" en Screen)
- CA-02.05 → Ya garantizado por diseño (`weight_record` es append-only, `INSERT` sin `UPDATE`/`DELETE`). No requiere trabajo adicional.
