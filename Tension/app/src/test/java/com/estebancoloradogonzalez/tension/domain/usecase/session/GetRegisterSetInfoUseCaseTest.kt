package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.model.RegisterSetInfo
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetRegisterSetInfoUseCaseTest {

    private val repository: SessionRepository = mockk()
    private val useCase = GetRegisterSetInfoUseCase(repository)

    @Test
    fun `invoke returns register set info when found`() = runTest {
        val expected = RegisterSetInfo(
            sessionExerciseId = 1L,
            exerciseId = 10L,
            exerciseName = "Press de banca",
            currentSetNumber = 2,
            totalSets = 4,
            lastWeightKg = 60.0,
            isBodyweight = false,
            isIsometric = false,
            isToTechnicalFailure = false,
        )
        coEvery { repository.getRegisterSetInfo(1L) } returns expected

        val result = useCase(1L)

        assertEquals(expected, result)
        coVerify { repository.getRegisterSetInfo(1L) }
    }

    @Test
    fun `invoke returns null when session exercise not found`() = runTest {
        coEvery { repository.getRegisterSetInfo(999L) } returns null

        val result = useCase(999L)

        assertNull(result)
        coVerify { repository.getRegisterSetInfo(999L) }
    }

    @Test
    fun `invoke returns null when exercise already completed`() = runTest {
        coEvery { repository.getRegisterSetInfo(1L) } returns null

        val result = useCase(1L)

        assertNull(result)
    }
}
