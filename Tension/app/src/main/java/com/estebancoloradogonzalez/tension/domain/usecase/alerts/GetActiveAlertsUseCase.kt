package com.estebancoloradogonzalez.tension.domain.usecase.alerts

import com.estebancoloradogonzalez.tension.domain.model.AlertItem
import com.estebancoloradogonzalez.tension.domain.repository.AlertRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetActiveAlertsUseCase @Inject constructor(
    private val alertRepository: AlertRepository,
) {
    operator fun invoke(): Flow<List<AlertItem>> = alertRepository.getActiveAlerts()
}
