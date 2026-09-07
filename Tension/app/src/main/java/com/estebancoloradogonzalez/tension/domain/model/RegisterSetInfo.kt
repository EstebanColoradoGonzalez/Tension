package com.estebancoloradogonzalez.tension.domain.model

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
     * Implemento de la última serie registrada del ejercicio, o la primera opción
     * admitida cuando no hay ninguna (CA-39.04).
     */
    val preselectedEquipmentTypeId: Long,
)
