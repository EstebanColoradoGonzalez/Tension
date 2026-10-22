package com.estebancoloradogonzalez.tension.domain.repository

import com.estebancoloradogonzalez.tension.domain.model.MovementCategory
import com.estebancoloradogonzalez.tension.domain.model.Routine
import com.estebancoloradogonzalez.tension.domain.model.RoutineVersion
import com.estebancoloradogonzalez.tension.domain.rules.CategorizedName
import kotlinx.coroutines.flow.Flow

interface RoutineRepository {
    fun getRoutines(): Flow<List<Routine>>
    suspend fun createRoutine(name: String, movementCategory: MovementCategory)

    /** Cambia la categoría. El caso de uso comprueba antes que el plan no la contradiga. */
    suspend fun updateMovementCategory(id: Long, category: MovementCategory)

    /** Ejercicios asignados a la rutina en cualquier versión, con su categoría (CA-46.14). */
    suspend fun getExercisesAssignedToRoutine(routineId: Long): List<CategorizedName>
    suspend fun updateRoutineName(id: Long, name: String)
    suspend fun deleteRoutine(id: Long)
    suspend fun reorderRoutines(orderedIds: List<Long>)
    fun getVersionsByRoutine(routineId: Long): Flow<List<RoutineVersion>>
    suspend fun createVersion(routineId: Long)
    suspend fun deleteVersion(versionId: Long, routineId: Long)
    suspend fun countRoutines(): Int
    suspend fun hasActiveSessionForRoutine(routineId: Long): Boolean
    suspend fun hasSessionsForRoutine(routineId: Long): Boolean
    suspend fun hasSessionsForVersion(routineVersionId: Long): Boolean
    suspend fun countVersionsByRoutine(routineId: Long): Int
    suspend fun routineNameExists(name: String): Boolean
    suspend fun routineNameExistsExcluding(name: String, excludeId: Long): Boolean
}
