package com.estebancoloradogonzalez.tension.data.local.seed

import com.estebancoloradogonzalez.tension.data.local.seed.model.SeedWeekDay
import com.estebancoloradogonzalez.tension.domain.model.WeekDay

/**
 * Relación permanente día → rutina de una instalación fresca (HU-36).
 *
 * Existen los **7 días**. Lunes a sábado apuntan a las seis rutinas de [DefaultPlan] en el
 * orden en que el plan las declara; el **domingo queda registrado sin rutina asignada**
 * (`routineId = null`), que es el modo en que el descanso se vuelve un concepto visible en
 * lugar de una fila ausente.
 *
 * Es solo el punto de partida: borrar una rutina deja su día sin rutina (`ON DELETE
 * SET NULL`), y el día sigue existiendo.
 */
object DefaultWeekDays {

    val ALL: List<SeedWeekDay> = listOf(
        SeedWeekDay(WeekDay.MONDAY, 1L),
        SeedWeekDay(WeekDay.TUESDAY, 2L),
        SeedWeekDay(WeekDay.WEDNESDAY, 3L),
        SeedWeekDay(WeekDay.THURSDAY, 4L),
        SeedWeekDay(WeekDay.FRIDAY, 5L),
        SeedWeekDay(WeekDay.SATURDAY, 6L),
        SeedWeekDay(WeekDay.SUNDAY, null),
    )
}
