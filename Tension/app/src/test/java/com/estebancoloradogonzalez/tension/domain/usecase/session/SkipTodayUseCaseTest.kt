package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SkipTodayUseCaseTest {

    private val repository: SessionRepository = mockk()
    private val useCase = SkipTodayUseCase(repository)

    @Test
    fun `invoke resolves the day when there is no session at all`() = runTest {
        coEvery { repository.hasSetsInActiveSession() } returns false
        coEvery { repository.skipToday() } just runs

        useCase()

        coVerify { repository.skipToday() }
    }

    // Abrir la sesión y no entrenar nada es justo el caso que hay que poder cancelar.

    @Test
    fun `invoke cancels an open session that has no sets`() = runTest {
        coEvery { repository.hasSetsInActiveSession() } returns false
        coEvery { repository.skipToday() } just runs

        useCase()

        coVerify { repository.skipToday() }
    }

    // Con una sola serie ya hubo entrenamiento: cancelar borraría trabajo real.

    @Test(expected = IllegalStateException::class)
    fun `invoke throws when the open session already has sets`() = runTest {
        coEvery { repository.hasSetsInActiveSession() } returns true

        useCase()
    }

    @Test
    fun `invoke persists nothing when the open session already has sets`() = runTest {
        coEvery { repository.hasSetsInActiveSession() } returns true

        runCatching { useCase() }

        coVerify(exactly = 0) { repository.skipToday() }
    }
}
