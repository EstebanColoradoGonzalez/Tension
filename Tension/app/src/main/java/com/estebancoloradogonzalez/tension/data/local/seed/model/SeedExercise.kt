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
    val muscleZoneIds: List<Long>,
    val mediaResource: String,
    val isBodyweight: Boolean = false,
    val isIsometric: Boolean = false,
    val isToTechnicalFailure: Boolean = false,
    val progressionDifficulty: ProgressionDifficulty = ProgressionDifficulty.MEDIUM,
)
