package com.estebancoloradogonzalez.tension.domain.model

data class Exercise(
    val id: Long,
    val name: String,
    /** Implementos con los que el ejercicio se puede hacer. Nunca vacío. */
    val equipmentTypes: List<String>,
    val muscleZones: List<String>,
    val muscleGroup: String?,
    val isBodyweight: Boolean,
    val isIsometric: Boolean,
    val isToTechnicalFailure: Boolean,
    val isCustom: Boolean,
    val mediaResource: String?,
    val progressionDifficulty: ProgressionDifficulty,
)
