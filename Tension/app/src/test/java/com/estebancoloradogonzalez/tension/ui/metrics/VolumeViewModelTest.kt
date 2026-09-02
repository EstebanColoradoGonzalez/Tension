package com.estebancoloradogonzalez.tension.ui.metrics

import com.estebancoloradogonzalez.tension.domain.model.MuscleGroupTonnage
import com.estebancoloradogonzalez.tension.domain.model.TonnageSnapshot
import com.estebancoloradogonzalez.tension.domain.usecase.metrics.GetMicrocycleMapUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.metrics.GetTonnageByMuscleGroupUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.metrics.GetTonnageEvolutionUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.metrics.GetVolumeDistributionUseCase
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VolumeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getMicrocycleMapUseCase: GetMicrocycleMapUseCase = mockk()
    private val getTonnageByMuscleGroupUseCase: GetTonnageByMuscleGroupUseCase = mockk()
    private val getVolumeDistributionUseCase: GetVolumeDistributionUseCase = mockk()
    private val getTonnageEvolutionUseCase: GetTonnageEvolutionUseCase = mockk()

    private val tonnage = listOf(MuscleGroupTonnage("Pecho", 4200.0), MuscleGroupTonnage("Gemelos", 0.0))
    private val distribution = mapOf("Pecho" to mapOf("Pectoral medio" to 60.0))

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { getTonnageByMuscleGroupUseCase(any()) } returns tonnage
        coEvery { getVolumeDistributionUseCase(any()) } returns distribution
        coEvery { getTonnageEvolutionUseCase(any()) } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = VolumeViewModel(
        getMicrocycleMapUseCase,
        getTonnageByMuscleGroupUseCase,
        getVolumeDistributionUseCase,
        getTonnageEvolutionUseCase,
    )

    @Test
    fun `given a history of microcycles, when volume loads, then the last one and its session count are exposed`() =
        runTest {
            // Given
            coEvery { getMicrocycleMapUseCase() } returns mapOf(
                1 to listOf(1L, 2L, 3L, 4L, 5L, 6L),
                2 to listOf(7L, 8L, 9L, 10L, 11L, 12L),
            )
            val viewModel = createViewModel()

            // When
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value as VolumeUiState.Content
            assertEquals(2, state.selectedMicrocycle)
            assertEquals(6, state.sessionsInSelectedMicrocycle)
        }

    @Test
    fun `given another microcycle, when it is selected, then its session count is recomputed`() = runTest {
        // Given
        coEvery { getMicrocycleMapUseCase() } returns mapOf(
            1 to listOf(1L, 2L),
            2 to listOf(3L, 4L, 5L, 6L, 7L, 8L),
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.selectMicrocycle(1)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value as VolumeUiState.Content
        assertEquals(1, state.selectedMicrocycle)
        assertEquals(2, state.sessionsInSelectedMicrocycle)
    }

    @Test
    fun `given an empty history, when volume loads, then the tonnage of the use case is kept and no session is counted`() =
        runTest {
            // Given
            coEvery { getMicrocycleMapUseCase() } returns emptyMap()
            val viewModel = createViewModel()

            // When
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value as VolumeUiState.Content
            assertEquals(0, state.sessionsInSelectedMicrocycle)
            assertEquals(tonnage, state.tonnageByGroup)
        }

    @Test
    fun `given a single microcycle, when volume loads, then the evolution is insufficient`() = runTest {
        // Given
        coEvery { getMicrocycleMapUseCase() } returns mapOf(1 to listOf(1L, 2L))
        val viewModel = createViewModel()

        // When
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value as VolumeUiState.Content
        assertTrue(state.insufficientEvolution)
    }

    @Test
    fun `given two microcycles, when volume loads, then the evolution is sufficient`() = runTest {
        // Given
        coEvery { getMicrocycleMapUseCase() } returns mapOf(1 to listOf(1L), 2 to listOf(2L))
        coEvery { getTonnageEvolutionUseCase(any()) } returns listOf(
            TonnageSnapshot(1, mapOf("Pecho" to 1000.0)),
            TonnageSnapshot(2, mapOf("Pecho" to 1200.0)),
        )
        val viewModel = createViewModel()

        // When
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value as VolumeUiState.Content
        assertFalse(state.insufficientEvolution)
    }

    @Test
    fun `given a failing use case, when volume loads, then the state is an error`() = runTest {
        // Given
        coEvery { getMicrocycleMapUseCase() } throws IllegalStateException("boom")
        val viewModel = createViewModel()

        // When
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value is VolumeUiState.Error)
    }
}
