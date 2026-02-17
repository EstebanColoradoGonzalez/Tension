package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.CorrectiveAction

object CorrectiveActionRule {

    const val MICRO_INCREMENT_THRESHOLD = 4
    const val ROTATE_VERSION_THRESHOLD = 6

    fun recommend(sessionsWithoutProgression: Int): List<CorrectiveAction> {
        if (sessionsWithoutProgression < MICRO_INCREMENT_THRESHOLD) return emptyList()
        val actions = mutableListOf(CorrectiveAction.MICRO_INCREMENT_OR_EXTEND_REPS)
        if (sessionsWithoutProgression >= ROTATE_VERSION_THRESHOLD) {
            actions.add(CorrectiveAction.ROTATE_VERSION)
        }
        return actions
    }
}
