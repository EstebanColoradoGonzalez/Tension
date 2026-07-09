## Análisis Arquitectónico

> Esta historia consolida HU-31 y HU-32 originales. Es la última HU del sistema (no habilita historias futuras).

### §1 Metadatos

| Campo | Valor |
|---|---|
| **ID Canónico** | HU-19 |
| **ID Legado** | HU-18 |
| **Título** | Backup y Restauración |
| **Requisitos** | RNF15, RNF16, RNF17, RNF18, RNF26, RNF27 |
| **Historias Originales Consolidadas** | HU-31 original (Exportar respaldo — RNF15, RNF17, RNF18, RNF26, RNF27) + HU-32 original (Importar respaldo — RNF16, RNF17, RNF18) |
| **Pantallas** | J2 (ExportBackupScreen), J3 (ImportBackupScreen) |
| **Estado** | Refinado |
| **Dependencias** | HU-01 a HU-18 (las 16 tablas que componen el modelo de datos completo) |
| **Migración DB** | No — la versión permanece en 7. Cero cambios de esquema |
| **Última actualización** | 2026-02-20 |

---

### §2 Dependencias Técnicas e Integración

#### Dependencias de entrada

| Dependencia | Razón |
|---|---|
| HU-01 a HU-18 (todas las historias previas) | Las 16 tablas exportadas fueron definidas y pobladas por esas HUs |
| `TensionDatabase` (v7) | Fuente del raw SQL — `openHelper.readableDatabase` / `writableDatabase` |
| `ImageStorageHelper` | Precedente arquitectónico exacto para `BackupFileManager` (mismo paquete, misma inyección, mismo `ContentResolver`) |
| `RepositoryModule` | Se agrega el 7mo binding: `BackupRepository → BackupRepositoryImpl` |

#### Dependencias de salida

Ninguna — HU-19 es la **última HU** del sistema (consolidación de HU-31 y HU-32 originales). No habilita historias futuras.

#### Dependencias de librerías

**Cero dependencias nuevas.** `org.json.JSONObject/JSONArray` están incluidos en el Android SDK. `FileProvider` es parte de `androidx.core`. `ActivityResultContracts` es parte de `androidx.activity`. El proyecto actualmente no tiene `kotlinx-serialization`, `moshi`, ni `gson` — confirmado en `libs.versions.toml` y `build.gradle.kts`.

#### Componentes NO tocados (verificado en código)

- Las 16 entidades (`*Entity.kt`) — cero cambios de esquema, cero migraciones. La versión de la BD permanece en 7
- Los 15 DAOs — el backup usa raw SQL, no queries tipadas de DAO
- `TensionDatabase.kt` — no se modifica (version = 7, exportSchema = false, PrepopulateCallback intacto)
- `DatabaseModule.kt` — `TensionDatabase` ya es `@Singleton` inyectable, no necesita nuevo provider
- Seeders (`ModuleSeeder`, `ExerciseSeeder`, `PlanSeeder`) — no se tocan; los datos seed se incluyen en el dump JSON como cualquier otra tabla
- `Converters.kt` — no se modifica; el backup exporta fechas como strings tal como Room las almacena
- Todos los flujos UI existentes: B (Home), C (Perfil), D (Catálogo), E (Sesión), F (Historial), G (Métricas), H (Alertas), I (Descarga)

---

### §3 Patrón y Justificación

**Patrón:** Cache-First Export + SAF Import con `BackupRepository` dedicado (raw SQL) + MVVM para 2 pantallas (J2, J3)

**Justificación:** HU-19 tiene dos caras independientes que comparten un formato de archivo JSON autodescriptivo (ADR-10). La cara **export** genera un dump completo de las 16 tablas a través de `SupportSQLiteDatabase` raw queries — evitando inyectar 15 DAOs al usar acceso genérico por cursor. Se escribe directamente via `CreateDocument` (SAF) a la ubicación elegida por el usuario (por defecto Descargas), cumpliendo CA-19.03 sin permisos runtime. La cara **import** usa `ActivityResultContracts.OpenDocument` para selección de archivo, valida metadatos del JSON, y reemplaza toda la BD en una transacción atómica — si falla, rollback preserva datos originales (CA-19.15). Post-import exitoso navega a B1 limpiando todo el back stack (reinicio lógico, Arquitectura Técnica §4.5). Se usa `org.json.JSONObject/JSONArray` (incluido en Android SDK) — cero dependencias nuevas.

---

### §4 Decisiones Fundamentadas

**1. Raw SQL (`SupportSQLiteDatabase`) en lugar de los 15 DAOs tipados para export/import.**

El export necesita `SELECT * FROM table` por cada una de las 16 tablas y el import necesita `DELETE FROM table` + `INSERT INTO table` masivo. Usar los 15 DAOs requeriría: (a) inyectarlos en el constructor del Repository, (b) crear métodos `getAll()` e `insertAll()` en cada DAO que no existen hoy, (c) mapear cada Entity a JSON y viceversa con tipado estricto. En cambio, `SupportSQLiteDatabase` permite iterar tablas genéricamente: cursor → columnas → `JSONObject`, y `JSONObject` → `ContentValues` → `INSERT`. El export e import se resuelven con un loop sobre una lista ordenada de nombres de tabla — ~100 líneas genéricas vs ~300 líneas tipadas por DAO.

**2. `org.json.JSONObject/JSONArray` (Android SDK) sin agregar dependencias de serialización.**

El proyecto no tiene ninguna librería de serialización JSON. `org.json` está incluido en el Android SDK y es suficiente para el patrón cursor-to-JSON del export y JSON-to-ContentValues del import. Agregar `kotlinx-serialization` requeriría: plugin en Gradle, dependencia `kotlinx-serialization-json`, y anotaciones `@Serializable` en las 16 entidades. El beneficio de tipado no justifica la complejidad adicional para un dump genérico de tablas.

**3. `CreateDocument` (SAF) como mecanismo primario de guardado + `ACTION_SEND` como secundario.**

CA-19.03 dice "almacenamiento externo del dispositivo O compartir vía apps del sistema". La Especificación Visual §J2 muestra explícitamente `"Ubicación: Descargas/"` en la card de confirmación post-export. Para que ese texto sea veraz, el export usa `ActivityResultContracts.CreateDocument("application/json")` que abre el file picker nativo (por defecto: carpeta Descargas). Esto permite: (a) cero permisos runtime (CA-19.06, RNF27), (b) guardado directo en almacenamiento externo (CA-19.03), (c) texto "Ubicación: Descargas/" real en la card. Tras el guardado, el botón "Compartir" (Filled Tonal Button) permite enviar el archivo via `ACTION_SEND` usando una copia temporal en `cacheDir` expuesta via `FileProvider`.

**4. `ActivityResultContracts.OpenDocument` — SAF sin permisos para importar.**

`OpenDocument` lanza el file picker nativo sin requerir NINGÚN permiso. El archivo seleccionado retorna como `Uri` → se lee via `ContentResolver.openInputStream(uri)` → se parsea como JSON. Este patrón tiene precedente en el proyecto: `ImageStorageHelper.kt` usa `context.contentResolver.openInputStream(uri)` para leer imágenes de ejercicios.

**5. Transacción atómica con rollback automático (CA-19.15).**

`SupportSQLiteDatabase.beginTransaction()` abre una transacción SQLite. Si ocurre CUALQUIER excepción durante el DELETE/INSERT de las 16 tablas, `endTransaction()` sin `setTransactionSuccessful()` revierte TODOS los cambios. Los datos originales permanecen intactos. El flujo: (1) `db.beginTransaction()`, (2) DELETE FROM cada tabla en orden FK (children first), (3) INSERT INTO cada tabla en orden FK (parents first), (4) `db.setTransactionSuccessful()`, (5) `db.endTransaction()`.

**6. Post-importación exitosa navega a B1 limpiando todo el back stack (Arq. Técnica §4.5).**

La Arquitectura Técnica §4.5 define J3→B1 como "Reinicio post-importación: tras importación exitosa, se navega a B1 limpiando todo el back stack. Equivale a un reinicio lógico de la app con los datos restaurados". Esto garantiza que todos los ViewModels se re-crean con Flows frescos que leen los nuevos datos. Implementación: `navController.navigate(HOME) { popUpTo(navController.graph.id) { inclusive = true } }`.

**7. `BackupMetadata` incluye schemaVersion = `TensionDatabase.VERSION` (actualmente 7).**

El ADR-10 establece que el JSON incluye "metadatos de versión de app y esquema de BD en el header". En la importación, se valida `backup.schemaVersion == TensionDatabase.VERSION`. Si no coincide, se rechaza la importación con un mensaje claro. La infraestructura de migración de backups entre versiones de schema es un stub para v2+ — CA-19.12 se satisface con la validación de versión y un mensaje informativo cuando la versión difiere.

**8. El archivo se nombra `tension_backup_YYYYMMDD.json` (ADR-10).**

Nombre determinístico basado en la fecha de exportación. Si el usuario exporta más de una vez el mismo día, el segundo archivo sobrescribe al primero en `cacheDir`. El nombre visible al compartir es el mismo.

**9. La advertencia de contenido sin cifrado se muestra ANTES de exportar (CA-19.05).**

La pantalla J2 muestra una `OutlinedCard` con ícono ⚠️ y texto "El archivo de respaldo contiene todos tus datos de entrenamiento y no está cifrado." como primer elemento visible. No hay popup ni diálogo separado — es información persistente en pantalla.

**10. `ExportBackupUiState` incluye fase ERROR como adición defensiva.**

Ni el wireframe §J2 ni la Especificación Visual §J2 definen un estado de error para la exportación. Sin embargo, `ExportBackupUiState` incluye la fase ERROR como protección ante fallos inesperados (disco lleno, error de I/O). En caso de error, se muestra un `Snackbar` con mensaje descriptivo y el botón "Exportar datos" se rehabilita para reintentar.

**11. La confirmación de reemplazo destructivo usa botón estilo Error (CA-19.10).**

J3 paso 3 muestra una `FilledCard` con fondo Error Container, texto "ATENCIÓN: Todos los datos actuales serán reemplazados. Esta operación no es reversible" y botón "Restaurar datos" de color `Error (#BA1A1A)` on `OnError (#FFFFFF)`.

---

### §5 Estructura JSON del backup (ADR-10)

```json
{
  "metadata": {
    "appVersion": "1.0",
    "schemaVersion": 7,
    "exportDate": "2026-02-20T14:30:00",
    "recordCount": 30247
  },
  "data": {
    "profile": [{ "id": 1, "height_m": 1.75, "..." }],
    "weight_record": ["..."],
    "module": ["..."],
    "muscle_zone": ["..."],
    "equipment_type": ["..."],
    "exercise": ["..."],
    "exercise_muscle_zone": ["..."],
    "module_version": ["..."],
    "plan_assignment": ["..."],
    "rotation_state": ["..."],
    "session": ["..."],
    "session_exercise": ["..."],
    "exercise_set": ["..."],
    "exercise_progression": ["..."],
    "alert": ["..."],
    "deload": ["..."]
  }
}
```

### Orden FK para DELETE (children first)

`exercise_set` → `session_exercise` → `session` → `exercise_progression` → `alert` → `plan_assignment` → `exercise_muscle_zone` → `exercise` → `module_version` → `deload` → `equipment_type` → `muscle_zone` → `module` → `weight_record` → `rotation_state` → `profile`

### Orden FK para INSERT (parents first)

`profile` → `rotation_state` → `weight_record` → `module` → `muscle_zone` → `equipment_type` → `deload` → `exercise` → `exercise_muscle_zone` → `module_version` → `plan_assignment` → `session` → `session_exercise` → `exercise_set` → `exercise_progression` → `alert`

---

### §6 Verificación de impacto (código real — paso 1.5)

- `TensionDatabase.kt`: version = 7, exportSchema = false, 16 entities, 15 abstract DAO methods. `openHelper.writableDatabase` accesible para raw SQL. `PrepopulateCallback` se ejecuta en `onCreate` y `onDestructiveMigration` — no interfiere con import (el import no destruye la BD, solo reemplaza datos dentro de la transacción existente).
- `DatabaseModule.kt`: `TensionDatabase` ya es `@Singleton` provisto por Hilt vía `provideTensionDatabase()`. No necesita nuevo provider — se inyecta directamente en `BackupRepositoryImpl`.
- `RepositoryModule.kt` (67 líneas): 6 bindings existentes. Se agrega el 7mo: `BackupRepository → BackupRepositoryImpl`.
- `NavigationRoutes.kt` (38 líneas): 25 rutas definidas. No hay `EXPORT_BACKUP` ni `IMPORT_BACKUP`. Se agregan 2 rutas nuevas.
- `TensionNavHost.kt` (422 líneas): `SettingsScreen` en L155 solo tiene `onNavigateToProfile`. Se agregan 2 lambdas y 2 composables nuevos.
- `SettingsScreen.kt` (50 líneas): Scaffold + TopBar + 1 ListItem "Editar perfil" + Divider. Se agrega la sección "Datos" con Divider + encabezado + 2 ListItems.
- `BottomNavigationBar.kt` (148 líneas): Tab SETTINGS tiene `childRoutes = setOf(PROFILE, WEIGHT_HISTORY)`. Se agregan `EXPORT_BACKUP` e `IMPORT_BACKUP`.
- `ImageStorageHelper.kt` (41 líneas): Usa `context.contentResolver.openInputStream(uri)` — precedente exacto para `BackupFileManager`. Está en `data.local.storage`.
- `AndroidManifest.xml`: No hay `FileProvider` declarado ni permisos. Se agrega `<provider>` para `FileProvider`.
- `libs.versions.toml` / `build.gradle.kts`: No hay `kotlinx-serialization`, `moshi`, ni `gson`. Se confirma uso de `org.json` (Android SDK built-in).
- `Migrations.kt`: Solo `MIGRATION_6_7`. No hay migraciones de backup.
- `Converters.kt`: Convierte `LocalDate ↔ String`. Las fechas en la BD ya son Text (ISO format) — el backup las exporta tal cual.

---

### §7 Riesgos Técnicos Identificados

1. **`CreateDocument` launcher requiere coordinación ViewModel↔`ActivityResultContract`:** El ViewModel genera el JSON pero el launcher opera a nivel Composable. La coordinación se maneja con `MutableStateFlow<String?>` para el JSON generado y un `LaunchedEffect` que observa el estado. Si el usuario cancela el picker, el ViewModel expone `onExportPickerCancelled()` que resetea el estado a IDLE.
2. **JSON de 2 años de datos (~5-15 MB) se mantiene in-memory como `String`:** Para dispositivos Android modernos (4+ GB RAM) esto es trivial. Para el import, el JSON se almacena en el ViewModel (reutilizado entre validación y ejecución), y se libera tras el import exitoso.
3. **`SupportSQLiteDatabase` raw SQL sin verificación de tipos en compilación:** Los queries son strings literales. Un typo solo se detecta en runtime. Mitigación: la lista de tablas es una constante definida una sola vez y verificada contra la lista de entities de `TensionDatabase`. Tests con Room in-memory DB validan el roundtrip.
4. **`FileProvider` duplicado si otra dependencia lo declara:** Actualmente el manifest NO tiene ningún `<provider>` — confirmado.
5. **Post-import: los ViewModels Hilt mantienen stale data en memoria:** La navegación `popUpTo(graph.id) { inclusive = true }` destruye TODOS los composables y ViewModels del NavGraph. Al recrear B1, `HomeViewModel` obtiene Flows frescos de Room.
6. **Barra de progreso determinada vs indeterminate:** El wireframe J2 muestra barra con porcentaje (75%) pero la Especificación Visual §J2 usa `LinearProgressIndicator indeterminate`. Decisión: usar indeterminate — cero complejidad de callbacks de progreso.
7. **Custom exercise images NO se incluyen en el backup:** `exercise.media_resource` almacena rutas absolutas a archivos en `filesDir/exercise_images/`. El JSON exporta la ruta como string pero NO el archivo binario. En restauración cross-device, las rutas serán inválidas y las imágenes mostrarán el placeholder. La pérdida es cosmética — todos los datos de entrenamiento se preservan intactos.

---

### §8 Notas Técnicas

**Nota 1 — `FileProvider` para compartir como acción secundaria.**

Android requiere `FileProvider` para compartir archivos desde `cacheDir` con otras apps vía `Intent.ACTION_SEND`. La configuración requiere: (a) `<provider>` en `AndroidManifest.xml` con `android:authorities="${applicationId}.fileprovider"`, `android:exported="false"`, `android:grantUriPermissions="true"`, y un `<meta-data>` apuntando a `@xml/file_paths`, (b) `file_paths.xml` en `res/xml/` con `<cache-path name="backups" path="." />`. El `Intent` para compartir: `Intent(ACTION_SEND).setType("application/json").putExtra(EXTRA_STREAM, FileProvider.getUriForFile(ctx, authority, file)).addFlags(FLAG_GRANT_READ_URI_PERMISSION)`. Este pattern es estándar en Android y no requiere permisos runtime.

**Nota 2 — `ActivityResultContracts.OpenDocument` para selección de archivo.**

En J3, se usa `rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument())` con MIME type `"application/json"`. Esto lanza el picker nativo del sistema que permite seleccionar archivos desde almacenamiento local, Google Drive, Gmail (adjuntos), etc. El archivo retorna como `Uri` → `ContentResolver.openInputStream(uri)` → `BufferedReader.readText()` → JSON string. Si el usuario cancela el picker, el launcher retorna `null` y no se hace nada.

**Nota 3 — Transacción atómica SQLite garantiza rollback (CA-19.15).**

```kotlin
val db = database.openHelper.writableDatabase
db.beginTransaction()
try {
    // DELETE all tables (children first)
    TABLE_ORDER_DELETE.forEach { table -> db.execSQL("DELETE FROM $table") }
    // INSERT all data (parents first)
    TABLE_ORDER_INSERT.forEach { table ->
        val rows = jsonData.getJSONArray(table)
        for (i in 0 until rows.length()) {
            db.insert(table, CONFLICT_REPLACE, rows.getJSONObject(i).toContentValues())
        }
    }
    db.setTransactionSuccessful()
} finally {
    db.endTransaction()
}
```

Si CUALQUIER excepción ocurre entre `beginTransaction()` y `setTransactionSuccessful()`, el `finally { endTransaction() }` revierte TODAS las operaciones.

**Nota 4 — Validación del backup antes de importar (CA-19.09).**

La validación ocurre ANTES de la confirmación destructiva. Se verifica: (a) el string es JSON válido, (b) existe el campo `metadata` con `appVersion`, `schemaVersion`, `exportDate`, (c) `schemaVersion == TensionDatabase.VERSION` (actualmente 7), (d) existe el campo `data` con al menos las tablas obligatorias. Si alguna verificación falla, se muestra la card de error y no se procede.

**Nota 5 — Post-import navegación con delay de 2 segundos.**

La Especificación Visual §J3 resultado OK dice "Navegación automática a B1 tras 2 segundos". Implementación: `LaunchedEffect(Unit) { delay(2000L); onNavigateToHome() }` en el composable de la fase SUCCESS.

**Nota 6 — Seed data se incluye en el backup.**

Las tablas `module`, `muscle_zone`, `equipment_type`, `exercise`, `exercise_muscle_zone`, `module_version`, `plan_assignment` contienen datos de seed prepopulados. El backup las exporta como cualquier otra tabla. Al importar, se reemplazan completas (DELETE + INSERT). Esto es correcto porque el usuario puede haber creado ejercicios custom (`is_custom = true`) mezclados con los seed, y la integridad referencial se mantiene porque se importan todas las tablas como bloque atómico.

**Nota 7 — Texto "Ubicación: Descargas/" en la card de confirmación J2.**

La Especificación Visual §J2 define explícitamente que la card de confirmación post-export muestra `"Ubicación: Descargas/"`. El export usa `ActivityResultContracts.CreateDocument("application/json")` — el usuario elige la carpeta de destino (por defecto Descargas). El texto de la card se actualiza dinámicamente con la URI de destino devuelta por `CreateDocument` via `BackupFileManager.extractDisplayPath(uri)`.

**Nota 8 — Bottom Navigation en J2 y J3.**

La Arquitectura Técnica §4.5.1 lista J2 y J3 en el grupo "Siempre visible" para Bottom Nav. El `showBottomBar` en `TensionNavHost.kt` usa un blocklist — como `export-backup` e `import-backup` no están bloqueados, Bottom Nav se muestra automáticamente. Para que el tab "Configuración" quede seleccionado, se agregan ambas rutas a `childRoutes` del tab SETTINGS en `BottomNavigationBar.kt`.

**Nota 9 — Sección "Entrenamiento" en J1 (frecuencia semanal) NO implementada por HU-19.**

El wireframe §J1 define 3 secciones: (1) Perfil, (2) Entrenamiento (selector de frecuencia semanal — HU-15), (3) Datos. HU-19 solo agrega la sección "Datos" — el selector de frecuencia queda pendiente como deuda visual de HU-15. El `SettingsScreen.kt` actual (50 líneas) solo contiene el ListItem "Editar perfil"; HU-19 agrega debajo un Divider + encabezado "Datos" + 2 ListItems de backup.

---

### §9 Verificación Cruzada de CAs

| CA | Mecanismo | Componente |
|----|-----------|------------|
| CA-19.01 | Raw SQL `SELECT * FROM table` × 16 tablas → `JSONObject` con metadata + `data` con 16 arrays | `BackupRepositoryImpl.exportToJson()` |
| CA-19.02 | JSON raíz con `metadata: { appVersion, schemaVersion, exportDate }` + `data: { table: [...] }`. `schemaVersion` = `TensionDatabase.VERSION` (7) — ADR-10 | `BackupRepositoryImpl.exportToJson()` |
| CA-19.03 | `CreateDocument("application/json")` primario: usuario elige destino. Botón "Compartir" envía copia via `ACTION_SEND` + `FileProvider` | `BackupFileManager.writeToUri()` + `ExportBackupViewModel.share()` |
| CA-19.04 | Raw SQL sequencial sobre 16 tablas + serialización JSON. ~30K registros → ~5-15 MB JSON. `Dispatchers.IO`. Total < 5s | `BackupRepositoryImpl.exportToJson()` en coroutine IO |
| CA-19.05 | `OutlinedCard` con ícono ⚠️ y texto de advertencia visible ANTES del botón "Exportar datos" | `ExportBackupScreen` — card pre-export (Especificación Visual §J2) |
| CA-19.06 | Cero permisos runtime. SAF + `FileProvider` no los requieren en API 26+ | Toda la cadena — sin `WRITE_EXTERNAL_STORAGE` ni `READ_EXTERNAL_STORAGE` |
| CA-19.07 | `LinearProgressIndicator` indeterminate + `FilledCard` Tertiary Container con ✅, nombre + ubicación real | `ExportBackupScreen` fases EXPORTING y SUCCESS |
| CA-19.08 | `rememberLauncherForActivityResult(OpenDocument("application/json"))` — file picker nativo | `ImportBackupScreen` — `OpenDocument` launcher |
| CA-19.09 | Parseo JSON → verificar `metadata.schemaVersion` == `TensionDatabase.VERSION` + estructura `data`. Si inválido: card Error Container con ❌ | `BackupRepositoryImpl.validateBackup()` → `ImportBackupScreen` fase INVALID |
| CA-19.10 | `FilledCard` Error Container + texto "ATENCIÓN: Todos los datos actuales serán reemplazados." + botón destructivo Error (#BA1A1A) + link "Cancelar" | `ImportBackupScreen` fase VALIDATED, paso 3 |
| CA-19.11 | Transacción atómica: DELETE FROM × 16 tablas (children first) + INSERT INTO × 16 tablas (parents first) | `BackupRepositoryImpl.importFromJson()` |
| CA-19.12 | Validación de `schemaVersion`. Si versión ≠ actual (7), rechaza con mensaje informativo. Stub de migración para v2+ | `BackupRepositoryImpl.validateBackup()` |
| CA-19.13 | Lectura URI + parseo JSON + transacción DELETE/INSERT. `Dispatchers.IO`. Total < 8s para ~30K registros | `BackupRepositoryImpl.importFromJson()` |
| CA-19.14 | `LinearProgressIndicator` indeterminate + ícono ✅ 48dp + "Datos restaurados exitosamente." → auto-nav a B1 tras 2s | `ImportBackupScreen` fases IMPORTING y SUCCESS |
| CA-19.15 | `db.beginTransaction()` / `db.setTransactionSuccessful()` / `finally { db.endTransaction() }`. Card error con rollback message + botón "Volver a Configuración" | `BackupRepositoryImpl.importFromJson()` + `ImportBackupScreen` fase ERROR |

---

### §10 Referencias

- Wireframes — §J2 Exportar Respaldo (8 elementos), §J3 Importar Respaldo (11 elementos)
- Mapa de Navegación — §J1 transiciones J1↔J2/J3, §J2 retorno J1, §J3 éxito→B1 + cancelar→J1
- Especificación Visual — §J2 component specs pre/durante/post, §J3 5-step specs con card destructiva
- Modelo de Datos — 16 tablas §3.1-§3.16, orden FK, columnas por tabla
- Arquitectura Técnica — §3.2 paquete ui.settings, §4.3 rutas J2/J3, §4.4 settings-graph, §4.5 showBottomBar "Siempre visible", §4.5.1 J3→B1 reinicio post-importación, §5.2 naming BackupRepository
- ADR — ADR-10 JSON como formato de backup, ADR-15 sin cifrado, ADR-03 Room, ADR-05 MVVM Domain
- Requerimientos — RNF15, RNF16, RNF17, RNF18, RNF26, RNF27

**Validado por:** esteban.colorado | **Fecha:** 2026-02-20 | **Enfoque:** Exploratorio
