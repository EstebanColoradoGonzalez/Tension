package com.estebancoloradogonzalez.tension.domain.usecase.routine

import com.estebancoloradogonzalez.tension.domain.model.MovementCategory
import com.estebancoloradogonzalez.tension.domain.repository.RoutineRepository
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/** HU-46, CA-46.13 — la categoría es obligatoria al crear una rutina. */
class CreateRoutineUseCaseTest {

    private val routineRepository: RoutineRepository = mockk()
    private val sessionRepository: SessionRepository = mockk()
    private val useCase = CreateRoutineUseCase(routineRepository, sessionRepository)

    @Before
    fun setUp() {
        coEvery { routineRepository.routineNameExists(any()) } returns false
        every { sessionRepository.getActiveSession() } returns flowOf(null)
        coEvery { sessionRepository.hasActiveDeload() } returns false
        coEvery { routineRepository.createRoutine(any(), any()) } just runs
    }

    @Test
    fun `given a category, when creating a routine, then it is created with it`() = runTest {
        useCase("Torso Extra", MovementCategory.PULL)

        coVerify { routineRepository.createRoutine("Torso Extra", MovementCategory.PULL) }
    }

    @Test
    fun `given no category, when creating a routine, then it is rejected`() = runTest {
        // QA-21
        try {
            useCase("Torso Extra", null)
            fail("se esperaba el rechazo por categoría")
        } catch (e: IllegalArgumentException) {
            assertEquals("Elige la categoría: Empuje, Jalón o Pierna", e.message)
        }

        coVerify(exactly = 0) { routineRepository.createRoutine(any(), any()) }
    }
}
