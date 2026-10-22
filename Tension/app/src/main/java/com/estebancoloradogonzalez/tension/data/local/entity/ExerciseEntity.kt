package com.estebancoloradogonzalez.tension.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.estebancoloradogonzalez.tension.domain.model.ProgressionDifficulty

/**
 * Ejercicio del catálogo.
 *
 * El equipamiento **no** es un atributo del ejercicio desde HU-39: vive en
 * [ExerciseEquipmentEntity] como lista de implementos admitidos. Con ello el nombre dejó
 * de formar clave compuesta con el equipamiento y es único por sí solo — el mismo
 * movimiento con distintos implementos es un ejercicio, no varios.
 */
@Entity(
    tableName = "exercise",
    indices = [
        Index(value = ["name"], unique = true),
    ],
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

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

    @ColumnInfo(name = "progression_difficulty", defaultValue = "MEDIUM")
    val progressionDifficulty: String = ProgressionDifficulty.MEDIUM.name,

    /**
     * Categoría de movimiento: `PUSH`, `PULL` o `LEGS` (HU-46, `MovementCategory`).
     *
     * Obligatoria y **sin valor por defecto**, ni en la columna ni aquí: el esquema cambia
     * por instalación fresca (ADR-019), así que no hay fila previa que rellenar, y un
     * default permitiría crear un ejercicio sin el dato que la historia existe para
     * capturar (CA-46.13). Es la que acota qué ejercicios ofrece la sesión del día y qué
     * ejercicios admite cada rutina del plan.
     */
    @ColumnInfo(name = "movement_category")
    val movementCategory: String,
)
