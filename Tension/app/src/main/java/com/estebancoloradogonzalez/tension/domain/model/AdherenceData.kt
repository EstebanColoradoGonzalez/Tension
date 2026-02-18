package com.estebancoloradogonzalez.tension.domain.model

data class AdherenceData(
    val completedSessions: Int,
    val plannedSessions: Int,
    val percentage: Double,
)
