package com.estebancoloradogonzalez.tension.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressionRateRuleTest {

    @Test
    fun `3 positive of 10 total returns 30 percent`() {
        assertEquals(30.0, ProgressionRateRule.calculate(3, 10), 0.001)
    }

    @Test
    fun `0 positive of 0 total returns 0 percent`() {
        assertEquals(0.0, ProgressionRateRule.calculate(0, 0), 0.001)
    }

    @Test
    fun `10 positive of 10 total returns 100 percent`() {
        assertEquals(100.0, ProgressionRateRule.calculate(10, 10), 0.001)
    }

    @Test
    fun `1 positive of 2 total returns 50 percent`() {
        assertEquals(50.0, ProgressionRateRule.calculate(1, 2), 0.001)
    }

    @Test
    fun `0 positive of 5 total returns 0 percent`() {
        assertEquals(0.0, ProgressionRateRule.calculate(0, 5), 0.001)
    }
}
