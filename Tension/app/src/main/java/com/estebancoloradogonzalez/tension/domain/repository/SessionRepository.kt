package com.estebancoloradogonzalez.tension.domain.repository

import com.estebancoloradogonzalez.tension.data.repository.model.SessionSummaryData
import com.estebancoloradogonzalez.tension.domain.model.ActiveSession
import com.estebancoloradogonzalez.tension.domain.model.Deload
import com.estebancoloradogonzalez.tension.domain.model.DeloadState
import com.estebancoloradogonzalez.tension.domain.model.RegisterSetInfo
import com.estebancoloradogonzalez.tension.domain.model.RotationState
import com.estebancoloradogonzalez.tension.domain.model.SessionExerciseDetail
import com.estebancoloradogonzalez.tension.domain.model.SubstituteExerciseInfo
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun getNextModuleVersionId(): Flow<Long>
    suspend fun startSession(moduleVersionId: Long): Long
    fun getActiveSession(): Flow<ActiveSession?>
    fun getSessionExercises(sessionId: Long): Flow<List<SessionExerciseDetail>>
    fun getRotationState(): Flow<RotationState?>
    fun getSessionModuleVersion(sessionId: Long): Flow<Pair<String, Int>?>
    suspend fun getRegisterSetInfo(sessionExerciseId: Long): RegisterSetInfo?
    suspend fun registerSet(sessionExerciseId: Long, weightKg: Double, reps: Int, rir: Int)
    suspend fun getSubstituteExerciseInfo(sessionExerciseId: Long): SubstituteExerciseInfo?
    suspend fun getExerciseIdsForSession(sessionId: Long): List<Long>
    suspend fun substituteExercise(sessionExerciseId: Long, newExerciseId: Long)
    suspend fun closeSession(sessionId: Long)
    suspend fun getSessionSummaryData(sessionId: Long): SessionSummaryData
    fun getActiveDeload(): Flow<Deload?>
    suspend fun activateDeload()
    suspend fun getDeloadState(): DeloadState
    suspend fun getDeloadIdBySessionId(sessionId: Long): Long?
    suspend fun countDeloadSessions(deloadId: Long): Int
}
