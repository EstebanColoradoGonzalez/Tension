package com.estebancoloradogonzalez.tension.ui.session

import com.estebancoloradogonzalez.tension.domain.model.ExerciseSessionStatus

data class ActiveSessionUiState(
    val isLoading: Boolean = true,
    val moduleCode: String = "",
    val versionNumber: Int = 0,
    val exercises: List<ExerciseUiItem> = emptyList(),
    val showCloseDialog: Boolean = false,
    val isClosing: Boolean = false,
) {
    val completedCount: Int get() = exercises.count { it.status == ExerciseSessionStatus.COMPLETED }
    val totalCount: Int get() = exercises.size
    val progress: Float get() = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    val incompleteCount: Int get() = totalCount - completedCount
    val isAllCompleted: Boolean get() = completedCount == totalCount && totalCount > 0
}

data class ExerciseUiItem(
    val sessionExerciseId: Long,
    val exerciseId: Long,
    val name: String,
    val equipmentTypeName: String,
    val muscleZones: String,
    val sets: Int,
    val reps: String,
    val prescribedLoadKg: Double?,
    val isBodyweight: Boolean,
    val isIsometric: Boolean,
    val isToTechnicalFailure: Boolean,
    val completedSets: Int,
    val status: ExerciseSessionStatus,
    val loadDisplayText: String,
    val statusDisplayText: String,
)
