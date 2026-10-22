package com.estebancoloradogonzalez.tension.domain.usecase.routine

import com.estebancoloradogonzalez.tension.domain.model.MovementCategory
import com.estebancoloradogonzalez.tension.domain.repository.RoutineRepository
import com.estebancoloradogonzalez.tension.domain.rules.MovementCategoryRule
import javax.inject.Inject

/**
 * Cambia la categoría de una rutina (HU-46, CA-46.14).
 *
 * Una rutina con ejercicios asignados de otra categoría —en cualquiera de sus versiones,
 * como puesto o como alternativa— no cambia: el rechazo nombra esos ejercicios. Una rutina
 * sin ejercicios en conflicto cambia libremente.
 */
class UpdateRoutineMovementCategoryUseCase @Inject constructor(
    private val routineRepository: RoutineRepository,
) {

    sealed interface Result {
        data object Updated : Result

        /** Ejercicios asignados que lo impiden. Nunca vacía. */
        data class ConflictsWithPlan(val exerciseNames: List<String>) : Result
    }

    suspend operator fun invoke(routineId: Long, category: MovementCategory): Result {
        val conflicts = MovementCategoryRule.conflicts(
            newCategory = category,
            otherSide = routineRepository.getExercisesAssignedToRoutine(routineId),
        )
        if (conflicts.isNotEmpty()) return Result.ConflictsWithPlan(conflicts)
        routineRepository.updateMovementCategory(routineId, category)
        return Result.Updated
    }
}
