## Dev Agent Record — Dev-Rápido

### Debug Log

| # | Tipo | Descripción | Resolución |
|---|------|-------------|------------|
| 1 | Compilación | `RegisterSetScreen`: el botón «Cancelar» seguía pasando `onNavigateBack` como `() -> Unit` después de cambiar la firma a `(String?) -> Unit`. | Se invoca `onNavigateBack(null)`. |
| 2 | Test | `SettingsViewModelTest` esperaba el default 5 del umbral base. | Se actualiza a 10: es el cambio que pide CA-46.07, no una regresión. |
| 3 | Test | El test nuevo del aviso devuelto por `navigateBack` no recibía la emisión: el colector, en `StandardTestDispatcher`, se suscribía después de emitir. | El colector se lanza con `UnconfinedTestDispatcher(testScheduler)`. |
| 4 | Entorno | `JAVA_HOME` apuntaba a un JDK 1.7 y Gradle no arrancaba. | Los comandos se corrieron con `JAVA_HOME` en el JDK 17 de Temurin, solo para la llamada. |
| 5 | Test instrumentado | `AjustarSesionDelDiaTest` y `MiUnRmEstimadoTest` insertan ejercicios con SQL crudo, y `movement_category` es `NOT NULL` sin default. Además, el ejercicio «fuera de la sesión» podía ser de otra categoría. | Los INSERT llevan la categoría y la consulta del ayudante filtra por la del día (Empuje). |
| 6 | UI (revisión visual del PO) | En la pestaña Plan, la etiqueta de categoría compartía fila con «1 versión» y el texto se partía letra a letra. En `TrainingPlanScreen` iba en la misma fila que el nombre y lo recortaba. | La etiqueta pasa a su propia línea en las dos pantallas y «N versiones» queda en una sola línea. El nombre se sigue recortando a 2 líneas: lo estrechan las 4–5 acciones de la derecha, y eso es anterior a esta HU. |

### Completion Notes

- ⚡ Dev-Rápido: **HU-46**, tres frentes con un solo esquema nuevo (25).
  - **Progresión por rango 8–12.** `HypertrophyRangeRule` sustituye a `DoubleThresholdRule`, que se retira. La sesión del par se lee por la mayoría estricta de las series registradas, sin series del plan ni RIR. «Bajar peso» es inmediato. La subida llega tras 4 sesiones consecutivas en el tope con el mismo peso de referencia (el mayor). El avance «N de 4» se ve en el resumen y junto al peso propuesto. El aviso en directo va en un `Snackbar` de la sesión y sobre el formulario de la serie siguiente. Peso corporal: nunca «bajar», y «añadir peso extra» solo si el ejercicio admite «Peso Añadido». Isométricos y descarga quedan fuera, con el conteo congelado.
  - **Umbral base de meseta por defecto en 10** (10 / 15 / 20).
  - **Categoría Empuje / Jalón / Pierna** como dato del ejercicio y de la rutina.
    - Las semillas tienen las 38 + 6 clasificaciones.
    - La categoría es obligatoria al crear.
    - El ejercicio creado desde la sesión hereda la categoría del día.
    - La hoja de añadir y los selectores del plan se filtran por categoría, con guarda en dominio y datos.
    - Los cambios de categoría que contradicen el plan se rechazan nombrando el conflicto.
  - **Respaldo** en formato 18.
  - **Ninguna pantalla nueva.**
- 🧪 Cobertura QA: 32/36. Pendientes:
  - QA-16 (Alta): cubierto por `ProgresionPorRangoYCategoriaTest` (instrumentado, compila) y por `ActiveSessionViewModelTest`. El filtro vive en SQL y su ejecución necesita emulador.
  - QA-15 (Media): la categoría del día sale de la rutina de la sesión, y la reasignación ya se agotó al elegir la versión. Sin test propio.
  - QA-31 (Media): una sesión sin series del par no entra al bucle de evaluación. Sin test propio.
  - QA-17 (Baja): estado vacío de la hoja; validación visual.
- ⚠️ El test instrumentado `ProgresionPorRangoYCategoriaTest` (8 casos) **compila pero no se ejecutó**: requiere emulador. Queda en la validación manual de `refinamiento.md`.
- 📝 `story_mapping_index.md` no lista `HU-44` ni `HU-45`: es una inconsistencia anterior a esta HU. Se añadió solo `HU-46`, y no se tocaron los conteos del §9.

### File List

| Acción | Archivo | Descripción |
|--------|---------|-------------|
| Creado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/domain/model/MovementCategory.kt` | Enum Empuje / Jalón / Pierna |
| Creado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/domain/rules/HypertrophyRangeRule.kt` | Regla pura: lectura, conteo, señal y aviso en directo |
| Creado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/domain/rules/MovementCategoryRule.kt` | Regla pura: compatibilidad y conflictos de categoría |
| Eliminado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/domain/rules/DoubleThresholdRule.kt` | Retirada por CA-46.06 |
| Creado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/domain/usecase/catalog/UpdateExerciseMovementCategoryUseCase.kt` | Cambio de categoría del ejercicio con conflicto |
| Creado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/domain/usecase/routine/UpdateRoutineMovementCategoryUseCase.kt` | Cambio de categoría de la rutina con conflicto |
| Creado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/domain/usecase/session/GetSessionMovementCategoryUseCase.kt` | Categoría del día |
| Creado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/ui/catalog/components/MovementCategorySelector.kt` | Selector segmentado de 48 dp, sin preselección y fijable |
| Creado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/ui/components/MovementCategoryLabel.kt` | Traducción enum → texto y etiqueta |
| Modificado | `.../data/local/entity/ExerciseEntity.kt`, `RoutineEntity.kt` | `movement_category` NOT NULL sin default |
| Modificado | `.../data/local/entity/ExerciseProgressionEntity.kt`, `SessionExerciseProgressionEntity.kt` | Conteo, peso de referencia y lectura de rango |
| Modificado | `.../data/local/entity/ProfileEntity.kt`, `domain/rules/PlateauThresholdRule.kt` | Umbral base por defecto 10 |
| Modificado | `.../data/local/database/TensionDatabase.kt`, `Migrations.kt` | Esquema 25 sin migración (ADR-019) |
| Creado | `Tension/app/schemas/.../TensionDatabase/25.json` | Esquema exportado por el build |
| Modificado | `.../data/local/seed/ExerciseCatalog.kt`, `DefaultPlan.kt`, `ExerciseSeeder.kt`, `PlanSeeder.kt`, `model/SeedExercise.kt`, `model/SeedRoutine.kt` | Categoría de los 38 ejercicios y las 6 rutinas |
| Modificado | `.../data/local/dao/ExerciseDao.kt`, `RoutineDao.kt`, `SessionExerciseDao.kt`, `SessionExerciseProgressionDao.kt` | Categoría, filtros, conflictos y estado de rango |
| Modificado | `.../data/repository/SessionRepositoryImpl.kt` | Cierre con la regla nueva, aviso en directo y sesión acotada a la categoría |
| Modificado | `.../data/repository/ExerciseRepositoryImpl.kt`, `PlanRepositoryImpl.kt`, `RoutineRepositoryImpl.kt` | Categoría en creación, lectura y cambio |
| Modificado | `.../data/repository/BackupRepositoryImpl.kt` | Formato 18; rechazo del 17 |
| Modificado | `.../domain/repository/ExerciseRepository.kt`, `PlanRepository.kt`, `RoutineRepository.kt`, `SessionRepository.kt` | Contratos ampliados |
| Modificado | `.../domain/model/ActionSignal.kt`, `Exercise.kt`, `Routine.kt`, `PlanVersionDetail.kt`, `RegisterSetInfo.kt`, `SessionExerciseDetail.kt` | Señales de rango y categoría |
| Modificado | `.../domain/rules/ActionSignalRule.kt` | La señal de rango manda |
| Modificado | `.../domain/usecase/catalog/CreateExerciseUseCase.kt`, `CreateAndAddExerciseToSessionUseCase.kt`, `routine/CreateRoutineUseCase.kt` | Categoría obligatoria; la del día desde la sesión |
| Modificado | `.../domain/usecase/plan/AssignExerciseToVersionUseCase.kt`, `AddAlternativeToSlotUseCase.kt` | Guarda de categoría |
| Modificado | `.../domain/usecase/session/RegisterSetUseCase.kt`, `GetSessionSummaryUseCase.kt` | Aviso en directo; señal de rango por par |
| Modificado | `.../ui/catalog/CreateExercise*.kt`, `ExerciseDetail*.kt`, `RoutineList*.kt`, `TrainingPlan*.kt`, `PlanVersionDetail*.kt` | Selector, diálogo de conflicto y etiquetas |
| Modificado | `.../ui/session/ActiveSession*.kt`, `RegisterSet*.kt`, `SessionSummaryScreen.kt`, `components/AddExerciseSheet.kt` | Categoría del día, indicación de rango, aviso y señales |
| Modificado | `.../ui/navigation/TensionNavHost.kt`, `NavigationRoutes.kt` | El aviso viaja como resultado de navegación |
| Modificado | `Tension/app/src/main/res/values/strings.xml` | Cadenas de HU-46; retirada la del rechazo del formato 16 |
| Creado | `test/.../domain/rules/HypertrophyRangeRuleTest.kt`, `MovementCategoryRuleTest.kt` | Reglas nuevas |
| Creado | `test/.../usecase/catalog/UpdateExerciseMovementCategoryUseCaseTest.kt`, `routine/UpdateRoutineMovementCategoryUseCaseTest.kt`, `routine/CreateRoutineUseCaseTest.kt`, `plan/AssignExerciseToVersionUseCaseTest.kt` | Casos de uso |
| Eliminado | `test/.../domain/rules/DoubleThresholdRuleTest.kt` | Con su regla |
| Modificado | `test/.../ActionSignalRuleTest.kt`, `PlateauThresholdRuleTest.kt`, `ExerciseCatalogTest.kt`, `DefaultPlanTest.kt`, `BackupRepositoryImplTest.kt`, `CreateExerciseUseCaseTest.kt`, `CreateAndAddExerciseToSessionUseCaseTest.kt`, `AddAlternativeToSlotUseCaseTest.kt`, `GetSessionSummaryUseCaseTest.kt`, `RegisterSetUseCaseTest.kt`, `ActiveSessionViewModelTest.kt`, `RegisterSetViewModelTest.kt`, `SettingsViewModelTest.kt` | Nuevos casos y firmas actualizadas |
| Creado | `androidTest/.../ProgresionPorRangoYCategoriaTest.kt` | Base real: conteo, prescripción, descarga, filtro y umbral |
| Modificado | `androidTest/.../AjustarSesionDelDiaTest.kt`, `MiUnRmEstimadoTest.kt` | INSERT con categoría |
| Modificado | `docs/architecture/domain_and_state_model.md`, `interfaces_contract.md`, `architecture_blueprint.md` | Esquema 25, triggers, reglas, ADR-019, D-07 y D-17 |
| Modificado | `docs/domain/stories/story_mapping_index.md` | HU-46 en §2 y §10 |
| Creado | `docs/domain/stories/HU-46-progresion-rango-categoria-dia/refinamiento.md`, `dev-record.md` | Artefactos de la HU |

### Revisión del diff

Herramienta: propia + revisor de estándares independiente — 106 hallazgos del revisor agrupados por regla. El revisor notó que el árbol cambió durante su lectura (las correcciones de la pasada propia), así que algunos números de línea pueden estar desplazados.

| # | Categoría | Hallazgo | Archivo | Resultado |
|---|-----------|----------|---------|-----------|
| 1 | duplicacion | `registerSet` resolvía «admite Peso Añadido» con su propia consulta, duplicando `pairLoadProfile` | `SessionRepositoryImpl.kt` | ✅ Aplicado |
| 2 | estandares | Líneas de más de 120 caracteres (§3.1) | `CreateAndAddExerciseToSessionUseCaseTest.kt`, `TensionNavHost.kt` y tests nuevos | ✅ Aplicado |
| 3 | limpieza | Import `SessionPairSummaryDto` sin uso en un caso de uso de dominio | `GetSessionSummaryUseCase.kt` | ✅ Aplicado |
| 4 | estandares | Imports fuera de orden (§3.2) | 9 archivos (`SessionRepositoryImpl`, `SessionRepository`, `GetSessionSummaryUseCase`, `ExerciseDetailScreen`, `RoutineListScreen`, `ActiveSessionScreen`, `SessionSummaryScreen`, 2 tests) | ✅ Aplicado |
| 5 | estandares | Más de 2 parámetros en una línea (§3.1) | `HypertrophyRangeRuleTest.kt` | ✅ Aplicado |
| 6 | estandares | Constantes `const val` en el nivel de archivo, fuera de un `object` (§2.5) | `MovementCategorySelector.kt`, `TensionNavHost.kt` | ✅ Aplicado: `SelectorAlpha` y `NavigationRoutes.SET_NOTICE_RESULT_KEY` |
| 7 | estandares | Constante `POLEA` en el nivel de archivo (§2.5) | `AssignExerciseToVersionUseCaseTest.kt` | — No aplicado: es la convención de los tests del repositorio (`MANCUERNA` en `AddAlternativeToSlotUseCaseTest`, `BARRA` en `RegisterSetUseCaseTest`) |
| 8 | estandares | Nombres de test sin `given/when/then` (§5.3) | 4 archivos de test nuevos | ✅ Aplicado |
| 9 | estandares | Nombres de test sin `given/when/then` en tests añadidos a archivos existentes y en el instrumentado | `DefaultPlanTest`, `ExerciseCatalogTest`, `BackupRepositoryImplTest`, `CreateAndAddExerciseToSessionUseCaseTest`, `AddAlternativeToSlotUseCaseTest`, `ActiveSessionViewModelTest`, `ProgresionPorRangoYCategoriaTest` | — No aplicado: cada test sigue la convención de su archivo (español libre en los unitarios, snake_case en los instrumentados). Cambiarla solo en los casos nuevos mezclaría dos estilos en el mismo archivo |
| 10 | estandares | Comentarios y KDoc en español (§2.1); identificadores en español en el instrumentado | ~78 archivos | — No aplicado: la convención real del repositorio desde HU-36 es KDoc en español (`SessionAdjustmentRule`, `ExternalLoadRule`, `RemoveExerciseEquipmentUseCase`…). Traducir solo lo de esta HU dejaría archivos con dos idiomas. Se propone decidirlo a nivel de proyecto |
| 11 | estandares | El ViewModel referencia `Context` para redactar mensajes (§3.5) | `CreateExerciseViewModel.kt`, `ExerciseDetailViewModel.kt`, `RegisterSetViewModel.kt` | — No aplicado: es el patrón vigente de todos los ViewModels con mensajes (`@ApplicationContext` + `getString`). Sustituirlo es un refactor transversal fuera de la HU |
| 12 | estandares | Validaciones con `require` o excepción en lugar de `Result.Error` (§4.4) | `AssignExerciseToVersionUseCase.kt`, `AddAlternativeToSlotUseCase.kt`, `CreateRoutineUseCase.kt`, `SessionRepositoryImpl.kt` | — No aplicado: los casos de uso existentes validan así y la UI captura `IllegalArgumentException`. Cambiar el contrato excede la HU |
| 13 | estandares | Mensaje de validación de dominio en español (§2.1) | `CreateRoutineUseCase.kt` | — No aplicado: el resto de mensajes del mismo caso de uso ya están en español y el diálogo los muestra tal cual |

Revisar primero: `SessionRepositoryImpl.kt` (cierre de sesión y prescripción: un error ahí no lo atrapa la suite JVM, solo el instrumentado que no se ejecutó) · `TensionDatabase.kt` (migración de esquema: 24 → 25 sin migración) · `BackupRepositoryImpl.kt` (contrato del formato de respaldo, 17 → 18) · `CreateRoutineUseCase.kt` (hallazgos #12 y #13 sin aplicar) · `RegisterSetViewModel.kt` (hallazgo #11 sin aplicar)

### Métricas Dev-Rápido

- Tiempo sesión IA: 453 min
- Tareas manuales DoD: 0 min
- Tiempo total: 453 min
