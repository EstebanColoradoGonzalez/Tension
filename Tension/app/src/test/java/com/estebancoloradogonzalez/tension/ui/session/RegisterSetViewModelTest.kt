package com.estebancoloradogonzalez.tension.ui.session

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.domain.model.CorrectableSet
import com.estebancoloradogonzalez.tension.domain.model.EquipmentType
import com.estebancoloradogonzalez.tension.domain.model.PrefilledLoad
import com.estebancoloradogonzalez.tension.domain.model.PreselectionOrigin
import com.estebancoloradogonzalez.tension.domain.model.RegisterSetInfo
import com.estebancoloradogonzalez.tension.domain.model.WeightUnit
import com.estebancoloradogonzalez.tension.domain.rules.ExternalLoadRule
import com.estebancoloradogonzalez.tension.domain.rules.SetRangeNotice
import com.estebancoloradogonzalez.tension.domain.usecase.session.CorrectSetUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.GetCorrectableSetUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.GetPrefilledLoadForEquipmentUseCase
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterSetViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getRegisterSetInfoUseCase: GetRegisterSetInfoUseCase = mockk()
    private val getPrefilledLoadForEquipmentUseCase: GetPrefilledLoadForEquipmentUseCase = mockk()
    private val registerSetUseCase: RegisterSetUseCase = mockk()
    private val getCorrectableSetUseCase: GetCorrectableSetUseCase = mockk()
    private val correctSetUseCase: CorrectSetUseCase = mockk()
    private val context: Context = mockk()

    private val sessionExerciseId = 42L

    private val barra = EquipmentType(id = 4L, name = "Barra")
    private val mancuerna = EquipmentType(id = 6L, name = "Mancuerna")
    private val barraFija = EquipmentType(id = 5L, name = "Barra Fija")
    private val maquina = EquipmentType(id = 1L, name = "Máquina")
    private val pesoAnadido = EquipmentType(id = 10L, name = ExternalLoadRule.PESO_ANADIDO)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { context.getString(any()) } returns "error"
        every { context.getString(any(), *anyVararg()) } returns "error"
        // Por defecto el par elegido no tiene historial: el campo queda vacío. Los tests
        // que necesitan memoria del par lo sobrescriben.
        coEvery { getPrefilledLoadForEquipmentUseCase(any(), any()) } returns
            PrefilledLoad(weightKg = null, captureUnit = WeightUnit.KG)
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
        equipmentOptions: List<EquipmentType> = listOf(barra),
        preselectedEquipmentTypeId: Long = equipmentOptions.first().id,
        preselectionOrigin: PreselectionOrigin = PreselectionOrigin.LAST_USED,
        previousSetNotice: SetRangeNotice? = null,
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
        equipmentOptions = equipmentOptions,
        preselectedEquipmentTypeId = preselectedEquipmentTypeId,
        preselectionOrigin = preselectionOrigin,
        previousSetNotice = previousSetNotice,
    )

    private fun createViewModel(correction: Boolean = false): RegisterSetViewModel {
        val args = mutableMapOf<String, Any>("sessionExerciseId" to sessionExerciseId)
        if (correction) args[RegisterSetViewModel.CORRECTION_ARG] = true
        return RegisterSetViewModel(
            getRegisterSetInfoUseCase = getRegisterSetInfoUseCase,
            getPrefilledLoadForEquipmentUseCase = getPrefilledLoadForEquipmentUseCase,
            registerSetUseCase = registerSetUseCase,
            getCorrectableSetUseCase = getCorrectableSetUseCase,
            correctSetUseCase = correctSetUseCase,
            savedStateHandle = SavedStateHandle(args),
            context = context,
        )
    }

    /** The mistyped S3 of `qa.md`: 18 kg × 18 × RIR 1 with dumbbell. */
    private fun correctable(
        weightKg: Double = 18.0,
        reps: Int = 18,
        rir: Int = 1,
        captureUnit: WeightUnit = WeightUnit.KG,
        equipment: EquipmentType = mancuerna,
        isBodyweight: Boolean = false,
        isIsometric: Boolean = false,
    ) = CorrectableSet(
        setId = 77L,
        sessionExerciseId = sessionExerciseId,
        setNumber = 3,
        totalSets = 3,
        exerciseName = "Elevación Lateral",
        isBodyweight = isBodyweight,
        isIsometric = isIsometric,
        weightKg = weightKg,
        reps = reps,
        rir = rir,
        captureUnit = captureUnit,
        equipmentTypeId = equipment.id,
        equipmentTypeName = equipment.name,
    )

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
                info(
                    isBodyweight = true,
                    lastWeightKg = 0.0,
                    equipmentOptions = listOf(barraFija, maquina, pesoAnadido),
                )

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
        coEvery {
            registerSetUseCase(any(), any(), any(), any(), any(), any(), any())
        } returns null

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onWeightChanged("45")
        viewModel.onRepsChanged("10")
        viewModel.onRirSelected(2)
        viewModel.onConfirm()
        advanceUntilIdle()

        coVerify {
            registerSetUseCase(sessionExerciseId, 20.41, 10, 2, WeightUnit.LB, barra.id, barra.name)
        }
    }

    @Test
    fun `given a weight captured in kg, when confirming, then the value is persisted unchanged`() =
        runTest {
            coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns
                info(captureUnit = WeightUnit.KG, lastWeightKg = null)
            coEvery {
            registerSetUseCase(any(), any(), any(), any(), any(), any(), any())
        } returns null

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onWeightChanged("62.5")
            viewModel.onRepsChanged("8")
            viewModel.onRirSelected(1)
            viewModel.onConfirm()
            advanceUntilIdle()

            coVerify {
                registerSetUseCase(
                    sessionExerciseId,
                    62.5,
                    8,
                    1,
                    WeightUnit.KG,
                    barra.id,
                    barra.name,
                )
            }
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
            coVerify(exactly = 0) {
                registerSetUseCase(any(), any(), any(), any(), any(), any(), any())
            }
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

    // ----- CA-39.04: preselección del implemento -----

    @Test
    fun `given a last used implement, when loading, then it comes preselected`() = runTest {
        coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns
            info(
                equipmentOptions = listOf(barra, mancuerna),
                preselectedEquipmentTypeId = mancuerna.id,
            )

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(mancuerna.id, state.selectedEquipmentTypeId)
        assertEquals(mancuerna.name, state.selectedEquipmentName)
    }

    @Test
    fun `given no history, when loading, then the first admitted option is preselected`() =
        runTest {
            coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns
                info(lastWeightKg = null, equipmentOptions = listOf(barra, mancuerna))

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(barra.id, viewModel.uiState.value.selectedEquipmentTypeId)
        }

    @Test
    fun `given a single admitted option, when loading, then it is already resolved`() = runTest {
        coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns
            info(equipmentOptions = listOf(barra))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.equipmentOptions.size)
        assertEquals(barra.id, state.selectedEquipmentTypeId)
    }

    // ----- CA-39.05: el implemento gobierna la captura del peso -----

    @Test
    fun `given dominadas on barra fija, when loading, then the weight is locked at zero`() =
        runTest {
            coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns
                info(
                    isBodyweight = true,
                    lastWeightKg = 0.0,
                    equipmentOptions = listOf(barraFija, maquina, pesoAnadido),
                )

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isWeightEditable)
            assertFalse(state.isAddedWeight)
            assertEquals("0", state.weightInput)
        }

    @Test
    fun `given dominadas on maquina asistida, when loading, then the weight stays at zero`() =
        runTest {
            coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns
                info(
                    isBodyweight = true,
                    lastWeightKg = 0.0,
                    equipmentOptions = listOf(barraFija, maquina, pesoAnadido),
                    preselectedEquipmentTypeId = maquina.id,
                )

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isWeightEditable)
            assertEquals("0", state.weightInput)
        }

    @Test
    fun `given dominadas, when switching to peso anadido, then the field is enabled and cleared`() =
        runTest {
            coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns
                info(
                    isBodyweight = true,
                    lastWeightKg = 0.0,
                    equipmentOptions = listOf(barraFija, maquina, pesoAnadido),
                )

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onEquipmentSelected(pesoAnadido.id)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isWeightEditable)
            assertTrue(state.isAddedWeight)
            assertTrue(state.isUnitSelectorVisible)
            // Se limpia en vez de heredar: lo anterior era 0, no un lastre.
            assertEquals("", state.weightInput)
        }

    @Test
    fun `given peso anadido, when switching back to barra fija, then the field returns to zero`() =
        runTest {
            coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns
                info(
                    isBodyweight = true,
                    lastWeightKg = 0.0,
                    equipmentOptions = listOf(barraFija, maquina, pesoAnadido),
                    preselectedEquipmentTypeId = pesoAnadido.id,
                )

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onWeightChanged("10")
            viewModel.onEquipmentSelected(barraFija.id)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isWeightEditable)
            assertEquals("0", state.weightInput)
        }

    @Test
    fun `given peso anadido with zero load, when confirming, then nothing is persisted`() =
        runTest {
            coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns
                info(
                    isBodyweight = true,
                    lastWeightKg = 0.0,
                    equipmentOptions = listOf(barraFija, maquina, pesoAnadido),
                    preselectedEquipmentTypeId = pesoAnadido.id,
                )
            coEvery {
                registerSetUseCase(any(), any(), any(), any(), any(), any(), any())
            } returns null

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onWeightChanged("0")
            viewModel.onRepsChanged("8")
            viewModel.onRirSelected(1)
            viewModel.onConfirm()
            advanceUntilIdle()

            assertNotNull(viewModel.uiState.value.weightError)
            assertFalse(viewModel.uiState.value.isConfirmEnabled)
            coVerify(exactly = 0) {
                registerSetUseCase(any(), any(), any(), any(), any(), any(), any())
            }
        }

    // ----- CA-40.03: al cambiar de implemento, la precarga se recalcula para el par -----

    @Test
    fun `given the new implement has history, when switching, then the field shows that pair's weight`() =
        runTest {
            // Given — el formulario nace con mancuerna a 12.5 kg
            coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns
                info(lastWeightKg = 12.5, equipmentOptions = listOf(mancuerna, barra))
            // ... y el par de la barra va por 40 kg
            coEvery { getPrefilledLoadForEquipmentUseCase(sessionExerciseId, barra.id) } returns
                PrefilledLoad(weightKg = 40.0, captureUnit = WeightUnit.KG)

            val viewModel = createViewModel()
            advanceUntilIdle()
            assertEquals("12.5", viewModel.uiState.value.weightInput)

            // When
            viewModel.onEquipmentSelected(barra.id)
            advanceUntilIdle()

            // Then — el peso es el del par nuevo, no el que había en el campo
            assertEquals("40.0", viewModel.uiState.value.weightInput)
        }

    @Test
    fun `given the new implement has no history, when switching, then the field is emptied`() =
        runTest {
            // Given — mancuerna con historial, máquina nunca entrenada
            coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns
                info(lastWeightKg = 12.5, equipmentOptions = listOf(mancuerna, maquina))
            coEvery { getPrefilledLoadForEquipmentUseCase(sessionExerciseId, maquina.id) } returns
                PrefilledLoad(weightKg = null, captureUnit = WeightUnit.KG)

            val viewModel = createViewModel()
            advanceUntilIdle()

            // When
            viewModel.onEquipmentSelected(maquina.id)
            advanceUntilIdle()

            // Then — vacío, nunca el peso del otro implemento
            assertEquals("", viewModel.uiState.value.weightInput)
        }

    @Test
    fun `given the new pair was captured in lb, when switching, then the unit changes with it`() =
        runTest {
            coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns
                info(
                    captureUnit = WeightUnit.KG,
                    lastWeightKg = 12.5,
                    equipmentOptions = listOf(mancuerna, maquina),
                )
            coEvery { getPrefilledLoadForEquipmentUseCase(sessionExerciseId, maquina.id) } returns
                PrefilledLoad(weightKg = 20.41, captureUnit = WeightUnit.LB)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onEquipmentSelected(maquina.id)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(WeightUnit.LB, state.captureUnit)
            assertEquals("45.0", state.weightInput)
        }

    @Test
    fun `given the same implement is chosen again, when switching, then nothing is recalculated`() =
        runTest {
            coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns
                info(lastWeightKg = 12.5, equipmentOptions = listOf(mancuerna, barra))

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onEquipmentSelected(mancuerna.id)
            advanceUntilIdle()

            assertEquals("12.5", viewModel.uiState.value.weightInput)
            coVerify(exactly = 0) {
                getPrefilledLoadForEquipmentUseCase(any(), any())
            }
        }

    @Test
    fun `given the executant types a weight, when the prefill arrives, then it does not overwrite it`() =
        runTest {
            // Una sugerencia que llega tarde y pisa lo ya tecleado impone en vez de sugerir.
            coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns
                info(lastWeightKg = 12.5, equipmentOptions = listOf(mancuerna, barra))
            coEvery { getPrefilledLoadForEquipmentUseCase(sessionExerciseId, barra.id) } returns
                PrefilledLoad(weightKg = 40.0, captureUnit = WeightUnit.KG)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onEquipmentSelected(barra.id)
            viewModel.onWeightChanged("35")
            advanceUntilIdle()

            assertEquals("35", viewModel.uiState.value.weightInput)
        }

    @Test
    fun `given a chosen implement, when confirming, then it travels to the use case`() = runTest {
        coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns
            info(lastWeightKg = null, equipmentOptions = listOf(barra, mancuerna))
        coEvery {
            registerSetUseCase(any(), any(), any(), any(), any(), any(), any())
        } returns null

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEquipmentSelected(mancuerna.id)
        viewModel.onWeightChanged("12")
        viewModel.onRepsChanged("10")
        viewModel.onRirSelected(1)
        viewModel.onConfirm()
        advanceUntilIdle()

        coVerify {
            registerSetUseCase(
                sessionExerciseId,
                12.0,
                10,
                1,
                WeightUnit.KG,
                mancuerna.id,
                mancuerna.name,
            )
        }
    }

    // ----- CA-41.05: el rótulo que explica la preselección -----

    @Test
    fun `given the plan suggests the implement, when loading, then the origin says so`() = runTest {
        coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns info(
            equipmentOptions = listOf(barra, mancuerna),
            preselectedEquipmentTypeId = barra.id,
            preselectionOrigin = PreselectionOrigin.PLAN_SUGGESTION,
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(barra.id, viewModel.uiState.value.selectedEquipmentTypeId)
        assertEquals(PreselectionOrigin.PLAN_SUGGESTION, viewModel.uiState.value.preselectionOrigin)
    }

    @Test
    fun `given the executant picks another implement, then the origin label disappears`() = runTest {
        // El último cambio manda: a partir de aquí el implemento es suyo, y un rótulo que
        // siguiera diciendo «sugerido por el plan» sería falso.
        coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns info(
            equipmentOptions = listOf(barra, mancuerna),
            preselectedEquipmentTypeId = barra.id,
            preselectionOrigin = PreselectionOrigin.PLAN_SUGGESTION,
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEquipmentSelected(mancuerna.id)
        advanceUntilIdle()

        assertEquals(mancuerna.id, viewModel.uiState.value.selectedEquipmentTypeId)
        assertNull(viewModel.uiState.value.preselectionOrigin)
    }

    @Test
    fun `given the first admitted option, when loading, then there is no label to show`() = runTest {
        // Ni historial ni sugerencia aplicable: el estado más común de un ejercicio nuevo,
        // y el único sin nada que explicar.
        coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns info(
            equipmentOptions = listOf(barra, mancuerna),
            preselectedEquipmentTypeId = barra.id,
            preselectionOrigin = PreselectionOrigin.FIRST_OPTION,
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(PreselectionOrigin.FIRST_OPTION, viewModel.uiState.value.preselectionOrigin)
    }

    // ----- HU-46, CA-46.05: aviso en directo -----

    @Test
    fun `given the previous set was under 8, when the form opens, then the notice is shown above it`() = runTest {
        every { context.getString(R.string.set_notice_lower_next) } returns "Baja el peso en la próxima serie"
        coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns
            info(previousSetNotice = SetRangeNotice.LowerNextSet)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals("Baja el peso en la próxima serie", viewModel.uiState.value.previousSetNotice)
    }

    @Test
    fun `given no previous notice, when the form opens, then nothing is shown`() = runTest {
        coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns info()

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.previousSetNotice)
    }

    @Test
    fun `given a top set, when confirming, then the session receives the informative notice`() = runTest {
        every { context.getString(R.string.set_notice_top_of_range, 2) } returns "Tope del rango (sesión 2 de 4)"
        coEvery { getRegisterSetInfoUseCase(sessionExerciseId) } returns info(lastWeightKg = 60.0)
        coEvery {
            registerSetUseCase(any(), any(), any(), any(), any(), any(), any())
        } returns SetRangeNotice.TopOfRange(sessionNumber = 2)

        val viewModel = createViewModel()
        advanceUntilIdle()
        val results = mutableListOf<SetRegistered>()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.navigateBack.collect { results.add(it) }
        }

        viewModel.onRepsChanged("12")
        viewModel.onRirSelected(1)
        viewModel.onConfirm()
        advanceUntilIdle()

        coVerify { registerSetUseCase(sessionExerciseId, any(), 12, 1, any(), any(), any()) }
        assertEquals(listOf(SetRegistered(notice = "Tope del rango (sesión 2 de 4)")), results)
        collector.cancel()
    }

    // ----- HU-47: correcting the last registered set -----

    @Test
    fun `given the correction mode, when the form opens, then it is preloaded with what was registered`() =
        runTest {
            // Given
            coEvery { getCorrectableSetUseCase(sessionExerciseId) } returns correctable()

            // When
            val viewModel = createViewModel(correction = true)
            advanceUntilIdle()

            // Then — CA-47.03 / QA-04
            val state = viewModel.uiState.value
            assertTrue(state.isCorrection)
            assertEquals(3, state.currentSetNumber)
            assertEquals("18.0", state.weightInput)
            assertEquals("18", state.reps)
            assertEquals(1, state.selectedRir)
            assertEquals(WeightUnit.KG, state.captureUnit)
            assertEquals(RegisteredSetValues("18.0", WeightUnit.KG, 18, 1), state.registeredValues)
            assertNull(state.previousSetNotice)
            assertTrue(state.isConfirmEnabled)
            coVerify(exactly = 0) { getRegisterSetInfoUseCase(any()) }
        }

    @Test
    fun `given the correction mode, when choosing another implement, then it is ignored`() =
        runTest {
            coEvery { getCorrectableSetUseCase(sessionExerciseId) } returns correctable()

            val viewModel = createViewModel(correction = true)
            advanceUntilIdle()
            viewModel.onEquipmentSelected(barra.id)
            advanceUntilIdle()

            // Then — CA-47.03: visible, resolved, not editable
            val state = viewModel.uiState.value
            assertEquals(listOf(mancuerna), state.equipmentOptions)
            assertEquals(mancuerna.id, state.selectedEquipmentTypeId)
            assertNull(state.preselectionOrigin)
            coVerify(exactly = 0) { getPrefilledLoadForEquipmentUseCase(any(), any()) }
        }

    @Test
    fun `given a correction, when confirming, then the correction is persisted and not a new set`() =
        runTest {
            // Given — QA-05: 18 × 18 → 10 × 10
            every { context.getString(R.string.correct_set_done_format, 3) } returns "Serie 3 corregida"
            coEvery { getCorrectableSetUseCase(sessionExerciseId) } returns correctable()
            coEvery { correctSetUseCase(any(), any(), any(), any(), any(), any(), any()) } just runs

            val viewModel = createViewModel(correction = true)
            advanceUntilIdle()
            val results = mutableListOf<SetRegistered>()
            val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.navigateBack.collect { results.add(it) }
            }

            // When
            viewModel.onWeightChanged("10")
            viewModel.onRepsChanged("10")
            viewModel.onConfirm()
            advanceUntilIdle()

            // Then — CA-47.04
            coVerify {
                correctSetUseCase(sessionExerciseId, 77L, 10.0, 10, 1, WeightUnit.KG, mancuerna.name)
            }
            coVerify(exactly = 0) { registerSetUseCase(any(), any(), any(), any(), any(), any(), any()) }
            assertEquals(listOf(SetRegistered(notice = "Serie 3 corregida")), results)
            collector.cancel()
        }

    @Test
    fun `given a set captured in lb, when correcting, then the preload is in lb and kilograms are persisted`() =
        runTest {
            // Given — QA-20: 132 lb registered (59.87 canonical kg)
            coEvery { getCorrectableSetUseCase(sessionExerciseId) } returns
                correctable(weightKg = 59.87, reps = 10, captureUnit = WeightUnit.LB, equipment = barra)
            coEvery { correctSetUseCase(any(), any(), any(), any(), any(), any(), any()) } just runs

            val viewModel = createViewModel(correction = true)
            advanceUntilIdle()
            assertEquals(WeightUnit.LB, viewModel.uiState.value.captureUnit)
            assertEquals("132.0", viewModel.uiState.value.weightInput)

            // When
            viewModel.onWeightChanged("110")
            viewModel.onConfirm()
            advanceUntilIdle()

            // Then — same conversion as the registration
            coVerify { correctSetUseCase(sessionExerciseId, 77L, 49.9, 10, 1, WeightUnit.LB, barra.name) }
        }

    @Test
    fun `given a weight with two decimals, when only the reps are corrected, then the weight is kept`() =
        runTest {
            // Given — 22.25 kg: rounding it to 22.3 would change a value nobody corrected
            coEvery { getCorrectableSetUseCase(sessionExerciseId) } returns
                correctable(weightKg = 22.25, reps = 18)
            coEvery { correctSetUseCase(any(), any(), any(), any(), any(), any(), any()) } just runs

            val viewModel = createViewModel(correction = true)
            advanceUntilIdle()
            assertEquals("22.25", viewModel.uiState.value.weightInput)

            // When
            viewModel.onRepsChanged("10")
            viewModel.onConfirm()
            advanceUntilIdle()

            // Then
            coVerify { correctSetUseCase(sessionExerciseId, 77L, 22.25, 10, 1, WeightUnit.KG, mancuerna.name) }
        }

    @Test
    fun `given invalid values, when correcting, then confirm is disabled and nothing is persisted`() =
        runTest {
            // Given — QA-09 / QA-10 / QA-11: same messages and same gate as the registration
            coEvery { getCorrectableSetUseCase(sessionExerciseId) } returns correctable()

            val viewModel = createViewModel(correction = true)
            advanceUntilIdle()

            // When
            viewModel.onWeightChanged("-1")

            // Then
            assertNotNull(viewModel.uiState.value.weightError)
            assertFalse(viewModel.uiState.value.isConfirmEnabled)

            viewModel.onWeightChanged("10")
            viewModel.onRepsChanged("0")
            assertNotNull(viewModel.uiState.value.repsError)
            assertFalse(viewModel.uiState.value.isConfirmEnabled)

            viewModel.onRepsChanged("10")
            assertTrue(viewModel.uiState.value.isConfirmEnabled)
            coVerify(exactly = 0) { correctSetUseCase(any(), any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `given an isometric set, when correcting, then seconds are typed and the weight stays at zero`() =
        runTest {
            // Given — QA-14: plank, 0 kg × 45 s × RIR 2
            val pesoCorporal = EquipmentType(id = 11L, name = ExternalLoadRule.PESO_CORPORAL)
            coEvery { getCorrectableSetUseCase(sessionExerciseId) } returns correctable(
                weightKg = 0.0,
                reps = 45,
                rir = 2,
                equipment = pesoCorporal,
                isBodyweight = true,
                isIsometric = true,
            )
            coEvery { correctSetUseCase(any(), any(), any(), any(), any(), any(), any()) } just runs

            val viewModel = createViewModel(correction = true)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.showChronometer)
            assertFalse(state.isWeightEditable)
            assertEquals("0", state.weightInput)
            assertEquals("45", state.reps)

            // When
            viewModel.onRepsChanged("40")
            viewModel.onConfirm()
            advanceUntilIdle()

            // Then
            coVerify {
                correctSetUseCase(sessionExerciseId, 77L, 0.0, 40, 2, WeightUnit.KG, pesoCorporal.name)
            }
        }

    @Test
    fun `given an added weight set, when correcting, then the ballast stays editable`() = runTest {
        // Given — QA-15: push-ups with 5 kg of «Peso Añadido»
        coEvery { getCorrectableSetUseCase(sessionExerciseId) } returns
            correctable(weightKg = 5.0, reps = 12, equipment = pesoAnadido, isBodyweight = true)
        coEvery { correctSetUseCase(any(), any(), any(), any(), any(), any(), any()) } just runs

        val viewModel = createViewModel(correction = true)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isWeightEditable)
        assertEquals("5.0", viewModel.uiState.value.weightInput)

        // When
        viewModel.onWeightChanged("2.5")
        viewModel.onConfirm()
        advanceUntilIdle()

        // Then
        coVerify {
            correctSetUseCase(sessionExerciseId, 77L, 2.5, 12, 1, WeightUnit.KG, pesoAnadido.name)
        }
    }

    @Test
    fun `given the window moved before opening, when the form loads, then it goes back without message`() =
        runTest {
            // Given — QA-12: the set is no longer the last registered one
            coEvery { getCorrectableSetUseCase(sessionExerciseId) } returns null

            // When
            val viewModel = createViewModel(correction = true)
            advanceUntilIdle()

            // Then
            assertTrue(viewModel.uiState.value.isCorrectionWindowClosed)
            assertFalse(viewModel.uiState.value.isConfirmEnabled)
        }

    @Test
    fun `given the window closed while correcting, when confirming, then it goes back without message`() =
        runTest {
            coEvery { getCorrectableSetUseCase(sessionExerciseId) } returns correctable()
            coEvery { correctSetUseCase(any(), any(), any(), any(), any(), any(), any()) } throws
                IllegalStateException("Set is no longer correctable")

            val viewModel = createViewModel(correction = true)
            advanceUntilIdle()
            val results = mutableListOf<SetRegistered>()
            val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.navigateBack.collect { results.add(it) }
            }

            viewModel.onConfirm()
            advanceUntilIdle()

            assertEquals(listOf(SetRegistered(notice = null)), results)
            collector.cancel()
        }
}
