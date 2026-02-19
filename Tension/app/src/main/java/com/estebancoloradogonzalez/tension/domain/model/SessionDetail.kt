package com.estebancoloradogonzalez.tension.domain.model

data class SessionDetail(
    val sessionId: Long,
    val date: String,
    val moduleCode: String,
    val versionNumber: Int,
    val status: String,
    val totalTonnageKg: Double,
    val totalExercises: Int,
    val completedExercises: Int,
    val exercises: List<SessionDetailExercise>,
)
