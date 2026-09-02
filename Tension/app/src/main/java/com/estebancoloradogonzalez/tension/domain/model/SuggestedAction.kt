package com.estebancoloradogonzalez.tension.domain.model

/**
 * The "what you can do" block of an alert: a concrete next step, plus a shortcut when
 * the step is something the app can take the executant to. A [target] of `null` is the
 * non-navigable case (reviewing technique, resting more), where the block is text only.
 */
data class SuggestedAction(
    val kind: SuggestedActionKind,
    val text: String,
    val target: SuggestedActionTarget?,
)

/**
 * Destination of the shortcut. Every value maps to a route that already exists: an
 * alert reuses the app's navigation, it does not introduce screens of its own.
 */
sealed interface SuggestedActionTarget {

    data class ExerciseHistory(val exerciseId: Long) : SuggestedActionTarget

    data object DeloadManagement : SuggestedActionTarget

    data object TrainingPlan : SuggestedActionTarget
}
