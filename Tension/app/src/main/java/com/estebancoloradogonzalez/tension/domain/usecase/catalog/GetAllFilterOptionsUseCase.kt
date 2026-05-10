package com.estebancoloradogonzalez.tension.domain.usecase.catalog

import com.estebancoloradogonzalez.tension.domain.model.FilterOptions
import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetAllFilterOptionsUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) {
    operator fun invoke(): Flow<FilterOptions> =
        combine(
            exerciseRepository.getAllEquipmentTypes(),
            exerciseRepository.getAllMuscleZones(),
        ) { equipmentTypes, muscleZones ->
            FilterOptions(
                equipmentTypes = equipmentTypes,
                muscleZones = muscleZones,
            )
        }
}
