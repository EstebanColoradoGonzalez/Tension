package com.estebancoloradogonzalez.tension.domain.usecase.catalog

import com.estebancoloradogonzalez.tension.domain.repository.ExerciseMuscleZoneIds
import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Zonas del ejercicio partidas por jerarquía, para que el formulario de la ficha nazca con
 * lo que ya está elegido.
 */
class GetExerciseMuscleZoneIdsUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) {
    operator fun invoke(exerciseId: Long): Flow<ExerciseMuscleZoneIds> =
        exerciseRepository.getMuscleZoneIdsOfExercise(exerciseId)
}
