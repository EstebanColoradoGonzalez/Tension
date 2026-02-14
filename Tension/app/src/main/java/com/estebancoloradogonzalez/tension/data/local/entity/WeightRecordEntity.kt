package com.estebancoloradogonzalez.tension.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "weight_record",
    indices = [Index(value = ["date"])],
)
data class WeightRecordEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "weight_kg")
    val weightKg: Double,

    @ColumnInfo(name = "date")
    val date: String,
)
