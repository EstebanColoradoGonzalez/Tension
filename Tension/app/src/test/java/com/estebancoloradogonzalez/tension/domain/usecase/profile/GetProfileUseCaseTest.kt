package com.estebancoloradogonzalez.tension.domain.usecase.profile

import com.estebancoloradogonzalez.tension.domain.model.ExperienceLevel
import com.estebancoloradogonzalez.tension.domain.model.Profile
import com.estebancoloradogonzalez.tension.domain.repository.ProfileRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class GetProfileUseCaseTest {

    private val repository: ProfileRepository = mockk()
    private val useCase = GetProfileUseCase(repository)

    @Test
    fun `invoke returns profile when it exists`() = runTest {
        val profile = Profile(
            currentWeightKg = 80.0,
            heightM = 1.80,
            experienceLevel = ExperienceLevel.ADVANCED,
            weeklyFrequency = 6,
            createdAt = LocalDate.of(2026, 1, 15),
        )
        every { repository.getProfile() } returns flowOf(profile)

        val result = useCase().first()

        assertNotNull(result)
        assertEquals(80.0, result!!.currentWeightKg, 0.01)
        assertEquals(1.80, result.heightM, 0.01)
        assertEquals(ExperienceLevel.ADVANCED, result.experienceLevel)
    }

    @Test
    fun `invoke returns null when profile does not exist`() = runTest {
        every { repository.getProfile() } returns flowOf(null)

        val result = useCase().first()

        assertNull(result)
    }
}
