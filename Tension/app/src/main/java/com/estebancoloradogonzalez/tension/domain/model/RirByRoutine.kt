package com.estebancoloradogonzalez.tension.domain.model

/**
 * Average RIR of one routine over the last sessions.
 *
 * [recordedSets] is the number of sets the average was computed from. It is not part of
 * the calculation: it is the evidence the presentation needs in order to state what is
 * missing when the routine has no recorded set.
 */
data class RirByRoutine(
    val routineId: Long,
    val routineName: String,
    val averageRir: Double?,
    val interpretation: RirInterpretation?,
    val recordedSets: Int,
)

enum class RirInterpretation {
    OPTIMAL,
    RISK_TOO_CLOSE,
    INSUFFICIENT_STIMULUS,
}
