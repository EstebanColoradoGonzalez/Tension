package com.estebancoloradogonzalez.tension.domain.model

data class PlanVersionDetail(
    val routineVersionId: Long,
    val routineName: String,
    val versionNumber: Int,
    val exercises: List<PlanExercise>,
    /** Categoría de la rutina: la que acota qué ejercicios admite la versión (HU-46). */
    val movementCategory: MovementCategory? = null,
)
