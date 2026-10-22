package com.estebancoloradogonzalez.tension.data.local.seed.model

import com.estebancoloradogonzalez.tension.domain.model.MovementCategory

/**
 * Rutina del plan de entrenamiento predeterminado.
 *
 * Cada rutina del seed tiene exactamente una versión (`version_number = 1`),
 * derivada por `PlanSeeder` a partir de este listado.
 */
data class SeedRoutine(
    val id: Long,
    val name: String,
    val sortOrder: Int,
    /** Categoría de la rutina, independiente del nombre (HU-46, CA-46.09). */
    val movementCategory: MovementCategory,
)
