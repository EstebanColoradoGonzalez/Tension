package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.ExerciseSessionData
import com.estebancoloradogonzalez.tension.domain.model.SetData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DoubleThresholdRuleTest {

    // ──────────────────────────────────────────────────────────────
    // Helper factories
    // ──────────────────────────────────────────────────────────────

    private fun sessionData(vararg sets: SetData) =
        ExerciseSessionData(sets = sets.toList())

    private fun set(weightKg: Double = 40.0, reps: Int = 10, rir: Int = 2) =
        SetData(weightKg = weightKg, reps = reps, rir = rir)

    // ──────────────────────────────────────────────────────────────
    // meetsDoubleThreshold tests
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `meetsDoubleThreshold — 4 sets x 12 reps x RIR 2 → true`() {
        val data = sessionData(
            set(reps = 12, rir = 2),
            set(reps = 12, rir = 2),
            set(reps = 12, rir = 2),
            set(reps = 12, rir = 2),
        )
        assertTrue(DoubleThresholdRule.meetsDoubleThreshold(data))
    }

    @Test
    fun `meetsDoubleThreshold — 3 of 4 sets ge 12 reps with avg RIR ge 2 → true`() {
        val data = sessionData(
            set(reps = 12, rir = 3),
            set(reps = 12, rir = 2),
            set(reps = 11, rir = 2),
            set(reps = 12, rir = 2),
        )
        assertTrue(DoubleThresholdRule.meetsDoubleThreshold(data))
    }

    @Test
    fun `meetsDoubleThreshold — 4 sets x 12 reps but avg RIR lt 2 → false`() {
        val data = sessionData(
            set(reps = 12, rir = 1),
            set(reps = 12, rir = 1),
            set(reps = 12, rir = 2),
            set(reps = 12, rir = 1),
        )
        assertFalse(DoubleThresholdRule.meetsDoubleThreshold(data))
    }

    @Test
    fun `meetsDoubleThreshold — 4 sets x 10 reps but avg RIR ge 2 → false`() {
        val data = sessionData(
            set(reps = 10, rir = 3),
            set(reps = 10, rir = 3),
            set(reps = 10, rir = 3),
            set(reps = 10, rir = 3),
        )
        assertFalse(DoubleThresholdRule.meetsDoubleThreshold(data))
    }

    @Test
    fun `meetsDoubleThreshold — only 1 of 4 sets ge 12 reps → false`() {
        val data = sessionData(
            set(reps = 10, rir = 3),
            set(reps = 10, rir = 3),
            set(reps = 10, rir = 3),
            set(reps = 12, rir = 3),
        )
        assertFalse(DoubleThresholdRule.meetsDoubleThreshold(data))
    }

    @Test
    fun `meetsDoubleThreshold — only 3 sets (incomplete) → false`() {
        val data = sessionData(
            set(reps = 12, rir = 3),
            set(reps = 12, rir = 3),
            set(reps = 12, rir = 3),
        )
        assertFalse(DoubleThresholdRule.meetsDoubleThreshold(data))
    }

    @Test
    fun `meetsDoubleThreshold — boundary RIR avg 1_75 → false`() {
        val data = sessionData(
            set(reps = 12, rir = 2),
            set(reps = 12, rir = 2),
            set(reps = 12, rir = 2),
            set(reps = 12, rir = 1),
        )
        assertFalse(DoubleThresholdRule.meetsDoubleThreshold(data))
    }

    // ──────────────────────────────────────────────────────────────
    // prescribeLoad tests
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `prescribeLoad — meets threshold with increment 2_5 (modules A or B)`() {
        val result = DoubleThresholdRule.prescribeLoad(
            currentAvgWeightKg = 40.0,
            loadIncrementKg = 2.5,
            meetsThreshold = true,
        )
        assertEquals(42.5, result, 0.001)
    }

    @Test
    fun `prescribeLoad — meets threshold with increment 5_0 (module C)`() {
        val result = DoubleThresholdRule.prescribeLoad(
            currentAvgWeightKg = 60.0,
            loadIncrementKg = 5.0,
            meetsThreshold = true,
        )
        assertEquals(65.0, result, 0.001)
    }

    @Test
    fun `prescribeLoad — does not meet threshold → same load`() {
        val result = DoubleThresholdRule.prescribeLoad(
            currentAvgWeightKg = 40.0,
            loadIncrementKg = 2.5,
            meetsThreshold = false,
        )
        assertEquals(40.0, result, 0.001)
    }

    @Test
    fun `prescribeLoad — meets threshold with zero base load`() {
        val result = DoubleThresholdRule.prescribeLoad(
            currentAvgWeightKg = 0.0,
            loadIncrementKg = 2.5,
            meetsThreshold = true,
        )
        assertEquals(2.5, result, 0.001)
    }
}
