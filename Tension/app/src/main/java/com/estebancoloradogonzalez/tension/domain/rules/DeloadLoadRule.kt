package com.estebancoloradogonzalez.tension.domain.rules

import kotlin.math.floor

object DeloadLoadRule {

    private const val DELOAD_PERCENTAGE = 0.60
    private const val RESET_PERCENTAGE = 0.90

    fun calculateDeloadLoad(lastWeightKg: Double, loadIncrementKg: Double): Double {
        if (loadIncrementKg <= 0.0) return 0.0
        if (lastWeightKg <= 0.0) return 0.0
        return floor(lastWeightKg * DELOAD_PERCENTAGE / loadIncrementKg) * loadIncrementKg
    }

    fun calculateResetLoad(preDeloadWeightKg: Double, loadIncrementKg: Double): Double {
        if (loadIncrementKg <= 0.0) return 0.0
        if (preDeloadWeightKg <= 0.0) return 0.0
        return floor(preDeloadWeightKg * RESET_PERCENTAGE / loadIncrementKg) * loadIncrementKg
    }
}
