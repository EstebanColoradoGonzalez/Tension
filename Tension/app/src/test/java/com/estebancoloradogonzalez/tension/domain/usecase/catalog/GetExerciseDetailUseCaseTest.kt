package com.estebancoloradogonzalez.tension.domain.usecase.catalog

import com.estebancoloradogonzalez.tension.domain.model.Exercise
import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class GetExerciseDetailUseCaseTest {

    private lateinit var exerciseRepository: ExerciseRepository
    private lateinit var useCase: GetExerciseDetailUseCase

    @Before
    fun setUp() {
        exerciseRepository = mockk()
        useCase = GetExerciseDetailUseCase(exerciseRepository)
    }

    @Test
    fun `invoke returns exercise when found`() = runTest {
        val exercise = Exercise(
            id = 1,
            name = "Press de banca",
            moduleCode = "A",
            moduleName = "Módulo A — Superior",
            equipmentTypeName = "Máquina",
            muscleZones = listOf("Pecho Medio"),
            isBodyweight = false,
            isIsometric = false,
            isToTechnicalFailure = false,
            isCustom = false,
            mediaResource = "press_de_banca_maquina",
        )
        every { exerciseRepository.getExerciseById(1L) } returns flowOf(exercise)

        val result = useCase(1L).first()

        assertEquals("Press de banca", result?.name)
        assertEquals("press_de_banca_maquina", result?.mediaResource)
        verify(exactly = 1) { exerciseRepository.getExerciseById(1L) }
    }

    @Test
    fun `invoke returns null when exercise not found`() = runTest {
        every { exerciseRepository.getExerciseById(999L) } returns flowOf(null)

        val result = useCase(999L).first()

        assertNull(result)
    }
}
