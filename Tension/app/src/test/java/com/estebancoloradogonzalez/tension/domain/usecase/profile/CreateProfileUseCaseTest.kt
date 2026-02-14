package com.estebancoloradogonzalez.tension.domain.usecase.profile

import com.estebancoloradogonzalez.tension.domain.model.ExperienceLevel
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
    fun `invoke with valid data creates profile successfully`() = runTest {
        coEvery { repository.createProfile(any(), any(), any()) } just Runs

        val result = useCase(75.0, 1.75, ExperienceLevel.INTERMEDIATE)

        assertTrue(result.isSuccess)
        coVerify { repository.createProfile(75.0, 1.75, ExperienceLevel.INTERMEDIATE) }
    }

    @Test
    fun `invoke with zero weight returns failure`() = runTest {
        val result = useCase(0.0, 1.75, ExperienceLevel.BEGINNER)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.createProfile(any(), any(), any()) }
    }

    @Test
    fun `invoke with negative weight returns failure`() = runTest {
        val result = useCase(-5.0, 1.75, ExperienceLevel.BEGINNER)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.createProfile(any(), any(), any()) }
    }

    @Test
    fun `invoke with zero height returns failure`() = runTest {
        val result = useCase(75.0, 0.0, ExperienceLevel.ADVANCED)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.createProfile(any(), any(), any()) }
    }

    @Test
    fun `invoke with negative height returns failure`() = runTest {
        val result = useCase(75.0, -1.0, ExperienceLevel.ADVANCED)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.createProfile(any(), any(), any()) }
    }

    @Test
    fun `invoke propagates repository exception as failure`() = runTest {
        coEvery { repository.createProfile(any(), any(), any()) } throws RuntimeException("DB error")

        val result = useCase(75.0, 1.75, ExperienceLevel.BEGINNER)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeException)
    }
}
