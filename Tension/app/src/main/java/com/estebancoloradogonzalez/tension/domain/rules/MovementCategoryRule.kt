package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.MovementCategory

/** Something of the plan that carries a movement category: a routine or an exercise. */
data class CategorizedName(
    val name: String,
    val category: MovementCategory?,
)

/**
 * Coherence between the category of an exercise and that of the routine it belongs to
 * (HU-46). Pure: whoever calls reads the plan, the rule decides over that reading.
 *
 * Categories are exclusive — an exercise is not push and pull at once, and legs are fully
 * isolated — so compatibility is **strict equality** (RN-9, RN-12). It governs four doors:
 * assigning an exercise to a routine or as an alternative of a slot (CA-46.12), adding an
 * exercise to the session of the day (CA-46.10), and changing the category of an exercise or
 * of a routine that the plan already ties together (CA-46.14).
 */
object MovementCategoryRule {

    /**
     * Whether an exercise of [exerciseCategory] may sit in a routine of [routineCategory].
     * An unknown category on either side is never compatible: assigning blind is exactly
     * what this story removes.
     */
    fun isAssignable(
        exerciseCategory: MovementCategory?,
        routineCategory: MovementCategory?,
    ): Boolean = exerciseCategory != null && exerciseCategory == routineCategory

    /**
     * Names on the other side of the plan that a change to [newCategory] would contradict:
     * the routines an exercise is assigned to, or the exercises a routine has assigned.
     * Empty means the change is free (CA-46.14). Names come back distinct and in the given
     * order, because the message lists them and a repeated name reads like a second conflict.
     */
    fun conflicts(
        newCategory: MovementCategory,
        otherSide: List<CategorizedName>,
    ): List<String> = otherSide
        .filter { !isAssignableTo(newCategory, it.category) }
        .map { it.name }
        .distinct()

    private fun isAssignableTo(category: MovementCategory, other: MovementCategory?): Boolean =
        other == category
}
