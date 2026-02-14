package com.estebancoloradogonzalez.tension.domain.repository

import com.estebancoloradogonzalez.tension.domain.model.Exercise
import com.estebancoloradogonzalez.tension.domain.model.ModuleWithVersions
import com.estebancoloradogonzalez.tension.domain.model.PlanVersionDetail
import kotlinx.coroutines.flow.Flow

interface PlanRepository {
    fun getModulesWithVersionCounts(): Flow<List<ModuleWithVersions>>
    fun getVersionDetail(moduleVersionId: Long): Flow<PlanVersionDetail?>
    fun getAvailableExercisesForVersion(moduleCode: String, moduleVersionId: Long): Flow<List<Exercise>>
    suspend fun assignExercise(moduleVersionId: Long, exerciseId: Long, sets: Int, reps: String)
    suspend fun unassignExercise(moduleVersionId: Long, exerciseId: Long)
}
