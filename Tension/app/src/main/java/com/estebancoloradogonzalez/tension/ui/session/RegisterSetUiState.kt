package com.estebancoloradogonzalez.tension.ui.session

data class RegisterSetUiState(
    val isLoading: Boolean = true,
    val exerciseName: String = "",
    val currentSetNumber: Int = 1,
    val totalSets: Int = 4,
    val weightKg: String = "",
    val reps: String = "",
    val selectedRir: Int? = null,
    val isWeightEditable: Boolean = true,
    val isIsometric: Boolean = false,
    val isBodyweight: Boolean = false,
    val weightError: String? = null,
    val repsError: String? = null,
    val isSaving: Boolean = false,
) {
    val isConfirmEnabled: Boolean
        get() = !isLoading &&
            selectedRir != null &&
            weightKg.isNotBlank() &&
            reps.isNotBlank() &&
            weightError == null &&
            repsError == null &&
            !isSaving
}
