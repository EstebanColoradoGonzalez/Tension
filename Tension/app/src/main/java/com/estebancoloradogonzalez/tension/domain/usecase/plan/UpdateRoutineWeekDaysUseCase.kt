package com.estebancoloradogonzalez.tension.domain.usecase.plan

import com.estebancoloradogonzalez.tension.domain.model.WeekDay
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import com.estebancoloradogonzalez.tension.domain.repository.WeekDayRepository
import javax.inject.Inject

/**
 * Fija los días de la semana que ejecutan una rutina.
 *
 * Es la edición **permanente** de la relación día → rutina, distinta de la reasignación
 * temporal de una sesión: aquí el plan sí cambia, y el cambio persiste.
 *
 * Una rutina puede ocupar varios días. Un día ocupa una sola rutina, de modo que asignar un
 * día que pertenecía a otra rutina lo mueve; el día liberado queda de descanso hasta que
 * alguien lo reclame.
 *
 * Se rechaza durante una descarga activa, como el resto de la gestión de rutinas. La razón
 * aquí es concreta: la descarga cierra cuando se han ejecutado tantas sesiones como versiones
 * congeladas, y dejar sin días a una rutina congelada haría que su sesión no se propusiera
 * nunca — el ciclo quedaría abierto de forma indefinida.
 */
class UpdateRoutineWeekDaysUseCase @Inject constructor(
    private val weekDayRepository: WeekDayRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(routineId: Long, weekDays: Set<WeekDay>) {
        require(!sessionRepository.hasActiveDeload()) {
            "No se pueden cambiar los días de una rutina durante una descarga activa"
        }
        weekDayRepository.setRoutineWeekDays(routineId, weekDays)
    }
}
