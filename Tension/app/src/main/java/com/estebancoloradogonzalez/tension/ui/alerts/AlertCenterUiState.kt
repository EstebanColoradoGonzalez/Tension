package com.estebancoloradogonzalez.tension.ui.alerts

import com.estebancoloradogonzalez.tension.domain.model.AlertItem

sealed interface AlertCenterUiState {
    data object Loading : AlertCenterUiState
    data object Empty : AlertCenterUiState
    data class Loaded(
        val crisisAlerts: List<AlertItem>,
        val regularAlerts: List<AlertItem>,
        val totalCount: Int,
    ) : AlertCenterUiState
}
