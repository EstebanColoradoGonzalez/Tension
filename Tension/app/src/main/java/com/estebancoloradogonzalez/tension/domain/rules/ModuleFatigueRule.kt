package com.estebancoloradogonzalez.tension.domain.rules

object ModuleFatigueRule {

    const val FATIGUE_THRESHOLD = 0.50

    fun detectFatigue(regressionCount: Int, exercisesWithRecords: Int): Boolean {
        if (exercisesWithRecords == 0) return false
        return regressionCount.toDouble() / exercisesWithRecords >= FATIGUE_THRESHOLD
    }
}
