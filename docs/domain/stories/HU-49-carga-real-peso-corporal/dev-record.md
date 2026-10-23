## Dev Agent Record — Dev-Rápido

### Debug Log

| # | Tipo | Descripción | Resolución |
|---|------|-------------|------------|
| 1 | Entorno | Gradle necesita JDK 17; el `PATH` no lo trae | Build y tests con `JAVA_HOME` = JDK 17 (Eclipse Adoptium), igual que en HU-48 |
| 2 | Entorno | No hay dispositivo ni emulador conectado (`adb devices` vacío) | `CargaRealPesoCorporalTest` se compila (`assembleDebugAndroidTest`), pero queda **sin ejecutar** |
| 3 | Herramienta | `--tests` combinado con `compileDebugAndroidTestKotlin` en una misma invocación de Gradle hace fallar el build | Se corrieron por separado: no es un fallo del código |
| 4 | Medición | No existe `docs/cosmic/measurement-strategy.json` aprobado | No se adelantó la medición en paralelo (step-01 §2c, compuerta 2) |

### Completion Notes

- ⚡ Dev-Rápido: **carga real de cada serie.**
  - Fórmula: carga externa + peso corporal vigente en la fecha de la sesión × porcentaje del ejercicio.
  - Solo aplica si el ejercicio no es isométrico y el implemento suma peso corporal. La regla pura que lo decide es `RealLoadRule`.
  - La serie sigue guardando la carga tecleada.
- ⚡ Dev-Rápido: **el tonelaje usa la carga real en todas sus vistas.** Cubre el resumen post-sesión, el historial y el detalle de sesión, el gráfico semanal de HU-48, Volumen, la evolución, la Tendencia y la alerta `TONNAGE_DROP` (evaluación y detalle).
  - El `SUM(weight_kg × reps)` en SQL se retiró.
  - El peso vigente se resuelve en SQL con el orden `date DESC, id DESC` de HU-48.
- ⚡ Dev-Rápido: **el 1RM se calcula sobre la carga real y guarda la fecha** (`exercise_one_rm.achieved_on`).
  - Toda escritura reconstruye el par desde sus series de referencia (`OneRmRecordWriter`): al registrar una serie, al corregir la última, al cambiar el porcentaje y al registrar un peso (los pares con series desde esa fecha).
  - `Peso Corporal` y `Barra Fija` producen 1RM. `Peso Añadido` cuenta cuerpo + lastre. La dominada asistida en `Máquina` sigue sin 1RM.
- ⚡ Dev-Rápido: **fuerza relativa en O1:** «N.NN× tu peso corporal» y «con X kg el DD/MM/AAAA», calculada al leer con el peso vigente en la fecha del récord.
- ⚡ Dev-Rápido: **«Peso corporal movido» en D2.**
  - Entero de 0 a 100, con error en línea y Guardar deshabilitado si el valor no es válido. Al guardar aparece el snackbar «Peso corporal movido actualizado».
  - No se muestra en ejercicios isométricos.
  - Guardar reconstruye el 1RM del ejercicio en la misma transacción.
  - Valores precargados: Dominadas 100, Sentadilla Búlgara 85, Zancadas 85, Crunch Abdominal 35; el resto 0.
- ⚡ Dev-Rápido: **esquema y respaldo.**
  - Esquema 26 → 27 sin migración (ADR-019).
  - Formato de respaldo 18 → 19. El 18 se rechaza con un mensaje propio; el 17 conserva el suyo.
- ⚡ Dev-Rápido: **el motor de decisión no cambia.** No se tocó ninguna regla de progresión, meseta, regresión, fatiga, descarga ni carga precargada o prescrita.
- ⚡ Dev-Rápido: **documentación:**
  - `domain_and_state_model.md`, `interfaces_contract.md` (D2-T1, E2-T1, E2-T2, E4-T1, E5-T1, G2-T1, H2-T1, J2-T1, O1-T1) y ADR-019 en `architecture_blueprint.md`;
  - RF-57;
  - `story_mapping_index.md`.
- ⚠️ **Decisión fuera de la letra de la historia** (Hallazgo 5 de `refinamiento.md`): registrar un peso recalcula el 1RM de las series de referencia del mismo día. Así el récord usa el mismo peso que el tonelaje y la fuerza relativa.
- ⚠️ **Para validar con el PO:**
  - La fuerza relativa sigue el formato numérico de la pantalla de 1RM (`"%.2f"`, según el idioma del dispositivo) y la unidad «kg» en minúscula de esa pantalla. En un dispositivo en español se verá «1,33×» y «con 80,0 kg», no «1.33×» ni «Kg».
  - Sin peso registrado para la fecha, la carga real es la externa y la tarjeta no muestra fuerza relativa.
- 🧪 Cobertura QA: 13/16. Pendientes:
  - QA-13 (Alta): el motor sin cambios se cumple por construcción, y la suite del motor sigue verde. La carga precargada externa solo la afirma el instrumentado `CargaRealPesoCorporalTest`, compilado pero sin ejecutar.
  - QA-14 (Media): que la serie muestre la carga tecleada solo lo afirma el instrumentado, sin ejecutar.
  - QA-03 (Media): el cálculo con 0 % e isométricos tiene test. Que el campo no aparezca en isométricos (UI) no se automatiza en este proyecto.
  - Nota: QA-04, QA-06, QA-12 y QA-16 tienen test JVM de la regla y el caso de uso. Su recorrido sobre la base real (totales de sesión, subconsulta de peso vigente, recálculo transaccional, respaldo de ida y vuelta) está en el instrumentado, sin ejecutar.

### File List

| Acción | Archivo | Descripción |
|--------|---------|-------------|
| Creado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/domain/rules/RealLoadRule.kt` | Regla pura: implementos que suman, carga real, validación del porcentaje |
| Creado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/domain/model/RealLoadSetData.kt` | Serie con los ingredientes de la carga real, para los totales de sesión |
| Creado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/domain/model/OneRmCandidate.kt` | `OneRmCandidate` y `OneRmRecord` (récord + fecha) |
| Creado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/data/repository/OneRmRecordWriter.kt` | Única escritura del 1RM: reconstruye par, ejercicio o desde fecha |
| Creado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/domain/usecase/catalog/UpdateExerciseBodyWeightPercentUseCase.kt` | Valida y guarda el porcentaje |
| Creado | `Tension/app/schemas/com.estebancoloradogonzalez.tension.data.local.database.TensionDatabase/27.json` | Esquema exportado por Room |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/data/local/entity/ExerciseEntity.kt` | `body_weight_percent` (default 0) |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/data/local/entity/ExerciseOneRmEntity.kt` | `achieved_on` y KDoc de la reconstrucción |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/data/local/database/TensionDatabase.kt` | Esquema 27 |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/data/local/database/Migrations.kt` | KDoc: HU-49 sube a 27 sin migración |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/data/local/seed/model/SeedExercise.kt` | `bodyWeightPercent` |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/data/local/seed/ExerciseCatalog.kt` | 100 / 85 / 85 / 35 |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/data/local/seed/ExerciseSeeder.kt` | Inserta el porcentaje |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/data/local/dao/ExerciseSetDao.kt` | Tonelaje con peso vigente; series de carga real por sesión; candidatos y pares del 1RM; se retira `getMaxQualifyingWeightForPair` |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/data/local/dao/SessionDao.kt` | Sin `SUM` de tonelaje; `SessionSummaryHeaderDto` |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/data/local/dao/ExerciseOneRmDao.kt` | `getAll` con fecha y peso vigente; se retira `getValue` |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/data/local/dao/ExerciseDao.kt` | Porcentaje en las 3 proyecciones; `updateBodyWeightPercent` |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/data/repository/SessionRepositoryImpl.kt` | Totales de sesión con carga real; registro y corrección con `OneRmRecordWriter`; alerta de tonelaje con carga real |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/data/repository/AlertRepositoryImpl.kt` | Detalle de `TONNAGE_DROP` con carga real |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/data/repository/ExerciseRepositoryImpl.kt` | `updateBodyWeightPercent` transaccional con reconstrucción del 1RM (`Result`) |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/data/repository/ProfileRepositoryImpl.kt` | Registrar un peso reconstruye los 1RM desde su fecha |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/data/repository/OneRmRepositoryImpl.kt` | Mapea fecha y peso vigente |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/data/repository/PlanRepositoryImpl.kt` | Mapea el porcentaje |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/data/repository/BackupRepositoryImpl.kt` | Formato 19; rechazo propio del 18 |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/domain/model/SetTonnageData.kt` | Ingredientes de la carga real y `realLoadKg` |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/domain/model/Exercise.kt` | `bodyWeightPercent` |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/domain/model/OneRmEntry.kt` | Fecha y peso vigente |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/domain/model/ExerciseOneRm.kt` | Fecha, peso y fuerza relativa por implemento |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/domain/repository/ExerciseRepository.kt` | `updateBodyWeightPercent` |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/domain/rules/OneRmRule.kt` | Calificación con carga real, `hasCountableLoad`, `record`, `relativeStrength` |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/domain/usecase/metrics/GetTonnageByMuscleGroupUseCase.kt` | Carga real |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/domain/usecase/metrics/GetTonnageEvolutionUseCase.kt` | Carga real |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/domain/usecase/metrics/GetMuscleGroupTrendUseCase.kt` | Carga real |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/domain/usecase/onerm/GetOneRmListUseCase.kt` | Fuerza relativa |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/ui/catalog/ExerciseDetailUiState.kt` | `isIsometric`, porcentaje, `BodyWeightPercentForm`, `ExerciseDetailEvent` |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/ui/catalog/ExerciseDetailViewModel.kt` | Formulario, guardado y eventos |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/ui/catalog/ExerciseDetailScreen.kt` | Campo «Peso corporal movido» y snackbar |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/ui/onerm/OneRmScreen.kt` | Fuerza relativa con peso y fecha |
| Modificado | `Tension/app/src/main/res/values/strings.xml` | Textos de D2, O1 y del rechazo del respaldo 18 |
| Creado | `Tension/app/src/test/java/com/estebancoloradogonzalez/tension/domain/rules/RealLoadRuleTest.kt` | 14 tests (QA-01, 02, 03, 06, 11) |
| Creado | `Tension/app/src/test/java/com/estebancoloradogonzalez/tension/domain/usecase/metrics/GetTonnageByMuscleGroupUseCaseTest.kt` | 2 tests (QA-02, 04) |
| Creado | `Tension/app/src/test/java/com/estebancoloradogonzalez/tension/domain/usecase/metrics/GetTonnageEvolutionUseCaseTest.kt` | 2 tests (QA-05, 06) |
| Creado | `Tension/app/src/test/java/com/estebancoloradogonzalez/tension/domain/usecase/catalog/UpdateExerciseBodyWeightPercentUseCaseTest.kt` | 4 tests (QA-10, 11) |
| Modificado | `Tension/app/src/test/java/com/estebancoloradogonzalez/tension/domain/rules/OneRmRuleTest.kt` | Firma nueva + 10 tests de récord y fuerza relativa (QA-02, 07, 08, 09, 12, 15) |
| Modificado | `Tension/app/src/test/java/com/estebancoloradogonzalez/tension/domain/usecase/onerm/GetOneRmListUseCaseTest.kt` | Fecha y peso + 2 tests de fuerza relativa (QA-09) |
| Modificado | `Tension/app/src/test/java/com/estebancoloradogonzalez/tension/ui/onerm/OneRmViewModelTest.kt` | Fecha y peso + 1 test (QA-07) |
| Modificado | `Tension/app/src/test/java/com/estebancoloradogonzalez/tension/data/local/seed/ExerciseCatalogTest.kt` | Porcentajes precargados (QA-10) |
| Modificado | `Tension/app/src/test/java/com/estebancoloradogonzalez/tension/data/repository/BackupRepositoryImplTest.kt` | Formato 19, rechazo del 18 y del 17 (QA-16) |
| Creado | `Tension/app/src/androidTest/java/com/estebancoloradogonzalez/tension/CargaRealPesoCorporalTest.kt` | Instrumentado, 11 tests (QA-01, 02, 04, 06, 07, 08, 12, 13, 14, 16). Compilado, sin ejecutar |
| Modificado | `Tension/app/src/androidTest/java/com/estebancoloradogonzalez/tension/PesoCorporalEnMetricasTest.kt` | Constructor de `ProfileRepositoryImpl` |
| Modificado | `Tension/app/src/androidTest/java/com/estebancoloradogonzalez/tension/MiUnRmEstimadoTest.kt` | KDoc: sin historial de peso la carga real es la externa |
| Modificado | `docs/architecture/domain_and_state_model.md` | `body_weight_percent`, `achieved_on`, peso vigente, carga real no persistida, esquema 27 |
| Modificado | `docs/architecture/interfaces_contract.md` | D2-T1, E2-T1, E2-T2, E4-T1, E5-T1, G2-T1, H2-T1, J2-T1, O1-T1 |
| Modificado | `docs/architecture/architecture_blueprint.md` | ADR-019: esquema 27, formato 19 |
| Modificado | `docs/domain/definition/requirements_specification.md` | RF-57 con carga real |
| Modificado | `docs/domain/stories/story_mapping_index.md` | HU-49 en `Lista para Revisión` y conteos |

### Revisión del diff

Herramienta: propia + revisor de estándares independiente

| # | Categoría | Hallazgo | Archivo | Resultado |
|---|-----------|----------|---------|-----------|
| 1 | estandares | Comentario nuevo en español (§2.1) | `data/repository/SessionRepositoryImpl.kt` | ✅ Aplicado |
| 2 | estandares | Import `androidx` después de los `com.*` (§3.2) | `data/repository/ExerciseRepositoryImpl.kt` | ✅ Aplicado |
| 3 | estandares | El repositorio lanzaba en lugar de devolver `Result` (§4.4) | `data/repository/ExerciseRepositoryImpl.kt` | ✅ Aplicado |
| 4 | estandares | Import `KeyboardOptions` fuera de orden (§3.2) | `ui/catalog/ExerciseDetailScreen.kt` | ✅ Aplicado |
| 5 | estandares | El flujo nuevo del ViewModel usaba `Context` (§3.5) | `ui/catalog/ExerciseDetailViewModel.kt` | ✅ Aplicado (el mensaje lo resuelve la pantalla) |
| 6 | estandares | Nombres de test sin given/when/then (§5.3) | `test/.../BackupRepositoryImplTest.kt` | ✅ Aplicado |
| 7 | estandares | Comentarios añadidos en español (§2.1) | `test/.../BackupRepositoryImplTest.kt` | ✅ Aplicado |
| 8 | estandares | Import fuera de orden (§3.2) | `test/.../OneRmRuleTest.kt` | ✅ Aplicado |
| 9 | estandares | Comentarios añadidos en español (§2.1) | `test/.../OneRmRuleTest.kt` | ✅ Aplicado |
| 10 | estandares | Comentarios en español (§2.1) | `test/.../RealLoadRuleTest.kt` | ✅ Aplicado |
| 11 | estandares | Comentarios en español (§2.1) | `test/.../UpdateExerciseBodyWeightPercentUseCaseTest.kt` | ✅ Aplicado |
| 12 | estandares | Comentarios en español (§2.1) | `test/.../GetTonnageByMuscleGroupUseCaseTest.kt` | ✅ Aplicado |
| 13 | estandares | Comentarios en español (§2.1) | `test/.../GetTonnageEvolutionUseCaseTest.kt` | ✅ Aplicado |
| 14 | estandares | Comentarios añadidos en español (§2.1) | `test/.../GetOneRmListUseCaseTest.kt` | ✅ Aplicado |
| 15 | estandares | Comentario añadido en español (§2.1) | `test/.../OneRmViewModelTest.kt` | ✅ Aplicado |
| 16 | estandares | Comentario de sección en español (§2.1) | `test/.../ExerciseCatalogTest.kt` | ✅ Aplicado |
| 17 | estandares | KDoc reescrito en español (§2.1) | `androidTest/.../MiUnRmEstimadoTest.kt` | ✅ Aplicado |
| 18 | duplicacion | La subconsulta del peso vigente por fecha se repite en 5 consultas SQL | `data/local/dao/ExerciseSetDao.kt`, `ExerciseOneRmDao.kt` | — No aplicado: Room no comparte fragmentos entre `@Query`, y una vista SQL sería otro cambio de esquema. Cada consulta documenta que replica el orden de `WeightRecordDao.getLatestWeight` |
| 19 | limpieza | La fuerza relativa se formatea con el idioma del dispositivo («1,33×» en español) y con «kg» en minúscula, mientras la historia escribe «1.33×» y «Kg» | `ui/onerm/OneRmScreen.kt` | — No aplicado: sigue la convención existente de la pantalla de 1RM (`"%.1f"` y «kg»). Para validar con el PO |

Revisar primero: `data/local/dao/ExerciseSetDao.kt` (hallazgo #18 sin aplicar) · `ui/onerm/OneRmScreen.kt` (hallazgo #19 sin aplicar) · `data/local/entity/ExerciseEntity.kt` (esquema de datos) · `data/local/entity/ExerciseOneRmEntity.kt` (esquema de datos) · `data/local/database/TensionDatabase.kt` (versión de esquema 27 sin migración) · `data/repository/BackupRepositoryImpl.kt` (formato de respaldo 19)

### Métricas Dev-Rápido

- Tiempo sesión IA: 58 min
- Tareas manuales DoD: 0 min
- Tiempo total: 58 min
