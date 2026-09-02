package com.estebancoloradogonzalez.tension.ui.tree

import com.estebancoloradogonzalez.tension.domain.model.TreeGrowthStage
import com.estebancoloradogonzalez.tension.domain.model.TreeState
import com.estebancoloradogonzalez.tension.domain.repository.TreeRepository
import com.estebancoloradogonzalez.tension.domain.usecase.tree.GetTreeStateUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.tree.RecalculateTreeStateUseCase
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TreeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val treeRepository: TreeRepository = mockk()
    private val getTreeStateUseCase = GetTreeStateUseCase(treeRepository)
    private val recalculateTreeStateUseCase = RecalculateTreeStateUseCase(treeRepository)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { treeRepository.recalculate() } just runs
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // CA-37.06 momento 3 — Abrir la pantalla nunca debe mostrar un valor rancio.

    @Test
    fun `init recalculates before exposing the state`() = runTest {
        every { treeRepository.getTreeState() } returns flowOf(youngTree())

        val viewModel = TreeViewModel(getTreeStateUseCase, recalculateTreeStateUseCase)
        advanceUntilIdle()

        coVerify(exactly = 1) { treeRepository.recalculate() }
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // Si el recalculo falla se muestra lo ultimo persistido: la pantalla nunca queda vacia.

    @Test
    fun `state is still exposed when the recalculation fails`() = runTest {
        coEvery { treeRepository.recalculate() } throws IllegalStateException("db closed")
        every { treeRepository.getTreeState() } returns flowOf(youngTree())

        val viewModel = TreeViewModel(getTreeStateUseCase, recalculateTreeStateUseCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(TreeGrowthStage.YOUNG, state.stage)
    }

    @Test
    fun `state reflects the persisted tree`() = runTest {
        every { treeRepository.getTreeState() } returns flowOf(youngTree())

        val viewModel = TreeViewModel(getTreeStateUseCase, recalculateTreeStateUseCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TreeGrowthStage.YOUNG, state.stage)
        assertEquals(50, state.healthScore)
        assertEquals(8, state.daysSinceLastSession)
        assertTrue(state.hasHistory)
    }

    // CA-37.10 — Sin historial no se muestra conteo de dias.

    @Test
    fun `executant without history sees a seed and no elapsed days`() = runTest {
        every { treeRepository.getTreeState() } returns flowOf(seedTree())

        val viewModel = TreeViewModel(getTreeStateUseCase, recalculateTreeStateUseCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TreeGrowthStage.SEED, state.stage)
        assertEquals(100, state.healthScore)
        assertFalse(state.hasHistory)
        assertNull(state.daysSinceLastSession)
    }

    // CA-37.11 — La ausencia prolongada marchita el arbol, no lo encoge.

    @Test
    fun `long absence keeps the mature stage with zero health`() = runTest {
        every { treeRepository.getTreeState() } returns flowOf(witheredMatureTree())

        val viewModel = TreeViewModel(getTreeStateUseCase, recalculateTreeStateUseCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TreeGrowthStage.MATURE, state.stage)
        assertEquals(0, state.healthScore)
        assertEquals(30, state.daysSinceLastSession)
        assertTrue(state.hasHistory)
    }

    private fun youngTree() = TreeState(
        stage = TreeGrowthStage.YOUNG,
        healthScore = 50,
        daysSinceLastSession = 8,
    )

    private fun seedTree() = TreeState(
        stage = TreeGrowthStage.SEED,
        healthScore = 100,
        daysSinceLastSession = null,
    )

    private fun witheredMatureTree() = TreeState(
        stage = TreeGrowthStage.MATURE,
        healthScore = 0,
        daysSinceLastSession = 30,
    )
}
