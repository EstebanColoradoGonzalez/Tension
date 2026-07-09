## Refinamiento Técnico (Developer)

**Autor**: Esteban Colorado González | **Fecha**: 2026-02-20

---

### Consideraciones Generales

**Basado en análisis arquitectónico:**
Análisis Arquitectónico de HU-19 con 11 decisiones fundamentadas, 16 tablas, 2 pantallas (J2, J3), y 15 CAs. Patrón MVVM con capa Domain explícita (ADR-05). Última historia del sistema — consolida HU-31 y HU-32 originales.

**Nivel de complejidad:**
MEDIA — Conceptualmente directa (export = SELECT * → JSON, import = JSON → DELETE + INSERT) pero con complejidad operacional en: (1) el orden FK estricto para DELETE/INSERT de 16 tablas dentro de una transacción atómica, (2) la integración con APIs de Android (`CreateDocument`, `OpenDocument`, `FileProvider`, `ContentResolver`) que son inherentemente asíncronas, (3) la validación robusta del archivo de backup antes de la operación destructiva, (4) el manejo de rollback (CA-19.15) y la navegación post-import con limpieza de back stack.

**Riesgos técnicos conocidos:**
1. `CreateDocument` launcher requiere coordinación ViewModel↔`ActivityResultContract` — el ViewModel genera el JSON pero el launcher opera a nivel Composable.
2. JSON de 2 años de datos (~5-15 MB) se mantiene in-memory como `String` — trivial en dispositivos modernos (4+ GB RAM).
3. `SupportSQLiteDatabase` raw SQL sin verificación de tipos en compilación — los queries son strings literales, un typo solo se detecta en runtime.
4. Post-import: los ViewModels Hilt mantienen stale data en memoria — la navegación `popUpTo(graph.id) { inclusive = true }` destruye TODOS los composables y ViewModels del NavGraph.

**Patrones y convenciones del equipo:**
- Código fuente en inglés, UI y datos de dominio en español (Arquitectura Técnica §5.1)
- Naming: `{Feature}Screen`, `{Feature}ViewModel`, `{Acción}{Entidad}UseCase`, `{Entidad}Repository`/`{Entidad}RepositoryImpl`, `{Entidad}Entity`, `{Entidad}Dao` (§5.2)
- Estructura Composable: hiltViewModel() + collectAsStateWithLifecycle() + LaunchedEffect para eventos (§5.3)
- Estructura ViewModel: `_uiState MutableStateFlow` / `uiState StateFlow` + `_events MutableSharedFlow` / `events SharedFlow` (§5.4)
- Sealed classes para UiState (Loading, Success, Error) y Events
- `operator fun invoke()` en Use Cases
- Callbacks en Composables con prefijo `on` (onNavigateBack, onRegisterProfile)

**Dependencias nuevas a instalar:**
Ninguna. `org.json.JSONObject/JSONArray` están incluidos en el Android SDK. `FileProvider` es parte de `androidx.core`. `ActivityResultContracts` es parte de `androidx.activity`.

**Estrategia de testing:**
JUnit 4 (ADR-18) + MockK + kotlinx-coroutines-test | Tests unitarios para 7 componentes: `BackupRepositoryImpl` (7 casos), `BackupFileManager` (2 casos), 3 Use Cases (delegación), 2 ViewModels (máquina de estados) | Cobertura: 100% Use Cases + Repository + FileManager + ViewModels

---

### Historias Relacionadas Consultadas

**Implementaciones similares analizadas:**
- **HU-18**: Creó `AlertRepository`/`AlertRepositoryImpl` — mismo patrón que se reutiliza para `BackupRepository`. La tabla `alert` con alertas activas y resueltas se incluye en el backup.
- **HU-05**: Creó `ImageStorageHelper` en `data.local.storage` con inyección de `@ApplicationContext` y uso de `ContentResolver.openInputStream(uri)`. Precedente arquitectónico exacto para `BackupFileManager`.
- **HU-01**: Creó `ProfileRepositoryImpl` que inicializa `RotationStateEntity`. Relevante para entender la secuencia de INSERT del backup: `profile` debe insertarse antes de `rotation_state` (FK implícita).

**Patrones de código reutilizados:**
- `ImageStorageHelper` → `BackupFileManager` (mismo patrón de inyección de Context + uso de ContentResolver)
- `AlertRepositoryImpl` → `BackupRepositoryImpl` (mismo patrón de @Singleton + @Binds binding)
- Transacción atómica de HU-01 `createProfile` → `importFromJson` (BEGIN → DELETE/INSERT → COMMIT/ROLLBACK → END)

**Mejores prácticas aplicadas:**
- Raw SQL genérico sobre `SupportSQLiteDatabase` — evita inyectar 15 DAOs y crear 30 métodos nuevos
- Transacción atómica con rollback automático via `beginTransaction()`/`endTransaction()`
- `CreateDocument` SAF como mecanismo primario — cero permisos runtime, texto de ubicación veraz
- `FileProvider` para compartir como acción secundaria — estándar de Android
- Separación de `BackupFileManager` (I/O de archivos) y `BackupRepositoryImpl` (lógica de datos) — Single Responsibility
- Constantes de orden FK definidas una sola vez como listas inmutables

---

### Tareas de Implementación

#### Fase 1: Domain Models

**ACs vinculados:** CA-19.02, CA-19.09

##### Domain Layer — Modelos

- [ ] **Crear `BackupMetadata`** (CA-19.02)
  - [ ] Crear archivo: `domain/model/BackupMetadata.kt`
  - [ ] `data class BackupMetadata(val appVersion: String, val schemaVersion: Int, val exportDate: String, val recordCount: Int)`
  - [ ] `appVersion`: versión de la app (ej: `"1.0"`)
  - [ ] `schemaVersion`: versión del schema de Room — `TensionDatabase.VERSION` (actualmente 7)
  - [ ] `exportDate`: ISO 8601 timestamp (ej: `"2026-02-20T14:30:00"`)
  - [ ] `recordCount`: suma total de filas de todas las 16 tablas

- [ ] **Crear `BackupValidationResult`** (CA-19.09)
  - [ ] Crear archivo: `domain/model/BackupValidationResult.kt`
  - [ ] `data class BackupValidationResult(val isValid: Boolean, val metadata: BackupMetadata?, val sessionCount: Int, val errorMessage: String?)`
  - [ ] `isValid`: true si el JSON tiene estructura correcta y `schemaVersion` == `TensionDatabase.VERSION`
  - [ ] `metadata`: metadatos extraídos del JSON (null si inválido)
  - [ ] `sessionCount`: cantidad de sesiones en el backup (para mostrar en J3 card validación OK)
  - [ ] `errorMessage`: mensaje de error descriptivo (null si válido)

#### Fase 2: BackupRepository

**ACs vinculados:** CA-19.01, CA-19.02, CA-19.09, CA-19.11, CA-19.12, CA-19.15

##### Domain Layer — Interfaz

- [ ] **Crear `BackupRepository`** (CA-19.01, CA-19.09, CA-19.11)
  - [ ] Crear archivo: `domain/repository/BackupRepository.kt`
  - [ ] `interface BackupRepository`
  - [ ] `suspend fun exportToJson(): String` — genera JSON completo con metadata + 16 tablas
  - [ ] `fun validateBackup(json: String): BackupValidationResult` — valida estructura y schemaVersion
  - [ ] `suspend fun importFromJson(json: String)` — reemplaza toda la BD en transacción atómica

##### Data Layer — Implementación

- [ ] **Crear `BackupRepositoryImpl`** (CA-19.01, CA-19.02, CA-19.09, CA-19.11, CA-19.12, CA-19.15)
  - [ ] Crear archivo: `data/repository/BackupRepositoryImpl.kt`
  - [ ] `@Singleton class BackupRepositoryImpl @Inject constructor(private val database: TensionDatabase, @ApplicationContext private val context: Context) : BackupRepository`
  - [ ] Constante `TABLE_ORDER_INSERT` (16 tablas, parents first):
    `profile`, `rotation_state`, `weight_record`, `module`, `muscle_zone`, `equipment_type`, `deload`, `exercise`, `exercise_muscle_zone`, `module_version`, `plan_assignment`, `session`, `session_exercise`, `exercise_set`, `exercise_progression`, `alert`
  - [ ] Constante `TABLE_ORDER_DELETE`: `TABLE_ORDER_INSERT.reversed()` (children first)
  - [ ] Constante `SCHEMA_VERSION` = 7
  - [ ] Constante `APP_VERSION` = `"1.0"`

- [ ] **Implementar `exportToJson()`** (CA-19.01, CA-19.02, CA-19.04)
  - [ ] `val db = database.openHelper.readableDatabase`
  - [ ] Crear `JSONObject` raíz con keys `"metadata"` y `"data"`
  - [ ] Para cada tabla en `TABLE_ORDER_INSERT`:
    ```kotlin
    val cursor = db.query("SELECT * FROM $table")
    val rows = JSONArray()
    while (cursor.moveToNext()) {
        val row = JSONObject()
        for (i in 0 until cursor.columnCount) {
            val name = cursor.getColumnName(i)
            when (cursor.getType(i)) {
                Cursor.FIELD_TYPE_NULL -> row.put(name, JSONObject.NULL)
                Cursor.FIELD_TYPE_INTEGER -> row.put(name, cursor.getLong(i))
                Cursor.FIELD_TYPE_FLOAT -> row.put(name, cursor.getDouble(i))
                Cursor.FIELD_TYPE_STRING -> row.put(name, cursor.getString(i))
                Cursor.FIELD_TYPE_BLOB -> row.put(name, Base64.encodeToString(cursor.getBlob(i), Base64.NO_WRAP))
            }
        }
        rows.put(row)
    }
    cursor.close()
    data.put(table, rows)
    ```
  - [ ] Metadata: `appVersion`, `schemaVersion` (SCHEMA_VERSION), `exportDate` (ISO 8601 now), `recordCount` (acumulador)
  - [ ] Retornar `root.toString(2)` (indentación de 2 espacios)
  - [ ] Ejecutar en `Dispatchers.IO` (delegado al UseCase/ViewModel)

- [ ] **Implementar `validateBackup()`** (CA-19.09, CA-19.12)
  - [ ] `try { JSONObject(json) }` — si lanza `JSONException` → invalid con `errorMessage = R.string.import_backup_invalid_json`
  - [ ] Verificar key `"metadata"` existe — si no → invalid con `R.string.import_backup_invalid_format`
  - [ ] Extraer `metadata.schemaVersion` — si no existe o `!= SCHEMA_VERSION` → invalid con `R.string.import_backup_incompatible_version` (CA-19.12 — stub para v2+)
  - [ ] Verificar key `"data"` existe — si no → invalid
  - [ ] Verificar que `data` contiene las tablas de `TABLE_ORDER_INSERT` — si falta alguna → invalid con `R.string.import_backup_incomplete`
  - [ ] Si todo OK → `BackupValidationResult(isValid = true, metadata = parsed, sessionCount = data.getJSONArray("session").length(), errorMessage = null)`

- [ ] **Implementar `importFromJson()`** (CA-19.11, CA-19.15)
  - [ ] `val db = database.openHelper.writableDatabase`
  - [ ] `val data = JSONObject(json).getJSONObject("data")`
  - [ ] Transacción atómica:
    ```kotlin
    db.beginTransaction()
    try {
        TABLE_ORDER_DELETE.forEach { table ->
            db.execSQL("DELETE FROM $table")
        }
        TABLE_ORDER_INSERT.forEach { table ->
            val rows = data.getJSONArray(table)
            for (i in 0 until rows.length()) {
                val row = rows.getJSONObject(i)
                val cv = ContentValues()
                row.keys().forEach { key ->
                    when {
                        row.isNull(key) -> cv.putNull(key)
                        else -> when (val value = row.get(key)) {
                            is Long -> cv.put(key, value)
                            is Int -> cv.put(key, value.toLong())
                            is Double -> cv.put(key, value)
                            is String -> cv.put(key, value)
                            else -> cv.put(key, value.toString())
                        }
                    }
                }
                db.insert(table, SQLiteDatabase.CONFLICT_REPLACE, cv)
            }
        }
        db.setTransactionSuccessful()
    } finally {
        db.endTransaction()
    }
    ```
  - [ ] Re-lanzar cualquier excepción para que el ViewModel la capture y muestre ERROR

##### DI Layer

- [ ] **Agregar binding en `RepositoryModule`**
  - [ ] Modificar: `di/RepositoryModule.kt`
  - [ ] Agregar 7mo binding: `@Binds @Singleton abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository`

#### Fase 3: BackupFileManager + FileProvider

**ACs vinculados:** CA-19.03, CA-19.06, CA-19.08

##### Data Layer — Storage Helper

- [ ] **Crear `BackupFileManager`** (CA-19.03, CA-19.06, CA-19.08)
  - [ ] Crear archivo: `data/local/storage/BackupFileManager.kt`
  - [ ] `@Singleton class BackupFileManager @Inject constructor(@ApplicationContext private val context: Context)`
  - [ ] `fun writeToUri(json: String, uri: Uri)`: escribe JSON al URI via `context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }`
  - [ ] `fun readFromUri(uri: Uri): String`: lee JSON via `context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() } ?: throw IOException("Cannot read URI")`
  - [ ] `fun writeToCacheForShare(json: String, fileName: String): File`: `File(context.cacheDir, fileName).also { it.writeText(json) }`
  - [ ] `fun getShareableUri(file: File): Uri`: `FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)`
  - [ ] `fun extractDisplayPath(uri: Uri): String`: si URI contiene `"Download"` o `"Descargas"` → `"Descargas/"`. Si no → último segmento del path o `"Almacenamiento del dispositivo"`
  - [ ] `fun generateBackupFileName(): String`: `"tension_backup_${LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.json"`

##### Resources — FileProvider

- [ ] **Crear `file_paths.xml`**
  - [ ] Crear archivo: `res/xml/file_paths.xml`
  - [ ] Contenido:
    ```xml
    <?xml version="1.0" encoding="utf-8"?>
    <paths>
        <cache-path name="backups" path="." />
    </paths>
    ```

- [ ] **Registrar `FileProvider` en `AndroidManifest.xml`** (CA-19.06)
  - [ ] Modificar: `AndroidManifest.xml`
  - [ ] Dentro de `<application>`, agregar:
    ```xml
    <provider
        android:name="androidx.core.content.FileProvider"
        android:authorities="${applicationId}.fileprovider"
        android:exported="false"
        android:grantUriPermissions="true">
        <meta-data
            android:name="android.support.FILE_PROVIDER_PATHS"
            android:resource="@xml/file_paths" />
    </provider>
    ```
  - [ ] Cero permisos runtime — SAF + FileProvider no los requieren en API 26+ (CA-19.06, RNF27)

#### Fase 4: Use Cases

**ACs vinculados:** CA-19.01, CA-19.09, CA-19.11

- [ ] **Crear `ExportBackupUseCase`** (CA-19.01)
  - [ ] Crear archivo: `domain/usecase/backup/ExportBackupUseCase.kt`
  - [ ] `class ExportBackupUseCase @Inject constructor(private val backupRepository: BackupRepository)`
  - [ ] `suspend operator fun invoke(): String = backupRepository.exportToJson()`

- [ ] **Crear `ValidateBackupUseCase`** (CA-19.09)
  - [ ] Crear archivo: `domain/usecase/backup/ValidateBackupUseCase.kt`
  - [ ] `class ValidateBackupUseCase @Inject constructor(private val backupRepository: BackupRepository)`
  - [ ] `operator fun invoke(json: String): BackupValidationResult = backupRepository.validateBackup(json)`

- [ ] **Crear `ImportBackupUseCase`** (CA-19.11)
  - [ ] Crear archivo: `domain/usecase/backup/ImportBackupUseCase.kt`
  - [ ] `class ImportBackupUseCase @Inject constructor(private val backupRepository: BackupRepository)`
  - [ ] `suspend operator fun invoke(json: String) = backupRepository.importFromJson(json)`

#### Fase 5: J2 — Exportar Respaldo

**ACs vinculados:** CA-19.01, CA-19.02, CA-19.03, CA-19.04, CA-19.05, CA-19.06, CA-19.07

##### UI Layer — Estado

- [ ] **Crear `ExportBackupUiState`**
  - [ ] Crear archivo: `ui/settings/ExportBackupUiState.kt`
  - [ ] `sealed interface ExportBackupUiState`:
    - `data object Idle` — advertencia + botón "Exportar datos"
    - `data object Exporting` — `LinearProgressIndicator` indeterminate
    - `data class Success(val fileName: String, val displayPath: String)` — card confirmación + botón "Compartir"
    - `data class Error(val message: String)` — Snackbar + botón habilitado para reintentar

##### UI Layer — ViewModel

- [ ] **Crear `ExportBackupViewModel`** (CA-19.01, CA-19.03, CA-19.04, CA-19.07)
  - [ ] Crear archivo: `ui/settings/ExportBackupViewModel.kt`
  - [ ] `@HiltViewModel class ExportBackupViewModel @Inject constructor(private val exportBackupUseCase: ExportBackupUseCase, private val backupFileManager: BackupFileManager)`
  - [ ] `private val _uiState = MutableStateFlow<ExportBackupUiState>(Idle)` + `val uiState: StateFlow<...> = _uiState.asStateFlow()`
  - [ ] Variable interna: `private var generatedJson: String? = null`
  - [ ] `fun generateBackupFileName(): String = backupFileManager.generateBackupFileName()`
  - [ ] **`fun export(uri: Uri)`**: `viewModelScope.launch(Dispatchers.IO)`:
    1. `_uiState.value = Exporting`
    2. `generatedJson = exportBackupUseCase()`
    3. `backupFileManager.writeToUri(generatedJson!!, uri)`
    4. `_uiState.value = Success(fileName = generateBackupFileName(), displayPath = backupFileManager.extractDisplayPath(uri))`
    - Catch: `_uiState.value = Error(e.message ?: "Error al exportar")`
  - [ ] **`fun share(context: Context)`**: si `generatedJson != null`:
    ```kotlin
    val file = backupFileManager.writeToCacheForShare(generatedJson!!, generateBackupFileName())
    val shareUri = backupFileManager.getShareableUri(file)
    Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, shareUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }.let { context.startActivity(Intent.createChooser(it, null)) }
    ```
  - [ ] **`fun onExportPickerCancelled()`**: `_uiState.value = Idle`

##### UI Layer — Pantalla

- [ ] **Crear `ExportBackupScreen`** (CA-19.03, CA-19.05, CA-19.07)
  - [ ] Crear archivo: `ui/settings/ExportBackupScreen.kt`
  - [ ] Params: `onNavigateBack: () -> Unit`, `viewModel: ExportBackupViewModel = hiltViewModel()`
  - [ ] Launcher: `rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> if (uri != null) viewModel.export(uri) else viewModel.onExportPickerCancelled() }`
  - [ ] Scaffold con `CenterAlignedTopAppBar`: "Exportar Respaldo", navigationIcon ArrowBack → `onNavigateBack`
  - [ ] Body `when (uiState)`:
    - **Idle** (CA-19.05):
      - `OutlinedCard` borde Outline, corner 12dp — ícono ⚠️ 24dp tint `#E65100` + Body Medium: "El archivo de respaldo contiene todos tus datos de entrenamiento y no está cifrado."
      - `FilledButton` full width, margin top 24dp: "Exportar datos". onClick: `launcher.launch(viewModel.generateBackupFileName())`
    - **Exporting** (CA-19.07):
      - `FilledCard` Surface Container — "Exportando datos…" + `LinearProgressIndicator` indeterminate Primary
    - **Success** (CA-19.07):
      - `FilledCard` Tertiary Container (`#E0EEDD`) — ✅ 24dp + "Respaldo exportado" Title Medium + "Archivo: ${fileName}" Body Medium + "Ubicación: ${displayPath}" Body Small
      - `FilledTonalButton` Secondary Container: "Compartir". onClick: `viewModel.share(LocalContext.current)`
    - **Error**: Snackbar con mensaje. `_uiState.value = Idle` para habilitar reintento

#### Fase 6: J3 — Importar Respaldo

**ACs vinculados:** CA-19.08, CA-19.09, CA-19.10, CA-19.11, CA-19.13, CA-19.14, CA-19.15

##### UI Layer — Estado

- [ ] **Crear `ImportBackupUiState`**
  - [ ] Crear archivo: `ui/settings/ImportBackupUiState.kt`
  - [ ] `sealed interface ImportBackupUiState`:
    - `data object Idle` — botón "Seleccionar archivo"
    - `data object Validating` — `CircularProgressIndicator` centrado
    - `data class Validated(val result: BackupValidationResult)` — card OK + advertencia + botón destructivo
    - `data class Invalid(val errorMessage: String)` — card error
    - `data object Importing` — `LinearProgressIndicator` indeterminate
    - `data object Success` — ✅ 48dp + auto-nav a B1 tras 2s
    - `data class Error(val message: String)` — card rollback + botón "Volver a Configuración"

##### UI Layer — ViewModel

- [ ] **Crear `ImportBackupViewModel`** (CA-19.08, CA-19.09, CA-19.10, CA-19.11, CA-19.14, CA-19.15)
  - [ ] Crear archivo: `ui/settings/ImportBackupViewModel.kt`
  - [ ] `@HiltViewModel class ImportBackupViewModel @Inject constructor(private val validateBackupUseCase: ValidateBackupUseCase, private val importBackupUseCase: ImportBackupUseCase, private val backupFileManager: BackupFileManager)`
  - [ ] `private var pendingJson: String? = null`
  - [ ] **`fun selectFile(uri: Uri)`** (CA-19.08, CA-19.09):
    1. `_uiState.value = Validating`
    2. `viewModelScope.launch(Dispatchers.IO)`:
       - `val json = backupFileManager.readFromUri(uri)`
       - `val result = validateBackupUseCase(json)`
       - Si `result.isValid`: `pendingJson = json; _uiState.value = Validated(result)`
       - Si no: `_uiState.value = Invalid(result.errorMessage ?: "Archivo no válido")`
    - Catch IO: `_uiState.value = Invalid("Error al leer el archivo")`
  - [ ] **`fun confirmImport()`** (CA-19.10, CA-19.11, CA-19.13, CA-19.14, CA-19.15):
    1. `val json = pendingJson ?: return`
    2. `_uiState.value = Importing`
    3. `viewModelScope.launch(Dispatchers.IO)`:
       - `importBackupUseCase(json)`
       - `pendingJson = null; _uiState.value = Success`
    - Catch: `_uiState.value = Error("La importación falló. Tus datos originales han sido preservados.")`
  - [ ] **`fun cancel()`**: `pendingJson = null; _uiState.value = Idle`

##### UI Layer — Pantalla

- [ ] **Crear `ImportBackupScreen`** (CA-19.08, CA-19.09, CA-19.10, CA-19.11, CA-19.14, CA-19.15)
  - [ ] Crear archivo: `ui/settings/ImportBackupScreen.kt`
  - [ ] Params: `onNavigateBack: () -> Unit`, `onNavigateToHome: () -> Unit`, `viewModel: ImportBackupViewModel = hiltViewModel()`
  - [ ] Launcher: `rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) viewModel.selectFile(uri) }`
  - [ ] Scaffold con `CenterAlignedTopAppBar`: "Importar Respaldo", ArrowBack → `onNavigateBack` (deshabilitado en Importing y Success)
  - [ ] Body `when (uiState)`:
    - **Idle** (CA-19.08):
      - `OutlinedButton` full width, leading icon `FileOpen`: "Seleccionar archivo". onClick: `launcher.launch(arrayOf("application/json"))`
    - **Validating**: `CircularProgressIndicator` centrado
    - **Validated** (CA-19.09, CA-19.10):
      - `FilledCard` Tertiary Container — ✅ + "Archivo válido" Title Medium + "Versión: ${metadata.appVersion}" + "Fecha del respaldo: ${metadata.exportDate}" + "Sesiones incluidas: ${result.sessionCount}"
      - `FilledCard` Error Container — ⚠️ Error + "ATENCIÓN" Title Medium Bold + "Todos los datos actuales serán reemplazados por los datos del respaldo. Esta operación no es reversible."
      - `FilledButton` containerColor Error (#BA1A1A), contentColor OnError (#FFFFFF), full width 48dp: "Restaurar datos". onClick: `viewModel.confirmImport()`
      - `TextButton` Primary centrado: "Cancelar". onClick: `viewModel.cancel(); onNavigateBack()`
    - **Invalid** (CA-19.09):
      - `FilledCard` Error Container (`#FFDAD6`) — ❌ Error + "Archivo no válido" Title Medium + `errorMessage` Body Medium (fallback: `R.string.import_backup_invalid_default`)
    - **Importing** (CA-19.13, CA-19.14):
      - Column: "Restaurando datos…" + `LinearProgressIndicator` indeterminate Primary
    - **Success** (CA-19.14):
      - Column centrada: ✅ 48dp verde + "Datos restaurados exitosamente." Body Large
      - `LaunchedEffect(Unit) { delay(2000L); onNavigateToHome() }`
    - **Error** (CA-19.15):
      - `FilledCard` Error Container — ❌ + "La importación falló. Tus datos originales han sido preservados."
      - `OutlinedButton` borderColor Outline: "Volver a Configuración". onClick: `onNavigateBack()`

#### Fase 7: Settings + Navegación

**ACs vinculados:** CA-19.03, CA-19.08

##### UI Layer — Modificar SettingsScreen

- [ ] **Agregar sección "Datos" a `SettingsScreen`**
  - [ ] Modificar: `ui/settings/SettingsScreen.kt`
  - [ ] Agregar 2 parámetros: `onNavigateToExportBackup: () -> Unit`, `onNavigateToImportBackup: () -> Unit`
  - [ ] Después del `HorizontalDivider()` existente (línea 48), agregar:
    ```kotlin
    Text(
        text = stringResource(R.string.settings_section_data),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
    )
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_export_backup)) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
        modifier = Modifier.clickable { onNavigateToExportBackup() },
    )
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_import_backup)) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
        modifier = Modifier.clickable { onNavigateToImportBackup() },
    )
    ```
  - [ ] **Nota:** NO agregar `HorizontalDivider()` entre los ListItems de la sección "Datos" — la spec solo usa Dividers entre secciones, no entre items de la misma sección.

##### Navigation Layer

- [ ] **Agregar rutas en `NavigationRoutes`**
  - [ ] Modificar: `ui/navigation/NavigationRoutes.kt`
  - [ ] `const val EXPORT_BACKUP = "export-backup"`
  - [ ] `const val IMPORT_BACKUP = "import-backup"`

- [ ] **Actualizar `TensionNavHost`**
  - [ ] Modificar: `ui/navigation/TensionNavHost.kt`
  - [ ] Actualizar composable `SETTINGS` para pasar las 2 nuevas lambdas a `SettingsScreen`:
    ```kotlin
    SettingsScreen(
        onNavigateToProfile = { navController.navigate(NavigationRoutes.PROFILE) },
        onNavigateToExportBackup = { navController.navigate(NavigationRoutes.EXPORT_BACKUP) },
        onNavigateToImportBackup = { navController.navigate(NavigationRoutes.IMPORT_BACKUP) },
    )
    ```
  - [ ] Agregar composable `EXPORT_BACKUP`:
    ```kotlin
    composable(NavigationRoutes.EXPORT_BACKUP) {
        ExportBackupScreen(onNavigateBack = { navController.popBackStack() })
    }
    ```
  - [ ] Agregar composable `IMPORT_BACKUP`:
    ```kotlin
    composable(NavigationRoutes.IMPORT_BACKUP) {
        ImportBackupScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToHome = {
                navController.navigate(NavigationRoutes.HOME) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            },
        )
    }
    ```

- [ ] **Actualizar `BottomNavigationBar`**
  - [ ] Modificar: `ui/components/BottomNavigationBar.kt`
  - [ ] Tab SETTINGS:
    - Antes: `childRoutes = setOf(PROFILE, WEIGHT_HISTORY)`
    - Después: `childRoutes = setOf(PROFILE, WEIGHT_HISTORY, EXPORT_BACKUP, IMPORT_BACKUP)`

#### Fase 8: Strings

**ACs vinculados:** CA-19.05, CA-19.07, CA-19.09, CA-19.10, CA-19.14, CA-19.15

- [ ] **Agregar strings en `strings.xml`**
  - [ ] Modificar: `res/values/strings.xml`
  - [ ] Sección `<!-- Settings J1 — Datos -->`:
    - `settings_section_data` = "Datos"
    - `settings_export_backup` = "Exportar respaldo"
    - `settings_import_backup` = "Importar respaldo"
  - [ ] Sección `<!-- Export Backup J2 -->`:
    - `export_backup_title` = "Exportar Respaldo"
    - `export_backup_warning` = "El archivo de respaldo contiene todos tus datos de entrenamiento y no está cifrado."
    - `export_backup_button` = "Exportar datos"
    - `export_backup_exporting` = "Exportando datos…"
    - `export_backup_success_title` = "Respaldo exportado"
    - `export_backup_file_label` = "Archivo: %1$s"
    - `export_backup_location_label` = "Ubicación: %1$s"
    - `export_backup_share` = "Compartir"
    - `export_backup_error` = "Error al exportar los datos"
  - [ ] Sección `<!-- Import Backup J3 -->`:
    - `import_backup_title` = "Importar Respaldo"
    - `import_backup_select_file` = "Seleccionar archivo"
    - `import_backup_valid_title` = "Archivo válido"
    - `import_backup_version` = "Versión: %1$s"
    - `import_backup_date` = "Fecha del respaldo: %1$s"
    - `import_backup_sessions` = "Sesiones incluidas: %1$d"
    - `import_backup_invalid_title` = "Archivo no válido"
    - `import_backup_warning_title` = "ATENCIÓN"
    - `import_backup_warning_message` = "Todos los datos actuales serán reemplazados por los datos del respaldo. Esta operación no es reversible."
    - `import_backup_restore_button` = "Restaurar datos"
    - `import_backup_cancel` = "Cancelar"
    - `import_backup_importing` = "Restaurando datos…"
    - `import_backup_success` = "Datos restaurados exitosamente."
    - `import_backup_error` = "La importación falló. Tus datos originales han sido preservados."
    - `import_backup_back_to_settings` = "Volver a Configuración"
    - `import_backup_read_error` = "Error al leer el archivo"
    - `import_backup_invalid_default` = "El archivo seleccionado no es un respaldo válido o está corrupto."
    - `import_backup_invalid_json` = "El archivo no contiene JSON válido"
    - `import_backup_invalid_format` = "Formato de respaldo no reconocido"
    - `import_backup_incompatible_version` = "Versión de respaldo incompatible (esperada: %1$d, encontrada: %2$d)"
    - `import_backup_incomplete` = "Archivo de respaldo incompleto"

#### Fase 9: Tests Unitarios

**ACs vinculados:** Todos los CAs

##### Tests — BackupRepositoryImpl

- [ ] **`BackupRepositoryImplTest`** (CA-19.01, CA-19.02, CA-19.04, CA-19.09, CA-19.11, CA-19.12, CA-19.15)
  - [ ] Crear archivo: `test/.../data/repository/BackupRepositoryImplTest.kt`
  - [ ] Setup: mock `TensionDatabase`, mock `SupportSQLiteDatabase`, mock `SupportSQLiteOpenHelper`
  - [ ] Caso 1: `exportToJson produces valid JSON with metadata and all 16 tables`
  - [ ] Caso 2: `exportToJson includes correct recordCount in metadata`
  - [ ] Caso 3: `validateBackup returns valid for correct JSON` — `isValid == true` + `sessionCount` correcto
  - [ ] Caso 4: `validateBackup returns invalid for malformed JSON` — string no JSON → `isValid == false`
  - [ ] Caso 5: `validateBackup returns invalid for wrong schemaVersion` — JSON con `schemaVersion = 99` → `isValid == false` (CA-19.12)
  - [ ] Caso 6: `validateBackup returns invalid for missing data section`
  - [ ] Caso 7: `importFromJson calls beginTransaction and setTransactionSuccessful`
  - [ ] Caso 8: `importFromJson deletes tables in children-first order` — capture `execSQL` calls, verify DELETE order matches `TABLE_ORDER_DELETE`
  - [ ] Caso 9: `importFromJson inserts tables in parents-first order` — verify INSERT order matches `TABLE_ORDER_INSERT`
  - [ ] Caso 10: `importFromJson rolls back on exception` — mock `db.insert` throws on 5th table → `setTransactionSuccessful` NOT called, `endTransaction` IS called (CA-19.15)

##### Tests — BackupFileManager

- [ ] **`BackupFileManagerTest`** (CA-19.03, CA-19.08)
  - [ ] Crear archivo: `test/.../data/local/storage/BackupFileManagerTest.kt`
  - [ ] Caso 1: `generateBackupFileName returns correct format` — matches `tension_backup_YYYYMMDD.json`
  - [ ] Caso 2: `extractDisplayPath returns Descargas for download URI` — mock URI con "Download" en path

##### Tests — Use Cases

- [ ] **`ExportBackupUseCaseTest`**: `invoke delegates to repository exportToJson`
- [ ] **`ValidateBackupUseCaseTest`**: `invoke delegates to repository validateBackup with correct json`
- [ ] **`ImportBackupUseCaseTest`**: `invoke delegates to repository importFromJson`

##### Tests — ViewModels

- [ ] **`ExportBackupViewModelTest`** (CA-19.01, CA-19.03, CA-19.04, CA-19.07)
  - [ ] Setup: mock UseCase, mock FileManager. `Dispatchers.setMain(StandardTestDispatcher())` en `@Before`
  - [ ] Caso 1: `export transitions Idle → Exporting → Success` con fileName y displayPath correctos
  - [ ] Caso 2: `export transitions to Error on IOException`
  - [ ] Caso 3: `onExportPickerCancelled resets to Idle`
  - [ ] Caso 4: `generateBackupFileName delegates to BackupFileManager`

- [ ] **`ImportBackupViewModelTest`** (CA-19.08, CA-19.09, CA-19.10, CA-19.11, CA-19.14, CA-19.15)
  - [ ] Setup: mock ValidateUseCase, mock ImportUseCase, mock FileManager. `Dispatchers.setMain(StandardTestDispatcher())` en `@Before`
  - [ ] Caso 1: `selectFile with valid JSON transitions Idle → Validating → Validated`
  - [ ] Caso 2: `selectFile with invalid JSON transitions to Invalid`
  - [ ] Caso 3: `selectFile with read error transitions to Invalid` con mensaje "Error al leer el archivo"
  - [ ] Caso 4: `confirmImport transitions Validated → Importing → Success`
  - [ ] Caso 5: `confirmImport transitions to Error on exception` con mensaje de rollback (CA-19.15)
  - [ ] Caso 6: `cancel resets to Idle and clears pendingJson`

#### Fase 10: QA y Deployment

- [ ] **Ejecutar Agente Peer Review** (MANUAL)
- [ ] **Resolver incidentes del Peer Review** (MANUAL, condicional)
- [ ] **Crear Pull Request** (MANUAL)
- [ ] **Ejecutar pipeline deployment DEV** (MANUAL)
- [ ] **Diseñar y ejecutar pruebas manuales** (MANUAL)

---

### Vinculación CAs → Fases

| CA | Fase(s) | Mecanismo |
|---|---|---|
| CA-19.01 | Fase 2, Fase 4, Fase 5 | `BackupRepositoryImpl.exportToJson()` + `ExportBackupViewModel` |
| CA-19.02 | Fase 1, Fase 2 | `BackupMetadata` + JSON metadata en export |
| CA-19.03 | Fase 3, Fase 5 | `CreateDocument` SAF + `BackupFileManager.writeToUri()` |
| CA-19.04 | Fase 2 | Raw SQL secuencial + `Dispatchers.IO` |
| CA-19.05 | Fase 5 | `OutlinedCard` con advertencia en J2 Idle |
| CA-19.06 | Fase 3 | SAF + FileProvider — cero permisos runtime |
| CA-19.07 | Fase 5 | `LinearProgressIndicator` + `FilledCard` Success en J2 |
| CA-19.08 | Fase 6 | `OpenDocument` launcher en J3 |
| CA-19.09 | Fase 2, Fase 6 | `validateBackup()` + card Validated/Invalid en J3 |
| CA-19.10 | Fase 6 | Card Error Container + botón destructivo en J3 |
| CA-19.11 | Fase 2, Fase 4, Fase 6 | Transacción atómica DELETE/INSERT |
| CA-19.12 | Fase 2 | Validación de `schemaVersion` en `validateBackup()` |
| CA-19.13 | Fase 2, Fase 6 | Lectura URI + parseo JSON + transacción |
| CA-19.14 | Fase 6 | `LinearProgressIndicator` + auto-nav a B1 tras 2s |
| CA-19.15 | Fase 2, Fase 6 | Transacción atómica con rollback automático |

---

### Notas de Implementación

- **Cero dependencias nuevas**: `org.json` está incluido en Android SDK
- **Cero migraciones de BD**: la versión permanece en 7
- **Cero componentes NO tocados**: las 16 entidades, 15 DAOs, `TensionDatabase`, `DatabaseModule`, seeders, `Converters` permanecen sin cambios
- **7 archivos nuevos** en Domain Layer (2 models + 1 interface + 1 impl + 3 use cases)
- **5 archivos nuevos** en UI Layer (2 UiState + 2 ViewModel + 1 Screen modification)
- **2 archivos nuevos** en Data Layer (BackupFileManager + file_paths.xml)
- **1 archivo nuevo** en DI (binding en RepositoryModule)
- **1 archivo nuevo** en Resources (file_paths.xml)
- **1 archivo nuevo** en Manifest (FileProvider)
- **~30 strings nuevos** en strings.xml
- **~7 archivos de test** cubriendo los 15 CAs
