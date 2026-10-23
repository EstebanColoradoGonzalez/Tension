## Refinamiento Técnico (Developer)
**Autor**: esteban.colorado | **Fecha**: 2026-10-23

---

### Contexto

**Arquitectura:** MVVM con capa de dominio explícita (ADR-05): UI → ViewModel → Use Case → Repository → DAO/Room. Las reglas son Kotlin puro en `domain/rules` (ADR-06). El esquema cambia sin migración durante la beta (ADR-019).

**Feature análoga: `HU-42`, 1RM estimado, con archivos agregados el 2026-09-11** (`OneRmRule`, `GetOneRmListUseCase`, `ui/onerm/*`, `OneRmRuleTest`, `MiUnRmEstimadoTest`). Es la más reciente que agrega un indicador nuevo a Métricas con este recorrido:

- una regla pura en `domain/rules` con su test;
- un caso de uso en `domain/usecase/*` que lee repositorios y aplica la regla;
- un estado `Content` en el ViewModel;
- la convención de presentación de `HU-35` (`MetricCard` / `MetricValue` / `MetricSufficiencyRules`).

Complementos:

- **Diálogo con campo numérico:** `PlanVersionDetailScreen` (`AlertDialog` + `OutlinedTextField`, modificado el 2026-10-22).
- **Formulario de perfil:** `HU-01` (`RegisterProfileScreen`, `ProfileScreen`).

Candidatas descartadas:

- **Por más antiguas:** `GetLoadVelocityUseCase` / `MetricsViewModel` (`HU-15`, 2026-02-18) y `MetricPresentation` (`HU-35`, 2026-09-01).
- **No análogas:** `HU-43`, `HU-46` y `HU-47` son posteriores, pero no agregan nada a Métricas.

#### Hallazgos que deciden el diseño

**1. El nivel de experiencia se retira por completo, sin migración.**

Pasos:

- se borra el enum `ExperienceLevel`;
- se borra el campo en `Profile`;
- se borra la columna `profile.experience_level`;
- se borra el parámetro en los dos casos de uso y en el repositorio;
- se retiran los dos `ExperienceLevelDropdown` y sus 4 strings.

`@Database(version)` pasa de **25 a 26** sin migración (ADR-019), igual que HU-39…HU-46. `LAST_MIGRATED_VERSION` sigue en 19. El KDoc de `Migrations` deja constancia de esto. El `schemas/26.json` lo genera el build.

**2. El formato de respaldo NO sube de versión (sigue en 18).**

Al importar, `BackupRepositoryImpl.importFromJson` **descarta las claves que ya no son columnas** (`columnsOf(db, table)`). Por eso un respaldo v18 que traiga `experience_level` se restaura íntegro sin esa clave, y no falta ningún dato que haya que inventar. Ese era el único motivo por el que HU-39…HU-46 subían el formato.

Lo que no se toca:

- el formato exportado pierde la columna solo, porque el volcado es por cursor;
- se actualiza el comentario del mecanismo (`original_exercise_id` deja de ser «la única» clave descartada);
- **no** se añade un mensaje de rechazo nuevo.

**3. El peso actual es la última entrada, y hoy no lo es con dos registros del mismo día.**

`WeightRecordDao.getLatestWeight` ordena solo por `date DESC`. Con dos registros el mismo día (QA-11), el «último» queda indeterminado. Se desempata con `id DESC` en `getLatestWeight` y en `getAllDescByDate`: el historial muestra primero el más reciente (Regla 3).

**4. El registro rápido es el mismo camino que el perfil.**

`UpdateWeightUseCase` (HU-02) ya crea la entrada del historial con fecha de hoy y valida `> 0`, así que se reutiliza tal cual (CA-48.04, Regla 6). En la UI, la validación replica la del perfil:

- `toDoubleOrNull() > 0`;
- mensaje `error_weight_positive`;
- teclado `KeyboardType.Decimal`.

**5. El tonelaje del gráfico es el tonelaje de sesión del sistema.**

Se lee de `SessionRepository.getSessionHistory()`, que trae una fila por sesión cerrada (`COMPLETED`/`INCOMPLETE`) con `totalTonnageKg = SUM(weight_kg × reps)`. Es la misma cifra del historial de sesiones (HU-17) y del resumen post-sesión. Cuando HU-49 cambie esa fuente, el gráfico mostrará la carga real sin tocar este código (Regla 8).

Ventajas:

- no se escribe SQL nuevo;
- no se reutiliza `getTonnageDataBySessionIds`, que multiplica la serie por cada zona muscular: sirve por grupo, no como total de sesión.

**6. Período compartido.** La sección usa la misma ventana que «Progresión» (`progressionWeeks`: 4/8/12 semanas, desde hoy menos N semanas, como `GetLoadVelocityUseCase`). Su tarjeta lleva el mismo `PeriodSelector`, ligado al mismo estado. Cambiarlo en cualquiera de las dos recalcula ambas.

**7. Variación del período — interpretación de «una sola entrada».**

- **Referencia:** la entrada más reciente con `fecha ≤ inicio del período`.
- **«Sin datos suficientes»** en dos casos:
  - no hay referencia;
  - **no hay ninguna entrada dentro del período posterior a la referencia**, es decir, la entrada actual es la propia referencia, y restarla daría un 0 falso.
- **Con referencia y al menos una entrada en el período**, la variación es un valor real, también cuando da `0.0` (QA-06, fila 5).

Así se cumplen QA-06 y QA-13. ⚠️ **Decisión para validar con el PO:** con referencia y una sola entrada en el período se muestra la variación, no «Sin datos suficientes».

**8. Formato numérico.** Se sigue la convención de `MetricFormatRules` (HU-35): locale `es-ES`, coma decimal y `kg` en minúscula. Por eso el panel mostrará `78,5 kg` / `+1,3 kg` / `IMC 24,8`, aunque `qa.md` escriba los valores con punto. Peso, variación e IMC llevan un decimal.

**9. La nota de peso desactualizado no es una alerta.** Es texto dentro de la sección y se calcula al leer: días entre el último registro y hoy, con aviso si son `> 30`. No pasa por `AlertRepository` ni por notificaciones (CA-48.06, Regla 7). QA-15 (el motor no cambia) se cumple por construcción: lo nuevo es de solo lectura, salvo `UpdateWeightUseCase`, que ya existía y nadie del motor lee.

---

### Reutilización

| Componente | Decisión | Motivo |
|------------|----------|--------|
| `domain/usecase/profile/UpdateWeightUseCase` | Se reutiliza | T15: el registro rápido es la misma entrada del historial que el perfil (Regla 6) |
| `domain/repository/ProfileRepository.getAllWeightRecords()` / `getProfile()` | Se reutiliza | T13: fuente del historial y de la altura |
| `domain/repository/SessionRepository.getSessionHistory()` | Se reutiliza | T13: tonelaje por sesión con fecha (Hallazgo 5) |
| `domain/util/CurrentDateProvider` | Se reutiliza | T13: «hoy» inyectable y testeable |
| `ui/components/MetricCard`, `MetricListCard`, `MetricSectionHeader`, `MetricValueText`, `MetricInsufficientBlock` | Se reutiliza | T16: convención HU-35 (etiqueta, valor, unidad, descripción, período) |
| `ui/components/MetricPresentation` (`MetricUnit`, `MetricSufficiencyRules`, `MetricFormatRules`, `MetricRequirementKind`) | Se extiende | T14: 3 unidades nuevas (peso, variación con signo, IMC) y 1 requisito (`WEIGHT_REFERENCE`) |
| `PeriodSelector` (privado en `MetricsScreen`) | Se reutiliza | T16: mismo selector y mismas opciones que «Progresión» |
| `R.string.error_weight_positive`, `weight_label` | Se reutiliza | T16: mismo mensaje y etiqueta que el perfil (CA-48.05) |
| `domain/rules/BodyWeightRule` | Se crea una vez | T12, la usan T13 (resumen) y T19 (tests): peso actual, referencia, variación, IMC, días, desactualizado y tonelaje semanal ISO. No hay equivalente: `TonnageRule` agrupa por grupo muscular, no por semana |
| `domain/model/BodyWeightSummary` + `WeeklyTonnage` | Se crea | T11: no hay modelo de peso por período ni de tonelaje semanal |
| `domain/usecase/metrics/GetBodyWeightSummaryUseCase` | Se crea | T13: no hay caso de uso que combine historial de peso, altura y tonelaje |
| `ui/metrics/BodyWeightChartComposable` | Se crea | T17: `TonnageChartComposable` es por microciclo y grupo muscular, con un solo eje Y. Este gráfico necesita eje temporal en fechas, dos escalas y dos series heterogéneas (línea + barras) |
| `ui/metrics/RegisterWeightDialog` (privado en `MetricsScreen`) | Se crea | T16: no hay diálogo de peso; el patrón es el de `PlanVersionDetailScreen` |

### Estándares aplicables

| Regla | Sección del estándar | Aplica a |
|-------|----------------------|----------|
| Código, KDoc y comentarios en inglés; strings de UI en español, en `res/values/strings.xml` | §2.1, §3.6 | Todas las tareas |
| Use Case `{Acción}{Entidad}UseCase` con `operator fun invoke` | §2.3, §2.4 | T13 |
| Regla pura en `domain/rules`, sin Room ni Android, testeable con JUnit 4 sin emulador | §4.5, §5.5 | T12, T19 |
| Domain no importa `android.*` / `androidx.*` / `com.google.*` | §3.2, §4.1 | T11–T13 |
| ViewModel: `@HiltViewModel` + `@Inject constructor`; `MutableStateFlow` privado → `StateFlow`; eventos one-shot con `MutableSharedFlow` → `SharedFlow`; sin `Context` ni `mutableStateOf` | §3.5, §4.2 | T15 |
| ViewModel → Use Case → Repository; el ViewModel no accede a DAOs | §4.1 | T15 |
| Screen: lambdas de navegación como parámetro, contenido en Composable interno con datos inmutables | §3.4 | T16 |
| Validación de dominio en el Use Case (`Weight must be > 0`); la UI solo habilita o deshabilita | §4.1, §4.4 | T15, T16 |
| `!!` solo en tests; sin wildcards; trailing commas; 4 espacios; ≤ 120 columnas | §3.1–§3.3 | Todas |
| Tests `given …, when …, then …` con Given/When/Then explícitos; Use Cases y ViewModels con repositorios y casos de uso mockeados (mockk) | §5.2–§5.4 | T19–T23 |
| Touch target ≥ 48 dp, padding de card 16 dp, radio de card 12 dp | §6.3 | T16, T17 |
| Señales nunca solo por color: el IMC no lleva color de valoración ni categoría | §6.4, CA-48.03 | T16 |
| Instrumentados: nombres en `snake_case` (DEX < 040 con minSdk 26 rechaza espacios), como en `CorrectLastSetTest` | §5.3 (convención del proyecto) | T24 |

### Tareas de Implementación

#### Fase 1 — Retirar el nivel de experiencia (CA-48.01, CA-48.02, CA-48.08)
- [x] **T1: Borrar `ExperienceLevel` y el campo `Profile.experienceLevel`** — `domain/model/ExperienceLevel.kt` (borrado), `domain/model/Profile.kt`
- [x] **T2: Quitar la columna `experience_level`; esquema 25 → 26 sin migración** — `data/local/entity/ProfileEntity.kt`, `data/local/database/TensionDatabase.kt`, KDoc de `data/local/database/Migrations.kt` (Base: subida de HU-46)
- [x] **T3: `createProfile(weightKg, heightM)` y `updateProfile(heightM)`** — `domain/repository/ProfileRepository.kt`, `data/repository/ProfileRepositoryImpl.kt`
- [x] **T4: Casos de uso sin nivel** — `domain/usecase/profile/CreateProfileUseCase.kt`, `UpdateProfileUseCase.kt`
- [x] **T5: Registro inicial (A1) con solo peso y altura; `isFormValid` sin nivel** — `ui/onboarding/RegisterProfileScreen.kt`, `RegisterProfileUiState.kt`, `RegisterProfileViewModel.kt`
- [x] **T6: Perfil (C1) sin nivel; `isDirty` e `isFormValid` sin nivel** — `ui/profile/ProfileScreen.kt`, `ProfileUiState.kt`, `ProfileViewModel.kt`
- [x] **T7: Retirar los strings `experience_*`** — `res/values/strings.xml`
- [x] **T8: Documentar que `experience_level` se descarta al importar; formato 18 sin cambios** — `data/repository/BackupRepositoryImpl.kt` (solo comentarios)
- [x] **T9: Seeders instrumentados sin `experience_level` en el `INSERT INTO profile`** — `androidTest/.../ProgresionPorRangoYCategoriaTest.kt`, `androidTest/.../testdata/TreeScenarioSeeder.kt`

#### Fase 2 — Peso corporal en Métricas (CA-48.03 – CA-48.07)
- [x] **T10: Desempate por `id DESC` en el último peso y en el historial** — `data/local/dao/WeightRecordDao.kt` (Hallazgo 3)
- [x] **T11: Modelos `BodyWeightSummary` y `WeeklyTonnage`** — `domain/model/BodyWeightSummary.kt`
- [x] **T12: Regla pura `BodyWeightRule`** (peso actual, referencia, variación, IMC, días desde el último registro, desactualizado `> 30`, puntos de peso del período, tonelaje por semana ISO sin semanas vacías) — `domain/rules/BodyWeightRule.kt` (Base: `OneRmRule`, `AdherenceRule`)
- [x] **T13: `GetBodyWeightSummaryUseCase(weeksBack)`** — `domain/usecase/metrics/GetBodyWeightSummaryUseCase.kt` (Base: `GetLoadVelocityUseCase`, `GetOneRmListUseCase`)
- [x] **T14: Presentación** — `MetricUnit.BODY_WEIGHT` (1 decimal, kg), `BODY_WEIGHT_CHANGE` (1 decimal con signo, kg), `BMI` (1 decimal, kg/m²); `MetricRequirementKind.WEIGHT_REFERENCE`; `MetricSufficiencyRules.weightVariation` — `ui/components/MetricPresentation.kt`, `ui/components/MetricCard.kt`
- [x] **T15: Estado y acciones del panel** — `bodyWeight` en `Content`, diálogo (abierto, valor, error), `openWeightDialog` / `onWeightInputChanged` / `dismissWeightDialog` / `saveWeight` (con `UpdateWeightUseCase`), recarga tras guardar y evento `WeightSaved` — `ui/metrics/MetricsUiState.kt`, `ui/metrics/MetricsViewModel.kt`
- [x] **T16: Sección «Peso corporal»** — nota de desactualizado con acceso al diálogo, tarjeta «Peso actual» (fecha + IMC secundario con su descripción), tarjeta «Variación del peso» con el selector de período, gráfico, botón «Registrar peso», diálogo y snackbar «Peso registrado» — `ui/metrics/MetricsScreen.kt` (Base: `MetricsScreen`, `PlanVersionDetailScreen`)
- [x] **T17: Gráfico de peso (línea, un punto por registro) y tonelaje (barras, una por semana ISO) en el mismo eje temporal, con escala propia por serie, leyenda con unidades y aviso «sin tonelaje en el período»** — `ui/metrics/BodyWeightChartComposable.kt` (Base: `TonnageChartComposable`)
- [x] **T18: Strings de la sección, el diálogo y la nota** — `res/values/strings.xml`

#### Fase 3 — Tests
- [x] **T19: `BodyWeightRuleTest`** — `test/.../domain/rules/BodyWeightRuleTest.kt` (cubre: `QA-05`, `QA-06`, `QA-07`, `QA-11`, `QA-12`, `QA-13`)
- [x] **T20: `GetBodyWeightSummaryUseCaseTest`** — `test/.../domain/usecase/metrics/GetBodyWeightSummaryUseCaseTest.kt` (cubre: `QA-05`, `QA-06`, `QA-07`, `QA-13`)
- [x] **T21: `MetricsViewModelTest`, ampliado** (carga de la sección, guardar, cancelar, validación, dos guardados, cambio de período) — `test/.../ui/metrics/MetricsViewModelTest.kt` (cubre: `QA-06`, `QA-08`, `QA-09`, `QA-10`, `QA-11`)
- [x] **T22: Tests de perfil sin nivel** — `CreateProfileUseCaseTest`, `UpdateProfileUseCaseTest`, `GetProfileUseCaseTest`, `SettingsViewModelTest` (ajuste de firma) + nuevos `RegisterProfileViewModelTest` y `ProfileViewModelTest` (cubre: `QA-01`, `QA-02`, `QA-03`)
- [x] **T23: `BackupRepositoryImplTest`: una fila de `profile` con `experience_level` se restaura sin esa clave** — `test/.../data/repository/BackupRepositoryImplTest.kt` (cubre: `QA-14`)
- [x] **T24: Instrumentado `PesoCorporalEnMetricasTest`** (base real: perfil sin nivel, registro rápido en el historial, último peso por `id`, ida y vuelta del respaldo) — `androidTest/.../PesoCorporalEnMetricasTest.kt` (Base: `CorrectLastSetTest`; cubre: `QA-01`, `QA-08`, `QA-11`, `QA-14`). Se compila con `assembleDebugAndroidTest`; ejecutarlo requiere un dispositivo
- [x] `QA-04` y `QA-15` no tienen test propio. QA-04 se verifica con la búsqueda de `experience`/`ExperienceLevel`/«nivel de experiencia» en `app/src` (cero resultados). QA-15 se cumple por construcción (Hallazgo 9). `QA-07` (dibujo) se verifica en la regla; el render del Canvas no se automatiza en este proyecto

#### Fase 4 — Documentación (DoD)
- [x] **T25: RF-01 sin nivel** — `docs/domain/definition/requirements_specification.md`
- [x] **T26: SDD §3.1, entidad Ejecutante sin nivel** — `docs/domain/definition/system_definition_document.md`
- [x] **T27: Tabla `profile` y enum `ExperienceLevel` retirados** — `docs/architecture/domain_and_state_model.md`
- [x] **T28: `A1-T1`, `C1-T1` y `ERR_VALIDATION_PROFILE` sin nivel; `G1-T1` con la sección «Peso corporal» y el registro rápido** — `docs/architecture/interfaces_contract.md`
