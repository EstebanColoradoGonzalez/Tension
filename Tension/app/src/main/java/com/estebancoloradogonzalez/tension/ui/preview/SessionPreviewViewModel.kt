package com.estebancoloradogonzalez.tension.ui.preview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.model.DeloadState
import com.estebancoloradogonzalez.tension.domain.model.TodaySession
import com.estebancoloradogonzalez.tension.domain.usecase.deload.GetDeloadStateUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.ClearTemporaryRoutineUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.GetActiveSessionUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.GetReassignableRoutinesUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.GetSessionPreviewUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.GetTodaySessionUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.SetTemporaryRoutineUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.StartSessionUseCase
import com.estebancoloradogonzalez.tension.domain.util.LoadDisplayMapper
import com.estebancoloradogonzalez.tension.domain.util.RepsDisplayMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

/**
 * Preview de la sesión propuesta para hoy.
 *
 * Los argumentos de navegación quedan como valor inicial —para que la primera composición no
 * parpadee— y la fuente de verdad pasa a ser `getTodaySessionUseCase`. Es lo que permite que
 * la reasignación se resuelva **en esta pantalla**: si la rutina propuesta cambia, la lista
 * de ejercicios se recarga sin re-navegar. El preview solo se alcanza desde la tarjeta de
 * Inicio, así que siempre muestra la propuesta de hoy.
 */
@HiltViewModel
class SessionPreviewViewModel @Inject constructor(
    private val getSessionPreviewUseCase: GetSessionPreviewUseCase,
    private val startSessionUseCase: StartSessionUseCase,
    private val getDeloadStateUseCase: GetDeloadStateUseCase,
    private val getTodaySessionUseCase: GetTodaySessionUseCase,
    private val getActiveSessionUseCase: GetActiveSessionUseCase,
    private val getReassignableRoutinesUseCase: GetReassignableRoutinesUseCase,
    private val setTemporaryRoutineUseCase: SetTemporaryRoutineUseCase,
    private val clearTemporaryRoutineUseCase: ClearTemporaryRoutineUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val initialRoutineVersionId: Long = checkNotNull(savedStateHandle["routineVersionId"])
    private val initialRoutineName: String = URLDecoder.decode(
        checkNotNull(savedStateHandle["routineName"]), "UTF-8",
    )
    private val initialVersionNumber: Int = checkNotNull(savedStateHandle["versionNumber"])

    private val _uiState = MutableStateFlow(
        SessionPreviewUiState(
            routineName = initialRoutineName,
            versionNumber = initialVersionNumber,
            routineVersionId = initialRoutineVersionId,
        ),
    )
    val uiState: StateFlow<SessionPreviewUiState> = _uiState.asStateFlow()

    private val _navigateToActiveSession = MutableSharedFlow<Long>(replay = 0)
    val navigateToActiveSession: SharedFlow<Long> = _navigateToActiveSession.asSharedFlow()

    init {
        observeTodaySession()
        observeExercises()
    }

    private fun observeTodaySession() {
        viewModelScope.launch {
            combine(
                getTodaySessionUseCase(),
                getActiveSessionUseCase(),
                getReassignableRoutinesUseCase(),
            ) { todaySession, activeSession, reassignOptions ->
                Triple(todaySession, activeSession != null, reassignOptions)
            }.collect { (todaySession, hasActiveSession, reassignOptions) ->
                _uiState.update { current ->
                    current.copy(
                        weekDay = todaySession.weekDay,
                        routineName = todaySession.session?.routineName ?: current.routineName,
                        versionNumber = todaySession.session?.versionNumber
                            ?: current.versionNumber,
                        routineVersionId = todaySession.session?.routineVersionId
                            ?: current.routineVersionId,
                        isTemporaryOverride = todaySession.isTemporaryOverride,
                        isDayResolved = todaySession.isDayResolved,
                        canReassign = !hasActiveSession && !todaySession.isDayResolved,
                        reassignOptions = reassignOptions,
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeExercises() {
        viewModelScope.launch {
            val deloadState = getDeloadStateUseCase()
            val isDeload = deloadState is DeloadState.DeloadActive
            val deloadSessionsRemaining = if (deloadState is DeloadState.DeloadActive) {
                deloadState.totalSessions - deloadState.progress
            } else {
                0
            }

            currentRoutineVersionIds()
                .flatMapLatest { routineVersionId ->
                    if (routineVersionId == 0L) {
                        flowOf(emptyList())
                    } else {
                        getSessionPreviewUseCase(routineVersionId)
                    }
                }
                .collect { exercises ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            isDeloadActive = isDeload,
                            deloadSessionsRemaining = deloadSessionsRemaining,
                            exercises = exercises.map { exercise ->
                                val loadText = LoadDisplayMapper.mapLoadDisplay(
                                    isDeload = isDeload,
                                    isIsometric = exercise.isIsometric,
                                    isBodyweight = exercise.isBodyweight,
                                    prescribedLoadKg = exercise.prescribedLoadKg,
                                    muscleGroup = exercise.muscleGroup,
                                )
                                val (repsText, isSpecial) = RepsDisplayMapper.mapRepsToDisplay(
                                    exercise.reps,
                                )
                                PreviewExerciseItem(
                                    exerciseId = exercise.exerciseId,
                                    name = exercise.exerciseName,
                                    equipmentTypeName = exercise.equipmentTypeName,
                                    muscleZones = exercise.muscleZones,
                                    setsDisplay = "${exercise.sets} series",
                                    repsDisplay = repsText,
                                    isRepsSpecial = isSpecial,
                                    loadDisplayText = loadText,
                                    isBodyweight = exercise.isBodyweight,
                                    showOutOfGymBadge = exercise.isBodyweight,
                                )
                            },
                        )
                    }
                }
        }
    }

    /**
     * La versión que el preview debe mostrar: la de hoy mientras exista propuesta, y la del
     * argumento de navegación hasta que el primer valor llegue.
     */
    private fun currentRoutineVersionIds() =
        getTodaySessionUseCase()
            .map(::routineVersionIdOf)
            .distinctUntilChanged()

    private fun routineVersionIdOf(todaySession: TodaySession): Long =
        todaySession.session?.routineVersionId ?: initialRoutineVersionId

    fun startSession() {
        // El día resuelto bloquea también aquí: si no, el preview sería la puerta trasera
        // para ejecutar una segunda sesión el mismo día.
        if (_uiState.value.isDayResolved) return
        val routineVersionId = _uiState.value.routineVersionId
        viewModelScope.launch {
            try {
                val sessionId = startSessionUseCase(routineVersionId)
                _navigateToActiveSession.emit(sessionId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun openReassignDialog() {
        if (!_uiState.value.canReassign) return
        _uiState.update { it.copy(isReassignDialogOpen = true) }
    }

    fun dismissReassignDialog() {
        _uiState.update { it.copy(isReassignDialogOpen = false) }
    }

    fun confirmReassign(routineId: Long) {
        _uiState.update { it.copy(isReassignDialogOpen = false) }
        viewModelScope.launch {
            try {
                setTemporaryRoutineUseCase(routineId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun undoReassign() {
        viewModelScope.launch {
            try {
                clearTemporaryRoutineUseCase()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
