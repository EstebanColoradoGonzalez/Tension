## Refinamiento Técnico (Developer)
**Autor**: Esteban Colorado González | **Fecha**: 2026-09-11

---

### Contexto

`HU-39` normalizó el equipamiento y `HU-40` reescribió el motor sobre el par. Esta historia no toca ninguno de los dos: **sustituye el catálogo que los alimenta**. Es un frente de datos y presentación —33 zonas donde había 20, jerarquía principal/secundaria, 38 ejercicios reclasificados, delta de plan con 35 sugerencias— sin una sola regla de decisión nueva.

**Feature análoga leída completa: `HU-39` (commit `94aa64f`).** Es el precedente exacto en las tres formas que esta historia necesita:

```kotlin
// EquipmentCatalog.kt — el catálogo cerrado sale del seeder a un object Kotlin puro,
// con ids nombrados. Es lo que hace verificable por test JVM una tabla cerrada de CA.
object EquipmentCatalog {
    const val MAQUINA = 1L
    val ALL: List<SeedEquipmentType> = listOf(SeedEquipmentType(MAQUINA, "Máquina"), …)
    fun byId(id: Long): SeedEquipmentType?
}
```

```kotlin
// ExerciseEquipmentEntity.kt — la tabla de unión con RESTRICT en ambos lados y el índice
// sobre la columna que no encabeza la PK. exercise_muscle_zone ya tiene esta forma.
primaryKeys = ["exercise_id", "equipment_type_id"]
indices = [Index(value = ["equipment_type_id"])]
```

```kotlin
// RemoveExerciseEquipmentUseCase.kt — la validación que devuelve el motivo en vez de
// lanzarlo, porque la pantalla lo presenta como error del campo. CA-41.08 añade un
// tercer motivo a este mismo sealed interface.
sealed interface Result { data object Removed; data object LastOption; data object HasRegisteredSets }
```

```kotlin
// Migrations.kt:1245 — el esquema sube sin migración y la guarda es una constante.
const val LAST_MIGRATED_VERSION = 19   // ADR-019
```

**Segunda análoga: `HU-29` (recategorización de catálogo + delta de plan).** Aporta la lección que esta historia hereda y que sus CAs ya aplican: la recategorización se declara como **tabla cerrada**, no como delta narrado, y un renombrado **conserva identificador, imagen y relaciones**. `Jalón al Pecho` (id 25, asset `tiron_de_dorsales_polea`) es la prueba viva de que funciona.

**Tercera análoga: `EquipmentMultiSelector` + `ProgressionDifficultySelector` (HU-39).** Forma del componente de selección del formulario de ejercicio, y del patrón *persistir al instante sin botón de guardar* de la ficha (`D2-T1`).

#### Superficie real del cambio

| Anillo | Qué contiene | Tamaño |
|---|---|---|
| **Datos semilla** | `MuscleZoneCatalog` (nuevo), `ExerciseCatalog`, `DefaultPlan`, `BaseDataSeeder`, `ExerciseSeeder`, `PlanSeeder`, 1 asset PNG nuevo | 6 archivos + 1 binario |
| **Esquema** | 2 columnas nuevas (`exercise_muscle_zone.is_primary`, `plan_assignment.suggested_equipment_type_id`) + 1 (`muscle_zone.sort_order`) | 3 entidades |
| **Consultas** | `MuscleZoneDao`, `ExerciseDao`, `PlanAssignmentDao`, `SessionExerciseDao` — orden y jerarquía | 4 DAOs |
| **Dominio** | Modelos que hoy aplanan la zona en `List<String>`, más 3 casos de uso nuevos | ~10 archivos |
| **Interfaz** | Formulario de ejercicio, ficha, detalle de versión del plan, registro de serie | 8 archivos + 2 componentes nuevos |

#### El hallazgo que decide el diseño

**El tonelaje y el volumen por grupo muscular no necesitan ni una línea.** `getSetDistributionBySessionIds` y `getTonnageDataBySessionIds` hacen `INNER JOIN exercise_muscle_zone` **sin filtro y sin ponderación**, y agregan por `mz.muscle_group`. Eso es literalmente lo que CA-41.04 exige: cuentan todas las zonas, principales y secundarias, sin peso. Las zonas finas caben dentro de los 14 grupos, así que el eje de agregación no se mueve. La CA se cumple **por no tocar nada**, y lo que aporta la historia es el test que lo fija.

Lo que sí destapa la jerarquía es un defecto latente: **seis consultas eligen «el» grupo muscular del ejercicio con `LIMIT 1` sin `ORDER BY`**, entre ellas `getPrimaryMuscleGroupByExercise`, de la que cuelga la alerta `TONNAGE_DROP`. Hoy devuelven un grupo arbitrario; con hasta cuatro zonas por ejercicio en lugar de una, arbitrario deja de ser inofensivo. Ver D6.

#### Lo que NO se toca

- **Los 14 grupos musculares.** Ninguno se añade ni se quita (CA-41.01).
- **`TonnageRule`, `VolumeDistributionRule` y las demás reglas puras.** Ninguna fórmula cambia (CA-41.04).
- **Las opciones de equipamiento de los 37 ejercicios existentes** (CA-39.06) y **su dificultad de progresión** (CA-41.03).
- **`exercise_progression`, `session_exercise_progression` y el motor por par** de `HU-40`.
- **La relación rutina ↔ día**, la rotación y el versionado del plan (CA-41.06).
- **`LAST_MIGRATED_VERSION`**, que sigue en 19: no hay migración (ADR-019).

---

### Decisiones técnicas

#### D1 — `exercise_muscle_zone` gana `is_primary`, y la PK ya impide la contradicción

Columna `is_primary INTEGER NOT NULL` **sin `defaultValue`**, por el mismo argumento de `HU-39` (D4) y `HU-40` (D1): el esquema cambia por instalación fresca, la tabla nace vacía y un default permitiría escribir una relación sin declarar su jerarquía, que es justo lo que la historia añade.

**La PK compuesta `(exercise_id, muscle_zone_id)` no cambia**, y por eso CA-41.09 —*«una misma zona no puede figurar a la vez como principal y como secundaria»*— queda garantizada por el esquema y no por una validación: una zona es **una fila**, y la fila lleva un solo valor de `is_primary`. No hay estado en el que la contradicción sea representable.

**Se descartó `role TEXT`** (`PRIMARY` / `SECONDARY`). El dominio es estrictamente binario y el proyecto ya expresa booleanos persistidos como `INTEGER` (`is_bodyweight`, `is_custom`, `is_isometric`). Un enum de dos valores en texto añadiría un `Converters` y una clase de error —el valor fuera de rango— que el entero no tiene.

**Se descartó una tabla `exercise_secondary_muscle_zone` separada.** Duplicaría la FK, el índice y las nueve consultas que hoy leen `exercise_muscle_zone`, para expresar un atributo de la relación que cabe en una columna.

#### D2 — El catálogo de zonas sale de `BaseDataSeeder` a `MuscleZoneCatalog`

Hoy las 20 zonas son 20 llamadas a `insertMuscleZone` dentro del seeder, que es código Android (`ContentValues`, `SupportSQLiteDatabase`) y por tanto **no verificable en JVM**. CA-41.01 declara una tabla cerrada de 33 filas con tres retiradas y dos conservadas a propósito: eso necesita test, y el test necesita que el dato sea Kotlin puro.

`MuscleZoneCatalog` nace con la forma exacta de `EquipmentCatalog`: ids nombrados como `const val`, `ALL: List<SeedMuscleZone>` y `byId`. `BaseDataSeeder` queda reducido a mapear, igual que ya hace con el equipamiento.

**Los identificadores de las 17 zonas que sobreviven no se tocan** (HU-29: un renombrado conserva su identificador). Las 16 nuevas ocupan 21-36. Las tres retiradas —`4 Espalda Media`, `7 Hombro`, `19 Antebrazo`— **dejan hueco y no se reutilizan**: reasignar un id retirado haría que cualquier referencia antigua —un comentario, un respaldo, un volcado de depuración— apuntara en silencio a otra cosa.

| Origen | Ids |
|---|---|
| Conservan nombre e id | 5, 10, 11, 12, 13, 16, 17, 20 (8 zonas) |
| Renombradas, mismo id | 1, 2, 3, 6, 8, 9, 14, 15, 18 (9 zonas) |
| Nuevas | 21-36 (16 zonas) |
| Retiradas, id sin reutilizar | 4, 7, 19 |

#### D3 — `muscle_zone` gana `sort_order`, porque el orden del selector es dato y no accidente

El selector de CA-41.02 presenta las 33 zonas **agrupadas por sus 14 grupos**, y tanto el orden de los grupos (Pecho → Hombro → Espalda → … → Cuello) como el de las zonas dentro de cada uno (*Pectoral Superior / Medio / Inferior / Mayor*) son anatómicos, no alfabéticos. `MuscleZoneDao` ordena hoy por `name ASC`, que produce *Inferior, Mayor, Medio, Superior*.

`EquipmentCatalog` resolvió el mismo problema encodando el orden en el id. **Aquí no se puede**: D2 obliga a conservar los ids de las 17 supervivientes, así que el id encoda la historia del catálogo, no su orden de presentación. Los dos criterios no caben en una columna, y el que la interfaz necesita es el orden.

`sort_order INTEGER NOT NULL` = posición en `MuscleZoneCatalog.ALL`. `ORDER BY sort_order ASC` sustituye a `ORDER BY name ASC` en las dos consultas del DAO. La tabla es **cerrada y sembrada** —no hay interfaz para crear zonas, igual que con `equipment_type`— así que el orden lo gobierna íntegramente el seed y no puede quedar incoherente en runtime.

#### D4 — `plan_assignment` gana `suggested_equipment_type_id`, `NOT NULL`

CA-41.07 dice *«ninguna asignación queda sin equipamiento sugerido»* y CA-41.08 que al asignar un ejercicio nuevo *«la sugerencia es obligatoria: no se persiste la asignación sin ella»*. `NOT NULL` es la traducción literal de las dos, y sin default por el argumento de D1.

FK a `equipment_type` con `RESTRICT`, índice sobre la columna. **Sin FK compuesta a `exercise_equipment`**, por el mismo motivo que `HU-40` (D1): declararía la invariante correcta —la sugerencia está entre los implementos admitidos— pero convertiría un rechazo que el caso de uso ya explica con palabras en una excepción de SQLite con peor mensaje, sobre una tabla que la ficha del ejercicio edita en caliente.

La validación vive donde se puede explicar: `SetSuggestedEquipmentUseCase` y `AssignExerciseToVersionUseCase` comprueban contra `exercise_equipment` antes de escribir (CA-41.08).

#### D5 — La precedencia de preselección del selector de serie

CA-41.05 y el preview describen tres estados del selector en `E2-T1`:

| Momento | Preselección | Rótulo |
|---|---|---|
| Primera serie del ejercicio **en la sesión**, con asignación de plan | sugerencia del plan | *sugerido por el plan* |
| Series siguientes, tras cambiarlo | último implemento usado | *último implemento usado* |
| Ejercicio **sin asignación** (añadido en sesión, `HU-43`) | último uso global, o primera opción admitida | — (CA-39.04) |

La precedencia queda: **(1)** implemento de la última serie registrada de *este* `session_exercise` → **(2)** sugerencia del plan de la asignación de esta versión de rutina, si sigue admitida → **(3)** CA-39.04 tal cual está hoy.

**Es la lectura literal del preview, y es una interpretación, no una CA cerrada.** CA-41.05 dice *«las series siguientes»* sin acotar a la sesión; leído sin el preview, podría significar que el último uso **global** manda siempre y la sugerencia solo aparece la primerísima vez. El preview rotula explícitamente *«Primera serie del ejercicio en la sesión»* sobre la sugerencia del plan, y esta es la única lectura que hace verdadero ese rótulo. La alternativa —último uso global por encima de la sugerencia— haría que un plan que sugiere `Mancuerna` no la preseleccionara nunca más después del primer día que se usara `Barra`, dejando a CA-41.07 sin efecto observable.

> **Queda marcada para confirmación del PO en la revisión de este plan.** Es la única decisión de la historia que cambia comportamiento observable según cuál de las dos lecturas se elija.

`RegisterSetInfo` gana `preselectionOrigin: PreselectionOrigin` (`PLAN_SUGGESTION` / `LAST_USED` / `FIRST_OPTION`) para que la pantalla escriba el rótulo correcto sin re-derivarlo.

#### D6 — El grupo muscular «principal» deja de ser arbitrario

Seis consultas eligen un único grupo muscular por ejercicio con `LIMIT 1` **y sin `ORDER BY`**:

| Consulta | Archivo | Consumidor |
|---|---|---|
| `getPrimaryMuscleGroupByExercise` | `SessionExerciseDao:227` | alerta `TONNAGE_DROP`, motor de cierre |
| subconsulta `muscleGroup` ×3 | `ExerciseDao:71, 103, 138` | filtro y ficha del Diccionario |
| subconsulta `muscleGroup` | `PlanAssignmentDao:104` | vista previa de sesión |
| subconsulta `muscleGroup` ×2 | `SessionExerciseDao:146, 301` | sesión activa, historial |

Hoy el ejercicio medio tiene **una** zona y el resultado es correcto por casualidad. Con 87 relaciones sobre 38 ejercicios —hasta cuatro zonas en `Remo T Inclinado`, `Remo Horizontal` y el nuevo `Trapecios con Apoyo en Banco Inclinado`— SQLite elegiría una cualquiera, y el grupo al que se atribuye una caída de tonelaje pasaría a depender del plan de ejecución.

Todas ganan `ORDER BY emz.is_primary DESC, mz.sort_order ASC`. **No es alcance adyacente**: es el defecto que la granularidad de CA-41.01 convierte en visible, y CA-41.04 exige que la alerta `TONNAGE_DROP` siga evaluándose *«sobre los mismos grupos»* — lo que presupone que el grupo esté determinado.

#### D7 — El tonelaje y el volumen no cambian: cambia su test

`getTonnageDataBySessionIds` y `getSetDistributionBySessionIds` ya cuentan todas las zonas sin ponderación y agregan por `muscle_group`. CA-41.04 se cumple sin tocarlas. Lo que la historia añade es **KDoc que declara la invariante y un test que la fija**, para que el día que alguien quiera ponderar por jerarquía el intento falle en rojo y no en silencio.

#### D8 — `RemoveExerciseEquipmentUseCase` gana el tercer motivo

CA-41.08, dirección inversa: *«si al editar un ejercicio se retira una opción de equipamiento que alguna asignación del plan tenía como sugerencia, el sistema lo impide mientras esa asignación exista»*.

Es exactamente la forma que `HU-39` ya construyó —comprobar antes de escribir y devolver el motivo— con un caso más:

```kotlin
sealed interface Result {
    data object Removed : Result
    data object LastOption : Result
    data object HasRegisteredSets : Result
    /** Rutinas cuyo plan lo sugiere, para que el mensaje las nombre. */
    data class SuggestedByPlan(val routineNames: List<String>) : Result
}
```

El orden de comprobación es el del coste de reparación para el ejecutante: última opción → series registradas → sugerencia del plan. Las tres se evalúan antes de la primera escritura.

#### D9 — La jerarquía viaja hasta el dominio como dos listas, no como un flag por elemento

`Exercise.muscleZones: List<String>` pasa a `primaryMuscleZones: List<String>` + `secondaryMuscleZones: List<String>`. Igual en `PlanExercise`, `ExerciseDetailItem` y `CreateExerciseUiState`.

**Se descartó `List<MuscleZoneRef(name, isPrimary)>`.** Las cuatro pantallas que lo consumen presentan las dos listas **por separado** —el preview lo dibuja así en el formulario y en la ficha—, con lo que una lista única obligaría a particionarla en cada `@Composable`. Las consultas ya devuelven las zonas como cadena agregada con `GROUP_CONCAT`; pasar a dos cadenas es un filtro por `emz.is_primary` en la subconsulta, no una estructura nueva.

Los modelos que solo **muestran** zonas sin distinguirlas —`SessionPreviewExercise`, `SessionExerciseDetail`— conservan su campo único: la sesión activa no necesita la jerarquía y forzarla ahí sería ruido.

#### D10 — Respaldo 14 → 15

El mecanismo es genérico: `exportToJson` recorre `TABLE_ORDER_INSERT` y vuelca columnas por cursor, así que las tres columnas nuevas viajan solas y **ninguna tabla se añade al orden**. Lo que no viaja solo es el rechazo: un respaldo v14 tiene `exercise_muscle_zone` sin `is_primary` y `plan_assignment` sin `suggested_equipment_type_id`, ambas `NOT NULL`. Aceptarlo exigiría inventar la jerarquía y la sugerencia, que es la restauración parcial que CA-41.10 prohíbe al pedir que reproduzca el catálogo y el plan *«sin recalcularlos»*.

`SCHEMA_VERSION` 14 → 15, `ACCEPTED_SCHEMA_VERSIONS` sigue siendo el conjunto de uno. Los formatos anteriores a `HU-39` siguen rechazados por la misma vía, que es lo que CA-41.10 pide.

#### D11 — Esquema 21 → 22 sin migración

ADR-019, igual que `HU-39` y `HU-40`. `LAST_MIGRATED_VERSION` se queda en **19**, y la guarda de continuidad de `MigrationV16ToV19Test` debe seguir pasando sin tocarse.

#### D12 — El selector de zona es un diálogo, no una lista embebida

`EquipmentMultiSelector` pinta sus 15 casillas en línea porque caben. **33 zonas no caben**, y el preview lo resuelve con un diálogo con buscador, encabezados de grupo y marcas de *ya elegida aquí* (`✓`) / *ya elegida en el otro campo* (`◦`).

`MuscleZoneHierarchySelector` (dos campos con filas quitables y un botón *+ Añadir zona*) y `MuscleZonePickerDialog` (el diálogo agrupado) son componentes nuevos en `ui/catalog/components/`. El error de CA-41.09 vive **en el campo de principales**, que es donde se incumple, no al pie del formulario.

En la ficha (`D2-T1`) la edición persiste al instante y sin botón de guardar, como el equipamiento, la imagen y la dificultad. En el formulario de creación (`D5-T1`) la validación es previa al guardado y el botón queda deshabilitado mientras no haya una principal.

#### D13 — El ejercicio 38 sustituye a `Remo al Mentón` en el cuarto puesto del viernes

**Cambio de alcance pedido por el PO el 2026-09-11**, posterior a la redacción de la historia. CA-41.06 metía `Remo al Mentón` en el puesto que `Remo Unilateral Polea Alta` deja libre; el puesto pasa a ocuparlo un ejercicio **nuevo**, el primero que esta historia añade al catálogo.

```kotlin
SeedExercise(
    38,
    "Trapecios con Apoyo en Banco Inclinado",
    listOf(MANCUERNA, BARRA, MAQUINA_SMITH),
    primaryMuscleZoneIds = listOf(TRAPECIO, TRAPECIO_INFERIOR),
    secondaryMuscleZoneIds = listOf(ROMBOIDES, DELTOIDES_POSTERIOR),
    "trapecios_con_apoyo_banco_inclinado_mancuernas",
)
```

El `mediaResource` **no lleva el «en»** que el nombre del ejercicio sí tiene: el asset entregado se llama `trapecios_con_apoyo_banco_inclinado_mancuernas.png`. No es un descuido que haya que corregir — `SeedAssetsTest` compara la cadena con el archivo, y el catálogo ya arrastra media docena de `mediaResource` que no derivan del nombre actual (`tiron_de_dorsales_polea` para `Jalón al Pecho`, `sentadilla_de_zumo_mancuerna` para `Sentadilla Sumo`). **El asset no se renombra**, que es la regla que `HU-29` fijó y `HU-39` respetó en sus ocho renombrados.

**Dos mapeos al catálogo de CA-41.01**, que no declara esas zonas con el nombre que el PO usó:

- **«Trapecio Medio» → `Trapecio` (id 17).** La tabla nombra las dos puntas —`Trapecio Superior` y `Trapecio Inferior`— y deja la genérica sin apellido, así que por eliminación la genérica **es** la porción media. CA-41.01 lo respalda al retirar `Espalda Media` por quedar *«cubierta por trapecio, romboides y espalda alta»*. La alternativa —una zona 34 `Trapecio Medio`— abriría la tabla cerrada de CA-41.01 y dejaría a `Trapecio` sin significado propio.
- **«Romboides mayor y menor» → `Romboides` (id 28).** El catálogo los unifica en una zona; separarlos sería la misma apertura de tabla.

**La sugerencia del plan es `Mancuerna`**, que es el valor atómico de la primera opción listada y una opción admitida por el ejercicio — las dos reglas que CA-41.07 fijó para resolver las otras 34.

**`Remo al Mentón` (27) permanece en el Diccionario con la clasificación que CA-41.03 le da** —`Deltoides Lateral` + `Trapecio Superior` principales, `Bíceps Braquial` secundaria—, igual que `Remo Unilateral Polea Alta` (37). Que un ejercicio salga del plan por defecto no lo borra del catálogo; ahora son ocho los que están fuera del plan y dentro del Diccionario.

**El asset PNG era el único artefacto que el desarrollo no podía producir, y ya está entregado.** `trapecios_con_apoyo_banco_inclinado_mancuernas.png` está en `assets/exercises/`, que pasa a tener 38 archivos. `SeedAssetsTest` exige correspondencia 1:1 y hoy afirma *exactamente 37*: la cifra sube en T39, y hasta entonces **el test falla en rojo por el PNG huérfano**, que es el orden correcto — el dato existe antes que el código que lo declara.

**Totales que cambian respecto a la historia escrita:**

| | Historia | Con D13 |
|---|---|---|
| Ejercicios del catálogo | 37 | **38** |
| Relaciones ejercicio ↔ zona | 83 (50 P / 33 S) | **87 (52 P / 35 S)** |
| Relaciones ejercicio ↔ equipamiento | 97 | **100** |
| Assets PNG | 37 | **38** (entregado) |
| Ejercicios fuera del plan por defecto | 7 | **8** |
| Asignaciones del plan | 35 | 35 (sin cambio) |
| Puestos duales | 4 | 4 (sin cambio) |

---

### Tareas de Implementación

#### Fase 1 — Esquema y catálogo de zonas (CA-41.01, CA-41.02)

- [x] **T1: `SeedMuscleZone` y `MuscleZoneCatalog` con las 33 zonas** — `data/local/seed/model/SeedMuscleZone.kt` (nuevo), `data/local/seed/MuscleZoneCatalog.kt` (nuevo) (Base: `seed/model/SeedEquipmentType.kt`, `seed/EquipmentCatalog.kt`)
  Ids nombrados; 17 conservan el suyo, 16 nuevas en 21-36, huecos en 4/7/19 sin reutilizar (D2). `sort_order` = posición en `ALL`, agrupada por los 14 grupos en orden anatómico (D3).
- [x] **T2: `muscle_zone` gana `sort_order`** — `data/local/entity/MuscleZoneEntity.kt`
  Depende de T1. `INTEGER NOT NULL` sin default (D3).
- [x] **T3: `exercise_muscle_zone` gana `is_primary`** — `data/local/entity/ExerciseMuscleZoneEntity.kt` (Base: `entity/ExerciseSetEntity.kt`, columna añadida en HU-39)
  `INTEGER NOT NULL` sin default; PK sin cambios — es la que impide principal y secundaria a la vez (D1).
- [x] **T4: `plan_assignment` gana `suggested_equipment_type_id`** — `data/local/entity/PlanAssignmentEntity.kt` (Base: `entity/ExerciseSetEntity.kt`)
  `NOT NULL` sin default, FK a `equipment_type` con `RESTRICT`, `Index(["suggested_equipment_type_id"])` (D4).
- [x] **T5: `BaseDataSeeder` mapea el catálogo en vez de contenerlo** — `data/local/seed/BaseDataSeeder.kt`
  Depende de T1, T2. Las 20 llamadas inline desaparecen; queda el `forEach` sobre `MuscleZoneCatalog.ALL`, igual que el de equipamiento.
- [x] **T6: Subir el esquema a 22** — `data/local/database/TensionDatabase.kt`
  Depende de T2-T4. `version = 22`. **Sin migración**; `LAST_MIGRATED_VERSION` sigue en 19 (D11, ADR-019).
- [x] **T7: Exportar `22.json`** — `app/schemas/…TensionDatabase/22.json`
  Depende de T6. Lo genera el build (`exportSchema = true`); se versiona en Git.

#### Fase 2 — Datos semilla: ejercicios y plan (CA-41.03, CA-41.06, CA-41.07)

- [x] **T8: `SeedExercise` separa principales de secundarias** — `data/local/seed/model/SeedExercise.kt`
  Depende de T3. `muscleZoneIds: List<Long>` → `primaryMuscleZoneIds` + `secondaryMuscleZoneIds` (D9).
- [x] **T9: Reclasificar los 37 ejercicios y añadir el 38** — `data/local/seed/ExerciseCatalog.kt`
  Depende de T1, T8, T10. La tabla íntegra de CA-41.03 más `Trapecios con Apoyo en Banco Inclinado` (id 38) de D13: **87 relaciones, 52 principales y 35 secundarias**; equipamiento `[Mancuerna, Barra, Máquina Smith]`, dificultad `MEDIUM` y `mediaResource = "trapecios_con_apoyo_banco_inclinado_mancuernas"` —**sin el «en»**, tal como llegó el archivo— para el nuevo. Las constantes privadas de zona se reemplazan por las de `MuscleZoneCatalog`. **Equipamiento y dificultad de los 37 existentes, intactos.**
- [x] **T10: Asset visual del ejercicio 38** — `app/src/main/assets/exercises/trapecios_con_apoyo_banco_inclinado_mancuernas.png` ✅ **Entregado por el PO (2026-09-11)**
  La carpeta pasa a 38 PNG. El nombre del archivo manda sobre el del ejercicio: el asset no se renombra (D13).
- [x] **T11: `ExerciseSeeder` escribe la jerarquía** — `data/local/seed/ExerciseSeeder.kt`
  Depende de T8, T9. `insertExerciseMuscleZone` gana `isPrimary`; dos recorridos, uno por lista.
- [x] **T12: `SeedAssignment` gana la sugerencia** — `data/local/seed/model/SeedAssignment.kt`
  Depende de T4. `suggestedEquipmentTypeId: Long`.
- [x] **T13: Delta de composición y las 35 sugerencias** — `data/local/seed/DefaultPlan.kt`
  Depende de T9, T12. Miércoles y sábado: `Aductores` al puesto 1 con el resto conservando orden relativo; viernes: `Remo Unilateral Polea Alta` (37) → **`Trapecios con Apoyo en Banco Inclinado` (38)** en el puesto 4, 3 series, sugerencia `Mancuerna` (D13). Las 35 sugerencias de CA-41.07 con esa sustitución. Totales intactos: 6 rutinas, 35 asignaciones, 4 puestos duales (lunes 2, martes 1, miércoles 3, viernes 2), rango 8-12.
- [x] **T14: `PlanSeeder` persiste la sugerencia** — `data/local/seed/PlanSeeder.kt`
  Depende de T12, T13. Un `put` más en `insertPlanAssignment`.

#### Fase 3 — Consultas (CA-41.02, CA-41.04, CA-41.05)

- [x] **T15: `MuscleZoneDao` ordena por catálogo** — `data/local/dao/MuscleZoneDao.kt`
  Depende de T2. `ORDER BY name ASC` → `ORDER BY sort_order ASC` en `getAll` y `getWithExercises` (D3).
- [x] **T16: `ExerciseDao` devuelve las zonas partidas por jerarquía** — `data/local/dao/ExerciseDao.kt`
  Depende de T3. Las tres proyecciones ganan `primaryMuscleZones` y `secondaryMuscleZones` vía `GROUP_CONCAT` filtrado por `emz.is_primary`, ordenadas por `mz.sort_order`. `insertAllMuscleZones` y `createExerciseWithRelations` transportan `isPrimary`.
- [x] **T17: El grupo muscular «principal» deja de ser arbitrario** — `data/local/dao/ExerciseDao.kt`, `data/local/dao/PlanAssignmentDao.kt`, `data/local/dao/SessionExerciseDao.kt`
  Depende de T3. Las seis subconsultas `LIMIT 1` y `getPrimaryMuscleGroupByExercise` ganan `ORDER BY emz.is_primary DESC, mz.sort_order ASC` (D6).
- [x] **T18: `PlanAssignmentDao` expone y escribe la sugerencia** — `data/local/dao/PlanAssignmentDao.kt`
  Depende de T4. `PlanAssignmentWithExerciseDetails` y `SessionPreviewExerciseDto` ganan el implemento sugerido con su nombre resuelto; nuevas `updateSuggestedEquipment(routineVersionId, exerciseId, equipmentTypeId)` y `getSuggestionsUsingEquipment(exerciseId, equipmentTypeId)` con los nombres de rutina, para D8.
- [x] **T19: Zonas con jerarquía en el detalle de versión y la vista previa** — `data/local/dao/PlanAssignmentDao.kt`
  Depende de T3, T16. Las proyecciones de zonas del DAO adoptan la partición de T16 donde la pantalla las distingue, y conservan la cadena única donde no (D9).
- [x] **T20: KDoc que fija la invariante de tonelaje y volumen** — `data/local/dao/ExerciseSetDao.kt`, `data/local/dao/SessionExerciseDao.kt`
  **Sin cambio de SQL** (D7). Se declara por escrito que `getSetDistributionBySessionIds` y `getTonnageDataBySessionIds` no filtran por `is_primary` ni ponderan, y por qué (CA-41.04).

#### Fase 4 — Dominio (CA-41.02, CA-41.05, CA-41.08, CA-41.09)

- [x] **T21: Los modelos que distinguen jerarquía** — `domain/model/Exercise.kt`, `domain/model/PlanExercise.kt`, `domain/model/MuscleZone.kt`
  Depende de T16. `muscleZones` → `primaryMuscleZones` + `secondaryMuscleZones` en los dos primeros; `MuscleZone` gana `sortOrder` (D9, D3).
- [x] **T22: `RegisterSetInfo` declara el origen de la preselección** — `domain/model/RegisterSetInfo.kt`, `domain/model/PreselectionOrigin.kt` (nuevo)
  `PLAN_SUGGESTION` / `LAST_USED` / `FIRST_OPTION`, para el rótulo de `E2-T1` (D5).
- [x] **T23: Contrato de los repositorios** — `domain/repository/ExerciseRepository.kt`, `domain/repository/PlanRepository.kt`
  Depende de T18. `createExercise` recibe las dos listas de zonas; nuevas `setMuscleZones`, `getMuscleZonesOfExercise`, `getPlanSuggestionsUsingEquipment`, `setSuggestedEquipment`, y `assignExercise` recibe la sugerencia.
- [x] **T24: Implementación de repositorios** — `data/repository/ExerciseRepositoryImpl.kt`, `data/repository/PlanRepositoryImpl.kt`
  Depende de T16, T18, T23.
- [x] **T25: Crear ejercicio exige al menos una zona principal** — `domain/usecase/catalog/CreateExerciseUseCase.kt`
  Depende de T23. `require(primaryMuscleZoneIds.isNotEmpty())`; secundarias opcionales; `require` de intersección vacía entre ambas listas (CA-41.09).
- [x] **T26: Editar las zonas del ejercicio desde la ficha** — `domain/usecase/catalog/SetExerciseMuscleZonesUseCase.kt` (nuevo) (Base: `usecase/catalog/AddExerciseEquipmentUseCase.kt`)
  Depende de T23. Persiste al instante; mismas dos invariantes que T25 (CA-41.02, CA-41.09).
- [x] **T27: Retirar un implemento que el plan sugiere queda impedido** — `domain/usecase/catalog/RemoveExerciseEquipmentUseCase.kt`
  Depende de T23. Cuarto caso `SuggestedByPlan(routineNames)`, comprobado antes de escribir (D8, CA-41.08).
- [x] **T28: La sugerencia se valida contra las opciones del ejercicio** — `domain/usecase/plan/SetSuggestedEquipmentUseCase.kt` (nuevo), `domain/usecase/plan/AssignExerciseToVersionUseCase.kt`, `domain/usecase/plan/AddAlternativeToSlotUseCase.kt`
  Depende de T23. La sugerencia es obligatoria al asignar y debe pertenecer a `exercise_equipment`; cada ejercicio de un puesto dual lleva la suya (CA-41.05, CA-41.08).
- [x] **T29: La preselección del selector de serie** — `data/repository/SessionRepositoryImpl.kt`
  Depende de T18, T22. `getRegisterSetInfo` aplica la precedencia de D5 y devuelve el origen. Nueva consulta: implemento de la última serie **de este `session_exercise`**.

#### Fase 5 — Interfaz (CA-41.02, CA-41.05, CA-41.09)

- [x] **T30: Selector de zonas con jerarquía** — `ui/catalog/components/MuscleZoneHierarchySelector.kt` (nuevo), `ui/catalog/components/MuscleZonePickerDialog.kt` (nuevo) (Base: `ui/catalog/components/EquipmentMultiSelector.kt`)
  Depende de T21. Dos campos con filas quitables y *+ Añadir zona*; diálogo con buscador, encabezados de los 14 grupos, marca de elegida aquí y de elegida en el otro campo, y bloqueo de la doble pertenencia (D12, CA-41.09).
- [x] **T31: Formulario de creación con las dos listas** — `ui/catalog/CreateExerciseUiState.kt`, `ui/catalog/CreateExerciseViewModel.kt`, `ui/catalog/CreateExerciseScreen.kt`
  Depende de T25, T30. `selectedMuscleZoneIds` → dos conjuntos; `canSave` exige principal no vacía; error inline en el campo de principales (CA-41.09).
- [x] **T32: Ficha del ejercicio: jerarquía visible y editable** — `ui/catalog/ExerciseDetailUiState.kt`, `ui/catalog/ExerciseDetailViewModel.kt`, `ui/catalog/ExerciseDetailScreen.kt`
  Depende de T26, T27, T30. *Zona principal* y *Zonas secundarias* como bloques distintos; edición que persiste al instante; el nuevo motivo de rechazo de T27 se presenta como error del campo de equipamiento.
- [x] **T33: Detalle de versión del plan con la sugerencia** — `ui/catalog/PlanVersionDetailUiState.kt`, `ui/catalog/PlanVersionDetailViewModel.kt`, `ui/catalog/PlanVersionDetailScreen.kt`
  Depende de T18, T28. Cada asignación muestra su implemento sugerido —incluidas las alternativas de puesto dual, cada una con la suya—; el diálogo de edición ofrece **solo** las opciones del ejercicio; la hoja de asignación exige elegir una (CA-41.05, CA-41.07, CA-41.08).
- [x] **T34: El selector de serie rotula de dónde viene la preselección** — `ui/session/RegisterSetViewModel.kt`, `ui/session/RegisterSetUiState.kt`, `ui/session/RegisterSetScreen.kt`
  Depende de T22, T29. *sugerido por el plan* / *último implemento usado*; sin rótulo cuando es la primera opción admitida (D5).
- [x] **T35: Cadenas nuevas** — `res/values/strings.xml`
  Depende de T30-T34. Etiquetas de los dos campos, título del diálogo, error de zona principal, rechazo de sugerencia no admitida, impedimento por sugerencia del plan, rótulos de origen. Español.

#### Fase 6 — Respaldo (CA-41.10)

- [x] **T36: Formato 15 y rechazo del 14** — `data/repository/BackupRepositoryImpl.kt`
  Depende de T3, T4. `SCHEMA_VERSION` 14 → 15; `ACCEPTED_SCHEMA_VERSIONS` sigue siendo el conjunto de uno; `TABLE_ORDER_INSERT` **sin cambios** — no hay tabla nueva (D10).

#### Fase 7 — Tests unitarios (JVM, sin emulador)

- [x] **T37: El catálogo de zonas** — `test/…/data/local/seed/MuscleZoneCatalogTest.kt` (nuevo) (Base: `ExerciseCatalogTest.kt`)
  33 zonas exactas con su grupo; los 14 grupos sin añadidos; `Hombro`, `Antebrazo` y `Espalda Media` ausentes; `Core` ausente; `Abductores` y `Cuello` presentes y sin ejercicio; ids únicos, los 17 conservados intactos y los retirados sin reutilizar; `sort_order` denso, único y agrupado por grupo.
- [x] **T38: La reclasificación de los 38** — ampliar `test/…/data/local/seed/ExerciseCatalogTest.kt`
  Depende de T9, T37. La tabla de CA-41.03 ejercicio a ejercicio más el 38 de D13; 38 ejercicios, 87 relaciones (52 P / 35 S), 100 de equipamiento; ninguno sin principal; ninguna zona a la vez principal y secundaria; toda zona existe en el catálogo; **equipamiento y dificultad de los 37 existentes idénticos a HU-39** (el test que prueba que la historia no los tocó).
- [x] **T39: Assets 1:1 con el catálogo** — ampliar `test/…/data/local/seed/SeedAssetsTest.kt`
  Depende de T9, T10. `exactamente 37 png` → 38; el resto de las aserciones son invariantes y no cambian de forma. `no orphan png without a catalog exercise` es el que hoy está en rojo y el que se pone en verde al declarar el 38 en T9 (D13).
- [x] **T40: Delta del plan y las 35 sugerencias** — ampliar `test/…/data/local/seed/DefaultPlanTest.kt`
  Depende de T13. Las seis composiciones de CA-41.07 con su orden; `Aductores` primero el miércoles y el sábado; **`Trapecios con Apoyo en Banco Inclinado` en el puesto 4 del viernes con sugerencia `Mancuerna`**; `Remo al Mentón` y `Remo Unilateral Polea Alta` fuera del plan pero presentes en el catálogo (8 en total); 35 asignaciones, 4 puestos duales, rango 8-12; **toda sugerencia pertenece a `exercise_equipment` de su ejercicio** (CA-41.07, verificación de CA-41.08); el puesto dual del martes con dos sugerencias distintas.
- [x] **T41: Tonelaje y volumen cuentan todas las zonas** — ampliar `test/…/domain/rules/TonnageRuleTest.kt`, `test/…/domain/rules/VolumeDistributionRuleTest.kt`
  Depende de T20. Un ejercicio con dos principales y dos secundarias reparte a sus grupos sin ponderación; el eje sigue siendo el grupo (CA-41.04, D7).
- [x] **T42: Las tres validaciones del ejercicio** — ampliar `test/…/domain/usecase/catalog/CreateExerciseUseCaseTest.kt`, nuevo `SetExerciseMuscleZonesUseCaseTest.kt`
  Depende de T25, T26. Sin principal → rechazo; secundarias vacías → aceptado; zona en ambas listas → rechazo (CA-41.09).
- [x] **T43: La sugerencia fuera de las opciones** — nuevo `test/…/domain/usecase/plan/SetSuggestedEquipmentUseCaseTest.kt`, ampliar `RemoveExerciseEquipmentUseCaseTest.kt`, `UpdatePlanAssignmentUseCaseTest.kt`
  Depende de T27, T28. Sugerir un implemento no admitido → rechazo; asignar sin sugerencia → rechazo; retirar un implemento que el plan sugiere → `SuggestedByPlan` con la rutina nombrada, **sin escritura** (CA-41.08).
- [x] **T44: La precedencia de preselección** — ampliar `test/…/ui/session/RegisterSetViewModelTest.kt`, `test/…/domain/usecase/session/GetRegisterSetInfoUseCaseTest.kt`
  Depende de T29. Primera serie con asignación → sugerencia del plan y su rótulo; tras cambiar → último usado; sin asignación → CA-39.04 intacto; sugerencia ya no admitida → se cae al siguiente nivel (D5).
- [x] **T45: Respaldo formato 15** — ampliar `test/…/data/repository/BackupRepositoryImplTest.kt`
  Depende de T36. `schemaVersion: 15`; las tres columnas nuevas presentes; rechazo del 14 con mensaje (D10, CA-41.10).
- [x] **T46: Adaptar los tests que cambian de firma** — `GetExerciseDetailUseCaseTest.kt`, `GetExercisesUseCaseTest.kt`, `GetAllFilterOptionsUseCaseTest.kt`, `GetFilterOptionsUseCaseTest.kt`, `GetWeekDayPlanUseCaseTest.kt`, `AddAlternativeToSlotUseCaseTest.kt`, `VolumeViewModelTest.kt`, `TrendViewModelTest.kt`
  Mecánico: modelos que pasan de una lista de zonas a dos, y asignaciones que ganan sugerencia.
- [x] **T47: Sembrado instrumentado** — `androidTest/…/CatalogoSembradoTest.kt`
  Depende de T5, T11, T14. Invariantes, no cifras: ninguna relación sin `is_primary` válido; ningún ejercicio sin principal; ninguna asignación sin sugerencia; toda sugerencia dentro de `exercise_equipment`; ninguna zona huérfana de grupo. `MigrationV16ToV19Test` **no cambia** y debe seguir pasando (D11).

#### Fase 8 — Documentación (DoD)

- [x] **T48: La historia recoge el cambio de alcance** — `HU-41-…/historia.md`, `HU-41-…/41.preview.txt`
  Depende de T13. CA-41.03 pasa a declarar 38 ejercicios con la fila del 38; CA-41.06 y CA-41.07 nombran `Trapecios con Apoyo en Banco Inclinado` en el cuarto puesto del viernes; la frontera de alcance añade que `Remo al Mentón` tampoco entra al plan. El preview actualiza el bloque del viernes y el del registro de serie (D13).
- [x] **T49: Modelo de dominio** — `docs/architecture/domain_and_state_model.md`
  Esquema 22; `muscle_zone` con `sort_order` y las 33 filas de §6.1; `exercise_muscle_zone` con `is_primary`; `plan_assignment` con `suggested_equipment_type_id`; datos semilla de los 38 ejercicios y las 35 asignaciones.
- [x] **T50: Contrato de interfaces** — `docs/architecture/interfaces_contract.md`
  `D1-T1` (filtro sobre las zonas nuevas), `D2-T1` y `D5-T1` (jerarquía visible y editable, validación), `D4-T1` y `D6-T1` (sugerencia por asignación y su edición acotada), `E2-T1` (precedencia de preselección y su rótulo), `G2-T1`/`G3-T1` (todas las zonas cuentan), `J2-T1`/`J3-T1` (formato 15).
- [x] **T51: Blueprint** — `docs/architecture/architecture_blueprint.md`
  Esquema 22; ADR-019 ampliado con v22; `MuscleZoneCatalog` entre los datos semilla.
- [x] **T52: Índice de historias y artefactos de la HU** — `docs/domain/stories/story_mapping_index.md`, `HU-41-…/index.md`, `cambios.md`, `dev-record.md`
  `HU-41` a `Lista para Revisión`; fases, métricas de tiempo y registro cronológico, incluido el cambio de alcance de D13.

---

### Riesgos y observaciones

**El riesgo dominante es de datos, no de diseño.** Tres tablas cerradas —33 zonas, 87 relaciones de ejercicio, 35 sugerencias— transcritas a mano desde las tablas de la historia. Un id equivocado no lanza ninguna excepción: produce un catálogo plausible y una métrica silenciosamente falsa, que es exactamente el problema que la historia existe para arreglar. La mitigación es que **T37, T38 y T40 transcriben las mismas tablas de forma independiente**, desde el texto de la CA y no desde el código, y las comparan. Si las dos transcripciones coinciden y ambas son erróneas, el error está en la historia, no en la implementación.

**El asset del ejercicio 38 ya está (T10 cerrada), y deja el repositorio en rojo hasta T9.** `SeedAssetsTest` afirma *exactamente 37 png* y *ningún png huérfano*, y ahora hay 38 archivos con uno que ningún ejercicio declara. Es el orden correcto —el dato llega antes que el código que lo nombra— pero significa que **el árbol no compila verde desde el principio de la implementación**, y que un rojo en `SeedAssetsTest` durante las Fases 1-2 es esperado y no una regresión.

**El `mediaResource` no deriva del nombre del ejercicio, y eso es normal aquí.** El archivo llegó como `trapecios_con_apoyo_banco_inclinado_mancuernas` y el ejercicio se llama *Trapecios con Apoyo **en** Banco Inclinado*. El catálogo ya tiene seis casos peores (`tiron_de_dorsales_polea` → `Jalón al Pecho`), porque `HU-29` y `HU-39` fijaron que **el asset no se renombra**. El riesgo real no es la discrepancia sino teclearla mal: es una cadena literal sin verificación en compilación, y solo `SeedAssetsTest` la atrapa.

**Los dos mapeos de zona de D13 son interpretación mía, no del PO.** «Trapecio Medio» → `Trapecio` y «Romboides mayor y menor» → `Romboides`. La alternativa es abrir la tabla cerrada de CA-41.01 a 34 o 35 zonas, lo que arrastraría `MuscleZoneCatalog`, su `sort_order`, el test T37 y el bloque de catálogo del preview. El cambio es de una línea en `ExerciseCatalog` si el PO prefiere la granularidad real, pero **no** de una línea si además hay que crear las zonas.

**La verificación cruzada de T40 es la que más aporta.** *«Toda sugerencia pertenece a `exercise_equipment` de su ejercicio»* cruza dos tablas escritas en historias distintas —CA-41.07 de esta y CA-39.06 de `HU-39`— y es la única forma de detectar en JVM lo que si no aparecería como un rechazo de `SetSuggestedEquipmentUseCase` sobre datos semilla, en tiempo de ejecución y sobre el dispositivo.

**La precedencia de preselección (D5) es la única decisión de comportamiento abierta**, y está marcada para confirmación del PO. Las dos lecturas son defendibles a partir del texto de CA-41.05; el preview desempata, pero el preview está rotulado *PROTOTIPO*. Si el PO elige la otra lectura, el cambio se localiza en T29 y su test T44 — ninguna otra tarea se mueve.

**Las seis consultas de D6 son un arreglo que la historia hace posible, no que pide.** Se incluyen porque `LIMIT 1` sin `ORDER BY` sobre cuatro zonas es no determinista y de ahí cuelga la alerta `TONNAGE_DROP`, que CA-41.04 exige que siga funcionando igual. Queda declarado que es una corrección aprovechada, no alcance adyacente colado.

**El ejercicio 38 es el primer ejercicio semilla que el proyecto añade desde `HU-29`, y llega por un cambio de alcance posterior a la historia.** La historia escrita dice 37 en tres CAs distintas; T48 existe para que el documento y el código no diverjan. Un catálogo que el `historia.md` describe mal es exactamente la deuda que `HU-29` enseñó a no aceptar.

**`Remo al Mentón` cambia de catalogación y ya no entra al plan.** `HU-29` lo dejó como `Espalda Alta`; CA-41.03 lo pasa a `Deltoides Lateral` + `Trapecio Superior` con `Bíceps Braquial` secundario, y D13 le retira el puesto del viernes que la historia le había dado. Sigue en el Diccionario. El comentario de `ExerciseCatalog` que hoy dice *«Recatalogado en HU-29: movimiento de Espalda Alta, no de Hombro y Trapecio»* queda obsoleto y se reescribe, no se borra: la trazabilidad de por qué cambió dos veces vale más que un catálogo limpio.

**El respaldo de ayer no sirve mañana, otra vez.** Tercera historia consecutiva que invalida el formato. Es coherente con ADR-019 y con lo que `HU-39` y `HU-40` ya hicieron, pero acumula: quien exportó bajo `HU-40` tampoco podrá restaurar.

**La ficha del ejercicio gana edición de zonas, que hoy no tiene.** CA-41.02 la pide en `D2-T1` explícitamente, y `HU-43` reutiliza ese formulario. Es la tarea de interfaz con más superficie nueva (T30 + T32) y la que más se beneficia de validación manual, porque no hay tests de UI instrumentados en el proyecto.

**Ningún test de UI instrumentado, igual que en `HU-40`.** Las pantallas con comportamiento nuevo se cubren por ViewModel y por caso de uso; el ensamblado visual del diálogo de 33 zonas queda en validación manual.

---

### Validación manual (no automatizable)

1. **Instalación fresca.** Desinstalar e instalar: debe abrir sin excepción de Room. Sobre una base v21 sin desinstalar debe fallar — es ADR-019, no un defecto.
2. **El catálogo de zonas.** En el filtro del Diccionario (`D1-T1`) y en el selector del formulario: 33 zonas, agrupadas por sus 14 grupos, en orden anatómico y no alfabético. `Hombro`, `Antebrazo` y `Espalda Media` no aparecen; `Abductores` y `Cuello` sí, sin ejercicio detrás.
3. **El ejercicio 38.** `Trapecios con Apoyo en Banco Inclinado` aparece en el Diccionario con su imagen, con `Mancuerna · Barra · Máquina Smith` como implementos admitidos, `Trapecio` y `Trapecio Inferior` como principales y `Romboides` y `Deltoides Posterior` como secundarias.
4. **La jerarquía en la ficha.** `Press de Banca Inclinado`: *Pectoral Superior* como principal, *Deltoides Anterior* y *Tríceps Braquial* como secundarias, visiblemente distintas.
5. **Crear un ejercicio sin zona principal.** El botón de guardar queda deshabilitado y el aviso aparece **en el campo de principales**, no al pie. Al añadir una, se habilita. Intentar elegir como secundaria una zona ya principal no debe ser posible.
6. **El plan sugiere.** En `D4-T1` del viernes: `Trapecios con Apoyo en Banco Inclinado` en el cuarto puesto con `Mancuerna`. En el puesto dual del martes, `Jalón al Pecho` con `Polea` y `Dominadas` con `Barra Fija` — dos sugerencias distintas en el mismo puesto.
7. **La sugerencia acotada (`D6-T1`).** Editar la del ejercicio 38: el diálogo ofrece **solo** Mancuerna, Barra y Máquina Smith. Asignar un ejercicio nuevo sin elegir sugerencia no debe persistir.
8. **El impedimento inverso (CA-41.08).** En la ficha del ejercicio 38, intentar quitar `Mancuerna`: rechazo que **nombra la rutina**, y la casilla vuelve a su sitio.
9. **La preselección (CA-41.05).** Primera serie del ejercicio 38 el viernes: `Mancuerna`, rotulado *sugerido por el plan*. Cambiar a `Barra` y registrar: la segunda serie nace en `Barra`, rotulada *último implemento usado*.
10. **El orden del plan (CA-41.06).** Miércoles y sábado abren con `Aductores`. `Remo al Mentón` y `Remo Unilateral Polea Alta` no están en el plan del viernes pero **sí en el Diccionario**, asignables a mano y elegibles como alternativa de puesto.
11. **Las métricas (CA-41.04).** Tras una sesión de `Press de Banca Inclinado`, el volumen reparte a `Pecho`, `Hombro` y `Tríceps` — las tres zonas cuentan. Los grupos del eje siguen siendo 14 y el tonelaje total no cambia respecto al mismo entrenamiento antes de la historia.
12. **Respaldo.** Exportar y comprobar `schemaVersion: 15`, `is_primary` en `exercise_muscle_zone` y `suggested_equipment_type_id` en `plan_assignment`. Restaurar: catálogo y plan reproducidos sin recalcular. Restaurar un respaldo v14: rechazo con mensaje, sin importar nada.
