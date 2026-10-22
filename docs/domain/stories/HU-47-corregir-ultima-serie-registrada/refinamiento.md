## Refinamiento Técnico (Developer)
**Autor**: esteban.colorado | **Fecha**: 2026-10-22

---

### Contexto

**Arquitectura:** MVVM con capa de dominio explícita (ADR-05). UI → ViewModel → Use Case → Repository → DAO/Room. Reglas en Kotlin puro en `domain/rules` (ADR-06).

**Feature análoga: `HU-43`, ajustar la sesión del día. Archivos agregados el 2026-09-11** (`WithdrawFromSessionUseCase`, `AjustarSesionDelDiaTest`, menú del ejercicio en `ActiveSessionScreen`). Es la más reciente que añade una **acción sobre la tarjeta del ejercicio de la sesión activa** que modifica datos de la sesión en curso, con el veredicto evaluado **dentro de la transacción del repositorio**. De ella se replica:

- un caso de uso delgado que delega y deja la guarda al repositorio (sin ventana entre comprobar y escribir);
- la columna derivada en `SessionExerciseDao.getBySessionIdWithDetails` (`Flow`) que la UI lee para habilitar la acción;
- tests de caso de uso y de ViewModel con `mockk`, más un test instrumentado sobre base real.

Complemento de la ruta de escritura: `HU-42` (`MiUnRmEstimadoTest`, 2026-09-11) y el flujo de registro `HU-06/HU-46` (`RegisterSetViewModel`, `RegisterSetScreen`), que es lo que el modo corrección reutiliza.

Candidatas descartadas por más antiguas: `RegisterSetUseCase` (`HU-06`, 2026-02-14). `HU-44`/`HU-45`/`HU-46` son posteriores pero no son análogas: las dos primeras solo tocan `assets/tree/tree.js` y la tercera no añade ninguna acción sobre datos registrados.

#### Hallazgos que deciden el diseño

**1. La ventana se deriva, no se guarda.** «La última serie registrada de la sesión» es la fila de `exercise_set` con el `id` más alto entre las de la sesión en curso (el proyecto ya usa `ORDER BY es.id DESC` como «la última»). Derivarla:

- sobrevive al cierre inesperado sin hacer nada (CA-47.10): vive en la base, no en memoria;
- se mueve sola al registrar cualquier serie (CA-47.02) y nunca puede estar en dos ejercicios (CA-47.07);
- muere con la sesión al filtrar por `session.status = 'IN_PROGRESS'` (CA-47.08).

**Sin cambio de esquema y sin migración**: no hay columna, ni tabla, ni subida de formato de respaldo.

**2. El único derivado persistido es el 1RM.** Todo lo demás se lee de `exercise_set` al consultarse:

- el resumen post-sesión, el detalle de sesión pasada, los KPIs y el tonelaje;
- el aviso de la serie anterior (`previousSetNotice`);
- la clasificación del cierre, que todavía no existe mientras la sesión está en curso.

`exercise_progression` solo recibe `insertIfNotExists` del par, y el par no cambia porque el implemento no se corrige.

**3. El 1RM se recalcula desde el historial y queda exacto.** El récord es el máximo de las series que califican (10 reps, RIR 1, carga externa > 0) y la fórmula es monótona en el peso a 10 repeticiones. Por eso «como si el error nunca hubiera existido» es: `récord = estimate(MAX(weight_kg) de las series del par que califican)`, y **borrar la fila** si ya no queda ninguna (CA-42.07: sin cero ni guion).

El riesgo que documenta `ExerciseOneRmEntity` —un máximo de una serie purgada o restaurada a medias— no aplica:

- no hay ninguna purga (`domain_and_state_model.md`, «Políticas de Depuración»);
- el respaldo es completo;
- la tabla existe desde el esquema 23, que se instala en limpio (ADR-019), así que toda serie que calificó pasó por `registerSet` y está en `exercise_set`.

El recálculo solo toca **el par de la serie corregida**.

**4. Las validaciones se comparten, no se copian.** El bloque de `require` de `RegisterSetUseCase` se extrae a una regla pura que usan el registro y la corrección (CA-47.06). En la UI, `RegisterSetViewModel.onConfirm` valida igual en los dos modos y solo cambia el caso de uso que persiste.

**5. El formulario es el mismo, en modo corrección.**

- Misma ruta `register-set/{sessionExerciseId}` con el argumento opcional `?correction=true`.
- El implemento se pasa como **única opción**: `SetEquipmentSelector` ya lo pinta «resuelto y sin interacción» (CA-47.03).
- El isométrico se corrige tecleando los segundos (campo «Segundos sostenidos» con su referencia), no con el cronómetro: se corrige un número ya registrado, no se vuelve a cronometrar (QA-14).

**6. Choque entre `qa.md` y el registro actual — se sigue la HU.** QA-09 espera que el peso `0` se rechace en `Press de Banca` (Barra). El registro actual lo **acepta**: `RegisterSetUseCaseTest` tiene `invoke with zero weight succeeds`, y el formulario solo exige > 0 con «Peso Añadido». CA-47.06 y la Regla 8 piden **exactamente las mismas** validaciones que el registro. Por eso la corrección acepta `0` donde el registro lo acepta. Cambiar la regla del registro está fuera del alcance de esta HU. Queda anotado para el PO.

---

### Reutilización

| Componente | Decisión | Motivo |
|------------|----------|--------|
| `ui/session/RegisterSetScreen.kt` + `RegisterSetViewModel.kt` + `RegisterSetUiState.kt` | Se reutiliza | T7, T8: el formulario gana un modo corrección; sin pantalla nueva (historia: «sin pantallas nuevas») |
| `ui/session/components/SetEquipmentSelector.kt` | Se reutiliza | T8: con una sola opción se pinta como etiqueta no editable (CA-47.03) |
| `domain/util/WeightConverter`, `WeightCaptureValidator` | Se reutiliza | T7: misma conversión a kg canónico y mismos mensajes que el registro (CA-47.04, QA-20) |
| `domain/rules/ExternalLoadRule` | Se reutiliza | T3, T4, T7: decide si hay carga externa y si es «Peso Añadido» (CA-47.09) |
| `domain/rules/OneRmRule` | Se reutiliza | T4: `qualifies`/`estimate` para recalcular el récord del par |
| `SessionExerciseDao.getBySessionIdWithDetails` | Se reutiliza (se amplía) | T5: nueva columna `ownsCorrectableSet`; la lista es un `Flow` y se refresca sola al registrar o corregir |
| Canal `navigateBack` / `SET_NOTICE_RESULT_KEY` + `Snackbar` de la sesión activa | Se reutiliza | T7, T9: la confirmación breve «Serie N corregida» viaja por el mismo resultado de navegación que el aviso de HU-46 |
| `SetValuesRule` (nueva, `domain/rules`) | Se crea una vez | T2 (`RegisterSetUseCase`) y T3 (`CorrectSetUseCase`): un solo sitio para las validaciones; copiarlas haría que pudieran divergir sin que ningún test lo note |
| `CorrectableSet` (modelo de dominio) | Se crea | T1, T6, T7: no existe un modelo que lleve una serie ya registrada con su id y su implemento |
| `GetCorrectableSetUseCase`, `CorrectSetUseCase` | Se crean | T3: no hay caso de uso que lea o actualice una serie existente (hasta hoy la serie era inmutable) |
| `ExerciseSetDao.getCorrectableSet`, `updateValues`, `getMaxQualifyingWeightForPair` · `ExerciseOneRmDao.deleteByPair` | Se crean | T4: no hay consulta de «la última serie de la sesión en curso», ni update de serie, ni borrado de récord |

### Estándares aplicables

| Regla | Sección del estándar | Aplica a |
|-------|----------------------|----------|
| Código, KDoc y mensajes de validación de dominio en inglés; strings visibles en español en `res/values/strings.xml` | §2.1, §3.6 | T2, T3, T8, T9 |
| Nombres `{Acción}{Entidad}UseCase` con `operator fun invoke`; DAO con prefijo por operación (`get…`, `update…`, `delete…`) | §2.3, §2.4 | T3, T4 |
| Domain sin imports de Android; interfaz de repositorio en Domain, implementación en Data | §3.2, §4.1 | T1, T2, T3, T4 |
| ViewModel no accede a DAOs; las validaciones de dominio viven en Use Cases, no en Composables | §4.1 | T3, T7, T8 |
| Estado como `MutableStateFlow` privado → `StateFlow`; eventos one-shot por `SharedFlow`; nada de `mutableStateOf` en ViewModels | §3.5, §4.2 | T6, T7 |
| Funciones suspend main-safe; transacción atómica en el repositorio (`withTransaction`) | §4.3, §4.4 | T4 |
| Regla pura en `domain/rules`, testeable en JVM sin mocks | §4.5, §5.4 | T2, T10 |
| `!!` solo en tests | §3.3 | todas |
| JUnit 4, Given/When/Then, nombres `` `given …, when …, then …` ``; Use Cases con repositorio mockeado; ViewModels con dispatcher de test y Use Cases mockeados | §5.1–§5.4 | T10–T13 |
| Touch target ≥ 48 dp; padding 16 dp; señales nunca solo por color (la acción lleva ícono ✎ y texto) | §6.3, §6.4 | T8, T9 |
| Trailing commas, 4 espacios, ≤ 120 columnas, sin wildcards | §3.1, §3.2 | todas |
| Toda UI en español en un único `strings.xml` | §6.4 | T8, T9 |

### Tareas de Implementación

#### Fase 1 — Dominio
- [x] **T1: Modelo `CorrectableSet`** con: `setId`, `sessionExerciseId`, `setNumber`, `totalSets`, `exerciseName`, `isBodyweight`, `isIsometric`, `weightKg`, `reps`, `rir`, `captureUnit`, `equipmentTypeId` y `equipmentTypeName` — `domain/model/CorrectableSet.kt` (Base: `domain/model/RegisterSetInfo.kt`)
- [x] **T2: `SetValuesRule.requireValid(weightKg, reps, rir, equipmentTypeName)`**, extraída de `RegisterSetUseCase`. `RegisterSetUseCase` pasa a usarla, sin cambio de comportamiento — `domain/rules/SetValuesRule.kt`, `domain/usecase/session/RegisterSetUseCase.kt` (Base: `domain/rules/ExternalLoadRule.kt`)
- [x] **T3: Contrato de corrección en `SessionRepository`** (`getCorrectableSet(sessionExerciseId): CorrectableSet?` y `correctSet(sessionExerciseId, setId, weightKg, reps, rir, captureUnit)`) y los casos de uso `GetCorrectableSetUseCase` y `CorrectSetUseCase`. Este último valida con `SetValuesRule` y delega; la guarda de ventana la evalúa el repositorio — `domain/repository/SessionRepository.kt`, `domain/usecase/session/GetCorrectableSetUseCase.kt`, `domain/usecase/session/CorrectSetUseCase.kt` (Base: `WithdrawFromSessionUseCase.kt`)

#### Fase 2 — Datos
- [x] **T4: DAO + `SessionRepositoryImpl.correctSet`** en una transacción:
  1. relee la serie de la ventana con `getCorrectableSet`; si ya no es `setId` o la sesión no está `IN_PROGRESS`, lanza `IllegalStateException` (CA-47.07, CA-47.08);
  2. resuelve `hasExternalLoad` con el implemento **registrado**;
  3. persiste peso/unidad con la misma regla del registro: sin carga externa, 0 kg y `KG`;
  4. actualiza `weight_kg`, `reps`, `rir` y `capture_unit`, y nada más: ni `set_number`, ni implemento, ni sesión (CA-47.03, CA-47.05);
  5. recalcula el 1RM del par: `upsert(estimate(max))` o `deleteByPair` (CA-47.11).

  `getCorrectableSet` hace el join a `session_exercise`, `exercise`, `equipment_type`, `session` y `plan_assignment` para el total de series, como `getExerciseInfoForSet` — `data/local/dao/ExerciseSetDao.kt`, `data/local/dao/ExerciseOneRmDao.kt`, `data/repository/SessionRepositoryImpl.kt` (Base: `registerSet`, `withdrawFromSession`)
- [x] **T5: Columna `ownsCorrectableSet`** en `getBySessionIdWithDetails`: `1` solo si `se.id` es el dueño de la última serie de la sesión **y** la sesión está `IN_PROGRESS`. Se propaga a `SessionExerciseDetail.canCorrectLastSet` en `getSessionExercises` — `data/local/dao/SessionExerciseDao.kt`, `domain/model/SessionExerciseDetail.kt`, `data/repository/SessionRepositoryImpl.kt`

#### Fase 3 — UI
- [x] **T6: `ExerciseUiItem.canCorrect`** mapeado desde `SessionExerciseDetail.canCorrectLastSet` — `ui/session/ActiveSessionUiState.kt`, `ui/session/ActiveSessionViewModel.kt`
- [x] **T7: Modo corrección en `RegisterSetViewModel`** (`savedStateHandle["correction"]`):
  - **Carga.** Usa `GetCorrectableSetUseCase` y precarga peso (en su unidad de captura), reps/segundos, RIR e implemento. El implemento va como **opción única** y `onEquipmentSelected` lo ignora. No hay cronómetro y no se muestra `previousSetNotice`.
  - **Ventana cerrada.** Si la serie ya no es corregible (`null`), vuelve a la sesión sin mensaje: la acción ya no existe.
  - **Confirmar.** Misma validación que el registro y después `CorrectSetUseCase`. Vuelve con el mensaje «Serie N corregida».
  - **Estado.** `RegisterSetUiState` gana `isCorrection` y `registeredSummary` (el texto «Registrada: 18 kg · 18 reps · RIR 1») — `ui/session/RegisterSetViewModel.kt`, `ui/session/RegisterSetUiState.kt`
- [x] **T8: `RegisterSetScreen` en modo corrección**:
  - título «Corregir serie N» junto al nombre del ejercicio;
  - tarjeta con lo registrado;
  - `RepsField` con «Segundos sostenidos» y su referencia en isométricos;
  - Confirmar y Cancelar iguales que en el registro.

  Strings nuevos en `strings.xml` — `ui/session/RegisterSetScreen.kt`, `res/values/strings.xml`
- [x] **T9: Acción «✎ Corregir»** bajo el nombre del ejercicio, como sub-acción de la tarjeta (preview E1), en las tres variantes de fila (No iniciado no la tiene nunca: no tiene series). Ruta `register-set/{id}?correction={correction}` y callback `onNavigateToCorrectSet` — `ui/session/ActiveSessionScreen.kt`, `ui/navigation/NavigationRoutes.kt`, `ui/navigation/TensionNavHost.kt`

#### Fase 4 — Tests
- [x] **T10: `SetValuesRuleTest`**: peso < 0, > 500, 0 aceptado sin «Peso Añadido», «Peso Añadido» > 0, reps ≥ 1, RIR 0..2, implemento obligatorio — `test/.../domain/rules/SetValuesRuleTest.kt` (cubre: `QA-09`, `QA-10`, `QA-11`)
- [x] **T11: `CorrectSetUseCaseTest` + `GetCorrectableSetUseCaseTest`**: delega con los valores; rechaza fuera de rango sin llamar al repositorio; propaga el `IllegalStateException` de ventana cerrada — `test/.../domain/usecase/session/` (Base: `WithdrawFromSessionUseCaseTest.kt`, `RegisterSetUseCaseTest.kt`) (cubre: `QA-09`, `QA-10`, `QA-12`)
- [x] **T12: `RegisterSetViewModelTest`, casos de modo corrección**:
  - precarga exacta (kg y lb) e implemento como opción única no cambiable;
  - confirmar llama a `CorrectSetUseCase` y no a `RegisterSetUseCase`, y vuelve con mensaje;
  - botón deshabilitado con datos inválidos;
  - isométrico con campo de segundos y sin cronómetro;
  - «Peso Añadido» editable;
  - ventana cerrada → vuelve sin mensaje.

  `test/.../ui/session/RegisterSetViewModelTest.kt` (cubre: `QA-04`, `QA-05`, `QA-07`, `QA-09`–`QA-11`, `QA-14`, `QA-15`, `QA-20`)
- [x] **T13: `ActiveSessionViewModelTest`**: `canCorrect` refleja `canCorrectLastSet`, y solo un ejercicio la tiene — `test/.../ui/session/ActiveSessionViewModelTest.kt` (cubre: `QA-01`)
- [x] **T14: `CorrectLastSetTest` instrumentado** (Room en memoria, camino de escritura real):
  - la ventana está en el último ejercicio registrado y se mueve al registrar otro o el mismo;
  - corregir actualiza solo esos campos;
  - el número de series no cambia;
  - corregir una serie que ya no es la última lanza;
  - sesión cerrada → sin ventana y corregir lanza;
  - 1RM baja, sube y desaparece;
  - lb → kg canónico;
  - sin carga externa → 0 kg.

  `androidTest/.../CorrectLastSetTest.kt` (Base: `MiUnRmEstimadoTest.kt`, `AjustarSesionDelDiaTest.kt`) (cubre: `QA-02`, `QA-03`, `QA-05`, `QA-06`, `QA-08`, `QA-12`, `QA-13`, `QA-16`, `QA-17`, `QA-18`, `QA-19`, `QA-20`)

  > Requiere emulador o dispositivo: se compila (`compileDebugAndroidTestKotlin`) pero solo se ejecuta con `connectedDebugAndroidTest`.

#### Fase 5 — Documentación
- [x] **T15: Arquitectura**:
  - `domain_and_state_model.md`: matiz de inmutabilidad de `exercise_set`, la ventana de la última serie en sesión en curso;
  - `interfaces_contract.md`: acción «Corregir» en E1, modo corrección de E2, reglas de inmutabilidad matizadas;
  - `architecture_blueprint.md`: D-03 sin cambios post-cierre, con nota de la ventana;
  - `story_mapping_index.md`: HU-47 y sus relaciones.

### Cobertura QA de prioridad Alta

| Caso | Test |
|------|------|
| QA-01 | T13 (+ T14: un solo dueño) |
| QA-02, QA-03, QA-12 | T14 |
| QA-04 | T12 |
| QA-05, QA-06 | T12 + T14 |
| QA-09, QA-10 | T10 + T11 + T12 (QA-09 fila «0 en Barra»: ver hallazgo 6) |
| QA-13 | T14 (datos). La ausencia visual en F2 no cambia: `SessionDetailScreen` no gana ninguna acción |
| QA-16 | T14: la ventana es derivada de la base. El cierre abrupto real del proceso es manual, en emulador |
| QA-17, QA-19 | T14: el resumen y el detalle leen `exercise_set`, y se verifica la fila corregida |
