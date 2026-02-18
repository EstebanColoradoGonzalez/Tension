package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.model.Deload
import com.estebancoloradogonzalez.tension.domain.model.NextSession
import com.estebancoloradogonzalez.tension.domain.model.RotationResolver
import com.estebancoloradogonzalez.tension.domain.model.RotationState
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetNextSessionInfoUseCaseTest {

    private val repository: SessionRepository = mockk()
    private val useCase = GetNextSessionInfoUseCase(repository)

    @Test
    fun `invoke with position 1 returns module A`() = runTest {
        val rotationState = RotationState(
            microcyclePosition = 1,
            currentVersionModuleA = 1,
            currentVersionModuleB = 1,
            currentVersionModuleC = 1,
            microcycleCount = 0,
        )
        every { repository.getRotationState() } returns flowOf(rotationState)
        every { repository.getNextModuleVersionId() } returns flowOf(1L)
        every { repository.getActiveDeload() } returns flowOf(null)

        val result = useCase().first()

        assertEquals(NextSession("A", 1, 1L), result)
    }

    @Test
    fun `invoke with position 2 returns module B`() = runTest {
        val rotationState = RotationState(
            microcyclePosition = 2,
            currentVersionModuleA = 1,
            currentVersionModuleB = 2,
            currentVersionModuleC = 1,
            microcycleCount = 0,
        )
        every { repository.getRotationState() } returns flowOf(rotationState)
        every { repository.getNextModuleVersionId() } returns flowOf(5L)
        every { repository.getActiveDeload() } returns flowOf(null)

        val result = useCase().first()

        assertEquals(NextSession("B", 2, 5L), result)
    }

    @Test
    fun `invoke with position 3 returns module C`() = runTest {
        val rotationState = RotationState(
            microcyclePosition = 3,
            currentVersionModuleA = 1,
            currentVersionModuleB = 1,
            currentVersionModuleC = 3,
            microcycleCount = 0,
        )
        every { repository.getRotationState() } returns flowOf(rotationState)
        every { repository.getNextModuleVersionId() } returns flowOf(9L)
        every { repository.getActiveDeload() } returns flowOf(null)

        val result = useCase().first()

        assertEquals(NextSession("C", 3, 9L), result)
    }

    @Test
    fun `invoke with position 4 returns module A second cycle`() = runTest {
        val rotationState = RotationState(
            microcyclePosition = 4,
            currentVersionModuleA = 2,
            currentVersionModuleB = 1,
            currentVersionModuleC = 1,
            microcycleCount = 1,
        )
        every { repository.getRotationState() } returns flowOf(rotationState)
        every { repository.getNextModuleVersionId() } returns flowOf(2L)
        every { repository.getActiveDeload() } returns flowOf(null)

        val result = useCase().first()

        assertEquals(NextSession("A", 2, 2L), result)
    }

    @Test
    fun `invoke with position 5 returns module B`() = runTest {
        val rotationState = RotationState(
            microcyclePosition = 5,
            currentVersionModuleA = 2,
            currentVersionModuleB = 3,
            currentVersionModuleC = 1,
            microcycleCount = 1,
        )
        every { repository.getRotationState() } returns flowOf(rotationState)
        every { repository.getNextModuleVersionId() } returns flowOf(6L)
        every { repository.getActiveDeload() } returns flowOf(null)

        val result = useCase().first()

        assertEquals(NextSession("B", 3, 6L), result)
    }

    @Test
    fun `invoke with position 6 returns module C`() = runTest {
        val rotationState = RotationState(
            microcyclePosition = 6,
            currentVersionModuleA = 2,
            currentVersionModuleB = 3,
            currentVersionModuleC = 2,
            microcycleCount = 1,
        )
        every { repository.getRotationState() } returns flowOf(rotationState)
        every { repository.getNextModuleVersionId() } returns flowOf(8L)
        every { repository.getActiveDeload() } returns flowOf(null)

        val result = useCase().first()

        assertEquals(NextSession("C", 2, 8L), result)
    }

    @Test
    fun `invoke with active deload uses frozen versions`() = runTest {
        val rotationState = RotationState(
            microcyclePosition = 1,
            currentVersionModuleA = 3,
            currentVersionModuleB = 3,
            currentVersionModuleC = 3,
            microcycleCount = 5,
        )
        val deload = Deload(
            id = 1L,
            status = "ACTIVE",
            activationDate = "2026-02-01",
            completionDate = null,
            frozenVersionModuleA = 1,
            frozenVersionModuleB = 2,
            frozenVersionModuleC = 3,
        )
        every { repository.getRotationState() } returns flowOf(rotationState)
        every { repository.getNextModuleVersionId() } returns flowOf(10L)
        every { repository.getActiveDeload() } returns flowOf(deload)

        val result = useCase().first()

        // Module A, frozen version 1
        assertEquals(NextSession("A", 1, 10L), result)
    }

    @Test
    fun `invoke with null rotation state returns null`() = runTest {
        every { repository.getRotationState() } returns flowOf(null)

        val result = useCase().first()

        assertNull(result)
    }

    @Test
    fun `resolveModuleCode maps all 6 positions correctly`() {
        assertEquals("A", RotationResolver.resolveModuleCode(1))
        assertEquals("B", RotationResolver.resolveModuleCode(2))
        assertEquals("C", RotationResolver.resolveModuleCode(3))
        assertEquals("A", RotationResolver.resolveModuleCode(4))
        assertEquals("B", RotationResolver.resolveModuleCode(5))
        assertEquals("C", RotationResolver.resolveModuleCode(6))
    }

    @Test(expected = IllegalStateException::class)
    fun `resolveModuleCode throws for invalid position`() {
        RotationResolver.resolveModuleCode(7)
    }
}
