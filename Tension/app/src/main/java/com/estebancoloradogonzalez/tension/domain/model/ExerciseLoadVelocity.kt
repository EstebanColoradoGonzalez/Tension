package com.estebancoloradogonzalez.tension.domain.model

data class ExerciseLoadVelocity(
    val exerciseId: Long,
    val exerciseName: String,
    val velocity: Double,
    val isBodyweight: Boolean,
)
