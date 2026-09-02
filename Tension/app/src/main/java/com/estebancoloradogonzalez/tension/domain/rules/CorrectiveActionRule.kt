package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.CorrectiveAction
import kotlin.math.ceil

/**
 * Escalates corrective actions for an exercise that stopped progressing.
 *
 * The escalation is expressed as a displacement over the exercise's effective plateau
 * threshold, not as an absolute count: with per-exercise thresholds of 5, 8 or 10
 * sessions, fixed steps would be exceeded the moment the plateau is declared and both
 * actions would surface at once. An alert that proposes two things at once proposes
 * none.
 *
 * The first action is available from the threshold itself; the second from half a
 * threshold later:
 *
 * | Effective threshold | First action | Second action |
 * |---------------------|--------------|---------------|
 * | 3                   | 3            | 5             |
 * | 5                   | 5            | 8             |
 * | 8                   | 8            | 12            |
 * | 10                  | 10           | 15            |
 */
object CorrectiveActionRule {

    fun recommend(
        sessionsWithoutProgression: Int,
        plateauThreshold: Int,
    ): List<CorrectiveAction> {
        if (sessionsWithoutProgression < plateauThreshold) return emptyList()
        val actions = mutableListOf(CorrectiveAction.MICRO_INCREMENT_OR_EXTEND_REPS)
        if (sessionsWithoutProgression >= rotateVersionStep(plateauThreshold)) {
            actions.add(CorrectiveAction.ROTATE_VERSION)
        }
        return actions
    }

    /** Session count from which rotating the routine version becomes the proposal. */
    fun rotateVersionStep(plateauThreshold: Int): Int =
        plateauThreshold + ceil(plateauThreshold / 2.0).toInt()
}
