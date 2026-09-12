package com.estebancoloradogonzalez.tension.ui.onerm

import com.estebancoloradogonzalez.tension.domain.model.OneRmEntry
import com.estebancoloradogonzalez.tension.domain.repository.OneRmRepository
import com.estebancoloradogonzalez.tension.domain.usecase.onerm.GetOneRmListUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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

@OptIn(ExperimentalCoroutinesApi::class)
class OneRmViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val repository: OneRmRepository = mockk()
    private val getOneRmListUseCase = GetOneRmListUseCase(repository)

    private val mancuerna = 1L
    private val polea = 2L
    private val elevacionLateral = 10L

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun lateralRaiseWithTwoImplements() = listOf(
        OneRmEntry(elevacionLateral, "Elevación Lateral", mancuerna, "Mancuerna", 16.0),
        OneRmEntry(elevacionLateral, "Elevación Lateral", polea, "Polea", 22.7),
    )

    // CA-42.02 — el primer implemento queda activo al abrir, sin que nadie lo elija.

    @Test
    fun `given records, when loaded, then the first implement of each card is active`() = runTest {
        every { repository.getAll() } returns flowOf(lateralRaiseWithTwoImplements())

        val viewModel = OneRmViewModel(getOneRmListUseCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val card = state.exercises.first()
        assertEquals(mancuerna, state.selectedEquipment(card).equipmentTypeId)
        assertEquals(16.0, state.selectedEquipment(card).oneRmKg, 0.001)
    }

    @Test
    fun `given a card with two implements, when one is selected, then the value changes in place`() =
        runTest {
            every { repository.getAll() } returns flowOf(lateralRaiseWithTwoImplements())

            val viewModel = OneRmViewModel(getOneRmListUseCase)
            advanceUntilIdle()

            viewModel.selectEquipment(elevacionLateral, polea)

            val state = viewModel.uiState.value
            val card = state.exercises.first()
            assertEquals(polea, state.selectedEquipment(card).equipmentTypeId)
            assertEquals(22.7, state.selectedEquipment(card).oneRmKg, 0.001)
            // Un solo ejercicio sigue en la lista: elegir implemento no navega ni recompone.
            assertEquals(1, state.exercises.size)
        }

    /**
     * CA-42.07 — el vacio de verdad se distingue de la carga. Mostrar el mensaje que enuncia
     * la condicion antes de saber si hay datos afirmaria algo que todavia no se sabe.
     */
    @Test
    fun `given no records, when loaded, then the state is empty and not loading`() = runTest {
        every { repository.getAll() } returns flowOf(emptyList())

        val viewModel = OneRmViewModel(getOneRmListUseCase)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun `given nothing has been collected yet, when created, then the state is loading not empty`() =
        runTest {
            every { repository.getAll() } returns flowOf(emptyList())

            val viewModel = OneRmViewModel(getOneRmListUseCase)

            assertTrue(viewModel.uiState.value.isLoading)
            assertFalse(viewModel.uiState.value.isEmpty)
        }

    /**
     * Una seleccion que apunta a un ejercicio que ya no esta se descarta en la siguiente
     * emision, en vez de arrastrarse: la tarjeta vuelve a su primer implemento, que es lo
     * que muestra una tarjeta recien llegada.
     */
    @Test
    fun `given a selection on an exercise that disappears, when reemitted, then it is dropped`() =
        runTest {
            val source = MutableStateFlow(lateralRaiseWithTwoImplements())
            every { repository.getAll() } returns source

            val viewModel = OneRmViewModel(getOneRmListUseCase)
            advanceUntilIdle()
            viewModel.selectEquipment(elevacionLateral, polea)
            assertTrue(viewModel.uiState.value.selection.containsKey(elevacionLateral))

            source.value = listOf(
                OneRmEntry(99L, "Curl Martillo", mancuerna, "Mancuerna", 21.3),
            )
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.selection.containsKey(elevacionLateral))
        }
}
