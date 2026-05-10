package com.estebancoloradogonzalez.tension.domain.rules

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineFatigueRuleTest {

    @Test
    fun `detectFatigue — 0 total exercises → false`() {
        assertFalse(RoutineFatigueRule.detectFatigue(regressionCount = 0, totalExercises = 0))
    }

    @Test
    fun `detectFatigue — exactly 50 percent regressions → true`() {
        assertTrue(RoutineFatigueRule.detectFatigue(regressionCount = 5, totalExercises = 10))
    }

    @Test
    fun `detectFatigue — 40 percent regressions → false`() {
        assertFalse(RoutineFatigueRule.detectFatigue(regressionCount = 4, totalExercises = 10))
    }

    @Test
    fun `detectFatigue — 60 percent regressions → true`() {
        assertTrue(RoutineFatigueRule.detectFatigue(regressionCount = 6, totalExercises = 10))
    }

    @Test
    fun `detectFatigue — 1 regression of 6 total exercises (incomplete session) → false`() {
        assertFalse(RoutineFatigueRule.detectFatigue(regressionCount = 1, totalExercises = 6))
    }
}
