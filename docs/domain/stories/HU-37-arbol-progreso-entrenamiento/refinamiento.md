## Refinamiento Técnico (Developer)
**Autor**: Esteban Colorado González | **Fecha**: 2026-09-02

---

### Contexto

El sistema ya tiene los dos datos que el árbol necesita, y no los tiene guardados en ningún sitio: se calculan sobre `session` cada vez que alguien pregunta.

1. **Cuántas sesiones se han registrado** — `SessionDao` sabe contarlas por rutina (`getSessionIdsByRoutineInRange`), por semana (`countSessionsInWeek`) y por descarga (`countDeloadSessions`), pero **no globalmente**.
2. **Cuándo fue la última** — `getLastSessionDateByRoutine` existe y filtra **por rutina**, porque `ROUTINE_INACTIVITY` mide inactividad por rutina. El árbol mide inactividad **global**: es una consulta nueva, no un parámetro más.

Ambas consultas comparten el mismo predicado que ya usa todo el sistema para decidir qué cuenta como entrenamiento: `status IN ('COMPLETED', 'INCOMPLETE')`. Esa cláusula aparece nueve veces en `SessionDao` y es exactamente lo que pide CA-37.07 — la historia no define un criterio propio, se ancla al que HU-36 dejó corregido.

**Feature análoga leída completa: HU-36 (días de la semana reasignables)** — es el precedente estructural exacto: entidad nueva de fila única, regla pura en `domain/rules`, esquema sin migración por la excepción de beta, y recálculo enganchado al ticker de cambio de día. **HU-22 (`SessionPreviewScreen`)** aporta el segundo patrón: pantalla dedicada colgada de Inicio, sin pestaña en la barra inferior.

| Capa | Artefacto | Responsabilidad | Dependencias | Artefactos asociados |
|---|---|---|---|---|
| `DB-01` | `DaySkipEntity` (`data/local/entity`) | Tabla de fila única `id = 1` con una fecha ISO | Room | `DaySkipDao`, `TensionDatabase`, `DatabaseModule` |
| `DB-01` | `RotationStateEntity` / `RotationStateDao` | Fila única con estado acumulado, `Flow` + `Once` | Room | `ProfileRepositoryImpl:51` la inicializa al crear el perfil |
| `DOM-01` | `DayResolutionRule` (`domain/rules`) | Regla pura sobre fechas ISO, sin dependencias de Android | Ninguna | `DayResolutionRuleTest` |
| `DOM-01` | `AdherenceRule` | Regla pura aritmética con recorte de rango | Ninguna | `AdherenceRuleTest` (7 casos, incluidos los límites) |
| `DOM-01` | `ResolveStaleSessionUseCase` | Barrido de la sesión de ayer | `SessionRepository` | `ResolveStaleSessionUseCaseTest` con `mockk` |
| `DOM-01` | `CurrentDateProvider` (`domain/util`) | `today()` y `dateFlow()` — primera emisión inmediata, reemisión a medianoche | Ninguna (`@Singleton`) | — |
| `DAT-01` | `SessionRepositoryImpl.closeSession` (`:565`) | Cierra, evalúa progresión, avanza rotación, genera alertas | 10 DAOs | sin test (depende de Room) |
| `DAT-01` | `BackupRepositoryImpl` | Volcado tabla a tabla desde `TABLE_ORDER_INSERT`, `SCHEMA_VERSION = 11`, camino legado v8 | `TensionDatabase` crudo | `ExportBackupUseCaseTest`, `ValidateBackupUseCaseTest`, `ImportBackupUseCaseTest` |
| `UI-01` | `MainViewModel` (`ui/navigation`) | Colecciona `dateFlow()` y ejecuta el barrido `B1-T7` | `CurrentDateProvider`, `ResolveStaleSessionUseCase` | — |
| `UI-01` | `SessionPreviewScreen` + VM + UiState (`ui/preview`) | Pantalla dedicada, `CenterAlignedTopAppBar` con back, ruta sin pestaña | `hiltViewModel`, `collectAsStateWithLifecycle` | — |
| `UI-02` | `HomeScreen` / `HomeUiState` | `LazyColumn` de tarjetas condicionales, propiedades derivadas en el UiState | `HomeViewModel` | `HomeViewModelTest` |
| `UI-02` | `LocalTensionSemanticColors` (`ui/theme`) | Colores de dominio con par claro/oscuro | `TensionTheme` | — |

#### El hallazgo que define el alcance

**El esquema real es v18, no v14.** `architecture_blueprint.md` §2.1 quedó desactualizado. Y de las 18 versiones, **las migraciones llegan hasta 15→16**: v17 (HU-36) y v18 no tienen `Migration`, por la excepción ADR-019 (beta sin migración). La historia hereda esa excepción: **v19 sin `MIGRATION_18_19`**, y `addMigrations(...)` no se toca.

#### La consecuencia que ninguna CA cubre

**El respaldo actual solo acepta dos versiones: la vigente y la legada v8.** Cualquier otra es rechazada por `validateBackup`. CA-37.09 exige que "un respaldo de la versión anterior se restaura sin error", y la anterior es **v11**, no v8. Sin una tercera rama de compatibilidad, subir a v12 dejaría inservible todo respaldo exportado por la versión actual de la app. Se resuelve en D8.

#### Lo que NO se toca

CA-37.08 es una restricción de no-cambio, y su verificación es que estos archivos **no aparezcan en el diff**:

- `domain/rules/` — `DoubleThresholdRule`, `PlateauThresholdRule`, `PlateauCausalAnalysisRule`, `RoutineFatigueRule`, `DeloadNeedRule`, `DeloadLoadRule`, `ProgressionClassificationRule`, `ProgressionRateRule`, `LoadIncrementResolver`, `PrefilledLoadRule`, `AlertThresholdRule`, `AlertNarrativeRule`, `AdherenceRule`, `TonnageRule`, `DailyRoutineRule`, `DayResolutionRule`, `NextTrainingDayRule`.
- `domain/model/RotationResolver.kt` — la rotación cíclica.
- `domain/usecase/alerts/`, `domain/usecase/metrics/`, `domain/usecase/deload/`.
- `docs/domain/definition/system_definition_document.md` — la exclusión §2.1 se mantiene intacta.

De `SessionRepositoryImpl.closeSession` **no se modifica ni una línea**: el recálculo se engancha un nivel por encima (D6).

---

### Decisiones técnicas

#### D1 — `tree_state`: fila única con lo persistido y nada derivable

Cuatro columnas, tabla `tree_state`, `id = 1`, mismo patrón que `rotation_state` y `day_skip`:

| Columna | Tipo | Razón |
|---|---|---|
| `health_score` | `Int` | CA-37.03 lo exige persistido |
| `growth_stage` | `String` | CA-37.04 lo exige persistido, junto al puntaje |
| `last_session_date` | `String?` | Fuente de los días transcurridos que pide CA-37.02. Nulo = sin historial |
| `calculated_at` | `String` | Fecha ISO del último recálculo. Hace auditable el orden de CA-37.06 |

**No se persiste el conteo de sesiones**: la etapa ya lo resume y guardarlo crearía un segundo lugar donde el mismo hecho puede quedar desincronizado. **No se persisten los días transcurridos**: dependen de la fecha de hoy, y un entero guardado ayer es rancio hoy — se derivan de `last_session_date` en cada lectura, que es justo lo que CA-37.06 pide evitar mostrar.

Sin FK. `tree_state` no referencia `session`: si lo hiciera, borrar una sesión arrastraría el árbol, y la dependencia declarada es de lectura, no de integridad.

#### D2 — Dos reglas puras, no una

`TreeHealthRule` y `TreeGrowthStageRule` viven separadas en `domain/rules` porque las dos dimensiones son ortogonales (regla de negocio 1) y esa ortogonalidad tiene que ser visible en el código, no solo en el documento. Una sola regla que devolviera un par obligaría a leer las dos ramas juntas para entender cualquiera de las dos. RNF29 y RNF30 se cumplen igual; la diferencia es que los límites de días y los cortes de sesiones se prueban por separado.

`TreeHealthRule.calculate(daysSinceLastSession: Int?): Int`

```
null  -> 100      // sin historial: no se castiga a quien no ha tenido oportunidad
d<=2  -> 100
d>=14 -> 0
else  -> round(100.0 * (14 - d) / 12).coerceIn(0, 100)
```

La recta se define entre `(2, 100)` y `(14, 0)`, que es la única que reproduce la tabla de verificación de CA-37.03: `d=3 → 91.67 → 92` · `d=5 → 75` · `d=8 → 50` · `d=11 → 25` · `d=13 → 8` · `d=14 → 0`. El redondeo es a entero más cercano y el `coerceIn` es defensivo: con `d` negativo —reloj movido hacia atrás— el resultado sigue siendo 100 y no un valor fuera de rango.

`TreeGrowthStageRule.resolve(sessionCount: Int): TreeGrowthStage` con los cortes 0 / 1–9 / 10–29 / 30+.

#### D3 — La etapa no retrocede porque el conteo no baja, no porque se guarde un máximo

CA-37.04 dice que la etapa nunca retrocede "cualquiera que sea la salud". La invariante es **frente a la salud**, no frente al historial: al ser función monótona del total de sesiones cerradas, la etapa solo puede subir mientras el ejecutante entrene.

Se descarta deliberadamente aplicar `max(persistida, calculada)`. Sería contradictorio con CA-37.09, que exige que tras restaurar un respaldo el árbol quede "en un estado válido **derivado del historial restaurado**": restaurar un respaldo antiguo con menos sesiones debe dar una etapa menor, y un máximo pegajoso lo impediría dejando un árbol maduro sobre un historial de brote.

#### D4 — `TreeRepository` propio, no un método más en `SessionRepository`

`SessionRepository` ya expone 33 métodos y es el contrato del motor de decisión. Colgar el árbol de él sería exactamente la dependencia que CA-37.08 prohíbe **hacia el otro lado**: no rompe el aislamiento, pero lo vuelve indistinguible. Un `TreeRepository` con dos métodos hace la frontera legible en el propio contrato.

```kotlin
interface TreeRepository {
    fun getTreeState(): Flow<TreeState>
    suspend fun recalculate()
}
```

`TreeRepositoryImpl` inyecta `TreeStateDao`, `SessionDao` y `CurrentDateProvider`. Leer `SessionDao` directamente desde otro repositorio es el patrón vigente del proyecto (`MetricsRepositoryImpl` y `AlertRepositoryImpl` ya lo hacen) y mantiene la dependencia unidireccional: el árbol lee de `session`, y nada lee de `tree_state`.

#### D5 — Los días se calculan al mapear, no al persistir

`getTreeState()` mapea `TreeStateEntity → TreeState` resolviendo `daysSinceLastSession` con `ChronoUnit.DAYS.between(lastSessionDate, currentDateProvider.today())`. Es lo que hace que la pantalla no pueda mostrar un conteo rancio aunque el recálculo fallara, y lo que permite que `last_session_date` sea la única fecha persistida.

Con `last_session_date` nulo el modelo expone `daysSinceLastSession = null` y `hasHistory = false`, que es la señal única que consumen la tarjeta y la pantalla para el caso Semilla de CA-37.10 — ninguna de las dos vuelve a comparar contra cero.

#### D6 — Los tres momentos se enganchan donde el orden ya está garantizado

CA-37.06 exige que en el cambio de día el recálculo corra **después** del barrido. La forma de garantizarlo no es coordinar dos observadores, sino usar el que ya existe:

| Momento | Enganche | Por qué ahí |
|---|---|---|
| 1. Cierre de sesión | `CloseSessionUseCase`, tras `sessionRepository.closeSession(...)` | El use case es el único punto por el que pasa un cierre manual, y es dominio: no toca `SessionRepositoryImpl` |
| 2. Cambio de día | `MainViewModel`, en el mismo `collect` de `dateFlow()`, **en la línea siguiente** a `resolveStaleSessionUseCase()` | El orden queda expresado como secuencia dentro de una corrutina, no como coincidencia entre dos observadores. Cubre arranque de app y cruce de medianoche |
| 3. Apertura de la pantalla | `TreeViewModel.init` | Garantiza que lo mostrado nunca sea rancio |

El cierre automático de la sesión de ayer llama a `sessionRepository.closeSession` directamente, **sin pasar por `CloseSessionUseCase`** — no lo cubre el momento 1. Lo cubre el momento 2, que corre inmediatamente después en la misma corrutina, y ese es precisamente el caso que la nota de CA-37.06 protege: recalcular antes del barrido marchitaría el árbol de alguien que sí entrenó.

El recálculo es *best-effort* en los momentos 1 y 2, con el mismo `try/catch` silencioso que ya envuelve al barrido: un árbol que no se actualiza es un defecto visual; una excepción que aborta el cierre de sesión es pérdida de datos.

#### D7 — El recálculo no bloquea la interfaz (RNF01)

Todo ocurre en `viewModelScope` sobre funciones `suspend`, y las consultas son dos agregados sobre `session` con el mismo predicado ya indexado que usa el resto del sistema. La tarjeta de Inicio se alimenta de un `Flow` sobre `tree_state` — una fila —, no de un cálculo en composición. Ninguna de las dos rutas atraviesa el hilo principal.

#### D8 — El respaldo sube a v12 y acepta el v11 como formato previo

`tree_state` es tabla nueva, así que el respaldo cambia de forma (D11 de HU-36 estableció el criterio: gana tablas, no columnas) y `SCHEMA_VERSION` pasa de 11 a 12.

Se añade una tercera rama de compatibilidad, `PREVIOUS_SCHEMA_VERSION = 11`, junto a la legada v8:

- **Validación**: un respaldo v11 es válido; su lista de tablas requeridas es `TABLE_ORDER_INSERT` sin `tree_state`.
- **Importación**: la lectura de cada tabla pasa de `getJSONArray` a `optJSONArray` con arreglo vacío por defecto, de modo que la ausencia de `tree_state` no aborte la restauración.
- **Reconstrucción**: `ImportBackupUseCase` invoca el recálculo tras `importFromJson`. Un respaldo v11 restaura un árbol coherente con el historial restaurado, y uno v12 recalcula igualmente sobre esas mismas sesiones. No hace falta distinguir: **el árbol siempre es derivable**, y por eso restaurar sin él nunca deja un estado inválido.

El camino legado v8 añade `result.put("tree_state", JSONArray())` en `transformV8ToV9`, por el mismo motivo por el que HU-36 tuvo que añadir `week_day`: `importFromJson` borra e reinserta todas las tablas de `TABLE_ORDER_INSERT`, y omitir una la dejaría vacía en silencio.

#### D9 — Cuatro íconos vectoriales, teñidos en tiempo de ejecución

Cuatro `drawable` nuevos (`ic_tree_seed`, `ic_tree_sprout`, `ic_tree_young`, `ic_tree_mature`) con `android:fillColor="#FFFFFFFF"`, pintados con `Icon(painter = painterResource(...), tint = ...)`. El tinte se resuelve en Compose desde `LocalTensionSemanticColors`, no en el XML: es lo que permite una sola matriz de 4 recursos en lugar de 20 (RNF31, regla de negocio 12), y lo que hace que HU-38 pueda sustituir el bloque sin tocar los assets.

La forma es la etapa, el color es la salud (CA-37.05). El caso Semilla usa gris neutro y **no** el verde de salud 100: comunicar "está sanísimo" sobre un árbol que aún no existe sería ruido, y el wireframe ya lo resuelve así.

#### D10 — Cinco colores semánticos nuevos, con par claro/oscuro

| Rol | Banda | Claro | Oscuro |
|---|---|---|---|
| `treeSeed` | Semilla (sin historial) | `#857370` | `#A08C88` |
| `treeHealthy` | Alta ≥ 67 | `#2E7D32` | `#81C784` |
| `treeDry` | Media 34–66 | `#8D6E00` | `#FFD54F` |
| `treeWithering` | Baja 1–33 | `#8D5524` | `#D2A679` |
| `treeWithered` | Marchito 0 | `#5D4037` | `#A1887F` |

Los tres primeros reutilizan los valores ya validados de `ProgressionPositive`, `Maintenance` y `MetricInsufficient`, de modo que el árbol habla el mismo idioma cromático que el resto de la app. Los dos marrones son nuevos y son el caso límite que señala el wireframe: en modo oscuro se invierten a marrones **claros**, porque un marrón oscuro sobre fondo `#1C1B1B` no se lee (RNF23).

La banda se resuelve en una función de presentación, `treeHealthColor(stage, healthScore)`, no en el dominio: es tinte, no regla de negocio, y `domain` no importa `androidx.compose` (§3.2 de los estándares).

#### D11 — La tarjeta de Inicio reutiliza el `Card` existente y se compone siempre

La tarjeta va **debajo** de todas las tarjetas de sesión y **antes** del divisor de `ProgressSection`, dentro del mismo `LazyColumn`. Reutiliza `Card` con `RoundedCornerShape(12.dp)` y `MaterialTheme.colorScheme.surfaceContainer`, igual que `RestDayCard` y `ResolvedDayCard` — no lleva el `primaryContainer` de la tarjeta de sesión, porque su posición ya le da la jerarquía subordinada y competir en color la contradiría.

Se compone **incondicionalmente**: es la única tarjeta de Inicio sin condición de visibilidad, porque el árbol existe siempre, incluso como semilla. El `Row` completo lleva `Modifier.heightIn(min = 48.dp).clickable(...)`, cumpliendo RNF06 sobre toda el área y no solo sobre el ícono.

**La tarjeta es nativa de forma permanente** (CA-37.01, RNF01). No es provisional a la espera de HU-38.

#### D12 — El layout de la pantalla aísla el área del árbol

`TreeScreen` compone, en `Column` centrada: `TreeVisual` (bloque de 180×180 dp) → etapa → puntaje → días → mensaje. `TreeVisual` es un composable privado con una única responsabilidad —pintar el ícono teñido dentro de un `Box` de tamaño fijo— y ningún otro elemento del layout depende de su contenido interno.

Esa es la costura que HU-38 necesita: sustituir `TreeVisual` por un `WebView` no reorganiza nada, porque el resto del `Column` solo conoce su altura. Es lo que hace verificable el último punto de la DoD.

---

### Tareas de Implementación

#### Fase 1 — Esquema y persistencia (`DB-01`)

- [ ] **T1: Crear `TreeStateEntity`** — `data/local/entity/TreeStateEntity.kt` (Base: `DaySkipEntity.kt`)

  Tabla `tree_state`, fila única `id: Int = 1` `@PrimaryKey`. `health_score` `Int`, `growth_stage` `String`, `last_session_date` `String?`, `calculated_at` `String`. Sin FK. KDoc en español explicando por qué no se persisten ni el conteo de sesiones ni los días transcurridos. (D1)

- [ ] **T2: Crear `TreeStateDao`** — `data/local/dao/TreeStateDao.kt` (Base: `RotationStateDao.kt`)

  `upsert(state)` con `OnConflictStrategy.REPLACE`; `getTreeState(): Flow<TreeStateEntity?>` (`WHERE id = 1`); `getTreeStateOnce(): TreeStateEntity?`.

- [ ] **T3: Registrar entidad y DAO, y subir a v19** — `data/local/database/TensionDatabase.kt`

  `TreeStateEntity::class` en `entities`, `treeStateDao()` abstracto, `version = 19`. **Sin `MIGRATION_18_19`** — se hereda la excepción ADR-019 ya aplicada a v17 y v18.

- [ ] **T4: Proveer el DAO nuevo** — `di/DatabaseModule.kt`

  Un `@Provides fun provideTreeStateDao(database: TensionDatabase)` siguiendo el patrón del archivo. `addMigrations(...)` **no se toca**.

#### Fase 2 — Dominio (`DOM-01`)

- [ ] **T5: Crear el dominio cerrado `TreeGrowthStage`** — `domain/model/TreeGrowthStage.kt` (Base: `WeightUnit.kt`)

  `enum class TreeGrowthStage(val code: String)` con `SEED`, `SPROUT`, `YOUNG`, `MATURE` y `fromCode(String): TreeGrowthStage` con retorno a `SEED` ante un código desconocido —un respaldo corrupto no debe reventar la pantalla—. (CA-37.04)

- [ ] **T6: Crear `TreeState`** — `domain/model/TreeState.kt` (Base: `TodaySession.kt`)

  `stage: TreeGrowthStage`, `healthScore: Int`, `daysSinceLastSession: Int?`. Propiedad derivada `hasHistory: Boolean get() = daysSinceLastSession != null`, para que ni la tarjeta ni la pantalla rehagan la lógica del caso Semilla. (D5)

- [ ] **T7: Crear `TreeHealthRule`** — `domain/rules/TreeHealthRule.kt` (Base: `AdherenceRule.kt`)

  `object` con `calculate(daysSinceLastSession: Int?): Int`. Constantes `FULL_HEALTH_DAYS = 2` y `WITHERED_DAYS = 14` en el propio `object`, con KDoc que explique que el corte de 14 coincide con el umbral de crisis de `ROUTINE_INACTIVITY` y no es arbitrario. Sin dependencias de Android (RNF29). (D2)

- [ ] **T8: Crear `TreeGrowthStageRule`** — `domain/rules/TreeGrowthStageRule.kt` (Base: `AdherenceRule.kt`)

  `object` con `resolve(sessionCount: Int): TreeGrowthStage`. Cortes en constantes: `SPROUT_MIN = 1`, `YOUNG_MIN = 10`, `MATURE_MIN = 30`. Expresión `when` (§3.1 de los estándares). KDoc anotando que la etapa no retrocede porque el conteo no baja, no por un máximo pegajoso. (D2, D3)

- [ ] **T9: Crear `TreeRepository`** — `domain/repository/TreeRepository.kt` (Base: `WeekDayRepository.kt`)

  `fun getTreeState(): Flow<TreeState>` y `suspend fun recalculate()`. KDoc declarando la dependencia unidireccional de CA-37.08: el árbol lee del historial y nada del sistema lee del árbol. (D4)

- [ ] **T10: Crear `GetTreeStateUseCase`** — `domain/usecase/tree/GetTreeStateUseCase.kt` (Base: `GetActiveSessionUseCase.kt`)

  `operator fun invoke(): Flow<TreeState>` delegando en el repositorio.

- [ ] **T11: Crear `RecalculateTreeStateUseCase`** — `domain/usecase/tree/RecalculateTreeStateUseCase.kt` (Base: `ResolveStaleSessionUseCase.kt`)

  `suspend operator fun invoke()` delegando en `treeRepository.recalculate()`. KDoc con los tres momentos de CA-37.06 y la razón del orden respecto al barrido.

#### Fase 3 — Datos (`DAT-01`)

- [ ] **T12: Añadir las dos consultas globales** — `data/local/dao/SessionDao.kt`

  `countClosedSessions(): Int` → `SELECT COUNT(*) FROM session WHERE status IN ('COMPLETED', 'INCOMPLETE')`.
  `getLastClosedSessionDate(): String?` → `SELECT MAX(date) FROM session WHERE status IN ('COMPLETED', 'INCOMPLETE')`.
  Ambas `suspend`. Son la contraparte global de `getLastSessionDateByRoutine`, que filtra por rutina para `ROUTINE_INACTIVITY`; el KDoc debe decir que son consultas distintas a propósito y que no deben acoplarse (CA-37.07, HU-18).

- [ ] **T13: Crear `TreeRepositoryImpl`** — `data/repository/TreeRepositoryImpl.kt` (Base: `WeekDayRepositoryImpl.kt`)

  `@Singleton`, inyecta `TreeStateDao`, `SessionDao` y `CurrentDateProvider`.

  `recalculate()`: lee conteo y última fecha, resuelve `TreeGrowthStageRule` y `TreeHealthRule` sobre los días entre la última fecha y hoy, y hace `upsert` con `calculated_at = today`.

  `getTreeState()`: `treeStateDao.getTreeState().map { ... }`. Con fila nula devuelve el estado inicial (`SEED`, 100, `null`) en lugar de fallar — es el estado del ejecutante recién instalado, antes del primer recálculo. Con fila presente, los días se derivan con `ChronoUnit.DAYS.between` contra `currentDateProvider.today()`. (D5)

- [ ] **T14: Enlazar el repositorio** — `di/RepositoryModule.kt`

  `@Binds @Singleton abstract fun bindTreeRepository(impl: TreeRepositoryImpl): TreeRepository`.

#### Fase 4 — Respaldo y restauración (CA-37.09)

- [ ] **T15: Incorporar `tree_state` al respaldo y subir a v12** — `data/repository/BackupRepositoryImpl.kt` (D8)

  `SCHEMA_VERSION = 12` y `PREVIOUS_SCHEMA_VERSION = 11` junto a `LEGACY_SCHEMA_VERSION = 8`. `"tree_state"` en `TABLE_ORDER_INSERT`, inmediatamente después de `"rotation_state"` (sin FK, el orden solo importa para las que la tienen). `transformV8ToV9` añade `result.put("tree_state", JSONArray())`.

- [ ] **T16: Aceptar el formato previo en la validación** — `data/repository/BackupRepositoryImpl.validateBackup`

  La comprobación de versión admite las tres. `requiredTables` para v11 es `TABLE_ORDER_INSERT` menos `tree_state`.

- [ ] **T17: Tolerar la tabla ausente en la importación** — `data/repository/BackupRepositoryImpl.importFromJson`

  `dataJson.getJSONArray(table)` → `dataJson.optJSONArray(table) ?: JSONArray()`. Un respaldo v11 no trae `tree_state` y no debe abortar la restauración.

- [ ] **T18: Reconstruir el árbol tras restaurar** — `domain/usecase/backup/ImportBackupUseCase.kt`

  Inyectar `RecalculateTreeStateUseCase` e invocarlo después de `backupRepository.importFromJson(json)`. Es lo que hace que un respaldo v11 quede en un estado válido derivado del historial restaurado, y que uno v12 quede coherente aunque su árbol viniera de otra fecha. (D8)

#### Fase 5 — Momentos de recálculo (CA-37.06)

- [ ] **T19: Recalcular al cerrar sesión** — `domain/usecase/session/CloseSessionUseCase.kt` (D6, momento 1)

  Inyectar `RecalculateTreeStateUseCase` e invocarlo tras `sessionRepository.closeSession(sessionId)`, envuelto en `try/catch` silencioso. **`SessionRepositoryImpl.closeSession` no se modifica.**

- [ ] **T20: Recalcular en el cambio de día, después del barrido** — `ui/navigation/MainViewModel.kt` (D6, momento 2)

  Dentro del mismo `collect` de `dateFlow()`, en la línea siguiente a `resolveStaleSessionUseCase()` y dentro del mismo `try`. Comentario en español explicando que el orden es la garantía de CA-37.06 y que invertirlo marchitaría el árbol de alguien que sí entrenó.

- [ ] **T21: Recalcular al abrir la pantalla** — `ui/tree/TreeViewModel.kt` (D6, momento 3 — se implementa junto a T25)

#### Fase 6 — Interfaz (`UI-01`, `UI-02`)

- [ ] **T22: Crear los cuatro íconos vectoriales** — `res/drawable/ic_tree_seed.xml`, `ic_tree_sprout.xml`, `ic_tree_young.xml`, `ic_tree_mature.xml` (D9)

  `<vector>` de 24×24 dp con `viewport` 24×24 y `android:fillColor="#FFFFFFFF"`, para que el tinte de Compose lo sustituya por completo. Cuatro siluetas distinguibles por forma a 48 dp: semilla, brote de dos hojas, arbolito con copa pequeña, árbol de copa ancha.

- [ ] **T23: Añadir los colores semánticos del árbol** — `ui/theme/Color.kt` y `ui/theme/Theme.kt` (D10)

  Diez constantes (`TreeSeedLight/Dark`, `TreeHealthyLight/Dark`, `TreeDryLight/Dark`, `TreeWitheringLight/Dark`, `TreeWitheredLight/Dark`) bajo un comentario de sección `// Semantic Domain Colors — Training Tree (B1, N1)`. Cinco campos nuevos en `TensionSemanticColors` y su asignación en `LightSemanticColors` y `DarkSemanticColors`.

- [ ] **T24: Crear el componente del árbol** — `ui/components/TreeIcon.kt` (D9, D10)

  `@Composable fun TreeIcon(stage: TreeGrowthStage, healthScore: Int, hasHistory: Boolean, size: Dp, modifier: Modifier)`. Dos funciones privadas: `treeIconRes(stage)` → `@DrawableRes Int`, y `treeHealthColor(stage, healthScore)` → `Color` leído de `LocalTensionSemanticColors`, con las bandas 0 / 1–33 / 34–66 / ≥67 y el gris de Semilla por delante de todas. `contentDescription` desde `strings.xml`.

- [ ] **T25: Crear la pantalla del árbol** — `ui/tree/TreeScreen.kt`, `ui/tree/TreeUiState.kt`, `ui/tree/TreeViewModel.kt` (Base: `ui/preview/SessionPreview*`) (CA-37.02, CA-37.10, CA-37.11, D12)

  `TreeUiState(isLoading, treeState: TreeState?)` con derivadas para el texto de días y el mensaje contextual.

  `TreeViewModel`: `init` invoca `RecalculateTreeStateUseCase` (T21) y luego colecciona `GetTreeStateUseCase()`.

  `TreeScreen(onNavigateBack)`: `Scaffold` con `CenterAlignedTopAppBar`, título `Tu árbol` y back nativo — sin ninguna otra acción de navegación. `Column` centrada con `TreeVisual` (`Box` de 180×180 dp que solo contiene el `TreeIcon`), nombre de la etapa, puntaje sobre la etiqueta `Salud`, línea de días y mensaje contextual. **Sin línea de días cuando `hasHistory` es falso** (CA-37.10).

- [ ] **T26: Registrar la ruta** — `ui/navigation/NavigationRoutes.kt` y `ui/navigation/TensionNavHost.kt`

  `const val TREE = "tree"` y su `composable(NavigationRoutes.TREE) { TreeScreen(onNavigateBack = { navController.popBackStack() }) }`. **No se toca `BottomNavigationBar`** ni la condición `showBottomBar`: la ruta no tiene pestaña propia (CA-37.02) y la barra sigue visible, como en `DELOAD_MANAGEMENT`.

- [ ] **T27: Añadir la tarjeta de acceso en Inicio** — `ui/home/HomeScreen.kt`, `ui/home/HomeUiState.kt`, `ui/home/HomeViewModel.kt` (CA-37.01, D11)

  `HomeUiState` gana `treeState: TreeState? = null`. `HomeViewModel` colecciona `GetTreeStateUseCase()` en un `viewModelScope.launch` propio —el `combine` existente ya está en su quinta arista— y **el `copy` del `collect` principal debe preservar `treeState`**, igual que hace con `deloadState`: omitirlo lo borraría en cada recomposición del estado del día.

  `TreeAccessCard` privada en `HomeScreen`, compuesta **siempre**, como último `item` antes del `Spacer(24.dp)` y el `HorizontalDivider` de `ProgressSection`. `Row` con `TreeIcon` de 48 dp, `Column` con `Tu árbol` y la línea dinámica de estado, y chevron. `Modifier.heightIn(min = 48.dp).clickable(onClick = onNavigateToTree)` sobre el `Row` completo (RNF06). `HomeScreen` recibe el parámetro nuevo `onNavigateToTree: () -> Unit`, cableado en `TensionNavHost`.

- [ ] **T28: Añadir las cadenas** — `res/values/strings.xml` (CA-37.01, CA-37.02)

  Bajo un comentario `<!-- Training tree N1 (HU-37) -->`: título `tree_title` (`Tu árbol`), etiqueta `tree_health_label` (`Salud`), los cuatro nombres de etapa (`Semilla`, `Brote`, `Joven`, `Maduro`), las cinco líneas de la tarjeta, las cinco de la pantalla y las variantes de días (`hoy`, `ayer`, `hace %1$d días`), exactamente como las fija la historia. Registro sobrio, segunda persona, sin signos de admiración ni emojis. `contentDescription` del ícono para accesibilidad.

#### Fase 7 — Tests unitarios (JVM, sin emulador)

- [ ] **T29: `TreeHealthRuleTest`** — `test/.../domain/rules/TreeHealthRuleTest.kt` (Base: `AdherenceRuleTest.kt`) (RNF30)

  Límites obligatorios de la DoD: `d = 0, 1, 2` → 100 · `d = 3` → 92 · `d = 5` → 75 · `d = 8` → 50 · `d = 11` → 25 · `d = 13` → 8 · `d = 14` → 0 · `d = 30` → 0. Más `null` → 100 (sin historial) y `d = -1` → 100 (reloj movido hacia atrás). Todos los resultados dentro de 0–100.

- [ ] **T30: `TreeGrowthStageRuleTest`** — `test/.../domain/rules/TreeGrowthStageRuleTest.kt` (RNF30)

  Cortes obligatorios de la DoD: 0 → `SEED` · 1 y 9 → `SPROUT` · 10 y 29 → `YOUNG` · 30 y 100 → `MATURE`.

- [ ] **T31: `RecalculateTreeStateUseCaseTest`** — `test/.../domain/usecase/tree/` (Base: `ResolveStaleSessionUseCaseTest.kt`)

  Delegación al repositorio con `mockk` y `coVerify`.

- [ ] **T32: `GetTreeStateUseCaseTest`** — `test/.../domain/usecase/tree/` (Base: `GetActiveSessionUseCaseTest.kt`)

  El flujo emitido es el del repositorio, sin transformación.

- [ ] **T33: Ampliar `CloseSessionUseCaseTest`** — `test/.../domain/usecase/session/CloseSessionUseCaseTest.kt`

  Que el recálculo se invoque tras el cierre, y que **una excepción del recálculo no propague** — el cierre de sesión no puede fallar por el árbol. (D6)

- [ ] **T34: Ampliar `ImportBackupUseCaseTest`** — `test/.../domain/usecase/backup/ImportBackupUseCaseTest.kt`

  Que el recálculo se invoque tras la importación. (T18)

- [ ] **T35: Ampliar `ValidateBackupUseCaseTest`** — `test/.../domain/usecase/backup/ValidateBackupUseCaseTest.kt`

  Un respaldo v12 completo es válido; uno v11 **sin `tree_state`** también; uno v10 sigue siendo rechazado. Es la verificación directa de CA-37.09. (D8)

- [ ] **T36: Ampliar `ExportBackupUseCaseTest`** — `test/.../domain/usecase/backup/ExportBackupUseCaseTest.kt`

  Los casos que fijan `schemaVersion` pasan a 12.

- [ ] **T37: `TreeViewModelTest`** — `test/.../ui/tree/TreeViewModelTest.kt` (Base: `HomeViewModelTest.kt`)

  Que `init` recalcula antes de exponer el estado; que el estado sin historial no expone conteo de días (CA-37.10); que un árbol maduro con salud 0 conserva la etapa (CA-37.11).

- [ ] **T38: Ampliar `HomeViewModelTest`** — `test/.../ui/home/HomeViewModelTest.kt`

  Que `treeState` llega al `uiState` y que **sobrevive a una emisión posterior del flujo del día** — es la regresión que T27 puede introducir.

#### Fase 8 — Documentación arquitectónica (CA-37.12)

- [ ] **T39: Documentar la entidad nueva** — `docs/architecture/domain_and_state_model.md`

  §2: bloque `tree_state` con diccionario inline y la razón de cada columna (D1). §3: anotar que `tree_state` **no tiene relaciones** y que la dependencia con `session` es de lectura, no de integridad. §4: dominio cerrado `TreeGrowthStage` con sus cuatro valores y sus cortes. §5.1: nota en el ciclo de vida de `session` sobre el recálculo posterior al cierre, marcada como lectura sin efecto sobre la sesión. Encabezado: **versión de esquema 19**, corrigiendo de paso el dato desactualizado y anotando que v17, v18 y v19 no traen migración por ADR-019.

- [ ] **T40: Documentar el componente nuevo y su aislamiento** — `docs/architecture/architecture_blueprint.md`

  §2.1: corregir la versión de esquema (v14 → v19) y la del respaldo (→ 12). §3 `DOM-01`: `TreeHealthRule`, `TreeGrowthStageRule`, `TreeGrowthStage`, `TreeState`, `TreeRepository` y los dos use cases. §3 `DAT-01`: `TreeStateDao`, `TreeRepositoryImpl`. §3 `UI-01`: `TreeScreen` y la ruta `tree`; `UI-02`: `TreeIcon` y la tarjeta de acceso. §4: fila de trazabilidad de HU-37. §5: **ADR nuevo** con la excepción de alcance — funcionalidad visual aislada, dependencia unidireccional, y la lista explícita de componentes que **no** leen el árbol (CA-37.08).

- [ ] **T41: Documentar el acceso y la pantalla** — `docs/architecture/interfaces_contract.md`

  §2.2 `Flujo B — Inicio`: trigger nuevo `B1-T8: Abrir el Árbol de Entrenamiento`, con precondiciones (ninguna), efecto (navegación a `tree`) y postcondición (sin cambio de estado del sistema). Nota en `B1-T7` de que el recálculo del árbol corre **después** del barrido. Sección nueva `Flujo N — Árbol de Entrenamiento` con `N1-T1: Recalcular y Mostrar el Estado del Árbol` y `N1-T2: Volver a Inicio`, declarando que el flujo no produce efectos sobre ningún otro contenedor.

- [ ] **T42: Verificar que `system_definition_document.md` no cambió** — `docs/domain/definition/system_definition_document.md`

  Comprobación explícita, no edición: la exclusión de §2.1 se mantiene intacta (CA-37.12).

#### Fase 9 — Cierre de la historia

- [ ] **T43: Registrar el desarrollo** — `docs/domain/stories/HU-37-arbol-progreso-entrenamiento/dev-record.md` (nuevo, patrón de HU-36)

- [ ] **T44: Actualizar fases y métricas** — `index.md` (Refinamiento y Desarrollo a ✅, métricas de tiempo) y `cambios.md` (entradas de refinamiento y desarrollo)

---

### Riesgos y observaciones

**El riesgo declarado por la historia —el orden dentro del cambio de día— se resuelve por construcción, no por coordinación.** T20 pone el recálculo en la línea siguiente al barrido, dentro de la misma corrutina y el mismo `try`. No hay dos observadores compitiendo, así que no hay carrera que ganar. Si algún día el barrido se mueve fuera de `MainViewModel`, el recálculo tiene que moverse con él: la dependencia es de secuencia, y el comentario de T20 existe para que quien lo mueva lo sepa.

**`TreeRepositoryImpl` no tendrá tests unitarios, igual que ningún otro repositorio del proyecto.** Depende de Room, y el cableado real —las dos consultas nuevas de T12 alimentando las reglas— solo se ejerce a mano. Lo que sí queda probado es lo que decide (T29, T30) y el contrato de los use cases (T31, T32); lo que no, es que el SQL cuente lo que se cree que cuenta. Los puntos 2 y 3 de la validación manual existen exactamente por esto.

**El respaldo rompe compatibilidad con el formato v9 y v10, y esta historia no lo empeora ni lo arregla.** `validateBackup` ya solo aceptaba v11 y v8; se añade v11 como "anterior" y se conserva v8 como legado. Un respaldo v9 o v10 sigue siendo irrestaurable, como antes de esta historia.

**`day_skip` no protege al árbol y esto va a sorprender.** CA-37.07 lo fija sin ambigüedad: marcar «Hoy no entreno» resuelve el día para la determinación de sesión, pero para el árbol es idéntico a no abrir la app. Es coherente con que `day_skip` tampoco cuente como adherencia ni silencie `ROUTINE_INACTIVITY`, y la implementación lo consigue **sin escribir código**: `tree_state` nunca lee `day_skip`. Se anota porque la ausencia de código es justo lo que hace difícil de verificar la decisión — el punto 6 de la validación manual la ejerce.

**La app arranca por primera vez con `tree_state` vacía y eso es correcto.** A diferencia de `rotation_state`, que `ProfileRepositoryImpl:51` inicializa al crear el perfil, el árbol **no se siembra**: T13 devuelve el estado inicial cuando la fila no existe, y el primer recálculo la crea. Sembrarla exigiría un seeder para un dato enteramente derivable. La consecuencia es que un ejecutante recién registrado ve la semilla antes de que exista ninguna fila, que es exactamente lo que CA-37.10 describe.

**El conteo de sesiones incluye las de descarga.** `countClosedSessions` no filtra por `deload_id`, porque una sesión de descarga es entrenamiento registrado y CA-37.07 solo excluye `IN_PROGRESS`. Es coherente con la historia y con `countSessionsInWeek`, que tampoco filtra; se anota porque `getSessionIdsByRoutineInRange` **sí** excluye las de descarga, y la asimetría entre consultas vecinas de `SessionDao` puede leerse como descuido.

**Los cuatro íconos son la única entrega no verificable por test.** Que la forma distinga las etapas a 48 dp y que el marrón oscuro se lea sobre fondo oscuro son juicios visuales. El wireframe ya señala el segundo como caso límite y D10 lo resuelve invirtiendo a marrones claros en modo oscuro, pero la verificación es el punto 7 de la validación manual.

**El wireframe muestra la fecha (`Jueves 3 de septiembre`) y el microciclo con posición (`Microciclo 14 · posición 4 de 6`); Inicio hoy no pinta ninguna de las dos.** Igual que en HU-36, el prototipo está declarado "pendiente de validación con Diseño" y ninguna CA los pide. No se añaden.

**HU-38 hereda una sola costura: `TreeVisual`.** D12 la aísla y el último punto de la DoD la verifica. Todo lo demás de esta historia —persistencia, cálculo, momentos de recálculo, ruta, respaldo— es infraestructura que HU-38 consume sin tocar.

---

### Validación manual (no automatizable)

Los tests JVM cubren las dos reglas, los use cases y los ViewModels; Room cubre el SQL en compilación. Lo que sigue verifica el cableado real sobre la base de datos y la pantalla — en particular T12 y T13, que ningún test del proyecto ejerce.

1. **Instalación fresca (T3, T4)** — Desinstalar la app e instalar el build nuevo. Debe abrir sin excepción de Room. Sobre una base v18 **sin desinstalar**, debe fallar al abrir: es el comportamiento acordado de ADR-019, no un defecto.
2. **CA-37.10 (sin historial)** — Registrar el perfil y, sin entrenar nada, abrir la tarjeta de Inicio. Debe decir `Tu árbol` / `Aún no ha germinado` con el ícono de semilla en gris. En la pantalla: etapa `Semilla`, puntaje 100, **sin línea de días** y el mensaje de invitación a la primera sesión.
3. **CA-37.03 y CA-37.06 momento 1** — Ejecutar y cerrar una sesión con al menos una serie. Volver a Inicio: la tarjeta debe haber pasado a brote verde sin necesidad de reiniciar la app. Inspeccionar `tree_state`: una fila, `health_score = 100`, `growth_stage = 'SPROUT'`, `last_session_date` con la fecha de hoy.
4. **CA-37.03 (descenso lineal)** — Con una sesión cerrada, adelantar la fecha del dispositivo y abrir la pantalla en cada salto: `+3 días` → 92 · `+5` → 75 · `+8` → 50 · `+11` → 25 · `+14` → 0. Es la verificación de la recta completa contra datos reales, no contra la regla aislada.
5. **CA-37.06 momento 2 (el riesgo principal)** — Iniciar una sesión, registrar una serie y **no cerrarla**. Adelantar la fecha un día y abrir la app. El barrido debe cerrarla como `INCOMPLETE` **conservando la fecha de ayer**, y el árbol debe quedar con salud 100 —`d = 1`—, no marchito. Si queda marchito, el recálculo corrió antes del barrido.
6. **CA-37.07 (`day_skip` no protege)** — Con una sesión cerrada hace 8 días, marcar «Hoy no entreno». El árbol debe seguir en 50, sin recuperar salud. Verificar además que la última sesión reasignada temporalmente cuenta como cualquier otra.
7. **CA-37.05 y RNF23** — Recorrer las cinco bandas en modo claro y oscuro, comprobando que la forma distingue las cuatro etapas a 48 dp y que el marrón de salud 0 se lee sobre el fondo oscuro.
8. **CA-37.11 (maduro y marchito)** — Con 30 sesiones acumuladas y 30 días sin entrenar: etapa `Maduro`, salud 0, ícono del tamaño de siempre en marrón oscuro. Registrar una sesión y verificar que recupera salud **conservando la etapa**.
9. **CA-37.09 (respaldo)** — Exportar con el build nuevo (debe decir `schemaVersion: 12` y traer `tree_state`), borrar datos y restaurar: el árbol vuelve. Luego restaurar un respaldo **exportado antes de esta historia** (v11): debe restaurar sin error y dejar el árbol coherente con el historial restaurado.
10. **CA-37.08 (aislamiento)** — Con el árbol marchito, comprobar que la prescripción de carga, el resumen post-sesión, el centro de alertas y la adherencia semanal se comportan exactamente igual que con el árbol sano. Revisar el diff: ningún archivo de la lista de "Lo que NO se toca" debe aparecer.
11. **DoD (layout sustituible)** — Verificar que `TreeVisual` es el único composable que conoce el contenido del área del árbol, y que cambiar su cuerpo por un `Box` vacío de 180×180 dp deja el resto de la pantalla intacto. Es la precondición de HU-38.
