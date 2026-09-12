## Refinamiento Técnico (Developer)
**Autor**: Esteban Colorado González | **Fecha**: 2026-09-11

---

### Contexto

`HU-39` creó el par `(ejercicio, equipamiento)` en la serie y `HU-40` lo convirtió en la unidad de comparación del motor. Esta historia **no toca ninguno de los dos**: añade una capacidad nueva encima del par, con su propia tabla, su propia pantalla y una sola regla de cálculo. Es la hija paralelizable de la partición, y su superficie es la más pequeña de las cuatro.

**Feature análoga leída completa: `HU-37` — el árbol de entrenamiento (`Flujo N`).** Es el precedente exacto porque resuelve el mismo problema de forma: *una funcionalidad puramente visual, colgada de una pantalla existente, con entidad propia y dependencia unidireccional hacia el historial*.

```kotlin
// TreeStateEntity.kt — la entidad satélite: sin FK hacia el historial, y guardando
// solo lo que los criterios exigen persistido, nada derivable en caliente.
@Entity(tableName = "tree_state")
data class TreeStateEntity(…)
```

```kotlin
// TreeRepository.kt — el aislamiento declarado en el propio contrato, no solo en la
// documentación. "El árbol lee del historial y nada del sistema lee del árbol."
interface TreeRepository { fun getTreeState(): Flow<TreeState>; suspend fun recalculate() }
```

```kotlin
// TensionNavHost.kt — pantalla dedicada, alcanzada desde una tarjeta de otra pantalla,
// sin pestaña en la barra inferior, con el retroceso nativo como única navegación.
composable(NavigationRoutes.TREE) { TreeScreen(onNavigateBack = { navController.popBackStack() }) }
```

```kotlin
// Migrations.kt:1245 — el esquema sube sin migración y la guarda es una constante.
const val LAST_MIGRATED_VERSION = 19   // ADR-019
```

Y la feature análoga **del lado de la escritura** es la propia `registerSet`, que ya escribe una segunda tabla dentro de su transacción:

```kotlin
// SessionRepositoryImpl.kt:665 — registrar una serie ya tiene un efecto colateral
// persistido sobre otro agregado, dentro de la misma transacción.
exerciseProgressionDao.insertIfNotExists(
    ExerciseProgressionEntity(exerciseId = info.exerciseId, equipmentTypeId = equipmentTypeId),
)
```

#### Superficie real del cambio

| Capa | Qué entra |
|---|---|
| `DB-01` | Una tabla nueva, `exercise_one_rm`. Esquema 22 → 23, **sin migración** (ADR-019). |
| `DAT-01` | `ExerciseOneRmEntity`, `ExerciseOneRmDao`, `OneRmRepositoryImpl`. Cuatro líneas dentro de `SessionRepositoryImpl.registerSet`. Respaldo 15 → 16. |
| `DOM-01` | `OneRmRule` (pura), `OneRmRepository`, `GetOneRmListUseCase`, dos modelos de dominio. |
| `UI-02` | `OneRmViewModel`, `OneRmUiState`. |
| `UI-01` | `OneRmScreen` (pantalla nueva), una ruta, una entrada en `MetricsScreen`. |

#### El hallazgo que decide el diseño

**La condición de exclusión de CA-42.05 ya está implementada y probada.** `ExternalLoadRule.isCaptureEnabled(isBodyweight, isIsometric, equipmentName)` decide exactamente lo que la CA describe, rama por rama: isométrico → falso; `Peso Corporal` → falso; `Peso Añadido` → verdadero; peso corporal con `Barra Fija` o `Máquina` → falso; cualquier otro → verdadero. No hay que escribir esa lógica: hay que **invocarla**, y las cuatro opciones de `Dominadas` que la DoD pide verificar son justo el caso que `ExternalLoadRuleTest` ya cubre.

Y `registerSet` **ya la ha evaluado** en el punto donde el 1RM se calcula: `hasExternalLoad` y `persistedWeightKg` están en la misma transacción, cuatro líneas antes. El cálculo no necesita releer nada.

#### Lo que NO se toca

- **El motor de decisión, íntegro.** Ninguna de las reglas, ningún caso de uso de cierre, ninguna alerta, ningún KPI. La dependencia es unidireccional y verificable: `exercise_one_rm` no aparece en ningún `SELECT` fuera de `ExerciseOneRmDao`.
- **`ExerciseSetEntity`.** El 1RM no añade columnas a la serie; se deriva de ella.
- **`ExternalLoadRule`.** Se reutiliza sin modificar.
- **El catálogo, el plan, la rotación, la descarga, el historial.**
- **Retro-cálculo.** No existe. ADR-019 deja la base vacía al llegar la historia, y no hay serie previa que barrer.

---

### Decisiones técnicas

#### D1 — `exercise_one_rm`: la tabla **es** la lista

CA-42.02 pide mostrar solo los ejercicios entrenados; CA-42.07 pide que no aparezca un par entrenado sin serie que califique. Las dos juntas describen exactamente el conjunto de filas de una tabla que **solo nace cuando una serie califica**. No hace falta filtrar por historial, ni comparar contra el Diccionario, ni decidir en Kotlin qué se oculta: la vista lista las filas que hay.

PK compuesta `(exercise_id, equipment_type_id)`, FK `RESTRICT` a ambos lados e índice sobre la columna que no encabeza la PK — la forma exacta de `exercise_progression` desde `HU-40` y de `exercise_equipment` desde `HU-39`.

#### D2 — El récord se escribe **dentro de la transacción de la serie**, no en un paso best-effort posterior

Es la única divergencia deliberada respecto a `HU-37`, y hay una razón para ella. El árbol recalcula *best-effort* porque es **enteramente derivable**: si un recálculo falla, el siguiente lo repara. **El 1RM no se repara nunca** — la propia CA-42.08 lo argumenta al exigir que el respaldo lo restaure en lugar de recalcularlo. Una escritura best-effort que falla es un máximo perdido para siempre, y sin retro-cálculo no hay forma de recuperarlo.

Por eso la actualización vive donde la atomicidad ya existe: dentro de `database.withTransaction` de `SessionRepositoryImpl.registerSet`, junto al `insertIfNotExists` de `exercise_progression` que ese mismo método ya hace. O se persisten la serie y su récord, o no se persiste ninguno de los dos.

El aislamiento que `HU-37` hizo legible en el contrato se conserva donde importa: **la lectura**. `OneRmRepository` es un contrato separado que solo lee su propia tabla y los nombres del catálogo, y ningún componente del sistema lo consume. Que `SessionRepositoryImpl` *escriba* el récord no invierte esa dirección — escribir no es leer.

Todo lo que decide vive en `OneRmRule`, pura y probada en JVM. Lo que queda en el repositorio son cuatro líneas de fontanería.

#### D3 — `OneRmRule`: la fórmula general, invocada solo en el punto único de operación

```kotlin
object OneRmRule {
    const val REFERENCE_REPS = 10
    const val REFERENCE_RIR = 1

    fun qualifies(reps: Int, rir: Int, hasExternalLoad: Boolean, weightKg: Double): Boolean
    fun estimate(weightKg: Double, reps: Int): Double   // weightKg / (1.0278 - 0.0278 * reps)
}
```

La fórmula se escribe **general**, tal como la enuncia CA-42.03, y no precomputada como `× 1.3337`. La constante mágica escondería que el denominador se anula en 36.97 repeticiones, que es precisamente lo que la CA quiere que quede dicho; y `qualifies` garantiza que el único valor con el que `estimate` se invoca es 10. La equivalencia `Peso × 1.3337` se afirma en el test, que es donde tiene valor.

#### D4 — Un 1RM de cero no es un 1RM

`hasExternalLoad` puede ser cierto con peso 0 — una máquina en la que el ejecutante teclea 0. La CA no lo contempla, pero CA-42.07 es explícita en que *nunca se muestra un cero*, y una fila con `one_rm_kg = 0` sería exactamente eso. `qualifies` exige `weightKg > 0`, de modo que el cero no crea fila en lugar de crearla y ocultarla después.

#### D5 — La monotonía es una comparación, no un `MAX` de SQLite

`INSERT … ON CONFLICT DO UPDATE` exige SQLite 3.24, es decir API 30; el `minSdk` del proyecto es 26. La monotonía se resuelve leyendo y comparando **dentro de la transacción que ya está abierta**:

```kotlin
val current = exerciseOneRmDao.getValue(exerciseId, equipmentTypeId)
if (current == null || estimated > current) exerciseOneRmDao.upsert(…)
```

Esto satisface las cuatro cláusulas de CA-42.04 de una vez: sube si es mayor, conserva si es igual o menor, gana la más alta cuando varias series de la misma sesión califican —cada una pasa por la misma comparación— y repetir una sesión idéntica no escribe nada en absoluto, así que no hay recálculo ni siquiera interno.

#### D6 — La tabla **no** lleva `calculated_at`

`tree_state` guarda la fecha del último recálculo porque hace auditable su orden respecto al barrido del cambio de día. Aquí no hay barrido, no hay orden que auditar y CA-42.06 dice explícitamente que la vista no expone la fecha. Una columna que nadie lee y que además cambiaría al repetir una sesión idéntica —contra el espíritu de CA-42.04— es peor que su ausencia. Tres columnas: el par y el valor.

#### D7 — Kilogramo canónico, dos unidades en presentación, conversión en la frontera

`one_rm_kg` es `REAL NOT NULL`, coherente con `weight_kg` y con la convención base §1 del modelo de dominio. Las libras **no se persisten**: se derivan con `WeightConverter.fromKg(kg, WeightUnit.LB)`, que ya aplica el factor 0.45359237 y redondea a un decimal. Es el patrón que `HU-30` estableció y que `SessionDetailScreen` ya usa.

La presentación no consulta `capture_unit` en ningún punto: ambas unidades se muestran siempre (CA-42.06).

> El preview muestra `33.3 kg · 73.5 lb`. Convertido desde los 33.3 kg redondeados salen 73.4 lb. La diferencia es del redondeo del prototipo, no del factor; el valor que se muestra se deriva siempre del canónico persistido.

#### D8 — El agrupado por ejercicio vive en el Use Case

El DAO devuelve filas planas ya ordenadas —`ORDER BY e.name ASC, q.id ASC`— porque ordenar es trabajo de SQL, y el orden de los implementos es el mismo que `HU-39` fijó para las casillas del formulario: el `id` encoda la posición declarada del catálogo. `GetOneRmListUseCase` agrupa esa lista plana en una tarjeta por ejercicio.

Se pone ahí, y no en el repositorio ni en el ViewModel, por una razón de pruebas: es la estrategia que el proyecto ya tiene —*Use Cases con repositorios mockeados*— y deja el agrupado, el orden y el caso vacío cubiertos en JVM sin emulador.

#### D9 — Un solo implemento entrenado: etiqueta, no chip

CA-42.02 dice que un ejercicio con un único equipamiento muestra su valor *sin exigir selección*. Se resuelve en el modelo, no en el Composable: `ExerciseOneRm.isSelectable` es `equipment.size > 1`. La pantalla no vuelve a contar.

#### D10 — Respaldo 15 → 16, y el 15 se rechaza

Cuarta historia consecutiva que invalida el formato, y por la misma razón que las tres anteriores: **el récord no se deriva de nada**. Un respaldo v15 no lleva `exercise_one_rm`, y aceptarlo dejaría una vista vacía sobre un historial lleno de series que calificaron. Recalcularlo desde el historial es justo lo que CA-42.08 prohíbe.

El mecanismo es genérico —vuelca columnas por cursor—, así que la tabla entra sola: basta añadirla a `TABLE_ORDER_INSERT`, detrás de `exercise` y `equipment_type`, que son sus padres. El mensaje de rechazo estrena cadena: el actual nombra la ausencia del equipamiento de las series, que no es la causa aquí.

#### D11 — Esquema 22 → 23 sin migración

`LAST_MIGRATED_VERSION` sigue en 19. El KDoc de la constante enumera las historias que suben el esquema sin migrar; esta añade la cuarta (ADR-019, CA-42.08).

#### D12 — `Flujo O — 1RM Estimado`, colgado de `G1`

`HU-37` creó `Flujo N` para la pantalla del árbol. Esta crea `Flujo O`, con el mismo perfil: una sola pantalla, alcanzable desde un único punto, sin efectos sobre ningún otro contenedor. `G1` gana `G1-T4` —la entrada— y `E2-T1` gana la nota del efecto colateral persistido, que hoy ya declara para `exercise_progression`.

#### D13 — La entrada en Métricas: decisión abierta ⚠️ → **resuelta: opción 2, aprobada por el PO (2026-09-11)**

El preview dibuja las tres entradas de `G1` —1RM, Volumen, Tendencia— como **tarjetas con título, subtítulo y chevron**. El código actual tiene las dos existentes como `TextButton` de ancho completo. Hay dos lecturas:

1. **Solo la nueva como tarjeta.** Respeta el preview para lo que la historia añade y no toca nada existente, pero deja tres accesos hermanos con dos aspectos distintos.
2. **Las tres como tarjeta.** Es lo que el preview dibuja y lo coherente visualmente, pero reestiliza dos accesos que esta historia no pide cambiar.

**Propuesta: la opción 2**, acotada a extraer un componente de acceso compartido en `ui/components`. Es un cambio de presentación sin lógica, el preview lo respalda y la alternativa deja una inconsistencia visible en la pantalla que la historia estrena. Queda marcada para confirmación: si se prefiere la 1, el cambio se localiza en T17 y ninguna otra tarea se mueve.

---

### Tareas de Implementación

#### Fase 1 — Esquema (CA-42.03, CA-42.08)

- [x] **T1: `ExerciseOneRmEntity`** — `data/local/entity/ExerciseOneRmEntity.kt` (nuevo) (Base: `entity/ExerciseProgressionEntity.kt`)
  Tabla `exercise_one_rm`. PK compuesta `(exercise_id, equipment_type_id)`, FK `RESTRICT` a `exercise` y a `equipment_type`, `Index(["equipment_type_id"])`. Tercera columna: `one_rm_kg REAL NOT NULL`. Sin `calculated_at` (D6). KDoc que declara el aislamiento y por qué se persiste un valor derivado (D1, D2).
- [x] **T2: `ExerciseOneRmDao`** — `data/local/dao/ExerciseOneRmDao.kt` (nuevo) (Base: `dao/TreeStateDao.kt`, `dao/ExerciseProgressionDao.kt`)
  Depende de T1. `getValue(exerciseId, equipmentTypeId): Double?` para la comparación de D5; `upsert` con `OnConflictStrategy.REPLACE`; `getAll(): Flow<List<ExerciseOneRmRow>>` con el `INNER JOIN` a `exercise` y `equipment_type` y `ORDER BY e.name ASC, q.id ASC` (D8). El DTO `ExerciseOneRmRow` acompaña al DAO, como los demás del proyecto.
- [x] **T3: Registrar entidad y DAO, esquema a 23** — `data/local/database/TensionDatabase.kt`
  Depende de T1, T2. `version = 23`. **Sin migración**; `LAST_MIGRATED_VERSION` sigue en 19 (D11).
- [x] **T4: KDoc de `LAST_MIGRATED_VERSION`** — `data/local/database/Migrations.kt`
  Depende de T3. Una frase: `HU-42` lleva el esquema a la 23, también sin migración (ADR-019, CA-42.08).
- [x] **T5: Proveer el DAO** — `di/DatabaseModule.kt`
  Depende de T2, T3. Un `@Provides` más, igual que `provideTreeStateDao`.
- [x] **T6: Exportar `23.json`** — `app/schemas/…TensionDatabase/23.json`
  Depende de T3. Lo genera el build (`exportSchema = true`); se versiona en Git.

#### Fase 2 — La regla (CA-42.03, CA-42.04, CA-42.05)

- [x] **T7: `OneRmRule`** — `domain/rules/OneRmRule.kt` (nuevo) (Base: `rules/TreeHealthRule.kt`, `rules/ExternalLoadRule.kt`)
  `REFERENCE_REPS = 10`, `REFERENCE_RIR = 1`. `qualifies(reps, rir, hasExternalLoad, weightKg)` — las cuatro condiciones a la vez, incluida `weightKg > 0` (D4). `estimate(weightKg, reps)` con la fórmula general (D3). Kotlin puro, sin Android, sin acceso a datos. KDoc con la indeterminación en 36.97 repeticiones y con la declaración de que **no forma parte del motor de decisión**, al modo de `ExternalLoadRule` y `TreeHealthRule`.

#### Fase 3 — Escritura del récord (CA-42.03, CA-42.04, CA-42.05)

- [x] **T8: `registerSet` actualiza el récord del par** — `data/repository/SessionRepositoryImpl.kt`
  Depende de T2, T7. Dentro de la transacción existente, detrás del `insertIfNotExists` de `exercise_progression`: `OneRmRule.qualifies(reps, rir, hasExternalLoad, persistedWeightKg)` y, si califica, leer-comparar-escribir (D5). Reutiliza `hasExternalLoad` y `persistedWeightKg` ya calculados; **no** vuelve a consultar el catálogo. Comentario que fija por qué esto es transaccional y no best-effort (D2).

#### Fase 4 — Lectura y dominio (CA-42.02, CA-42.06, CA-42.07)

- [x] **T9: Modelos de dominio** — `domain/model/OneRmEntry.kt` (nuevo), `domain/model/ExerciseOneRm.kt` (nuevo) (Base: `domain/model/TreeState.kt`)
  `OneRmEntry` es la fila plana; `ExerciseOneRm` es la tarjeta —`exerciseId`, `exerciseName`, `equipment: List<EquipmentOneRm>`— con `isSelectable` derivado del tamaño de la lista (D9).
- [x] **T10: `OneRmRepository`** — `domain/repository/OneRmRepository.kt` (nuevo) (Base: `domain/repository/TreeRepository.kt`)
  Un solo método: `getAll(): Flow<List<OneRmEntry>>`. KDoc con el aislamiento declarado en el contrato, como hace `TreeRepository`.
- [x] **T11: `OneRmRepositoryImpl`** — `data/repository/OneRmRepositoryImpl.kt` (nuevo) (Base: `data/repository/TreeRepositoryImpl.kt`)
  Depende de T2, T9, T10. `@Singleton`, mapea las filas del DAO a `OneRmEntry` y nada más: no ordena —lo hace SQL— y no agrupa —lo hace el Use Case (D8).
- [x] **T12: Vincular el repositorio** — `di/RepositoryModule.kt`
  Depende de T10, T11. Un `@Binds` más.
- [x] **T13: `GetOneRmListUseCase`** — `domain/usecase/onerm/GetOneRmListUseCase.kt` (nuevo) (Base: `domain/usecase/tree/GetTreeStateUseCase.kt`)
  Depende de T9, T10. Agrupa la lista plana en tarjetas conservando el orden de llegada (D8). Lista vacía entra, lista vacía sale: el estado vacío lo resuelve la pantalla.

#### Fase 5 — Interfaz (CA-42.01, CA-42.02, CA-42.06, CA-42.07)

- [x] **T14: `OneRmUiState`** — `ui/onerm/OneRmUiState.kt` (nuevo) (Base: `ui/tree/TreeUiState.kt`)
  `isLoading`, `exercises: List<ExerciseOneRm>`, `selection: Map<Long, Long>` —ejercicio → implemento activo—. `isEmpty` derivado. El primer implemento de cada tarjeta queda activo al componer.
- [x] **T15: `OneRmViewModel`** — `ui/onerm/OneRmViewModel.kt` (nuevo) (Base: `ui/tree/TreeViewModel.kt`)
  Depende de T13, T14. `@HiltViewModel`, observa el Use Case, expone `selectEquipment(exerciseId, equipmentTypeId)`. Sin eventos one-shot: aquí no se navega ni se decide nada.
- [x] **T16: `OneRmScreen`** — `ui/onerm/OneRmScreen.kt` (nuevo) (Base: `ui/tree/TreeScreen.kt`, `ui/components/MetricCard.kt`)
  Depende de T15. `CenterAlignedTopAppBar` con retroceso nativo como única navegación. `LazyColumn` de tarjetas —radio 12 dp, padding 16 dp—; `FilterChip` por implemento cuando `isSelectable`, etiqueta no interactiva cuando no (D9); valor en kg y lb en la misma línea, derivado con `WeightConverter.fromKg` (D7). Estado vacío que **enuncia la condición de cálculo**, no una lista en blanco (CA-42.07).
- [x] **T17: Entrada en el panel de Métricas** — `ui/metrics/MetricsScreen.kt`, `ui/components/` (componente de acceso compartido)
  Depende de T16, T18, T19. Alcance según D13 — **opción 2, aprobada**: `MetricNavigationEntry` en `ui/components/MetricCard.kt`, usada por las tres entradas. Área táctil ≥ 48 dp (RNF06). La entrada se compone siempre, con o sin sesiones. **No** se toca `BottomNavigationBar` ni `HomeScreen`.
- [x] **T18: Ruta y destino** — `ui/navigation/NavigationRoutes.kt`, `ui/navigation/TensionNavHost.kt`
  `const val ONE_RM = "one-rm"` dentro del grafo de Métricas, con `onNavigateBack = { navController.popBackStack() }`, igual que `TREE`.
- [x] **T19: Cadenas de la pantalla** — `res/values/strings.xml`
  Título, subtítulo de la entrada, formato del valor doble, título y cuerpo del estado vacío. Todas en español, ninguna en el dominio.

#### Fase 6 — Respaldo (CA-42.08)

- [x] **T20: Formato 16** — `data/repository/BackupRepositoryImpl.kt`
  Depende de T1. `exercise_one_rm` en `TABLE_ORDER_INSERT` detrás de `exercise` y `equipment_type`; `SCHEMA_VERSION = 16`; `ACCEPTED_SCHEMA_VERSIONS` solo el vigente. KDoc que añade el motivo del rechazo del 15 (D10).
- [x] **T21: Mensaje de rechazo del formato 15** — `res/values/strings.xml`
  Depende de T20. Cadena nueva: el respaldo anterior no incluye el 1RM y el 1RM no se recalcula desde el historial. La de `import_backup_no_equipment` se conserva para lo que nombra.

#### Fase 7 — Tests

- [x] **T22: `OneRmRuleTest`** — `test/…/domain/rules/OneRmRuleTest.kt` (nuevo) (Base: `rules/ExternalLoadRuleTest.kt`)
  Depende de T7. La tabla completa de calificación del preview: `10/1` ✓; `10/0`, `10/2`, `9/1`, `11/1`, `12/1` ✗. Los tres ejemplos numéricos: 12.0 → 16.0, 70.0 → 93.4, 30.0 → 40.0. La equivalencia `Peso × 1.3337` para 10 repeticiones. Sin carga externa → no califica. Peso 0 con carga habilitada → no califica (D4).
- [x] **T23: `GetOneRmListUseCaseTest`** — `test/…/domain/usecase/onerm/GetOneRmListUseCaseTest.kt` (nuevo) (Base: `usecase/tree/GetTreeStateUseCaseTest.kt`)
  Depende de T13. Agrupado por ejercicio conservando el orden; ejercicio con un implemento → `isSelectable` falso; ejercicio con dos → verdadero; lista vacía → lista vacía.
- [x] **T24: `OneRmViewModelTest`** — `test/…/ui/onerm/OneRmViewModelTest.kt` (nuevo) (Base: `ui/tree/TreeViewModelTest.kt`)
  Depende de T15. El primer implemento queda activo al cargar; `selectEquipment` cambia el valor mostrado sin navegar; el estado vacío se distingue del de carga.
- [x] **T25: `BackupRepositoryImplTest` acompaña al formato 16** — `test/…/data/repository/BackupRepositoryImplTest.kt`
  Depende de T20. `PREVIOUS_SCHEMA_VERSION` pasa a 15; el formato vigente es 16; `exercise_one_rm` viaja en el respaldo y se inserta **después** de `exercise` y `equipment_type`; un respaldo v15 se rechaza; un respaldo vigente sin la tabla se rechaza por incompleto.
- [x] **T26: `MiUnRmEstimadoTest` (instrumentado)** — `androidTest/…/MiUnRmEstimadoTest.kt` (nuevo) (Base: `androidTest/ProgresionPorEquipamientoTest.kt`)
  Depende de T8. Es el único test que ejercita la **escritura** sobre una base real, que es donde vive: `registerSet` → transacción → récord. Cubre lo que la DoD pide verificar y ninguna regla pura alcanza: la serie que califica crea la fila; `9/1`, `11/1`, `10/0` y `10/2` no la crean ni la tocan; un 1RM menor **no** deteriora el guardado; varias series que califican en la misma sesión dejan la más alta; las **cuatro opciones de `Dominadas`** dejan una sola fila, la de `Peso Añadido`, calculada sobre el lastre; un isométrico no deja ninguna.
- [x] **T27: El aislamiento como comprobación documentada** — `HU-42-…/dev-record.md`
  Depende de T8. La DoD pide verificar que ningún componente del motor lee el 1RM. Se afirma por construcción: `exercise_one_rm` y `ExerciseOneRmDao` no aparecen en ningún archivo fuera de los que esta historia crea, más `SessionRepositoryImpl`, `TensionDatabase`, `DatabaseModule` y `BackupRepositoryImpl`. Queda como comprobación de revisión con criterio explícito, no como test automatizado — el proyecto no tiene infraestructura de test de arquitectura.

#### Fase 8 — Documentación (DoD)

- [x] **T28: Modelo de dominio** — `docs/architecture/domain_and_state_model.md`
  Esquema 23; bloque `model exercise_one_rm` en §2 al modo de `tree_state`; fila en la matriz de relaciones §3; §6.1 con *0 filas, no se siembra*; nota en §1 sobre por qué este valor derivado **sí** se persiste, que es la excepción a la convención de «los derivados no se persisten».
- [x] **T29: Contrato de interfaces** — `docs/architecture/interfaces_contract.md`
  `Flujo O — 1RM Estimado` con `O1-T1` (consultar), `O1-T2` (seleccionar implemento) y `O1-T3` (volver a `G1`); `G1-T4` (la entrada); nota en `E2-T1` sobre el efecto colateral persistido; `J2-T1`/`J3-T1` con el formato 16 (D10).
- [x] **T30: Blueprint** — `docs/architecture/architecture_blueprint.md`
  Esquema 23; ADR-019 ampliado con v23; `OneRmRule` entre las notas del motor de reglas **declarando que no forma parte de él**; `OneRmRepositoryImpl` y `ExerciseOneRmDao` en `DAT-01`; `ExerciseOneRmEntity` en el inventario de entities; entrada de trazabilidad funcional al modo de la de `HU-37` — **no mapea ningún RF**.
- [x] **T31: Índice de historias y artefactos de la HU** — `docs/domain/stories/story_mapping_index.md`, `HU-42-…/index.md`, `cambios.md`, `dev-record.md`
  `HU-42` a `Lista para Revisión` con sus hermanas y dependencias; fases, métricas de tiempo y registro cronológico.

---

### Riesgos y observaciones

**El riesgo dominante es que la escritura no tiene red.** El 1RM es el primer dato del sistema que **no se puede reconstruir**: no hay retro-cálculo, el respaldo lo restaura en lugar de recalcularlo y una serie ya registrada no vuelve a pasar por el disparador. Un defecto en T8 —un operador de comparación invertido, la condición evaluada sobre el peso tecleado en vez del persistido— no produce una excepción: produce un número plausible y permanentemente equivocado. La mitigación es doble: toda la decisión está en `OneRmRule`, pura y exhaustivamente probada (T22), y la fontanería restante se ejercita sobre una base real en T26.

**D2 es una divergencia consciente respecto al precedente de `HU-37`, y conviene que se lea como tal.** El árbol escribe best-effort porque puede permitírselo; el 1RM no. Si la revisión prefiere la simetría con `HU-37` —un `UpdateOneRmUseCase` orquestado desde `RegisterSetUseCase`—, el cambio es acotado pero **cambia la garantía**: pasa de «o ambos o ninguno» a «casi siempre ambos». Queda dicho para que la elección sea explícita.

**El preview redondea dos veces y el código una.** `33.3 kg · 73.5 lb` sale de convertir el valor sin redondear; convirtiendo desde los 33.3 mostrados salen 73.4. No es un defecto del factor, y el criterio del sistema —`kg` canónico, presentación derivada— manda sobre el prototipo. Merece decirse porque en una revisión visual contra el preview parece un error de conversión.

**D13 se resolvió por la opción 2, aprobada por el PO.** Las tres entradas de `Flujo G` comparten `MetricNavigationEntry`. Si se quisiera revertir, el coste sigue siendo una tarea, T17.

**El respaldo de ayer no sirve mañana, por cuarta vez consecutiva.** `HU-39` invalidó el 12, `HU-40` el 13, `HU-41` el 14 y esta el 15. Es coherente con ADR-019, pero acumula: quien exportó bajo `HU-41` tampoco podrá restaurar. Al cerrar la partición conviene decidir si el formato vuelve a ser compatible hacia atrás o si la beta asume que un respaldo solo vale dentro de una misma versión.

**`registerSet` gana su segundo efecto colateral persistido.** Ya escribía `exercise_progression`; ahora escribe también `exercise_one_rm`. Dos es todavía legible; el tercero pedirá extraer la orquestación del repositorio. Queda anotado como deuda que esta historia no paga pero sí acerca.

**Ningún test de UI instrumentado, igual que en `HU-40` y `HU-41`.** La pantalla nueva se cubre por ViewModel y por caso de uso; el ensamblado visual de las tarjetas con chips queda en validación manual.

**La DoD pide verificar un negativo.** *«Ningún componente del motor de decisión, alerta o KPI lee el 1RM»* no es afirmable con un test unitario en este proyecto. T27 lo convierte en una comprobación de revisión con criterio explícito y acotado, que es lo más fuerte que se puede sostener sin introducir infraestructura nueva.

---

### Validación manual (no automatizable)

1. **Instalación fresca.** Desinstalar e instalar: debe abrir sin excepción de Room. Sobre una base v22 sin desinstalar debe fallar — es ADR-019, no un defecto.
2. **La entrada existe desde el primer minuto.** Con la app recién instalada y cero sesiones, el panel de Métricas ya muestra la entrada al 1RM, y al tocarla aparece el **estado vacío explicativo** que enuncia *10 repeticiones con RIR 1* — no una lista en blanco ni un error.
3. **La entrada no se propaga.** Inicio sin tarjeta de 1RM; la barra inferior sigue con cinco pestañas.
4. **La primera serie que califica.** Registrar `Elevación Lateral` con `Mancuerna`, 12.0 kg × 10 · RIR 1. Volver a Métricas → 1RM: aparece la tarjeta con **16.0 kg · 35.3 lb**, ambas unidades a la vez y sin conmutador.
5. **Las que no califican no dejan rastro.** En el mismo par, registrar 20 kg × 9 · RIR 1 y 20 kg × 10 · RIR 0. El valor sigue en 16.0 kg — dos series más pesadas que no lo mueven es exactamente el comportamiento pedido.
6. **El récord no baja.** Registrar 11.0 kg × 10 · RIR 1: sigue en 16.0 kg. Registrar 13.0 kg × 10 · RIR 1: sube a 17.3 kg.
7. **Varias en la misma sesión.** En una sola sesión, series de 11.5 y 12.0 kg ambas a 10 · RIR 1: gana la de 12.0.
8. **El par, no el ejercicio.** Entrenar `Elevación Lateral` también con `Polea` hasta que califique: la tarjeta muestra **dos chips**, y tocar el segundo cambia el número **en su sitio**, sin navegar.
9. **El par sin serie que califique no aparece.** Entrenar ese mismo ejercicio con `Máquina` sin llegar nunca a 10 · RIR 1: `Máquina` **no** aparece como chip, ni con cero ni con guion.
10. **Las cuatro opciones de `Dominadas`.** Con `Peso Corporal`, `Barra Fija` y `Máquina` a 10 · RIR 1 no aparece nada. Con `Peso Añadido` y 10 kg de lastre a 10 · RIR 1 aparece **13.3 kg**, calculado sobre el lastre — el peso corporal no se suma.
11. **Isométricos, fuera.** Un ejercicio isométrico a 10 · RIR 1 no crea ninguna tarjeta.
12. **Un solo implemento, sin selección.** Un ejercicio entrenado con un único implemento muestra su valor directamente, con el implemento como etiqueta y no como chip pulsable.
13. **Orden alfabético.** Con cuatro o cinco ejercicios en la lista, el orden es por nombre de ejercicio, no por fecha ni por valor.
14. **Descarga.** Durante una descarga activa, una serie que cumpla 10 · RIR 1 con carga reducida no deteriora ningún récord.
15. **El 1RM no decide nada.** Tras varios récords altos, comprobar que la carga prescrita del siguiente entrenamiento, las alertas y los KPIs son idénticos a los que serían sin la historia.
16. **Respaldo.** Exportar y comprobar `schemaVersion: 16` y la presencia de `exercise_one_rm` con sus filas. Restaurar: los valores se reproducen **tal cual**, sin recalcularse. Restaurar un respaldo v15: rechazo con mensaje explícito, sin importar nada.
