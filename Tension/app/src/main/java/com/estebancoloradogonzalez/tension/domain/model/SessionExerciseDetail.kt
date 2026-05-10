package com.estebancoloradogonzalez.tension.domain.model

data class SessionExerciseDetail(
    val sessionExerciseId: Long,
    val exerciseId: Long?,
    val name: String?,
    val equipmentTypeName: String?,
    val muscleZones: List<String>,
    val sets: Int,
    val reps: String,
    val isBodyweight: Boolean,
    val isIsometric: Boolean,
    val isToTechnicalFailure: Boolean,
    val prescribedLoadKg: Double?,
    val completedSets: Int,
    val status: ExerciseSessionStatus,
    val muscleGroup: String?,
    val isFinalized: Boolean,
    val pendingSelection: Boolean,
    val slot: Int,
    val hasAlternatives: Boolean = false,
)
