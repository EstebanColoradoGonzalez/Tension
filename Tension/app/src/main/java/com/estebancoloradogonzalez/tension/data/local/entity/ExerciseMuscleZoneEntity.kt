package com.estebancoloradogonzalez.tension.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "exercise_muscle_zone",
    primaryKeys = ["exercise_id", "muscle_zone_id"],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = MuscleZoneEntity::class,
            parentColumns = ["id"],
            childColumns = ["muscle_zone_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["muscle_zone_id"]),
    ],
)
data class ExerciseMuscleZoneEntity(
    @ColumnInfo(name = "exercise_id")
    val exerciseId: Long,

    @ColumnInfo(name = "muscle_zone_id")
    val muscleZoneId: Long,
)
