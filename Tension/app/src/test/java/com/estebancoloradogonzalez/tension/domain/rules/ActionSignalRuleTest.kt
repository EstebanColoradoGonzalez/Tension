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

    // --- Escenario 13: estándar regresión + routineRequiresDeload ---
    @Test
    fun `resolve — standard regression + routine requires deload → ConsiderDeload`() {
        val result = resolve(
            classification = ProgressionClassification.REGRESSION,
            routineRequiresDeload = true,
            avgWeightKg = 60.0,
        )
        assertEquals(ActionSignal.ConsiderDeload, result)
    }

    // --- Escenario 14: estándar regresión aislada ---
    @Test
    fun `resolve — standard regression isolated → MaintainLoad`() {
        val result = resolve(
            classification = ProgressionClassification.REGRESSION,
            routineRequiresDeload = false,
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

    // --- Escenario 19: bodyweight regresión + routineRequiresDeload → BodyweightSignal (NOT ConsiderDeload) ---
    @Test
    fun `resolve — bodyweight regression + routine deload → BodyweightSignal (not ConsiderDeload)`() {
        val result = resolve(
            classification = ProgressionClassification.REGRESSION,
            isBodyweight = true,
            routineRequiresDeload = true,
            totalReps = 40,
            previousTotalReps = 45,
        )
        assertEquals(ActionSignal.BodyweightSignal(40, -5), result)
    }

    // --- Escenario: isDeload = true → DeloadSession ---
    @Test
    fun `resolve — isDeload true → DeloadSession regardless of classification`() {
        val result = resolve(
            classification = ProgressionClassification.REGRESSION,
            isDeload = true,
        )
        assertEquals(ActionSignal.DeloadSession, result)
    }

    @Test
    fun `resolve — isDeload true with null classification → DeloadSession`() {
        val result = resolve(
            classification = null,
            isDeload = true,
        )
        assertEquals(ActionSignal.DeloadSession, result)
    }

    // --- Helper factory ---
    private fun resolve(
        classification: ProgressionClassification? = ProgressionClassification.MAINTENANCE,
        prescribedLoadKg: Double? = null,
        avgWeightKg: Double = 0.0,
        routineRequiresDeload: Boolean = false,
        isBodyweight: Boolean = false,
        isIsometric: Boolean = false,
        totalReps: Int = 0,
        previousTotalReps: Int? = null,
        setCount: Int = 4,
        isMastered: Boolean = false,
        isDeload: Boolean = false,
        rangeSignal: RangeSignal? = null,
    ): ActionSignal {
        return ActionSignalRule.resolve(
            classification = classification,
            prescribedLoadKg = prescribedLoadKg,
            avgWeightKg = avgWeightKg,
            routineRequiresDeload = routineRequiresDeload,
            isBodyweight = isBodyweight,
            isIsometric = isIsometric,
            totalReps = totalReps,
            previousTotalReps = previousTotalReps,
            setCount = setCount,
            isMastered = isMastered,
            isDeload = isDeload,
            rangeSignal = rangeSignal,
        )
    }

    // ----- HU-46: la señal de rango manda -----

    @Test
    fun `given a range reading, when resolving, then it prevails over the first session`() {
        val result = resolve(classification = null, rangeSignal = RangeSignal.Progressing(1))
        assertEquals(ActionSignal.TopOfRange(1), result)
    }

    @Test
    fun `given an increase by range, when resolving, then the target is the prescribed load`() {
        val result = resolve(
            classification = ProgressionClassification.MAINTENANCE,
            prescribedLoadKg = 62.5,
            avgWeightKg = 60.0,
            rangeSignal = RangeSignal.Increase(4),
        )
        assertEquals(ActionSignal.IncreaseLoad(62.5, 4), result)
    }

    @Test
    fun `given a deload session, when resolving, then the range signal is not shown`() {
        // CA-46.18
        val result = resolve(isDeload = true, rangeSignal = RangeSignal.Decrease)
        assertEquals(ActionSignal.DeloadSession, result)
    }

    @Test
    fun `given an isometric exercise, when resolving with a range reading, then the seconds signal is kept`() {
        // CA-46.16, QA-28: los isométricos miden resistencia y conservan su progresión por segundos
        val result = resolve(
            classification = ProgressionClassification.POSITIVE_PROGRESSION,
            isIsometric = true,
            totalReps = 90,
            setCount = 3,
            rangeSignal = RangeSignal.Decrease,
        )
        assertEquals(ActionSignal.IsometricSignal(3, 30), result)
    }
}
