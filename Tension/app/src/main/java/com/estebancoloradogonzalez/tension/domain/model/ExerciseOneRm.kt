package com.estebancoloradogonzalez.tension.domain.model

/** One implement of an exercise, with the record of that pair. */
data class OneRmEquipment(
    val equipmentTypeId: Long,
    val equipmentTypeName: String,
    val oneRmKg: Double,
)

/**
 * One exercise with every implement it has a record for — the unit of composition of the 1RM
 * screen, one card per exercise.
 *
 * [equipment] carries **only the implements that have a record**, never the ones the exercise
 * admits: an implement trained without a qualifying set is not shown at all, not with a zero
 * and not with a dash (CA-42.07).
 */
data class ExerciseOneRm(
    val exerciseId: Long,
    val exerciseName: String,
    val equipment: List<OneRmEquipment>,
) {
    /**
     * Whether the card asks the executant to choose.
     *
     * Resolved here and not in the Composable so that the screen never counts again: a single
     * trained implement shows its value directly, as a plain label (CA-42.02).
     */
    val isSelectable: Boolean get() = equipment.size > 1
}
