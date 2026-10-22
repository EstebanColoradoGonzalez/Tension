package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.model.WeightUnit
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import com.estebancoloradogonzalez.tension.domain.rules.SetRangeNotice
import com.estebancoloradogonzalez.tension.domain.rules.SetValuesRule
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
     *
     * Returns the live notice of the set against the 8–12 range (HU-46, CA-46.05), or
     * `null`. It never blocks the registration nor alters what was registered.
     */
    suspend operator fun invoke(
        sessionExerciseId: Long,
        weightKg: Double,
        reps: Int,
        rir: Int,
        captureUnit: WeightUnit = WeightUnit.KG,
        equipmentTypeId: Long,
        equipmentTypeName: String? = null,
    ): SetRangeNotice? {
        // Shared with the correction of the last set (CA-47.06): one rule, not two copies.
        SetValuesRule.requireValid(weightKg, reps, rir, equipmentTypeName)
        require(equipmentTypeId > 0) { "Equipment type is required" }
        return sessionRepository.registerSet(
            sessionExerciseId,
            weightKg,
            reps,
            rir,
            captureUnit,
            equipmentTypeId,
        )
    }
}
