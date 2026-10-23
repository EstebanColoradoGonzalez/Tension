package com.estebancoloradogonzalez.tension.ui.profile

import com.estebancoloradogonzalez.tension.domain.model.Profile
import com.estebancoloradogonzalez.tension.domain.usecase.profile.GetProfileUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.profile.UpdateProfileUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.profile.UpdateWeightUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/** Profile edition with weight and height only (HU-48, CA-48.02). */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getProfileUseCase: GetProfileUseCase = mockk()
    private val updateProfileUseCase: UpdateProfileUseCase = mockk()
    private val updateWeightUseCase: UpdateWeightUseCase = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getProfileUseCase() } returns flowOf(
            Profile(
                currentWeightKg = 78.5,
                heightM = 1.78,
                weeklyFrequency = 6,
                plateauBaseThreshold = 10,
                createdAt = LocalDate.of(2026, 9, 2),
            ),
        )
        coEvery { updateProfileUseCase(any()) } returns Result.success(Unit)
        coEvery { updateWeightUseCase(any()) } returns Result.success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ProfileViewModel(getProfileUseCase, updateProfileUseCase, updateWeightUseCase)

    @Test
    fun `given a stored profile, when it loads, then weight and height are the only editable data`() = runTest {
        // Given
        val viewModel = createViewModel()

        // When
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals("78.5", state.weightKg)
        assertEquals("1.78", state.heightM)
        assertTrue(state.isFormValid)
        assertFalse(state.canSave)
    }

    @Test
    fun `given a new height, when it is saved, then only the height is updated and no weight entry is created`() =
        runTest {
            // Given — QA-03
            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onHeightChanged("1.80")

            // When
            assertTrue(viewModel.uiState.value.canSave)
            viewModel.onSaveClick()
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { updateProfileUseCase(1.80) }
            coVerify(exactly = 0) { updateWeightUseCase(any()) }
            assertFalse(viewModel.uiState.value.isDirty)
        }

    @Test
    fun `given a new weight, when it is saved, then it becomes a new entry of the weight history`() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onWeightChanged("79")

        // When
        viewModel.onSaveClick()
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { updateWeightUseCase(79.0) }
        coVerify(exactly = 1) { updateProfileUseCase(1.78) }
    }
}
