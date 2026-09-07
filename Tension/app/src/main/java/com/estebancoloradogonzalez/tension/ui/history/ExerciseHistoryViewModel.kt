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

    /** El historial completo, sin segmentar. Se carga una vez y se filtra en memoria. */
    private var fullHistory: ExerciseHistoryData? = null

    init {
        viewModelScope.launch {
            val historyData = getExerciseHistoryUseCase(exerciseId)
            fullHistory = historyData
            if (historyData.entries.isEmpty()) {
                _uiState.value = ExerciseHistoryUiState.Empty
            } else {
                _uiState.value = loadedState(
                    historyData,
                    historyData.equipmentOptions.firstOrNull(),
                )
            }
        }
    }

    /**
     * Cambia el implemento de la lectura.
     *
     * Se resuelve sobre lo ya cargado y no vuelve a consultar: la segmentación la hizo la
     * base al agrupar por implemento, así que aquí solo se elige qué subconjunto se lee.
     */
    fun onEquipmentSelected(equipmentTypeName: String) {
        val history = fullHistory ?: return
        _uiState.value = loadedState(history, equipmentTypeName)
    }

    private fun loadedState(
        history: ExerciseHistoryData,
        selectedEquipment: String?,
    ): ExerciseHistoryUiState.Loaded {
        val filtered = if (selectedEquipment == null) {
            history
        } else {
            history.copy(
                entries = history.entries.filter { it.equipmentTypeName == selectedEquipment },
            )
        }
        val (trendPoints, yAxisLabel) = buildTrendData(filtered)
        return ExerciseHistoryUiState.Loaded(
            data = filtered,
            trendPoints = trendPoints,
            yAxisLabel = yAxisLabel,
            equipmentOptions = history.equipmentOptions,
            selectedEquipment = selectedEquipment,
        )
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
