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
import org.junit.Before
import org.junit.Test

class GetExercisesUseCaseTest {

    private lateinit var exerciseRepository: ExerciseRepository
    private lateinit var useCase: GetExercisesUseCase

    @Before
    fun setUp() {
        exerciseRepository = mockk()
        useCase = GetExercisesUseCase(exerciseRepository)
    }

    @Test
    fun `invoke returns all exercises from repository`() = runTest {
        val exercises = listOf(
            exercise(id = 1, name = "Press de banca"),
            exercise(id = 2, name = "Curl de bíceps"),
        )
        every { exerciseRepository.getAllExercises() } returns flowOf(exercises)

        val result = useCase().first()

        assertEquals(2, result.size)
        assertEquals("Press de banca", result[0].name)
        assertEquals("Curl de bíceps", result[1].name)
        verify(exactly = 1) { exerciseRepository.getAllExercises() }
    }

    @Test
    fun `invoke returns empty list when no exercises`() = runTest {
        every { exerciseRepository.getAllExercises() } returns flowOf(emptyList())

        val result = useCase().first()

        assertEquals(0, result.size)
    }

    private fun exercise(id: Long, name: String) = Exercise(
        id = id,
        name = name,
        moduleCode = "A",
        moduleName = "Módulo A — Superior",
        equipmentTypeName = "Máquina",
        muscleZones = listOf("Pecho Medio"),
        isBodyweight = false,
        isIsometric = false,
        isToTechnicalFailure = false,
        isCustom = false,
        mediaResource = "test",
    )
}
