package com.estebancoloradogonzalez.tension.domain.usecase.catalog

import com.estebancoloradogonzalez.tension.domain.model.EquipmentType
import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Implementos que el ejercicio admite **hoy**, con su identificador y en orden de catálogo.
 *
 * Los consume el selector de equipamiento sugerido del plan, que solo puede ofrecer estos
 * (CA-41.08). Se resuelve al abrir el diálogo y no se cachea en la lista: el ejercicio
 * pudo ganar o perder opciones desde que la pantalla se pintó.
 */
class GetExerciseEquipmentOptionsUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) {
    operator fun invoke(exerciseId: Long): Flow<List<EquipmentType>> =
        combine(
            exerciseRepository.getEquipmentIdsOfExercise(exerciseId),
            exerciseRepository.getAllEquipmentTypes(),
        ) { admittedIds, allTypes ->
            // Se filtra el catálogo en vez de consultar por ids para conservar el orden
            // declarado, que `IN (...)` no garantiza.
            allTypes.filter { it.id in admittedIds }
        }
}
