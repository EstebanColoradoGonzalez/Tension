package com.estebancoloradogonzalez.tension.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.rules.PlateauThresholdRule
import com.estebancoloradogonzalez.tension.domain.usecase.profile.GetProfileUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.profile.UpdatePlateauBaseThresholdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val updatePlateauBaseThresholdUseCase: UpdatePlateauBaseThresholdUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val profile = getProfileUseCase().first()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    baseThreshold = profile?.plateauBaseThreshold
                        ?: PlateauThresholdRule.DEFAULT_BASE_THRESHOLD,
                )
            }
        }
    }

    fun onIncreaseThreshold() {
        applyThreshold(_uiState.value.baseThreshold + 1)
    }

    fun onDecreaseThreshold() {
        applyThreshold(_uiState.value.baseThreshold - 1)
    }

    /**
     * The range belongs to the domain, not to the control: the stepper stops at the
     * bounds, but the use case is the one that decides whether a value is valid. An
     * out-of-range value is rejected there and surfaces as an observable error state.
     */
    private fun applyThreshold(value: Int) {
        viewModelScope.launch {
            updatePlateauBaseThresholdUseCase(value)
                .onSuccess {
                    _uiState.update { it.copy(baseThreshold = value, rangeError = null) }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            rangeError = "El umbral debe estar entre " +
                                "${PlateauThresholdRule.MIN_BASE_THRESHOLD} y " +
                                "${PlateauThresholdRule.MAX_BASE_THRESHOLD}",
                        )
                    }
                }
        }
    }

    fun onDismissRangeError() {
        _uiState.update { it.copy(rangeError = null) }
    }
}
