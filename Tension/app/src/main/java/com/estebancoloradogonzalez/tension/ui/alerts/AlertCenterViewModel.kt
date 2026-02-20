package com.estebancoloradogonzalez.tension.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.usecase.alerts.GetActiveAlertsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertCenterViewModel @Inject constructor(
    private val getActiveAlertsUseCase: GetActiveAlertsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AlertCenterUiState>(AlertCenterUiState.Loading)
    val uiState: StateFlow<AlertCenterUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getActiveAlertsUseCase().collect { alerts ->
                _uiState.value = if (alerts.isEmpty()) {
                    AlertCenterUiState.Empty
                } else {
                    val crisisAlerts = alerts.filter { it.level == "CRISIS" }
                    val regularAlerts = alerts.filter { it.level != "CRISIS" }
                    AlertCenterUiState.Loaded(
                        crisisAlerts = crisisAlerts,
                        regularAlerts = regularAlerts,
                        totalCount = alerts.size,
                    )
                }
            }
        }
    }
}
