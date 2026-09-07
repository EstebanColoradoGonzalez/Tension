package com.estebancoloradogonzalez.tension.domain.model

sealed interface AlertTriggerData {

    data class PlateauTrigger(
        val sessions: List<PlateauSession>,
        /**
         * Implements that have reached the exercise's effective threshold (CA-40.05).
         *
         * A plateau is only declared when all of them are stalled, so naming them is what
         * makes the alert actionable: without the concrete pair, the recommendation has no
         * subject. Derived from the pairs' current counters and never persisted — between
         * raising the alert and reading it, what matters is today's state.
         */
        val stalledPairs: List<StalledPair> = emptyList(),
    ) : AlertTriggerData

    data class PlateauSession(
        val date: String,
        val weightKg: Double,
        val totalReps: Int,
    )

    data class StalledPair(
        val equipmentTypeName: String,
        val sessionsWithoutProgression: Int,
    )

    data class ProgressionRateTrigger(
        val rate: Double,
        val exerciseName: String,
    ) : AlertTriggerData

    data class RirTrigger(
        val avgRir: Double,
        val routineId: Long,
        val routineName: String,
        val isLow: Boolean,
    ) : AlertTriggerData

    data class AdherenceTrigger(
        val percentage: Double,
        val completedSessions: Int,
        val plannedSessions: Int,
        val consecutiveWeeks: Int,
    ) : AlertTriggerData

    data class TonnageDropTrigger(
        val muscleGroup: String,
        val dropPercentage: Double,
        val previousTonnage: Double,
        val currentTonnage: Double,
        val isDeloadContextualized: Boolean,
    ) : AlertTriggerData

    data class InactivityTrigger(
        val routineId: Long,
        val routineName: String,
        val daysSinceLastSession: Long,
        val muscleGroups: List<String>,
    ) : AlertTriggerData

    data class DeloadTrigger(
        val routineId: Long,
        val routineName: String,
        val muscleGroups: List<String>,
    ) : AlertTriggerData
}
