package com.estebancoloradogonzalez.tension.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.model.DeloadHomeState
import com.estebancoloradogonzalez.tension.domain.usecase.deload.GetDeloadStateUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.GetActiveSessionUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.GetMicrocycleCountUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.GetNextSessionInfoUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.StartSessionUseCase
import com.estebancoloradogonzalez.tension.domain.model.DeloadState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getNextSessionInfoUseCase: GetNextSessionInfoUseCase,
    private val getActiveSessionUseCase: GetActiveSessionUseCase,
    private val startSessionUseCase: StartSessionUseCase,
    private val getMicrocycleCountUseCase: GetMicrocycleCountUseCase,
    private val getDeloadStateUseCase: GetDeloadStateUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<Long>(replay = 0)
    val navigationEvent: SharedFlow<Long> = _navigationEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(
                getNextSessionInfoUseCase(),
                getActiveSessionUseCase(),
                getMicrocycleCountUseCase(),
            ) { nextSession, activeSession, microcycleCount ->
                HomeUiState(
                    isLoading = false,
                    nextSession = if (activeSession != null) null else nextSession,
                    activeSession = activeSession,
                    microcycleCount = microcycleCount,
                    alertCount = 0,
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }

        viewModelScope.launch {
            try {
                val deloadState = getDeloadStateUseCase()
                val homeState = when (deloadState) {
                    is DeloadState.DeloadActive -> DeloadHomeState.Active(
                        progress = deloadState.progress,
                        moduleCode = "",
                    )
                    is DeloadState.DeloadRequired -> DeloadHomeState.Required(
                        moduleCode = deloadState.modules.firstOrNull() ?: "",
                    )
                    else -> null
                }
                _uiState.update { it.copy(deloadState = homeState) }
            } catch (_: Exception) {
                // Deload state is non-critical — silently ignore errors
            }
        }
    }

    fun startSession() {
        val moduleVersionId = _uiState.value.nextSession?.moduleVersionId ?: return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val sessionId = startSessionUseCase(moduleVersionId)
                _navigationEvent.emit(sessionId)
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun resumeSession(sessionId: Long) {
        viewModelScope.launch {
            _navigationEvent.emit(sessionId)
        }
    }
}
