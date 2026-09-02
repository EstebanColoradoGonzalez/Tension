package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.repository.WeekDayRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ClearTemporaryRoutineUseCaseTest {

    private val weekDayRepository: WeekDayRepository = mockk()
    private val useCase = ClearTemporaryRoutineUseCase(weekDayRepository)

    @Test
    fun `invoke clears the override`() = runTest {
        coEvery { weekDayRepository.clearTodayOverride() } just runs

        useCase()

        coVerify { weekDayRepository.clearTodayOverride() }
    }
}
