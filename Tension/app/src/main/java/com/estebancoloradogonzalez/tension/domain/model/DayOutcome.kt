package com.estebancoloradogonzalez.tension.domain.model

/**
 * Cómo quedó resuelto el día de hoy. `null` significa que aún está por resolver.
 *
 * Un día resuelto no vuelve a proponerse: es lo que impide ejecutar la misma sesión varias
 * veces el mismo día. La resolución es del **día**, no de la rutina — da igual qué rutina se
 * ejecutara, incluso si vino de una reasignación temporal.
 */
enum class DayOutcome {
    /** Se cerró una sesión hoy, completa o incompleta, con al menos una serie registrada. */
    TRAINED,

    /** El ejecutante declaró que hoy no entrena. No existe sesión ni la habrá para este día. */
    SKIPPED,
}
