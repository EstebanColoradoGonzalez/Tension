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

    private fun createEntries(count: Int, equipment: String = "Barra") = (1..count).map { i ->
        ExerciseHistoryEntry(
            date = "2026-02-${15 - i + 1}",
            routineName = "Push",
            versionNumber = 1,
            equipmentTypeName = equipment,
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
            equipmentOptions = listOf("Barra"),
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
            equipmentOptions = listOf("Barra"),
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
            equipmentOptions = listOf("Peso Corporal"),
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
            equipmentOptions = emptyList(),
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
                date = "2026-02-15", routineName = "Push", versionNumber = 1,
                equipmentTypeName = "Barra",
                avgWeightKg = 65.0, totalReps = 40, avgRir = 2.0,
                classification = ProgressionClassification.POSITIVE_PROGRESSION,
            ),
            ExerciseHistoryEntry(
                date = "2026-02-10", routineName = "Push", versionNumber = 1,
                equipmentTypeName = "Barra",
                avgWeightKg = 60.0, totalReps = 38, avgRir = 2.5,
                classification = ProgressionClassification.MAINTENANCE,
            ),
            ExerciseHistoryEntry(
                date = "2026-02-05", routineName = "Push", versionNumber = 1,
                equipmentTypeName = "Barra",
                avgWeightKg = 55.0, totalReps = 36, avgRir = 3.0,
                classification = null,
            ),
        )
        val data = ExerciseHistoryData(
            exerciseName = "Press Banca",
            progressionStatus = "IN_PROGRESSION",
            isBodyweight = false,
            isIsometric = false,
            equipmentOptions = listOf("Barra"),
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

    // CA-39.08 — el historial se lee por implemento

    @Test
    fun `given two implements, when loaded, then only the first one feeds the chart`() = runTest {
        coEvery { getExerciseHistoryUseCase(10L) } returns twoImplementHistory()

        val viewModel = ExerciseHistoryViewModel(
            getExerciseHistoryUseCase,
            SavedStateHandle(mapOf("exerciseId" to 10L)),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value as ExerciseHistoryUiState.Loaded
        assertEquals(listOf("Mancuerna", "Polea"), state.equipmentOptions)
        assertEquals("Mancuerna", state.selectedEquipment)
        assertEquals(2, state.data.entries.size)
        assertEquals(2, state.trendPoints.size)
        assertTrue(state.data.entries.all { it.equipmentTypeName == "Mancuerna" })
    }

    @Test
    fun `given a selected implement, when another is chosen, then the series is rebuilt`() =
        runTest {
            coEvery { getExerciseHistoryUseCase(10L) } returns twoImplementHistory()

            val viewModel = ExerciseHistoryViewModel(
                getExerciseHistoryUseCase,
                SavedStateHandle(mapOf("exerciseId" to 10L)),
            )
            advanceUntilIdle()

            viewModel.onEquipmentSelected("Polea")

            val state = viewModel.uiState.value as ExerciseHistoryUiState.Loaded
            assertEquals("Polea", state.selectedEquipment)
            assertEquals(1, state.data.entries.size)
            assertEquals(1, state.trendPoints.size)
            // La única serie de polea pesa 20.0: nunca se promedió con las de mancuerna.
            assertEquals(20.0f, state.trendPoints[0].value)
        }

    @Test
    fun `given a single implement, when loaded, then it is the only option offered`() = runTest {
        val data = ExerciseHistoryData(
            exerciseName = "Curl de Isquiotibiales Sentado",
            progressionStatus = "IN_PROGRESSION",
            isBodyweight = false,
            isIsometric = false,
            equipmentOptions = listOf("Máquina"),
            entries = createEntries(2, equipment = "Máquina"),
        )
        coEvery { getExerciseHistoryUseCase(6L) } returns data

        val viewModel = ExerciseHistoryViewModel(
            getExerciseHistoryUseCase,
            SavedStateHandle(mapOf("exerciseId" to 6L)),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value as ExerciseHistoryUiState.Loaded
        assertEquals(listOf("Máquina"), state.equipmentOptions)
        assertEquals(2, state.data.entries.size)
    }

    /** Elevación Lateral: dos sesiones con mancuerna y una con polea. */
    private fun twoImplementHistory() = ExerciseHistoryData(
        exerciseName = "Elevación Lateral",
        progressionStatus = "IN_PROGRESSION",
        isBodyweight = false,
        isIsometric = false,
        equipmentOptions = listOf("Mancuerna", "Polea"),
        entries = listOf(
            ExerciseHistoryEntry(
                date = "2026-02-15", routineName = "Push", versionNumber = 1,
                equipmentTypeName = "Polea",
                avgWeightKg = 20.0, totalReps = 40, avgRir = 1.0,
                classification = ProgressionClassification.POSITIVE_PROGRESSION,
            ),
            ExerciseHistoryEntry(
                date = "2026-02-15", routineName = "Push", versionNumber = 1,
                equipmentTypeName = "Mancuerna",
                avgWeightKg = 12.0, totalReps = 38, avgRir = 1.0,
                classification = ProgressionClassification.POSITIVE_PROGRESSION,
            ),
            ExerciseHistoryEntry(
                date = "2026-02-08", routineName = "Push", versionNumber = 1,
                equipmentTypeName = "Mancuerna",
                avgWeightKg = 11.5, totalReps = 36, avgRir = 1.0,
                classification = ProgressionClassification.MAINTENANCE,
            ),
        ),
    )
}
