package com.estebancoloradogonzalez.tension.ui.components

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Unit in which an analytics indicator is presented. The kilogram is the canonical
 * unit of every load and tonnage aggregate of the system: capture may happen in
 * pounds, storage and presentation never do.
 */
enum class MetricUnit {
    KILOGRAM,
    KILOGRAM_PER_SESSION,
    PERCENTAGE,
    RIR,
    COUNT,
}

/**
 * Nature of the evidence an indicator is missing in order to be computable.
 * Each kind maps to one explicit sentence telling the executant what is lacking.
 */
enum class MetricRequirementKind {
    /** Times the exercise was classified inside the evaluation window. */
    EXERCISE_OBSERVATIONS,

    /** Sessions in which the exercise was performed inside the evaluation window. */
    EXERCISE_SESSIONS,

    /** Sets with a recorded RIR inside the routine window. */
    ROUTINE_SETS,

    /** Weekly session target declared by the executant. */
    WEEKLY_TARGET,

    /** Sessions recorded inside the selected microcycle. */
    MICROCYCLE_SESSIONS,

    /** Fully completed microcycles available in the history. */
    COMPLETE_MICROCYCLES,
}

/**
 * What is missing for an indicator to be computable, and how much of it.
 */
data class MetricRequirement(
    val kind: MetricRequirementKind,
    val available: Int,
    val needed: Int,
) {
    /** Amount still lacking. Never negative. */
    val missing: Int get() = (needed - available).coerceAtLeast(0)
}

/**
 * State of a single analytics indicator.
 *
 * The three states are mutually exclusive and exhaustive, which makes it impossible
 * to render a zero where the datum is simply absent: a computed zero always travels
 * as [Available] and an absent one never reaches that branch.
 */
sealed interface MetricValue {

    /** The indicator was computed. [amount] may legitimately be zero. */
    data class Available(val amount: Double, val unit: MetricUnit) : MetricValue

    /** There is not enough history to compute the indicator. */
    data class Insufficient(val requirement: MetricRequirement) : MetricValue

    /** The indicator does not apply to this element — bodyweight or isometric load. */
    data object NotApplicable : MetricValue
}

/**
 * Decides whether an analytics indicator has enough data to be shown.
 *
 * Every threshold here is a transcription of the guard the corresponding calculation
 * rule already executes before returning `0.0`. No threshold is calibrated here: this
 * object only makes visible a decision the domain was already taking silently.
 */
object MetricSufficiencyRules {

    /** Mirrors `ProgressionRateRule.calculate`, which returns 0.0 when `totalCount == 0`. */
    const val MIN_PROGRESSION_OBSERVATIONS = 1

    /** Mirrors `LoadVelocityRule.calculate`, which returns 0.0 when `sessionCount <= 1`. */
    const val MIN_LOAD_VELOCITY_SESSIONS = 2

    /** Mirrors `AvgRirRule.calculate`, which returns 0.0 when the value list is empty. */
    const val MIN_RIR_SETS = 1

    /** Mirrors `AdherenceRule.calculate`, which returns 0.0 when `plannedSessions == 0`. */
    const val MIN_WEEKLY_TARGET = 1

    /**
     * Mirrors `GetTonnageByMuscleGroupUseCase`, which pads every muscle group with 0.0
     * when the selected microcycle carries no session, and `VolumeDistributionRule`,
     * which maps every zone to 0.0 when there is no set at all.
     */
    const val MIN_MICROCYCLE_SESSIONS = 1

    /** Mirrors the evolution guard of `VolumeViewModel`: fewer than 2 microcycles is not a series. */
    const val MIN_EVOLUTION_MICROCYCLES = 2

    /** Mirrors `GetMuscleGroupTrendUseCase`, which returns an empty list below 4 complete microcycles. */
    const val MIN_TREND_MICROCYCLES = 4

    /** Progression rate of one exercise over the evaluation window. */
    fun progressionRate(rate: Double, observations: Int): MetricValue =
        if (observations < MIN_PROGRESSION_OBSERVATIONS) {
            insufficient(MetricRequirementKind.EXERCISE_OBSERVATIONS, observations, MIN_PROGRESSION_OBSERVATIONS)
        } else {
            MetricValue.Available(rate, MetricUnit.PERCENTAGE)
        }

    /**
     * Load velocity of one exercise. Bodyweight and isometric exercises carry no external
     * load, so the indicator does not apply to them — a state distinct from lacking data,
     * which is why it is resolved first.
     */
    fun loadVelocity(velocity: Double, sessionCount: Int, isBodyweight: Boolean): MetricValue = when {
        isBodyweight -> MetricValue.NotApplicable
        sessionCount < MIN_LOAD_VELOCITY_SESSIONS ->
            insufficient(MetricRequirementKind.EXERCISE_SESSIONS, sessionCount, MIN_LOAD_VELOCITY_SESSIONS)
        else -> MetricValue.Available(velocity, MetricUnit.KILOGRAM_PER_SESSION)
    }

    /** Average RIR of one routine over the last sessions. */
    fun averageRir(averageRir: Double?, recordedSets: Int): MetricValue =
        if (averageRir == null || recordedSets < MIN_RIR_SETS) {
            insufficient(MetricRequirementKind.ROUTINE_SETS, recordedSets, MIN_RIR_SETS)
        } else {
            MetricValue.Available(averageRir, MetricUnit.RIR)
        }

    /** Weekly adherence against the declared session target. */
    fun adherence(percentage: Double, plannedSessions: Int): MetricValue =
        if (plannedSessions < MIN_WEEKLY_TARGET) {
            insufficient(MetricRequirementKind.WEEKLY_TARGET, plannedSessions, MIN_WEEKLY_TARGET)
        } else {
            MetricValue.Available(percentage, MetricUnit.PERCENTAGE)
        }

    /**
     * Tonnage of one muscle group inside a microcycle. A group that was not trained in a
     * microcycle that does have sessions is a legitimate zero; a microcycle without any
     * session is missing data.
     */
    fun tonnage(tonnageKg: Double, sessionsInMicrocycle: Int): MetricValue =
        if (sessionsInMicrocycle < MIN_MICROCYCLE_SESSIONS) {
            insufficient(MetricRequirementKind.MICROCYCLE_SESSIONS, sessionsInMicrocycle, MIN_MICROCYCLE_SESSIONS)
        } else {
            MetricValue.Available(tonnageKg, MetricUnit.KILOGRAM)
        }

    /** Share of volume of one muscle zone inside a microcycle. */
    fun distribution(percentage: Double, sessionsInMicrocycle: Int): MetricValue =
        if (sessionsInMicrocycle < MIN_MICROCYCLE_SESSIONS) {
            insufficient(MetricRequirementKind.MICROCYCLE_SESSIONS, sessionsInMicrocycle, MIN_MICROCYCLE_SESSIONS)
        } else {
            MetricValue.Available(percentage, MetricUnit.PERCENTAGE)
        }

    /** Requirement of the tonnage evolution chart, or null when it can be drawn. */
    fun evolution(totalMicrocycles: Int): MetricRequirement? =
        requirementOrNull(
            MetricRequirementKind.COMPLETE_MICROCYCLES,
            totalMicrocycles,
            MIN_EVOLUTION_MICROCYCLES,
        )

    /** Requirement of the muscle group trend, or null when it can be classified. */
    fun trend(completeMicrocycles: Int): MetricRequirement? =
        requirementOrNull(
            MetricRequirementKind.COMPLETE_MICROCYCLES,
            completeMicrocycles,
            MIN_TREND_MICROCYCLES,
        )

    private fun insufficient(
        kind: MetricRequirementKind,
        available: Int,
        needed: Int,
    ): MetricValue = MetricValue.Insufficient(MetricRequirement(kind, available, needed))

    private fun requirementOrNull(
        kind: MetricRequirementKind,
        available: Int,
        needed: Int,
    ): MetricRequirement? =
        if (available < needed) MetricRequirement(kind, available, needed) else null
}

/**
 * Formats the numeric part of an indicator. The unit symbol is never concatenated here:
 * the card composes it as a separate element so it can carry its own typography.
 */
object MetricFormatRules {

    private val locale = Locale("es", "ES")

    /** Formats [amount] with the precision and grouping that [unit] requires. */
    fun formatAmount(amount: Double, unit: MetricUnit): String = when (unit) {
        MetricUnit.KILOGRAM -> integerFormat().format(amount.roundToLong())
        MetricUnit.PERCENTAGE -> integerFormat().format(amount.roundToLong())
        MetricUnit.COUNT -> integerFormat().format(amount.roundToLong())
        MetricUnit.RIR -> decimalFormat().format(amount)
        MetricUnit.KILOGRAM_PER_SESSION -> signed(amount)
    }

    private fun signed(amount: Double): String {
        val formatted = decimalFormat().format(amount)
        return if (amount > 0) "+$formatted" else formatted
    }

    private fun integerFormat(): NumberFormat = NumberFormat.getIntegerInstance(locale)

    private fun decimalFormat(): NumberFormat = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }
}
