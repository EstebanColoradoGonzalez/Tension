package com.estebancoloradogonzalez.tension.domain.model

data class ClassificationCountByGroup(
    val muscleGroup: String,
    val positiveCount: Int,
    val totalCount: Int,
)
