package com.estebancoloradogonzalez.tension.data.local.seed.model

import com.estebancoloradogonzalez.tension.domain.model.WeekDay

/**
 * Relación permanente día → rutina del plan predeterminado.
 *
 * [routineId] nulo significa día registrado **sin rutina asignada** — es el caso del
 * domingo. No es ausencia de dato: la fila existe y el día es visible como descanso.
 */
data class SeedWeekDay(
    val weekDay: WeekDay,
    val routineId: Long?,
)
