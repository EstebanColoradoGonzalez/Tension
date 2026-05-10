package com.estebancoloradogonzalez.tension.domain.model

data class ExerciseSummaryItem(
    val exerciseId: Long,
    val name: String,
    val classification: ProgressionClassification?,
    val signal: ActionSignal,
    val weightKg: Double,
    val isBodyweight: Boolean,
    val isIsometric: Boolean,
    val isMastered: Boolean,
    val isDeload: Boolean,
    val completedSets: Int,
    val prescribedSets: Int,
)
