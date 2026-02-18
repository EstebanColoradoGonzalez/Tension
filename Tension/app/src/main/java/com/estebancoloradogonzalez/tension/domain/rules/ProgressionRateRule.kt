package com.estebancoloradogonzalez.tension.domain.rules

object ProgressionRateRule {

    fun calculate(positiveCount: Int, totalCount: Int): Double {
        if (totalCount == 0) return 0.0
        return (positiveCount.toDouble() / totalCount) * 100.0
    }
}
