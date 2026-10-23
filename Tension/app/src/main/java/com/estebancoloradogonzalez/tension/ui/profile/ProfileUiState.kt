package com.estebancoloradogonzalez.tension.ui.profile

data class ProfileUiState(
    val isLoading: Boolean = true,
    val weightKg: String = "",
    val heightM: String = "",
    val originalWeightKg: String = "",
    val originalHeightM: String = "",
    val showWeightError: Boolean = false,
    val showHeightError: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isDirty: Boolean
        get() = weightKg != originalWeightKg ||
            heightM != originalHeightM

    val isFormValid: Boolean
        get() {
            val weight = weightKg.toDoubleOrNull()
            val height = heightM.toDoubleOrNull()
            return weight != null && weight > 0 &&
                height != null && height > 0
        }

    val canSave: Boolean
        get() = isDirty && isFormValid && !isSaving
}

sealed interface ProfileEvent {
    data object SaveSuccess : ProfileEvent
    data class SaveError(val message: String) : ProfileEvent
}
