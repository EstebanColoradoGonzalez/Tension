package com.estebancoloradogonzalez.tension.domain.usecase.plan

import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import com.estebancoloradogonzalez.tension.domain.repository.PlanRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Fija el implemento que el plan sugiere para un puesto (CA-41.05, `D6-T1`).
 *
 * La sugerencia **debe ser una de las opciones que el ejercicio admite**, y eso se
 * comprueba aquí y no con una FK compuesta a `exercise_equipment`: `RESTRICT` sobre una
 * tabla que la ficha del ejercicio edita en caliente convertiría este rechazo explicado en
 * una excepción de SQLite. El selector de la pantalla solo ofrece las admitidas, así que
 * este `require` es la red y no la puerta — pero la red hace falta, porque el ejercicio
 * puede perder una opción entre que la pantalla se pinta y el ejecutante confirma.
 *
 * No se propaga al slot, a diferencia de series y repeticiones: las alternativas de un
 * puesto dual son ejercicios distintos con opciones distintas.
 */
class SetSuggestedEquipmentUseCase @Inject constructor(
    private val planRepository: PlanRepository,
    private val exerciseRepository: ExerciseRepository,
) {
    suspend operator fun invoke(
        routineVersionId: Long,
        exerciseId: Long,
        equipmentTypeId: Long,
    ) {
        val admitted = exerciseRepository.getEquipmentIdsOfExercise(exerciseId).first()
        require(equipmentTypeId in admitted) {
            "Suggested equipment must be one of the options the exercise admits"
        }
        planRepository.setSuggestedEquipment(routineVersionId, exerciseId, equipmentTypeId)
    }
}
