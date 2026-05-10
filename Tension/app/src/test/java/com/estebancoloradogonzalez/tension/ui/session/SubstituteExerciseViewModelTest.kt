package com.estebancoloradogonzalez.tension.ui.session

import androidx.lifecycle.SavedStateHandle
import com.estebancoloradogonzalez.tension.domain.model.Exercise
import com.estebancoloradogonzalez.tension.domain.model.SubstituteExerciseInfo
import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import com.estebancoloradogonzalez.tension.domain.usecase.session.SubstituteExerciseUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SubstituteExerciseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val substituteExerciseUseCase: SubstituteExerciseUseCase = mockk()
    private val sessionRepository: SessionRepository = mockk()
    private val exerciseRepository: ExerciseRepository = mockk()

    private val sessionExerciseId = 42L
    private val sessionId = 10L
    private val muscleZoneIds = listOf(1L, 2L)

    private val substituteInfo = SubstituteExerciseInfo(
        sessionExerciseId = sessionExerciseId,
        currentExerciseId = 1L,
        currentExerciseName = "Tiro de dorsales (Agarre ancho)",
        sessionId = sessionId,
        muscleZoneIds = muscleZoneIds,
    )

    private val eligibleExercises = listOf(
        Exercise(
            id = 5L,
            name = "Curl de Contracción",
            equipmentTypeName = "Mancuerna",
            muscleZones = listOf("Bíceps"),
            muscleGroup = null,
            isBodyweight = false,
            isIsometric = false,
            isToTechnicalFailure = false,
            isCustom = false,
            mediaResource = null,
        ),
        Exercise(
            id = 6L,
            name = "Curl de martillo",
            equipmentTypeName = "Mancuerna",
            muscleZones = listOf("Bíceps"),
            muscleGroup = null,
            isBodyweight = false,
            isIsometric = false,
            isToTechnicalFailure = false,
            isCustom = false,
            mediaResource = null,
        ),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createSavedStateHandle(): SavedStateHandle {
        return SavedStateHandle(mapOf("sessionExerciseId" to sessionExerciseId))
    }

    private fun createViewModel(): SubstituteExerciseViewModel {
        return SubstituteExerciseViewModel(
            substituteExerciseUseCase = substituteExerciseUseCase,
            sessionRepository = sessionRepository,
            exerciseRepository = exerciseRepository,
            savedStateHandle = createSavedStateHandle(),
        )
    }

    @Test
    fun `initial state is loading`() = runTest {
        coEvery { sessionRepository.getSubstituteExerciseInfo(sessionExerciseId) } returns substituteInfo
        every { exerciseRepository.getEligibleSubstitutes(sessionId, muscleZoneIds) } returns MutableStateFlow(eligibleExercises)

        val viewModel = createViewModel()

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `when getSubstituteExerciseInfo returns null emits navigateBack`() = runTest {
        coEvery { sessionRepository.getSubstituteExerciseInfo(sessionExerciseId) } returns null

        val viewModel = createViewModel()

        val results = mutableListOf<Boolean>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.navigateBack.collect { results.add(it) }
        }
        advanceUntilIdle()

        assertEquals(1, results.size)
        assertTrue(results[0])
    }

    @Test
    fun `successful load transitions to loaded state with exercises`() = runTest {
        coEvery { sessionRepository.getSubstituteExerciseInfo(sessionExerciseId) } returns substituteInfo
        every { exerciseRepository.getEligibleSubstitutes(sessionId, muscleZoneIds) } returns MutableStateFlow(eligibleExercises)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Tiro de dorsales (Agarre ancho)", state.originalExerciseName)
        assertEquals(2, state.eligibleExercises.size)
        assertEquals("Curl de Contracción", state.eligibleExercises[0].name)
        assertEquals("Curl de martillo", state.eligibleExercises[1].name)
    }

    @Test
    fun `getEligibleSubstitutes is invoked with sessionId and muscleZoneIds`() = runTest {
        coEvery { sessionRepository.getSubstituteExerciseInfo(sessionExerciseId) } returns substituteInfo
        every { exerciseRepository.getEligibleSubstitutes(sessionId, muscleZoneIds) } returns MutableStateFlow(eligibleExercises)

        createViewModel()
        advanceUntilIdle()

        coVerify(exactly = 1) { exerciseRepository.getEligibleSubstitutes(sessionId, muscleZoneIds) }
    }

    @Test
    fun `onExerciseSelected updates selectedExercise and shows dialog`() = runTest {
        coEvery { sessionRepository.getSubstituteExerciseInfo(sessionExerciseId) } returns substituteInfo
        every { exerciseRepository.getEligibleSubstitutes(sessionId, muscleZoneIds) } returns MutableStateFlow(eligibleExercises)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val exerciseItem = viewModel.uiState.value.eligibleExercises[0]
        viewModel.onExerciseSelected(exerciseItem)

        val state = viewModel.uiState.value
        assertEquals(exerciseItem, state.selectedExercise)
        assertTrue(state.showConfirmDialog)
    }

    @Test
    fun `onDismissDialog clears selectedExercise and hides dialog`() = runTest {
        coEvery { sessionRepository.getSubstituteExerciseInfo(sessionExerciseId) } returns substituteInfo
        every { exerciseRepository.getEligibleSubstitutes(sessionId, muscleZoneIds) } returns MutableStateFlow(eligibleExercises)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val exerciseItem = viewModel.uiState.value.eligibleExercises[0]
        viewModel.onExerciseSelected(exerciseItem)
        viewModel.onDismissDialog()

        val state = viewModel.uiState.value
        assertNull(state.selectedExercise)
        assertFalse(state.showConfirmDialog)
    }

    @Test
    fun `onConfirmSubstitution invokes use case and emits navigateBack`() = runTest {
        coEvery { sessionRepository.getSubstituteExerciseInfo(sessionExerciseId) } returns substituteInfo
        every { exerciseRepository.getEligibleSubstitutes(sessionId, muscleZoneIds) } returns MutableStateFlow(eligibleExercises)
        coEvery { substituteExerciseUseCase(sessionExerciseId, 5L) } returns Unit

        val viewModel = createViewModel()
        advanceUntilIdle()

        val results = mutableListOf<Boolean>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.navigateBack.collect { results.add(it) }
        }

        val exerciseItem = viewModel.uiState.value.eligibleExercises[0]
        viewModel.onExerciseSelected(exerciseItem)
        viewModel.onConfirmSubstitution()
        advanceUntilIdle()

        coVerify(exactly = 1) { substituteExerciseUseCase(sessionExerciseId, 5L) }
        assertEquals(1, results.size)
        assertTrue(results[0])
    }
}
