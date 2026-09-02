package com.estebancoloradogonzalez.tension.domain.usecase.plan

import com.estebancoloradogonzalez.tension.domain.model.WeekDay
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import com.estebancoloradogonzalez.tension.domain.repository.WeekDayRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class UpdateRoutineWeekDaysUseCaseTest {

    private val repository: WeekDayRepository = mockk()
    private val sessionRepository: SessionRepository = mockk()
    private val useCase = UpdateRoutineWeekDaysUseCase(repository, sessionRepository)

    @Before
    fun setUp() {
        coEvery { sessionRepository.hasActiveDeload() } returns false
    }

    @Test
    fun `invoke assigns the selected days to the routine`() = runTest {
        val days = setOf(WeekDay.MONDAY, WeekDay.THURSDAY)
        coEvery { repository.setRoutineWeekDays(4L, days) } just runs

        useCase(4L, days)

        coVerify { repository.setRoutineWeekDays(4L, days) }
    }

    @Test
    fun `invoke with an empty selection leaves the routine without days`() = runTest {
        coEvery { repository.setRoutineWeekDays(4L, emptySet()) } just runs

        useCase(4L, emptySet())

        coVerify { repository.setRoutineWeekDays(4L, emptySet()) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invoke is rejected during an active deload`() = runTest {
        coEvery { sessionRepository.hasActiveDeload() } returns true

        useCase(4L, setOf(WeekDay.MONDAY))
    }

    @Test
    fun `invoke does not touch the plan during an active deload`() = runTest {
        coEvery { sessionRepository.hasActiveDeload() } returns true

        runCatching { useCase(4L, setOf(WeekDay.MONDAY)) }

        coVerify(exactly = 0) { repository.setRoutineWeekDays(any(), any()) }
    }
}
