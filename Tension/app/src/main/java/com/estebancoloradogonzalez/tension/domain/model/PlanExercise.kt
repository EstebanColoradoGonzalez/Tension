package com.estebancoloradogonzalez.tension.domain.model

data class PlanExercise(
    val exerciseId: Long,
    val name: String,
    val equipmentTypeName: String,
    val muscleZones: List<String>,
    val sets: Int,
    val reps: String,
    val isBodyweight: Boolean,
    val isIsometric: Boolean,
    val isToTechnicalFailure: Boolean,
    val isCustom: Boolean,
)
