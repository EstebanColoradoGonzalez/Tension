package com.estebancoloradogonzalez.tension.ui.history

import com.estebancoloradogonzalez.tension.domain.model.ExerciseHistoryData
import com.estebancoloradogonzalez.tension.ui.components.TrendPoint

sealed interface ExerciseHistoryUiState {
    data object Loading : ExerciseHistoryUiState
    data object Empty : ExerciseHistoryUiState
    data class Loaded(
        val data: ExerciseHistoryData,
        val trendPoints: List<TrendPoint>,
        val yAxisLabel: String,
    ) : ExerciseHistoryUiState
}
