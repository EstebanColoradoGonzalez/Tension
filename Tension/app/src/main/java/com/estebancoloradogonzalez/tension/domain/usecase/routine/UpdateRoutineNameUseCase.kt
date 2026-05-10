package com.estebancoloradogonzalez.tension.domain.usecase.routine

import com.estebancoloradogonzalez.tension.domain.repository.RoutineRepository
import javax.inject.Inject

class UpdateRoutineNameUseCase @Inject constructor(
    private val routineRepository: RoutineRepository,
) {

    suspend operator fun invoke(id: Long, name: String) {
        val trimmed = name.trim()
        require(trimmed.isNotBlank()) { "El nombre no puede estar vacío" }
        require(trimmed.length <= MAX_NAME_LENGTH) { "El nombre no puede exceder $MAX_NAME_LENGTH caracteres" }
        require(!routineRepository.routineNameExistsExcluding(trimmed, id)) {
            "Ya existe una rutina con ese nombre"
        }
        routineRepository.updateRoutineName(id, trimmed)
    }

    companion object {
        const val MAX_NAME_LENGTH = 50
    }
}
