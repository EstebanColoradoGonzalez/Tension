package com.estebancoloradogonzalez.tension.ui.session

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.domain.usecase.session.GetRegisterSetInfoUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.RegisterSetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterSetViewModel @Inject constructor(
    private val getRegisterSetInfoUseCase: GetRegisterSetInfoUseCase,
    private val registerSetUseCase: RegisterSetUseCase,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val sessionExerciseId: Long = savedStateHandle.get<Long>("sessionExerciseId") ?: 0L

    private val _uiState = MutableStateFlow(RegisterSetUiState())
    val uiState: StateFlow<RegisterSetUiState> = _uiState.asStateFlow()

    private val _navigateBack = MutableSharedFlow<Boolean>(replay = 0)
    val navigateBack: SharedFlow<Boolean> = _navigateBack.asSharedFlow()

    init {
        viewModelScope.launch {
            val info = getRegisterSetInfoUseCase(sessionExerciseId) ?: return@launch

            val weightKg = when {
                info.isBodyweight || info.isIsometric -> "0"
                info.lastWeightKg != null -> String.format(java.util.Locale.US, "%.1f", info.lastWeightKg)
                else -> ""
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    exerciseName = info.exerciseName,
                    currentSetNumber = info.currentSetNumber,
                    totalSets = info.totalSets,
                    weightKg = weightKg,
                    isWeightEditable = !info.isBodyweight && !info.isIsometric,
                    isIsometric = info.isIsometric,
                    isBodyweight = info.isBodyweight,
                )
            }
        }
    }

    fun onWeightChanged(value: String) {
        val error = value.toDoubleOrNull()?.let { parsed ->
            if (parsed < 0) context.getString(R.string.error_weight_negative) else null
        }
        _uiState.update { it.copy(weightKg = value, weightError = error) }
    }

    fun onRepsChanged(value: String) {
        val error = value.toIntOrNull()?.let { parsed ->
            if (parsed < 1) {
                if (_uiState.value.isIsometric) {
                    context.getString(R.string.error_seconds_min)
                } else {
                    context.getString(R.string.error_reps_min)
                }
            } else {
                null
            }
        }
        _uiState.update { it.copy(reps = value, repsError = error) }
    }

    fun onRirSelected(rir: Int) {
        _uiState.update { it.copy(selectedRir = rir) }
    }

    fun onConfirm() {
        val state = _uiState.value
        val weight = state.weightKg.toDoubleOrNull()
        val parsedReps = state.reps.toIntOrNull()

        if (weight == null) {
            _uiState.update {
                it.copy(weightError = context.getString(R.string.error_weight_negative))
            }
            return
        }
        if (parsedReps == null) {
            _uiState.update {
                it.copy(
                    repsError = if (state.isIsometric) {
                        context.getString(R.string.error_seconds_min)
                    } else {
                        context.getString(R.string.error_reps_min)
                    },
                )
            }
            return
        }
        if (state.selectedRir == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                registerSetUseCase(sessionExerciseId, weight, parsedReps, state.selectedRir)
                _navigateBack.emit(true)
            } catch (_: IllegalArgumentException) {
                _uiState.update {
                    it.copy(
                        weightError = if (weight < 0) {
                            context.getString(R.string.error_weight_negative)
                        } else {
                            null
                        },
                        repsError = if (parsedReps < 1) {
                            if (state.isIsometric) {
                                context.getString(R.string.error_seconds_min)
                            } else {
                                context.getString(R.string.error_reps_min)
                            }
                        } else {
                            null
                        },
                    )
                }
            } catch (_: IllegalStateException) {
                _navigateBack.emit(true)
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
