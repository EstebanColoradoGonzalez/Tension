package com.estebancoloradogonzalez.tension.ui.session

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.domain.model.DeloadState
import com.estebancoloradogonzalez.tension.domain.model.SessionAdjustment
import com.estebancoloradogonzalez.tension.domain.model.ExerciseSessionStatus
import com.estebancoloradogonzalez.tension.domain.usecase.deload.GetDeloadStateUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.CloseSessionUseCase
import com.estebancoloradogonzalez.tension.domain.util.LoadDisplayMapper
import com.estebancoloradogonzalez.tension.domain.usecase.session.GetSessionExercisesUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.FinalizeExerciseUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.AddExerciseToSessionUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.GetAddableExercisesUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.GetSessionAdjustmentUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.WithdrawFromSessionUseCase
import com.estebancoloradogonzalez.tension.domain.rules.SessionAdjustmentRule
import com.estebancoloradogonzalez.tension.domain.rules.WithdrawalVerdict
import com.estebancoloradogonzalez.tension.domain.repository.PlanRepository
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActiveSessionViewModel @Inject constructor(
    private val getSessionExercisesUseCase: GetSessionExercisesUseCase,
    private val closeSessionUseCase: CloseSessionUseCase,
    private val finalizeExerciseUseCase: FinalizeExerciseUseCase,
    private val sessionRepository: SessionRepository,
    private val planRepository: PlanRepository,
    private val getDeloadStateUseCase: GetDeloadStateUseCase,
    private val getSessionAdjustmentUseCase: GetSessionAdjustmentUseCase,
    private val getAddableExercisesUseCase: GetAddableExercisesUseCase,
    private val addExerciseToSessionUseCase: AddExerciseToSessionUseCase,
    private val withdrawFromSessionUseCase: WithdrawFromSessionUseCase,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val sessionId: Long = savedStateHandle.get<Long>("sessionId")
        ?: throw IllegalArgumentException("sessionId is required")

    private val _uiState = MutableStateFlow(ActiveSessionUiState())
    val uiState: StateFlow<ActiveSessionUiState> = _uiState.asStateFlow()

    private val _navigateToSessionSummary = MutableSharedFlow<Long>(replay = 0)
    val navigateToSessionSummary: SharedFlow<Long> = _navigateToSessionSummary.asSharedFlow()

    private val _alternativeSelectionState = MutableStateFlow(AlternativeSelectionUiState())
    val alternativeSelectionState: StateFlow<AlternativeSelectionUiState> = _alternativeSelectionState.asStateFlow()

    private val _addExerciseSheetState = MutableStateFlow(AddExerciseSheetState())
    val addExerciseSheetState: StateFlow<AddExerciseSheetState> = _addExerciseSheetState.asStateFlow()

    private val _withdrawDialogState = MutableStateFlow(WithdrawDialogState())
    val withdrawDialogState: StateFlow<WithdrawDialogState> = _withdrawDialogState.asStateFlow()

    init {
        viewModelScope.launch {
            val deloadId = sessionRepository.getDeloadIdBySessionId(sessionId)
            val isDeload = deloadId != null
            val deloadProgressText = if (deloadId != null) {
                val deloadState = getDeloadStateUseCase()
                if (deloadState is DeloadState.DeloadActive) {
                    "${deloadState.progress + 1}/${deloadState.totalSessions}"
                } else {
                    ""
                }
            } else {
                ""
            }

            combine(
                getSessionExercisesUseCase(sessionId),
                sessionRepository.getSessionRoutineVersion(sessionId),
                getSessionAdjustmentUseCase(sessionId),
            ) { exercises, routineVersion, adjustment ->
                Triple(exercises, routineVersion, adjustment)
            }.collect { (exercises, routineVersion, adjustment) ->
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        isDeloadSession = isDeload,
                        deloadProgress = deloadProgressText,
                        routineName = routineVersion?.first ?: "",
                        versionNumber = routineVersion?.second ?: 0,
                        addedCount = adjustment.addedCount,
                        withdrawnCount = adjustment.withdrawnCount,
                        exercises = exercises.map { detail ->
                            val statusLabel = when (detail.status) {
                                ExerciseSessionStatus.NOT_STARTED -> "No Iniciado"
                                ExerciseSessionStatus.IN_PROGRESS -> "En Ejecución"
                                ExerciseSessionStatus.COMPLETED -> "Completado"
                            }
                            val loadText = LoadDisplayMapper.mapLoadDisplay(
                                isDeload = isDeload,
                                isIsometric = detail.isIsometric,
                                isBodyweight = detail.isBodyweight,
                                prescribedLoadKg = detail.prescribedLoadKg,
                                muscleGroup = detail.muscleGroup,
                            )
                            ExerciseUiItem(
                                sessionExerciseId = detail.sessionExerciseId,
                                exerciseId = detail.exerciseId,
                                name = detail.name ?: "",
                                equipmentSummary = detail.equipmentTypes.joinToString(" · "),
                                muscleZones = detail.muscleZones.joinToString(", "),
                                sets = detail.sets,
                                reps = detail.reps,
                                prescribedLoadKg = detail.prescribedLoadKg,
                                isBodyweight = detail.isBodyweight,
                                isIsometric = detail.isIsometric,
                                isToTechnicalFailure = detail.isToTechnicalFailure,
                                completedSets = detail.completedSets,
                                status = detail.status,
                                loadDisplayText = loadText,
                                statusDisplayText = "$statusLabel · ${detail.completedSets}/${detail.sets} series",
                                isFinalized = detail.isFinalized,
                                slot = detail.slot,
                                hasAlternatives = detail.hasAlternatives,
                                isExtra = detail.isExtra,
                                withdrawalBlockReason = withdrawalBlockReason(
                                    isExtra = detail.isExtra,
                                    completedSets = detail.completedSets,
                                    adjustment = adjustment,
                                ),
                            )
                        },
                    )
                }
            }
        }
    }

    fun onCloseSessionRequested() {
        if (_uiState.value.isClosing) return
        _uiState.update { it.copy(showCloseDialog = true) }
    }

    fun onCloseDialogDismissed() {
        _uiState.update { it.copy(showCloseDialog = false) }
    }

    fun onCloseSessionConfirmed() {
        if (_uiState.value.isClosing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isClosing = true, showCloseDialog = false, errorMessage = null) }
            try {
                closeSessionUseCase(sessionId)
                _navigateToSessionSummary.emit(sessionId)
            } catch (e: Exception) {
                _uiState.update { it.copy(isClosing = false, errorMessage = e.message) }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onFinalizeExercise(sessionExerciseId: Long) {
        viewModelScope.launch {
            try {
                finalizeExerciseUseCase(sessionExerciseId)
            } catch (_: Exception) {
                // Finalization failed — no-op
            }
        }
    }

    fun onSelectAlternative(exercise: ExerciseUiItem) {
        if (!exercise.hasAlternatives) return
        if (exercise.completedSets > 0) return
        if (exercise.isFinalized) return
        viewModelScope.launch {
            try {
                val routineVersionId = sessionRepository.getRoutineVersionIdBySessionId(sessionId)
                val options = planRepository.getAlternativesForSlot(routineVersionId, exercise.slot)
                _alternativeSelectionState.value = AlternativeSelectionUiState(
                    isVisible = true,
                    sessionExerciseId = exercise.sessionExerciseId,
                    slot = exercise.slot,
                    alternatives = options.map { alt ->
                        AlternativeOption(
                            exerciseId = alt.exerciseId,
                            name = alt.name,
                            equipmentSummary = alt.equipmentTypes.joinToString(" · "),
                            muscleZonesSummary = alt.muscleZones.joinToString(", "),
                        )
                    },
                )
            } catch (_: Exception) {
                // Failed to load alternatives — no-op
            }
        }
    }

    fun onAlternativeSelected(exerciseId: Long) {
        _alternativeSelectionState.update { it.copy(selectedExerciseId = exerciseId) }
    }

    fun onConfirmAlternativeSelection() {
        val state = _alternativeSelectionState.value
        val exerciseId = state.selectedExerciseId ?: return
        viewModelScope.launch {
            try {
                sessionRepository.switchAlternativeInSession(state.sessionExerciseId, exerciseId)
            } catch (_: Exception) {
                // Failed — no-op
            } finally {
                _alternativeSelectionState.value = AlternativeSelectionUiState()
            }
        }
    }

    fun onDismissAlternativeSelection() {
        _alternativeSelectionState.value = AlternativeSelectionUiState()
    }

    /**
     * Por qué este ejercicio no puede retirarse, ya redactado. `null` habilita la acción.
     *
     * El veredicto lo decide [SessionAdjustmentRule] —incluido el orden de evaluación, que
     * antepone las series al presupuesto—; aquí solo se traduce a la línea que el menú
     * muestra bajo la acción deshabilitada. Decirlo antes de tocar, y no después en un
     * `Snackbar`, es lo que convierte la restricción en explicación.
     */
    private fun withdrawalBlockReason(
        isExtra: Boolean,
        completedSets: Int,
        adjustment: SessionAdjustment,
    ): String? {
        val verdict = if (isExtra) {
            SessionAdjustmentRule.verdictForAddedExercise(
                addedCount = adjustment.addedCount,
                withdrawnCount = adjustment.withdrawnCount,
                completedSets = completedSets,
            )
        } else {
            SessionAdjustmentRule.verdictForPlanExercise(
                addedCount = adjustment.addedCount,
                withdrawnCount = adjustment.withdrawnCount,
                completedSets = completedSets,
            )
        }
        return when (verdict) {
            is WithdrawalVerdict.Allowed -> null
            is WithdrawalVerdict.HasSets -> if (verdict.count == 1) {
                context.getString(R.string.session_withdraw_blocked_sets_one)
            } else {
                context.getString(R.string.session_withdraw_blocked_sets, verdict.count)
            }
            // "Otro" en cuanto ya se anadio algo: con presupuesto agotado y un anadido
            // hecho, pedir "un ejercicio" sonaria a que no se ha anadido ninguno.
            is WithdrawalVerdict.BudgetExhausted -> if (adjustment.addedCount == 0) {
                context.getString(R.string.session_withdraw_blocked_budget)
            } else {
                context.getString(R.string.session_withdraw_blocked_budget_more)
            }
            // El nombre siempre existe: el veredicto implica que queda un retiro sin
            // reponer. La demostracion vive en el KDoc de la regla.
            is WithdrawalVerdict.WouldBreakInvariant -> context.getString(
                R.string.session_withdraw_blocked_invariant,
                adjustment.pendingWithdrawals.firstOrNull()?.name.orEmpty(),
            )
        }
    }

    fun onAddExerciseRequested() {
        _addExerciseSheetState.value = AddExerciseSheetState(isVisible = true)
        viewModelScope.launch {
            getAddableExercisesUseCase(sessionId).collect { catalog ->
                _addExerciseSheetState.update { current ->
                    if (!current.isVisible) return@update current
                    current.copy(
                        exercises = catalog.map {
                            AddableExerciseUiItem(
                                exerciseId = it.exerciseId,
                                name = it.name,
                                equipmentSummary = it.equipmentSummary,
                                muscleZonesSummary = it.muscleZonesSummary,
                                isAlreadyInSession = it.isAlreadyInSession,
                            )
                        },
                    )
                }
            }
        }
    }

    fun onAddQueryChanged(query: String) {
        _addExerciseSheetState.update { it.copy(query = query) }
    }

    fun onExerciseChosen(exerciseId: Long) {
        if (_addExerciseSheetState.value.isAdding) return
        viewModelScope.launch {
            _addExerciseSheetState.update { it.copy(isAdding = true) }
            try {
                addExerciseToSessionUseCase(sessionId, exerciseId)
                _addExerciseSheetState.value = AddExerciseSheetState()
            } catch (e: Exception) {
                _addExerciseSheetState.update { it.copy(isAdding = false) }
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun onDismissAddExercise() {
        _addExerciseSheetState.value = AddExerciseSheetState()
    }

    fun onWithdrawRequested(exercise: ExerciseUiItem) {
        if (exercise.withdrawalBlockReason != null) return
        _withdrawDialogState.value = WithdrawDialogState(
            isVisible = true,
            sessionExerciseId = exercise.sessionExerciseId,
            exerciseName = exercise.name,
            isExtra = exercise.isExtra,
        )
    }

    fun onWithdrawConfirmed() {
        val state = _withdrawDialogState.value
        if (!state.isVisible) return
        _withdrawDialogState.value = WithdrawDialogState()
        viewModelScope.launch {
            try {
                withdrawFromSessionUseCase(state.sessionExerciseId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun onDismissWithdrawDialog() {
        _withdrawDialogState.value = WithdrawDialogState()
    }
}
