## Análisis Arquitectónico

> HU-12 consolida cuatro historias originales del diseño (HU-12, HU-14, HU-15, HU-16) que forman una cadena de decisión inseparable: detectar → diagnosticar → prescribir → escalar.

**Patrón arquitectónico:** Extensión del pipeline de cierre de sesión (HU-10/HU-11) + reglas puras en `domain/rules/` (ADR-06) + nueva capa de persistencia de alertas.

**Justificación:** HU-12 sigue la cadena establecida por HU-10 y HU-11: lógica de backend sin pantalla propia, ejecutada dentro de la transacción de `closeSession()`, con funciones puras en `domain/rules/` testeables sin emulador. HU-12 agrega una dimensión nueva: la evaluación a **nivel de módulo** (post-loop), que opera DESPUÉS de que el loop per-ejercicio actualice todos los estados. Además, establece la **infraestructura de alertas** (`AlertEntity` + `AlertDao`) que HU-17 (Sistema de Alertas) reutilizará.

### Decisiones de Diseño

**1. HU-12 es lógica pura de backend — no tiene pantalla propia.**

Mapa de Navegación: E5 (HU-13) consume señales de regresión/descarga, H1/H2 (HU-17) consume alertas de meseta, B1 muestra badge de alertas activas. HU-12 produce datos, no los visualiza. Todo ocurre dentro de la transacción de `closeSession()`.

**2. Siete de los 24 CAs ya están cubiertos por HU-10 → HU-12 no los reimplementa.**

| CA ya cubierto | Mecanismo existente (HU-10) |
|---|---|
| CA-12.01 (regresión por reps) | `classifyStandard()` → `totalReps < previous → REGRESSION` |
| CA-12.02 (regresión por RIR) | `classifyStandard()` → `rirRise >= 1.5 → REGRESSION` |
| CA-12.03 (regresión por carga) | `classifyStandard()` → `weightLower → REGRESSION` |
| CA-12.07 (meseta a 3 sesiones) | `resolveNewProgressionState()` → `counter >= 3 → IN_PLATEAU` |
| CA-12.08 (consecutividad por ejercicio) | Diseño inherente — `sessionsWithoutProgression` solo se incrementa cuando ESE ejercicio se evalúa |
| CA-12.14 (actualización de estado) | `resolveNewProgressionState()` → `status = IN_PLATEAU` |
| CA-12.18 (no bloqueante) | Las alertas y recomendaciones son informativas por diseño — no alteran ningún flujo |

**Nota sobre CA-12.01:** El MDS R4 define regresión como "reps caen en ≥ 2 de las 4 series" (criterio per-serie). HU-10 implementa un criterio agregado (`totalReps < previous → REGRESSION`). Ambos convergen en la práctica: si las repeticiones totales bajan, al menos algunas series individuales bajaron. El edge case (series individuales cambian pero total se mantiene) es un patrón atípico de entrenamiento real. Se mantiene el enfoque agregado como suficiente para el MVP.

**3. Cuatro reglas puras nuevas en `domain/rules/` (ADR-06).**

Cada regla es un `object` Kotlin con funciones puras, sin dependencias Android, testeable con JUnit:

| Regla | MDS | Invocación | Responsabilidad |
|---|---|---|---|
| `ModuleFatigueRule` | R4 (módulo) | Write-time (cierre de sesión) | Detectar fatiga acumulada del módulo (≥50% regresiones en sesión) |
| `DeloadNeedRule` | R3 §3ᵃ intervención + R4 | Write-time (cierre de sesión) | Detectar si módulo requiere descarga |
| `PlateauCausalAnalysisRule` | R3 (análisis) | Read-time (detalle de alerta) | Analizar causa de meseta (RIR bajo/alto, estancamiento grupal) |
| `CorrectiveActionRule` | R3 §1ᵃ/2ᵃ intervención | Read-time (detalle de alerta) | Recomendar acciones correctivas escalonadas (sesión 4/6) |

**4. HU-12 establece la infraestructura de alertas que HU-17 reutiliza.**

El Modelo de Datos §3.16 define la tabla `alert` con 7 tipos. HU-12 crea `AlertEntity` + `AlertDao` (la infraestructura completa) pero solo escribe 2 tipos: `PLATEAU` y `MODULE_REQUIRES_DELOAD`. HU-17 agregará los otros 5 tipos (`LOW_PROGRESSION_RATE`, `RIR_OUT_OF_RANGE`, `LOW_ADHERENCE`, `TONNAGE_DROP`, `MODULE_INACTIVITY`) y construirá las pantallas H1/H2. Esto sigue el principio de extensión incremental: HU-12 introduce la tabla y los primeros consumidores; HU-17 amplía.

**5. Separación de responsabilidades WRITE-TIME vs READ-TIME.**

- **Write-time** (cierre de sesión): Detección de condiciones + creación/resolución de alertas en tabla `alert`. Datos persistidos: tipo, nivel, entidad afectada, mensaje, estado activo/resuelto.
- **Read-time** (detalle de alerta en H2): Análisis causal y recomendaciones correctivas. Computados dinámicamente desde datos crudos (series, sesiones).
- **Justificación:** El Modelo de Datos §3.16 es explícito: *"Los datos que dispararon la alerta y las recomendaciones escalonadas no se almacenan — se recalculan dinámicamente en la capa de aplicación a partir de las series, sesiones y la lógica del motor de reglas. Dado que las sesiones cerradas son inmutables, el recálculo siempre produce el mismo resultado."*

**6. El análisis causal (CA-12.10-12.12) recibe datos pre-computados.**

`PlateauCausalAnalysisRule.analyze()` recibe: (a) lista de avgRIR de las últimas 3 sesiones del ejercicio, (b) flag de estancamiento grupal. La obtención de estos datos (queries históricas + JOINs de `exercise_muscle_zone`) la hace el Repository/UseCase que sirve H2 — no la regla. La regla solo clasifica.

**7. Las recomendaciones correctivas (CA-12.15-12.17) se derivan del contador existente.**

`exercise_progression.sessions_without_progression` ya se mantiene por HU-10. `CorrectiveActionRule.recommend(counter)` aplica umbrales: `≥ 4` → microincremento/extensión de reps, `≥ 6` → rotar versión (acumulativo, CA-12.17). No requiere persistencia adicional — la información se computa al renderizar H2.

**8. Deduplicación y resolución automática de alertas.**

- **Deduplicación:** Antes de insertar un `PLATEAU` alert, verificar que no exista uno activo para el mismo `exercise_id`. Antes de insertar `MODULE_REQUIRES_DELOAD`, verificar que no exista uno activo para el mismo `module_code`.
- **Resolución automática (CA-12.14 implícito):** Cuando un ejercicio sale de `IN_PLATEAU` (progresión positiva → `IN_PROGRESSION`, counter reset a 0), la alerta PLATEAU se resuelve (`is_active = 0`, `resolved_at = hoy`). Cuando el módulo ya no cumple el umbral de descarga, la alerta MODULE_REQUIRES_DELOAD se resuelve.

**9. El umbral del 50% tiene denominadores diferentes según el contexto.**

| Detección | Denominador | CA |
|---|---|---|
| Fatiga acumulada del módulo | Ejercicios **con registros** en la sesión (no prescritos totales) | CA-12.05 |
| Necesidad de descarga | Total de ejercicios **prescritos** para la versión del módulo | CA-12.22 |

**10. `moduleVersionId` se obtiene de la tabla `session` y se pasa como parámetro.**

`ActiveSessionInfo` no incluye `moduleVersionId` — solo `moduleCode` y `versionNumber`. Para las queries de deload need (CA-12.22), se necesita `moduleVersionId`. Se obtiene directamente de `session.module_version_id` mediante una nueva query en `SessionDao` y se pasa como parámetro a `evaluateProgression()`.

**11. Los mensajes de alerta son genéricos; el nombre de ejercicio/módulo se resuelve por JOIN.**

`alert.message` almacena texto descriptivo genérico ("3 sesiones sin progresión", "≥50% ejercicios en meseta/regresión"). Los nombres se obtienen al leer la alerta vía FKs (`exercise_id` → `exercise.name`, `module_code` → `module.name`). Esto evita denormalización y mantiene los mensajes actualizables.

**12. Guardia de descarga: la detección de módulo-nivel se omite durante sesiones de descarga.**

Si la sesión que se cierra pertenece a un ciclo de descarga activo (`session.deload_id != null`), el paso 6 (detección de fatiga/deload del módulo) se omite completamente. Justificación: durante la descarga, la carga se reduce al 60% — las regresiones en ese contexto son esperadas y no representan fatiga real. Emitir una alerta `MODULE_REQUIRES_DELOAD` durante una descarga que ya está en curso sería paradójico. La guardia usa `session.deload_id` que ya existe en `SessionEntity`. El paso 5d (alertas per-ejercicio de meseta) también se omite durante descarga porque `resolveNewProgressionState()` de HU-10 ya tiene guardia `IN_DELOAD`: no modifica estado ni contador durante descarga.

**13. E5 (HU-13) deriva "Considerar descarga" combinando clasificación y alertas.**

CA-13.05 espera una señal "Considerar descarga" por ejercicio en el resumen post-sesión (E5). La ruta de derivación es: E5 muestra "Considerar descarga" cuando `session_exercise.progression_classification == 'REGRESSION'` en la sesión actual **Y** existe una alerta activa `MODULE_REQUIRES_DELOAD` para el módulo del ejercicio. Si solo hay regresión aislada sin alerta de módulo, E5 muestra "↓ Regresión" pero no "Considerar descarga". La implementación de esta lógica de derivación es responsabilidad de HU-13, pero HU-12 provee los datos subyacentes.

### Especificaciones de Componentes Nuevos

#### `ModuleFatigueRule.kt`

```kotlin
package com.estebancoloradogonzalez.tension.domain.rules

object ModuleFatigueRule {

    const val FATIGUE_THRESHOLD = 0.50

    fun detectFatigue(regressionCount: Int, exercisesWithRecords: Int): Boolean {
        if (exercisesWithRecords == 0) return false
        return regressionCount.toDouble() / exercisesWithRecords >= FATIGUE_THRESHOLD
    }
}
```

#### `DeloadNeedRule.kt`

```kotlin
package com.estebancoloradogonzalez.tension.domain.rules

object DeloadNeedRule {

    const val DELOAD_THRESHOLD = 0.50

    fun needsDeload(
        affectedCount: Int,
        totalCount: Int,
        fatigueDetected: Boolean,
    ): Boolean {
        if (fatigueDetected) return true // CA-12.21
        if (totalCount == 0) return false
        return affectedCount.toDouble() / totalCount >= DELOAD_THRESHOLD // CA-12.20
    }
}
```

#### `PlateauCausalAnalysisRule.kt`

```kotlin
package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.PlateauCause

object PlateauCausalAnalysisRule {

    const val LOW_RIR_THRESHOLD = 1.0
    const val HIGH_RIR_THRESHOLD = 3.0

    fun analyze(
        lastSessionsAvgRir: List<Double>,
        isGroupStagnant: Boolean,
    ): PlateauCause {
        if (isGroupStagnant) return PlateauCause.GROUP_STAGNATION // CA-12.12
        if (lastSessionsAvgRir.isEmpty()) return PlateauCause.MIXED
        val overallAvg = lastSessionsAvgRir.average()
        return when {
            overallAvg <= LOW_RIR_THRESHOLD -> PlateauCause.LOW_RIR_LIMIT // CA-12.10
            overallAvg >= HIGH_RIR_THRESHOLD -> PlateauCause.HIGH_RIR_CONSERVATIVE // CA-12.11
            else -> PlateauCause.MIXED
        }
    }
}
```

#### `CorrectiveActionRule.kt`

```kotlin
package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.CorrectiveAction

object CorrectiveActionRule {

    const val MICRO_INCREMENT_THRESHOLD = 4
    const val ROTATE_VERSION_THRESHOLD = 6

    fun recommend(sessionsWithoutProgression: Int): List<CorrectiveAction> {
        if (sessionsWithoutProgression < MICRO_INCREMENT_THRESHOLD) return emptyList()
        val actions = mutableListOf(CorrectiveAction.MICRO_INCREMENT_OR_EXTEND_REPS) // CA-12.15
        if (sessionsWithoutProgression >= ROTATE_VERSION_THRESHOLD) {
            actions.add(CorrectiveAction.ROTATE_VERSION) // CA-12.16, CA-12.17
        }
        return actions
    }
}
```

#### `PlateauCause.kt`

```kotlin
package com.estebancoloradogonzalez.tension.domain.model

enum class PlateauCause {
    LOW_RIR_LIMIT,         // CA-12.10: RIR 0-1, entrenando cerca del fallo
    HIGH_RIR_CONSERVATIVE, // CA-12.11: RIR 3+, carga conservadora
    GROUP_STAGNATION,      // CA-12.12: múltiples ejercicios del grupo muscular estancados
    MIXED,                 // Ningún patrón claro dominante
}
```

#### `CorrectiveAction.kt`

```kotlin
package com.estebancoloradogonzalez.tension.domain.model

enum class CorrectiveAction {
    MICRO_INCREMENT_OR_EXTEND_REPS, // CA-12.15: sesión 4+, microincremento o extensión de reps
    ROTATE_VERSION,                 // CA-12.16: sesión 6+, rotar a otra versión del módulo
}
```

#### `AlertEntity.kt`

```kotlin
package com.estebancoloradogonzalez.tension.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "alert",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = ModuleEntity::class,
            parentColumns = ["code"],
            childColumns = ["module_code"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["is_active"]),
        Index(value = ["type"]),
        Index(value = ["exercise_id"]),
        Index(value = ["module_code"]),
    ],
)
data class AlertEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "level")
    val level: String,

    @ColumnInfo(name = "exercise_id")
    val exerciseId: Long? = null,

    @ColumnInfo(name = "module_code")
    val moduleCode: String? = null,

    @ColumnInfo(name = "muscle_group")
    val muscleGroup: String? = null,

    @ColumnInfo(name = "message")
    val message: String,

    @ColumnInfo(name = "is_active", defaultValue = "1")
    val isActive: Int = 1,

    @ColumnInfo(name = "created_at")
    val createdAt: String,

    @ColumnInfo(name = "resolved_at")
    val resolvedAt: String? = null,
)
```

#### `AlertDao.kt`

```kotlin
package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.estebancoloradogonzalez.tension.data.local.entity.AlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {

    @Insert
    suspend fun insert(alert: AlertEntity): Long

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM alert
            WHERE exercise_id = :exerciseId AND type = :type AND is_active = 1
        )
        """,
    )
    suspend fun existsActiveByExercise(exerciseId: Long, type: String): Boolean

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM alert
            WHERE module_code = :moduleCode AND type = :type AND is_active = 1
        )
        """,
    )
    suspend fun existsActiveByModule(moduleCode: String, type: String): Boolean

    @Query(
        """
        UPDATE alert
        SET is_active = 0, resolved_at = :resolvedAt
        WHERE exercise_id = :exerciseId AND type = :type AND is_active = 1
        """,
    )
    suspend fun resolveByExerciseAndType(exerciseId: Long, type: String, resolvedAt: String)

    @Query(
        """
        UPDATE alert
        SET is_active = 0, resolved_at = :resolvedAt
        WHERE module_code = :moduleCode AND type = :type AND is_active = 1
        """,
    )
    suspend fun resolveByModuleAndType(moduleCode: String, type: String, resolvedAt: String)

    @Query("SELECT COUNT(*) FROM alert WHERE is_active = 1")
    fun countActive(): Flow<Int>

    @Query("SELECT * FROM alert WHERE is_active = 1 ORDER BY level ASC, created_at DESC")
    fun getActiveAlerts(): Flow<List<AlertEntity>>
}
```

### Especificaciones de Componentes Modificados

#### Modificación #1 — `SessionExerciseForProgression` (verificación)

El DTO ya tiene `moduleCode: String` desde HU-10. No se requiere modificación adicional.

#### Modificación #2 — `SessionDao` (queries nuevas)

```kotlin
@Query("SELECT module_version_id FROM session WHERE id = :sessionId")
suspend fun getModuleVersionIdBySessionId(sessionId: Long): Long

@Query("SELECT deload_id FROM session WHERE id = :sessionId")
suspend fun getDeloadIdBySessionId(sessionId: Long): Long?
```

#### Modificación #3 — `PlanAssignmentDao` (queries nuevas)

```kotlin
@Query("SELECT COUNT(*) FROM plan_assignment WHERE module_version_id = :moduleVersionId")
suspend fun countExercisesForModuleVersion(moduleVersionId: Long): Int

@Query(
    """
    SELECT COUNT(DISTINCT pa.exercise_id)
    FROM plan_assignment pa
    LEFT JOIN exercise_progression ep ON pa.exercise_id = ep.exercise_id
    LEFT JOIN session_exercise se ON pa.exercise_id = se.exercise_id
        AND se.session_id = :sessionId
    WHERE pa.module_version_id = :moduleVersionId
    AND (ep.status = 'IN_PLATEAU' OR se.progression_classification = 'REGRESSION')
    """,
)
suspend fun countAffectedForDeload(moduleVersionId: Long, sessionId: Long): Int
```

#### Modificación #4 — `evaluateProgression()` en `SessionRepositoryImpl`

Cambio de firma: `private suspend fun evaluateProgression(sessionId: Long)` → `private suspend fun evaluateProgression(sessionId: Long, moduleVersionId: Long, isDeloadSession: Boolean)`
