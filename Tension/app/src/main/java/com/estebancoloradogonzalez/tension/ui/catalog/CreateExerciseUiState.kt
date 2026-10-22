package com.estebancoloradogonzalez.tension.ui.catalog

import com.estebancoloradogonzalez.tension.domain.model.EquipmentType
import com.estebancoloradogonzalez.tension.domain.model.MovementCategory
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
    /**
     * Empuje, Jalón o Pierna (HU-46). Nula hasta que se elige: no hay preselección al crear
     * desde el Diccionario (CA-46.13). Desde la sesión llega con la del día (CA-46.11).
     */
    val movementCategory: MovementCategory? = null,
    /** La categoría llegó fijada por la sesión y no se puede cambiar (CA-46.11). */
    val isCategoryFixed: Boolean = false,
    val categoryError: String? = null,
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
     *
     * La categoría **no** entra a propósito: guardar sin ella tiene que llegar al
     * ViewModel para que diga «elige la categoría» junto al campo (CA-46.13). Un botón
     * deshabilitado no explica qué falta.
     */
    val canSave: Boolean
        get() = name.isNotBlank() &&
            selectedEquipmentTypeIds.isNotEmpty() &&
            primaryMuscleZoneIds.isNotEmpty() &&
            !isSaving
}
