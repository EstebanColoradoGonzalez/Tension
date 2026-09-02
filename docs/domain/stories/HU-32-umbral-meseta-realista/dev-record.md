## Dev Agent Record — Dev-Rápido

**Autor**: Esteban Colorado González | **Fecha**: 2026-08-31

### Debug Log

| # | Tipo | Descripción | Resolución |
|---|------|-------------|------------|
| 1 | Entorno | `./gradlew` abortó con *"Gradle 8.11.1 requires Java 1.8 or later. You are currently using Java 1.7"* — el `JAVA_HOME` del sistema apunta a `C:\apps\java\jdk1.7.0_79` | Se ejecutó el build exportando `JAVA_HOME=C:/apps/java/JDK_17.0.5`. No se modificó ninguna configuración del proyecto |
| 2 | Compilación | `PlanRepositoryImpl.toDomainModel()` no pasaba `progressionDifficulty` al construir `Exercise`. Es un segundo mapeador de `ExerciseWithDetails`, fuera de `ExerciseRepositoryImpl` | Se añadió el mapeo con `ProgressionDifficulty.fromCode(...)` y su import, respetando el orden alfabético |
| 3 | Compilación (tests) | Cuatro fixtures existentes construían `Exercise` o `Profile` posicionalmente y quedaron sin el campo nuevo: `GetExerciseDetailUseCaseTest`, `GetExercisesUseCaseTest`, `GetProfileUseCaseTest`, `SubstituteExerciseViewModelTest` | Se completaron con `ProgressionDifficulty.MEDIUM` y `plateauBaseThreshold = 5`. Anticipado en T29 del refinamiento |

Ningún test del motor de reglas ajeno a la meseta requirió cambios: HU-10, HU-11 y HU-14 sin regresión.

### Completion Notes

- ⚡ **Dev-Rápido:** el umbral de meseta deja de ser la constante global `PLATEAU_THRESHOLD = 3` y pasa a componerse en cada evaluación como `techo(umbral base × multiplicador de dificultad)`.
- **`PlateauThresholdRule`** (regla pura, `domain/rules/`) es dueña de la fórmula, del rango válido `3..15` y del valor por defecto `5`. `ProgressionClassificationRule.resolveNewProgressionState` recibe el umbral como parámetro obligatorio.
- **Esquema v15**: `exercise.progression_difficulty` (dominio cerrado `LOW`/`MEDIUM`/`HIGH`, default `MEDIUM`) y `profile.plateau_base_threshold` (default 5). `MIGRATION_14_15` con dos `ALTER TABLE`; no reclasifica el catálogo existente, por lo que una base migrada queda íntegramente en `MEDIUM` (CA-32.06 aplica a instalación fresca).
- **Catálogo seed clasificado**: 13 ejercicios en `HIGH`, 8 en `LOW`, 16 en `MEDIUM` por defecto (no anotados). Con base 5 → 10 / 5 / 8 sesiones respectivamente.
- **CA-32.07, CA-32.08 y CA-32.09 no requirieron código nuevo**: el contador ya era agnóstico del umbral y las guardas de `IN_DELOAD` / `MASTERED` ya cortaban antes. Se cubrieron con tests de no regresión.
- **UI**: selector segmentado de 3 opciones (48 dp por opción) en el detalle y en la creación de ejercicio, con persistencia inline en el detalle; stepper acotado en Ajustes con desglose en vivo por dificultad.
- El mensaje de la alerta `PLATEAU`, antes fijo en `"3 sesiones sin progresión"`, se construye ahora con el umbral efectivo del ejercicio.
- **Observación levantada para HU-33:** `CorrectiveActionRule` escalona acciones en 4 y 6 sesiones, valores absolutos calibrados contra el umbral 3. Con umbrales de 5, 8 o 10 ambas acciones aparecen juntas desde la primera alerta. Comportamiento intacto por estar fuera del alcance de esta historia.

### File List

| Acción | Archivo | Descripción |
|--------|---------|-------------|
| Creado | `domain/model/ProgressionDifficulty.kt` | Dominio cerrado `LOW`/`MEDIUM`/`HIGH` con multiplicador y `fromCode` con default `MEDIUM` |
| Creado | `domain/rules/PlateauThresholdRule.kt` | Regla pura del umbral efectivo, rango válido y valor por defecto |
| Creado | `domain/usecase/catalog/UpdateExerciseProgressionDifficultyUseCase.kt` | Persistencia de la dificultad del ejercicio |
| Creado | `domain/usecase/profile/UpdatePlateauBaseThresholdUseCase.kt` | Persistencia del umbral base con validación de rango |
| Creado | `ui/catalog/components/ProgressionDifficultySelector.kt` | Selector segmentado de 3 opciones, 48 dp por opción |
| Creado | `ui/settings/SettingsUiState.kt` | Estado de Ajustes con desglose derivado por dificultad |
| Creado | `ui/settings/SettingsViewModel.kt` | Carga y stepper acotado del umbral base |
| Creado | `app/schemas/…TensionDatabase/15.json` | Esquema Room v15 exportado |
| Modificado | `domain/rules/ProgressionClassificationRule.kt` | `PLATEAU_THRESHOLD` eliminado; `plateauThreshold` como parámetro obligatorio |
| Modificado | `domain/model/Exercise.kt` | Campo `progressionDifficulty` |
| Modificado | `domain/model/Profile.kt` | Campo `plateauBaseThreshold` |
| Modificado | `domain/repository/ExerciseRepository.kt` | `createExercise` con dificultad + `updateProgressionDifficulty` |
| Modificado | `domain/repository/ProfileRepository.kt` | `updatePlateauBaseThreshold` |
| Modificado | `domain/usecase/catalog/CreateExerciseUseCase.kt` | Parámetro de dificultad con default `MEDIUM` |
| Modificado | `data/local/entity/ExerciseEntity.kt` | Columna `progression_difficulty` |
| Modificado | `data/local/entity/ProfileEntity.kt` | Columna `plateau_base_threshold` |
| Modificado | `data/local/dao/ExerciseDao.kt` | Dificultad en las 4 proyecciones + `updateProgressionDifficulty` |
| Modificado | `data/local/dao/ProfileDao.kt` | `updatePlateauBaseThreshold` |
| Modificado | `data/local/dao/SessionExerciseDao.kt` | Dificultad en la proyección de progresión |
| Modificado | `data/local/database/Migrations.kt` | `MIGRATION_14_15` |
| Modificado | `data/local/database/TensionDatabase.kt` | `version = 15` |
| Modificado | `di/DatabaseModule.kt` | Registro de la migración v14→v15 |
| Modificado | `data/repository/ExerciseRepositoryImpl.kt` | Mapeo y escritura de la dificultad |
| Modificado | `data/repository/PlanRepositoryImpl.kt` | Mapeo de la dificultad en su `toDomainModel` |
| Modificado | `data/repository/ProfileRepositoryImpl.kt` | Mapeo y escritura del umbral base |
| Modificado | `data/repository/SessionRepositoryImpl.kt` | Composición del umbral efectivo en `evaluateProgression` y mensaje dinámico de la alerta |
| Modificado | `data/local/seed/model/SeedExercise.kt` | Campo de dificultad con default `MEDIUM` |
| Modificado | `data/local/seed/ExerciseCatalog.kt` | Clasificación de los 37 ejercicios del catálogo |
| Modificado | `data/local/seed/ExerciseSeeder.kt` | Persistencia de la dificultad en el seed |
| Modificado | `ui/catalog/ExerciseDetailUiState.kt` | Dificultad y umbral efectivo en el ítem de detalle |
| Modificado | `ui/catalog/ExerciseDetailViewModel.kt` | `combine` con el perfil y persistencia inline de la dificultad |
| Modificado | `ui/catalog/ExerciseDetailScreen.kt` | Selector, umbral efectivo en vivo y texto de ayuda |
| Modificado | `ui/catalog/CreateExerciseUiState.kt` | Dificultad con `MEDIUM` preseleccionado |
| Modificado | `ui/catalog/CreateExerciseViewModel.kt` | Handler y propagación al caso de uso |
| Modificado | `ui/catalog/CreateExerciseScreen.kt` | Selector en el formulario de creación |
| Modificado | `ui/settings/SettingsScreen.kt` | Sección Entrenamiento con el stepper del umbral base |
| Modificado | `res/values/strings.xml` | Textos del atributo y del control de Ajustes |
| Creado | `test/…/domain/rules/PlateauThresholdRuleTest.kt` | 14 casos: multiplicadores, redondeo, rango, acotación y `fromCode` |
| Modificado | `test/…/domain/rules/ProgressionClassificationRuleTest.kt` | Umbral explícito en los 11 casos previos + 11 casos nuevos de HU-32 |
| Modificado | `test/…/data/local/seed/ExerciseCatalogTest.kt` | 6 casos de clasificación seed y umbral efectivo |
| Creado | `test/…/domain/usecase/profile/UpdatePlateauBaseThresholdUseCaseTest.kt` | 6 casos de validación de rango |
| Creado | `test/…/ui/settings/SettingsViewModelTest.kt` | 8 casos: carga, stepper, topes, desglose y error |
| Modificado | `test/…/domain/usecase/catalog/GetExerciseDetailUseCaseTest.kt` | Fixture actualizada |
| Modificado | `test/…/domain/usecase/catalog/GetExercisesUseCaseTest.kt` | Fixture actualizada |
| Modificado | `test/…/domain/usecase/profile/GetProfileUseCaseTest.kt` | Fixture actualizada |
| Modificado | `test/…/ui/session/SubstituteExerciseViewModelTest.kt` | Fixtures actualizadas |
| Modificado | `docs/architecture/domain_and_state_model.md` | Esquema v15, ambas columnas, enum `ProgressionDifficulty`, §5.3 (CA-32.10) |
| Modificado | `docs/architecture/interfaces_contract.md` | `D2-T1` ampliado, `D2-T2` y `J1-T2` nuevos |
| Creado | `docs/domain/stories/HU-32-umbral-meseta-realista/refinamiento.md` | Plan técnico, 31 tareas en 9 fases |
| Creado | `docs/domain/stories/HU-32-umbral-meseta-realista/dev-record.md` | Este registro |
| Modificado | `docs/domain/stories/HU-32-umbral-meseta-realista/index.md` | Fases de Refinamiento y Desarrollo |

### Verificación

| Comando | Resultado |
|---|---|
| `./gradlew testDebugUnitTest` | **520 tests · 0 fallos · 0 errores · 0 omitidos** |
| `./gradlew assembleDebug` | OK |
| `./gradlew build` (debug + release + lint + tests) | **BUILD SUCCESSFUL** |
| Esquema exportado | `15.json` con `exercise.progression_difficulty` y `profile.plateau_base_threshold` |

La validación manual de los 9 escenarios queda registrada en `refinamiento.md` § *Validación manual (no automatizable)* y no se ejecutó en esta sesión.

### Métricas Dev-Rápido

- Tiempo sesión IA: 27 min
- Tareas manuales DoD: 0 min
- Tiempo total: 27 min
