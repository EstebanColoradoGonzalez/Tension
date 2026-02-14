package com.estebancoloradogonzalez.tension.ui.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.repository.PlanRepository
import com.estebancoloradogonzalez.tension.domain.usecase.plan.AssignExerciseToVersionUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.plan.GetPlanVersionDetailUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.plan.UnassignExerciseFromVersionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlanVersionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getPlanVersionDetailUseCase: GetPlanVersionDetailUseCase,
    private val assignExerciseToVersionUseCase: AssignExerciseToVersionUseCase,
    private val unassignExerciseFromVersionUseCase: UnassignExerciseFromVersionUseCase,
    private val planRepository: PlanRepository,
) : ViewModel() {

    private val moduleVersionId: Long = checkNotNull(savedStateHandle["moduleVersionId"])

    val uiState: StateFlow<PlanVersionDetailUiState> =
        getPlanVersionDetailUseCase(moduleVersionId).map { detail ->
            if (detail == null) {
                PlanVersionDetailUiState(isLoading = false)
            } else {
                PlanVersionDetailUiState(
                    isLoading = false,
                    moduleCode = detail.moduleCode,
                    moduleName = detail.moduleName,
                    versionNumber = detail.versionNumber,
                    exercises = detail.exercises.map { pe ->
                        val (repsDisplay, isSpecial) = mapRepsToDisplay(pe.reps)
                        PlanExerciseItem(
                            exerciseId = pe.exerciseId,
                            name = pe.name,
                            equipmentTypeName = pe.equipmentTypeName,
                            muscleZonesSummary = pe.muscleZones.joinToString(", "),
                            sets = pe.sets,
                            repsDisplay = repsDisplay,
                            isSpecialCondition = isSpecial,
                            isCustom = pe.isCustom,
                        )
                    },
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlanVersionDetailUiState(),
        )

    private val _sheetState = MutableStateFlow(AssignExerciseSheetState())
    val sheetState: StateFlow<AssignExerciseSheetState> = _sheetState.asStateFlow()

    private val _deleteDialogState = MutableStateFlow<PlanExerciseItem?>(null)
    val deleteDialogState: StateFlow<PlanExerciseItem?> = _deleteDialogState.asStateFlow()

    fun onDeleteExercise(exercise: PlanExerciseItem) {
        _deleteDialogState.value = exercise
    }

    fun onConfirmDelete() {
        val exercise = _deleteDialogState.value ?: return
        _deleteDialogState.value = null
        viewModelScope.launch {
            unassignExerciseFromVersionUseCase(moduleVersionId, exercise.exerciseId)
        }
    }

    fun onDismissDeleteDialog() {
        _deleteDialogState.value = null
    }

    fun onFabClick() {
        val moduleCode = uiState.value.moduleCode
        if (moduleCode.isBlank()) return
        viewModelScope.launch {
            val exercises = planRepository
                .getAvailableExercisesForVersion(moduleCode, moduleVersionId)
                .first()
            _sheetState.update {
                AssignExerciseSheetState(
                    isVisible = true,
                    availableExercises = exercises.map { e ->
                        AssignableExerciseItem(
                            id = e.id,
                            name = e.name,
                            equipmentTypeName = e.equipmentTypeName,
                            muscleZonesSummary = e.muscleZones.joinToString(", "),
                        )
                    },
                    sets = "4",
                    reps = "8-12",
                )
            }
        }
    }

    fun onExerciseSelected(exerciseId: Long) {
        _sheetState.update { it.copy(selectedExerciseId = exerciseId) }
    }

    fun onSetsChanged(sets: String) {
        _sheetState.update { it.copy(sets = sets) }
    }

    fun onRepsSelected(reps: String) {
        _sheetState.update { it.copy(reps = reps) }
    }

    fun onConfirmAssign() {
        val state = _sheetState.value
        val exerciseId = state.selectedExerciseId ?: return
        val sets = state.sets.toIntOrNull() ?: return
        if (sets <= 0) return

        _sheetState.update { it.copy(isAssigning = true) }
        viewModelScope.launch {
            try {
                assignExerciseToVersionUseCase(moduleVersionId, exerciseId, sets, state.reps)
            } finally {
                _sheetState.value = AssignExerciseSheetState()
            }
        }
    }

    fun onDismissSheet() {
        _sheetState.value = AssignExerciseSheetState()
    }

    companion object {
        fun mapRepsToDisplay(reps: String): Pair<String, Boolean> = when (reps) {
            "TO_TECHNICAL_FAILURE" -> "Al fallo técnico" to true
            "30-45_SEC" -> "30\u201345 seg" to true
            else -> "$reps reps" to false
        }
    }
}
