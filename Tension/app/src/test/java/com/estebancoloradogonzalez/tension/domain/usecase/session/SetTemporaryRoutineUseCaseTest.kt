package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import com.estebancoloradogonzalez.tension.domain.repository.WeekDayRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SetTemporaryRoutineUseCaseTest {

    private val weekDayRepository: WeekDayRepository = mockk()
    private val sessionRepository: SessionRepository = mockk()
    private val useCase = SetTemporaryRoutineUseCase(weekDayRepository, sessionRepository)

    @Test
    fun `invoke persists the override when no session is active`() = runTest {
        coEvery { sessionRepository.hasActiveSession() } returns false
        coEvery { weekDayRepository.setTodayOverride(2L) } just runs

        useCase(2L)

        coVerify { weekDayRepository.setTodayOverride(2L) }
    }

    // CA-36.06 — Reasignación solo antes de iniciar

    @Test(expected = IllegalStateException::class)
    fun `invoke throws when a session is already active`() = runTest {
        coEvery { sessionRepository.hasActiveSession() } returns true

        useCase(2L)
    }

    @Test
    fun `invoke does not persist anything when a session is already active`() = runTest {
        coEvery { sessionRepository.hasActiveSession() } returns true

        runCatching { useCase(2L) }

        coVerify(exactly = 0) { weekDayRepository.setTodayOverride(any()) }
    }
}
