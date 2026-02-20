package com.estebancoloradogonzalez.tension.domain.model

data class ModuleInactivityData(
    val moduleCode: String,
    val lastSessionDate: String?,
    val daysSinceLastSession: Long,
    val muscleGroups: List<String>,
)
