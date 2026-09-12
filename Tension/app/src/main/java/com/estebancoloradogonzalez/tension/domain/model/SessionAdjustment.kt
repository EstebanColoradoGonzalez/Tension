package com.estebancoloradogonzalez.tension.domain.model

import com.estebancoloradogonzalez.tension.domain.rules.SessionAdjustmentRule

/**
 * Estado del ajuste temporal de una sesión en curso (HU-43).
 *
 * Sostiene la invariante de CA-43.05 —*el número de añadidos nunca es menor que el de
 * retirados*— y el presupuesto de CA-43.04, que es su diferencia.
 */
data class SessionAdjustment(
    val addedCount: Int = 0,
    val withdrawnCount: Int = 0,
    /** Retiros que siguen sin reponer. El primero es el que los mensajes nombran. */
    val pendingWithdrawals: List<WithdrawnExercise> = emptyList(),
) {
    /** Cuántos ejercicios del plan pueden retirarse todavía. */
    val budget: Int get() = SessionAdjustmentRule.budget(addedCount, withdrawnCount)
}

data class WithdrawnExercise(
    val exerciseId: Long,
    val name: String,
)
