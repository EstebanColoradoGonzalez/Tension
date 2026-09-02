package com.estebancoloradogonzalez.tension.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.domain.model.WeekDay

/**
 * Nombre del día tal como lo lee el ejecutante. La tabla `week_day` guarda el código del
 * dominio cerrado, no la etiqueta: esta es la única traducción del proyecto, para que Inicio,
 * el selector de reasignación y la pestaña Plan no puedan divergir.
 */
@Composable
fun weekDayName(weekDay: WeekDay): String = when (weekDay) {
    WeekDay.MONDAY -> stringResource(R.string.week_day_monday)
    WeekDay.TUESDAY -> stringResource(R.string.week_day_tuesday)
    WeekDay.WEDNESDAY -> stringResource(R.string.week_day_wednesday)
    WeekDay.THURSDAY -> stringResource(R.string.week_day_thursday)
    WeekDay.FRIDAY -> stringResource(R.string.week_day_friday)
    WeekDay.SATURDAY -> stringResource(R.string.week_day_saturday)
    WeekDay.SUNDAY -> stringResource(R.string.week_day_sunday)
}

/** Abreviatura de tres letras, para las listas donde el día es una columna y no un título. */
@Composable
fun weekDayShortName(weekDay: WeekDay): String = when (weekDay) {
    WeekDay.MONDAY -> stringResource(R.string.week_day_short_monday)
    WeekDay.TUESDAY -> stringResource(R.string.week_day_short_tuesday)
    WeekDay.WEDNESDAY -> stringResource(R.string.week_day_short_wednesday)
    WeekDay.THURSDAY -> stringResource(R.string.week_day_short_thursday)
    WeekDay.FRIDAY -> stringResource(R.string.week_day_short_friday)
    WeekDay.SATURDAY -> stringResource(R.string.week_day_short_saturday)
    WeekDay.SUNDAY -> stringResource(R.string.week_day_short_sunday)
}

/**
 * Los días de una rutina, abreviados y en una sola línea. Una rutina puede ocupar más de un
 * día, así que la lista —no un valor único— es la forma natural del dato.
 */
@Composable
fun weekDaysShortLabel(weekDays: List<WeekDay>): String =
    weekDays.map { weekDayShortName(it) }.joinToString(" ")

/** Los días de una rutina con su nombre completo, para textos de una sola línea. */
@Composable
fun weekDaysLabel(weekDays: List<WeekDay>): String =
    weekDays.map { weekDayName(it) }.joinToString(", ")
