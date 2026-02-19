package com.estebancoloradogonzalez.tension.ui.history

import com.estebancoloradogonzalez.tension.domain.model.SessionHistoryItem
import com.estebancoloradogonzalez.tension.domain.usecase.history.GetSessionHistoryUseCase
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
class SessionHistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getSessionHistoryUseCase: GetSessionHistoryUseCase = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() = runTest {
        coEvery { getSessionHistoryUseCase() } returns emptyList()

        val viewModel = SessionHistoryViewModel(getSessionHistoryUseCase)

        assertTrue(viewModel.uiState.value is SessionHistoryUiState.Loading)
    }

    @Test
    fun `state transitions to Loaded with sessions`() = runTest {
        val sessions = listOf(
            SessionHistoryItem(
                sessionId = 1L,
                date = "2026-02-15",
                moduleCode = "A",
                versionNumber = 1,
                status = "COMPLETED",
                totalTonnageKg = 5000.0,
            ),
        )
        coEvery { getSessionHistoryUseCase() } returns sessions

        val viewModel = SessionHistoryViewModel(getSessionHistoryUseCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SessionHistoryUiState.Loaded)
        assertEquals(1, (state as SessionHistoryUiState.Loaded).sessions.size)
    }

    @Test
    fun `state transitions to Empty when no sessions`() = runTest {
        coEvery { getSessionHistoryUseCase() } returns emptyList()

        val viewModel = SessionHistoryViewModel(getSessionHistoryUseCase)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is SessionHistoryUiState.Empty)
    }
}
