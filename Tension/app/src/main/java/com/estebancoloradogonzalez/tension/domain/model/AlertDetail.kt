package com.estebancoloradogonzalez.tension.domain.model

data class AlertDetail(
    val alertId: Long,
    val type: String,
    val level: String,
    val entityName: String,
    val message: String,
    val createdAt: String,
    val triggerData: AlertTriggerData,
    val causalAnalysis: String,
    val recommendations: List<String>,
    val showExerciseHistoryLink: Boolean,
    val showDeloadLink: Boolean,
    val exerciseId: Long?,
)
