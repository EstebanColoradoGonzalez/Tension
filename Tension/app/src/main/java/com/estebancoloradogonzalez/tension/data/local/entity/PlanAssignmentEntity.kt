package com.estebancoloradogonzalez.tension.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "plan_assignment",
    primaryKeys = ["routine_version_id", "exercise_id"],
    foreignKeys = [
        ForeignKey(
            entity = RoutineVersionEntity::class,
            parentColumns = ["id"],
            childColumns = ["routine_version_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["exercise_id"]),
    ],
)
data class PlanAssignmentEntity(
    @ColumnInfo(name = "routine_version_id")
    val routineVersionId: Long,

    @ColumnInfo(name = "exercise_id")
    val exerciseId: Long,

    @ColumnInfo(name = "sets")
    val sets: Int,

    @ColumnInfo(name = "reps")
    val reps: String,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "slot", defaultValue = "0")
    val slot: Int = 0,
)
