package com.estebancoloradogonzalez.tension.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "routine",
    indices = [Index(value = ["name"], unique = true)],
)
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,

    @ColumnInfo(name = "created_at")
    val createdAt: String,

    /**
     * Categoría de la rutina: `PUSH`, `PULL` o `LEGS` (HU-46), independiente del nombre.
     *
     * Codificar la división en el nombre libre («Push — …») impedía que el sistema la usara
     * para validar. Es la **categoría del día** de toda sesión de esta rutina. Sin default,
     * por la misma razón que `exercise.movement_category`.
     */
    @ColumnInfo(name = "movement_category")
    val movementCategory: String,
)
