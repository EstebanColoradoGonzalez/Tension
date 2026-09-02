package com.estebancoloradogonzalez.tension.ui.metrics

import com.estebancoloradogonzalez.tension.domain.model.MuscleGroupTrend
import com.estebancoloradogonzalez.tension.ui.components.MetricRequirement

sealed interface TrendUiState {
    data object Loading : TrendUiState

    /**
     * [evaluatedMicrocycles] is the window the classification was actually computed
     * over — the last complete microcycles the trend rule takes, not the whole history.
     */
    data class Content(
        val trends: List<MuscleGroupTrend>,
        val evaluatedMicrocycles: Int,
    ) : TrendUiState

    data class InsufficientData(val requirement: MetricRequirement) : TrendUiState

    data class Error(val message: String) : TrendUiState
}
