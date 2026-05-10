package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.model.NextSession
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetNextSessionInfoUseCaseTest {

    private val repository: SessionRepository = mockk()
    private val useCase = GetNextSessionInfoUseCase(repository)

    @Test
    fun `invoke returns next session from repository`() = runTest {
        val nextSession = NextSession(
            routineId = 1L,
            routineName = "Pull+Abs",
            versionNumber = 1,
            routineVersionId = 1L,
        )
        every { repository.getNextSessionInfo() } returns flowOf(nextSession)

        val result = useCase().first()

        assertEquals(nextSession, result)
    }

    @Test
    fun `invoke returns null when repository returns null`() = runTest {
        every { repository.getNextSessionInfo() } returns flowOf(null)

        val result = useCase().first()

        assertNull(result)
    }
}
