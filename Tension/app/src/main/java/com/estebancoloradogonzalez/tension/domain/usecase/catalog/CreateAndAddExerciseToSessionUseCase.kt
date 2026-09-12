package com.estebancoloradogonzalez.tension.domain.usecase.catalog

import com.estebancoloradogonzalez.tension.domain.model.ProgressionDifficulty
import com.estebancoloradogonzalez.tension.domain.usecase.session.AddExerciseToSessionUseCase
import javax.inject.Inject

/**
 * Crea un ejercicio desde la sesión activa y lo añade a ella en el mismo gesto (CA-43.02).
 *
 * El destino del ejercicio creado es **asimétrico y deliberado**: queda registrado en el
 * Diccionario como personalizado —disponible para asignarlo al plan más adelante— y queda
 * añadido a la sesión, pero **no entra al plan**.
 *
 * Los dos pasos **no** comparten transacción, y es la decisión que hace que la CA se cumpla
 * en lugar de romperse. La historia exige que el ejercicio *permanezca en el Diccionario
 * aunque la sesión se cierre, se abandone o el ejercicio se retire de ella*: envolver ambos
 * pasos en una sola transacción produciría justo el resultado prohibido —un ejercicio creado
 * que se desvanece— si el añadido fallara. Con la composición separada, un fallo del añadido
 * deja el ejercicio en el Diccionario y el ejecutante lo añade a mano, que es exactamente el
 * comportamiento que la asimetría describe.
 *
 * Las validaciones del formulario no se repiten aquí: viven íntegras en [CreateExerciseUseCase]
 * —al menos un equipamiento (CA-39.09) y al menos una zona principal (CA-41.09)— porque el
 * formulario es el mismo del Diccionario, sin recortes.
 */
class CreateAndAddExerciseToSessionUseCase @Inject constructor(
    private val createExerciseUseCase: CreateExerciseUseCase,
    private val addExerciseToSessionUseCase: AddExerciseToSessionUseCase,
) {
    suspend operator fun invoke(
        sessionId: Long,
        name: String,
        equipmentTypeIds: List<Long>,
        primaryMuscleZoneIds: List<Long>,
        secondaryMuscleZoneIds: List<Long>,
        isBodyweight: Boolean,
        isIsometric: Boolean,
        isToTechnicalFailure: Boolean,
        mediaResource: String?,
        progressionDifficulty: ProgressionDifficulty = ProgressionDifficulty.MEDIUM,
    ): Long {
        val exerciseId = createExerciseUseCase(
            name = name,
            equipmentTypeIds = equipmentTypeIds,
            primaryMuscleZoneIds = primaryMuscleZoneIds,
            secondaryMuscleZoneIds = secondaryMuscleZoneIds,
            isBodyweight = isBodyweight,
            isIsometric = isIsometric,
            isToTechnicalFailure = isToTechnicalFailure,
            mediaResource = mediaResource,
            progressionDifficulty = progressionDifficulty,
        )
        addExerciseToSessionUseCase(sessionId, exerciseId)
        return exerciseId
    }
}
