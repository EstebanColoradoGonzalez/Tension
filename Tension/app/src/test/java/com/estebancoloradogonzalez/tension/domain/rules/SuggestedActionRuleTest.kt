package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.SuggestedActionKind
import com.estebancoloradogonzalez.tension.domain.model.SuggestedActionTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestedActionRuleTest {

    private fun context(
        alertType: String,
        level: String = "MEDIUM_ALERT",
        exerciseId: Long? = 7L,
        hasSlotAlternative: Boolean = false,
        sessionsWithoutProgression: Int = 0,
        plateauThreshold: Int = 5,
        isRirLow: Boolean = false,
    ) = SuggestedActionContext(
        alertType = alertType,
        level = level,
        exerciseId = exerciseId,
        hasSlotAlternative = hasSlotAlternative,
        sessionsWithoutProgression = sessionsWithoutProgression,
        plateauThreshold = plateauThreshold,
        isRirLow = isRirLow,
    )

    private val allTypes = listOf(
        "PLATEAU",
        "LOW_PROGRESSION_RATE",
        "RIR_OUT_OF_RANGE",
        "LOW_ADHERENCE",
        "TONNAGE_DROP",
        "ROUTINE_INACTIVITY",
        "ROUTINE_REQUIRES_DELOAD",
    )

    @Test
    fun `given any of the seven families, when resolved, then all of them propose an action`() {
        allTypes.forEach { type ->
            val (kind, _) = SuggestedActionRule.resolve(context(type))
            assertNotNull("$type produced no action", kind)
        }
    }

    @Test
    fun `given an unknown alert type, when resolved, then it still proposes an action`() {
        val (kind, target) = SuggestedActionRule.resolve(context("SOMETHING_ELSE"))

        assertEquals(SuggestedActionKind.REVIEW_TECHNIQUE, kind)
        assertNull(target)
    }

    @Test
    fun `given a plateau just declared, when resolved, then it proposes extending reps`() {
        val (kind, target) = SuggestedActionRule.resolve(
            context("PLATEAU", sessionsWithoutProgression = 5, plateauThreshold = 5),
        )

        assertEquals(SuggestedActionKind.EXTEND_REPS_BEFORE_LOAD, kind)
        assertEquals(SuggestedActionTarget.ExerciseHistory(7L), target)
    }

    @Test
    fun `given a long plateau with an alternative in the slot, when resolved, then it proposes the swap`() {
        val (kind, target) = SuggestedActionRule.resolve(
            context(
                "PLATEAU",
                hasSlotAlternative = true,
                sessionsWithoutProgression = 9,
                plateauThreshold = 5,
            ),
        )

        assertEquals(SuggestedActionKind.SWITCH_TO_SLOT_ALTERNATIVE, kind)
        assertEquals(SuggestedActionTarget.TrainingPlan, target)
    }

    @Test
    fun `given a long plateau without an alternative, when resolved, then the swap is never proposed`() {
        val (kind, target) = SuggestedActionRule.resolve(
            context(
                "PLATEAU",
                hasSlotAlternative = false,
                sessionsWithoutProgression = 9,
                plateauThreshold = 5,
            ),
        )

        assertEquals(SuggestedActionKind.ROTATE_ROUTINE_VERSION, kind)
        assertEquals(SuggestedActionTarget.TrainingPlan, target)
    }

    @Test
    fun `given a progression crisis without an alternative, when resolved, then it proposes raising load`() {
        val (kind, target) = SuggestedActionRule.resolve(
            context("LOW_PROGRESSION_RATE", level = "CRISIS", hasSlotAlternative = false),
        )

        assertEquals(SuggestedActionKind.INCREASE_LOAD_SLIGHTLY, kind)
        assertEquals(SuggestedActionTarget.ExerciseHistory(7L), target)
    }

    @Test
    fun `given a progression crisis with an alternative, when resolved, then it proposes the swap`() {
        val (kind, _) = SuggestedActionRule.resolve(
            context("LOW_PROGRESSION_RATE", level = "CRISIS", hasSlotAlternative = true),
        )

        assertEquals(SuggestedActionKind.SWITCH_TO_SLOT_ALTERNATIVE, kind)
    }

    @Test
    fun `given a low rir alert, when resolved, then the action is text only`() {
        val (kind, target) = SuggestedActionRule.resolve(
            context("RIR_OUT_OF_RANGE", exerciseId = null, isRirLow = true),
        )

        assertEquals(SuggestedActionKind.LEAVE_REPS_IN_RESERVE, kind)
        assertNull(target)
    }

    @Test
    fun `given a high rir alert, when resolved, then it points at the plan`() {
        val (kind, target) = SuggestedActionRule.resolve(
            context("RIR_OUT_OF_RANGE", exerciseId = null, isRirLow = false),
        )

        assertEquals(SuggestedActionKind.INCREASE_LOAD_FOR_STIMULUS, kind)
        assertEquals(SuggestedActionTarget.TrainingPlan, target)
    }

    @Test
    fun `given a low adherence alert, when resolved, then the action is text only`() {
        val (kind, target) = SuggestedActionRule.resolve(
            context("LOW_ADHERENCE", exerciseId = null),
        )

        assertEquals(SuggestedActionKind.INCREASE_WEEKLY_FREQUENCY, kind)
        assertNull(target)
    }

    @Test
    fun `given a deload alert, when resolved, then it points at deload management`() {
        val (kind, target) = SuggestedActionRule.resolve(
            context("ROUTINE_REQUIRES_DELOAD", exerciseId = null),
        )

        assertEquals(SuggestedActionKind.START_DELOAD, kind)
        assertEquals(SuggestedActionTarget.DeloadManagement, target)
    }

    @Test
    fun `given an inactivity alert, when resolved, then it points at the plan`() {
        val (kind, target) = SuggestedActionRule.resolve(
            context("ROUTINE_INACTIVITY", exerciseId = null),
        )

        assertEquals(SuggestedActionKind.RESUME_MODULE, kind)
        assertEquals(SuggestedActionTarget.TrainingPlan, target)
    }

    @Test
    fun `given a tonnage alert, when resolved, then it points at the plan`() {
        val (kind, target) = SuggestedActionRule.resolve(
            context("TONNAGE_DROP", exerciseId = null),
        )

        assertEquals(SuggestedActionKind.REDUCE_VOLUME, kind)
        assertEquals(SuggestedActionTarget.TrainingPlan, target)
    }

    @Test
    fun `given every family, when resolved, then no destination outside the defined ones appears`() {
        allTypes.forEach { type ->
            val (_, target) = SuggestedActionRule.resolve(context(type))
            val isKnown = target == null ||
                target is SuggestedActionTarget.ExerciseHistory ||
                target == SuggestedActionTarget.DeloadManagement ||
                target == SuggestedActionTarget.TrainingPlan
            assertTrue("$type produced an unknown destination", isKnown)
        }
    }
}
