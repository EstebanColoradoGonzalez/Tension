package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.PlateauCause
import org.junit.Assert.assertEquals
import org.junit.Test

class PlateauCausalAnalysisRuleTest {

    @Test
    fun `analyze — low RIR avg (0_77) → LOW_RIR_LIMIT`() {
        val result = PlateauCausalAnalysisRule.analyze(
            lastSessionsAvgRir = listOf(0.5, 0.8, 1.0),
            isGroupStagnant = false,
        )
        assertEquals(PlateauCause.LOW_RIR_LIMIT, result)
    }

    @Test
    fun `analyze — high RIR avg (3_5) → HIGH_RIR_CONSERVATIVE`() {
        val result = PlateauCausalAnalysisRule.analyze(
            lastSessionsAvgRir = listOf(3.5, 4.0, 3.0),
            isGroupStagnant = false,
        )
        assertEquals(PlateauCause.HIGH_RIR_CONSERVATIVE, result)
    }

    @Test
    fun `analyze — mid RIR avg (2_0) → MIXED`() {
        val result = PlateauCausalAnalysisRule.analyze(
            lastSessionsAvgRir = listOf(1.5, 2.0, 2.5),
            isGroupStagnant = false,
        )
        assertEquals(PlateauCause.MIXED, result)
    }

    @Test
    fun `analyze — group stagnant overrides RIR → GROUP_STAGNATION`() {
        val result = PlateauCausalAnalysisRule.analyze(
            lastSessionsAvgRir = listOf(0.5, 0.8, 1.0),
            isGroupStagnant = true,
        )
        assertEquals(PlateauCause.GROUP_STAGNATION, result)
    }

    @Test
    fun `analyze — empty RIR list → MIXED`() {
        val result = PlateauCausalAnalysisRule.analyze(
            lastSessionsAvgRir = emptyList(),
            isGroupStagnant = false,
        )
        assertEquals(PlateauCause.MIXED, result)
    }

    @Test
    fun `analyze — boundary exactly 1_0 → LOW_RIR_LIMIT`() {
        val result = PlateauCausalAnalysisRule.analyze(
            lastSessionsAvgRir = listOf(1.0, 1.0, 1.0),
            isGroupStagnant = false,
        )
        assertEquals(PlateauCause.LOW_RIR_LIMIT, result)
    }

    @Test
    fun `analyze — boundary exactly 3_0 → HIGH_RIR_CONSERVATIVE`() {
        val result = PlateauCausalAnalysisRule.analyze(
            lastSessionsAvgRir = listOf(3.0, 3.0, 3.0),
            isGroupStagnant = false,
        )
        assertEquals(PlateauCause.HIGH_RIR_CONSERVATIVE, result)
    }
}
