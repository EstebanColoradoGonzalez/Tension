package com.estebancoloradogonzalez.tension.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.data.local.storage.BackupFileManager
import com.estebancoloradogonzalez.tension.domain.usecase.backup.ImportBackupUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.backup.ValidateBackupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImportBackupViewModel @Inject constructor(
    private val validateBackupUseCase: ValidateBackupUseCase,
    private val importBackupUseCase: ImportBackupUseCase,
    private val backupFileManager: BackupFileManager,
) : ViewModel() {

    internal var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val _uiState = MutableStateFlow<ImportBackupUiState>(ImportBackupUiState.Idle)
    val uiState: StateFlow<ImportBackupUiState> = _uiState.asStateFlow()

    private var pendingJson: String? = null

    fun selectFile(uri: Uri) {
        _uiState.value = ImportBackupUiState.Validating
        viewModelScope.launch(ioDispatcher) {
            try {
                val json = backupFileManager.readFromUri(uri)
                val result = validateBackupUseCase(json)
                if (result.isValid) {
                    pendingJson = json
                    _uiState.value = ImportBackupUiState.Validated(result)
                } else {
                    _uiState.value = ImportBackupUiState.Invalid(
                        result.errorMessage
                            ?: "El archivo seleccionado no es un respaldo válido o está corrupto.",
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ImportBackupUiState.Invalid(
                    e.message ?: "Error al leer el archivo",
                )
            }
        }
    }

    fun confirmImport() {
        val json = pendingJson ?: return
        _uiState.value = ImportBackupUiState.Importing
        viewModelScope.launch(ioDispatcher) {
            try {
                importBackupUseCase(json)
                pendingJson = null
                _uiState.value = ImportBackupUiState.Success
            } catch (_: Exception) {
                _uiState.value = ImportBackupUiState.Error(
                    "La importación falló. Tus datos originales han sido preservados.",
                )
            }
        }
    }

    fun cancel() {
        pendingJson = null
        _uiState.value = ImportBackupUiState.Idle
    }
}
