package com.estebancoloradogonzalez.tension.domain.model

data class PlanExercise(
    val exerciseId: Long,
    val name: String,
    val equipmentTypes: List<String>,
    /** Zonas que ejecutan el movimiento. Nunca vacía. */
    val primaryMuscleZones: List<String>,
    /** Zonas que asisten. Puede estar vacía. */
    val secondaryMuscleZones: List<String>,
    /** Implemento que el plan sugiere para este puesto. Nunca nulo (CA-41.07). */
    val suggestedEquipmentTypeId: Long,
    val suggestedEquipmentName: String,
    val sets: Int,
    val reps: String,
    val isBodyweight: Boolean,
    val isIsometric: Boolean,
    val isToTechnicalFailure: Boolean,
    val isCustom: Boolean,
    val slot: Int = 0,
) {
    /** Todas las zonas, principales primero. */
    val muscleZones: List<String> get() = primaryMuscleZones + secondaryMuscleZones
}
