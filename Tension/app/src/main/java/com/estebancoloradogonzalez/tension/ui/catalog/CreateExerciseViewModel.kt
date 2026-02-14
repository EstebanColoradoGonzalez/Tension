package com.estebancoloradogonzalez.tension.ui.catalog

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.data.local.storage.ImageStorageHelper
import com.estebancoloradogonzalez.tension.domain.usecase.catalog.CreateExerciseUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.catalog.GetFilterOptionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateExerciseViewModel @Inject constructor(
    private val getFilterOptionsUseCase: GetFilterOptionsUseCase,
    private val createExerciseUseCase: CreateExerciseUseCase,
    private val imageStorageHelper: ImageStorageHelper,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateExerciseUiState())
    val uiState: StateFlow<CreateExerciseUiState> = _uiState.asStateFlow()

    init {
        loadOptions()
    }

    private fun loadOptions() {
        viewModelScope.launch {
            val options = getFilterOptionsUseCase().first()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    modules = options.modules,
                    equipmentTypes = options.equipmentTypes,
                    muscleZones = options.muscleZones,
                )
            }
        }
    }

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(name = name, nameError = null) }
    }

    fun onModuleSelected(code: String) {
        _uiState.update { it.copy(selectedModuleCode = code, moduleError = null) }
    }

    fun onEquipmentTypeSelected(id: Long) {
        _uiState.update { it.copy(selectedEquipmentTypeId = id, equipmentError = null) }
    }

    fun onMuscleZoneToggled(id: Long) {
        _uiState.update { state ->
            val newSet = state.selectedMuscleZoneIds.toMutableSet()
            if (newSet.contains(id)) newSet.remove(id) else newSet.add(id)
            state.copy(selectedMuscleZoneIds = newSet, muscleZoneError = null)
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
        if (state.selectedModuleCode == null) {
            _uiState.update { it.copy(moduleError = "Selecciona un módulo") }
            hasError = true
        }
        if (state.selectedEquipmentTypeId == null) {
            _uiState.update { it.copy(equipmentError = "Selecciona un tipo de equipo") }
            hasError = true
        }
        if (state.selectedMuscleZoneIds.isEmpty()) {
            _uiState.update { it.copy(muscleZoneError = "Selecciona al menos una zona muscular") }
            hasError = true
        }
        if (hasError) return

        _uiState.update { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            try {
                createExerciseUseCase(
                    name = state.name,
                    moduleCode = state.selectedModuleCode!!,
                    equipmentTypeId = state.selectedEquipmentTypeId!!,
                    muscleZoneIds = state.selectedMuscleZoneIds.toList(),
                    isBodyweight = state.isBodyweight,
                    isIsometric = state.isIsometric,
                    isToTechnicalFailure = state.isToTechnicalFailure,
                    mediaResource = state.imageUri,
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
