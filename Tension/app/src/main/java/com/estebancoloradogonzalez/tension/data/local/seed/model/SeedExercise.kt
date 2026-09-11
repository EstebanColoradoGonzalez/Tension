package com.estebancoloradogonzalez.tension.data.local.seed.model

import com.estebancoloradogonzalez.tension.domain.model.ProgressionDifficulty

/**
 * Ejercicio del catálogo base precargado en instalación fresca.
 *
 * Estructura pura sin dependencias de Android: los datos semilla son verificables
 * por tests JVM y `ExerciseSeeder` se limita a mapearlos a `ContentValues`.
 */
data class SeedExercise(
    val id: Long,
    val name: String,
    /**
     * Implementos admitidos, en el orden declarado por el catálogo. Nunca vacío: un
     * ejercicio sin equipamiento no puede existir (HU-39).
     */
    val equipmentTypeIds: List<Long>,
    /**
     * Zonas que **ejecutan** el movimiento. Nunca vacía: todo ejercicio tiene al menos una
     * principal (CA-41.02). Varias son legítimas cuando el movimiento reparte el trabajo
     * por igual.
     */
    val primaryMuscleZoneIds: List<Long>,
    /**
     * Zonas que **asisten** al movimiento. Puede estar vacía, y nunca comparte una zona
     * con [primaryMuscleZoneIds] (CA-41.09).
     */
    val secondaryMuscleZoneIds: List<Long>,
    val mediaResource: String,
    val isBodyweight: Boolean = false,
    val isIsometric: Boolean = false,
    val isToTechnicalFailure: Boolean = false,
    val progressionDifficulty: ProgressionDifficulty = ProgressionDifficulty.MEDIUM,
) {
    /** Todas las zonas del ejercicio, principales primero. Para las lecturas que no distinguen. */
    val allMuscleZoneIds: List<Long> get() = primaryMuscleZoneIds + secondaryMuscleZoneIds
}
