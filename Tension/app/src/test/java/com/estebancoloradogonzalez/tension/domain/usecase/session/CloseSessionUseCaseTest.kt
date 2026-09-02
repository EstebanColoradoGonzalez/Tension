package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CloseSessionUseCaseTest {

    private val repository: SessionRepository = mockk()
    private val useCase = CloseSessionUseCase(repository)

    @Test
    fun `invoke delegates to repository`() = runTest {
        coEvery { repository.closeSession(42L) } just runs

        useCase(42L)

        coVerify { repository.closeSession(42L) }
    }

    @Test(expected = IllegalStateException::class)
    fun `invoke propagates IllegalStateException when no active session`() = runTest {
        coEvery { repository.closeSession(any()) } throws
            IllegalStateException("No active session found")

        useCase(999L)
    }

    // Una sesión sin ninguna serie no se cierra: se cancela el día desde Inicio.

    @Test(expected = IllegalStateException::class)
    fun `invoke propagates IllegalStateException when the session has no sets`() = runTest {
        coEvery { repository.closeSession(any()) } throws
            IllegalStateException("Cannot close a session without any registered set")

        useCase(42L)
    }
}
