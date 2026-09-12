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
    /** Zonas que ejecutan el movimiento. Obligatorio: al menos una (CA-41.09). */
    val primaryMuscleZoneIds: List<Long> = emptyList(),
    /** Zonas que asisten. Opcional, y nunca comparte una zona con las principales. */
    val secondaryMuscleZoneIds: List<Long> = emptyList(),
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
    /**
     * El formulario se abrió desde la sesión activa: al guardar, el ejercicio queda en el
     * Diccionario **y** añadido a la sesión, en un solo gesto (CA-43.02).
     */
    val isFromSession: Boolean = false,
) {
    /**
     * El botón permanece deshabilitado mientras no haya una zona principal (CA-41.09).
     * Las secundarias no entran: son opcionales.
     */
    val canSave: Boolean
        get() = name.isNotBlank() &&
            selectedEquipmentTypeIds.isNotEmpty() &&
            primaryMuscleZoneIds.isNotEmpty() &&
            !isSaving
}
