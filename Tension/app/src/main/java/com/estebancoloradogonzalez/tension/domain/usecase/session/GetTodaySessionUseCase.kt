package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.model.TodaySession
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTodaySessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {

    operator fun invoke(): Flow<TodaySession> {
        return sessionRepository.getTodaySession()
    }
}
