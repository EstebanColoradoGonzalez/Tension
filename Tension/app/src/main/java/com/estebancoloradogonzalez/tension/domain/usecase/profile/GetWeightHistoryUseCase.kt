package com.estebancoloradogonzalez.tension.domain.usecase.profile

import com.estebancoloradogonzalez.tension.domain.model.WeightRecord
import com.estebancoloradogonzalez.tension.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetWeightHistoryUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
) {
    operator fun invoke(): Flow<List<WeightRecord>> =
        profileRepository.getAllWeightRecords()
}
