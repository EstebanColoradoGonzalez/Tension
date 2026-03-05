package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.model.SessionPreviewExercise
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetSessionPreviewUseCaseTest {

    private val sessionRepository: SessionRepository = mockk()
    private val useCase = GetSessionPreviewUseCase(sessionRepository)

    @Test
    fun `invoke returns exercises from repository`() = runTest {
        val exercises = listOf(
            SessionPreviewExercise(
                exerciseId = 1L,
                exerciseName = "Sentadilla",
                moduleCode = "C",
                equipmentTypeName = "Barra",
                muscleZones = "Cuádriceps",
                sets = 4,
                reps = "8-12",
                isBodyweight = false,
                isIsometric = false,
                isToTechnicalFailure = false,
                prescribedLoadKg = 60.0,
                loadIncrementKg = 2.5,
            ),
        )
        every { sessionRepository.getSessionPreviewExercises(1L) } returns flowOf(exercises)

        val result = useCase(1L).first()

        assertEquals(1, result.size)
        assertEquals("Sentadilla", result[0].exerciseName)
    }

    @Test
    fun `invoke returns empty list when no exercises`() = runTest {
        every { sessionRepository.getSessionPreviewExercises(99L) } returns flowOf(emptyList())

        val result = useCase(99L).first()

        assertEquals(0, result.size)
    }
}
