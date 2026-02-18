package com.estebancoloradogonzalez.tension.domain.model

sealed interface DeloadState {
    data object NoDeloadNeeded : DeloadState

    data class DeloadRequired(val modules: List<String>) : DeloadState

    data class DeloadActive(
        val progress: Int,
        val totalSessions: Int,
        val frozenVersionA: Int,
        val frozenVersionB: Int,
        val frozenVersionC: Int,
    ) : DeloadState

    data class DeloadCompleted(val resetLoads: List<ExerciseResetLoad>) : DeloadState
}

data class ExerciseResetLoad(
    val exerciseName: String,
    val resetLoadKg: Double,
)
