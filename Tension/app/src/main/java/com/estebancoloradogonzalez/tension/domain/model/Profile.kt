package com.estebancoloradogonzalez.tension.domain.model

import java.time.LocalDate

data class Profile(
    val currentWeightKg: Double,
    val heightM: Double,
    val weeklyFrequency: Int,
    val plateauBaseThreshold: Int,
    val createdAt: LocalDate,
)
