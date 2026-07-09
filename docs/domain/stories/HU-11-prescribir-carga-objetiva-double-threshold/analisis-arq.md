## Análisis Arquitectónico

> Esta historia implementa la Regla de Doble Umbral (Double Threshold Rule) como función pura en `domain.rules`, extendiendo el punto de integración establecido por HU-10 en `evaluateProgression()`.

**Patrón arquitectónico:** Función pura de dominio (ADR-06), integrada en el repositorio como extensión del loop de progresión existente.

### Decisiones de Diseño

**1. HU-11 es lógica pura de backend — no tiene pantalla propia.**

Mapa de Navegación §5 (fila HU-11): "representación indirecta (output en pantallas existentes)". HU-11 produce un dato (`prescribed_load_kg`) que consumen E1 (HU-05: carga objetivo al iniciar sesión) y E5 (HU-13: señal "Subir carga → X Kg" en resumen post-sesión). No hay UI ni ViewModel nuevos. La lógica se ejecuta dentro de `evaluateProgression()` en `SessionRepositoryImpl`, extendiendo el punto de extensión ya establecido por HU-10.

**2. La Regla de Doble Umbral es una función pura en `domain.rules` (ADR-06).**

ADR-06 establece reglas como funciones puras testeables sin emulador. Arquitectura Técnica §5.2 prescribe naming `{Nombre}Rule` — ejemplo explícito: `DoubleThresholdRule`. HU-10 Nota Técnica 2 anticipó este archivo como segundo en `domain/rules/`. La regla implementa MDS R1 (Doble Umbral) y R2 (Mantenimiento de carga) como operaciones complementarias.

**3. La "carga actual" es `avgWeightKg` de los sets de la sesión actual.**

MDS R1 dice: $Peso_{siguiente} = Peso_{actual} + \Delta_{min}$. El "peso actual" se interpreta como el peso promedio usado en la sesión actual (`ExerciseSessionData.avgWeightKg`), no el `prescribed_load_kg` anterior. Justificación: el ejecutante puede no haber usado la carga prescrita (ajustó por disponibilidad de equipo, sensación del día, etc.). El peso real registrado es la fuente de verdad.

**4. Los incrementos provienen de la base de datos (`module.load_increment_kg`), no de constantes hardcodeadas.**

ADR D-07 sugiere constantes (`LOAD_INCREMENT_UPPER = 2.5`, `LOAD_INCREMENT_LOWER = 5.0`), pero la base de datos ya contiene estos valores en la tabla `module` (columna `load_increment_kg`: A=2.5, B=2.5, C=5.0), seeded por `ModuleSeeder`. Duplicar los valores en el motor de reglas crea dos fuentes de verdad — si un valor cambia en seed data pero no en la constante (o viceversa), el comportamiento diverge silenciosamente. La decisión: **leer `load_increment_kg` desde la DB** extendiendo el DTO `SessionExerciseForProgression` con un campo `loadIncrementKg` (costo marginal: un JOIN adicional a `module`). La regla pura recibe el incremento como parámetro — sigue siendo testeable sin DB. ADR D-07 se reinterpreta: los VALORES son 2.5/5.0, pero la FUENTE es la tabla `module`, no constantes Kotlin.

**5. Bodyweight e isométricos quedan excluidos — `prescribed_load_kg` permanece `NULL`.**

CA-11.08 y RF31: No aplica el Doble Umbral a ejercicios de peso corporal. Los isométricos son subconjunto de bodyweight (MDS R7, HU-10 Decisión 6). Para ambos tipos, `prescribed_load_kg` queda `NULL` permanentemente. El Modelo de Datos §3.13 lo confirma: *"`NULL` para ejercicios de peso corporal e isométricos"*. El guard `isBodyweight || isIsometric` cortocircuita la evaluación antes de invocar la regla.

**6. CA-11.05 (reinicio a 8 reps) no requiere almacenamiento — es derivable.**

CA-11.05 dice "el objetivo de repeticiones se restablece al límite inferior del rango (8 repeticiones)". No hay columna `target_reps` en `exercise_progression` ni en ninguna otra tabla. El rango prescrito `8-12` ya está en `plan_assignment.reps` (Modelo de Datos §3.7) y la query `getBySessionIdWithDetails` ya lo incluye como `reps: String` en `SessionExerciseWithDetails`. Cuando `prescribed_load_kg` sube (Doble Umbral cumplido), la UI de E1 ya muestra el rango `8-12` — el ejecutante sabe que debe apuntar a 8 reps con la nueva carga. No se requiere persistencia adicional.

**7. La evaluación del Doble Umbral es POSTERIOR a la clasificación de HU-10.**

El flujo dentro de `evaluateProgression()` es secuencial por ejercicio: (1) HU-10 clasifica progresión, (2) HU-10 actualiza estado + contador, (3) **HU-11 prescribe carga**. El paso 3 usa datos del paso 1 (clasificación) y datos frescos (sets actuales). `prescribed_load_kg` se persiste en la misma llamada `exerciseProgressionDao.update()` que ya actualiza `status` y `sessionsWithoutProgression` — se extiende el `copy()` existente para incluir `prescribedLoadKg`.

**8. La prescripción de carga se ejecuta siempre — no solo cuando hay Doble Umbral.**

CA-11.06 requiere que cuando NO se cumple el Doble Umbral, la carga se mantenga. Esto significa que HU-11 siempre calcula `prescribed_load_kg`: si se cumple → carga + incremento; si no se cumple → carga actual (mantenimiento). Para ejercicios estándar, el valor NUNCA queda `NULL` después de la primera sesión con sets. Solo queda `NULL` para bodyweight/isométricos y para ejercicios que nunca se han ejecutado.

### Componentes Nuevos

**1. `DoubleThresholdRule`** — `domain/rules/DoubleThresholdRule.kt`

```kotlin
package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.ExerciseSessionData

object DoubleThresholdRule {

    const val REP_THRESHOLD = 12
    const val MIN_SERIES_MEETING_REP_THRESHOLD = 3
    const val REQUIRED_SETS = 4
    const val RIR_THRESHOLD = 2.0

    fun meetsDoubleThreshold(current: ExerciseSessionData): Boolean {
        if (current.setCount < REQUIRED_SETS) return false

        val seriesMeetingRepThreshold = current.sets.count { it.reps >= REP_THRESHOLD }
        val meetsRepCondition = seriesMeetingRepThreshold >= MIN_SERIES_MEETING_REP_THRESHOLD
        val meetsRirCondition = current.avgRir >= RIR_THRESHOLD

        return meetsRepCondition && meetsRirCondition
    }

    fun prescribeLoad(
        currentAvgWeightKg: Double,
        loadIncrementKg: Double,
        meetsThreshold: Boolean,
    ): Double {
        if (!meetsThreshold) return currentAvgWeightKg
        return currentAvgWeightKg + loadIncrementKg
    }
}
```

Función pura, testeable unitariamente sin emulador (ADR-06, RNF29, RNF30). Opera sobre modelos de dominio (`ExerciseSessionData`). La condición del Doble Umbral verifica AMBOS criterios simultáneamente (CA-11.02):

- **Reps:** ≥ 12 en al menos 3 de 4 series → `seriesMeetingRepThreshold >= 3`
- **RIR:** Promedio ≥ 2 → `avgRir >= 2.0`
- **Guard:** `setCount < 4` → `false` — CA-11.01 y RF25 dicen "3 de las 4 series", por lo que se requieren las 4 series completas

`prescribeLoad()` recibe `loadIncrementKg` como parámetro (leído de la DB), no como constante interna. Es idempotente — dado los mismos inputs, produce el mismo output.

### Componentes Modificados

**2. `SessionExerciseDao.kt`** — extender DTO y query con `loadIncrementKg`

```kotlin
data class SessionExerciseForProgression(
    val sessionExerciseId: Long,
    val exerciseId: Long,
    val isBodyweight: Int,
    val isIsometric: Int,
    val moduleCode: String,
    val loadIncrementKg: Double,
)
```

Query actualizada — agrega JOIN a `module` y selecciona `load_increment_kg`:

```sql
SELECT 
    se.id AS sessionExerciseId,
    se.exercise_id AS exerciseId,
    e.is_bodyweight AS isBodyweight,
    e.is_isometric AS isIsometric,
    e.module_code AS moduleCode,
    m.load_increment_kg AS loadIncrementKg
FROM session_exercise se
INNER JOIN exercise e ON se.exercise_id = e.id
INNER JOIN module m ON e.module_code = m.code
WHERE se.session_id = :sessionId
```

El JOIN a `module` es de costo marginal (PK string lookup) y elimina la duplicación de constantes con la BD.

**3. `SessionRepositoryImpl.evaluateProgression()`** — extender con prescripción de carga

El flujo actual (HU-10) para cada ejercicio es:
1. Obtener sets actuales y históricos
2. Clasificar progresión
3. Actualizar `session_exercise.progression_classification`
4. Resolver nuevo estado de progresión
5. Actualizar `exercise_progression` (status + counter)

HU-11 agrega un paso 5b entre el paso 4 y 5:
- Si el ejercicio es bodyweight o isométrico → `prescribedLoadKg` = `null` (sin cambio)
- Si no → evaluar Doble Umbral → calcular `prescribedLoadKg`
- Incluir `prescribedLoadKg` en el `copy()` de `exerciseProgressionDao.update()`

```kotlin
private suspend fun evaluateProgression(sessionId: Long) {
    val exercises = sessionExerciseDao.getSessionExercisesForProgression(sessionId)

    for (exercise in exercises) {
        val currentSetDtos = exerciseSetDao.getSetsForSessionExercise(
            exercise.sessionExerciseId,
        )
        if (currentSetDtos.isEmpty()) continue

        val currentData = ExerciseSessionData(
            sets = currentSetDtos.map { SetData(it.weightKg, it.reps, it.rir) },
        )

        val previousSetDtos = exerciseSetDao.getLastHistoricalSets(
            exercise.exerciseId,
            sessionId,
        )
        val previousData = if (previousSetDtos.isNotEmpty()) {
            ExerciseSessionData(
                sets = previousSetDtos.map { SetData(it.weightKg, it.reps, it.rir) },
            )
        } else {
            null
        }

        val isBodyweight = exercise.isBodyweight == 1
        val isIsometric = exercise.isIsometric == 1

        val classification = ProgressionClassificationRule.classify(
            current = currentData,
            previous = previousData,
            isBodyweight = isBodyweight,
            isIsometric = isIsometric,
        )

        sessionExerciseDao.updateProgressionClassification(
            exercise.sessionExerciseId,
            classification?.name,
        )

        val isMastered = isIsometric &&
            ProgressionClassificationRule.isIsometricMastered(currentData)

        val currentProgression = exerciseProgressionDao
            .getByExerciseId(exercise.exerciseId).first()

        if (currentProgression == null) continue

        val (newStatus, newCounter) =
            ProgressionClassificationRule.resolveNewProgressionState(
                currentStatus = currentProgression.status,
                currentCounter = currentProgression.sessionsWithoutProgression,
                classification = classification,
                isIsometric = isIsometric,
                isMastered = isMastered,
            )

        // Step 5b: Prescribe load (HU-11)
        val prescribedLoadKg = if (isBodyweight || isIsometric) {
            null
        } else {
            val meetsThreshold = DoubleThresholdRule.meetsDoubleThreshold(currentData)
            DoubleThresholdRule.prescribeLoad(
                currentAvgWeightKg = currentData.avgWeightKg,
                loadIncrementKg = exercise.loadIncrementKg,
                meetsThreshold = meetsThreshold,
            )
        }

        exerciseProgressionDao.update(
            currentProgression.copy(
                status = newStatus,
                sessionsWithoutProgression = newCounter,
                prescribedLoadKg = prescribedLoadKg,
            ),
        )
    }
}
```

Cambios respecto al código HU-10 existente: (a) calcular `prescribedLoadKg` antes del `update`, (b) incluirlo en el `copy()`, y (c) usar `exercise.loadIncrementKg` del DTO. El import de `DoubleThresholdRule` se agrega en la sección de imports.

### Verificación Exhaustiva CA por CA

**CA-11.01 — Detección de cumplimiento del Doble Umbral:**

| Capa | Implementación | Evidencia |
|------|----------------|-----------|
| Rule | `DoubleThresholdRule.meetsDoubleThreshold(currentData)` | Verifica ambas condiciones sobre `ExerciseSessionData` |
| Condición reps | `sets.count { it.reps >= 12 } >= 3` | ≥ 12 reps en ≥ 3 de 4 series |
| Condición RIR | `avgRir >= 2.0` | RIR promedio ≥ 2 |
| MDS R1 | Fórmula del Doble Umbral | Match exacto |

**CA-11.02 — Exigencia de ambas condiciones:**

| Capa | Implementación | Evidencia |
|------|----------------|-----------|
| Rule | `return meetsRepCondition && meetsRirCondition` | Operador AND — ambas deben ser true |
| Guard | `setCount < REQUIRED_SETS (4) → false` | Si hay <4 sets, no se evalúa (RF25: "3 de las 4 series") |

**CA-11.03 — Incremento de carga para tren superior (Módulos A y B):**

| Capa | Implementación | Evidencia |
|------|----------------|-----------|
| DB | `module.load_increment_kg = 2.5` para módulos A y B | `ModuleSeeder`: A=2.5, B=2.5 |
| Rule | `prescribeLoad(loadIncrementKg = 2.5)` | Recibe incremento como parámetro |
| Cálculo | `currentAvgWeightKg + 2.5` | Peso actual + incremento |

**CA-11.04 — Incremento de carga para tren inferior (Módulo C):**

| Capa | Implementación | Evidencia |
|------|----------------|----------|
| DB | `module.load_increment_kg = 5.0` para módulo C | `ModuleSeeder`: C=5.0 |
| Rule | `prescribeLoad(loadIncrementKg = 5.0)` | Recibe incremento como parámetro |
| Cálculo | `currentAvgWeightKg + 5.0` | Peso actual + incremento |

**CA-11.05 — Reinicio del objetivo de repeticiones tras incremento:**

| Capa | Implementación | Evidencia |
|------|----------------|-----------|
| Derivable | Sin persistencia — rango `8-12` ya en `plan_assignment.reps` | `SessionExerciseWithDetails.reps` = `"8-12"` |
| UX | Con nueva carga, el ejecutante apunta a 8 reps | Modelo de Datos no tiene columna `target_reps` |
| MDS R1 | "reestablece el objetivo de repeticiones al límite inferior del rango" | Implícito por el rango prescrito |

**CA-11.06 — Mantenimiento de carga cuando no se cumple el Doble Umbral:**

| Capa | Implementación | Evidencia |
|------|----------------|-----------|
| Rule | `if (!meetsThreshold) return currentAvgWeightKg` | `prescribeLoad()` con flag false |
| MDS R2 | "la carga se mantiene para la próxima sesión" | `prescribed_load_kg` = peso actual |
| RF27 | "prescribir la misma carga" | Match exacto |

**CA-11.07 — Persistencia de la carga prescrita:**

| Capa | Implementación | Evidencia |
|------|----------------|-----------|
| Repository | `exerciseProgressionDao.update(currentProgression.copy(prescribedLoadKg = ...))` | Persistido en `exercise_progression.prescribed_load_kg` |
| Consumo | E1 → `SessionExerciseWithDetails.prescribedLoadKg` (JOIN ya existe) | Query `getBySessionIdWithDetails` ya incluye `ep.prescribed_load_kg` |
| Modelo de Datos | §3.13: "Calculada por el motor de Doble Umbral al cierre de sesión y persistida" | Match exacto |

**CA-11.08 — No aplica a ejercicios de peso corporal:**

| Capa | Implementación | Evidencia |
|------|----------------|-----------|
| Guard | `if (isBodyweight \|\| isIsometric) → prescribedLoadKg = null` | Skip antes de invocar regla |
| RF31 | "sin aplicar la Regla de Doble Umbral de carga" | Match exacto |
| Modelo de Datos | §3.13: "`NULL` para ejercicios de peso corporal e isométricos" | Match exacto |

### Verificación Cruzada de CAs

| CA | Estado | Mecanismo | Implementado en |
|---|---|---|-----------------|
| CA-11.01 | 🔨 Por implementar | `meetsDoubleThreshold()` — verifica reps ≥ 12 en ≥ 3 series AND avgRir ≥ 2 | HU-11 (Rule) |
| CA-11.02 | 🔨 Por implementar | `&&` operator — ambas condiciones simultáneas | HU-11 (Rule) |
| CA-11.03 | 🔨 Por implementar | `prescribeLoad(loadIncrementKg = 2.5)` — incremento leído de `module.load_increment_kg` | HU-11 (Rule + DTO) |
| CA-11.04 | 🔨 Por implementar | `prescribeLoad(loadIncrementKg = 5.0)` — incremento leído de `module.load_increment_kg` | HU-11 (Rule + DTO) |
| CA-11.05 | ✅ Ya cubierto | Derivable — rango `8-12` en `plan_assignment.reps` | HU-05 (query existente) |
| CA-11.06 | 🔨 Por implementar | `prescribeLoad()` con `meetsThreshold = false` → misma carga | HU-11 (Rule) |
| CA-11.07 | 🔨 Por implementar | `exerciseProgressionDao.update(copy(prescribedLoadKg = ...))` | HU-11 (Repository) |
| CA-11.08 | 🔨 Por implementar | Guard `isBodyweight \|\| isIsometric → null` | HU-11 (Repository) |

### Hitos de Implementación

| # | Entregable | Archivos | CAs |
|---|-----------|----------|-----|
| 1 | Regla pura — `DoubleThresholdRule` + tests unitarios exhaustivos | `DoubleThresholdRule.kt`, `DoubleThresholdRuleTest.kt` | CA-11.01, CA-11.02, CA-11.03, CA-11.04, CA-11.06 |
| 2 | DTO + query — extender `SessionExerciseForProgression` con `loadIncrementKg` | `SessionExerciseDao.kt` | CA-11.03, CA-11.04 |
| 3 | Integración — extender `evaluateProgression()` con prescripción de carga | `SessionRepositoryImpl.kt` | CA-11.07, CA-11.08 |

### Notas Técnicas

1. **HU-11 agrega 0 queries nuevas, solo extiende 1 existente.** La query `getSessionExercisesForProgression` se extiende con un JOIN a `module` para obtener `load_increment_kg`. El resto de la información ya está disponible en el loop: `currentData` (sets con reps, rir, weight), `exercise.isBodyweight`, `exercise.isIsometric`. El `moduleCode` fue previsto por HU-10 Nota Técnica 5.

2. **El `avgWeightKg` usa el peso PROMEDIO de los sets, no la moda ni el máximo.** En la práctica, todas las series de un ejercicio se ejecutan con el mismo peso (el peso prescrito o disponible). Pero si el ejecutante ajustó el peso intra-ejercicio (ej: warm-up set, strip set), el promedio refleja mejor la intensidad real. El Modelo de Datos §3.12 registra `weight_kg` por set individual — el promedio se computa desde los datos reales.

3. **El guard `setCount < REQUIRED_SETS (4)` en `meetsDoubleThreshold`.** CA-11.01 y RF25 dicen explícitamente "al menos 3 **de las 4 series**", fijando 4 como la cantidad requerida. Si el ejercicio tiene <4 sets (sesión incompleta), el Doble Umbral no se evalúa — la carga se mantiene (CA-11.06 fallback). Esto previene que una sesión con 3 sets brillantes active un incremento prematuro. La constante `REQUIRED_SETS = 4` hace explícita esta decisión.

4. **La constante `RIR_THRESHOLD = 2.0` es Double, no Int.** Aunque el RIR se registra como Int (0-5) por set, el promedio de 4 sets es un Double. La comparación `avgRir >= 2.0` es consistente con el tipo. Ejemplo: sets con RIR [2, 2, 1, 2] → promedio = 1.75 → NO cumple (< 2.0).

5. **`prescribeLoad()` siempre retorna un valor.** No retorna `null` — para ejercicios estándar, siempre hay una carga prescrita (la actual o la incrementada). El `null` solo ocurre para bodyweight/isométricos, y ese caso se resuelve en el guard del repositorio ANTES de invocar la regla. La separación de responsabilidades es clara: el repositorio decide SI prescribir; la regla decide CUÁNTO.

6. **La prescripción de carga durante `IN_DELOAD` sigue el flujo normal.** HU-10 ya guarda el estado `IN_DELOAD` (no lo modifica). HU-11 prescribe la carga basada en los sets reales de la sesión de descarga (que son al 60% de la carga habitual). Cuando la descarga termine (HU-17), la carga se reiniciará al 90% de la pre-descarga — eso es responsabilidad de HU-17 (CA-17.05), no de HU-11. HU-11 simplemente prescribe carga actual + incremento (si aplica) o carga actual (si no aplica), independientemente del estado de descarga.

7. **El Doble Umbral NO depende de la clasificación de progresión.** Son evaluaciones independientes: la clasificación de HU-10 compara contra historial (sesión actual vs anterior); el Doble Umbral evalúa solo la sesión actual (¿cumplió los umbrales de reps y RIR?). Un ejercicio puede tener `MAINTENANCE` (misma carga, mismas reps) y aun así cumplir el Doble Umbral (12+ reps con RIR ≥ 2) — en ese caso, se prescribe incremento.

### Referencias y Validación

**Documentación consultada:**

- MDS §6-A Regla 1: Doble Umbral — fórmula completa y condiciones.
- MDS §6-A Regla 2: Mantenimiento de carga — cuando R1 no se cumple.
- MDS §6-A Regla 6: Peso corporal — exclusión del Doble Umbral.
- Modelo de Datos §3.13 (`exercise_progression`): columna `prescribed_load_kg` con lógica de step 6.
- ADR-06: Motor de reglas como Kotlin puro en `domain.rules`.
- ADR D-07: Valores de incremento 2.5/5.0 — fuente real: `module.load_increment_kg` en la BD.
- Arquitectura Técnica §5.2: Naming `DoubleThresholdRule`.
- HU-10 análisis: Nota Técnica 2 (primer archivo en `domain/rules/`), Nota Técnica 5 (`moduleCode` proactivo).
- HU-10 implementación: `evaluateProgression()` como punto de extensión.
- Requerimientos: RF25, RF26, RF27, RF31.

---

<!-- AUDITORÍA PROFUNDA (2026-02-15):

=== SCOPE ===
7 docs arquitectura + 5 docs business + 32 HUs + Mapa de Historias + 8 archivos de código fuente verificados.

=== DOCUMENTOS CRUZADOS ===
Arquitectura: Modelo de Datos §3.13 (exercise_progression.prescribed_load_kg — "Calculada por el motor de Doble Umbral al cierre de sesión y persistida (CA-11.07). NULL para bodyweight/isométricos. Tras descarga: 90% (CA-17.05)" ✅), §3.13 paso 6 lógica de actualización ("Calcular y persistir prescribed_load_kg según Doble Umbral R1 o mantenimiento R2" ✅), module.load_increment_kg §3.1 (CHECK > 0, seed A=2.5 B=2.5 C=5.0 ✅), ADR-06 (reglas puras en domain.rules ✅), ADR D-07 (reinterpretado: valores 2.5/5.0 son de BD, no constantes Kotlin ✅), Arquitectura Técnica §2.5 (Motor de Reglas R1-R7 ✅), §5.2 (DoubleThresholdRule naming explícito ✅), Mapa de Navegación §5 (HU-11 "representación indirecta en E1/E5" ✅), Especificación Visual (E5: "Subir carga → X Kg" / "Progresar en reps" ✅), Wireframes (E1: carga objetivo, E5: señal de subir carga ✅).
Business: Requerimientos RF25 (≥12 reps en ≥3/4 AND RIR avg ≥2 ✅), RF26 (prescribir +2.5 A/B, +5 C ✅), RF27 (misma carga si no cumple ✅), RF31 (bodyweight excluido ✅), RNF30 (pruebas unitarias Doble Umbral ✅). MDS §6-A R1 (fórmula Doble Umbral completa ✅), R2 (mantenimiento de carga ✅), R6 (Δmin=0 para bodyweight ✅).

=== HUs CRUZADAS (11 con referencias) ===
Predecesoras (6): HU-03 (ModuleEntity.load_increment_kg creada ✅), HU-05 (14 refs: ExerciseProgressionEntity con prescribedLoadKg, SessionExerciseWithDetails JOIN, DAO methods ✅), HU-06 (7 refs: insertIfNotExists crea entity, exercise_set data producer, CA redistribución ✅), HU-08 (14 refs: CA-08.03→CA-11.08, bodyweight exclusión ✅), HU-09 (2 refs: punto extensión closeSession ✅), HU-10 (11 refs: evaluateProgression() loop, moduleCode proactivo, ProgressionClassificationRule patrón, paso 6 diferido ✅).
Sucesoras (5): HU-13 (E5 "Subir carga → X Kg" consume prescribedLoadKg ✅), HU-15 (microincremento sesión 4 usa prescribedLoadKg como base ✅), HU-17 (CA-17.05 sobrescribe prescribedLoadKg con 90% pre-descarga ✅), HU-20 (relación débil — tonnage usa exercise_set, no prescribedLoadKg; podría derivar progresión de carga indirectamente ⚠️), HU-23 (relación débil — evolución de carga usa exercise_set; podría mostrar prescribedLoadKg opcionalmente ⚠️).
Peers (1): HU-12 (regresión independiente, comparte evaluateProgression, no lee/escribe prescribedLoadKg ✅).
Sin referencias (17): HU-01, HU-02, HU-04, HU-07, HU-14, HU-16, HU-18, HU-19, HU-21, HU-24, HU-25-HU-32 ✅. -->
