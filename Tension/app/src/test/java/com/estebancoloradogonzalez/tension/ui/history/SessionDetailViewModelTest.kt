package com.estebancoloradogonzalez.tension.ui.history

import androidx.lifecycle.SavedStateHandle
import com.estebancoloradogonzalez.tension.domain.model.ProgressionClassification
import com.estebancoloradogonzalez.tension.domain.model.SessionDetail
import com.estebancoloradogonzalez.tension.domain.model.SessionDetailExercise
import com.estebancoloradogonzalez.tension.domain.model.SetData
import com.estebancoloradogonzalez.tension.domain.usecase.history.GetSessionDetailUseCase
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
class SessionDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getSessionDetailUseCase: GetSessionDetailUseCase = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads session detail on init`() = runTest {
        val detail = SessionDetail(
            sessionId = 1L,
            date = "2026-02-15",
            moduleCode = "A",
            versionNumber = 1,
            status = "COMPLETED",
            totalTonnageKg = 5000.0,
            totalExercises = 1,
            completedExercises = 1,
            exercises = listOf(
                SessionDetailExercise(
                    exerciseId = 10L,
                    exerciseName = "Press Banca",
                    classification = ProgressionClassification.POSITIVE_PROGRESSION,
                    originalExerciseName = null,
                    sets = listOf(SetData(weightKg = 60.0, reps = 10, rir = 2)),
                ),
            ),
        )
        coEvery { getSessionDetailUseCase(1L) } returns detail

        val savedStateHandle = SavedStateHandle(mapOf("sessionId" to 1L))
        val viewModel = SessionDetailViewModel(getSessionDetailUseCase, savedStateHandle)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SessionDetailUiState.Loaded)
        assertEquals("A", (state as SessionDetailUiState.Loaded).detail.moduleCode)
        assertEquals(1, state.detail.exercises.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `throws when sessionId missing`() {
        val savedStateHandle = SavedStateHandle()
        SessionDetailViewModel(getSessionDetailUseCase, savedStateHandle)
    }
}
