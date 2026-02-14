package com.estebancoloradogonzalez.tension.domain.usecase.profile

import com.estebancoloradogonzalez.tension.domain.repository.ProfileRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckProfileExistsUseCaseTest {

    private val repository: ProfileRepository = mockk()
    private val useCase = CheckProfileExistsUseCase(repository)

    @Test
    fun `invoke returns true when profile exists`() = runTest {
        every { repository.profileExists() } returns flowOf(true)

        val result = useCase().first()

        assertTrue(result)
    }

    @Test
    fun `invoke returns false when profile does not exist`() = runTest {
        every { repository.profileExists() } returns flowOf(false)

        val result = useCase().first()

        assertFalse(result)
    }
}
