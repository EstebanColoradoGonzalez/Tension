package com.estebancoloradogonzalez.tension.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercise",
    foreignKeys = [
        ForeignKey(
            entity = ModuleEntity::class,
            parentColumns = ["code"],
            childColumns = ["module_code"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = EquipmentTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["equipment_type_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["module_code"]),
        Index(value = ["equipment_type_id"]),
        Index(value = ["name", "equipment_type_id"], unique = true),
    ],
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "module_code")
    val moduleCode: String,

    @ColumnInfo(name = "equipment_type_id")
    val equipmentTypeId: Long,

    @ColumnInfo(name = "is_bodyweight", defaultValue = "0")
    val isBodyweight: Int = 0,

    @ColumnInfo(name = "is_isometric", defaultValue = "0")
    val isIsometric: Int = 0,

    @ColumnInfo(name = "is_to_technical_failure", defaultValue = "0")
    val isToTechnicalFailure: Int = 0,

    @ColumnInfo(name = "is_custom", defaultValue = "0")
    val isCustom: Int = 0,

    @ColumnInfo(name = "media_resource")
    val mediaResource: String? = null,
)
