package com.estebancoloradogonzalez.tension.domain.usecase.catalog

import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import javax.inject.Inject

/**
 * Añade un implemento a las opciones que el ejercicio admite (CA-39.03).
 *
 * Se persiste de inmediato, sin botón de guardado — mismo patrón que el cambio de imagen y
 * el de dificultad de progresión en la ficha del ejercicio.
 */
class AddExerciseEquipmentUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) {
    suspend operator fun invoke(exerciseId: Long, equipmentTypeId: Long) {
        exerciseRepository.addEquipmentToExercise(exerciseId, equipmentTypeId)
    }
}
