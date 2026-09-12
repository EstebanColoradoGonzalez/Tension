package com.estebancoloradogonzalez.tension.ui.session

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.domain.model.ExerciseSessionStatus
import com.estebancoloradogonzalez.tension.domain.model.SessionAdjustment
import com.estebancoloradogonzalez.tension.domain.model.SessionExerciseDetail
import com.estebancoloradogonzalez.tension.domain.model.WithdrawnExercise
import com.estebancoloradogonzalez.tension.domain.repository.PlanRepository
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import com.estebancoloradogonzalez.tension.domain.usecase.deload.GetDeloadStateUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.AddExerciseToSessionUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.CloseSessionUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.FinalizeExerciseUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.GetAddableExercisesUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.GetSessionAdjustmentUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.GetSessionExercisesUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.WithdrawFromSessionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveSessionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getSessionExercisesUseCase: GetSessionExercisesUseCase = mockk()
    private val closeSessionUseCase: CloseSessionUseCase = mockk()
    private val finalizeExerciseUseCase: FinalizeExerciseUseCase = mockk()
    private val sessionRepository: SessionRepository = mockk()
    private val planRepository: PlanRepository = mockk()
    private val getDeloadStateUseCase: GetDeloadStateUseCase = mockk()
    private val getSessionAdjustmentUseCase: GetSessionAdjustmentUseCase = mockk()
    private val getAddableExercisesUseCase: GetAddableExercisesUseCase = mockk()
    private val addExerciseToSessionUseCase: AddExerciseToSessionUseCase = mockk()
    private val withdrawFromSessionUseCase: WithdrawFromSessionUseCase = mockk()
    private val context: Context = mockk()

    private val sessionId = 7L

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { context.getString(R.string.session_withdraw_blocked_budget) } returns
            "Añade un ejercicio primero"
        every { context.getString(R.string.session_withdraw_blocked_budget_more) } returns
            "Añade otro ejercicio primero"
        every { context.getString(R.string.session_withdraw_blocked_sets_one) } returns
            "Ya tiene 1 serie registrada"
        every { context.getString(R.string.session_withdraw_blocked_sets, any()) } answers {
            "Ya tiene ${secondArg<Array<Any>>()[0]} series registradas"
        }
        every { context.getString(R.string.session_withdraw_blocked_invariant, any()) } answers {
            "Repón «${secondArg<Array<Any>>()[0]}» primero"
        }
        coEvery { sessionRepository.getDeloadIdBySessionId(sessionId) } returns null
        every { sessionRepository.getSessionRoutineVersion(sessionId) } returns
            flowOf("Pull — Dorsal Ancho" to 1)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun detail(
        id: Long,
        name: String,
        completedSets: Int = 0,
        isExtra: Boolean = false,
    ) = SessionExerciseDetail(
        sessionExerciseId = id,
        exerciseId = id * 10,
        name = name,
        equipmentTypes = listOf("Polea"),
        muscleZones = listOf("Dorsal Ancho"),
        sets = if (isExtra) 3 else 4,
        reps = "8-12",
        isBodyweight = false,
        isIsometric = false,
        isToTechnicalFailure = false,
        prescribedLoadKg = null,
        completedSets = completedSets,
        status = if (completedSets == 0) {
            ExerciseSessionStatus.NOT_STARTED
        } else {
            ExerciseSessionStatus.IN_PROGRESS
        },
        muscleGroup = "Espalda",
        isFinalized = false,
        pendingSelection = false,
        slot = 0,
        hasAlternatives = false,
        isExtra = isExtra,
    )

    private fun viewModel(
        exercises: List<SessionExerciseDetail>,
        adjustment: SessionAdjustment,
    ): ActiveSessionViewModel {
        every { getSessionExercisesUseCase(sessionId) } returns flowOf(exercises)
        every { getSessionAdjustmentUseCase(sessionId) } returns flowOf(adjustment)
        return ActiveSessionViewModel(
            getSessionExercisesUseCase = getSessionExercisesUseCase,
            closeSessionUseCase = closeSessionUseCase,
            finalizeExerciseUseCase = finalizeExerciseUseCase,
            sessionRepository = sessionRepository,
            planRepository = planRepository,
            getDeloadStateUseCase = getDeloadStateUseCase,
            getSessionAdjustmentUseCase = getSessionAdjustmentUseCase,
            getAddableExercisesUseCase = getAddableExercisesUseCase,
            addExerciseToSessionUseCase = addExerciseToSessionUseCase,
            withdrawFromSessionUseCase = withdrawFromSessionUseCase,
            context = context,
            savedStateHandle = SavedStateHandle(mapOf("sessionId" to sessionId)),
        )
    }

    @Test
    fun `sin anadidos ningun ejercicio del plan puede retirarse`() = runTest {
        val vm = viewModel(
            exercises = listOf(detail(1, "Jalón al Pecho"), detail(2, "Curl Bayesian")),
            adjustment = SessionAdjustment(),
        )
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(0, state.withdrawalBudget)
        assertTrue(state.exercises.all { it.withdrawalBlockReason == "Añade un ejercicio primero" })
    }

    @Test
    fun `con un anadido los del plan sin series quedan habilitados`() = runTest {
        val vm = viewModel(
            exercises = listOf(
                detail(1, "Jalón al Pecho", completedSets = 4),
                detail(2, "Curl Bayesian"),
                detail(3, "Press Pallof", isExtra = true),
            ),
            adjustment = SessionAdjustment(addedCount = 1, withdrawnCount = 0),
        )
        advanceUntilIdle()

        val exercises = vm.uiState.value.exercises
        assertEquals(1, vm.uiState.value.withdrawalBudget)
        assertNull(exercises.first { it.sessionExerciseId == 2L }.withdrawalBlockReason)
        // El que tiene series conserva su causa aunque haya presupuesto de sobra.
        assertEquals(
            "Ya tiene 4 series registradas",
            exercises.first { it.sessionExerciseId == 1L }.withdrawalBlockReason,
        )
    }

    @Test
    fun `agotado el presupuesto el mensaje pide anadir otro`() = runTest {
        val vm = viewModel(
            exercises = listOf(detail(2, "Pull-Over"), detail(3, "Press Pallof", isExtra = true)),
            adjustment = SessionAdjustment(
                addedCount = 1,
                withdrawnCount = 1,
                pendingWithdrawals = listOf(WithdrawnExercise(20L, "Curl Bayesian")),
            ),
        )
        advanceUntilIdle()

        assertEquals(
            "Añade otro ejercicio primero",
            vm.uiState.value.exercises.first { it.sessionExerciseId == 2L }.withdrawalBlockReason,
        )
    }

    @Test
    fun `deshacer un anadido con un retiro vigente nombra el ejercicio a reponer`() = runTest {
        val vm = viewModel(
            exercises = listOf(detail(3, "Press Pallof", isExtra = true)),
            adjustment = SessionAdjustment(
                addedCount = 1,
                withdrawnCount = 1,
                pendingWithdrawals = listOf(WithdrawnExercise(20L, "Curl Bayesian")),
            ),
        )
        advanceUntilIdle()

        assertEquals(
            "Repón «Curl Bayesian» primero",
            vm.uiState.value.exercises.first().withdrawalBlockReason,
        )
    }

    @Test
    fun `repuesto el retirado el anadido original ya puede quitarse`() = runTest {
        val vm = viewModel(
            exercises = listOf(
                detail(3, "Press Pallof", isExtra = true),
                detail(4, "Curl Bayesian", isExtra = true),
            ),
            adjustment = SessionAdjustment(
                addedCount = 2,
                withdrawnCount = 1,
                pendingWithdrawals = emptyList(),
            ),
        )
        advanceUntilIdle()

        assertTrue(vm.uiState.value.exercises.all { it.withdrawalBlockReason == null })
    }

    @Test
    fun `una serie registrada bloquea tambien al anadido`() = runTest {
        val vm = viewModel(
            exercises = listOf(detail(3, "Press Pallof", completedSets = 1, isExtra = true)),
            adjustment = SessionAdjustment(addedCount = 1, withdrawnCount = 0),
        )
        advanceUntilIdle()

        assertEquals(
            "Ya tiene 1 serie registrada",
            vm.uiState.value.exercises.first().withdrawalBlockReason,
        )
    }

    @Test
    fun `los anadidos se marcan y llegan al final de la lista`() = runTest {
        // El orden lo fija el SQL; lo que aquí se comprueba es que el ViewModel lo respeta
        // y que la marca viaja hasta el ítem que la pinta.
        val vm = viewModel(
            exercises = listOf(
                detail(1, "Jalón al Pecho"),
                detail(2, "Curl Bayesian"),
                detail(3, "Press Pallof", isExtra = true),
            ),
            adjustment = SessionAdjustment(addedCount = 1, withdrawnCount = 0),
        )
        advanceUntilIdle()

        val exercises = vm.uiState.value.exercises
        assertTrue(exercises.last().isExtra)
        assertEquals(listOf(false, false, true), exercises.map { it.isExtra })
    }

    @Test
    fun `el retiro no se pide cuando la accion esta bloqueada`() = runTest {
        val vm = viewModel(
            exercises = listOf(detail(1, "Jalón al Pecho")),
            adjustment = SessionAdjustment(),
        )
        advanceUntilIdle()

        vm.onWithdrawRequested(vm.uiState.value.exercises.first())
        advanceUntilIdle()

        assertTrue(!vm.withdrawDialogState.value.isVisible)
    }

    @Test
    fun `confirmar el retiro delega en el caso de uso`() = runTest {
        coEvery { withdrawFromSessionUseCase(2L) } just runs
        val vm = viewModel(
            exercises = listOf(detail(2, "Curl Bayesian"), detail(3, "Press Pallof", isExtra = true)),
            adjustment = SessionAdjustment(addedCount = 1, withdrawnCount = 0),
        )
        advanceUntilIdle()

        vm.onWithdrawRequested(vm.uiState.value.exercises.first { it.sessionExerciseId == 2L })
        assertTrue(vm.withdrawDialogState.value.isVisible)
        vm.onWithdrawConfirmed()
        advanceUntilIdle()

        coVerify { withdrawFromSessionUseCase(2L) }
        assertTrue(!vm.withdrawDialogState.value.isVisible)
    }

    @Test
    fun `un retiro rechazado por la guarda de datos llega como error`() = runTest {
        coEvery { withdrawFromSessionUseCase(2L) } throws
            IllegalStateException("Exercise cannot be withdrawn: BudgetExhausted")
        val vm = viewModel(
            exercises = listOf(detail(2, "Curl Bayesian"), detail(3, "Press Pallof", isExtra = true)),
            adjustment = SessionAdjustment(addedCount = 1, withdrawnCount = 0),
        )
        advanceUntilIdle()

        vm.onWithdrawRequested(vm.uiState.value.exercises.first { it.sessionExerciseId == 2L })
        vm.onWithdrawConfirmed()
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `anadir un duplicado llega como error y no cierra la hoja en silencio`() = runTest {
        every { getAddableExercisesUseCase(sessionId) } returns flowOf(emptyList())
        coEvery { addExerciseToSessionUseCase(sessionId, 10L) } throws
            IllegalStateException("Exercise is already in this session")
        val vm = viewModel(
            exercises = listOf(detail(1, "Jalón al Pecho")),
            adjustment = SessionAdjustment(),
        )
        advanceUntilIdle()

        vm.onAddExerciseRequested()
        advanceUntilIdle()
        vm.onExerciseChosen(10L)
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.errorMessage)
        assertTrue(vm.addExerciseSheetState.value.isVisible)
    }
}
