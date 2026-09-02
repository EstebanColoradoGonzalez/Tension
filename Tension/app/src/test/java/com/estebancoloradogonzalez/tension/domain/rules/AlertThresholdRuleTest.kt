package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.ProgressionDifficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlertThresholdRuleTest {

    // Progression rate — weighted by the difficulty of the exercise (CA-33.04)

    @Test
    fun `given a low difficulty exercise at 39 percent, when evaluated, then it alerts`() {
        assertEquals(
            "MEDIUM_ALERT",
            AlertThresholdRule.progressionLevel(39.9, ProgressionDifficulty.LOW),
        )
    }

    @Test
    fun `given a low difficulty exercise at 40 percent, when evaluated, then nothing is raised`() {
        assertNull(AlertThresholdRule.progressionLevel(40.0, ProgressionDifficulty.LOW))
    }

    @Test
    fun `given a low difficulty exercise at 19 percent, when evaluated, then it is a crisis`() {
        assertEquals(
            "CRISIS",
            AlertThresholdRule.progressionLevel(19.9, ProgressionDifficulty.LOW),
        )
    }

    @Test
    fun `given a medium difficulty exercise at 34 percent, when evaluated, then it alerts`() {
        assertEquals(
            "MEDIUM_ALERT",
            AlertThresholdRule.progressionLevel(34.9, ProgressionDifficulty.MEDIUM),
        )
    }

    @Test
    fun `given a medium difficulty exercise at 38 percent, when evaluated, then nothing is raised`() {
        assertNull(AlertThresholdRule.progressionLevel(38.0, ProgressionDifficulty.MEDIUM))
    }

    @Test
    fun `given a medium difficulty exercise at 14 percent, when evaluated, then it is a crisis`() {
        assertEquals(
            "CRISIS",
            AlertThresholdRule.progressionLevel(14.9, ProgressionDifficulty.MEDIUM),
        )
    }

    @Test
    fun `given a high difficulty exercise at 24 percent, when evaluated, then it alerts`() {
        assertEquals(
            "MEDIUM_ALERT",
            AlertThresholdRule.progressionLevel(24.9, ProgressionDifficulty.HIGH),
        )
    }

    @Test
    fun `given a high difficulty exercise at 30 percent, when evaluated, then nothing is raised`() {
        assertNull(AlertThresholdRule.progressionLevel(30.0, ProgressionDifficulty.HIGH))
    }

    @Test
    fun `given a high difficulty exercise at 9 percent, when evaluated, then it is a crisis`() {
        assertEquals(
            "CRISIS",
            AlertThresholdRule.progressionLevel(9.9, ProgressionDifficulty.HIGH),
        )
    }

    @Test
    fun `given the same 30 percent rate, when difficulty differs, then only the easy one alerts`() {
        assertEquals(
            "MEDIUM_ALERT",
            AlertThresholdRule.progressionLevel(30.0, ProgressionDifficulty.LOW),
        )
        assertNull(AlertThresholdRule.progressionLevel(30.0, ProgressionDifficulty.HIGH))
    }

    @Test
    fun `given the progression family, when its window is read, then it spans six weeks`() {
        assertEquals(6L, AlertThresholdRule.PROGRESSION_WINDOW_WEEKS)
    }

    @Test
    fun `given the progression family, when its minimum is read, then it needs three sessions`() {
        assertEquals(3, AlertThresholdRule.PROGRESSION_MIN_OBSERVATIONS)
    }

    // RIR — thresholds stay on the 0..2 scale, only the window widens

    @Test
    fun `given the rir family, when its window is read, then it spans three sessions`() {
        assertEquals(3, AlertThresholdRule.RIR_SUSTAINED_SESSIONS)
    }

    @Test
    fun `given the rir family, when its bounds are read, then they match the captured scale`() {
        assertEquals(0.5, AlertThresholdRule.RIR_LOW_THRESHOLD, 0.0)
        assertEquals(1.8, AlertThresholdRule.RIR_HIGH_THRESHOLD, 0.0)
    }

    @Test
    fun `given an average rir of 0 point 4, when evaluated, then it is low`() {
        assertEquals(true, AlertThresholdRule.isRirLow(0.4))
        assertEquals(false, AlertThresholdRule.isRirLow(0.5))
    }

    @Test
    fun `given an average rir of 1 point 9, when evaluated, then it is high`() {
        assertEquals(true, AlertThresholdRule.isRirHigh(1.9))
        assertEquals(false, AlertThresholdRule.isRirHigh(1.8))
    }

    @Test
    fun `given an average rir of 1, when evaluated, then it is inside the range`() {
        assertEquals(false, AlertThresholdRule.isRirOutOfRange(1.0))
    }

    // Adherence — consecutive weeks below the threshold

    @Test
    fun `given one week below the threshold, when evaluated, then nothing is raised`() {
        assertNull(AlertThresholdRule.adherenceLevel(1))
        assertNull(AlertThresholdRule.adherenceLevel(0))
    }

    @Test
    fun `given two consecutive weeks below the threshold, when evaluated, then it alerts`() {
        assertEquals("MEDIUM_ALERT", AlertThresholdRule.adherenceLevel(2))
    }

    @Test
    fun `given three or more consecutive weeks, when evaluated, then it is a crisis`() {
        assertEquals("CRISIS", AlertThresholdRule.adherenceLevel(3))
        assertEquals("CRISIS", AlertThresholdRule.adherenceLevel(4))
    }

    @Test
    fun `given a weekly percentage of 59, when evaluated, then adherence is low`() {
        assertEquals(true, AlertThresholdRule.isAdherenceLow(59.9))
        assertEquals(false, AlertThresholdRule.isAdherenceLow(60.0))
    }

    // Tonnage

    @Test
    fun `given a 12 percent drop, when evaluated, then nothing is raised`() {
        assertNull(AlertThresholdRule.tonnageLevel(12.0, false))
    }

    @Test
    fun `given a 16 percent drop, when evaluated, then it alerts`() {
        assertEquals("MEDIUM_ALERT", AlertThresholdRule.tonnageLevel(16.0, false))
    }

    @Test
    fun `given a 22 percent drop, when evaluated, then it is still not a crisis`() {
        assertEquals("MEDIUM_ALERT", AlertThresholdRule.tonnageLevel(22.0, false))
    }

    @Test
    fun `given a 26 percent drop, when evaluated, then it is a crisis`() {
        assertEquals("CRISIS", AlertThresholdRule.tonnageLevel(26.0, false))
    }

    @Test
    fun `given a planned deload, when tonnage collapses, then nothing is raised`() {
        assertNull(AlertThresholdRule.tonnageLevel(30.0, true))
        assertNull(AlertThresholdRule.tonnageLevel(16.0, true))
    }

    @Test
    fun `given the tonnage family, when its window is read, then it spans two microcycles`() {
        assertEquals(2, AlertThresholdRule.TONNAGE_MICROCYCLES)
    }

    // Inactivity

    @Test
    fun `given 12 days without a session, when evaluated, then nothing is raised`() {
        assertNull(AlertThresholdRule.inactivityLevel(12L))
    }

    @Test
    fun `given 15 days without a session, when evaluated, then it alerts`() {
        assertEquals("MEDIUM_ALERT", AlertThresholdRule.inactivityLevel(15L))
    }

    @Test
    fun `given 20 days without a session, when evaluated, then it is still not a crisis`() {
        assertEquals("MEDIUM_ALERT", AlertThresholdRule.inactivityLevel(20L))
    }

    @Test
    fun `given 22 days without a session, when evaluated, then it is a crisis`() {
        assertEquals("CRISIS", AlertThresholdRule.inactivityLevel(22L))
    }
}
