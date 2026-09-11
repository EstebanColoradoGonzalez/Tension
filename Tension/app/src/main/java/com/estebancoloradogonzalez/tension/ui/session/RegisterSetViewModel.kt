package com.estebancoloradogonzalez.tension.ui.session

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.domain.model.WeightUnit
import com.estebancoloradogonzalez.tension.domain.rules.ExternalLoadRule
import com.estebancoloradogonzalez.tension.domain.usecase.session.GetPrefilledLoadForEquipmentUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.GetRegisterSetInfoUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.RegisterSetUseCase
import com.estebancoloradogonzalez.tension.domain.util.RepsRangeParser
import com.estebancoloradogonzalez.tension.domain.util.WeightCaptureError
import com.estebancoloradogonzalez.tension.domain.util.WeightCaptureValidator
import com.estebancoloradogonzalez.tension.domain.util.WeightConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class RegisterSetViewModel @Inject constructor(
    private val getRegisterSetInfoUseCase: GetRegisterSetInfoUseCase,
    private val getPrefilledLoadForEquipmentUseCase: GetPrefilledLoadForEquipmentUseCase,
    private val registerSetUseCase: RegisterSetUseCase,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val sessionExerciseId: Long = savedStateHandle.get<Long>("sessionExerciseId") ?: 0L

    private val _uiState = MutableStateFlow(RegisterSetUiState())
    val uiState: StateFlow<RegisterSetUiState> = _uiState.asStateFlow()

    private val _navigateBack = MutableSharedFlow<Boolean>(replay = 0)
    val navigateBack: SharedFlow<Boolean> = _navigateBack.asSharedFlow()

    private var timerJob: Job? = null
    private var timerStartRealtime: Long = 0L

    /**
     * Resolución en curso de la precarga del par elegido.
     *
     * Se cancela en cuanto el ejecutante toca el peso: una sugerencia que llega tarde y
     * pisa lo que ya se tecleó es peor que no sugerir nada. El sistema sugiere, no impone.
     */
    private var prefillJob: Job? = null

    init {
        viewModelScope.launch {
            val info = getRegisterSetInfoUseCase(sessionExerciseId) ?: return@launch

            // El implemento preseleccionado gobierna si hay carga externa que capturar
            // (CA-39.05). Con `Peso Añadido` el lastre no se hereda de otra serie: la carga
            // anterior es de la barra fija o de la máquina, y allí siempre fue 0.
            val equipmentName = info.equipmentOptions
                .firstOrNull { it.id == info.preselectedEquipmentTypeId }?.name
            val captureEnabled = ExternalLoadRule.isCaptureEnabled(
                isBodyweight = info.isBodyweight,
                isIsometric = info.isIsometric,
                equipmentName = equipmentName,
            )

            // The prefilled load arrives in kilograms; it is shown in the active unit so
            // the executant never has to convert the number in their head.
            val weightInput = when {
                !captureEnabled -> "0"
                info.lastWeightKg != null && info.lastWeightKg > 0.0 -> format(
                    WeightConverter.fromKg(info.lastWeightKg, info.captureUnit),
                )
                else -> ""
            }

            val range = RepsRangeParser.parse(info.prescribedReps)
            val showChronometer: Boolean
            val minSeconds: Int?
            val maxSeconds: Int?

            if (range.isSeconds) {
                showChronometer = true
                minSeconds = range.min
                maxSeconds = range.max
            } else if (info.isIsometric) {
                showChronometer = true
                minSeconds = 30
                maxSeconds = 60
            } else {
                showChronometer = false
                minSeconds = null
                maxSeconds = null
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    exerciseName = info.exerciseName,
                    currentSetNumber = info.currentSetNumber,
                    totalSets = info.totalSets,
                    weightInput = weightInput,
                    captureUnit = info.captureUnit,
                    convertedWeightKg = convertedWeightKg(weightInput, info.captureUnit),
                    isIsometric = info.isIsometric,
                    isBodyweight = info.isBodyweight,
                    equipmentOptions = info.equipmentOptions,
                    selectedEquipmentTypeId = info.preselectedEquipmentTypeId,
                    preselectionOrigin = info.preselectionOrigin,
                    showChronometer = showChronometer,
                    minSeconds = minSeconds,
                    maxSeconds = maxSeconds,
                )
            }
        }
    }

    fun onStartTimer() {
        timerStartRealtime = SystemClock.elapsedRealtime()
        _uiState.update { it.copy(timerState = TimerState.RUNNING, timerSeconds = 0) }
        timerJob = viewModelScope.launch {
            while (true) {
                delay(100)
                val elapsed = ((SystemClock.elapsedRealtime() - timerStartRealtime) / 1000).toInt()
                val max = _uiState.value.maxSeconds
                if (max != null && elapsed >= max) {
                    _uiState.update { it.copy(timerSeconds = max) }
                    stopTimerInternal()
                    break
                }
                _uiState.update { it.copy(timerSeconds = elapsed) }
            }
        }
    }

    fun onStopTimer() {
        stopTimerInternal()
    }

    fun onResetTimer() {
        _uiState.update {
            it.copy(
                timerState = TimerState.IDLE,
                timerSeconds = 0,
                reps = "",
            )
        }
    }

    private fun stopTimerInternal() {
        timerJob?.cancel()
        timerJob = null
        val seconds = _uiState.value.timerSeconds
        _uiState.update {
            it.copy(
                timerState = TimerState.STOPPED,
                reps = seconds.toString(),
            )
        }
    }

    /**
     * Cambia el implemento y **recalcula** la precarga y la unidad para el par nuevo
     * (CA-40.03).
     *
     * El peso que había en el campo pertenecía a otro implemento, y entre dos implementos
     * el peso no es comparable: heredarlo sería sugerir la carga de otra cosa. Se vuelve a
     * resolver la precedencia completa —prescripción del par, serie anterior del par,
     * última serie del par— y si el par nuevo no tiene historial el campo queda **vacío**.
     *
     * Al pasar a un implemento sin carga externa el peso se fija en `"0"`, porque es el
     * valor que se va a registrar y mostrarlo evita que el ejecutante crea que se guardó
     * lo que tenía teclado.
     */
    fun onEquipmentSelected(equipmentTypeId: Long) {
        if (_uiState.value.selectedEquipmentTypeId == equipmentTypeId) return

        _uiState.update { state ->
            // El rótulo desaparece al elegir a mano: ya no es una preselección.
            state.copy(
                selectedEquipmentTypeId = equipmentTypeId,
                preselectionOrigin = null,
                equipmentError = null,
            )
        }

        prefillJob?.cancel()
        prefillJob = viewModelScope.launch {
            val prefilled = getPrefilledLoadForEquipmentUseCase(
                sessionExerciseId,
                equipmentTypeId,
            )
            _uiState.update { state ->
                // La selección pudo cambiar de nuevo mientras se resolvía: aplicar un
                // resultado tardío pondría en el campo el peso de un implemento que ya no
                // está elegido.
                if (state.selectedEquipmentTypeId != equipmentTypeId) return@update state

                val unit = if (state.isWeightEditable) {
                    prefilled?.captureUnit ?: state.captureUnit
                } else {
                    WeightUnit.KG
                }
                val weightKg = prefilled?.weightKg
                val input = when {
                    !state.isWeightEditable -> "0"
                    weightKg != null && weightKg > 0.0 ->
                        format(WeightConverter.fromKg(weightKg, unit))
                    else -> ""
                }
                state.withWeightInput(input, unit)
            }
        }
    }

    fun onWeightChanged(value: String) {
        prefillJob?.cancel()
        _uiState.update { state -> state.withWeightInput(value, state.captureUnit) }
    }

    /**
     * Switches the capture unit, converting the current input so the field keeps
     * expressing the same physical load.
     */
    fun onUnitSelected(unit: WeightUnit) {
        prefillJob?.cancel()
        _uiState.update { state ->
            if (state.captureUnit == unit) return@update state

            val currentValue = state.weightInput.toDoubleOrNull()
            val converted = if (currentValue != null) {
                format(WeightConverter.fromKg(WeightConverter.toKg(currentValue, state.captureUnit), unit))
            } else {
                state.weightInput
            }
            state.withWeightInput(converted, unit)
        }
    }

    fun onWeightStep(increase: Boolean) {
        prefillJob?.cancel()
        _uiState.update { state ->
            val current = state.weightInput.toDoubleOrNull() ?: 0.0
            val stepped = WeightConverter.step(current, state.captureUnit, increase)
            state.withWeightInput(format(stepped), state.captureUnit)
        }
    }

    fun onRepsChanged(value: String) {
        val error = value.toIntOrNull()?.let { parsed ->
            if (parsed < 1) {
                if (_uiState.value.isIsometric) {
                    context.getString(R.string.error_seconds_min)
                } else {
                    context.getString(R.string.error_reps_min)
                }
            } else {
                null
            }
        }
        _uiState.update { it.copy(reps = value, repsError = error) }
    }

    fun onRirSelected(rir: Int) {
        _uiState.update { it.copy(selectedRir = rir) }
    }

    fun onConfirm() {
        val state = _uiState.value
        val captureError = WeightCaptureValidator.validate(state.weightInput, state.captureUnit)
        val weightKg = state.convertedWeightKg
        val parsedReps = state.reps.toIntOrNull()

        if (captureError != null || weightKg == null) {
            _uiState.update {
                it.copy(
                    weightError = errorMessage(
                        captureError ?: WeightCaptureError.NotNumeric,
                        state.captureUnit,
                    ),
                )
            }
            return
        }
        if (parsedReps == null) {
            _uiState.update {
                it.copy(
                    repsError = if (state.isIsometric) {
                        context.getString(R.string.error_seconds_min)
                    } else {
                        context.getString(R.string.error_reps_min)
                    },
                )
            }
            return
        }
        if (state.selectedRir == null) return
        val equipmentTypeId = state.selectedEquipmentTypeId
        if (equipmentTypeId == null) {
            _uiState.update {
                it.copy(
                    equipmentError = context.getString(
                        R.string.register_set_equipment_error_required,
                    ),
                )
            }
            return
        }
        if (state.isAddedWeight && weightKg <= 0.0) {
            _uiState.update {
                it.copy(weightError = context.getString(R.string.error_weight_added_min))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                registerSetUseCase(
                    sessionExerciseId = sessionExerciseId,
                    weightKg = weightKg,
                    reps = parsedReps,
                    rir = state.selectedRir,
                    captureUnit = state.captureUnit,
                    equipmentTypeId = equipmentTypeId,
                    equipmentTypeName = state.selectedEquipmentName,
                )
                _navigateBack.emit(true)
            } catch (_: IllegalArgumentException) {
                _uiState.update {
                    it.copy(
                        weightError = errorMessage(
                            WeightCaptureValidator.validate(state.weightInput, state.captureUnit),
                            state.captureUnit,
                        ),
                        repsError = if (parsedReps < 1) {
                            if (state.isIsometric) {
                                context.getString(R.string.error_seconds_min)
                            } else {
                                context.getString(R.string.error_reps_min)
                            }
                        } else {
                            null
                        },
                    )
                }
            } catch (_: IllegalStateException) {
                _navigateBack.emit(true)
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun RegisterSetUiState.withWeightInput(
        value: String,
        unit: WeightUnit,
    ): RegisterSetUiState {
        val error = WeightCaptureValidator.validate(value, unit)
        return copy(
            weightInput = value,
            captureUnit = unit,
            weightError = errorMessage(error, unit),
            convertedWeightKg = if (error == null) convertedWeightKg(value, unit) else null,
        )
    }

    private fun convertedWeightKg(value: String, unit: WeightUnit): Double? {
        return value.toDoubleOrNull()?.let { WeightConverter.toKg(it, unit) }
    }

    private fun errorMessage(error: WeightCaptureError?, unit: WeightUnit): String? {
        return when (error) {
            null -> null
            WeightCaptureError.NotNumeric -> context.getString(R.string.error_weight_not_numeric)
            WeightCaptureError.Negative -> context.getString(R.string.error_weight_negative)
            WeightCaptureError.AboveMax -> when (unit) {
                WeightUnit.KG -> context.getString(R.string.error_weight_max_kg)
                WeightUnit.LB -> context.getString(
                    R.string.error_weight_max_lb_format,
                    format(WeightConverter.fromKg(WeightConverter.MAX_WEIGHT_KG, WeightUnit.LB)),
                )
            }
        }
    }

    private fun format(value: Double): String = String.format(Locale.US, "%.1f", value)
}
