package com.estebancoloradogonzalez.tension.ui.onboarding

import com.estebancoloradogonzalez.tension.domain.model.ExperienceLevel

data class RegisterProfileUiState(
    val weightKg: String = "",
    val heightM: String = "",
    val experienceLevel: ExperienceLevel? = null,
    val showWeightError: Boolean = false,
    val showHeightError: Boolean = false,
    val isLoading: Boolean = false,
) {
    val isFormValid: Boolean
        get() {
            val weight = weightKg.toDoubleOrNull()
            val height = heightM.toDoubleOrNull()
            return weight != null && weight > 0 &&
                height != null && height > 0 &&
                experienceLevel != null
        }
}

sealed interface RegisterProfileEvent {
    data object NavigateToHome : RegisterProfileEvent
}
