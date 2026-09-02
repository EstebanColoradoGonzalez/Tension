package com.estebancoloradogonzalez.tension.domain.model

/**
 * Full content of an alert. [suggestedAction] is not optional: an alert that only
 * describes a problem leaves the executant to work out the next step, which is the work
 * the system exists to do. Modelling it as a single non-nullable field makes an alert
 * without an action inexpressible.
 */
data class AlertDetail(
    val alertId: Long,
    val type: String,
    val level: String,
    val entityName: String,
    val message: String,
    val createdAt: String,
    val triggerData: AlertTriggerData,
    val causalAnalysis: String,
    val suggestedAction: SuggestedAction,
    val exerciseId: Long?,
)
