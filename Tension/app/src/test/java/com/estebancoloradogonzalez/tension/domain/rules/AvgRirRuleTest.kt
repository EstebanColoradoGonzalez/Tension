package com.estebancoloradogonzalez.tension.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Test

class AvgRirRuleTest {

    @Test
    fun `average of 2 3 2 3 returns 2_5`() {
        assertEquals(2.5, AvgRirRule.calculate(listOf(2, 3, 2, 3)), 0.001)
    }

    @Test
    fun `single value returns itself`() {
        assertEquals(1.0, AvgRirRule.calculate(listOf(1)), 0.001)
    }

    @Test
    fun `empty list returns 0_0`() {
        assertEquals(0.0, AvgRirRule.calculate(emptyList()), 0.001)
    }

    @Test
    fun `average of 2 3 2 4 returns 2_8 with one decimal rounding`() {
        // (2+3+2+4) / 4 = 11/4 = 2.75 → round to 2.8
        assertEquals(2.8, AvgRirRule.calculate(listOf(2, 3, 2, 4)), 0.001)
    }

    @Test
    fun `average of 1 1 1 returns 1_0`() {
        assertEquals(1.0, AvgRirRule.calculate(listOf(1, 1, 1)), 0.001)
    }

    @Test
    fun `average of 0 0 0 returns 0_0`() {
        assertEquals(0.0, AvgRirRule.calculate(listOf(0, 0, 0)), 0.001)
    }

    @Test
    fun `average of 3 4 5 returns 4_0`() {
        assertEquals(4.0, AvgRirRule.calculate(listOf(3, 4, 5)), 0.001)
    }
}
