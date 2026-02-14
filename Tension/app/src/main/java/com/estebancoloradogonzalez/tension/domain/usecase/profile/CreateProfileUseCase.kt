package com.estebancoloradogonzalez.tension.domain.usecase.profile

import com.estebancoloradogonzalez.tension.domain.model.ExperienceLevel
import com.estebancoloradogonzalez.tension.domain.repository.ProfileRepository
import javax.inject.Inject

class CreateProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(
        weightKg: Double,
        heightM: Double,
        experienceLevel: ExperienceLevel,
    ): Result<Unit> {
        if (weightKg <= 0) {
            return Result.failure(IllegalArgumentException("Weight must be > 0"))
        }
        if (heightM <= 0) {
            return Result.failure(IllegalArgumentException("Height must be > 0"))
        }
        return try {
            profileRepository.createProfile(weightKg, heightM, experienceLevel)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
