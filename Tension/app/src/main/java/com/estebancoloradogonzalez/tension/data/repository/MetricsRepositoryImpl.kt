package com.estebancoloradogonzalez.tension.data.repository

import com.estebancoloradogonzalez.tension.data.local.dao.ExerciseSetDao
import com.estebancoloradogonzalez.tension.data.local.dao.ProfileDao
import com.estebancoloradogonzalez.tension.data.local.dao.SessionDao
import com.estebancoloradogonzalez.tension.data.local.dao.SessionExerciseDao
import com.estebancoloradogonzalez.tension.domain.model.ClassificationCount
import com.estebancoloradogonzalez.tension.domain.model.ClassificationCountByGroup
import com.estebancoloradogonzalez.tension.domain.model.ExerciseSessionRange
import com.estebancoloradogonzalez.tension.domain.model.SetDistributionData
import com.estebancoloradogonzalez.tension.domain.model.SetTonnageData
import com.estebancoloradogonzalez.tension.domain.repository.MetricsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class MetricsRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
    private val exerciseSetDao: ExerciseSetDao,
    private val sessionExerciseDao: SessionExerciseDao,
    private val profileDao: ProfileDao,
) : MetricsRepository {

    override suspend fun getSessionsCompletedInWeek(
        weekStartDate: String,
        weekEndDate: String,
    ): Int = sessionDao.countSessionsInWeek(weekStartDate, weekEndDate)

    override suspend fun getWeeklyFrequency(): Int {
        val profile = profileDao.getProfile().first()
        return profile?.weeklyFrequency ?: 6
    }

    override suspend fun getRirValuesByModule(
        moduleCode: String,
        sessionLimit: Int,
    ): List<Int> {
        val sessionIds = sessionDao.getSessionIdsByModuleInRange(moduleCode, sessionLimit)
        if (sessionIds.isEmpty()) return emptyList()
        return exerciseSetDao.getRirValuesBySessionIds(sessionIds)
    }

    override suspend fun getClassificationCounts(
        startDate: String,
    ): List<ClassificationCount> =
        sessionExerciseDao.getClassificationCountsByPeriod(startDate)

    override suspend fun getClassificationCountsBySessionIds(
        sessionIds: List<Long>,
    ): List<ClassificationCountByGroup> {
        if (sessionIds.isEmpty()) return emptyList()
        return sessionExerciseDao.getClassificationCountsBySessionIds(sessionIds)
    }

    override suspend fun getExerciseSessionRanges(
        startDate: String,
    ): List<ExerciseSessionRange> =
        sessionExerciseDao.getExerciseSessionRangeByPeriod(startDate)

    override suspend fun getAvgWeightForExerciseInSession(
        exerciseId: Long,
        sessionId: Long,
    ): Double? = exerciseSetDao.getAvgWeightByExerciseInSession(exerciseId, sessionId)

    override suspend fun getSessionIdsGroupedByMicrocycle(): Map<Int, List<Long>> {
        val sessions = sessionDao.getClosedSessionsOrdered()
        return sessions.chunked(6).mapIndexed { index, chunk ->
            (index + 1) to chunk.map { it.id }
        }.toMap()
    }

    override suspend fun getTonnageDataBySessionIds(
        sessionIds: List<Long>,
    ): List<SetTonnageData> {
        if (sessionIds.isEmpty()) return emptyList()
        return exerciseSetDao.getTonnageDataBySessionIds(sessionIds)
    }

    override suspend fun getSetDistributionBySessionIds(
        sessionIds: List<Long>,
    ): List<SetDistributionData> {
        if (sessionIds.isEmpty()) return emptyList()
        return exerciseSetDao.getSetDistributionBySessionIds(sessionIds)
    }
}
