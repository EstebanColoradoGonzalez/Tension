package com.estebancoloradogonzalez.tension.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.model.Exercise
import com.estebancoloradogonzalez.tension.domain.usecase.catalog.GetExercisesUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.catalog.GetFilterOptionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExerciseDictionaryViewModel @Inject constructor(
    private val getExercisesUseCase: GetExercisesUseCase,
    private val getFilterOptionsUseCase: GetFilterOptionsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseDictionaryUiState())
    val uiState: StateFlow<ExerciseDictionaryUiState> = _uiState.asStateFlow()

    private val _selectedModule = MutableStateFlow<String?>(null)
    private val _selectedEquipment = MutableStateFlow<String?>(null)
    private val _selectedMuscleZone = MutableStateFlow<String?>(null)

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                getExercisesUseCase(),
                getFilterOptionsUseCase(),
                _selectedModule,
                _selectedEquipment,
                _selectedMuscleZone,
            ) { exercises, filterOptions, selectedModule, selectedEquipment, selectedMuscleZone ->
                val filtered = exercises.filter { exercise ->
                    val matchesModule = selectedModule == null || exercise.moduleCode == selectedModule
                    val matchesEquipment = selectedEquipment == null || exercise.equipmentTypeName == selectedEquipment
                    val matchesMuscleZone = selectedMuscleZone == null || exercise.muscleZones.any { it == selectedMuscleZone }
                    matchesModule && matchesEquipment && matchesMuscleZone
                }

                ExerciseDictionaryUiState(
                    isLoading = false,
                    exercises = filtered.map { it.toExerciseItem() },
                    totalCount = exercises.size,
                    moduleOptions = filterOptions.modules.map { it.code },
                    equipmentOptions = filterOptions.equipmentTypes.map { it.name },
                    muscleZoneOptions = filterOptions.muscleZones.map { it.name },
                    selectedModule = selectedModule,
                    selectedEquipment = selectedEquipment,
                    selectedMuscleZone = selectedMuscleZone,
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onModuleFilterSelected(code: String?) {
        _selectedModule.value = code
    }

    fun onEquipmentFilterSelected(name: String?) {
        _selectedEquipment.value = name
    }

    fun onMuscleZoneFilterSelected(name: String?) {
        _selectedMuscleZone.value = name
    }

    private fun Exercise.toExerciseItem() = ExerciseItem(
        id = id,
        name = name,
        moduleCode = moduleCode,
        equipmentTypeName = equipmentTypeName,
        muscleZonesSummary = muscleZones.joinToString(", "),
        isCustom = isCustom,
    )
}
