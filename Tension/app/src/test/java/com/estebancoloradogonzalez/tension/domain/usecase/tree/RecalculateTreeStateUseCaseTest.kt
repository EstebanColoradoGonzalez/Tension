package com.estebancoloradogonzalez.tension.domain.usecase.tree

import com.estebancoloradogonzalez.tension.domain.repository.TreeRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RecalculateTreeStateUseCaseTest {

    private val repository: TreeRepository = mockk()
    private val useCase = RecalculateTreeStateUseCase(repository)

    @Test
    fun `invoke delegates the recalculation to the repository`() = runTest {
        coEvery { repository.recalculate() } just runs

        useCase()

        coVerify(exactly = 1) { repository.recalculate() }
    }

    // El arbol es best-effort en sus llamadores, pero el caso de uso no traga el fallo: quien
    // decide si una excepcion importa es quien lo invoca.

    @Test(expected = IllegalStateException::class)
    fun `invoke propagates repository failures to its caller`() = runTest {
        coEvery { repository.recalculate() } throws IllegalStateException("db closed")

        useCase()
    }
}
