## Dev Agent Record — Dev-Rápido

### Debug Log

| # | Tipo | Descripción | Resolución |
|---|------|-------------|------------|
| 1 | Entorno | Gradle 8.11.1 no arranca con el Java 1.7 del `PATH` | Build y tests con `JAVA_HOME` = JDK 17 (Eclipse Adoptium) |
| 2 | Entorno | No hay dispositivo ni emulador conectado (`adb devices` vacío) | `PesoCorporalEnMetricasTest` se compila (`assembleDebugAndroidTest`), pero queda **sin ejecutar** |
| 3 | Repositorio | `androidTest/.../testdata/TreeScenarioSeeder.kt` está excluido en `.git/info/exclude` | Se actualizó su `INSERT INTO profile` (sin `experience_level`) en disco, aunque git no lo muestra en el diff |

### Completion Notes

- ⚡ Dev-Rápido: **nivel de experiencia retirado por completo.** Ya no aparece en:
  - el registro inicial (A1) ni el perfil (C1);
  - el modelo de dominio (`Profile`, enum `ExperienceLevel` borrado);
  - la tabla `profile` (esquema 25 → 26 sin migración, ADR-019);
  - los strings ni la documentación.
- ⚡ Dev-Rápido: **el formato del respaldo sigue en 18.** La importación descarta la clave `experience_level` de los respaldos anteriores (CA-48.08).
- ⚡ Dev-Rápido: **nueva sección «Peso corporal» en Métricas (G1), con el mismo período que «Progresión»:**
  - peso actual con su fecha;
  - IMC secundario, sin categoría;
  - variación del período, con «Sin datos suficientes» cuando no hay referencia;
  - gráfico de peso (un punto por registro) y tonelaje (una barra por semana ISO, sin semanas vacías);
  - nota de peso desactualizado (> 30 días), que no es una alerta;
  - registro rápido de peso: reutiliza `UpdateWeightUseCase`, la misma entrada del historial que el perfil.
- ⚡ Dev-Rápido: **corrección del último peso.** `WeightRecordDao` desempata por `id DESC` los registros del mismo día (QA-11).
- ⚡ Dev-Rápido: **documentación actualizada:**
  - RF-01;
  - SDD §3.1;
  - `domain_and_state_model.md` (`profile`, `weight_record`, enum retirado);
  - `interfaces_contract.md` (`A1-T1`, `C1-T1`, `ERR_VALIDATION_PROFILE`, sección `PESO CORPORAL` de `G1-T1` y nuevo `G1-T5`).
- ⚠️ Decisión para validar con el PO (Hallazgo 7 de `refinamiento.md`): con referencia y una sola entrada en el período se muestra la variación, no «Sin datos suficientes».
- 🧪 Cobertura QA: 13/15. Pendientes:
  - QA-04 (Media): sin test propio. Se verifica con la búsqueda de `ExperienceLevel`/`experienceLevel`/«nivel de experiencia» en `app/src`: no queda en código productivo.
  - QA-15 (Media): se cumple por construcción. La sección es de solo lectura y el registro usa el `UpdateWeightUseCase` existente, que el motor de decisión no lee.
  - Nota: QA-01, QA-11 y QA-14 tienen además el instrumentado `PesoCorporalEnMetricasTest`, compilado pero sin ejecutar.
  - Nota: el render del gráfico (QA-07) y la ausencia visual de categorías del IMC (QA-05) se verifican en la regla y el estado, no en pantalla.

### File List

| Acción | Archivo | Descripción |
|--------|---------|-------------|
| Eliminado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/domain/model/ExperienceLevel.kt` | Enum sin consumidores |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/domain/model/Profile.kt` | Sin `experienceLevel` |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/data/local/entity/ProfileEntity.kt` | Sin columna `experience_level` |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/data/local/database/TensionDatabase.kt` | Esquema 26 |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/data/local/database/Migrations.kt` | KDoc: HU-48 sube a 26 sin migración |
| Creado | `Tension/app/schemas/com.estebancoloradogonzalez.tension.data.local.database.TensionDatabase/26.json` | Esquema exportado por Room |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/data/local/dao/WeightRecordDao.kt` | Desempate `id DESC` en el último peso y el historial |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/domain/repository/ProfileRepository.kt` | `createProfile(weight, height)`, `updateProfile(height)` |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/data/repository/ProfileRepositoryImpl.kt` | Ídem |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/data/repository/BackupRepositoryImpl.kt` | Solo comentarios: la clave retirada se descarta al importar; el formato sigue en 18 |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/domain/usecase/profile/CreateProfileUseCase.kt` | Sin nivel |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/domain/usecase/profile/UpdateProfileUseCase.kt` | Sin nivel |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/ui/onboarding/RegisterProfileScreen.kt` | A1 sin selector de nivel |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/ui/onboarding/RegisterProfileUiState.kt` | `isFormValid` con peso y altura |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/ui/onboarding/RegisterProfileViewModel.kt` | Sin nivel |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/ui/profile/ProfileScreen.kt` | C1 sin selector de nivel |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/ui/profile/ProfileUiState.kt` | `isDirty`/`isFormValid` sin nivel |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/ui/profile/ProfileViewModel.kt` | Sin nivel |
| Creado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/domain/model/BodyWeightSummary.kt` | `BodyWeightSummary`, `WeeklyTonnage`, `SessionTonnage` |
| Creado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/domain/rules/BodyWeightRule.kt` | Regla pura: peso actual, referencia, variación, IMC, desactualizado, series del gráfico |
| Creado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/domain/usecase/metrics/GetBodyWeightSummaryUseCase.kt` | Resumen de la sección (`Result`) |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/ui/components/MetricPresentation.kt` | Unidades `BODY_WEIGHT`, `BODY_WEIGHT_CHANGE`, `BMI`; requisito `WEIGHT_REFERENCE` |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/ui/components/MetricCard.kt` | Símbolos y texto del requisito nuevo |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/ui/metrics/MetricsUiState.kt` | `bodyWeight`, `WeightDialogState`, `MetricsEvent` |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/ui/metrics/MetricsViewModel.kt` | Carga y observación de la sección, diálogo y guardado |
| Modificado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/ui/metrics/MetricsScreen.kt` | Sección «Peso corporal», nota, diálogo, snackbar y selector de período compartido |
| Creado | `Tension/app/src/main/java/com/estebancoloradogonzalez/tension/ui/metrics/BodyWeightChartComposable.kt` | Gráfico de peso (línea) y tonelaje semanal (barras) con doble escala |
| Modificado | `Tension/app/src/main/res/values/strings.xml` | Retira `experience_*`; textos de la sección, el diálogo y la nota |
| Modificado | `Tension/app/src/androidTest/java/com/estebancoloradogonzalez/tension/ProgresionPorRangoYCategoriaTest.kt` | `INSERT INTO profile` sin nivel |
| Modificado | `Tension/app/src/androidTest/java/com/estebancoloradogonzalez/tension/testdata/TreeScenarioSeeder.kt` | `INSERT INTO profile` sin nivel (excluido de git localmente) |
| Creado | `Tension/app/src/androidTest/java/com/estebancoloradogonzalez/tension/PesoCorporalEnMetricasTest.kt` | Instrumentado: columna retirada, último peso por `id`, ida y vuelta del respaldo |
| Creado | `Tension/app/src/test/java/com/estebancoloradogonzalez/tension/domain/rules/BodyWeightRuleTest.kt` | 19 tests (QA-05, 06, 07, 11, 12, 13) |
| Creado | `Tension/app/src/test/java/com/estebancoloradogonzalez/tension/domain/usecase/metrics/GetBodyWeightSummaryUseCaseTest.kt` | 5 tests (QA-05, 06, 07, 12, 13) |
| Modificado | `Tension/app/src/test/java/com/estebancoloradogonzalez/tension/ui/metrics/MetricsViewModelTest.kt` | +10 tests (QA-06, 08, 09, 10, 11) |
| Creado | `Tension/app/src/test/java/com/estebancoloradogonzalez/tension/ui/onboarding/RegisterProfileViewModelTest.kt` | QA-01, QA-02 |
| Creado | `Tension/app/src/test/java/com/estebancoloradogonzalez/tension/ui/profile/ProfileViewModelTest.kt` | QA-03 |
| Modificado | `Tension/app/src/test/java/com/estebancoloradogonzalez/tension/data/repository/BackupRepositoryImplTest.kt` | QA-14: la clave `experience_level` se descarta al importar |
| Modificado | `Tension/app/src/test/java/com/estebancoloradogonzalez/tension/domain/usecase/profile/CreateProfileUseCaseTest.kt` | Firma sin nivel + límite mínimo válido |
| Modificado | `Tension/app/src/test/java/com/estebancoloradogonzalez/tension/domain/usecase/profile/UpdateProfileUseCaseTest.kt` | Firma sin nivel |
| Modificado | `Tension/app/src/test/java/com/estebancoloradogonzalez/tension/domain/usecase/profile/GetProfileUseCaseTest.kt` | Modelo sin nivel |
| Modificado | `Tension/app/src/test/java/com/estebancoloradogonzalez/tension/ui/settings/SettingsViewModelTest.kt` | Modelo sin nivel |
| Modificado | `docs/domain/definition/requirements_specification.md` | RF-01 sin nivel |
| Modificado | `docs/domain/definition/system_definition_document.md` | §3.1 Ejecutante sin nivel |
| Modificado | `docs/architecture/domain_and_state_model.md` | `profile` sin columna, enum retirado, desempate de `weight_record` |
| Modificado | `docs/architecture/interfaces_contract.md` | `A1-T1`, `C1-T1`, `ERR_VALIDATION_PROFILE`, sección `PESO CORPORAL` en `G1-T1`, nuevo `G1-T5` |

### Revisión del diff

Herramienta: propia + revisor de estándares independiente

| # | Categoría | Hallazgo | Archivo | Resultado |
|---|-----------|----------|---------|-----------|
| 1 | estandares | Sufijo «Kg» del diálogo escrito como literal, no en `strings.xml` (§3.6) | `ui/metrics/MetricsScreen.kt` | ✅ Aplicado |
| 2 | estandares | KDoc agregado en español (§2.1) | `data/local/database/Migrations.kt` | ✅ Aplicado |
| 3 | estandares | KDoc agregado en español (§2.1) | `data/repository/BackupRepositoryImpl.kt` | ✅ Aplicado |
| 4 | estandares | `refreshBodyWeight` descartaba el error en silencio (§4.4) | `ui/metrics/MetricsViewModel.kt` | ✅ Aplicado |
| 5 | estandares | El caso de uso no devolvía `Result<T>` (§4.4) | `domain/usecase/metrics/GetBodyWeightSummaryUseCase.kt` | ✅ Aplicado |
| 6 | estandares | Más de 2 parámetros en una línea (§3.1) | `ui/metrics/BodyWeightChartComposable.kt` | ✅ Aplicado |
| 7 | estandares | Más de 2 parámetros en una línea (§3.1) | `test/.../GetBodyWeightSummaryUseCaseTest.kt` | ✅ Aplicado |
| 8 | estandares | Línea de más de 120 caracteres (§3.1) | `test/.../BodyWeightRuleTest.kt` | ✅ Aplicado |
| 9 | estandares | Tests sin separación Given/When/Then (§5.2) | `test/.../BodyWeightRuleTest.kt` | ✅ Aplicado |
| 10 | duplicacion | El formateo del peso precargado (BigDecimal, 2 decimales, sin ceros) repite el de `ProfileViewModel` | `ui/metrics/MetricsViewModel.kt` | — No aplicado: unificarlo obliga a tocar `ProfileViewModel`, código preexistente; son 4 líneas sin riesgo de divergencia funcional |
| 11 | bug | Sin registros de peso dentro del período (p. ej. último peso de hace 42 días con 4 semanas), el gráfico no dibuja línea de peso | `ui/metrics/BodyWeightChartComposable.kt` | — No aplicado: la Regla 8 define «un punto por cada entrada dentro del período»; dibujar la referencia sería inventar un punto. La tarjeta «Peso actual» y la nota de desactualizado sí lo muestran. Para validar con el PO |

Revisar primero: `ui/metrics/MetricsViewModel.kt` (hallazgo #10 sin aplicar) · `ui/metrics/BodyWeightChartComposable.kt` (hallazgo #11 sin aplicar) · `data/local/entity/ProfileEntity.kt` (esquema de datos) · `data/local/database/TensionDatabase.kt` (versión de esquema 26 sin migración)

### Métricas Dev-Rápido

- Tiempo sesión IA: 27 min
- Tareas manuales DoD: 0 min
- Tiempo total: 27 min
