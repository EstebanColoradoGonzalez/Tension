package com.estebancoloradogonzalez.tension.ui.alerts

import com.estebancoloradogonzalez.tension.domain.model.AlertDetail

sealed interface AlertDetailUiState {
    data object Loading : AlertDetailUiState
    data class Loaded(val detail: AlertDetail) : AlertDetailUiState
}
