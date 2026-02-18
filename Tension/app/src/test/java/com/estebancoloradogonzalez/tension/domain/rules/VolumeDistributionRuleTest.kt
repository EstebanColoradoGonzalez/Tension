package com.estebancoloradogonzalez.tension.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Test

class VolumeDistributionRuleTest {

    @Test
    fun `two zones with 16 total sets`() {
        val setsByZone = mapOf("Pecho" to 10, "Espalda" to 6)
        val result = VolumeDistributionRule.calculate(setsByZone, 16)
        assertEquals(62.5, result["Pecho"]!!, 0.001)
        assertEquals(37.5, result["Espalda"]!!, 0.001)
    }

    @Test
    fun `total sets zero returns all zeros`() {
        val setsByZone = mapOf("Pecho" to 10, "Espalda" to 6)
        val result = VolumeDistributionRule.calculate(setsByZone, 0)
        assertEquals(0.0, result["Pecho"]!!, 0.001)
        assertEquals(0.0, result["Espalda"]!!, 0.001)
    }

    @Test
    fun `single zone at 100 percent`() {
        val setsByZone = mapOf("Pecho" to 8)
        val result = VolumeDistributionRule.calculate(setsByZone, 8)
        assertEquals(100.0, result["Pecho"]!!, 0.001)
    }

    @Test
    fun `three zones distribute correctly`() {
        val setsByZone = mapOf("Pecho" to 4, "Espalda" to 4, "Hombro" to 2)
        val result = VolumeDistributionRule.calculate(setsByZone, 10)
        assertEquals(40.0, result["Pecho"]!!, 0.001)
        assertEquals(40.0, result["Espalda"]!!, 0.001)
        assertEquals(20.0, result["Hombro"]!!, 0.001)
    }

    @Test
    fun `empty map returns empty map`() {
        val result = VolumeDistributionRule.calculate(emptyMap(), 0)
        assertEquals(emptyMap<String, Double>(), result)
    }
}
