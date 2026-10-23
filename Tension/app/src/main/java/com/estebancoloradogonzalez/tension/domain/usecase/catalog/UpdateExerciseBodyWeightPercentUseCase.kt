package com.estebancoloradogonzalez.tension.domain.usecase.catalog

import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import com.estebancoloradogonzalez.tension.domain.rules.RealLoadRule
import javax.inject.Inject

/**
 * Saves the share of body weight an exercise moves (HU-49, CA-49.04).
 *
 * It receives the text as typed and validates it here, in the domain (CA-49.05): only an
 * integer from 0 to 100 is accepted. A decimal, a value out of range or a blank fails without
 * writing, so the exercise keeps its previous percentage and is never left without one. A valid
 * value is saved together with the rebuild of the exercise's 1RM records (CA-49.08).
 */
class UpdateExerciseBodyWeightPercentUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) {
    suspend operator fun invoke(exerciseId: Long, input: String): Result<Int> {
        val percent = RealLoadRule.parsePercent(input)
            ?: return Result.failure(IllegalArgumentException(INVALID_PERCENT_MESSAGE))
        return exerciseRepository.updateBodyWeightPercent(exerciseId, percent).map { percent }
    }

    companion object {
        const val INVALID_PERCENT_MESSAGE = "Body weight percent must be an integer between 0 and 100"
    }
}
