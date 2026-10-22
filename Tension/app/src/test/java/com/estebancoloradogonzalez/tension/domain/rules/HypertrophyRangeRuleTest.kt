package com.estebancoloradogonzalez.tension.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * HU-46 — progression by the 8–12 hypertrophy range. The data of each test is transcribed
 * from `qa.md` (QA-NN), not from the code.
 */
class HypertrophyRangeRuleTest {

    // ----- Reading by strict majority (CA-46.01, QA-01, QA-02) -----

    @Test
    fun `given 12 12 11, when the session is read, then it is at the top`() {
        assertEquals(RangeReading.TOP, HypertrophyRangeRule.read(listOf(12, 12, 11)))
    }

    @Test
    fun `given 12 11 9, when the session is read, then it is within the range`() {
        assertEquals(RangeReading.WITHIN, HypertrophyRangeRule.read(listOf(12, 11, 9)))
    }

    @Test
    fun `given 6 7 9, when the session is read, then it is below the range`() {
        assertEquals(RangeReading.BELOW, HypertrophyRangeRule.read(listOf(6, 7, 9)))
    }

    @Test
    fun `given four sets with three at the top, when the session is read, then it is at the top`() {
        assertEquals(RangeReading.TOP, HypertrophyRangeRule.read(listOf(12, 12, 12, 10)))
    }

    @Test
    fun `given a single set of 13, when the session is read, then it is at the top`() {
        assertEquals(RangeReading.TOP, HypertrophyRangeRule.read(listOf(13)))
    }

    @Test
    fun `given 7 8, when the session is read, then one of two is not a majority`() {
        assertEquals(RangeReading.WITHIN, HypertrophyRangeRule.read(listOf(7, 8)))
    }

    @Test
    fun `given a tie of two at the top and two below, when the session is read, then it is within`() {
        assertEquals(RangeReading.WITHIN, HypertrophyRangeRule.read(listOf(12, 12, 6, 7)))
    }

    // ----- Reference weight: the highest, never the average (CA-46.03, QA-34, QA-35) -----

    @Test
    fun `given 60 60 and 62_5 kg, when the reference weight is taken, then it is 62_5`() {
        assertEquals(62.5, HypertrophyRangeRule.referenceWeight(listOf(60.0, 60.0, 62.5)), 0.001)
    }

    // ----- Streak (CA-46.03, CA-46.17) -----

    @Test
    fun `given four top sessions with the same weight, when the streak advances, then it reaches 4`() {
        // QA-04
        var streak = TopRangeStreak.NONE
        repeat(4) { streak = HypertrophyRangeRule.nextStreak(streak, RangeReading.TOP, 60.0) }
        assertEquals(4, streak.count)
        assertEquals(60.0, streak.referenceWeightKg!!, 0.001)
    }

    @Test
    fun `given a session within the range, when the streak advances, then it goes back to 0`() {
        // QA-29: 1 de 4, 2 de 4, dentro del rango, 1 de 4
        var streak = TopRangeStreak.NONE
        streak = HypertrophyRangeRule.nextStreak(streak, RangeReading.TOP, 60.0)
        streak = HypertrophyRangeRule.nextStreak(streak, RangeReading.TOP, 60.0)
        streak = HypertrophyRangeRule.nextStreak(streak, RangeReading.WITHIN, 60.0)
        assertEquals(0, streak.count)
        streak = HypertrophyRangeRule.nextStreak(streak, RangeReading.TOP, 60.0)
        assertEquals(1, streak.count)
    }

    @Test
    fun `given a session below the range, when the streak advances, then it goes back to 0`() {
        // QA-03
        val streak = HypertrophyRangeRule.nextStreak(TopRangeStreak(2, 60.0), RangeReading.BELOW, 60.0)
        assertEquals(TopRangeStreak.NONE, streak)
    }

    @Test
    fun `given a top session with another weight, when the streak advances, then it restarts at 1`() {
        // QA-30: 60, 60, 62.5 → 1 de 4
        var streak = TopRangeStreak.NONE
        streak = HypertrophyRangeRule.nextStreak(streak, RangeReading.TOP, 60.0)
        streak = HypertrophyRangeRule.nextStreak(streak, RangeReading.TOP, 60.0)
        streak = HypertrophyRangeRule.nextStreak(streak, RangeReading.TOP, 62.5)
        assertEquals(1, streak.count)
        assertEquals(62.5, streak.referenceWeightKg!!, 0.001)
    }

    @Test
    fun `given the same highest weight with a different average, when the streak advances, then it continues`() {
        // QA-34: 60·60·62.5 y luego 62.5·60·60 → 2 de 4; luego 60·60·60 → 1 de 4
        val first = HypertrophyRangeRule.referenceWeight(listOf(60.0, 60.0, 62.5))
        val second = HypertrophyRangeRule.referenceWeight(listOf(62.5, 60.0, 60.0))
        val third = HypertrophyRangeRule.referenceWeight(listOf(60.0, 60.0, 60.0))
        var streak = HypertrophyRangeRule.nextStreak(TopRangeStreak.NONE, RangeReading.TOP, first)
        streak = HypertrophyRangeRule.nextStreak(streak, RangeReading.TOP, second)
        assertEquals(2, streak.count)
        streak = HypertrophyRangeRule.nextStreak(streak, RangeReading.TOP, third)
        assertEquals(1, streak.count)
    }

    // ----- Session signal (CA-46.02, CA-46.03, CA-46.04, CA-46.06) -----

    @Test
    fun `given a session below the range, when the signal is resolved, then lower the weight`() {
        assertEquals(RangeSignal.Decrease, signal(RangeReading.BELOW, TopRangeStreak.NONE))
    }

    @Test
    fun `given one to three top sessions, when the signal is resolved, then keep with N of 4`() {
        assertEquals(RangeSignal.Progressing(1), signal(RangeReading.TOP, TopRangeStreak(1, 60.0)))
        assertEquals(RangeSignal.Progressing(3), signal(RangeReading.TOP, TopRangeStreak(3, 60.0)))
    }

    @Test
    fun `given the fourth top session, when the signal is resolved, then increase`() {
        // QA-04: la regla no recibe las series del plan ni el RIR (CA-46.06, QA-10)
        assertEquals(RangeSignal.Increase(4), signal(RangeReading.TOP, TopRangeStreak(4, 60.0)))
    }

    @Test
    fun `given a fifth top session without increasing, when the signal is resolved, then increase stays`() {
        // QA-05
        assertEquals(RangeSignal.Increase(5), signal(RangeReading.TOP, TopRangeStreak(5, 60.0)))
    }

    @Test
    fun `given a session within the range, when the signal is resolved, then keep`() {
        assertEquals(RangeSignal.InRange, signal(RangeReading.WITHIN, TopRangeStreak.NONE))
    }

    // ----- Bodyweight (CA-46.15, QA-25, QA-26, QA-27) -----

    @Test
    fun `given a bodyweight pair below the range, when the signal is resolved, then never lower`() {
        val result = HypertrophyRangeRule.sessionSignal(
            RangeReading.BELOW,
            TopRangeStreak.NONE,
            hasExternalLoad = false,
            admitsAddedWeight = true,
        )
        assertEquals(RangeSignal.InRange, result)
    }

    @Test
    fun `given a bodyweight pair that admits added weight, when four top sessions close, then add extra weight`() {
        val result = HypertrophyRangeRule.sessionSignal(
            RangeReading.TOP,
            TopRangeStreak(4, 0.0),
            hasExternalLoad = false,
            admitsAddedWeight = true,
        )
        assertEquals(RangeSignal.AddExtraWeight(4), result)
    }

    @Test
    fun `given a bodyweight pair without added weight, when four top sessions close, then no weight signal`() {
        val result = HypertrophyRangeRule.sessionSignal(
            RangeReading.TOP,
            TopRangeStreak(4, 0.0),
            hasExternalLoad = false,
            admitsAddedWeight = false,
        )
        assertEquals(RangeSignal.InRange, result)
    }

    @Test
    fun `given the added weight pair below the range, when the signal is resolved, then lower the weight`() {
        // QA-27: el par «Peso Añadido» tiene carga externa y sigue la regla completa
        assertEquals(RangeSignal.Decrease, signal(RangeReading.BELOW, TopRangeStreak.NONE))
    }

    // ----- Carried signal (CA-46.02, CA-46.04) -----

    @Test
    fun `given a pair never read by the range, when the carried signal is resolved, then there is none`() {
        assertNull(HypertrophyRangeRule.carriedSignal(null, TopRangeStreak.NONE, true, false))
    }

    @Test
    fun `given a pair last read below the range, when the carried signal is resolved, then lower`() {
        assertEquals(
            RangeSignal.Decrease,
            HypertrophyRangeRule.carriedSignal(RangeReading.BELOW, TopRangeStreak.NONE, true, false),
        )
    }

    // ----- Live notice (CA-46.05, QA-07, QA-08, QA-09, QA-36) -----

    @Test
    fun `given a first set of 7, when it is registered, then lower the weight in the next set`() {
        assertEquals(SetRangeNotice.LowerNextSet, notice(reps = 7, streak = TopRangeStreak.NONE))
    }

    @Test
    fun `given a set of 8, when it is registered, then there is no notice`() {
        assertNull(notice(reps = 8, streak = TopRangeStreak.NONE))
    }

    @Test
    fun `given sets between 8 and 11, when they are registered, then there is no notice`() {
        listOf(8, 10, 11).forEach { reps -> assertNull(notice(reps = reps, streak = TopRangeStreak(1, 60.0))) }
    }

    @Test
    fun `given one top session behind, when a set of 12 or 15 is registered, then it counts as session 2 of 4`() {
        val streak = TopRangeStreak(1, 60.0)
        assertEquals(SetRangeNotice.TopOfRange(2), notice(reps = 12, streak = streak))
        assertEquals(SetRangeNotice.TopOfRange(2), notice(reps = 15, streak = streak))
    }

    @Test
    fun `given a streak with another weight, when a top set is registered, then it is session 1 of 4`() {
        assertEquals(
            SetRangeNotice.TopOfRange(1),
            notice(reps = 12, streak = TopRangeStreak(2, 60.0), weightKg = 62.5),
        )
    }

    @Test
    fun `given the increase already earned, when a top set keeps the weight, then it is time to increase`() {
        // QA-36
        assertEquals(SetRangeNotice.IncreaseDue, notice(reps = 12, streak = TopRangeStreak(4, 60.0)))
        assertNull(notice(reps = 10, streak = TopRangeStreak(4, 60.0)))
    }

    @Test
    fun `given a bodyweight pair, when a set under 8 is registered, then there is no notice`() {
        // QA-25
        val result = HypertrophyRangeRule.setNotice(
            reps = 5,
            weightKg = 0.0,
            streak = TopRangeStreak.NONE,
            hasExternalLoad = false,
            admitsAddedWeight = true,
        )
        assertNull(result)
    }

    @Test
    fun `given a bodyweight pair without added weight, when a top set is registered, then there is no notice`() {
        val result = HypertrophyRangeRule.setNotice(
            reps = 12,
            weightKg = 0.0,
            streak = TopRangeStreak(4, 0.0),
            hasExternalLoad = false,
            admitsAddedWeight = false,
        )
        assertNull(result)
    }

    @Test
    fun `given a bodyweight pair with the increase earned, when a top set is registered, then add weight`() {
        val result = HypertrophyRangeRule.setNotice(
            reps = 12,
            weightKg = 0.0,
            streak = TopRangeStreak(4, 0.0),
            hasExternalLoad = false,
            admitsAddedWeight = true,
        )
        assertEquals(SetRangeNotice.AddWeightDue, result)
    }

    private fun signal(reading: RangeReading, streak: TopRangeStreak): RangeSignal =
        HypertrophyRangeRule.sessionSignal(reading, streak, hasExternalLoad = true, admitsAddedWeight = false)

    private fun notice(
        reps: Int,
        streak: TopRangeStreak,
        weightKg: Double = 60.0,
    ): SetRangeNotice? =
        HypertrophyRangeRule.setNotice(
            reps = reps,
            weightKg = weightKg,
            streak = streak,
            hasExternalLoad = true,
            admitsAddedWeight = false,
        )
}
