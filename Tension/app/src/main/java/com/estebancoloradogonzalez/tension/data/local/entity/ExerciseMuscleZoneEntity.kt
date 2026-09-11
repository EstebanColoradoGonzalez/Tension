package com.estebancoloradogonzalez.tension.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Tabla de unión N:M entre `exercise` y `muscle_zone`, con la **jerarquía** de la relación
 * en [isPrimary] (HU-41).
 *
 * La zona principal es la que ejecuta el movimiento; la secundaria, la que asiste. El
 * criterio es biomecánico: el músculo que trabaja, no la máquina ni la ubicación aparente.
 * Un ejercicio tiene **al menos una** principal y puede tener **cero** secundarias, y
 * puede tener varias principales cuando el movimiento reparte el trabajo por igual
 * (*Peso Muerto Rumano*: isquiotibiales y glúteo mayor).
 *
 * **La jerarquía es informativa, no un peso de cálculo.** Para el tonelaje y el volumen
 * por grupo muscular cuentan todas las zonas, principales y secundarias, sin ponderación
 * (CA-41.04): ningún KPI cambió de definición al ganar la jerarquía.
 *
 * La PK compuesta es la que hace **irrepresentable** que una zona sea a la vez principal y
 * secundaria del mismo ejercicio (CA-41.09): una zona es una fila, y la fila lleva un solo
 * valor de [isPrimary]. No hace falta validarlo.
 */
@Entity(
    tableName = "exercise_muscle_zone",
    primaryKeys = ["exercise_id", "muscle_zone_id"],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = MuscleZoneEntity::class,
            parentColumns = ["id"],
            childColumns = ["muscle_zone_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["muscle_zone_id"]),
    ],
)
data class ExerciseMuscleZoneEntity(
    @ColumnInfo(name = "exercise_id")
    val exerciseId: Long,

    @ColumnInfo(name = "muscle_zone_id")
    val muscleZoneId: Long,

    /**
     * `1` si la zona es principal, `0` si es secundaria.
     *
     * Sin `defaultValue` a propósito: el esquema cambia por instalación fresca (ADR-019),
     * la tabla nace vacía y no hay fila previa que rellenar. Un default permitiría
     * escribir una relación sin declarar su jerarquía, que es justo lo que HU-41 añade.
     */
    @ColumnInfo(name = "is_primary")
    val isPrimary: Int,
)
