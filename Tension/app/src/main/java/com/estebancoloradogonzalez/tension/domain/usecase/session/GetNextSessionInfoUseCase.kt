package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.model.NextSession
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNextSessionInfoUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {

    operator fun invoke(): Flow<NextSession?> {
        return sessionRepository.getNextSessionInfo()
    }
}
