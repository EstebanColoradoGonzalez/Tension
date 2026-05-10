package com.estebancoloradogonzalez.tension.domain.usecase.plan

import com.estebancoloradogonzalez.tension.domain.repository.PlanRepository
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import javax.inject.Inject

class AddAlternativeToSlotUseCase @Inject constructor(
    private val planRepository: PlanRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(routineVersionId: Long, slot: Int, exerciseId: Long) {
        require(!sessionRepository.hasActiveDeload()) {
            "No se puede modificar el plan durante una descarga activa"
        }
        require(!sessionRepository.hasActiveSessionForVersion(routineVersionId)) {
            "No se puede modificar el plan mientras hay una sesión activa de esta versión"
        }
        planRepository.addAlternativeToSlot(routineVersionId, slot, exerciseId)
    }
}
