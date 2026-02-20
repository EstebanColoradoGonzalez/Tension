package com.estebancoloradogonzalez.tension.ui.settings

sealed interface ExportBackupUiState {
    data object Idle : ExportBackupUiState
    data object Exporting : ExportBackupUiState
    data class Success(val fileName: String, val displayPath: String) : ExportBackupUiState
    data class Error(val message: String) : ExportBackupUiState
}
