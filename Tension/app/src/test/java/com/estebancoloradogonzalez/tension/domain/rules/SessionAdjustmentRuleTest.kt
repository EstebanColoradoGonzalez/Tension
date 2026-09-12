package com.estebancoloradogonzalez.tension.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionAdjustmentRuleTest {

    // ----- Prescripción por defecto (CA-43.03) -----

    @Test
    fun `un ejercicio corriente se prescribe a 3 series de 8 a 12`() {
        assertEquals(3, SessionAdjustmentRule.DEFAULT_SETS)
        assertEquals(
            "8-12",
            SessionAdjustmentRule.defaultReps(isIsometric = false, isToTechnicalFailure = false),
        )
    }

    @Test
    fun `un isometrico se prescribe en segundos`() {
        assertEquals(
            "30-45_SEC",
            SessionAdjustmentRule.defaultReps(isIsometric = true, isToTechnicalFailure = false),
        )
    }

    @Test
    fun `uno de fallo tecnico se prescribe sin limite superior`() {
        assertEquals(
            "TO_TECHNICAL_FAILURE",
            SessionAdjustmentRule.defaultReps(isIsometric = false, isToTechnicalFailure = true),
        )
    }

    @Test
    fun `el modo isometrico manda sobre el de fallo tecnico`() {
        assertEquals(
            "30-45_SEC",
            SessionAdjustmentRule.defaultReps(isIsometric = true, isToTechnicalFailure = true),
        )
    }

    // ----- Presupuesto: uno entra, uno puede salir (CA-43.04) -----

    @Test
    fun `sin anadidos el presupuesto es cero`() {
        assertEquals(0, SessionAdjustmentRule.budget(addedCount = 0, withdrawnCount = 0))
    }

    @Test
    fun `la secuencia completa del ajuste conserva el presupuesto esperado`() {
        // añadir uno
        assertEquals(1, SessionAdjustmentRule.budget(addedCount = 1, withdrawnCount = 0))
        // retirar uno del plan
        assertEquals(0, SessionAdjustmentRule.budget(addedCount = 1, withdrawnCount = 1))
        // reponer el retirado: entra como añadido
        assertEquals(1, SessionAdjustmentRule.budget(addedCount = 2, withdrawnCount = 1))
        // deshacer el añadido original
        assertEquals(0, SessionAdjustmentRule.budget(addedCount = 1, withdrawnCount = 1))
    }

    @Test
    fun `dos anadidos permiten retirar dos`() {
        assertEquals(2, SessionAdjustmentRule.budget(addedCount = 2, withdrawnCount = 0))
    }

    // ----- Veredicto de un ejercicio del plan (CA-43.04, CA-43.06) -----

    @Test
    fun `sin anadidos ningun ejercicio del plan puede retirarse`() {
        assertEquals(
            WithdrawalVerdict.BudgetExhausted,
            SessionAdjustmentRule.verdictForPlanExercise(
                addedCount = 0,
                withdrawnCount = 0,
                completedSets = 0,
            ),
        )
    }

    @Test
    fun `con presupuesto un ejercicio del plan sin series puede retirarse`() {
        assertEquals(
            WithdrawalVerdict.Allowed,
            SessionAdjustmentRule.verdictForPlanExercise(
                addedCount = 1,
                withdrawnCount = 0,
                completedSets = 0,
            ),
        )
    }

    @Test
    fun `agotado el presupuesto vuelve a bloquearse`() {
        assertEquals(
            WithdrawalVerdict.BudgetExhausted,
            SessionAdjustmentRule.verdictForPlanExercise(
                addedCount = 1,
                withdrawnCount = 1,
                completedSets = 0,
            ),
        )
    }

    @Test
    fun `las series mandan sobre el presupuesto`() {
        // Con presupuesto disponible, la causa sigue siendo la serie: es la única que no se
        // levanta añadiendo nada. El orden de evaluación vive en la regla, no en quien llama.
        assertEquals(
            WithdrawalVerdict.HasSets(2),
            SessionAdjustmentRule.verdictForPlanExercise(
                addedCount = 5,
                withdrawnCount = 0,
                completedSets = 2,
            ),
        )
    }

    // ----- Veredicto de un ejercicio añadido (CA-43.05, CA-43.06) -----

    @Test
    fun `un anadido sin retiros vigentes puede quitarse`() {
        assertEquals(
            WithdrawalVerdict.Allowed,
            SessionAdjustmentRule.verdictForAddedExercise(
                addedCount = 1,
                withdrawnCount = 0,
                completedSets = 0,
            ),
        )
    }

    @Test
    fun `deshacer un anadido con un retiro vigente romperia la invariante`() {
        assertEquals(
            WithdrawalVerdict.WouldBreakInvariant,
            SessionAdjustmentRule.verdictForAddedExercise(
                addedCount = 1,
                withdrawnCount = 1,
                completedSets = 0,
            ),
        )
    }

    @Test
    fun `repuesto el retirado el anadido original ya puede quitarse`() {
        assertEquals(
            WithdrawalVerdict.Allowed,
            SessionAdjustmentRule.verdictForAddedExercise(
                addedCount = 2,
                withdrawnCount = 1,
                completedSets = 0,
            ),
        )
    }

    @Test
    fun `un anadido con series tampoco se retira`() {
        assertEquals(
            WithdrawalVerdict.HasSets(1),
            SessionAdjustmentRule.verdictForAddedExercise(
                addedCount = 3,
                withdrawnCount = 0,
                completedSets = 1,
            ),
        )
    }

    @Test
    fun `las series mandan tambien sobre la invariante`() {
        assertEquals(
            WithdrawalVerdict.HasSets(3),
            SessionAdjustmentRule.verdictForAddedExercise(
                addedCount = 1,
                withdrawnCount = 1,
                completedSets = 3,
            ),
        )
    }

    /**
     * La invariante enunciada como propiedad: recorriendo todo el espacio de conteos
     * razonables, ninguna secuencia permitida deja los añadidos por debajo de los retirados.
     */
    @Test
    fun `ninguna secuencia permitida deja los anadidos por debajo de los retirados`() {
        for (added in 0..6) {
            for (withdrawn in 0..added) {
                val afterPlanWithdrawal = SessionAdjustmentRule.verdictForPlanExercise(
                    addedCount = added,
                    withdrawnCount = withdrawn,
                    completedSets = 0,
                )
                if (afterPlanWithdrawal == WithdrawalVerdict.Allowed) {
                    assert(added >= withdrawn + 1) {
                        "retirar del plan con $added/$withdrawn romperia la invariante"
                    }
                }
                val afterUndo = SessionAdjustmentRule.verdictForAddedExercise(
                    addedCount = added,
                    withdrawnCount = withdrawn,
                    completedSets = 0,
                )
                if (afterUndo == WithdrawalVerdict.Allowed) {
                    assert(added - 1 >= withdrawn) {
                        "deshacer con $added/$withdrawn romperia la invariante"
                    }
                }
            }
        }
    }
}
