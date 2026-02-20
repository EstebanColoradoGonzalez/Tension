package com.estebancoloradogonzalez.tension.ui.alerts

import androidx.lifecycle.SavedStateHandle
import com.estebancoloradogonzalez.tension.domain.model.AlertDetail
import com.estebancoloradogonzalez.tension.domain.model.AlertTriggerData
import com.estebancoloradogonzalez.tension.domain.usecase.alerts.GetAlertDetailUseCase
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
class AlertDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getAlertDetailUseCase: GetAlertDetailUseCase = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads alert detail on init`() = runTest {
        val detail = AlertDetail(
            alertId = 5L,
            type = "PLATEAU",
            level = "HIGH_ALERT",
            entityName = "Press banca",
            message = "3 sesiones sin progresión",
            createdAt = "2026-02-15",
            triggerData = AlertTriggerData.PlateauTrigger(emptyList()),
            causalAnalysis = "Análisis",
            recommendations = listOf("Rec 1"),
            showExerciseHistoryLink = true,
            showDeloadLink = false,
            exerciseId = 10L,
        )
        coEvery { getAlertDetailUseCase(5L) } returns detail

        val savedStateHandle = SavedStateHandle(mapOf("alertId" to 5L))
        val viewModel = AlertDetailViewModel(getAlertDetailUseCase, savedStateHandle)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AlertDetailUiState.Loaded)
        assertEquals("PLATEAU", (state as AlertDetailUiState.Loaded).detail.type)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `throws when alertId missing`() {
        val savedStateHandle = SavedStateHandle()
        AlertDetailViewModel(getAlertDetailUseCase, savedStateHandle)
    }
}
