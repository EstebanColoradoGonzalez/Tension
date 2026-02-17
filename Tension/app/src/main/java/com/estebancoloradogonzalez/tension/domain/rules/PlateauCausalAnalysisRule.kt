package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.PlateauCause

object PlateauCausalAnalysisRule {

    const val LOW_RIR_THRESHOLD = 1.0
    const val HIGH_RIR_THRESHOLD = 3.0

    fun analyze(
        lastSessionsAvgRir: List<Double>,
        isGroupStagnant: Boolean,
    ): PlateauCause {
        if (isGroupStagnant) return PlateauCause.GROUP_STAGNATION
        if (lastSessionsAvgRir.isEmpty()) return PlateauCause.MIXED
        val overallAvg = lastSessionsAvgRir.average()
        return when {
            overallAvg <= LOW_RIR_THRESHOLD -> PlateauCause.LOW_RIR_LIMIT
            overallAvg >= HIGH_RIR_THRESHOLD -> PlateauCause.HIGH_RIR_CONSERVATIVE
            else -> PlateauCause.MIXED
        }
    }
}
