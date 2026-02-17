package com.estebancoloradogonzalez.tension.domain.model

data class SessionSummary(
    val status: String,
    val moduleCode: String,
    val versionNumber: Int,
    val totalTonnageKg: Double,
    val completedExercises: Int,
    val totalExercises: Int,
    val exercises: List<ExerciseSummaryItem>,
)
