package com.estebancoloradogonzalez.tension.domain.model

data class ActiveSession(
    val sessionId: Long,
    val moduleCode: String,
    val versionNumber: Int,
    val totalExercises: Int,
    val completedExercises: Int,
)
