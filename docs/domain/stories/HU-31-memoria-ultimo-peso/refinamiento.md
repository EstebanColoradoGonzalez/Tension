## Refinamiento Técnico (Developer)
**Autor**: Esteban Colorado González | **Fecha**: 2026-08-31

---

### Contexto

La cadena de precarga es corta y está localizada: `RegisterSetViewModel.init` → `GetRegisterSetInfoUseCase` → `SessionRepository.getRegisterSetInfo` → `SessionRepositoryImpl.getRegisterSetInfo` (líneas 313–365) → `ExerciseSetDao` + `ExerciseProgressionDao` + `SessionExerciseDao`. Toda la decisión de "qué peso precargo" vive hoy dentro de un `if / else if / else` en el repositorio, sin regla pura ni test.

Esta historia **no toca el esquema**: no hay columnas nuevas, no hay migración. Todo el cambio es resolución de precedencia y dos consultas nuevas de lectura.

#### Estado actual relevante

| Hecho verificado en código | Consecuencia para esta HU |
|---|---|
| `SessionRepositoryImpl:338-339` → `prescribedLoad = progression?.prescribedLoadKg?.takeIf { it > 0.0 }` y luego `prescribedLoad ?: getLastWeightForExercise(...)` | La prescripción gana **siempre** que exista y sea > 0. La memoria nunca se alcanza en la práctica |
| `SessionRepositoryImpl:695` → cuando el doble umbral **no** se cumple: `prescribedLoadKg = currentProgression.prescribedLoadKg ?: currentData.avgWeightKg` | **Causa raíz del defecto.** En la primera evaluación `prescribed_load_kg` queda fijado al promedio de esa sesión y **nunca vuelve a moverse** salvo que se cumpla el umbral. Los aumentos manuales del ejecutante no lo actualizan → la precarga se congela en el valor inicial |
| `ExerciseSetDao.getLastWeightForExercise` ordena por `es.id DESC` sin distinguir sesión actual de sesión previa, y **no** filtra `s.status` | Los niveles 2 y 3 de la precedencia están fusionados en una consulta implícita; entran series de sesiones aún `IN_PROGRESS` distintas de la actual |
| Todas las consultas de `ExerciseSetDao` usan `COALESCE(se.original_exercise_id, se.exercise_id)` → resuelven por **slot**, no por ejercicio ejecutado | CA-31.08 exige lo contrario para la memoria → se cambia solo en las consultas de memoria y unidad |
| `ActionSignalRule:38` → `if (prescribedLoadKg != null && prescribedLoadKg - avgWeightKg > WEIGHT_TOLERANCE)` con `WEIGHT_TOLERANCE = 0.01` | El proyecto **ya modela** "prescripción activa" como *aumento pendiente respecto a lo manejado*. La precedencia de esta HU reutiliza ese criterio, no inventa uno |
| `SetExerciseInfo` (proyección de `getExerciseInfoForSet`) no expone `session_id` | Se agrega `s.id AS sessionId` para poder separar el nivel 2 del nivel 3 |
| Rama de descarga (`SessionRepositoryImpl:319-333`): `DeloadLoadRule.calculateDeloadLoad(...)` con fallback a `getLastWeightForExercise` | CA-31.09 la ratifica → conserva su prioridad; solo su fallback pasa por la memoria nueva |
| Rama de peso corporal / isométrico devuelve `0.0` → el ViewModel muestra `"0"` y `isWeightEditable = false` | CA-31.06 se cumple sin tocar nada |
| Sin historial y sin prescripción → `getLastWeightForExercise` devuelve `null` → `weightInput = ""` | CA-31.07 ya se cumple; se preserva |
| `RegisterSetViewModel:57-62` ya convierte `lastWeightKg` a la unidad activa con `WeightConverter.fromKg` (HU-30, D4) | CA-31.05 se cumple sin tocar el ViewModel: basta que la unidad y el peso salgan del mismo ejercicio |
| No existe `SessionRepositoryImplTest`; `androidTest` solo cubre migraciones | La verificación automatizada va a la regla pura (RNF29/RNF30); la resolución del repositorio se valida manualmente |
| Estados de `session`: `IN_PROGRESS`, `COMPLETED`, `INCOMPLETE` | El nivel 3 filtra `IN ('COMPLETED', 'INCOMPLETE')`, espejo de `getLastHistoricalSets` |

---

### Decisiones técnicas

#### D1 — "Prescripción activa" = prescripción que representa un aumento pendiente

Es **la decisión central de la historia** y merece revisión explícita.

CA-31.01 dice literalmente que el nivel 1 aplica *"si existe y es mayor que cero"*. Ese es exactamente el código que ya está en producción (`takeIf { it > 0.0 }`), y es el código que produce el defecto: `prescribed_load_kg` se congela en el promedio de la primera sesión evaluada (`SessionRepositoryImpl:695`) y desde entonces gana la precedencia para siempre. Implementar el nivel 1 de forma literal deja la historia sin efecto observable.

Se implementa entonces la lectura que la propia historia hace del problema — *"el defecto no está en que el motor prescriba, sino en el valor al que el sistema cae cuando **no hay prescripción activa**"* — dando contenido preciso a "activa":

> Una prescripción está **activa** cuando supera el último peso efectivamente manejado por más de la tolerancia de 0.01 kg. Si el ejecutante ya alcanzó o superó la carga prescrita, esa prescripción está consumida y la memoria toma el relevo.

Esto **no desactiva HU-11**: cuando el doble umbral prescribe `avgWeight + incremento`, el valor prescrito es estrictamente mayor que lo manejado y gana la precedencia (CA-31.04 se cumple). Lo que deja de ganar es una prescripción obsoleta que ya fue superada, que es precisamente el caso que la historia reporta como defecto.

No se toca `evaluatePostSession`. Arreglar el congelamiento en la escritura de `prescribed_load_kg` cambiaría el comportamiento del motor de progresión (y de las señales de acción de HU-13) fuera del alcance de esta historia. La corrección se aplica en la lectura, donde vive la precedencia.

**Alternativa descartada:** anular `prescribed_load_kg` una vez alcanzado. Requiere escritura en el flujo de registro de serie, acopla la precarga a la persistencia del motor y hace irreversible una decisión de presentación.

#### D2 — La precedencia es una regla pura, no un `if` en el repositorio

`PrefilledLoadRule` en `domain/rules/`, Kotlin puro sin dependencias de Android, con la firma de los cuatro niveles como parámetros explícitos. Cumple RNF29 y RNF30 (la HU los declara aplicables y la DoD exige "regla de precedencia cubierta por pruebas unitarias"), y sigue el precedente exacto de `ActionSignalRule` / `DeloadLoadRule`: `object` + función `resolve` + tolerancia como constante privada.

El repositorio queda como orquestador: consulta los tres insumos y delega la decisión.

#### D3 — Los niveles 2 y 3 se consultan por separado

Hoy `getLastWeightForExercise` los fusiona con un `ORDER BY es.id DESC` global. Funciona por accidente (los ids son monótonos), pero la precedencia queda implícita, no es verificable y arrastra dos defectos: no filtra `s.status`, de modo que entran series de otras sesiones `IN_PROGRESS`, y no distingue "esta sesión" de "la anterior".

Se separan:

- **Nivel 2** — última serie de **este** `session_exercise`, por `set_number DESC`. Es el ámbito correcto: la sustitución solo se permite con 0 series registradas (`E3-T1`), así que la fila nunca contiene series de otro ejercicio.
- **Nivel 3** — última serie del ejercicio en su sesión cerrada más reciente, espejo de `getLastHistoricalSets`: excluye la sesión actual, exige `status IN ('COMPLETED', 'INCOMPLETE')`, excluye descargas y ordena `s.date DESC, s.id DESC` para elegir la sesión, luego `set_number DESC` para elegir la serie. **Última serie, no la primera** — CA-31.03.

`getLastWeightForExercise` queda sin consumidores y se elimina.

#### D4 — La memoria se resuelve por ejercicio ejecutado; la prescripción sigue resolviéndose por slot

CA-31.08 y la regla de negocio 4 son explícitas: *"el historial es del ejercicio, no del slot"*. Las consultas de memoria usan `se.exercise_id` (el ejercicio efectivamente ejecutado), no `COALESCE(se.original_exercise_id, se.exercise_id)`. En el caso sin sustitución ambas expresiones coinciden, así que no hay regresión.

El **nivel 1 conserva su resolución por slot** (`originalExerciseId ?: exerciseId`): `exercise_progression` es una tabla por slot por diseño de HU-11/HU-26, y cambiar su keying sí sería una regresión de esas historias.

Consecuencia declarada: si el ejecutante intercambia por la alternativa y el slot tiene una prescripción **activa** (un aumento pendiente), esa prescripción gana sobre el historial del alternativo. CA-31.08 se verifica en su escenario declarado — intercambio *antes de registrar la primera serie*, sin prescripción activa — donde el campo aparece con el historial del alternativo o vacío si no lo tiene.

#### D5 — La unidad de captura también se resuelve por ejercicio ejecutado

CA-31.05 pide coherencia entre el valor precargado y la unidad mostrada. Si el peso sale del alternativo y la unidad del primario, el número es físicamente correcto pero se presenta con el rótulo de otra máquina.

`getLastCaptureUnitForExercise` pasa de `COALESCE(...)` a `se.exercise_id`. Es más fiel a la justificación que HU-30 dejó escrita para esa consulta (*"la unidad es el rótulo impreso en la máquina"*): el ejercicio alternativo es otra máquina. Se conserva intacta la otra decisión de HU-30 sobre esa consulta: **no** excluye sesiones de descarga.

#### D6 — La rama de descarga conserva su prioridad

CA-31.09 la ratifica sin ambigüedad. La rama `isDeload` no cambia de estructura ni de prioridad: `DeloadLoadRule.calculateDeloadLoad(...)` sigue ganando. Solo su fallback (cuando no hay `prescribed_load_kg` desde el cual calcular) pasa a usar la memoria nueva en lugar de `getLastWeightForExercise`. HU-14 sin regresión.

#### D7 — El ViewModel no se toca

HU-30 ya dejó `RegisterSetViewModel.init` convirtiendo `lastWeightKg` a la unidad activa y tratando `null` como campo vacío. `RegisterSetInfo` no cambia de forma. CA-31.05, CA-31.06, CA-31.07 y CA-31.10 se cumplen con el ViewModel tal como está — lo que se corrige es el valor que le llega.

#### D8 — Ubicación de las piezas nuevas

| Pieza | Ruta | Precedente |
|---|---|---|
| Regla de precedencia | `domain/rules/PrefilledLoadRule.kt` | `domain/rules/ActionSignalRule.kt` |
| Test de la regla | `test/.../domain/rules/PrefilledLoadRuleTest.kt` | `test/.../domain/rules/ActionSignalRuleTest.kt` |

---

### Tareas de Implementación

#### Fase 1 — Regla pura de precedencia

- [x] **T1: Crear `PrefilledLoadRule`** — `domain/rules/PrefilledLoadRule.kt` (Base: `domain/rules/ActionSignalRule.kt`)

  ```kotlin
  object PrefilledLoadRule {
      private const val WEIGHT_TOLERANCE = 0.01

      fun resolve(
          prescribedLoadKg: Double?,
          lastWeightInSessionKg: Double?,
          lastWeightInPreviousSessionKg: Double?,
      ): Double?
  }
  ```

  Cuerpo: `memoryKg = lastWeightInSessionKg ?: lastWeightInPreviousSessionKg` (niveles 2 y 3); `prescribed = prescribedLoadKg?.takeIf { it > 0.0 }`; se devuelve `prescribed` si está **activo** — no hay memoria, o `prescribed - memoryKg > WEIGHT_TOLERANCE` (D1); en cualquier otro caso `memoryKg`, que puede ser `null` → campo vacío (nivel 4, CA-31.07). KDoc en inglés documentando los cuatro niveles.

#### Fase 2 — Consultas de memoria

- [x] **T2: Reemplazar `getLastWeightForExercise` por las dos consultas de la precedencia** — `data/local/dao/ExerciseSetDao.kt` (Base: `getLastHistoricalSets` en el mismo DAO)

  - Eliminar `getLastWeightForExercise` (sin consumidores tras T4).
  - `getLastWeightForSessionExercise(sessionExerciseId: Long): Double?` — nivel 2:
    `SELECT weight_kg FROM exercise_set WHERE session_exercise_id = :sessionExerciseId ORDER BY set_number DESC LIMIT 1`
  - `getLastWeightInPreviousSession(exerciseId: Long, currentSessionId: Long): Double?` — nivel 3: espejo de `getLastHistoricalSets` con `se.exercise_id = :exerciseId` (D4), `s.id != :currentSessionId`, `s.status IN ('COMPLETED', 'INCOMPLETE')`, `s.deload_id IS NULL`, subconsulta ordenada por `s.date DESC, s.id DESC LIMIT 1` y serie elegida por `ORDER BY es.set_number DESC LIMIT 1` (CA-31.03).

  KDoc en inglés en ambas, indicando el nivel de precedencia que sirven y por qué la memoria se resuelve por ejercicio ejecutado.

- [x] **T3: Resolver la unidad de captura por ejercicio ejecutado** — `data/local/dao/ExerciseSetDao.kt`

  En `getLastCaptureUnitForExercise`: `COALESCE(se.original_exercise_id, se.exercise_id)` → `se.exercise_id` (D5). Actualizar su KDoc: se mantiene la no exclusión de descargas y se explica el cambio de keying. Se conserva el `INNER JOIN session_exercise`.

#### Fase 3 — Orquestación en el repositorio

- [x] **T4: Exponer `session_id` en la proyección de la serie** — `data/local/dao/SessionExerciseDao.kt`

  `SetExerciseInfo` gana `sessionId: Long`; `getExerciseInfoForSet` agrega `s.id AS sessionId` a la proyección (el `INNER JOIN session s` ya existe).

- [x] **T5: Reescribir la resolución de la precarga** — `data/repository/SessionRepositoryImpl.kt` (`getRegisterSetInfo`, líneas 313–365)

  - Calcular una sola vez, solo para ejercicios con carga externa:
    `lastInSession = exerciseSetDao.getLastWeightForSessionExercise(sessionExerciseId)` y
    `lastInPrevious = exerciseSetDao.getLastWeightInPreviousSession(info.exerciseId, info.sessionId)`.
  - Rama peso corporal / isométrico: `0.0`, sin cambios (CA-31.06).
  - Rama descarga: sin cambios de prioridad; el fallback pasa a `lastInSession ?: lastInPrevious` (D6, CA-31.09).
  - Rama estándar: `PrefilledLoadRule.resolve(progression?.prescribedLoadKg, lastInSession, lastInPrevious)`. Desaparece el `takeIf { it > 0.0 }` inline — ahora vive en la regla.
  - `progressionExerciseId = info.originalExerciseId ?: info.exerciseId` se conserva **solo** para leer `exercise_progression` y el grupo muscular (D4).
  - `captureUnit`: `getLastCaptureUnitForExercise(info.exerciseId)` (D5), en lugar de `originalExerciseId ?: exerciseId`.
  - Comentario en inglés sobre la rama estándar explicando la noción de prescripción activa.

#### Fase 4 — Tests unitarios (JVM, sin emulador)

- [x] **T6: Crear `PrefilledLoadRuleTest`** — `test/.../domain/rules/PrefilledLoadRuleTest.kt` (Base: `test/.../domain/rules/ActionSignalRuleTest.kt`)

  Helper privado `resolve(...)` con valores por defecto, comentarios `// --- Escenario N ---` y un caso por criterio:

  | Caso | Entrada | Esperado | CA |
  |---|---|---|---|
  | Prescripción activa gana a la sesión anterior | `prescribed = 45.0`, sesión `null`, previa `40.0` | `45.0` | CA-31.01, CA-31.04 |
  | Prescripción activa gana a la serie anterior en sesión | `prescribed = 45.0`, sesión `42.5`, previa `40.0` | `45.0` | CA-31.01 |
  | Prescripción consumida cede a la memoria intra-sesión | `prescribed = 40.0`, sesión `42.5`, previa `40.0` | `42.5` | CA-31.01, CA-31.02 |
  | Prescripción congelada cede a la sesión anterior | `prescribed = 40.0`, sesión `null`, previa `47.5` | `47.5` | CA-31.01, CA-31.03 |
  | Prescripción igual a lo manejado cede a la memoria | `prescribed = 42.5`, sesión `42.5`, previa `null` | `42.5` | D1 |
  | Diferencia dentro de la tolerancia cede a la memoria | `prescribed = 42.505`, sesión `42.5`, previa `null` | `42.5` | D1 |
  | Diferencia por encima de la tolerancia activa la prescripción | `prescribed = 42.52`, sesión `42.5`, previa `null` | `42.52` | D1 |
  | Prescripción en cero se ignora | `prescribed = 0.0`, sesión `null`, previa `40.0` | `40.0` | CA-31.01 |
  | Prescripción nula cae en la memoria intra-sesión | `prescribed = null`, sesión `42.5`, previa `40.0` | `42.5` | CA-31.02 |
  | La serie anterior gana a la sesión previa | `prescribed = null`, sesión `42.5`, previa `50.0` | `42.5` | CA-31.01, regla de negocio 3 |
  | Solo sesión previa | `prescribed = null`, sesión `null`, previa `40.0` | `40.0` | CA-31.03 |
  | Sin insumos → vacío | todo `null` | `null` | CA-31.07 |
  | Prescripción activa sin ninguna memoria | `prescribed = 45.0`, resto `null` | `45.0` | CA-31.01 |
  | Alternativo sin historial y sin prescripción → vacío | todo `null` | `null` | CA-31.08 |

- [x] **T7: Ejecutar la suite completa** — `./gradlew test`

  Verde al 100 %. Atención especial a `ActionSignalRuleTest`, `DoubleThresholdRuleTest`, `DeloadLoadRuleTest`, `RegisterSetViewModelTest` y `GetRegisterSetInfoUseCaseTest`: ninguno debe requerir cambios, y que no los requieran es la evidencia de que HU-11, HU-14 y HU-30 no sufren regresión.

#### Fase 5 — Documentación

- [x] **T8: Documentar la precedencia en el contrato de interfaces** — `docs/architecture/interfaces_contract.md`

  En la descripción de `E2-T1` (Registrar Serie de Ejercicio), agregar el párrafo de precedencia del valor precargado: prescripción activa → serie anterior en la sesión actual → última serie de la sesión cerrada más reciente → campo vacío; que "activa" significa aumento pendiente respecto a lo manejado; que la memoria se resuelve por ejercicio ejecutado y la prescripción por slot; y que la carga de descarga conserva su prioridad. Sin cambios en el payload ni en los estados de error.

---

### Validación manual (no automatizable)

La precedencia se verifica por regla unitaria; lo que sigue verifica el cableado real sobre la base de datos.

1. **CA-31.02** — Registrar la serie 1 de un ejercicio con carga externa en 40 kg. Entrar a la serie 2: el campo debe traer 40. Registrarla en 42,5 kg y entrar a la serie 3: debe traer 42,5.
2. **CA-31.03** — Cerrar la sesión. En la siguiente sesión de esa rutina, abrir el mismo ejercicio: el campo debe traer el peso de la **última** serie de la sesión anterior, no el de la primera ni el del primer día que se registró el ejercicio.
3. **CA-31.04 / HU-11** — Cerrar una sesión cumpliendo el doble umbral (4 series, ≥ 3 con reps ≥ 12, RIR promedio ≥ 2). En la sesión siguiente la precarga debe ser la carga prescrita (promedio + incremento), por encima del historial. La señal de acción del resumen post-sesión debe seguir anunciando el mismo aumento.
4. **CA-31.05 / HU-30** — Con un ejercicio cuya última serie se capturó en `Lb`: el selector debe aparecer en `Lb` y el valor precargado convertido a libras, coincidiendo con lo capturado originalmente.
5. **CA-31.06** — Abrir un ejercicio de peso corporal y uno isométrico: campo en 0, no editable, sin selector de unidad.
6. **CA-31.07** — Registrar un ejercicio por primera vez: campo vacío y registro permitido.
7. **CA-31.08 / HU-26** — Intercambiar un ejercicio por la alternativa de su slot antes de la primera serie. Si el alternativo tiene historial propio, la precarga es la suya; si no lo tiene y el slot no tiene prescripción activa, el campo aparece vacío — nunca el peso del primario.
8. **CA-31.09 / HU-14** — En un microciclo de descarga, la precarga debe ser la carga de descarga calculada, no el último peso manejado.
9. **CA-31.10** — Modificar el valor precargado en cualquiera de los escenarios anteriores y confirmar: se persiste el valor editado.
