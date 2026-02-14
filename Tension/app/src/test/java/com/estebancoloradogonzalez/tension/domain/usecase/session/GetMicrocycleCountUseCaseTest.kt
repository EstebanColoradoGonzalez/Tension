package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.model.RotationState
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetMicrocycleCountUseCaseTest {

    private val repository: SessionRepository = mockk()
    private val useCase = GetMicrocycleCountUseCase(repository)

    @Test
    fun `invoke returns microcycle count from rotation state`() = runTest {
        val rotationState = RotationState(
            microcyclePosition = 3,
            currentVersionModuleA = 1,
            currentVersionModuleB = 1,
            currentVersionModuleC = 1,
            microcycleCount = 5,
        )
        every { repository.getRotationState() } returns flowOf(rotationState)

        val result = useCase().first()

        assertEquals(5, result)
    }

    @Test
    fun `invoke returns zero when rotation state is null`() = runTest {
        every { repository.getRotationState() } returns flowOf(null)

        val result = useCase().first()

        assertEquals(0, result)
    }
}
