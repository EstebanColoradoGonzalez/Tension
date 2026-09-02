package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.WeekDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeekDayAssignmentRuleTest {

    private val editedRoutineId = 4L
    private val otherRoutineId = 2L

    @Test
    fun `a selected day takes the edited routine`() {
        val result = WeekDayAssignmentRule.resolveRoutineFor(
            weekDay = WeekDay.THURSDAY,
            currentRoutineId = null,
            editedRoutineId = editedRoutineId,
            selectedWeekDays = setOf(WeekDay.THURSDAY),
        )

        assertEquals(editedRoutineId, result)
    }

    @Test
    fun `a selected day held by another routine is moved, not duplicated`() {
        val result = WeekDayAssignmentRule.resolveRoutineFor(
            weekDay = WeekDay.TUESDAY,
            currentRoutineId = otherRoutineId,
            editedRoutineId = editedRoutineId,
            selectedWeekDays = setOf(WeekDay.TUESDAY),
        )

        assertEquals(editedRoutineId, result)
    }

    @Test
    fun `a day dropped from the selection becomes a rest day`() {
        val result = WeekDayAssignmentRule.resolveRoutineFor(
            weekDay = WeekDay.THURSDAY,
            currentRoutineId = editedRoutineId,
            editedRoutineId = editedRoutineId,
            selectedWeekDays = emptySet(),
        )

        assertNull(result)
    }

    @Test
    fun `a day of another routine that was not selected is left untouched`() {
        val result = WeekDayAssignmentRule.resolveRoutineFor(
            weekDay = WeekDay.TUESDAY,
            currentRoutineId = otherRoutineId,
            editedRoutineId = editedRoutineId,
            selectedWeekDays = setOf(WeekDay.THURSDAY),
        )

        assertEquals(otherRoutineId, result)
    }

    @Test
    fun `a rest day that was not selected stays a rest day`() {
        val result = WeekDayAssignmentRule.resolveRoutineFor(
            weekDay = WeekDay.SUNDAY,
            currentRoutineId = null,
            editedRoutineId = editedRoutineId,
            selectedWeekDays = setOf(WeekDay.THURSDAY),
        )

        assertNull(result)
    }

    @Test
    fun `a routine can hold several days at once`() {
        val selection = setOf(WeekDay.MONDAY, WeekDay.THURSDAY)

        val monday = WeekDayAssignmentRule.resolveRoutineFor(
            weekDay = WeekDay.MONDAY,
            currentRoutineId = editedRoutineId,
            editedRoutineId = editedRoutineId,
            selectedWeekDays = selection,
        )
        val thursday = WeekDayAssignmentRule.resolveRoutineFor(
            weekDay = WeekDay.THURSDAY,
            currentRoutineId = otherRoutineId,
            editedRoutineId = editedRoutineId,
            selectedWeekDays = selection,
        )

        assertEquals(editedRoutineId, monday)
        assertEquals(editedRoutineId, thursday)
    }

    @Test
    fun `clearing every day leaves the whole week at rest for that routine`() {
        val days = WeekDay.entries.map { day ->
            WeekDayAssignmentRule.resolveRoutineFor(
                weekDay = day,
                currentRoutineId = editedRoutineId,
                editedRoutineId = editedRoutineId,
                selectedWeekDays = emptySet(),
            )
        }

        assertEquals(List(WeekDay.entries.size) { null }, days)
    }
}
