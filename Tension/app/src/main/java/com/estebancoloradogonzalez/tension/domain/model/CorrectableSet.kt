package com.estebancoloradogonzalez.tension.domain.model

/**
 * The one set of the session in progress that can still be corrected: the most recently
 * registered one (HU-47, CA-47.01).
 *
 * The correction window is **derived, not stored**. It is the set with the highest `id`
 * among the sets of the session, and only while the session is `IN_PROGRESS`, so it moves
 * by itself when any set is registered (CA-47.02), it can never sit on two exercises
 * (CA-47.07), it dies with the session (CA-47.08) and it survives an unexpected shutdown
 * because it lives in the database (CA-47.10).
 *
 * [equipmentTypeId] and [equipmentTypeName] are carried to show the implement and to decide
 * whether there is external load, never to change it: the set counts for the pair it was
 * registered with (CA-47.03).
 */
data class CorrectableSet(
    val setId: Long,
    val sessionExerciseId: Long,
    val setNumber: Int,
    val totalSets: Int,
    val exerciseName: String,
    val isBodyweight: Boolean,
    val isIsometric: Boolean,
    /** Canonical value, in kilograms. */
    val weightKg: Double,
    val reps: Int,
    val rir: Int,
    /** Unit the weight was typed in. Presentation only — [weightKg] is canonical. */
    val captureUnit: WeightUnit,
    val equipmentTypeId: Long,
    val equipmentTypeName: String,
)
