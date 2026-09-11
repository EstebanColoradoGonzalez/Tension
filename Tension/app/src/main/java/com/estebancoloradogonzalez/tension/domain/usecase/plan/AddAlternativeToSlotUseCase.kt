package com.estebancoloradogonzalez.tension.domain.usecase.plan

import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import com.estebancoloradogonzalez.tension.domain.repository.PlanRepository
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AddAlternativeToSlotUseCase @Inject constructor(
    private val planRepository: PlanRepository,
    private val sessionRepository: SessionRepository,
    private val exerciseRepository: ExerciseRepository,
) {
    suspend operator fun invoke(
        routineVersionId: Long,
        slot: Int,
        exerciseId: Long,
        suggestedEquipmentTypeId: Long,
    ) {
        require(!sessionRepository.hasActiveDeload()) {
            "No se puede modificar el plan durante una descarga activa"
        }
        require(!sessionRepository.hasActiveSessionForVersion(routineVersionId)) {
            "No se puede modificar el plan mientras hay una sesión activa de esta versión"
        }
        // La alternativa hereda series y repeticiones del puesto, pero **no** el
        // implemento: cada ejercicio del puesto dual lleva el suyo (CA-41.05).
        val admitted = exerciseRepository.getEquipmentIdsOfExercise(exerciseId).first()
        require(suggestedEquipmentTypeId in admitted) {
            "Suggested equipment must be one of the options the exercise admits"
        }
        planRepository.addAlternativeToSlot(
            routineVersionId = routineVersionId,
            slot = slot,
            exerciseId = exerciseId,
            suggestedEquipmentTypeId = suggestedEquipmentTypeId,
        )
    }
}
