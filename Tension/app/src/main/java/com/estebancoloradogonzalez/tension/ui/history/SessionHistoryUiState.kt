package com.estebancoloradogonzalez.tension.ui.history

import com.estebancoloradogonzalez.tension.domain.model.SessionHistoryItem

sealed interface SessionHistoryUiState {
    data object Loading : SessionHistoryUiState
    data object Empty : SessionHistoryUiState
    data class Loaded(val sessions: List<SessionHistoryItem>) : SessionHistoryUiState
}
