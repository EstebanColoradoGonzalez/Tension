package com.estebancoloradogonzalez.tension.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.estebancoloradogonzalez.tension.domain.model.WeightUnit

@Entity(
    tableName = "exercise_set",
    foreignKeys = [
        ForeignKey(
            entity = SessionExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = EquipmentTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["equipment_type_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["session_exercise_id"]),
        Index(value = ["session_exercise_id", "set_number"], unique = true),
        Index(value = ["equipment_type_id"]),
    ],
)
data class ExerciseSetEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "session_exercise_id")
    val sessionExerciseId: Long,

    @ColumnInfo(name = "set_number")
    val setNumber: Int,

    @ColumnInfo(name = "weight_kg")
    val weightKg: Double,

    @ColumnInfo(name = "reps")
    val reps: Int,

    @ColumnInfo(name = "rir")
    val rir: Int,

    /** Unit the executant typed the weight in. Presentation only — weightKg is canonical. */
    @ColumnInfo(name = "capture_unit", defaultValue = "KG")
    val captureUnit: String = WeightUnit.KG.name,

    /**
     * Implement actually used in this set. Mandatory and without default: the schema
     * changes on a fresh install, so there is no prior row to backfill, and a default
     * would let a set be written without the one datum HU-39 exists to capture.
     */
    @ColumnInfo(name = "equipment_type_id")
    val equipmentTypeId: Long,
)
