package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.CorrectiveAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CorrectiveActionRuleTest {

    @Test
    fun `recommend — counter 0 → empty`() {
        assertTrue(CorrectiveActionRule.recommend(0).isEmpty())
    }

    @Test
    fun `recommend — counter 3 (plateau just detected) → empty`() {
        assertTrue(CorrectiveActionRule.recommend(3).isEmpty())
    }

    @Test
    fun `recommend — counter 4 → micro increment only`() {
        val result = CorrectiveActionRule.recommend(4)
        assertEquals(1, result.size)
        assertEquals(CorrectiveAction.MICRO_INCREMENT_OR_EXTEND_REPS, result[0])
    }

    @Test
    fun `recommend — counter 5 → micro increment only (no rotate yet)`() {
        val result = CorrectiveActionRule.recommend(5)
        assertEquals(1, result.size)
        assertEquals(CorrectiveAction.MICRO_INCREMENT_OR_EXTEND_REPS, result[0])
    }

    @Test
    fun `recommend — counter 6 → micro increment + rotate version (cumulative)`() {
        val result = CorrectiveActionRule.recommend(6)
        assertEquals(2, result.size)
        assertEquals(CorrectiveAction.MICRO_INCREMENT_OR_EXTEND_REPS, result[0])
        assertEquals(CorrectiveAction.ROTATE_VERSION, result[1])
    }

    @Test
    fun `recommend — counter 10 → both actions persist`() {
        val result = CorrectiveActionRule.recommend(10)
        assertEquals(2, result.size)
        assertEquals(CorrectiveAction.MICRO_INCREMENT_OR_EXTEND_REPS, result[0])
        assertEquals(CorrectiveAction.ROTATE_VERSION, result[1])
    }

    @Test
    fun `recommend — counter 1 → empty (progression without plateau)`() {
        assertTrue(CorrectiveActionRule.recommend(1).isEmpty())
    }
}
