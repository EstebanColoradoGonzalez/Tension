package com.estebancoloradogonzalez.tension.data.local.seed.model

/**
 * Asignación de un ejercicio a una versión de rutina del plan predeterminado.
 *
 * Dos asignaciones que comparten [slot] dentro de la misma [routineVersionId] forman
 * un slot dual: el de menor [sortOrder] es el primario y el otro su alternativa.
 */
data class SeedAssignment(
    val routineVersionId: Long,
    val exerciseId: Long,
    val sets: Int,
    val reps: String,
    val sortOrder: Int,
    val slot: Int,
)
