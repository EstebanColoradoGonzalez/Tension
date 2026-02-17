package com.estebancoloradogonzalez.tension.data.repository.model

import com.estebancoloradogonzalez.tension.data.local.dao.ExerciseSummaryDto
import com.estebancoloradogonzalez.tension.data.local.dao.SessionSummaryInfo

data class SessionSummaryData(
    val info: SessionSummaryInfo,
    val exercises: List<ExerciseSummaryDto>,
    val moduleRequiresDeload: Boolean,
)
