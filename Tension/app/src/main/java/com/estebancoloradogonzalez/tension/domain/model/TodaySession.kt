package com.estebancoloradogonzalez.tension.domain.model

/**
 * Lo que el sistema propone hoy.
 *
 * Sustituye al `NextSession?` anulable de HU-05, que colapsaba en `null` tres situaciones
 * distintas —no hay estado de rotación, no hay rutinas, la versión vigente está vacía— y
 * dejaba a Inicio sin nada que mostrar. Con el domingo como día registrado sin rutina, la
 * ausencia de propuesta pasa a ser un estado **presentable** y la ambigüedad deja de ser
 * tolerable.
 *
 * Un día se resuelve entrenando u omitiéndolo ([dayOutcome]). Resuelto, deja de proponerse y
 * se presenta [upcoming] en su lugar: informa qué toca después sin dejar iniciarlo. Es lo que
 * impide ejecutar la misma sesión varias veces el mismo día.
 *
 * @param weekDay el día de hoy.
 * @param session la sesión que puede iniciarse hoy, o nulo si no hay ninguna.
 * @param isRestDay true cuando el día no tiene rutina asignada y no hay reasignación vigente.
 * @param isTemporaryOverride true cuando la propuesta proviene de una reasignación temporal.
 * @param overriddenFromWeekDay el día dueño de la rutina propuesta, cuando no es el de hoy.
 * @param dayOutcome cómo quedó resuelto el día, o nulo si sigue abierto.
 * @param upcoming la sesión del siguiente día con rutina. No es iniciable.
 */
data class TodaySession(
    val weekDay: WeekDay,
    val session: NextSession? = null,
    val isRestDay: Boolean = false,
    val isTemporaryOverride: Boolean = false,
    val overriddenFromWeekDay: WeekDay? = null,
    val dayOutcome: DayOutcome? = null,
    val upcoming: UpcomingSession? = null,
) {
    /** El día ya se resolvió: no se propone nada iniciable hasta que cambie el día. */
    val isDayResolved: Boolean get() = dayOutcome != null

    val showSessionCard: Boolean get() = session != null && !isDayResolved
    val showRestDayCard: Boolean get() = session == null && isRestDay && !isDayResolved
    val showResolvedCard: Boolean get() = isDayResolved
}
