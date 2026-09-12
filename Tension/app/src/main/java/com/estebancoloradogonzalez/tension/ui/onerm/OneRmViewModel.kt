package com.estebancoloradogonzalez.tension.ui.onerm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.usecase.onerm.GetOneRmListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The 1RM screen.
 *
 * There is nothing to recalculate on opening — unlike the tree, the record is written when
 * the set is registered and is already correct — so this only observes. And there are no
 * one-shot events: the screen neither navigates nor decides anything; selecting an implement
 * changes a number in place.
 */
@HiltViewModel
class OneRmViewModel @Inject constructor(
    private val getOneRmListUseCase: GetOneRmListUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OneRmUiState())
    val uiState: StateFlow<OneRmUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getOneRmListUseCase().collect { exercises ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        exercises = exercises,
                        // A selection pointing at a pair that is no longer listed is dropped
                        // rather than carried: the state falls back to the first implement,
                        // which is what an incoming card shows anyway.
                        selection = state.selection.filterKeys { exerciseId ->
                            exercises.any { it.exerciseId == exerciseId }
                        },
                    )
                }
            }
        }
    }

    fun selectEquipment(exerciseId: Long, equipmentTypeId: Long) {
        _uiState.update { it.copy(selection = it.selection + (exerciseId to equipmentTypeId)) }
    }
}
