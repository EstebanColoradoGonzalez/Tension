package com.estebancoloradogonzalez.tension.data.repository.model

import com.estebancoloradogonzalez.tension.data.local.dao.ExerciseSummaryDto
import com.estebancoloradogonzalez.tension.data.local.dao.SessionExerciseProgressionDao
import com.estebancoloradogonzalez.tension.data.local.dao.SessionPairSummaryDto
import com.estebancoloradogonzalez.tension.data.local.dao.SessionSummaryInfo

data class SessionSummaryData(
    val info: SessionSummaryInfo,
    val exercises: List<ExerciseSummaryDto>,
    /**
     * One entry per (session exercise, implement) trained, as
     * [SessionExerciseProgressionDao.getPairSummariesForSession] returns them (HU-40).
     */
    val pairs: List<SessionPairSummaryDto>,
    val routineRequiresDeload: Boolean,
)
