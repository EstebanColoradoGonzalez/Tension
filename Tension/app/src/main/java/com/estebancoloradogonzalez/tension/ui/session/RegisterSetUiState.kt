package com.estebancoloradogonzalez.tension.ui.session

enum class TimerState {
    IDLE,
    RUNNING,
    STOPPED,
}

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
    val showChronometer: Boolean = false,
    val timerState: TimerState = TimerState.IDLE,
    val timerSeconds: Int = 0,
    val minSeconds: Int? = null,
    val maxSeconds: Int? = null,
) {
    val isConfirmEnabled: Boolean
        get() = !isLoading &&
            selectedRir != null &&
            weightKg.isNotBlank() &&
            weightError == null &&
            repsError == null &&
            !isSaving &&
            if (showChronometer) {
                timerState == TimerState.STOPPED && timerSeconds > 0
            } else {
                reps.isNotBlank()
            }
}
