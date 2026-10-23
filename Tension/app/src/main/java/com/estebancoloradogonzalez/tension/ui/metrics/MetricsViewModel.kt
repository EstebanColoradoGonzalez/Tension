package com.estebancoloradogonzalez.tension.ui.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.usecase.metrics.GetAdherenceUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.metrics.GetAvgRirByRoutineUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.metrics.GetBodyWeightSummaryUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.metrics.GetLoadVelocityUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.metrics.GetProgressionRateUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.profile.GetProfileUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.profile.GetWeightHistoryUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.profile.UpdateWeightUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.RoundingMode
import javax.inject.Inject

@HiltViewModel
class MetricsViewModel @Inject constructor(
    private val getAdherenceUseCase: GetAdherenceUseCase,
    private val getAvgRirByRoutineUseCase: GetAvgRirByRoutineUseCase,
    private val getProgressionRateUseCase: GetProgressionRateUseCase,
    private val getLoadVelocityUseCase: GetLoadVelocityUseCase,
    private val getBodyWeightSummaryUseCase: GetBodyWeightSummaryUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val getWeightHistoryUseCase: GetWeightHistoryUseCase,
    private val updateWeightUseCase: UpdateWeightUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MetricsUiState>(MetricsUiState.Loading)
    val uiState: StateFlow<MetricsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MetricsEvent>()
    val events: SharedFlow<MetricsEvent> = _events.asSharedFlow()

    private var progressionWeeks = DEFAULT_PROGRESSION_WEEKS
    private var rirSessionLimit = DEFAULT_RIR_SESSION_LIMIT

    init {
        loadMetrics()
        observeBodyWeightSources()
    }

    private fun loadMetrics() {
        viewModelScope.launch {
            try {
                val adherence = getAdherenceUseCase()
                val rir = getAvgRirByRoutineUseCase(rirSessionLimit)
                val rates = getProgressionRateUseCase(progressionWeeks)
                val velocities = getLoadVelocityUseCase(progressionWeeks)
                val bodyWeight = getBodyWeightSummaryUseCase(progressionWeeks).getOrThrow()
                val openDialog = (_uiState.value as? MetricsUiState.Content)?.weightDialog
                _uiState.value = MetricsUiState.Content(
                    adherence = adherence,
                    rirByRoutine = rir,
                    progressionRates = rates,
                    loadVelocities = velocities,
                    progressionWeeks = progressionWeeks,
                    rirSessionLimit = rirSessionLimit,
                    bodyWeight = bodyWeight,
                    weightDialog = openDialog,
                )
            } catch (e: Exception) {
                _uiState.value = MetricsUiState.Error(
                    e.message ?: "Error al cargar métricas",
                )
            }
        }
    }

    /**
     * The weight history and the height also change outside this screen (profile edition).
     * Room re-emits on every change, so the section follows without a manual refresh.
     */
    private fun observeBodyWeightSources() {
        viewModelScope.launch {
            combine(getProfileUseCase(), getWeightHistoryUseCase()) { _, _ -> }
                .collect { refreshBodyWeight() }
        }
    }

    private fun refreshBodyWeight() {
        viewModelScope.launch {
            if (_uiState.value !is MetricsUiState.Content) return@launch
            getBodyWeightSummaryUseCase(progressionWeeks)
                .onSuccess { summary -> updateContent { it.copy(bodyWeight = summary) } }
                .onFailure { error ->
                    // Same treatment as a failing load: the panel reports it, never a stale section.
                    _uiState.value = MetricsUiState.Error(error.message ?: "Error al cargar métricas")
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

    /** Opens the quick weight entry prefilled with the last registered weight (CA-48.04). */
    fun openWeightDialog() {
        updateContent { content ->
            val lastWeight = content.bodyWeight?.currentWeightKg
            content.copy(weightDialog = WeightDialogState(input = lastWeight?.let(::formatWeight).orEmpty()))
        }
    }

    fun onWeightInputChanged(value: String) {
        updateContent { content ->
            content.copy(weightDialog = content.weightDialog?.copy(input = value))
        }
    }

    /** Cancelling creates no entry: the history is left untouched (CA-48.05). */
    fun dismissWeightDialog() {
        updateContent { it.copy(weightDialog = null) }
    }

    /**
     * Saves through [UpdateWeightUseCase], the same path as the profile: a new entry of the
     * weight history dated today, which also becomes the current weight (Rule 6).
     */
    fun saveWeight() {
        val dialog = (_uiState.value as? MetricsUiState.Content)?.weightDialog ?: return
        val weight = dialog.weightKg ?: return
        if (!dialog.canSave) return

        updateContent { it.copy(weightDialog = dialog.copy(isSaving = true)) }
        viewModelScope.launch {
            updateWeightUseCase(weight)
                .onSuccess {
                    updateContent { it.copy(weightDialog = null) }
                    _events.emit(MetricsEvent.WeightSaved)
                    refreshBodyWeight()
                }
                .onFailure { error ->
                    updateContent { it.copy(weightDialog = it.weightDialog?.copy(isSaving = false)) }
                    _events.emit(MetricsEvent.SaveError(error.message ?: "Error"))
                }
        }
    }

    private fun updateContent(transform: (MetricsUiState.Content) -> MetricsUiState.Content) {
        _uiState.update { state ->
            if (state is MetricsUiState.Content) transform(state) else state
        }
    }

    /** Same rendering as the weight field of the profile: up to two decimals, no trailing zeros. */
    private fun formatWeight(weightKg: Double): String =
        weightKg.toBigDecimal()
            .setScale(2, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()

    companion object {
        const val DEFAULT_PROGRESSION_WEEKS = 4
        const val DEFAULT_RIR_SESSION_LIMIT = 2
    }
}
