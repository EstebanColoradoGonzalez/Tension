package com.estebancoloradogonzalez.tension.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "module_version",
    foreignKeys = [
        ForeignKey(
            entity = ModuleEntity::class,
            parentColumns = ["code"],
            childColumns = ["module_code"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["module_code"]),
        Index(value = ["module_code", "version_number"], unique = true),
    ],
)
data class ModuleVersionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "module_code")
    val moduleCode: String,

    @ColumnInfo(name = "version_number")
    val versionNumber: Int,
)
