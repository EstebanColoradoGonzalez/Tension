package com.estebancoloradogonzalez.tension.domain.usecase.catalog

import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Ids de los implementos que el ejercicio admite, en orden de catálogo. */
class GetExerciseEquipmentIdsUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) {
    operator fun invoke(exerciseId: Long): Flow<List<Long>> =
        exerciseRepository.getEquipmentIdsOfExercise(exerciseId)
}
