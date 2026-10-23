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

class CreateProfileUseCaseTest {

    private val repository: ProfileRepository = mockk()
    private val useCase = CreateProfileUseCase(repository)

    @Test
    fun `given only weight and height, when the profile is created, then it is created with both`() = runTest {
        // Given
        coEvery { repository.createProfile(any(), any()) } just Runs

        // When
        val result = useCase(78.5, 1.78)

        // Then
        assertTrue(result.isSuccess)
        coVerify { repository.createProfile(78.5, 1.78) }
    }

    @Test
    fun `given the minimum valid values, when the profile is created, then it is accepted`() = runTest {
        // Given
        coEvery { repository.createProfile(any(), any()) } just Runs

        // When
        val result = useCase(0.1, 0.1)

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `invoke with zero weight returns failure`() = runTest {
        val result = useCase(0.0, 1.75)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.createProfile(any(), any()) }
    }

    @Test
    fun `invoke with negative weight returns failure`() = runTest {
        val result = useCase(-5.0, 1.75)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.createProfile(any(), any()) }
    }

    @Test
    fun `invoke with zero height returns failure`() = runTest {
        val result = useCase(75.0, 0.0)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.createProfile(any(), any()) }
    }

    @Test
    fun `invoke with negative height returns failure`() = runTest {
        val result = useCase(75.0, -1.0)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.createProfile(any(), any()) }
    }

    @Test
    fun `invoke propagates repository exception as failure`() = runTest {
        coEvery { repository.createProfile(any(), any()) } throws RuntimeException("DB error")

        val result = useCase(75.0, 1.75)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeException)
    }
}
