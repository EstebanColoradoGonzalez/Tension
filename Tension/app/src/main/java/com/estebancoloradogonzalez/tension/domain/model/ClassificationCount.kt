package com.estebancoloradogonzalez.tension.domain.model

data class ClassificationCount(
    val exerciseId: Long,
    val exerciseName: String,
    val isBodyweight: Int,
    val progressionDifficulty: String,
    val positiveCount: Int,
    val totalCount: Int,
)
