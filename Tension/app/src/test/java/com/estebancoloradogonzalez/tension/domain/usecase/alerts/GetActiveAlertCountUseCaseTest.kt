package com.estebancoloradogonzalez.tension.domain.usecase.alerts

import com.estebancoloradogonzalez.tension.domain.repository.AlertRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetActiveAlertCountUseCaseTest {

    private val repository: AlertRepository = mockk()
    private val useCase = GetActiveAlertCountUseCase(repository)

    @Test
    fun `invoke returns flow with count from repository`() = runTest {
        every { repository.countActive() } returns flowOf(5)

        val result = useCase().first()

        assertEquals(5, result)
    }

    @Test
    fun `invoke returns zero when no active alerts`() = runTest {
        every { repository.countActive() } returns flowOf(0)

        val result = useCase().first()

        assertEquals(0, result)
    }
}
