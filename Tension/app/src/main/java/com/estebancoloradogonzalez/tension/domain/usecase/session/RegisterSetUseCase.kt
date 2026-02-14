package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import javax.inject.Inject

class RegisterSetUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(
        sessionExerciseId: Long,
        weightKg: Double,
        reps: Int,
        rir: Int,
    ) {
        require(weightKg >= 0) { "Weight must be >= 0" }
        require(reps >= 1) { "Reps must be >= 1" }
        require(rir in 0..5) { "RIR must be between 0 and 5" }
        sessionRepository.registerSet(sessionExerciseId, weightKg, reps, rir)
    }
}
