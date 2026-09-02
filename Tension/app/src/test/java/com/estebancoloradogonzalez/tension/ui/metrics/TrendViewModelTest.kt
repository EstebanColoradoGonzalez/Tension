package com.estebancoloradogonzalez.tension.ui.metrics

import com.estebancoloradogonzalez.tension.domain.model.MuscleGroupTrend
import com.estebancoloradogonzalez.tension.domain.model.TrendDirection
import com.estebancoloradogonzalez.tension.domain.usecase.metrics.GetMicrocycleMapUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.metrics.GetMuscleGroupTrendUseCase
import com.estebancoloradogonzalez.tension.ui.components.MetricRequirementKind
import com.estebancoloradogonzalez.tension.ui.components.MetricSufficiencyRules
import io.mockk.coEvery
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
class TrendViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getMicrocycleMapUseCase: GetMicrocycleMapUseCase = mockk()
    private val getMuscleGroupTrendUseCase: GetMuscleGroupTrendUseCase = mockk()

    private val trends = listOf(
        MuscleGroupTrend("Pecho", TrendDirection.ASCENDING),
        MuscleGroupTrend("Espalda", TrendDirection.STABLE),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { getMuscleGroupTrendUseCase(any()) } returns trends
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = TrendViewModel(
        getMicrocycleMapUseCase,
        getMuscleGroupTrendUseCase,
    )

    private fun microcycles(count: Int, sessionsEach: Int = 2): Map<Int, List<Long>> =
        (1..count).associateWith { number ->
            (1..sessionsEach).map { (number * 10 + it).toLong() }
        }

    @Test
    fun `given fewer complete microcycles than required, when trends load, then the missing amount is stated`() =
        runTest {
            // Given
            coEvery { getMicrocycleMapUseCase() } returns microcycles(2)
            val viewModel = createViewModel()

            // When
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value as TrendUiState.InsufficientData
            assertEquals(MetricRequirementKind.COMPLETE_MICROCYCLES, state.requirement.kind)
            assertEquals(2, state.requirement.missing)
        }

    @Test
    fun `given enough complete microcycles, when trends load, then the evaluated window is exposed`() = runTest {
        // Given
        coEvery { getMicrocycleMapUseCase() } returns microcycles(6)
        val viewModel = createViewModel()

        // When
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value as TrendUiState.Content
        assertEquals(MetricSufficiencyRules.MIN_TREND_MICROCYCLES, state.evaluatedMicrocycles)
    }

    @Test
    fun `given classified trends, when the state is read, then they are not transformed`() = runTest {
        // Given
        coEvery { getMicrocycleMapUseCase() } returns microcycles(4)
        val viewModel = createViewModel()

        // When
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value as TrendUiState.Content
        assertEquals(trends, state.trends)
    }

    @Test
    fun `given a failing use case, when trends load, then the state is an error`() = runTest {
        // Given
        coEvery { getMicrocycleMapUseCase() } throws IllegalStateException("boom")
        val viewModel = createViewModel()

        // When
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value is TrendUiState.Error)
    }
}
