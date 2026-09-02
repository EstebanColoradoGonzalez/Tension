package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.model.WeightUnit
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import com.estebancoloradogonzalez.tension.domain.util.WeightConverter
import javax.inject.Inject

class RegisterSetUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    /**
     * Registers a set. [weightKg] is always the canonical value: the caller converts
     * from the capture unit before invoking, so the range is checked on kilograms.
     */
    suspend operator fun invoke(
        sessionExerciseId: Long,
        weightKg: Double,
        reps: Int,
        rir: Int,
        captureUnit: WeightUnit = WeightUnit.KG,
    ) {
        require(weightKg >= 0) { "Weight must be >= 0" }
        require(weightKg <= WeightConverter.MAX_WEIGHT_KG) { "Weight must be <= 500 kg" }
        require(reps >= 1) { "Reps must be >= 1" }
        require(rir in 0..2) { "RIR must be between 0 and 2" }
        sessionRepository.registerSet(sessionExerciseId, weightKg, reps, rir, captureUnit)
    }
}
