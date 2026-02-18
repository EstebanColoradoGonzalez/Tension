package com.estebancoloradogonzalez.tension.ui.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.usecase.metrics.GetMicrocycleMapUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.metrics.GetMuscleGroupTrendUseCase
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
                val completedCount = microcycleMap.count { it.value.size == 6 }
                if (completedCount < 4) {
                    _uiState.value = TrendUiState.InsufficientData(
                        remaining = 4 - completedCount,
                    )
                    return@launch
                }
                val trends = getMuscleGroupTrendUseCase(microcycleMap)
                _uiState.value = TrendUiState.Content(trends)
            } catch (e: Exception) {
                _uiState.value = TrendUiState.Error(
                    e.message ?: "Error al cargar tendencias",
                )
            }
        }
    }
}
