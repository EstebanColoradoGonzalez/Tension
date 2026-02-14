package com.estebancoloradogonzalez.tension.domain.model

data class RegisterSetInfo(
    val sessionExerciseId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val currentSetNumber: Int,
    val totalSets: Int,
    val lastWeightKg: Double?,
    val isBodyweight: Boolean,
    val isIsometric: Boolean,
    val isToTechnicalFailure: Boolean,
)
