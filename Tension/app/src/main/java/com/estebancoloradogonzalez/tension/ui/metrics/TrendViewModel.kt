package com.estebancoloradogonzalez.tension.ui.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.usecase.metrics.GetMicrocycleMapUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.metrics.GetMuscleGroupTrendUseCase
import com.estebancoloradogonzalez.tension.ui.components.MetricSufficiencyRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrendViewModel @Inject constructor(
    private val getMicrocycleMapUseCase: GetMicrocycleMapUseCase,
    private val getMuscleGroupTrendUseCase: GetMuscleGroupTrendUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TrendUiState>(TrendUiState.Loading)
    val uiState: StateFlow<TrendUiState> = _uiState.asStateFlow()

    init {
        loadTrends()
    }

    private fun loadTrends() {
        viewModelScope.launch {
            try {
                val microcycleMap = getMicrocycleMapUseCase()
                val cycleSize = microcycleMap.values.maxOfOrNull { it.size } ?: 1
                val completedCount = microcycleMap.count { it.value.size == cycleSize }
                val requirement = MetricSufficiencyRules.trend(completedCount)
                if (requirement != null) {
                    _uiState.value = TrendUiState.InsufficientData(requirement)
                    return@launch
                }
                val trends = getMuscleGroupTrendUseCase(microcycleMap)
                _uiState.value = TrendUiState.Content(
                    trends = trends,
                    evaluatedMicrocycles = minOf(
                        completedCount,
                        MetricSufficiencyRules.MIN_TREND_MICROCYCLES,
                    ),
                )
            } catch (e: Exception) {
                _uiState.value = TrendUiState.Error(
                    e.message ?: "Error al cargar tendencias",
                )
            }
        }
    }
}
