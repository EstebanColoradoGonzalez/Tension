package com.estebancoloradogonzalez.tension.domain.model

data class PlanVersionDetail(
    val routineVersionId: Long,
    val routineName: String,
    val versionNumber: Int,
    val exercises: List<PlanExercise>,
)
