package com.estebancoloradogonzalez.tension.domain.usecase.profile

import com.estebancoloradogonzalez.tension.domain.repository.ProfileRepository
import javax.inject.Inject

class UpdateProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(heightM: Double): Result<Unit> {
        if (heightM <= 0) {
            return Result.failure(IllegalArgumentException("Height must be > 0"))
        }
        return try {
            profileRepository.updateProfile(heightM)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
