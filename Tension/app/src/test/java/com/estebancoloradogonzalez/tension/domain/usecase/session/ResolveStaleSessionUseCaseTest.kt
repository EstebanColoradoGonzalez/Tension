package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ResolveStaleSessionUseCaseTest {

    private val repository: SessionRepository = mockk()
    private val useCase = ResolveStaleSessionUseCase(repository)

    @Test
    fun `invoke does nothing when no session was left open`() = runTest {
        coEvery { repository.getStaleActiveSessionId() } returns null

        useCase()

        coVerify(exactly = 0) { repository.closeSession(any()) }
        coVerify(exactly = 0) { repository.discardSession(any()) }
    }

    // Con series registradas hubo entrenamiento: se cierra como lo habría hecho el ejecutante.

    @Test
    fun `invoke closes a session left open with sets`() = runTest {
        coEvery { repository.getStaleActiveSessionId() } returns 7L
        coEvery { repository.hasSetsInActiveSession() } returns true
        coEvery { repository.closeSession(7L) } just runs

        useCase()

        coVerify { repository.closeSession(7L) }
        coVerify(exactly = 0) { repository.discardSession(any()) }
    }

    // Sin ninguna serie no hubo entrenamiento: la sesión no debe llegar al historial.

    @Test
    fun `invoke discards a session left open without sets`() = runTest {
        coEvery { repository.getStaleActiveSessionId() } returns 7L
        coEvery { repository.hasSetsInActiveSession() } returns false
        coEvery { repository.discardSession(7L) } just runs

        useCase()

        coVerify { repository.discardSession(7L) }
        coVerify(exactly = 0) { repository.closeSession(any()) }
    }

    @Test
    fun `invoke only resolves the session it found`() = runTest {
        coEvery { repository.getStaleActiveSessionId() } returns 42L
        coEvery { repository.hasSetsInActiveSession() } returns true
        coEvery { repository.closeSession(42L) } just runs

        useCase()

        coVerify(exactly = 1) { repository.closeSession(42L) }
    }
}
