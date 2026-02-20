package com.estebancoloradogonzalez.tension.domain.usecase.alerts

import com.estebancoloradogonzalez.tension.domain.model.AlertDetail
import com.estebancoloradogonzalez.tension.domain.repository.AlertRepository
import javax.inject.Inject

class GetAlertDetailUseCase @Inject constructor(
    private val alertRepository: AlertRepository,
) {
    suspend operator fun invoke(alertId: Long): AlertDetail =
        alertRepository.getAlertDetail(alertId)
}
