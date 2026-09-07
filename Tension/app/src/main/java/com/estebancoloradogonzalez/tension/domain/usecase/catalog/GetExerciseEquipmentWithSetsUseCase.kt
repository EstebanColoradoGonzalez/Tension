package com.estebancoloradogonzalez.tension.domain.usecase.catalog

import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import javax.inject.Inject

/**
 * De los implementos que el ejercicio admite, cuáles ya tienen series registradas.
 *
 * Es lo que la ficha marca con candado y lo que hace fallar la retirada (CA-39.10). Se
 * resuelve sobre la lista admitida y no sobre el catálogo entero: preguntar por los quince
 * tipos serían quince consultas para responder por los que el ejercicio ni ofrece.
 */
class GetExerciseEquipmentWithSetsUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) {
    suspend operator fun invoke(exerciseId: Long, equipmentTypeIds: List<Long>): Set<Long> =
        equipmentTypeIds
            .filter { exerciseRepository.countSetsWithEquipment(exerciseId, it) > 0 }
            .toSet()
}
