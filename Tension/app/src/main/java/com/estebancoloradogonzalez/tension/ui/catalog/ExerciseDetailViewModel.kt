package com.estebancoloradogonzalez.tension.ui.catalog

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.data.local.storage.ImageStorageHelper
import com.estebancoloradogonzalez.tension.domain.model.ProgressionDifficulty
import com.estebancoloradogonzalez.tension.domain.rules.PlateauThresholdRule
import com.estebancoloradogonzalez.tension.domain.usecase.catalog.GetExerciseDetailUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.catalog.UpdateExerciseImageUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.catalog.UpdateExerciseProgressionDifficultyUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.profile.GetProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getExerciseDetailUseCase: GetExerciseDetailUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val updateExerciseImageUseCase: UpdateExerciseImageUseCase,
    private val updateExerciseProgressionDifficultyUseCase: UpdateExerciseProgressionDifficultyUseCase,
    private val imageStorageHelper: ImageStorageHelper,
) : ViewModel() {

    private val exerciseId: Long = checkNotNull(savedStateHandle["exerciseId"])

    private val _uiState = MutableStateFlow<ExerciseDetailUiState>(ExerciseDetailUiState.Loading)
    val uiState: StateFlow<ExerciseDetailUiState> = _uiState.asStateFlow()

    init {
        loadExerciseDetail()
    }

    private fun loadExerciseDetail() {
        viewModelScope.launch {
            combine(
                getExerciseDetailUseCase(exerciseId),
                getProfileUseCase(),
            ) { exercise, profile ->
                val baseThreshold = profile?.plateauBaseThreshold
                    ?: PlateauThresholdRule.DEFAULT_BASE_THRESHOLD
                exercise to baseThreshold
            }.collect { (exercise, baseThreshold) ->
                _uiState.value = if (exercise != null) {
                    ExerciseDetailUiState.Success(
                        exercise = ExerciseDetailItem(
                            id = exercise.id,
                            name = exercise.name,
                            equipmentTypeName = exercise.equipmentTypeName,
                            muscleZones = exercise.muscleZones.joinToString(", "),
                            isCustom = exercise.isCustom,
                            mediaResource = exercise.mediaResource,
                            progressionDifficulty = exercise.progressionDifficulty,
                            effectiveThresholdSessions = PlateauThresholdRule.effectiveThreshold(
                                baseThreshold,
                                exercise.progressionDifficulty,
                            ),
                        ),
                    )
                } else {
                    ExerciseDetailUiState.Error("Ejercicio no encontrado")
                }
            }
        }
    }

    fun onImageSelected(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            val savedPath = imageStorageHelper.saveImageToInternal(uri)
            if (savedPath != null) {
                val currentState = _uiState.value
                if (currentState is ExerciseDetailUiState.Success) {
                    imageStorageHelper.deleteImageIfInternal(currentState.exercise.mediaResource)
                }
                updateExerciseImageUseCase(exerciseId, savedPath)
            }
        }
    }

    /**
     * Persists the difficulty immediately, following the same pattern as the exercise
     * image: this screen has no save button, and the Room flow re-emits the detail.
     */
    fun onProgressionDifficultySelected(difficulty: ProgressionDifficulty) {
        viewModelScope.launch {
            updateExerciseProgressionDifficultyUseCase(exerciseId, difficulty)
        }
    }
}
