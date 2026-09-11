package com.estebancoloradogonzalez.tension.domain.usecase.plan

import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import com.estebancoloradogonzalez.tension.domain.repository.PlanRepository
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AssignExerciseToVersionUseCase @Inject constructor(
    private val planRepository: PlanRepository,
    private val sessionRepository: SessionRepository,
    private val exerciseRepository: ExerciseRepository,
) {
    suspend operator fun invoke(
        routineVersionId: Long,
        exerciseId: Long,
        sets: Int,
        reps: String,
        suggestedEquipmentTypeId: Long,
    ) {
        require(!sessionRepository.hasActiveDeload()) {
            "No se puede modificar el plan durante una descarga activa"
        }
        require(!sessionRepository.hasActiveSessionForVersion(routineVersionId)) {
            "No se puede modificar el plan mientras hay una sesión activa de esta versión"
        }
        require(sets > 0) { "Sets must be greater than 0" }
        require(reps in VALID_REPS) { "Invalid reps value: $reps" }
        // La sugerencia es obligatoria y debe estar entre las opciones del ejercicio: no
        // se persiste la asignación sin ella (CA-41.08).
        val admitted = exerciseRepository.getEquipmentIdsOfExercise(exerciseId).first()
        require(suggestedEquipmentTypeId in admitted) {
            "Suggested equipment must be one of the options the exercise admits"
        }
        planRepository.assignExercise(
            routineVersionId = routineVersionId,
            exerciseId = exerciseId,
            sets = sets,
            reps = reps,
            suggestedEquipmentTypeId = suggestedEquipmentTypeId,
        )
    }

    companion object {
        private val VALID_REPS = setOf("8-12", "TO_TECHNICAL_FAILURE", "30-45_SEC")
    }
}
