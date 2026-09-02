package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.WeekDay

/**
 * Decide qué rutina queda en cada día al fijar los días de una rutina.
 *
 * El día es el dueño de la relación: cada día apunta a una rutina o a ninguna. De ahí las
 * tres únicas transiciones posibles, y de ahí que asignar un día ocupado lo **mueva** en
 * lugar de duplicarlo:
 *
 * | Día | Estado actual | Resultado |
 * |---|---|---|
 * | está en la selección | cualquiera | pasa a la rutina editada — se mueve si era de otra |
 * | no está en la selección | era de la rutina editada | queda sin rutina, es decir de descanso |
 * | no está en la selección | era de otra rutina o de ninguna | no se toca |
 *
 * Una rutina puede ocupar varios días; lo que el modelo no admite es que un día ejecute dos.
 */
object WeekDayAssignmentRule {

    /**
     * @param weekDay el día que se está evaluando.
     * @param currentRoutineId la rutina que el día tiene hoy, o null si es de descanso.
     * @param editedRoutineId la rutina cuyos días se están fijando.
     * @param selectedWeekDays los días marcados para [editedRoutineId].
     * @return la rutina que debe quedar en [weekDay], o null si queda de descanso.
     */
    fun resolveRoutineFor(
        weekDay: WeekDay,
        currentRoutineId: Long?,
        editedRoutineId: Long,
        selectedWeekDays: Set<WeekDay>,
    ): Long? = when {
        weekDay in selectedWeekDays -> editedRoutineId
        currentRoutineId == editedRoutineId -> null
        else -> currentRoutineId
    }
}
