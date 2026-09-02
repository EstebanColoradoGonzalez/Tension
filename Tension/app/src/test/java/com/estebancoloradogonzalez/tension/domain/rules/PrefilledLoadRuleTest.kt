package com.estebancoloradogonzalez.tension.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrefilledLoadRuleTest {

    // --- Escenario 1: prescripción activa gana a la sesión anterior — CA-31.01, CA-31.04 ---
    @Test
    fun `resolve — active prescription wins over previous session weight`() {
        val result = resolve(prescribed = 45.0, inSession = null, inPrevious = 40.0)
        assertEquals(45.0, result!!, DELTA)
    }

    // --- Escenario 2: prescripción activa gana a la serie anterior en sesión — CA-31.01 ---
    @Test
    fun `resolve — active prescription wins over previous set in current session`() {
        val result = resolve(prescribed = 45.0, inSession = 42.5, inPrevious = 40.0)
        assertEquals(45.0, result!!, DELTA)
    }

    // --- Escenario 3: prescripción consumida cede a la memoria intra-sesión — CA-31.01, CA-31.02 ---
    @Test
    fun `resolve — spent prescription yields to in-session memory`() {
        val result = resolve(prescribed = 40.0, inSession = 42.5, inPrevious = 40.0)
        assertEquals(42.5, result!!, DELTA)
    }

    // --- Escenario 4: prescripción congelada cede a la sesión anterior — CA-31.01, CA-31.03 ---
    @Test
    fun `resolve — frozen prescription yields to previous session weight`() {
        val result = resolve(prescribed = 40.0, inSession = null, inPrevious = 47.5)
        assertEquals(47.5, result!!, DELTA)
    }

    // --- Escenario 5: prescripción igual a lo manejado cede a la memoria ---
    @Test
    fun `resolve — prescription equal to handled weight yields to memory`() {
        val result = resolve(prescribed = 42.5, inSession = 42.5, inPrevious = null)
        assertEquals(42.5, result!!, DELTA)
    }

    // --- Escenario 6: diferencia dentro de la tolerancia cede a la memoria ---
    @Test
    fun `resolve — difference within tolerance yields to memory`() {
        val result = resolve(prescribed = 42.505, inSession = 42.5, inPrevious = null)
        assertEquals(42.5, result!!, DELTA)
    }

    // --- Escenario 7: diferencia por encima de la tolerancia activa la prescripción ---
    @Test
    fun `resolve — difference above tolerance activates the prescription`() {
        val result = resolve(prescribed = 42.52, inSession = 42.5, inPrevious = null)
        assertEquals(42.52, result!!, DELTA)
    }

    // --- Escenario 8: prescripción en cero se ignora — CA-31.01 ---
    @Test
    fun `resolve — zero prescription is ignored`() {
        val result = resolve(prescribed = 0.0, inSession = null, inPrevious = 40.0)
        assertEquals(40.0, result!!, DELTA)
    }

    // --- Escenario 9: prescripción nula cae en la memoria intra-sesión — CA-31.02 ---
    @Test
    fun `resolve — null prescription falls back to in-session memory`() {
        val result = resolve(prescribed = null, inSession = 42.5, inPrevious = 40.0)
        assertEquals(42.5, result!!, DELTA)
    }

    // --- Escenario 10: la serie anterior gana a la sesión previa — CA-31.01, regla de negocio 3 ---
    @Test
    fun `resolve — previous set in session wins over previous session`() {
        val result = resolve(prescribed = null, inSession = 42.5, inPrevious = 50.0)
        assertEquals(42.5, result!!, DELTA)
    }

    // --- Escenario 11: solo sesión previa — CA-31.03 ---
    @Test
    fun `resolve — only previous session weight available`() {
        val result = resolve(prescribed = null, inSession = null, inPrevious = 40.0)
        assertEquals(40.0, result!!, DELTA)
    }

    // --- Escenario 12: sin insumos → campo vacío — CA-31.07 ---
    @Test
    fun `resolve — no inputs returns null`() {
        val result = resolve(prescribed = null, inSession = null, inPrevious = null)
        assertNull(result)
    }

    // --- Escenario 13: prescripción activa sin ninguna memoria — CA-31.01 ---
    @Test
    fun `resolve — active prescription without any memory`() {
        val result = resolve(prescribed = 45.0, inSession = null, inPrevious = null)
        assertEquals(45.0, result!!, DELTA)
    }

    // --- Escenario 14: alternativo sin historial ni prescripción → vacío — CA-31.08 ---
    @Test
    fun `resolve — alternative exercise without history stays empty`() {
        val result = resolve(prescribed = 0.0, inSession = null, inPrevious = null)
        assertNull(result)
    }

    private fun resolve(
        prescribed: Double?,
        inSession: Double?,
        inPrevious: Double?,
    ): Double? = PrefilledLoadRule.resolve(
        prescribedLoadKg = prescribed,
        lastWeightInSessionKg = inSession,
        lastWeightInPreviousSessionKg = inPrevious,
    )

    private companion object {
        const val DELTA = 0.001
    }
}
