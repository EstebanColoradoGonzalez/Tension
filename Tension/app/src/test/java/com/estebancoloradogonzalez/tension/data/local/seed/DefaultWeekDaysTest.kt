package com.estebancoloradogonzalez.tension.data.local.seed

import com.estebancoloradogonzalez.tension.domain.model.WeekDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultWeekDaysTest {

    // CA-36.01 — El día es una entidad relacionada

    @Test
    fun `there are exactly seven days without repetition`() {
        assertEquals(7, DefaultWeekDays.ALL.size)
        assertEquals(WeekDay.entries.toSet(), DefaultWeekDays.ALL.map { it.weekDay }.toSet())
    }

    @Test
    fun `monday to saturday point to the six seed routines in order`() {
        val assignments = DefaultWeekDays.ALL.associate { it.weekDay to it.routineId }

        assertEquals(1L, assignments[WeekDay.MONDAY])
        assertEquals(2L, assignments[WeekDay.TUESDAY])
        assertEquals(3L, assignments[WeekDay.WEDNESDAY])
        assertEquals(4L, assignments[WeekDay.THURSDAY])
        assertEquals(5L, assignments[WeekDay.FRIDAY])
        assertEquals(6L, assignments[WeekDay.SATURDAY])
    }

    @Test
    fun `sunday is registered without routine`() {
        val sunday = DefaultWeekDays.ALL.single { it.weekDay == WeekDay.SUNDAY }

        assertNull(sunday.routineId)
    }

    @Test
    fun `every referenced routine exists in the default plan`() {
        val planRoutineIds = DefaultPlan.ROUTINES.map { it.id }.toSet()

        DefaultWeekDays.ALL.mapNotNull { it.routineId }.forEach { routineId ->
            assertTrue(
                "El día apunta a la rutina $routineId, que no existe en el plan",
                routineId in planRoutineIds,
            )
        }
    }
}
