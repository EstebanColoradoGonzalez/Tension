package com.estebancoloradogonzalez.tension.domain.usecase.catalog

import com.estebancoloradogonzalez.tension.domain.model.ProgressionDifficulty
import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import javax.inject.Inject

class UpdateExerciseProgressionDifficultyUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) {
    suspend operator fun invoke(exerciseId: Long, difficulty: ProgressionDifficulty) {
        exerciseRepository.updateProgressionDifficulty(exerciseId, difficulty)
    }
}
