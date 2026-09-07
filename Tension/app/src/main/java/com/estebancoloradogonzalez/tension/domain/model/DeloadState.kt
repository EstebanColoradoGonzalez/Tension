package com.estebancoloradogonzalez.tension.domain.model

sealed interface DeloadState {
    data object NoDeloadNeeded : DeloadState

    data class DeloadRequired(val routineNames: List<String>) : DeloadState

    data class DeloadActive(
        val progress: Int,
        val totalSessions: Int,
        val frozenVersions: Map<String, Int>,
    ) : DeloadState

    data class DeloadCompleted(val resetLoads: List<ExerciseResetLoad>) : DeloadState
}

/**
 * Load one **(exercise, implement) pair** restarts at when the deload closes (CA-40.06).
 *
 * The 60% reduction and the 90% restart are computed over each pair's own load, so an
 * exercise trained with two implements produces two entries. A pair with no history
 * produces none: there is no previous load to reduce.
 */
data class ExerciseResetLoad(
    val exerciseName: String,
    val equipmentTypeName: String,
    val resetLoadKg: Double,
)
