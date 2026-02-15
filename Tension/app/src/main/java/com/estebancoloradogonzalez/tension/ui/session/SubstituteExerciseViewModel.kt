package com.estebancoloradogonzalez.tension.ui.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.model.Exercise
import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import com.estebancoloradogonzalez.tension.domain.usecase.session.SubstituteExerciseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubstituteExerciseUiState(
    val isLoading: Boolean = true,
    val originalExerciseName: String = "",
    val eligibleExercises: List<SubstituteExerciseUiItem> = emptyList(),
    val selectedExercise: SubstituteExerciseUiItem? = null,
    val showConfirmDialog: Boolean = false,
    val isSubstituting: Boolean = false,
)

data class SubstituteExerciseUiItem(
    val exerciseId: Long,
    val name: String,
    val muscleZones: String,
    val equipmentTypeName: String,
)

@HiltViewModel
class SubstituteExerciseViewModel @Inject constructor(
    private val substituteExerciseUseCase: SubstituteExerciseUseCase,
    private val sessionRepository: SessionRepository,
    private val exerciseRepository: ExerciseRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val sessionExerciseId: Long = savedStateHandle.get<Long>("sessionExerciseId") ?: 0L

    private val _uiState = MutableStateFlow(SubstituteExerciseUiState())
    val uiState: StateFlow<SubstituteExerciseUiState> = _uiState.asStateFlow()

    private val _navigateBack = MutableSharedFlow<Boolean>(replay = 0)
    val navigateBack: SharedFlow<Boolean> = _navigateBack.asSharedFlow()

    init {
        viewModelScope.launch {
            val info = sessionRepository.getSubstituteExerciseInfo(sessionExerciseId)
            if (info == null) {
                _navigateBack.emit(true)
                return@launch
            }

            val excludedIds = sessionRepository.getExerciseIdsForSession(info.sessionId)

            exerciseRepository.getEligibleSubstitutes(info.moduleCode, excludedIds)
                .collect { exercises ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            originalExerciseName = info.currentExerciseName,
                            eligibleExercises = exercises.map { exercise ->
                                exercise.toUiItem()
                            },
                        )
                    }
                }
        }
    }

    fun onExerciseSelected(exercise: SubstituteExerciseUiItem) {
        _uiState.update { it.copy(selectedExercise = exercise, showConfirmDialog = true) }
    }

    fun onDismissDialog() {
        _uiState.update { it.copy(showConfirmDialog = false, selectedExercise = null) }
    }

    fun onConfirmSubstitution() {
        val selected = _uiState.value.selectedExercise ?: return
        if (_uiState.value.isSubstituting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubstituting = true) }
            try {
                substituteExerciseUseCase(sessionExerciseId, selected.exerciseId)
                _navigateBack.emit(true)
            } catch (_: IllegalStateException) {
                _navigateBack.emit(true)
            } finally {
                _uiState.update { it.copy(isSubstituting = false) }
            }
        }
    }

    private fun Exercise.toUiItem() = SubstituteExerciseUiItem(
        exerciseId = id,
        name = name,
        muscleZones = muscleZones.joinToString(", "),
        equipmentTypeName = equipmentTypeName,
    )
}
