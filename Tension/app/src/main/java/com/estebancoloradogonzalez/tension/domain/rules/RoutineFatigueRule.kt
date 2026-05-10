package com.estebancoloradogonzalez.tension.domain.rules

object RoutineFatigueRule {

    const val FATIGUE_THRESHOLD = 0.50

    fun detectFatigue(regressionCount: Int, totalExercises: Int): Boolean {
        if (totalExercises == 0) return false
        return regressionCount.toDouble() / totalExercises >= FATIGUE_THRESHOLD
    }
}
