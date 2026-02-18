package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.TrendDirection

object TrendClassificationRule {

    fun classify(values: List<Double>): TrendDirection {
        if (values.size < 2) return TrendDirection.STABLE
        val n = values.size
        val xMean = (n - 1) / 2.0
        val yMean = values.average()
        var numerator = 0.0
        var denominator = 0.0
        for (i in values.indices) {
            val xDiff = i - xMean
            numerator += xDiff * (values[i] - yMean)
            denominator += xDiff * xDiff
        }
        if (denominator == 0.0) return TrendDirection.STABLE
        val slope = numerator / denominator
        val threshold = yMean * 0.05
        return when {
            slope > threshold -> TrendDirection.ASCENDING
            slope < -threshold -> TrendDirection.DECLINING
            else -> TrendDirection.STABLE
        }
    }
}
