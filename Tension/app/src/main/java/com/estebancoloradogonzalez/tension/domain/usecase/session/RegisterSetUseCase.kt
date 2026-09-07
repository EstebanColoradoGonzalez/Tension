package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.model.WeightUnit
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import com.estebancoloradogonzalez.tension.domain.rules.ExternalLoadRule
import com.estebancoloradogonzalez.tension.domain.util.WeightConverter
import javax.inject.Inject

class RegisterSetUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    /**
     * Registers a set. [weightKg] is always the canonical value: the caller converts
     * from the capture unit before invoking, so the range is checked on kilograms.
     *
     * [equipmentTypeId] is mandatory — the implement is what the set exists to record
     * (CA-39.04) — and [equipmentTypeName] is only read to enforce the one load rule the
     * name carries: `Peso Añadido` means external ballast, and ballast of zero is not
     * ballast (CA-39.05). Everything else about the implement is resolved in the
     * repository, which owns the catalog.
     */
    suspend operator fun invoke(
        sessionExerciseId: Long,
        weightKg: Double,
        reps: Int,
        rir: Int,
        captureUnit: WeightUnit = WeightUnit.KG,
        equipmentTypeId: Long,
        equipmentTypeName: String? = null,
    ) {
        require(weightKg >= 0) { "Weight must be >= 0" }
        require(weightKg <= WeightConverter.MAX_WEIGHT_KG) { "Weight must be <= 500 kg" }
        require(reps >= 1) { "Reps must be >= 1" }
        require(rir in 0..2) { "RIR must be between 0 and 2" }
        require(equipmentTypeId > 0) { "Equipment type is required" }
        if (ExternalLoadRule.requiresPositiveLoad(equipmentTypeName)) {
            require(weightKg > 0) { "Added weight must be > 0" }
        }
        sessionRepository.registerSet(
            sessionExerciseId,
            weightKg,
            reps,
            rir,
            captureUnit,
            equipmentTypeId,
        )
    }
}
