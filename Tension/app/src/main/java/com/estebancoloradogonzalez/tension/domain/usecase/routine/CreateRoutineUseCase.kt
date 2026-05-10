package com.estebancoloradogonzalez.tension.domain.usecase.routine

import com.estebancoloradogonzalez.tension.domain.repository.RoutineRepository
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CreateRoutineUseCase @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val sessionRepository: SessionRepository,
) {

    suspend operator fun invoke(name: String) {
        val trimmed = name.trim()
        require(trimmed.isNotBlank()) { "El nombre no puede estar vacío" }
        require(trimmed.length <= MAX_NAME_LENGTH) { "El nombre no puede exceder $MAX_NAME_LENGTH caracteres" }
        require(!routineRepository.routineNameExists(trimmed)) { "Ya existe una rutina con ese nombre" }
        val hasActive = sessionRepository.getActiveSession().first() != null
        require(!hasActive) {
            "No se puede crear una rutina mientras hay una sesión activa"
        }
        require(!sessionRepository.hasActiveDeload()) {
            "No se puede crear una rutina durante una descarga activa"
        }
        routineRepository.createRoutine(trimmed)
    }

    companion object {
        const val MAX_NAME_LENGTH = 50
    }
}
