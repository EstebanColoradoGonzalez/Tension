package com.estebancoloradogonzalez.tension.ui.catalog

data class ExerciseDictionaryUiState(
    val isLoading: Boolean = true,
    val exercises: List<ExerciseItem> = emptyList(),
    val totalCount: Int = 0,
    val equipmentOptions: List<String> = emptyList(),
    val muscleZoneOptions: List<String> = emptyList(),
    val selectedEquipment: String? = null,
    val selectedMuscleZone: String? = null,
)

data class ExerciseItem(
    val id: Long,
    val name: String,
    val equipmentTypeName: String,
    val muscleZonesSummary: String,
    val isCustom: Boolean,
)
