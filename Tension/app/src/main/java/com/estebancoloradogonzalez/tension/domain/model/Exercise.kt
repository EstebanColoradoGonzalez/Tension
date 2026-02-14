package com.estebancoloradogonzalez.tension.domain.model

data class Exercise(
    val id: Long,
    val name: String,
    val moduleCode: String,
    val moduleName: String,
    val equipmentTypeName: String,
    val muscleZones: List<String>,
    val isBodyweight: Boolean,
    val isIsometric: Boolean,
    val isToTechnicalFailure: Boolean,
    val isCustom: Boolean,
    val mediaResource: String?,
)
