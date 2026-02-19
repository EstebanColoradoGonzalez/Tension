package com.estebancoloradogonzalez.tension.ui.history

import androidx.lifecycle.SavedStateHandle
import com.estebancoloradogonzalez.tension.domain.model.ExerciseHistoryData
import com.estebancoloradogonzalez.tension.domain.model.ExerciseHistoryEntry
import com.estebancoloradogonzalez.tension.domain.model.ProgressionClassification
import com.estebancoloradogonzalez.tension.domain.usecase.history.GetExerciseHistoryUseCase
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
class ExerciseHistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getExerciseHistoryUseCase: GetExerciseHistoryUseCase = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createEntries(count: Int) = (1..count).map { i ->
        ExerciseHistoryEntry(
            date = "2026-02-${15 - i + 1}",
            moduleCode = "B",
            versionNumber = 1,
            avgWeightKg = 50.0 + i * 2.5,
            totalReps = 36 + i,
            avgRir = 2.0,
            classification = if (i == 1) null else ProgressionClassification.POSITIVE_PROGRESSION,
        )
    }

    @Test
    fun `state is Loaded with standard exercise trend points in Kg`() = runTest {
        val data = ExerciseHistoryData(
            exerciseName = "Press Banca",
            progressionStatus = "IN_PROGRESSION",
            isBodyweight = false,
            isIsometric = false,
            entries = createEntries(3),
        )
        coEvery { getExerciseHistoryUseCase(10L) } returns data

        val savedStateHandle = SavedStateHandle(mapOf("exerciseId" to 10L))
        val viewModel = ExerciseHistoryViewModel(getExerciseHistoryUseCase, savedStateHandle)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ExerciseHistoryUiState.Loaded)
        val loaded = state as ExerciseHistoryUiState.Loaded
        assertEquals("Kg", loaded.yAxisLabel)
        assertEquals(3, loaded.trendPoints.size)
    }

    @Test
    fun `state is Loaded with bodyweight exercise trend points in reps`() = runTest {
        val data = ExerciseHistoryData(
            exerciseName = "Flexiones",
            progressionStatus = "IN_PROGRESSION",
            isBodyweight = true,
            isIsometric = false,
            entries = createEntries(2),
        )
        coEvery { getExerciseHistoryUseCase(4L) } returns data

        val savedStateHandle = SavedStateHandle(mapOf("exerciseId" to 4L))
        val viewModel = ExerciseHistoryViewModel(getExerciseHistoryUseCase, savedStateHandle)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ExerciseHistoryUiState.Loaded)
        assertEquals("reps", (state as ExerciseHistoryUiState.Loaded).yAxisLabel)
    }

    @Test
    fun `state is Loaded with isometric exercise trend points in seconds`() = runTest {
        val data = ExerciseHistoryData(
            exerciseName = "Plancha",
            progressionStatus = "IN_PROGRESSION",
            isBodyweight = true,
            isIsometric = true,
            entries = createEntries(2),
        )
        coEvery { getExerciseHistoryUseCase(14L) } returns data

        val savedStateHandle = SavedStateHandle(mapOf("exerciseId" to 14L))
        val viewModel = ExerciseHistoryViewModel(getExerciseHistoryUseCase, savedStateHandle)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ExerciseHistoryUiState.Loaded)
        assertEquals("s", (state as ExerciseHistoryUiState.Loaded).yAxisLabel)
    }

    @Test
    fun `state is Empty when no entries`() = runTest {
        val data = ExerciseHistoryData(
            exerciseName = "Nuevo Ejercicio",
            progressionStatus = "NO_HISTORY",
            isBodyweight = false,
            isIsometric = false,
            entries = emptyList(),
        )
        coEvery { getExerciseHistoryUseCase(99L) } returns data

        val savedStateHandle = SavedStateHandle(mapOf("exerciseId" to 99L))
        val viewModel = ExerciseHistoryViewModel(getExerciseHistoryUseCase, savedStateHandle)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is ExerciseHistoryUiState.Empty)
    }

    @Test
    fun `trend points are reversed for chronological chart`() = runTest {
        // Entries are DESC: index 0 is most recent, index 2 is oldest
        val entries = listOf(
            ExerciseHistoryEntry(
                date = "2026-02-15", moduleCode = "B", versionNumber = 1,
                avgWeightKg = 65.0, totalReps = 40, avgRir = 2.0,
                classification = ProgressionClassification.POSITIVE_PROGRESSION,
            ),
            ExerciseHistoryEntry(
                date = "2026-02-10", moduleCode = "B", versionNumber = 1,
                avgWeightKg = 60.0, totalReps = 38, avgRir = 2.5,
                classification = ProgressionClassification.MAINTENANCE,
            ),
            ExerciseHistoryEntry(
                date = "2026-02-05", moduleCode = "B", versionNumber = 1,
                avgWeightKg = 55.0, totalReps = 36, avgRir = 3.0,
                classification = null,
            ),
        )
        val data = ExerciseHistoryData(
            exerciseName = "Press Banca",
            progressionStatus = "IN_PROGRESSION",
            isBodyweight = false,
            isIsometric = false,
            entries = entries,
        )
        coEvery { getExerciseHistoryUseCase(10L) } returns data

        val savedStateHandle = SavedStateHandle(mapOf("exerciseId" to 10L))
        val viewModel = ExerciseHistoryViewModel(getExerciseHistoryUseCase, savedStateHandle)
        advanceUntilIdle()

        val state = viewModel.uiState.value as ExerciseHistoryUiState.Loaded
        // TrendPoints[0] should be the oldest (55.0 Kg), TrendPoints[2] the most recent (65.0 Kg)
        assertEquals(55.0f, state.trendPoints[0].value)
        assertEquals(60.0f, state.trendPoints[1].value)
        assertEquals(65.0f, state.trendPoints[2].value)
        assertEquals("S1", state.trendPoints[0].label)
        assertEquals("S3", state.trendPoints[2].label)
    }
}
