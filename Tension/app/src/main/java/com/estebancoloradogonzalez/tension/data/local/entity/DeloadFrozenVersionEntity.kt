package com.estebancoloradogonzalez.tension.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "deload_frozen_version",
    primaryKeys = ["deload_id", "routine_id"],
    foreignKeys = [
        ForeignKey(
            entity = DeloadEntity::class,
            parentColumns = ["id"],
            childColumns = ["deload_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routine_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
)
data class DeloadFrozenVersionEntity(
    @ColumnInfo(name = "deload_id")
    val deloadId: Long,

    @ColumnInfo(name = "routine_id")
    val routineId: Long,

    @ColumnInfo(name = "frozen_version_number")
    val frozenVersionNumber: Int,
)
