package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.model.ActiveSession
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetActiveSessionUseCaseTest {

    private val repository: SessionRepository = mockk()
    private val useCase = GetActiveSessionUseCase(repository)

    @Test
    fun `invoke returns active session when one exists`() = runTest {
        val activeSession = ActiveSession(
            sessionId = 1L,
            moduleCode = "A",
            versionNumber = 1,
            totalExercises = 10,
            completedExercises = 3,
        )
        every { repository.getActiveSession() } returns flowOf(activeSession)

        val result = useCase().first()

        assertEquals(activeSession, result)
    }

    @Test
    fun `invoke returns null when no active session`() = runTest {
        every { repository.getActiveSession() } returns flowOf(null)

        val result = useCase().first()

        assertNull(result)
    }
}
