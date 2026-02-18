package com.estebancoloradogonzalez.tension.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Test

class AdherenceRuleTest {

    @Test
    fun `5 of 6 planned returns 83_3 percent`() {
        assertEquals(83.333, AdherenceRule.calculate(5, 6), 0.01)
    }

    @Test
    fun `6 of 6 planned returns 100 percent`() {
        assertEquals(100.0, AdherenceRule.calculate(6, 6), 0.001)
    }

    @Test
    fun `7 of 6 planned capped at 100 percent`() {
        assertEquals(100.0, AdherenceRule.calculate(7, 6), 0.001)
    }

    @Test
    fun `0 of 6 planned returns 0 percent`() {
        assertEquals(0.0, AdherenceRule.calculate(0, 6), 0.001)
    }

    @Test
    fun `0 of 0 planned returns 0 percent`() {
        assertEquals(0.0, AdherenceRule.calculate(0, 0), 0.001)
    }

    @Test
    fun `3 of 4 planned returns 75 percent`() {
        assertEquals(75.0, AdherenceRule.calculate(3, 4), 0.001)
    }

    @Test
    fun `4 of 5 planned returns 80 percent`() {
        assertEquals(80.0, AdherenceRule.calculate(4, 5), 0.001)
    }
}
