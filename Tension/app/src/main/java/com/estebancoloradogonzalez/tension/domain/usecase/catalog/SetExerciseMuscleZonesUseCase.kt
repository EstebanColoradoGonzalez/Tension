package com.estebancoloradogonzalez.tension.domain.usecase.catalog

import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import javax.inject.Inject

/**
 * Reemplaza las zonas musculares de un ejercicio y su jerarquía (CA-41.02).
 *
 * Se persiste de inmediato, sin botón de guardado — mismo patrón que el cambio de imagen,
 * el de dificultad de progresión y el de equipamiento en la ficha del ejercicio.
 *
 * Devuelve el motivo en lugar de lanzarlo, como [RemoveExerciseEquipmentUseCase]: la
 * pantalla lo presenta como error del campo de zonas principales, que es donde el
 * ejecutante incumple la regla.
 */
class SetExerciseMuscleZonesUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) {

    sealed interface Result {
        data object Saved : Result

        /** Ninguna zona principal. El ejercicio no se persiste (CA-41.09). */
        data object NoPrimaryZone : Result

        /** Alguna zona figura a la vez como principal y como secundaria (CA-41.09). */
        data object ZoneInBothLists : Result
    }

    suspend operator fun invoke(
        exerciseId: Long,
        primaryMuscleZoneIds: List<Long>,
        secondaryMuscleZoneIds: List<Long>,
    ): Result {
        if (primaryMuscleZoneIds.isEmpty()) return Result.NoPrimaryZone
        if (primaryMuscleZoneIds.intersect(secondaryMuscleZoneIds.toSet()).isNotEmpty()) {
            return Result.ZoneInBothLists
        }
        exerciseRepository.setMuscleZones(
            exerciseId = exerciseId,
            primaryMuscleZoneIds = primaryMuscleZoneIds.distinct(),
            secondaryMuscleZoneIds = secondaryMuscleZoneIds.distinct(),
        )
        return Result.Saved
    }
}
