package com.estebancoloradogonzalez.tension.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.data.local.dao.RoutineVersionDao
import com.estebancoloradogonzalez.tension.domain.model.WeekDay
import com.estebancoloradogonzalez.tension.domain.usecase.plan.GetWeekDayPlanUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.plan.UpdateRoutineWeekDaysUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.routine.CreateRoutineUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.routine.DeleteRoutineUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.routine.ReorderRoutinesUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.routine.UpdateRoutineNameUseCase
import com.estebancoloradogonzalez.tension.domain.repository.RoutineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoutineListViewModel @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val routineVersionDao: RoutineVersionDao,
    private val createRoutineUseCase: CreateRoutineUseCase,
    private val updateRoutineNameUseCase: UpdateRoutineNameUseCase,
    private val deleteRoutineUseCase: DeleteRoutineUseCase,
    private val reorderRoutinesUseCase: ReorderRoutinesUseCase,
    private val getWeekDayPlanUseCase: GetWeekDayPlanUseCase,
    private val updateRoutineWeekDaysUseCase: UpdateRoutineWeekDaysUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutineListUiState())
    val uiState: StateFlow<RoutineListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                routineRepository.getRoutines(),
                routineVersionDao.getAllWithExerciseCount(),
                getWeekDayPlanUseCase(),
            ) { routines, versionCounts, weekDayPlan ->
                val countMap = versionCounts.groupBy { it.routineId }
                    .mapValues { (_, versions) -> versions.size }
                val daysByRoutineId = weekDayPlan
                    .mapNotNull { day -> day.routineId?.let { it to day.weekDay } }
                    .groupBy({ it.first }, { it.second })
                val items = routines.map { routine ->
                    RoutineItem(
                        id = routine.id,
                        name = routine.name,
                        sortOrder = routine.sortOrder,
                        versionCount = countMap[routine.id] ?: 0,
                        weekDays = daysByRoutineId[routine.id].orEmpty(),
                    )
                }
                items to weekDayPlan.filter { it.isRestDay }.map { it.weekDay }
            }.collect { (items, restDays) ->
                _uiState.update { state ->
                    state.copy(isLoading = false, routines = items, restDays = restDays)
                }
            }
        }
    }

    fun onShowCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true, createDialogName = "", errorMessage = null) }
    }

    fun onDismissCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false, createDialogName = "", errorMessage = null) }
    }

    fun onCreateDialogNameChanged(name: String) {
        _uiState.update { it.copy(createDialogName = name, errorMessage = null) }
    }

    fun onConfirmCreate() {
        viewModelScope.launch {
            try {
                createRoutineUseCase(_uiState.value.createDialogName)
                _uiState.update { it.copy(showCreateDialog = false, createDialogName = "") }
            } catch (e: IllegalArgumentException) {
                _uiState.update { it.copy(errorMessage = e.message) }
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al crear la rutina") }
            }
        }
    }

    fun onShowEditDialog(routine: RoutineItem) {
        _uiState.update {
            it.copy(
                showEditDialog = true,
                editTarget = routine,
                editDialogName = routine.name,
                errorMessage = null,
            )
        }
    }

    fun onDismissEditDialog() {
        _uiState.update {
            it.copy(showEditDialog = false, editTarget = null, editDialogName = "", errorMessage = null)
        }
    }

    fun onEditDialogNameChanged(name: String) {
        _uiState.update { it.copy(editDialogName = name, errorMessage = null) }
    }

    fun onConfirmEdit() {
        val target = _uiState.value.editTarget ?: return
        viewModelScope.launch {
            try {
                updateRoutineNameUseCase(target.id, _uiState.value.editDialogName)
                _uiState.update {
                    it.copy(showEditDialog = false, editTarget = null, editDialogName = "")
                }
            } catch (e: IllegalArgumentException) {
                _uiState.update { it.copy(errorMessage = e.message) }
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al renombrar la rutina") }
            }
        }
    }

    fun onShowDeleteDialog(routine: RoutineItem) {
        _uiState.update { it.copy(deleteTarget = routine, errorMessage = null) }
    }

    fun onDismissDeleteDialog() {
        _uiState.update { it.copy(deleteTarget = null, errorMessage = null) }
    }

    fun onConfirmDelete() {
        val target = _uiState.value.deleteTarget ?: return
        viewModelScope.launch {
            try {
                deleteRoutineUseCase(target.id)
                _uiState.update { it.copy(deleteTarget = null) }
            } catch (e: IllegalArgumentException) {
                _uiState.update { it.copy(deleteTarget = null, errorMessage = e.message) }
            }
        }
    }

    fun onMoveRoutine(fromIndex: Int, toIndex: Int) {
        val previousList = _uiState.value.routines
        val currentList = previousList.toMutableList()
        if (fromIndex !in currentList.indices || toIndex !in currentList.indices) return
        val item = currentList.removeAt(fromIndex)
        currentList.add(toIndex, item)
        _uiState.update { it.copy(routines = currentList) }
        viewModelScope.launch {
            try {
                reorderRoutinesUseCase(currentList.map { it.id })
            } catch (e: IllegalArgumentException) {
                _uiState.update { it.copy(routines = previousList, errorMessage = e.message) }
            }
        }
    }

    fun onShowWeekDaysDialog(routine: RoutineItem) {
        _uiState.update {
            it.copy(
                weekDaysTarget = routine,
                weekDaysSelection = routine.weekDays.toSet(),
                errorMessage = null,
            )
        }
    }

    fun onDismissWeekDaysDialog() {
        _uiState.update {
            it.copy(weekDaysTarget = null, weekDaysSelection = emptySet(), errorMessage = null)
        }
    }

    fun onToggleWeekDay(weekDay: WeekDay) {
        _uiState.update { state ->
            val selection = state.weekDaysSelection
            state.copy(
                weekDaysSelection = if (weekDay in selection) {
                    selection - weekDay
                } else {
                    selection + weekDay
                },
            )
        }
    }

    fun onConfirmWeekDays() {
        val target = _uiState.value.weekDaysTarget ?: return
        val selection = _uiState.value.weekDaysSelection
        viewModelScope.launch {
            try {
                updateRoutineWeekDaysUseCase(target.id, selection)
                _uiState.update { it.copy(weekDaysTarget = null, weekDaysSelection = emptySet()) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        weekDaysTarget = null,
                        weekDaysSelection = emptySet(),
                        errorMessage = e.message,
                    )
                }
            }
        }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
