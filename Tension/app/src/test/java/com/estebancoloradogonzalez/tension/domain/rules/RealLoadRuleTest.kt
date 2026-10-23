package com.estebancoloradogonzalez.tension.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RealLoadRuleTest {

    private fun realLoad(
        externalKg: Double,
        percent: Int,
        equipment: String?,
        bodyWeightKg: Double? = BODY_WEIGHT,
        isIsometric: Boolean = false,
    ) = RealLoadRule.realLoadKg(
        externalLoadKg = externalKg,
        bodyWeightKg = bodyWeightKg,
        bodyWeightPercent = percent,
        isIsometric = isIsometric,
        equipmentTypeName = equipment,
    )

    // CA-49.01 — the three examples of the story, with an 80 kg body weight (QA-01).

    @Test
    fun `given pull-ups on the bar at full percent, when calculated, then the load is the body weight`() {
        // Given / When
        val load = realLoad(externalKg = 0.0, percent = 100, equipment = "Barra Fija")

        // Then — 80 × 8 = 640 kg of tonnage
        assertEquals(80.0, load, TOLERANCE)
        assertEquals(640.0, load * 8, TOLERANCE)
    }

    @Test
    fun `given weighted pull-ups, when calculated, then the load is body plus ballast`() {
        // Given / When
        val load = realLoad(externalKg = 10.0, percent = 100, equipment = ExternalLoadRule.PESO_ANADIDO)

        // Then — (80 + 10) × 6 = 540 kg
        assertEquals(90.0, load, TOLERANCE)
        assertEquals(540.0, load * 6, TOLERANCE)
    }

    @Test
    fun `given a bulgarian split squat with dumbbells, when calculated, then eighty five percent is added`() {
        // Given / When
        val load = realLoad(externalKg = 20.0, percent = 85, equipment = "Mancuerna")

        // Then — (68 + 20) × 10 = 880 kg
        assertEquals(88.0, load, TOLERANCE)
        assertEquals(880.0, load * 10, TOLERANCE)
    }

    @Test
    fun `given the rest of the qa-01 table, when calculated, then every tonnage matches`() {
        // Given / When / Then
        assertEquals(816.0, realLoad(0.0, 85, ExternalLoadRule.PESO_CORPORAL) * 12, TOLERANCE)
        assertEquals(1_080.0, realLoad(40.0, 85, "Barra") * 10, TOLERANCE)
        assertEquals(420.0, realLoad(0.0, 35, ExternalLoadRule.PESO_CORPORAL) * 15, TOLERANCE)
    }

    // Rule 3 — implements that add body weight and implements that do not (QA-02).

    @Test
    fun `given each implement that adds body weight, when asked, then it adds`() {
        // Given
        val adders = listOf(
            "Peso Corporal", "Barra Fija", "Peso Añadido", "Mancuerna", "Barra", "Barra EZ",
            "Máquina Smith", "Pesa Rusa", "Balón Medicinal", "Paralelas/Dip Station",
            "Rodillo de Abdomen",
        )

        // When / Then
        adders.forEach { assertTrue(it, RealLoadRule.addsBodyWeight(it)) }
    }

    @Test
    fun `given machines bands and suspension, when asked, then they do not add body weight`() {
        // Given
        val nonAdders = listOf("Máquina", "Polea", "Banda Elástica", "TRX/Suspensión", null, "Desconocido")

        // When / Then
        nonAdders.forEach { assertFalse(it.toString(), RealLoadRule.addsBodyWeight(it)) }
    }

    @Test
    fun `given a crunch on cable and on machine, when calculated, then only the external load counts`() {
        // Given / When / Then — QA-02: 30 × 12 = 360 and 25 × 12 = 300
        assertEquals(360.0, realLoad(30.0, 35, "Polea") * 12, TOLERANCE)
        assertEquals(300.0, realLoad(25.0, 35, "Máquina") * 12, TOLERANCE)
    }

    @Test
    fun `given an assisted pull-up on machine, when calculated, then it still weighs zero`() {
        // Given / When
        val load = realLoad(externalKg = 0.0, percent = 100, equipment = "Máquina")

        // Then — CA-49.07
        assertEquals(0.0, load, TOLERANCE)
    }

    // CA-49.07 — 0 %, isometrics and no body weight in force (QA-03).

    @Test
    fun `given an exercise at zero percent, when calculated, then the load is the external load`() {
        // Given / When / Then — bench press 60 kg × 10 = 600 kg, as before this story
        assertEquals(600.0, realLoad(60.0, 0, "Barra") * 10, TOLERANCE)
    }

    @Test
    fun `given an isometric exercise, when calculated, then no body weight is added`() {
        // Given / When
        val load = realLoad(externalKg = 0.0, percent = 50, equipment = "Peso Corporal", isIsometric = true)

        // Then
        assertEquals(0.0, load, TOLERANCE)
    }

    @Test
    fun `given no body weight in force on the date, when calculated, then the load is the external load`() {
        // Given / When
        val load = realLoad(externalKg = 10.0, percent = 100, equipment = "Peso Añadido", bodyWeightKg = null)

        // Then
        assertEquals(10.0, load, TOLERANCE)
    }

    @Test
    fun `given the body weight of each date, when calculated, then each session uses its own`() {
        // Given / When / Then — QA-06: 78 kg on 05/10 and 80 kg on 14/10, 8 pull-ups
        assertEquals(624.0, realLoad(0.0, 100, "Barra Fija", bodyWeightKg = 78.0) * 8, TOLERANCE)
        assertEquals(640.0, realLoad(0.0, 100, "Barra Fija", bodyWeightKg = 80.0) * 8, TOLERANCE)
    }

    // CA-49.05 — validation of the percentage (QA-11).

    @Test
    fun `given the limits zero and one hundred, when parsed, then both are accepted`() {
        // Given / When / Then
        assertEquals(0, RealLoadRule.parsePercent("0"))
        assertEquals(100, RealLoadRule.parsePercent("100"))
        assertEquals(70, RealLoadRule.parsePercent(" 70 "))
    }

    @Test
    fun `given out of range decimal or blank values, when parsed, then they are rejected`() {
        // Given
        val invalid = listOf("-1", "101", "50.5", "50,5", "", "   ", "abc", "+5", "99999999999")

        // When / Then
        invalid.forEach { assertNull(it, RealLoadRule.parsePercent(it)) }
    }

    private companion object {
        const val BODY_WEIGHT = 80.0
        const val TOLERANCE = 0.001
    }
}
