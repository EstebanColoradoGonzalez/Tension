package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.model.RegisterSetInfo
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import javax.inject.Inject

class GetRegisterSetInfoUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(sessionExerciseId: Long): RegisterSetInfo? {
        return sessionRepository.getRegisterSetInfo(sessionExerciseId)
    }
}
