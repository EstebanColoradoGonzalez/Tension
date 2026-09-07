package com.estebancoloradogonzalez.tension.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Tabla de unión N:M entre `exercise` y `equipment_type`: los implementos con los que
 * el ejercicio se puede hacer.
 *
 * El equipamiento dejó de ser identidad del ejercicio (HU-39). Un ejercicio declara la
 * lista de implementos que admite —siempre al menos uno— y cada serie declara cuál se
 * usó, en `exercise_set.equipment_type_id`.
 *
 * `RESTRICT` en ambos lados, como en [ExerciseMuscleZoneEntity]: ni un ejercicio ni un
 * tipo de equipamiento se pueden borrar mientras exista la relación.
 */
@Entity(
    tableName = "exercise_equipment",
    primaryKeys = ["exercise_id", "equipment_type_id"],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
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
        Index(value = ["equipment_type_id"]),
    ],
)
data class ExerciseEquipmentEntity(
    @ColumnInfo(name = "exercise_id")
    val exerciseId: Long,

    @ColumnInfo(name = "equipment_type_id")
    val equipmentTypeId: Long,
)
