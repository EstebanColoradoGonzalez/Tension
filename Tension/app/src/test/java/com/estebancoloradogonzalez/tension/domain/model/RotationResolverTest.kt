package com.estebancoloradogonzalez.tension.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RotationResolverTest {

    @Test
    fun `resolveRoutineIndex position 1 with 3 routines returns 0`() {
        assertEquals(0, RotationResolver.resolveRoutineIndex(1, 3))
    }

    @Test
    fun `resolveRoutineIndex position 3 with 3 routines returns 2`() {
        assertEquals(2, RotationResolver.resolveRoutineIndex(3, 3))
    }

    @Test
    fun `resolveRoutineIndex position 4 with 3 routines wraps to 0`() {
        assertEquals(0, RotationResolver.resolveRoutineIndex(4, 3))
    }

    @Test
    fun `microcycleSize equals routineCount`() {
        assertEquals(3, RotationResolver.microcycleSize(3))
        assertEquals(5, RotationResolver.microcycleSize(5))
    }

    @Test
    fun `advanceRotation from position 1 with 3 routines increments to 2`() {
        val current = RotationState(microcyclePosition = 1, microcycleCount = 0)
        val result = RotationResolver.advanceRotation(current, routineCount = 3)
        assertEquals(2, result.microcyclePosition)
        assertEquals(0, result.microcycleCount)
    }

    @Test
    fun `advanceRotation from position 2 with 3 routines increments to 3`() {
        val current = RotationState(microcyclePosition = 2, microcycleCount = 0)
        val result = RotationResolver.advanceRotation(current, routineCount = 3)
        assertEquals(3, result.microcyclePosition)
        assertEquals(0, result.microcycleCount)
    }

    @Test
    fun `advanceRotation from last position wraps to 1 and increments microcycleCount`() {
        val current = RotationState(microcyclePosition = 3, microcycleCount = 0)
        val result = RotationResolver.advanceRotation(current, routineCount = 3)
        assertEquals(1, result.microcyclePosition)
        assertEquals(1, result.microcycleCount)
    }

    @Test
    fun `advanceRotation preserves microcycleCount when not wrapping`() {
        val current = RotationState(microcyclePosition = 1, microcycleCount = 5)
        val result = RotationResolver.advanceRotation(current, routineCount = 3)
        assertEquals(2, result.microcyclePosition)
        assertEquals(5, result.microcycleCount)
    }

    @Test
    fun `advanceRotation at last position wraps and increments microcycleCount`() {
        val current = RotationState(microcyclePosition = 3, microcycleCount = 2)
        val result = RotationResolver.advanceRotation(current, routineCount = 3)
        assertEquals(1, result.microcyclePosition)
        assertEquals(3, result.microcycleCount)
    }

    @Test
    fun `advanceRotation with single routine always wraps`() {
        val current = RotationState(microcyclePosition = 1, microcycleCount = 0)
        val result = RotationResolver.advanceRotation(current, routineCount = 1)
        assertEquals(1, result.microcyclePosition)
        assertEquals(1, result.microcycleCount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `resolveRoutineIndex with position 0 throws`() {
        RotationResolver.resolveRoutineIndex(0, 3)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `resolveRoutineIndex with negative position throws`() {
        RotationResolver.resolveRoutineIndex(-1, 3)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `resolveRoutineIndex with zero routineCount throws`() {
        RotationResolver.resolveRoutineIndex(1, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `advanceRotation with zero routineCount throws`() {
        RotationResolver.advanceRotation(RotationState(1, 0), routineCount = 0)
    }
}
