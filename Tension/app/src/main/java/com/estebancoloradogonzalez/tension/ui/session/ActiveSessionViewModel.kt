package com.estebancoloradogonzalez.tension.ui.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.model.DeloadState
import com.estebancoloradogonzalez.tension.domain.model.ExerciseSessionStatus
import com.estebancoloradogonzalez.tension.domain.usecase.deload.GetDeloadStateUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.CloseSessionUseCase
import com.estebancoloradogonzalez.tension.domain.util.LoadDisplayMapper
import com.estebancoloradogonzalez.tension.domain.usecase.session.GetSessionExercisesUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.FinalizeExerciseUseCase
import com.estebancoloradogonzalez.tension.domain.repository.PlanRepository
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
    private val finalizeExerciseUseCase: FinalizeExerciseUseCase,
    private val sessionRepository: SessionRepository,
    private val planRepository: PlanRepository,
    private val getDeloadStateUseCase: GetDeloadStateUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val sessionId: Long = savedStateHandle.get<Long>("sessionId")
        ?: throw IllegalArgumentException("sessionId is required")

    private val _uiState = MutableStateFlow(ActiveSessionUiState())
    val uiState: StateFlow<ActiveSessionUiState> = _uiState.asStateFlow()

    private val _navigateToSessionSummary = MutableSharedFlow<Long>(replay = 0)
    val navigateToSessionSummary: SharedFlow<Long> = _navigateToSessionSummary.asSharedFlow()

    private val _alternativeSelectionState = MutableStateFlow(AlternativeSelectionUiState())
    val alternativeSelectionState: StateFlow<AlternativeSelectionUiState> = _alternativeSelectionState.asStateFlow()

    init {
        viewModelScope.launch {
            val deloadId = sessionRepository.getDeloadIdBySessionId(sessionId)
            val isDeload = deloadId != null
            val deloadProgressText = if (deloadId != null) {
                val deloadState = getDeloadStateUseCase()
                if (deloadState is DeloadState.DeloadActive) {
                    "${deloadState.progress + 1}/${deloadState.totalSessions}"
                } else {
                    ""
                }
            } else {
                ""
            }

            combine(
                getSessionExercisesUseCase(sessionId),
                sessionRepository.getSessionRoutineVersion(sessionId),
            ) { exercises, routineVersion ->
                exercises to routineVersion
            }.collect { (exercises, routineVersion) ->
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        isDeloadSession = isDeload,
                        deloadProgress = deloadProgressText,
                        routineName = routineVersion?.first ?: "",
                        versionNumber = routineVersion?.second ?: 0,
                        exercises = exercises.map { detail ->
                            val statusLabel = when (detail.status) {
                                ExerciseSessionStatus.NOT_STARTED -> "No Iniciado"
                                ExerciseSessionStatus.IN_PROGRESS -> "En Ejecución"
                                ExerciseSessionStatus.COMPLETED -> "Completado"
                            }
                            val loadText = LoadDisplayMapper.mapLoadDisplay(
                                isDeload = isDeload,
                                isIsometric = detail.isIsometric,
                                isBodyweight = detail.isBodyweight,
                                prescribedLoadKg = detail.prescribedLoadKg,
                                muscleGroup = detail.muscleGroup,
                            )
                            ExerciseUiItem(
                                sessionExerciseId = detail.sessionExerciseId,
                                exerciseId = detail.exerciseId,
                                name = detail.name ?: "",
                                equipmentTypeName = detail.equipmentTypeName ?: "",
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
                                statusDisplayText = "$statusLabel · ${detail.completedSets}/${detail.sets} series",
                                isFinalized = detail.isFinalized,
                                slot = detail.slot,
                                hasAlternatives = detail.hasAlternatives,
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
            _uiState.update { it.copy(isClosing = true, showCloseDialog = false, errorMessage = null) }
            try {
                closeSessionUseCase(sessionId)
                _navigateToSessionSummary.emit(sessionId)
            } catch (e: Exception) {
                _uiState.update { it.copy(isClosing = false, errorMessage = e.message) }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onFinalizeExercise(sessionExerciseId: Long) {
        viewModelScope.launch {
            try {
                finalizeExerciseUseCase(sessionExerciseId)
            } catch (_: Exception) {
                // Finalization failed — no-op
            }
        }
    }

    fun onSelectAlternative(exercise: ExerciseUiItem) {
        if (!exercise.hasAlternatives) return
        if (exercise.completedSets > 0) return
        if (exercise.isFinalized) return
        viewModelScope.launch {
            try {
                val routineVersionId = sessionRepository.getRoutineVersionIdBySessionId(sessionId)
                val options = planRepository.getAlternativesForSlot(routineVersionId, exercise.slot)
                _alternativeSelectionState.value = AlternativeSelectionUiState(
                    isVisible = true,
                    sessionExerciseId = exercise.sessionExerciseId,
                    slot = exercise.slot,
                    alternatives = options.map { alt ->
                        AlternativeOption(
                            exerciseId = alt.exerciseId,
                            name = alt.name,
                            equipmentTypeName = alt.equipmentTypeName,
                            muscleZonesSummary = alt.muscleZones.joinToString(", "),
                        )
                    },
                )
            } catch (_: Exception) {
                // Failed to load alternatives — no-op
            }
        }
    }

    fun onAlternativeSelected(exerciseId: Long) {
        _alternativeSelectionState.update { it.copy(selectedExerciseId = exerciseId) }
    }

    fun onConfirmAlternativeSelection() {
        val state = _alternativeSelectionState.value
        val exerciseId = state.selectedExerciseId ?: return
        viewModelScope.launch {
            try {
                sessionRepository.switchAlternativeInSession(state.sessionExerciseId, exerciseId)
            } catch (_: Exception) {
                // Failed — no-op
            } finally {
                _alternativeSelectionState.value = AlternativeSelectionUiState()
            }
        }
    }

    fun onDismissAlternativeSelection() {
        _alternativeSelectionState.value = AlternativeSelectionUiState()
    }
}
