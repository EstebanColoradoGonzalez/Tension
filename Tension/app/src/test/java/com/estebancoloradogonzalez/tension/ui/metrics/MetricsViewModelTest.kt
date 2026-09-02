package com.estebancoloradogonzalez.tension.ui.metrics

import com.estebancoloradogonzalez.tension.domain.model.AdherenceData
import com.estebancoloradogonzalez.tension.domain.model.ExerciseLoadVelocity
import com.estebancoloradogonzalez.tension.domain.model.ExerciseProgressionRate
import com.estebancoloradogonzalez.tension.domain.model.RirByRoutine
import com.estebancoloradogonzalez.tension.domain.model.RirInterpretation
import com.estebancoloradogonzalez.tension.domain.usecase.metrics.GetAdherenceUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.metrics.GetAvgRirByRoutineUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.metrics.GetLoadVelocityUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.metrics.GetProgressionRateUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MetricsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getAdherenceUseCase: GetAdherenceUseCase = mockk()
    private val getAvgRirByRoutineUseCase: GetAvgRirByRoutineUseCase = mockk()
    private val getProgressionRateUseCase: GetProgressionRateUseCase = mockk()
    private val getLoadVelocityUseCase: GetLoadVelocityUseCase = mockk()

    private val adherence = AdherenceData(completedSessions = 5, plannedSessions = 6, percentage = 83.3)
    private val rir = listOf(
        RirByRoutine(1L, "Push", 1.2, RirInterpretation.OPTIMAL, recordedSets = 18),
        RirByRoutine(2L, "Pull", null, null, recordedSets = 0),
    )
    private val rates = listOf(
        ExerciseProgressionRate(10L, "Press Banca", 64.0, isBodyweight = false, observations = 5),
    )
    private val velocities = listOf(
        ExerciseLoadVelocity(10L, "Press Banca", 2.5, isBodyweight = false, sessionCount = 6),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { getAdherenceUseCase() } returns adherence
        coEvery { getAvgRirByRoutineUseCase(any()) } returns rir
        coEvery { getProgressionRateUseCase(any()) } returns rates
        coEvery { getLoadVelocityUseCase(any()) } returns velocities
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = MetricsViewModel(
        getAdherenceUseCase,
        getAvgRirByRoutineUseCase,
        getProgressionRateUseCase,
        getLoadVelocityUseCase,
    )

    @Test
    fun `given a fresh screen, when metrics load, then the state carries the default windows`() = runTest {
        // Given
        val viewModel = createViewModel()

        // When
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value as MetricsUiState.Content
        assertEquals(MetricsViewModel.DEFAULT_PROGRESSION_WEEKS, state.progressionWeeks)
        assertEquals(MetricsViewModel.DEFAULT_RIR_SESSION_LIMIT, state.rirSessionLimit)
    }

    @Test
    fun `given loaded metrics, when the state is read, then no value is transformed`() = runTest {
        // Given
        val viewModel = createViewModel()

        // When
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value as MetricsUiState.Content
        assertEquals(adherence, state.adherence)
        assertEquals(rir, state.rirByRoutine)
        assertEquals(rates, state.progressionRates)
        assertEquals(velocities, state.loadVelocities)
    }

    @Test
    fun `given a new progression period, when it is selected, then the state and the use cases follow it`() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.changeProgressionPeriod(8)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value as MetricsUiState.Content
        assertEquals(8, state.progressionWeeks)
        coVerify { getProgressionRateUseCase(8) }
        coVerify { getLoadVelocityUseCase(8) }
    }

    @Test
    fun `given a new rir period, when it is selected, then the state and the use case follow it`() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.changeRirPeriod(6)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value as MetricsUiState.Content
        assertEquals(6, state.rirSessionLimit)
        coVerify { getAvgRirByRoutineUseCase(6) }
    }

    @Test
    fun `given a failing use case, when metrics load, then the state is an error`() = runTest {
        // Given
        coEvery { getAdherenceUseCase() } throws IllegalStateException("boom")
        val viewModel = createViewModel()

        // When
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value is MetricsUiState.Error)
    }
}
