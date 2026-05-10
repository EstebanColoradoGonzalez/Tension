package com.estebancoloradogonzalez.tension.data.repository

import androidx.room.withTransaction
import com.estebancoloradogonzalez.tension.data.local.dao.AlertDao
import com.estebancoloradogonzalez.tension.data.local.dao.DeloadDao
import com.estebancoloradogonzalez.tension.data.local.dao.DeloadFrozenVersionDao
import com.estebancoloradogonzalez.tension.data.local.dao.ExerciseDao
import com.estebancoloradogonzalez.tension.data.local.dao.ExerciseProgressionDao
import com.estebancoloradogonzalez.tension.data.local.dao.ExerciseSetDao
import com.estebancoloradogonzalez.tension.data.local.dao.PlanAssignmentDao
import com.estebancoloradogonzalez.tension.data.local.dao.ProfileDao
import com.estebancoloradogonzalez.tension.data.local.dao.RotationStateDao
import com.estebancoloradogonzalez.tension.data.local.dao.RoutineCurrentVersionDao
import com.estebancoloradogonzalez.tension.data.local.dao.RoutineDao
import com.estebancoloradogonzalez.tension.data.local.dao.RoutineVersionDao
import com.estebancoloradogonzalez.tension.data.local.dao.SessionDao
import com.estebancoloradogonzalez.tension.data.local.dao.SessionExerciseDao
import com.estebancoloradogonzalez.tension.data.local.database.TensionDatabase
import com.estebancoloradogonzalez.tension.data.local.entity.AlertEntity
import com.estebancoloradogonzalez.tension.data.local.entity.DeloadEntity
import com.estebancoloradogonzalez.tension.data.local.entity.DeloadFrozenVersionEntity
import com.estebancoloradogonzalez.tension.data.local.entity.ExerciseProgressionEntity
import com.estebancoloradogonzalez.tension.data.local.entity.ExerciseSetEntity
import com.estebancoloradogonzalez.tension.data.local.entity.SessionEntity
import com.estebancoloradogonzalez.tension.data.local.entity.SessionExerciseEntity
import com.estebancoloradogonzalez.tension.data.repository.model.SessionSummaryData
import com.estebancoloradogonzalez.tension.domain.model.ActiveSession
import com.estebancoloradogonzalez.tension.domain.model.DeloadState
import com.estebancoloradogonzalez.tension.domain.model.ExerciseHistoryData
import com.estebancoloradogonzalez.tension.domain.model.ExerciseHistoryEntry
import com.estebancoloradogonzalez.tension.domain.model.ExerciseResetLoad
import com.estebancoloradogonzalez.tension.domain.model.ExerciseSessionData
import com.estebancoloradogonzalez.tension.domain.model.ExerciseSessionStatus
import com.estebancoloradogonzalez.tension.domain.model.NextSession
import com.estebancoloradogonzalez.tension.domain.model.ProgressionClassification
import com.estebancoloradogonzalez.tension.domain.model.RegisterSetInfo
import com.estebancoloradogonzalez.tension.domain.model.RotationResolver
import com.estebancoloradogonzalez.tension.domain.model.RotationState
import com.estebancoloradogonzalez.tension.domain.model.SessionDetail
import com.estebancoloradogonzalez.tension.domain.model.SessionDetailExercise
import com.estebancoloradogonzalez.tension.domain.model.SessionExerciseDetail
import com.estebancoloradogonzalez.tension.domain.model.SessionHistoryItem
import com.estebancoloradogonzalez.tension.domain.model.SessionPreviewExercise
import com.estebancoloradogonzalez.tension.domain.model.SetData
import com.estebancoloradogonzalez.tension.domain.model.SetForTonnage
import com.estebancoloradogonzalez.tension.domain.model.SubstituteExerciseInfo
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import com.estebancoloradogonzalez.tension.domain.rules.AdherenceRule
import com.estebancoloradogonzalez.tension.domain.rules.AlertThresholdRule
import com.estebancoloradogonzalez.tension.domain.rules.AvgRirRule
import com.estebancoloradogonzalez.tension.domain.rules.DeloadLoadRule
import com.estebancoloradogonzalez.tension.domain.rules.DeloadNeedRule
import com.estebancoloradogonzalez.tension.domain.rules.DoubleThresholdRule
import com.estebancoloradogonzalez.tension.domain.rules.LoadIncrementResolver
import com.estebancoloradogonzalez.tension.domain.rules.RoutineFatigueRule
import com.estebancoloradogonzalez.tension.domain.rules.ProgressionClassificationRule
import com.estebancoloradogonzalez.tension.domain.rules.ProgressionRateRule
import com.estebancoloradogonzalez.tension.domain.rules.TonnageRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@Suppress("TooManyFunctions", "LargeClass")
class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
    private val sessionExerciseDao: SessionExerciseDao,
    private val planAssignmentDao: PlanAssignmentDao,
    private val rotationStateDao: RotationStateDao,
    private val routineDao: RoutineDao,
    private val routineVersionDao: RoutineVersionDao,
    private val routineCurrentVersionDao: RoutineCurrentVersionDao,
    private val deloadFrozenVersionDao: DeloadFrozenVersionDao,
    private val exerciseSetDao: ExerciseSetDao,
    private val exerciseProgressionDao: ExerciseProgressionDao,
    private val alertDao: AlertDao,
    private val database: TensionDatabase,
    private val deloadDao: DeloadDao,
    private val exerciseDao: ExerciseDao,
    private val profileDao: ProfileDao,
) : SessionRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getNextSessionInfo(): Flow<NextSession?> {
        return rotationStateDao.getRotationState().flatMapLatest { rotationState ->
            if (rotationState == null) {
                flowOf(null)
            } else {
                deloadDao.getActiveDeload().flatMapLatest { deload ->
                    routineDao.getAll().flatMapLatest { routines ->
                        if (routines.isEmpty()) return@flatMapLatest flowOf(null)

                        if (deload != null) {
                            val frozenVersions = deloadFrozenVersionDao.getByDeloadId(deload.id)
                            if (frozenVersions.isEmpty()) return@flatMapLatest flowOf(null)
                            val frozenCount = frozenVersions.size
                            val frozenIndex = RotationResolver.resolveRoutineIndex(
                                rotationState.microcyclePosition,
                                frozenCount,
                            )
                            val routineSortMap = routines.associate { it.id to it.sortOrder }
                            val sortedFrozen = frozenVersions.sortedBy { routineSortMap[it.routineId] ?: Int.MAX_VALUE }
                            val frozenVersion = sortedFrozen[frozenIndex]
                            val frozenRoutine = routines.find { it.id == frozenVersion.routineId }
                                ?: return@flatMapLatest flowOf(null)
                            val versionNumber = frozenVersion.frozenVersionNumber
                            val rv = routineVersionDao.getByRoutineIdAndVersion(
                                frozenRoutine.id,
                                versionNumber,
                            )
                            val routineVersionId = rv?.id ?: return@flatMapLatest flowOf(null)

                            val hasExercises = planAssignmentDao
                                .countExercisesForRoutineVersion(routineVersionId) > 0
                            if (!hasExercises) return@flatMapLatest flowOf(null)

                            flowOf(
                                NextSession(
                                    routineId = frozenRoutine.id,
                                    routineName = frozenRoutine.name,
                                    versionNumber = versionNumber,
                                    routineVersionId = routineVersionId,
                                ),
                            )
                        } else {
                            routineCurrentVersionDao.getAll().flatMapLatest { currentVersions ->
                                val routineCount = routines.size
                                val routineIndex = RotationResolver.resolveRoutineIndex(
                                    rotationState.microcyclePosition,
                                    routineCount,
                                )
                                val sortedRoutines = routines.sortedBy { it.sortOrder }
                                val routine = sortedRoutines[routineIndex]
                                val cv = currentVersions.find { it.routineId == routine.id }
                                    ?: return@flatMapLatest flowOf(null)
                                val versionNumber = cv.currentVersionNumber
                                val rv = routineVersionDao.getByRoutineIdAndVersion(
                                    routine.id,
                                    versionNumber,
                                )
                                val routineVersionId = rv?.id ?: return@flatMapLatest flowOf(null)

                                val hasExercises = planAssignmentDao
                                    .countExercisesForRoutineVersion(routineVersionId) > 0
                                if (!hasExercises) return@flatMapLatest flowOf(null)

                                flowOf(
                                    NextSession(
                                        routineId = routine.id,
                                        routineName = routine.name,
                                        versionNumber = versionNumber,
                                        routineVersionId = routineVersionId,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override suspend fun startSession(routineVersionId: Long): Long {
        return database.withTransaction {
            val activeSession = sessionDao.getActiveSession().first()
            if (activeSession != null) {
                throw IllegalStateException("A session is already in progress")
            }

            val rv = routineVersionDao.getById(routineVersionId).first()
                ?: throw IllegalStateException("Routine version not found")

            val activeDeload = deloadDao.getActiveDeloadOnce()

            if (activeDeload == null) {
                val cv = routineCurrentVersionDao.getByRoutineId(rv.routineId)
                if (cv != null && cv.currentVersionNumber != rv.versionNumber) {
                    throw IllegalStateException("Routine version is no longer the active version")
                }
            } else {
                val frozenVersions = deloadFrozenVersionDao.getByDeloadId(activeDeload.id)
                val isFrozen = frozenVersions.any {
                    it.routineId == rv.routineId && it.frozenVersionNumber == rv.versionNumber
                }
                if (!isFrozen) {
                    throw IllegalStateException("Routine version is not part of the active deload")
                }
            }

            val sessionId = sessionDao.insert(
                SessionEntity(
                    routineVersionId = routineVersionId,
                    date = LocalDate.now().toString(),
                    status = "IN_PROGRESS",
                    deloadId = activeDeload?.id,
                ),
            )

            val planAssignments = planAssignmentDao.getByRoutineVersionId(routineVersionId).first()
            if (planAssignments.isEmpty()) {
                throw IllegalStateException("No exercises assigned to this routine version")
            }

            // Group assignments by slot. Always assign the primary exercise (first by sort_order).
            // The user can switch to an alternative from within the exercise during the session.
            val bySlot = planAssignments.groupBy { it.slot }
            val sessionExercises = bySlot.entries
                .sortedBy { it.key }
                .map { (slot, alternatives) ->
                    SessionExerciseEntity(
                        sessionId = sessionId,
                        exerciseId = alternatives.first().exerciseId,
                        originalExerciseId = null,
                        progressionClassification = null,
                        pendingSelection = 0,
                        slot = slot,
                    )
                }
            sessionExerciseDao.insertAll(sessionExercises)

            sessionId
        }
    }

    override fun getActiveSession(): Flow<ActiveSession?> {
        return sessionDao.getActiveSessionWithRoutineVersion().map { info ->
            info?.let {
                ActiveSession(
                    sessionId = it.sessionId,
                    routineName = it.routineName,
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
                val isPending = detail.pendingSelection == 1
                val status = when {
                    isPending -> ExerciseSessionStatus.NOT_STARTED
                    detail.isFinalized == 1 -> ExerciseSessionStatus.COMPLETED
                    detail.completedSets == 0 -> ExerciseSessionStatus.NOT_STARTED
                    else -> ExerciseSessionStatus.IN_PROGRESS
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
                    muscleGroup = detail.muscleGroup,
                    isFinalized = detail.isFinalized == 1,
                    pendingSelection = isPending,
                    slot = detail.slot,
                    hasAlternatives = detail.alternativesInSlot > 1,
                )
            }
        }
    }

    override fun getRotationState(): Flow<RotationState?> {
        return rotationStateDao.getRotationState().map { entity ->
            entity?.let {
                RotationState(
                    microcyclePosition = it.microcyclePosition,
                    microcycleCount = it.microcycleCount,
                )
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getSessionRoutineVersion(sessionId: Long): Flow<Pair<String, Int>?> {
        return sessionDao.getById(sessionId).flatMapLatest { session ->
            if (session == null) {
                flowOf(null)
            } else {
                routineVersionDao.getById(session.routineVersionId).map { rv ->
                    if (rv != null) {
                        val routine = routineDao.getById(rv.routineId)
                        Pair(routine?.name ?: "", rv.versionNumber)
                    } else {
                        null
                    }
                }
            }
        }
    }

    override suspend fun getRegisterSetInfo(sessionExerciseId: Long): RegisterSetInfo? {
        val info = sessionExerciseDao.getExerciseInfoForSet(sessionExerciseId) ?: return null
        val nextSetNumber = exerciseSetDao.getNextSetNumber(sessionExerciseId)

        val isDeload = info.deloadId != null

        val lastWeightKg = if (info.isBodyweight == 1 || info.isIsometric == 1) {
            0.0
        } else if (isDeload) {
            val progressionExerciseId = info.originalExerciseId ?: info.exerciseId
            val progression = exerciseProgressionDao
                .getByExerciseId(progressionExerciseId).first()
            val prescribedLoad = progression?.prescribedLoadKg
            if (prescribedLoad != null) {
                val muscleGroup = sessionExerciseDao
                    .getPrimaryMuscleGroupByExercise(progressionExerciseId) ?: ""
                val increment = LoadIncrementResolver.resolve(muscleGroup)
                DeloadLoadRule.calculateDeloadLoad(prescribedLoad, increment)
            } else {
                exerciseSetDao.getLastWeightForExercise(progressionExerciseId)
            }
        } else {
            val progressionExerciseId = info.originalExerciseId ?: info.exerciseId
            val progression = exerciseProgressionDao
                .getByExerciseId(progressionExerciseId).first()
            val prescribedLoad = progression?.prescribedLoadKg?.takeIf { it > 0.0 }
            prescribedLoad ?: exerciseSetDao.getLastWeightForExercise(progressionExerciseId)
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
            prescribedReps = info.reps,
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

            exerciseSetDao.insert(
                ExerciseSetEntity(
                    sessionExerciseId = sessionExerciseId,
                    setNumber = nextSetNumber,
                    weightKg = weightKg,
                    reps = reps,
                    rir = rir,
                ),
            )

            val progressionExerciseId = info.originalExerciseId ?: info.exerciseId
            exerciseProgressionDao.insertIfNotExists(
                ExerciseProgressionEntity(exerciseId = progressionExerciseId),
            )
        }
    }

    override suspend fun getSubstituteExerciseInfo(
        sessionExerciseId: Long,
    ): SubstituteExerciseInfo? {
        val info = sessionExerciseDao.getSessionExerciseForSubstitution(sessionExerciseId)
            ?: return null
        if (info.completedSets > 0) return null
        val referenceExerciseId = info.originalExerciseId ?: info.exerciseId
        val muscleZoneIds = exerciseDao.getMuscleZoneIdsByExerciseId(referenceExerciseId)
        return SubstituteExerciseInfo(
            sessionExerciseId = sessionExerciseId,
            currentExerciseId = info.exerciseId,
            currentExerciseName = info.exerciseName,
            sessionId = info.sessionId,
            muscleZoneIds = muscleZoneIds,
        )
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

            if (info.exerciseId == newExerciseId) {
                throw IllegalArgumentException("Cannot substitute with the same exercise")
            }

            val referenceExerciseId = info.originalExerciseId ?: info.exerciseId
            val originalZones = exerciseDao.getMuscleZoneIdsByExerciseId(referenceExerciseId)
            val newZones = exerciseDao.getMuscleZoneIdsByExerciseId(newExerciseId)
            if (originalZones.intersect(newZones.toSet()).isEmpty()) {
                throw IllegalArgumentException("Substitute must share at least one muscle zone")
            }

            val existsInSession = sessionExerciseDao.getBySessionId(info.sessionId).first()
                .any { it.exerciseId == newExerciseId && it.id != sessionExerciseId }
            if (existsInSession) {
                throw IllegalArgumentException("Exercise is already in this session")
            }

            val originalExerciseId = info.originalExerciseId ?: info.exerciseId

            sessionExerciseDao.updateExerciseId(
                sessionExerciseId,
                newExerciseId,
                originalExerciseId,
            )
        }
    }

    override suspend fun finalizeExercise(sessionExerciseId: Long) {
        sessionExerciseDao.finalizeExercise(sessionExerciseId)
    }

    override suspend fun switchAlternativeInSession(sessionExerciseId: Long, exerciseId: Long) {
        sessionExerciseDao.switchAlternativeExercise(sessionExerciseId, exerciseId)
    }

    override suspend fun closeSession(sessionId: Long) {
        database.withTransaction {
            // Determine COMPLETED/INCOMPLETE BEFORE bulk-finalizing
            val sessionInfo = sessionDao.getActiveSessionWithRoutineVersion().first()
                ?: throw IllegalStateException("No active session found")
            if (sessionInfo.sessionId != sessionId) {
                throw IllegalStateException("Session $sessionId is not the active session")
            }

            val status = if (sessionInfo.completedExercises == sessionInfo.totalExercises) {
                "COMPLETED"
            } else {
                "INCOMPLETE"
            }

            // Finalize all un-finalized exercises before closing
            sessionExerciseDao.finalizeAllInSession(sessionId)
            sessionDao.updateStatus(sessionId, status)

            val routineVersionId = sessionDao.getRoutineVersionIdBySessionId(sessionId)
            val deloadId = sessionDao.getDeloadIdBySessionId(sessionId)
            evaluateProgression(
                sessionId = sessionId,
                routineVersionId = routineVersionId,
                isDeloadSession = deloadId != null,
            )

            val routineCount: Int
            if (deloadId != null) {
                routineCount = deloadFrozenVersionDao.getByDeloadId(deloadId).size
            } else {
                routineCount = routineDao.countRoutines()
            }
            val rotationEntity = rotationStateDao.getRotationState().first()
                ?: throw IllegalStateException("Rotation state not found")
            val currentRotation = RotationState(
                microcyclePosition = rotationEntity.microcyclePosition,
                microcycleCount = rotationEntity.microcycleCount,
            )
            val newRotation = RotationResolver.advanceRotation(
                currentRotation,
                routineCount,
            )

            // Advance version numbers at end of microcycle (if not deload)
            if (newRotation.microcycleCount > currentRotation.microcycleCount && deloadId == null) {
                val allCurrentVersions = routineCurrentVersionDao.getAllOnce()
                for (cv in allCurrentVersions) {
                    val newVersion = findNextNonEmptyVersion(
                        cv.routineId,
                        cv.currentVersionNumber,
                    )
                    if (newVersion != cv.currentVersionNumber) {
                        routineCurrentVersionDao.update(
                            cv.copy(currentVersionNumber = newVersion),
                        )
                    }
                }
            }

            rotationStateDao.update(
                rotationEntity.copy(
                    microcyclePosition = newRotation.microcyclePosition,
                    microcycleCount = if (deloadId != null) {
                        currentRotation.microcycleCount
                    } else {
                        newRotation.microcycleCount
                    },
                ),
            )

            if (deloadId != null) {
                val deloadSessionCount = sessionDao.countDeloadSessions(deloadId)
                val frozenCount = deloadFrozenVersionDao.getByDeloadId(deloadId).size
                if (deloadSessionCount == frozenCount) {
                    val today = LocalDate.now().toString()
                    val deload = deloadDao.getById(deloadId)
                        ?: throw IllegalStateException("Deload $deloadId not found")

                    deloadDao.complete(deloadId, today)
                    deloadFrozenVersionDao.deleteByDeloadId(deloadId)

                    val allInDeload = exerciseProgressionDao.getAllInDeload()
                    for (progression in allInDeload) {
                        val exercise = exerciseDao.getByIdOnce(progression.exerciseId)
                            ?: continue
                        val isBodyweight = exercise.isBodyweight == 1
                        val isIsometric = exercise.isIsometric == 1

                        if (isBodyweight || isIsometric) {
                            exerciseProgressionDao.update(
                                progression.copy(
                                    status = "IN_PROGRESSION",
                                    sessionsWithoutProgression = 0,
                                ),
                            )
                        } else {
                            val preDeloadWeight = exerciseSetDao.getPreDeloadAvgWeight(
                                progression.exerciseId,
                                deload.activationDate,
                            )
                            val muscleGroup = getMuscleGroupForExercise(progression.exerciseId)
                            val loadIncrementKg = LoadIncrementResolver.resolve(muscleGroup)

                            val resetLoad = if (
                                preDeloadWeight != null && preDeloadWeight > 0.0
                            ) {
                                DeloadLoadRule.calculateResetLoad(
                                    preDeloadWeight,
                                    loadIncrementKg,
                                )
                            } else {
                                null
                            }

                            exerciseProgressionDao.update(
                                progression.copy(
                                    status = "IN_PROGRESSION",
                                    prescribedLoadKg = resetLoad,
                                    sessionsWithoutProgression = 0,
                                ),
                            )
                        }
                    }

                    alertDao.resolveAllByType("ROUTINE_REQUIRES_DELOAD", today)
                }
            }
        }
    }

    private suspend fun getMuscleGroupForExercise(exerciseId: Long): String? {
        val exerciseDetail = exerciseDao.getById(exerciseId).first()
        return exerciseDetail?.muscleGroup
    }

    private suspend fun evaluateProgression(
        sessionId: Long,
        routineVersionId: Long,
        isDeloadSession: Boolean,
    ) {
        val routineVersion = routineVersionDao.getById(routineVersionId).first()
        val routineId = routineVersion?.routineId ?: return

        val exercises = sessionExerciseDao.getSessionExercisesForProgression(sessionId)
        val today = LocalDate.now().toString()

        var regressionCount = 0
        var exercisesWithRecords = 0

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

            // During deload, skip classification and progression update entirely.
            // Intentionally underloaded deload work (60% load) should not affect
            // progression state or produce misleading classification labels.
            if (isDeloadSession) {
                val prog = exerciseProgressionDao
                    .getByExerciseId(exercise.exerciseId).first()
                if (prog == null || prog.status != "IN_DELOAD") continue
                // Preserve prescribed load for IN_DELOAD exercises (state is frozen)
                exerciseProgressionDao.update(
                    prog.copy(
                        prescribedLoadKg = prog.prescribedLoadKg,
                    ),
                )
                continue
            }

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

            val prescribedLoadKg = if (isBodyweight || isIsometric) {
                null
            } else {
                val meetsThreshold = DoubleThresholdRule.meetsDoubleThreshold(currentData)
                if (meetsThreshold) {
                    val loadIncrementKg = LoadIncrementResolver.resolve(exercise.muscleGroup)
                    currentData.avgWeightKg + loadIncrementKg
                } else {
                    currentProgression.prescribedLoadKg ?: currentData.avgWeightKg
                }
            }

            if (classification != null) {
                exercisesWithRecords++
                if (classification == ProgressionClassification.REGRESSION) {
                    regressionCount++
                }
            }

            if (!isDeloadSession) {
                val previousStatus = currentProgression.status
                if (previousStatus != "IN_PLATEAU" && newStatus == "IN_PLATEAU") {
                    if (!alertDao.existsActiveByExercise(exercise.exerciseId, "PLATEAU")) {
                        alertDao.insert(
                            AlertEntity(
                                type = "PLATEAU",
                                level = "HIGH_ALERT",
                                exerciseId = exercise.exerciseId,
                                routineId = routineId,
                                message = "3 sesiones sin progresión",
                                isActive = 1,
                                createdAt = today,
                            ),
                        )
                    }
                } else if (previousStatus == "IN_PLATEAU" && newStatus != "IN_PLATEAU") {
                    alertDao.resolveByExerciseAndType(exercise.exerciseId, "PLATEAU", today)
                }
            }

            exerciseProgressionDao.update(
                currentProgression.copy(
                    status = newStatus,
                    sessionsWithoutProgression = newCounter,
                    prescribedLoadKg = prescribedLoadKg,
                ),
            )
        }

        try { evaluateLowAdherence(today) } catch (_: Exception) { }
        try { evaluateRoutineInactivity(today, routineId) } catch (_: Exception) { }

        if (isDeloadSession) return

        // Use slot count (not individual exercise count) so that having alternatives
        // in a slot does not artificially inflate the denominator and dilute the ratios.
        val totalSlots = planAssignmentDao.countDistinctSlots(routineVersionId)
        val fatigueDetected = RoutineFatigueRule.detectFatigue(
            regressionCount,
            totalSlots,
        )
        val affectedCount = planAssignmentDao.countAffectedSlotsForDeload(
            routineVersionId,
            sessionId,
        )
        val deloadNeeded = DeloadNeedRule.needsDeload(
            affectedCount,
            totalSlots,
            fatigueDetected,
        )

        if (deloadNeeded) {
            if (!alertDao.existsActiveByRoutine(routineId, "ROUTINE_REQUIRES_DELOAD")) {
                alertDao.insert(
                    AlertEntity(
                        type = "ROUTINE_REQUIRES_DELOAD",
                        level = "HIGH_ALERT",
                        routineId = routineId,
                        message = "≥50% ejercicios en meseta/regresión",
                        isActive = 1,
                        createdAt = today,
                    ),
                )
            }
        } else {
            alertDao.resolveByRoutineAndType(routineId, "ROUTINE_REQUIRES_DELOAD", today)
        }

        try { evaluateLowProgressionRate(today) } catch (_: Exception) { }
        try { evaluateRirOutOfRange(today) } catch (_: Exception) { }
        try { evaluateTonnageDrop(today) } catch (_: Exception) { }
    }

    override suspend fun getSessionSummaryData(sessionId: Long): SessionSummaryData {
        val info = sessionDao.getSessionSummaryInfo(sessionId)
        val exercises = sessionExerciseDao.getExercisesForSummary(sessionId)
        val routineRequiresDeload = alertDao.existsActiveByRoutine(
            info.routineId,
            "ROUTINE_REQUIRES_DELOAD",
        )
        return SessionSummaryData(info, exercises, routineRequiresDeload)
    }

    override suspend fun activateDeload() {
        database.withTransaction {
            val existingDeload = deloadDao.getActiveDeloadOnce()
            if (existingDeload != null) {
                throw IllegalStateException("A deload cycle is already active")
            }
            val today = LocalDate.now().toString()
            val deloadId = deloadDao.insert(
                DeloadEntity(
                    activationDate = today,
                ),
            )
            val currentVersions = routineCurrentVersionDao.getAllOnce()
            val frozenEntities = currentVersions.mapNotNull { cv ->
                val rv = routineVersionDao.getByRoutineIdAndVersion(
                    cv.routineId,
                    cv.currentVersionNumber,
                ) ?: return@mapNotNull null
                val hasExercises = planAssignmentDao
                    .countExercisesForRoutineVersion(rv.id) > 0
                if (!hasExercises) return@mapNotNull null
                DeloadFrozenVersionEntity(
                    deloadId = deloadId,
                    routineId = cv.routineId,
                    frozenVersionNumber = cv.currentVersionNumber,
                )
            }
            require(frozenEntities.isNotEmpty()) {
                "No routines with exercises available for deload"
            }
            deloadFrozenVersionDao.insertAll(frozenEntities)

            val exerciseIdsInDeload = frozenEntities.flatMap { frozen ->
                val rv = routineVersionDao.getByRoutineIdAndVersion(
                    frozen.routineId,
                    frozen.frozenVersionNumber,
                )
                if (rv != null) {
                    planAssignmentDao.getExerciseIdsByRoutineVersionId(rv.id)
                } else {
                    emptyList()
                }
            }.distinct()
            if (exerciseIdsInDeload.isNotEmpty()) {
                exerciseProgressionDao.transitionToDeload(exerciseIdsInDeload)
            }
        }
    }

    override suspend fun getDeloadState(): DeloadState {
        val activeDeload = deloadDao.getActiveDeloadOnce()
        if (activeDeload != null) {
            val frozenVersions = deloadFrozenVersionDao.getByDeloadId(activeDeload.id)
            if (frozenVersions.isEmpty()) {
                deloadDao.complete(activeDeload.id, LocalDate.now().toString())
                deloadFrozenVersionDao.deleteByDeloadId(activeDeload.id)
                return DeloadState.NoDeloadNeeded
            }
            val progress = sessionDao.countDeloadSessions(activeDeload.id)
            return DeloadState.DeloadActive(
                progress = progress,
                totalSessions = frozenVersions.size,
                frozenVersions = frozenVersions.associate { fv ->
                    val routineName = routineDao.getById(fv.routineId)?.name ?: "Rutina ${fv.routineId}"
                    routineName to fv.frozenVersionNumber
                },
            )
        }

        val lastCompleted = deloadDao.getLastCompletedDeload()
        if (lastCompleted != null) {
            val hasPostDeloadSession = sessionDao.hasSessionAfterDeload(lastCompleted.id)
            if (!hasPostDeloadSession) {
                val resetLoads = getResetLoadsForCompletedDeload(lastCompleted.id)
                return DeloadState.DeloadCompleted(resetLoads)
            }
        }

        val deloadAlerts = alertDao.getActiveAlertsByType("ROUTINE_REQUIRES_DELOAD")
        if (deloadAlerts.isNotEmpty()) {
            val routineNames = deloadAlerts.mapNotNull { alert ->
                alert.routineId?.let { routineDao.getById(it)?.name }
            }.distinct()
            return DeloadState.DeloadRequired(routineNames)
        }

        return DeloadState.NoDeloadNeeded
    }

    override suspend fun hasActiveDeload(): Boolean =
        deloadDao.getActiveDeloadOnce() != null

    override suspend fun hasActiveSessionForVersion(routineVersionId: Long): Boolean =
        sessionDao.hasActiveSessionForVersion(routineVersionId)

    override suspend fun getDeloadIdBySessionId(sessionId: Long): Long? =
        sessionDao.getDeloadIdBySessionId(sessionId)

    override suspend fun getRoutineVersionIdBySessionId(sessionId: Long): Long =
        sessionDao.getRoutineVersionIdBySessionId(sessionId)

    private suspend fun getResetLoadsForCompletedDeload(deloadId: Long): List<ExerciseResetLoad> {
        val deloadSessionIds = sessionDao.getSessionIdsByDeloadId(deloadId)
        if (deloadSessionIds.isEmpty()) return emptyList()
        val deloadExerciseIds = deloadSessionIds.flatMap { sessionId ->
            sessionExerciseDao.getExerciseIdsBySessionId(sessionId)
        }.distinct()
        if (deloadExerciseIds.isEmpty()) return emptyList()

        val progressions = exerciseProgressionDao.getAllWithPrescribedLoad()
        return progressions.filter { it.exerciseId in deloadExerciseIds }.mapNotNull { progression ->
            val exercise = exerciseDao.getByIdOnce(progression.exerciseId) ?: return@mapNotNull null
            if (exercise.isBodyweight == 1 || exercise.isIsometric == 1) return@mapNotNull null
            val loadKg = progression.prescribedLoadKg ?: return@mapNotNull null
            ExerciseResetLoad(exerciseName = exercise.name, resetLoadKg = loadKg)
        }
    }

    override suspend fun getSessionHistory(): List<SessionHistoryItem> {
        return sessionDao.getClosedSessionsWithSummary().map { dto ->
            SessionHistoryItem(
                sessionId = dto.sessionId,
                date = dto.date,
                routineName = dto.routineName,
                versionNumber = dto.versionNumber,
                status = dto.status,
                totalTonnageKg = dto.totalTonnageKg,
            )
        }
    }

    override suspend fun getSessionDetail(sessionId: Long): SessionDetail {
        val summaryInfo = sessionDao.getSessionSummaryInfo(sessionId)
        val sessionEntity = sessionDao.getById(sessionId).first()
            ?: throw IllegalArgumentException("Session not found: $sessionId")
        val exerciseDtos = sessionExerciseDao.getExercisesForSessionDetail(sessionId)

        val exercises = exerciseDtos.map { dto ->
            val sets = exerciseSetDao.getSetsForSessionExercise(dto.sessionExerciseId).map { set ->
                SetData(weightKg = set.weightKg, reps = set.reps, rir = set.rir)
            }
            SessionDetailExercise(
                exerciseId = dto.exerciseId,
                exerciseName = dto.exerciseName,
                classification = dto.classification?.let { parseClassification(it) },
                originalExerciseName = dto.originalExerciseName,
                sets = sets,
                isDeload = dto.isDeload,
            )
        }

        return SessionDetail(
            sessionId = sessionId,
            date = sessionEntity.date,
            routineName = summaryInfo.routineName,
            versionNumber = summaryInfo.versionNumber,
            status = summaryInfo.status,
            totalTonnageKg = summaryInfo.totalTonnageKg,
            totalExercises = summaryInfo.totalExercises,
            completedExercises = summaryInfo.completedExercises,
            exercises = exercises,
        )
    }

    override suspend fun getExerciseHistory(exerciseId: Long): ExerciseHistoryData {
        val exercise = exerciseDao.getByIdOnce(exerciseId)
            ?: throw IllegalArgumentException("Exercise not found: $exerciseId")
        val progression = exerciseProgressionDao.getByExerciseId(exerciseId).first()
        val historyDtos = sessionExerciseDao.getExerciseHistoryEntries(exerciseId)

        val entries = historyDtos.map { dto ->
            ExerciseHistoryEntry(
                date = dto.date,
                routineName = dto.routineName,
                versionNumber = dto.versionNumber,
                avgWeightKg = dto.avgWeightKg,
                totalReps = dto.totalReps,
                avgRir = dto.avgRir,
                classification = dto.classification?.let { parseClassification(it) },
                isDeload = dto.isDeload,
            )
        }

        return ExerciseHistoryData(
            exerciseName = exercise.name,
            progressionStatus = progression?.status ?: "NO_HISTORY",
            isBodyweight = exercise.isBodyweight == 1,
            isIsometric = exercise.isIsometric == 1,
            entries = entries,
        )
    }

    private fun parseClassification(value: String): ProgressionClassification? {
        return try {
            ProgressionClassification.valueOf(value)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private suspend fun evaluateLowProgressionRate(today: String) {
        val startDate = LocalDate.now().minusWeeks(4).toString()
        val counts = sessionExerciseDao.getClassificationCountsByPeriod(startDate)

        for (exerciseCount in counts) {
            val rate = ProgressionRateRule.calculate(
                exerciseCount.positiveCount,
                exerciseCount.totalCount,
            )
            val level = AlertThresholdRule.progressionLevel(rate)
            val exerciseId = exerciseCount.exerciseId

            if (level != null) {
                alertDao.resolveByExerciseAndType(
                    exerciseId,
                    "LOW_PROGRESSION_RATE",
                    today,
                )
                alertDao.insert(
                    AlertEntity(
                        type = "LOW_PROGRESSION_RATE",
                        level = level,
                        exerciseId = exerciseId,
                        message = "Tasa: ${rate.toInt()}%",
                        isActive = 1,
                        createdAt = today,
                    ),
                )
            } else {
                alertDao.resolveByExerciseAndType(
                    exerciseId,
                    "LOW_PROGRESSION_RATE",
                    today,
                )
            }
        }
    }

    private suspend fun evaluateRirOutOfRange(today: String) {
        val routines = routineDao.getAllOnce()
        for (routine in routines) {
            val sessionIds = sessionDao.getSessionIdsByRoutineInRange(routine.id, 2)
            if (sessionIds.size < 2) continue

            val firstRirValues = exerciseSetDao.getRirValuesBySessionIds(
                listOf(sessionIds[0]),
            )
            val secondRirValues = exerciseSetDao.getRirValuesBySessionIds(
                listOf(sessionIds[1]),
            )
            if (firstRirValues.isEmpty() || secondRirValues.isEmpty()) continue

            val firstAvg = AvgRirRule.calculate(firstRirValues)
            val secondAvg = AvgRirRule.calculate(secondRirValues)

            val bothLow = AlertThresholdRule.isRirLow(firstAvg) &&
                AlertThresholdRule.isRirLow(secondAvg)
            val bothHigh = AlertThresholdRule.isRirHigh(firstAvg) &&
                AlertThresholdRule.isRirHigh(secondAvg)

            if (bothLow || bothHigh) {
                alertDao.resolveByRoutineAndType(routine.id, "RIR_OUT_OF_RANGE", today)
                alertDao.insert(
                    AlertEntity(
                        type = "RIR_OUT_OF_RANGE",
                        level = "MEDIUM_ALERT",
                        routineId = routine.id,
                        message = if (bothLow) "RIR <0.5 sostenido" else "RIR >1.8 sostenido",
                        isActive = 1,
                        createdAt = today,
                    ),
                )
            } else {
                val bothOptimal = !AlertThresholdRule.isRirOutOfRange(firstAvg) &&
                    !AlertThresholdRule.isRirOutOfRange(secondAvg)
                if (bothOptimal) {
                    alertDao.resolveByRoutineAndType(
                        routine.id,
                        "RIR_OUT_OF_RANGE",
                        today,
                    )
                }
            }
        }
    }

    private suspend fun findNextNonEmptyVersion(
        routineId: Long,
        currentVersionNumber: Int,
    ): Int {
        val allVersions = routineVersionDao.getVersionNumbersByRoutineId(routineId)
        if (allVersions.isEmpty()) return currentVersionNumber
        val sorted = allVersions.sorted()
        val startIdx = sorted.indexOf(currentVersionNumber)
        for (offset in 1..sorted.size) {
            val candidate = sorted[(startIdx + offset) % sorted.size]
            val rv = routineVersionDao.getByRoutineIdAndVersion(routineId, candidate)
            if (rv != null && planAssignmentDao.countExercisesForRoutineVersion(rv.id) > 0) {
                return candidate
            }
        }
        return currentVersionNumber
    }

    private suspend fun evaluateLowAdherence(today: String) {
        val firstSessionDate = sessionDao.getFirstSessionDate() ?: return
        val todayDate = LocalDate.now()
        if (ChronoUnit.DAYS.between(
                LocalDate.parse(firstSessionDate), todayDate,
            ) < 7
        ) return

        val profile = profileDao.getProfile().first() ?: return
        val weeklyFrequency = profile.weeklyFrequency

        val prevWeekStart = todayDate.minusWeeks(1).with(DayOfWeek.MONDAY).toString()
        val prevWeekEnd = todayDate.minusWeeks(1).with(DayOfWeek.SUNDAY).toString()
        val prevWeekSessions = sessionDao.countSessionsInWeek(prevWeekStart, prevWeekEnd)
        val prevAdherence = AdherenceRule.calculate(prevWeekSessions, weeklyFrequency)

        if (AlertThresholdRule.isAdherenceLow(prevAdherence)) {
            val twoWeeksAgoStart = todayDate.minusWeeks(2).with(DayOfWeek.MONDAY).toString()
            val twoWeeksAgoEnd = todayDate.minusWeeks(2).with(DayOfWeek.SUNDAY).toString()
            val twoWeeksAgoSessions = sessionDao.countSessionsInWeek(
                twoWeeksAgoStart,
                twoWeeksAgoEnd,
            )
            val twoWeeksAgoAdherence = AdherenceRule.calculate(
                twoWeeksAgoSessions,
                weeklyFrequency,
            )

            val level = if (AlertThresholdRule.isAdherenceLow(twoWeeksAgoAdherence)) {
                "CRISIS"
            } else {
                "MEDIUM_ALERT"
            }

            alertDao.resolveAllByType("LOW_ADHERENCE", today)
            alertDao.insert(
                AlertEntity(
                    type = "LOW_ADHERENCE",
                    level = level,
                    message = "Adherencia: ${prevAdherence.toInt()}%",
                    isActive = 1,
                    createdAt = today,
                ),
            )
        } else {
            alertDao.resolveAllByType("LOW_ADHERENCE", today)
        }
    }

    private suspend fun evaluateTonnageDrop(today: String) {
        val closedSessions = sessionDao.getClosedSessionsOrdered()
            .filter { it.deloadId == null }
        val routineCount = routineDao.countRoutines()
        val cycleSize = if (routineCount > 0) routineCount else 1
        val completeMicrocycles = closedSessions.chunked(cycleSize).filter { it.size == cycleSize }
        if (completeMicrocycles.size < 2) return

        val currentMicrocycle = completeMicrocycles.last()
        val previousMicrocycle = completeMicrocycles[completeMicrocycles.size - 2]

        val currentTonnageData = exerciseSetDao.getTonnageDataBySessionIds(
            currentMicrocycle.map { it.id },
        )
        val currentTonnage = TonnageRule.calculateForMuscleGroup(
            currentTonnageData.map {
                SetForTonnage(it.weightKg, it.reps, it.muscleGroup)
            },
        )

        val previousTonnageData = exerciseSetDao.getTonnageDataBySessionIds(
            previousMicrocycle.map { it.id },
        )
        val previousTonnage = TonnageRule.calculateForMuscleGroup(
            previousTonnageData.map {
                SetForTonnage(it.weightKg, it.reps, it.muscleGroup)
            },
        )

        val allMuscleGroups = (currentTonnage.keys + previousTonnage.keys).toSet()
        for (muscleGroup in allMuscleGroups) {
            val prev = previousTonnage[muscleGroup] ?: 0.0
            val curr = currentTonnage[muscleGroup] ?: 0.0
            if (prev <= 0.0) continue

            val dropPercentage = ((prev - curr) / prev) * 100.0
            val level = AlertThresholdRule.tonnageLevel(dropPercentage, false)

            if (level != null) {
                alertDao.resolveByMuscleGroupAndType(muscleGroup, "TONNAGE_DROP", today)
                val message = "Caída de tonelaje −${dropPercentage.toInt()}%"
                alertDao.insert(
                    AlertEntity(
                        type = "TONNAGE_DROP",
                        level = level,
                        muscleGroup = muscleGroup,
                        message = message,
                        isActive = 1,
                        createdAt = today,
                    ),
                )
            } else {
                alertDao.resolveByMuscleGroupAndType(muscleGroup, "TONNAGE_DROP", today)
            }
        }
    }

    private suspend fun evaluateRoutineInactivity(today: String, currentRoutineId: Long) {
        val routines = routineDao.getAllOnce()
        for (routine in routines) {
            if (routine.id == currentRoutineId) {
                alertDao.resolveByRoutineAndType(routine.id, "ROUTINE_INACTIVITY", today)
                continue
            }

            val lastDate = sessionDao.getLastSessionDateByRoutine(routine.id)
                ?: routine.createdAt
            val daysSince = ChronoUnit.DAYS.between(
                LocalDate.parse(lastDate),
                LocalDate.now(),
            )
            val level = AlertThresholdRule.inactivityLevel(daysSince)

            if (level != null) {
                alertDao.resolveByRoutineAndType(routine.id, "ROUTINE_INACTIVITY", today)
                val muscleZones = planAssignmentDao.getMuscleZoneNamesByRoutineId(routine.id)
                val zonesText = if (muscleZones.isNotEmpty()) {
                    " (${muscleZones.joinToString(", ")})"
                } else {
                    ""
                }
                alertDao.insert(
                    AlertEntity(
                        type = "ROUTINE_INACTIVITY",
                        level = level,
                        routineId = routine.id,
                        message = "Rutina ${routine.name}: $daysSince días sin sesión$zonesText",
                        isActive = 1,
                        createdAt = today,
                    ),
                )
            } else {
                alertDao.resolveByRoutineAndType(routine.id, "ROUTINE_INACTIVITY", today)
            }
        }
    }

    override fun getSessionPreviewExercises(
        routineVersionId: Long,
    ): Flow<List<SessionPreviewExercise>> {
        return planAssignmentDao.getPreviewByRoutineVersionId(routineVersionId).map { dtos ->
            // HU-25: a slot may have multiple alternatives. The preview shows ONE entry per slot
            // (the primary exercise: lowest sort_order). dtos are already ORDER BY sort_order ASC,
            // so groupBy preserves that order and first() picks the primary.
            dtos.groupBy { it.slot }.values.map { slotDtos -> slotDtos.first() }.map { dto ->
                SessionPreviewExercise(
                    exerciseId = dto.exerciseId,
                    exerciseName = dto.exerciseName,
                    equipmentTypeName = dto.equipmentTypeName,
                    muscleZones = dto.muscleZones ?: "",
                    sets = dto.sets,
                    reps = dto.reps,
                    isBodyweight = dto.isBodyweight == 1,
                    isIsometric = dto.isIsometric == 1,
                    isToTechnicalFailure = dto.isToTechnicalFailure == 1,
                    prescribedLoadKg = dto.prescribedLoadKg,
                    muscleGroup = dto.muscleGroup,
                )
            }
        }
    }
}
