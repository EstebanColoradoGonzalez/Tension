package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.ProgressionClassification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionConsolidationRuleTest {

    // ----- CA-40.04: la progresión consolida por disyunción -----

    @Test
    fun `given one pair progressed and another did not, when consolidating, then the exercise progressed`() {
        // Given — mancuerna progresa, polea se mantiene
        val classifications = listOf(
            ProgressionClassification.POSITIVE_PROGRESSION,
            ProgressionClassification.MAINTENANCE,
        )

        // When
        val result = ProgressionConsolidationRule.consolidate(classifications)

        // Then — alternar implementos no diluye la progresión general
        assertEquals(ProgressionClassification.POSITIVE_PROGRESSION, result)
    }

    @Test
    fun `given a progressing pair and a regressing one, when consolidating, then the exercise progressed`() {
        val result = ProgressionConsolidationRule.consolidate(
            listOf(
                ProgressionClassification.REGRESSION,
                ProgressionClassification.POSITIVE_PROGRESSION,
            ),
        )

        assertEquals(ProgressionClassification.POSITIVE_PROGRESSION, result)
    }

    @Test
    fun `given maintenance and regression, when consolidating, then it is maintenance`() {
        // Ninguno progresó, pero no todos retrocedieron: declarar regresión exigiría que
        // el mantenimiento del otro implemento no contase.
        val result = ProgressionConsolidationRule.consolidate(
            listOf(
                ProgressionClassification.MAINTENANCE,
                ProgressionClassification.REGRESSION,
            ),
        )

        assertEquals(ProgressionClassification.MAINTENANCE, result)
    }

    @Test
    fun `given every pair regressed, when consolidating, then it is a regression`() {
        val result = ProgressionConsolidationRule.consolidate(
            listOf(
                ProgressionClassification.REGRESSION,
                ProgressionClassification.REGRESSION,
            ),
        )

        assertEquals(ProgressionClassification.REGRESSION, result)
    }

    // ----- CA-40.02: estrenar un implemento nunca es regresión -----

    @Test
    fun `given the only pair of the session has no history, when consolidating, then it is null`() {
        // Given — primera vez que se entrena el ejercicio con polea
        val classifications = listOf<ProgressionClassification?>(null)

        // When
        val result = ProgressionConsolidationRule.consolidate(classifications)

        // Then — Sin Historial, jamás REGRESSION: es el bache que no existió
        assertNull(result)
    }

    @Test
    fun `given a classified pair and an unclassified one, when consolidating, then the unclassified is ignored`() {
        // Estrenar la polea en la misma sesión en la que la mancuerna progresó no puede
        // rebajar la lectura del ejercicio.
        val result = ProgressionConsolidationRule.consolidate(
            listOf(ProgressionClassification.POSITIVE_PROGRESSION, null),
        )

        assertEquals(ProgressionClassification.POSITIVE_PROGRESSION, result)
    }

    @Test
    fun `given a regressing pair and a strange new one, when consolidating, then it is a regression`() {
        // El par nuevo no aporta información; lo que el ejercicio hizo es lo que hizo el
        // único par con historial.
        val result = ProgressionConsolidationRule.consolidate(
            listOf(ProgressionClassification.REGRESSION, null),
        )

        assertEquals(ProgressionClassification.REGRESSION, result)
    }

    @Test
    fun `given no pairs at all, when consolidating, then it is null`() {
        assertNull(ProgressionConsolidationRule.consolidate(emptyList()))
    }

    // ----- CA-40.08: un solo implemento se comporta como antes de la historia -----

    @Test
    fun `given a single pair, when consolidating, then the result is that pair's classification`() {
        ProgressionClassification.entries.forEach { classification ->
            assertEquals(
                classification,
                ProgressionConsolidationRule.consolidate(listOf(classification)),
            )
        }
    }

    // ----- CA-40.05: la meseta consolida por conjunción -----

    @Test
    fun `given one pair above the threshold and another below, when evaluating, then there is no plateau`() {
        // Given — mancuerna lleva 11 sesiones paradas, polea 2, con umbral efectivo 10
        val counters = listOf(11, 2)

        // When
        val result = ProgressionConsolidationRule.isInPlateau(counters, effectiveThreshold = 10)

        // Then — un implemento en progresión desmiente la meseta
        assertFalse(result)
    }

    @Test
    fun `given every pair above the threshold, when evaluating, then the exercise is in plateau`() {
        assertTrue(
            ProgressionConsolidationRule.isInPlateau(listOf(11, 12), effectiveThreshold = 10),
        )
    }

    @Test
    fun `given a pair exactly at the threshold, when evaluating, then it counts as stalled`() {
        // El umbral se alcanza, no se supera: es la frontera que PlateauThresholdRule fija.
        assertTrue(
            ProgressionConsolidationRule.isInPlateau(listOf(10), effectiveThreshold = 10),
        )
    }

    @Test
    fun `given a single pair below the threshold, when evaluating, then there is no plateau`() {
        assertFalse(
            ProgressionConsolidationRule.isInPlateau(listOf(9), effectiveThreshold = 10),
        )
    }

    @Test
    fun `given an exercise with no pairs, when evaluating, then there is no plateau`() {
        // La conjunción vacía sería cierta por vacuidad, y un ejercicio nunca entrenado
        // acabaría con una alerta de meseta sobre nada.
        assertFalse(
            ProgressionConsolidationRule.isInPlateau(emptyList(), effectiveThreshold = 10),
        )
    }
}
