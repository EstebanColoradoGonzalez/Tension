package com.estebancoloradogonzalez.tension.domain.repository

import com.estebancoloradogonzalez.tension.domain.model.DailyRoutineOverride
import com.estebancoloradogonzalez.tension.domain.model.ReassignableRoutine
import com.estebancoloradogonzalez.tension.domain.model.WeekDay
import com.estebancoloradogonzalez.tension.domain.model.WeekDayRoutine
import kotlinx.coroutines.flow.Flow

interface WeekDayRepository {
    /** Los 7 días con la rutina que cada uno tiene asignada de forma permanente. */
    fun getWeekDayPlan(): Flow<List<WeekDayRoutine>>

    /** Rutinas ejecutables hoy: toda rutina cuya versión vigente tenga ejercicios. */
    fun getReassignableRoutines(): Flow<List<ReassignableRoutine>>

    /**
     * Fija los días que ejecutan [routineId]. Una rutina puede ocupar varios días; un día
     * ocupa una sola rutina, así que asignar un día que pertenecía a otra rutina lo mueve.
     * Los días que la rutina tenía y no aparecen en [weekDays] quedan sin rutina.
     */
    suspend fun setRoutineWeekDays(routineId: Long, weekDays: Set<WeekDay>)

    fun getTodayOverride(): Flow<DailyRoutineOverride?>
    suspend fun setTodayOverride(routineId: Long)
    suspend fun clearTodayOverride()
}
