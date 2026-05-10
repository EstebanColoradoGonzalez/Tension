package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FinalizeExerciseUseCaseTest {

    private val repository: SessionRepository = mockk()
    private val useCase = FinalizeExerciseUseCase(repository)

    @Test
    fun `invoke delegates to repository`() = runTest {
        coEvery { repository.finalizeExercise(42L) } just runs

        useCase(42L)

        coVerify(exactly = 1) { repository.finalizeExercise(42L) }
    }
}
