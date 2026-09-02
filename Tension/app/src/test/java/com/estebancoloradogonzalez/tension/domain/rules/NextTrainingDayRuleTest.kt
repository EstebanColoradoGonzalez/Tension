package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.WeekDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NextTrainingDayRuleTest {

    private val defaultPlanDays = setOf(
        WeekDay.MONDAY, WeekDay.TUESDAY, WeekDay.WEDNESDAY,
        WeekDay.THURSDAY, WeekDay.FRIDAY, WeekDay.SATURDAY,
    )

    @Test
    fun `the next day is tomorrow when tomorrow has a routine`() {
        assertEquals(
            WeekDay.FRIDAY,
            NextTrainingDayRule.resolve(WeekDay.THURSDAY, defaultPlanDays),
        )
    }

    @Test
    fun `rest days are skipped`() {
        // Tras el sábado, lo útil es el lunes: el domingo no tiene rutina.
        assertEquals(
            WeekDay.MONDAY,
            NextTrainingDayRule.resolve(WeekDay.SATURDAY, defaultPlanDays),
        )
    }

    @Test
    fun `from a rest day the next training day is found too`() {
        assertEquals(
            WeekDay.MONDAY,
            NextTrainingDayRule.resolve(WeekDay.SUNDAY, defaultPlanDays),
        )
    }

    @Test
    fun `several consecutive rest days are skipped`() {
        assertEquals(
            WeekDay.THURSDAY,
            NextTrainingDayRule.resolve(WeekDay.MONDAY, setOf(WeekDay.MONDAY, WeekDay.THURSDAY)),
        )
    }

    @Test
    fun `with a single training day the next occurrence is that same day next week`() {
        assertEquals(
            WeekDay.MONDAY,
            NextTrainingDayRule.resolve(WeekDay.MONDAY, setOf(WeekDay.MONDAY)),
        )
    }

    @Test
    fun `without any day assigned there is no next training day`() {
        assertNull(NextTrainingDayRule.resolve(WeekDay.THURSDAY, emptySet()))
    }

    @Test
    fun `every day of the week resolves to the following one when all train`() {
        val allDays = WeekDay.entries.toSet()
        assertEquals(WeekDay.SUNDAY, NextTrainingDayRule.resolve(WeekDay.SATURDAY, allDays))
        assertEquals(WeekDay.MONDAY, NextTrainingDayRule.resolve(WeekDay.SUNDAY, allDays))
    }
}
