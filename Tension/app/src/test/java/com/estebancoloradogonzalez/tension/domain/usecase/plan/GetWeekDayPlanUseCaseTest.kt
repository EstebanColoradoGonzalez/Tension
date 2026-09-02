package com.estebancoloradogonzalez.tension.domain.usecase.plan

import com.estebancoloradogonzalez.tension.domain.model.WeekDay
import com.estebancoloradogonzalez.tension.domain.model.WeekDayRoutine
import com.estebancoloradogonzalez.tension.domain.repository.WeekDayRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GetWeekDayPlanUseCaseTest {

    private val repository: WeekDayRepository = mockk()
    private val useCase = GetWeekDayPlanUseCase(repository)

    @Test
    fun `invoke returns the seven days in order`() = runTest {
        every { repository.getWeekDayPlan() } returns flowOf(defaultPlan())

        val result = useCase().first()

        assertEquals(7, result.size)
        assertEquals(WeekDay.entries.toList(), result.map { it.weekDay })
    }

    @Test
    fun `sunday is a rest day`() = runTest {
        every { repository.getWeekDayPlan() } returns flowOf(defaultPlan())

        val sunday = useCase().first().single { it.weekDay == WeekDay.SUNDAY }

        assertNull(sunday.routineId)
        assertTrue(sunday.isRestDay)
    }

    private fun defaultPlan(): List<WeekDayRoutine> = WeekDay.entries.map { day ->
        val routineId = day.isoNumber.toLong().takeIf { day != WeekDay.SUNDAY }
        WeekDayRoutine(
            weekDay = day,
            routineId = routineId,
            routineName = routineId?.let { "Rutina $it" },
        )
    }
}
