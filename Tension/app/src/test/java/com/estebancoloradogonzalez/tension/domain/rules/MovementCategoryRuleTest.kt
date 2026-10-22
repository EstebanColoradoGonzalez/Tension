package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.MovementCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** HU-46 — coherence between the category of an exercise and that of its routine. */
class MovementCategoryRuleTest {

    @Test
    fun `given the same category, when assignability is checked, then it is assignable`() {
        assertTrue(MovementCategoryRule.isAssignable(MovementCategory.PULL, MovementCategory.PULL))
    }

    @Test
    fun `given different categories, when assignability is checked, then it is not assignable`() {
        // QA-19, QA-20: Press Militar (Empuje) no entra en una rutina de Jalón
        assertFalse(MovementCategoryRule.isAssignable(MovementCategory.PUSH, MovementCategory.PULL))
        assertFalse(MovementCategoryRule.isAssignable(MovementCategory.LEGS, MovementCategory.PUSH))
    }

    @Test
    fun `given an unknown category, when assignability is checked, then it is never assignable`() {
        assertFalse(MovementCategoryRule.isAssignable(null, MovementCategory.PUSH))
        assertFalse(MovementCategoryRule.isAssignable(MovementCategory.PUSH, null))
        assertFalse(MovementCategoryRule.isAssignable(null, null))
    }

    @Test
    fun `given an exercise assigned to push routines, when it changes to pull, then both routines conflict`() {
        // QA-22: Aperturas, de Empuje, asignada a las dos rutinas Push
        val routines = listOf(
            CategorizedName("Push — Foco Deltoides Lateral y Medio", MovementCategory.PUSH),
            CategorizedName("Push — Foco Tríceps", MovementCategory.PUSH),
        )
        assertEquals(
            listOf("Push — Foco Deltoides Lateral y Medio", "Push — Foco Tríceps"),
            MovementCategoryRule.conflicts(MovementCategory.PULL, routines),
        )
    }

    @Test
    fun `given an exercise without assignments, when it changes category, then nothing conflicts`() {
        // QA-23
        assertTrue(MovementCategoryRule.conflicts(MovementCategory.PULL, emptyList()).isEmpty())
    }

    @Test
    fun `given a routine with legs exercises, when it changes to push, then the exercises conflict once each`() {
        // QA-24
        val exercises = listOf(
            CategorizedName("Aductores", MovementCategory.LEGS),
            CategorizedName("Aductores", MovementCategory.LEGS),
            CategorizedName("Press Militar", MovementCategory.PUSH),
        )
        assertEquals(listOf("Aductores"), MovementCategoryRule.conflicts(MovementCategory.PUSH, exercises))
    }
}
