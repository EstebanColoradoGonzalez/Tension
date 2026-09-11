package com.estebancoloradogonzalez.tension.ui.catalog

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.data.local.storage.ImageStorageHelper
import com.estebancoloradogonzalez.tension.domain.model.ProgressionDifficulty
import com.estebancoloradogonzalez.tension.domain.usecase.catalog.CreateExerciseUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.catalog.GetAllFilterOptionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateExerciseViewModel @Inject constructor(
    private val getAllFilterOptionsUseCase: GetAllFilterOptionsUseCase,
    private val createExerciseUseCase: CreateExerciseUseCase,
    private val imageStorageHelper: ImageStorageHelper,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateExerciseUiState())
    val uiState: StateFlow<CreateExerciseUiState> = _uiState.asStateFlow()

    init {
        loadOptions()
    }

    private fun loadOptions() {
        viewModelScope.launch {
            val options = getAllFilterOptionsUseCase().first()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    equipmentTypes = options.equipmentTypes,
                    muscleZones = options.muscleZones,
                )
            }
        }
    }

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(name = name, nameError = null) }
    }

    fun onEquipmentTypeToggled(id: Long) {
        _uiState.update { state ->
            val newSet = state.selectedEquipmentTypeIds.toMutableSet()
            if (newSet.contains(id)) newSet.remove(id) else newSet.add(id)
            state.copy(selectedEquipmentTypeIds = newSet, equipmentError = null)
        }
    }

    fun onPrimaryMuscleZoneAdded(id: Long) {
        _uiState.update { state ->
            if (id in state.primaryMuscleZoneIds) return@update state
            state.copy(
                primaryMuscleZoneIds = state.primaryMuscleZoneIds + id,
                // Una zona no puede estar en los dos campos (CA-41.09). El diálogo ya no
                // la deja elegir, así que esto es la red, no la puerta.
                secondaryMuscleZoneIds = state.secondaryMuscleZoneIds - id,
                muscleZoneError = null,
            )
        }
    }

    fun onPrimaryMuscleZoneRemoved(id: Long) {
        _uiState.update { state ->
            state.copy(primaryMuscleZoneIds = state.primaryMuscleZoneIds - id)
        }
    }

    fun onSecondaryMuscleZoneAdded(id: Long) {
        _uiState.update { state ->
            if (id in state.secondaryMuscleZoneIds || id in state.primaryMuscleZoneIds) {
                return@update state
            }
            state.copy(secondaryMuscleZoneIds = state.secondaryMuscleZoneIds + id)
        }
    }

    fun onSecondaryMuscleZoneRemoved(id: Long) {
        _uiState.update { state ->
            state.copy(secondaryMuscleZoneIds = state.secondaryMuscleZoneIds - id)
        }
    }

    fun onBodyweightChanged(value: Boolean) {
        _uiState.update { it.copy(isBodyweight = value) }
    }

    fun onIsometricChanged(value: Boolean) {
        _uiState.update { it.copy(isIsometric = value) }
    }

    fun onToTechnicalFailureChanged(value: Boolean) {
        _uiState.update { it.copy(isToTechnicalFailure = value) }
    }

    fun onProgressionDifficultyChanged(difficulty: ProgressionDifficulty) {
        _uiState.update { it.copy(progressionDifficulty = difficulty) }
    }

    fun onImageSelected(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            val oldPath = _uiState.value.imageUri
            val savedPath = imageStorageHelper.saveImageToInternal(uri)
            if (savedPath != null) {
                imageStorageHelper.deleteImageIfInternal(oldPath)
                _uiState.update { it.copy(imageUri = savedPath) }
            }
        }
    }

    fun onSave() {
        val state = _uiState.value
        var hasError = false

        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = "El nombre es obligatorio") }
            hasError = true
        }
        if (state.selectedEquipmentTypeIds.isEmpty()) {
            _uiState.update {
                it.copy(equipmentError = context.getString(R.string.exercise_equipment_error_empty))
            }
            hasError = true
        }
        if (state.primaryMuscleZoneIds.isEmpty()) {
            _uiState.update {
                it.copy(muscleZoneError = context.getString(R.string.muscle_zone_error_no_primary))
            }
            hasError = true
        }
        if (hasError) return

        _uiState.update { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            try {
                createExerciseUseCase(
                    name = state.name,
                    equipmentTypeIds = state.selectedEquipmentTypeIds.toList(),
                    primaryMuscleZoneIds = state.primaryMuscleZoneIds,
                    secondaryMuscleZoneIds = state.secondaryMuscleZoneIds,
                    isBodyweight = state.isBodyweight,
                    isIsometric = state.isIsometric,
                    isToTechnicalFailure = state.isToTechnicalFailure,
                    mediaResource = state.imageUri,
                    progressionDifficulty = state.progressionDifficulty,
                )
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: IllegalArgumentException) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveError = e.message ?: "Error al crear el ejercicio",
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveError = "Error al crear el ejercicio",
                    )
                }
            }
        }
    }

    fun onDismissSaveError() {
        _uiState.update { it.copy(saveError = null) }
    }
}
