package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.DailyRoutineOverride
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyRoutineRuleTest {

    private val today = "2026-09-01"

    // CA-36.02 — Reasignación temporal de la rutina de un día

    @Test
    fun `override of today replaces the permanent routine`() {
        val result = DailyRoutineRule.resolve(
            today = today,
            permanentRoutineId = 4L,
            override = DailyRoutineOverride(date = today, routineId = 2L),
        )

        assertEquals(2L, result.routineId)
        assertTrue(result.isOverridden)
    }

    // CA-36.04 — Reversión automática

    @Test
    fun `override of a previous day is ignored`() {
        val result = DailyRoutineRule.resolve(
            today = today,
            permanentRoutineId = 5L,
            override = DailyRoutineOverride(date = "2026-08-31", routineId = 2L),
        )

        assertEquals(5L, result.routineId)
        assertFalse(result.isOverridden)
    }

    @Test
    fun `override of a future day is ignored`() {
        val result = DailyRoutineRule.resolve(
            today = today,
            permanentRoutineId = 5L,
            override = DailyRoutineOverride(date = "2026-09-02", routineId = 2L),
        )

        assertEquals(5L, result.routineId)
        assertFalse(result.isOverridden)
    }

    @Test
    fun `without override the permanent routine is proposed`() {
        val result = DailyRoutineRule.resolve(
            today = today,
            permanentRoutineId = 3L,
            override = null,
        )

        assertEquals(3L, result.routineId)
        assertFalse(result.isOverridden)
    }

    // CA-36.01 — El domingo queda registrado como día sin rutina asignada

    @Test
    fun `a day without permanent routine and without override resolves to no routine`() {
        val result = DailyRoutineRule.resolve(
            today = today,
            permanentRoutineId = null,
            override = null,
        )

        assertNull(result.routineId)
        assertFalse(result.isOverridden)
    }

    // CA-36.07 — Reasignar la rutina del domingo

    @Test
    fun `a day without permanent routine can be overridden`() {
        val result = DailyRoutineRule.resolve(
            today = today,
            permanentRoutineId = null,
            override = DailyRoutineOverride(date = today, routineId = 6L),
        )

        assertEquals(6L, result.routineId)
        assertTrue(result.isOverridden)
    }

    // CA-36.08 — Reasignar a la rutina que ya correspondía

    @Test
    fun `overriding with the routine already assigned resolves to the same routine`() {
        val withoutOverride = DailyRoutineRule.resolve(
            today = today,
            permanentRoutineId = 4L,
            override = null,
        )
        val withRedundantOverride = DailyRoutineRule.resolve(
            today = today,
            permanentRoutineId = 4L,
            override = DailyRoutineOverride(date = today, routineId = 4L),
        )

        assertEquals(withoutOverride.routineId, withRedundantOverride.routineId)
        assertTrue(withRedundantOverride.isOverridden)
    }

    // CA-36.03 — La rotación no entra en la resolución

    @Test
    fun `resolution does not depend on any rotation input`() {
        // La firma es el contrato: la regla no recibe posición ni conteo de microciclo, así
        // que no hay forma de que una reasignación alcance el estado de rotación.
        val result = DailyRoutineRule.resolve(
            today = today,
            permanentRoutineId = 1L,
            override = DailyRoutineOverride(date = today, routineId = 6L),
        )

        assertEquals(6L, result.routineId)
    }
}
