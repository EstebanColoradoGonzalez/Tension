package com.estebancoloradogonzalez.tension.domain.model

sealed interface AlertTriggerData {

    data class PlateauTrigger(
        val sessions: List<PlateauSession>,
    ) : AlertTriggerData

    data class PlateauSession(
        val date: String,
        val weightKg: Double,
        val totalReps: Int,
    )

    data class ProgressionRateTrigger(
        val rate: Double,
        val exerciseName: String,
    ) : AlertTriggerData

    data class RirTrigger(
        val avgRir: Double,
        val moduleCode: String,
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
        val moduleCode: String,
        val daysSinceLastSession: Long,
        val muscleGroups: List<String>,
    ) : AlertTriggerData
}
