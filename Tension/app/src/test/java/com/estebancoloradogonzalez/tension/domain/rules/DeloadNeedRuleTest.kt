package com.estebancoloradogonzalez.tension.domain.rules

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeloadNeedRuleTest {

    @Test
    fun `needsDeload — fatigue detected with 0 affected → true`() {
        assertTrue(
            DeloadNeedRule.needsDeload(
                affectedCount = 0,
                totalCount = 11,
                fatigueDetected = true,
            ),
        )
    }

    @Test
    fun `needsDeload — fatigue detected with 100 percent affected → true`() {
        assertTrue(
            DeloadNeedRule.needsDeload(
                affectedCount = 11,
                totalCount = 11,
                fatigueDetected = true,
            ),
        )
    }

    @Test
    fun `needsDeload — no fatigue, 6 of 11 affected (54 pct) → true`() {
        assertTrue(
            DeloadNeedRule.needsDeload(
                affectedCount = 6,
                totalCount = 11,
                fatigueDetected = false,
            ),
        )
    }

    @Test
    fun `needsDeload — no fatigue, 5 of 11 affected (45 pct) → false`() {
        assertFalse(
            DeloadNeedRule.needsDeload(
                affectedCount = 5,
                totalCount = 11,
                fatigueDetected = false,
            ),
        )
    }

    @Test
    fun `needsDeload — no fatigue, 5 of 9 affected module C (55 pct) → true`() {
        assertTrue(
            DeloadNeedRule.needsDeload(
                affectedCount = 5,
                totalCount = 9,
                fatigueDetected = false,
            ),
        )
    }

    @Test
    fun `needsDeload — no fatigue, 0 total → false`() {
        assertFalse(
            DeloadNeedRule.needsDeload(
                affectedCount = 0,
                totalCount = 0,
                fatigueDetected = false,
            ),
        )
    }

    @Test
    fun `needsDeload — no fatigue, boundary 5 of 10 (exactly 50 pct) → true`() {
        assertTrue(
            DeloadNeedRule.needsDeload(
                affectedCount = 5,
                totalCount = 10,
                fatigueDetected = false,
            ),
        )
    }
}
