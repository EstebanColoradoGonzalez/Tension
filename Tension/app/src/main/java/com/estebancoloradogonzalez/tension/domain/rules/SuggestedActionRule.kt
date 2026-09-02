package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.CorrectiveAction
import com.estebancoloradogonzalez.tension.domain.model.SuggestedActionKind
import com.estebancoloradogonzalez.tension.domain.model.SuggestedActionTarget

/**
 * Everything the rule needs to decide what an alert should propose. It is all
 * primitives: the rule stays a pure function and the executability check has a single,
 * explicit input.
 */
data class SuggestedActionContext(
    val alertType: String,
    val level: String,
    val exerciseId: Long?,
    val hasSlotAlternative: Boolean,
    val sessionsWithoutProgression: Int,
    val plateauThreshold: Int,
    val isRirLow: Boolean,
)

/**
 * Decides which action an alert proposes and whether it is reachable by navigation.
 *
 * Two invariants:
 * - **Every alert gets an action.** The fallback branch covers any type the rule does
 *   not know, so no alert can end up describing a problem and proposing nothing.
 * - **The action must be executable.** Switching an exercise for the alternative in its
 *   slot is only proposed when the slot actually has one; otherwise the rule falls back
 *   to something the executant can do right now.
 */
object SuggestedActionRule {

    private const val TYPE_PLATEAU = "PLATEAU"
    private const val TYPE_LOW_PROGRESSION_RATE = "LOW_PROGRESSION_RATE"
    private const val TYPE_RIR_OUT_OF_RANGE = "RIR_OUT_OF_RANGE"
    private const val TYPE_LOW_ADHERENCE = "LOW_ADHERENCE"
    private const val TYPE_TONNAGE_DROP = "TONNAGE_DROP"
    private const val TYPE_ROUTINE_INACTIVITY = "ROUTINE_INACTIVITY"
    private const val TYPE_ROUTINE_REQUIRES_DELOAD = "ROUTINE_REQUIRES_DELOAD"

    private const val LEVEL_CRISIS = "CRISIS"

    fun resolve(
        context: SuggestedActionContext,
    ): Pair<SuggestedActionKind, SuggestedActionTarget?> {
        return when (context.alertType) {
            TYPE_PLATEAU -> resolvePlateau(context)
            TYPE_LOW_PROGRESSION_RATE -> resolveProgressionRate(context)
            TYPE_RIR_OUT_OF_RANGE -> resolveRir(context)
            TYPE_LOW_ADHERENCE ->
                SuggestedActionKind.INCREASE_WEEKLY_FREQUENCY to null
            TYPE_TONNAGE_DROP ->
                SuggestedActionKind.REDUCE_VOLUME to SuggestedActionTarget.TrainingPlan
            TYPE_ROUTINE_INACTIVITY ->
                SuggestedActionKind.RESUME_MODULE to SuggestedActionTarget.TrainingPlan
            TYPE_ROUTINE_REQUIRES_DELOAD ->
                SuggestedActionKind.START_DELOAD to SuggestedActionTarget.DeloadManagement
            else -> SuggestedActionKind.REVIEW_TECHNIQUE to null
        }
    }

    private fun resolvePlateau(
        context: SuggestedActionContext,
    ): Pair<SuggestedActionKind, SuggestedActionTarget?> {
        val actions = CorrectiveActionRule.recommend(
            context.sessionsWithoutProgression,
            context.plateauThreshold,
        )
        if (!actions.contains(CorrectiveAction.ROTATE_VERSION)) {
            return SuggestedActionKind.EXTEND_REPS_BEFORE_LOAD to exerciseTarget(context)
        }
        return if (context.hasSlotAlternative) {
            SuggestedActionKind.SWITCH_TO_SLOT_ALTERNATIVE to SuggestedActionTarget.TrainingPlan
        } else {
            SuggestedActionKind.ROTATE_ROUTINE_VERSION to SuggestedActionTarget.TrainingPlan
        }
    }

    private fun resolveProgressionRate(
        context: SuggestedActionContext,
    ): Pair<SuggestedActionKind, SuggestedActionTarget?> {
        return if (context.level == LEVEL_CRISIS && context.hasSlotAlternative) {
            SuggestedActionKind.SWITCH_TO_SLOT_ALTERNATIVE to SuggestedActionTarget.TrainingPlan
        } else {
            SuggestedActionKind.INCREASE_LOAD_SLIGHTLY to exerciseTarget(context)
        }
    }

    private fun resolveRir(
        context: SuggestedActionContext,
    ): Pair<SuggestedActionKind, SuggestedActionTarget?> {
        return if (context.isRirLow) {
            SuggestedActionKind.LEAVE_REPS_IN_RESERVE to null
        } else {
            SuggestedActionKind.INCREASE_LOAD_FOR_STIMULUS to SuggestedActionTarget.TrainingPlan
        }
    }

    private fun exerciseTarget(context: SuggestedActionContext): SuggestedActionTarget? =
        context.exerciseId?.let { SuggestedActionTarget.ExerciseHistory(it) }
}
