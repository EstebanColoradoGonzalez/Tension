package com.estebancoloradogonzalez.tension.data.local.seed.model

/**
 * Tipo de equipamiento del catálogo base.
 *
 * Estructura pura sin dependencias de Android, igual que [SeedExercise]: la tabla cerrada
 * de CA-39.01 queda verificable por tests JVM y `BaseDataSeeder` se limita a mapearla.
 */
data class SeedEquipmentType(
    val id: Long,
    val name: String,
)
