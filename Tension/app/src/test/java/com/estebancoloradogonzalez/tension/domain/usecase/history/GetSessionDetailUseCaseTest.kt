package com.estebancoloradogonzalez.tension.domain.usecase.history

import com.estebancoloradogonzalez.tension.domain.model.ProgressionClassification
import com.estebancoloradogonzalez.tension.domain.model.SessionDetail
import com.estebancoloradogonzalez.tension.domain.model.SessionDetailExercise
import com.estebancoloradogonzalez.tension.domain.model.SetData
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GetSessionDetailUseCaseTest {

    private val repository: SessionRepository = mockk()
    private val useCase = GetSessionDetailUseCase(repository)

    @Test
    fun `invoke returns session detail with exercises and sets`() = runTest {
        val detail = SessionDetail(
            sessionId = 1L,
            date = "2026-02-15",
            moduleCode = "A",
            versionNumber = 1,
            status = "COMPLETED",
            totalTonnageKg = 5000.0,
            totalExercises = 2,
            completedExercises = 2,
            exercises = listOf(
                SessionDetailExercise(
                    exerciseId = 10L,
                    exerciseName = "Press Banca",
                    classification = ProgressionClassification.POSITIVE_PROGRESSION,
                    originalExerciseName = null,
                    sets = listOf(
                        SetData(weightKg = 60.0, reps = 10, rir = 2),
                        SetData(weightKg = 62.5, reps = 9, rir = 2),
                    ),
                ),
                SessionDetailExercise(
                    exerciseId = 11L,
                    exerciseName = "Abdominales",
                    classification = ProgressionClassification.MAINTENANCE,
                    originalExerciseName = null,
                    sets = listOf(
                        SetData(weightKg = 0.0, reps = 15, rir = 3),
                    ),
                ),
            ),
        )
        coEvery { repository.getSessionDetail(1L) } returns detail

        val result = useCase(1L)

        assertEquals(2, result.exercises.size)
        assertEquals("Press Banca", result.exercises[0].exerciseName)
        assertEquals(2, result.exercises[0].sets.size)
        assertEquals(5000.0, result.totalTonnageKg, 0.01)
    }

    @Test
    fun `invoke returns detail with substitution note`() = runTest {
        val detail = SessionDetail(
            sessionId = 2L,
            date = "2026-02-14",
            moduleCode = "B",
            versionNumber = 2,
            status = "COMPLETED",
            totalTonnageKg = 4000.0,
            totalExercises = 1,
            completedExercises = 1,
            exercises = listOf(
                SessionDetailExercise(
                    exerciseId = 20L,
                    exerciseName = "Curl Martillo",
                    classification = ProgressionClassification.POSITIVE_PROGRESSION,
                    originalExerciseName = "Curl Bíceps",
                    sets = listOf(
                        SetData(weightKg = 12.0, reps = 10, rir = 2),
                    ),
                ),
            ),
        )
        coEvery { repository.getSessionDetail(2L) } returns detail

        val result = useCase(2L)

        assertNotNull(result.exercises[0].originalExerciseName)
        assertEquals("Curl Bíceps", result.exercises[0].originalExerciseName)
    }
}
