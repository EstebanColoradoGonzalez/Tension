# Refinamiento Técnico

<!-- SECCIÓN AGREGADA POR: Workflow refinamiento-tecnico -->
<!-- ETAPA: Refinamiento Técnico -->
<!-- RESPONSABLE: Developer -->
<!-- BASE: Análisis Arquitectónico (Arquitecto) - Ver sección arriba -->
<!-- FECHA: 2026-02-14 -->
<!-- ESTADO: Listo para Desarrollo -->
<!-- AUDITORÍA: Completada 2026-02-14 — SEGUNDA AUDITORÍA EXHAUSTIVA: cruce archivo-por-archivo contra TODA la documentación + 32 HUs + código implementado. 10 DIMENSIONES AUDITADAS. CROSS-CHECK 32 HUs: 16/16 PASS + 14 PASS + 2 INFO. CORRECCIONES: (M1) Domain require() messages cambiados de español a inglés per §5.7; (M2) eliminadas referencias falsas a "BD CHECK constraints" — Room no genera CHECK constraints nativamente; (L1) referencia MDS §7.7 corregida a §6.B.7/§4.C.7; (L2) "inyecta 6 DAOs" corregido a "6 parámetros (5 DAOs + TensionDatabase)"; (L3) "158 líneas" corregido a "164 líneas"; (L4) agregadas HU-20/HU-21/HU-27 a dependencias futuras; (L5) conteo correcto de strings: ~16; (L6) formateo de peso corregido a String.format("%.1f"). RESULTADO FINAL: 0 HIGH, 0 MEDIUM (2 corregidos), 1 LOW documentada (keyboardType Decimal), 6 LOW corregidas. -->

## Consideraciones Generales

**Basado en análisis arquitectónico:**
HU-06 con 9 hitos, ~8 componentes nuevos/modificados, 6 riesgos identificados. Sexta historia — primera historia de captura de datos transaccional. Introduce E2 (Registro de Serie) con 3 variantes, validación en 2 capas, transacción atómica para asignación secuencial de número de serie, y precarga cross-session del último peso utilizado.

**Nivel de complejidad:**
MEDIA — No introduce entidades Room nuevas (las 4 tablas de sesión ya existen desde HU-05). Modifica 3 DAOs existentes, extiende `SessionRepository`/`SessionRepositoryImpl` con 2 métodos nuevos y 2 DAOs inyectados, crea 1 modelo de dominio, 2 Use Cases, 1 pantalla (E2) con ViewModel y UiState, 1 ruta de navegación, y ~16 strings.

**Riesgos técnicos conocidos:**
1. Condición de carrera en asignación de `set_number` — `database.withTransaction {}` + `UNIQUE` constraint + `isSaving = true` en UI.
2. LEFT JOIN `plan_assignment` falla con ejercicio sustituido (HU-07 futuro) — LEFT JOIN + `COALESCE(pa.sets, 4)` protege.
3. Registro de serie 5+ por error de lógica — Validación en repository + UI no muestra "Registrar" en ejercicios COMPLETED.
4. Precarga retorna peso de sesión de descarga (HU-17 futuro) — Comportamiento intencionado per CA-06.04.

**Patrones y convenciones del equipo (establecidos en HU-01—HU-05):**
- Código fuente en inglés, UI y datos de dominio en español (Arquitectura Técnica §5.1)
- Naming: `{Feature}Screen`, `{Feature}ViewModel`, `{Acción}{Entidad}UseCase` (§5.2)
- Estructura Composable: `hiltViewModel()` + `collectAsStateWithLifecycle()` + `LaunchedEffect` para eventos oneshot (§5.3)
- Estructura ViewModel: `_uiState MutableStateFlow` / `uiState StateFlow` + `MutableSharedFlow` para eventos de navegación (§5.4)
- Data classes intermedias de query en archivos DAO (patrón `SessionExerciseWithDetails`, `ActiveSessionInfo`)

**Dependencias nuevas a instalar:** Ninguna.

**Estrategia de testing:**
JUnit 4 (ADR-18) + MockK + kotlinx-coroutines-test | Tests unitarios para los 2 Use Cases | Cobertura: 100% Use Cases

## Historias Relacionadas Consultadas

**Patrones de código reutilizados:**
- `database.withTransaction {}` de `SessionRepositoryImpl.startSession()` → reutilizado en `registerSet()`.
- `SavedStateHandle` de `ActiveSessionViewModel` → reutilizado en `RegisterSetViewModel`.
- `MutableSharedFlow<Boolean>(replay = 0)` para evento oneshot de navegación back.
- Data class intermedia en DAO (`SessionExerciseWithDetails`) → patrón reutilizado para `SetExerciseInfo`.
- `showBottomBar` exclusión por prefijo → se extiende para `"register-set"`.

**HUs futuras que dependen de artefactos de HU-06:**
- HU-07: Sustitución → LEFT JOIN en `getExerciseInfoForSet()` con `COALESCE(pa.sets, 4)` ya prepara E2 para ejercicios sustituidos.
- HU-08: CAs de registro (CA-08.01, CA-08.04, CA-08.05, CA-08.08) cubiertos completamente en E2. CAs de progresión son de HU-10/HU-11.
- HU-09: Cerrar sesión → lee `exercise_set` para determinar estado completado vs incompleto.
- HU-10: Clasificación de progresión → lee `exercise_set` para calcular promedios peso/reps/RIR.
- HU-11: Doble Umbral → lee `exercise_set` para verificar condiciones de progresión, escribe `exercise_progression.prescribed_load_kg`.
- HU-12: Detección de regresión → lee `exercise_set` (weight_kg, reps, rir) para comparar contra historial.
- HU-13: Resumen post-sesión → lee `exercise_set` para calcular tonelaje y promedios.
- HU-17: Descarga → lee `exercise_set.weight_kg` para calcular 60% (carga descarga) y 90% (carga reinicio).
- HU-20: KPIs de volumen → lee `exercise_set` (weight_kg × reps) para tonelaje acumulado semanal.
- HU-21: KPIs de intensidad → lee `exercise_set.rir` para RIR promedio.
- HU-23: Historial de carga → lee `exercise_set` para historial completo por ejercicio.
- HU-24: Historial de sesiones → lee `exercise_set` para detalle de series registradas.
- HU-27: Alertas de RIR → lee `exercise_set.rir` para promedios por módulo.

## Código existente verificado (HU-01 a HU-05 implementados)

| Componente | Archivo | Estado |
| --- | --- | --- |
| `ExerciseSetDao` | `data/local/dao/ExerciseSetDao.kt` | Existe — stub vacío. Se modifica: +3 métodos |
| `ExerciseSetEntity` | `data/local/entity/ExerciseSetEntity.kt` | Existe — 6 columnas, UNIQUE, FK CASCADE. No se modifica |
| `SessionExerciseDao` | `data/local/dao/SessionExerciseDao.kt` | Existe — `insertAll()`, `getBySessionId()`, `getBySessionIdWithDetails()`. Se modifica: +`getExerciseInfoForSet()` + `SetExerciseInfo` |
| `ExerciseProgressionDao` | `data/local/dao/ExerciseProgressionDao.kt` | Existe — `getByExerciseId()`, `insert()`, `update()`. Se modifica: +`insertIfNotExists()` |
| `ExerciseProgressionEntity` | `data/local/entity/ExerciseProgressionEntity.kt` | Existe — PK exerciseId, status default "NO_HISTORY", prescribedLoadKg nullable. No se modifica |
| `SessionRepository` | `domain/repository/SessionRepository.kt` | Existe — 6 métodos. Se modifica: +2 métodos |
| `SessionRepositoryImpl` | `data/repository/SessionRepositoryImpl.kt` | Existe — 164 líneas, 6 parámetros (5 DAOs + TensionDatabase). Se modifica: +2 métodos, +2 DAOs |
| `NavigationRoutes` | `ui/navigation/NavigationRoutes.kt` | Existe. Se modifica: +REGISTER_SET + helper |
| `TensionNavHost` | `ui/navigation/TensionNavHost.kt` | Existe — TODO HU-06 stub confirmado. Se modifica: wiring + composable + showBottomBar |
| `ActiveSessionScreen` | `ui/session/ActiveSessionScreen.kt` | Existe — botón "Registrar" pasa `sessionExerciseId` via `onNavigateToRegisterSet`. No se modifica |
| `TensionDatabase` | `data/local/database/TensionDatabase.kt` | Existe — version 4, 14 entities, `exerciseSetDao()` expuesto. No se modifica |
| `DatabaseModule` | `di/DatabaseModule.kt` | Existe — providers para `exerciseSetDao()` y `exerciseProgressionDao()` ya existen. No se modifica |

## Tareas de Implementación

### Fase 1: Data Layer — DAOs (3 modificaciones)

> Basado en Hito #1 del Análisis Arquitectónico

- [ ] **Modificar ExerciseSetDao** (+3 métodos) (AC: 06.04, 06.06, 06.09)
  - [ ] Convertir de stub vacío a DAO funcional. Agregar imports: `@Insert`, `@Query`, `ExerciseSetEntity`.
  - [ ] `@Insert suspend fun insert(set: ExerciseSetEntity): Long` — Retorna `id` generado.
  - [ ] `@Query suspend fun getNextSetNumber(sessionExerciseId: Long): Int` — `SELECT COUNT(*) + 1 FROM exercise_set WHERE session_exercise_id = :sessionExerciseId`. Si ya hay 4 series, retorna 5 — el repository lo valida.
  - [ ] `@Query suspend fun getLastWeightForExercise(exerciseId: Long): Double?` — Query cross-session (ver query completa en §5 §1 DAOs). `ORDER BY es.id DESC LIMIT 1`. Retorna `null` si sin historial.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/dao/ExerciseSetDao.kt`

- [ ] **Modificar SessionExerciseDao** (+1 método + 1 data class) (AC: 06.08)
  - [ ] **Definir `SetExerciseInfo`** como data class fuera de la interfaz (no `@Entity`): `exerciseId: Long`, `exerciseName: String`, `isBodyweight: Int`, `isIsometric: Int`, `isToTechnicalFailure: Int`, `totalSets: Int`.
  - [ ] `@Query suspend fun getExerciseInfoForSet(sessionExerciseId: Long): SetExerciseInfo?` — Query JOIN con LEFT JOIN + `COALESCE(pa.sets, 4)` (ver query completa en §5 §1 DAOs). Método `suspend` (no Flow) — consulta one-shot al abrir E2.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/dao/SessionExerciseDao.kt`

- [ ] **Modificar ExerciseProgressionDao** (+1 método) (AC: 06.08)
  - [ ] `@Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertIfNotExists(progression: ExerciseProgressionEntity)` — Crea fila `exercise_progression` con defaults al registrar la primera serie. Si la fila ya existe (ejercicio con historial), IGNORE la descarta silenciosamente. Agregar import de `OnConflictStrategy`.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/dao/ExerciseProgressionDao.kt`

### Fase 2: Domain Layer — Model

> Basado en Hito #2 del Análisis Arquitectónico

- [ ] **Crear RegisterSetInfo** (AC: 06.01, 06.04)
  - [ ] Data class Kotlin puro con 9 campos (ver detalle completo en §5 §2 Domain Models). `lastWeightKg: Double?` para precarga; `isBodyweight`, `isIsometric`, `isToTechnicalFailure: Boolean` para variantes del formulario.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/model/RegisterSetInfo.kt`

### Fase 3: Domain Layer — Repository Interface (Modificación)

> Basado en Hito #3 del Análisis Arquitectónico

- [ ] **Modificar SessionRepository** (+2 métodos) (AC: 06.01-06.09)
  - [ ] `suspend fun getRegisterSetInfo(sessionExerciseId: Long): RegisterSetInfo?` — One-shot (no Flow). Retorna `null` si no existe o ejercicio ya completado.
  - [ ] `suspend fun registerSet(sessionExerciseId: Long, weightKg: Double, reps: Int, rir: Int)` — Transacción atómica. Lanza `IllegalStateException` si ya tiene 4 series.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/repository/SessionRepository.kt`

### Fase 4: Domain Layer — Use Cases (2 nuevos)

> Basado en Hito #4 del Análisis Arquitectónico

- [ ] **Crear GetRegisterSetInfoUseCase** (AC: 06.01, 06.04)
  - [ ] Wrapper puro de `SessionRepository.getRegisterSetInfo()`. `suspend operator fun invoke(sessionExerciseId: Long): RegisterSetInfo?`.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/usecase/session/GetRegisterSetInfoUseCase.kt`
  - [ ] Test unitario: delegación, info encontrada, null — Archivo: `...GetRegisterSetInfoUseCaseTest.kt`

- [ ] **Crear RegisterSetUseCase** (AC: 06.05, 06.06, 06.07, 06.08, 06.09)
  - [ ] Valida `require(weightKg >= 0)`, `require(reps >= 1)`, `require(rir in 0..5)` (mensajes en inglés per §5.7). Delega a `sessionRepository.registerSet()`.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/usecase/session/RegisterSetUseCase.kt`
  - [ ] Test unitario: peso < 0 lanza, reps < 1 lanza, rir fuera de 0-5 lanza, caso exitoso delega — Archivo: `...RegisterSetUseCaseTest.kt`

### Fase 5: Data Layer — Repository Implementation (Modificación)

> Basado en Hito #5 del Análisis Arquitectónico

- [ ] **Modificar SessionRepositoryImpl** (+2 métodos, +2 DAOs inyectados) (AC: 06.01-06.09)
  - [ ] **Agregar al constructor:** `private val exerciseSetDao: ExerciseSetDao` y `private val exerciseProgressionDao: ExerciseProgressionDao`. El constructor pasa de 6 a 8 parámetros. Los providers ya existen en `DatabaseModule`.
  - [ ] **Implementar `getRegisterSetInfo()`:** Ver lógica completa en §5 §5 Repository Implementation (4 pasos: getExerciseInfo → getNextSetNumber → lastWeightKg → mapeo).
  - [ ] **Implementar `registerSet()`:** En `database.withTransaction {}`. Ver lógica completa en §5 §5 (5 pasos: getNextSetNumber → getExerciseInfo → validar ≤ totalSets → INSERT exercise_set → INSERT idempotente exercise_progression).
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/repository/SessionRepositoryImpl.kt`

### Fase 6: UI Layer — State + ViewModel

> Basado en Hito #6 del Análisis Arquitectónico

- [ ] **Crear RegisterSetUiState** (AC: 06.01, 06.03, 06.04)
  - [ ] Data class con `isLoading`, `exerciseName`, `currentSetNumber`, `totalSets`, `weightKg: String`, `reps: String`, `selectedRir: Int?`, `isWeightEditable`, `isIsometric`, `isBodyweight`, `weightError: String?`, `repsError: String?`, `isSaving`. Propiedad derivada `isConfirmEnabled: Boolean`. Ver definición completa en §5 §6 Estado y ViewModel.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/session/RegisterSetUiState.kt`

- [ ] **Crear RegisterSetViewModel** (AC: 06.01-06.09)
  - [ ] `@HiltViewModel`. Inyecta `GetRegisterSetInfoUseCase`, `RegisterSetUseCase`, `SavedStateHandle`. Extrae `sessionExerciseId` del `SavedStateHandle`. `_navigateBack: MutableSharedFlow<Boolean>(replay = 0)`. `init` carga `RegisterSetInfo` y puebla el estado. Métodos: `onWeightChanged()`, `onRepsChanged()`, `onRirSelected()`, `onConfirm()` con `isSaving` protection y manejo de `IllegalArgumentException`/`IllegalStateException`. Ver lógica completa en §5 §6.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/session/RegisterSetViewModel.kt`

### Fase 7: UI Layer — Screen E2

> Basado en Hito #7 del Análisis Arquitectónico

- [ ] **Crear RegisterSetScreen** (AC: 06.01-06.09, CA-08.01, CA-08.04, CA-08.05)
  - [ ] `CenterAlignedTopAppBar` con cierre (✕) + título (ejerciseName + "Serie N de 4"). Body con `Column` padding 16 dp: OutlinedTextField Peso (3 variantes: estándar/bodyweight/isométrico con `KeyboardType.Decimal`), OutlinedTextField Reps/Segundos (2 variantes con `KeyboardType.Number`), selector RIR (6 chips `Box` 48×48 dp circulares, single select), `Button("Confirmar")` full width enabled=isConfirmEnabled, `TextButton("Cancelar")`. `LaunchedEffect` recolecta `navigateBack`. Sin Bottom Nav. Ver especificaciones de colores, bordes e interacciones en §5 §7.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/session/RegisterSetScreen.kt`

### Fase 8: Navigation + Strings

> Basado en Hitos #8 y #9 del Análisis Arquitectónico

- [ ] **Agregar ruta REGISTER_SET** (AC: 06.06)
  - [ ] `const val REGISTER_SET = "register-set/{sessionExerciseId}"` y `fun registerSetRoute(sessionExerciseId: Long) = "register-set/$sessionExerciseId"` — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/navigation/NavigationRoutes.kt`

- [ ] **Actualizar NavHost** (AC: 06.06)
  - [ ] Wiring del callback en `ACTIVE_SESSION` (reemplazar TODO HU-06 con navegación real). Nuevo composable entry `REGISTER_SET` con `navArgument("sessionExerciseId", NavType.LongType)`. Extender `showBottomBar` para excluir `"register-set"` (ver lógica completa en §5 §8 Navegación) — Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/ui/navigation/TensionNavHost.kt`

- [ ] **Actualizar strings.xml** (AC: 06.01)
  - [ ] Agregar los 16 strings de E2 (ver sección Resources en §5 §9). — Archivo: `app/src/main/res/values/strings.xml`

### Fase 9: QA y Deployment

#### Code Quality

- [ ] **Ejecutar Agente Peer Review** — MANUAL
- [ ] **Resolver incidentes del Peer Review** (condicional) — MANUAL

#### Deployment DEV

- [ ] **Crear Pull Request** — MANUAL
- [ ] **Ejecutar pipeline deployment DEV** — MANUAL

#### Testing Manual

- [ ] **Diseñar set de pruebas manuales** — MANUAL
- [ ] **Ejecutar pruebas manuales** — MANUAL

---

**Vinculación CA → Fase de implementación:**
- CA-06.01 → Fases 2, 6, 7 (RegisterSetInfo model + RegisterSetUiState 3 campos + RegisterSetScreen 3 inputs)
- CA-06.02 → Fase 7 (peso precargado + chips RIR = flujo óptimo 3 toques, RNF02)
- CA-06.03 → Fase 7 (`KeyboardType.Decimal` para peso, `KeyboardType.Number` para reps/segundos, chips directos para RIR, RNF03)
- CA-06.04 → Fases 1, 5, 6 (`ExerciseSetDao.getLastWeightForExercise()` cross-session → `RegisterSetInfo.lastWeightKg` → precarga en ViewModel, RNF04)
- CA-06.05 → Fases 4, 6, 7 (`RegisterSetUseCase: require(weightKg >= 0)` + validación RT en ViewModel, RNF12)
- CA-06.06 → Fases 4, 6, 7 (`RegisterSetUseCase: require(reps >= 1)` + validación RT, RNF12)
- CA-06.07 → Fases 4, 7 (Chips 0-5 por construcción + `require(rir in 0..5)`, RNF12)
- CA-06.08 → Fases 1, 5 (metadatos automáticos por cadena relacional `exercise_set → session_exercise → session → module_version`, RF14)
- CA-06.09 → Fases 1, 5 (`ExerciseSetDao.getNextSetNumber()` COUNT(*)+1 + transacción atómica + UNIQUE constraint, RF15)
- CA-06.10 → Sin trabajo en HU-06 (ya implementado en HU-05: E1 botón "Registrar" independiente por ejercicio, RF15)
- CA-06.11 → Sin trabajo en HU-06 (cumplido automáticamente: `session_exercise.exercise_id` = ejercicio efectivo)
- CA-06.12 → Sin trabajo en HU-06 (ya implementado en HU-05: Flow reactivo via COUNT subquery, RF17/RF22)
- CA-06.13 → Sin trabajo adicional en HU-06 (Room INSERT atómico + HU-05 B1 Card "Reanudar", RNF10)
