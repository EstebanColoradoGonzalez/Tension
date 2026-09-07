package com.estebancoloradogonzalez.tension.domain.model

/**
 * Session range of one (exercise, implement) pair over the evaluation window (HU-40).
 *
 * The load-velocity KPI resolves its slope over each pair separately and consolidates the
 * result afterwards: the weight handled with a cable and with a dumbbell are not the same
 * magnitude, and a slope drawn between them would not measure anything.
 */
data class ExercisePairSessionRange(
    val exerciseId: Long,
    val exerciseName: String,
    val equipmentTypeId: Long,
    val equipmentTypeName: String,
    val isBodyweight: Int,
    val isIsometric: Int,
    val firstSessionId: Long,
    val lastSessionId: Long,
    val sessionCount: Int,
)
