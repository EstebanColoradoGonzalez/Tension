package com.estebancoloradogonzalez.tension.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.data.local.storage.BackupFileManager
import com.estebancoloradogonzalez.tension.domain.usecase.backup.ExportBackupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExportBackupViewModel @Inject constructor(
    private val exportBackupUseCase: ExportBackupUseCase,
    private val backupFileManager: BackupFileManager,
) : ViewModel() {

    internal var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val _uiState = MutableStateFlow<ExportBackupUiState>(ExportBackupUiState.Idle)
    val uiState: StateFlow<ExportBackupUiState> = _uiState.asStateFlow()

    private var generatedJson: String? = null

    fun generateBackupFileName(): String = backupFileManager.generateBackupFileName()

    fun export(uri: Uri) {
        viewModelScope.launch(ioDispatcher) {
            _uiState.value = ExportBackupUiState.Exporting
            try {
                val json = exportBackupUseCase()
                generatedJson = json
                backupFileManager.writeToUri(json, uri)
                val fileName = backupFileManager.generateBackupFileName()
                val displayPath = backupFileManager.extractDisplayPath(uri)
                _uiState.value = ExportBackupUiState.Success(fileName, displayPath)
            } catch (e: Exception) {
                _uiState.value = ExportBackupUiState.Error(
                    e.message ?: "Error al exportar los datos",
                )
            }
        }
    }

    fun share(context: Context) {
        val json = generatedJson ?: return
        val fileName = backupFileManager.generateBackupFileName()
        val file = backupFileManager.writeToCacheForShare(json, fileName)
        val shareUri = backupFileManager.getShareableUri(file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
    }

    fun onExportPickerCancelled() {
        _uiState.value = ExportBackupUiState.Idle
    }
}
