package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import com.estebancoloradogonzalez.tension.domain.repository.TreeRepository
import com.estebancoloradogonzalez.tension.domain.usecase.tree.RecalculateTreeStateUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CloseSessionUseCaseTest {

    private val repository: SessionRepository = mockk()
    private val treeRepository: TreeRepository = mockk()
    private val useCase = CloseSessionUseCase(
        repository,
        RecalculateTreeStateUseCase(treeRepository),
    )

    @Test
    fun `invoke delegates to repository`() = runTest {
        coEvery { repository.closeSession(42L) } just runs
        coEvery { treeRepository.recalculate() } just runs

        useCase(42L)

        coVerify { repository.closeSession(42L) }
    }

    @Test(expected = IllegalStateException::class)
    fun `invoke propagates IllegalStateException when no active session`() = runTest {
        coEvery { repository.closeSession(any()) } throws
            IllegalStateException("No active session found")

        useCase(999L)
    }

    // Una sesión sin ninguna serie no se cierra: se cancela el día desde Inicio.

    @Test(expected = IllegalStateException::class)
    fun `invoke propagates IllegalStateException when the session has no sets`() = runTest {
        coEvery { repository.closeSession(any()) } throws
            IllegalStateException("Cannot close a session without any registered set")

        useCase(42L)
    }

    // El árbol reacciona de inmediato al entrenamiento recién registrado.

    @Test
    fun `invoke recalculates the tree after closing`() = runTest {
        coEvery { repository.closeSession(42L) } just runs
        coEvery { treeRepository.recalculate() } just runs

        useCase(42L)

        coVerify(exactly = 1) { treeRepository.recalculate() }
    }

    // Un árbol desactualizado es un defecto visual; una excepción aquí convertiría una sesión
    // ya cerrada en un error para el ejecutante.

    @Test
    fun `invoke does not fail when the tree recalculation throws`() = runTest {
        coEvery { repository.closeSession(42L) } just runs
        coEvery { treeRepository.recalculate() } throws IllegalStateException("db closed")

        useCase(42L)

        coVerify { repository.closeSession(42L) }
    }

    // Si el cierre falla, no hay nada nuevo que reflejar en el árbol.

    @Test
    fun `invoke does not recalculate when closing fails`() = runTest {
        coEvery { repository.closeSession(any()) } throws
            IllegalStateException("No active session found")

        runCatching { useCase(999L) }

        coVerify(exactly = 0) { treeRepository.recalculate() }
    }
}
