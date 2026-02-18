package com.estebancoloradogonzalez.tension.domain.model

data class RirByModule(
    val moduleCode: String,
    val averageRir: Double?,
    val interpretation: RirInterpretation?,
)

enum class RirInterpretation {
    OPTIMAL,
    RISK_TOO_CLOSE,
    INSUFFICIENT_STIMULUS,
}
