## Refinamiento Técnico (Developer)
**Autor**: Esteban Colorado González | **Fecha**: 2026-08-30

---

### Contexto

El flujo de registro de serie está completo y estable: `RegisterSetScreen` → `RegisterSetViewModel` → `RegisterSetUiState` → `GetRegisterSetInfoUseCase` / `RegisterSetUseCase` → `SessionRepository` → `SessionRepositoryImpl` → `ExerciseSetDao` → `ExerciseSetEntity`. Esta historia interviene esa cadena de extremo a extremo, más `SessionDetailScreen` para la visualización de la unidad de captura en el historial.

El kilogramo ya es la unidad canónica del sistema (`domain_and_state_model.md` §1 — *Manejo de Valores de Alta Precisión*) y toda la agregación (tonelaje, promedios, KPIs, alertas) opera sobre `exercise_set.weight_kg`. La unidad de captura entra **solo en la frontera de captura y presentación**: ninguna regla del motor (R1–R7), ningún cálculo y ninguna consulta agregada la consulta.

#### Estado actual relevante

| Hecho verificado en código | Consecuencia para esta HU |
|---|---|
| `RegisterSetScreen.WeightField` es un `OutlinedTextField` con sufijo fijo `"Kg"`. **No existen controles − / +** | CA-30.04 y el preview los exigen → se crean en esta historia |
| `RegisterSetViewModel.onWeightChanged` solo valida `parsed < 0`. **No hay límite máximo** | CA-30.05 exige máximo 500 kg → se agrega |
| `RegisterSetUseCase` valida `weightKg >= 0`, `reps >= 1`, `rir in 0..2` | Se agrega `weightKg <= 500` |
| `ExerciseSetEntity` tiene 6 columnas; sin unidad de captura | Se agrega `capture_unit` |
| BD en versión 13, cadena de migraciones 6→13 intacta, `DatabaseModule` **sin** `fallbackToDestructiveMigration`, `exportSchema = true` (`app/schemas/…/13.json`) | Se agrega `MIGRATION_13_14` y se sube a versión 14 |
| `BackupRepositoryImpl` exporta e importa **por columnas del cursor**, de forma genérica | Sin cambios: la columna nueva viaja sola y los respaldos antiguos se resuelven con el `DEFAULT 'KG'` |
| `ActiveSessionScreen` y `SessionSummaryScreen` solo muestran carga prescrita / tonelaje en Kg | CA-30.07 se cumple sin tocarlos |
| `LoadIncrementResolver` resuelve 2.5 / 5.0 kg | Es el incremento de **prescripción de carga** (HU-11), concepto distinto del paso de captura (0.5 kg / 1 lb). No se toca |
| Los tests JVM corren con `isReturnDefaultValues = true`, sin Robolectric. `mockk` y `kotlinx-coroutines-test` disponibles | Las reglas puras se testean directo; el ViewModel se testea con `Context` mockeado |

---

### Decisiones técnicas

#### D1 — Sí se implementa la migración 13 → 14 (desviación deliberada de la HU)

La historia documenta una excepción a RNF19 ("*Beta sin migración: la base de datos se reinicia*"). **Se implementa la migración de todos modos**, porque:

- La cadena 6→13 está completa y `DatabaseModule` **no** declara `fallbackToDestructiveMigration`. Sin migración, cualquier instalación existente lanza `IllegalStateException` al abrir la BD — no "se reinicia sola".
- El cambio es una sola sentencia aditiva: `ALTER TABLE exercise_set ADD COLUMN capture_unit TEXT NOT NULL DEFAULT 'KG'`. El costo es una línea; el beneficio es no perder el historial de entrenamiento del ejecutante.

Es estrictamente aditivo respecto al alcance de la HU: no cambia ningún CA, solo evita una pérdida de datos innecesaria. La validación sobre instalación fresca que pide la HU se sigue haciendo.

#### D2 — La unidad se deriva de la última serie del ejercicio; no se crea tabla ni columna de preferencia

CA-30.03 dice literalmente: *"registró previamente una serie de un ejercicio en libras → el selector aparece preseleccionado en `Lb`"*, y *"un ejercicio sin unidad registrada previamente aparece preseleccionado en `Kg`"*. Eso es exactamente una consulta a la última serie registrada del ejercicio — no hace falta estado de preferencia adicional.

`ExerciseSetDao.getLastCaptureUnitForExercise(exerciseId)` es el espejo de `getLastWeightForExercise`, con **una diferencia deliberada**: no filtra `s.deload_id IS NULL`. La unidad es el rótulo físico de la máquina y no depende de si la sesión fue de descarga; excluir las descargas perdería la unidad correcta sin ningún beneficio. Sí conserva el `COALESCE(se.original_exercise_id, se.exercise_id)` para que la unidad siga al ejercicio original tras una sustitución.

#### D3 — El valor capturado no se persiste; se reconstruye desde kg

Ningún CA pide almacenar el número tal como se tecleó. CA-30.07 pide que *"la unidad de captura original se muestra únicamente en el detalle de la serie individual"* — la **unidad**, no el valor. El detalle del historial reconstruye el valor: `45 lb → 20.41 kg → 20.41 / 0.45359237 = 44.98 → 45.0 lb` a un decimal. La reconstrucción es exacta a un decimal en todo el rango útil (verificado por test de ida y vuelta en `WeightConverterTest`). Se evita así una columna redundante.

#### D4 — El campo de peso siempre expresa la carga física en la unidad activa

Dos momentos de conversión en la UI:

- **Al cargar la pantalla:** se resuelve la unidad y el valor precargado (`lastWeightKg`, que viene en kg) se muestra convertido a esa unidad. Sin esto, preseleccionar `Lb` mostraría un número en kg y el ejecutante volvería a hacer la conversión mental que la historia elimina.
- **Al cambiar el selector (CA-30.08):** el valor del campo se convierte a la nueva unidad. El ejecutante que corrige la unidad quiere ver el número que trae rotulado el implemento, no reinterpretar su propio tecleo.

Alternativa considerada y descartada: dejar el número intacto y reinterpretarlo en la nueva unidad. Es más simple, pero convierte un cambio de selector en un cambio silencioso de la carga real registrada.

#### D5 — Validación como regla pura, no como lógica del ViewModel

Hoy la validación vive en `RegisterSetViewModel`, que depende de `Context` para los mensajes y por eso no es verificable en JVM. Se extrae la decisión a `WeightCaptureValidator` (Kotlin puro, devuelve un `sealed interface` de error) y el ViewModel queda como mapeador error → `strings.xml`. Cumple RNF29 y RNF30 sin emulador, y es coherente con el precedente de `RepsRangeParser` / `LoadDisplayMapper`.

#### D6 — Ubicación de las piezas nuevas

| Pieza | Ruta | Precedente |
|---|---|---|
| Enum de unidad | `domain/model/WeightUnit.kt` | `domain/model/ProgressionClassification` (enum de dominio) |
| Conversión | `domain/util/WeightConverter.kt` | `domain/util/RepsRangeParser.kt` |
| Validación | `domain/util/WeightCaptureValidator.kt` | `domain/util/RepsRangeParser.kt` |
| Selector segmentado | `ui/session/components/WeightUnitSelector.kt` | `ui/session/components/IsometricChronometer.kt` |

El selector se construye con `Surface` + `Row` siguiendo el patrón visual y de accesibilidad de `RirSelector` (48 dp, `semantics { contentDescription }`), en lugar de `SingleChoiceSegmentedButtonRow`: mismo resultado visual, sin introducir una API de M3 no usada aún en el proyecto.

---

### Tareas de Implementación

#### Fase 1 — Dominio puro: unidad, conversión y validación

- [x] **T1: Crear `WeightUnit`** — `domain/model/WeightUnit.kt`

  `enum class WeightUnit(val captureStep: Double) { KG(0.5), LB(1.0) }` con `companion object { fun fromCode(code: String?): WeightUnit }` que resuelve por `name` y cae en `KG` ante `null` o valor desconocido. Se persiste como TEXT usando `name` (`"KG"` / `"LB"`), coherente con los demás enums del esquema. Las etiquetas visibles (`Kg` / `Lb`) viven en `strings.xml`, no en el enum (§2.1 de los estándares: dominio en inglés, UI en español).

- [x] **T2: Crear `WeightConverter`** — `domain/util/WeightConverter.kt` (Base: `domain/util/RepsRangeParser.kt`)

  Kotlin puro, sin imports de Android:
  - `const val LB_TO_KG = 0.45359237`
  - `const val MAX_WEIGHT_KG = 500.0`
  - `fun toKg(value: Double, unit: WeightUnit): Double` — `KG` devuelve el valor; `LB` multiplica y redondea a **2 decimales**. Sin ajuste al múltiplo de 0.5 (regla de negocio 3).
  - `fun fromKg(weightKg: Double, unit: WeightUnit): Double` — inversa, redondeada a **1 decimal** (precisión de captura y presentación).
  - `fun step(value: Double, unit: WeightUnit, increase: Boolean): Double` — aplica `unit.captureStep`, nunca por debajo de 0, redondeado a 1 decimal.

- [x] **T3: Crear `WeightCaptureValidator`** — `domain/util/WeightCaptureValidator.kt`

  `sealed interface WeightCaptureError { NotNumeric, Negative, AboveMax }` y `fun validate(rawInput: String, unit: WeightUnit): WeightCaptureError?`, que convierte a kg **antes** de evaluar el rango (CA-30.05). Entrada en blanco → `null` (el botón de confirmar ya queda deshabilitado por `isConfirmEnabled`, no se muestra error mientras el campo está vacío).

#### Fase 2 — Persistencia

- [x] **T4: Agregar `capture_unit` a `ExerciseSetEntity`** — `data/local/entity/ExerciseSetEntity.kt`

  `@ColumnInfo(name = "capture_unit") val captureUnit: String = WeightUnit.KG.name`.

- [x] **T5: Crear `MIGRATION_13_14` y subir la versión de la BD** — `data/local/database/Migrations.kt`, `data/local/database/TensionDatabase.kt`, `di/DatabaseModule.kt` (Base: `Migrations.MIGRATION_12_13`)

  `ALTER TABLE exercise_set ADD COLUMN capture_unit TEXT NOT NULL DEFAULT 'KG'`. `version = 13` → `14` y `Migrations.MIGRATION_13_14` agregada a `addMigrations(...)`. El build regenera `app/schemas/…/14.json` (`exportSchema = true`).

- [x] **T6: Extender `ExerciseSetDao`** — `data/local/dao/ExerciseSetDao.kt`

  - `ExerciseSetData` gana `captureUnit: String`; se agrega `capture_unit AS captureUnit` a las proyecciones de `getSetsForSessionExercise` y `getLastHistoricalSets`.
  - Nueva `getLastCaptureUnitForExercise(exerciseId: Long): String?` — espejo de `getLastWeightForExercise` con `COALESCE(se.original_exercise_id, se.exercise_id)`, **sin** el filtro `s.deload_id IS NULL` (D2).

#### Fase 3 — Dominio y repositorio

- [x] **T7: Propagar la unidad en el modelo de dominio** — `domain/model/RegisterSetInfo.kt`, `domain/model/ExerciseSessionData.kt`

  - `RegisterSetInfo` gana `captureUnit: WeightUnit`.
  - `SetData` gana `captureUnit: WeightUnit = WeightUnit.KG` — el valor por defecto mantiene intactos los usos del motor de reglas (`ExerciseSessionData`, comparación histórica), que solo leen `weightKg`.

- [x] **T8: Extender el contrato del repositorio y su implementación** — `domain/repository/SessionRepository.kt`, `data/repository/SessionRepositoryImpl.kt`

  - `registerSet(sessionExerciseId, weightKg, reps, rir, captureUnit: WeightUnit)`.
  - `getRegisterSetInfo`: resuelve la unidad con `getLastCaptureUnitForExercise` sobre `originalExerciseId ?: exerciseId`; fuerza `KG` para peso corporal e isométricos (CA-30.06).
  - `registerSet`: persiste `captureUnit.name`, forzando `KG` cuando el ejercicio es de peso corporal o isométrico. La inserción sigue dentro de `database.withTransaction` (RNF11).
  - `getSessionDetail`: mapea `captureUnit = WeightUnit.fromCode(set.captureUnit)` al construir `SetData`.

- [x] **T9: Validar el rango canónico en el caso de uso** — `domain/usecase/session/RegisterSetUseCase.kt`

  Nuevo parámetro `captureUnit: WeightUnit = WeightUnit.KG` y guarda `require(weightKg <= WeightConverter.MAX_WEIGHT_KG) { "Weight must be <= 500 kg" }`. El caso de uso recibe el valor **ya convertido a kg** — es la última barrera sobre el valor canónico, no sobre el capturado.

#### Fase 4 — Interfaz de usuario

- [x] **T10: Rediseñar `RegisterSetUiState`** — `ui/session/RegisterSetUiState.kt`

  - `weightKg: String` → `weightInput: String` (valor en la unidad activa).
  - Nuevos: `captureUnit: WeightUnit = WeightUnit.KG`, `convertedWeightKg: Double?`.
  - Nuevo derivado `isUnitSelectorVisible: Boolean get() = !isBodyweight && !isIsometric` (CA-30.06).
  - `isConfirmEnabled` se ajusta al nuevo nombre de campo; el resto de su lógica no cambia.

- [x] **T11: Crear `WeightUnitSelector`** — `ui/session/components/WeightUnitSelector.kt` (Base: `RirSelector` en `RegisterSetScreen.kt`, `ui/session/components/IsometricChronometer.kt`)

  Dos opciones adyacentes en un `Row` dentro de un `Surface` con borde y esquinas de 4 dp (coherente con `OutlinedTextField`), alto 48 dp y cada mitad con área táctil ≥ 48 × 48 dp (RNF06). La opción activa usa `colorScheme.primary` / `onPrimary`; la inactiva `surfaceContainer` / `onSurface`. `contentDescription` por opción para accesibilidad.

- [x] **T12: Rediseñar el bloque de peso en `RegisterSetScreen`** — `ui/session/RegisterSetScreen.kt`

  - `WeightField` recibe la unidad: etiqueta `Peso (Kg)` / `Peso (Lb)` desde `register_set_weight_label_format`. El sufijo `Kg` dentro del campo se mantiene **solo** cuando el selector está oculto (peso corporal / isométrico), donde no hay otra señal de unidad.
  - `Row` con el campo y, a su derecha, el `WeightUnitSelector` — solo si `isUnitSelectorVisible`.
  - Nuevo `WeightStepControls` privado: botones `−` y `+` de 48 × 48 dp con `contentDescription`, más el hint de incremento activo (`incremento 0,5 kg` / `incremento 1 lb`) en `bodySmall` / `onSurfaceVariant`. Solo visible cuando el peso es editable.
  - Hint informativo `Se guardará como 20,41 Kg` cuando la unidad activa es `Lb` y el valor es válido, en el `supportingText` del campo (reemplazado por el mensaje de error cuando hay error).
  - El resto de la pantalla (cronómetro, reps, RIR, CTA) no se toca.

- [x] **T13: Extender el ViewModel** — `ui/session/RegisterSetViewModel.kt`

  - `init`: toma `info.captureUnit`, precarga `lastWeightKg` **convertido** a esa unidad con `WeightConverter.fromKg` (D4) y publica `convertedWeightKg`.
  - `onWeightChanged`: delega en `WeightCaptureValidator.validate(value, unit)` y mapea el error a `strings.xml`; recalcula `convertedWeightKg`.
  - Nuevo `onUnitSelected(unit: WeightUnit)`: convierte el valor del campo a la nueva unidad (D4), revalida y actualiza `convertedWeightKg`.
  - Nuevo `onWeightStep(increase: Boolean)`: aplica `WeightConverter.step` con el paso de la unidad activa (CA-30.04) y revalida.
  - `onConfirm`: convierte a kg con `WeightConverter.toKg`, revalida y llama `registerSetUseCase(..., captureUnit)`. El `catch (IllegalArgumentException)` mapea también el nuevo error de máximo.

- [x] **T14: Agregar los strings de UI** — `res/values/strings.xml`

  | Clave | Valor |
  |---|---|
  | `register_set_weight_label_format` | `Peso (%1$s)` |
  | `register_set_unit_kg` | `Kg` |
  | `register_set_unit_lb` | `Lb` |
  | `register_set_unit_kg_description` | `Unidad kilogramos` |
  | `register_set_unit_lb_description` | `Unidad libras` |
  | `register_set_weight_decrease` | `Disminuir peso` |
  | `register_set_weight_increase` | `Aumentar peso` |
  | `register_set_increment_hint_format` | `Incremento %1$s` |
  | `register_set_increment_kg` | `0,5 Kg` |
  | `register_set_increment_lb` | `1 lb` |
  | `register_set_converted_hint_format` | `Se guardará como %1$s Kg` |
  | `error_weight_not_numeric` | `Ingresa un valor numérico válido` |
  | `error_weight_max_kg` | `El peso máximo permitido es 500 Kg` |
  | `error_weight_max_lb_format` | `El peso máximo permitido es 500 Kg (%1$s lb)` |
  | `session_detail_set_capture_unit_format` | `capturado como %1$s lb` |

  `error_weight_negative` (`El peso debe ser ≥ 0 Kg`) se reutiliza tal cual. Las etiquetas de peso corporal e isométrico conservan su `(Kg)` literal porque en esos casos no hay selector.

- [x] **T15: Mostrar la unidad de captura en el detalle de la serie** — `ui/history/SessionDetailScreen.kt`

  `SetRow` pasa a `Column`: la línea actual `Serie 1: 20,4 Kg × 10 · RIR 2` intacta y, **solo si `set.captureUnit == WeightUnit.LB`**, una segunda línea `capturado como 45 lb` en `bodySmall` / `onSurfaceVariant`, con el valor reconstruido por `WeightConverter.fromKg` (D3). Tonelaje del ejercicio y de la sesión siguen en Kg sin cambios (CA-30.07).

#### Fase 5 — Tests unitarios (JVM, sin emulador)

- [x] **T16: Crear `WeightConverterTest`** — `test/.../domain/util/WeightConverterTest.kt` (Base: `test/.../domain/util/RepsRangeParserTest.kt`)

  - `45 lb → 20.41 kg` exacto — CA-30.02, CA-30.09
  - `toKg` en `KG` es identidad; `toKg` en `LB` redondea a 2 decimales y **no** cae en múltiplo de 0.5 — CA-30.02
  - `fromKg(20.41, LB) == 45.0` — ida y vuelta a 1 decimal — CA-30.09
  - `step` suma y resta 0.5 en `KG` y 1.0 en `LB`; nunca baja de 0 — CA-30.04
  - Frontera de `MAX_WEIGHT_KG`: `500 kg` y `1102.31 lb` (= 500.00 kg) admitidos; `1102.4 lb` excede

- [x] **T17: Crear `WeightCaptureValidatorTest`** — `test/.../domain/util/WeightCaptureValidatorTest.kt`

  - No numérico (`"abc"`, `"1,5"`, `"--3"`) → `NotNumeric` — CA-30.05
  - Negativo en ambas unidades → `Negative` — CA-30.05
  - `1200 lb` → `AboveMax` (evaluado sobre 544.31 kg, no sobre 1200) — CA-30.05
  - `1100 lb` (= 498.95 kg) → válido: confirma que el límite se aplica al valor convertido y no al capturado
  - Entrada en blanco → `null`

- [x] **T18: Crear `RegisterSetViewModelTest`** — `test/.../ui/session/RegisterSetViewModelTest.kt` (Base: `test/.../ui/session/SubstituteExerciseViewModelTest.kt`)

  `Context` mockeado con `mockk` devolviendo los mensajes; `StandardTestDispatcher` + `SavedStateHandle`.

  - Unidad `LB` en el `RegisterSetInfo` → estado inicial con `captureUnit = LB` y `weightInput` precargado en libras — CA-30.03, D4
  - Ejercicio sin historial de unidad → `KG` — CA-30.03
  - Peso corporal / isométrico → `isUnitSelectorVisible = false` y unidad `KG` — CA-30.06
  - `onUnitSelected(LB)` con `20.41` en el campo → `45.0`, sin error — CA-30.01, CA-30.08
  - `onWeightStep(true)` con paso 1 en `LB` y 0.5 en `KG` — CA-30.04
  - `onConfirm` con `45` en `LB` → `registerSetUseCase(..., 20.41, ..., LB)` — CA-30.02
  - `onConfirm` con `1200` en `LB` → error de máximo y **sin** llamada al caso de uso — CA-30.05

- [x] **T19: Extender los tests existentes afectados** — `test/.../domain/usecase/session/RegisterSetUseCaseTest.kt`, `GetRegisterSetInfoUseCaseTest.kt`, `test/.../ui/history/SessionDetailViewModelTest.kt`

  Ajustar las firmas y construcciones de modelo, y agregar en `RegisterSetUseCaseTest`: `500.0 kg` admitido y `500.01 kg` lanzando `IllegalArgumentException` — CA-30.05.

- [x] **T20: Ejecutar la suite completa** — `./gradlew test`

  Debe quedar en verde al 100 %, sin regresión en los tests del motor de reglas ni en los de backup (la columna nueva viaja por el camino genérico de `BackupRepositoryImpl`).

#### Fase 6 — Documentación (CA-30.10)

- [x] **T21: Actualizar `domain_and_state_model.md`** — `docs/architecture/domain_and_state_model.md`
  - Entidad `exercise_set`: agregar `capture_unit TEXT @notNull @default("KG")` con su comentario (unidad de captura, preferencia de presentación; siempre `KG` para peso corporal e isométricos).
  - §1 *Manejo de Valores de Alta Precisión (Peso en Kg)*: dejar explícita la convención de kilogramo canónico y que la conversión desde libras usa el factor 0.45359237 con 2 decimales, **sin** ajuste al múltiplo de 0.5.
  - Enum `WeightUnit` (`KG` / `LB`) en la sección de enumeraciones.
  - Versión de esquema 13 → 14.

- [x] **T22: Actualizar `interfaces_contract.md`** — `docs/architecture/interfaces_contract.md`
  - `E2-T1` (Registrar Serie de Ejercicio): agregar `capture_unit` al payload, documentando que `weight_kg` llega **ya convertido** y que el rango se valida sobre el valor canónico.
  - Estado de error de `E2-T1`: incluir el máximo de 500 Kg evaluado sobre el valor convertido.

- [x] **T23: Actualizar `architecture_blueprint.md` si declara la versión de esquema o el inventario de columnas** — `docs/architecture/architecture_blueprint.md`

  Verificar y, si aplica, sincronizar la versión de la BD y la mención de `exercise_set`.

---

### Validación manual (no automatizable)

Sobre instalación fresca **y** sobre una instalación existente actualizada (para verificar `MIGRATION_13_14`):

1. Registrar una serie de un ejercicio con carga externa en `Kg`; confirmar el paso de 0,5 kg en − / +.
2. Cambiar a `Lb`, verificar que el valor se convierte, que el paso pasa a 1 lb y que el hint anuncia el kg resultante.
3. Confirmar la serie y volver a entrar al mismo ejercicio: el selector debe aparecer en `Lb` (CA-30.03), incluso tras cerrar la sesión y en una sesión posterior.
4. Registrar la serie siguiente en `Kg` sobre el mismo ejercicio (CA-30.08) y verificar el tonelaje del ejercicio en el resumen post-sesión.
5. Abrir un ejercicio de peso corporal (*Dominadas*) y uno isométrico: sin selector, campo en 0 y no editable (CA-30.06).
6. En el detalle de la sesión en historial: la serie capturada en libras muestra `capturado como 45 lb` bajo el valor en Kg; tonelaje siempre en Kg (CA-30.07).
7. Exportar e importar un respaldo con series en ambas unidades y verificar que la unidad sobrevive el ciclo.
