package com.estebancoloradogonzalez.tension.domain.usecase.deload

import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import javax.inject.Inject

class ActivateDeloadUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {

    suspend operator fun invoke() {
        sessionRepository.activateDeload()
    }
}
