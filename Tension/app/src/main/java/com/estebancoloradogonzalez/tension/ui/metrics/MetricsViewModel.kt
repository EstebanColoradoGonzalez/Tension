package com.estebancoloradogonzalez.tension.ui.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.usecase.metrics.GetAdherenceUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.metrics.GetAvgRirByModuleUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.metrics.GetLoadVelocityUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.metrics.GetProgressionRateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MetricsViewModel @Inject constructor(
    private val getAdherenceUseCase: GetAdherenceUseCase,
    private val getAvgRirByModuleUseCase: GetAvgRirByModuleUseCase,
    private val getProgressionRateUseCase: GetProgressionRateUseCase,
    private val getLoadVelocityUseCase: GetLoadVelocityUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MetricsUiState>(MetricsUiState.Loading)
    val uiState: StateFlow<MetricsUiState> = _uiState.asStateFlow()

    private var progressionWeeks = 4
    private var rirSessionLimit = 2

    init {
        loadMetrics()
    }

    private fun loadMetrics() {
        viewModelScope.launch {
            try {
                val adherence = getAdherenceUseCase()
                val rir = getAvgRirByModuleUseCase(rirSessionLimit)
                val rates = getProgressionRateUseCase(progressionWeeks)
                val velocities = getLoadVelocityUseCase(progressionWeeks)
                _uiState.value = MetricsUiState.Content(adherence, rir, rates, velocities)
            } catch (e: Exception) {
                _uiState.value = MetricsUiState.Error(
                    e.message ?: "Error al cargar métricas",
                )
            }
        }
    }

    fun changeProgressionPeriod(weeks: Int) {
        progressionWeeks = weeks
        loadMetrics()
    }

    fun changeRirPeriod(sessionLimit: Int) {
        rirSessionLimit = sessionLimit
        loadMetrics()
    }
}
