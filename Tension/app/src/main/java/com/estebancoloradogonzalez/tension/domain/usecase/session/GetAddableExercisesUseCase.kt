package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.model.AddableExercise
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** El Diccionario visto desde la sesión, con los ya presentes marcados (CA-43.01). */
class GetAddableExercisesUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(sessionId: Long): Flow<List<AddableExercise>> =
        sessionRepository.getAddableExercises(sessionId)
}
