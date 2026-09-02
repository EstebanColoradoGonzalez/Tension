package com.estebancoloradogonzalez.tension.domain.model

/**
 * Progression rate of one exercise over the evaluation window.
 *
 * [observations] is the number of classifications the rate was computed from. It is not
 * part of the calculation: it is the evidence the presentation needs in order to tell a
 * computed zero apart from an exercise that was never registered in the window.
 */
data class ExerciseProgressionRate(
    val exerciseId: Long,
    val exerciseName: String,
    val rate: Double,
    val isBodyweight: Boolean,
    val observations: Int,
)
