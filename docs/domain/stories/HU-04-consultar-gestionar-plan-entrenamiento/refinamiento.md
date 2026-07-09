## Refinamiento Técnico (Developer)
**Autor**: [Por asignar] | **Fecha**: [Por asignar]

---

### Contexto

HU-04 es la cuarta historia en implementarse. Transforma el stub `TrainingPlanScreen` (D3) creado en HU-03 en una vista funcional y construye la nueva vista D4 (Detalle de Versión del Plan). Reutiliza la infraestructura de datos creada en HU-03 — las entidades `ModuleVersionEntity`, `PlanAssignmentEntity`, `ModuleEntity` y `ExerciseEntity` ya existen con su seed data (9 module_versions, 93 plan_assignments).

---

### Tareas de Implementación

#### Fase 1 — Data Layer — DAO Modificaciones

- [ ] **T1: Modificar `PlanAssignmentDao`** — `data/local/dao/PlanAssignmentDao.kt`
  - Agregar `getDetailsByModuleVersionId(moduleVersionId: Long): Flow<List<PlanAssignmentWithExerciseDetails>>` — Query JOIN multi-tabla.
  - Agregar `insert(assignment: PlanAssignmentEntity)` — Insert individual.
  - Agregar `delete(moduleVersionId: Long, exerciseId: Long)` — Delete por PK compuesta.
  - Crear data class `PlanAssignmentWithExerciseDetails` con campos: `exerciseId`, `exerciseName`, `moduleCode`, `equipmentTypeName`, `muscleZones` (GROUP_CONCAT), `sets`, `reps`, `isBodyweight`, `isIsometric`, `isToTechnicalFailure`, `isCustom`.

- [ ] **T2: Modificar `ModuleVersionDao`** — `data/local/dao/ModuleVersionDao.kt`
  - Agregar `getAllWithExerciseCount(): Flow<List<ModuleVersionWithCount>>` — Query con LEFT JOIN + COUNT + GROUP BY.
  - Agregar `getById(moduleVersionId: Long): Flow<ModuleVersionEntity?>`.
  - Crear data class `ModuleVersionWithCount` con campos: `id`, `moduleCode`, `versionNumber`, `exerciseCount`.

- [ ] **T3: Modificar `ExerciseDao`** — `data/local/dao/ExerciseDao.kt`
  - Agregar `getByModuleCodeNotInVersion(moduleCode: String, moduleVersionId: Long): Flow<List<ExerciseWithDetails>>` — Query con subconsulta NOT IN.

#### Fase 2 — Domain Layer

- [ ] **T4: Crear modelos de dominio** — `domain/model/`
  - `ModuleWithVersions`: `module: Module`, `versions: List<VersionSummary>`.
  - `VersionSummary`: `moduleVersionId: Long`, `versionNumber: Int`, `exerciseCount: Int`.
  - `PlanVersionDetail`: `moduleVersionId: Long`, `moduleCode: String`, `moduleName: String`, `versionNumber: Int`, `exercises: List<PlanExercise>`.
  - `PlanExercise`: `exerciseId: Long`, `name: String`, `equipmentTypeName: String`, `muscleZones: List<String>`, `sets: Int`, `reps: String`, `isBodyweight: Boolean`, `isIsometric: Boolean`, `isToTechnicalFailure: Boolean`, `isCustom: Boolean`.

- [ ] **T5: Crear interfaz `PlanRepository`** — `domain/repository/PlanRepository.kt`
  - 5 métodos: `getModulesWithVersionCounts()`, `getVersionDetail()`, `getAvailableExercisesForVersion()`, `assignExercise()`, `unassignExercise()`.

- [ ] **T6: Crear 4 Use Cases** — `domain/usecase/plan/`
  - `GetTrainingPlanUseCase` — Lectura para D3.
  - `GetPlanVersionDetailUseCase` — Lectura para D4.
  - `AssignExerciseToVersionUseCase` — Valida sets > 0 y reps válido.
  - `UnassignExerciseFromVersionUseCase` — Incluye guard `hasActiveSessionForVersion`.

#### Fase 3 — Data Layer — Repository

- [ ] **T7: Implementar `PlanRepositoryImpl`** — `data/repository/PlanRepositoryImpl.kt`
  - Inyecta `PlanAssignmentDao`, `ModuleVersionDao`, `ModuleDao`, `ExerciseDao`.
  - `getModulesWithVersionCounts()`: Usa `combine()` de moduleDao + moduleVersionDao.
  - `getVersionDetail()`: Combina moduleVersionDao + planAssignmentDao + moduleDao lookup.
  - `getAvailableExercisesForVersion()`: Delega a exerciseDao.
  - `assignExercise()` / `unassignExercise()`: Opera sobre planAssignmentDao.

#### Fase 4 — DI

- [ ] **T8: Actualizar `RepositoryModule`** — Agregar binding `PlanRepository` ↔ `PlanRepositoryImpl` con `@Binds @Singleton`.

#### Fase 5 — UI — D3

- [ ] **T9: Reemplazar stub `TrainingPlanScreen`** — `ui/catalog/TrainingPlanScreen.kt`
  - Top Bar: Reutiliza tabs existentes.
  - Body: `LazyColumn` con 3 secciones de módulo, cada una con 3 versiones y conteos dinámicos.

- [ ] **T10: Crear `TrainingPlanViewModel` y estados UI** — `ui/catalog/`
  - `TrainingPlanViewModel`: Inyecta `GetTrainingPlanUseCase`.
  - `TrainingPlanUiState`, `ModuleSectionItem`, `VersionItem`.

#### Fase 6 — UI — D4

- [ ] **T11: Crear `PlanVersionDetailScreen`** — `ui/catalog/PlanVersionDetailScreen.kt`
  - Top Bar con título dinámico.
  - Lista de ejercicios con prescripción y acciones.
  - FAB asignar → Bottom Sheet.
  - AlertDialog desasignación.
  - Estado vacío.

- [ ] **T12: Crear `PlanVersionDetailViewModel` y estados UI** — `ui/catalog/`
  - `PlanVersionDetailViewModel`: Recibe `moduleVersionId` via `SavedStateHandle`.
  - `PlanVersionDetailUiState`, `PlanExerciseItem`, `AssignExerciseSheetState`.

- [ ] **T13: Implementar mapeo `reps` → texto español** — Función `mapRepsToDisplay()`.

#### Fase 7 — Navegación

- [ ] **T14: Actualizar `NavigationRoutes`** — Agregar `PLAN_VERSION_DETAIL` y helper function.

- [ ] **T15: Actualizar `TensionNavHost`** — Actualizar D3, agregar composable D4 con `navArgument("moduleVersionId", NavType.LongType)`.

- [ ] **T16: Actualizar `BottomNavigationBar`** — Agregar `"plan-version-detail"` a `childRoutePrefixes` del ítem Diccionario.

#### Fase 8 — Recursos

- [ ] **T17: Agregar strings en `strings.xml`** — Strings para D3 y D4 (13 strings nuevos).

---

### Hitos de implementación

| Hito | Contenido | Dependencias |
| --- | --- | --- |
| 1 | Data Layer — DAO Modificaciones | HU-03 completada |
| 2 | Domain Layer: modelos, interfaz, Use Cases | — |
| 3 | Data Layer — Repository | Hito 1, Hito 2 |
| 4 | DI: binding PlanRepository | Hito 3 |
| 5 | UI — D3: TrainingPlanScreen | Hito 2, Hito 3 |
| 6 | UI — D4: PlanVersionDetailScreen | Hito 2, Hito 3 |
| 7 | Navegación | Hito 5, Hito 6 |
| 8 | Recursos: strings.xml | — (independiente) |
