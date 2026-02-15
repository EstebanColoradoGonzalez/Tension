package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SubstituteExerciseUseCaseTest {

    private val repository: SessionRepository = mockk()
    private val useCase = SubstituteExerciseUseCase(repository)

    @Test
    fun `invoke delegates to repository`() = runTest {
        coEvery { repository.substituteExercise(1L, 10L) } just runs

        useCase(1L, 10L)

        coVerify { repository.substituteExercise(1L, 10L) }
    }

    @Test(expected = IllegalStateException::class)
    fun `invoke propagates IllegalStateException when exercise not found`() = runTest {
        coEvery { repository.substituteExercise(any(), any()) } throws
            IllegalStateException("Session exercise not found")

        useCase(999L, 10L)
    }

    @Test(expected = IllegalStateException::class)
    fun `invoke propagates IllegalStateException when exercise has registered sets`() = runTest {
        coEvery { repository.substituteExercise(any(), any()) } throws
            IllegalStateException("Cannot substitute exercise with registered sets")

        useCase(1L, 10L)
    }
}
