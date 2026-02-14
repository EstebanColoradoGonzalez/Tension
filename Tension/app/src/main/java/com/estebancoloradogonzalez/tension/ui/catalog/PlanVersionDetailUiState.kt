package com.estebancoloradogonzalez.tension.ui.catalog

data class PlanVersionDetailUiState(
    val isLoading: Boolean = true,
    val moduleCode: String = "",
    val moduleName: String = "",
    val versionNumber: Int = 0,
    val exercises: List<PlanExerciseItem> = emptyList(),
    val showDeleteDialog: Boolean = false,
    val exerciseToDelete: PlanExerciseItem? = null,
)

data class PlanExerciseItem(
    val exerciseId: Long,
    val name: String,
    val equipmentTypeName: String,
    val muscleZonesSummary: String,
    val sets: Int,
    val repsDisplay: String,
    val isSpecialCondition: Boolean,
    val isCustom: Boolean,
)

data class AssignExerciseSheetState(
    val isVisible: Boolean = false,
    val availableExercises: List<AssignableExerciseItem> = emptyList(),
    val selectedExerciseId: Long? = null,
    val sets: String = "4",
    val reps: String = "8-12",
    val isAssigning: Boolean = false,
)

data class AssignableExerciseItem(
    val id: Long,
    val name: String,
    val equipmentTypeName: String,
    val muscleZonesSummary: String,
)
