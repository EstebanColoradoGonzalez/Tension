package com.estebancoloradogonzalez.tension.domain.usecase.history

import com.estebancoloradogonzalez.tension.domain.model.SessionHistoryItem
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetSessionHistoryUseCaseTest {

    private val repository: SessionRepository = mockk()
    private val useCase = GetSessionHistoryUseCase(repository)

    @Test
    fun `invoke returns list of closed sessions`() = runTest {
        val sessions = listOf(
            SessionHistoryItem(
                sessionId = 1L,
                date = "2026-02-15",
                moduleCode = "A",
                versionNumber = 1,
                status = "COMPLETED",
                totalTonnageKg = 5000.0,
            ),
            SessionHistoryItem(
                sessionId = 2L,
                date = "2026-02-14",
                moduleCode = "B",
                versionNumber = 2,
                status = "INCOMPLETE",
                totalTonnageKg = 3200.0,
            ),
        )
        coEvery { repository.getSessionHistory() } returns sessions

        val result = useCase()

        assertEquals(2, result.size)
        assertEquals("A", result[0].moduleCode)
        assertEquals("INCOMPLETE", result[1].status)
    }

    @Test
    fun `invoke returns empty list when no sessions`() = runTest {
        coEvery { repository.getSessionHistory() } returns emptyList()

        val result = useCase()

        assertTrue(result.isEmpty())
    }
}
