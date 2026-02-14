package com.estebancoloradogonzalez.tension.domain.usecase.catalog

import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import javax.inject.Inject

class UpdateExerciseImageUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) {
    suspend operator fun invoke(exerciseId: Long, mediaResource: String?) {
        exerciseRepository.updateExerciseImage(exerciseId, mediaResource)
    }
}
