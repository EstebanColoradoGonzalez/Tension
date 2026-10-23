package com.estebancoloradogonzalez.tension.ui.metrics

import com.estebancoloradogonzalez.tension.domain.model.AdherenceData
import com.estebancoloradogonzalez.tension.domain.model.BodyWeightSummary
import com.estebancoloradogonzalez.tension.domain.model.ExerciseLoadVelocity
import com.estebancoloradogonzalez.tension.domain.model.ExerciseProgressionRate
import com.estebancoloradogonzalez.tension.domain.model.Profile
import com.estebancoloradogonzalez.tension.domain.model.RirByRoutine
import com.estebancoloradogonzalez.tension.domain.model.RirInterpretation
import com.estebancoloradogonzalez.tension.domain.model.WeightRecord
import com.estebancoloradogonzalez.tension.domain.usecase.metrics.GetAdherenceUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.metrics.GetAvgRirByRoutineUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.metrics.GetBodyWeightSummaryUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.metrics.GetLoadVelocityUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.metrics.GetProgressionRateUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.profile.GetProfileUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.profile.GetWeightHistoryUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.profile.UpdateWeightUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class MetricsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getAdherenceUseCase: GetAdherenceUseCase = mockk()
    private val getAvgRirByRoutineUseCase: GetAvgRirByRoutineUseCase = mockk()
    private val getProgressionRateUseCase: GetProgressionRateUseCase = mockk()
    private val getLoadVelocityUseCase: GetLoadVelocityUseCase = mockk()
    private val getBodyWeightSummaryUseCase: GetBodyWeightSummaryUseCase = mockk()
    private val getProfileUseCase: GetProfileUseCase = mockk()
    private val getWeightHistoryUseCase: GetWeightHistoryUseCase = mockk()
    private val updateWeightUseCase: UpdateWeightUseCase = mockk()

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

    private val today = LocalDate.of(2026, 10, 22)
    private val weightHistory = MutableStateFlow(
        listOf(WeightRecord(id = 3, weightKg = 78.5, date = LocalDate.of(2026, 10, 20))),
    )

    private fun summary(currentWeightKg: Double = 78.5, variationKg: Double? = 1.3) = BodyWeightSummary(
        currentWeightKg = currentWeightKg,
        recordedOn = LocalDate.of(2026, 10, 20),
        daysSinceLastRecord = 2,
        isStale = false,
        variationKg = variationKg,
        bmi = currentWeightKg / (1.78 * 1.78),
        periodStart = today.minusWeeks(4),
        periodEnd = today,
        weightPoints = emptyList(),
        weeklyTonnage = emptyList(),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { getAdherenceUseCase() } returns adherence
        coEvery { getAvgRirByRoutineUseCase(any()) } returns rir
        coEvery { getProgressionRateUseCase(any()) } returns rates
        coEvery { getLoadVelocityUseCase(any()) } returns velocities
        coEvery { getBodyWeightSummaryUseCase(any()) } returns Result.success(summary())
        every { getProfileUseCase() } returns flowOf(
            Profile(78.5, 1.78, weeklyFrequency = 6, plateauBaseThreshold = 10, createdAt = today),
        )
        every { getWeightHistoryUseCase() } returns weightHistory
        coEvery { updateWeightUseCase(any()) } returns Result.success(Unit)
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
        getBodyWeightSummaryUseCase,
        getProfileUseCase,
        getWeightHistoryUseCase,
        updateWeightUseCase,
    )

    private fun MetricsViewModel.content() = uiState.value as MetricsUiState.Content

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

    // ----- HU-48: body weight section -----

    @Test
    fun `given a profile, when metrics load, then the body weight section is read over the default period`() =
        runTest {
            // Given
            val viewModel = createViewModel()

            // When
            advanceUntilIdle()

            // Then
            assertEquals(summary(), viewModel.content().bodyWeight)
            assertNull(viewModel.content().weightDialog)
            coVerify { getBodyWeightSummaryUseCase(MetricsViewModel.DEFAULT_PROGRESSION_WEEKS) }
        }

    @Test
    fun `given the shared period selector, when 8 weeks are chosen, then the body weight section follows it`() =
        runTest {
            // Given — QA-06: one selector for the whole panel
            coEvery { getBodyWeightSummaryUseCase(8) } returns Result.success(summary(variationKg = null))
            val viewModel = createViewModel()
            advanceUntilIdle()

            // When
            viewModel.changeProgressionPeriod(8)
            advanceUntilIdle()

            // Then
            assertNull(viewModel.content().bodyWeight!!.variationKg)
            coVerify { getBodyWeightSummaryUseCase(8) }
        }

    @Test
    fun `given the last weight is 78_5, when the dialog opens, then it is prefilled with it`() = runTest {
        // Given — QA-08
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.openWeightDialog()

        // Then
        val dialog = viewModel.content().weightDialog!!
        assertEquals("78.5", dialog.input)
        assertTrue(dialog.canSave)
        assertFalse(dialog.showError)
    }

    @Test
    fun `given a valid weight, when it is saved, then the same weight entry is created and the section refreshes`() =
        runTest {
            // Given — QA-08
            val viewModel = createViewModel()
            advanceUntilIdle()
            val events = mutableListOf<MetricsEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }
            coEvery { getBodyWeightSummaryUseCase(any()) } returns
                Result.success(summary(currentWeightKg = 79.0, variationKg = 1.8))
            viewModel.openWeightDialog()
            viewModel.onWeightInputChanged("79.0")

            // When
            viewModel.saveWeight()
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { updateWeightUseCase(79.0) }
            assertNull(viewModel.content().weightDialog)
            assertEquals(79.0, viewModel.content().bodyWeight!!.currentWeightKg, 0.0)
            assertEquals(1.8, viewModel.content().bodyWeight!!.variationKg!!, 0.0)
            assertEquals(listOf<MetricsEvent>(MetricsEvent.WeightSaved), events)
        }

    @Test
    fun `given an edited value, when the dialog is cancelled, then no entry is created`() = runTest {
        // Given — QA-09
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.openWeightDialog()
        viewModel.onWeightInputChanged("80.0")

        // When
        viewModel.dismissWeightDialog()
        advanceUntilIdle()

        // Then
        assertNull(viewModel.content().weightDialog)
        assertEquals(78.5, viewModel.content().bodyWeight!!.currentWeightKg, 0.0)
        coVerify(exactly = 0) { updateWeightUseCase(any()) }
    }

    @Test
    fun `given the qa values, when typed in the dialog, then error and save button match the profile criterion`() =
        runTest {
            // Given — QA-10
            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.openWeightDialog()
            val expected = mapOf(
                // value to (showError, canSave)
                "0" to (true to false),
                "-2" to (true to false),
                "" to (false to false),
                "0.1" to (false to true),
            )

            expected.forEach { (value, outcome) ->
                // When
                viewModel.onWeightInputChanged(value)

                // Then
                val dialog = viewModel.content().weightDialog!!
                assertEquals("showError for '$value'", outcome.first, dialog.showError)
                assertEquals("canSave for '$value'", outcome.second, dialog.canSave)
            }
        }

    @Test
    fun `given an invalid value, when save is attempted, then nothing is written`() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.openWeightDialog()
        viewModel.onWeightInputChanged("0")

        // When
        viewModel.saveWeight()
        advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { updateWeightUseCase(any()) }
        assertEquals("0", viewModel.content().weightDialog!!.input)
    }

    @Test
    fun `given two quick entries the same day, when both are saved, then two entries are created in order`() =
        runTest {
            // Given — QA-11
            val viewModel = createViewModel()
            advanceUntilIdle()

            // When
            viewModel.openWeightDialog()
            viewModel.onWeightInputChanged("79.0")
            viewModel.saveWeight()
            advanceUntilIdle()
            viewModel.openWeightDialog()
            viewModel.onWeightInputChanged("78.8")
            viewModel.saveWeight()
            advanceUntilIdle()

            // Then
            coVerifyOrder {
                updateWeightUseCase(79.0)
                updateWeightUseCase(78.8)
            }
        }

    @Test
    fun `given a failing save, when it is attempted, then the dialog stays open and the error is reported`() =
        runTest {
            // Given
            coEvery { updateWeightUseCase(any()) } returns Result.failure(IllegalStateException("disk"))
            val viewModel = createViewModel()
            advanceUntilIdle()
            val events = mutableListOf<MetricsEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }
            viewModel.openWeightDialog()

            // When
            viewModel.saveWeight()
            advanceUntilIdle()

            // Then
            val dialog = viewModel.content().weightDialog!!
            assertFalse(dialog.isSaving)
            assertTrue(dialog.canSave)
            assertEquals(listOf<MetricsEvent>(MetricsEvent.SaveError("disk")), events)
        }

    @Test
    fun `given the weight changes outside the panel, when the history emits, then the section refreshes`() =
        runTest {
            // Given — the profile edition writes the same history
            val viewModel = createViewModel()
            advanceUntilIdle()
            coEvery { getBodyWeightSummaryUseCase(any()) } returns Result.success(summary(currentWeightKg = 110.0))

            // When
            weightHistory.value = weightHistory.value +
                WeightRecord(id = 4, weightKg = 110.0, date = today)
            advanceUntilIdle()

            // Then
            assertEquals(110.0, viewModel.content().bodyWeight!!.currentWeightKg, 0.0)
        }
}
