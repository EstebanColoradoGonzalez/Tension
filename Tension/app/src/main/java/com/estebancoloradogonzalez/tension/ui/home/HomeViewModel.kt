package com.estebancoloradogonzalez.tension.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.model.DeloadHomeState
import com.estebancoloradogonzalez.tension.domain.usecase.alerts.GetActiveAlertCountUseCase
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
    private val getActiveAlertCountUseCase: GetActiveAlertCountUseCase,
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
                getActiveAlertCountUseCase(),
            ) { nextSession, activeSession, microcycleCount, alertCount ->
                HomeUiState(
                    isLoading = false,
                    nextSession = if (activeSession != null) null else nextSession,
                    activeSession = activeSession,
                    microcycleCount = microcycleCount,
                    alertCount = alertCount,
                )
            }.collect { newState ->
                _uiState.update { current ->
                    newState.copy(deloadState = current.deloadState)
                }
            }
        }

        loadDeloadState()
    }

    private fun loadDeloadState() {
        viewModelScope.launch {
            try {
                val deloadState = getDeloadStateUseCase()
                val homeState = when (deloadState) {
                    is DeloadState.DeloadActive -> DeloadHomeState.Active(
                        progress = deloadState.progress,
                        totalSessions = deloadState.totalSessions,
                        routineName = "",
                    )
                    is DeloadState.DeloadRequired -> DeloadHomeState.Required(
                        routineName = deloadState.routineNames.firstOrNull() ?: "",
                    )
                    else -> null
                }
                _uiState.update { it.copy(deloadState = homeState) }
            } catch (_: Exception) {
                // Deload state is non-critical — silently ignore errors
            }
        }
    }

    fun refreshDeloadState() {
        loadDeloadState()
    }

    fun startSession() {
        val routineVersionId = _uiState.value.nextSession?.routineVersionId ?: return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val sessionId = startSessionUseCase(routineVersionId)
                _navigationEvent.emit(sessionId)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resumeSession(sessionId: Long) {
        viewModelScope.launch {
            _navigationEvent.emit(sessionId)
        }
    }
}
