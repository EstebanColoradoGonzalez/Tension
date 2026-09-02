package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.ProgressionDifficulty

/**
 * Single declaration point for the thresholds and observation windows of the five
 * alert families. Every family declares, together, the value it compares against and
 * the window over which it observes — an alert whose window is too short converts
 * normal fluctuation into noise, and noise trains the executant to ignore every alert,
 * including the ones that mattered.
 *
 * ### Progression rate
 * Window of [PROGRESSION_WINDOW_WEEKS] weeks, weighted by the exercise's progression
 * difficulty: an exercise that is intrinsically hard to progress is not judged by the
 * yardstick of an easy one. Requires at least [PROGRESSION_MIN_OBSERVATIONS] classified
 * sessions — a ratio over one or two sessions is a coin toss, not a rate.
 *
 * ### RIR out of range
 * Sustained over [RIR_SUSTAINED_SESSIONS] consecutive sessions. Thresholds are expressed
 * on the 0..2 scale this system captures RIR in, not on the 0..5 scale used elsewhere in
 * the literature: below [RIR_LOW_THRESHOLD] the executant trains at technical failure,
 * above [RIR_HIGH_THRESHOLD] the stimulus may be insufficient.
 *
 * ### Weekly adherence
 * Below [ADHERENCE_THRESHOLD] percent for [ADHERENCE_ALERT_WEEKS] consecutive weeks; a
 * single bad week is not an adherence problem. Crisis from [ADHERENCE_CRISIS_WEEKS] weeks.
 *
 * ### Tonnage drop
 * Over [TONNAGE_MICROCYCLES] consecutive microcycles. A planned deload is a controlled
 * drop, not a regression, so it never raises this family.
 *
 * ### Module inactivity
 * Natural days since the module's last session, [INACTIVITY_ALERT_DAYS] to alert and
 * [INACTIVITY_CRISIS_DAYS] to crisis.
 */
object AlertThresholdRule {

    const val PROGRESSION_WINDOW_WEEKS = 6L
    const val PROGRESSION_MIN_OBSERVATIONS = 3

    const val RIR_LOW_THRESHOLD = 0.5
    const val RIR_HIGH_THRESHOLD = 1.8
    const val RIR_SUSTAINED_SESSIONS = 3

    const val ADHERENCE_THRESHOLD = 60.0
    const val ADHERENCE_ALERT_WEEKS = 2
    const val ADHERENCE_CRISIS_WEEKS = 3
    const val ADHERENCE_LOOKBACK_WEEKS = 4

    const val TONNAGE_ALERT_THRESHOLD = 15.0
    const val TONNAGE_CRISIS_THRESHOLD = 25.0
    const val TONNAGE_MICROCYCLES = 2

    const val INACTIVITY_ALERT_DAYS = 14L
    const val INACTIVITY_CRISIS_DAYS = 21L

    private const val LEVEL_CRISIS = "CRISIS"
    private const val LEVEL_MEDIUM_ALERT = "MEDIUM_ALERT"

    /**
     * Alert threshold of the progression rate, in percent, for the given [difficulty].
     * An exercise that advances slowly by nature is expected to advance slowly.
     */
    fun progressionAlertThreshold(difficulty: ProgressionDifficulty): Double = when (difficulty) {
        ProgressionDifficulty.LOW -> 40.0
        ProgressionDifficulty.MEDIUM -> 35.0
        ProgressionDifficulty.HIGH -> 25.0
    }

    /** Crisis threshold of the progression rate, in percent, for the given [difficulty]. */
    fun progressionCrisisThreshold(difficulty: ProgressionDifficulty): Double = when (difficulty) {
        ProgressionDifficulty.LOW -> 20.0
        ProgressionDifficulty.MEDIUM -> 15.0
        ProgressionDifficulty.HIGH -> 10.0
    }

    fun isProgressionAlert(rate: Double, difficulty: ProgressionDifficulty): Boolean =
        rate < progressionAlertThreshold(difficulty)

    fun isProgressionCrisis(rate: Double, difficulty: ProgressionDifficulty): Boolean =
        rate < progressionCrisisThreshold(difficulty)

    fun progressionLevel(rate: Double, difficulty: ProgressionDifficulty): String? {
        return when {
            isProgressionCrisis(rate, difficulty) -> LEVEL_CRISIS
            isProgressionAlert(rate, difficulty) -> LEVEL_MEDIUM_ALERT
            else -> null
        }
    }

    fun isRirLow(avgRir: Double): Boolean = avgRir < RIR_LOW_THRESHOLD

    fun isRirHigh(avgRir: Double): Boolean = avgRir > RIR_HIGH_THRESHOLD

    fun isRirOutOfRange(avgRir: Double): Boolean = isRirLow(avgRir) || isRirHigh(avgRir)

    fun isAdherenceLow(percentage: Double): Boolean = percentage < ADHERENCE_THRESHOLD

    /**
     * Severity of the adherence family from the number of consecutive weeks below
     * [ADHERENCE_THRESHOLD]. Below [ADHERENCE_ALERT_WEEKS] there is nothing to report:
     * one bad week is fluctuation.
     */
    fun adherenceLevel(consecutiveLowWeeks: Int): String? {
        return when {
            consecutiveLowWeeks >= ADHERENCE_CRISIS_WEEKS -> LEVEL_CRISIS
            consecutiveLowWeeks >= ADHERENCE_ALERT_WEEKS -> LEVEL_MEDIUM_ALERT
            else -> null
        }
    }

    fun isTonnageAlert(dropPercentage: Double): Boolean =
        dropPercentage > TONNAGE_ALERT_THRESHOLD

    fun isTonnageCrisis(dropPercentage: Double): Boolean =
        dropPercentage > TONNAGE_CRISIS_THRESHOLD

    /**
     * Severity of the tonnage family. A planned deload never raises it: the drop is
     * intended, and reporting it would contradict the protocol the executant activated.
     */
    fun tonnageLevel(dropPercentage: Double, isDeloadSession: Boolean): String? {
        if (isDeloadSession) return null
        return when {
            isTonnageCrisis(dropPercentage) -> LEVEL_CRISIS
            isTonnageAlert(dropPercentage) -> LEVEL_MEDIUM_ALERT
            else -> null
        }
    }

    fun isInactivityAlert(days: Long): Boolean = days > INACTIVITY_ALERT_DAYS

    fun isInactivityCrisis(days: Long): Boolean = days > INACTIVITY_CRISIS_DAYS

    fun inactivityLevel(days: Long): String? {
        return when {
            isInactivityCrisis(days) -> LEVEL_CRISIS
            isInactivityAlert(days) -> LEVEL_MEDIUM_ALERT
            else -> null
        }
    }
}
