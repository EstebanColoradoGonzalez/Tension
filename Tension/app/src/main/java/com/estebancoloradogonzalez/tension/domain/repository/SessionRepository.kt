package com.estebancoloradogonzalez.tension.domain.repository

import com.estebancoloradogonzalez.tension.domain.model.ActiveSession
import com.estebancoloradogonzalez.tension.domain.model.NextSession
import com.estebancoloradogonzalez.tension.domain.model.RotationState
import com.estebancoloradogonzalez.tension.domain.model.SessionExerciseDetail
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun getNextModuleVersionId(): Flow<Long>
    suspend fun startSession(moduleVersionId: Long): Long
    fun getActiveSession(): Flow<ActiveSession?>
    fun getSessionExercises(sessionId: Long): Flow<List<SessionExerciseDetail>>
    fun getRotationState(): Flow<RotationState?>
    fun getSessionModuleVersion(sessionId: Long): Flow<Pair<String, Int>?>
}
