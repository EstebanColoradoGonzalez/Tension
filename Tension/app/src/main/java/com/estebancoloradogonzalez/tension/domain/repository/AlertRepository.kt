package com.estebancoloradogonzalez.tension.domain.repository

import com.estebancoloradogonzalez.tension.domain.model.AlertDetail
import com.estebancoloradogonzalez.tension.domain.model.AlertItem
import kotlinx.coroutines.flow.Flow

interface AlertRepository {
    fun countActive(): Flow<Int>
    fun getActiveAlerts(): Flow<List<AlertItem>>
    suspend fun getAlertDetail(alertId: Long): AlertDetail
}
