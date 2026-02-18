package com.estebancoloradogonzalez.tension.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Test

class LoadVelocityRuleTest {

    @Test
    fun `60 to 40 over 5 sessions returns 5_0 Kg per session`() {
        // (60 - 40) / (5 - 1) = 20 / 4 = 5.0
        assertEquals(5.0, LoadVelocityRule.calculate(60.0, 40.0, 5), 0.001)
    }

    @Test
    fun `same weight returns 0_0 Kg per session`() {
        assertEquals(0.0, LoadVelocityRule.calculate(50.0, 50.0, 3), 0.001)
    }

    @Test
    fun `only 1 session returns 0_0`() {
        assertEquals(0.0, LoadVelocityRule.calculate(60.0, 40.0, 1), 0.001)
    }

    @Test
    fun `0 sessions returns 0_0`() {
        assertEquals(0.0, LoadVelocityRule.calculate(60.0, 40.0, 0), 0.001)
    }

    @Test
    fun `weight decrease returns negative velocity`() {
        // (30 - 40) / (3 - 1) = -10 / 2 = -5.0
        assertEquals(-5.0, LoadVelocityRule.calculate(30.0, 40.0, 3), 0.001)
    }

    @Test
    fun `2 sessions with increment returns correct velocity`() {
        // (45 - 40) / (2 - 1) = 5 / 1 = 5.0
        assertEquals(5.0, LoadVelocityRule.calculate(45.0, 40.0, 2), 0.001)
    }
}
