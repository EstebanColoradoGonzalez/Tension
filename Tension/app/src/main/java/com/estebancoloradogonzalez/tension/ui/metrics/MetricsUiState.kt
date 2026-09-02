package com.estebancoloradogonzalez.tension.ui.metrics

import com.estebancoloradogonzalez.tension.domain.model.AdherenceData
import com.estebancoloradogonzalez.tension.domain.model.ExerciseLoadVelocity
import com.estebancoloradogonzalez.tension.domain.model.ExerciseProgressionRate
import com.estebancoloradogonzalez.tension.domain.model.RirByRoutine

sealed interface MetricsUiState {
    data object Loading : MetricsUiState

    /**
     * [progressionWeeks] and [rirSessionLimit] are the windows the values were actually
     * computed over. They belong to the state and not to the screen so the period shown
     * next to a value is always the one that produced it.
     */
    data class Content(
        val adherence: AdherenceData,
        val rirByRoutine: List<RirByRoutine>,
        val progressionRates: List<ExerciseProgressionRate>,
        val loadVelocities: List<ExerciseLoadVelocity>,
        val progressionWeeks: Int,
        val rirSessionLimit: Int,
    ) : MetricsUiState

    data class Error(val message: String) : MetricsUiState
}
