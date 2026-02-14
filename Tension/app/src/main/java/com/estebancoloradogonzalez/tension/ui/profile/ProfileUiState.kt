package com.estebancoloradogonzalez.tension.ui.profile

import com.estebancoloradogonzalez.tension.domain.model.ExperienceLevel

data class ProfileUiState(
    val isLoading: Boolean = true,
    val weightKg: String = "",
    val heightM: String = "",
    val experienceLevel: ExperienceLevel? = null,
    val originalWeightKg: String = "",
    val originalHeightM: String = "",
    val originalExperienceLevel: ExperienceLevel? = null,
    val showWeightError: Boolean = false,
    val showHeightError: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isDirty: Boolean
        get() = weightKg != originalWeightKg ||
            heightM != originalHeightM ||
            experienceLevel != originalExperienceLevel

    val isFormValid: Boolean
        get() {
            val weight = weightKg.toDoubleOrNull()
            val height = heightM.toDoubleOrNull()
            return weight != null && weight > 0 &&
                height != null && height > 0 &&
                experienceLevel != null
        }

    val canSave: Boolean
        get() = isDirty && isFormValid && !isSaving
}

sealed interface ProfileEvent {
    data object SaveSuccess : ProfileEvent
    data class SaveError(val message: String) : ProfileEvent
}
