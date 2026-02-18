package com.estebancoloradogonzalez.tension.domain.repository

import com.estebancoloradogonzalez.tension.domain.model.ClassificationCount
import com.estebancoloradogonzalez.tension.domain.model.ClassificationCountByGroup
import com.estebancoloradogonzalez.tension.domain.model.ExerciseSessionRange
import com.estebancoloradogonzalez.tension.domain.model.SetDistributionData
import com.estebancoloradogonzalez.tension.domain.model.SetTonnageData

interface MetricsRepository {

    suspend fun getSessionsCompletedInWeek(weekStartDate: String, weekEndDate: String): Int

    suspend fun getWeeklyFrequency(): Int

    suspend fun getRirValuesByModule(moduleCode: String, sessionLimit: Int): List<Int>

    suspend fun getClassificationCounts(startDate: String): List<ClassificationCount>

    suspend fun getClassificationCountsBySessionIds(
        sessionIds: List<Long>,
    ): List<ClassificationCountByGroup>

    suspend fun getExerciseSessionRanges(startDate: String): List<ExerciseSessionRange>

    suspend fun getAvgWeightForExerciseInSession(exerciseId: Long, sessionId: Long): Double?

    suspend fun getSessionIdsGroupedByMicrocycle(): Map<Int, List<Long>>

    suspend fun getTonnageDataBySessionIds(sessionIds: List<Long>): List<SetTonnageData>

    suspend fun getSetDistributionBySessionIds(sessionIds: List<Long>): List<SetDistributionData>
}
