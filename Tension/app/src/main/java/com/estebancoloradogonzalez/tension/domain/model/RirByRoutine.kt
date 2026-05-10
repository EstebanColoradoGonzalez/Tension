package com.estebancoloradogonzalez.tension.domain.model

data class RirByRoutine(
    val routineId: Long,
    val routineName: String,
    val averageRir: Double?,
    val interpretation: RirInterpretation?,
)

enum class RirInterpretation {
    OPTIMAL,
    RISK_TOO_CLOSE,
    INSUFFICIENT_STIMULUS,
}
