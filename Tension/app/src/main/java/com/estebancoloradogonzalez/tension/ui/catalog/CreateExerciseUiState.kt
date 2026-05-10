package com.estebancoloradogonzalez.tension.ui.catalog

import com.estebancoloradogonzalez.tension.domain.model.EquipmentType
import com.estebancoloradogonzalez.tension.domain.model.MuscleZone

data class CreateExerciseUiState(
    val isLoading: Boolean = true,
    val name: String = "",
    val equipmentTypes: List<EquipmentType> = emptyList(),
    val selectedEquipmentTypeId: Long? = null,
    val muscleZones: List<MuscleZone> = emptyList(),
    val selectedMuscleZoneIds: Set<Long> = emptySet(),
    val isBodyweight: Boolean = false,
    val isIsometric: Boolean = false,
    val isToTechnicalFailure: Boolean = false,
    val imageUri: String? = null,
    val isSaving: Boolean = false,
    val nameError: String? = null,
    val equipmentError: String? = null,
    val muscleZoneError: String? = null,
    val saveSuccess: Boolean = false,
    val saveError: String? = null,
) {
    val selectedEquipmentName: String
        get() = equipmentTypes.find { it.id == selectedEquipmentTypeId }?.name ?: ""

    val canSave: Boolean
        get() = name.isNotBlank() &&
            selectedEquipmentTypeId != null &&
            selectedMuscleZoneIds.isNotEmpty() &&
            !isSaving
}
