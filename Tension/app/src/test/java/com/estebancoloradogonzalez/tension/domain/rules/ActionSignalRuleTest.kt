package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.ActionSignal
import com.estebancoloradogonzalez.tension.domain.model.ProgressionClassification
import org.junit.Assert.assertEquals
import org.junit.Test

class ActionSignalRuleTest {

    // --- Escenario 1: classification == null → FirstSession ---
    @Test
    fun `resolve — null classification → FirstSession`() {
        val result = resolve(classification = null)
        assertEquals(ActionSignal.FirstSession, result)
    }

    // --- Escenario 2: isométrico dominado → IsometricMastered ---
    @Test
    fun `resolve — isometric mastered → IsometricMastered`() {
        val result = resolve(
            classification = ProgressionClassification.POSITIVE_PROGRESSION,
            isIsometric = true,
            isMastered = true,
            totalReps = 180,
            setCount = 4,
        )
        assertEquals(ActionSignal.IsometricMastered, result)
    }

    // --- Escenario 3: isométrico progresión → IsometricSignal ---
    @Test
    fun `resolve — isometric positive progression → IsometricSignal(4, 42)`() {
        val result = resolve(
            classification = ProgressionClassification.POSITIVE_PROGRESSION,
            isIsometric = true,
            totalReps = 168,
            setCount = 4,
        )
        assertEquals(ActionSignal.IsometricSignal(4, 42), result)
    }

    // --- Escenario 4: isométrico mantenimiento ---
    @Test
    fun `resolve — isometric maintenance → IsometricSignal(4, 40)`() {
        val result = resolve(
            classification = ProgressionClassification.MAINTENANCE,
            isIsometric = true,
            totalReps = 160,
            setCount = 4,
        )
        assertEquals(ActionSignal.IsometricSignal(4, 40), result)
    }

    // --- Escenario 5: isométrico regresión ---
    @Test
    fun `resolve — isometric regression → IsometricSignal(4, 35)`() {
        val result = resolve(
            classification = ProgressionClassification.REGRESSION,
            isIsometric = true,
            totalReps = 140,
            setCount = 4,
        )
        assertEquals(ActionSignal.IsometricSignal(4, 35), result)
    }

    // --- Escenario 6: bodyweight progresión con historial ---
    @Test
    fun `resolve — bodyweight positive with history → BodyweightSignal(48, +3)`() {
        val result = resolve(
            classification = ProgressionClassification.POSITIVE_PROGRESSION,
            isBodyweight = true,
            totalReps = 48,
            previousTotalReps = 45,
        )
        assertEquals(ActionSignal.BodyweightSignal(48, 3), result)
    }

    // --- Escenario 7: bodyweight mantenimiento ---
    @Test
    fun `resolve — bodyweight maintenance → BodyweightSignal(45, 0)`() {
        val result = resolve(
            classification = ProgressionClassification.MAINTENANCE,
            isBodyweight = true,
            totalReps = 45,
            previousTotalReps = 45,
        )
        assertEquals(ActionSignal.BodyweightSignal(45, 0), result)
    }

    // --- Escenario 8: bodyweight regresión ---
    @Test
    fun `resolve — bodyweight regression → BodyweightSignal(40, -5)`() {
        val result = resolve(
            classification = ProgressionClassification.REGRESSION,
            isBodyweight = true,
            totalReps = 40,
            previousTotalReps = 45,
        )
        assertEquals(ActionSignal.BodyweightSignal(40, -5), result)
    }

    // --- Escenario 9: bodyweight sin historial previo ---
    @Test
    fun `resolve — bodyweight no previous → BodyweightSignal(48, null)`() {
        val result = resolve(
            classification = ProgressionClassification.POSITIVE_PROGRESSION,
            isBodyweight = true,
            totalReps = 48,
            previousTotalReps = null,
        )
        assertEquals(ActionSignal.BodyweightSignal(48, null), result)
    }

    // --- Escenario 10: estándar DU cumplido ---
    @Test
    fun `resolve — standard DU met → IncreaseLoad(62_5)`() {
        val result = resolve(
            classification = ProgressionClassification.POSITIVE_PROGRESSION,
            prescribedLoadKg = 62.5,
            avgWeightKg = 60.0,
        )
        assertEquals(ActionSignal.IncreaseLoad(62.5), result)
    }

    // --- Escenario 11: estándar DU NO cumplido + progresión ---
    @Test
    fun `resolve — standard DU not met + positive → ProgressInReps`() {
        val result = resolve(
            classification = ProgressionClassification.POSITIVE_PROGRESSION,
            prescribedLoadKg = 60.0,
            avgWeightKg = 60.0,
        )
        assertEquals(ActionSignal.ProgressInReps, result)
    }

    // --- Escenario 12: estándar DU NO cumplido + mantenimiento ---
    @Test
    fun `resolve — standard DU not met + maintenance → MaintainLoad with reps message`() {
        val result = resolve(
            classification = ProgressionClassification.MAINTENANCE,
            prescribedLoadKg = 60.0,
            avgWeightKg = 60.0,
        )
        assertEquals(
            ActionSignal.MaintainLoad("Mantener carga — progresar en reps"),
            result,
        )
    }

    // --- Escenario 13: estándar regresión + moduleRequiresDeload ---
    @Test
    fun `resolve — standard regression + module requires deload → ConsiderDeload`() {
        val result = resolve(
            classification = ProgressionClassification.REGRESSION,
            moduleRequiresDeload = true,
            avgWeightKg = 60.0,
        )
        assertEquals(ActionSignal.ConsiderDeload, result)
    }

    // --- Escenario 14: estándar regresión aislada ---
    @Test
    fun `resolve — standard regression isolated → MaintainLoad`() {
        val result = resolve(
            classification = ProgressionClassification.REGRESSION,
            moduleRequiresDeload = false,
            avgWeightKg = 60.0,
        )
        assertEquals(ActionSignal.MaintainLoad("Mantener carga"), result)
    }

    // --- Escenario 15: boundary tolerance (0.01 diff NOT > TOLERANCE) ---
    @Test
    fun `resolve — standard prescribed exactly 0_01 above avg → ProgressInReps (not IncreaseLoad)`() {
        val result = resolve(
            classification = ProgressionClassification.POSITIVE_PROGRESSION,
            prescribedLoadKg = 60.01,
            avgWeightKg = 60.0,
        )
        assertEquals(ActionSignal.ProgressInReps, result)
    }

    // --- Escenario 16: estándar prescribedLoadKg == null + mantenimiento ---
    @Test
    fun `resolve — standard null prescribed + maintenance → MaintainLoad`() {
        val result = resolve(
            classification = ProgressionClassification.MAINTENANCE,
            prescribedLoadKg = null,
            avgWeightKg = 60.0,
        )
        assertEquals(
            ActionSignal.MaintainLoad("Mantener carga — progresar en reps"),
            result,
        )
    }

    // --- Escenario 17: estándar prescribedLoadKg == avgWeightKg + progresión ---
    @Test
    fun `resolve — standard prescribed equals avg + positive → ProgressInReps`() {
        val result = resolve(
            classification = ProgressionClassification.POSITIVE_PROGRESSION,
            prescribedLoadKg = 60.0,
            avgWeightKg = 60.0,
        )
        assertEquals(ActionSignal.ProgressInReps, result)
    }

    // --- Escenario 18: isométrico con classification == null → FirstSession ---
    @Test
    fun `resolve — isometric null classification → FirstSession (null priority)`() {
        val result = resolve(
            classification = null,
            isIsometric = true,
            totalReps = 160,
            setCount = 4,
        )
        assertEquals(ActionSignal.FirstSession, result)
    }

    // --- Escenario 19: bodyweight regresión + moduleRequiresDeload → BodyweightSignal (NOT ConsiderDeload) ---
    @Test
    fun `resolve — bodyweight regression + module deload → BodyweightSignal (not ConsiderDeload)`() {
        val result = resolve(
            classification = ProgressionClassification.REGRESSION,
            isBodyweight = true,
            moduleRequiresDeload = true,
            totalReps = 40,
            previousTotalReps = 45,
        )
        assertEquals(ActionSignal.BodyweightSignal(40, -5), result)
    }

    // --- Helper factory ---
    private fun resolve(
        classification: ProgressionClassification? = ProgressionClassification.MAINTENANCE,
        prescribedLoadKg: Double? = null,
        avgWeightKg: Double = 0.0,
        moduleRequiresDeload: Boolean = false,
        isBodyweight: Boolean = false,
        isIsometric: Boolean = false,
        totalReps: Int = 0,
        previousTotalReps: Int? = null,
        setCount: Int = 4,
        isMastered: Boolean = false,
    ): ActionSignal {
        return ActionSignalRule.resolve(
            classification = classification,
            prescribedLoadKg = prescribedLoadKg,
            avgWeightKg = avgWeightKg,
            moduleRequiresDeload = moduleRequiresDeload,
            isBodyweight = isBodyweight,
            isIsometric = isIsometric,
            totalReps = totalReps,
            previousTotalReps = previousTotalReps,
            setCount = setCount,
            isMastered = isMastered,
        )
    }
}
