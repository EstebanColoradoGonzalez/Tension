package com.estebancoloradogonzalez.tension.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.usecase.profile.CheckProfileExistsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class StartDestination {
    LOADING,
    ONBOARDING,
    HOME,
}

@HiltViewModel
class MainViewModel @Inject constructor(
    checkProfileExistsUseCase: CheckProfileExistsUseCase,
) : ViewModel() {

    val startDestination: StateFlow<StartDestination> = checkProfileExistsUseCase()
        .map { exists ->
            if (exists) StartDestination.HOME else StartDestination.ONBOARDING
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StartDestination.LOADING,
        )
}
