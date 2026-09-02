package com.estebancoloradogonzalez.tension.ui.settings

import com.estebancoloradogonzalez.tension.domain.model.ExperienceLevel
import com.estebancoloradogonzalez.tension.domain.model.Profile
import com.estebancoloradogonzalez.tension.domain.usecase.profile.GetProfileUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.profile.UpdatePlateauBaseThresholdUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getProfileUseCase: GetProfileUseCase = mockk()
    private val updatePlateauBaseThresholdUseCase: UpdatePlateauBaseThresholdUseCase = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun profile(plateauBaseThreshold: Int) = Profile(
        currentWeightKg = 80.0,
        heightM = 1.80,
        experienceLevel = ExperienceLevel.INTERMEDIATE,
        weeklyFrequency = 6,
        plateauBaseThreshold = plateauBaseThreshold,
        createdAt = LocalDate.of(2026, 1, 1),
    )

    private fun buildViewModel(storedThreshold: Int?): SettingsViewModel {
        every { getProfileUseCase() } returns flowOf(storedThreshold?.let { profile(it) })
        coEvery { updatePlateauBaseThresholdUseCase(any()) } returns Result.success(Unit)
        return SettingsViewModel(getProfileUseCase, updatePlateauBaseThresholdUseCase)
    }

    @Test
    fun `given a stored threshold, when the screen loads, then the state reflects it`() = runTest {
        val viewModel = buildViewModel(storedThreshold = 9)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(9, viewModel.uiState.value.baseThreshold)
    }

    @Test
    fun `given no profile yet, when the screen loads, then the state falls back to the default`() = runTest {
        val viewModel = buildViewModel(storedThreshold = null)
        advanceUntilIdle()

        assertEquals(5, viewModel.uiState.value.baseThreshold)
    }

    @Test
    fun `given a threshold of 5, when it is increased, then it becomes 6 and is persisted`() = runTest {
        val viewModel = buildViewModel(storedThreshold = 5)
        advanceUntilIdle()

        viewModel.onIncreaseThreshold()
        advanceUntilIdle()

        assertEquals(6, viewModel.uiState.value.baseThreshold)
        coVerify { updatePlateauBaseThresholdUseCase(6) }
    }

    @Test
    fun `given a threshold of 5, when it is decreased, then it becomes 4 and is persisted`() = runTest {
        val viewModel = buildViewModel(storedThreshold = 5)
        advanceUntilIdle()

        viewModel.onDecreaseThreshold()
        advanceUntilIdle()

        assertEquals(4, viewModel.uiState.value.baseThreshold)
        coVerify { updatePlateauBaseThresholdUseCase(4) }
    }

    @Test
    fun `given the upper bound, when the state is read, then increasing is disabled`() = runTest {
        val viewModel = buildViewModel(storedThreshold = 15)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.canIncreaseThreshold)
        assertTrue(viewModel.uiState.value.canDecreaseThreshold)
    }

    @Test
    fun `given the lower bound, when the state is read, then decreasing is disabled`() = runTest {
        val viewModel = buildViewModel(storedThreshold = 3)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.canDecreaseThreshold)
        assertTrue(viewModel.uiState.value.canIncreaseThreshold)
    }

    @Test
    fun `given the base threshold, when the breakdown is read, then it matches the rule`() = runTest {
        val viewModel = buildViewModel(storedThreshold = 5)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(5, state.lowThresholdSessions)
        assertEquals(8, state.mediumThresholdSessions)
        assertEquals(10, state.highThresholdSessions)
    }

    @Test
    fun `given a rejected value, when the update fails, then the range error is exposed`() = runTest {
        every { getProfileUseCase() } returns flowOf(profile(5))
        coEvery { updatePlateauBaseThresholdUseCase(any()) } returns
            Result.failure(IllegalArgumentException("out of range"))
        val viewModel = SettingsViewModel(getProfileUseCase, updatePlateauBaseThresholdUseCase)
        advanceUntilIdle()

        viewModel.onIncreaseThreshold()
        advanceUntilIdle()

        assertEquals(5, viewModel.uiState.value.baseThreshold)
        assertNotNull(viewModel.uiState.value.rangeError)

        viewModel.onDismissRangeError()
        assertNull(viewModel.uiState.value.rangeError)
    }
}
