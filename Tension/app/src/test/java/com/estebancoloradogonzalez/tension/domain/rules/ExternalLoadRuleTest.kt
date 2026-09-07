package com.estebancoloradogonzalez.tension.domain.rules

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val MAQUINA = "Máquina"
private const val BARRA_FIJA = "Barra Fija"
private const val POLEA = "Polea"

class ExternalLoadRuleTest {

    // CA-39.05 — las cuatro opciones de Dominadas, dos comportamientos

    @Test
    fun `given a bodyweight exercise on peso corporal, when evaluated, then capture is disabled`() {
        assertFalse(
            ExternalLoadRule.isCaptureEnabled(
                isBodyweight = true,
                isIsometric = false,
                equipmentName = ExternalLoadRule.PESO_CORPORAL,
            ),
        )
    }

    @Test
    fun `given a bodyweight exercise on barra fija, when evaluated, then capture is disabled`() {
        assertFalse(
            ExternalLoadRule.isCaptureEnabled(
                isBodyweight = true,
                isIsometric = false,
                equipmentName = BARRA_FIJA,
            ),
        )
    }

    /**
     * La máquina asistida es el caso que motiva la regla: su carga es un contrapeso que
     * **resta** esfuerzo, y registrarlo como peso invertiría el significado del dato.
     */
    @Test
    fun `given a bodyweight exercise on assisted machine, when evaluated, then capture is disabled`() {
        assertFalse(
            ExternalLoadRule.isCaptureEnabled(
                isBodyweight = true,
                isIsometric = false,
                equipmentName = MAQUINA,
            ),
        )
    }

    @Test
    fun `given a bodyweight exercise on peso anadido, when evaluated, then capture is enabled`() {
        assertTrue(
            ExternalLoadRule.isCaptureEnabled(
                isBodyweight = true,
                isIsometric = false,
                equipmentName = ExternalLoadRule.PESO_ANADIDO,
            ),
        )
    }

    // El caso que la CA no contempla y la regla resuelve sin contradecirla

    /**
     * `Crunch Abdominal`, `Sentadilla Búlgara` y `Zancadas` admiten `Peso Corporal` sin
     * estar marcados como peso corporal. Decidir solo por la marca les pediría teclear un
     * peso para una serie hecha con el propio cuerpo.
     */
    @Test
    fun `given a regular exercise on peso corporal, when evaluated, then capture is disabled`() {
        assertFalse(
            ExternalLoadRule.isCaptureEnabled(
                isBodyweight = false,
                isIsometric = false,
                equipmentName = ExternalLoadRule.PESO_CORPORAL,
            ),
        )
    }

    /** Sobre un ejercicio normal la máquina sí es carga: la rama del contrapeso no aplica. */
    @Test
    fun `given a regular exercise on a machine, when evaluated, then capture is enabled`() {
        assertTrue(
            ExternalLoadRule.isCaptureEnabled(
                isBodyweight = false,
                isIsometric = false,
                equipmentName = MAQUINA,
            ),
        )
    }

    @Test
    fun `given a regular exercise on a cable, when evaluated, then capture is enabled`() {
        assertTrue(
            ExternalLoadRule.isCaptureEnabled(
                isBodyweight = false,
                isIsometric = false,
                equipmentName = POLEA,
            ),
        )
    }

    // El isométrico manda sobre el implemento: el cronómetro sustituye la captura

    @Test
    fun `given an isometric exercise, when evaluated, then capture is disabled whatever the implement`() {
        listOf(POLEA, MAQUINA, ExternalLoadRule.PESO_ANADIDO).forEach { equipment ->
            assertFalse(
                "El isométrico no captura peso con $equipment",
                ExternalLoadRule.isCaptureEnabled(
                    isBodyweight = true,
                    isIsometric = true,
                    equipmentName = equipment,
                ),
            )
        }
    }

    @Test
    fun `given no implement yet, when evaluated on a regular exercise, then capture is enabled`() {
        assertTrue(
            ExternalLoadRule.isCaptureEnabled(
                isBodyweight = false,
                isIsometric = false,
                equipmentName = null,
            ),
        )
    }

    // requiresPositiveLoad — el lastre es lo único que exige ser mayor que 0

    @Test
    fun `given peso anadido, when asked for a positive load, then it is required`() {
        assertTrue(ExternalLoadRule.requiresPositiveLoad(ExternalLoadRule.PESO_ANADIDO))
    }

    @Test
    fun `given any other implement, when asked for a positive load, then it is not required`() {
        listOf(POLEA, MAQUINA, BARRA_FIJA, ExternalLoadRule.PESO_CORPORAL, null).forEach {
            assertFalse("$it no debería exigir carga positiva", ExternalLoadRule.requiresPositiveLoad(it))
        }
    }
}
