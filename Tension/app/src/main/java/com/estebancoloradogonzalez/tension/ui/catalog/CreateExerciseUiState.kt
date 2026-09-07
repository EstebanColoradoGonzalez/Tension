package com.estebancoloradogonzalez.tension.ui.catalog

import com.estebancoloradogonzalez.tension.domain.model.EquipmentType
import com.estebancoloradogonzalez.tension.domain.model.MuscleZone
import com.estebancoloradogonzalez.tension.domain.model.ProgressionDifficulty

data class CreateExerciseUiState(
    val isLoading: Boolean = true,
    val name: String = "",
    val equipmentTypes: List<EquipmentType> = emptyList(),
    val selectedEquipmentTypeIds: Set<Long> = emptySet(),
    val muscleZones: List<MuscleZone> = emptyList(),
    val selectedMuscleZoneIds: Set<Long> = emptySet(),
    val isBodyweight: Boolean = false,
    val isIsometric: Boolean = false,
    val isToTechnicalFailure: Boolean = false,
    val progressionDifficulty: ProgressionDifficulty = ProgressionDifficulty.MEDIUM,
    val imageUri: String? = null,
    val isSaving: Boolean = false,
    val nameError: String? = null,
    val equipmentError: String? = null,
    val muscleZoneError: String? = null,
    val saveSuccess: Boolean = false,
    val saveError: String? = null,
) {
    val canSave: Boolean
        get() = name.isNotBlank() &&
            selectedEquipmentTypeIds.isNotEmpty() &&
            selectedMuscleZoneIds.isNotEmpty() &&
            !isSaving
}
