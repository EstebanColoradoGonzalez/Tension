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
 * @param weekDay el día de hoy.
 * @param session la sesión propuesta, o nulo si no hay ninguna.
 * @param isRestDay true cuando el día no tiene rutina asignada y no hay reasignación vigente.
 * @param isTemporaryOverride true cuando la propuesta proviene de una reasignación temporal.
 * @param overriddenFromWeekDay el día dueño de la rutina propuesta, cuando no es el de hoy.
 */
data class TodaySession(
    val weekDay: WeekDay,
    val session: NextSession? = null,
    val isRestDay: Boolean = false,
    val isTemporaryOverride: Boolean = false,
    val overriddenFromWeekDay: WeekDay? = null,
) {
    val showSessionCard: Boolean get() = session != null
    val showRestDayCard: Boolean get() = session == null && isRestDay
}
