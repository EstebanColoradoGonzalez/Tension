package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import com.estebancoloradogonzalez.tension.domain.repository.WeekDayRepository
import javax.inject.Inject

/**
 * Reasigna temporalmente la rutina de hoy.
 *
 * La reasignación solo está disponible **antes** de iniciar la sesión: una vez iniciada, la
 * rutina queda fijada. La interfaz ya lo garantiza por construcción —la acción vive en la
 * tarjeta de sesión propuesta, que no se compone con una sesión en curso—, y esta validación
 * es la de la ruta de datos.
 */
class SetTemporaryRoutineUseCase @Inject constructor(
    private val weekDayRepository: WeekDayRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(routineId: Long) {
        if (sessionRepository.hasActiveSession()) {
            throw IllegalStateException("Cannot reassign the routine of a session already started")
        }
        weekDayRepository.setTodayOverride(routineId)
    }
}
