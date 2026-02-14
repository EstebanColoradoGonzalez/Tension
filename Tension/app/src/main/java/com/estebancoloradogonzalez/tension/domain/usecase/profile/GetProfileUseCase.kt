package com.estebancoloradogonzalez.tension.domain.usecase.profile

import com.estebancoloradogonzalez.tension.domain.model.Profile
import com.estebancoloradogonzalez.tension.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
) {
    operator fun invoke(): Flow<Profile?> {
        return profileRepository.getProfile()
    }
}
