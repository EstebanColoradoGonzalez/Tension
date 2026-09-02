package com.estebancoloradogonzalez.tension.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.estebancoloradogonzalez.tension.domain.rules.PlateauThresholdRule

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

    @ColumnInfo(name = "plateau_base_threshold", defaultValue = "5")
    val plateauBaseThreshold: Int = PlateauThresholdRule.DEFAULT_BASE_THRESHOLD,

    @ColumnInfo(name = "created_at")
    val createdAt: String,
)
