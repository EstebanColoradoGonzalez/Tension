package com.estebancoloradogonzalez.tension.domain.model

data class ExerciseSessionRange(
    val exerciseId: Long,
    val exerciseName: String,
    val isBodyweight: Int,
    val isIsometric: Int,
    val firstSessionId: Long,
    val lastSessionId: Long,
    val sessionCount: Int,
)
