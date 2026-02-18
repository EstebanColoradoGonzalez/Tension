package com.estebancoloradogonzalez.tension.domain.model

data class SessionExerciseDetail(
    val sessionExerciseId: Long,
    val exerciseId: Long,
    val name: String,
    val equipmentTypeName: String,
    val muscleZones: List<String>,
    val sets: Int,
    val reps: String,
    val isBodyweight: Boolean,
    val isIsometric: Boolean,
    val isToTechnicalFailure: Boolean,
    val prescribedLoadKg: Double?,
    val completedSets: Int,
    val status: ExerciseSessionStatus,
    val loadIncrementKg: Double = 2.5,
)
