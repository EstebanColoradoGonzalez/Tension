package com.estebancoloradogonzalez.tension.domain.usecase.profile

import com.estebancoloradogonzalez.tension.domain.repository.ProfileRepository
import com.estebancoloradogonzalez.tension.domain.rules.PlateauThresholdRule
import javax.inject.Inject

class UpdatePlateauBaseThresholdUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(value: Int): Result<Unit> {
        if (!PlateauThresholdRule.isValidBaseThreshold(value)) {
            return Result.failure(
                IllegalArgumentException(
                    "Plateau base threshold must be between " +
                        "${PlateauThresholdRule.MIN_BASE_THRESHOLD} and " +
                        "${PlateauThresholdRule.MAX_BASE_THRESHOLD}",
                ),
            )
        }
        return try {
            profileRepository.updatePlateauBaseThreshold(value)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
