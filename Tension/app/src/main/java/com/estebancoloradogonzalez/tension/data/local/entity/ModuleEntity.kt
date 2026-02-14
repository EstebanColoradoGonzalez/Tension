package com.estebancoloradogonzalez.tension.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "module")
data class ModuleEntity(
    @PrimaryKey
    @ColumnInfo(name = "code")
    val code: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "group_description")
    val groupDescription: String,

    @ColumnInfo(name = "load_increment_kg")
    val loadIncrementKg: Double,
)
