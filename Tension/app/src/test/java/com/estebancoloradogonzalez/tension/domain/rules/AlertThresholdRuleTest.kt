package com.estebancoloradogonzalez.tension.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertThresholdRuleTest {

    // Progression Alert

    @Test
    fun `isProgressionAlert returns true for rate below 40`() {
        assertTrue(AlertThresholdRule.isProgressionAlert(39.9))
    }

    @Test
    fun `isProgressionAlert returns false for rate at 40`() {
        assertFalse(AlertThresholdRule.isProgressionAlert(40.0))
    }

    @Test
    fun `isProgressionCrisis returns true for rate below 20`() {
        assertTrue(AlertThresholdRule.isProgressionCrisis(19.9))
    }

    @Test
    fun `isProgressionCrisis returns false for rate at 20`() {
        assertFalse(AlertThresholdRule.isProgressionCrisis(20.0))
    }

    // RIR

    @Test
    fun `isRirLow returns true for avgRir below 0_5`() {
        assertTrue(AlertThresholdRule.isRirLow(0.4))
    }

    @Test
    fun `isRirLow returns false for avgRir at 0_5`() {
        assertFalse(AlertThresholdRule.isRirLow(0.5))
    }

    @Test
    fun `isRirHigh returns true for avgRir above 1_8`() {
        assertTrue(AlertThresholdRule.isRirHigh(1.9))
    }

    @Test
    fun `isRirHigh returns false for avgRir at 1_8`() {
        assertFalse(AlertThresholdRule.isRirHigh(1.8))
    }

    @Test
    fun `isRirOutOfRange returns true when low or high`() {
        assertTrue(AlertThresholdRule.isRirOutOfRange(0.3))
        assertTrue(AlertThresholdRule.isRirOutOfRange(2.0))
    }

    @Test
    fun `isRirOutOfRange returns false when in optimal range`() {
        assertFalse(AlertThresholdRule.isRirOutOfRange(1.0))
    }

    // Adherence

    @Test
    fun `isAdherenceLow returns true for percentage below 60`() {
        assertTrue(AlertThresholdRule.isAdherenceLow(59.9))
    }

    @Test
    fun `isAdherenceLow returns false for percentage at 60`() {
        assertFalse(AlertThresholdRule.isAdherenceLow(60.0))
    }

    // Tonnage

    @Test
    fun `isTonnageAlert returns true for drop above 10 percent`() {
        assertTrue(AlertThresholdRule.isTonnageAlert(10.1))
    }

    @Test
    fun `isTonnageAlert returns false for drop at 10 percent`() {
        assertFalse(AlertThresholdRule.isTonnageAlert(10.0))
    }

    @Test
    fun `isTonnageCrisis returns true for drop above 20 percent`() {
        assertTrue(AlertThresholdRule.isTonnageCrisis(20.1))
    }

    @Test
    fun `isTonnageCrisis returns false for drop at 20 percent`() {
        assertFalse(AlertThresholdRule.isTonnageCrisis(20.0))
    }

    // Inactivity

    @Test
    fun `isInactivityAlert returns true for days above 10`() {
        assertTrue(AlertThresholdRule.isInactivityAlert(11))
    }

    @Test
    fun `isInactivityAlert returns false for days at 10`() {
        assertFalse(AlertThresholdRule.isInactivityAlert(10))
    }

    @Test
    fun `isInactivityCrisis returns true for days above 14`() {
        assertTrue(AlertThresholdRule.isInactivityCrisis(15))
    }

    @Test
    fun `isInactivityCrisis returns false for days at 14`() {
        assertFalse(AlertThresholdRule.isInactivityCrisis(14))
    }

    // Composite Level Functions

    @Test
    fun `progressionLevel returns CRISIS for rate below 20`() {
        assertEquals("CRISIS", AlertThresholdRule.progressionLevel(15.0))
    }

    @Test
    fun `progressionLevel returns MEDIUM_ALERT for rate between 20 and 40`() {
        assertEquals("MEDIUM_ALERT", AlertThresholdRule.progressionLevel(30.0))
    }

    @Test
    fun `progressionLevel returns null for rate at 40 or above`() {
        assertNull(AlertThresholdRule.progressionLevel(40.0))
        assertNull(AlertThresholdRule.progressionLevel(80.0))
    }

    @Test
    fun `tonnageLevel with deload always returns MEDIUM_ALERT never CRISIS`() {
        assertEquals("MEDIUM_ALERT", AlertThresholdRule.tonnageLevel(25.0, true))
        assertEquals("MEDIUM_ALERT", AlertThresholdRule.tonnageLevel(15.0, true))
    }

    @Test
    fun `tonnageLevel without deload returns CRISIS for drop above 20`() {
        assertEquals("CRISIS", AlertThresholdRule.tonnageLevel(25.0, false))
    }

    @Test
    fun `tonnageLevel without deload returns MEDIUM_ALERT for drop between 10 and 20`() {
        assertEquals("MEDIUM_ALERT", AlertThresholdRule.tonnageLevel(15.0, false))
    }

    @Test
    fun `tonnageLevel returns null for drop at 10 or below`() {
        assertNull(AlertThresholdRule.tonnageLevel(10.0, false))
        assertNull(AlertThresholdRule.tonnageLevel(5.0, true))
    }

    @Test
    fun `inactivityLevel returns CRISIS for days above 14`() {
        assertEquals("CRISIS", AlertThresholdRule.inactivityLevel(15))
    }

    @Test
    fun `inactivityLevel returns MEDIUM_ALERT for days between 10 and 14`() {
        assertEquals("MEDIUM_ALERT", AlertThresholdRule.inactivityLevel(12))
    }

    @Test
    fun `inactivityLevel returns null for days at 10 or below`() {
        assertNull(AlertThresholdRule.inactivityLevel(10))
        assertNull(AlertThresholdRule.inactivityLevel(5))
    }
}
