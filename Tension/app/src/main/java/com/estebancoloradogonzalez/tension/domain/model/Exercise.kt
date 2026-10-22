package com.estebancoloradogonzalez.tension.domain.model

data class Exercise(
    val id: Long,
    val name: String,
    /**
     * Implementos con los que el ejercicio se puede hacer, en orden de catálogo. Nunca
     * vacío.
     *
     * Llevan el identificador y no solo el nombre porque el selector de equipamiento
     * sugerido del plan tiene que **identificar** la opción elegida, no solo mostrarla, y
     * emparejar por nombre rompería el día que dos implementos se llamen parecido.
     */
    val equipmentOptions: List<EquipmentType>,
    /** Zonas que ejecutan el movimiento. Nunca vacía: todo ejercicio tiene al menos una. */
    val primaryMuscleZones: List<String>,
    /** Zonas que asisten. Puede estar vacía. */
    val secondaryMuscleZones: List<String>,
    /** Grupo de agregación: el de la primera zona principal. */
    val muscleGroup: String?,
    val isBodyweight: Boolean,
    val isIsometric: Boolean,
    val isToTechnicalFailure: Boolean,
    val isCustom: Boolean,
    val mediaResource: String?,
    val progressionDifficulty: ProgressionDifficulty,
    /**
     * Empuje, Jalón o Pierna (HU-46). La base la exige `NOT NULL`; `null` aquí solo
     * significaría un código persistido que no se reconoce, y entonces el ejercicio no se
     * ofrece en ninguna sesión ni rutina en lugar de caer en una categoría inventada.
     */
    val movementCategory: MovementCategory? = null,
) {
    /** Nombres de los implementos, para las pantallas que solo los enseñan. */
    val equipmentTypes: List<String> get() = equipmentOptions.map { it.name }

    /** Todas las zonas, principales primero. Para las lecturas que no distinguen jerarquía. */
    val muscleZones: List<String> get() = primaryMuscleZones + secondaryMuscleZones
}
