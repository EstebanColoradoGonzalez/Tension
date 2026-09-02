package com.estebancoloradogonzalez.tension.ui.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.usecase.metrics.GetAdherenceUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.metrics.GetAvgRirByRoutineUseCase
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
    private val getAvgRirByRoutineUseCase: GetAvgRirByRoutineUseCase,
    private val getProgressionRateUseCase: GetProgressionRateUseCase,
    private val getLoadVelocityUseCase: GetLoadVelocityUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MetricsUiState>(MetricsUiState.Loading)
    val uiState: StateFlow<MetricsUiState> = _uiState.asStateFlow()

    private var progressionWeeks = DEFAULT_PROGRESSION_WEEKS
    private var rirSessionLimit = DEFAULT_RIR_SESSION_LIMIT

    init {
        loadMetrics()
    }

    private fun loadMetrics() {
        viewModelScope.launch {
            try {
                val adherence = getAdherenceUseCase()
                val rir = getAvgRirByRoutineUseCase(rirSessionLimit)
                val rates = getProgressionRateUseCase(progressionWeeks)
                val velocities = getLoadVelocityUseCase(progressionWeeks)
                _uiState.value = MetricsUiState.Content(
                    adherence = adherence,
                    rirByRoutine = rir,
                    progressionRates = rates,
                    loadVelocities = velocities,
                    progressionWeeks = progressionWeeks,
                    rirSessionLimit = rirSessionLimit,
                )
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

    companion object {
        const val DEFAULT_PROGRESSION_WEEKS = 4
        const val DEFAULT_RIR_SESSION_LIMIT = 2
    }
}
