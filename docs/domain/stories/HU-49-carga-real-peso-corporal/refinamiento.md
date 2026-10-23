## Refinamiento Técnico (Developer)
**Autor**: esteban.colorado | **Fecha**: 2026-10-23

---

### Contexto

**Arquitectura:** MVVM con capa de dominio explícita (ADR-05): UI → ViewModel → Use Case → Repository → DAO/Room. Las reglas son Kotlin puro en `domain/rules` (ADR-06). El esquema cambia sin migración durante la beta (ADR-019).

**Feature análoga: `HU-48`, peso corporal en Métricas, con archivos agregados el 2026-10-23** (`BodyWeightRule`, `GetBodyWeightSummaryUseCase`, `MetricsViewModel.saveWeight` + `MetricsEvent.WeightSaved`, `BodyWeightRuleTest`, `PesoCorporalEnMetricasTest`). Es la más reciente y la que define cómo el sistema lee hoy el historial de peso corporal:

- el peso vigente es la última entrada, con desempate por `id DESC` (`WeightRecordDao`, `BodyWeightRule.reference`);
- un campo con guardado explícito, validación en el caso de uso (`Result`) y confirmación por snackbar a través de un evento one-shot.

Complementos, por la parte que cada uno ya resolvió:

- **`HU-47` (2026-10-22), corrección de la última serie:** `SessionRepositoryImpl.rebuildOneRm`. Reconstruye el 1RM de un par desde sus series de referencia; es la base del recálculo de CA-49.08.
- **`HU-46` (2026-10-22), categoría de movimiento:** atributo nuevo del ejercicio, editable en la ficha (`UpdateExerciseMovementCategoryUseCase`), con subida de esquema y de formato de respaldo, y mensaje de rechazo propio para el formato anterior.
- **`HU-42` (2026-09-11), 1RM estimado:** `OneRmRule`, `exercise_one_rm`, `GetOneRmListUseCase`, `ui/onerm/*`.

Candidatas descartadas:

- **Por más antiguas:** `HU-32`, dificultad de progresión (2026-02-16, la que cita la historia). Además, persiste al instante, sin botón; la historia pide guardado explícito.

#### Hallazgos que deciden el diseño

**1. El tonelaje tiene dos fuentes en el código, y las dos se cambian.**

- `ExerciseSetDao.getTonnageDataBySessionIds` → `TonnageRule`. Alimenta Volumen (G2), Tendencia (G3), la evolución del tonelaje, la evaluación de la alerta `TONNAGE_DROP` (cierre de sesión) y su detalle (H2).
- `SUM(es.weight_kg * es.reps)` en SQL dentro de `SessionDao.getSessionSummaryInfo` y `getClosedSessionsWithSummary`. Alimenta el resumen post-sesión (E5), el historial y el detalle de sesión (HU-17) y el tonelaje semanal del gráfico de peso de HU-48 (G1).

El cálculo de la carga real pasa a Kotlin, en **una sola regla**. El SQL agregado se retira: la regla de implementos compara nombres del catálogo, como `ExternalLoadRule`, y duplicarla en tres consultas SQL es justo la segunda copia que nadie detecta.

**2. La carga real es una regla pura nueva: `RealLoadRule`.**

```
carga real = carga externa + peso vigente × porcentaje / 100
```

Solo aplica si se cumplen las tres condiciones; en cualquier otro caso, carga real = carga externa (Regla 4, CA-49.07):

- el porcentaje es mayor que 0;
- el ejercicio no es isométrico;
- el implemento suma peso corporal.

Implementos que suman, por **lista explícita** (Regla 3): Peso Corporal, Barra Fija, Peso Añadido, Mancuerna, Barra, Barra EZ, Máquina Smith, Pesa Rusa, Balón Medicinal, Paralelas/Dip Station y Rodillo de Abdomen. Un nombre desconocido no suma. Es lo seguro, porque deja el tonelaje como hoy.

Sin peso vigente en la fecha (ninguna entrada ≤ fecha), la carga real es la externa. La regla también valida el porcentaje: entero de 0 a 100, sin decimales ni vacío (CA-49.05).

**3. El peso vigente por fecha se resuelve en SQL, con el mismo orden que `WeightRecordDao.getLatestWeight`.**

```sql
(SELECT wr.weight_kg FROM weight_record wr
 WHERE wr.date <= s.date
 ORDER BY wr.date DESC, wr.id DESC LIMIT 1)
```

Es la contraparte SQL de `BodyWeightRule.reference` (HU-48). Cumple la Regla 1 y CA-49.06: la entrada más reciente con fecha ≤ la de la sesión y, si hay varias ese día, la última. Un peso nuevo no cambia el pasado, porque solo cuenta para fechas ≥ la suya.

Se resuelve en la consulta, y no cargando el historial en cada repositorio, porque así no hay que tocar los constructores de `MetricsRepositoryImpl`, `AlertRepositoryImpl` ni `SessionRepositoryImpl`. Las fechas son `TEXT` ISO `YYYY-MM-DD` en las dos tablas, así que se comparan como texto.

**4. Las filas de tonelaje traen los ingredientes y la regla decide.**

- `SetTonnageData` gana `bodyWeightKg`, `bodyWeightPercent`, `isIsometric` y `equipmentTypeName`. Los 5 sitios que arman `SetForTonnage` pasan a usar `realLoadKg` en lugar de `weightKg`. Son `GetTonnageByMuscleGroupUseCase`, `GetTonnageEvolutionUseCase`, `GetMuscleGroupTrendUseCase`, `AlertRepositoryImpl` y `SessionRepositoryImpl.evaluateTonnageDrop`.
- Los totales de sesión salen de una consulta nueva por serie (`RealLoadSetData`, con los mismos ingredientes), que agrupa por sesión en Kotlin. Hay dos variantes: una para una sesión y otra para todas las cerradas.
- La variante de todas las cerradas filtra por estado y no con `IN (:ids)`, para no chocar con el límite de 999 parámetros de SQLite en API 26.
- `SessionSummaryInfo` y `ClosedSessionDto` pierden `totalTonnageKg`. Así ninguna lectura vuelve a tomar la cifra vieja por accidente.

**5. El 1RM se reconstruye siempre desde las series de referencia, con la carga real.**

Hoy el 1RM se escribe de forma incremental al registrar (`getValue` + «estrictamente mayor»), y se reconstruye al corregir (`getMaxQualifyingWeightForPair`, que toma el `MAX(weight_kg)`). Con carga real, ese `MAX` en SQL deja de valer: dos series con el mismo lastre en fechas con distinto peso corporal tienen cargas reales distintas.

Diseño:

- **Regla pura** `OneRmRule.record(candidates)`: del conjunto de series de referencia (10 repeticiones, RIR 1) de un par, devuelve el récord **y su fecha**.
  - Una serie califica si su carga real es mayor que 0 y además el implemento captura carga externa o suma peso corporal.
  - Con esto, Peso Corporal y Barra Fija califican cuando el porcentaje es mayor que 0. Peso Añadido calcula sobre cuerpo + lastre. Dominadas en Máquina (asistida) sigue sin 1RM, porque ni captura carga ni suma peso corporal (CA-49.02, CA-49.07).
  - En empate gana la fecha más antigua: es «la sesión en que se alcanzó», la misma semántica de «estrictamente mayor» de CA-42.04.
- **Ayudante de datos** `OneRmRecordWriter` (clase simple en `data/repository`, sin Hilt, construida con `ExerciseSetDao` y `ExerciseOneRmDao`). Lee los candidatos con su peso vigente y escribe o borra el récord. Ofrece tres operaciones:
  - reconstruir un par;
  - reconstruir todos los pares de un ejercicio;
  - reconstruir los pares con series de referencia desde una fecha.
- **Quién la usa:**
  - **Registrar una serie:** si la serie tiene 10 repeticiones y RIR 1, reconstruye su par. Sustituye a la escritura incremental: el resultado es el mismo máximo, pero la fecha y la carga real salen de un solo sitio. Sigue dentro de la transacción de la serie.
  - **Corregir la última serie (HU-47):** reconstruye su par, igual que hoy.
  - **Cambiar el porcentaje (CA-49.08):** reconstruye todos los pares del ejercicio en la transacción que guarda el porcentaje. Puede subir, bajar, aparecer o desaparecer. Guardar el mismo valor no cambia nada (QA-12, paso 4).
  - **Registrar un peso corporal:** reconstruye los pares con series de referencia **desde la fecha del registro**. Es el único caso en que un peso nuevo toca algo ya registrado: una serie de referencia del mismo día, registrada antes que el peso. Sin esto, su 1RM quedaría calculado con el peso anterior, mientras el tonelaje y la fuerza relativa (que se leen al consultar) ya usan el nuevo. Lo anterior a esa fecha no se toca (CA-49.06).
- `getMaxQualifyingWeightForPair` se retira; lo sustituye la consulta de candidatos.

**6. `exercise_one_rm` gana `achieved_on`, y la fuerza relativa se calcula al leer.**

- `achieved_on TEXT NOT NULL`: la fecha de la sesión de la serie que fijó el récord (CA-49.02).
- `ExerciseOneRmDao.getAll` trae además el peso vigente en `achieved_on`, con la misma subconsulta del Hallazgo 3.
- `OneRmRule.relativeStrength(oneRm, bodyWeight)` = 1RM ÷ peso, o nulo sin peso. Es el único cálculo de la fuerza relativa.
- La tarjeta de O1 muestra «N.NN× tu peso corporal» y «con X Kg el DD/MM/AAAA». Sin peso vigente en esa fecha, esas dos líneas no se muestran, en lugar de inventar un valor.
- La fuerza relativa no se persiste. Es un derivado: el récord y el historial de peso ya viajan en el respaldo.

**7. `exercise.body_weight_percent INTEGER NOT NULL DEFAULT 0`.**

- **Seed:** Dominadas 100, Sentadilla Búlgara 85, Zancadas (Lunges) 85, Crunch Abdominal 35; el resto 0 (CA-49.04).
- **Ejercicios creados por el ejecutante:** reciben el default 0. `CreateExerciseUseCase` no cambia.
- **Edición:** `UpdateExerciseBodyWeightPercentUseCase(exerciseId, input)` valida con `RealLoadRule.parsePercent` y devuelve `Result`. Un valor inválido no escribe (CA-49.05). Si es válido, `ExerciseRepository.updateBodyWeightPercent` guarda y reconstruye los 1RM del ejercicio en una sola transacción (Hallazgo 5).

**8. Esquema 26 → 27 sin migración, y formato de respaldo 18 → 19.**

- `@Database(version = 27)`, sin migración (ADR-019). `LAST_MIGRATED_VERSION` sigue en 19, y el KDoc de `Migrations` lo registra. El build genera `schemas/27.json`.
- El formato de respaldo sube a **19**, con el mismo criterio que HU-46. `exercise_one_rm.achieved_on` es `NOT NULL` y no se deriva de un respaldo 18: el 18 guarda el valor del récord, no cuándo se alcanzó. El porcentaje también es una decisión del ejecutante.
- **Rechazo de formatos anteriores:**
  - el 18 se rechaza con un mensaje propio, que nombra lo que le falta;
  - el 17 conserva el suyo (categoría de movimiento);
  - los más antiguos siguen cayendo por el equipamiento.
- La historia descarta la compatibilidad hacia atrás («Sin migración»).
- Las dos columnas viajan solas en el respaldo, porque el volcado es por cursor (CA-49.10).

**9. El motor de decisión no se toca (CA-49.09).**

No se modifica ninguna de estas piezas:

- reglas: `HypertrophyRangeRule`, `ProgressionClassificationRule`, `PlateauThresholdRule`, `PlateauCausalAnalysisRule`, `DeloadNeedRule`, `DeloadLoadRule`, `PrefilledLoadRule`, `ActionSignalRule`, `RoutineFatigueRule`;
- en `SessionRepositoryImpl`: `resolvePrefilledLoad`, `rangeNotice` y la clasificación al cierre.

La serie sigue guardando la carga externa tecleada (`persistedWeightKg`). Solo cambian las lecturas de tonelaje y el 1RM. La evaluación de `TONNAGE_DROP` sí cambia, porque cambia el tonelaje mismo (Regla 5, RF-57).

**10. UI.**

- **D2:** campo «Peso corporal movido» (sufijo %, teclado numérico) con botón «Guardar».
  - Mientras el valor no es válido se ve el error en línea «Introduce un número entero entre 0 y 100» y el botón queda deshabilitado (preview).
  - Al guardar aparece el snackbar «Peso corporal movido actualizado».
  - El campo no se muestra en ejercicios isométricos.
  - El estado del formulario vive en un `StateFlow` aparte, como `categoryConflict`, porque no es un dato persistido. La confirmación es un `SharedFlow` de eventos, como `MetricsEvent.WeightSaved`.
- **O1:** dos líneas nuevas bajo el valor en Kg y Lb. El formato numérico es el de la pantalla (`"%.2f"`, como el `"%.1f"` del 1RM).
- **E5, G1, G2, G3 y H2:** sin cambio de diseño.

**11. Medición en paralelo omitida.** No existe `docs/cosmic/measurement-strategy.json` con `status: APPROVED` (COMPUERTA 2 de §2c). No se adelanta la medición: se medirá al cierre, o quedará pendiente.

---

### Reutilización

| Componente | Decisión | Motivo |
|------------|----------|--------|
| `domain/rules/ExternalLoadRule.isCaptureEnabled` | Se reutiliza | T5, T9: sigue decidiendo si la serie captura carga externa; es una de las dos condiciones con las que una serie califica para 1RM |
| `domain/rules/OneRmRule.qualifies` / `estimate` / `REFERENCE_REPS` / `REFERENCE_RIR` | Se reutiliza y extiende | T4: misma fórmula y misma serie de referencia (Regla 6); gana `record(candidates)` y `relativeStrength` |
| `domain/rules/TonnageRule.calculateForMuscleGroup` | Se reutiliza | T7: recibe `SetForTonnage` armado con la carga real; la agregación por grupo no cambia |
| `SessionRepositoryImpl.rebuildOneRm` (HU-47) | Se reemplaza por `OneRmRecordWriter` | T6: el `MAX(weight_kg)` en SQL deja de ser el máximo con carga real, y la reconstrucción la necesitan tres repositorios |
| Orden `date DESC, id DESC` de `WeightRecordDao.getLatestWeight` (HU-48) | Se reutiliza | T3, T6, T10: misma regla de peso vigente, aplicada a una fecha |
| `ui/catalog/ExerciseDetailViewModel` / `ExerciseDetailScreen` | Se extiende | T13, T14: el atributo se edita en la ficha, junto a la dificultad (HU-32) y la categoría (HU-46) |
| Patrón evento + snackbar de `MetricsViewModel` (HU-48) | Se replica | T13, T14: confirmación breve al guardar |
| `ui/onerm/OneRmScreen` / `GetOneRmListUseCase` / `OneRmEntry` / `OneRmEquipment` | Se extiende | T10, T11, T15: la fuerza relativa va en la tarjeta existente |
| `BackupRepositoryImpl` (volcado por cursor) | Se reutiliza | T17: las columnas nuevas viajan solas; solo cambia la versión del formato y el mensaje de rechazo |
| `domain/rules/RealLoadRule` | Se crea una vez | T2. La usan T4–T7 (tonelaje y 1RM), T12 (validación) y T13 (habilitar Guardar). No hay equivalente: `ExternalLoadRule` decide si se teclea un peso, no cuánta carga se mueve |
| `domain/model/RealLoadSetData` | Se crea | T3, T8: fila por serie con los ingredientes de la carga real, para los totales de sesión. `SetTonnageData` trae el grupo muscular y multiplica la serie por zona |
| `data/repository/OneRmRecordWriter` | Se crea una vez | T6. Lo usan T9 (registro y corrección), T12 (cambio de porcentaje) y T16 (registro de peso): la reconstrucción del récord en un solo sitio |
| `domain/usecase/catalog/UpdateExerciseBodyWeightPercentUseCase` | Se crea | T12: no hay caso de uso que edite el porcentaje; base `UpdateExerciseProgressionDifficultyUseCase` |

### Estándares aplicables

| Regla | Sección del estándar | Aplica a |
|-------|----------------------|----------|
| Código, KDoc y comentarios en inglés; strings de UI en español, en `res/values/strings.xml`; datos semilla en español | §2.1, §3.6 | Todas las tareas |
| Use Case `{Acción}{Entidad}UseCase` con `operator fun invoke` | §2.3, §2.4 | T12 |
| Regla pura en `domain/rules`, sin Room ni Android, testeable con JUnit 4 sin emulador; cada regla con su archivo de tests | §4.5, §5.5 | T2, T4, T18, T19 |
| Domain no importa `android.*` / `androidx.*` / `com.google.*` | §3.2, §4.1 | T2–T4, T11, T12 |
| Repository: interfaz en Domain, implementación en Data; el ViewModel no accede a DAOs | §4.1 | T12, T13 |
| Validaciones de dominio en el Use Case, con mensaje interno en inglés (`"Body weight percent must be ..."`); la UI solo habilita o deshabilita y muestra el texto en español | §2.1, §4.1, §4.4 | T12–T14 |
| Use Case devuelve `Result<T>`; el ViewModel lo mapea a estado observable, sin excepciones sin capturar | §4.4 | T12, T13 |
| ViewModel: `@HiltViewModel` + `@Inject constructor`; `MutableStateFlow` privado → `StateFlow`; eventos one-shot con `MutableSharedFlow` → `SharedFlow`; sin `mutableStateOf` | §3.5, §4.2 | T13 |
| Screen: el contenido recibe datos inmutables y lambdas; eventos recolectados en `LaunchedEffect` | §3.4 | T14, T15 |
| Nombres de columna Room en snake_case; booleanos `INTEGER NOT NULL DEFAULT 0`; fechas `TEXT` ISO `YYYY-MM-DD` | §3.6, `domain_and_state_model.md` §1 | T1 |
| Prepopulación por Seeder con datos en código | §4.7 | T1 |
| Room/DAO main-safe (las suspend de Room ya lo son); escrituras compuestas en `withTransaction` | §4.3 | T6, T9, T12, T16 |
| `!!` solo en tests; sin wildcards; trailing commas; 4 espacios; ≤ 120 columnas; `when` con 3+ ramas | §3.1–§3.3 | Todas |
| Tests `given …, when …, then …` con Given/When/Then explícitos; Use Cases y ViewModels con dependencias mockeadas (mockk) | §5.2–§5.4 | T18–T23 |
| Touch target ≥ 48 dp, márgenes 16 dp, `OutlinedTextField` radio 4 dp | §6.3 | T14 |
| Instrumentados: nombres en `snake_case` (DEX < 040 con minSdk 26 rechaza espacios), como en `CorrectLastSetTest` | §5.3 (convención del proyecto) | T24 |

### Tareas de Implementación

#### Fase 1 — Esquema y semilla (CA-49.04, CA-49.10)
- [x] **T1: `exercise.body_weight_percent` (default 0) y `exercise_one_rm.achieved_on` (NOT NULL); esquema 26 → 27 sin migración; seed con 100/85/85/35** — `data/local/entity/ExerciseEntity.kt`, `ExerciseOneRmEntity.kt`, `data/local/database/TensionDatabase.kt`, KDoc de `Migrations.kt`, `data/local/seed/model/SeedExercise.kt`, `ExerciseCatalog.kt`, `ExerciseSeeder.kt` (Base: subida de HU-46)

#### Fase 2 — Carga real y 1RM en dominio y datos (CA-49.01, 02, 06, 07, 08)
- [x] **T2: Regla pura `RealLoadRule`** (implementos que suman, `appliesBodyWeight`, `realLoadKg`, `parsePercent`, límites 0–100) — `domain/rules/RealLoadRule.kt` (Base: `ExternalLoadRule`, `BodyWeightRule`)
- [x] **T3: Ingredientes de la carga real en las filas** (`SetTonnageData` + 4 campos y `realLoadKg`; nuevo `RealLoadSetData`) y consultas con el peso vigente por fecha (`getTonnageDataBySessionIds`, `getRealLoadSetsBySession`, `getRealLoadSetsOfClosedSessions`) — `domain/model/SetTonnageData.kt`, `domain/model/RealLoadSetData.kt`, `data/local/dao/ExerciseSetDao.kt`
- [x] **T4: `OneRmRule.record(candidates)` (récord + fecha, desempate por la más antigua), calificación con carga real y `relativeStrength`** — `domain/rules/OneRmRule.kt`, `domain/model/OneRmCandidate.kt`
- [x] **T5: Consulta de candidatos de 1RM por par (con fecha y peso vigente) y de pares con series de referencia (por ejercicio y desde una fecha); se retira `getMaxQualifyingWeightForPair`** — `data/local/dao/ExerciseSetDao.kt`
- [x] **T6: `OneRmRecordWriter`** (reconstruir par, ejercicio, desde fecha) — `data/repository/OneRmRecordWriter.kt` (Base: `SessionRepositoryImpl.rebuildOneRm`)
- [x] **T7: Los 5 consumidores de `SetForTonnage` usan la carga real** — `domain/usecase/metrics/GetTonnageByMuscleGroupUseCase.kt`, `GetTonnageEvolutionUseCase.kt`, `GetMuscleGroupTrendUseCase.kt`, `data/repository/AlertRepositoryImpl.kt`, `data/repository/SessionRepositoryImpl.kt` (`evaluateTonnageDrop`)
- [x] **T8: Totales de sesión con carga real** (resumen, historial, detalle); `SessionSummaryInfo` y `ClosedSessionDto` sin `totalTonnageKg` — `data/local/dao/SessionDao.kt`, `data/repository/SessionRepositoryImpl.kt`
- [x] **T9: Registro y corrección de serie reconstruyen el 1RM con `OneRmRecordWriter`** — `data/repository/SessionRepositoryImpl.kt`
- [x] **T10: `ExerciseOneRmDao.getAll` con `achieved_on` y peso vigente; `upsert` con fecha** — `data/local/dao/ExerciseOneRmDao.kt`, `data/repository/OneRmRepositoryImpl.kt`, `domain/model/OneRmEntry.kt`
- [x] **T11: `GetOneRmListUseCase` arma `OneRmEquipment` con fecha, peso y fuerza relativa** — `domain/usecase/onerm/GetOneRmListUseCase.kt`, `domain/model/ExerciseOneRm.kt`

#### Fase 3 — Porcentaje editable y peso nuevo (CA-49.04, 05, 06, 08)
- [x] **T12: `UpdateExerciseBodyWeightPercentUseCase` + `ExerciseRepository.updateBodyWeightPercent` (guarda y reconstruye 1RM en una transacción); `Exercise.bodyWeightPercent` mapeado desde las 3 consultas de `ExerciseDao` y desde `PlanRepositoryImpl`** — `domain/usecase/catalog/UpdateExerciseBodyWeightPercentUseCase.kt`, `domain/repository/ExerciseRepository.kt`, `data/repository/ExerciseRepositoryImpl.kt`, `data/local/dao/ExerciseDao.kt`, `domain/model/Exercise.kt`, `data/repository/PlanRepositoryImpl.kt` (Base: `UpdateExerciseProgressionDifficultyUseCase`)
- [x] **T13: Estado del formulario (valor, error, habilitado), `onBodyWeightPercentChanged`, `saveBodyWeightPercent`, evento de confirmación; `isIsometric` y porcentaje en `ExerciseDetailItem`** — `ui/catalog/ExerciseDetailViewModel.kt`, `ExerciseDetailUiState.kt` (Base: `MetricsViewModel.saveWeight`)
- [x] **T14: Campo «Peso corporal movido» con sufijo %, ayuda, error en línea, botón Guardar y snackbar; oculto en isométricos** — `ui/catalog/ExerciseDetailScreen.kt`, `res/values/strings.xml`
- [x] **T15: Tarjeta de 1RM con «N.NN× tu peso corporal» y «con X Kg el DD/MM/AAAA»** — `ui/onerm/OneRmScreen.kt`, `res/values/strings.xml`
- [x] **T16: Registrar un peso reconstruye los 1RM con series de referencia desde su fecha** — `data/repository/ProfileRepositoryImpl.kt` (Hallazgo 5)

#### Fase 4 — Respaldo (CA-49.10)
- [x] **T17: Formato 18 → 19; el 18 se rechaza con mensaje propio y el 17 conserva el suyo** — `data/repository/BackupRepositoryImpl.kt`, `res/values/strings.xml`

#### Fase 5 — Tests
- [x] **T18: `RealLoadRuleTest`** (los tres ejemplos de CA-49.01, tabla de implementos, 0 %, isométrico, sin peso vigente, `parsePercent`) — `test/.../domain/rules/RealLoadRuleTest.kt` (cubre: `QA-01`, `QA-02`, `QA-03`, `QA-11`)
- [x] **T19: `OneRmRuleTest` ampliado** (récord con carga real, Barra Fija y Peso Corporal califican, asistida no, máximo con fecha y desempate, fuerza relativa) — `test/.../domain/rules/OneRmRuleTest.kt` (cubre: `QA-07`, `QA-08`, `QA-09`, `QA-12`, `QA-15`)
- [x] **T20: Tonelaje con carga real en Volumen, Evolución y Tendencia** — `test/.../domain/usecase/metrics/GetTonnageByMuscleGroupUseCaseTest.kt` (nuevo), `GetTonnageEvolutionUseCaseTest.kt` (nuevo) (cubre: `QA-04`, `QA-05`, `QA-06`)
- [x] **T21: `GetOneRmListUseCaseTest` y `OneRmViewModelTest` con fecha, peso y fuerza relativa** — `test/.../domain/usecase/onerm/GetOneRmListUseCaseTest.kt`, `test/.../ui/onerm/OneRmViewModelTest.kt` (cubre: `QA-07`, `QA-09`)
- [x] **T22: `UpdateExerciseBodyWeightPercentUseCaseTest` y `ExerciseCatalogTest` (valores precargados)** — `test/.../domain/usecase/catalog/UpdateExerciseBodyWeightPercentUseCaseTest.kt`, `test/.../data/local/seed/ExerciseCatalogTest.kt` (cubre: `QA-10`, `QA-11`, `QA-12`)
- [x] **T23: `BackupRepositoryImplTest`: formato 19 aceptado, 18 y 17 rechazados con su mensaje** — `test/.../data/repository/BackupRepositoryImplTest.kt` (cubre: `QA-16`)
- [x] **T24: Instrumentado `CargaRealPesoCorporalTest`** (base real: tonelaje de sesión e historial con carga real, peso vigente por fecha, 1RM de Barra Fija con fecha, asistida sin 1RM, cambio de porcentaje que recalcula, carga precargada externa, ida y vuelta del respaldo); ajustar los constructores de repositorios en los instrumentados existentes — `androidTest/.../CargaRealPesoCorporalTest.kt`, `PesoCorporalEnMetricasTest.kt` (Base: `MiUnRmEstimadoTest`, `PesoCorporalEnMetricasTest`; cubre: `QA-01`, `QA-02`, `QA-04`, `QA-06`, `QA-08`, `QA-12`, `QA-13`, `QA-14`, `QA-16`). Se compila con `assembleDebugAndroidTest`; ejecutarlo requiere un dispositivo
- [x] Pruebas existentes que se ajustan por firma o por el 1RM con carga real: `OneRmRuleTest`, `MiUnRmEstimadoTest`, `CorrectLastSetTest`, `GetCorrectableSetUseCaseTest` u otras que fallen al compilar. `QA-13` (motor sin cambios) se cubre por construcción (Hallazgo 9), por la suite existente del motor sin cambios y por la aserción de carga precargada externa de T24

#### Fase 6 — Documentación (DoD)
- [x] **T25: `exercise.body_weight_percent`, `exercise_one_rm.achieved_on`, versión de esquema 27, carga real como derivado no persistido** — `docs/architecture/domain_and_state_model.md`
- [x] **T26: `D2-T1` (porcentaje editable), `O1-T1` (fuerza relativa), los triggers de tonelaje con carga real y el formato de respaldo 19** — `docs/architecture/interfaces_contract.md`
- [x] **T27: ADR-019, esquema 27 y formato 19** — `docs/architecture/architecture_blueprint.md`
- [x] **T28: RF-57 lee la carga real** — `docs/domain/definition/requirements_specification.md`
- [x] **T29: HU-49 y sus relaciones** — `docs/domain/stories/story_mapping_index.md`
