package com.estebancoloradogonzalez.tension.domain.model

data class Routine(
    val id: Long,
    val name: String,
    val sortOrder: Int,
    val createdAt: String,
    /**
     * Categoría de la rutina, independiente de su nombre libre (HU-46). Es la categoría del
     * día de toda sesión que ejecute esta rutina. `null` solo ante un código no reconocido.
     */
    val movementCategory: MovementCategory? = null,
)
