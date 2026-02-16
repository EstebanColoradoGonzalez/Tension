package com.estebancoloradogonzalez.tension.data.repository

import androidx.room.withTransaction
import com.estebancoloradogonzalez.tension.data.local.dao.ExerciseProgressionDao
import com.estebancoloradogonzalez.tension.data.local.dao.ExerciseSetDao
import com.estebancoloradogonzalez.tension.data.local.dao.ModuleVersionDao
import com.estebancoloradogonzalez.tension.data.local.dao.PlanAssignmentDao
import com.estebancoloradogonzalez.tension.data.local.dao.RotationStateDao
import com.estebancoloradogonzalez.tension.data.local.dao.SessionDao
import com.estebancoloradogonzalez.tension.data.local.dao.SessionExerciseDao
import com.estebancoloradogonzalez.tension.data.local.database.TensionDatabase
import com.estebancoloradogonzalez.tension.data.local.entity.ExerciseProgressionEntity
import com.estebancoloradogonzalez.tension.data.local.entity.ExerciseSetEntity
import com.estebancoloradogonzalez.tension.data.local.entity.SessionEntity
import com.estebancoloradogonzalez.tension.data.local.entity.SessionExerciseEntity
import com.estebancoloradogonzalez.tension.domain.model.ActiveSession
import com.estebancoloradogonzalez.tension.domain.model.ExerciseSessionData
import com.estebancoloradogonzalez.tension.domain.model.ExerciseSessionStatus
import com.estebancoloradogonzalez.tension.domain.model.RegisterSetInfo
import com.estebancoloradogonzalez.tension.domain.model.RotationResolver
import com.estebancoloradogonzalez.tension.domain.model.RotationState
import com.estebancoloradogonzalez.tension.domain.model.SessionExerciseDetail
import com.estebancoloradogonzalez.tension.domain.model.SetData
import com.estebancoloradogonzalez.tension.domain.model.SubstituteExerciseInfo
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import com.estebancoloradogonzalez.tension.domain.rules.DoubleThresholdRule
import com.estebancoloradogonzalez.tension.domain.rules.ProgressionClassificationRule
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
    private val exerciseSetDao: ExerciseSetDao,
    private val exerciseProgressionDao: ExerciseProgressionDao,
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

    override suspend fun getRegisterSetInfo(sessionExerciseId: Long): RegisterSetInfo? {
        val info = sessionExerciseDao.getExerciseInfoForSet(sessionExerciseId) ?: return null
        val nextSetNumber = exerciseSetDao.getNextSetNumber(sessionExerciseId)
        if (nextSetNumber > info.totalSets) return null

        val lastWeightKg = if (info.isBodyweight == 1 || info.isIsometric == 1) {
            0.0
        } else {
            exerciseSetDao.getLastWeightForExercise(info.exerciseId)
        }

        return RegisterSetInfo(
            sessionExerciseId = sessionExerciseId,
            exerciseId = info.exerciseId,
            exerciseName = info.exerciseName,
            currentSetNumber = nextSetNumber,
            totalSets = info.totalSets,
            lastWeightKg = lastWeightKg,
            isBodyweight = info.isBodyweight == 1,
            isIsometric = info.isIsometric == 1,
            isToTechnicalFailure = info.isToTechnicalFailure == 1,
        )
    }

    override suspend fun registerSet(
        sessionExerciseId: Long,
        weightKg: Double,
        reps: Int,
        rir: Int,
    ) {
        database.withTransaction {
            val nextSetNumber = exerciseSetDao.getNextSetNumber(sessionExerciseId)
            val info = sessionExerciseDao.getExerciseInfoForSet(sessionExerciseId)
                ?: throw IllegalStateException("Session exercise not found")

            if (nextSetNumber > info.totalSets) {
                throw IllegalStateException("Exercise already has maximum sets registered")
            }

            exerciseSetDao.insert(
                ExerciseSetEntity(
                    sessionExerciseId = sessionExerciseId,
                    setNumber = nextSetNumber,
                    weightKg = weightKg,
                    reps = reps,
                    rir = rir,
                ),
            )

            exerciseProgressionDao.insertIfNotExists(
                ExerciseProgressionEntity(exerciseId = info.exerciseId),
            )
        }
    }

    override suspend fun getSubstituteExerciseInfo(
        sessionExerciseId: Long,
    ): SubstituteExerciseInfo? {
        val info = sessionExerciseDao.getSessionExerciseForSubstitution(sessionExerciseId)
            ?: return null
        if (info.completedSets > 0) return null
        return SubstituteExerciseInfo(
            sessionExerciseId = sessionExerciseId,
            currentExerciseId = info.exerciseId,
            currentExerciseName = info.exerciseName,
            moduleCode = info.moduleCode,
            sessionId = info.sessionId,
        )
    }

    override suspend fun getExerciseIdsForSession(sessionId: Long): List<Long> {
        return sessionExerciseDao.getExerciseIdsForSession(sessionId)
    }

    override suspend fun substituteExercise(
        sessionExerciseId: Long,
        newExerciseId: Long,
    ) {
        database.withTransaction {
            val info = sessionExerciseDao.getSessionExerciseForSubstitution(sessionExerciseId)
                ?: throw IllegalStateException("Session exercise not found")

            if (info.completedSets > 0) {
                throw IllegalStateException("Cannot substitute exercise with registered sets")
            }

            val originalExerciseId = info.originalExerciseId ?: info.exerciseId

            sessionExerciseDao.updateExerciseId(
                sessionExerciseId,
                newExerciseId,
                originalExerciseId,
            )
        }
    }

    override suspend fun closeSession(sessionId: Long) {
        database.withTransaction {
            val sessionInfo = sessionDao.getActiveSessionWithModuleVersion().first()
                ?: throw IllegalStateException("No active session found")
            if (sessionInfo.sessionId != sessionId) {
                throw IllegalStateException("Session $sessionId is not the active session")
            }

            val status = if (sessionInfo.completedExercises == sessionInfo.totalExercises) {
                "COMPLETED"
            } else {
                "INCOMPLETE"
            }
            sessionDao.updateStatus(sessionId, status)

            // Step 2: Evaluate progression (HU-10)
            evaluateProgression(sessionId)

            // Step 3: Advance rotation (HU-09)
            val rotationEntity = rotationStateDao.getRotationState().first()
                ?: throw IllegalStateException("Rotation state not found")
            val currentRotation = RotationState(
                microcyclePosition = rotationEntity.microcyclePosition,
                currentVersionModuleA = rotationEntity.currentVersionModuleA,
                currentVersionModuleB = rotationEntity.currentVersionModuleB,
                currentVersionModuleC = rotationEntity.currentVersionModuleC,
                microcycleCount = rotationEntity.microcycleCount,
            )
            val newRotation = RotationResolver.advanceRotation(currentRotation)
            rotationStateDao.update(
                rotationEntity.copy(
                    microcyclePosition = newRotation.microcyclePosition,
                    currentVersionModuleA = newRotation.currentVersionModuleA,
                    currentVersionModuleB = newRotation.currentVersionModuleB,
                    currentVersionModuleC = newRotation.currentVersionModuleC,
                    microcycleCount = newRotation.microcycleCount,
                ),
            )
        }
    }

    private suspend fun evaluateProgression(sessionId: Long) {
        val exercises = sessionExerciseDao.getSessionExercisesForProgression(sessionId)

        for (exercise in exercises) {
            val currentSetDtos = exerciseSetDao.getSetsForSessionExercise(
                exercise.sessionExerciseId,
            )
            if (currentSetDtos.isEmpty()) continue

            val currentData = ExerciseSessionData(
                sets = currentSetDtos.map { SetData(it.weightKg, it.reps, it.rir) },
            )

            val previousSetDtos = exerciseSetDao.getLastHistoricalSets(
                exercise.exerciseId,
                sessionId,
            )
            val previousData = if (previousSetDtos.isNotEmpty()) {
                ExerciseSessionData(
                    sets = previousSetDtos.map { SetData(it.weightKg, it.reps, it.rir) },
                )
            } else {
                null
            }

            val isBodyweight = exercise.isBodyweight == 1
            val isIsometric = exercise.isIsometric == 1

            val classification = ProgressionClassificationRule.classify(
                current = currentData,
                previous = previousData,
                isBodyweight = isBodyweight,
                isIsometric = isIsometric,
            )

            sessionExerciseDao.updateProgressionClassification(
                exercise.sessionExerciseId,
                classification?.name,
            )

            val isMastered = isIsometric &&
                ProgressionClassificationRule.isIsometricMastered(currentData)

            val currentProgression = exerciseProgressionDao
                .getByExerciseId(exercise.exerciseId).first()

            if (currentProgression == null) continue

            val (newStatus, newCounter) =
                ProgressionClassificationRule.resolveNewProgressionState(
                    currentStatus = currentProgression.status,
                    currentCounter = currentProgression.sessionsWithoutProgression,
                    classification = classification,
                    isIsometric = isIsometric,
                    isMastered = isMastered,
                )

            // Step 5b: Prescribe load (HU-11)
            val prescribedLoadKg = if (isBodyweight || isIsometric) {
                null
            } else {
                val meetsThreshold = DoubleThresholdRule.meetsDoubleThreshold(currentData)
                DoubleThresholdRule.prescribeLoad(
                    currentAvgWeightKg = currentData.avgWeightKg,
                    loadIncrementKg = exercise.loadIncrementKg,
                    meetsThreshold = meetsThreshold,
                )
            }

            exerciseProgressionDao.update(
                currentProgression.copy(
                    status = newStatus,
                    sessionsWithoutProgression = newCounter,
                    prescribedLoadKg = prescribedLoadKg,
                ),
            )
        }
    }
}
