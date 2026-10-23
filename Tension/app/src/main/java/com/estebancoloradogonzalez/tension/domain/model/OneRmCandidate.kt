package com.estebancoloradogonzalez.tension.domain.model

/**
 * A set that may set the 1RM record of its pair (HU-49): its repetitions and RIR, its real
 * load, whether its implement carries load the 1RM may count, and the date of its session.
 */
data class OneRmCandidate(
    val reps: Int,
    val rir: Int,
    val loadKg: Double,
    val hasCountableLoad: Boolean,
    /** Session date, `YYYY-MM-DD`. ISO dates order correctly as text. */
    val date: String,
)

/** The record of a pair: the estimated 1RM and the date it was reached (CA-49.02). */
data class OneRmRecord(
    val oneRmKg: Double,
    val achievedOn: String,
)
