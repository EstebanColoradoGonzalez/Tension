package com.estebancoloradogonzalez.tension.ui.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.repository.PlanRepository
import com.estebancoloradogonzalez.tension.domain.usecase.plan.AddAlternativeToSlotUseCase
import com.estebancoloradogonzalez.tension.domain.util.RepsDisplayMapper
import com.estebancoloradogonzalez.tension.domain.usecase.plan.AssignExerciseToVersionUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.plan.GetPlanVersionDetailUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.plan.UnassignExerciseFromVersionUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.plan.UpdatePlanAssignmentUseCase
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
    private val addAlternativeToSlotUseCase: AddAlternativeToSlotUseCase,
    private val unassignExerciseFromVersionUseCase: UnassignExerciseFromVersionUseCase,
    private val updatePlanAssignmentUseCase: UpdatePlanAssignmentUseCase,
    private val planRepository: PlanRepository,
) : ViewModel() {

    private val routineVersionId: Long = checkNotNull(savedStateHandle["routineVersionId"])

    val uiState: StateFlow<PlanVersionDetailUiState> =
        getPlanVersionDetailUseCase(routineVersionId).map { detail ->
            if (detail == null) {
                PlanVersionDetailUiState(isLoading = false)
            } else {
                // Build a map of all items per slot first
                val allItems = detail.exercises.map { pe ->
                    val (repsDisplay, isSpecial) = RepsDisplayMapper.mapRepsToDisplay(pe.reps)
                    PlanExerciseItem(
                        exerciseId = pe.exerciseId,
                        name = pe.name,
                        equipmentTypeName = pe.equipmentTypeName,
                        muscleZonesSummary = pe.muscleZones.joinToString(", "),
                        sets = pe.sets,
                        repsDisplay = repsDisplay,
                        repsRaw = pe.reps,
                        isSpecialCondition = isSpecial,
                        isCustom = pe.isCustom,
                        isBodyweight = pe.isBodyweight,
                        slot = pe.slot,
                    )
                }
                // Group by slot and emit ONE item per slot (the first by sort_order)
                // with alternativeNames populated from remaining exercises in same slot
                val bySlot = allItems.groupBy { it.slot }
                val exercisesOnePerSlot = bySlot.entries
                    .sortedBy { it.key }
                    .map { (_, slotItems) ->
                        val primary = slotItems.first()
                        primary.copy(
                            alternativeNames = slotItems.drop(1).map { it.name },
                        )
                    }
                PlanVersionDetailUiState(
                    isLoading = false,
                    routineName = detail.routineName,
                    versionNumber = detail.versionNumber,
                    exercises = exercisesOnePerSlot,
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

    private val _editState = MutableStateFlow(EditPlanAssignmentState())
    val editState: StateFlow<EditPlanAssignmentState> = _editState.asStateFlow()

    private val _addAlternativeState = MutableStateFlow(AddAlternativeSheetState())
    val addAlternativeState: StateFlow<AddAlternativeSheetState> = _addAlternativeState.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun onUserMessageShown() {
        _userMessage.value = null
    }

    fun onDeleteExercise(exercise: PlanExerciseItem) {
        _deleteDialogState.value = exercise
    }

    fun onConfirmDelete() {
        val exercise = _deleteDialogState.value ?: return
        _deleteDialogState.value = null
        viewModelScope.launch {
            try {
                unassignExerciseFromVersionUseCase(routineVersionId, exercise.exerciseId)
            } catch (e: IllegalArgumentException) {
                _userMessage.value = e.message
            } catch (_: Exception) {
                // Unexpected failure — no-op, list unchanged
            }
        }
    }

    fun onDismissDeleteDialog() {
        _deleteDialogState.value = null
    }

    fun onFabClick() {
        viewModelScope.launch {
            val exercises = planRepository
                .getAvailableExercisesForVersion(routineVersionId)
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
        if (state.isAssigning) return
        val exerciseId = state.selectedExerciseId ?: return
        val sets = state.sets.toIntOrNull() ?: return
        if (sets <= 0) return

        _sheetState.update { it.copy(isAssigning = true) }
        viewModelScope.launch {
            try {
                assignExerciseToVersionUseCase(routineVersionId, exerciseId, sets, state.reps)
            } catch (e: IllegalArgumentException) {
                _userMessage.value = e.message
            } catch (_: Exception) {
                // Unexpected failure — no-op
            } finally {
                _sheetState.value = AssignExerciseSheetState()
            }
        }
    }

    fun onDismissSheet() {
        _sheetState.value = AssignExerciseSheetState()
    }

    fun onEditExercise(exercise: PlanExerciseItem) {
        _editState.value = EditPlanAssignmentState(
            isVisible = true,
            exerciseId = exercise.exerciseId,
            exerciseName = exercise.name,
            sets = exercise.sets,
            reps = exercise.repsRaw,
        )
    }

    fun onEditSetsChanged(sets: Int) {
        _editState.update { it.copy(sets = sets.coerceIn(1, 10)) }
    }

    fun onEditRepsSelected(reps: String) {
        _editState.update { it.copy(reps = reps) }
    }

    fun onConfirmEdit() {
        val state = _editState.value
        if (state.isSaving) return
        _editState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                updatePlanAssignmentUseCase(
                    routineVersionId,
                    state.exerciseId,
                    state.sets,
                    state.reps,
                )
            } catch (e: IllegalArgumentException) {
                _userMessage.value = e.message
            } catch (_: Exception) {
                // Edit failed — dialog closes, list unchanged
            } finally {
                _editState.value = EditPlanAssignmentState()
            }
        }
    }

    fun onDismissEdit() {
        _editState.value = EditPlanAssignmentState()
    }

    fun onAddAlternativeClick(exercise: PlanExerciseItem) {
        viewModelScope.launch {
            val exercises = planRepository
                .getAvailableExercisesForVersion(routineVersionId)
                .first()
            _addAlternativeState.value = AddAlternativeSheetState(
                isVisible = true,
                slot = exercise.slot,
                slotName = exercise.name,
                availableExercises = exercises.map { e ->
                    AssignableExerciseItem(
                        id = e.id,
                        name = e.name,
                        equipmentTypeName = e.equipmentTypeName,
                        muscleZonesSummary = e.muscleZones.joinToString(", "),
                    )
                },
            )
        }
    }

    fun onAlternativeExerciseSelected(exerciseId: Long) {
        _addAlternativeState.update { it.copy(selectedExerciseId = exerciseId) }
    }

    fun onConfirmAddAlternative() {
        val state = _addAlternativeState.value
        if (state.isAssigning) return
        val exerciseId = state.selectedExerciseId ?: return
        _addAlternativeState.update { it.copy(isAssigning = true) }
        viewModelScope.launch {
            try {
                addAlternativeToSlotUseCase(routineVersionId, state.slot, exerciseId)
            } catch (e: IllegalArgumentException) {
                _userMessage.value = e.message
            } catch (_: Exception) {
                // Unexpected failure — no-op
            } finally {
                _addAlternativeState.value = AddAlternativeSheetState()
            }
        }
    }

    fun onDismissAddAlternative() {
        _addAlternativeState.value = AddAlternativeSheetState()
    }

}
