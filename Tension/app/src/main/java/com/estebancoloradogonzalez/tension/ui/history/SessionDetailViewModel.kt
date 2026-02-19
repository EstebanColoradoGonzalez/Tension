package com.estebancoloradogonzalez.tension.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.usecase.history.GetSessionDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    private val getSessionDetailUseCase: GetSessionDetailUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val sessionId: Long = savedStateHandle.get<Long>("sessionId")
        ?: throw IllegalArgumentException("sessionId is required")

    private val _uiState = MutableStateFlow<SessionDetailUiState>(SessionDetailUiState.Loading)
    val uiState: StateFlow<SessionDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val detail = getSessionDetailUseCase(sessionId)
            _uiState.value = SessionDetailUiState.Loaded(detail)
        }
    }
}
