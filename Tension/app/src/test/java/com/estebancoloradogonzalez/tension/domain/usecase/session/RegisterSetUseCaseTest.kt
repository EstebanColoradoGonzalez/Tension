package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RegisterSetUseCaseTest {

    private val repository: SessionRepository = mockk()
    private val useCase = RegisterSetUseCase(repository)

    @Test
    fun `invoke with valid data delegates to repository`() = runTest {
        coEvery { repository.registerSet(1L, 60.0, 8, 2) } just runs

        useCase(1L, 60.0, 8, 2)

        coVerify { repository.registerSet(1L, 60.0, 8, 2) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invoke with negative weight throws IllegalArgumentException`() = runTest {
        useCase(1L, -1.0, 8, 2)
    }

    @Test
    fun `invoke with zero weight succeeds`() = runTest {
        coEvery { repository.registerSet(1L, 0.0, 8, 2) } just runs

        useCase(1L, 0.0, 8, 2)

        coVerify { repository.registerSet(1L, 0.0, 8, 2) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invoke with zero reps throws IllegalArgumentException`() = runTest {
        useCase(1L, 60.0, 0, 2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invoke with negative reps throws IllegalArgumentException`() = runTest {
        useCase(1L, 60.0, -1, 2)
    }

    @Test
    fun `invoke with one rep succeeds`() = runTest {
        coEvery { repository.registerSet(1L, 60.0, 1, 2) } just runs

        useCase(1L, 60.0, 1, 2)

        coVerify { repository.registerSet(1L, 60.0, 1, 2) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invoke with rir below zero throws IllegalArgumentException`() = runTest {
        useCase(1L, 60.0, 8, -1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invoke with rir above five throws IllegalArgumentException`() = runTest {
        useCase(1L, 60.0, 8, 6)
    }

    @Test
    fun `invoke with rir zero succeeds`() = runTest {
        coEvery { repository.registerSet(1L, 60.0, 8, 0) } just runs

        useCase(1L, 60.0, 8, 0)

        coVerify { repository.registerSet(1L, 60.0, 8, 0) }
    }

    @Test
    fun `invoke with rir five succeeds`() = runTest {
        coEvery { repository.registerSet(1L, 60.0, 8, 5) } just runs

        useCase(1L, 60.0, 8, 5)

        coVerify { repository.registerSet(1L, 60.0, 8, 5) }
    }

    @Test(expected = IllegalStateException::class)
    fun `invoke propagates exception when exercise already has max sets`() = runTest {
        coEvery { repository.registerSet(any(), any(), any(), any()) } throws
            IllegalStateException("Exercise already has maximum sets registered")

        useCase(1L, 60.0, 8, 2)
    }
}
