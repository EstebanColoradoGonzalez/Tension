package com.estebancoloradogonzalez.tension.domain.usecase.catalog

import com.estebancoloradogonzalez.tension.domain.model.ProgressionDifficulty
import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import javax.inject.Inject

class CreateExerciseUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) {
    suspend operator fun invoke(
        name: String,
        equipmentTypeIds: List<Long>,
        muscleZoneIds: List<Long>,
        isBodyweight: Boolean,
        isIsometric: Boolean,
        isToTechnicalFailure: Boolean,
        mediaResource: String?,
        progressionDifficulty: ProgressionDifficulty = ProgressionDifficulty.MEDIUM,
    ): Long {
        require(name.isNotBlank()) { "Exercise name must not be blank" }
        require(muscleZoneIds.isNotEmpty()) { "At least one muscle zone must be selected" }
        // El equipamiento es obligatorio y multivalor: ningún ejercicio puede existir con
        // la lista vacía (CA-39.02, CA-39.09).
        require(equipmentTypeIds.isNotEmpty()) { "At least one equipment type must be selected" }
        // El nombre es único por sí solo: el implemento dejó de formar clave compuesta con
        // él, porque el mismo movimiento con distintos implementos es un solo ejercicio.
        require(
            !exerciseRepository.exerciseExistsByName(name.trim()),
        ) { "An exercise with this name already exists" }

        return exerciseRepository.createExercise(
            name = name.trim(),
            equipmentTypeIds = equipmentTypeIds.distinct(),
            muscleZoneIds = muscleZoneIds,
            isBodyweight = isBodyweight,
            isIsometric = isIsometric,
            isToTechnicalFailure = isToTechnicalFailure,
            mediaResource = mediaResource,
            progressionDifficulty = progressionDifficulty,
        )
    }
}
