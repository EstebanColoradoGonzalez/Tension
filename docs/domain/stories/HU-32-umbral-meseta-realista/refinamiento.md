## Refinamiento Técnico (Developer)
**Autor**: Esteban Colorado González | **Fecha**: 2026-08-31

---

### Contexto

El umbral de meseta vive hoy en una sola constante: `ProgressionClassificationRule.PLATEAU_THRESHOLD = 3`, consultada en un único punto (`resolveNewProgressionState`), invocado desde un único llamador (`SessionRepositoryImpl.evaluateProgression`, líneas 621–751). La cadena completa es corta y está localizada:

`CloseSessionUseCase` → `SessionRepository.evaluatePostSession` → `SessionRepositoryImpl.evaluateProgression` → `ProgressionClassificationRule.resolveNewProgressionState` → `exercise_progression.status` / `sessions_without_progression` → alerta `PLATEAU`.

Lo que la historia cambia no es el mecanismo del contador — que se conserva intacto — sino **el número contra el que se compara**. Ese número pasa de constante global a composición de dos datos persistidos que hoy no existen: un atributo del ejercicio y un parámetro del ejecutante.

#### Estado actual relevante

| Hecho verificado en código | Consecuencia para esta HU |
|---|---|
| `ProgressionClassificationRule:11` → `const val PLATEAU_THRESHOLD = 3`, usado en `:54` (`newCounter >= PLATEAU_THRESHOLD -> "IN_PLATEAU"`) | Punto único de cambio. El umbral pasa a parámetro de la función; la constante desaparece |
| `resolveNewProgressionState` ya corta antes en `IN_DELOAD` (`:44`) y `MASTERED` (`:45`) | **CA-32.09 se cumple sin escribir código.** Basta con no tocar esas guardas y cubrirlas con test |
| `resolveNewProgressionState` resetea a `IN_PROGRESSION to 0` ante `POSITIVE_PROGRESSION` (`:48`) | **CA-32.08 se cumple sin escribir código.** Se cubre con test de no regresión |
| El contador `sessions_without_progression` es independiente del umbral: se incrementa siempre y solo se compara al final | **CA-32.07 se cumple por construcción.** Cambiar la dificultad cambia el umbral, no el acumulado |
| `SessionRepositoryImpl:734` → `message = "3 sesiones sin progresión"` **hardcodeado** | El mensaje de la alerta miente en cuanto el umbral deja de ser 3. Se hace dinámico |
| `SessionRepositoryImpl` ya inyecta `profileDao` (`:88`) | El umbral base se lee sin tocar el grafo de Hilt |
| `SessionExerciseForProgression` (`SessionExerciseDao:54`) proyecta `isBodyweight`, `isIsometric`, `muscleGroup` desde `exercise` | La dificultad viaja por el mismo camino: una columna más en la misma proyección |
| `ExerciseEntity` no tiene columna de dificultad; `Exercise` (dominio) tampoco | Columna nueva + migración + propagación por las 4 consultas de `ExerciseDao` que proyectan `ExerciseWithDetails` |
| `profile` es tabla de fila única (`id = 1`) y ya aloja un parámetro de entrenamiento: `weekly_frequency` | El umbral base es un parámetro **de la persona** (así lo argumenta la regla de negocio 2 de la HU). Cabe en `profile` sin inventar tabla de settings |
| `SettingsScreen` es un menú sin ViewModel ni estado | Necesita `SettingsViewModel` + `SettingsUiState` nuevos. La ruta de navegación **no cambia** (el VM entra por `hiltViewModel()` por defecto) |
| `ExerciseDetailScreen` no tiene botón "Guardar": la imagen se persiste al seleccionarla | El selector de dificultad persiste inline, con el mismo patrón. No se introduce un botón de guardado que la pantalla no tiene |
| `WeightUnitSelector` (HU-30) es un segmentado de 2 opciones con 48 dp por opción y `contentDescription` explícito | Precedente exacto para el selector de 3 opciones |
| `BackupRepositoryImpl.exportToJson` vuelca `SELECT *` y recorre columnas por cursor | **El respaldo absorbe las columnas nuevas sin cambios.** HU-19 sin regresión |
| `ExerciseCatalog` (37 ejercicios) es Kotlin puro con test JVM `ExerciseCatalogTest` | La clasificación seed se declara en el catálogo y se verifica sin emulador (RNF31) |
| `androidTest` solo cubre migraciones v6→v7 y v7→v8 | La migración v14→v15 se valida sobre instalación fresca, coherente con la excepción declarada en la HU |
| `CorrectiveActionRule` escalona acciones en 4 y 6 sesiones — valores absolutos calibrados contra el umbral 3 | **Observación fuera de alcance.** Ver "Riesgos y observaciones" |

---

### Decisiones técnicas

#### D1 — El umbral efectivo es una regla pura, no un cálculo en el repositorio

`PlateauThresholdRule` en `domain/rules/`, Kotlin puro. La DoD exige *"regla de umbral efectivo cubierta por pruebas unitarias"* y RNF29/RNF30 lo imponen. Sigue el precedente de `LoadIncrementResolver` y `AlertThresholdRule`: `object` + función pura + constantes del dominio.

```
umbral efectivo = techo(umbral base × multiplicador de dificultad)
```

La regla también es dueña del rango válido (`3..15`) y del valor por defecto (`5`): son constantes de dominio, no de UI. Tanto el caso de uso de Ajustes como el ViewModel consumen las mismas constantes — no se duplica el número 3, el 5 ni el 15 en ninguna otra capa.

`ProgressionClassificationRule.resolveNewProgressionState` recibe `plateauThreshold: Int` como parámetro **sin valor por defecto**. Un default sería un umbral implícito que el compilador dejaría pasar silenciosamente en un llamador nuevo; con el parámetro obligatorio, cada llamador declara de dónde salió su umbral. `PLATEAU_THRESHOLD = 3` se elimina.

#### D2 — La dificultad es un atributo del ejercicio; el umbral base, un atributo del ejecutante

Es la traducción literal de las reglas de negocio 1 y 2 de la historia, y determina dónde vive cada dato:

| Dato | Ubicación | Justificación |
|---|---|---|
| Dificultad de progresión | `exercise.progression_difficulty` | Propiedad intrínseca del ejercicio: depende de su naturaleza y del salto mínimo de su implemento. Varía por ejercicio, no por persona |
| Umbral base | `profile.plateau_base_threshold` | *"El ritmo depende del metabolismo, la genética y la condición corporal"*. Es un parámetro de la persona, y `profile` ya aloja `weekly_frequency`, un parámetro de entrenamiento del mismo tipo |

**Alternativa descartada:** tabla genérica `app_setting` clave-valor. Introduce una entidad nueva, un DAO, un repositorio y un contrato de tipos débilmente tipado para persistir **un** entero, en una app single-user cuya tabla de fila única ya existe y ya cumple exactamente ese papel. Se reconsiderará cuando aparezca el tercer parámetro global sin dueño natural.

Que el control se edite desde Ajustes y no desde Perfil es una decisión de presentación de la HU y no obliga a mover el dato: Ajustes lo lee y lo escribe a través de un caso de uso, igual que cualquier otra pantalla.

#### D3 — Dominio cerrado en inglés, etiquetas en español

`ProgressionDifficulty { LOW, MEDIUM, HIGH }`, persistido como el nombre del enum. Es la convención vigente del proyecto para dominios cerrados — `ExperienceLevel` (`BEGINNER`/`INTERMEDIATE`/`ADVANCED`), `ExerciseProgressionStatus` (`IN_PLATEAU`…), `WeightUnit` (`KG`/`LB`) — frente a la convención de datos de catálogo en español, que aplica a valores libres (`"Sentadilla Búlgara"`, `"Pecho"`), no a enums. `Baja` / `Media` / `Alta` viven en `strings.xml`.

El multiplicador se declara como propiedad del enum (`LOW(1.0)`, `MEDIUM(1.5)`, `HIGH(2.0)`), igual que `WeightUnit(captureStep)`. Mantiene juntos el valor y su semántica, y hace imposible un `when` incompleto. `fromCode(code: String?)` devuelve `MEDIUM` ante nulo o valor desconocido — **CA-32.05 se cumple también en la frontera de lectura**, no solo en el `DEFAULT` de la columna.

#### D4 — Esquema v15 con migración, pese a la excepción concedida

La HU autoriza a no migrar (*"beta sin migración: la base de datos se reinicia"*). Aun así se escribe `MIGRATION_14_15`: son dos `ALTER TABLE ... ADD COLUMN` con `DEFAULT`, el proyecto tiene la cadena de migraciones completa desde v6 y romperla obligaría a documentar un hueco permanente en `domain_and_state_model.md`. El coste de escribirla es menor que el de justificar su ausencia.

```sql
ALTER TABLE exercise ADD COLUMN progression_difficulty TEXT NOT NULL DEFAULT 'MEDIUM';
ALTER TABLE profile  ADD COLUMN plateau_base_threshold INTEGER NOT NULL DEFAULT 5;
```

La migración **no reclasifica el catálogo seed**: CA-32.06 habla explícitamente de *"instalación fresca"*, y reescribir la dificultad de ejercicios ya existentes pisaría una edición manual del ejecutante. Una base migrada queda con todo en `MEDIUM` — el valor por defecto correcto según CA-32.05 — y el ejecutante ajusta lo que le interese. En instalación fresca manda `ExerciseCatalog`.

#### D5 — La clasificación seed se declara en el catálogo, no en el seeder

`SeedExercise` gana `progressionDifficulty: ProgressionDifficulty = MEDIUM`; `ExerciseSeeder` solo la mapea a `ContentValues`. Es la separación que HU-29 ya estableció (*"los datos residen en `ExerciseCatalog`; aquí solo se mapean"*) y la que permite verificar la clasificación en JVM sin emulador.

El valor por defecto del parámetro hace que **los 16 ejercicios de dificultad media no se anoten**: solo se declaran las 13 excepciones `HIGH` y las 8 `LOW`. La lista se toma de la propuesta del PO en el preview, contrastada contra los 37 ejercicios reales del catálogo:

| Dificultad | Ejercicios (id) | Total |
|---|---|:---:|
| `HIGH` ×2 → 10 | Cruce de Polea Alta (2), Curl Bayesian (4), Curl de Concentración (5), Curl de Martillo Cruzado (7), Curl de Predicador (8), Elevación Lateral (10), Extensión de Tríceps Pushdown (12), Extensión de Tríceps sobre Cabeza (13), Face Pull (14), Vuelos Posteriores (26), Aperturas (28), Curl Martillo (30), Rompecráneos (31) | 13 |
| `LOW` ×1 → 5 | Hip Thrust (15), Peso Muerto Rumano (16), Prensa Inclinada (17), Press de Banca Inclinado (18), Press de Banca Plano (19), Remo T Inclinado (21), Sentadilla Hack (24), Press Militar (34) | 8 |
| `MEDIUM` ×1.5 → 8 | El resto del catálogo | 16 |

#### D6 — El umbral base se lee una vez por sesión, no una vez por ejercicio

`evaluateProgression` itera sobre los ejercicios de la sesión. El umbral base es único y no cambia dentro del bucle: se lee antes de entrar (`profileDao.getProfile().first()?.plateauBaseThreshold ?: DEFAULT_BASE_THRESHOLD`) y dentro solo se compone con la dificultad de cada ejercicio. Evita N consultas idénticas y deja explícito que el parámetro es global.

El `?: DEFAULT_BASE_THRESHOLD` no es defensivo por costumbre: `getProfile()` devuelve `Flow<ProfileEntity?>` y el nulo es un estado real del sistema antes del onboarding.

#### D7 — El mensaje de la alerta deja de mentir

`message = "3 sesiones sin progresión"` pasa a construirse con el umbral efectivo del ejercicio. No es cosmético: con dificultad `Alta` y base 5, la alerta afirmaría "3 sesiones" sobre un ejercicio que acumuló 10. El texto completo de la familia de alertas es alcance de HU-33; aquí solo se corrige el número para que no contradiga el dato que la propia alerta representa.

#### D8 — Persistencia inline en el detalle, sin botón de guardado

`ExerciseDetailScreen` no tiene botón "Guardar": la imagen se persiste en el momento de seleccionarla. El selector de dificultad sigue ese patrón — al elegir una opción se invoca el caso de uso y el `Flow` de Room reemite el detalle actualizado. El preview dibuja un botón "Guardar cambios" que la pantalla no tiene hoy; introducirlo obligaría a convertir toda la pantalla a edición con estado sucio, cambio que la HU no pide y que afectaría a campos fuera de su alcance.

El detalle muestra además el umbral efectivo en vivo (*"Se considerará estancado tras N sesiones sin progresar"*), lo que obliga a que `ExerciseDetailViewModel` conozca el umbral base: se inyecta `GetProfileUseCase` y se combinan ambos flujos.

#### D9 — Ubicación de las piezas nuevas

| Pieza | Ruta | Precedente |
|---|---|---|
| Enum de dominio | `domain/model/ProgressionDifficulty.kt` | `domain/model/WeightUnit.kt` |
| Regla de umbral | `domain/rules/PlateauThresholdRule.kt` | `domain/rules/LoadIncrementResolver.kt` |
| Caso de uso de dificultad | `domain/usecase/catalog/UpdateExerciseProgressionDifficultyUseCase.kt` | `domain/usecase/catalog/UpdateExerciseImageUseCase.kt` |
| Caso de uso de umbral base | `domain/usecase/profile/UpdatePlateauBaseThresholdUseCase.kt` | `domain/usecase/profile/UpdateProfileUseCase.kt` |
| Selector segmentado | `ui/catalog/components/ProgressionDifficultySelector.kt` | `ui/session/components/WeightUnitSelector.kt` |
| Estado y VM de Ajustes | `ui/settings/SettingsUiState.kt`, `ui/settings/SettingsViewModel.kt` | `ui/settings/ExportBackupUiState.kt`, `ExportBackupViewModel.kt` |
| Tests de reglas | `test/.../domain/rules/PlateauThresholdRuleTest.kt` | `test/.../domain/rules/LoadIncrementResolverTest.kt` |

---

### Tareas de Implementación

#### Fase 1 — Dominio puro

- [x] **T1: Crear `ProgressionDifficulty`** — `domain/model/ProgressionDifficulty.kt` (Base: `domain/model/WeightUnit.kt`)

  ```kotlin
  enum class ProgressionDifficulty(val thresholdMultiplier: Double) {
      LOW(1.0),
      MEDIUM(1.5),
      HIGH(2.0),
      ;

      companion object {
          fun fromCode(code: String?): ProgressionDifficulty
      }
  }
  ```

  `fromCode` devuelve `MEDIUM` ante `null` o código desconocido (CA-32.05, D3). KDoc en inglés explicando que la dificultad modela la capacidad intrínseca de progresión del ejercicio y que el multiplicador escala el umbral base.

- [x] **T2: Crear `PlateauThresholdRule`** — `domain/rules/PlateauThresholdRule.kt` (Base: `domain/rules/LoadIncrementResolver.kt`)

  ```kotlin
  object PlateauThresholdRule {
      const val DEFAULT_BASE_THRESHOLD = 5
      const val MIN_BASE_THRESHOLD = 3
      const val MAX_BASE_THRESHOLD = 15

      fun effectiveThreshold(baseThreshold: Int, difficulty: ProgressionDifficulty): Int
      fun isValidBaseThreshold(value: Int): Boolean
      fun coerceBaseThreshold(value: Int): Int
  }
  ```

  `effectiveThreshold` = `ceil(baseThreshold.coerceIn(MIN, MAX) * difficulty.thresholdMultiplier).toInt()` (CA-32.01, CA-32.03). El `coerceIn` interno protege contra un valor persistido fuera de rango. `coerceBaseThreshold` sirve al stepper de Ajustes. KDoc en inglés con la fórmula y la tabla base 5 → 5 / 8 / 10.

- [x] **T3: Parametrizar el umbral en `ProgressionClassificationRule`** — `domain/rules/ProgressionClassificationRule.kt`

  Eliminar `const val PLATEAU_THRESHOLD = 3`. `resolveNewProgressionState` gana `plateauThreshold: Int` sin default (D1) y compara `newCounter >= plateauThreshold`. **No se toca nada más de la función**: las guardas de `IN_DELOAD` y `MASTERED` (CA-32.09) y el reset ante `POSITIVE_PROGRESSION` (CA-32.08) quedan exactamente como están. Actualizar el KDoc indicando que el umbral lo resuelve el llamador vía `PlateauThresholdRule`.

#### Fase 2 — Esquema y persistencia

- [x] **T4: Añadir la columna de dificultad a la entidad de ejercicio** — `data/local/entity/ExerciseEntity.kt`

  ```kotlin
  @ColumnInfo(name = "progression_difficulty", defaultValue = "MEDIUM")
  val progressionDifficulty: String = ProgressionDifficulty.MEDIUM.name,
  ```

- [x] **T5: Añadir el umbral base a la entidad de perfil** — `data/local/entity/ProfileEntity.kt`

  ```kotlin
  @ColumnInfo(name = "plateau_base_threshold", defaultValue = "5")
  val plateauBaseThreshold: Int = PlateauThresholdRule.DEFAULT_BASE_THRESHOLD,
  ```

- [x] **T6: Crear `MIGRATION_14_15` y elevar la versión** — `data/local/database/Migrations.kt`, `data/local/database/TensionDatabase.kt`, `di/DatabaseModule.kt` (Base: `MIGRATION_13_14`)

  Los dos `ALTER TABLE` de D4, con comentario en inglés indicando que no se reclasifica el catálogo existente y por qué. `version = 15` en `@Database`. Registrar `Migrations.MIGRATION_14_15` en `addMigrations(...)`. El esquema `15.json` lo genera el build.

- [x] **T7: Propagar la dificultad por `ExerciseDao`** — `data/local/dao/ExerciseDao.kt`

  - `ExerciseWithDetails` gana `progressionDifficulty: String`.
  - Las **cuatro** consultas que proyectan `ExerciseWithDetails` (`getAll`, `getById`, `getNotInVersion`, `getEligibleSubstitutesForSession`) añaden `e.progression_difficulty AS progressionDifficulty`.
  - Nueva: `@Query("UPDATE exercise SET progression_difficulty = :difficulty WHERE id = :exerciseId") suspend fun updateProgressionDifficulty(exerciseId: Long, difficulty: String)` (Base: `updateMediaResource`).

- [x] **T8: Exponer la dificultad en la proyección de progresión** — `data/local/dao/SessionExerciseDao.kt`

  `SessionExerciseForProgression` gana `progressionDifficulty: String`; `getSessionExercisesForProgression` añade `e.progression_difficulty AS progressionDifficulty` (el `INNER JOIN exercise e` ya existe).

- [x] **T9: Añadir la escritura del umbral base** — `data/local/dao/ProfileDao.kt`

  `@Query("UPDATE profile SET plateau_base_threshold = :value WHERE id = 1") suspend fun updatePlateauBaseThreshold(value: Int)`. Escritura puntual en lugar de `@Update` de la entidad completa: evita leer-modificar-escribir un perfil que la pantalla de Ajustes no posee.

#### Fase 3 — Modelos y repositorios

- [x] **T10: Propagar al modelo de dominio de ejercicio** — `domain/model/Exercise.kt`, `domain/repository/ExerciseRepository.kt`, `data/repository/ExerciseRepositoryImpl.kt`

  - `Exercise` gana `progressionDifficulty: ProgressionDifficulty`.
  - `toDomainModel()` mapea con `ProgressionDifficulty.fromCode(progressionDifficulty)` (D3).
  - `createExercise(...)` gana `progressionDifficulty: ProgressionDifficulty` y lo persiste como `.name`.
  - Nuevo: `suspend fun updateProgressionDifficulty(exerciseId: Long, difficulty: ProgressionDifficulty)`.

- [x] **T11: Propagar al modelo de dominio de perfil** — `domain/model/Profile.kt`, `domain/repository/ProfileRepository.kt`, `data/repository/ProfileRepositoryImpl.kt`

  - `Profile` gana `plateauBaseThreshold: Int`.
  - Nuevo: `suspend fun updatePlateauBaseThreshold(value: Int)`.
  - `createProfile` deja que la entidad aplique su default — el onboarding no pregunta por el umbral.

#### Fase 4 — Casos de uso

- [x] **T12: Crear `UpdateExerciseProgressionDifficultyUseCase`** — `domain/usecase/catalog/UpdateExerciseProgressionDifficultyUseCase.kt` (Base: `UpdateExerciseImageUseCase.kt`)

  Delegación directa al repositorio. El tipo enum hace innecesaria la validación de dominio cerrado (CA-32.02).

- [x] **T13: Crear `UpdatePlateauBaseThresholdUseCase`** — `domain/usecase/profile/UpdatePlateauBaseThresholdUseCase.kt` (Base: `UpdateProfileUseCase.kt`)

  Devuelve `Result<Unit>`. Rechaza con `IllegalArgumentException("Plateau base threshold must be between 3 and 15")` cuando `!PlateauThresholdRule.isValidBaseThreshold(value)` (CA-32.04). Mensaje de validación de dominio en inglés, por convención.

- [x] **T14: Añadir la dificultad a `CreateExerciseUseCase`** — `domain/usecase/catalog/CreateExerciseUseCase.kt`

  Parámetro `progressionDifficulty: ProgressionDifficulty = ProgressionDifficulty.MEDIUM` (CA-32.05), propagado al repositorio.

#### Fase 5 — Motor: composición del umbral efectivo

- [x] **T15: Aplicar el umbral efectivo en la evaluación de progresión** — `data/repository/SessionRepositoryImpl.kt` (`evaluateProgression`, líneas 621–751)

  - Antes del bucle: `val baseThreshold = profileDao.getProfile().first()?.plateauBaseThreshold ?: PlateauThresholdRule.DEFAULT_BASE_THRESHOLD` (D6).
  - Dentro del bucle, por ejercicio:
    ```kotlin
    val effectiveThreshold = PlateauThresholdRule.effectiveThreshold(
        baseThreshold,
        ProgressionDifficulty.fromCode(exercise.progressionDifficulty),
    )
    ```
  - Pasar `plateauThreshold = effectiveThreshold` a `resolveNewProgressionState`.
  - `message = "$effectiveThreshold sesiones sin progresión"` (D7).
  - Comentario en inglés sobre la composición base × dificultad y sobre por qué el contador no se reinicia al cambiar la dificultad (CA-32.07).

#### Fase 6 — Datos semilla

- [x] **T16: Declarar la dificultad en el modelo semilla** — `data/local/seed/model/SeedExercise.kt`

  `val progressionDifficulty: ProgressionDifficulty = ProgressionDifficulty.MEDIUM` (D5).

- [x] **T17: Clasificar el catálogo seed** — `data/local/seed/ExerciseCatalog.kt`

  Anotar `progressionDifficulty = ProgressionDifficulty.HIGH` en los 13 ejercicios y `LOW` en los 8 de la tabla de D5. Los 16 restantes quedan en el default. Comentario en español sobre el criterio de clasificación, en línea con los comentarios ya presentes en el archivo.

- [x] **T18: Persistir la dificultad en el seeder** — `data/local/seed/ExerciseSeeder.kt`

  `put("progression_difficulty", exercise.progressionDifficulty.name)` en `insertExercise`.

#### Fase 7 — Interfaz

- [x] **T19: Crear `ProgressionDifficultySelector`** — `ui/catalog/components/ProgressionDifficultySelector.kt` (Base: `ui/session/components/WeightUnitSelector.kt`)

  Segmentado de 3 opciones, 48 dp de alto y `defaultMinSize(minWidth = 48.dp)` por opción (RNF06), `contentDescription` explícito por opción, `Surface` con `RoundedCornerShape(4.dp)` y borde `outline`. Parámetros `selectedDifficulty`, `onDifficultySelected`, `modifier`.

- [x] **T20: Añadir el atributo al detalle de ejercicio** — `ui/catalog/ExerciseDetailUiState.kt`, `ui/catalog/ExerciseDetailViewModel.kt`, `ui/catalog/ExerciseDetailScreen.kt`

  - `ExerciseDetailItem` gana `progressionDifficulty: ProgressionDifficulty` y `effectiveThresholdSessions: Int`.
  - El VM inyecta `GetProfileUseCase` y `UpdateExerciseProgressionDifficultyUseCase`; combina el flujo del ejercicio con el del perfil (`combine`) para calcular el umbral efectivo con `PlateauThresholdRule` (D8).
  - `fun onProgressionDifficultySelected(difficulty: ProgressionDifficulty)` persiste de inmediato; el `Flow` de Room reemite (CA-32.02).
  - La pantalla inserta el selector tras el campo de zona muscular, con etiqueta, texto de ayuda y la línea de umbral efectivo en vivo.

- [x] **T21: Añadir el atributo a la creación de ejercicio** — `ui/catalog/CreateExerciseUiState.kt`, `ui/catalog/CreateExerciseViewModel.kt`, `ui/catalog/CreateExerciseScreen.kt`

  - Estado: `progressionDifficulty: ProgressionDifficulty = ProgressionDifficulty.MEDIUM` (CA-32.05).
  - VM: `fun onProgressionDifficultyChanged(...)`; `onSave` propaga el valor a `CreateExerciseUseCase`.
  - Pantalla: el selector entre las zonas musculares y los checkboxes de tipo, con el texto "Puedes cambiarlo después".

- [x] **T22: Crear el control de umbral base en Ajustes** — `ui/settings/SettingsUiState.kt`, `ui/settings/SettingsViewModel.kt`, `ui/settings/SettingsScreen.kt` (Base: `ui/settings/ExportBackupUiState.kt` / `ExportBackupViewModel.kt`)

  - `SettingsUiState`: `baseThreshold: Int`, `isLoading: Boolean`, `rangeError: String?`, y propiedades derivadas `lowThresholdSessions` / `mediumThresholdSessions` / `highThresholdSessions` calculadas con `PlateauThresholdRule` para el desglose en vivo.
  - `SettingsViewModel`: `@HiltViewModel`, inyecta `GetProfileUseCase` y `UpdatePlateauBaseThresholdUseCase`. `onIncreaseThreshold()` / `onDecreaseThreshold()` ajustan en pasos de 1; los botones se deshabilitan en los extremos (`MIN` y `MAX`) y el `Result.failure` del caso de uso se mapea a `rangeError` (CA-32.04).
  - `SettingsScreen`: sección "Entrenamiento" antes de "Datos", con el stepper `−  N  +` (48×48 dp cada control), la unidad "sesiones", el texto explicativo, el desglose por dificultad y la leyenda de rango. La firma del composable gana `viewModel: SettingsViewModel = hiltViewModel()` — **`TensionNavHost` no cambia**.

- [x] **T23: Añadir los textos** — `res/main/res/values/strings.xml`

  Etiquetas del atributo (`Dificultad de progresión`, `Baja`, `Media`, `Alta` y sus `contentDescription`), el texto de ayuda del diccionario, la plantilla del umbral efectivo (`Se considerará estancado tras %1$d sesiones sin progresar`), y el bloque de Ajustes: título de sección, etiqueta del control, unidad, explicación, plantillas del desglose, leyenda de rango y mensaje de error. Todo en español (ADR-17).

#### Fase 8 — Tests unitarios (JVM, sin emulador)

- [x] **T24: Crear `PlateauThresholdRuleTest`** — `test/.../domain/rules/PlateauThresholdRuleTest.kt` (Base: `test/.../domain/rules/LoadIncrementResolverTest.kt`)

  | Caso | Entrada | Esperado | CA |
  |---|---|---|---|
  | Base por defecto | — | `DEFAULT_BASE_THRESHOLD == 5` | CA-32.01 |
  | Baja ×1 | `5, LOW` | `5` | CA-32.03 |
  | Media ×1.5 con techo | `5, MEDIUM` | `8` (7.5 → 8) | CA-32.03 |
  | Alta ×2 | `5, HIGH` | `10` | CA-32.03 |
  | Techo en el mínimo | `3, MEDIUM` | `5` (4.5 → 5) | CA-32.03, CA-32.04 |
  | Media exacta sin techo | `4, MEDIUM` | `6` | CA-32.03 |
  | Máximo del rango | `15, HIGH` | `30` | CA-32.04 |
  | Rango válido | `3`, `5`, `15` | `true` | CA-32.04 |
  | Rango inválido | `2`, `16`, `0`, `-1` | `false` | CA-32.04 |
  | Acotación | `1 → 3`, `20 → 15`, `7 → 7` | — | CA-32.04 |
  | Valor persistido fuera de rango se acota | `1, LOW` | `3` | D2 |
  | Multiplicadores del enum | `LOW/MEDIUM/HIGH` | `1.0 / 1.5 / 2.0` | CA-32.03 |
  | `fromCode` por defecto | `null`, `""`, `"BAJA"` | `MEDIUM` | CA-32.05 |
  | `fromCode` válido | `"HIGH"` | `HIGH` | CA-32.02 |

- [x] **T25: Ampliar `ProgressionClassificationRuleTest`** — `test/.../domain/rules/ProgressionClassificationRuleTest.kt`

  Actualizar las llamadas existentes a `resolveNewProgressionState` con el parámetro nuevo y añadir:

  | Caso | Escenario | CA |
  |---|---|---|
  | Umbral 5 no dispara en 3 ni en 4 | 4 × `MAINTENANCE`, umbral 5 → sigue `IN_PROGRESSION`, contador 4 | CA-32.01 |
  | Umbral 5 dispara en la quinta | 5ª sesión → `IN_PLATEAU`, contador 5 | CA-32.01 |
  | Umbral 8 (media) no dispara en 5 | umbral 8, contador 5 → `IN_PROGRESSION` | CA-32.03 |
  | Umbral 10 (alta) dispara en la décima | umbral 10 → `IN_PLATEAU` en la décima, no antes | CA-32.03 |
  | Bajar la dificultad con acumulado | contador 6 con umbral 10 → reevaluar con umbral 5 ⇒ `IN_PLATEAU`, contador **7**, no reiniciado | CA-32.07 |
  | Subir la dificultad con acumulado | `IN_PLATEAU` contador 5 con umbral 5 → umbral 10 ⇒ contador 6, sin reinicio | CA-32.07 |
  | Progresión positiva reinicia | contador 7 + `POSITIVE_PROGRESSION` → `IN_PROGRESSION`, contador 0 | CA-32.08 |
  | Descarga excluida | `IN_DELOAD` + `REGRESSION` con umbral 5 → sin cambios | CA-32.09 |
  | Dominado excluido | `MASTERED` + `REGRESSION` con umbral 5 → sin cambios | CA-32.09 |

  El caso `state — IN_PROGRESSION + REGRESSION x3 → IN_PLATEAU, counter 3` pasa a ejecutarse con `plateauThreshold = 3` explícito: documenta que el mecanismo del contador no cambió, solo el número contra el que compara.

- [x] **T26: Ampliar `ExerciseCatalogTest`** — `test/.../data/local/seed/ExerciseCatalogTest.kt`

  | Caso | Aserción | CA |
  |---|---|---|
  | Ningún ejercicio sin dificultad | los 37 tienen un valor del enum | CA-32.05 |
  | Recuento por dificultad | 13 `HIGH`, 8 `LOW`, 16 `MEDIUM` | CA-32.06 |
  | Aislamiento de zona pequeña en alta | la lista de nombres `HIGH` ordenada coincide con la esperada | CA-32.06 |
  | Compuestos pesados en baja | la lista de nombres `LOW` ordenada coincide con la esperada | CA-32.06 |
  | El resto en media | ningún nombre de las dos listas anteriores aparece entre los `MEDIUM` | CA-32.06 |
  | Umbral efectivo del catálogo con base 5 | Elevación Lateral → 10, Prensa Inclinada → 5, Jalón al Pecho → 8 | CA-32.03, CA-32.06 |

- [x] **T27: Crear `UpdatePlateauBaseThresholdUseCaseTest`** — `test/.../domain/usecase/profile/UpdatePlateauBaseThresholdUseCaseTest.kt` (Base: `UpdateProfileUseCaseTest.kt`)

  Éxito en `3`, `5`, `15`; fallo con `IllegalArgumentException` en `2` y `16`; propagación del fallo del repositorio; verificación de que en el caso inválido **no** se invoca al repositorio (CA-32.04).

- [x] **T28: Crear `SettingsViewModelTest`** — `test/.../ui/settings/SettingsViewModelTest.kt` (Base: `test/.../ui/settings/ExportBackupViewModelTest.kt`)

  Carga inicial desde el perfil; incremento y decremento; tope superior en 15 e inferior en 3 sin invocar al caso de uso; desglose por dificultad coherente con `PlateauThresholdRule`; mapeo del `Result.failure` a `rangeError` (CA-32.04).

- [x] **T29: Ejecutar la suite completa** — `./gradlew test`

  Verde al 100 %. Atención a `GetExerciseDetailUseCaseTest`, `GetExercisesUseCaseTest`, `GetAllFilterOptionsUseCaseTest`, `CreateProfileUseCaseTest` y `GetProfileUseCaseTest`: los constructores de `Exercise` y `Profile` cambian de forma y sus fixtures requieren el campo nuevo. Que ningún test de reglas ajeno a la meseta necesite cambios es la evidencia de que HU-10, HU-11 y HU-14 no sufren regresión.

#### Fase 9 — Documentación

- [x] **T30: Actualizar el modelo de dominio** — `docs/architecture/domain_and_state_model.md` (CA-32.10)

  - Cabecera: versión de esquema `14` → `15`.
  - `model exercise`: columna `progression_difficulty` con dominio cerrado y default `MEDIUM`.
  - `model profile`: columna `plateau_base_threshold` con rango `3..15` y default `5`.
  - §4 Dominios Cerrados: `enum ProgressionDifficulty` con los tres valores, su multiplicador y su criterio de asignación.
  - `model exercise_progression`: el comentario de `sessions_without_progression` deja de decir *"Umbral de meseta: 3"* y pasa a describir el umbral efectivo compuesto.
  - §5.3 Ciclo de Vida de `exercise_progression`: sustituir *"Si llega a 3"* por el umbral efectivo; dejar constancia de que cambiar la dificultad no reinicia el acumulado y de que el cambio del umbral base no recalcula estados ya asignados.

- [x] **T31: Actualizar el contrato de interfaces** — `docs/architecture/interfaces_contract.md`

  Flujo D (Catálogo): el detalle y la creación de ejercicio exponen el atributo de dificultad, editable, con `Media` por defecto. Flujo J (Ajustes): el control de umbral base, su rango `3..15`, su efecto sobre las evaluaciones posteriores y la ausencia de recálculo retroactivo.

---

### Riesgos y observaciones

**`CorrectiveActionRule` queda descalibrada — fuera de alcance, se declara.** Sus umbrales de acción escalonada (`MICRO_INCREMENT_THRESHOLD = 4`, `ROTATE_VERSION_THRESHOLD = 6`) son valores absolutos calibrados contra un umbral de meseta de 3: la escalada tenía sentido porque la meseta se declaraba en 3, la primera acción en 4 y la segunda en 6. Con umbral efectivo 5, 8 o 10, cualquier ejercicio en meseta ya supera ambos y las dos acciones correctivas aparecen simultáneamente desde la primera alerta. **No es un defecto introducido por esta historia** — la alerta y sus acciones siguen siendo válidas — pero la gradación se pierde. La HU no lo cubre en ningún criterio y HU-33 (alertas comprensibles y accionables) es su dueño natural: allí procede convertir esos umbrales en desplazamientos relativos al umbral efectivo. Se deja el comportamiento intacto y se levanta como insumo para el refinamiento de HU-33.

**Bases de datos migradas quedan sin clasificación seed.** Consecuencia declarada de D4: quien migre desde v14 verá todo el catálogo en `Media` (umbral 8 con base 5), no en la clasificación de CA-32.06. La HU acota el criterio a instalación fresca y autoriza el reinicio de la base en beta.

---

### Validación manual (no automatizable)

Las reglas se verifican con tests JVM; lo que sigue verifica el cableado real sobre la base de datos.

1. **CA-32.02** — Abrir Elevación Lateral en el Diccionario sobre instalación fresca: la dificultad debe aparecer en `Alta`. Cambiarla a `Baja` y volver a entrar: debe persistir.
2. **CA-32.03** — Con umbral base 5, la línea del detalle debe decir 10 sesiones en `Alta`, 8 en `Media` y 5 en `Baja`, actualizándose al tocar cada opción.
3. **CA-32.04** — En Ajustes, subir el umbral hasta 15: el botón `+` debe deshabilitarse. Bajarlo hasta 3: el `−` debe deshabilitarse. El desglose por dificultad debe recalcularse en cada paso. Salir y volver: el valor debe persistir.
4. **CA-32.05** — Crear un ejercicio sin tocar el selector: debe guardarse en `Media` y mostrarse así en su detalle.
5. **CA-32.06** — Sobre instalación fresca, recorrer el Diccionario y verificar por muestreo la clasificación de la tabla de D5: Prensa Inclinada en `Baja`, Face Pull en `Alta`, Jalón al Pecho en `Media`.
6. **CA-32.01 / CA-32.08** — Con umbral base 5 y un ejercicio en `Baja`, cerrar cuatro sesiones consecutivas manteniendo carga y repeticiones: no debe emitirse alerta de meseta. En la quinta debe emitirse, y su mensaje debe decir 5 sesiones. Registrar entonces una sesión con progresión positiva: la alerta debe resolverse y el ejercicio volver a progresión activa.
7. **CA-32.07** — Sobre un ejercicio en `Alta` con seis sesiones de mantenimiento acumuladas (sin alerta, umbral 10), cambiar la dificultad a `Baja` y cerrar la sesión siguiente: debe entrar en meseta con el acumulado en 7, no reiniciarse a 1.
8. **CA-32.09** — Con un ciclo de descarga activo, cerrar una sesión de descarga: el estado del ejercicio debe permanecer en `IN_DELOAD` y su contador intacto, sin evaluación de meseta.
9. **HU-19 sin regresión** — Exportar un respaldo y verificar que el JSON incluye `progression_difficulty` en `exercise` y `plateau_base_threshold` en `profile`. Importarlo sobre una instalación limpia y comprobar que ambos valores se restauran.
