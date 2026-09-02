package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.ProgressionDifficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlateauThresholdRuleTest {

    // ──────────────────────────────────────────────────────────────
    // Base threshold — CA-32.01
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `given the system defaults, when the base threshold is read, then it is 5 sessions`() {
        assertEquals(5, PlateauThresholdRule.DEFAULT_BASE_THRESHOLD)
    }

    // ──────────────────────────────────────────────────────────────
    // Effective threshold — CA-32.03
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `given low difficulty and base 5, when the threshold is composed, then it is 5`() {
        assertEquals(
            5,
            PlateauThresholdRule.effectiveThreshold(5, ProgressionDifficulty.LOW),
        )
    }

    @Test
    fun `given medium difficulty and base 5, when the threshold is composed, then 7 point 5 rounds up to 8`() {
        assertEquals(
            8,
            PlateauThresholdRule.effectiveThreshold(5, ProgressionDifficulty.MEDIUM),
        )
    }

    @Test
    fun `given high difficulty and base 5, when the threshold is composed, then it is 10`() {
        assertEquals(
            10,
            PlateauThresholdRule.effectiveThreshold(5, ProgressionDifficulty.HIGH),
        )
    }

    @Test
    fun `given medium difficulty and base 3, when the threshold is composed, then 4 point 5 rounds up to 5`() {
        assertEquals(
            5,
            PlateauThresholdRule.effectiveThreshold(3, ProgressionDifficulty.MEDIUM),
        )
    }

    @Test
    fun `given medium difficulty and base 4, when the product is exact, then no rounding is applied`() {
        assertEquals(
            6,
            PlateauThresholdRule.effectiveThreshold(4, ProgressionDifficulty.MEDIUM),
        )
    }

    @Test
    fun `given high difficulty and the maximum base, when the threshold is composed, then it is 30`() {
        assertEquals(
            30,
            PlateauThresholdRule.effectiveThreshold(15, ProgressionDifficulty.HIGH),
        )
    }

    @Test
    fun `given a base persisted below the range, when the threshold is composed, then the base is coerced`() {
        assertEquals(
            3,
            PlateauThresholdRule.effectiveThreshold(1, ProgressionDifficulty.LOW),
        )
    }

    @Test
    fun `given a base persisted above the range, when the threshold is composed, then the base is coerced`() {
        assertEquals(
            15,
            PlateauThresholdRule.effectiveThreshold(40, ProgressionDifficulty.LOW),
        )
    }

    // ──────────────────────────────────────────────────────────────
    // Valid range — CA-32.04
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `given values inside the range, when validated, then they are accepted`() {
        assertTrue(PlateauThresholdRule.isValidBaseThreshold(3))
        assertTrue(PlateauThresholdRule.isValidBaseThreshold(5))
        assertTrue(PlateauThresholdRule.isValidBaseThreshold(15))
    }

    @Test
    fun `given values outside the range, when validated, then they are rejected`() {
        assertFalse(PlateauThresholdRule.isValidBaseThreshold(2))
        assertFalse(PlateauThresholdRule.isValidBaseThreshold(16))
        assertFalse(PlateauThresholdRule.isValidBaseThreshold(0))
        assertFalse(PlateauThresholdRule.isValidBaseThreshold(-1))
    }

    @Test
    fun `given values off the range, when coerced, then they land on the nearest bound`() {
        assertEquals(3, PlateauThresholdRule.coerceBaseThreshold(1))
        assertEquals(15, PlateauThresholdRule.coerceBaseThreshold(20))
        assertEquals(7, PlateauThresholdRule.coerceBaseThreshold(7))
    }

    @Test
    fun `given the declared range, when its bounds are read, then they are 3 and 15`() {
        assertEquals(3, PlateauThresholdRule.MIN_BASE_THRESHOLD)
        assertEquals(15, PlateauThresholdRule.MAX_BASE_THRESHOLD)
    }

    // ──────────────────────────────────────────────────────────────
    // Difficulty domain — CA-32.02, CA-32.03, CA-32.05
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `given the closed difficulty domain, when its multipliers are read, then they are 1, 1 point 5 and 2`() {
        assertEquals(1.0, ProgressionDifficulty.LOW.thresholdMultiplier, 0.0001)
        assertEquals(1.5, ProgressionDifficulty.MEDIUM.thresholdMultiplier, 0.0001)
        assertEquals(2.0, ProgressionDifficulty.HIGH.thresholdMultiplier, 0.0001)
    }

    @Test
    fun `given an absent or unknown code, when it is resolved, then the difficulty is MEDIUM`() {
        assertEquals(ProgressionDifficulty.MEDIUM, ProgressionDifficulty.fromCode(null))
        assertEquals(ProgressionDifficulty.MEDIUM, ProgressionDifficulty.fromCode(""))
        assertEquals(ProgressionDifficulty.MEDIUM, ProgressionDifficulty.fromCode("BAJA"))
    }

    @Test
    fun `given a persisted code, when it is resolved, then it maps to its difficulty`() {
        assertEquals(ProgressionDifficulty.LOW, ProgressionDifficulty.fromCode("LOW"))
        assertEquals(ProgressionDifficulty.MEDIUM, ProgressionDifficulty.fromCode("MEDIUM"))
        assertEquals(ProgressionDifficulty.HIGH, ProgressionDifficulty.fromCode("HIGH"))
    }
}
