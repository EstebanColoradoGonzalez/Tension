package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.SessionTonnage
import com.estebancoloradogonzalez.tension.domain.model.WeeklyTonnage
import com.estebancoloradogonzalez.tension.domain.model.WeightRecord
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Reads the body weight history in the context of the training period (HU-48).
 *
 * **It is not part of the decision engine.** Like [OneRmRule], it lives in this package
 * because it is pure and testable on the JVM. Load prescription, signals, plateau,
 * regression, fatigue, deload and alerts never invoke it (Rule 9).
 */
object BodyWeightRule {

    /** A last entry older than this many days earns the informative note (CA-48.06). */
    const val STALE_AFTER_DAYS = 30L

    private val chronological = compareBy<WeightRecord>({ it.date }, { it.id })

    /**
     * The current weight: the last entry. Entries of the same day are ordered by `id`, so
     * the one saved last wins (Rule 3).
     */
    fun latest(records: List<WeightRecord>): WeightRecord? = records.maxWithOrNull(chronological)

    /** First day of a window of [weeks] weeks ending [today], as every other Metrics window. */
    fun periodStart(today: LocalDate, weeks: Int): LocalDate = today.minusWeeks(weeks.toLong())

    /** The weight in force when the period started: the latest entry dated on or before it. */
    fun reference(records: List<WeightRecord>, periodStart: LocalDate): WeightRecord? =
        records.filter { !it.date.isAfter(periodStart) }.maxWithOrNull(chronological)

    /**
     * Current weight minus the weight in force at [periodStart] (Rule 4).
     *
     * `null` means «not enough data», never zero:
     * - there is no entry on or before the start of the period, or
     * - the current entry *is* that reference, so nothing was registered inside the period
     *   and subtracting it from itself would show a zero that measures nothing.
     *
     * With a reference and at least one later entry the result is a real value, `0.0`
     * included.
     */
    fun variation(records: List<WeightRecord>, periodStart: LocalDate): Double? {
        val current = latest(records) ?: return null
        val reference = reference(records, periodStart) ?: return null
        if (current.id == reference.id) return null
        return current.weightKg - reference.weightKg
    }

    /** Body mass index: weight ÷ height². A context datum, without category (Rule 5). */
    fun bmi(weightKg: Double, heightM: Double): Double {
        require(heightM > 0) { "Height must be > 0" }
        return weightKg / (heightM * heightM)
    }

    fun daysSince(recordedOn: LocalDate, today: LocalDate): Long =
        ChronoUnit.DAYS.between(recordedOn, today)

    fun isStale(daysSinceLastRecord: Long): Boolean = daysSinceLastRecord > STALE_AFTER_DAYS

    /** Entries of the history inside `[periodStart, today]`, oldest first: one point each. */
    fun weightPoints(
        records: List<WeightRecord>,
        periodStart: LocalDate,
        today: LocalDate,
    ): List<WeightRecord> =
        records.filter { it.date.inPeriod(periodStart, today) }.sortedWith(chronological)

    /**
     * Tonnage of the period, one point per ISO week (Monday to Sunday) that has sessions
     * inside `[periodStart, today]`, oldest first (Rule 8).
     *
     * A week without sessions has no point, so it can never be drawn as a zero. The week in
     * progress adds up the sessions registered so far.
     */
    fun weeklyTonnage(
        sessions: List<SessionTonnage>,
        periodStart: LocalDate,
        today: LocalDate,
    ): List<WeeklyTonnage> =
        sessions
            .filter { it.date.inPeriod(periodStart, today) }
            .groupBy { weekStart(it.date) }
            .map { (weekStart, weekSessions) ->
                WeeklyTonnage(weekStart, weekSessions.sumOf { it.tonnageKg })
            }
            .sortedBy { it.weekStart }

    /** Monday of the ISO week [date] belongs to. */
    fun weekStart(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    private fun LocalDate.inPeriod(start: LocalDate, end: LocalDate): Boolean =
        !isBefore(start) && !isAfter(end)
}
