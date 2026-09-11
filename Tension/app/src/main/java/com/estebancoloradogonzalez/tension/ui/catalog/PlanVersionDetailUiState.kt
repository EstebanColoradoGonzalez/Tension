package com.estebancoloradogonzalez.tension.ui.catalog

import com.estebancoloradogonzalez.tension.domain.model.EquipmentType

data class PlanVersionDetailUiState(
    val isLoading: Boolean = true,
    val routineName: String = "",
    val versionNumber: Int = 0,
    val exercises: List<PlanExerciseItem> = emptyList(),
)

data class PlanExerciseItem(
    val exerciseId: Long,
    val name: String,
    val equipmentSummary: String,
    /** Implemento que el plan sugiere para este puesto. Nunca vacío (CA-41.07). */
    val suggestedEquipmentName: String,
    val suggestedEquipmentTypeId: Long,
    val muscleZonesSummary: String,
    val sets: Int,
    val repsDisplay: String,
    val repsRaw: String,
    val isSpecialCondition: Boolean,
    val isCustom: Boolean,
    val isBodyweight: Boolean = false,
    val slot: Int = 0,
    /**
     * Alternativas del mismo puesto, cada una **con su propia sugerencia**: comparten
     * puesto, series y repeticiones, pero no implemento (CA-41.05).
     */
    val alternatives: List<PlanAlternativeItem> = emptyList(),
)

data class PlanAlternativeItem(
    val exerciseId: Long,
    val name: String,
    val suggestedEquipmentName: String,
    val suggestedEquipmentTypeId: Long,
)

data class AssignExerciseSheetState(
    val isVisible: Boolean = false,
    val availableExercises: List<AssignableExerciseItem> = emptyList(),
    val selectedExerciseId: Long? = null,
    val sets: String = "4",
    val reps: String = "8-12",
    /** Obligatorio: sin sugerencia no se persiste la asignación (CA-41.08). */
    val selectedSuggestedEquipmentId: Long? = null,
    val isAssigning: Boolean = false,
) {
    /** Opciones del ejercicio elegido — solo entre esas puede estar la sugerencia. */
    val equipmentOptionsForSelection: List<EquipmentType>
        get() = availableExercises
            .firstOrNull { it.id == selectedExerciseId }
            ?.equipmentOptions
            .orEmpty()

    val canAssign: Boolean
        get() = selectedExerciseId != null &&
            selectedSuggestedEquipmentId != null &&
            !isAssigning
}

data class AssignableExerciseItem(
    val id: Long,
    val name: String,
    val equipmentSummary: String,
    val muscleZonesSummary: String,
    /** Implementos que admite, para acotar el selector de sugerencia (CA-41.08). */
    val equipmentOptions: List<EquipmentType> = emptyList(),
)

data class EditPlanAssignmentState(
    val isVisible: Boolean = false,
    val exerciseId: Long = 0,
    val exerciseName: String = "",
    val sets: Int = 4,
    val reps: String = "8-12",
    val suggestedEquipmentTypeId: Long = 0,
    /** Solo los implementos que el ejercicio admite (CA-41.08). */
    val equipmentOptions: List<EquipmentType> = emptyList(),
    val isSaving: Boolean = false,
)

data class AddAlternativeSheetState(
    val isVisible: Boolean = false,
    val slot: Int = 0,
    val slotName: String = "",
    val availableExercises: List<AssignableExerciseItem> = emptyList(),
    val selectedExerciseId: Long? = null,
    /** La alternativa hereda series y repeticiones del puesto, pero no el implemento. */
    val selectedSuggestedEquipmentId: Long? = null,
    val isAssigning: Boolean = false,
) {
    val equipmentOptionsForSelection: List<EquipmentType>
        get() = availableExercises
            .firstOrNull { it.id == selectedExerciseId }
            ?.equipmentOptions
            .orEmpty()

    val canAssign: Boolean
        get() = selectedExerciseId != null &&
            selectedSuggestedEquipmentId != null &&
            !isAssigning
}
