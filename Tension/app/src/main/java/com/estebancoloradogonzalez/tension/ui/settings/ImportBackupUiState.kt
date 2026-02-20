package com.estebancoloradogonzalez.tension.ui.settings

import com.estebancoloradogonzalez.tension.domain.model.BackupValidationResult

sealed interface ImportBackupUiState {
    data object Idle : ImportBackupUiState
    data object Validating : ImportBackupUiState
    data class Validated(val result: BackupValidationResult) : ImportBackupUiState
    data class Invalid(val errorMessage: String) : ImportBackupUiState
    data object Importing : ImportBackupUiState
    data object Success : ImportBackupUiState
    data class Error(val message: String) : ImportBackupUiState
}
