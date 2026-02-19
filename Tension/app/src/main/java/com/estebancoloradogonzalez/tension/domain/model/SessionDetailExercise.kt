package com.estebancoloradogonzalez.tension.domain.model

data class SessionDetailExercise(
    val exerciseId: Long,
    val exerciseName: String,
    val classification: ProgressionClassification?,
    val originalExerciseName: String?,
    val sets: List<SetData>,
)
