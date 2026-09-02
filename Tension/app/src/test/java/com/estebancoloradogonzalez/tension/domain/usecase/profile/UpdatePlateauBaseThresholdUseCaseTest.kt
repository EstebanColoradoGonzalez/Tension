package com.estebancoloradogonzalez.tension.domain.usecase.profile

import com.estebancoloradogonzalez.tension.domain.repository.ProfileRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePlateauBaseThresholdUseCaseTest {

    private val repository: ProfileRepository = mockk()
    private val useCase = UpdatePlateauBaseThresholdUseCase(repository)

    @Test
    fun `invoke with the lower bound updates the threshold successfully`() = runTest {
        coEvery { repository.updatePlateauBaseThreshold(any()) } just Runs

        val result = useCase(3)

        assertTrue(result.isSuccess)
        coVerify { repository.updatePlateauBaseThreshold(3) }
    }

    @Test
    fun `invoke with the default value updates the threshold successfully`() = runTest {
        coEvery { repository.updatePlateauBaseThreshold(any()) } just Runs

        val result = useCase(5)

        assertTrue(result.isSuccess)
        coVerify { repository.updatePlateauBaseThreshold(5) }
    }

    @Test
    fun `invoke with the upper bound updates the threshold successfully`() = runTest {
        coEvery { repository.updatePlateauBaseThreshold(any()) } just Runs

        val result = useCase(15)

        assertTrue(result.isSuccess)
        coVerify { repository.updatePlateauBaseThreshold(15) }
    }

    @Test
    fun `invoke below the range returns failure without touching the repository`() = runTest {
        val result = useCase(2)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        coVerify(exactly = 0) { repository.updatePlateauBaseThreshold(any()) }
    }

    @Test
    fun `invoke above the range returns failure without touching the repository`() = runTest {
        val result = useCase(16)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        coVerify(exactly = 0) { repository.updatePlateauBaseThreshold(any()) }
    }

    @Test
    fun `invoke propagates repository exception as failure`() = runTest {
        coEvery { repository.updatePlateauBaseThreshold(any()) } throws RuntimeException("DB error")

        val result = useCase(8)

        assertTrue(result.isFailure)
    }
}
