package com.estebancoloradogonzalez.tension.ui.metrics

import com.estebancoloradogonzalez.tension.domain.model.MuscleGroupTrend

sealed interface TrendUiState {
    data object Loading : TrendUiState
    data class Content(val trends: List<MuscleGroupTrend>) : TrendUiState
    data class InsufficientData(val remaining: Int) : TrendUiState
    data class Error(val message: String) : TrendUiState
}
