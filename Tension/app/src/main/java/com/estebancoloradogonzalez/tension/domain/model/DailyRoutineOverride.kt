package com.estebancoloradogonzalez.tension.domain.model

/**
 * Reasignación temporal vigente: la rutina que se ejecutará en lugar de la que el día tiene
 * asignada de forma permanente.
 *
 * [date] es el día ISO al que aplica. La reasignación no se borra al pasar el día — deja de
 * honrarse, que es lo que [DailyRoutineRule][com.estebancoloradogonzalez.tension.domain.rules.DailyRoutineRule]
 * decide.
 */
data class DailyRoutineOverride(
    val date: String,
    val routineId: Long,
)
