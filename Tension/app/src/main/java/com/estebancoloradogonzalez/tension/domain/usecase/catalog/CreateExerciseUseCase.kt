package com.estebancoloradogonzalez.tension.domain.usecase.catalog

import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import javax.inject.Inject

class CreateExerciseUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) {
    suspend operator fun invoke(
        name: String,
        moduleCode: String,
        equipmentTypeId: Long,
        muscleZoneIds: List<Long>,
        isBodyweight: Boolean,
        isIsometric: Boolean,
        isToTechnicalFailure: Boolean,
        mediaResource: String?,
    ): Long {
        require(name.isNotBlank()) { "Exercise name must not be blank" }
        require(muscleZoneIds.isNotEmpty()) { "At least one muscle zone must be selected" }
        require(
            !exerciseRepository.exerciseExistsByNameAndEquipment(name.trim(), equipmentTypeId),
        ) { "An exercise with this name and equipment type already exists" }

        return exerciseRepository.createExercise(
            name = name.trim(),
            moduleCode = moduleCode,
            equipmentTypeId = equipmentTypeId,
            muscleZoneIds = muscleZoneIds,
            isBodyweight = isBodyweight,
            isIsometric = isIsometric,
            isToTechnicalFailure = isToTechnicalFailure,
            mediaResource = mediaResource,
        )
    }
}
