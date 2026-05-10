package com.estebancoloradogonzalez.tension.ui.catalog

data class PlanVersionDetailUiState(
    val isLoading: Boolean = true,
    val routineName: String = "",
    val versionNumber: Int = 0,
    val exercises: List<PlanExerciseItem> = emptyList(),
)

data class PlanExerciseItem(
    val exerciseId: Long,
    val name: String,
    val equipmentTypeName: String,
    val muscleZonesSummary: String,
    val sets: Int,
    val repsDisplay: String,
    val repsRaw: String,
    val isSpecialCondition: Boolean,
    val isCustom: Boolean,
    val isBodyweight: Boolean = false,
    val slot: Int = 0,
    /** Names of alternative exercises in the same slot (excluding the primary) */
    val alternativeNames: List<String> = emptyList(),
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

data class EditPlanAssignmentState(
    val isVisible: Boolean = false,
    val exerciseId: Long = 0,
    val exerciseName: String = "",
    val sets: Int = 4,
    val reps: String = "8-12",
    val isSaving: Boolean = false,
)

data class AddAlternativeSheetState(
    val isVisible: Boolean = false,
    val slot: Int = 0,
    val slotName: String = "",
    val availableExercises: List<AssignableExerciseItem> = emptyList(),
    val selectedExerciseId: Long? = null,
    val isAssigning: Boolean = false,
)
