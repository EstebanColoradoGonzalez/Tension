package com.estebancoloradogonzalez.tension.domain.model

data class ExerciseHistoryEntry(
    val date: String,
    val moduleCode: String,
    val versionNumber: Int,
    val avgWeightKg: Double,
    val totalReps: Int,
    val avgRir: Double,
    val classification: ProgressionClassification?,
)
