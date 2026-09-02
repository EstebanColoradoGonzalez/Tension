## Dev Agent Record — Dev-Rápido

### Debug Log

| # | Tipo | Descripción | Resolución |
|---|------|-------------|------------|
| 1 | Desviación del alcance | La historia documenta una excepción a RNF19 ("beta sin migración: la base de datos se reinicia"). Verificado en código: la cadena de migraciones 6→13 está completa y `DatabaseModule` **no** declara `fallbackToDestructiveMigration`. Sin migración, cualquier instalación existente lanzaría `IllegalStateException` al abrir la BD — la base no "se reinicia sola". | Se implementó `MIGRATION_13_14` de todos modos: una sola sentencia aditiva `ALTER TABLE exercise_set ADD COLUMN capture_unit TEXT NOT NULL DEFAULT 'KG'`. Desviación estrictamente aditiva, aprobada por el usuario en el refinamiento (D1). Ningún CA cambió. |
| 2 | Gap del código vs. el preview | CA-30.04 y el preview exigen controles − / + junto al campo de peso. Esos controles **no existían** en `RegisterSetScreen`: el campo era un `OutlinedTextField` sin ajuste por pulsación. | Se creó el composable privado `WeightStepControls` (48 × 48 dp, `contentDescription`, hint de incremento activo) como parte de esta historia. |
| 3 | Testabilidad | La validación del peso vivía en `RegisterSetViewModel`, que depende de `Context` para los mensajes. Con `testOptions.unitTests.isReturnDefaultValues = true` y sin Robolectric, la regla de rango no era verificable en JVM (RNF29 / RNF30). | Se extrajo la decisión a `WeightCaptureValidator` (Kotlin puro, `sealed interface WeightCaptureError`). El ViewModel quedó como mapeador error → `strings.xml` y se pudo testear con `Context` mockeado por `mockk` (D5). |
| 4 | Entorno | `./gradlew` abortaba con "Gradle 8.11.1 requires Java 1.8 or later. You are currently using Java 1.7" — `JAVA_HOME` apunta a `C:\apps\java\jdk1.7.0_79`. | Ejecución con `JAVA_HOME` apuntando al JDK 17 de Temurin ya instalado. Sin cambios en el repositorio. Incidencia recurrente: ya registrada en HU-29. |
| 5 | Verificación de esquema | Riesgo de desalineación entre el `DEFAULT` que genera Room y el de la migración manual — provocaría fallo de validación de identidad al migrar. | Verificado en el esquema exportado `app/schemas/…/14.json`: Room genera `capture_unit TEXT NOT NULL DEFAULT 'KG'`, idéntico al SQL de `MIGRATION_13_14`. Room entrecomilla el literal TEXT por sí solo. |

### Completion Notes

- ⚡ **Dev-Rápido:** selector de unidad `Kg` / `Lb` por ejercicio en la pantalla de registro de serie, con conversión a kilogramos bajo el capó. El kilogramo sigue siendo la única unidad de almacenamiento, cálculo y agregación: la unidad de captura vive exclusivamente en la frontera de captura y presentación, y ninguna regla del motor (R1–R7), consulta agregada ni métrica la consulta.
- **Regla de conversión como dominio puro:** `WeightConverter` (factor fijo 0.45359237, 2 decimales al persistir, 1 decimal al presentar) y `WeightCaptureValidator` (rango evaluado **siempre** sobre el valor ya convertido a kg). Ambos en Kotlin puro, sin imports de Android, testeados en JVM sin emulador — RNF29 y RNF30.
- **Sin redondeo al múltiplo de 0.5:** 45 lb persiste como 20.41 kg, no como 20.5 kg. El incremento del sistema rige los controles de ajuste, no la precisión del dato. Cubierto por test explícito.
- **La unidad es del ejercicio y no requirió estado nuevo:** se deriva de la última serie registrada del mismo ejercicio vía `getLastCaptureUnitForExercise`, espejo de `getLastWeightForExercise`. Diferencia deliberada: no excluye sesiones de descarga, porque el rótulo de la máquina no depende del microciclo. Conserva el `COALESCE(original_exercise_id, exercise_id)` para que la unidad siga al ejercicio original tras una sustitución (D2).
- **Coherencia entre valor y unidad:** el campo de peso siempre expresa la carga física en la unidad activa — se convierte al cargar la pantalla y al cambiar el selector. Sin esto, preseleccionar `Lb` habría mostrado un número en kg y devuelto al ejecutante la conversión mental que la historia elimina (D4).
- **El valor capturado no se persiste:** el detalle del historial lo reconstruye desde kg (`20.41 kg → 45.0 lb`). Se evitó una columna redundante; la exactitud a un decimal está verificada por un test de ida y vuelta sobre todo el rango 1–1100 lb (D3).
- **Nuevo límite de rango:** 500 kg, ausente hasta ahora en el sistema. Se valida en la UI y como última barrera en `RegisterSetUseCase`, siempre sobre el valor canónico. El mensaje expresa el límite en la unidad activa (500 Kg / 1102,3 lb).
- **Selector oculto para peso corporal e isométricos**, con `capture_unit` forzado a `KG` en el repositorio incluso si la UI enviara otra cosa — la invariante se defiende en la capa de datos, no solo en la pantalla.
- **Migración 13 → 14** implementada pese a la excepción beta documentada en la historia, para no perder el historial de entrenamiento en instalaciones existentes (D1). El respaldo JSON no requirió cambios: `BackupRepositoryImpl` exporta e importa por columnas del cursor, y los respaldos antiguos resuelven la columna nueva con su `DEFAULT 'KG'`.
- **Tests:** 36 tests nuevos en 3 suites (`WeightConverterTest` 13, `WeightCaptureValidatorTest` 8, `RegisterSetViewModelTest` 15) más 3 casos agregados a `RegisterSetUseCaseTest`. Suite completa **459/459 en verde** (baseline 420). `assembleDebug` y `lintDebug` sin errores; ninguno de los strings nuevos quedó marcado como recurso sin uso.
- **Pendiente de validación manual:** los 7 puntos listados en `refinamiento.md`, incluida la verificación de `MIGRATION_13_14` sobre una instalación existente actualizada, además de la instalación fresca.

### File List

| Acción | Archivo | Descripción |
|--------|---------|-------------|
| Creado | `Tension/app/src/main/java/.../domain/model/WeightUnit.kt` | Enum `KG` / `LB` con el paso de captura por unidad y resolución tolerante desde el código persistido |
| Creado | `Tension/app/src/main/java/.../domain/util/WeightConverter.kt` | Conversión canónica: factor 0.45359237, `MAX_WEIGHT_KG`, redondeo a 2 decimales al persistir y 1 al presentar, paso de incremento |
| Creado | `Tension/app/src/main/java/.../domain/util/WeightCaptureValidator.kt` | Regla pura de validación del campo de peso sobre el valor convertido a kg, con `sealed interface WeightCaptureError` |
| Modificado | `Tension/app/src/main/java/.../data/local/entity/ExerciseSetEntity.kt` | Nueva columna `capture_unit` (`TEXT NOT NULL DEFAULT 'KG'`) |
| Modificado | `Tension/app/src/main/java/.../data/local/database/Migrations.kt` | `MIGRATION_13_14`: alta aditiva de `capture_unit` preservando el historial |
| Modificado | `Tension/app/src/main/java/.../data/local/database/TensionDatabase.kt` | Versión de esquema 13 → 14 |
| Modificado | `Tension/app/src/main/java/.../di/DatabaseModule.kt` | `MIGRATION_13_14` registrada en `addMigrations` |
| Modificado | `Tension/app/src/main/java/.../data/local/dao/ExerciseSetDao.kt` | `getLastCaptureUnitForExercise`; `capture_unit` en las proyecciones de series; `ExerciseSetData.captureUnit` |
| Modificado | `Tension/app/src/main/java/.../domain/model/RegisterSetInfo.kt` | Campo `captureUnit` para preseleccionar el selector |
| Modificado | `Tension/app/src/main/java/.../domain/model/ExerciseSessionData.kt` | `SetData.captureUnit` con default `KG` — los usos del motor de reglas quedan intactos |
| Modificado | `Tension/app/src/main/java/.../domain/repository/SessionRepository.kt` | `registerSet` recibe la unidad de captura |
| Modificado | `Tension/app/src/main/java/.../data/repository/SessionRepositoryImpl.kt` | Resuelve la unidad en `getRegisterSetInfo`, la persiste en `registerSet` forzando `KG` sin carga externa, y la mapea en `getSessionDetail` |
| Modificado | `Tension/app/src/main/java/.../domain/usecase/session/RegisterSetUseCase.kt` | Guarda de máximo 500 kg sobre el valor canónico y propagación de la unidad |
| Modificado | `Tension/app/src/main/java/.../ui/session/RegisterSetUiState.kt` | `weightKg` → `weightInput` en la unidad activa; `captureUnit`, `convertedWeightKg`, `isUnitSelectorVisible` |
| Modificado | `Tension/app/src/main/java/.../ui/session/RegisterSetViewModel.kt` | Precarga convertida, `onUnitSelected`, `onWeightStep`, validación delegada a la regla pura y mapeo de errores a strings |
| Creado | `Tension/app/src/main/java/.../ui/session/components/WeightUnitSelector.kt` | Selector segmentado `Kg` / `Lb` de 48 dp con `contentDescription` por opción (RNF06) |
| Modificado | `Tension/app/src/main/java/.../ui/session/RegisterSetScreen.kt` | `WeightSection` con campo + selector, `WeightStepControls` (− / +, hint de incremento) y hint del kg resultante en modo libras |
| Modificado | `Tension/app/src/main/java/.../ui/history/SessionDetailScreen.kt` | Segunda línea `capturado como X lb` en la serie individual — única ubicación donde aparece la unidad de captura |
| Modificado | `Tension/app/src/main/res/values/strings.xml` | 15 strings nuevos (selector, incremento, hint de conversión, errores de peso, unidad en el historial); `register_set_weight_label` reemplazado por su variante con formato |
| Creado | `Tension/app/src/test/java/.../domain/util/WeightConverterTest.kt` | 13 tests — conversión exacta, dos decimales, sin múltiplo de 0.5, ida y vuelta 1–1100 lb, paso por unidad, frontera de 500 kg |
| Creado | `Tension/app/src/test/java/.../domain/util/WeightCaptureValidatorTest.kt` | 8 tests — no numérico, negativo, máximo sobre el valor convertido, y el caso 1100 lb que pasa mientras 600 kg falla |
| Creado | `Tension/app/src/test/java/.../ui/session/RegisterSetViewModelTest.kt` | 15 tests — preselección de unidad, selector oculto sin carga externa, cambio de unidad, incremento por unidad, persistencia canónica y bloqueo por máximo |
| Modificado | `Tension/app/src/test/java/.../domain/usecase/session/RegisterSetUseCaseTest.kt` | Firma actualizada y 3 casos nuevos: máximo admitido, máximo excedido y propagación de la unidad |
| Modificado | `Tension/app/src/test/java/.../domain/usecase/session/GetRegisterSetInfoUseCaseTest.kt` | `RegisterSetInfo` con `captureUnit` |
| Generado | `Tension/app/schemas/…/14.json` | Esquema exportado por Room para la versión 14 |
| Modificado | `docs/architecture/domain_and_state_model.md` | Convención de kilogramo canónico, columna `capture_unit`, enum `WeightUnit`, versión de esquema 14 |
| Modificado | `docs/architecture/interfaces_contract.md` | `E2-T1` con selector, `capture_unit` y validación sobre el valor canónico; `F2-T1` con la unidad en el detalle de la serie |
| Modificado | `docs/architecture/architecture_blueprint.md` | Versión de esquema actual 13 → 14 |
| Creado | `docs/domain/stories/HU-30-unidad-carga-kg-lb/refinamiento.md` | Plan técnico aprobado — 23 tareas en 6 fases y 6 decisiones técnicas |
| Modificado | `docs/domain/stories/HU-30-unidad-carga-kg-lb/cambios.md` | Bitácora de refinamiento y desarrollo |
| Modificado | `docs/domain/stories/HU-30-unidad-carga-kg-lb/index.md` | Fases Refinamiento Técnico y Desarrollo → ✅ Completada |
| Creado | `docs/domain/stories/HU-30-unidad-carga-kg-lb/dev-record.md` | Este registro |

### Métricas Dev-Rápido

- Tiempo sesión IA: ~45 min de trabajo activo (ventana de reloj 2026-08-30 23:14 → 2026-08-31 11:55, con pausa nocturna entre la aprobación del plan y la implementación)
- Tareas manuales DoD: 0 min — validación en dispositivo pendiente
- Tiempo total: ~45 min
