package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UndoSkipTodayUseCaseTest {

    private val repository: SessionRepository = mockk()
    private val useCase = UndoSkipTodayUseCase(repository)

    @Test
    fun `invoke clears the skip so the day proposes its session again`() = runTest {
        coEvery { repository.undoSkipToday() } just runs

        useCase()

        coVerify { repository.undoSkipToday() }
    }
}
