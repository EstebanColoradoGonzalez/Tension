package com.estebancoloradogonzalez.tension.data.local.seed.model

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
)
