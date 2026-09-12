package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import javax.inject.Inject

/**
 * Retira un ejercicio de la sesión en curso (CA-43.04, CA-43.05, CA-43.06).
 *
 * El veredicto lo evalúa el repositorio dentro de su transacción, con los conteos releídos:
 * decidirlo aquí, fuera de ella, dejaría una ventana entre la comprobación y el borrado en
 * la que la invariante podría romperse.
 */
class WithdrawFromSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(sessionExerciseId: Long) {
        sessionRepository.withdrawFromSession(sessionExerciseId)
    }
}
