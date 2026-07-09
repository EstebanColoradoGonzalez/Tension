## Análisis Arquitectónico

> HU-02 es la segunda historia en implementarse. La infraestructura base, la capa de datos de peso y la navegación C1→C2 ya existen gracias a HU-01. Esta historia reemplaza el stub de C2 con la pantalla funcional completa.

**Patrón arquitectónico:** MVVM con capa Domain explícita (4 capas: UI → ViewModel → Domain → Data), según ADR-05. Mismo patrón establecido en HU-01.

### Componentes afectados

#### 1. Domain Layer — Use Case (Nuevo)

- **`GetWeightHistoryUseCase`**: Use Case en Kotlin puro. Invoca `ProfileRepository.getAllWeightRecords()` que retorna `Flow<List<WeightRecord>>` ordenado descendente por fecha. No requiere validación — es lectura pura. La lista siempre tiene al menos 1 entrada (el registro inicial creado junto al perfil en HU-01). Paquete: `domain.usecase.profile`.

#### 2. UI Layer — WeightHistoryViewModel (Nuevo)

Paquete: `ui.profile`.

- **`WeightHistoryViewModel`**: `@HiltViewModel`. Inyecta `GetWeightHistoryUseCase`. En `init`, recolecta el `Flow<List<WeightRecord>>` y lo transforma a `StateFlow<WeightHistoryUiState>`. La transformación determina cuál es la entrada más antigua de la lista (última posición, ya que la lista viene ordenada DESC) y la marca como "Registro inicial" (CA-02.04, Modelo de Datos §3.9: `MIN(date)`).

#### 3. UI Layer — WeightHistoryUiState (Nuevo)

Paquete: `ui.profile`.

- **`WeightHistoryUiState`**: Data class con `isLoading: Boolean` y `weightEntries: List<WeightEntryItem>`.
- **`WeightEntryItem`**: Data class con `weightKg: Double`, `date: LocalDate`, `isInitialRecord: Boolean`. El flag `isInitialRecord` se calcula en el ViewModel comparando la fecha de cada registro contra la fecha más antigua de la lista. Solo la entrada con la fecha más antigua tiene `isInitialRecord = true`.

#### 4. UI Layer — WeightHistoryScreen (Modificación — Reemplazar stub)

Paquete: `ui.profile`.

Reemplaza el stub actual (Box con texto centrado) con la pantalla funcional completa según Wireframes C2 y Especificación Visual §8 C2.

- **Top Bar**: `TensionTopAppBar` con `←` retorno + título "Historial de Peso". Ya existe en el stub.
- **Body**: `LazyColumn` scrollable con las entradas de peso. Cada fila es un `ListItem` M3 de 56 dp:
  - `headlineContent`: fecha en formato "dd MMM yyyy" (ej: "10 feb 2026"), alineada izquierda, `Body Medium`, `On Surface`.
  - `trailingContent`: peso con sufijo "Kg" (ej: "78.5 Kg"), `Body Medium`, `On Surface`, `fontWeight = Medium`.
  - Si `isInitialRecord == true`: **dentro del `ListItem`** se muestra como `supportingContent` un `Text` con "Registro inicial" en `Body Small`, `On Surface Variant`, `paddingTop = 2.dp` (Especificación Visual §8 C2: *"Padding top: 2 dp bajo el peso"*, §7.7: *"Etiqueta 'Registro inicial' como Body Small si aplica"*). La etiqueta es parte de la fila, no un elemento separado debajo.
  - `Divider` M3 entre filas: 1 dp, `Outline Variant`, full width.
- **Bottom Navigation**: `BottomNavigationBar` con Configuración activo. C2 se muestra dentro del Scaffold del `TensionNavHost` donde la Bottom Nav ya es visible. **Requiere ajuste en la lógica de selección del `BottomNavigationBar`**: actualmente determina el ítem activo por `currentRoute == item.route`, pero cuando la ruta es `weight-history` (o `profile`) ninguno de los 5 ítems coincide. Se debe ampliar la condición de selección del ítem Configuración para incluir las rutas hijas del flujo perfil: `currentRoute in setOf("settings", "profile", "weight-history")`. Esto aplica según Especificación Visual §8 C2 y Wireframes C2: *"Configuración marcado como activo"*.
- **Estado vacío**: No aplica formalmente — siempre hay al menos 1 entrada (el registro inicial del perfil, CA-02.04). No obstante, se mantiene un guard defensivo en el composable por si la lista está cargando.

**Firma actualizada del composable:**

```kotlin
@Composable
fun WeightHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: WeightHistoryViewModel = hiltViewModel(),
)
```

#### 5. UI Layer — BottomNavigationBar (Modificación menor)

Paquete: `ui.components`.

- **`BottomNavigationBar`**: Extender la lógica de selección del ítem activo. Actualmente `selected = currentRoute == item.route`. Para el ítem Configuración, debe ser `selected = currentRoute in setOf("settings", "profile", "weight-history")`. Esto hace que Configuración se muestre activo cuando el ejecutante está en C1 o C2, que son pantallas hijas del flujo perfil accesible desde J1. Sin esta corrección, ningún ítem del Bottom Nav se marca como activo en C1 ni C2, contradiciendo la Especificación Visual §8 C1 y C2.

#### 6. Recursos (Modificación)

- **`strings.xml`**: Agregar `weight_history_initial_record` = "Registro inicial". Agregar `weight_history_kg_suffix` = "Kg" (sufijo de peso, se compone en código como `"$value Kg"`). El string `weight_history_empty` existente se conserva como fallback defensivo.

---

### Integraciones

| Interfaz / Contrato | Productor | Consumidor | Descripción |
| --- | --- | --- | --- |
| `ProfileRepository.getAllWeightRecords()` | `ProfileRepositoryImpl` (existente) | `GetWeightHistoryUseCase` | Retorna `Flow<List<WeightRecord>>` ordenado DESC por fecha. Ya implementado en HU-01 |
| `StateFlow<WeightHistoryUiState>` | `WeightHistoryViewModel` | `WeightHistoryScreen` | Estado reactivo con lista de entradas de peso + flag de registro inicial |

---

### Riesgos

| Riesgo | Probabilidad | Impacto | Mitigación |
| --- | --- | --- | --- |
| Formato de fecha `LocalDate` → "dd MMM yyyy" en español puede variar según locale del dispositivo | Baja | Bajo | Usar `DateTimeFormatter` con `Locale("es")` explícito para garantizar formato consistente (RNF08: UI en español) |
| Bottom Navigation no marca Configuración como activo en C2 | Certeza | Bajo | Extender lógica de selección del `BottomNavigationBar` para incluir rutas hijas del flujo perfil (`profile`, `weight-history`) como asociadas al ítem Configuración |

---

### Hitos de implementación

| Hito | Contenido | Dependencias |
| --- | --- | --- |
| 1 | Domain: `GetWeightHistoryUseCase` | — (Kotlin puro, usa interfaz existente) |
| 2 | UI: `WeightHistoryUiState`, `WeightHistoryViewModel` | Hito 1 |
| 3 | UI: Reemplazar stub de `WeightHistoryScreen` con lista funcional + strings | Hito 2 |
| 4 | UI: Ajustar `BottomNavigationBar` para marcar Configuración activo en rutas hijas del flujo perfil | — (independiente) |

---

### Notas de auditoría

1. **CA-02.01 ya está cubierto por HU-01.** El `ProfileViewModel` en C1 ya invoca `UpdateWeightUseCase` cuando el peso cambia, que a su vez inserta un nuevo `WeightRecordEntity` sin borrar anteriores. No se requiere trabajo adicional para este CA.
2. **CA-02.03 (tendencia temporal) se cumple mediante la lista cronológica ordenada.** Ni el wireframe C2 ni la Especificación Visual §8 C2 definen un componente gráfico de tendencia. La presentación descendente por fecha con pesos visibles permite al ejecutante identificar la tendencia visualmente (ascendente, descendente o estable) leyendo los valores secuenciales.
3. **CA-02.05 (conservación) ya está garantizado por diseño.** `updateWeight()` en `ProfileRepositoryImpl` hace `INSERT`, nunca `UPDATE` ni `DELETE`. La tabla `weight_record` es append-only.
4. **"Registro inicial" se identifica por `MIN(date)`** (Modelo de Datos §3.9), no por flag. En la lista DESC, es el último elemento. El ViewModel lo calcula comparando contra el último ítem de la lista ordenada.
5. **No se requieren cambios en Data Layer ni DI.** `WeightRecordDao.getAllDescByDate()`, `ProfileRepository.getAllWeightRecords()` y `ProfileRepositoryImpl.getAllWeightRecords()` ya existen. El `DatabaseModule` ya provee `WeightRecordDao`. No hay nuevas entidades, DAOs ni rutinas Hilt.
6. **El NavHost no requiere cambios estructurales.** La ruta `weight-history` ya existe y ya inyecta `WeightHistoryScreen` con el callback `onNavigateBack`. Solo cambia la implementación interna del composable para aceptar el ViewModel.
7. **Formato de fecha**: Según wireframe C2 el formato es "dd mmm yyyy" (ej: "10 feb 2026"). Se usa `DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es"))` para garantizar español en cumplimiento de RNF08, independientemente del locale del dispositivo.
8. **`BottomNavigationBar` requiere ajuste de selección** para que Configuración se marque como activo cuando la ruta actual es `profile` o `weight-history`. Esto no es un cambio estructural sino una extensión de la lógica de `selected` existente.
9. **Compatibilidad con HU-19 (Backup y Restauración) verificada.** HU-19 lista "historial de peso" como dato incluido en backup/restore, confirmando que el diseño append-only de `weight_record` es correcto.
