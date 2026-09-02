package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.model.NextSession
import com.estebancoloradogonzalez.tension.domain.model.TodaySession
import com.estebancoloradogonzalez.tension.domain.model.WeekDay
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GetTodaySessionUseCaseTest {

    private val repository: SessionRepository = mockk()
    private val useCase = GetTodaySessionUseCase(repository)

    @Test
    fun `invoke returns the proposal of the day from repository`() = runTest {
        val todaySession = TodaySession(
            weekDay = WeekDay.THURSDAY,
            session = NextSession(
                routineId = 4L,
                routineName = "Push — Foco Tríceps",
                versionNumber = 1,
                routineVersionId = 4L,
            ),
        )
        every { repository.getTodaySession() } returns flowOf(todaySession)

        val result = useCase().first()

        assertEquals(todaySession, result)
        assertTrue(result.showSessionCard)
        assertFalse(result.showRestDayCard)
    }

    @Test
    fun `invoke returns a rest day when the day has no routine`() = runTest {
        every { repository.getTodaySession() } returns flowOf(
            TodaySession(weekDay = WeekDay.SUNDAY, isRestDay = true),
        )

        val result = useCase().first()

        assertNull(result.session)
        assertTrue(result.showRestDayCard)
        assertFalse(result.showSessionCard)
    }

    @Test
    fun `invoke reports the source day of a temporary reassignment`() = runTest {
        every { repository.getTodaySession() } returns flowOf(
            TodaySession(
                weekDay = WeekDay.THURSDAY,
                session = NextSession(
                    routineId = 2L,
                    routineName = "Pull — Foco Dorsal Ancho",
                    versionNumber = 1,
                    routineVersionId = 2L,
                ),
                isTemporaryOverride = true,
                overriddenFromWeekDay = WeekDay.TUESDAY,
            ),
        )

        val result = useCase().first()

        assertTrue(result.isTemporaryOverride)
        assertEquals(WeekDay.TUESDAY, result.overriddenFromWeekDay)
        assertEquals(2L, result.session?.routineId)
    }
}
