package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.model.ExerciseSessionStatus
import com.estebancoloradogonzalez.tension.domain.model.SessionExerciseDetail
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetSessionExercisesUseCaseTest {

    private val repository: SessionRepository = mockk()
    private val useCase = GetSessionExercisesUseCase(repository)

    @Test
    fun `invoke returns exercises for a session`() = runTest {
        val exercises = listOf(
            SessionExerciseDetail(
                sessionExerciseId = 1L,
                exerciseId = 10L,
                name = "Press Banca",
                equipmentTypeName = "Barra",
                muscleZones = listOf("Pecho", "Tríceps"),
                sets = 4,
                reps = "8-12",
                isBodyweight = false,
                isIsometric = false,
                isToTechnicalFailure = false,
                prescribedLoadKg = 60.0,
                completedSets = 0,
                status = ExerciseSessionStatus.NOT_STARTED,
            ),
        )
        every { repository.getSessionExercises(1L) } returns flowOf(exercises)

        val result = useCase(1L).first()

        assertEquals(1, result.size)
        assertEquals("Press Banca", result[0].name)
        assertEquals(60.0, result[0].prescribedLoadKg!!, 0.01)
    }

    @Test
    fun `invoke returns empty list when no exercises`() = runTest {
        every { repository.getSessionExercises(1L) } returns flowOf(emptyList())

        val result = useCase(1L).first()

        assertTrue(result.isEmpty())
    }
}
