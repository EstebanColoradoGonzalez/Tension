package com.estebancoloradogonzalez.tension.ui.session

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.estebancoloradogonzalez.tension.domain.model.RegisterSetInfo
import com.estebancoloradogonzalez.tension.domain.model.WeightUnit
import com.estebancoloradogonzalez.tension.domain.usecase.session.GetRegisterSetInfoUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.RegisterSetUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterSetViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getRegisterSetInfoUseCase: GetRegisterSetInfoUseCase = mockk()
    private val registerSetUseCase: RegisterSetUseCase = mockk()
    private val context: Context = mockk()

    private val sessionExerciseId = 42L

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { context.getString(any()) } returns "error"
        every { context.getString(any(), *anyVararg()) } returns "error"
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun info(
        captureUnit: WeightUnit = WeightUnit.KG,
        lastWeightKg: Double? = 40.0,
        isBodyweight: Boolean = false,
        isIsometric: Boolean = false,
    ) = RegisterSetInfo(
        sessionExerciseId = sessionExerciseId,
        exerciseId = 10L,
        exerciseName = "Press de Banca Plano",
        currentSetNumber = 2,
        totalSets = 3,
        lastWeightKg = lastWeightKg,
        isBodyweight = isBodyweight,
        isIsometric = isIsometric,
        isToTechnicalFailure = false,
        prescribedReps = "8-12",
        captureUnit = captureUnit,
    )

    private fun createViewModel(): RegisterSetViewModel {
        return RegisterSetViewModel(
            getRegisterSetInfoUseCase = getRegisterSetInfoUseCase,
            registerSetUseCase = registerSetUseCase,
            savedStateHandle = SavedStateHandle(
                mapOf("sessionExerciseId" to sessionExerciseId),
            ),
            context = context,
        )
    }

    // ----- CA-30.03: unit preselection -----

    @Test
    fun `given the last set was captured in lb, when loading, then the selector starts in lb`() =
        runTest {
            // Given — 20.41 kg was captured as 45 lb
            coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns
                info(captureUnit = WeightUnit.LB, lastWeightKg = 20.41)

            // When
            val viewModel = createViewModel()
            advanceUntilIdle()

            // Then — the prefilled load is shown in the active unit, not in kilograms
            val state = viewModel.uiState.value
            assertEquals(WeightUnit.LB, state.captureUnit)
            assertEquals("45.0", state.weightInput)
            assertEquals(20.41, state.convertedWeightKg!!, 0.0)
            assertNull(state.weightError)
        }

    @Test
    fun `given an exercise with no registered unit, when loading, then the selector starts in kg`() =
        runTest {
            coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns
                info(captureUnit = WeightUnit.KG, lastWeightKg = 40.0)

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(WeightUnit.KG, state.captureUnit)
            assertEquals("40.0", state.weightInput)
            assertTrue(state.isUnitSelectorVisible)
        }

    // ----- CA-30.06: exercises without external load -----

    @Test
    fun `given a bodyweight exercise, when loading, then the selector is hidden and unit is kg`() =
        runTest {
            coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns
                info(isBodyweight = true, lastWeightKg = 0.0)

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isUnitSelectorVisible)
            assertFalse(state.isWeightEditable)
            assertEquals(WeightUnit.KG, state.captureUnit)
            assertEquals("0", state.weightInput)
        }

    @Test
    fun `given an isometric exercise, when loading, then the selector is hidden`() = runTest {
        coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns
            info(isIsometric = true, lastWeightKg = 0.0)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isUnitSelectorVisible)
    }

    // ----- CA-30.01 / CA-30.08: switching unit -----

    @Test
    fun `given kg selected, when switching to lb, then the input keeps the same physical load`() =
        runTest {
            coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns
                info(captureUnit = WeightUnit.KG, lastWeightKg = 20.41)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onUnitSelected(WeightUnit.LB)

            val state = viewModel.uiState.value
            assertEquals(WeightUnit.LB, state.captureUnit)
            assertEquals("45.0", state.weightInput)
            assertNull(state.weightError)
        }

    @Test
    fun `given lb selected, when switching back to kg, then no error is raised`() = runTest {
        coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns
            info(captureUnit = WeightUnit.LB, lastWeightKg = 20.41)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onUnitSelected(WeightUnit.KG)

        val state = viewModel.uiState.value
        assertEquals(WeightUnit.KG, state.captureUnit)
        assertEquals("20.4", state.weightInput)
        assertNull(state.weightError)
    }

    @Test
    fun `given the same unit, when selected again, then nothing changes`() = runTest {
        coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns
            info(captureUnit = WeightUnit.KG, lastWeightKg = 40.0)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onUnitSelected(WeightUnit.KG)

        assertEquals("40.0", viewModel.uiState.value.weightInput)
    }

    // ----- CA-30.04: increment per unit -----

    @Test
    fun `given kg unit, when stepping the weight, then it moves by 0 point 5`() = runTest {
        coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns
            info(captureUnit = WeightUnit.KG, lastWeightKg = 40.0)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onWeightStep(increase = true)
        assertEquals("40.5", viewModel.uiState.value.weightInput)

        viewModel.onWeightStep(increase = false)
        assertEquals("40.0", viewModel.uiState.value.weightInput)
    }

    @Test
    fun `given lb unit, when stepping the weight, then it moves by 1`() = runTest {
        coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns
            info(captureUnit = WeightUnit.LB, lastWeightKg = 20.41)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onWeightStep(increase = true)
        assertEquals("46.0", viewModel.uiState.value.weightInput)
    }

    // ----- CA-30.02: canonical persistence -----

    @Test
    fun `given a weight captured in lb, when confirming, then kilograms are persisted`() = runTest {
        coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns
            info(captureUnit = WeightUnit.LB, lastWeightKg = null)
        coEvery { registerSetUseCase(any(), any(), any(), any(), any()) } just runs

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onWeightChanged("45")
        viewModel.onRepsChanged("10")
        viewModel.onRirSelected(2)
        viewModel.onConfirm()
        advanceUntilIdle()

        coVerify { registerSetUseCase(sessionExerciseId, 20.41, 10, 2, WeightUnit.LB) }
    }

    @Test
    fun `given a weight captured in kg, when confirming, then the value is persisted unchanged`() =
        runTest {
            coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns
                info(captureUnit = WeightUnit.KG, lastWeightKg = null)
            coEvery { registerSetUseCase(any(), any(), any(), any(), any()) } just runs

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onWeightChanged("62.5")
            viewModel.onRepsChanged("8")
            viewModel.onRirSelected(1)
            viewModel.onConfirm()
            advanceUntilIdle()

            coVerify { registerSetUseCase(sessionExerciseId, 62.5, 8, 1, WeightUnit.KG) }
        }

    // ----- CA-30.05: validation on the converted value -----

    @Test
    fun `given 1200 lb, when confirming, then an error is shown and nothing is persisted`() =
        runTest {
            coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns
                info(captureUnit = WeightUnit.LB, lastWeightKg = null)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onWeightChanged("1200")
            viewModel.onRepsChanged("10")
            viewModel.onRirSelected(2)
            viewModel.onConfirm()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNotNull(state.weightError)
            assertFalse(state.isConfirmEnabled)
            coVerify(exactly = 0) { registerSetUseCase(any(), any(), any(), any(), any()) }
        }

    @Test
    fun `given 1100 lb, when typed, then no error is raised because it converts below the limit`() =
        runTest {
            coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns
                info(captureUnit = WeightUnit.LB, lastWeightKg = null)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onWeightChanged("1100")

            assertNull(viewModel.uiState.value.weightError)
        }

    @Test
    fun `given a non numeric weight, when typed, then an error is raised`() = runTest {
        coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns
            info(captureUnit = WeightUnit.KG, lastWeightKg = null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onWeightChanged("abc")

        val state = viewModel.uiState.value
        assertNotNull(state.weightError)
        assertNull(state.convertedWeightKg)
    }

    @Test
    fun `given a negative weight, when typed, then an error is raised`() = runTest {
        coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns
            info(captureUnit = WeightUnit.KG, lastWeightKg = null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onWeightChanged("-5")

        assertNotNull(viewModel.uiState.value.weightError)
    }
}
