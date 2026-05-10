package com.estebancoloradogonzalez.tension.domain.model

data class RoutineVersion(
    val id: Long,
    val routineId: Long,
    val versionNumber: Int,
    val exerciseCount: Int,
)
