package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import javax.inject.Inject

/** Revierte la omisión de hoy: el día vuelve a proponer su sesión. */
class UndoSkipTodayUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke() {
        sessionRepository.undoSkipToday()
    }
}
