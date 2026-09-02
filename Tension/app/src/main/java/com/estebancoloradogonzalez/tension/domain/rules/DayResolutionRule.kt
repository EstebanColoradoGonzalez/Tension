package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.DayOutcome

/**
 * Decide si el día de hoy ya está resuelto y de qué modo.
 *
 * Un día se resuelve entrenando o declarando que no se entrena; en ambos casos deja de
 * proponerse, que es lo que evita ejecutar varias sesiones el mismo día.
 *
 * La omisión se compara **por fecha**: una omisión de ayer no resuelve hoy. Es la misma
 * mecánica de caducidad que la reasignación temporal — semántica, sin borrado programado.
 */
object DayResolutionRule {

    /**
     * @param today fecha de hoy en ISO `YYYY-MM-DD`.
     * @param hasClosedSessionToday si existe una sesión cerrada con fecha de hoy.
     * @param skippedDate fecha de la omisión persistida, de cualquier día.
     * @return cómo quedó resuelto el día, o `null` si sigue abierto.
     */
    fun resolve(
        today: String,
        hasClosedSessionToday: Boolean,
        skippedDate: String?,
    ): DayOutcome? = when {
        hasClosedSessionToday -> DayOutcome.TRAINED
        skippedDate == today -> DayOutcome.SKIPPED
        else -> null
    }
}
