package com.estebancoloradogonzalez.tension.ui.session

import com.estebancoloradogonzalez.tension.domain.model.SessionSummary

sealed interface SessionSummaryUiState {
    data object Loading : SessionSummaryUiState
    data class Success(val summary: SessionSummary) : SessionSummaryUiState
    data class Error(val message: String) : SessionSummaryUiState
}
