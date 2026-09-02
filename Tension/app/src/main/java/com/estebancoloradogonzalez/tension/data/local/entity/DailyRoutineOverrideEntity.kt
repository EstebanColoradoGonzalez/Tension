package com.estebancoloradogonzalez.tension.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Reasignación temporal de la rutina de un día (HU-36).
 *
 * Tabla de fila única (`id = 1`), como `profile` y `rotation_state`: solo puede existir una
 * reasignación vigente. [date] es el día ISO al que aplica y es lo que la vuelve temporal —
 * la reversión automática es **semántica, no un efecto**: la fila solo se honra cuando
 * [date] es la fecha de hoy. No hay tarea programada ni borrado al cerrar la sesión.
 *
 * La consecuencia buscada es que la reasignación caduque al cambiar el día aunque la app no
 * se abra en medio, y que sobreviva a un segundo inicio de sesión el mismo día: la unidad de
 * la reasignación es el día, no la sesión.
 */
@Entity(
    tableName = "daily_routine_override",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routine_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["routine_id"])],
)
data class DailyRoutineOverrideEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 1,

    @ColumnInfo(name = "date")
    val date: String,

    @ColumnInfo(name = "routine_id")
    val routineId: Long,
)
