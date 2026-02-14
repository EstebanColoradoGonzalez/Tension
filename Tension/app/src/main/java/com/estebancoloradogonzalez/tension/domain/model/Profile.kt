package com.estebancoloradogonzalez.tension.domain.model

import java.time.LocalDate

data class Profile(
    val currentWeightKg: Double,
    val heightM: Double,
    val experienceLevel: ExperienceLevel,
    val weeklyFrequency: Int,
    val createdAt: LocalDate,
)
