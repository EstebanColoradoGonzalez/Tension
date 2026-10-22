package com.estebancoloradogonzalez.tension.domain.usecase.catalog

import com.estebancoloradogonzalez.tension.domain.model.MovementCategory
import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import com.estebancoloradogonzalez.tension.domain.rules.MovementCategoryRule
import javax.inject.Inject

/**
 * Cambia la categoría de movimiento de un ejercicio desde su ficha (HU-46, CA-46.14).
 *
 * Un ejercicio **sin** asignaciones en el plan cambia libremente. Uno asignado —como puesto
 * o como alternativa— a rutinas de otra categoría no cambia: el plan dejaría de respetar su
 * propia división (CA-46.12), y el cambio se rechaza nombrando esas rutinas.
 *
 * Devuelve el motivo en lugar de lanzarlo, igual que `RemoveExerciseEquipmentUseCase`: la
 * pantalla lo presenta con nombres, y un «no se puede» sin decir dónde obliga a buscar.
 */
class UpdateExerciseMovementCategoryUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) {

    sealed interface Result {
        data object Updated : Result

        /** Rutinas del plan que lo impiden. Nunca vacía. */
        data class ConflictsWithPlan(val routineNames: List<String>) : Result
    }

    suspend operator fun invoke(exerciseId: Long, category: MovementCategory): Result {
        val conflicts = MovementCategoryRule.conflicts(
            newCategory = category,
            otherSide = exerciseRepository.getRoutinesAssigningExercise(exerciseId),
        )
        if (conflicts.isNotEmpty()) return Result.ConflictsWithPlan(conflicts)
        exerciseRepository.updateMovementCategory(exerciseId, category)
        return Result.Updated
    }
}
