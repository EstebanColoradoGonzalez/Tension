package com.estebancoloradogonzalez.tension.domain.model

import java.time.LocalDate

data class WeightRecord(
    val id: Long,
    val weightKg: Double,
    val date: LocalDate,
)
