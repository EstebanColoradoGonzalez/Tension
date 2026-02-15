package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.ExerciseSessionData
import com.estebancoloradogonzalez.tension.domain.model.ProgressionClassification
import com.estebancoloradogonzalez.tension.domain.model.SetData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionClassificationRuleTest {

    // ──────────────────────────────────────────────────────────────
    // Helper factories
    // ──────────────────────────────────────────────────────────────

    private fun sessionData(vararg sets: SetData) =
        ExerciseSessionData(sets = sets.toList())

    private fun set(weightKg: Double = 40.0, reps: Int = 10, rir: Int = 2) =
        SetData(weightKg = weightKg, reps = reps, rir = rir)

    // ──────────────────────────────────────────────────────────────
    // classify() — dispatcher tests
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `classify returns null when previous is null`() {
        val current = sessionData(set())
        val result = ProgressionClassificationRule.classify(
            current = current,
            previous = null,
            isBodyweight = false,
            isIsometric = false,
        )
        assertNull(result)
    }

    @Test
    fun `classify returns null when previous sets are empty`() {
        val current = sessionData(set())
        val previous = sessionData()
        val result = ProgressionClassificationRule.classify(
            current = current,
            previous = previous,
            isBodyweight = false,
            isIsometric = false,
        )
        assertNull(result)
    }

    @Test
    fun `classify returns null when current sets are empty`() {
        val current = sessionData()
        val previous = sessionData(set())
        val result = ProgressionClassificationRule.classify(
            current = current,
            previous = previous,
            isBodyweight = false,
            isIsometric = false,
        )
        assertNull(result)
    }

    @Test
    fun `classify dispatches to isometric when isIsometric is true`() {
        val current = sessionData(
            set(weightKg = 0.0, reps = 40, rir = 2),
            set(weightKg = 0.0, reps = 40, rir = 2),
            set(weightKg = 0.0, reps = 40, rir = 2),
            set(weightKg = 0.0, reps = 40, rir = 2),
        )
        val previous = sessionData(
            set(weightKg = 0.0, reps = 35, rir = 2),
            set(weightKg = 0.0, reps = 35, rir = 2),
            set(weightKg = 0.0, reps = 35, rir = 2),
            set(weightKg = 0.0, reps = 35, rir = 2),
        )
        val result = ProgressionClassificationRule.classify(
            current = current,
            previous = previous,
            isBodyweight = true,
            isIsometric = true,
        )
        assertEquals(ProgressionClassification.POSITIVE_PROGRESSION, result)
    }

    // ──────────────────────────────────────────────────────────────
    // classifyStandard tests
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `standard — weight increased with stable RIR → POSITIVE_PROGRESSION`() {
        val current = sessionData(
            set(weightKg = 42.5, reps = 10, rir = 2),
            set(weightKg = 42.5, reps = 10, rir = 2),
            set(weightKg = 42.5, reps = 10, rir = 2),
            set(weightKg = 42.5, reps = 10, rir = 2),
        )
        val previous = sessionData(
            set(weightKg = 40.0, reps = 10, rir = 2),
            set(weightKg = 40.0, reps = 10, rir = 2),
            set(weightKg = 40.0, reps = 10, rir = 2),
            set(weightKg = 40.0, reps = 10, rir = 2),
        )
        assertEquals(
            ProgressionClassification.POSITIVE_PROGRESSION,
            ProgressionClassificationRule.classify(current, previous, false, false),
        )
    }

    @Test
    fun `standard — same weight more reps stable RIR → POSITIVE_PROGRESSION`() {
        val current = sessionData(
            set(weightKg = 40.0, reps = 12, rir = 2),
            set(weightKg = 40.0, reps = 12, rir = 2),
            set(weightKg = 40.0, reps = 11, rir = 2),
            set(weightKg = 40.0, reps = 10, rir = 2),
        )
        val previous = sessionData(
            set(weightKg = 40.0, reps = 10, rir = 2),
            set(weightKg = 40.0, reps = 10, rir = 2),
            set(weightKg = 40.0, reps = 10, rir = 2),
            set(weightKg = 40.0, reps = 10, rir = 2),
        )
        assertEquals(
            ProgressionClassification.POSITIVE_PROGRESSION,
            ProgressionClassificationRule.classify(current, previous, false, false),
        )
    }

    @Test
    fun `standard — weight increased with RIR rise ge 1_5 → MAINTENANCE`() {
        val current = sessionData(
            set(weightKg = 42.5, reps = 10, rir = 4),
            set(weightKg = 42.5, reps = 10, rir = 4),
            set(weightKg = 42.5, reps = 10, rir = 4),
            set(weightKg = 42.5, reps = 10, rir = 4),
        )
        val previous = sessionData(
            set(weightKg = 40.0, reps = 10, rir = 2),
            set(weightKg = 40.0, reps = 10, rir = 2),
            set(weightKg = 40.0, reps = 10, rir = 2),
            set(weightKg = 40.0, reps = 10, rir = 2),
        )
        assertEquals(
            ProgressionClassification.MAINTENANCE,
            ProgressionClassificationRule.classify(current, previous, false, false),
        )
    }

    @Test
    fun `standard — same weight same reps stable RIR → MAINTENANCE`() {
        val current = sessionData(
            set(weightKg = 40.0, reps = 10, rir = 2),
            set(weightKg = 40.0, reps = 10, rir = 2),
            set(weightKg = 40.0, reps = 10, rir = 2),
            set(weightKg = 40.0, reps = 10, rir = 2),
        )
        val previous = sessionData(
            set(weightKg = 40.0, reps = 10, rir = 2),
            set(weightKg = 40.0, reps = 10, rir = 2),
            set(weightKg = 40.0, reps = 10, rir = 2),
            set(weightKg = 40.0, reps = 10, rir = 2),
        )
        assertEquals(
            ProgressionClassification.MAINTENANCE,
            ProgressionClassificationRule.classify(current, previous, false, false),
        )
    }

    @Test
    fun `standard — weight decreased → REGRESSION`() {
        val current = sessionData(
            set(weightKg = 37.5, reps = 10, rir = 2),
            set(weightKg = 37.5, reps = 10, rir = 2),
            set(weightKg = 37.5, reps = 10, rir = 2),
            set(weightKg = 37.5, reps = 10, rir = 2),
        )
        val previous = sessionData(
            set(weightKg = 40.0, reps = 10, rir = 2),
            set(weightKg = 40.0, reps = 10, rir = 2),
            set(weightKg = 40.0, reps = 10, rir = 2),
            set(weightKg = 40.0, reps = 10, rir = 2),
        )
        assertEquals(
            ProgressionClassification.REGRESSION,
            ProgressionClassificationRule.classify(current, previous, false, false),
        )
    }

    @Test
    fun `standard — same weight fewer reps → REGRESSION`() {
        val current = sessionData(
            set(weightKg = 40.0, reps = 8, rir = 2),
            set(weightKg = 40.0, reps = 8, rir = 2),
            set(weightKg = 40.0, reps = 8, rir = 2),
            set(weightKg = 40.0, reps = 8, rir = 2),
        )
        val previous = sessionData(
            set(weightKg = 40.0, reps = 10, rir = 2),
            set(weightKg = 40.0, reps = 10, rir = 2),
            set(weightKg = 40.0, reps = 10, rir = 2),
            set(weightKg = 40.0, reps = 10, rir = 2),
        )
        assertEquals(
            ProgressionClassification.REGRESSION,
            ProgressionClassificationRule.classify(current, previous, false, false),
        )
    }

    @Test
    fun `standard — same weight RIR rise ge 1_5 → REGRESSION`() {
        val current = sessionData(
            set(weightKg = 40.0, reps = 10, rir = 4),
            set(weightKg = 40.0, reps = 10, rir = 4),
            set(weightKg = 40.0, reps = 10, rir = 4),
            set(weightKg = 40.0, reps = 10, rir = 4),
        )
        val previous = sessionData(
            set(weightKg = 40.0, reps = 10, rir = 2),
            set(weightKg = 40.0, reps = 10, rir = 2),
            set(weightKg = 40.0, reps = 10, rir = 2),
            set(weightKg = 40.0, reps = 10, rir = 2),
        )
        assertEquals(
            ProgressionClassification.REGRESSION,
            ProgressionClassificationRule.classify(current, previous, false, false),
        )
    }

    @Test
    fun `standard — weight increased reps decreased stable RIR → POSITIVE_PROGRESSION`() {
        val current = sessionData(
            set(weightKg = 42.5, reps = 8, rir = 2),
            set(weightKg = 42.5, reps = 8, rir = 2),
            set(weightKg = 42.5, reps = 8, rir = 2),
            set(weightKg = 42.5, reps = 8, rir = 2),
        )
        val previous = sessionData(
            set(weightKg = 40.0, reps = 12, rir = 2),
            set(weightKg = 40.0, reps = 12, rir = 2),
            set(weightKg = 40.0, reps = 12, rir = 2),
            set(weightKg = 40.0, reps = 12, rir = 2),
        )
        assertEquals(
            ProgressionClassification.POSITIVE_PROGRESSION,
            ProgressionClassificationRule.classify(current, previous, false, false),
        )
    }

    // ──────────────────────────────────────────────────────────────
    // classifyBodyweight tests
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `bodyweight — total reps increased stable RIR → POSITIVE_PROGRESSION`() {
        val current = sessionData(
            set(weightKg = 0.0, reps = 15, rir = 2),
            set(weightKg = 0.0, reps = 14, rir = 2),
            set(weightKg = 0.0, reps = 13, rir = 2),
            set(weightKg = 0.0, reps = 12, rir = 2),
        )
        val previous = sessionData(
            set(weightKg = 0.0, reps = 12, rir = 2),
            set(weightKg = 0.0, reps = 12, rir = 2),
            set(weightKg = 0.0, reps = 12, rir = 2),
            set(weightKg = 0.0, reps = 12, rir = 2),
        )
        assertEquals(
            ProgressionClassification.POSITIVE_PROGRESSION,
            ProgressionClassificationRule.classify(current, previous, true, false),
        )
    }

    @Test
    fun `bodyweight — same reps stable RIR → MAINTENANCE`() {
        val current = sessionData(
            set(weightKg = 0.0, reps = 12, rir = 2),
            set(weightKg = 0.0, reps = 12, rir = 2),
            set(weightKg = 0.0, reps = 12, rir = 2),
            set(weightKg = 0.0, reps = 12, rir = 2),
        )
        val previous = sessionData(
            set(weightKg = 0.0, reps = 12, rir = 2),
            set(weightKg = 0.0, reps = 12, rir = 2),
            set(weightKg = 0.0, reps = 12, rir = 2),
            set(weightKg = 0.0, reps = 12, rir = 2),
        )
        assertEquals(
            ProgressionClassification.MAINTENANCE,
            ProgressionClassificationRule.classify(current, previous, true, false),
        )
    }

    @Test
    fun `bodyweight — fewer reps → REGRESSION`() {
        val current = sessionData(
            set(weightKg = 0.0, reps = 10, rir = 2),
            set(weightKg = 0.0, reps = 10, rir = 2),
            set(weightKg = 0.0, reps = 10, rir = 2),
            set(weightKg = 0.0, reps = 10, rir = 2),
        )
        val previous = sessionData(
            set(weightKg = 0.0, reps = 12, rir = 2),
            set(weightKg = 0.0, reps = 12, rir = 2),
            set(weightKg = 0.0, reps = 12, rir = 2),
            set(weightKg = 0.0, reps = 12, rir = 2),
        )
        assertEquals(
            ProgressionClassification.REGRESSION,
            ProgressionClassificationRule.classify(current, previous, true, false),
        )
    }

    @Test
    fun `bodyweight — same reps RIR rise ge 1_5 → REGRESSION`() {
        val current = sessionData(
            set(weightKg = 0.0, reps = 12, rir = 4),
            set(weightKg = 0.0, reps = 12, rir = 4),
            set(weightKg = 0.0, reps = 12, rir = 4),
            set(weightKg = 0.0, reps = 12, rir = 4),
        )
        val previous = sessionData(
            set(weightKg = 0.0, reps = 12, rir = 2),
            set(weightKg = 0.0, reps = 12, rir = 2),
            set(weightKg = 0.0, reps = 12, rir = 2),
            set(weightKg = 0.0, reps = 12, rir = 2),
        )
        assertEquals(
            ProgressionClassification.REGRESSION,
            ProgressionClassificationRule.classify(current, previous, true, false),
        )
    }

    @Test
    fun `bodyweight — more reps but RIR rise ge 1_5 → MAINTENANCE`() {
        val current = sessionData(
            set(weightKg = 0.0, reps = 14, rir = 4),
            set(weightKg = 0.0, reps = 14, rir = 4),
            set(weightKg = 0.0, reps = 14, rir = 4),
            set(weightKg = 0.0, reps = 14, rir = 4),
        )
        val previous = sessionData(
            set(weightKg = 0.0, reps = 12, rir = 2),
            set(weightKg = 0.0, reps = 12, rir = 2),
            set(weightKg = 0.0, reps = 12, rir = 2),
            set(weightKg = 0.0, reps = 12, rir = 2),
        )
        assertEquals(
            ProgressionClassification.MAINTENANCE,
            ProgressionClassificationRule.classify(current, previous, true, false),
        )
    }

    // ──────────────────────────────────────────────────────────────
    // classifyIsometric tests
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `isometric — seconds increased stable RIR → POSITIVE_PROGRESSION`() {
        val current = sessionData(
            set(weightKg = 0.0, reps = 40, rir = 2),
            set(weightKg = 0.0, reps = 40, rir = 2),
            set(weightKg = 0.0, reps = 40, rir = 2),
            set(weightKg = 0.0, reps = 40, rir = 2),
        )
        val previous = sessionData(
            set(weightKg = 0.0, reps = 35, rir = 2),
            set(weightKg = 0.0, reps = 35, rir = 2),
            set(weightKg = 0.0, reps = 35, rir = 2),
            set(weightKg = 0.0, reps = 35, rir = 2),
        )
        assertEquals(
            ProgressionClassification.POSITIVE_PROGRESSION,
            ProgressionClassificationRule.classify(current, previous, true, true),
        )
    }

    @Test
    fun `isometric — same seconds stable RIR → MAINTENANCE`() {
        val current = sessionData(
            set(weightKg = 0.0, reps = 35, rir = 2),
            set(weightKg = 0.0, reps = 35, rir = 2),
            set(weightKg = 0.0, reps = 35, rir = 2),
            set(weightKg = 0.0, reps = 35, rir = 2),
        )
        val previous = sessionData(
            set(weightKg = 0.0, reps = 35, rir = 2),
            set(weightKg = 0.0, reps = 35, rir = 2),
            set(weightKg = 0.0, reps = 35, rir = 2),
            set(weightKg = 0.0, reps = 35, rir = 2),
        )
        assertEquals(
            ProgressionClassification.MAINTENANCE,
            ProgressionClassificationRule.classify(current, previous, true, true),
        )
    }

    @Test
    fun `isometric — seconds decreased → REGRESSION`() {
        val current = sessionData(
            set(weightKg = 0.0, reps = 30, rir = 2),
            set(weightKg = 0.0, reps = 30, rir = 2),
            set(weightKg = 0.0, reps = 30, rir = 2),
            set(weightKg = 0.0, reps = 30, rir = 2),
        )
        val previous = sessionData(
            set(weightKg = 0.0, reps = 35, rir = 2),
            set(weightKg = 0.0, reps = 35, rir = 2),
            set(weightKg = 0.0, reps = 35, rir = 2),
            set(weightKg = 0.0, reps = 35, rir = 2),
        )
        assertEquals(
            ProgressionClassification.REGRESSION,
            ProgressionClassificationRule.classify(current, previous, true, true),
        )
    }

    @Test
    fun `isometric — same seconds RIR rise ge 1_5 → REGRESSION`() {
        val current = sessionData(
            set(weightKg = 0.0, reps = 35, rir = 4),
            set(weightKg = 0.0, reps = 35, rir = 4),
            set(weightKg = 0.0, reps = 35, rir = 4),
            set(weightKg = 0.0, reps = 35, rir = 4),
        )
        val previous = sessionData(
            set(weightKg = 0.0, reps = 35, rir = 2),
            set(weightKg = 0.0, reps = 35, rir = 2),
            set(weightKg = 0.0, reps = 35, rir = 2),
            set(weightKg = 0.0, reps = 35, rir = 2),
        )
        assertEquals(
            ProgressionClassification.REGRESSION,
            ProgressionClassificationRule.classify(current, previous, true, true),
        )
    }

    @Test
    fun `isometric — more seconds but RIR rise ge 1_5 → MAINTENANCE`() {
        val current = sessionData(
            set(weightKg = 0.0, reps = 40, rir = 4),
            set(weightKg = 0.0, reps = 40, rir = 4),
            set(weightKg = 0.0, reps = 40, rir = 4),
            set(weightKg = 0.0, reps = 40, rir = 4),
        )
        val previous = sessionData(
            set(weightKg = 0.0, reps = 35, rir = 2),
            set(weightKg = 0.0, reps = 35, rir = 2),
            set(weightKg = 0.0, reps = 35, rir = 2),
            set(weightKg = 0.0, reps = 35, rir = 2),
        )
        assertEquals(
            ProgressionClassification.MAINTENANCE,
            ProgressionClassificationRule.classify(current, previous, true, true),
        )
    }

    // ──────────────────────────────────────────────────────────────
    // isIsometricMastered tests
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `isIsometricMastered — 4 sets all ge 45s → true`() {
        val data = sessionData(
            set(reps = 45), set(reps = 50), set(reps = 45), set(reps = 46),
        )
        assertTrue(ProgressionClassificationRule.isIsometricMastered(data))
    }

    @Test
    fun `isIsometricMastered — 4 sets one below 45s → false`() {
        val data = sessionData(
            set(reps = 45), set(reps = 44), set(reps = 45), set(reps = 45),
        )
        assertFalse(ProgressionClassificationRule.isIsometricMastered(data))
    }

    @Test
    fun `isIsometricMastered — 3 sets all ge 45s → false (incomplete)`() {
        val data = sessionData(
            set(reps = 45), set(reps = 45), set(reps = 45),
        )
        assertFalse(ProgressionClassificationRule.isIsometricMastered(data))
    }

    // ──────────────────────────────────────────────────────────────
    // resolveNewProgressionState tests
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `state — isometric mastered → MASTERED, counter 0`() {
        val (status, counter) = ProgressionClassificationRule.resolveNewProgressionState(
            currentStatus = "IN_PROGRESSION",
            currentCounter = 2,
            classification = ProgressionClassification.POSITIVE_PROGRESSION,
            isIsometric = true,
            isMastered = true,
        )
        assertEquals("MASTERED", status)
        assertEquals(0, counter)
    }

    @Test
    fun `state — null classification → no change`() {
        val (status, counter) = ProgressionClassificationRule.resolveNewProgressionState(
            currentStatus = "NO_HISTORY",
            currentCounter = 0,
            classification = null,
            isIsometric = false,
            isMastered = false,
        )
        assertEquals("NO_HISTORY", status)
        assertEquals(0, counter)
    }

    @Test
    fun `state — IN_DELOAD with any classification → no change`() {
        val (status, counter) = ProgressionClassificationRule.resolveNewProgressionState(
            currentStatus = "IN_DELOAD",
            currentCounter = 1,
            classification = ProgressionClassification.REGRESSION,
            isIsometric = false,
            isMastered = false,
        )
        assertEquals("IN_DELOAD", status)
        assertEquals(1, counter)
    }

    @Test
    fun `state — MASTERED with any classification → no change (terminal)`() {
        val (status, counter) = ProgressionClassificationRule.resolveNewProgressionState(
            currentStatus = "MASTERED",
            currentCounter = 0,
            classification = ProgressionClassification.REGRESSION,
            isIsometric = true,
            isMastered = false,
        )
        assertEquals("MASTERED", status)
        assertEquals(0, counter)
    }

    @Test
    fun `state — POSITIVE_PROGRESSION → IN_PROGRESSION, counter 0`() {
        val (status, counter) = ProgressionClassificationRule.resolveNewProgressionState(
            currentStatus = "IN_PROGRESSION",
            currentCounter = 2,
            classification = ProgressionClassification.POSITIVE_PROGRESSION,
            isIsometric = false,
            isMastered = false,
        )
        assertEquals("IN_PROGRESSION", status)
        assertEquals(0, counter)
    }

    @Test
    fun `state — NO_HISTORY + MAINTENANCE → IN_PROGRESSION, counter 1`() {
        val (status, counter) = ProgressionClassificationRule.resolveNewProgressionState(
            currentStatus = "NO_HISTORY",
            currentCounter = 0,
            classification = ProgressionClassification.MAINTENANCE,
            isIsometric = false,
            isMastered = false,
        )
        assertEquals("IN_PROGRESSION", status)
        assertEquals(1, counter)
    }

    @Test
    fun `state — IN_PROGRESSION + MAINTENANCE x2 → counter 2, status unchanged`() {
        val (status1, counter1) = ProgressionClassificationRule.resolveNewProgressionState(
            currentStatus = "IN_PROGRESSION",
            currentCounter = 0,
            classification = ProgressionClassification.MAINTENANCE,
            isIsometric = false,
            isMastered = false,
        )
        assertEquals("IN_PROGRESSION", status1)
        assertEquals(1, counter1)

        val (status2, counter2) = ProgressionClassificationRule.resolveNewProgressionState(
            currentStatus = status1,
            currentCounter = counter1,
            classification = ProgressionClassification.MAINTENANCE,
            isIsometric = false,
            isMastered = false,
        )
        assertEquals("IN_PROGRESSION", status2)
        assertEquals(2, counter2)
    }

    @Test
    fun `state — IN_PROGRESSION + REGRESSION x3 → IN_PLATEAU, counter 3`() {
        var status = "IN_PROGRESSION"
        var counter = 0

        repeat(3) {
            val (newStatus, newCounter) =
                ProgressionClassificationRule.resolveNewProgressionState(
                    currentStatus = status,
                    currentCounter = counter,
                    classification = ProgressionClassification.REGRESSION,
                    isIsometric = false,
                    isMastered = false,
                )
            status = newStatus
            counter = newCounter
        }

        assertEquals("IN_PLATEAU", status)
        assertEquals(3, counter)
    }

    @Test
    fun `state — IN_PLATEAU + POSITIVE_PROGRESSION → IN_PROGRESSION, counter 0`() {
        val (status, counter) = ProgressionClassificationRule.resolveNewProgressionState(
            currentStatus = "IN_PLATEAU",
            currentCounter = 3,
            classification = ProgressionClassification.POSITIVE_PROGRESSION,
            isIsometric = false,
            isMastered = false,
        )
        assertEquals("IN_PROGRESSION", status)
        assertEquals(0, counter)
    }

    @Test
    fun `state — counter continues past 3 for escalated actions`() {
        var status = "IN_PLATEAU"
        var counter = 3

        val (newStatus, newCounter) = ProgressionClassificationRule.resolveNewProgressionState(
            currentStatus = status,
            currentCounter = counter,
            classification = ProgressionClassification.MAINTENANCE,
            isIsometric = false,
            isMastered = false,
        )

        assertEquals("IN_PLATEAU", newStatus)
        assertEquals(4, newCounter)
    }
}
