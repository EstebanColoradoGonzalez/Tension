package com.estebancoloradogonzalez.tension.domain.model

data class PlanVersionDetail(
    val moduleVersionId: Long,
    val moduleCode: String,
    val moduleName: String,
    val versionNumber: Int,
    val exercises: List<PlanExercise>,
)
