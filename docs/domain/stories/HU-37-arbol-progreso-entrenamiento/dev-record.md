## Dev Agent Record — Dev-Rápido

**Autor**: Esteban Colorado González | **Fecha**: 2026-09-02

### Debug Log

| # | Tipo | Descripción | Resolución |
|---|------|-------------|------------|
| 1 | Alcance | **CA-37.09 pedía algo que el respaldo no podía cumplir.** `validateBackup` solo aceptaba dos versiones: la vigente (11) y la legada (8). El criterio "un respaldo de la versión anterior se restaura sin error" apunta a **v11**, no a v8, así que subir a v12 sin más habría inutilizado en silencio todo respaldo exportado por la versión de la app en manos del ejecutante. Ninguna CA lo menciona | Tercera rama de compatibilidad, `PREVIOUS_SCHEMA_VERSION = 11`: se valida sin `tree_state` en la lista de tablas requeridas y se importa con `optJSONArray`. La reconstrucción la hace `ImportBackupUseCase` invocando el recálculo tras restaurar. No hace falta distinguir formatos: **el árbol siempre es derivable**, y por eso restaurar sin él nunca deja un estado inválido. Documentado como D8 |
| 2 | Contradicción entre CAs | **CA-37.04 ("la etapa nunca retrocede") y CA-37.09 ("estado válido derivado del historial restaurado") no pueden cumplirse a la vez** si la etapa persiste un máximo pegajoso: restaurar un respaldo antiguo con menos sesiones dejaría un árbol maduro sobre un historial de brote | Se leyó la invariante donde el criterio la sitúa — "cualquiera que sea **la salud**" —: es frente a la salud, no frente al historial. La etapa se deriva sin memoria del total de sesiones cerradas, y al ser función monótona no puede bajar mientras se entrene. Sin `max(persistida, calculada)`. Fijado por test (`stage never regresses as sessions accumulate`) y documentado como D3 y D-16 |
| 3 | Dato desactualizado | `architecture_blueprint.md` §2.1 declaraba **esquema v14**; el real era **v18**, y las migraciones solo llegan a 15→16 — v17 y v18 no tienen ninguna, por ADR-019 | Se heredó la excepción: `version = 19` sin `MIGRATION_18_19` y sin `fallbackToDestructiveMigration`. `addMigrations(...)` intacto. De paso se corrigió el dato del blueprint y el del respaldo (T40) |
| 4 | Aislamiento | El barrido del cambio de día (`ResolveStaleSessionUseCase`) llama a `sessionRepository.closeSession` **directamente**, sin pasar por `CloseSessionUseCase` — enganchar el recálculo solo en el use case habría dejado el cierre automático sin cubrir | El momento 2 lo cubre: en `MainViewModel`, el recálculo va en la **línea siguiente** al barrido, dentro de la misma corrutina y el mismo `try`. El orden queda expresado como secuencia, no como coincidencia entre dos observadores, que es justo lo que la nota de CA-37.06 protege. `SessionRepositoryImpl.closeSession` no se editó (D6) |
| 5 | Regresión introducida y evitada | El `collect` principal de `HomeViewModel` hace `newState.copy(deloadState = current.deloadState, …)`: cualquier campo del `UiState` que no viaje en el `combine` **se borra en cada emisión del estado del día**. El árbol es uno de ellos | `treeState = current.treeState` en el mismo `copy`, siguiendo el precedente de `deloadState`. El `combine` no se amplió — ya está en su sobrecarga de cinco flujos y el árbol no depende de ninguno de ellos. Fijado por el test `tree state survives a later emission of the day state`, que es el que fallaría si alguien lo quitara |
| 6 | Corrección propia | La inserción de `"tree_state"` en `TABLE_ORDER_INSERT` cayó también en `LEGACY_TABLE_ORDER`: ambas listas empiezan con la misma secuencia `profile, rotation_state, weight_record`. `LEGACY_TABLE_ORDER` describe el formato **v8**, que no tiene esa tabla, y habría hecho fallar la validación de todo respaldo legado | Se revirtió en la lista legada. La tabla se repone vacía en `transformV8ToV9`, por el mismo motivo por el que HU-36 tuvo que reponer `week_day`: `importFromJson` borra e reinserta cada tabla de `TABLE_ORDER_INSERT` y omitirla la dejaría vacía en silencio |
| 7 | Entorno | `./gradlew` aborta con *"Gradle 8.11.1 requires Java 1.8 or later. You are currently using Java 1.7"* — el `JAVA_HOME` del sistema apunta a `C:\apps\java\jdk1.7.0_79`. Mismo hallazgo que HU-36 | Se ejecutó el build exportando `JAVA_HOME=C:/Program Files/Eclipse Adoptium/jdk-17.0.20.101-hotspot`. No se modificó ninguna configuración del proyecto |

### Completion Notes

- ⚡ **Dev-Rápido:** el árbol de entrenamiento entra completo con representación **nativa** — persistencia, cálculo, los tres momentos de recálculo, ruta nueva, pantalla dedicada, tarjeta de acceso y respaldo. Es la primera de las dos hijas de la partición; `HU-38` sustituirá **solo** el bloque visual.
- **El riesgo declarado por la historia se resolvió por construcción, no por coordinación (D6).** El recálculo del cambio de día va en la línea siguiente al barrido, en la misma corrutina. No hay dos observadores compitiendo, así que no hay carrera que ganar. El comentario en `MainViewModel` dice explícitamente que si el barrido se mueve, esto se mueve con él.
- **CA-37.08 se cumple estructuralmente y se verificó sobre el diff.** Tres fronteras: (a) `TreeRepository` es un contrato propio y no dos métodos más en `SessionRepository` — colgarlo del contrato del motor de decisión no rompería el aislamiento, pero lo volvería indistinguible; (b) `tree_state` no declara **ninguna clave foránea**, así que la dependencia con `session` es de cálculo y no de integridad; (c) los enganches viven en `CloseSessionUseCase`, `ImportBackupUseCase` y `MainViewModel`, y **`SessionRepositoryImpl` solo ganó dos consultas de lectura**. Se comprobó que ningún archivo de `domain/rules` del motor, `RotationResolver`, `usecase/alerts`, `usecase/metrics` ni `usecase/deload` aparece en el diff.
- **`system_definition_document.md` no se tocó** (CA-37.12). Verificado con `git status`: sin cambios en `docs/domain/definition/`. La excepción de alcance quedó registrada como **ADR-020**, no como modificación de la frontera.
- **Dos reglas puras, no una (D2).** Las dimensiones son ortogonales y esa ortogonalidad tenía que ser visible en el código: `TreeHealthRule` recibe días y `TreeGrowthStageRule` recibe conteo, ninguna recibe a la otra. La recta de salud se define entre `(2, 100)` y `(14, 0)`, la única que reproduce los cinco puntos de verificación de CA-37.03. El corte de 14 se **alinea** con `ROUTINE_INACTIVITY` sin acoplarse: la alerta mide inactividad por rutina, el árbol la mide global, y por eso `getLastClosedSessionDate` es una consulta nueva y no un parámetro más de `getLastSessionDateByRoutine`.
- **Se persiste lo que los criterios exigen y nada derivable (D1).** `tree_state` tiene cuatro columnas. **No guarda el conteo de sesiones** —la etapa ya lo resume, y guardarlo crearía un segundo sitio donde el mismo hecho puede desincronizarse— ni **los días transcurridos**, que dependen de la fecha de hoy: un entero guardado ayer es rancio hoy, y mostrar valores rancios es justo lo que el recálculo pretende evitar. Se derivan al mapear (D5), de modo que la pantalla no puede mostrar un conteo desactualizado ni aunque el recálculo hubiera fallado.
- **`tree_state` no se siembra, a diferencia de `rotation_state`.** La lectura devuelve el estado de partida cuando la fila no existe, y el primer recálculo la crea. Sembrarla exigiría un seeder para un dato enteramente derivable, y el estado sin fila es exactamente lo que CA-37.10 describe.
- **`day_skip` no protege al árbol, y se consiguió sin escribir código** (CA-37.07). `TreeRepositoryImpl` nunca lee esa tabla. Se anota porque la ausencia de código es lo que vuelve difícil de verificar la decisión — el punto 6 de la validación manual la ejerce a mano.
- **El recálculo es *best-effort* en los momentos 1 y 2.** Un árbol desactualizado es un defecto visual que el siguiente cambio de día corrige; una excepción propagada convertiría una sesión ya cerrada en un error para el ejecutante. `CloseSessionUseCaseTest` fija ambas mitades: que el recálculo se invoca, y que **su fallo no propaga**. `RecalculateTreeStateUseCase` sí propaga: quien decide si una excepción importa es quien lo invoca.
- **Cuatro íconos y no una matriz de veinte (D9, D10).** La forma comunica la etapa, el color comunica la salud, y el tinte se resuelve en Compose desde `LocalTensionSemanticColors`. Los cinco colores nuevos reutilizan tres valores ya validados del tema (`ProgressionPositive`, `Maintenance`, `MetricInsufficient`) y añaden dos marrones que **se invierten a marrones claros en modo oscuro**: el caso límite que señalaba el wireframe — un marrón oscuro sobre `#1C1B1B` no se lee (RNF23).
- **El layout deja lista la costura de HU-38 (D12).** `TreeVisual` es un composable privado con una única responsabilidad, de tamaño fijo 180×180 dp. Ningún otro elemento del layout conoce su contenido, solo su altura: sustituirlo por un `WebView` no reorganiza la pantalla. Es el último punto de la DoD y es verificable.
- **La tarjeta de Inicio se compone siempre y es nativa de forma permanente (D11).** Es la única tarjeta de B1 sin condición de visibilidad, porque el árbol existe desde antes de la primera sesión. Reutiliza el `Card` de `RestDayCard` / `ResolvedDayCard` y no el contenedor primario de la tarjeta de sesión: su posición debajo ya le da la jerarquía subordinada, y competir en color la contradiría. El área táctil cubre la fila entera vía `heightIn(min = 48.dp)` sobre el `Row`, no solo el ícono (RNF06).
- **La ruta no toca la barra inferior.** `NavigationRoutes.TREE` se añadió sin modificar `BottomNavigationBar` ni la condición `showBottomBar`, igual que `DELOAD_MANAGEMENT` (CA-37.02).
- **Tests: 5 archivos nuevos, 4 ampliados. 656 → 701 (+45), 0 fallos.** Los límites que la DoD exige —2, 3, 13 y 14 días; 0, 1, 9, 10, 29 y 30 sesiones— están cubiertos uno por uno, más dos propiedades: que la salud nunca sube al crecer los días y que la etapa nunca retrocede al acumular sesiones.
- **La verificación real de CA-37.09 quedó en `BackupRepositoryImplTest`, no en `ValidateBackupUseCaseTest` como decía T35.** El test del use case mockea el repositorio, así que ampliarlo solo habría probado el mock. Se añadieron allí los cinco casos que ejercen la validación de verdad: v12 completo válido, v11 sin `tree_state` válido, v10 rechazado, v12 al que le falta `tree_state` rechazado, y la importación de un v11 completando la transacción. **T36 resultó ser un no-op**: `ExportBackupUseCaseTest` no fija `schemaVersion` en ninguna aserción, y los `BackupMetadata(…, 7, …)` de otros tests son valores mockeados que no dependen de la constante.
- **`TreeRepositoryImpl` no tiene tests unitarios**, como ningún repositorio del proyecto — depende de Room. Queda probado lo que decide (las dos reglas) y el contrato de los use cases; lo que no, es que las dos consultas nuevas cuenten lo que se cree que cuentan. Los puntos 3 y 4 de la validación manual existen por eso.
- **Lint: 5 warnings nuevos, ninguno accionable.** Cuatro `PluralsCandidate` sobre `%d días` — el proyecto ya tenía 24 del mismo tipo, y las cuatro cadenas solo se usan en ramas donde `d ≥ 2` (0 y 1 tienen sus propias cadenas: "hoy" y "ayer"); y un `Typos` que propone "memento" por "momento", falso positivo del corrector en inglés. **`UnusedResources` sigue en 24**, las mismas de antes: las 21 cadenas y los 4 drawables nuevos están todos en uso.

### File List

| Acción | Archivo | Descripción |
|--------|---------|-------------|
| Creado | `data/local/entity/TreeStateEntity.kt` | Tabla `tree_state` de fila única: `health_score`, `growth_stage`, `last_session_date` anulable, `calculated_at`. Sin FK (D1) |
| Creado | `data/local/dao/TreeStateDao.kt` | `upsert`, `getTreeState(): Flow`, `getTreeStateOnce()` sobre `id = 1` |
| Creado | `domain/model/TreeGrowthStage.kt` | Dominio cerrado de cuatro valores con `fromCode` que cae a `SEED` ante código desconocido |
| Creado | `domain/model/TreeState.kt` | Modelo de las dos dimensiones, con `hasHistory` derivado — la señal única del caso Semilla (D5) |
| Creado | `domain/rules/TreeHealthRule.kt` | Regla pura de salud. Descenso lineal entre `(2, 100)` y `(14, 0)`, `null` → 100, resultado siempre en 0–100 (D2) |
| Creado | `domain/rules/TreeGrowthStageRule.kt` | Regla pura de etapa. Cortes 0 / 1–9 / 10–29 / 30+, sin memoria (D2, D3) |
| Creado | `domain/repository/TreeRepository.kt` | Contrato de dos métodos. Su existencia separada **es** la frontera de CA-37.08 (D4) |
| Creado | `domain/usecase/tree/GetTreeStateUseCase.kt` | Estado observable para la tarjeta y la pantalla |
| Creado | `domain/usecase/tree/RecalculateTreeStateUseCase.kt` | Recálculo con los tres momentos y el orden respecto al barrido documentados en KDoc |
| Creado | `data/repository/TreeRepositoryImpl.kt` | Deriva de `SessionDao`, persiste en `TreeStateDao`, resuelve días al mapear. Estado inicial con fila ausente (D5) |
| Creado | `ui/components/TreeIcon.kt` | Etapa → recurso, salud → tinte. Única traducción, compartida por tarjeta y pantalla. Bandas 0 / 1–33 / 34–66 / ≥67 (D9, D10) |
| Creado | `ui/tree/TreeUiState.kt` | Estado de la pantalla con derivadas de etapa, salud e historial |
| Creado | `ui/tree/TreeViewModel.kt` | Recalcula en `init` **antes** de observar. Momento 3 de CA-37.06 |
| Creado | `ui/tree/TreeScreen.kt` | Barra superior con back nativo como única navegación, `TreeVisual` aislado, etapa, puntaje, días y mensaje contextual (D12) |
| Creado | `res/drawable/ic_tree_seed.xml` | Semilla enterrada. `fillColor` blanco opaco para que el tinte lo sustituya |
| Creado | `res/drawable/ic_tree_sprout.xml` | Brote de dos hojas |
| Creado | `res/drawable/ic_tree_young.xml` | Copa estrecha en punta, tronco definido |
| Creado | `res/drawable/ic_tree_mature.xml` | Copa ancha de tres lóbulos, tronco grueso con raíces |
| Modificado | `data/local/dao/SessionDao.kt` | `countClosedSessions()` y `getLastClosedSessionDate()` — contraparte **global** de la consulta por rutina, con KDoc que declara que no deben acoplarse (T12) |
| Modificado | `data/local/database/TensionDatabase.kt` | `TreeStateEntity` registrada, `treeStateDao()`, `version = 19`. **Sin migración** (ADR-019) |
| Modificado | `di/DatabaseModule.kt` | `provideTreeStateDao`. `addMigrations(...)` intacto |
| Modificado | `di/RepositoryModule.kt` | `bindTreeRepository` |
| Modificado | `data/repository/BackupRepositoryImpl.kt` | `SCHEMA_VERSION = 12`, `PREVIOUS_SCHEMA_VERSION = 11`, `tree_state` en el orden de inserción, validación por versión, `optJSONArray` en la importación, tabla vacía en el camino legado v8 (D8) |
| Modificado | `domain/usecase/backup/ImportBackupUseCase.kt` | Reconstruye el árbol tras restaurar. Resuelve a la vez el respaldo v11 sin tabla y el v12 con fecha vieja |
| Modificado | `domain/usecase/session/CloseSessionUseCase.kt` | Recálculo tras cerrar, best-effort. Momento 1 de CA-37.06 |
| Modificado | `ui/navigation/MainViewModel.kt` | Recálculo **tras** el barrido, misma corrutina. Momento 2 y garantía del orden (D6) |
| Modificado | `ui/navigation/NavigationRoutes.kt` | `TREE = "tree"` |
| Modificado | `ui/navigation/TensionNavHost.kt` | Destino `tree` y cableado de `onNavigateToTree`. `showBottomBar` sin tocar |
| Modificado | `ui/home/HomeUiState.kt` | Campo `treeState` |
| Modificado | `ui/home/HomeViewModel.kt` | `observeTreeState()` en flujo propio y **preservación de `treeState` en el `copy`** del estado del día (Debug Log 5) |
| Modificado | `ui/home/HomeScreen.kt` | `TreeAccessCard` compuesta siempre, bajo las tarjetas de sesión y antes del divisor de progreso. Área táctil de 48 dp sobre la fila (D11) |
| Modificado | `ui/theme/Color.kt` | Diez constantes de árbol con par claro/oscuro; los marrones se invierten en oscuro (D10) |
| Modificado | `ui/theme/Theme.kt` | Cinco roles semánticos nuevos en `TensionSemanticColors` y sus dos paletas |
| Modificado | `res/values/strings.xml` | 21 cadenas: título, etiqueta, cuatro etapas, cinco líneas de tarjeta, cinco mensajes, tres variantes de días y dos descripciones de accesibilidad |
| Creado | `test/domain/rules/TreeHealthRuleTest.kt` | 14 casos: los cinco puntos de verificación, los límites 0/1/2/13/14/30, `null`, día negativo, rango y monotonía |
| Creado | `test/domain/rules/TreeGrowthStageRuleTest.kt` | 8 casos: los cortes 0, 1, 9, 10, 29, 30, 100 y la no-regresión como propiedad |
| Creado | `test/domain/usecase/tree/RecalculateTreeStateUseCaseTest.kt` | Delegación y propagación de fallo |
| Creado | `test/domain/usecase/tree/GetTreeStateUseCaseTest.kt` | Emisión sin transformación y las dos ramas de `hasHistory` |
| Creado | `test/ui/tree/TreeViewModelTest.kt` | Recálculo previo, estado con recálculo fallido, CA-37.10 y CA-37.11 |
| Modificado | `test/domain/usecase/session/CloseSessionUseCaseTest.kt` | +3: recálculo invocado, fallo no propaga, sin recálculo si el cierre falla |
| Modificado | `test/domain/usecase/backup/ImportBackupUseCaseTest.kt` | +2: orden restaurar→recalcular y sin recálculo si la restauración falla |
| Modificado | `test/data/repository/BackupRepositoryImplTest.kt` | +5: la verificación real de CA-37.09 — v12 con tabla, v11 sin tabla aceptado, v10 rechazado, v12 incompleto rechazado, importación de v11 |
| Modificado | `test/ui/home/HomeViewModelTest.kt` | +3: el árbol llega al estado, **sobrevive a una emisión del día** y el caso sin historial |
| Modificado | `docs/architecture/domain_and_state_model.md` | Esquema v19, `tree_state` con diccionario, ausencia de relaciones como dato, `TreeGrowthStage`, nota en §5.1 y el porqué de no sembrar (T39) |
| Modificado | `docs/architecture/architecture_blueprint.md` | v14→v19 y respaldo→12 corregidos, componentes en las cuatro capas, trazabilidad de HU-37, **ADR-020** y decisión de dominio D-16 (T40) |
| Modificado | `docs/architecture/interfaces_contract.md` | `B1-T8`, nota de orden en `B1-T7`, **Flujo N** con `N1-T1` y `N1-T2`, compatibilidad de formato en `J3-T1` y dos restricciones de interfaz (T41) |
| Creado | `app/schemas/…/19.json` | Esquema exportado por Room. `tree_state` sin FK ni índices |

### Verificación

| Comando | Resultado |
|---|---|
| `./gradlew :app:compileDebugKotlin` | **BUILD SUCCESSFUL** — 0 errores; 3 warnings de deprecación de iconos, preexistentes |
| `./gradlew :app:testDebugUnitTest` | **701 tests · 0 fallos · 0 errores · 0 omitidos** (98 suites) |
| `./gradlew build` (debug + release + lint + tests) | **BUILD SUCCESSFUL** — release también **701 · 0 fallos** (98 suites) |
| Android Lint | **0 errores · 100 warnings** (95 preexistentes + 5 nuevos: 4 `PluralsCandidate` sobre `%d días` —el proyecto ya tenía 24 del mismo tipo— y 1 `Typos` falso positivo). **`UnusedResources` sigue en 24**: ninguna cadena ni drawable nuevo aparece sin uso |
| Versión de esquema | v18 → **v19**. `19.json` generado con `tree_state` (fila única, sin FK). **Sin `MIGRATION_18_19`** por ADR-019 |
| Versión de respaldo | 11 → **12**, con v11 aceptado como formato previo y v8 como legado (D8) |
| Aislamiento CA-37.08 | Diff revisado: **ningún** archivo de `domain/rules` del motor de decisión, `RotationResolver`, `usecase/alerts`, `usecase/metrics` ni `usecase/deload`. `SessionRepositoryImpl` **no aparece**: solo `SessionDao` ganó dos consultas de lectura |
| CA-37.12 | `git status docs/domain/definition/` **vacío**: `system_definition_document.md` sin modificar |

Balance de la suite: **+45** — 32 de los 5 archivos nuevos y 13 de los 4 ampliados; 656 → 701.

La validación manual de los 11 escenarios —instalación fresca, el fallo deliberado al abrir sobre v18, el descenso lineal con la fecha adelantada, el orden respecto al barrido, `day_skip`, las cinco bandas en claro y oscuro, y el respaldo v11— queda registrada en `refinamiento.md` § *Validación manual (no automatizable)* y **no se ejecutó en esta sesión**: requiere dispositivo y manipulación de la fecha del sistema.

### Métricas Dev-Rápido

- Tiempo sesión IA: ~65 min (aproximado; no se instrumentó el reloj)
- Tareas manuales DoD: 0 min
- Tiempo total: ~65 min

### Desviaciones respecto del plan aprobado

| Tarea | Desviación | Motivo |
|---|---|---|
| T35 | La verificación de compatibilidad de respaldo se implementó en `BackupRepositoryImplTest`, no en `ValidateBackupUseCaseTest` | El test del use case mockea `BackupRepository`: ampliarlo habría probado el mock, no la validación de versiones. `BackupRepositoryImplTest` ejerce el JSON real |
| T36 | No requirió cambios | `ExportBackupUseCaseTest` no fija `schemaVersion` en ninguna aserción. Se verificó que la suite de respaldo pasa con `SCHEMA_VERSION = 12` |
