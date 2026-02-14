package com.estebancoloradogonzalez.tension.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 1,

    @ColumnInfo(name = "height_m")
    val heightM: Double,

    @ColumnInfo(name = "experience_level")
    val experienceLevel: String,

    @ColumnInfo(name = "weekly_frequency")
    val weeklyFrequency: Int = 6,

    @ColumnInfo(name = "created_at")
    val createdAt: String,
)
