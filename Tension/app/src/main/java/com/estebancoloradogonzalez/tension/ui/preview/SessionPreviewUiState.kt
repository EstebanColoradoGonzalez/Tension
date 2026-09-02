package com.estebancoloradogonzalez.tension.ui.preview

import com.estebancoloradogonzalez.tension.domain.model.ReassignableRoutine
import com.estebancoloradogonzalez.tension.domain.model.WeekDay

data class SessionPreviewUiState(
    val isLoading: Boolean = true,
    val weekDay: WeekDay? = null,
    val routineName: String = "",
    val versionNumber: Int = 0,
    val routineVersionId: Long = 0L,
    val exercises: List<PreviewExerciseItem> = emptyList(),
    val isDeloadActive: Boolean = false,
    val deloadSessionsRemaining: Int = 0,
    val isTemporaryOverride: Boolean = false,
    val canReassign: Boolean = false,
    /** El día ya se resolvió: la sesión no puede iniciarse desde aquí tampoco. */
    val isDayResolved: Boolean = false,
    val reassignOptions: List<ReassignableRoutine> = emptyList(),
    val isReassignDialogOpen: Boolean = false,
    val errorMessage: String? = null,
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
