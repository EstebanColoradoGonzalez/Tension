package com.estebancoloradogonzalez.tension.domain.rules

object LoadVelocityRule {

    fun calculate(currentWeightKg: Double, initialWeightKg: Double, sessionCount: Int): Double {
        if (sessionCount <= 1) return 0.0
        return (currentWeightKg - initialWeightKg) / (sessionCount - 1)
    }
}
