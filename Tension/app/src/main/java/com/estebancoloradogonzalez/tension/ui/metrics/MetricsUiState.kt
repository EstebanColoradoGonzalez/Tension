package com.estebancoloradogonzalez.tension.ui.metrics

import com.estebancoloradogonzalez.tension.domain.model.AdherenceData
import com.estebancoloradogonzalez.tension.domain.model.BodyWeightSummary
import com.estebancoloradogonzalez.tension.domain.model.ExerciseLoadVelocity
import com.estebancoloradogonzalez.tension.domain.model.ExerciseProgressionRate
import com.estebancoloradogonzalez.tension.domain.model.RirByRoutine

sealed interface MetricsUiState {
    data object Loading : MetricsUiState

    /**
     * [progressionWeeks] and [rirSessionLimit] are the windows the values were actually
     * computed over. They belong to the state and not to the screen so the period shown
     * next to a value is always the one that produced it.
     *
     * [bodyWeight] shares [progressionWeeks]: the body weight section is read over the same
     * period as the rest of the indicators (CA-48.03). It is `null` only without a profile.
     * [weightDialog] is `null` while the quick weight entry is closed.
     */
    data class Content(
        val adherence: AdherenceData,
        val rirByRoutine: List<RirByRoutine>,
        val progressionRates: List<ExerciseProgressionRate>,
        val loadVelocities: List<ExerciseLoadVelocity>,
        val progressionWeeks: Int,
        val rirSessionLimit: Int,
        val bodyWeight: BodyWeightSummary? = null,
        val weightDialog: WeightDialogState? = null,
    ) : MetricsUiState

    data class Error(val message: String) : MetricsUiState
}

/**
 * Quick weight entry (CA-48.04, CA-48.05). Same criterion as the weight of the profile: a
 * number greater than zero. An empty field disables saving without showing the error; any
 * other invalid value shows it.
 */
data class WeightDialogState(
    val input: String,
    val isSaving: Boolean = false,
) {
    val weightKg: Double?
        get() = input.toDoubleOrNull()?.takeIf { it > 0 }

    val showError: Boolean
        get() = input.isNotBlank() && weightKg == null

    val canSave: Boolean
        get() = weightKg != null && !isSaving
}

sealed interface MetricsEvent {
    data object WeightSaved : MetricsEvent
    data class SaveError(val message: String) : MetricsEvent
}
