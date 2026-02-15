package com.estebancoloradogonzalez.tension.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RotationResolverTest {

    @Test
    fun `advanceRotation from position 1 increments to 2`() {
        val current = RotationState(
            microcyclePosition = 1,
            currentVersionModuleA = 1,
            currentVersionModuleB = 1,
            currentVersionModuleC = 1,
            microcycleCount = 0,
        )
        val result = RotationResolver.advanceRotation(current)
        assertEquals(2, result.microcyclePosition)
        assertEquals(1, result.currentVersionModuleA)
        assertEquals(1, result.currentVersionModuleB)
        assertEquals(1, result.currentVersionModuleC)
        assertEquals(0, result.microcycleCount)
    }

    @Test
    fun `advanceRotation from position 5 increments to 6`() {
        val current = RotationState(
            microcyclePosition = 5,
            currentVersionModuleA = 2,
            currentVersionModuleB = 3,
            currentVersionModuleC = 1,
            microcycleCount = 1,
        )
        val result = RotationResolver.advanceRotation(current)
        assertEquals(6, result.microcyclePosition)
        assertEquals(2, result.currentVersionModuleA)
        assertEquals(3, result.currentVersionModuleB)
        assertEquals(1, result.currentVersionModuleC)
        assertEquals(1, result.microcycleCount)
    }

    @Test
    fun `advanceRotation from position 6 wraps to 1 and advances versions V1 to V2`() {
        val current = RotationState(
            microcyclePosition = 6,
            currentVersionModuleA = 1,
            currentVersionModuleB = 1,
            currentVersionModuleC = 1,
            microcycleCount = 0,
        )
        val result = RotationResolver.advanceRotation(current)
        assertEquals(1, result.microcyclePosition)
        assertEquals(2, result.currentVersionModuleA)
        assertEquals(2, result.currentVersionModuleB)
        assertEquals(2, result.currentVersionModuleC)
        assertEquals(1, result.microcycleCount)
    }

    @Test
    fun `advanceRotation from position 6 wraps versions V3 to V1`() {
        val current = RotationState(
            microcyclePosition = 6,
            currentVersionModuleA = 3,
            currentVersionModuleB = 3,
            currentVersionModuleC = 3,
            microcycleCount = 2,
        )
        val result = RotationResolver.advanceRotation(current)
        assertEquals(1, result.microcyclePosition)
        assertEquals(1, result.currentVersionModuleA)
        assertEquals(1, result.currentVersionModuleB)
        assertEquals(1, result.currentVersionModuleC)
        assertEquals(3, result.microcycleCount)
    }

    @Test
    fun `advanceRotation from position 6 with mixed versions advances each independently`() {
        val current = RotationState(
            microcyclePosition = 6,
            currentVersionModuleA = 1,
            currentVersionModuleB = 2,
            currentVersionModuleC = 3,
            microcycleCount = 5,
        )
        val result = RotationResolver.advanceRotation(current)
        assertEquals(1, result.microcyclePosition)
        assertEquals(2, result.currentVersionModuleA)
        assertEquals(3, result.currentVersionModuleB)
        assertEquals(1, result.currentVersionModuleC)
        assertEquals(6, result.microcycleCount)
    }

    @Test
    fun `advanceRotation from position 6 increments microcycleCount`() {
        val current = RotationState(
            microcyclePosition = 6,
            currentVersionModuleA = 2,
            currentVersionModuleB = 2,
            currentVersionModuleC = 2,
            microcycleCount = 10,
        )
        val result = RotationResolver.advanceRotation(current)
        assertEquals(1, result.microcyclePosition)
        assertEquals(11, result.microcycleCount)
    }
}
