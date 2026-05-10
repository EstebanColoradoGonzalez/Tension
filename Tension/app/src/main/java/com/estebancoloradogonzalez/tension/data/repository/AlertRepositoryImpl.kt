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
import com.estebancoloradogonzalez.tension.domain.model.CorrectiveAction
import com.estebancoloradogonzalez.tension.domain.model.SetForTonnage
import com.estebancoloradogonzalez.tension.domain.repository.AlertRepository
import com.estebancoloradogonzalez.tension.domain.rules.AdherenceRule
import com.estebancoloradogonzalez.tension.domain.rules.AlertThresholdRule
import com.estebancoloradogonzalez.tension.domain.rules.AvgRirRule
import com.estebancoloradogonzalez.tension.domain.rules.CorrectiveActionRule
import com.estebancoloradogonzalez.tension.domain.rules.LoadIncrementResolver
import com.estebancoloradogonzalez.tension.domain.rules.PlateauCausalAnalysisRule
import com.estebancoloradogonzalez.tension.domain.rules.ProgressionRateRule
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

        val triggerData = buildTriggerData(alert)
        val causalAnalysis = buildCausalAnalysis(alert)
        val recommendations = buildRecommendations(alert)

        val showExerciseHistoryLink = alert.type in listOf(
            "PLATEAU",
            "LOW_PROGRESSION_RATE",
        ) && alert.exerciseId != null

        val showDeloadLink = alert.type == "ROUTINE_REQUIRES_DELOAD" ||
            (alert.type == "RIR_OUT_OF_RANGE" && isRirLowAlert(alert))

        return AlertDetail(
            alertId = alert.id,
            type = alert.type,
            level = alert.level,
            entityName = entityName,
            message = alert.message,
            createdAt = alert.createdAt,
            triggerData = triggerData,
            causalAnalysis = causalAnalysis,
            recommendations = recommendations,
            showExerciseHistoryLink = showExerciseHistoryLink,
            showDeloadLink = showDeloadLink,
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
        val startDate = LocalDate.now().minusWeeks(4).toString()
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

        val twoWeeksAgoStart = today.minusWeeks(2).with(DayOfWeek.MONDAY).toString()
        val twoWeeksAgoEnd = today.minusWeeks(2).with(DayOfWeek.SUNDAY).toString()
        val twoWeeksAgoSessions = sessionDao.countSessionsInWeek(
            twoWeeksAgoStart,
            twoWeeksAgoEnd,
        )
        val twoWeeksAgoPercentage = AdherenceRule.calculate(twoWeeksAgoSessions, weeklyFrequency)
        val consecutiveWeeks =
            if (AlertThresholdRule.isAdherenceLow(twoWeeksAgoPercentage)) 2 else 1

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
            "LOW_PROGRESSION_RATE" -> {
                if (alert.level == "CRISIS") {
                    "Tasa de progresión en estado crítico. El ejercicio muestra estancamiento " +
                        "prolongado que compromete las adaptaciones."
                } else {
                    "Tasa de progresión por debajo del umbral de alerta. El ejercicio muestra " +
                        "estancamiento sostenido."
                }
            }
            "RIR_OUT_OF_RANGE" -> {
                val routineId = alert.routineId ?: return "RIR fuera de rango sostenido."
                val sessionIds = sessionDao.getSessionIdsByRoutineInRange(routineId, 2)
                if (sessionIds.isNotEmpty()) {
                    val rirValues = exerciseSetDao.getRirValuesBySessionIds(
                        listOf(sessionIds.first()),
                    )
                    val avgRir = AvgRirRule.calculate(rirValues)
                    if (AlertThresholdRule.isRirLow(avgRir)) {
                        "El ejecutante está entrenando demasiado cerca del fallo técnico de " +
                            "forma sostenida. Se recomienda prescribir una descarga para " +
                            "permitir recuperación del SNC."
                    } else {
                        "El estímulo puede ser insuficiente para generar adaptación. Se " +
                            "recomienda incrementar la carga de los ejercicios de la rutina."
                    }
                } else {
                    "RIR fuera de rango sostenido en la rutina."
                }
            }
            "LOW_ADHERENCE" -> {
                if (alert.level == "CRISIS") {
                    "Adherencia por debajo del 60% durante semanas consecutivas. Las " +
                        "comparaciones entre sesiones pierden validez por el excesivo tiempo " +
                        "entre ellas."
                } else {
                    "Adherencia por debajo del 60% esta semana. La baja frecuencia puede " +
                        "afectar la resolución temporal de las señales del sistema."
                }
            }
            "TONNAGE_DROP" -> {
                val closedSessions = sessionDao.getClosedSessionsOrdered()
                    .filter { it.deloadId == null }
                val routineCount = routineDao.countRoutines()
                val cycleSize = if (routineCount > 0) routineCount else 1
                val microcycles = closedSessions.chunked(cycleSize)
                val currentMicrocycleDates = if (microcycles.isNotEmpty()) {
                    microcycles.last().map { it.date }.toSet()
                } else {
                    emptySet()
                }
                val allSessions = sessionDao.getClosedSessionsOrdered()
                val isDeload = allSessions.any {
                    it.deloadId != null && it.date in currentMicrocycleDates
                }
                if (isDeload) {
                    "Descarga planificada — caída de tonelaje esperada y controlada."
                } else {
                    "Caída no intencional de tonelaje. Evaluar causas y considerar ajustar " +
                        "el volumen de entrenamiento."
                }
            }
            "ROUTINE_INACTIVITY" -> {
                val routineId = alert.routineId ?: return ""
                val routine = routineDao.getById(routineId)
                val routineName = routine?.name ?: ""
                val lastDate = sessionDao.getLastSessionDateByRoutine(routineId)
                val referenceDate = lastDate ?: routine?.createdAt
                val daysSince = if (referenceDate != null) {
                    ChronoUnit.DAYS.between(LocalDate.parse(referenceDate), LocalDate.now())
                } else {
                    0L
                }
                val muscleGroups = planAssignmentDao.getMuscleZoneNamesByRoutineId(routineId)
                    .joinToString(", ")
                "Rutina $routineName lleva $daysSince días sin sesión. Los grupos musculares " +
                    "asociados ($muscleGroups) pueden estar perdiendo adaptaciones."
            }
            else -> ""
        }
    }

    private suspend fun buildPlateauCausalAnalysis(alert: AlertEntity): String {
        val exerciseId = alert.exerciseId ?: return "Meseta detectada."
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
                rate < AlertThresholdRule.PROGRESSION_ALERT_THRESHOLD
            } else {
                false
            }
        } else {
            false
        }

        val cause = PlateauCausalAnalysisRule.analyze(lastRirs, isGroupStagnant)
        return when (cause) {
            com.estebancoloradogonzalez.tension.domain.model.PlateauCause.LOW_RIR_LIMIT ->
                "Entrenando cerca del fallo técnico — posible límite de carga."
            com.estebancoloradogonzalez.tension.domain.model.PlateauCause.HIGH_RIR_CONSERVATIVE ->
                "Carga conservadora — margen para incrementar."
            com.estebancoloradogonzalez.tension.domain.model.PlateauCause.GROUP_STAGNATION ->
                "Fatiga sistémica del grupo muscular."
            com.estebancoloradogonzalez.tension.domain.model.PlateauCause.MIXED ->
                "Meseta detectada — evaluar causas posibles."
        }
    }

    private suspend fun buildDeloadCausalAnalysis(alert: AlertEntity): String {
        val routineId = alert.routineId
            ?: return "Se detectó necesidad de descarga."
        val routineName = routineDao.getById(routineId)?.name ?: "rutina"
        val sessionIds = sessionDao.getSessionIdsByRoutineInRange(routineId, 4)
        if (sessionIds.size < 2) {
            return "Rutina $routineName muestra signos de fatiga acumulada."
        }
        val counts = sessionExerciseDao.getClassificationCountsForSessions(sessionIds)
        val totalNonPositive = counts.sumOf { it.totalCount - it.positiveCount }
        val totalCount = counts.sumOf { it.totalCount }
        val regressPct = if (totalCount > 0) (totalNonPositive * 100 / totalCount) else 0
        return "Rutina $routineName presenta regresión o estancamiento en $regressPct% de ejercicios " +
            "en sesiones recientes. Fatiga acumulada — se recomienda activar descarga."
    }

    private suspend fun buildRecommendations(alert: AlertEntity): List<String> {
        return when (alert.type) {
            "PLATEAU" -> {
                val exerciseId = alert.exerciseId
                val progression = exerciseId?.let {
                    exerciseProgressionDao.getByExerciseId(it).first()
                }
                val sessionsWithout = progression?.sessionsWithoutProgression ?: 0
                val muscleGroup = exerciseId?.let {
                    sessionExerciseDao.getPrimaryMuscleGroupByExercise(it)
                }
                val increment = LoadIncrementResolver.resolve(muscleGroup ?: "")
                val incrementText = if (increment == 5.0) "+5.0 Kg" else "+2.5 Kg"
                CorrectiveActionRule.recommend(sessionsWithout).map { action ->
                    when (action) {
                        CorrectiveAction.MICRO_INCREMENT_OR_EXTEND_REPS ->
                            "Intentar microincremento ($incrementText) o extensión de reps"
                        CorrectiveAction.ROTATE_VERSION -> {
                            val routineId = alert.routineId
                            val routineName = routineId?.let { routineDao.getById(it)?.name }
                            if (routineName != null) {
                                "Considerar rotar a otra versión de la rutina \"$routineName\""
                            } else {
                                "Considerar rotar versión de la rutina"
                            }
                        }
                    }
                }
            }
            "ROUTINE_REQUIRES_DELOAD" -> listOf(
                "Activar protocolo de descarga para esta rutina",
                "Reducir cargas al 60% durante el próximo microciclo",
            )
            "LOW_PROGRESSION_RATE" -> listOf(
                "Evaluar si la carga o el volumen necesitan ajuste",
                "Considerar variar el rango de repeticiones",
            )
            "RIR_OUT_OF_RANGE" -> {
                val isLow = isRirLowAlert(alert)
                if (isLow) {
                    listOf("Considerar prescribir una descarga para permitir recuperación")
                } else {
                    listOf("Incrementar la carga de los ejercicios de la rutina")
                }
            }
            "LOW_ADHERENCE" -> listOf(
                "Incrementar la frecuencia de entrenamiento semanal",
            )
            "TONNAGE_DROP" -> listOf(
                "Evaluar causas de la caída de tonelaje",
                "Considerar ajustar el volumen de entrenamiento",
            )
            "ROUTINE_INACTIVITY" -> {
                val routineName = alert.routineId?.let {
                    routineDao.getById(it)?.name
                } ?: "la rutina"
                listOf("Priorizar la rutina $routineName en las próximas sesiones")
            }
            else -> emptyList()
        }
    }
}
