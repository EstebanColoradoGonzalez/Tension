package com.estebancoloradogonzalez.tension.ui.preview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import com.estebancoloradogonzalez.tension.domain.usecase.session.GetSessionPreviewUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.StartSessionUseCase
import com.estebancoloradogonzalez.tension.domain.util.LoadDisplayMapper
import com.estebancoloradogonzalez.tension.domain.util.RepsDisplayMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionPreviewViewModel @Inject constructor(
    private val getSessionPreviewUseCase: GetSessionPreviewUseCase,
    private val startSessionUseCase: StartSessionUseCase,
    private val sessionRepository: SessionRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val moduleVersionId: Long = checkNotNull(savedStateHandle["moduleVersionId"])
    private val moduleCode: String = checkNotNull(savedStateHandle["moduleCode"])
    private val versionNumber: Int = checkNotNull(savedStateHandle["versionNumber"])

    private val _uiState = MutableStateFlow(
        SessionPreviewUiState(
            moduleCode = moduleCode,
            versionNumber = versionNumber,
            moduleVersionId = moduleVersionId,
        ),
    )
    val uiState: StateFlow<SessionPreviewUiState> = _uiState.asStateFlow()

    private val _navigateToActiveSession = MutableSharedFlow<Long>(replay = 0)
    val navigateToActiveSession: SharedFlow<Long> = _navigateToActiveSession.asSharedFlow()

    init {
        viewModelScope.launch {
            val activeDeload = sessionRepository.getActiveDeload().first()

            val isDeload = activeDeload != null
            val deloadSessionsRemaining = if (activeDeload != null) {
                val count = sessionRepository.countDeloadSessions(activeDeload.id)
                6 - count
            } else {
                0
            }

            getSessionPreviewUseCase(moduleVersionId).collect { exercises ->
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        isDeloadActive = isDeload,
                        deloadSessionsRemaining = deloadSessionsRemaining,
                        exercises = exercises.map { exercise ->
                            val loadText = LoadDisplayMapper.mapLoadDisplay(
                                isDeload = isDeload,
                                isIsometric = exercise.isIsometric,
                                isBodyweight = exercise.isBodyweight,
                                prescribedLoadKg = exercise.prescribedLoadKg,
                                loadIncrementKg = exercise.loadIncrementKg,
                            )
                            val (repsText, isSpecial) = RepsDisplayMapper.mapRepsToDisplay(
                                exercise.reps,
                            )
                            PreviewExerciseItem(
                                exerciseId = exercise.exerciseId,
                                name = exercise.exerciseName,
                                equipmentTypeName = exercise.equipmentTypeName,
                                muscleZones = exercise.muscleZones,
                                setsDisplay = "${exercise.sets} series",
                                repsDisplay = repsText,
                                isRepsSpecial = isSpecial,
                                loadDisplayText = loadText,
                                isBodyweight = exercise.isBodyweight,
                                showOutOfGymBadge = exercise.isBodyweight &&
                                    exercise.moduleCode == "A",
                            )
                        },
                    )
                }
            }
        }
    }

    fun startSession() {
        viewModelScope.launch {
            try {
                val sessionId = startSessionUseCase(moduleVersionId)
                _navigateToActiveSession.emit(sessionId)
            } catch (_: Exception) {
                // Session creation failed — no action needed
            }
        }
    }
}
