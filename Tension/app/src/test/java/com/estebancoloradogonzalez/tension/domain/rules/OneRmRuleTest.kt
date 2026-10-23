package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.OneRmCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val TOLERANCE = 0.05

class OneRmRuleTest {

    // CA-42.03 — la condicion es EXACTA: 10 repeticiones y RIR 1. Nada mas califica.

    @Test
    fun `given ten reps with rir one over external load, when evaluated, then the set qualifies`() {
        assertTrue(OneRmRule.qualifies(reps = 10, rir = 1, hasCountableLoad = true, loadKg = 20.0))
    }

    @Test
    fun `given ten reps with rir zero, when evaluated, then the set does not qualify`() {
        assertFalse(OneRmRule.qualifies(reps = 10, rir = 0, hasCountableLoad = true, loadKg = 20.0))
    }

    @Test
    fun `given ten reps with rir two, when evaluated, then the set does not qualify`() {
        assertFalse(OneRmRule.qualifies(reps = 10, rir = 2, hasCountableLoad = true, loadKg = 20.0))
    }

    @Test
    fun `given nine reps with rir one, when evaluated, then the set does not qualify`() {
        assertFalse(OneRmRule.qualifies(reps = 9, rir = 1, hasCountableLoad = true, loadKg = 20.0))
    }

    @Test
    fun `given eleven reps with rir one, when evaluated, then the set does not qualify`() {
        assertFalse(OneRmRule.qualifies(reps = 11, rir = 1, hasCountableLoad = true, loadKg = 20.0))
    }

    /**
     * Doce repeticiones con RIR 1 es el caso del preview que mas se parece a calificar: mas
     * trabajo, misma proximidad al fallo, y aun asi fuera. La condicion no es un minimo.
     */
    @Test
    fun `given twelve reps with rir one, when evaluated, then the set does not qualify`() {
        assertFalse(OneRmRule.qualifies(reps = 12, rir = 1, hasCountableLoad = true, loadKg = 20.0))
    }

    // CA-42.05 — sin carga externa no hay 1RM. El veredicto llega de ExternalLoadRule.

    @Test
    fun `given the reference set without external load, when evaluated, then it does not qualify`() {
        assertFalse(
            OneRmRule.qualifies(reps = 10, rir = 1, hasCountableLoad = false, loadKg = 20.0),
        )
    }

    /**
     * Un isometrico entra aqui como `hasCountableLoad = false`: en el las repeticiones son
     * segundos sostenidos, asi que «10 repeticiones» no significa nada.
     */
    @Test
    fun `given an isometric set, when evaluated, then it does not qualify`() {
        assertFalse(
            OneRmRule.qualifies(
                reps = OneRmRule.REFERENCE_REPS,
                rir = OneRmRule.REFERENCE_RIR,
                hasCountableLoad = ExternalLoadRule.isCaptureEnabled(
                    isBodyweight = false,
                    isIsometric = true,
                    equipmentName = "Colchoneta",
                ),
                loadKg = 20.0,
            ),
        )
    }

    /**
     * The four `Dominadas` options resolved the way the system resolves them since HU-49: with
     * [OneRmRule.hasCountableLoad]. At 0 % only `Peso Añadido` leaves a 1RM, as in HU-42.
     */
    @Test
    fun `given the pull-up options at zero percent, when evaluated, then only peso anadido qualifies`() {
        // Given
        val options = mapOf(
            ExternalLoadRule.PESO_CORPORAL to false,
            "Barra Fija" to false,
            "Máquina" to false,
            ExternalLoadRule.PESO_ANADIDO to true,
        )

        options.forEach { (equipmentName, expected) ->
            // When
            val qualifies = OneRmRule.qualifies(
                reps = 10,
                rir = 1,
                hasCountableLoad = OneRmRule.hasCountableLoad(
                    isBodyweight = true,
                    isIsometric = false,
                    equipmentTypeName = equipmentName,
                    bodyWeightPercent = 0,
                ),
                loadKg = 10.0,
            )

            // Then
            assertTrue(equipmentName, expected == qualifies)
        }
    }

    /**
     * CA-49.02 / CA-49.07 — at 100 %, `Barra Fija` and `Peso Corporal` qualify through the body
     * weight; the assisted pull-up on `Máquina` still has no 1RM.
     */
    @Test
    fun `given the pull-up options at full percent, when evaluated, then only the assisted machine fails`() {
        // Given
        val options = mapOf(
            ExternalLoadRule.PESO_CORPORAL to true,
            "Barra Fija" to true,
            "Máquina" to false,
            ExternalLoadRule.PESO_ANADIDO to true,
        )

        options.forEach { (equipmentName, expected) ->
            // When
            val qualifies = OneRmRule.qualifies(
                reps = 10,
                rir = 1,
                hasCountableLoad = OneRmRule.hasCountableLoad(
                    isBodyweight = true,
                    isIsometric = false,
                    equipmentTypeName = equipmentName,
                    bodyWeightPercent = 100,
                ),
                loadKg = 80.0,
            )

            // Then
            assertTrue(equipmentName, expected == qualifies)
        }
    }

    /**
     * Un cero no es un 1RM. La CA no lo enuncia, pero CA-42.07 prohibe mostrar ceros, y no
     * crear la fila es mas barato que crearla y ocultarla despues.
     */
    @Test
    fun `given zero weight with capture enabled, when evaluated, then it does not qualify`() {
        assertFalse(OneRmRule.qualifies(reps = 10, rir = 1, hasCountableLoad = true, loadKg = 0.0))
    }

    // CA-42.03 — la formula, en los tres ejemplos que la historia fija.

    @Test
    fun `given twelve kilos at ten reps, when estimated, then the one rm is sixteen`() {
        assertEquals(16.0, OneRmRule.estimate(12.0, 10), TOLERANCE)
    }

    @Test
    fun `given seventy kilos at ten reps, when estimated, then the one rm is ninety three point four`() {
        assertEquals(93.4, OneRmRule.estimate(70.0, 10), TOLERANCE)
    }

    @Test
    fun `given thirty kilos at ten reps, when estimated, then the one rm is forty`() {
        assertEquals(40.0, OneRmRule.estimate(30.0, 10), TOLERANCE)
    }

    /**
     * La equivalencia que la formula general esconde y que justifica no precomputarla: a 10
     * repeticiones el denominador vale 0.7498 y el resultado es `Peso × 1.3337`.
     */
    @Test
    fun `given ten reps, when estimated, then the formula equals weight times the fixed factor`() {
        listOf(5.0, 12.5, 47.5, 100.0, 220.0).forEach { weightKg ->
            assertEquals(
                weightKg * 1.3337,
                OneRmRule.estimate(weightKg, OneRmRule.REFERENCE_REPS),
                TOLERANCE,
            )
        }
    }

    /**
     * La razon por la que la condicion exacta es tambien una garantia numerica: el
     * denominador se anula en 36.97 repeticiones y es negativo por encima. El punto unico de
     * operacion queda muy lejos, y esta prueba documenta de que se libra.
     */
    @Test
    fun `given the reference reps, when estimated, then the formula is far from its singularity`() {
        val singularity = 1.0278 / 0.0278

        assertTrue(singularity > 36.9 && singularity < 37.0)
        assertTrue(OneRmRule.REFERENCE_REPS < singularity)
        assertTrue(OneRmRule.estimate(20.0, OneRmRule.REFERENCE_REPS) > 0)
        // Por encima de la singularidad la formula devuelve un valor negativo. Ninguna serie
        // llega aqui porque `qualifies` lo impide, y eso es lo que esta prueba fija.
        assertTrue(OneRmRule.estimate(20.0, 40) < 0)
        assertFalse(OneRmRule.qualifies(reps = 40, rir = 1, hasCountableLoad = true, loadKg = 20.0))
    }

    // HU-49 — the record is rebuilt from the reference sets, over their real load.

    private fun candidate(
        loadKg: Double,
        date: String,
        reps: Int = OneRmRule.REFERENCE_REPS,
        rir: Int = OneRmRule.REFERENCE_RIR,
        hasCountableLoad: Boolean = true,
    ) = OneRmCandidate(reps, rir, loadKg, hasCountableLoad, date)

    @Test
    fun `given reference sets on the real load, when the record is built, then it matches qa-07`() {
        // Given — pull-ups on the bar 80, weighted 90, split squat with 20 kg 88
        val cases = mapOf(80.0 to 106.7, 90.0 to 120.0, 88.0 to 117.4)

        cases.forEach { (loadKg, expectedOneRm) ->
            // When
            val record = OneRmRule.record(listOf(candidate(loadKg, "2026-10-14")))

            // Then
            assertEquals(expectedOneRm, record!!.oneRmKg, TOLERANCE)
            assertEquals("2026-10-14", record.achievedOn)
        }
    }

    @Test
    fun `given sets over several sessions, when the record is built, then it is the maximum with its date`() {
        // Given — QA-08: 78 kg on 05/10, 80 kg on 14/10, 77 kg on 22/10
        val candidates = listOf(
            candidate(78.0, "2026-10-05"),
            candidate(80.0, "2026-10-14"),
            candidate(77.0, "2026-10-22"),
        )

        // When
        val record = OneRmRule.record(candidates)

        // Then
        assertEquals(106.7, record!!.oneRmKg, TOLERANCE)
        assertEquals("2026-10-14", record.achievedOn)
    }

    @Test
    fun `given two sets with the same real load, when the record is built, then the earliest date wins`() {
        // Given
        val candidates = listOf(candidate(80.0, "2026-10-20"), candidate(80.0, "2026-10-14"))

        // When
        val record = OneRmRule.record(candidates)

        // Then
        assertEquals("2026-10-14", record!!.achievedOn)
    }

    @Test
    fun `given sets that do not qualify, when the record is built, then they are ignored`() {
        // Given — 9 reps, RIR 2, no countable load and zero load
        val candidates = listOf(
            candidate(120.0, "2026-10-22", reps = 9),
            candidate(120.0, "2026-10-22", rir = 2),
            candidate(120.0, "2026-10-22", hasCountableLoad = false),
            candidate(0.0, "2026-10-22"),
            candidate(80.0, "2026-10-14"),
        )

        // When
        val record = OneRmRule.record(candidates)

        // Then
        assertEquals(106.7, record!!.oneRmKg, TOLERANCE)
    }

    @Test
    fun `given no qualifying set, when the record is built, then there is none`() {
        // Given — QA-15 step 3: the set corrected to 9 reps no longer qualifies
        val candidates = listOf(candidate(80.0, "2026-10-22", reps = 9))

        // When / Then
        assertNull(OneRmRule.record(candidates))
        assertNull(OneRmRule.record(emptyList()))
    }

    @Test
    fun `given a lower percentage, when the record is rebuilt, then it goes down`() {
        // Given — QA-12: split squat with 20 kg at 80 %: 05/10 (78 kg) 82.4 and 14/10 (80 kg) 84.0
        val candidates = listOf(
            candidate(RealLoadRule.realLoadKg(20.0, 78.0, 80, false, "Mancuerna"), "2026-10-05"),
            candidate(RealLoadRule.realLoadKg(20.0, 80.0, 80, false, "Mancuerna"), "2026-10-14"),
        )

        // When
        val record = OneRmRule.record(candidates)

        // Then — 84 × 1.3337 = 112.0 kg, 1.40× over 80 kg
        assertEquals(112.0, record!!.oneRmKg, TOLERANCE)
        assertEquals("2026-10-14", record.achievedOn)
        assertEquals(1.40, OneRmRule.relativeStrength(record.oneRmKg, 80.0)!!, 0.005)
    }

    // CA-49.03 — relative strength.

    @Test
    fun `given a record and the body weight of its date, when divided, then the relative strength follows`() {
        // Given / When / Then — QA-09: 93.4 / 78 = 1.20, not 93.4 / 80 = 1.17
        assertEquals(1.20, OneRmRule.relativeStrength(93.4, 78.0)!!, 0.005)
    }

    @Test
    fun `given no body weight, when divided, then there is no relative strength`() {
        // Given / When / Then
        assertNull(OneRmRule.relativeStrength(93.4, null))
        assertNull(OneRmRule.relativeStrength(93.4, 0.0))
    }
}
