package com.estebancoloradogonzalez.tension.ui.preview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.model.DeloadState
import com.estebancoloradogonzalez.tension.domain.usecase.deload.GetDeloadStateUseCase
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

@HiltViewModel
class SessionPreviewViewModel @Inject constructor(
    private val getSessionPreviewUseCase: GetSessionPreviewUseCase,
    private val startSessionUseCase: StartSessionUseCase,
    private val getDeloadStateUseCase: GetDeloadStateUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val routineVersionId: Long = checkNotNull(savedStateHandle["routineVersionId"])
    private val routineName: String = URLDecoder.decode(
        checkNotNull(savedStateHandle["routineName"]), "UTF-8",
    )
    private val versionNumber: Int = checkNotNull(savedStateHandle["versionNumber"])

    private val _uiState = MutableStateFlow(
        SessionPreviewUiState(
            routineName = routineName,
            versionNumber = versionNumber,
            routineVersionId = routineVersionId,
        ),
    )
    val uiState: StateFlow<SessionPreviewUiState> = _uiState.asStateFlow()

    private val _navigateToActiveSession = MutableSharedFlow<Long>(replay = 0)
    val navigateToActiveSession: SharedFlow<Long> = _navigateToActiveSession.asSharedFlow()

    init {
        viewModelScope.launch {
            val deloadState = getDeloadStateUseCase()

            val isDeload = deloadState is DeloadState.DeloadActive
            val deloadSessionsRemaining = if (deloadState is DeloadState.DeloadActive) {
                deloadState.totalSessions - deloadState.progress
            } else {
                0
            }

            getSessionPreviewUseCase(routineVersionId).collect { exercises ->
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
                                muscleGroup = exercise.muscleGroup,
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
                                showOutOfGymBadge = exercise.isBodyweight,
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
                val sessionId = startSessionUseCase(routineVersionId)
                _navigateToActiveSession.emit(sessionId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
