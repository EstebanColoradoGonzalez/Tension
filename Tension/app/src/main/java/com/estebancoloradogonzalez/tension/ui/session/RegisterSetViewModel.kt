package com.estebancoloradogonzalez.tension.ui.session

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.domain.model.CorrectableSet
import com.estebancoloradogonzalez.tension.domain.model.EquipmentType
import com.estebancoloradogonzalez.tension.domain.model.WeightUnit
import com.estebancoloradogonzalez.tension.domain.rules.ExternalLoadRule
import com.estebancoloradogonzalez.tension.domain.rules.SetRangeNotice
import com.estebancoloradogonzalez.tension.domain.usecase.session.CorrectSetUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.GetCorrectableSetUseCase
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

/**
 * Result of registering a set: the live notice, if any (CA-46.05). When correcting,
 * the short confirmation that the set was corrected (CA-47.04).
 */
data class SetRegistered(val notice: String?)

@HiltViewModel
class RegisterSetViewModel @Inject constructor(
    private val getRegisterSetInfoUseCase: GetRegisterSetInfoUseCase,
    private val getPrefilledLoadForEquipmentUseCase: GetPrefilledLoadForEquipmentUseCase,
    private val registerSetUseCase: RegisterSetUseCase,
    private val getCorrectableSetUseCase: GetCorrectableSetUseCase,
    private val correctSetUseCase: CorrectSetUseCase,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val sessionExerciseId: Long = savedStateHandle.get<Long>("sessionExerciseId") ?: 0L

    /**
     * Correction mode (HU-47): the same form, preloaded with the last registered set. The
     * navigation argument decides it, not the session state: the window is checked again on
     * load and once more inside the transaction that writes.
     */
    private val isCorrection: Boolean = savedStateHandle.get<Boolean>(CORRECTION_ARG) ?: false

    /** The set being corrected. Only in correction mode, once loaded. */
    private var correctingSet: CorrectableSet? = null

    private val _uiState = MutableStateFlow(RegisterSetUiState())
    val uiState: StateFlow<RegisterSetUiState> = _uiState.asStateFlow()

    private val _navigateBack = MutableSharedFlow<SetRegistered>(replay = 0)

    /**
     * Vuelta a la sesión. Lleva el aviso en directo de la serie recién registrada (HU-46,
     * CA-46.05): la pantalla de registro se cierra al guardar, así que el aviso se muestra
     * donde el ejecutante aterriza, en ese mismo momento.
     */
    val navigateBack: SharedFlow<SetRegistered> = _navigateBack.asSharedFlow()

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
            if (isCorrection) loadCorrection() else loadRegistration()
        }
    }

    /**
     * Preloads the set to correct with what was registered (CA-47.03).
     *
     * - The implement arrives as the **only option**: the selector draws it resolved and
     *   without interaction, which is exactly «visible but not editable».
     * - No chronometer: an already registered number is corrected by typing it, not timed
     *   again; an isometric shows the «Segundos sostenidos» field (CA-47.09).
     * - No previous-set notice: the form is not the one for the next set.
     */
    private suspend fun loadCorrection() {
        val set = getCorrectableSetUseCase(sessionExerciseId)
        if (set == null) {
            _uiState.update { it.copy(isLoading = false, isCorrectionWindowClosed = true) }
            return
        }
        correctingSet = set

        val captureEnabled = ExternalLoadRule.isCaptureEnabled(
            isBodyweight = set.isBodyweight,
            isIsometric = set.isIsometric,
            equipmentName = set.equipmentTypeName,
        )
        val unit = if (captureEnabled) set.captureUnit else WeightUnit.KG
        val weightInput = when {
            !captureEnabled -> "0"
            // In kilograms the stored value is shown as is (2 decimals): rounding it to one,
            // as the registration prefill does, would turn a 22.25 kg set into 22.3 when only
            // its reps were corrected, without anyone touching the weight.
            unit == WeightUnit.KG -> formatRegistered(set.weightKg)
            else -> format(WeightConverter.fromKg(set.weightKg, unit))
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                isCorrection = true,
                exerciseName = set.exerciseName,
                currentSetNumber = set.setNumber,
                totalSets = set.totalSets,
                weightInput = weightInput,
                captureUnit = unit,
                convertedWeightKg = convertedWeightKg(weightInput, unit),
                reps = set.reps.toString(),
                selectedRir = set.rir,
                isIsometric = set.isIsometric,
                isBodyweight = set.isBodyweight,
                equipmentOptions = listOf(
                    EquipmentType(id = set.equipmentTypeId, name = set.equipmentTypeName),
                ),
                selectedEquipmentTypeId = set.equipmentTypeId,
                preselectionOrigin = null,
                showChronometer = false,
                registeredValues = RegisteredSetValues(
                    weight = weightInput,
                    unit = unit,
                    reps = set.reps,
                    rir = set.rir,
                ),
            )
        }
    }

    private suspend fun loadRegistration() {
        val info = getRegisterSetInfoUseCase(sessionExerciseId) ?: return

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
                previousSetNotice = info.previousSetNotice?.let { noticeText(it) },
            )
        }
    }

    /**
     * Texto del aviso en directo (CA-46.05). Uno solo para la vuelta a la sesión y para el
     * formulario de la serie siguiente, de modo que no puedan decir cosas distintas.
     */
    private fun noticeText(notice: SetRangeNotice): String = when (notice) {
        is SetRangeNotice.LowerNextSet -> context.getString(R.string.set_notice_lower_next)
        is SetRangeNotice.TopOfRange ->
            context.getString(R.string.set_notice_top_of_range, notice.sessionNumber)
        is SetRangeNotice.IncreaseDue -> context.getString(R.string.set_notice_increase_due)
        is SetRangeNotice.AddWeightDue -> context.getString(R.string.set_notice_add_weight_due)
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
        // The implement of a registered set is not correctable (CA-47.03).
        if (isCorrection) return
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
                val correcting = correctingSet
                val message = if (correcting != null) {
                    // Same validations as the registration, up to here and inside the use
                    // case (CA-47.06). Only what gets persisted changes.
                    correctSetUseCase(
                        sessionExerciseId = sessionExerciseId,
                        setId = correcting.setId,
                        weightKg = weightKg,
                        reps = parsedReps,
                        rir = state.selectedRir,
                        captureUnit = state.captureUnit,
                        equipmentTypeName = correcting.equipmentTypeName,
                    )
                    context.getString(R.string.correct_set_done_format, correcting.setNumber)
                } else {
                    registerSetUseCase(
                        sessionExerciseId = sessionExerciseId,
                        weightKg = weightKg,
                        reps = parsedReps,
                        rir = state.selectedRir,
                        captureUnit = state.captureUnit,
                        equipmentTypeId = equipmentTypeId,
                        equipmentTypeName = state.selectedEquipmentName,
                    )?.let { noticeText(it) }
                }
                _navigateBack.emit(SetRegistered(notice = message))
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
                _navigateBack.emit(SetRegistered(notice = null))
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

    /** Like [format], but keeping the second decimal when the stored value has one. */
    private fun formatRegistered(value: Double): String {
        val oneDecimal = format(value)
        return if (oneDecimal.toDouble() == value) oneDecimal else String.format(Locale.US, "%.2f", value)
    }

    companion object {
        /** Navigation argument of the correction mode (HU-47). */
        const val CORRECTION_ARG = "correction"
    }
}
