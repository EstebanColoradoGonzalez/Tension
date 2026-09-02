## Refinamiento Técnico (Developer)
**Autor**: Esteban Colorado González | **Fecha**: 2026-09-01

---

### Contexto

Hoy el día de la semana no existe en el sistema. Vive en dos sitios, ninguno de los cuales es una relación:

1. **Dentro del texto** del nombre de la rutina — `DefaultPlan.ROUTINES` (`"Lunes: Pecho y Hombro (Push - Foco Deltoides Lateral y Medio)"`).
2. **Implícito en `routine.sort_order`**, que `RotationResolver.resolveRoutineIndex` usa como índice del microciclo. La coincidencia entre "posición 4" y "jueves" es una convención del seed, no una relación: en cuanto el ejecutante falta un día, la posición y el calendario se separan.

**Feature análoga leída completa: la determinación de sesión de HU-05 / HU-09 / HU-14** — es la misma vertical que esta historia interviene, y es el único artefacto del proyecto que resuelve "qué toca hoy".

| Capa | Artefacto | Responsabilidad | Dependencias | Artefactos asociados |
|---|---|---|---|---|
| `DOM-01` | `RotationResolver` | Regla pura: índice de rutina desde la posición, avance del microciclo | Ninguna (Kotlin puro) | `RotationResolverTest` |
| `DOM-01` | `RotationState`, `NextSession` | Modelos de estado y propuesta | — | — |
| `DOM-01` | `GetNextSessionInfoUseCase`, `StartSessionUseCase`, `CloseSessionUseCase`, `GetSessionPreviewUseCase` | Delegan al repositorio | `SessionRepository` | tests homónimos con `mockk` |
| `DAT-01` | `SessionRepositoryImpl.getNextSessionInfo` (`:94`) | Resuelve `rotation_state` → índice → rutina → versión vigente (o versión congelada si hay descarga) | 6 DAOs, `RotationResolver` | sin test (depende de Room) |
| `DAT-01` | `SessionRepositoryImpl.startSession` (`:173`) | Valida versión vigente/congelada, crea `session` + `session_exercise` por slot | `routineVersionDao`, `planAssignmentDao` | — |
| `DAT-01` | `SessionRepositoryImpl.closeSession` (`:432`) | Cierra, evalúa progresión, **avanza rotación** y versiones de fin de microciclo | `rotationStateDao`, `RotationResolver` | — |
| `DB-01` | `rotation_state` (fila única) | `microcycle_position`, `microcycle_count` | — | `MIGRATION_11_12`, backup |
| `UI-02` | `HomeViewModel` | Combina próxima sesión, sesión activa, microciclos y alertas | 5 use cases | sin test |
| `UI-01` | `HomeScreen.NextSessionCard`, `SessionPreviewScreen` | Presentan la propuesta y disparan el inicio | — | — |
| `DAT-01` | `WeekDaySeeder` (**no existe**) | — | — | patrón: `BaseDataSeeder`, `PlanSeeder`, `PrepopulateFacade` |

#### El hallazgo que define el alcance

**El texto de la historia y su wireframe piden dos modelos distintos, y hubo que elegir.**

- La descripción y CA-36.03 dicen que *"la rotación cíclica no se toca"*.
- CA-36.04 dice que la siguiente sesión *"propone de nuevo la relación permanente día → rutina"*, y CA-36.07 más el mockup del domingo (*"Tu plan no tiene rutina asignada al domingo"*) **solo son realizables si el día decide la rutina**. Bajo rotación pura, el domingo recibe la rutina de la posición vigente igual que cualquier otro día: la tarjeta de descanso no puede existir.

Decisión tomada con el PO (2026-09-01): **el día de la semana determina la rutina propuesta.** `rotation_state` conserva íntegro su rol de avanzar posición y contar microciclos al cerrar sesión — que es lo que CA-36.03 protege y de lo que dependen HU-14 y el protocolo de descarga — pero **deja de indexar la rutina**. `resolveRoutineIndex` sale de la ruta de determinación; `advanceRotation` no se toca.

#### La consecuencia que ninguna CA cubre

Bajo determinación por día, **una rutina sin día asignado nunca se propone**. Hoy, una rutina creada por el ejecutante (HU-23) entra en la rotación automáticamente: sube `routineCount`, el microciclo crece y le llega su turno. Después de esta historia, `week_day` solo cubre las 6 rutinas del seed y **la reasignación permanente está declarada fuera de alcance** por la propia historia (regla de negocio 7), así que no hay forma de darle un día.

Se mitiga dentro del alcance en **D6**: el selector de reasignación ofrece *toda* rutina con versión vigente no vacía, no solo las seis que tienen día. Toda rutina sigue siendo **ejecutable**; lo que se pierde es que sea **propuesta automáticamente**. Queda levantado en Riesgos como cobertura pendiente de una historia futura.

#### Lo que NO se toca

- `RotationResolver.advanceRotation` y `microcycleSize` — CA-36.03. Ni firma ni comportamiento.
- `closeSession` en su bloque de rotación, versiones de fin de microciclo y cierre de descarga (`SessionRepositoryImpl.kt:456-561`).
- `activateDeload`, `getDeloadState`, `DeloadNeedRule`, `RoutineFatigueRule` — HU-14 intacta.
- `startSession` — sigue recibiendo un `routineVersionId` ya resuelto. La reasignación se resuelve **antes**, en la determinación; el inicio no distingue si la rutina venía del día o del override. Esta es la frontera que hace que CA-36.03 se cumpla por construcción.
- Historial, detalle de sesión y métricas: `getSessionHistory` y `getSessionDetail` derivan el nombre de `session.routine_version_id` → `routine`, es decir de la rutina **ejecutada**. CA-36.09 ya se cumple sin cambiar código (verificación en la validación manual, punto 8).
- `RoutineRepositoryImpl.deleteRoutine` y su ajuste de `microcycle_position` — el `ON DELETE SET NULL` de `week_day.routine_id` cubre el día huérfano.

---

### Decisiones técnicas

#### D1 — `week_day`: tabla de 7 filas con `routine_id` anulable

`id` es el número ISO-8601 del día (1 = lunes … 7 = domingo), que coincide con `java.time.DayOfWeek.getValue()`: la traducción "hoy" → fila es `LocalDate.now().dayOfWeek.value`, sin mapa intermedio. `code` guarda el valor del dominio cerrado (`MONDAY`…`SUNDAY`) y es la clave del `enum class WeekDay` de la capa de aplicación, según la convención de §4 del modelo de dominio.

El domingo es una fila con `routine_id = NULL`, no una ausencia de fila: CA-36.01 exige que quede **registrado** como día sin rutina, y así el modelo admite asignarle rutina en el futuro sin cambiar de forma.

`ON DELETE SET NULL` sobre `routine_id`: borrar una rutina deja su día sin rutina, no borra el día.

#### D2 — El override es una fila única con fecha, no un borrado programado

`daily_routine_override` es de fila única (`id = 1`), como `profile` y `rotation_state`. Guarda `date` (el día ISO al que aplica) y `routine_id`.

La reversión automática de CA-36.04 es **semántica, no un efecto**: la fila solo se honra si `date` es hoy. Ninguna tarea programada, ningún borrado al cerrar sesión. Esto tiene dos consecuencias buscadas:

- La reasignación no sobrevive al cambio de día aunque la app no se abra en medio.
- Cerrar la sesión reasignada y abrir otra el mismo día conserva la reasignación, que es lo correcto: *"aplica únicamente a esa sesión y ese día"* — el día es la unidad.

`ON DELETE CASCADE` sobre `routine_id`: si la rutina reasignada desaparece, el override desaparece con ella.

#### D3 — La regla de resolución es pura y vive en `domain/rules`

`DailyRoutineRule.resolve(today, permanentRoutineId, override)` → `Resolution(routineId, isOverridden)`. Kotlin puro, sin Android y sin Room (RNF29), y es donde caen CA-36.02, .04, .07 y .08 como tests de mesa (RNF30):

| Entrada | Salida | AC |
|---|---|---|
| override de hoy | rutina del override, `isOverridden = true` | CA-36.02 |
| override de ayer | rutina permanente, `isOverridden = false` | CA-36.04 |
| sin override, permanente `null` | `null` → día de descanso | CA-36.01 |
| override de hoy, permanente `null` | rutina del override | CA-36.07 |
| override de hoy == permanente | misma rutina, ruta idéntica | CA-36.08 |

CA-36.08 sale gratis: la regla no compara, sustituye. Reasignar a la rutina que ya correspondía produce el mismo `routineId` y el mismo `routineVersionId`, y el único efecto observable es el banner "solo por hoy".

#### D4 — `getNextSessionInfo` se convierte en `getTodaySession` y devuelve un estado, no un anulable

Hoy `Flow<NextSession?>` colapsa tres situaciones distintas en `null`: no hay `rotation_state`, no hay rutinas, la versión vigente está vacía. Inicio no puede distinguirlas y no muestra nada. Con el domingo, `null` pasa a ser un estado **presentable** y la ambigüedad deja de ser tolerable.

`TodaySession` lleva el día de hoy, si es día de descanso, si la propuesta viene de una reasignación, el día dueño de la rutina propuesta y la propuesta misma. Inicio decide con eso entre tarjeta de sesión, tarjeta de descanso y nada.

Se renombra el use case a `GetTodaySessionUseCase` y se borra `GetNextSessionInfoUseCase` con su test: mantener el nombre `NextSessionInfo` sobre un tipo que ya no describe "la siguiente" sino "la de hoy" dejaría la mentira en la capa que más se lee.

`NextSession` se conserva tal cual — es lo que `startSession` y el preview necesitan y no gana campos.

#### D5 — Con descarga activa, el día resuelve la rutina y la descarga resuelve la versión

`getNextSessionInfo` indexa hoy las versiones congeladas por `microcyclePosition` (`SessionRepositoryImpl.kt:139`). Bajo determinación por día eso se sustituye por una búsqueda directa: día → `routine_id` → `deload_frozen_version` de **esa** rutina.

Es más estricto que lo anterior y no toca HU-14: el conteo de sesiones de descarga (`countDeloadSessions == frozenCount`), el cierre del ciclo y el reinicio de cargas siguen intactos porque dependen del número de sesiones cerradas, no de qué rutina tocaba.

Efecto colateral aceptado y documentado: durante una descarga, el domingo no propone sesión, así que un ciclo de descarga de 6 sesiones ocupa una semana de calendario en lugar de 6 días corridos. Antes, la rotación llenaba el domingo.

#### D6 — El selector ofrece todas las rutinas ejecutables, no solo las que tienen día

El wireframe dibuja los 6 días. Ofrecer *toda* rutina con versión vigente no vacía es un superconjunto que dibuja igual (las 6 del seed tienen día y muestran su etiqueta) y es lo que evita que una rutina creada por el ejecutante quede inalcanzable — ver "La consecuencia que ninguna CA cubre".

El elemento del día de hoy se marca `actual` y **no se excluye**: seleccionarlo es CA-36.08.

#### D7 — La acción no se deshabilita: no existe cuando hay sesión activa

CA-36.06 se cumple en dos capas, y la de arriba es estructural:

- `HomeUiState.showNextSessionCard` ya es `activeSession == null && …`. La tarjeta que aloja la acción **no se compone** con sesión en curso; Inicio muestra `ResumeSessionCard`, que no la lleva. `ActiveSessionScreen` no la gana.
- `SetTemporaryRoutineUseCase` valida `hasActiveSession()` y lanza. Es la red de la ruta de datos, no la del botón, y es lo que el test unitario ejerce.

No se añade estado "deshabilitado": un control visible que rechaza es peor que un control ausente cuando la razón de la ausencia ya está en pantalla.

#### D8 — El preview pasa a observar la sesión de hoy; los argumentos de navegación quedan como valor inicial

`SessionPreviewViewModel` recibe hoy `routineVersionId`, `routineName` y `versionNumber` por la ruta. Si la reasignación ocurre dentro del preview, esos tres argumentos quedan obsoletos y no hay forma de re-navegar sin un salto visible.

`onNavigateToPreview` tiene **un único invocador** en todo el proyecto: `HomeScreen`, desde la tarjeta de la sesión propuesta (`TensionNavHost.kt:144`). El preview siempre muestra la propuesta de hoy. Por tanto el ViewModel puede observar `GetTodaySessionUseCase()` y usar los argumentos de la ruta solo como valor inicial, para que la primera composición no parpadee. La ruta no cambia de firma.

#### D9 — Los nombres del seed pierden el día; el enfoque se queda en el nombre

CA-36.01: *"el nombre de la rutina deja de depender del día"*. Se aplica el nombre del wireframe (`LUN  Push · Foco Deltoides Lat. y Med.`):

| id | Antes | Después |
|---|---|---|
| 1 | `Lunes: Pecho y Hombro (Push - Foco Deltoides Lateral y Medio)` | `Push — Foco Deltoides Lateral y Medio` |
| 2 | `Martes: Espalda, Bíceps y Abdomen (Pull - Foco Dorsal Ancho)` | `Pull — Foco Dorsal Ancho` |
| 3 | `Miércoles: Pierna (Lower - Foco Cuádriceps)` | `Lower — Foco Cuádriceps` |
| 4 | `Jueves: Pecho y Tríceps (Push - Foco Tríceps)` | `Push — Foco Tríceps` |
| 5 | `Viernes: Espalda, Bíceps y Abdomen (Pull - Foco Trapecios y Espalda Media)` | `Pull — Foco Trapecios y Espalda Media` |
| 6 | `Sábado: Pierna (Lower - Foco Isquiotibiales y Glúteo)` | `Lower — Foco Isquiotibiales y Glúteo` |

**No se añade columna `focus` a `routine`.** El enfoque es parte del nombre, como hasta ahora; extraerlo a su propio campo obligaría a un editor de enfoque que ninguna CA pide. Los seis nombres siguen siendo distintos entre sí: el índice único sobre `routine.name` se respeta.

La enumeración de grupos musculares (`Pecho y Hombro`, `Espalda, Bíceps y Abdomen`) desaparece del nombre siguiendo el wireframe. Es información derivable del contenido de la versión y visible en el detalle del plan.

#### D10 — Esquema v17 sin migración, por decisión de producto

La historia declara *"Beta sin migración: la base de datos se reinicia"*. El PO precisó el mecanismo (2026-09-01): **el reinicio lo hace el ejecutante desinstalando y reinstalando; la app no lo hace por sí sola** y debe comportarse como si fuera su primera salida a producción.

En consecuencia:

- Se sube `TensionDatabase.version` a 17.
- **No se escribe `MIGRATION_16_17`** ni se registra nada nuevo en `DatabaseModule.addMigrations`.
- **No se añade `fallbackToDestructiveMigration()`**: la app no borra datos por su cuenta. Una instalación en v16 fallará al abrir con la excepción de Room, que es exactamente la señal de "reinstala".
- Las migraciones históricas `MIGRATION_6_7` … `MIGRATION_15_16` **se conservan sin tocar**. Describen el esquema de su época; retirarlas es una limpieza que esta historia no pide.

Es una excepción documentada a RNF19, limitada a esta historia, tal como la historia la declara.

#### D11 — El backup sube de versión porque gana tablas, no columnas

Las HU-30 a HU-34 añadieron y quitaron **columnas** sin mover `BackupRepositoryImpl.SCHEMA_VERSION` (9). `week_day` es distinto: es una **tabla** con FK a `routine`.

La importación borra e reinserta `routine` en el orden de `TABLE_ORDER_DELETE`/`INSERT`. Si `week_day` no está en la lista, el borrado de `routine` dispara el `SET NULL` de D1 y **la relación día → rutina se pierde en cada restauración**, sin que nada la reponga. La tabla tiene que estar, e insertarse después de `routine`.

`daily_routine_override` entra por la misma razón (FK `CASCADE`), aunque su contenido caduque en horas.

Con dos tablas nuevas el formato deja de ser compatible: `SCHEMA_VERSION` pasa a **10**. `LEGACY_SCHEMA_VERSION` (8) se conserva. Los respaldos en formato 9 quedan fuera — consistente con D10: no hay instalación previa que preservar.

#### D12 — Las etiquetas de día son de presentación, no de datos

`week_day` guarda `id` y `code`; ni "Lunes" ni "LUN" viven en la base. Las etiquetas salen de `strings.xml` a través de `weekDayName()` / `weekDayShortName()`, replicando `ui/alerts/AlertTypeLabel.kt` — un único sitio para que Inicio, el selector y la pestaña Plan no puedan divergir.

`muscle_zone` y `equipment_type` sí guardan nombre porque son catálogos que el ejecutante extiende. `week_day` es un dominio cerrado de 7 valores.

---

### Tareas de Implementación

#### Fase 1 — Esquema y semilla (`DB-01`)

- [x] **T1: Crear `WeekDayEntity`** — `data/local/entity/WeekDayEntity.kt` (Base: `RoutineCurrentVersionEntity.kt`)

  Tabla `week_day`. `id` `Int` `@PrimaryKey` (sin autogenerar, 1..7 ISO-8601), `code` `String`, `routine_id` `Long?` con FK a `RoutineEntity` `ON DELETE SET NULL`. Índice único sobre `code`, índice sobre `routine_id`. (D1)

- [x] **T2: Crear `DailyRoutineOverrideEntity`** — `data/local/entity/DailyRoutineOverrideEntity.kt` (Base: `RotationStateEntity.kt`)

  Tabla `daily_routine_override`, fila única `id = 1`. `date` `String` no nulo (ISO `YYYY-MM-DD`), `routine_id` `Long` no nulo con FK a `RoutineEntity` `ON DELETE CASCADE`, índice sobre `routine_id`. (D2)

- [x] **T3: Crear `WeekDayDao`** — `data/local/dao/WeekDayDao.kt` (Base: `RoutineCurrentVersionDao.kt`)

  `getAll(): Flow<List<WeekDayEntity>>` ordenado por `id`; `getAllOnce()`; `getByIdFlow(id: Int): Flow<WeekDayEntity?>`; `getByIdOnce(id: Int)`; `update(entity)`.

- [x] **T4: Crear `DailyRoutineOverrideDao`** — `data/local/dao/DailyRoutineOverrideDao.kt` (Base: `RotationStateDao.kt`)

  `getOverride(): Flow<DailyRoutineOverrideEntity?>` (`WHERE id = 1`); `getOverrideOnce()`; `upsert(entity)` con `OnConflictStrategy.REPLACE`; `clear()` (`DELETE FROM daily_routine_override`).

- [x] **T5: Registrar entidades y DAOs, y subir a v17** — `data/local/database/TensionDatabase.kt`

  `WeekDayEntity` y `DailyRoutineOverrideEntity` en `entities`, `weekDayDao()` y `dailyRoutineOverrideDao()` como abstractos, `version = 17`. **Sin `MIGRATION_16_17`.** (D10)

- [x] **T6: Proveer los DAOs nuevos** — `di/DatabaseModule.kt`

  Dos `@Provides` siguiendo el patrón del archivo. `addMigrations` **no se toca**. (D10)

- [x] **T7: Renombrar las rutinas del seed** — `data/local/seed/DefaultPlan.kt` (D9)

  Los 6 valores de `ROUTINES` según la tabla de D9. Actualizar también los comentarios de sección de `ASSIGNMENTS`, que hoy nombran el día (`// ===== Rutina 1 — Lunes, Push …`): el día pasa a ser dato, no comentario.

- [x] **T8: Crear `DefaultWeekDays`** — `data/local/seed/DefaultWeekDays.kt` (Base: `DefaultPlan.kt`)

  Los 7 días como datos: `WeekDay` del dominio + `routineId: Long?`. Lunes→1 … Sábado→6, Domingo→`null`. KDoc en español explicando que el domingo es día registrado sin rutina, no ausencia. (D1)

- [x] **T9: Crear `WeekDaySeeder`** — `data/local/seed/WeekDaySeeder.kt` (Base: `PlanSeeder.kt`)

  Inserta las 7 filas con `ContentValues` y `CONFLICT_REPLACE`, leyendo de `DefaultWeekDays`.

- [x] **T10: Sembrar los días tras el plan** — `data/local/seed/PrepopulateFacade.kt`

  `WeekDaySeeder.seed(db)` después de `PlanSeeder.seed(db)`, dentro de la misma transacción: la FK a `routine` exige que las rutinas existan.

#### Fase 2 — Dominio (`DOM-01`)

- [x] **T11: Crear el dominio cerrado `WeekDay`** — `domain/model/WeekDay.kt` (Base: `WeightUnit.kt`)

  `enum class WeekDay(val isoNumber: Int, val code: String)` con `MONDAY(1)` … `SUNDAY(7)`, `fromIso(Int)` y `fromCode(String)`. (D1)

- [x] **T12: Crear `DailyRoutineOverride`** — `domain/model/DailyRoutineOverride.kt`

  `date: String`, `routineId: Long`.

- [x] **T13: Crear `WeekDayRoutine`** — `domain/model/WeekDayRoutine.kt`

  `weekDay: WeekDay`, `routineId: Long?`, `routineName: String?`. Modelo de la relación permanente; alimenta la pestaña Plan.

- [x] **T14: Crear `TodaySession`** — `domain/model/TodaySession.kt` (D4)

  `weekDay: WeekDay`, `session: NextSession?`, `isRestDay: Boolean`, `isTemporaryOverride: Boolean`, `overriddenFromWeekDay: WeekDay?`. Propiedades derivadas `showSessionCard` y `showRestDayCard` para que Inicio no rehaga la lógica.

- [x] **T15: Crear `ReassignableRoutine`** — `domain/model/ReassignableRoutine.kt` (D6)

  `routineId`, `routineName`, `routineVersionId`, `weekDay: WeekDay?`, `isTodaysRoutine: Boolean`.

- [x] **T16: Crear la regla pura `DailyRoutineRule`** — `domain/rules/DailyRoutineRule.kt` (Base: `PrefilledLoadRule.kt`) (D3)

  `data class Resolution(val routineId: Long?, val isOverridden: Boolean)` y `resolve(today: String, permanentRoutineId: Long?, override: DailyRoutineOverride?): Resolution`. Kotlin puro, sin imports de Android ni Room (RNF29). KDoc con la tabla de D3.

- [x] **T17: Crear `GetTodaySessionUseCase`** — `domain/usecase/session/GetTodaySessionUseCase.kt` (Base: `GetNextSessionInfoUseCase.kt`) (D4)

  Delega en `SessionRepository.getTodaySession(): Flow<TodaySession>`.

- [x] **T18: Borrar `GetNextSessionInfoUseCase`** — `domain/usecase/session/GetNextSessionInfoUseCase.kt` (D4)

- [x] **T19: Crear `GetReassignableRoutinesUseCase`** — `domain/usecase/session/GetReassignableRoutinesUseCase.kt` (D6)

  Delega en `WeekDayRepository.getReassignableRoutines(): Flow<List<ReassignableRoutine>>`.

- [x] **T20: Crear `SetTemporaryRoutineUseCase`** — `domain/usecase/session/SetTemporaryRoutineUseCase.kt` (Base: `ActivateDeloadUseCase.kt`) (D7)

  `suspend operator fun invoke(routineId: Long)`. Rechaza con `IllegalStateException` si `sessionRepository.hasActiveSession()`; en otro caso persiste el override para hoy vía `WeekDayRepository`. Es la validación de CA-36.06 en la ruta de datos.

- [x] **T21: Crear `ClearTemporaryRoutineUseCase`** — `domain/usecase/session/ClearTemporaryRoutineUseCase.kt`

  Borra el override. Respalda la acción "Deshacer".

- [x] **T22: Crear `GetWeekDayPlanUseCase`** — `domain/usecase/plan/GetWeekDayPlanUseCase.kt` (Base: `GetTrainingPlanUseCase.kt`)

  Delega en `WeekDayRepository.getWeekDayPlan(): Flow<List<WeekDayRoutine>>`, los 7 días ordenados.

- [x] **T23: Declarar `WeekDayRepository`** — `domain/repository/WeekDayRepository.kt` (Base: `RoutineRepository.kt`)

  `getWeekDayPlan()`, `getReassignableRoutines()`, `getTodayOverride(): Flow<DailyRoutineOverride?>`, `setTodayOverride(routineId: Long)`, `clearTodayOverride()`.

- [x] **T24: Actualizar el contrato de `SessionRepository`** — `domain/repository/SessionRepository.kt`

  `getNextSessionInfo(): Flow<NextSession?>` → `getTodaySession(): Flow<TodaySession>`. Añadir `suspend fun hasActiveSession(): Boolean` si no existe equivalente utilizable por T20 (hoy existe `hasActiveSessionForVersion`, que no sirve: la reasignación no conoce versión).

#### Fase 3 — Datos (`DAT-01`)

- [x] **T25: Implementar `WeekDayRepositoryImpl`** — `data/repository/WeekDayRepositoryImpl.kt` (Base: `RoutineRepositoryImpl.kt`)

  `getWeekDayPlan` combina `weekDayDao.getAll()` con `routineDao.getAll()` para resolver el nombre. `getReassignableRoutines` cruza rutinas, `routine_current_version`, conteo de ejercicios de la versión vigente y `week_day` (D6). `setTodayOverride` hace `upsert` con `LocalDate.now().toString()`. `clearTodayOverride` borra.

- [x] **T26: Registrar el binding** — `di/RepositoryModule.kt`

  `WeekDayRepository` → `WeekDayRepositoryImpl`, patrón del archivo.

- [x] **T27: Reescribir la determinación de sesión** — `data/repository/SessionRepositoryImpl.kt` (`getNextSessionInfo` → `getTodaySession`, `:94-171`) (D4, D5)

  Nueva cadena: `weekDayDao.getByIdFlow(LocalDate.now().dayOfWeek.value)` + `dailyRoutineOverrideDao.getOverride()` → `DailyRoutineRule.resolve` → `routineId`.
  - `routineId == null` → `TodaySession(isRestDay = true)`.
  - Con descarga activa: buscar en `deloadFrozenVersionDao.getByDeloadId` la fila de **esa** rutina; sin ella, sin propuesta. `resolveRoutineIndex` sale de esta ruta (D5).
  - Sin descarga: `routine_current_version` de esa rutina → `routine_version` → validar que tenga ejercicios.
  - Poblar `isTemporaryOverride` y `overriddenFromWeekDay` (el día dueño de la rutina propuesta, si otro día la tiene).

  Inyectar `weekDayDao` y `dailyRoutineOverrideDao` en el constructor.

- [x] **T28: Añadir `hasActiveSession`** — `data/repository/SessionRepositoryImpl.kt`

  `sessionDao.getActiveSession().first() != null`, si T24 lo requiere.

- [x] **T29: Verificar que `closeSession` no cambia** — `data/repository/SessionRepositoryImpl.kt:432-561`

  Tarea de verificación, no de edición: confirmar que el bloque de rotación no lee el override ni el día, y que `advanceRotation` recibe el mismo `routineCount` que hoy. Es la garantía de CA-36.03 en el código.

- [x] **T30: Incorporar las tablas nuevas al respaldo** — `data/repository/BackupRepositoryImpl.kt` (D11)

  `week_day` y `daily_routine_override` en `TABLE_ORDER_INSERT` **después de `routine`** (`TABLE_ORDER_DELETE` se deriva por `reversed()`). `SCHEMA_VERSION` de 9 a 10. `LEGACY_TABLE_ORDER` y `LEGACY_SCHEMA_VERSION` sin cambios.

#### Fase 4 — Interfaz (`UI-01`, `UI-02`)

- [x] **T31: Crear las etiquetas de día** — `ui/components/WeekDayLabel.kt` (Base: `ui/alerts/AlertTypeLabel.kt`) (D12)

  `@Composable fun weekDayName(WeekDay): String` y `weekDayShortName(WeekDay): String` sobre `stringResource`.

- [x] **T32: Añadir las cadenas** — `res/values/strings.xml`

  7 nombres + 7 abreviaturas; `home_today_label` ("Hoy te toca"), `home_reassign_action` ("Hacer otra rutina hoy"), `home_reassign_undo` ("Deshacer"), `home_reassign_notice` ("Solo por hoy. Mañana vuelve tu plan normal."), `home_rest_day_title` ("Día de descanso"), `home_rest_day_body`, `home_rest_day_action` ("Entrenar de todas formas"), `reassign_dialog_title`, `reassign_dialog_subtitle` ("Solo para la sesión de hoy. Tu plan no cambia."), `reassign_dialog_current` ("actual"), `reassign_dialog_confirm`, `reassign_dialog_cancel`, `plan_rest_day_row` ("— descanso"), `session_day_routine_format` (`%1$s — %2$s`).

- [x] **T33: Crear el selector de reasignación** — `ui/components/ReassignRoutineDialog.kt` (Base: `ui/catalog/components/ProgressionDifficultySelector.kt`)

  `AlertDialog` con lista de radios, etiqueta de día cuando la rutina tiene uno, marca `actual` en la de hoy, área táctil ≥ 48 dp, `Cancelar` / `Confirmar`. Compartido por Inicio y Preview (CA-36.05: sin ruta nueva). (D6)

- [x] **T34: Extender el estado de Inicio** — `ui/home/HomeUiState.kt`

  `todaySession: TodaySession?`, `reassignOptions: List<ReassignableRoutine>`, `isReassignDialogOpen: Boolean`. `showNextSessionCard` pasa a `activeSession == null && todaySession?.session != null`; añadir `showRestDayCard` y `canReassign` (`activeSession == null`). `nextSession` se deriva de `todaySession`.

- [x] **T35: Extender el ViewModel de Inicio** — `ui/home/HomeViewModel.kt`

  Sustituir `GetNextSessionInfoUseCase` por `GetTodaySessionUseCase` en el `combine`. Inyectar `GetReassignableRoutinesUseCase`, `SetTemporaryRoutineUseCase` y `ClearTemporaryRoutineUseCase`. Añadir `openReassignDialog()`, `dismissReassignDialog()`, `confirmReassign(routineId)`, `undoReassign()`, con errores a `errorMessage` como hace `startSession`.

- [x] **T36: Rehacer la tarjeta de sesión propuesta** — `ui/home/HomeScreen.kt` (CA-36.05)

  `NextSessionCard` conserva forma, color y jerarquía. Cambia: encabezado "Hoy te toca", nombre como `Día — Rutina` con `EntityNameText` (HU-28), aviso ⓘ cuando `isTemporaryOverride`, y bajo el botón las acciones `Hacer otra rutina hoy` / `Deshacer` como `TextButton`. Sin rediseño.

- [x] **T37: Añadir la tarjeta de día de descanso** — `ui/home/HomeScreen.kt` (CA-36.07)

  `RestDayCard` con el título, el cuerpo explicando que el plan no asigna rutina a ese día, y `Entrenar de todas formas` abriendo el mismo selector de T33. Se compone cuando `showRestDayCard`.

- [x] **T38: Montar el selector en Inicio** — `ui/home/HomeScreen.kt`

  `ReassignRoutineDialog` cuando `isReassignDialogOpen`, cableado a los métodos de T35.

- [x] **T39: Extender el estado del preview** — `ui/preview/SessionPreviewUiState.kt`

  `weekDay: WeekDay?`, `isTemporaryOverride: Boolean`, `reassignOptions`, `isReassignDialogOpen`, `canReassign`.

- [x] **T40: Reorientar el ViewModel del preview** — `ui/preview/SessionPreviewViewModel.kt` (D8)

  Observar `GetTodaySessionUseCase()` y derivar de ahí `routineVersionId`, `routineName` y `versionNumber`; los argumentos de `SavedStateHandle` quedan como valor inicial del `MutableStateFlow`. Recolectar `getSessionPreviewUseCase` con `flatMapLatest` sobre el `routineVersionId` vigente para que la reasignación recargue la lista. Añadir los mismos métodos de reasignación que T35.

- [x] **T41: Montar la acción en el preview** — `ui/preview/SessionPreviewScreen.kt` (CA-36.05)

  Encabezado `Día — Rutina`, aviso ⓘ cuando hay reasignación, acción y `ReassignRoutineDialog`. Sin cambios de ruta.

- [x] **T42: Presentar la relación día → rutina en la pestaña Plan** — `ui/catalog/TrainingPlanUiState.kt`, `TrainingPlanViewModel.kt`, `TrainingPlanScreen.kt` (CA-36.01)

  `RoutineSectionItem` gana `weekDay: WeekDay?`; el ViewModel inyecta `GetWeekDayPlanUseCase` y lo combina con el plan. La pantalla muestra la abreviatura del día como prefijo (`LUN`, `MAR`, …), vacío cuando la rutina no tiene día, y añade una fila final no navegable `DOM — descanso` cuando el domingo no tiene rutina.

#### Fase 5 — Tests unitarios (JVM, sin emulador)

- [x] **T43: Crear `DailyRoutineRuleTest`** — `test/…/domain/rules/DailyRoutineRuleTest.kt` (Base: `PrefilledLoadRuleTest.kt`)

  Los cinco casos de la tabla de D3, uno por AC: override de hoy (CA-36.02), override caducado (CA-36.04), permanente nulo sin override (CA-36.01), permanente nulo con override (CA-36.07), override igual al permanente (CA-36.08).

- [x] **T44: Crear `GetTodaySessionUseCaseTest`** — `test/…/domain/usecase/session/GetTodaySessionUseCaseTest.kt` (Base: `GetNextSessionInfoUseCaseTest.kt`)

  Propuesta normal, día de descanso, propuesta con reasignación.

- [x] **T45: Borrar `GetNextSessionInfoUseCaseTest`** — `test/…/domain/usecase/session/GetNextSessionInfoUseCaseTest.kt` (D4)

- [x] **T46: Crear `SetTemporaryRoutineUseCaseTest`** — `test/…/domain/usecase/session/SetTemporaryRoutineUseCaseTest.kt` (Base: `StartSessionUseCaseTest.kt`)

  Persiste sin sesión activa; **lanza con sesión activa** (CA-36.06); no persiste cuando lanza.

- [x] **T47: Crear `ClearTemporaryRoutineUseCaseTest`** — `test/…/domain/usecase/session/ClearTemporaryRoutineUseCaseTest.kt`

- [x] **T48: Crear `GetReassignableRoutinesUseCaseTest`** — `test/…/domain/usecase/session/GetReassignableRoutinesUseCaseTest.kt`

  Incluye la rutina de hoy marcada `isTodaysRoutine`; incluye una rutina sin día (D6).

- [x] **T49: Crear `GetWeekDayPlanUseCaseTest`** — `test/…/domain/usecase/plan/GetWeekDayPlanUseCaseTest.kt`

  7 días en orden; domingo con `routineId` nulo.

- [x] **T50: Crear `DefaultWeekDaysTest`** — `test/…/data/local/seed/DefaultWeekDaysTest.kt` (Base: `DefaultPlanTest.kt`)

  CA-36.01: exactamente 7 días, uno por valor de `WeekDay`, sin repetir; lunes a sábado apuntan a las rutinas 1..6; **domingo con `routineId` nulo**; toda rutina referenciada existe en `DefaultPlan.ROUTINES`.

- [x] **T51: Actualizar `DefaultPlanTest`** — `test/…/data/local/seed/DefaultPlanTest.kt` (D9)

  `routine names reflect their focus`: los 6 nombres nuevos. Añadir aserción de que **ningún nombre contiene un día de la semana** — es la que impide que CA-36.01 se revierta por descuido en un rename futuro.

- [x] **T52: Reforzar `RotationResolverTest`** — `test/…/domain/model/RotationResolverTest.kt` (CA-36.03)

  Añadir un test que fije por contrato que `advanceRotation` depende solo de `RotationState` y `routineCount`, y que su resultado es idéntico se haya ejecutado la rutina del día o una reasignada. Es la prueba de mesa de "la rotación no se altera".

- [x] **T53: Crear `HomeViewModelTest`** — `test/…/ui/home/HomeViewModelTest.kt` (Base: `ui/session/RegisterSetViewModelTest.kt`)

  Estado de descanso; estado con reasignación activa; `canReassign` falso con sesión activa (CA-36.06); `confirmReassign` cierra el diálogo y publica el error cuando el use case lanza.

- [x] **T54: Ajustar los tests de respaldo** — `test/…/data/repository/BackupRepositoryImplTest.kt`, `test/…/domain/usecase/backup/*Test.kt` (D11)

  Alinear con `SCHEMA_VERSION = 10` y con las dos tablas nuevas donde se aserte el listado o la versión.

#### Fase 6 — Documentación arquitectónica (CA-36.10)

- [x] **T55: Documentar las entidades nuevas** — `docs/architecture/domain_and_state_model.md`

  §2: bloques `week_day` y `daily_routine_override` con diccionario inline (D1, D2). §3: cuatro filas nuevas en la matriz de relaciones (`routine` `1:N` `week_day` SET NULL; `routine` `1:N` `daily_routine_override` CASCADE). §4: dominio cerrado `WeekDay` con los 7 valores y el domingo anotado como día sin rutina. §5: nota en el ciclo de vida de `session` sobre la resolución día → rutina previa al nacimiento. §6.1: las 7 filas de `week_day` en las condiciones de inicialización, con el domingo nulo. Encabezado: **versión de esquema 17**, anotando que v17 no trae migración por la excepción de D10.

- [x] **T56: Documentar el componente nuevo** — `docs/architecture/architecture_blueprint.md`

  §3 `DOM-01`: `DailyRoutineRule`, `WeekDay`, `TodaySession`, `WeekDayRepository` y los cuatro use cases. §3 `DAT-01`: `WeekDayDao`, `DailyRoutineOverrideDao`, `WeekDayRepositoryImpl`, `WeekDaySeeder`. §3 `UI-01`/`UI-02`: `ReassignRoutineDialog`, `WeekDayLabel`, tarjeta de descanso. §4: fila de trazabilidad de HU-36. §5: **ADR nuevo** con la decisión de D10 (v17 sin migración, reinicio a cargo del ejecutante) y la frontera día / override / rotación de D4-D5.

- [x] **T57: Documentar la acción de reasignación** — `docs/architecture/interfaces_contract.md`

  §2.2 `Flujo B — Inicio`: nuevos triggers `B1-T3: Reasignar Temporalmente la Rutina de Hoy` y `B1-T4: Deshacer Reasignación Temporal`, con precondiciones (sin sesión activa), efectos (fila de `daily_routine_override`) y postcondiciones (la rotación no se altera). Actualizar `B1-T1` para que su determinación describa día → rutina con override en lugar de posición del microciclo. §3.2: código de error para "reasignación con sesión en curso".

#### Fase 7 — Cierre de la historia

- [x] **T58: Registrar el desarrollo** — `docs/domain/stories/HU-36-dias-semana-reasignables/dev-record.md` (nuevo, patrón de HU-34)

- [x] **T59: Actualizar fases y métricas** — `index.md` (Refinamiento y Desarrollo a ✅, métricas de tiempo) y `cambios.md` (entradas de refinamiento y desarrollo)

---

### Riesgos y observaciones

**El riesgo declarado por la historia se resolvió eligiendo, no conciliando.** El texto pedía que la rotación no se tocara y el wireframe pedía un domingo sin rutina; ambas cosas juntas no son realizables. La frontera quedó en D4 y D5: el día decide **qué rutina**, la rotación decide **cuándo cierra el microciclo**. `advanceRotation` no cambia de firma ni de comportamiento y T52 lo fija por contrato. Si algo de HU-14 se degrada, el sitio donde mirar es T27, no `closeSession`.

**Una rutina sin día asignado deja de proponerse sola, y esta historia no da forma de asignarle día.** Es la consecuencia directa de la decisión y no la cubre ninguna CA — la reasignación permanente está explícitamente fuera de alcance (regla de negocio 7). D6 evita que quede inalcanzable (el selector la ofrece), pero no que quede fuera del plan automático. Con el seed de 6 rutinas y 6 días esto no se manifiesta; se manifiesta en cuanto el ejecutante crea la séptima. **Se levanta como necesidad de una historia futura: editar la relación permanente día → rutina.**

**`SessionRepositoryImpl` no tiene tests unitarios y T27 es el cambio más consecuente de la historia.** Depende de Room, así que la cadena real día → override → rutina → versión (incluida la variante con descarga de D5) solo se ejerce a mano. Lo que sí queda probado es la regla que decide (T43) y el contrato del use case (T44); lo que no, es el cableado de los seis DAOs. Los puntos 1 a 7 de la validación manual existen por esto.

**El domingo cambia la duración en calendario de un ciclo de descarga.** Antes la rotación llenaba los 7 días; ahora el domingo no propone sesión. Un ciclo de 6 sesiones pasa a ocupar una semana en lugar de 6 días corridos. No afecta el conteo ni el cierre del ciclo (D5), pero sí lo que el ejecutante ve, y ninguna CA lo menciona.

**La adherencia semanal se mide contra una frecuencia objetivo que el día no conoce.** `AdherenceRule` y la alerta `LOW_ADHERENCE` comparan sesiones cerradas contra `profile.target_weekly_frequency`. Con determinación por día, el máximo alcanzable sin reasignar es 6 (el domingo no propone). Si la frecuencia objetivo está en 7, la alerta pasa a ser inevitable. No es objeto de esta historia y no se toca; se levanta como interacción no verificada con HU-18.

**El formato de respaldo rompe compatibilidad con la versión 9 (D11).** Es la consecuencia de que `week_day` sea una tabla y no una columna: sin ella en la lista, cada restauración perdería la relación día → rutina en silencio, lo que es peor. Se acepta porque D10 ya declara que no hay instalación previa que preservar. Un respaldo exportado antes de esta historia **no se podrá restaurar**.

**No hay test que ejerza el arranque en v17.** `MigrationV6ToV7Test` y `MigrationV7ToV8Test` no usan `MigrationTestHelper`: abren una base en memoria con `PrepopulateCallback` y verifican la **semilla**, no el camino de migración. Como D10 elimina la migración, no hay camino que probar; lo que sí hay que verificar es que la semilla nueva (T9, T10) deja las 7 filas correctas, y eso es instalación fresca — punto 1 de la validación manual.

**El wireframe muestra "5 ejercicios · ~55 min" y la implementación no calcula duración.** `NextSessionCard` no muestra hoy ni conteo de ejercicios ni estimación de tiempo, y ninguna CA los pide. El prototipo está declarado "pendiente de validación con Diseño": se respeta la implementación existente y no se añaden. Si Diseño valida el wireframe, es una historia aparte.

**El wireframe también muestra "Microciclo 14 · posición 4 de 6" y la implementación muestra solo el conteo.** `ProgressSection` pinta `microcycleCount` y la etiqueta "microciclos"; la posición no se presenta. Se deja como está por CA-36.05 (presentación equivalente a la actual). La posición sigue existiendo en `rotation_state` y sigue avanzando.

---

### Validación manual (no automatizable)

Los tests JVM cubren la regla de resolución, los use cases y las fixtures de semilla; Room cubre el SQL en compilación. Lo que sigue verifica el cableado real sobre la base de datos y la pantalla — en particular la determinación de T27, que ningún test del proyecto ejerce.

1. **Instalación fresca (D10, T9, T10)** — Desinstalar la app e instalar el build nuevo. Debe abrir sin excepción de Room, con el plan sembrado y los nombres de rutina **sin día** (D9). Inspeccionar `week_day`: 7 filas, lunes a sábado con `routine_id` 1..6, domingo con `routine_id` nulo.
2. **Instalación previa en v16 (D10)** — Sobre una base v16, instalar el build nuevo **sin desinstalar**. Debe fallar al abrir con la excepción de Room. Es el comportamiento acordado, no un defecto: la señal de "reinstala".
3. **CA-36.01 (Inicio y Plan)** — En un día laborable, Inicio debe mostrar `Día — Rutina` con el nombre sin día embebido. En la pestaña Plan, cada rutina debe llevar su abreviatura de día y debe existir la fila `DOM — descanso`.
4. **CA-36.02** — Pulsar "Hacer otra rutina hoy", elegir la rutina de otro día y confirmar. La tarjeta debe pasar a esa rutina, con el aviso "Solo por hoy". Iniciar la sesión y verificar que los ejercicios son los de la rutina reasignada.
5. **CA-36.03 (el riesgo principal)** — Anotar posición y conteo de microciclo antes de empezar. Ejecutar y cerrar una sesión **reasignada**. Verificar que la posición avanzó exactamente en uno y que el conteo se comportó igual que en un cierre normal. Repetir sin reasignar y comparar: los dos avances deben ser idénticos.
6. **CA-36.04** — Con una reasignación activa, cambiar la fecha del dispositivo al día siguiente y volver a Inicio. Debe proponer la rutina del nuevo día por relación permanente, sin aviso de reasignación. Confirmar que la fila de `daily_routine_override` ya no se honra.
7. **CA-36.06** — Iniciar una sesión y volver a Inicio: debe verse la tarjeta de reanudar, **sin** acción de reasignación. Entrar a la sesión activa: tampoco debe haberla.
8. **CA-36.07 (domingo)** — Poner la fecha en domingo. Inicio debe mostrar la tarjeta de descanso con "Entrenar de todas formas". Usarla, elegir una rutina, confirmar y ejecutar la sesión completa. Al cerrar, la rotación debe avanzar con normalidad y `week_day` del domingo debe seguir con `routine_id` nulo.
9. **CA-36.08** — Abrir el selector y elegir la rutina que ya correspondía a hoy. Debe confirmar sin error y proponer exactamente la misma sesión.
10. **CA-36.09** — Cerrar una sesión reasignada y abrir el historial, el detalle de esa sesión y las métricas. Las tres deben mostrar la rutina **ejecutada**, no la del día. Es la CA que no cambia código: verifica que sigue siendo cierta.
11. **Descarga activa (D5)** — Activar descarga y recorrer varios días. Cada día debe proponer la versión **congelada** de la rutina de ese día. Reasignar durante la descarga y verificar que se propone la versión congelada de la rutina reasignada. Completar el ciclo y confirmar que se cierra al número de sesiones esperado y que las cargas se reinician.
12. **Rutina sin día (D6)** — Crear una rutina nueva con ejercicios en su versión vigente. Debe aparecer en el selector de reasignación y ser ejecutable. Confirmar el límite conocido: no se propone sola ningún día.
13. **Borrado de rutina (D1)** — Borrar una rutina que tenga día asignado. La app no debe fallar; su día debe quedar sin rutina y comportarse como el domingo (tarjeta de descanso ese día).
14. **Respaldo — ida y vuelta en v17 (D11)** — Exportar un respaldo con una reasignación activa y restaurarlo. El JSON debe contener `week_day` y `daily_routine_override`; tras restaurar, la relación día → rutina debe estar intacta y la pestaña Plan debe mostrarla igual que antes de exportar.
15. **Respaldo — archivo en formato 9 (D11)** — Intentar restaurar un respaldo exportado antes de esta historia. Debe rechazarse con el mensaje de versión incompatible, no fallar con una excepción de SQLite.
16. **Preview (D8, CA-36.05)** — Abrir el preview desde la tarjeta de Inicio, reasignar desde ahí y verificar que la lista de ejercicios se recarga **en la misma pantalla**, sin salto de navegación ni pantalla intermedia.
