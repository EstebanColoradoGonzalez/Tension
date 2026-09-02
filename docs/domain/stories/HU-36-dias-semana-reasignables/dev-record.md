## Dev Agent Record — Dev-Rápido

**Autor**: Esteban Colorado González | **Fecha**: 2026-09-01

### Debug Log

| # | Tipo | Descripción | Resolución |
|---|------|-------------|------------|
| 1 | Alcance | **El texto de la historia y su wireframe piden dos modelos incompatibles.** La descripción y CA-36.03 dicen que la rotación cíclica no se toca; CA-36.04 ("propone de nuevo la relación permanente día → rutina"), CA-36.07 y el mockup del domingo ("Tu plan no tiene rutina asignada al domingo") solo son realizables si el día decide la rutina. Bajo rotación pura el domingo recibe la rutina de la posición vigente y la tarjeta de descanso no puede existir | Se consultó con el PO antes de escribir el plan, presentando las tres lecturas posibles. Decisión: **el día determina la rutina**; `rotation_state` conserva íntegro el avance de posición y el conteo de microciclos pero deja de indexar la rutina. Documentado como el hallazgo de alcance del refinamiento y en D4/D5 |
| 2 | Alcance | **Bajo determinación por día, una rutina sin día asignado nunca se propone.** Hoy una rutina creada por el ejecutante (HU-23) entra en la rotación sola: sube `routineCount` y le llega su turno. `week_day` solo cubre las 6 del seed, y la reasignación permanente está declarada fuera de alcance por la propia historia (regla de negocio 7). Ninguna CA cubre el caso | Mitigado dentro del alcance (D6): el selector ofrece *toda* rutina con versión vigente no vacía, no solo las seis con día. La rutina sigue siendo **ejecutable**; lo que se pierde es que sea **propuesta automáticamente**. Levantado en Riesgos como necesidad de una historia futura |
| 3 | Esquema | La historia declara "Beta sin migración: la base de datos se reinicia", pero el repo trae migraciones para todas las HU previas y `DatabaseModule` no tiene `fallbackToDestructiveMigration` | Se consultó con el PO. Precisión recibida: **el reinicio lo hace el ejecutante desinstalando y reinstalando; la app no lo hace por sí sola** y debe comportarse como si fuera su primera salida a producción. `version = 17` sin `MIGRATION_16_17` y sin `fallbackToDestructiveMigration`. Migraciones históricas intactas. Documentado como D10 y como ADR-019 |
| 4 | Regresión | `week_day` lleva FK a `routine` con `ON DELETE SET NULL`, y la importación de respaldo borra e reinserta `routine`. Si la tabla no entra en `TABLE_ORDER_INSERT`, **cada restauración perdería la relación día → rutina en silencio** y nada la repondría. Es una tabla nueva, no una columna: las HU-30 a HU-34 pudieron dejar `SCHEMA_VERSION` en 9, esta no | Ambas tablas añadidas a `TABLE_ORDER_INSERT` justo después de `routine` (el orden de borrado se deriva por `reversed()`, así que quedan antes de ella al borrar). `SCHEMA_VERSION` 9 → **10**. Documentado como D11 |
| 5 | Regresión | El camino legado (`transformV8ToV9`) no produce `week_day`, y `importFromJson` recorre `TABLE_ORDER_INSERT` con `getJSONArray` — un respaldo v8 habría lanzado `JSONException`, y si se hubiera puesto un array vacío los 7 días habrían quedado sin rutina tras restaurar | El transformador reconstruye los 7 días asignando las rutinas presentes en el respaldo de lunes en adelante (`buildLegacyWeekDays`); los días sin rutina disponible quedan como descanso, que es su estado válido. `daily_routine_override` se añade vacía |
| 6 | Corrección propia | Primera implementación: `getTodaySession()` leía `LocalDate.now()` **al construir el flujo**. Cruzar la medianoche con la app abierta habría dejado la propuesta congelada en el día en que se abrió la pantalla, y la reversión automática de CA-36.04 nunca habría ocurrido sin reiniciar el proceso | La fecha pasa a venir de un flujo `currentDate()` que reemite al cruzar la medianoche local: no es sondeo — espera exactamente hasta el siguiente cambio de día y se cancela con el alcance de quien recolecta. En `getReassignableRoutines` la fecha se lee dentro de la transformación, no al construir el flujo |
| 7 | Entorno | `./gradlew` aborta con *"Gradle 8.11.1 requires Java 1.8 or later. You are currently using Java 1.7"* — el `JAVA_HOME` del sistema apunta a `C:\apps\java\jdk1.7.0_79` | Se ejecutó el build exportando `JAVA_HOME=C:/Program Files/Eclipse Adoptium/jdk-17.0.20.101-hotspot`. No se modificó ninguna configuración del proyecto |
| 8 | Compilación | `SessionRepositoryImpl` ya tenía un `private companion object` con `DAYS_PER_WEEK`; añadir un segundo dio *"Only one companion object is allowed per class"* | La constante del tick de cambio de día se fusionó en el companion existente |

### Completion Notes

- ⚡ **Dev-Rápido:** el día de la semana pasa de ser texto dentro del nombre de la rutina a ser **una entidad del dominio con relación explícita a la rutina**, y se añade la reasignación temporal de esa relación para una sola sesión. Es la novena y última hija de HU-28, y la de mayor riesgo arquitectónico del conjunto.
- **El riesgo declarado por la historia se resolvió eligiendo, no conciliando (D4, D5).** La frontera quedó así: **el día decide qué rutina, la rotación decide cuándo cierra el microciclo.** `RotationResolver.advanceRotation` no cambió ni de firma ni de comportamiento, y `resolveRoutineIndex` salió de la ruta de determinación sin desaparecer. `closeSession` no se editó en su bloque de rotación: se verificó (T29) que no lee el día ni la reasignación.
- **CA-36.03 quedó fijada por contrato, no por disciplina.** `DailyRoutineRule.resolve(today, permanentRoutineId, override)` **no recibe posición ni conteo de microciclo**: no hay parámetro por el que una reasignación pueda alcanzar `rotation_state`. `RotationResolverTest` añade dos casos que declaran esa independencia como comportamiento esperado.
- **CA-36.08 sale gratis por diseño.** La regla no compara, sustituye: reasignar a la rutina que ya correspondía produce el mismo `routineId` por la misma ruta, y el único efecto observable es el aviso de que aplica solo hoy.
- **CA-36.06 se cumple en dos capas, y la de arriba es estructural (D7).** La acción vive en la tarjeta de sesión propuesta y en la de día de descanso, y `HomeUiState.showNextSessionCard` ya excluía la sesión activa: con sesión en curso Inicio compone `ResumeSessionCard`, que no la aloja, y `ActiveSessionScreen` no la gana. `SetTemporaryRoutineUseCase` valida y lanza — es la red de la ruta de datos, y es lo que el test ejerce. **No se añadió estado deshabilitado**: un control visible que rechaza es peor que un control ausente cuando la razón ya está en pantalla.
- **CA-36.09 no necesitó código.** `getSessionHistory` y `getSessionDetail` derivan el nombre de `session.routine_version_id` → `routine`, es decir de la rutina **ejecutada**. La sesión no guarda de dónde vino la rutina, así que una sesión reasignada ya aparece asociada a lo que realmente se entrenó. Queda como verificación manual (punto 10), no como cambio.
- **La reversión automática es semántica, no un efecto (D2).** `daily_routine_override` guarda la fecha a la que aplica y solo se honra si es hoy: sin tarea programada y sin borrado al cerrar sesión. Caduca al cambiar el día aunque la app no se abra, y sobrevive a un segundo inicio de sesión el mismo día — la unidad de la reasignación es el día.
- **La staleness de la fecha era un defecto real y se corrigió antes de cerrar (Debug Log 6).** La determinación depende del calendario; leerlo una sola vez al construir el flujo habría hecho inalcanzable la reversión con la app abierta.
- **Con descarga activa, el día resuelve la rutina y la descarga resuelve la versión (D5).** Se sustituyó el indexado de versiones congeladas por `microcyclePosition` por una búsqueda directa día → rutina → `deload_frozen_version` de esa rutina. Más estricto que lo anterior y sin tocar HU-14: el conteo de sesiones de descarga, el cierre del ciclo y el reinicio de cargas dependen del número de sesiones cerradas, no de qué rutina tocaba. Efecto colateral aceptado: el domingo ya no propone sesión, así que un ciclo de 6 sesiones ocupa una semana de calendario en lugar de 6 días corridos.
- **Los nombres del seed perdieron el día y no ganaron columna (D9).** `"Lunes: Pecho y Hombro (Push - Foco Deltoides Lateral y Medio)"` → `"Push — Foco Deltoides Lateral y Medio"`, siguiendo el wireframe. **No se añadió `routine.focus`**: extraer el enfoque a su propio campo obligaría a un editor que ninguna CA pide. `DefaultPlanTest` gana una aserción de que **ningún nombre menciona un día**, para que CA-36.01 no se revierta por descuido en un rename futuro.
- **Las etiquetas de día son presentación (D12).** `week_day` guarda `id` y `code`; "Lunes" y "LUN" viven en `strings.xml` tras `weekDayName()` / `weekDayShortName()`, replicando `AlertTypeLabel.kt`. Un único sitio para que Inicio, el selector y la pestaña Plan no puedan divergir. `muscle_zone` y `equipment_type` sí guardan nombre porque son catálogos extensibles; `week_day` es un dominio cerrado de 7 valores.
- **El preview pasó a observar la sesión de hoy (D8).** `onNavigateToPreview` tiene un único invocador —la tarjeta de Inicio—, así que el preview siempre muestra la propuesta del día: los argumentos de la ruta quedan como valor inicial para que la primera composición no parpadee, y la fuente de verdad es `GetTodaySessionUseCase`. Es lo que permite reasignar **en la misma pantalla** sin re-navegar. La ruta no cambió de firma.
- **`getNextSessionInfo` se convirtió en `getTodaySession` y devuelve un estado, no un anulable (D4).** El `NextSession?` anterior colapsaba tres situaciones distintas en `null` y dejaba a Inicio sin nada que mostrar; con el domingo, la ausencia de propuesta pasa a ser presentable. Se renombró el use case en lugar de conservar `NextSessionInfo` sobre un tipo que ya no describe "la siguiente" sino "la de hoy".
- **Tests: 9 archivos nuevos, 4 actualizados, 1 eliminado.** La regla pura recibió los 8 casos de las cinco CA que decide; `HomeViewModel` —que no tenía test— recibió 7, incluidos los dos de CA-36.06. Balance de la suite: **611 tests** (+2 respecto de los 609 previos: +38 nuevos, −2 del test retirado, y los ajustes de fixtures no cambian el conteo).
- **`SessionRepositoryImpl` sigue sin tests unitarios** —depende de Room— y `getTodaySession` es el cambio más consecuente de la historia. Lo que queda probado es la regla que decide y el contrato del use case; lo que no, el cableado de los seis DAOs, incluida la variante con descarga. Los puntos 1 a 7 y 11 de la validación manual existen por eso.
- **Ningún test ejerce el arranque en v17** porque D10 elimina la migración: no hay camino que probar. Lo que sí se verifica es la semilla (`DefaultWeekDaysTest`) y, a mano, la instalación fresca.
- **Observaciones levantadas, no corregidas:** (a) una rutina sin día no se propone sola y esta historia no da forma de asignarle día — necesita historia futura; (b) `AdherenceRule` compara contra `profile.target_weekly_frequency`, y con el domingo sin propuesta el máximo alcanzable sin reasignar es 6: una frecuencia objetivo de 7 vuelve la alerta `LOW_ADHERENCE` inevitable. No es objeto de esta historia y no se tocó.
- **El wireframe no se implementó en dos puntos, deliberadamente.** Dibuja "5 ejercicios · ~55 min" y "Microciclo 14 · posición 4 de 6"; la implementación no calcula duración ni presenta la posición, y ninguna CA los pide. El prototipo está declarado "pendiente de validación con Diseño", de modo que se respetó la implementación existente por CA-36.05 (presentación equivalente a la actual). Si Diseño valida el wireframe, es una historia aparte.

### File List

| Acción | Archivo | Descripción |
|--------|---------|-------------|
| Creado | `data/local/entity/WeekDayEntity.kt` | Tabla `week_day`: 7 filas, `id` ISO-8601, `code` del dominio cerrado, `routine_id` anulable con FK `SET NULL` (D1) |
| Creado | `data/local/entity/DailyRoutineOverrideEntity.kt` | Tabla `daily_routine_override` de fila única: `date` + `routine_id` con FK `CASCADE` (D2) |
| Creado | `data/local/dao/WeekDayDao.kt` | Lectura de los 7 días, del día por `id` (reactiva y puntual) y actualización |
| Creado | `data/local/dao/DailyRoutineOverrideDao.kt` | `upsert` con `REPLACE`, lectura reactiva y puntual, `clear` |
| Creado | `data/local/seed/model/SeedWeekDay.kt` | Modelo de semilla de la relación día → rutina |
| Creado | `data/local/seed/DefaultWeekDays.kt` | Los 7 días: lunes a sábado → rutinas 1..6, domingo `null` |
| Creado | `data/local/seed/WeekDaySeeder.kt` | Mapeo a `ContentValues`; se ejecuta después de `PlanSeeder` por la FK |
| Creado | `domain/model/WeekDay.kt` | Dominio cerrado de 7 valores con `isoNumber`, `fromIso` y `fromCode`. Sin etiquetas (D12) |
| Creado | `domain/model/WeekDayRoutine.kt` | Relación permanente día → rutina, con `isRestDay` |
| Creado | `domain/model/DailyRoutineOverride.kt` | Reasignación vigente: fecha + rutina |
| Creado | `domain/model/TodaySession.kt` | Estado de la propuesta del día: sesión, descanso, reasignación y día de origen (D4) |
| Creado | `domain/model/ReassignableRoutine.kt` | Opción del selector, con día dueño y marca de "actual" (D6) |
| Creado | `domain/rules/DailyRoutineRule.kt` | **Regla pura** de resolución día → rutina con reasignación. Sin Android ni Room (RNF29). Su firma es la frontera con la rotación (D3) |
| Creado | `domain/usecase/session/GetTodaySessionUseCase.kt` | Reemplaza a `GetNextSessionInfoUseCase` |
| Creado | `domain/usecase/session/GetReassignableRoutinesUseCase.kt` | Opciones del selector |
| Creado | `domain/usecase/session/SetTemporaryRoutineUseCase.kt` | Persiste la reasignación; rechaza con sesión activa (CA-36.06) |
| Creado | `domain/usecase/session/ClearTemporaryRoutineUseCase.kt` | Respalda la acción "Deshacer" |
| Creado | `domain/usecase/plan/GetWeekDayPlanUseCase.kt` | Los 7 días para la pestaña Plan |
| Creado | `domain/repository/WeekDayRepository.kt` | Contrato: plan de días, opciones de reasignación y CRUD del override |
| Creado | `data/repository/WeekDayRepositoryImpl.kt` | Implementación; la fecha se lee al resolver, no al construir el flujo |
| Creado | `ui/components/WeekDayLabel.kt` | `weekDayName` / `weekDayShortName` — única traducción de `WeekDay` a etiqueta (D12) |
| Creado | `ui/components/ReassignRoutineDialog.kt` | Selector compartido por Inicio y preview; radios de ≥ 48 dp, marca "actual", sin ruta nueva |
| Eliminado | `domain/usecase/session/GetNextSessionInfoUseCase.kt` | Sustituido por `GetTodaySessionUseCase` (D4) |
| Modificado | `data/local/database/TensionDatabase.kt` | 2 entidades y 2 DAOs nuevos; `version = 17`. **Sin migración** (D10) |
| Modificado | `di/DatabaseModule.kt` | 2 `@Provides` nuevos. `addMigrations` sin tocar (D10) |
| Modificado | `di/RepositoryModule.kt` | `@Binds` de `WeekDayRepository` |
| Modificado | `data/local/seed/DefaultPlan.kt` | Los 6 nombres sin día, comentarios de sección incluidos; KDoc remite a `DefaultWeekDays` (D9) |
| Modificado | `data/local/seed/PrepopulateFacade.kt` | `WeekDaySeeder.seed(db)` tras `PlanSeeder`, en la misma transacción |
| Modificado | `domain/repository/SessionRepository.kt` | `getNextSessionInfo` → `getTodaySession`; añadido `hasActiveSession` |
| Modificado | `data/repository/SessionRepositoryImpl.kt` | `getTodaySession` + `resolveTodaySession` + `buildTodaySession` + `currentDate()` (flujo de fecha con tick de medianoche); `hasActiveSession`; 2 DAOs inyectados. `closeSession` **sin cambios** |
| Modificado | `data/repository/BackupRepositoryImpl.kt` | 2 tablas en `TABLE_ORDER_INSERT` tras `routine`; `SCHEMA_VERSION` 9 → 10; `buildLegacyWeekDays` para el camino v8 (D11) |
| Modificado | `ui/home/HomeUiState.kt` | `todaySession`, opciones y estado del selector; `showRestDayCard`, `canReassign`, `isTemporaryOverride`; `nextSession` derivado |
| Modificado | `ui/home/HomeViewModel.kt` | `GetTodaySessionUseCase` en el `combine` (5 flujos); 3 use cases nuevos; abrir/cerrar/confirmar/deshacer |
| Modificado | `ui/home/HomeScreen.kt` | `NextSessionCard` con "Hoy te toca", `Día — Rutina`, aviso de reasignación y acciones; `RestDayCard`; `ReassignRoutineDialog` |
| Modificado | `ui/preview/SessionPreviewUiState.kt` | Día, reasignación y estado del selector |
| Modificado | `ui/preview/SessionPreviewViewModel.kt` | Observa la sesión de hoy; los argumentos de ruta quedan como valor inicial; recarga por `flatMapLatest` (D8) |
| Modificado | `ui/preview/SessionPreviewScreen.kt` | Título `Día — Rutina`, aviso, acción y selector. Sin cambios de ruta |
| Modificado | `ui/catalog/TrainingPlanUiState.kt` | `weekDay` por rutina y `restDays` |
| Modificado | `ui/catalog/TrainingPlanViewModel.kt` | Combina el plan con la relación día → rutina |
| Modificado | `ui/catalog/TrainingPlanScreen.kt` | Abreviatura de día como columna guía; filas de día de descanso al cierre |
| Modificado | `res/values/strings.xml` | 14 etiquetas de día + 14 cadenas de reasignación, descanso y plan. `home_next_session_label` retirado (quedó sin uso) |
| Creado | `app/schemas/…/17.json` | Esquema v17 generado por KSP, con ambas tablas y sus claves foráneas |
| Creado | `test/…/domain/rules/DailyRoutineRuleTest.kt` | 8 casos: las cinco CA que la regla decide, más la independencia de la rotación |
| Creado | `test/…/domain/usecase/session/GetTodaySessionUseCaseTest.kt` | Propuesta normal, día de descanso, reasignación con día de origen |
| Creado | `test/…/domain/usecase/session/SetTemporaryRoutineUseCaseTest.kt` | Persiste sin sesión activa; lanza con sesión activa; no persiste al lanzar (CA-36.06) |
| Creado | `test/…/domain/usecase/session/ClearTemporaryRoutineUseCaseTest.kt` | Borrado del override |
| Creado | `test/…/domain/usecase/session/GetReassignableRoutinesUseCaseTest.kt` | Marca de "actual"; rutina sin día incluida (D6) |
| Creado | `test/…/domain/usecase/plan/GetWeekDayPlanUseCaseTest.kt` | 7 días en orden; domingo como descanso |
| Creado | `test/…/data/local/seed/DefaultWeekDaysTest.kt` | 7 días sin repetición, lunes a sábado → 1..6, domingo nulo, rutinas existentes |
| Creado | `test/…/ui/home/HomeViewModelTest.kt` | 7 casos: descanso, reasignación activa, `canReassign` con sesión activa, confirmar, error, deshacer |
| Eliminado | `test/…/domain/usecase/session/GetNextSessionInfoUseCaseTest.kt` | 2 casos obsoletos |
| Modificado | `test/…/data/local/seed/DefaultPlanTest.kt` | Los 6 nombres nuevos; aserción de que ningún nombre menciona un día |
| Modificado | `test/…/domain/model/RotationResolverTest.kt` | 2 casos que fijan CA-36.03: el avance no depende de la rutina ejecutada |
| Modificado | `test/…/data/repository/BackupRepositoryImplTest.kt` | Versión y builders alineados con `SCHEMA_VERSION` en lugar del literal 9 |
| Modificado | `docs/architecture/domain_and_state_model.md` | §2 ambas entidades con diccionario; §3 dos relaciones; §4 dominio cerrado `WeekDay`; §5.1 determinación previa al nacimiento; §6.1 las 7 filas y el plan renombrado; encabezado a v17 (CA-36.10) |
| Modificado | `docs/architecture/architecture_blueprint.md` | Componentes en las 4 capas; trazabilidad de RF-09/10/11 reescrita; **ADR-019** (v17 sin migración); D-01 revisada y D-09/D-10 añadidas (CA-36.10) |
| Modificado | `docs/architecture/interfaces_contract.md` | `B1-T1` con determinación por día; **`B1-T3`** y **`B1-T4`** nuevos; `ERR_REASSIGN_SESSION_ACTIVE`; 2 restricciones de interfaz (CA-36.10) |
| Creado | `docs/domain/stories/HU-36-…/refinamiento.md` | Refinamiento técnico: 12 decisiones, 59 tareas, riesgos y 16 escenarios manuales |
| Creado | `docs/domain/stories/HU-36-…/dev-record.md` | Este registro |
| Modificado | `docs/domain/stories/HU-36-…/index.md` | Fases de Refinamiento y Desarrollo a ✅; métricas de tiempo |
| Modificado | `docs/domain/stories/HU-36-…/cambios.md` | Entradas de refinamiento y desarrollo |

### Verificación

| Comando | Resultado |
|---|---|
| `./gradlew :app:compileDebugKotlin` | **BUILD SUCCESSFUL** — 0 errores; 3 warnings de deprecación preexistentes en iconos |
| `./gradlew :app:testDebugUnitTest` | **611 tests · 0 fallos · 0 errores · 0 omitidos** (86 suites) |
| `./gradlew build` (debug + release + lint + tests) | **BUILD SUCCESSFUL** — release también 611/611 |
| Android Lint | **0 errores · 95 warnings**, todos preexistentes. Ninguna de las 28 cadenas nuevas aparece como recurso sin uso; las 24 `UnusedResources` reportadas son las mismas de antes de la historia |
| Versión de esquema | v16 → **v17**. `17.json` generado con `week_day` (FK `SET NULL`, índice único en `code`) y `daily_routine_override` (FK `CASCADE`). **Sin `MIGRATION_16_17`** por D10 |
| Barrido de residuos | Sin referencias a `getNextSessionInfo` ni `GetNextSessionInfoUseCase` en `main` ni en `test`. Ningún nombre de rutina del seed menciona un día (fijado por test) |

Balance de la suite: **+38** por los 9 archivos nuevos, **−2** por el test retirado de `GetNextSessionInfoUseCase`; 609 → 611.

La validación manual de los 16 escenarios —incluida la instalación fresca, el fallo deliberado al abrir sobre v16, el domingo, el avance de rotación con y sin reasignación, y el ciclo de descarga— queda registrada en `refinamiento.md` § *Validación manual (no automatizable)* y **no se ejecutó en esta sesión**: requiere dispositivo y manipulación de la fecha del sistema.

### Métricas Dev-Rápido

- Tiempo sesión IA: 49 min
- Tareas manuales DoD: 0 min
- Tiempo total: 49 min

---

### Corrección posterior a la entrega (2026-09-01)

Revisión del ejecutante sobre la app. Se corrigió en código y documentación arquitectónica, sin abrir historia nueva (decisión del PO).

**Dos afirmaciones de este registro quedan rectificadas:**

1. **«Pestaña Plan con día explícito» era falso donde importa.** La columna de día se añadió a `TrainingPlanScreen` (ruta `training-plan`), que **solo es alcanzable desde el detalle de una alerta**. La pestaña "Plan" del Diccionario que el ejecutante usa es `RoutineListScreen` (ruta `routine-list`), y no se tocó. CA-36.01 quedó cumplida en una pantalla secundaria. Corregido: `RoutineListScreen` muestra ahora los días de cada rutina y las filas de descanso.

2. **La observación «una rutina sin día no se propone sola y necesita historia futura» queda resuelta.** La edición permanente de la relación día → rutina se implementó (`UpdateRoutineWeekDaysUseCase`, `WeekDayAssignmentRule`, selector múltiple en `RoutineListScreen`). Una rutina creada por el ejecutante ya puede tomar un día y proponerse sola.

**Defecto heredado corregido de paso:** `RoutineVersionDao` contaba los ejercicios de una versión con `COUNT(pa.exercise_id)`, así que un slot dual —dos ejercicios que se alternan— contaba 2. Es **uno**: o se hace uno o se hace el otro. Pasa a `COUNT(DISTINCT pa.slot)`, la misma unidad que ya usaban el preview, `startSession` y el protocolo de descarga. Viene de HU-26/HU-29, no de esta historia.

**Cardinalidad decidida con el PO:** una rutina puede ocupar **varios días**; un día ocupa **una sola rutina**. Asignar un día que pertenecía a otra rutina lo **mueve**, y el selector lo advierte antes de confirmar. `ReassignableRoutine.weekDay` pasó a `weekDays`.

**Restricción añadida:** cambiar los días se rechaza durante una descarga activa. La razón es concreta, no de consistencia: la descarga cierra al ejecutar tantas sesiones como versiones congeladas, y dejar sin días a una rutina congelada haría que su sesión no se propusiera nunca.

**Verificación:** `./gradlew build` BUILD SUCCESSFUL · **625 tests · 0 fallos** en debug y release (611 → 625) · Lint 0 errores, 95 warnings, ninguno nuevo.

| Acción | Archivo | Descripción |
|--------|---------|-------------|
| Creado | `domain/rules/WeekDayAssignmentRule.kt` | Regla pura: tomar, liberar o no tocar el día al editar los días de una rutina |
| Creado | `domain/usecase/plan/UpdateRoutineWeekDaysUseCase.kt` | Edición permanente día → rutina; rechaza durante descarga activa |
| Creado | `test/…/domain/rules/WeekDayAssignmentRuleTest.kt` | 7 casos, incluida la rutina en varios días y el traslado de un día ocupado |
| Creado | `test/…/domain/usecase/plan/UpdateRoutineWeekDaysUseCaseTest.kt` | 4 casos, incluido el rechazo durante descarga |
| Modificado | `data/local/dao/RoutineVersionDao.kt` | Conteo de ejercicios por slot en las dos consultas de listado |
| Modificado | `domain/repository/WeekDayRepository.kt` + `data/repository/WeekDayRepositoryImpl.kt` | `setRoutineWeekDays` transaccional; `weekDay` → `weekDays` en las opciones de reasignación |
| Modificado | `ui/catalog/RoutineListUiState.kt`, `RoutineListViewModel.kt`, `RoutineListScreen.kt` | Columna de días, filas de descanso y selector múltiple de días |
| Modificado | `ui/catalog/TrainingPlanUiState.kt`, `TrainingPlanViewModel.kt`, `TrainingPlanScreen.kt` | Días en plural |
| Modificado | `ui/components/WeekDayLabel.kt`, `ReassignRoutineDialog.kt` | `weekDaysShortLabel` / `weekDaysLabel`; el selector lista todos los días de la rutina |
| Modificado | `res/values/strings.xml` | 5 cadenas del selector de días |
| Modificado | `test/…/data/local/seed/DefaultPlanTest.kt` | 2 casos que fijan el conteo por slot del plan predeterminado |
| Modificado | `test/…/domain/usecase/session/GetReassignableRoutinesUseCaseTest.kt`, `test/…/ui/home/HomeViewModelTest.kt` | Adaptados a `weekDays` |
| Modificado | `docs/architecture/*.md` | `D6-T2`, `ERR_WEEK_DAYS_DELOAD_ACTIVE`, D-11 y D-12, cardinalidad y conteo por slot |
