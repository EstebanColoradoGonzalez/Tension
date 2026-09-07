package com.estebancoloradogonzalez.tension.ui.session

import com.estebancoloradogonzalez.tension.domain.model.EquipmentType
import com.estebancoloradogonzalez.tension.domain.model.WeightUnit
import com.estebancoloradogonzalez.tension.domain.rules.ExternalLoadRule

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
    val isIsometric: Boolean = false,
    val isBodyweight: Boolean = false,
    /** Implementos que el ejercicio admite, en orden de catálogo. */
    val equipmentOptions: List<EquipmentType> = emptyList(),
    val selectedEquipmentTypeId: Long? = null,
    val equipmentError: String? = null,
    val weightError: String? = null,
    val repsError: String? = null,
    val isSaving: Boolean = false,
    val showChronometer: Boolean = false,
    val timerState: TimerState = TimerState.IDLE,
    val timerSeconds: Int = 0,
    val minSeconds: Int? = null,
    val maxSeconds: Int? = null,
) {
    val selectedEquipmentName: String?
        get() = equipmentOptions.firstOrNull { it.id == selectedEquipmentTypeId }?.name

    /**
     * Si el ejecutante teclea un peso para esta serie.
     *
     * Lo decide el implemento, no solo la marca del ejercicio: la dominada estricta y la
     * máquina asistida registran 0, y solo `Peso Añadido` habilita la captura (CA-39.05).
     */
    val isWeightEditable: Boolean
        get() = ExternalLoadRule.isCaptureEnabled(
            isBodyweight = isBodyweight,
            isIsometric = isIsometric,
            equipmentName = selectedEquipmentName,
        )

    /** Sin carga externa no hay unidad que elegir. */
    val isUnitSelectorVisible: Boolean
        get() = isWeightEditable

    /** El lastre es lo único que se captura sobre un ejercicio de peso corporal. */
    val isAddedWeight: Boolean
        get() = ExternalLoadRule.requiresPositiveLoad(selectedEquipmentName)

    val isConfirmEnabled: Boolean
        get() = !isLoading &&
            selectedRir != null &&
            selectedEquipmentTypeId != null &&
            weightInput.isNotBlank() &&
            weightError == null &&
            repsError == null &&
            !isSaving &&
            (!isAddedWeight || (convertedWeightKg ?: 0.0) > 0.0) &&
            if (showChronometer) {
                timerState == TimerState.STOPPED && timerSeconds > 0
            } else {
                reps.isNotBlank()
            }
}
