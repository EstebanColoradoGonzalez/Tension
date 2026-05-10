package com.estebancoloradogonzalez.tension.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "routine_current_version",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routine_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class RoutineCurrentVersionEntity(
    @PrimaryKey
    @ColumnInfo(name = "routine_id")
    val routineId: Long,

    @ColumnInfo(name = "current_version_number", defaultValue = "1")
    val currentVersionNumber: Int = 1,
)
