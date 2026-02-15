package com.estebancoloradogonzalez.tension.ui.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.model.ExerciseSessionStatus
import com.estebancoloradogonzalez.tension.domain.usecase.session.CloseSessionUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.GetSessionExercisesUseCase
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
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
class ActiveSessionViewModel @Inject constructor(
    private val getSessionExercisesUseCase: GetSessionExercisesUseCase,
    private val closeSessionUseCase: CloseSessionUseCase,
    private val sessionRepository: SessionRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val sessionId: Long = savedStateHandle.get<Long>("sessionId")
        ?: throw IllegalArgumentException("sessionId is required")

    private val _uiState = MutableStateFlow(ActiveSessionUiState())
    val uiState: StateFlow<ActiveSessionUiState> = _uiState.asStateFlow()

    private val _navigateToSessionSummary = MutableSharedFlow<Long>(replay = 0)
    val navigateToSessionSummary: SharedFlow<Long> = _navigateToSessionSummary.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(
                getSessionExercisesUseCase(sessionId),
                sessionRepository.getSessionModuleVersion(sessionId),
            ) { exercises, moduleVersion ->
                exercises to moduleVersion
            }.collect { (exercises, moduleVersion) ->
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        moduleCode = moduleVersion?.first ?: "",
                        versionNumber = moduleVersion?.second ?: 0,
                        exercises = exercises.map { detail ->
                            val statusLabel = when (detail.status) {
                                ExerciseSessionStatus.NOT_STARTED -> "No Iniciado"
                                ExerciseSessionStatus.IN_PROGRESS -> "En Ejecuci\u00F3n"
                                ExerciseSessionStatus.COMPLETED -> "Completado"
                            }
                            val loadText = when {
                                detail.isIsometric -> "Isom\u00E9trico (30\u201345s)"
                                detail.isBodyweight -> "Peso corporal"
                                detail.prescribedLoadKg != null ->
                                    "%.1f Kg".format(detail.prescribedLoadKg)
                                else -> "Sin historial \u2014 establecer carga"
                            }
                            ExerciseUiItem(
                                sessionExerciseId = detail.sessionExerciseId,
                                exerciseId = detail.exerciseId,
                                name = detail.name,
                                equipmentTypeName = detail.equipmentTypeName,
                                muscleZones = detail.muscleZones.joinToString(", "),
                                sets = detail.sets,
                                reps = detail.reps,
                                prescribedLoadKg = detail.prescribedLoadKg,
                                isBodyweight = detail.isBodyweight,
                                isIsometric = detail.isIsometric,
                                isToTechnicalFailure = detail.isToTechnicalFailure,
                                completedSets = detail.completedSets,
                                status = detail.status,
                                loadDisplayText = loadText,
                                statusDisplayText = "$statusLabel \u00B7 ${detail.completedSets}/${detail.sets} series",
                            )
                        },
                    )
                }
            }
        }
    }

    fun onCloseSessionRequested() {
        if (_uiState.value.isClosing) return
        _uiState.update { it.copy(showCloseDialog = true) }
    }

    fun onCloseDialogDismissed() {
        _uiState.update { it.copy(showCloseDialog = false) }
    }

    fun onCloseSessionConfirmed() {
        if (_uiState.value.isClosing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isClosing = true, showCloseDialog = false) }
            try {
                closeSessionUseCase(sessionId)
                _navigateToSessionSummary.emit(sessionId)
            } catch (_: Exception) {
                _uiState.update { it.copy(isClosing = false) }
            }
        }
    }
}
