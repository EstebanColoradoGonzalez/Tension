package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.model.PrefilledLoad
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import javax.inject.Inject

/**
 * Resuelve el peso precargado y la unidad de captura al cambiar de implemento sin salir del
 * formulario de registro de serie (CA-40.03).
 *
 * La precedencia es la de siempre —prescripción activa, serie anterior en esta sesión,
 * última serie de la sesión cerrada más reciente, campo vacío—, pero resuelta sobre el par
 * `(ejercicio, equipamiento)`. Si el par nuevo no tiene historial el campo queda vacío:
 * heredar el peso del implemento anterior sería sugerir la carga de otra cosa.
 */
class GetPrefilledLoadForEquipmentUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(
        sessionExerciseId: Long,
        equipmentTypeId: Long,
    ): PrefilledLoad? = sessionRepository.getPrefilledLoadForPair(
        sessionExerciseId,
        equipmentTypeId,
    )
}
