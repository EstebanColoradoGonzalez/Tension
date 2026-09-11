package com.estebancoloradogonzalez.tension.ui.catalog

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.repository.PlanRepository
import com.estebancoloradogonzalez.tension.domain.usecase.plan.AddAlternativeToSlotUseCase
import com.estebancoloradogonzalez.tension.domain.util.RepsDisplayMapper
import com.estebancoloradogonzalez.tension.domain.usecase.plan.AssignExerciseToVersionUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.catalog.GetExerciseEquipmentOptionsUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.plan.GetPlanVersionDetailUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.plan.SetSuggestedEquipmentUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.plan.UnassignExerciseFromVersionUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.plan.UpdatePlanAssignmentUseCase
import com.estebancoloradogonzalez.tension.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val setSuggestedEquipmentUseCase: SetSuggestedEquipmentUseCase,
    private val getExerciseEquipmentOptionsUseCase: GetExerciseEquipmentOptionsUseCase,
    private val planRepository: PlanRepository,
    @ApplicationContext private val context: Context,
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
                        equipmentSummary = pe.equipmentTypes.joinToString(" · "),
                        suggestedEquipmentName = pe.suggestedEquipmentName,
                        suggestedEquipmentTypeId = pe.suggestedEquipmentTypeId,
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
                            // Cada alternativa lleva su propia sugerencia: comparten
                            // puesto y prescripción, no implemento (CA-41.05).
                            alternatives = slotItems.drop(1).map { alt ->
                                PlanAlternativeItem(
                                    exerciseId = alt.exerciseId,
                                    name = alt.name,
                                    suggestedEquipmentName = alt.suggestedEquipmentName,
                                    suggestedEquipmentTypeId = alt.suggestedEquipmentTypeId,
                                )
                            },
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
                            equipmentSummary = e.equipmentTypes.joinToString(" · "),
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
        _sheetState.update { state ->
            val options = state.availableExercises
                .firstOrNull { it.id == exerciseId }?.equipmentOptions.orEmpty()
            // Se propone la primera opción del ejercicio, que es la regla con la que se
            // resolvieron las 35 del seed. Sigue siendo editable, y obligatoria: sin ella
            // el botón no se habilita (CA-41.08).
            state.copy(
                selectedExerciseId = exerciseId,
                selectedSuggestedEquipmentId = options.firstOrNull()?.id,
            )
        }
    }

    fun onSuggestedEquipmentSelected(equipmentTypeId: Long) {
        _sheetState.update { it.copy(selectedSuggestedEquipmentId = equipmentTypeId) }
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
        val suggestedEquipmentId = state.selectedSuggestedEquipmentId ?: return
        val sets = state.sets.toIntOrNull() ?: return
        if (sets <= 0) return

        _sheetState.update { it.copy(isAssigning = true) }
        viewModelScope.launch {
            try {
                assignExerciseToVersionUseCase(
                    routineVersionId = routineVersionId,
                    exerciseId = exerciseId,
                    sets = sets,
                    reps = state.reps,
                    suggestedEquipmentTypeId = suggestedEquipmentId,
                )
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
        openEditDialog(
            exerciseId = exercise.exerciseId,
            exerciseName = exercise.name,
            sets = exercise.sets,
            reps = exercise.repsRaw,
            suggestedEquipmentTypeId = exercise.suggestedEquipmentTypeId,
        )
    }

    /**
     * Editar la alternativa de un puesto dual entra por aquí y no por [onEditExercise]:
     * comparte series y repeticiones con el primario —y el caso de uso las propaga al
     * slot— pero su implemento sugerido es suyo (CA-41.05).
     */
    fun onEditAlternative(primary: PlanExerciseItem, alternative: PlanAlternativeItem) {
        openEditDialog(
            exerciseId = alternative.exerciseId,
            exerciseName = alternative.name,
            sets = primary.sets,
            reps = primary.repsRaw,
            suggestedEquipmentTypeId = alternative.suggestedEquipmentTypeId,
        )
    }

    private fun openEditDialog(
        exerciseId: Long,
        exerciseName: String,
        sets: Int,
        reps: String,
        suggestedEquipmentTypeId: Long,
    ) {
        viewModelScope.launch {
            // Las opciones se piden al abrir y no se guardan en el item de la lista: el
            // ejercicio pudo ganar o perder implementos desde que la pantalla se pintó, y
            // el selector debe ofrecer los de ahora (CA-41.08).
            val options = getExerciseEquipmentOptionsUseCase(exerciseId).first()
            _editState.value = EditPlanAssignmentState(
                isVisible = true,
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                sets = sets,
                reps = reps,
                suggestedEquipmentTypeId = suggestedEquipmentTypeId,
                equipmentOptions = options,
            )
        }
    }

    fun onEditSuggestedEquipmentSelected(equipmentTypeId: Long) {
        _editState.update { it.copy(suggestedEquipmentTypeId = equipmentTypeId) }
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
                // La sugerencia se escribe aparte porque no se propaga al slot, a
                // diferencia de series y repeticiones.
                //
                // Su rechazo se traduce aquí y no se deja escapar: los `require` del
                // dominio llevan mensaje en inglés y de uso interno, y dejarlos llegar a
                // la pantalla mostraría al ejecutante un texto que no es para él.
                try {
                    setSuggestedEquipmentUseCase(
                        routineVersionId = routineVersionId,
                        exerciseId = state.exerciseId,
                        equipmentTypeId = state.suggestedEquipmentTypeId,
                    )
                } catch (_: IllegalArgumentException) {
                    _userMessage.value = context.getString(
                        R.string.plan_suggested_equipment_not_admitted_format,
                        state.equipmentOptions
                            .firstOrNull { it.id == state.suggestedEquipmentTypeId }?.name.orEmpty(),
                        state.exerciseName,
                    )
                }
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
                        equipmentSummary = e.equipmentTypes.joinToString(" · "),
                        muscleZonesSummary = e.muscleZones.joinToString(", "),
                        equipmentOptions = e.equipmentOptions,
                    )
                },
            )
        }
    }

    fun onAlternativeExerciseSelected(exerciseId: Long) {
        _addAlternativeState.update { state ->
            val options = state.availableExercises
                .firstOrNull { it.id == exerciseId }?.equipmentOptions.orEmpty()
            state.copy(
                selectedExerciseId = exerciseId,
                selectedSuggestedEquipmentId = options.firstOrNull()?.id,
            )
        }
    }

    fun onAlternativeSuggestedEquipmentSelected(equipmentTypeId: Long) {
        _addAlternativeState.update { it.copy(selectedSuggestedEquipmentId = equipmentTypeId) }
    }

    fun onConfirmAddAlternative() {
        val state = _addAlternativeState.value
        if (state.isAssigning) return
        val exerciseId = state.selectedExerciseId ?: return
        val suggestedEquipmentId = state.selectedSuggestedEquipmentId ?: return
        _addAlternativeState.update { it.copy(isAssigning = true) }
        viewModelScope.launch {
            try {
                addAlternativeToSlotUseCase(
                    routineVersionId = routineVersionId,
                    slot = state.slot,
                    exerciseId = exerciseId,
                    suggestedEquipmentTypeId = suggestedEquipmentId,
                )
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
