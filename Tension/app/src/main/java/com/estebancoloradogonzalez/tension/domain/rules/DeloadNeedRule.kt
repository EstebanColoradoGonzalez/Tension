package com.estebancoloradogonzalez.tension.domain.rules

object DeloadNeedRule {

    const val DELOAD_THRESHOLD = 0.50

    fun needsDeload(
        affectedCount: Int,
        totalCount: Int,
        fatigueDetected: Boolean,
    ): Boolean {
        if (fatigueDetected) return true
        if (totalCount == 0) return false
        return affectedCount.toDouble() / totalCount >= DELOAD_THRESHOLD
    }
}
