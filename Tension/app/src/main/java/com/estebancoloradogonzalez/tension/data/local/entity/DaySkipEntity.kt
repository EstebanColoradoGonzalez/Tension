package com.estebancoloradogonzalez.tension.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Día que el ejecutante resolvió sin entrenar.
 *
 * Tabla de fila única (`id = 1`), como `daily_routine_override`: solo interesa el día en
 * curso. [date] es la fecha ISO omitida y es lo que la vuelve efímera — la fila solo se honra
 * cuando coincide con hoy, así que caduca sola al cambiar el día sin tarea programada alguna.
 *
 * No crea `session`: omitir un día es exactamente **no haber entrenado**, y por eso no debe
 * aparecer en el historial, no cuenta como adherencia y no silencia la alerta de inactividad.
 * Esa es la diferencia con el mecanismo anterior —iniciar y cerrar una sesión vacía—, que
 * dejaba una fila `INCOMPLETE` contando como si se hubiera entrenado.
 */
@Entity(tableName = "day_skip")
data class DaySkipEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 1,

    @ColumnInfo(name = "date")
    val date: String,
)
