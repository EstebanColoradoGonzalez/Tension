package com.estebancoloradogonzalez.tension.domain.usecase.deload

import com.estebancoloradogonzalez.tension.domain.model.DeloadState
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import javax.inject.Inject

class GetDeloadStateUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {

    suspend operator fun invoke(): DeloadState {
        return sessionRepository.getDeloadState()
    }
}
