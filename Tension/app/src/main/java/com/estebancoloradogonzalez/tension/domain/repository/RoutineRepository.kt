package com.estebancoloradogonzalez.tension.domain.repository

import com.estebancoloradogonzalez.tension.domain.model.Routine
import com.estebancoloradogonzalez.tension.domain.model.RoutineVersion
import kotlinx.coroutines.flow.Flow

interface RoutineRepository {
    fun getRoutines(): Flow<List<Routine>>
    suspend fun createRoutine(name: String)
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
