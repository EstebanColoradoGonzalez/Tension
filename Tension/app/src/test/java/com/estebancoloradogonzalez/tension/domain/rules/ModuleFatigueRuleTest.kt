package com.estebancoloradogonzalez.tension.domain.rules

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleFatigueRuleTest {

    @Test
    fun `detectFatigue — 0 exercises with records → false`() {
        assertFalse(ModuleFatigueRule.detectFatigue(regressionCount = 0, exercisesWithRecords = 0))
    }

    @Test
    fun `detectFatigue — exactly 50 percent regressions → true`() {
        assertTrue(ModuleFatigueRule.detectFatigue(regressionCount = 5, exercisesWithRecords = 10))
    }

    @Test
    fun `detectFatigue — 40 percent regressions → false`() {
        assertFalse(ModuleFatigueRule.detectFatigue(regressionCount = 4, exercisesWithRecords = 10))
    }

    @Test
    fun `detectFatigue — 60 percent regressions → true`() {
        assertTrue(ModuleFatigueRule.detectFatigue(regressionCount = 6, exercisesWithRecords = 10))
    }

    @Test
    fun `detectFatigue — 1 regression of 1 exercise (incomplete session) → true`() {
        assertTrue(ModuleFatigueRule.detectFatigue(regressionCount = 1, exercisesWithRecords = 1))
    }
}
