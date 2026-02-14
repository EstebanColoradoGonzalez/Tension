package com.estebancoloradogonzalez.tension.domain.usecase.catalog

import com.estebancoloradogonzalez.tension.domain.model.Exercise
import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetExerciseDetailUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) {
    operator fun invoke(exerciseId: Long): Flow<Exercise?> =
        exerciseRepository.getExerciseById(exerciseId)
}
