package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.DayOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DayResolutionRuleTest {

    private val today = "2026-09-01"

    @Test
    fun `a day without session and without skip stays open`() {
        assertNull(DayResolutionRule.resolve(today, hasClosedSessionToday = false, skippedDate = null))
    }

    @Test
    fun `a closed session today resolves the day as trained`() {
        assertEquals(
            DayOutcome.TRAINED,
            DayResolutionRule.resolve(today, hasClosedSessionToday = true, skippedDate = null),
        )
    }

    @Test
    fun `an explicit skip for today resolves the day as skipped`() {
        assertEquals(
            DayOutcome.SKIPPED,
            DayResolutionRule.resolve(today, hasClosedSessionToday = false, skippedDate = today),
        )
    }

    @Test
    fun `a skip from another day does not resolve today`() {
        assertNull(
            DayResolutionRule.resolve(today, hasClosedSessionToday = false, skippedDate = "2026-08-31"),
        )
    }

    @Test
    fun `training wins over a stale skip of the same day`() {
        assertEquals(
            DayOutcome.TRAINED,
            DayResolutionRule.resolve(today, hasClosedSessionToday = true, skippedDate = today),
        )
    }
}
