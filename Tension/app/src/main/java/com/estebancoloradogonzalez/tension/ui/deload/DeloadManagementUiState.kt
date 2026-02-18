package com.estebancoloradogonzalez.tension.ui.deload

import com.estebancoloradogonzalez.tension.domain.model.DeloadState

sealed interface DeloadManagementUiState {
    data object Loading : DeloadManagementUiState
    data class Content(val deloadState: DeloadState) : DeloadManagementUiState
    data class Error(val message: String) : DeloadManagementUiState
}
