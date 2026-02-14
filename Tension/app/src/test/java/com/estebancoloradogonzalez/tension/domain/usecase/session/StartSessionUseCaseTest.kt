package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class StartSessionUseCaseTest {

    private val repository: SessionRepository = mockk()
    private val useCase = StartSessionUseCase(repository)

    @Test
    fun `invoke delegates to repository and returns session id`() = runTest {
        coEvery { repository.startSession(1L) } returns 42L

        val result = useCase(1L)

        assertEquals(42L, result)
        coVerify { repository.startSession(1L) }
    }

    @Test(expected = IllegalStateException::class)
    fun `invoke propagates exception when session already active`() = runTest {
        coEvery { repository.startSession(any()) } throws
            IllegalStateException("A session is already in progress")

        useCase(1L)
    }
}
