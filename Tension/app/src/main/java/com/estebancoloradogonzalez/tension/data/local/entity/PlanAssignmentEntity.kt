package com.estebancoloradogonzalez.tension.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "plan_assignment",
    primaryKeys = ["module_version_id", "exercise_id"],
    foreignKeys = [
        ForeignKey(
            entity = ModuleVersionEntity::class,
            parentColumns = ["id"],
            childColumns = ["module_version_id"],
            onDelete = ForeignKey.RESTRICT,
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
    @ColumnInfo(name = "module_version_id")
    val moduleVersionId: Long,

    @ColumnInfo(name = "exercise_id")
    val exerciseId: Long,

    @ColumnInfo(name = "sets")
    val sets: Int,

    @ColumnInfo(name = "reps")
    val reps: String,
)
