package com.estebancoloradogonzalez.tension.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Progression state of one **(exercise, equipment) pair** (HU-40).
 *
 * The weight handled in a movement is not comparable across implements: the tension of a
 * cable, the stabilisation a dumbbell demands and the guided path of a machine produce
 * different effective loads for the same effort. Comparing them against each other reads a
 * change of implement as a regression, so the unit of comparison is the pair, not the
 * exercise.
 *
 * [status], [prescribedLoadKg] and [sessionsWithoutProgression] are therefore per pair. The
 * *progression difficulty* that scales the plateau threshold stays on the exercise — it is a
 * property of the movement — and is compared against each pair's own counter.
 *
 * The consolidated reading of the exercise (progresses if **any** pair progressed; plateaus
 * only if **all** pairs stalled) is derived by `ProgressionConsolidationRule` and is never
 * persisted: a stored consolidation is a second source for a fact this table already holds.
 */
@Entity(
    tableName = "exercise_progression",
    primaryKeys = ["exercise_id", "equipment_type_id"],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = EquipmentTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["equipment_type_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["equipment_type_id"]),
    ],
)
data class ExerciseProgressionEntity(
    @ColumnInfo(name = "exercise_id")
    val exerciseId: Long,

    /**
     * Implement this progression state belongs to. Mandatory and **without default**: the
     * schema changes on a fresh install (ADR-019), so there is no prior row to backfill,
     * and a default would let a progression state exist without the implement that
     * identifies it.
     */
    @ColumnInfo(name = "equipment_type_id")
    val equipmentTypeId: Long,

    @ColumnInfo(name = "status", defaultValue = "NO_HISTORY")
    val status: String = "NO_HISTORY",

    @ColumnInfo(name = "prescribed_load_kg")
    val prescribedLoadKg: Double? = null,

    @ColumnInfo(name = "sessions_without_progression", defaultValue = "0")
    val sessionsWithoutProgression: Int = 0,

    /**
     * Sesiones consecutivas del par en el tope del rango (12 o más por mayoría) **con el
     * mismo peso de referencia** (HU-46, CA-46.03). A las 4 se propone subir.
     *
     * Es un dato persistido y no una lectura del historial porque la continuidad tiene
     * excepciones que el historial no dice por sí solo: una sesión sin series del par no la
     * rompe y una de descarga la deja congelada (CA-46.17, CA-46.18). Viaja en el respaldo
     * (CA-46.19).
     */
    @ColumnInfo(name = "top_range_streak", defaultValue = "0")
    val topRangeStreak: Int = 0,

    /**
     * Peso de referencia que sostiene [topRangeStreak]: el **mayor** de las series del par en
     * la última sesión que sumó, nunca el promedio (CA-46.03). Nulo mientras no hay racha.
     */
    @ColumnInfo(name = "top_range_weight_kg")
    val topRangeWeightKg: Double? = null,

    /**
     * Última lectura del par en el rango: `TOP`, `WITHIN` o `BELOW` (`RangeReading`). Es lo
     * que la sesión siguiente necesita para mostrar «bajar peso» junto al peso propuesto
     * (CA-46.02). Nula si el par nunca se evaluó por rango.
     */
    @ColumnInfo(name = "last_range_reading")
    val lastRangeReading: String? = null,
)
