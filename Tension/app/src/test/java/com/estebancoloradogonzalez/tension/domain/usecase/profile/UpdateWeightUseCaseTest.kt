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

class UpdateWeightUseCaseTest {

    private val repository: ProfileRepository = mockk()
    private val useCase = UpdateWeightUseCase(repository)

    @Test
    fun `invoke with valid weight updates successfully`() = runTest {
        coEvery { repository.updateWeight(any()) } just Runs

        val result = useCase(82.5)

        assertTrue(result.isSuccess)
        coVerify { repository.updateWeight(82.5) }
    }

    @Test
    fun `invoke with zero weight returns failure`() = runTest {
        val result = useCase(0.0)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.updateWeight(any()) }
    }

    @Test
    fun `invoke with negative weight returns failure`() = runTest {
        val result = useCase(-10.0)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.updateWeight(any()) }
    }

    @Test
    fun `invoke propagates repository exception as failure`() = runTest {
        coEvery { repository.updateWeight(any()) } throws RuntimeException("DB error")

        val result = useCase(80.0)

        assertTrue(result.isFailure)
    }
}
