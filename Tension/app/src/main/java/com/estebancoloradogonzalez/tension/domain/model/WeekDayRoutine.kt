package com.estebancoloradogonzalez.tension.domain.model

/**
 * Relación permanente día → rutina.
 *
 * [routineId] y [routineName] nulos significan día sin rutina asignada: el domingo del plan
 * predeterminado, o cualquier día cuya rutina se haya borrado.
 */
data class WeekDayRoutine(
    val weekDay: WeekDay,
    val routineId: Long?,
    val routineName: String?,
) {
    val isRestDay: Boolean get() = routineId == null
}
