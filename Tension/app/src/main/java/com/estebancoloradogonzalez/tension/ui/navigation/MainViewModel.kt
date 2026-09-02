package com.estebancoloradogonzalez.tension.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.usecase.profile.CheckProfileExistsUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.ResolveStaleSessionUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.tree.RecalculateTreeStateUseCase
import com.estebancoloradogonzalez.tension.domain.util.CurrentDateProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StartDestination {
    LOADING,
    ONBOARDING,
    HOME,
}

@HiltViewModel
class MainViewModel @Inject constructor(
    checkProfileExistsUseCase: CheckProfileExistsUseCase,
    private val resolveStaleSessionUseCase: ResolveStaleSessionUseCase,
    private val recalculateTreeStateUseCase: RecalculateTreeStateUseCase,
    private val currentDateProvider: CurrentDateProvider,
) : ViewModel() {

    init {
        // La primera emisión es inmediata —cubre el arranque de la app— y las siguientes
        // llegan al cruzar la medianoche con la app abierta. Es el único momento en que el
        // sistema puede resolver lo que quedó del día anterior: con la app cerrada no corre
        // nada, así que el barrido ocurre en cuanto vuelve a abrirse.
        viewModelScope.launch {
            currentDateProvider.dateFlow().collect {
                try {
                    resolveStaleSessionUseCase()
                    // DESPUÉS del barrido, nunca antes, y por eso va aquí y no en un
                    // observador aparte: el barrido cierra la sesión de ayer conservando su
                    // fecha original. Recalcular primero leería una fecha de último
                    // entrenamiento desactualizada y marchitaría el árbol de alguien que sí
                    // entrenó. Si el barrido se mueve algún día, esto se mueve con él.
                    recalculateTreeStateUseCase()
                } catch (_: Exception) {
                    // El barrido es best-effort: si falla, la sesión sigue reanudable a mano.
                }
            }
        }
    }

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
