package com.estebancoloradogonzalez.tension.domain.usecase.profile

import com.estebancoloradogonzalez.tension.domain.repository.ProfileRepository
import javax.inject.Inject

class UpdateWeightUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(weightKg: Double): Result<Unit> {
        if (weightKg <= 0) {
            return Result.failure(IllegalArgumentException("Weight must be > 0"))
        }
        return try {
            profileRepository.updateWeight(weightKg)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
