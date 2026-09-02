package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.CorrectiveAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CorrectiveActionRuleTest {

    // With a threshold of 3 the escalation reproduces the original gradation (3 and 5):
    // what changed is the number it is measured against, not the mechanism.

    @Test
    fun `given no accumulated sessions, when recommended with threshold 3, then nothing is proposed`() {
        assertTrue(CorrectiveActionRule.recommend(0, 3).isEmpty())
        assertTrue(CorrectiveActionRule.recommend(1, 3).isEmpty())
        assertTrue(CorrectiveActionRule.recommend(2, 3).isEmpty())
    }

    @Test
    fun `given the plateau just declared, when recommended with threshold 3, then only the micro increment`() {
        val result = CorrectiveActionRule.recommend(3, 3)

        assertEquals(1, result.size)
        assertEquals(CorrectiveAction.MICRO_INCREMENT_OR_EXTEND_REPS, result[0])
    }

    @Test
    fun `given four sessions, when recommended with threshold 3, then rotation is still not proposed`() {
        val result = CorrectiveActionRule.recommend(4, 3)

        assertEquals(1, result.size)
        assertEquals(CorrectiveAction.MICRO_INCREMENT_OR_EXTEND_REPS, result[0])
    }

    @Test
    fun `given five sessions, when recommended with threshold 3, then both actions are proposed`() {
        val result = CorrectiveActionRule.recommend(5, 3)

        assertEquals(2, result.size)
        assertEquals(CorrectiveAction.MICRO_INCREMENT_OR_EXTEND_REPS, result[0])
        assertEquals(CorrectiveAction.ROTATE_VERSION, result[1])
    }

    @Test
    fun `given the plateau just declared, when recommended with threshold 5, then only the micro increment`() {
        val result = CorrectiveActionRule.recommend(5, 5)

        assertEquals(1, result.size)
        assertEquals(CorrectiveAction.MICRO_INCREMENT_OR_EXTEND_REPS, result[0])
    }

    @Test
    fun `given eight sessions, when recommended with threshold 5, then rotation joins in`() {
        val result = CorrectiveActionRule.recommend(8, 5)

        assertEquals(2, result.size)
        assertEquals(CorrectiveAction.ROTATE_VERSION, result[1])
    }

    @Test
    fun `given twelve sessions, when recommended with threshold 10, then rotation is not proposed yet`() {
        val result = CorrectiveActionRule.recommend(12, 10)

        assertEquals(1, result.size)
        assertEquals(CorrectiveAction.MICRO_INCREMENT_OR_EXTEND_REPS, result[0])
    }

    @Test
    fun `given fifteen sessions, when recommended with threshold 10, then both actions are proposed`() {
        val result = CorrectiveActionRule.recommend(15, 10)

        assertEquals(2, result.size)
        assertEquals(CorrectiveAction.ROTATE_VERSION, result[1])
    }

    @Test
    fun `given a threshold, when the rotation step is read, then it is half a threshold later`() {
        assertEquals(5, CorrectiveActionRule.rotateVersionStep(3))
        assertEquals(8, CorrectiveActionRule.rotateVersionStep(5))
        assertEquals(12, CorrectiveActionRule.rotateVersionStep(8))
        assertEquals(15, CorrectiveActionRule.rotateVersionStep(10))
    }
}
