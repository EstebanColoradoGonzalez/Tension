package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.model.ReassignableRoutine
import com.estebancoloradogonzalez.tension.domain.model.WeekDay
import com.estebancoloradogonzalez.tension.domain.repository.WeekDayRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetReassignableRoutinesUseCaseTest {

    private val repository: WeekDayRepository = mockk()
    private val useCase = GetReassignableRoutinesUseCase(repository)

    @Test
    fun `invoke returns every executable routine and marks today's`() = runTest {
        every { repository.getReassignableRoutines() } returns flowOf(
            listOf(
                option(1L, "Push — Foco Deltoides Lateral y Medio", listOf(WeekDay.MONDAY), false),
                option(4L, "Push — Foco Tríceps", listOf(WeekDay.THURSDAY), true),
            ),
        )

        val result = useCase().first()

        assertEquals(2, result.size)
        assertEquals(4L, result.single { it.isTodaysRoutine }.routineId)
    }

    @Test
    fun `invoke includes a routine that no week day claims`() = runTest {
        every { repository.getReassignableRoutines() } returns flowOf(
            listOf(
                option(1L, "Push — Foco Deltoides Lateral y Medio", listOf(WeekDay.MONDAY), true),
                option(7L, "Rutina propia", emptyList(), false),
            ),
        )

        val result = useCase().first()

        assertTrue(result.single { it.routineId == 7L }.weekDays.isEmpty())
        assertEquals(listOf(WeekDay.MONDAY), result.single { it.routineId == 1L }.weekDays)
    }

    @Test
    fun `invoke reports every day that executes the routine`() = runTest {
        every { repository.getReassignableRoutines() } returns flowOf(
            listOf(
                option(
                    1L,
                    "Push — Foco Deltoides Lateral y Medio",
                    listOf(WeekDay.MONDAY, WeekDay.THURSDAY),
                    false,
                ),
            ),
        )

        val result = useCase().first()

        assertEquals(listOf(WeekDay.MONDAY, WeekDay.THURSDAY), result.single().weekDays)
    }

    private fun option(
        routineId: Long,
        routineName: String,
        weekDays: List<WeekDay>,
        isTodaysRoutine: Boolean,
    ) = ReassignableRoutine(
        routineId = routineId,
        routineName = routineName,
        routineVersionId = routineId,
        versionNumber = 1,
        weekDays = weekDays,
        isTodaysRoutine = isTodaysRoutine,
    )
}
