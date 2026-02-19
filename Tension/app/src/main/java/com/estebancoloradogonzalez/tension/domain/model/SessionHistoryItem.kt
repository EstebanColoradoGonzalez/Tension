package com.estebancoloradogonzalez.tension.domain.model

data class SessionHistoryItem(
    val sessionId: Long,
    val date: String,
    val moduleCode: String,
    val versionNumber: Int,
    val status: String,
    val totalTonnageKg: Double,
)
