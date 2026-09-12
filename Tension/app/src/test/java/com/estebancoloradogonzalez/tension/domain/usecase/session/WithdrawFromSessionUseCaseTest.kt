package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WithdrawFromSessionUseCaseTest {

    private val repository: SessionRepository = mockk()
    private val useCase = WithdrawFromSessionUseCase(repository)

    @Test
    fun `delega el retiro en el repositorio`() = runTest {
        coEvery { repository.withdrawFromSession(5L) } just runs

        useCase(5L)

        coVerify { repository.withdrawFromSession(5L) }
    }

    @Test(expected = IllegalStateException::class)
    fun `propaga el veredicto negativo del repositorio`() = runTest {
        // El veredicto se evalúa dentro de la transacción, no aquí: decidirlo fuera dejaría
        // una ventana entre la comprobación y el borrado en la que la invariante se rompe.
        coEvery { repository.withdrawFromSession(5L) } throws
            IllegalStateException("Exercise cannot be withdrawn: BudgetExhausted")

        useCase(5L)
    }
}
