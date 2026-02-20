package com.estebancoloradogonzalez.tension.ui.alerts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.usecase.alerts.GetAlertDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertDetailViewModel @Inject constructor(
    private val getAlertDetailUseCase: GetAlertDetailUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val alertId: Long = savedStateHandle.get<Long>("alertId")
        ?: throw IllegalArgumentException("alertId is required")

    private val _uiState = MutableStateFlow<AlertDetailUiState>(AlertDetailUiState.Loading)
    val uiState: StateFlow<AlertDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val detail = getAlertDetailUseCase(alertId)
            _uiState.value = AlertDetailUiState.Loaded(detail)
        }
    }
}
