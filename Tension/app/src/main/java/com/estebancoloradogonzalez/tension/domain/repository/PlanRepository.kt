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
    suspend fun assignExercise(routineVersionId: Long, exerciseId: Long, sets: Int, reps: String)
    suspend fun addAlternativeToSlot(routineVersionId: Long, slot: Int, exerciseId: Long)
    suspend fun getAlternativesForSlot(routineVersionId: Long, slot: Int): List<PlanExercise>
    suspend fun unassignExercise(routineVersionId: Long, exerciseId: Long)
    suspend fun updatePlanAssignment(routineVersionId: Long, exerciseId: Long, sets: Int, reps: String)
}
