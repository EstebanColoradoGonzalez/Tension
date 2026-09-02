package com.estebancoloradogonzalez.tension.domain.model

/**
 * Load velocity of one exercise over the evaluation window.
 *
 * [sessionCount] is the number of sessions the velocity was computed from. It is not
 * part of the calculation: it is the evidence the presentation needs in order to tell a
 * computed zero apart from an exercise with a single session, for which the rule
 * cannot produce a slope.
 */
data class ExerciseLoadVelocity(
    val exerciseId: Long,
    val exerciseName: String,
    val velocity: Double,
    val isBodyweight: Boolean,
    val sessionCount: Int,
)
