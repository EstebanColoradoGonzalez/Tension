package com.estebancoloradogonzalez.tension.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rotation_state")
data class RotationStateEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 1,

    @ColumnInfo(name = "microcycle_position")
    val microcyclePosition: Int = 1,

    @ColumnInfo(name = "microcycle_count")
    val microcycleCount: Int = 0,
)
