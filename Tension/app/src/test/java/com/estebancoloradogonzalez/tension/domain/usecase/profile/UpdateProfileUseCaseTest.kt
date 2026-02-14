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

class UpdateProfileUseCaseTest {

    private val repository: ProfileRepository = mockk()
    private val useCase = UpdateProfileUseCase(repository)

    @Test
    fun `invoke with valid data updates profile successfully`() = runTest {
        coEvery { repository.updateProfile(any(), any()) } just Runs

        val result = useCase(1.80, ExperienceLevel.ADVANCED)

        assertTrue(result.isSuccess)
        coVerify { repository.updateProfile(1.80, ExperienceLevel.ADVANCED) }
    }

    @Test
    fun `invoke with zero height returns failure`() = runTest {
        val result = useCase(0.0, ExperienceLevel.INTERMEDIATE)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.updateProfile(any(), any()) }
    }

    @Test
    fun `invoke with negative height returns failure`() = runTest {
        val result = useCase(-1.5, ExperienceLevel.BEGINNER)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.updateProfile(any(), any()) }
    }

    @Test
    fun `invoke propagates repository exception as failure`() = runTest {
        coEvery { repository.updateProfile(any(), any()) } throws RuntimeException("DB error")

        val result = useCase(1.75, ExperienceLevel.INTERMEDIATE)

        assertTrue(result.isFailure)
    }
}
