package com.estebancoloradogonzalez.tension.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.model.ExperienceLevel
import com.estebancoloradogonzalez.tension.domain.usecase.profile.GetProfileUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.profile.UpdateProfileUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.profile.UpdateWeightUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.RoundingMode
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val updateWeightUseCase: UpdateWeightUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ProfileEvent>()
    val events: SharedFlow<ProfileEvent> = _events.asSharedFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val profile = getProfileUseCase().first()
            if (profile != null) {
                val weightStr = profile.currentWeightKg.toBigDecimal()
                    .setScale(2, RoundingMode.HALF_UP)
                    .stripTrailingZeros()
                    .toPlainString()
                val heightStr = profile.heightM.toBigDecimal()
                    .setScale(2, RoundingMode.HALF_UP)
                    .stripTrailingZeros()
                    .toPlainString()
                _uiState.update {
                    ProfileUiState(
                        isLoading = false,
                        weightKg = weightStr,
                        heightM = heightStr,
                        experienceLevel = profile.experienceLevel,
                        originalWeightKg = weightStr,
                        originalHeightM = heightStr,
                        originalExperienceLevel = profile.experienceLevel,
                    )
                }
            }
        }
    }

    fun onWeightChanged(weight: String) {
        _uiState.update { it.copy(weightKg = weight, showWeightError = false) }
    }

    fun onHeightChanged(height: String) {
        _uiState.update { it.copy(heightM = height, showHeightError = false) }
    }

    fun onExperienceLevelSelected(level: ExperienceLevel) {
        _uiState.update { it.copy(experienceLevel = level) }
    }

    fun onSaveClick() {
        val state = _uiState.value
        val weight = state.weightKg.toDoubleOrNull()
        val height = state.heightM.toDoubleOrNull()
        val level = state.experienceLevel

        if (weight == null || weight <= 0) {
            _uiState.update { it.copy(showWeightError = true) }
            return
        }
        if (height == null || height <= 0) {
            _uiState.update { it.copy(showHeightError = true) }
            return
        }
        if (level == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val weightChanged = state.weightKg != state.originalWeightKg

            if (weightChanged) {
                updateWeightUseCase(weight).onFailure { error ->
                    _uiState.update { s -> s.copy(isSaving = false) }
                    _events.emit(ProfileEvent.SaveError(error.message ?: "Error"))
                    return@launch
                }
            }

            updateProfileUseCase(height, level)
                .onSuccess {
                    _uiState.update { s ->
                        s.copy(
                            isSaving = false,
                            originalWeightKg = state.weightKg,
                            originalHeightM = state.heightM,
                            originalExperienceLevel = level,
                        )
                    }
                    _events.emit(ProfileEvent.SaveSuccess)
                }
                .onFailure { error ->
                    _uiState.update { s -> s.copy(isSaving = false) }
                    _events.emit(ProfileEvent.SaveError(error.message ?: "Error"))
                }
        }
    }
}
