package com.estebancoloradogonzalez.tension.domain.model

data class ExerciseProgressionRate(
    val exerciseId: Long,
    val exerciseName: String,
    val rate: Double,
    val isBodyweight: Boolean,
)
