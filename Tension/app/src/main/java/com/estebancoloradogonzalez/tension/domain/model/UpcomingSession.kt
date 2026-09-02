package com.estebancoloradogonzalez.tension.domain.model

/**
 * La sesión del siguiente día con rutina asignada.
 *
 * Se presenta cuando el día de hoy ya está resuelto: informa qué toca después sin permitir
 * iniciarlo. **No lleva `routineVersionId` a propósito** — no es iniciable, y la versión
 * vigente de la rutina puede cambiar antes de que ese día llegue. El identificador se resuelve
 * ese día, no ahora.
 */
data class UpcomingSession(
    val weekDay: WeekDay,
    val routineName: String,
    val versionNumber: Int,
)
