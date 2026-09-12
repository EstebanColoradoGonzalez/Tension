package com.estebancoloradogonzalez.tension.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val TOLERANCE = 0.05

class OneRmRuleTest {

    // CA-42.03 — la condicion es EXACTA: 10 repeticiones y RIR 1. Nada mas califica.

    @Test
    fun `given ten reps with rir one over external load, when evaluated, then the set qualifies`() {
        assertTrue(OneRmRule.qualifies(reps = 10, rir = 1, hasExternalLoad = true, weightKg = 20.0))
    }

    @Test
    fun `given ten reps with rir zero, when evaluated, then the set does not qualify`() {
        assertFalse(OneRmRule.qualifies(reps = 10, rir = 0, hasExternalLoad = true, weightKg = 20.0))
    }

    @Test
    fun `given ten reps with rir two, when evaluated, then the set does not qualify`() {
        assertFalse(OneRmRule.qualifies(reps = 10, rir = 2, hasExternalLoad = true, weightKg = 20.0))
    }

    @Test
    fun `given nine reps with rir one, when evaluated, then the set does not qualify`() {
        assertFalse(OneRmRule.qualifies(reps = 9, rir = 1, hasExternalLoad = true, weightKg = 20.0))
    }

    @Test
    fun `given eleven reps with rir one, when evaluated, then the set does not qualify`() {
        assertFalse(OneRmRule.qualifies(reps = 11, rir = 1, hasExternalLoad = true, weightKg = 20.0))
    }

    /**
     * Doce repeticiones con RIR 1 es el caso del preview que mas se parece a calificar: mas
     * trabajo, misma proximidad al fallo, y aun asi fuera. La condicion no es un minimo.
     */
    @Test
    fun `given twelve reps with rir one, when evaluated, then the set does not qualify`() {
        assertFalse(OneRmRule.qualifies(reps = 12, rir = 1, hasExternalLoad = true, weightKg = 20.0))
    }

    // CA-42.05 — sin carga externa no hay 1RM. El veredicto llega de ExternalLoadRule.

    @Test
    fun `given the reference set without external load, when evaluated, then it does not qualify`() {
        assertFalse(
            OneRmRule.qualifies(reps = 10, rir = 1, hasExternalLoad = false, weightKg = 20.0),
        )
    }

    /**
     * Un isometrico entra aqui como `hasExternalLoad = false`: en el las repeticiones son
     * segundos sostenidos, asi que «10 repeticiones» no significa nada.
     */
    @Test
    fun `given an isometric set, when evaluated, then it does not qualify`() {
        assertFalse(
            OneRmRule.qualifies(
                reps = OneRmRule.REFERENCE_REPS,
                rir = OneRmRule.REFERENCE_RIR,
                hasExternalLoad = ExternalLoadRule.isCaptureEnabled(
                    isBodyweight = false,
                    isIsometric = true,
                    equipmentName = "Colchoneta",
                ),
                weightKg = 20.0,
            ),
        )
    }

    /**
     * Las cuatro opciones de `Dominadas` (CA-42.05) resueltas como el sistema las resuelve:
     * con [ExternalLoadRule], y solo `Peso Añadido` deja 1RM.
     */
    @Test
    fun `given the four pull-up options, when evaluated, then only peso anadido qualifies`() {
        val options = mapOf(
            ExternalLoadRule.PESO_CORPORAL to false,
            "Barra Fija" to false,
            "Máquina" to false,
            ExternalLoadRule.PESO_ANADIDO to true,
        )

        options.forEach { (equipmentName, expected) ->
            val qualifies = OneRmRule.qualifies(
                reps = 10,
                rir = 1,
                hasExternalLoad = ExternalLoadRule.isCaptureEnabled(
                    isBodyweight = true,
                    isIsometric = false,
                    equipmentName = equipmentName,
                ),
                weightKg = 10.0,
            )
            assertTrue(equipmentName, expected == qualifies)
        }
    }

    /**
     * Un cero no es un 1RM. La CA no lo enuncia, pero CA-42.07 prohibe mostrar ceros, y no
     * crear la fila es mas barato que crearla y ocultarla despues.
     */
    @Test
    fun `given zero weight with capture enabled, when evaluated, then it does not qualify`() {
        assertFalse(OneRmRule.qualifies(reps = 10, rir = 1, hasExternalLoad = true, weightKg = 0.0))
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
        assertFalse(OneRmRule.qualifies(reps = 40, rir = 1, hasExternalLoad = true, weightKg = 20.0))
    }
}
