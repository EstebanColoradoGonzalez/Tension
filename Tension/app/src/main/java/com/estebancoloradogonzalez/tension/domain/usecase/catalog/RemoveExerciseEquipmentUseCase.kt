package com.estebancoloradogonzalez.tension.domain.usecase.catalog

import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import com.estebancoloradogonzalez.tension.domain.repository.PlanRepository
import javax.inject.Inject

/**
 * Retira un implemento de las opciones que el ejercicio admite.
 *
 * Tres razones lo pueden impedir, y las tres se comprueban antes de escribir:
 *
 * - **Es la última opción** (CA-39.09). El atributo es obligatorio y no admite lista
 *   vacía, igual al crear que al editar.
 * - **Tiene series registradas** (CA-39.10). Una serie es inmutable tras su creación: no
 *   puede quedar apuntando a un equipamiento que el ejercicio dejó de admitir.
 * - **El plan lo sugiere** (CA-41.08). `plan_assignment.suggested_equipment_type_id` es
 *   `NOT NULL` y debe estar entre los implementos admitidos; retirarlo dejaría la
 *   asignación sugiriendo algo que el ejercicio ya no acepta.
 *
 * El orden es el del coste de reparación para el ejecutante: quitar la última opción no
 * tiene arreglo posible, borrar series tampoco, y cambiar la sugerencia del plan sí.
 *
 * Devuelve el motivo en lugar de lanzarlo porque la pantalla lo presenta como error del
 * campo, junto a la casilla que el ejecutante intentó desmarcar.
 */
class RemoveExerciseEquipmentUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val planRepository: PlanRepository,
) {

    sealed interface Result {
        data object Removed : Result
        data object LastOption : Result
        data object HasRegisteredSets : Result

        /**
         * Rutinas cuyo plan lo sugiere. Se devuelven los nombres y no un conteo para que
         * el mensaje las nombre: un «no se puede» sin decir dónde obliga a buscar a mano.
         */
        data class SuggestedByPlan(val routineNames: List<String>) : Result
    }

    suspend operator fun invoke(exerciseId: Long, equipmentTypeId: Long): Result {
        if (exerciseRepository.countEquipmentOfExercise(exerciseId) <= 1) {
            return Result.LastOption
        }
        if (exerciseRepository.countSetsWithEquipment(exerciseId, equipmentTypeId) > 0) {
            return Result.HasRegisteredSets
        }
        val suggestingRoutines = planRepository.getRoutinesSuggestingEquipment(
            exerciseId,
            equipmentTypeId,
        )
        if (suggestingRoutines.isNotEmpty()) {
            return Result.SuggestedByPlan(suggestingRoutines)
        }
        exerciseRepository.removeEquipmentFromExercise(exerciseId, equipmentTypeId)
        return Result.Removed
    }
}
