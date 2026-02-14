package com.estebancoloradogonzalez.tension.ui.catalog

sealed interface ExerciseDetailUiState {
    data object Loading : ExerciseDetailUiState
    data class Success(val exercise: ExerciseDetailItem) : ExerciseDetailUiState
    data class Error(val message: String) : ExerciseDetailUiState
}

data class ExerciseDetailItem(
    val id: Long,
    val name: String,
    val moduleCode: String,
    val moduleName: String,
    val equipmentTypeName: String,
    val muscleZones: String,
    val isCustom: Boolean,
    val mediaResource: String?,
)
