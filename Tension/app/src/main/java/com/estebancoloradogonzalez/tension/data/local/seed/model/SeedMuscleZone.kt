package com.estebancoloradogonzalez.tension.data.local.seed.model

/**
 * Zona muscular del catálogo base.
 *
 * Estructura pura sin dependencias de Android, igual que [SeedEquipmentType]: la tabla
 * cerrada de CA-41.01 queda verificable por tests JVM y `BaseDataSeeder` se limita a
 * mapearla.
 *
 * [sortOrder] es la posición declarada en `MuscleZoneCatalog.ALL`. Existe porque el
 * identificador no puede encodar el orden —lo encoda la historia del catálogo, ya que un
 * renombrado conserva su id (HU-29)— y el selector de zonas necesita el orden anatómico,
 * no el alfabético ni el de creación.
 */
data class SeedMuscleZone(
    val id: Long,
    val name: String,
    val muscleGroup: String,
    val sortOrder: Int,
)
