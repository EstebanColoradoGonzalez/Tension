package com.estebancoloradogonzalez.tension.domain.rules

object AdherenceRule {

    fun calculate(completedSessions: Int, plannedSessions: Int): Double {
        if (plannedSessions == 0) return 0.0
        val raw = (completedSessions.toDouble() / plannedSessions) * 100.0
        return raw.coerceAtMost(100.0)
    }
}
