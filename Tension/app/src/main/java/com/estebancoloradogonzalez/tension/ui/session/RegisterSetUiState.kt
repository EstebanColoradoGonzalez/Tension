package com.estebancoloradogonzalez.tension.ui.session

import com.estebancoloradogonzalez.tension.domain.model.WeightUnit

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
    /** Weight as typed by the executant, expressed in [captureUnit]. */
    val weightInput: String = "",
    val captureUnit: WeightUnit = WeightUnit.KG,
    /** Canonical value that would be persisted, or null when the input is not usable. */
    val convertedWeightKg: Double? = null,
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
    /** Exercises without external load have no unit to choose. */
    val isUnitSelectorVisible: Boolean
        get() = !isBodyweight && !isIsometric

    val isConfirmEnabled: Boolean
        get() = !isLoading &&
            selectedRir != null &&
            weightInput.isNotBlank() &&
            weightError == null &&
            repsError == null &&
            !isSaving &&
            if (showChronometer) {
                timerState == TimerState.STOPPED && timerSeconds > 0
            } else {
                reps.isNotBlank()
            }
}
