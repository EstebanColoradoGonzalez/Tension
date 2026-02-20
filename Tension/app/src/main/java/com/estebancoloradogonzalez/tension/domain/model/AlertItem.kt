package com.estebancoloradogonzalez.tension.domain.model

data class AlertItem(
    val alertId: Long,
    val type: String,
    val level: String,
    val entityName: String,
    val message: String,
    val createdAt: String,
)
