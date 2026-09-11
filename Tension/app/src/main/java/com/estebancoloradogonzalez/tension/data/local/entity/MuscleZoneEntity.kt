package com.estebancoloradogonzalez.tension.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Zona muscular del catálogo. Tabla **cerrada y sembrada**: no existe interfaz para crear
 * zonas, igual que con `equipment_type`.
 *
 * [muscleGroup] es el grupo de agregación de los KPIs y sigue siendo uno de los 14 de
 * siempre. La granularidad fina que HU-41 introdujo —33 zonas donde había 20— cabe
 * íntegramente dentro de ellos: gana precisión por debajo sin cambiar el eje por el que se
 * agrega.
 */
@Entity(
    tableName = "muscle_zone",
    indices = [Index(value = ["muscle_group"])],
)
data class MuscleZoneEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "muscle_group")
    val muscleGroup: String,

    /**
     * Orden declarado por el catálogo, que es el que el selector de zonas presenta: los 14
     * grupos en orden anatómico y, dentro de cada uno, las zonas de mayor a menor.
     *
     * Existe porque el identificador **no puede** encodar el orden, a diferencia de
     * `equipment_type`: el id de una zona encoda la historia del catálogo, ya que un
     * renombrado conserva el suyo (HU-29) y los retirados quedan libres. Ordenar por
     * nombre produciría *Inferior, Mayor, Medio, Superior* — alfabético y sin sentido
     * anatómico.
     *
     * Sin `defaultValue`: la tabla nace sembrada y el orden nunca es opcional (ADR-019).
     */
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
)
