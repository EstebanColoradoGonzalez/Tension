package com.estebancoloradogonzalez.tension.domain.usecase.catalog

import com.estebancoloradogonzalez.tension.domain.model.FilterOptions
import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetFilterOptionsUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) {
    operator fun invoke(): Flow<FilterOptions> =
        combine(
            exerciseRepository.getAllModules(),
            exerciseRepository.getAllEquipmentTypes(),
            exerciseRepository.getAllMuscleZones(),
        ) { modules, equipmentTypes, muscleZones ->
            FilterOptions(
                modules = modules,
                equipmentTypes = equipmentTypes,
                muscleZones = muscleZones,
            )
        }
}
