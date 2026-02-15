package com.estebancoloradogonzalez.tension.domain.model

data class SubstituteExerciseInfo(
    val sessionExerciseId: Long,
    val currentExerciseId: Long,
    val currentExerciseName: String,
    val moduleCode: String,
    val sessionId: Long,
)
