package com.estebancoloradogonzalez.tension.data.repository

import com.estebancoloradogonzalez.tension.data.local.dao.AlertDao
import com.estebancoloradogonzalez.tension.data.local.dao.ExerciseDao
import com.estebancoloradogonzalez.tension.data.local.dao.ExerciseProgressionDao
import com.estebancoloradogonzalez.tension.data.local.dao.ExerciseSetDao
import com.estebancoloradogonzalez.tension.data.local.dao.PlanAssignmentDao
import com.estebancoloradogonzalez.tension.data.local.dao.ProfileDao
import com.estebancoloradogonzalez.tension.data.local.dao.RoutineDao
import com.estebancoloradogonzalez.tension.data.local.dao.SessionDao
import com.estebancoloradogonzalez.tension.data.local.dao.SessionExerciseDao
import com.estebancoloradogonzalez.tension.data.local.entity.AlertEntity
import com.estebancoloradogonzalez.tension.domain.model.AlertDetail
import com.estebancoloradogonzalez.tension.domain.model.AlertItem
import com.estebancoloradogonzalez.tension.domain.model.AlertTriggerData
import com.estebancoloradogonzalez.tension.domain.model.PlateauCause
import com.estebancoloradogonzalez.tension.domain.model.ProgressionDifficulty
import com.estebancoloradogonzalez.tension.domain.model.SetForTonnage
import com.estebancoloradogonzalez.tension.domain.model.SuggestedAction
import com.estebancoloradogonzalez.tension.domain.repository.AlertRepository
import com.estebancoloradogonzalez.tension.domain.rules.AdherenceRule
import com.estebancoloradogonzalez.tension.domain.rules.AlertNarrativeRule
import com.estebancoloradogonzalez.tension.domain.rules.AlertThresholdRule
import com.estebancoloradogonzalez.tension.domain.rules.AvgRirRule
import com.estebancoloradogonzalez.tension.domain.rules.LoadIncrementResolver
import com.estebancoloradogonzalez.tension.domain.rules.PlateauCausalAnalysisRule
import com.estebancoloradogonzalez.tension.domain.rules.PlateauThresholdRule
import com.estebancoloradogonzalez.tension.domain.rules.ProgressionRateRule
import com.estebancoloradogonzalez.tension.domain.rules.SuggestedActionContext
import com.estebancoloradogonzalez.tension.domain.rules.SuggestedActionRule
import com.estebancoloradogonzalez.tension.domain.rules.TonnageRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class AlertRepositoryImpl @Inject constructor(
    private val alertDao: AlertDao,
    private val sessionDao: SessionDao,
    private val exerciseDao: ExerciseDao,
    private val exerciseSetDao: ExerciseSetDao,
    private val sessionExerciseDao: SessionExerciseDao,
    private val exerciseProgressionDao: ExerciseProgressionDao,
    private val profileDao: ProfileDao,
    private val routineDao: RoutineDao,
    private val planAssignmentDao: PlanAssignmentDao,
) : AlertRepository {

    override fun countActive(): Flow<Int> = alertDao.countActive()

    override fun getActiveAlerts(): Flow<List<AlertItem>> {
        return alertDao.getActiveAlerts().map { entities ->
            entities.map { entity -> mapToAlertItem(entity) }
        }
    }

    private suspend fun mapToAlertItem(entity: AlertEntity): AlertItem {
        val entityName = when {
            entity.exerciseId != null ->
                exerciseDao.getByIdOnce(entity.exerciseId)?.name ?: "Ejercicio"
            entity.routineId != null ->
                routineDao.getById(entity.routineId)?.name ?: "Rutina"
            entity.muscleGroup != null -> entity.muscleGroup
            else -> ""
        }
        return AlertItem(
            alertId = entity.id,
            type = entity.type,
            level = entity.level,
            entityName = entityName,
            message = entity.message,
            createdAt = entity.createdAt,
        )
    }

    override suspend fun getAlertDetail(alertId: Long): AlertDetail {
        val alert = alertDao.getAlertById(alertId)
            ?: throw IllegalArgumentException("Alert not found: $alertId")

        val entityName = when {
            alert.exerciseId != null ->
                exerciseDao.getByIdOnce(alert.exerciseId)?.name ?: "Ejercicio"
            alert.routineId != null ->
                routineDao.getById(alert.routineId)?.name ?: "Rutina"
            alert.muscleGroup != null -> alert.muscleGroup
            else -> ""
        }

        return AlertDetail(
            alertId = alert.id,
            type = alert.type,
            level = alert.level,
            entityName = entityName,
            message = alert.message,
            createdAt = alert.createdAt,
            triggerData = buildTriggerData(alert),
            causalAnalysis = buildCausalAnalysis(alert),
            suggestedAction = buildSuggestedAction(alert),
            exerciseId = alert.exerciseId,
        )
    }

    private suspend fun isRirLowAlert(alert: AlertEntity): Boolean {
        val routineId = alert.routineId ?: return false
        val sessionIds = sessionDao.getSessionIdsByRoutineInRange(routineId, 2)
        if (sessionIds.isEmpty()) return false
        val rirValues = exerciseSetDao.getRirValuesBySessionIds(listOf(sessionIds.first()))
        if (rirValues.isEmpty()) return false
        val avgRir = AvgRirRule.calculate(rirValues)
        return AlertThresholdRule.isRirLow(avgRir)
    }

    private suspend fun buildTriggerData(alert: AlertEntity): AlertTriggerData {
        return when (alert.type) {
            "PLATEAU" -> buildPlateauTrigger(alert)
            "LOW_PROGRESSION_RATE" -> buildProgressionRateTrigger(alert)
            "RIR_OUT_OF_RANGE" -> buildRirTrigger(alert)
            "LOW_ADHERENCE" -> buildAdherenceTrigger()
            "TONNAGE_DROP" -> buildTonnageDropTrigger(alert)
            "ROUTINE_INACTIVITY" -> buildInactivityTrigger(alert)
            "ROUTINE_REQUIRES_DELOAD" -> buildDeloadTrigger(alert)
            else -> AlertTriggerData.ProgressionRateTrigger(rate = 0.0, exerciseName = "")
        }
    }

    private suspend fun buildPlateauTrigger(alert: AlertEntity): AlertTriggerData.PlateauTrigger {
        val exerciseId = alert.exerciseId ?: return AlertTriggerData.PlateauTrigger(emptyList())
        val entries = sessionExerciseDao.getExerciseHistoryEntries(exerciseId)
        val sessions = entries.filter { !it.isDeload }.take(3).map { entry ->
            AlertTriggerData.PlateauSession(
                date = entry.date,
                weightKg = entry.avgWeightKg,
                totalReps = entry.totalReps,
            )
        }
        return AlertTriggerData.PlateauTrigger(sessions)
    }

    private suspend fun buildDeloadTrigger(alert: AlertEntity): AlertTriggerData.DeloadTrigger {
        val routineId = alert.routineId
            ?: return AlertTriggerData.DeloadTrigger(0L, "", emptyList())
        val routineName = routineDao.getById(routineId)?.name ?: ""
        val muscleGroups = planAssignmentDao.getMuscleZoneNamesByRoutineId(routineId)
        return AlertTriggerData.DeloadTrigger(
            routineId = routineId,
            routineName = routineName,
            muscleGroups = muscleGroups,
        )
    }

    private suspend fun buildProgressionRateTrigger(
        alert: AlertEntity,
    ): AlertTriggerData.ProgressionRateTrigger {
        val exerciseId = alert.exerciseId
            ?: return AlertTriggerData.ProgressionRateTrigger(0.0, "")
        val exerciseName = exerciseDao.getByIdOnce(exerciseId)?.name ?: ""
        val startDate = LocalDate.now()
            .minusWeeks(AlertThresholdRule.PROGRESSION_WINDOW_WEEKS)
            .toString()
        val counts = sessionExerciseDao.getClassificationCountsByPeriod(startDate)
        val exerciseCount = counts.find { it.exerciseId == exerciseId }
        val rate = if (exerciseCount != null) {
            ProgressionRateRule.calculate(exerciseCount.positiveCount, exerciseCount.totalCount)
        } else {
            0.0
        }
        return AlertTriggerData.ProgressionRateTrigger(rate = rate, exerciseName = exerciseName)
    }

    private suspend fun buildRirTrigger(alert: AlertEntity): AlertTriggerData.RirTrigger {
        val routineId = alert.routineId
            ?: return AlertTriggerData.RirTrigger(0.0, 0L, "", false)
        val routineName = routineDao.getById(routineId)?.name ?: ""
        val sessionIds = sessionDao.getSessionIdsByRoutineInRange(routineId, 2)
        if (sessionIds.isEmpty()) {
            return AlertTriggerData.RirTrigger(0.0, routineId, routineName, false)
        }
        val rirValues = exerciseSetDao.getRirValuesBySessionIds(sessionIds)
        val avgRir = AvgRirRule.calculate(rirValues)
        val isLow = AlertThresholdRule.isRirLow(avgRir)
        return AlertTriggerData.RirTrigger(
            avgRir = avgRir,
            routineId = routineId,
            routineName = routineName,
            isLow = isLow,
        )
    }

    private suspend fun buildAdherenceTrigger(): AlertTriggerData.AdherenceTrigger {
        val profile = profileDao.getProfile().first()
        val weeklyFrequency = profile?.weeklyFrequency ?: 6
        val today = LocalDate.now()
        val prevWeekStart = today.minusWeeks(1).with(DayOfWeek.MONDAY).toString()
        val prevWeekEnd = today.minusWeeks(1).with(DayOfWeek.SUNDAY).toString()
        val completedSessions = sessionDao.countSessionsInWeek(prevWeekStart, prevWeekEnd)
        val percentage = AdherenceRule.calculate(completedSessions, weeklyFrequency)

        val consecutiveWeeks = countConsecutiveLowAdherenceWeeks(today, weeklyFrequency)

        return AlertTriggerData.AdherenceTrigger(
            percentage = percentage,
            completedSessions = completedSessions,
            plannedSessions = weeklyFrequency,
            consecutiveWeeks = consecutiveWeeks,
        )
    }

    private suspend fun buildTonnageDropTrigger(
        alert: AlertEntity,
    ): AlertTriggerData.TonnageDropTrigger {
        val muscleGroup = alert.muscleGroup
            ?: return AlertTriggerData.TonnageDropTrigger("", 0.0, 0.0, 0.0, false)
        val closedSessions = sessionDao.getClosedSessionsOrdered()
        val nonDeloadSessions = closedSessions.filter { it.deloadId == null }
        val routineCount = routineDao.countRoutines()
        val cycleSize = if (routineCount > 0) routineCount else 1
        val completeMicrocycles = nonDeloadSessions.chunked(cycleSize).filter { it.size == cycleSize }
        if (completeMicrocycles.size < 2) {
            return AlertTriggerData.TonnageDropTrigger(muscleGroup, 0.0, 0.0, 0.0, false)
        }
        val currentMicrocycle = completeMicrocycles.last()
        val previousMicrocycle = completeMicrocycles[completeMicrocycles.size - 2]
        val currentMicrocycleDates = currentMicrocycle.map { it.date }.toSet()
        val isDeload = closedSessions.any {
            it.deloadId != null && it.date in currentMicrocycleDates
        }

        val currentTonnageData = exerciseSetDao.getTonnageDataBySessionIds(
            currentMicrocycle.map { it.id },
        )
        val currentTonnage = TonnageRule.calculateForMuscleGroup(
            currentTonnageData.map { SetForTonnage(it.weightKg, it.reps, it.muscleGroup) },
        )
        val previousTonnageData = exerciseSetDao.getTonnageDataBySessionIds(
            previousMicrocycle.map { it.id },
        )
        val previousTonnage = TonnageRule.calculateForMuscleGroup(
            previousTonnageData.map { SetForTonnage(it.weightKg, it.reps, it.muscleGroup) },
        )

        val prevValue = previousTonnage[muscleGroup] ?: 0.0
        val currValue = currentTonnage[muscleGroup] ?: 0.0
        val dropPercentage = if (prevValue > 0) {
            ((prevValue - currValue) / prevValue) * 100.0
        } else {
            0.0
        }

        return AlertTriggerData.TonnageDropTrigger(
            muscleGroup = muscleGroup,
            dropPercentage = dropPercentage,
            previousTonnage = prevValue,
            currentTonnage = currValue,
            isDeloadContextualized = isDeload,
        )
    }

    private suspend fun buildInactivityTrigger(
        alert: AlertEntity,
    ): AlertTriggerData.InactivityTrigger {
        val routineId = alert.routineId
            ?: return AlertTriggerData.InactivityTrigger(0L, "", 0, emptyList())
        val routineName = routineDao.getById(routineId)?.name ?: ""
        val lastDate = sessionDao.getLastSessionDateByRoutine(routineId)
        val fallbackDate = routineDao.getById(routineId)?.createdAt
        val referenceDate = lastDate ?: fallbackDate
        val daysSince = if (referenceDate != null) {
            ChronoUnit.DAYS.between(LocalDate.parse(referenceDate), LocalDate.now())
        } else {
            0L
        }
        val muscleGroups = planAssignmentDao.getMuscleZoneNamesByRoutineId(routineId)
        return AlertTriggerData.InactivityTrigger(
            routineId = routineId,
            routineName = routineName,
            daysSinceLastSession = daysSince,
            muscleGroups = muscleGroups,
        )
    }

    private suspend fun buildCausalAnalysis(alert: AlertEntity): String {
        return when (alert.type) {
            "PLATEAU" -> buildPlateauCausalAnalysis(alert)
            "ROUTINE_REQUIRES_DELOAD" -> buildDeloadCausalAnalysis(alert)
            "LOW_PROGRESSION_RATE" -> buildProgressionRateCausalAnalysis(alert)
            "RIR_OUT_OF_RANGE" -> buildRirCausalAnalysis(alert)
            "LOW_ADHERENCE" -> buildAdherenceCausalAnalysis()
            "TONNAGE_DROP" -> buildTonnageCausalAnalysis(alert)
            "ROUTINE_INACTIVITY" -> buildInactivityCausalAnalysis(alert)
            else -> ""
        }
    }

    private suspend fun buildProgressionRateCausalAnalysis(alert: AlertEntity): String {
        val trigger = buildProgressionRateTrigger(alert)
        val difficulty = ProgressionDifficulty.fromCode(
            alert.exerciseId?.let { exerciseDao.getByIdOnce(it)?.progressionDifficulty },
        )
        return AlertNarrativeRule.progressionRateExplanation(
            exerciseName = trigger.exerciseName,
            rate = trigger.rate.toInt(),
            difficulty = difficulty,
            isCritical = alert.level == "CRISIS",
        )
    }

    private suspend fun buildRirCausalAnalysis(alert: AlertEntity): String {
        val trigger = buildRirTrigger(alert)
        return AlertNarrativeRule.rirExplanation(
            routineName = trigger.routineName,
            avgRir = trigger.avgRir,
            isLow = trigger.isLow,
        )
    }

    private suspend fun buildAdherenceCausalAnalysis(): String {
        val trigger = buildAdherenceTrigger()
        return AlertNarrativeRule.adherenceExplanation(
            percentage = trigger.percentage.toInt(),
            consecutiveWeeks = trigger.consecutiveWeeks,
        )
    }

    private suspend fun buildTonnageCausalAnalysis(alert: AlertEntity): String {
        val trigger = buildTonnageDropTrigger(alert)
        return AlertNarrativeRule.tonnageExplanation(
            muscleGroup = trigger.muscleGroup,
            dropPercentage = trigger.dropPercentage.toInt(),
            isDeload = trigger.isDeloadContextualized,
        )
    }

    private suspend fun buildInactivityCausalAnalysis(alert: AlertEntity): String {
        val trigger = buildInactivityTrigger(alert)
        return AlertNarrativeRule.inactivityExplanation(
            routineName = trigger.routineName,
            days = trigger.daysSinceLastSession,
            muscleGroups = trigger.muscleGroups,
        )
    }

    private suspend fun buildPlateauCausalAnalysis(alert: AlertEntity): String {
        val exerciseId = alert.exerciseId
            ?: return AlertNarrativeRule.plateauExplanation(
                exerciseName = "",
                sessions = 0,
                difficulty = ProgressionDifficulty.MEDIUM,
                cause = PlateauCause.MIXED,
            )
        val exercise = exerciseDao.getByIdOnce(exerciseId)
        val difficulty = ProgressionDifficulty.fromCode(exercise?.progressionDifficulty)
        val entries = sessionExerciseDao.getExerciseHistoryEntries(exerciseId)
        val lastRirs = entries.filter { !it.isDeload }.take(3).map { it.avgRir }

        val routineId = alert.routineId
        val isGroupStagnant = if (routineId != null) {
            val sessionIds = sessionDao.getSessionIdsByRoutineInRange(routineId, 4)
            if (sessionIds.size >= 2) {
                val counts = sessionExerciseDao.getClassificationCountsForSessions(sessionIds)
                val totalPositive = counts.sumOf { it.positiveCount }
                val totalCount = counts.sumOf { it.totalCount }
                val rate = ProgressionRateRule.calculate(totalPositive, totalCount)
                AlertThresholdRule.isProgressionAlert(rate, difficulty)
            } else {
                false
            }
        } else {
            false
        }

        return AlertNarrativeRule.plateauExplanation(
            exerciseName = exercise?.name ?: "",
            sessions = resolvePlateauThreshold(exerciseId),
            difficulty = difficulty,
            cause = PlateauCausalAnalysisRule.analyze(lastRirs, isGroupStagnant),
        )
    }

    private suspend fun buildDeloadCausalAnalysis(alert: AlertEntity): String {
        val routineId = alert.routineId
            ?: return AlertNarrativeRule.deloadExplanation("", 0)
        val routineName = routineDao.getById(routineId)?.name ?: ""
        val sessionIds = sessionDao.getSessionIdsByRoutineInRange(routineId, 4)
        if (sessionIds.size < 2) {
            return AlertNarrativeRule.deloadExplanation(routineName, 0)
        }
        val counts = sessionExerciseDao.getClassificationCountsForSessions(sessionIds)
        val totalNonPositive = counts.sumOf { it.totalCount - it.positiveCount }
        val totalCount = counts.sumOf { it.totalCount }
        val regressPct = if (totalCount > 0) (totalNonPositive * 100 / totalCount) else 0
        return AlertNarrativeRule.deloadExplanation(routineName, regressPct)
    }

    /**
     * The block of what the executant can do. Which action applies, and whether it can
     * actually be carried out, is decided by [SuggestedActionRule]; this only gathers
     * the context that rule needs and hands the chosen action to the narrative.
     */
    private suspend fun buildSuggestedAction(alert: AlertEntity): SuggestedAction {
        val exercise = alert.exerciseId?.let { exerciseDao.getByIdOnce(it) }
        val routineName = alert.routineId?.let { routineDao.getById(it)?.name } ?: ""
        val sessionsWithoutProgression = alert.exerciseId?.let {
            exerciseProgressionDao.getByExerciseId(it).first()?.sessionsWithoutProgression
        } ?: 0

        val context = SuggestedActionContext(
            alertType = alert.type,
            level = alert.level,
            exerciseId = alert.exerciseId,
            hasSlotAlternative = alert.exerciseId?.let {
                planAssignmentDao.hasSlotAlternative(it)
            } ?: false,
            sessionsWithoutProgression = sessionsWithoutProgression,
            plateauThreshold = alert.exerciseId?.let { resolvePlateauThreshold(it) }
                ?: PlateauThresholdRule.DEFAULT_BASE_THRESHOLD,
            isRirLow = alert.type == "RIR_OUT_OF_RANGE" && isRirLowAlert(alert),
        )
        val (kind, target) = SuggestedActionRule.resolve(context)

        val muscleGroup = alert.exerciseId?.let {
            sessionExerciseDao.getPrimaryMuscleGroupByExercise(it)
        }
        return SuggestedAction(
            kind = kind,
            text = AlertNarrativeRule.suggestedActionText(
                kind = kind,
                exerciseName = exercise?.name ?: "",
                routineName = routineName,
                incrementKg = LoadIncrementResolver.resolve(muscleGroup),
            ),
            target = target,
        )
    }

    /** Effective plateau threshold of an exercise: base threshold scaled by its difficulty. */
    private suspend fun resolvePlateauThreshold(exerciseId: Long): Int {
        val baseThreshold = profileDao.getProfile().first()?.plateauBaseThreshold
            ?: PlateauThresholdRule.DEFAULT_BASE_THRESHOLD
        val difficulty = ProgressionDifficulty.fromCode(
            exerciseDao.getByIdOnce(exerciseId)?.progressionDifficulty,
        )
        return PlateauThresholdRule.effectiveThreshold(baseThreshold, difficulty)
    }

    /** Consecutive weeks below the adherence threshold, counting back from last week. */
    private suspend fun countConsecutiveLowAdherenceWeeks(
        today: LocalDate,
        weeklyFrequency: Int,
    ): Int {
        return (1..AlertThresholdRule.ADHERENCE_LOOKBACK_WEEKS)
            .map { weeksAgo ->
                val weekDate = today.minusWeeks(weeksAgo.toLong())
                AdherenceRule.calculate(
                    sessionDao.countSessionsInWeek(
                        weekDate.with(DayOfWeek.MONDAY).toString(),
                        weekDate.with(DayOfWeek.SUNDAY).toString(),
                    ),
                    weeklyFrequency,
                )
            }
            .takeWhile { AlertThresholdRule.isAdherenceLow(it) }
            .size
    }
}
