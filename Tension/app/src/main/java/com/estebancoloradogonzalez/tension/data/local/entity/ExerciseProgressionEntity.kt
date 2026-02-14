package com.estebancoloradogonzalez.tension.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercise_progression",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
)
data class ExerciseProgressionEntity(
    @PrimaryKey
    @ColumnInfo(name = "exercise_id")
    val exerciseId: Long,

    @ColumnInfo(name = "status", defaultValue = "NO_HISTORY")
    val status: String = "NO_HISTORY",

    @ColumnInfo(name = "prescribed_load_kg")
    val prescribedLoadKg: Double? = null,

    @ColumnInfo(name = "sessions_without_progression", defaultValue = "0")
    val sessionsWithoutProgression: Int = 0,
)
