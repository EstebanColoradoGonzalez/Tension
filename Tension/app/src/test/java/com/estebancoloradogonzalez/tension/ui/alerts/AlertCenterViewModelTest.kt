package com.estebancoloradogonzalez.tension.ui.alerts

import com.estebancoloradogonzalez.tension.domain.model.AlertItem
import com.estebancoloradogonzalez.tension.domain.usecase.alerts.GetActiveAlertsUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlertCenterViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getActiveAlertsUseCase: GetActiveAlertsUseCase = mockk()

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
        every { getActiveAlertsUseCase() } returns flowOf(emptyList())

        val viewModel = AlertCenterViewModel(getActiveAlertsUseCase)

        assertTrue(viewModel.uiState.value is AlertCenterUiState.Loading)
    }

    @Test
    fun `state transitions to Empty when no alerts`() = runTest {
        every { getActiveAlertsUseCase() } returns flowOf(emptyList())

        val viewModel = AlertCenterViewModel(getActiveAlertsUseCase)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AlertCenterUiState.Empty)
    }

    @Test
    fun `state transitions to Loaded with crisis and regular alerts separated`() = runTest {
        val alerts = listOf(
            AlertItem(1L, "PLATEAU", "HIGH_ALERT", "Press banca", "msg", "2026-02-15"),
            AlertItem(2L, "LOW_PROGRESSION_RATE", "CRISIS", "Sentadilla", "msg2", "2026-02-15"),
            AlertItem(3L, "RIR_OUT_OF_RANGE", "MEDIUM_ALERT", "Módulo A", "msg3", "2026-02-15"),
        )
        every { getActiveAlertsUseCase() } returns flowOf(alerts)

        val viewModel = AlertCenterViewModel(getActiveAlertsUseCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value as AlertCenterUiState.Loaded
        assertEquals(1, state.crisisAlerts.size)
        assertEquals(2, state.regularAlerts.size)
        assertEquals("LOW_PROGRESSION_RATE", state.crisisAlerts[0].type)
    }

    @Test
    fun `totalCount reflects total number of alerts`() = runTest {
        val alerts = listOf(
            AlertItem(1L, "PLATEAU", "HIGH_ALERT", "Press banca", "msg", "2026-02-15"),
            AlertItem(2L, "LOW_ADHERENCE", "MEDIUM_ALERT", "Semanal", "msg", "2026-02-15"),
        )
        every { getActiveAlertsUseCase() } returns flowOf(alerts)

        val viewModel = AlertCenterViewModel(getActiveAlertsUseCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value as AlertCenterUiState.Loaded
        assertEquals(2, state.totalCount)
    }
}
