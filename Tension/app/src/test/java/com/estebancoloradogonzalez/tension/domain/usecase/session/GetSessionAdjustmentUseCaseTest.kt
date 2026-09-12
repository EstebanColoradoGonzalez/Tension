package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.model.SessionAdjustment
import com.estebancoloradogonzalez.tension.domain.model.WithdrawnExercise
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetSessionAdjustmentUseCaseTest {

    private val repository: SessionRepository = mockk()
    private val useCase = GetSessionAdjustmentUseCase(repository)

    @Test
    fun `el presupuesto es la diferencia entre anadidos y retirados`() = runTest {
        every { repository.getSessionAdjustment(1L) } returns flowOf(
            SessionAdjustment(
                addedCount = 2,
                withdrawnCount = 1,
                pendingWithdrawals = listOf(WithdrawnExercise(9L, "Curl Bayesian")),
            ),
        )

        val adjustment = useCase(1L).first()

        assertEquals(1, adjustment.budget)
        assertEquals("Curl Bayesian", adjustment.pendingWithdrawals.first().name)
    }

    @Test
    fun `una sesion sin ajustar tiene presupuesto cero`() = runTest {
        every { repository.getSessionAdjustment(1L) } returns flowOf(SessionAdjustment())

        assertEquals(0, useCase(1L).first().budget)
    }
}
