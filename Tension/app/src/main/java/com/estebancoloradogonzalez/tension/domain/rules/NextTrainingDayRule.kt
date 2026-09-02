package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.WeekDay

/**
 * El siguiente día de la semana que tiene rutina asignada, contando desde mañana.
 *
 * Salta los días de descanso: tras cerrar el sábado, lo útil es saber que el lunes toca Push,
 * no que el domingo se descansa. La búsqueda recorre la semana completa y vuelve al principio,
 * de modo que si hoy es el único día con rutina el resultado es hoy mismo — su próxima
 * ocurrencia, la semana que viene.
 */
object NextTrainingDayRule {

    private const val DAYS_IN_WEEK = 7

    /**
     * @param today el día de hoy.
     * @param daysWithRoutine días que tienen una rutina asignada.
     * @return el siguiente día con rutina, o `null` si ningún día la tiene.
     */
    fun resolve(today: WeekDay, daysWithRoutine: Set<WeekDay>): WeekDay? {
        if (daysWithRoutine.isEmpty()) return null
        for (offset in 1..DAYS_IN_WEEK) {
            val isoNumber = (today.isoNumber - 1 + offset) % DAYS_IN_WEEK + 1
            val candidate = WeekDay.fromIso(isoNumber)
            if (candidate in daysWithRoutine) return candidate
        }
        return null
    }
}
