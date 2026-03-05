package com.estebancoloradogonzalez.tension.ui.preview

data class SessionPreviewUiState(
    val isLoading: Boolean = true,
    val moduleCode: String = "",
    val versionNumber: Int = 0,
    val moduleVersionId: Long = 0L,
    val exercises: List<PreviewExerciseItem> = emptyList(),
    val isDeloadActive: Boolean = false,
    val deloadSessionsRemaining: Int = 0,
)

data class PreviewExerciseItem(
    val exerciseId: Long,
    val name: String,
    val equipmentTypeName: String,
    val muscleZones: String,
    val setsDisplay: String,
    val repsDisplay: String,
    val isRepsSpecial: Boolean,
    val loadDisplayText: String,
    val isBodyweight: Boolean,
    val showOutOfGymBadge: Boolean,
)
