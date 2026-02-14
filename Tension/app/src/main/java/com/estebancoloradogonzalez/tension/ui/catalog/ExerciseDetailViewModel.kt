package com.estebancoloradogonzalez.tension.ui.catalog

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.data.local.storage.ImageStorageHelper
import com.estebancoloradogonzalez.tension.domain.usecase.catalog.GetExerciseDetailUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.catalog.UpdateExerciseImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getExerciseDetailUseCase: GetExerciseDetailUseCase,
    private val updateExerciseImageUseCase: UpdateExerciseImageUseCase,
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
            getExerciseDetailUseCase(exerciseId).collect { exercise ->
                _uiState.value = if (exercise != null) {
                    ExerciseDetailUiState.Success(
                        exercise = ExerciseDetailItem(
                            id = exercise.id,
                            name = exercise.name,
                            moduleCode = exercise.moduleCode,
                            moduleName = exercise.moduleName,
                            equipmentTypeName = exercise.equipmentTypeName,
                            muscleZones = exercise.muscleZones.joinToString(", "),
                            isCustom = exercise.isCustom,
                            mediaResource = exercise.mediaResource,
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
}
