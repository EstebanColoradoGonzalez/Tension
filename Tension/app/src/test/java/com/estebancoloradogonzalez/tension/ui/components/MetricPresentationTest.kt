package com.estebancoloradogonzalez.tension.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MetricPresentationTest {

    // --- Progression rate -------------------------------------------------

    @Test
    fun `given observations and a zero rate, when sufficiency is resolved, then the zero is available`() {
        // Given
        val observations = 5

        // When
        val result = MetricSufficiencyRules.progressionRate(rate = 0.0, observations = observations)

        // Then
        assertEquals(MetricValue.Available(0.0, MetricUnit.PERCENTAGE), result)
    }

    @Test
    fun `given no observation, when sufficiency is resolved, then the rate is insufficient`() {
        // Given
        val observations = 0

        // When
        val result = MetricSufficiencyRules.progressionRate(rate = 0.0, observations = observations)

        // Then
        assertEquals(
            MetricValue.Insufficient(
                MetricRequirement(MetricRequirementKind.EXERCISE_OBSERVATIONS, 0, 1),
            ),
            result,
        )
    }

    @Test
    fun `given a computed rate, when sufficiency is resolved, then the value is preserved`() {
        // Given
        val rate = 64.0

        // When
        val result = MetricSufficiencyRules.progressionRate(rate = rate, observations = 5)

        // Then
        assertEquals(MetricValue.Available(64.0, MetricUnit.PERCENTAGE), result)
    }

    // --- Load velocity ----------------------------------------------------

    @Test
    fun `given a single session, when load velocity is resolved, then one more session is missing`() {
        // Given
        val sessionCount = 1

        // When
        val result = MetricSufficiencyRules.loadVelocity(0.0, sessionCount, isBodyweight = false)

        // Then
        val requirement = (result as MetricValue.Insufficient).requirement
        assertEquals(MetricRequirementKind.EXERCISE_SESSIONS, requirement.kind)
        assertEquals(1, requirement.missing)
    }

    @Test
    fun `given several sessions and no load change, when load velocity is resolved, then zero is available`() {
        // Given
        val sessionCount = 6

        // When
        val result = MetricSufficiencyRules.loadVelocity(0.0, sessionCount, isBodyweight = false)

        // Then
        assertEquals(MetricValue.Available(0.0, MetricUnit.KILOGRAM_PER_SESSION), result)
    }

    @Test
    fun `given a bodyweight exercise, when load velocity is resolved, then it is not applicable`() {
        // Given
        val isBodyweight = true

        // When
        val result = MetricSufficiencyRules.loadVelocity(0.0, sessionCount = 1, isBodyweight = isBodyweight)

        // Then
        assertEquals(MetricValue.NotApplicable, result)
    }

    // --- Average RIR ------------------------------------------------------

    @Test
    fun `given no recorded set, when average rir is resolved, then it is insufficient`() {
        // Given
        val recordedSets = 0

        // When
        val result = MetricSufficiencyRules.averageRir(averageRir = null, recordedSets = recordedSets)

        // Then
        assertEquals(
            MetricValue.Insufficient(MetricRequirement(MetricRequirementKind.ROUTINE_SETS, 0, 1)),
            result,
        )
    }

    @Test
    fun `given recorded sets and a zero average, when average rir is resolved, then the zero is available`() {
        // Given
        val recordedSets = 12

        // When
        val result = MetricSufficiencyRules.averageRir(averageRir = 0.0, recordedSets = recordedSets)

        // Then
        assertEquals(MetricValue.Available(0.0, MetricUnit.RIR), result)
    }

    // --- Adherence --------------------------------------------------------

    @Test
    fun `given no weekly target, when adherence is resolved, then it is insufficient`() {
        // Given
        val plannedSessions = 0

        // When
        val result = MetricSufficiencyRules.adherence(percentage = 0.0, plannedSessions = plannedSessions)

        // Then
        assertEquals(
            MetricValue.Insufficient(MetricRequirement(MetricRequirementKind.WEEKLY_TARGET, 0, 1)),
            result,
        )
    }

    @Test
    fun `given a weekly target and no session yet, when adherence is resolved, then the zero is available`() {
        // Given
        val plannedSessions = 6

        // When
        val result = MetricSufficiencyRules.adherence(percentage = 0.0, plannedSessions = plannedSessions)

        // Then
        assertEquals(MetricValue.Available(0.0, MetricUnit.PERCENTAGE), result)
    }

    // --- Tonnage and distribution ----------------------------------------

    @Test
    fun `given a microcycle with sessions and an untrained group, when tonnage is resolved, then the zero is available`() {
        // Given
        val sessionsInMicrocycle = 6

        // When
        val result = MetricSufficiencyRules.tonnage(tonnageKg = 0.0, sessionsInMicrocycle = sessionsInMicrocycle)

        // Then
        assertEquals(MetricValue.Available(0.0, MetricUnit.KILOGRAM), result)
    }

    @Test
    fun `given a microcycle without sessions, when tonnage is resolved, then it is insufficient`() {
        // Given
        val sessionsInMicrocycle = 0

        // When
        val result = MetricSufficiencyRules.tonnage(tonnageKg = 0.0, sessionsInMicrocycle = sessionsInMicrocycle)

        // Then
        assertEquals(
            MetricValue.Insufficient(
                MetricRequirement(MetricRequirementKind.MICROCYCLE_SESSIONS, 0, 1),
            ),
            result,
        )
    }

    @Test
    fun `given a microcycle without sessions, when distribution is resolved, then it is insufficient`() {
        // Given
        val sessionsInMicrocycle = 0

        // When
        val result = MetricSufficiencyRules.distribution(percentage = 0.0, sessionsInMicrocycle = sessionsInMicrocycle)

        // Then
        assertTrue(result is MetricValue.Insufficient)
    }

    // --- Evolution and trend ---------------------------------------------

    @Test
    fun `given a single microcycle, when evolution is resolved, then one more is missing`() {
        // Given
        val totalMicrocycles = 1

        // When
        val result = MetricSufficiencyRules.evolution(totalMicrocycles)

        // Then
        assertEquals(1, result?.missing)
    }

    @Test
    fun `given two microcycles, when evolution is resolved, then nothing is missing`() {
        // Given
        val totalMicrocycles = 2

        // When
        val result = MetricSufficiencyRules.evolution(totalMicrocycles)

        // Then
        assertNull(result)
    }

    @Test
    fun `given two complete microcycles, when trend is resolved, then two more are missing`() {
        // Given
        val completeMicrocycles = 2

        // When
        val result = MetricSufficiencyRules.trend(completeMicrocycles)

        // Then
        assertEquals(MetricRequirementKind.COMPLETE_MICROCYCLES, result?.kind)
        assertEquals(2, result?.missing)
    }

    @Test
    fun `given four complete microcycles, when trend is resolved, then nothing is missing`() {
        // Given
        val completeMicrocycles = 4

        // When
        val result = MetricSufficiencyRules.trend(completeMicrocycles)

        // Then
        assertNull(result)
    }

    // --- Thresholds -------------------------------------------------------

    @Test
    fun `given the sufficiency thresholds, when they are read, then they transcribe the calculation guards`() {
        // Given / When / Then
        assertEquals(1, MetricSufficiencyRules.MIN_PROGRESSION_OBSERVATIONS)
        assertEquals(2, MetricSufficiencyRules.MIN_LOAD_VELOCITY_SESSIONS)
        assertEquals(1, MetricSufficiencyRules.MIN_RIR_SETS)
        assertEquals(1, MetricSufficiencyRules.MIN_WEEKLY_TARGET)
        assertEquals(1, MetricSufficiencyRules.MIN_MICROCYCLE_SESSIONS)
        assertEquals(2, MetricSufficiencyRules.MIN_EVOLUTION_MICROCYCLES)
        assertEquals(4, MetricSufficiencyRules.MIN_TREND_MICROCYCLES)
    }

    @Test
    fun `given every insufficient case, when the state is inspected, then none of them is available`() {
        // Given
        val insufficientStates = listOf(
            MetricSufficiencyRules.progressionRate(0.0, 0),
            MetricSufficiencyRules.loadVelocity(0.0, 1, false),
            MetricSufficiencyRules.averageRir(null, 0),
            MetricSufficiencyRules.adherence(0.0, 0),
            MetricSufficiencyRules.tonnage(0.0, 0),
            MetricSufficiencyRules.distribution(0.0, 0),
        )

        // When / Then
        insufficientStates.forEach { state ->
            assertTrue(state is MetricValue.Insufficient)
        }
    }

    @Test
    fun `given a requirement already met, when missing is read, then it is never negative`() {
        // Given
        val requirement = MetricRequirement(MetricRequirementKind.COMPLETE_MICROCYCLES, 9, 4)

        // When
        val missing = requirement.missing

        // Then
        assertEquals(0, missing)
    }

    // --- Formatting -------------------------------------------------------

    @Test
    fun `given a tonnage above a thousand, when it is formatted, then the thousand separator is applied`() {
        // Given
        val amount = 12480.0

        // When
        val result = MetricFormatRules.formatAmount(amount, MetricUnit.KILOGRAM)

        // Then
        assertEquals("12.480", result)
    }

    @Test
    fun `given a positive load velocity, when it is formatted, then the sign is explicit`() {
        // Given
        val amount = 2.5

        // When
        val result = MetricFormatRules.formatAmount(amount, MetricUnit.KILOGRAM_PER_SESSION)

        // Then
        assertEquals("+2,5", result)
    }

    @Test
    fun `given a negative load velocity, when it is formatted, then it keeps its minus sign`() {
        // Given
        val amount = -1.0

        // When
        val result = MetricFormatRules.formatAmount(amount, MetricUnit.KILOGRAM_PER_SESSION)

        // Then
        assertEquals("-1,0", result)
    }

    @Test
    fun `given a zero load velocity, when it is formatted, then it carries no sign`() {
        // Given
        val amount = 0.0

        // When
        val result = MetricFormatRules.formatAmount(amount, MetricUnit.KILOGRAM_PER_SESSION)

        // Then
        assertEquals("0,0", result)
    }

    @Test
    fun `given a rir average, when it is formatted, then it keeps a single decimal`() {
        // Given
        val amount = 1.25

        // When
        val result = MetricFormatRules.formatAmount(amount, MetricUnit.RIR)

        // Then
        assertEquals(1, result.substringAfter(",", "").length)
    }

    @Test
    fun `given a percentage, when it is formatted, then it is rounded to an integer`() {
        // Given
        val amount = 83.4

        // When
        val result = MetricFormatRules.formatAmount(amount, MetricUnit.PERCENTAGE)

        // Then
        assertEquals("83", result)
    }
}
