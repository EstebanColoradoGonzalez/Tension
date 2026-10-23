package com.estebancoloradogonzalez.tension.ui.onboarding

import com.estebancoloradogonzalez.tension.domain.usecase.profile.CreateProfileUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Initial registration with weight and height only (HU-48, CA-48.01). */
@OptIn(ExperimentalCoroutinesApi::class)
class RegisterProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val createProfileUseCase: CreateProfileUseCase = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { createProfileUseCase(any(), any()) } returns Result.success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given the qa combinations, when typed, then Register depends only on weight and height`() {
        // Given — QA-02
        val viewModel = RegisterProfileViewModel(createProfileUseCase)
        val expected = listOf(
            Triple("78.5", "", false),
            Triple("", "1.78", false),
            Triple("0", "1.78", false),
            Triple("78.5", "0", false),
            Triple("0.1", "0.1", true),
            Triple("78.5", "1.78", true),
        )

        expected.forEach { (weight, height, enabled) ->
            // When
            viewModel.onWeightChanged(weight)
            viewModel.onHeightChanged(height)

            // Then
            assertEquals("Register for $weight / $height", enabled, viewModel.uiState.value.isFormValid)
        }
    }

    @Test
    fun `given weight and height, when Register is pressed, then the profile is created with both and home opens`() =
        runTest {
            // Given — QA-01
            val viewModel = RegisterProfileViewModel(createProfileUseCase)
            val events = mutableListOf<RegisterProfileEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }
            viewModel.onWeightChanged("78.5")
            viewModel.onHeightChanged("1.78")

            // When
            viewModel.onRegisterClick()
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { createProfileUseCase(78.5, 1.78) }
            assertEquals(listOf<RegisterProfileEvent>(RegisterProfileEvent.NavigateToHome), events)
        }

    @Test
    fun `given a weight of zero, when Register is pressed, then the weight error shows and nothing is created`() =
        runTest {
            // Given
            val viewModel = RegisterProfileViewModel(createProfileUseCase)
            viewModel.onWeightChanged("0")
            viewModel.onHeightChanged("1.78")

            // When
            viewModel.onRegisterClick()
            advanceUntilIdle()

            // Then
            assertTrue(viewModel.uiState.value.showWeightError)
            coVerify(exactly = 0) { createProfileUseCase(any(), any()) }
        }
}
