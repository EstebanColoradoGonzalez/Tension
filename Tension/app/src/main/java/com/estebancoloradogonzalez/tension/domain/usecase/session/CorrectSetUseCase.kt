package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.model.WeightUnit
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import com.estebancoloradogonzalez.tension.domain.rules.SetValuesRule
import javax.inject.Inject

/**
 * Corrects the last registered set of the session in progress (HU-47, CA-47.04).
 *
 * The values are validated with [SetValuesRule], the same rule the registration applies
 * (CA-47.06). [weightKg] is canonical: the caller converts from the capture unit first,
 * exactly as when registering. [equipmentTypeName] is the implement the set was registered
 * with — it is not correctable, and it is only read for the `Peso Añadido` load rule.
 *
 * Whether [setId] is still inside the correction window is **not** decided here: the
 * repository re-evaluates it inside the transaction that writes, as `HU-43` does with the
 * withdrawal verdict. Deciding it out here would leave a gap between the check and the
 * write in which another set could be registered.
 */
class CorrectSetUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(
        sessionExerciseId: Long,
        setId: Long,
        weightKg: Double,
        reps: Int,
        rir: Int,
        captureUnit: WeightUnit,
        equipmentTypeName: String?,
    ) {
        SetValuesRule.requireValid(weightKg, reps, rir, equipmentTypeName)
        sessionRepository.correctSet(
            sessionExerciseId = sessionExerciseId,
            setId = setId,
            weightKg = weightKg,
            reps = reps,
            rir = rir,
            captureUnit = captureUnit,
        )
    }
}
