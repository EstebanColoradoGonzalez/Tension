package com.estebancoloradogonzalez.tension.ui.history

import com.estebancoloradogonzalez.tension.domain.model.SessionDetail

sealed interface SessionDetailUiState {
    data object Loading : SessionDetailUiState
    data class Loaded(val detail: SessionDetail) : SessionDetailUiState
}
