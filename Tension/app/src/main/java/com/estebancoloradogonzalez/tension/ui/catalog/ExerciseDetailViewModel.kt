package com.estebancoloradogonzalez.tension.ui.catalog

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.data.local.storage.ImageStorageHelper
import com.estebancoloradogonzalez.tension.domain.model.ProgressionDifficulty
import com.estebancoloradogonzalez.tension.domain.rules.PlateauThresholdRule
import com.estebancoloradogonzalez.tension.domain.usecase.catalog.AddExerciseEquipmentUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.catalog.GetAllFilterOptionsUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.catalog.GetExerciseDetailUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.catalog.GetExerciseEquipmentIdsUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.catalog.GetExerciseEquipmentWithSetsUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.catalog.RemoveExerciseEquipmentUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.catalog.UpdateExerciseImageUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.catalog.UpdateExerciseProgressionDifficultyUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.profile.GetProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getExerciseDetailUseCase: GetExerciseDetailUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val getAllFilterOptionsUseCase: GetAllFilterOptionsUseCase,
    private val getExerciseEquipmentIdsUseCase: GetExerciseEquipmentIdsUseCase,
    private val getExerciseEquipmentWithSetsUseCase: GetExerciseEquipmentWithSetsUseCase,
    private val updateExerciseImageUseCase: UpdateExerciseImageUseCase,
    private val updateExerciseProgressionDifficultyUseCase: UpdateExerciseProgressionDifficultyUseCase,
    private val addExerciseEquipmentUseCase: AddExerciseEquipmentUseCase,
    private val removeExerciseEquipmentUseCase: RemoveExerciseEquipmentUseCase,
    private val imageStorageHelper: ImageStorageHelper,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val exerciseId: Long = checkNotNull(savedStateHandle["exerciseId"])

    private val _uiState = MutableStateFlow<ExerciseDetailUiState>(ExerciseDetailUiState.Loading)
    val uiState: StateFlow<ExerciseDetailUiState> = _uiState.asStateFlow()

    /**
     * Error del campo de equipamiento.
     *
     * Vive fuera del `combine` porque no es un dato persistido: es la respuesta a un
     * intento rechazado, y el flujo de Room —que no cambia cuando el rechazo evita la
     * escritura— no lo repondría en la siguiente emisión.
     */
    private val _equipmentError = MutableStateFlow<String?>(null)

    init {
        loadExerciseDetail()
    }

    private fun loadExerciseDetail() {
        viewModelScope.launch {
            combine(
                getExerciseDetailUseCase(exerciseId),
                getProfileUseCase(),
                getAllFilterOptionsUseCase(),
                getExerciseEquipmentIdsUseCase(exerciseId),
                _equipmentError,
            ) { exercise, profile, filterOptions, equipmentIds, equipmentError ->
                val baseThreshold = profile?.plateauBaseThreshold
                    ?: PlateauThresholdRule.DEFAULT_BASE_THRESHOLD
                if (exercise == null) {
                    ExerciseDetailUiState.Error(
                        context.getString(R.string.exercise_detail_not_found),
                    )
                } else {
                    ExerciseDetailUiState.Success(
                        exercise = ExerciseDetailItem(
                            id = exercise.id,
                            name = exercise.name,
                            equipmentTypes = exercise.equipmentTypes,
                            equipmentOptions = filterOptions.equipmentTypes,
                            selectedEquipmentIds = equipmentIds.toSet(),
                            equipmentWithSets = getExerciseEquipmentWithSetsUseCase(
                                exerciseId,
                                equipmentIds,
                            ),
                            equipmentError = equipmentError,
                            muscleZones = exercise.muscleZones.joinToString(", "),
                            isCustom = exercise.isCustom,
                            mediaResource = exercise.mediaResource,
                            progressionDifficulty = exercise.progressionDifficulty,
                            effectiveThresholdSessions = PlateauThresholdRule.effectiveThreshold(
                                baseThreshold,
                                exercise.progressionDifficulty,
                            ),
                        ),
                    )
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onImageSelected(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            val savedPath = imageStorageHelper.saveImageToInternal(uri)
            if (savedPath != null) {
                val currentState = _uiState.value
                if (currentState is ExerciseDetailUiState.Success) {
                    imageStorageHelper.deleteImageIfInternal(currentState.exercise.mediaResource)
                }
                updateExerciseImageUseCase(exerciseId, savedPath)
            }
        }
    }

    /**
     * Persists the difficulty immediately, following the same pattern as the exercise
     * image: this screen has no save button, and the Room flow re-emits the detail.
     */
    fun onProgressionDifficultySelected(difficulty: ProgressionDifficulty) {
        viewModelScope.launch {
            updateExerciseProgressionDifficultyUseCase(exerciseId, difficulty)
        }
    }

    /**
     * Añade o retira un implemento, persistiendo al instante — el mismo patrón que la
     * imagen y la dificultad.
     *
     * Retirar valida **antes** de escribir: si es la última opción (CA-39.09) o si tiene
     * series registradas (CA-39.10) no se escribe nada y la casilla vuelve a su sitio,
     * porque el estado se deriva del flujo de Room y ese flujo no cambió.
     */
    fun onEquipmentToggled(equipmentTypeId: Long) {
        val state = _uiState.value
        if (state !is ExerciseDetailUiState.Success) return
        val isSelected = equipmentTypeId in state.exercise.selectedEquipmentIds

        viewModelScope.launch {
            _equipmentError.value = null
            if (!isSelected) {
                addExerciseEquipmentUseCase(exerciseId, equipmentTypeId)
                return@launch
            }
            when (removeExerciseEquipmentUseCase(exerciseId, equipmentTypeId)) {
                is RemoveExerciseEquipmentUseCase.Result.Removed -> Unit
                is RemoveExerciseEquipmentUseCase.Result.LastOption ->
                    _equipmentError.value = context.getString(
                        R.string.exercise_equipment_error_last_option,
                    )
                is RemoveExerciseEquipmentUseCase.Result.HasRegisteredSets -> {
                    val name = state.exercise.equipmentOptions
                        .firstOrNull { it.id == equipmentTypeId }?.name.orEmpty()
                    _equipmentError.value = context.getString(
                        R.string.exercise_equipment_error_has_sets_format,
                        name,
                    )
                }
            }
        }
    }

    fun onEquipmentErrorDismissed() {
        _equipmentError.value = null
    }
}
