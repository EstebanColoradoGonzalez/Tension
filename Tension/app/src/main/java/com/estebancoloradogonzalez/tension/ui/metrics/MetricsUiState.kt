package com.estebancoloradogonzalez.tension.ui.metrics

import com.estebancoloradogonzalez.tension.domain.model.AdherenceData
import com.estebancoloradogonzalez.tension.domain.model.ExerciseLoadVelocity
import com.estebancoloradogonzalez.tension.domain.model.ExerciseProgressionRate
import com.estebancoloradogonzalez.tension.domain.model.RirByRoutine

sealed interface MetricsUiState {
    data object Loading : MetricsUiState
    data class Content(
        val adherence: AdherenceData,
        val rirByRoutine: List<RirByRoutine>,
        val progressionRates: List<ExerciseProgressionRate>,
        val loadVelocities: List<ExerciseLoadVelocity>,
    ) : MetricsUiState
    data class Error(val message: String) : MetricsUiState
}
