package com.estebancoloradogonzalez.tension.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.model.ExerciseHistoryData
import com.estebancoloradogonzalez.tension.domain.usecase.history.GetExerciseHistoryUseCase
import com.estebancoloradogonzalez.tension.ui.components.TrendPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExerciseHistoryViewModel @Inject constructor(
    private val getExerciseHistoryUseCase: GetExerciseHistoryUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val exerciseId: Long = savedStateHandle.get<Long>("exerciseId")
        ?: throw IllegalArgumentException("exerciseId is required")

    private val _uiState = MutableStateFlow<ExerciseHistoryUiState>(ExerciseHistoryUiState.Loading)
    val uiState: StateFlow<ExerciseHistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val historyData = getExerciseHistoryUseCase(exerciseId)
            if (historyData.entries.isEmpty()) {
                _uiState.value = ExerciseHistoryUiState.Empty
            } else {
                val (trendPoints, yAxisLabel) = buildTrendData(historyData)
                _uiState.value = ExerciseHistoryUiState.Loaded(
                    data = historyData,
                    trendPoints = trendPoints,
                    yAxisLabel = yAxisLabel,
                )
            }
        }
    }

    private fun buildTrendData(data: ExerciseHistoryData): Pair<List<TrendPoint>, String> {
        // Entries are DESC (most recent first), reverse for chronological chart (oldest first)
        val chronological = data.entries.reversed()

        return when {
            data.isIsometric -> {
                val points = chronological.mapIndexed { i, entry ->
                    TrendPoint(label = "S${i + 1}", value = entry.totalReps.toFloat())
                }
                points to "s"
            }
            data.isBodyweight -> {
                val points = chronological.mapIndexed { i, entry ->
                    TrendPoint(label = "S${i + 1}", value = entry.totalReps.toFloat())
                }
                points to "reps"
            }
            else -> {
                val points = chronological.mapIndexed { i, entry ->
                    TrendPoint(label = "S${i + 1}", value = entry.avgWeightKg.toFloat())
                }
                points to "Kg"
            }
        }
    }
}
