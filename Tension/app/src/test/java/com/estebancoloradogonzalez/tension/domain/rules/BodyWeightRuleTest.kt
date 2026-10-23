package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.SessionTonnage
import com.estebancoloradogonzalez.tension.domain.model.WeeklyTonnage
import com.estebancoloradogonzalez.tension.domain.model.WeightRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Body weight read over the Metrics period (HU-48). Dates and values are those of `qa.md`:
 * «today» is 2026-10-22 and history B1 holds 77.2 (02/09), 77.9 (01/10) and 78.5 (20/10).
 */
class BodyWeightRuleTest {

    private val today = LocalDate.of(2026, 10, 22)

    private val historyB1 = listOf(
        WeightRecord(id = 1, weightKg = 77.2, date = LocalDate.of(2026, 9, 2)),
        WeightRecord(id = 2, weightKg = 77.9, date = LocalDate.of(2026, 10, 1)),
        WeightRecord(id = 3, weightKg = 78.5, date = LocalDate.of(2026, 10, 20)),
    )

    private fun start(weeks: Int) = BodyWeightRule.periodStart(today, weeks)

    // ----- Rule 3: current weight -----

    @Test
    fun `given history B1, when reading the current weight, then it is the last entry`() {
        // When
        val latest = BodyWeightRule.latest(historyB1)

        // Then
        assertEquals(78.5, latest!!.weightKg, 0.0)
        assertEquals(LocalDate.of(2026, 10, 20), latest.date)
    }

    @Test
    fun `given two entries on the same day, when reading the current weight, then the one saved last wins`() {
        // Given — QA-11: 79.0 and then 78.8, both today
        val history = historyB1 +
            WeightRecord(id = 4, weightKg = 79.0, date = today) +
            WeightRecord(id = 5, weightKg = 78.8, date = today)

        // When
        val latest = BodyWeightRule.latest(history.shuffled())

        // Then
        assertEquals(78.8, latest!!.weightKg, 0.0)
    }

    @Test
    fun `given no entries, when reading the current weight, then there is none`() {
        // Given
        val history = emptyList<WeightRecord>()

        // When
        val latest = BodyWeightRule.latest(history)

        // Then
        assertNull(latest)
    }

    // ----- Periods -----

    @Test
    fun `given the three windows, when computing their start, then they match qa dates`() {
        // When
        val starts = listOf(start(4), start(8), start(12))

        // Then
        assertEquals(
            listOf(LocalDate.of(2026, 9, 24), LocalDate.of(2026, 8, 27), LocalDate.of(2026, 7, 30)),
            starts,
        )
    }

    // ----- Rule 4 / QA-06: variation over the period -----

    @Test
    fun `given history B1 and 4 weeks, when computing the variation, then it is plus 1_3 kg`() {
        // When
        val variation = BodyWeightRule.variation(historyB1, start(4))

        // Then — reference is 77.2 (02/09), the latest entry on or before 24/09
        assertEquals(1.3, variation!!, 1e-9)
    }

    @Test
    fun `given history B1 and 8 or 12 weeks, when computing the variation, then there is not enough data`() {
        // When — 02/09 is after both starts (27/08 and 30/07): there is no reference
        val eightWeeks = BodyWeightRule.variation(historyB1, start(8))
        val twelveWeeks = BodyWeightRule.variation(historyB1, start(12))

        // Then
        assertNull(eightWeeks)
        assertNull(twelveWeeks)
    }

    @Test
    fun `given a new entry of 76_9 today, when computing 4 weeks, then the variation is minus 0_3 kg`() {
        // Given
        val history = historyB1 + WeightRecord(id = 4, weightKg = 76.9, date = today)

        // When
        val variation = BodyWeightRule.variation(history, start(4))

        // Then
        assertEquals(-0.3, variation!!, 1e-9)
    }

    @Test
    fun `given a new entry equal to the reference, when computing 4 weeks, then the variation is a real zero`() {
        // Given — QA-06 row 5: a real 0.0, not «not enough data»
        val history = historyB1 + WeightRecord(id = 4, weightKg = 77.2, date = today)

        // When
        val variation = BodyWeightRule.variation(history, start(4))

        // Then
        assertEquals(0.0, variation!!, 1e-9)
    }

    @Test
    fun `given a single entry registered today, when computing the variation, then there is not enough data`() {
        // Given — history B2 (QA-13)
        val history = listOf(WeightRecord(id = 1, weightKg = 78.5, date = today))

        // When / Then
        assertNull(BodyWeightRule.variation(history, start(4)))
    }

    @Test
    fun `given the only entry is the reference itself, when computing the variation, then it is not a zero`() {
        // Given — a single old entry is both the reference and the current weight
        val history = listOf(WeightRecord(id = 1, weightKg = 77.2, date = LocalDate.of(2026, 9, 2)))

        // When / Then
        assertNull(BodyWeightRule.variation(history, start(4)))
    }

    @Test
    fun `given an entry dated exactly on the period start, when computing the variation, then it is the reference`() {
        // Given
        val history = listOf(
            WeightRecord(id = 1, weightKg = 80.0, date = start(4)),
            WeightRecord(id = 2, weightKg = 79.0, date = today),
        )

        // When / Then
        assertEquals(-1.0, BodyWeightRule.variation(history, start(4))!!, 1e-9)
    }

    // ----- Rule 5 / QA-05: BMI without category -----

    @Test
    fun `given the qa profiles, when computing the BMI, then it matches to one decimal`() {
        // Given — weight and height of QA-05, plus 79.0 kg after the quick entry of QA-08
        val profiles = listOf(78.5 to 1.78, 110.0 to 1.70, 50.0 to 1.80, 79.0 to 1.78)

        // When
        val bmis = profiles.map { (weight, height) -> round1(BodyWeightRule.bmi(weight, height)) }

        // Then
        assertEquals(listOf(24.8, 38.1, 15.4, 24.9), bmis)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `given a height of zero, when computing the BMI, then it is rejected`() {
        // Given
        val heightM = 0.0

        // When / Then — rejected with IllegalArgumentException
        BodyWeightRule.bmi(78.5, heightM)
    }

    // ----- Rule 7 / QA-12: stale weight -----

    @Test
    fun `given 30, 31 and 42 days, when deciding the note, then only beyond 30 days is it stale`() {
        // Given
        val thirty = BodyWeightRule.daysSince(LocalDate.of(2026, 9, 22), today)
        val thirtyOne = BodyWeightRule.daysSince(LocalDate.of(2026, 9, 21), today)
        val fortyTwo = BodyWeightRule.daysSince(LocalDate.of(2026, 9, 10), today)

        // Then
        assertEquals(30L, thirty)
        assertEquals(31L, thirtyOne)
        assertEquals(42L, fortyTwo)
        assertFalse(BodyWeightRule.isStale(thirty))
        assertTrue(BodyWeightRule.isStale(thirtyOne))
        assertTrue(BodyWeightRule.isStale(fortyTwo))
    }

    // ----- Rule 8 / QA-07: chart series -----

    @Test
    fun `given history B1 and 4 weeks, when listing weight points, then only entries inside the period remain`() {
        // When
        val points = BodyWeightRule.weightPoints(historyB1.reversed(), start(4), today)

        // Then
        assertEquals(listOf(77.9, 78.5), points.map { it.weightKg })
    }

    @Test
    fun `given the qa sessions, when grouping by ISO week, then weeks sum their sessions and empty ones are absent`() {
        // Given — QA-07
        val sessions = listOf(
            SessionTonnage(LocalDate.of(2026, 10, 8), 1400.0),
            SessionTonnage(LocalDate.of(2026, 10, 13), 1200.0),
            SessionTonnage(LocalDate.of(2026, 10, 15), 1500.0),
            SessionTonnage(LocalDate.of(2026, 10, 21), 1300.0),
        )

        // When
        val weekly = BodyWeightRule.weeklyTonnage(sessions.shuffled(), start(4), today)

        // Then — weeks of 21/09 and 28/09 have no point at all
        assertEquals(
            listOf(
                WeeklyTonnage(LocalDate.of(2026, 10, 5), 1400.0),
                WeeklyTonnage(LocalDate.of(2026, 10, 12), 2700.0),
                WeeklyTonnage(LocalDate.of(2026, 10, 19), 1300.0),
            ),
            weekly,
        )
    }

    @Test
    fun `given a session before the period start in the same ISO week, when grouping, then it is left out`() {
        // Given — 24/09 is a Thursday; 22/09 belongs to the same week but not to the period
        val sessions = listOf(
            SessionTonnage(LocalDate.of(2026, 9, 22), 900.0),
            SessionTonnage(LocalDate.of(2026, 9, 25), 1000.0),
        )

        // When
        val weekly = BodyWeightRule.weeklyTonnage(sessions, start(4), today)

        // Then
        assertEquals(listOf(WeeklyTonnage(LocalDate.of(2026, 9, 21), 1000.0)), weekly)
    }

    @Test
    fun `given no sessions in the period, when grouping, then there is no tonnage point`() {
        // Given — QA-13
        val sessions = listOf(SessionTonnage(LocalDate.of(2026, 8, 1), 1000.0))

        // When / Then
        assertTrue(BodyWeightRule.weeklyTonnage(sessions, start(4), today).isEmpty())
    }

    @Test
    fun `given any day of the week, when resolving its ISO week, then it is the previous or same monday`() {
        // Given — the Monday and the Sunday of the same ISO week
        val monday = LocalDate.of(2026, 10, 19)
        val sunday = LocalDate.of(2026, 10, 25)

        // When
        val starts = listOf(BodyWeightRule.weekStart(monday), BodyWeightRule.weekStart(sunday))

        // Then
        assertEquals(listOf(monday, monday), starts)
    }

    private fun round1(value: Double): Double = Math.round(value * 10) / 10.0
}
