package com.estebancoloradogonzalez.tension.domain.model

import com.estebancoloradogonzalez.tension.domain.rules.SetRangeNotice

data class RegisterSetInfo(
    val sessionExerciseId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val currentSetNumber: Int,
    val totalSets: Int,
    val lastWeightKg: Double?,
    val isBodyweight: Boolean,
    val isIsometric: Boolean,
    val isToTechnicalFailure: Boolean,
    val prescribedReps: String,
    val captureUnit: WeightUnit,
    /** Opciones que el ejercicio admite, en orden de catálogo. Nunca vacío. */
    val equipmentOptions: List<EquipmentType>,
    /**
     * Implemento con el que nace el selector, resuelto por la precedencia de
     * [PreselectionOrigin] (CA-41.05, CA-39.04).
     */
    val preselectedEquipmentTypeId: Long,
    /** Por qué ese implemento y no otro. Gobierna el rótulo bajo el selector. */
    val preselectionOrigin: PreselectionOrigin,
    /**
     * Aviso de la serie anterior de este ejercicio en la sesión, para pintarlo sobre el
     * formulario de la siguiente (CA-46.05): «baja el peso» se lee justo cuando se elige el
     * peso. `null` en la primera serie o si la anterior no dejó aviso.
     */
    val previousSetNotice: SetRangeNotice? = null,
)
