package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.SetForTonnage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TonnageRuleTest {

    @Test
    fun `two muscle groups calculated correctly`() {
        val sets = listOf(
            SetForTonnage(60.0, 10, "Pecho"),
            SetForTonnage(60.0, 8, "Pecho"),
            SetForTonnage(40.0, 12, "Espalda"),
        )
        val result = TonnageRule.calculateForMuscleGroup(sets)
        // Pecho: 60*10 + 60*8 = 1080
        assertEquals(1080.0, result["Pecho"]!!, 0.001)
        // Espalda: 40*12 = 480
        assertEquals(480.0, result["Espalda"]!!, 0.001)
    }

    @Test
    fun `empty sets returns empty map`() {
        val result = TonnageRule.calculateForMuscleGroup(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `multi-zone exercise contributes fully to each group`() {
        // Sentadilla Búlgara: same set contributes to both Cuádriceps and Glúteos
        val sets = listOf(
            SetForTonnage(50.0, 8, "Cuádriceps"),
            SetForTonnage(50.0, 8, "Glúteos"),
        )
        val result = TonnageRule.calculateForMuscleGroup(sets)
        assertEquals(400.0, result["Cuádriceps"]!!, 0.001)
        assertEquals(400.0, result["Glúteos"]!!, 0.001)
    }

    @Test
    fun `single set single group`() {
        val sets = listOf(SetForTonnage(100.0, 5, "Hombro"))
        val result = TonnageRule.calculateForMuscleGroup(sets)
        assertEquals(500.0, result["Hombro"]!!, 0.001)
    }

    @Test
    fun `bodyweight with zero weight produces zero tonnage`() {
        val sets = listOf(SetForTonnage(0.0, 10, "Espalda"))
        val result = TonnageRule.calculateForMuscleGroup(sets)
        assertEquals(0.0, result["Espalda"]!!, 0.001)
    }
}
