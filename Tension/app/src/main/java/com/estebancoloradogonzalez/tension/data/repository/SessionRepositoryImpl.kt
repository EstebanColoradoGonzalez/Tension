package com.estebancoloradogonzalez.tension.data.repository

import androidx.room.withTransaction
import com.estebancoloradogonzalez.tension.data.local.dao.ModuleVersionDao
import com.estebancoloradogonzalez.tension.data.local.dao.PlanAssignmentDao
import com.estebancoloradogonzalez.tension.data.local.dao.RotationStateDao
import com.estebancoloradogonzalez.tension.data.local.dao.SessionDao
import com.estebancoloradogonzalez.tension.data.local.dao.SessionExerciseDao
import com.estebancoloradogonzalez.tension.data.local.database.TensionDatabase
import com.estebancoloradogonzalez.tension.data.local.entity.SessionEntity
import com.estebancoloradogonzalez.tension.data.local.entity.SessionExerciseEntity
import com.estebancoloradogonzalez.tension.domain.model.ActiveSession
import com.estebancoloradogonzalez.tension.domain.model.ExerciseSessionStatus
import com.estebancoloradogonzalez.tension.domain.model.NextSession
import com.estebancoloradogonzalez.tension.domain.model.RotationResolver
import com.estebancoloradogonzalez.tension.domain.model.RotationState
import com.estebancoloradogonzalez.tension.domain.model.SessionExerciseDetail
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
    private val sessionExerciseDao: SessionExerciseDao,
    private val planAssignmentDao: PlanAssignmentDao,
    private val rotationStateDao: RotationStateDao,
    private val moduleVersionDao: ModuleVersionDao,
    private val database: TensionDatabase,
) : SessionRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getNextModuleVersionId(): Flow<Long> {
        return rotationStateDao.getRotationState().flatMapLatest { rotationState ->
            if (rotationState == null) {
                flowOf(0L)
            } else {
                val moduleCode = RotationResolver.resolveModuleCode(
                    rotationState.microcyclePosition,
                )
                val versionNumber = RotationResolver.resolveVersionNumber(
                    moduleCode,
                    rotationState.currentVersionModuleA,
                    rotationState.currentVersionModuleB,
                    rotationState.currentVersionModuleC,
                )
                moduleVersionDao.getByModuleCodeAndVersion(moduleCode, versionNumber)
                    .map { it?.id ?: 0L }
            }
        }
    }

    override suspend fun startSession(moduleVersionId: Long): Long {
        return database.withTransaction {
            val activeSession = sessionDao.getActiveSession().first()
            if (activeSession != null) {
                throw IllegalStateException("A session is already in progress")
            }

            val sessionId = sessionDao.insert(
                SessionEntity(
                    moduleVersionId = moduleVersionId,
                    date = LocalDate.now().toString(),
                    status = "IN_PROGRESS",
                    deloadId = null,
                ),
            )

            val planAssignments = planAssignmentDao.getByModuleVersionId(moduleVersionId).first()
            if (planAssignments.isEmpty()) {
                throw IllegalStateException("No exercises assigned to this module version")
            }

            val sessionExercises = planAssignments.map { pa ->
                SessionExerciseEntity(
                    sessionId = sessionId,
                    exerciseId = pa.exerciseId,
                    originalExerciseId = null,
                    progressionClassification = null,
                )
            }
            sessionExerciseDao.insertAll(sessionExercises)

            sessionId
        }
    }

    override fun getActiveSession(): Flow<ActiveSession?> {
        return sessionDao.getActiveSessionWithModuleVersion().map { info ->
            info?.let {
                ActiveSession(
                    sessionId = it.sessionId,
                    moduleCode = it.moduleCode,
                    versionNumber = it.versionNumber,
                    totalExercises = it.totalExercises,
                    completedExercises = it.completedExercises,
                )
            }
        }
    }

    override fun getSessionExercises(sessionId: Long): Flow<List<SessionExerciseDetail>> {
        return sessionExerciseDao.getBySessionIdWithDetails(sessionId).map { list ->
            list.map { detail ->
                val status = when {
                    detail.completedSets == 0 -> ExerciseSessionStatus.NOT_STARTED
                    detail.completedSets < detail.sets -> ExerciseSessionStatus.IN_PROGRESS
                    else -> ExerciseSessionStatus.COMPLETED
                }
                SessionExerciseDetail(
                    sessionExerciseId = detail.sessionExerciseId,
                    exerciseId = detail.exerciseId,
                    name = detail.exerciseName,
                    equipmentTypeName = detail.equipmentTypeName,
                    muscleZones = detail.muscleZones
                        ?.split(",")
                        ?.map { it.trim() }
                        ?.filter { it.isNotEmpty() }
                        ?: emptyList(),
                    sets = detail.sets,
                    reps = detail.reps,
                    isBodyweight = detail.isBodyweight == 1,
                    isIsometric = detail.isIsometric == 1,
                    isToTechnicalFailure = detail.isToTechnicalFailure == 1,
                    prescribedLoadKg = detail.prescribedLoadKg,
                    completedSets = detail.completedSets,
                    status = status,
                )
            }
        }
    }

    override fun getRotationState(): Flow<RotationState?> {
        return rotationStateDao.getRotationState().map { entity ->
            entity?.let {
                RotationState(
                    microcyclePosition = it.microcyclePosition,
                    currentVersionModuleA = it.currentVersionModuleA,
                    currentVersionModuleB = it.currentVersionModuleB,
                    currentVersionModuleC = it.currentVersionModuleC,
                    microcycleCount = it.microcycleCount,
                )
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getSessionModuleVersion(sessionId: Long): Flow<Pair<String, Int>?> {
        return sessionDao.getById(sessionId).flatMapLatest { session ->
            if (session == null) {
                flowOf(null)
            } else {
                moduleVersionDao.getById(session.moduleVersionId).map { mv ->
                    mv?.let { Pair(it.moduleCode, it.versionNumber) }
                }
            }
        }
    }
}
