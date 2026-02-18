package com.estebancoloradogonzalez.tension.ui.deload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.usecase.deload.ActivateDeloadUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.deload.GetDeloadStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeloadManagementViewModel @Inject constructor(
    private val getDeloadStateUseCase: GetDeloadStateUseCase,
    private val activateDeloadUseCase: ActivateDeloadUseCase,
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<DeloadManagementUiState>(DeloadManagementUiState.Loading)
    val uiState: StateFlow<DeloadManagementUiState> = _uiState.asStateFlow()

    init {
        loadState()
    }

    private fun loadState() {
        viewModelScope.launch {
            try {
                val state = getDeloadStateUseCase()
                _uiState.value = DeloadManagementUiState.Content(state)
            } catch (e: Exception) {
                _uiState.value =
                    DeloadManagementUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun activateDeload() {
        viewModelScope.launch {
            try {
                activateDeloadUseCase()
                loadState()
            } catch (e: Exception) {
                _uiState.value =
                    DeloadManagementUiState.Error(e.message ?: "Error al activar")
            }
        }
    }
}
