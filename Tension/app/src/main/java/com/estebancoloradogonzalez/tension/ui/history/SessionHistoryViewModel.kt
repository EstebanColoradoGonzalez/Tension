package com.estebancoloradogonzalez.tension.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.usecase.history.GetSessionHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionHistoryViewModel @Inject constructor(
    private val getSessionHistoryUseCase: GetSessionHistoryUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SessionHistoryUiState>(SessionHistoryUiState.Loading)
    val uiState: StateFlow<SessionHistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val sessions = getSessionHistoryUseCase()
            _uiState.value = if (sessions.isEmpty()) {
                SessionHistoryUiState.Empty
            } else {
                SessionHistoryUiState.Loaded(sessions)
            }
        }
    }
}
