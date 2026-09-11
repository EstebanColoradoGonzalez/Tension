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

    // ============================================================
    // CA-41.04 — todas las zonas cuentan, sin ponderación
    // ============================================================

    /**
     * `Press de Banca Inclinado` tras HU-41: una zona principal (`Pectoral Superior`) y dos
     * secundarias (`Deltoides Anterior`, `Tríceps Braquial`), en tres grupos distintos.
     *
     * La misma serie aporta su tonelaje **íntegro** a los tres. La jerarquía es informativa
     * para el ejecutante, no un peso de cálculo: si alguna vez se ponderara, este es el
     * caso que se pondría rojo.
     */
    @Test
    fun `primary and secondary zones contribute the same tonnage`() {
        val sets = listOf(
            SetForTonnage(80.0, 10, "Pecho"),
            SetForTonnage(80.0, 10, "Hombro"),
            SetForTonnage(80.0, 10, "Tríceps"),
        )

        val result = TonnageRule.calculateForMuscleGroup(sets)

        assertEquals(800.0, result["Pecho"]!!, 0.001)
        assertEquals(800.0, result["Hombro"]!!, 0.001)
        assertEquals(800.0, result["Tríceps"]!!, 0.001)
    }

    @Test
    fun `the aggregation axis is the muscle group, not the zone`() {
        // Las 33 zonas caben dentro de los 14 grupos, así que dos zonas finas del mismo
        // grupo suman en él y no abren un eje nuevo: ningún KPI cambió de definición.
        val sets = listOf(
            SetForTonnage(30.0, 12, "Hombro"), // Deltoides Lateral
            SetForTonnage(45.0, 10, "Hombro"), // Deltoides Anterior
        )

        val result = TonnageRule.calculateForMuscleGroup(sets)

        assertEquals(1, result.size)
        assertEquals(810.0, result["Hombro"]!!, 0.001)
    }
}
