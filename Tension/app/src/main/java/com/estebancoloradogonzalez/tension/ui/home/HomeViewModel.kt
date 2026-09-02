package com.estebancoloradogonzalez.tension.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.model.DeloadHomeState
import com.estebancoloradogonzalez.tension.domain.usecase.alerts.GetActiveAlertCountUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.deload.GetDeloadStateUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.ClearTemporaryRoutineUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.GetActiveSessionUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.GetMicrocycleCountUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.GetReassignableRoutinesUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.GetTodaySessionUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.SetTemporaryRoutineUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.SkipTodayUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.StartSessionUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.UndoSkipTodayUseCase
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
    private val getTodaySessionUseCase: GetTodaySessionUseCase,
    private val getActiveSessionUseCase: GetActiveSessionUseCase,
    private val startSessionUseCase: StartSessionUseCase,
    private val getMicrocycleCountUseCase: GetMicrocycleCountUseCase,
    private val getDeloadStateUseCase: GetDeloadStateUseCase,
    private val getActiveAlertCountUseCase: GetActiveAlertCountUseCase,
    private val getReassignableRoutinesUseCase: GetReassignableRoutinesUseCase,
    private val setTemporaryRoutineUseCase: SetTemporaryRoutineUseCase,
    private val clearTemporaryRoutineUseCase: ClearTemporaryRoutineUseCase,
    private val skipTodayUseCase: SkipTodayUseCase,
    private val undoSkipTodayUseCase: UndoSkipTodayUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<Long>(replay = 0)
    val navigationEvent: SharedFlow<Long> = _navigationEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(
                getTodaySessionUseCase(),
                getActiveSessionUseCase(),
                getMicrocycleCountUseCase(),
                getActiveAlertCountUseCase(),
                getReassignableRoutinesUseCase(),
            ) { todaySession, activeSession, microcycleCount, alertCount, reassignOptions ->
                HomeUiState(
                    isLoading = false,
                    todaySession = todaySession,
                    activeSession = activeSession,
                    microcycleCount = microcycleCount,
                    alertCount = alertCount,
                    reassignOptions = reassignOptions,
                )
            }.collect { newState ->
                _uiState.update { current ->
                    newState.copy(
                        deloadState = current.deloadState,
                        isReassignDialogOpen = current.isReassignDialogOpen,
                    )
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

    fun openReassignDialog() {
        if (!_uiState.value.canReassign) return
        _uiState.update { it.copy(isReassignDialogOpen = true) }
    }

    fun dismissReassignDialog() {
        _uiState.update { it.copy(isReassignDialogOpen = false) }
    }

    /**
     * Persiste la reasignación temporal. La propuesta se recompone por el flujo de
     * `getTodaySessionUseCase`, no se escribe aquí: el estado de la pantalla sigue derivando
     * de la base, y una reasignación rechazada no deja rastro en la interfaz.
     */
    fun confirmReassign(routineId: Long) {
        _uiState.update { it.copy(isReassignDialogOpen = false) }
        viewModelScope.launch {
            try {
                setTemporaryRoutineUseCase(routineId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    /** Declara que hoy no se entrena. No crea sesión: el día queda resuelto sin entrenar. */
    fun skipToday() {
        if (!_uiState.value.canSkipToday) return
        viewModelScope.launch {
            try {
                skipTodayUseCase()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun undoSkipToday() {
        viewModelScope.launch {
            try {
                undoSkipTodayUseCase()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun undoReassign() {
        viewModelScope.launch {
            try {
                clearTemporaryRoutineUseCase()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
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
