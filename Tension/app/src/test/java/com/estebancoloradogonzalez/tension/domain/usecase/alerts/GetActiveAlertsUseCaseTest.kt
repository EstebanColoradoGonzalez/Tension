package com.estebancoloradogonzalez.tension.domain.usecase.alerts

import com.estebancoloradogonzalez.tension.domain.model.AlertItem
import com.estebancoloradogonzalez.tension.domain.repository.AlertRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetActiveAlertsUseCaseTest {

    private val repository: AlertRepository = mockk()
    private val useCase = GetActiveAlertsUseCase(repository)

    @Test
    fun `invoke returns flow with alerts from repository`() = runTest {
        val alerts = listOf(
            AlertItem(
                alertId = 1L,
                type = "PLATEAU",
                level = "HIGH_ALERT",
                entityName = "Press banca",
                message = "3 sesiones sin progresión",
                createdAt = "2026-02-15",
            ),
        )
        every { repository.getActiveAlerts() } returns flowOf(alerts)

        val result = useCase().first()

        assertEquals(1, result.size)
        assertEquals("PLATEAU", result[0].type)
    }

    @Test
    fun `invoke returns empty flow when no alerts`() = runTest {
        every { repository.getActiveAlerts() } returns flowOf(emptyList())

        val result = useCase().first()

        assertEquals(0, result.size)
    }
}
