package com.estebancoloradogonzalez.tension.data.repository

import androidx.room.withTransaction
import com.estebancoloradogonzalez.tension.data.local.dao.AlertDao
import com.estebancoloradogonzalez.tension.data.local.dao.DeloadDao
import com.estebancoloradogonzalez.tension.data.local.dao.ExerciseDao
import com.estebancoloradogonzalez.tension.data.local.dao.ExerciseProgressionDao
import com.estebancoloradogonzalez.tension.data.local.dao.ExerciseSetDao
import com.estebancoloradogonzalez.tension.data.local.dao.ModuleDao
import com.estebancoloradogonzalez.tension.data.local.dao.ModuleVersionDao
import com.estebancoloradogonzalez.tension.data.local.dao.PlanAssignmentDao
import com.estebancoloradogonzalez.tension.data.local.dao.ProfileDao
import com.estebancoloradogonzalez.tension.data.local.dao.RotationStateDao
import com.estebancoloradogonzalez.tension.data.local.dao.SessionDao
import com.estebancoloradogonzalez.tension.data.local.dao.SessionExerciseDao
import com.estebancoloradogonzalez.tension.data.local.database.TensionDatabase
import com.estebancoloradogonzalez.tension.data.repository.model.SessionSummaryData
import com.estebancoloradogonzalez.tension.data.local.entity.AlertEntity
import com.estebancoloradogonzalez.tension.data.local.entity.DeloadEntity
import com.estebancoloradogonzalez.tension.data.local.entity.ExerciseProgressionEntity
import com.estebancoloradogonzalez.tension.data.local.entity.ExerciseSetEntity
import com.estebancoloradogonzalez.tension.data.local.entity.SessionEntity
import com.estebancoloradogonzalez.tension.data.local.entity.SessionExerciseEntity
import com.estebancoloradogonzalez.tension.domain.model.ActiveSession
import com.estebancoloradogonzalez.tension.domain.model.Deload
import com.estebancoloradogonzalez.tension.domain.model.DeloadState
import com.estebancoloradogonzalez.tension.domain.model.ExerciseHistoryData
import com.estebancoloradogonzalez.tension.domain.model.ExerciseHistoryEntry
import com.estebancoloradogonzalez.tension.domain.model.ExerciseResetLoad
import com.estebancoloradogonzalez.tension.domain.model.ExerciseSessionData
import com.estebancoloradogonzalez.tension.domain.model.ExerciseSessionStatus
import com.estebancoloradogonzalez.tension.domain.model.ProgressionClassification
import com.estebancoloradogonzalez.tension.domain.model.RegisterSetInfo
import com.estebancoloradogonzalez.tension.domain.model.RotationResolver
import com.estebancoloradogonzalez.tension.domain.model.RotationState
import com.estebancoloradogonzalez.tension.domain.model.SessionDetail
import com.estebancoloradogonzalez.tension.domain.model.SessionDetailExercise
import com.estebancoloradogonzalez.tension.domain.model.SessionExerciseDetail
import com.estebancoloradogonzalez.tension.domain.model.SessionHistoryItem
import com.estebancoloradogonzalez.tension.domain.model.SetData
import com.estebancoloradogonzalez.tension.domain.model.SetForTonnage
import com.estebancoloradogonzalez.tension.domain.model.SubstituteExerciseInfo
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import com.estebancoloradogonzalez.tension.domain.rules.DeloadLoadRule
import com.estebancoloradogonzalez.tension.domain.rules.DeloadNeedRule
import com.estebancoloradogonzalez.tension.domain.rules.DoubleThresholdRule
import com.estebancoloradogonzalez.tension.domain.rules.ModuleFatigueRule
import com.estebancoloradogonzalez.tension.domain.rules.ProgressionClassificationRule
import com.estebancoloradogonzalez.tension.domain.rules.AdherenceRule
import com.estebancoloradogonzalez.tension.domain.rules.AlertThresholdRule
import com.estebancoloradogonzalez.tension.domain.rules.AvgRirRule
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

class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
    private val sessionExerciseDao: SessionExerciseDao,
    private val planAssignmentDao: PlanAssignmentDao,
    private val rotationStateDao: RotationStateDao,
    private val moduleVersionDao: ModuleVersionDao,
    private val exerciseSetDao: ExerciseSetDao,
    private val exerciseProgressionDao: ExerciseProgressionDao,
    private val alertDao: AlertDao,
    private val database: TensionDatabase,
    private val deloadDao: DeloadDao,
    private val exerciseDao: ExerciseDao,
    private val moduleDao: ModuleDao,
    private val profileDao: ProfileDao,
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
                deloadDao.getActiveDeload().flatMapLatest { deload ->
                    val versionNumber = if (deload != null) {
                        RotationResolver.resolveVersionNumber(
                            moduleCode,
                            deload.frozenVersionModuleA,
                            deload.frozenVersionModuleB,
                            deload.frozenVersionModuleC,
                        )
                    } else {
                        RotationResolver.resolveVersionNumber(
                            moduleCode,
                            rotationState.currentVersionModuleA,
                            rotationState.currentVersionModuleB,
                            rotationState.currentVersionModuleC,
                        )
                    }
                    moduleVersionDao.getByModuleCodeAndVersion(moduleCode, versionNumber)
                        .map { it?.id ?: 0L }
                }
            }
        }
    }

    override suspend fun startSession(moduleVersionId: Long): Long {
        return database.withTransaction {
            val activeSession = sessionDao.getActiveSession().first()
            if (activeSession != null) {
                throw IllegalStateException("A session is already in progress")
            }

            val activeDeload = deloadDao.getActiveDeloadOnce()

            val sessionId = sessionDao.insert(
                SessionEntity(
                    moduleVersionId = moduleVersionId,
                    date = LocalDate.now().toString(),
                    status = "IN_PROGRESS",
                    deloadId = activeDeload?.id,
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
                    loadIncrementKg = detail.loadIncrementKg,
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

            // Step 2: Evaluate progression (HU-10, HU-11, HU-12)
            val moduleVersionId = sessionDao.getModuleVersionIdBySessionId(sessionId)
            val deloadId = sessionDao.getDeloadIdBySessionId(sessionId)
            evaluateProgression(
                sessionId = sessionId,
                moduleVersionId = moduleVersionId,
                isDeloadSession = deloadId != null,
            )

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
            val isDeloadSession = deloadId != null
            val newRotation = RotationResolver.advanceRotation(currentRotation, isDeloadSession)
            rotationStateDao.update(
                rotationEntity.copy(
                    microcyclePosition = newRotation.microcyclePosition,
                    currentVersionModuleA = newRotation.currentVersionModuleA,
                    currentVersionModuleB = newRotation.currentVersionModuleB,
                    currentVersionModuleC = newRotation.currentVersionModuleC,
                    microcycleCount = newRotation.microcycleCount,
                ),
            )

            // Step 4: Deload finalization (HU-14)
            if (isDeloadSession && deloadId != null) {
                val deloadSessionCount = sessionDao.countDeloadSessions(deloadId)
                if (deloadSessionCount == 6) {
                    val today = LocalDate.now().toString()
                    val deload = deloadDao.getById(deloadId)
                        ?: throw IllegalStateException("Deload $deloadId not found")

                    deloadDao.complete(deloadId, today)

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
                            val module = moduleDao.getByCode(exercise.moduleCode)
                            val loadIncrementKg = module?.loadIncrementKg ?: 2.5

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

                    alertDao.resolveAllByType("MODULE_REQUIRES_DELOAD", today)
                }
            }
        }
    }

    private suspend fun evaluateProgression(
        sessionId: Long,
        moduleVersionId: Long,
        isDeloadSession: Boolean,
    ) {
        val exercises = sessionExerciseDao.getSessionExercisesForProgression(sessionId)
        val today = LocalDate.now().toString()

        // HU-12: Accumulators for module-level analysis
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
            // HU-14: During deload, preserve existing prescribed_load_kg
            val prescribedLoadKg = if (isDeloadSession) {
                currentProgression.prescribedLoadKg
            } else if (isBodyweight || isIsometric) {
                null
            } else {
                val meetsThreshold = DoubleThresholdRule.meetsDoubleThreshold(currentData)
                DoubleThresholdRule.prescribeLoad(
                    currentAvgWeightKg = currentData.avgWeightKg,
                    loadIncrementKg = exercise.loadIncrementKg,
                    meetsThreshold = meetsThreshold,
                )
            }

            // Step 5c: Collect classification for module-level analysis (HU-12)
            if (classification != null) {
                exercisesWithRecords++
                if (classification == ProgressionClassification.REGRESSION) {
                    regressionCount++
                }
            }

            // Step 5d: Plateau alert management (HU-12)
            // HU-14: Skip plateau alerting during deload sessions
            if (!isDeloadSession) {
                val previousStatus = currentProgression.status
                if (previousStatus != "IN_PLATEAU" && newStatus == "IN_PLATEAU") {
                    if (!alertDao.existsActiveByExercise(exercise.exerciseId, "PLATEAU")) {
                        alertDao.insert(
                            AlertEntity(
                                type = "PLATEAU",
                                level = "HIGH_ALERT",
                                exerciseId = exercise.exerciseId,
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

        // Step 6: Module-level detection (HU-12, post-loop)
        val moduleCode = exercises.firstOrNull()?.moduleCode ?: return

        // HU-17 Steps 9 & 11: Adherence + Inactivity (evaluate before deload guard)
        // Adherence: deload sessions count as completed sessions (CA-17.12)
        // Inactivity: deload sessions resolve inactivity for that module (CA-17.29)
        try { evaluateLowAdherence(today) } catch (_: Exception) { }
        try { evaluateModuleInactivity(today, moduleCode) } catch (_: Exception) { }

        // DELOAD GUARD: Skip progression/RIR/tonnage evaluation during deload sessions
        if (isDeloadSession) return

        // CA-12.04, CA-12.05: Module fatigue detection
        val fatigueDetected = ModuleFatigueRule.detectFatigue(
            regressionCount,
            exercisesWithRecords,
        )

        // CA-12.20, CA-12.21, CA-12.22: Deload need detection
        val totalExercises = planAssignmentDao.countExercisesForModuleVersion(moduleVersionId)
        val affectedCount = planAssignmentDao.countAffectedForDeload(
            moduleVersionId,
            sessionId,
        )
        val deloadNeeded = DeloadNeedRule.needsDeload(
            affectedCount,
            totalExercises,
            fatigueDetected,
        )

        if (deloadNeeded) {
            // CA-12.20, CA-12.21, CA-12.23, CA-12.24
            if (!alertDao.existsActiveByModule(moduleCode, "MODULE_REQUIRES_DELOAD")) {
                alertDao.insert(
                    AlertEntity(
                        type = "MODULE_REQUIRES_DELOAD",
                        level = "HIGH_ALERT",
                        moduleCode = moduleCode,
                        message = "≥50% ejercicios en meseta/regresión",
                        isActive = 1,
                        createdAt = today,
                    ),
                )
            }
        } else {
            alertDao.resolveByModuleAndType(moduleCode, "MODULE_REQUIRES_DELOAD", today)
        }

        // HU-17 Steps 7, 8, 10: Progression Rate, RIR, Tonnage
        // These are correctly guarded by deload because:
        // - Progression: deload sessions use IN_DELOAD classification
        // - RIR: deload targets RIR 4-5 intentionally
        // - Tonnage: deload uses 60% load, drops are expected
        try { evaluateLowProgressionRate(today) } catch (_: Exception) { }
        try { evaluateRirOutOfRange(today) } catch (_: Exception) { }
        try { evaluateTonnageDrop(today) } catch (_: Exception) { }
    }

    override suspend fun getSessionSummaryData(sessionId: Long): SessionSummaryData {
        val info = sessionDao.getSessionSummaryInfo(sessionId)
        val exercises = sessionExerciseDao.getExercisesForSummary(sessionId)
        val moduleRequiresDeload = alertDao.existsActiveByModule(
            info.moduleCode,
            "MODULE_REQUIRES_DELOAD",
        )
        return SessionSummaryData(info, exercises, moduleRequiresDeload)
    }

    override fun getActiveDeload(): Flow<Deload?> = deloadDao.getActiveDeload().map { entity ->
        entity?.let {
            Deload(
                id = it.id,
                status = it.status,
                activationDate = it.activationDate,
                completionDate = it.completionDate,
                frozenVersionModuleA = it.frozenVersionModuleA,
                frozenVersionModuleB = it.frozenVersionModuleB,
                frozenVersionModuleC = it.frozenVersionModuleC,
            )
        }
    }

    override suspend fun activateDeload() {
        database.withTransaction {
            val existingDeload = deloadDao.getActiveDeloadOnce()
            if (existingDeload != null) {
                throw IllegalStateException("A deload cycle is already active")
            }
            val rotationEntity = rotationStateDao.getRotationState().first()
                ?: throw IllegalStateException("Rotation state not found")
            val today = LocalDate.now().toString()
            deloadDao.insert(
                DeloadEntity(
                    activationDate = today,
                    frozenVersionModuleA = rotationEntity.currentVersionModuleA,
                    frozenVersionModuleB = rotationEntity.currentVersionModuleB,
                    frozenVersionModuleC = rotationEntity.currentVersionModuleC,
                ),
            )
            exerciseProgressionDao.transitionToDeload()
        }
    }

    override suspend fun getDeloadState(): DeloadState {
        val activeDeload = deloadDao.getActiveDeloadOnce()
        if (activeDeload != null) {
            val progress = sessionDao.countDeloadSessions(activeDeload.id)
            return DeloadState.DeloadActive(
                progress = progress,
                totalSessions = 6,
                frozenVersionA = activeDeload.frozenVersionModuleA,
                frozenVersionB = activeDeload.frozenVersionModuleB,
                frozenVersionC = activeDeload.frozenVersionModuleC,
            )
        }

        val lastCompleted = deloadDao.getLastCompletedDeload()
        if (lastCompleted != null) {
            val hasPostDeloadSession = sessionDao.hasSessionAfterDeload(lastCompleted.id)
            if (!hasPostDeloadSession) {
                val resetLoads = getResetLoadsForCompletedDeload()
                return DeloadState.DeloadCompleted(resetLoads)
            }
        }

        val deloadAlerts = alertDao.getActiveAlertsByType("MODULE_REQUIRES_DELOAD")
        if (deloadAlerts.isNotEmpty()) {
            val modules = deloadAlerts.mapNotNull { it.moduleCode }.distinct()
            return DeloadState.DeloadRequired(modules)
        }

        return DeloadState.NoDeloadNeeded
    }

    override suspend fun getDeloadIdBySessionId(sessionId: Long): Long? =
        sessionDao.getDeloadIdBySessionId(sessionId)

    override suspend fun countDeloadSessions(deloadId: Long): Int =
        sessionDao.countDeloadSessions(deloadId)

    private suspend fun getResetLoadsForCompletedDeload(): List<ExerciseResetLoad> {
        val progressions = exerciseProgressionDao.getAllWithPrescribedLoad()
        return progressions.mapNotNull { progression ->
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
                moduleCode = dto.moduleCode,
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
            )
        }

        return SessionDetail(
            sessionId = sessionId,
            date = sessionEntity.date,
            moduleCode = summaryInfo.moduleCode,
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
                moduleCode = dto.moduleCode,
                versionNumber = dto.versionNumber,
                avgWeightKg = dto.avgWeightKg,
                totalReps = dto.totalReps,
                avgRir = dto.avgRir,
                classification = dto.classification?.let { parseClassification(it) },
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

    // =========================================================================
    // HU-17: Alert Pipeline Steps 7-11
    // =========================================================================

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
        for (moduleCode in listOf("A", "B", "C")) {
            val sessionIds = sessionDao.getSessionIdsByModuleInRange(moduleCode, 2)
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
                // Resolve + re-insert pattern: ensures the message always
                // reflects the current condition (low vs high) and supports
                // transitions between bothLow ↔ bothHigh without stale data.
                alertDao.resolveByModuleAndType(moduleCode, "RIR_OUT_OF_RANGE", today)
                val isLow = bothLow
                alertDao.insert(
                    AlertEntity(
                        type = "RIR_OUT_OF_RANGE",
                        level = "MEDIUM_ALERT",
                        moduleCode = moduleCode,
                        message = if (isLow) "RIR <1.5 sostenido" else "RIR >3.5 sostenido",
                        isActive = 1,
                        createdAt = today,
                    ),
                )
            } else {
                val bothOptimal = !AlertThresholdRule.isRirOutOfRange(firstAvg) &&
                    !AlertThresholdRule.isRirOutOfRange(secondAvg)
                if (bothOptimal) {
                    alertDao.resolveByModuleAndType(
                        moduleCode,
                        "RIR_OUT_OF_RANGE",
                        today,
                    )
                }
            }
        }
    }

    private suspend fun evaluateLowAdherence(today: String) {
        val profile = profileDao.getProfile().first() ?: return
        val weeklyFrequency = profile.weeklyFrequency

        // CA-17.12: Evaluate the PREVIOUS completed week ("al finalizar una semana natural")
        // The current week is in progress — the user may still complete sessions.
        // Only a fully elapsed week provides a definitive adherence measurement.
        val todayDate = LocalDate.now()
        val prevWeekStart = todayDate.minusWeeks(1).with(DayOfWeek.MONDAY).toString()
        val prevWeekEnd = todayDate.minusWeeks(1).with(DayOfWeek.SUNDAY).toString()
        val prevWeekSessions = sessionDao.countSessionsInWeek(prevWeekStart, prevWeekEnd)
        val prevAdherence = AdherenceRule.calculate(prevWeekSessions, weeklyFrequency)

        if (AlertThresholdRule.isAdherenceLow(prevAdherence)) {
            // CA-17.13: Check 2 weeks ago for crisis (2+ consecutive weeks < 60%)
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
            // CA-17.16: Resolve when the previous completed week has >= 60%
            alertDao.resolveAllByType("LOW_ADHERENCE", today)
        }
    }

    private suspend fun evaluateTonnageDrop(today: String) {
        val closedSessions = sessionDao.getClosedSessionsOrdered()
        // Only compare complete microcycles (exactly 6 sessions each).
        // An incomplete last chunk (e.g., 7 sessions → [6,1]) would produce
        // false tonnage drops because 1 session has far less volume than 6.
        val completeMicrocycles = closedSessions.chunked(6).filter { it.size == 6 }
        if (completeMicrocycles.size < 2) return

        val currentMicrocycle = completeMicrocycles.last()
        val previousMicrocycle = completeMicrocycles[completeMicrocycles.size - 2]
        val isDeloadMicrocycle = currentMicrocycle.any { it.deloadId != null }

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
            val level = AlertThresholdRule.tonnageLevel(dropPercentage, isDeloadMicrocycle)

            if (level != null) {
                alertDao.resolveByMuscleGroupAndType(muscleGroup, "TONNAGE_DROP", today)
                val message = if (isDeloadMicrocycle) {
                    "Descarga planificada — caída esperada"
                } else {
                    "Caída de tonelaje −${dropPercentage.toInt()}%"
                }
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

    private suspend fun evaluateModuleInactivity(today: String, currentModuleCode: String) {
        for (moduleCode in listOf("A", "B", "C")) {
            if (moduleCode == currentModuleCode) {
                alertDao.resolveByModuleAndType(moduleCode, "MODULE_INACTIVITY", today)
                continue
            }

            val lastDate = sessionDao.getLastSessionDateByModule(moduleCode) ?: continue
            val daysSince = ChronoUnit.DAYS.between(
                LocalDate.parse(lastDate),
                LocalDate.now(),
            )
            val level = AlertThresholdRule.inactivityLevel(daysSince)

            if (level != null) {
                val muscleGroups = AlertThresholdRule.MUSCLE_GROUPS_BY_MODULE[moduleCode]
                    ?: emptyList()
                alertDao.resolveByModuleAndType(moduleCode, "MODULE_INACTIVITY", today)
                alertDao.insert(
                    AlertEntity(
                        type = "MODULE_INACTIVITY",
                        level = level,
                        moduleCode = moduleCode,
                        muscleGroup = muscleGroups.joinToString(", "),
                        message = "Módulo $moduleCode: $daysSince días sin sesión",
                        isActive = 1,
                        createdAt = today,
                    ),
                )
            } else {
                alertDao.resolveByModuleAndType(moduleCode, "MODULE_INACTIVITY", today)
            }
        }
    }
}
