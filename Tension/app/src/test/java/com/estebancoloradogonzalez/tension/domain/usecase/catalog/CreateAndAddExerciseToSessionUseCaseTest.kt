package com.estebancoloradogonzalez.tension.domain.usecase.catalog

import com.estebancoloradogonzalez.tension.domain.model.ProgressionDifficulty
import com.estebancoloradogonzalez.tension.domain.usecase.session.AddExerciseToSessionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class CreateAndAddExerciseToSessionUseCaseTest {

    private val createExerciseUseCase: CreateExerciseUseCase = mockk()
    private val addExerciseToSessionUseCase: AddExerciseToSessionUseCase = mockk()
    private val useCase = CreateAndAddExerciseToSessionUseCase(
        createExerciseUseCase,
        addExerciseToSessionUseCase,
    )

    private suspend fun invoke() = useCase(
        sessionId = 7L,
        name = "Press Pallof",
        equipmentTypeIds = listOf(1L),
        primaryMuscleZoneIds = listOf(2L),
        secondaryMuscleZoneIds = listOf(3L),
        isBodyweight = false,
        isIsometric = false,
        isToTechnicalFailure = false,
        mediaResource = null,
        progressionDifficulty = ProgressionDifficulty.MEDIUM,
    )

    @Test
    fun `crea el ejercicio y lo anade a la sesion en el mismo gesto`() = runTest {
        coEvery { createExerciseUseCase(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns 42L
        coEvery { addExerciseToSessionUseCase(7L, 42L) } returns 99L

        val exerciseId = invoke()

        assertEquals(42L, exerciseId)
        coVerify { addExerciseToSessionUseCase(7L, 42L) }
    }

    @Test
    fun `si el anadido falla el ejercicio sigue creado en el diccionario`() = runTest {
        // La asimetría es del negocio, no un descuido: CA-43.02 exige que el ejercicio
        // permanezca en el Diccionario aunque la sesión se cierre, se abandone o el
        // ejercicio se retire de ella. Una transacción conjunta produciría justo el
        // resultado prohibido — un ejercicio creado que se desvanece.
        coEvery { createExerciseUseCase(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns 42L
        coEvery { addExerciseToSessionUseCase(7L, 42L) } throws
            IllegalStateException("Cannot adjust a session that is not in progress")

        try {
            invoke()
            fail("se esperaba la excepción del añadido")
        } catch (_: IllegalStateException) {
            // esperado
        }

        coVerify { createExerciseUseCase(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `si la creacion falla no se intenta anadir nada`() = runTest {
        coEvery { createExerciseUseCase(any(), any(), any(), any(), any(), any(), any(), any(), any()) } throws
            IllegalArgumentException("An exercise with this name already exists")

        try {
            invoke()
            fail("se esperaba la excepción de la creación")
        } catch (_: IllegalArgumentException) {
            // esperado
        }

        coVerify(exactly = 0) { addExerciseToSessionUseCase(any(), any()) }
    }
}
