package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.ExerciseSessionData

object DoubleThresholdRule {

    const val REP_THRESHOLD = 12
    const val MIN_SERIES_MEETING_REP_THRESHOLD = 3
    const val REQUIRED_SETS = 4
    const val RIR_THRESHOLD = 2.0

    fun meetsDoubleThreshold(current: ExerciseSessionData): Boolean {
        if (current.setCount < REQUIRED_SETS) return false

        val seriesMeetingRepThreshold = current.sets.count { it.reps >= REP_THRESHOLD }
        val meetsRepCondition = seriesMeetingRepThreshold >= MIN_SERIES_MEETING_REP_THRESHOLD
        val meetsRirCondition = current.avgRir >= RIR_THRESHOLD

        return meetsRepCondition && meetsRirCondition
    }

    fun prescribeLoad(
        currentAvgWeightKg: Double,
        loadIncrementKg: Double,
        meetsThreshold: Boolean,
    ): Double {
        if (!meetsThreshold) return currentAvgWeightKg
        return currentAvgWeightKg + loadIncrementKg
    }
}
