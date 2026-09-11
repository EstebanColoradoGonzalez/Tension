package com.estebancoloradogonzalez.tension.domain.repository

import com.estebancoloradogonzalez.tension.domain.model.Exercise
import com.estebancoloradogonzalez.tension.domain.model.PlanExercise
import com.estebancoloradogonzalez.tension.domain.model.PlanVersionDetail
import com.estebancoloradogonzalez.tension.domain.model.Routine
import com.estebancoloradogonzalez.tension.domain.model.RoutineWithVersions
import kotlinx.coroutines.flow.Flow

interface PlanRepository {
    fun getRoutinesWithVersionCounts(): Flow<List<RoutineWithVersions>>
    suspend fun getAllRoutines(): List<Routine>
    fun getVersionDetail(routineVersionId: Long): Flow<PlanVersionDetail?>
    fun getAvailableExercisesForVersion(routineVersionId: Long): Flow<List<Exercise>>
    suspend fun assignExercise(
        routineVersionId: Long,
        exerciseId: Long,
        sets: Int,
        reps: String,
        suggestedEquipmentTypeId: Long,
    )

    suspend fun addAlternativeToSlot(
        routineVersionId: Long,
        slot: Int,
        exerciseId: Long,
        suggestedEquipmentTypeId: Long,
    )
    suspend fun getAlternativesForSlot(routineVersionId: Long, slot: Int): List<PlanExercise>
    suspend fun unassignExercise(routineVersionId: Long, exerciseId: Long)
    suspend fun updatePlanAssignment(routineVersionId: Long, exerciseId: Long, sets: Int, reps: String)

    /**
     * Cambia el implemento que el plan sugiere para un puesto.
     *
     * **No se propaga al slot**, a diferencia de series y repeticiones: las alternativas
     * de un puesto dual comparten prescripción pero no implemento, porque son ejercicios
     * distintos con opciones distintas (CA-41.05).
     */
    suspend fun setSuggestedEquipment(
        routineVersionId: Long,
        exerciseId: Long,
        equipmentTypeId: Long,
    )

    /**
     * Rutinas cuyo plan sugiere ese implemento para ese ejercicio. Vacía si ninguna.
     *
     * Sostiene el impedimento de CA-41.08 al retirar una opción de equipamiento.
     */
    suspend fun getRoutinesSuggestingEquipment(exerciseId: Long, equipmentTypeId: Long): List<String>
}
