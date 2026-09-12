package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AddExerciseToSessionUseCaseTest {

    private val repository: SessionRepository = mockk()
    private val useCase = AddExerciseToSessionUseCase(repository)

    @Test
    fun `delega en el repositorio y devuelve la fila creada`() = runTest {
        coEvery { repository.addExerciseToSession(7L, 42L) } returns 99L

        val result = useCase(sessionId = 7L, exerciseId = 42L)

        assertEquals(99L, result)
        coVerify { repository.addExerciseToSession(7L, 42L) }
    }

    @Test(expected = IllegalStateException::class)
    fun `propaga el rechazo de un ejercicio que ya esta en la sesion`() = runTest {
        coEvery { repository.addExerciseToSession(7L, 42L) } throws
            IllegalStateException("Exercise is already in this session")

        useCase(sessionId = 7L, exerciseId = 42L)
    }

    @Test(expected = IllegalStateException::class)
    fun `propaga el rechazo de una sesion que ya no esta en curso`() = runTest {
        coEvery { repository.addExerciseToSession(7L, 42L) } throws
            IllegalStateException("Cannot adjust a session that is not in progress")

        useCase(sessionId = 7L, exerciseId = 42L)
    }
}
