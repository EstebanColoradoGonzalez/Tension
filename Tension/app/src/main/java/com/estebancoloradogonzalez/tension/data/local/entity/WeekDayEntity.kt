package com.estebancoloradogonzalez.tension.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Día de la semana como entidad del dominio (HU-36).
 *
 * La tabla tiene exactamente 7 filas y no crece: es un dominio cerrado, no un catálogo
 * extensible. [id] es el número ISO-8601 del día (1 = lunes … 7 = domingo), que coincide
 * con `java.time.DayOfWeek.getValue()`, de modo que traducir "hoy" a su fila no requiere
 * ningún mapa intermedio.
 *
 * [routineId] es la relación permanente día → rutina y admite `NULL`: el domingo se
 * registra como día **sin rutina asignada**, no como ausencia de día. Así el descanso es
 * un concepto visible y el modelo queda abierto a asignarle rutina sin cambiar de forma.
 */
@Entity(
    tableName = "week_day",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routine_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["code"], unique = true),
        Index(value = ["routine_id"]),
    ],
)
data class WeekDayEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int,

    @ColumnInfo(name = "code")
    val code: String,

    @ColumnInfo(name = "routine_id")
    val routineId: Long? = null,
)
