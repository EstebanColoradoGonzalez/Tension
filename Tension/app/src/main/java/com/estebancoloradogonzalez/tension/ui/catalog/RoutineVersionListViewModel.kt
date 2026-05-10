package com.estebancoloradogonzalez.tension.ui.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.usecase.routine.CreateRoutineVersionUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.routine.DeleteRoutineVersionUseCase
import com.estebancoloradogonzalez.tension.domain.repository.RoutineRepository
import com.estebancoloradogonzalez.tension.data.local.dao.RoutineDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoutineVersionListViewModel @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val routineDao: RoutineDao,
    private val createRoutineVersionUseCase: CreateRoutineVersionUseCase,
    private val deleteRoutineVersionUseCase: DeleteRoutineVersionUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val routineId: Long = checkNotNull(savedStateHandle["routineId"])

    private val _uiState = MutableStateFlow(RoutineVersionListUiState())
    val uiState: StateFlow<RoutineVersionListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val routine = routineDao.getById(routineId)
            _uiState.update { it.copy(routineName = routine?.name ?: "") }

            routineRepository.getVersionsByRoutine(routineId).collect { versions ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        versions = versions.map { v ->
                            RoutineVersionItem(
                                id = v.id,
                                routineId = v.routineId,
                                versionNumber = v.versionNumber,
                                exerciseCount = v.exerciseCount,
                            )
                        },
                    )
                }
            }
        }
    }

    fun onCreateVersion() {
        viewModelScope.launch {
            try {
                createRoutineVersionUseCase(routineId)
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al crear la versión") }
            }
        }
    }

    fun onShowDeleteDialog(version: RoutineVersionItem) {
        _uiState.update { it.copy(deleteTarget = version, errorMessage = null) }
    }

    fun onDismissDeleteDialog() {
        _uiState.update { it.copy(deleteTarget = null, errorMessage = null) }
    }

    fun onConfirmDelete() {
        val target = _uiState.value.deleteTarget ?: return
        viewModelScope.launch {
            try {
                deleteRoutineVersionUseCase(target.id, target.routineId)
                _uiState.update { it.copy(deleteTarget = null) }
            } catch (e: IllegalArgumentException) {
                _uiState.update { it.copy(deleteTarget = null, errorMessage = e.message) }
            }
        }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
