package com.estebancoloradogonzalez.tension.domain.model

data class NextSession(
    val routineId: Long,
    val routineName: String,
    val versionNumber: Int,
    val routineVersionId: Long,
)
