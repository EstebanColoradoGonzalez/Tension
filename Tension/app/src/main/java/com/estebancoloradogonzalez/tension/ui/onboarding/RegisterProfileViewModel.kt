package com.estebancoloradogonzalez.tension.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.model.ExperienceLevel
import com.estebancoloradogonzalez.tension.domain.usecase.profile.CreateProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
class RegisterProfileViewModel @Inject constructor(
    private val createProfileUseCase: CreateProfileUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterProfileUiState())
    val uiState: StateFlow<RegisterProfileUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<RegisterProfileEvent>()
    val events: SharedFlow<RegisterProfileEvent> = _events.asSharedFlow()

    fun onWeightChanged(weight: String) {
        _uiState.update {
            it.copy(weightKg = weight, showWeightError = false)
        }
    }

    fun onHeightChanged(height: String) {
        _uiState.update {
            it.copy(heightM = height, showHeightError = false)
        }
    }

    fun onExperienceLevelSelected(level: ExperienceLevel) {
        _uiState.update {
            it.copy(experienceLevel = level)
        }
    }

    fun onRegisterClick() {
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
            _uiState.update { it.copy(isLoading = true) }
            createProfileUseCase(weight, height, level)
                .onSuccess {
                    _events.emit(RegisterProfileEvent.NavigateToHome)
                }
                .onFailure {
                    _uiState.update { s -> s.copy(isLoading = false) }
                }
        }
    }
}
