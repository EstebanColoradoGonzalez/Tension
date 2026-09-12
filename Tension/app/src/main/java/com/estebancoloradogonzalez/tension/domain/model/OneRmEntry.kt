package com.estebancoloradogonzalez.tension.domain.model

/**
 * One (exercise, equipment) pair with the record it holds.
 *
 * The flat row the data layer emits, already ordered — alphabetically by exercise and then by
 * the declared position of the implement in the catalog — before it is grouped into the cards
 * of [ExerciseOneRm].
 */
data class OneRmEntry(
    val exerciseId: Long,
    val exerciseName: String,
    val equipmentTypeId: Long,
    val equipmentTypeName: String,
    val oneRmKg: Double,
)
