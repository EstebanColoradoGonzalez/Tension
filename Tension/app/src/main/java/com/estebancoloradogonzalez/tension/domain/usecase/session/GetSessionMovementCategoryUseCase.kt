package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.model.MovementCategory
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import javax.inject.Inject

/**
 * Categoría del día: la de la rutina de la sesión, también si esa rutina llegó por una
 * reasignación temporal (HU-46, CA-46.09).
 */
class GetSessionMovementCategoryUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(sessionId: Long): MovementCategory? =
        sessionRepository.getSessionMovementCategory(sessionId)
}
