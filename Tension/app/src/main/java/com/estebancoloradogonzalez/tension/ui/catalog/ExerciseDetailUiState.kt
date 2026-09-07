package com.estebancoloradogonzalez.tension.ui.catalog

import com.estebancoloradogonzalez.tension.domain.model.EquipmentType
import com.estebancoloradogonzalez.tension.domain.model.ProgressionDifficulty

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
    val muscleZones: String,
    val isCustom: Boolean,
    val mediaResource: String?,
    val progressionDifficulty: ProgressionDifficulty,
    val effectiveThresholdSessions: Int,
) {
    val equipmentSummary: String get() = equipmentTypes.joinToString(" · ")
}
