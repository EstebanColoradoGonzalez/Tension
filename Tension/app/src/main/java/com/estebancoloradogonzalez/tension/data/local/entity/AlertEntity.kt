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
