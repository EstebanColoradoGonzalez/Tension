package com.estebancoloradogonzalez.tension.domain.model

data class SessionPreviewExercise(
    val exerciseId: Long,
    val exerciseName: String,
    val moduleCode: String,
    val equipmentTypeName: String,
    val muscleZones: String,
    val sets: Int,
    val reps: String,
    val isBodyweight: Boolean,
    val isIsometric: Boolean,
    val isToTechnicalFailure: Boolean,
    val prescribedLoadKg: Double?,
    val loadIncrementKg: Double,
)
