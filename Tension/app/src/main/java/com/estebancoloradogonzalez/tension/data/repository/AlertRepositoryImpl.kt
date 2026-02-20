package com.estebancoloradogonzalez.tension.data.repository

import com.estebancoloradogonzalez.tension.data.local.dao.AlertDao
import com.estebancoloradogonzalez.tension.data.local.dao.ExerciseDao
import com.estebancoloradogonzalez.tension.data.local.dao.ExerciseProgressionDao
import com.estebancoloradogonzalez.tension.data.local.dao.ExerciseSetDao
import com.estebancoloradogonzalez.tension.data.local.dao.ProfileDao
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
            entity.moduleCode != null -> "Módulo ${entity.moduleCode}"
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
            alert.moduleCode != null -> "Módulo ${alert.moduleCode}"
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

        val showDeloadLink = alert.type == "MODULE_REQUIRES_DELOAD" ||
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
        val moduleCode = alert.moduleCode ?: return false
        val sessionIds = sessionDao.getSessionIdsByModuleInRange(moduleCode, 2)
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
            "MODULE_INACTIVITY" -> buildInactivityTrigger(alert)
            "MODULE_REQUIRES_DELOAD" -> buildPlateauTrigger(alert)
            else -> AlertTriggerData.ProgressionRateTrigger(rate = 0.0, exerciseName = "")
        }
    }

    private suspend fun buildPlateauTrigger(alert: AlertEntity): AlertTriggerData.PlateauTrigger {
        val exerciseId = alert.exerciseId ?: return AlertTriggerData.PlateauTrigger(emptyList())
        val entries = sessionExerciseDao.getExerciseHistoryEntries(exerciseId)
        val sessions = entries.take(3).map { entry ->
            AlertTriggerData.PlateauSession(
                date = entry.date,
                weightKg = entry.avgWeightKg,
                totalReps = entry.totalReps,
            )
        }
        return AlertTriggerData.PlateauTrigger(sessions)
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
        val moduleCode = alert.moduleCode ?: return AlertTriggerData.RirTrigger(0.0, "", false)
        val sessionIds = sessionDao.getSessionIdsByModuleInRange(moduleCode, 2)
        if (sessionIds.isEmpty()) return AlertTriggerData.RirTrigger(0.0, moduleCode, false)
        val rirValues = exerciseSetDao.getRirValuesBySessionIds(sessionIds)
        val avgRir = AvgRirRule.calculate(rirValues)
        val isLow = AlertThresholdRule.isRirLow(avgRir)
        return AlertTriggerData.RirTrigger(
            avgRir = avgRir,
            moduleCode = moduleCode,
            isLow = isLow,
        )
    }

    private suspend fun buildAdherenceTrigger(): AlertTriggerData.AdherenceTrigger {
        val profile = profileDao.getProfile().first()
        val weeklyFrequency = profile?.weeklyFrequency ?: 6
        // Consistent with pipeline: show PREVIOUS completed week data
        // (the week that actually triggered the alert)
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
        // Only use complete microcycles (exactly 6 sessions) — consistent with pipeline
        val completeMicrocycles = closedSessions.chunked(6).filter { it.size == 6 }
        if (completeMicrocycles.size < 2) {
            return AlertTriggerData.TonnageDropTrigger(muscleGroup, 0.0, 0.0, 0.0, false)
        }
        val currentMicrocycle = completeMicrocycles.last()
        val previousMicrocycle = completeMicrocycles[completeMicrocycles.size - 2]
        val isDeload = currentMicrocycle.any { it.deloadId != null }

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
        val moduleCode = alert.moduleCode
            ?: return AlertTriggerData.InactivityTrigger("", 0, emptyList())
        val lastDate = sessionDao.getLastSessionDateByModule(moduleCode)
        val daysSince = if (lastDate != null) {
            ChronoUnit.DAYS.between(LocalDate.parse(lastDate), LocalDate.now())
        } else {
            0L
        }
        val muscleGroups = AlertThresholdRule.MUSCLE_GROUPS_BY_MODULE[moduleCode] ?: emptyList()
        return AlertTriggerData.InactivityTrigger(
            moduleCode = moduleCode,
            daysSinceLastSession = daysSince,
            muscleGroups = muscleGroups,
        )
    }

    private suspend fun buildCausalAnalysis(alert: AlertEntity): String {
        return when (alert.type) {
            "PLATEAU", "MODULE_REQUIRES_DELOAD" -> buildPlateauCausalAnalysis(alert)
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
                val moduleCode = alert.moduleCode ?: ""
                val sessionIds = sessionDao.getSessionIdsByModuleInRange(moduleCode, 2)
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
                            "recomienda incrementar la carga de los ejercicios del módulo."
                    }
                } else {
                    "RIR fuera de rango sostenido en el módulo."
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
                val microcycles = closedSessions.chunked(6)
                val isDeload = if (microcycles.isNotEmpty()) {
                    microcycles.last().any { it.deloadId != null }
                } else {
                    false
                }
                if (isDeload) {
                    "Descarga planificada — caída de tonelaje esperada y controlada."
                } else {
                    "Caída no intencional de tonelaje. Evaluar causas y considerar ajustar " +
                        "el volumen de entrenamiento."
                }
            }
            "MODULE_INACTIVITY" -> {
                val moduleCode = alert.moduleCode ?: ""
                val lastDate = sessionDao.getLastSessionDateByModule(moduleCode)
                val daysSince = if (lastDate != null) {
                    ChronoUnit.DAYS.between(LocalDate.parse(lastDate), LocalDate.now())
                } else {
                    0L
                }
                val muscleGroups = AlertThresholdRule.MUSCLE_GROUPS_BY_MODULE[moduleCode]
                    ?.joinToString(", ") ?: ""
                "Módulo $moduleCode lleva $daysSince días sin sesión. Los grupos musculares " +
                    "asociados ($muscleGroups) pueden estar perdiendo adaptaciones."
            }
            else -> ""
        }
    }

    private suspend fun buildPlateauCausalAnalysis(alert: AlertEntity): String {
        val exerciseId = alert.exerciseId ?: return "Meseta detectada."
        val entries = sessionExerciseDao.getExerciseHistoryEntries(exerciseId)
        val lastRirs = entries.take(3).map { it.avgRir }

        val moduleCode = alert.moduleCode ?: ""
        val sessionIds = if (moduleCode.isNotEmpty()) {
            sessionDao.getSessionIdsByModuleInRange(moduleCode, 4)
        } else {
            emptyList()
        }
        val isGroupStagnant = if (sessionIds.size >= 2) {
            val counts = sessionExerciseDao.getClassificationCountsByPeriod(
                LocalDate.now().minusWeeks(4).toString(),
            )
            val totalPositive = counts.sumOf { it.positiveCount }
            val totalCount = counts.sumOf { it.totalCount }
            val rate = ProgressionRateRule.calculate(totalPositive, totalCount)
            rate < AlertThresholdRule.PROGRESSION_ALERT_THRESHOLD
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

    private suspend fun buildRecommendations(alert: AlertEntity): List<String> {
        return when (alert.type) {
            "PLATEAU", "MODULE_REQUIRES_DELOAD" -> {
                val exerciseId = alert.exerciseId
                val progression = exerciseId?.let {
                    exerciseProgressionDao.getByExerciseId(it).first()
                }
                val sessionsWithout = progression?.sessionsWithoutProgression ?: 0
                CorrectiveActionRule.recommend(sessionsWithout).map { action ->
                    when (action) {
                        CorrectiveAction.MICRO_INCREMENT_OR_EXTEND_REPS ->
                            "Intentar microincremento (+2.5 Kg) o extensión de reps"
                        CorrectiveAction.ROTATE_VERSION ->
                            "Considerar rotar versión del módulo"
                    }
                }
            }
            "LOW_PROGRESSION_RATE" -> listOf(
                "Evaluar si la carga o el volumen necesitan ajuste",
                "Considerar variar el rango de repeticiones",
            )
            "RIR_OUT_OF_RANGE" -> {
                val isLow = isRirLowAlert(alert)
                if (isLow) {
                    listOf("Considerar prescribir una descarga para permitir recuperación")
                } else {
                    listOf("Incrementar la carga de los ejercicios del módulo")
                }
            }
            "LOW_ADHERENCE" -> listOf(
                "Incrementar la frecuencia de entrenamiento semanal",
            )
            "TONNAGE_DROP" -> listOf(
                "Evaluar causas de la caída de tonelaje",
                "Considerar ajustar el volumen de entrenamiento",
            )
            "MODULE_INACTIVITY" -> {
                val moduleCode = alert.moduleCode ?: ""
                listOf("Priorizar el módulo $moduleCode en las próximas sesiones")
            }
            else -> emptyList()
        }
    }
}
