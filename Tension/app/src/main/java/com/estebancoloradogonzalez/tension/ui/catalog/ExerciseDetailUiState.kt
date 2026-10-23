package com.estebancoloradogonzalez.tension.ui.catalog

import com.estebancoloradogonzalez.tension.domain.model.EquipmentType
import com.estebancoloradogonzalez.tension.domain.model.MovementCategory
import com.estebancoloradogonzalez.tension.domain.model.MuscleZone
import com.estebancoloradogonzalez.tension.domain.model.ProgressionDifficulty
import com.estebancoloradogonzalez.tension.domain.rules.RealLoadRule

sealed interface ExerciseDetailUiState {
    data object Loading : ExerciseDetailUiState
    data class Success(val exercise: ExerciseDetailItem) : ExerciseDetailUiState
    data class Error(val message: String) : ExerciseDetailUiState
}

data class ExerciseDetailItem(
    val id: Long,
    val name: String,
    /** Implementos que el ejercicio admite hoy, en orden de catálogo. */
    val equipmentTypes: List<String>,
    /** Catálogo completo, para poder añadir opciones desde la ficha. */
    val equipmentOptions: List<EquipmentType> = emptyList(),
    val selectedEquipmentIds: Set<Long> = emptySet(),
    /** Opciones que no se pueden retirar porque tienen series registradas (CA-39.10). */
    val equipmentWithSets: Set<Long> = emptySet(),
    val equipmentError: String? = null,
    /** Zonas que ejecutan el movimiento, para el bloque destacado de la ficha. */
    val primaryMuscleZones: List<String>,
    /** Zonas que asisten. Puede estar vacía. */
    val secondaryMuscleZones: List<String>,
    /** Catálogo completo, ya ordenado, para el diálogo de selección. */
    val muscleZoneOptions: List<MuscleZone> = emptyList(),
    val selectedPrimaryZoneIds: List<Long> = emptyList(),
    val selectedSecondaryZoneIds: List<Long> = emptyList(),
    val muscleZoneError: String? = null,
    val isCustom: Boolean,
    val mediaResource: String?,
    val progressionDifficulty: ProgressionDifficulty,
    val effectiveThresholdSessions: Int,
    /** Empuje, Jalón o Pierna (HU-46). Editable desde la ficha con la restricción de CA-46.14. */
    val movementCategory: MovementCategory? = null,
    /** Isometric exercises do not show the body-weight percentage (HU-49, CA-49.07). */
    val isIsometric: Boolean = false,
    /** Persisted share of body weight the exercise moves, 0 to 100 (HU-49). */
    val bodyWeightPercent: Int = 0,
) {
    val equipmentSummary: String get() = equipmentTypes.joinToString(" · ")
}

/**
 * The «Peso corporal movido» field while it is being edited (HU-49, CA-49.04, CA-49.05).
 *
 * It lives apart from the detail because it is not persisted data: Room re-emits the saved
 * percentage, not what is being typed. [isEdited] says whether the executant has touched it;
 * while untouched, the field follows the persisted value.
 */
data class BodyWeightPercentForm(
    val input: String,
    val isEdited: Boolean = false,
    val isSaving: Boolean = false,
) {
    /** Only an integer from 0 to 100 can be saved; anything else shows the inline error. */
    val isValid: Boolean get() = RealLoadRule.parsePercent(input) != null

    val canSave: Boolean get() = isValid && !isSaving
}

/** One-shot events of the exercise detail. */
sealed interface ExerciseDetailEvent {
    /** The percentage was saved: brief confirmation (HU-49). */
    data object BodyWeightPercentSaved : ExerciseDetailEvent

    /** The percentage could not be stored; the screen resolves the message. */
    data object BodyWeightPercentSaveFailed : ExerciseDetailEvent
}
