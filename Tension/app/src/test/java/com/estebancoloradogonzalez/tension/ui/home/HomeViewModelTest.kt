package com.estebancoloradogonzalez.tension.ui.home

import com.estebancoloradogonzalez.tension.domain.model.ActiveSession
import com.estebancoloradogonzalez.tension.domain.model.DayOutcome
import com.estebancoloradogonzalez.tension.domain.model.DeloadState
import com.estebancoloradogonzalez.tension.domain.model.NextSession
import com.estebancoloradogonzalez.tension.domain.model.ReassignableRoutine
import com.estebancoloradogonzalez.tension.domain.model.TodaySession
import com.estebancoloradogonzalez.tension.domain.model.TreeGrowthStage
import com.estebancoloradogonzalez.tension.domain.model.TreeState
import com.estebancoloradogonzalez.tension.domain.model.UpcomingSession
import com.estebancoloradogonzalez.tension.domain.model.WeekDay
import com.estebancoloradogonzalez.tension.domain.usecase.alerts.GetActiveAlertCountUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.deload.GetDeloadStateUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.ClearTemporaryRoutineUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.GetActiveSessionUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.GetMicrocycleCountUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.GetReassignableRoutinesUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.GetTodaySessionUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.SetTemporaryRoutineUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.SkipTodayUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.StartSessionUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.session.UndoSkipTodayUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.tree.GetTreeStateUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getTodaySessionUseCase: GetTodaySessionUseCase = mockk()
    private val getActiveSessionUseCase: GetActiveSessionUseCase = mockk()
    private val startSessionUseCase: StartSessionUseCase = mockk()
    private val getMicrocycleCountUseCase: GetMicrocycleCountUseCase = mockk()
    private val getDeloadStateUseCase: GetDeloadStateUseCase = mockk()
    private val getActiveAlertCountUseCase: GetActiveAlertCountUseCase = mockk()
    private val getReassignableRoutinesUseCase: GetReassignableRoutinesUseCase = mockk()
    private val setTemporaryRoutineUseCase: SetTemporaryRoutineUseCase = mockk()
    private val clearTemporaryRoutineUseCase: ClearTemporaryRoutineUseCase = mockk()
    private val skipTodayUseCase: SkipTodayUseCase = mockk()
    private val undoSkipTodayUseCase: UndoSkipTodayUseCase = mockk()
    private val getTreeStateUseCase: GetTreeStateUseCase = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getMicrocycleCountUseCase() } returns flowOf(14)
        every { getActiveAlertCountUseCase() } returns flowOf(0)
        every { getReassignableRoutinesUseCase() } returns flowOf(reassignOptions())
        coEvery { getDeloadStateUseCase() } returns DeloadState.NoDeloadNeeded
        every { getTreeStateUseCase() } returns flowOf(sproutTree())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // CA-36.07 — Domingo como día sin rutina asignada

    @Test
    fun `rest day shows the rest card and no session card`() = runTest {
        every { getTodaySessionUseCase() } returns flowOf(
            TodaySession(weekDay = WeekDay.SUNDAY, isRestDay = true),
        )
        every { getActiveSessionUseCase() } returns flowOf(null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showRestDayCard)
        assertFalse(state.showNextSessionCard)
        assertTrue(state.canReassign)
    }

    // CA-36.02 — Reasignación temporal

    @Test
    fun `temporary override is reported by the state`() = runTest {
        every { getTodaySessionUseCase() } returns flowOf(overriddenTodaySession())
        every { getActiveSessionUseCase() } returns flowOf(null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isTemporaryOverride)
        assertTrue(state.showNextSessionCard)
        assertEquals(2L, state.nextSession?.routineId)
    }

    // CA-36.06 — Reasignación solo antes de iniciar

    @Test
    fun `reassignment is unavailable while a session is active`() = runTest {
        every { getTodaySessionUseCase() } returns flowOf(regularTodaySession())
        every { getActiveSessionUseCase() } returns flowOf(activeSession())

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.canReassign)
        assertFalse(viewModel.uiState.value.showNextSessionCard)
    }

    // Cancelar el día es la única vía, y deja de estarlo en cuanto hay una serie

    @Test
    fun `cancelling the day is offered for an open session without sets`() = runTest {
        every { getTodaySessionUseCase() } returns flowOf(regularTodaySession())
        every { getActiveSessionUseCase() } returns flowOf(activeSession(registeredSets = 0))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showSkipToday)
        assertTrue(viewModel.uiState.value.canSkipToday)
    }

    @Test
    fun `cancelling the day is shown but blocked once a set is registered`() = runTest {
        every { getTodaySessionUseCase() } returns flowOf(regularTodaySession())
        every { getActiveSessionUseCase() } returns flowOf(activeSession(registeredSets = 1))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showSkipToday)
        assertFalse(viewModel.uiState.value.canSkipToday)
    }

    @Test
    fun `skipToday does nothing when the open session already has sets`() = runTest {
        every { getTodaySessionUseCase() } returns flowOf(regularTodaySession())
        every { getActiveSessionUseCase() } returns flowOf(activeSession(registeredSets = 3))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.skipToday()
        advanceUntilIdle()

        coVerify(exactly = 0) { skipTodayUseCase() }
    }

    @Test
    fun `openReassignDialog does nothing while a session is active`() = runTest {
        every { getTodaySessionUseCase() } returns flowOf(regularTodaySession())
        every { getActiveSessionUseCase() } returns flowOf(activeSession())

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.openReassignDialog()

        assertFalse(viewModel.uiState.value.isReassignDialogOpen)
    }

    @Test
    fun `openReassignDialog opens the selector when no session is active`() = runTest {
        every { getTodaySessionUseCase() } returns flowOf(regularTodaySession())
        every { getActiveSessionUseCase() } returns flowOf(null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.openReassignDialog()

        assertTrue(viewModel.uiState.value.isReassignDialogOpen)
        assertEquals(2, viewModel.uiState.value.reassignOptions.size)
    }

    @Test
    fun `confirmReassign closes the selector and persists the override`() = runTest {
        every { getTodaySessionUseCase() } returns flowOf(regularTodaySession())
        every { getActiveSessionUseCase() } returns flowOf(null)
        coEvery { setTemporaryRoutineUseCase(2L) } just runs

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.openReassignDialog()

        viewModel.confirmReassign(2L)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isReassignDialogOpen)
        coVerify { setTemporaryRoutineUseCase(2L) }
    }

    @Test
    fun `confirmReassign surfaces the error when the use case rejects it`() = runTest {
        every { getTodaySessionUseCase() } returns flowOf(regularTodaySession())
        every { getActiveSessionUseCase() } returns flowOf(null)
        coEvery { setTemporaryRoutineUseCase(2L) } throws IllegalStateException("rechazado")

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.confirmReassign(2L)
        advanceUntilIdle()

        assertEquals("rechazado", viewModel.uiState.value.errorMessage)
    }

    // CA-36.04 — Deshacer devuelve el día a su relación permanente

    @Test
    fun `undoReassign clears the override`() = runTest {
        every { getTodaySessionUseCase() } returns flowOf(overriddenTodaySession())
        every { getActiveSessionUseCase() } returns flowOf(null)
        coEvery { clearTemporaryRoutineUseCase() } just runs

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.undoReassign()
        advanceUntilIdle()

        coVerify { clearTemporaryRoutineUseCase() }
    }

    // Un día resuelto no vuelve a proponerse: es lo que evita varias sesiones el mismo día

    @Test
    fun `a day already trained shows the resolved card and hides the session card`() = runTest {
        every { getTodaySessionUseCase() } returns flowOf(trainedTodaySession())
        every { getActiveSessionUseCase() } returns flowOf(null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showResolvedCard)
        assertFalse(state.showNextSessionCard)
        assertEquals(DayOutcome.TRAINED, state.dayOutcome)
    }

    @Test
    fun `a resolved day reports the upcoming session without making it startable`() = runTest {
        every { getTodaySessionUseCase() } returns flowOf(trainedTodaySession())
        every { getActiveSessionUseCase() } returns flowOf(null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(WeekDay.FRIDAY, state.upcoming?.weekDay)
        assertNull(state.nextSession)
    }

    @Test
    fun `a resolved day offers neither reassignment nor skipping`() = runTest {
        every { getTodaySessionUseCase() } returns flowOf(trainedTodaySession())
        every { getActiveSessionUseCase() } returns flowOf(null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.canReassign)
        assertFalse(viewModel.uiState.value.canSkipToday)
    }

    @Test
    fun `skipToday resolves the day without a session`() = runTest {
        every { getTodaySessionUseCase() } returns flowOf(regularTodaySession())
        every { getActiveSessionUseCase() } returns flowOf(null)
        coEvery { skipTodayUseCase() } just runs

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.skipToday()
        advanceUntilIdle()

        coVerify { skipTodayUseCase() }
    }

    @Test
    fun `skipToday does nothing on an already resolved day`() = runTest {
        every { getTodaySessionUseCase() } returns flowOf(skippedTodaySession())
        every { getActiveSessionUseCase() } returns flowOf(null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.skipToday()
        advanceUntilIdle()

        coVerify(exactly = 0) { skipTodayUseCase() }
    }

    @Test
    fun `undoSkipToday gives the day back its session`() = runTest {
        every { getTodaySessionUseCase() } returns flowOf(skippedTodaySession())
        every { getActiveSessionUseCase() } returns flowOf(null)
        coEvery { undoSkipTodayUseCase() } just runs

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.undoSkipToday()
        advanceUntilIdle()

        coVerify { undoSkipTodayUseCase() }
    }

    // CA-37.01 — La tarjeta de acceso refleja el estado actual del arbol

    @Test
    fun `tree state reaches the ui state`() = runTest {
        every { getTodaySessionUseCase() } returns flowOf(regularTodaySession())
        every { getActiveSessionUseCase() } returns flowOf(null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TreeGrowthStage.SPROUT, state.treeState?.stage)
        assertEquals(100, state.treeState?.healthScore)
    }

    // El arbol no viaja en el combine del estado del dia: si el copy no lo preservara, cada
    // emision del dia lo borraria de la pantalla.

    @Test
    fun `tree state survives a later emission of the day state`() = runTest {
        val todaySessions = MutableSharedFlow<TodaySession>(replay = 1)
        todaySessions.tryEmit(regularTodaySession())
        every { getTodaySessionUseCase() } returns todaySessions
        every { getActiveSessionUseCase() } returns flowOf(null)

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.treeState)

        todaySessions.tryEmit(trainedTodaySession())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(DayOutcome.TRAINED, state.dayOutcome)
        assertEquals(TreeGrowthStage.SPROUT, state.treeState?.stage)
    }

    // CA-37.10 — Sin historial no hay conteo de dias que mostrar

    @Test
    fun `tree without history reports no elapsed days`() = runTest {
        every { getTodaySessionUseCase() } returns flowOf(regularTodaySession())
        every { getActiveSessionUseCase() } returns flowOf(null)
        every { getTreeStateUseCase() } returns flowOf(seedTree())

        val viewModel = createViewModel()
        advanceUntilIdle()

        val treeState = viewModel.uiState.value.treeState
        assertEquals(TreeGrowthStage.SEED, treeState?.stage)
        assertFalse(treeState?.hasHistory == true)
        assertNull(treeState?.daysSinceLastSession)
    }

    private fun sproutTree() = TreeState(
        stage = TreeGrowthStage.SPROUT,
        healthScore = 100,
        daysSinceLastSession = 0,
    )

    private fun seedTree() = TreeState(
        stage = TreeGrowthStage.SEED,
        healthScore = 100,
        daysSinceLastSession = null,
    )

    private fun createViewModel() = HomeViewModel(
        getTodaySessionUseCase = getTodaySessionUseCase,
        getActiveSessionUseCase = getActiveSessionUseCase,
        startSessionUseCase = startSessionUseCase,
        getMicrocycleCountUseCase = getMicrocycleCountUseCase,
        getDeloadStateUseCase = getDeloadStateUseCase,
        getActiveAlertCountUseCase = getActiveAlertCountUseCase,
        getReassignableRoutinesUseCase = getReassignableRoutinesUseCase,
        setTemporaryRoutineUseCase = setTemporaryRoutineUseCase,
        clearTemporaryRoutineUseCase = clearTemporaryRoutineUseCase,
        skipTodayUseCase = skipTodayUseCase,
        undoSkipTodayUseCase = undoSkipTodayUseCase,
        getTreeStateUseCase = getTreeStateUseCase,
    )

    private fun regularTodaySession() = TodaySession(
        weekDay = WeekDay.THURSDAY,
        session = NextSession(
            routineId = 4L,
            routineName = "Push — Foco Tríceps",
            versionNumber = 1,
            routineVersionId = 4L,
        ),
    )

    private fun overriddenTodaySession() = TodaySession(
        weekDay = WeekDay.THURSDAY,
        session = NextSession(
            routineId = 2L,
            routineName = "Pull — Foco Dorsal Ancho",
            versionNumber = 1,
            routineVersionId = 2L,
        ),
        isTemporaryOverride = true,
        overriddenFromWeekDay = WeekDay.TUESDAY,
    )

    private fun activeSession(registeredSets: Int = 4) = ActiveSession(
        sessionId = 9L,
        routineName = "Push — Foco Tríceps",
        versionNumber = 1,
        completedExercises = 1,
        totalExercises = 5,
        registeredSets = registeredSets,
    )

    private fun trainedTodaySession() = TodaySession(
        weekDay = WeekDay.THURSDAY,
        dayOutcome = DayOutcome.TRAINED,
        upcoming = UpcomingSession(
            weekDay = WeekDay.FRIDAY,
            routineName = "Pull — Foco Trapecios y Espalda Media",
            versionNumber = 1,
        ),
    )

    private fun skippedTodaySession() = TodaySession(
        weekDay = WeekDay.THURSDAY,
        dayOutcome = DayOutcome.SKIPPED,
        upcoming = UpcomingSession(
            weekDay = WeekDay.FRIDAY,
            routineName = "Pull — Foco Trapecios y Espalda Media",
            versionNumber = 1,
        ),
    )

    private fun reassignOptions() = listOf(
        ReassignableRoutine(
            routineId = 4L,
            routineName = "Push — Foco Tríceps",
            routineVersionId = 4L,
            versionNumber = 1,
            weekDays = listOf(WeekDay.THURSDAY),
            isTodaysRoutine = true,
        ),
        ReassignableRoutine(
            routineId = 2L,
            routineName = "Pull — Foco Dorsal Ancho",
            routineVersionId = 2L,
            versionNumber = 1,
            weekDays = listOf(WeekDay.TUESDAY),
            isTodaysRoutine = false,
        ),
    )
}
