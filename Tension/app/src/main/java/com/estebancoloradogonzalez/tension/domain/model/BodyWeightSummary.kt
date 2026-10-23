package com.estebancoloradogonzalez.tension.domain.model

import java.time.LocalDate

/**
 * Body weight read in the context of training, for the selected period (HU-48).
 *
 * It is a complement, never a metric of its own: nothing in the decision engine reads it.
 *
 * @property variationKg current weight minus the weight in force when the period started, or
 * `null` when there is not enough history to compute it. `null` and `0.0` are different
 * states: a real zero is a valid variation.
 * @property bmi context datum without any category or judgement (CA-48.03).
 * @property weightPoints entries of the weight history inside the period, oldest first.
 * @property weeklyTonnage one point per ISO week with sessions inside the period. A week
 * without sessions carries no point at all, so it can never be drawn as a zero.
 */
data class BodyWeightSummary(
    val currentWeightKg: Double,
    val recordedOn: LocalDate,
    val daysSinceLastRecord: Long,
    val isStale: Boolean,
    val variationKg: Double?,
    val bmi: Double,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val weightPoints: List<WeightRecord>,
    val weeklyTonnage: List<WeeklyTonnage>,
)

/** Tonnage of every session of one ISO week (Monday to Sunday). */
data class WeeklyTonnage(
    val weekStart: LocalDate,
    val tonnageKg: Double,
)

/** Tonnage of one closed session, as the system computes it, on the date it was trained. */
data class SessionTonnage(
    val date: LocalDate,
    val tonnageKg: Double,
)
