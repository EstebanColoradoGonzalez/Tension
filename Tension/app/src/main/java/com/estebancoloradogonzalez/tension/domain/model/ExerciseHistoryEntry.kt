package com.estebancoloradogonzalez.tension.domain.model

data class ExerciseHistoryEntry(
    val date: String,
    val routineName: String,
    val versionNumber: Int,
    /** Implemento de las series que promedia esta entrada. */
    val equipmentTypeName: String,
    val avgWeightKg: Double,
    val totalReps: Int,
    val avgRir: Double,
    val classification: ProgressionClassification?,
    val isDeload: Boolean = false,
)
