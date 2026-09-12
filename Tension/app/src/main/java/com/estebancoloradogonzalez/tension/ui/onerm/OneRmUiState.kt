package com.estebancoloradogonzalez.tension.ui.onerm

import com.estebancoloradogonzalez.tension.domain.model.ExerciseOneRm
import com.estebancoloradogonzalez.tension.domain.model.OneRmEquipment

data class OneRmUiState(
    val isLoading: Boolean = true,
    val exercises: List<ExerciseOneRm> = emptyList(),
    /** Active implement per exercise. Absent means «the first one», never «none». */
    val selection: Map<Long, Long> = emptyMap(),
) {
    /**
     * Distinguished from the loading state on purpose: an empty list while still loading is
     * not an empty state, and showing the explanatory message during the first frame would
     * tell the executant that nothing has ever qualified before knowing whether that is true.
     */
    val isEmpty: Boolean get() = !isLoading && exercises.isEmpty()

    /**
     * The implement whose value the card shows.
     *
     * Falls back to the first one — which is what «the first chip is active on opening»
     * means — and also covers the selection surviving a reload in which that implement is no
     * longer there. It cannot return null: a card exists only because it has at least one
     * record.
     */
    fun selectedEquipment(exercise: ExerciseOneRm): OneRmEquipment {
        val selectedId = selection[exercise.exerciseId]
        return exercise.equipment.firstOrNull { it.equipmentTypeId == selectedId }
            ?: exercise.equipment.first()
    }
}
