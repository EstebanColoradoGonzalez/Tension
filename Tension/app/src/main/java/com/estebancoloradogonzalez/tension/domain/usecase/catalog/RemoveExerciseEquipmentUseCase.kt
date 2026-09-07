package com.estebancoloradogonzalez.tension.domain.usecase.catalog

import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import javax.inject.Inject

/**
 * Retira un implemento de las opciones que el ejercicio admite.
 *
 * Dos razones lo pueden impedir, y las dos se comprueban antes de escribir:
 *
 * - **Es la última opción** (CA-39.09). El atributo es obligatorio y no admite lista
 *   vacía, igual al crear que al editar.
 * - **Tiene series registradas** (CA-39.10). Una serie es inmutable tras su creación: no
 *   puede quedar apuntando a un equipamiento que el ejercicio dejó de admitir.
 *
 * Devuelve el motivo en lugar de lanzarlo porque la pantalla lo presenta como error del
 * campo, junto a la casilla que el ejecutante intentó desmarcar.
 */
class RemoveExerciseEquipmentUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) {

    sealed interface Result {
        data object Removed : Result
        data object LastOption : Result
        data object HasRegisteredSets : Result
    }

    suspend operator fun invoke(exerciseId: Long, equipmentTypeId: Long): Result {
        if (exerciseRepository.countEquipmentOfExercise(exerciseId) <= 1) {
            return Result.LastOption
        }
        if (exerciseRepository.countSetsWithEquipment(exerciseId, equipmentTypeId) > 0) {
            return Result.HasRegisteredSets
        }
        exerciseRepository.removeEquipmentFromExercise(exerciseId, equipmentTypeId)
        return Result.Removed
    }
}
