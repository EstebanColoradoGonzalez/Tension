package com.estebancoloradogonzalez.tension.domain.usecase.catalog

import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** HU-49 — the percentage is saved only if it is an integer from 0 to 100 (CA-49.05, QA-11). */
class UpdateExerciseBodyWeightPercentUseCaseTest {

    private val repository: ExerciseRepository = mockk()
    private val useCase = UpdateExerciseBodyWeightPercentUseCase(repository)

    @Test
    fun `given a valid percentage, when saved, then the repository saves it and rebuilds`() = runTest {
        // Given — QA-10: «Fondos en Banco» goes to 70 %
        coEvery { repository.updateBodyWeightPercent(7L, 70) } returns Result.success(Unit)

        // When
        val result = useCase(7L, "70")

        // Then
        assertEquals(70, result.getOrNull())
        coVerify(exactly = 1) { repository.updateBodyWeightPercent(7L, 70) }
    }

    @Test
    fun `given the limits zero and one hundred, when saved, then both are accepted`() = runTest {
        // Given
        coEvery { repository.updateBodyWeightPercent(any(), any()) } returns Result.success(Unit)

        // When / Then
        assertEquals(0, useCase(22L, "0").getOrNull())
        assertEquals(100, useCase(22L, "100").getOrNull())
    }

    @Test
    fun `given an invalid value, when saved, then it fails and nothing is written`() = runTest {
        // Given
        val invalid = listOf("-1", "101", "50.5", "")

        invalid.forEach { input ->
            // When
            val result = useCase(22L, input)

            // Then — the exercise keeps its previous value: it is never left without one
            assertTrue(input, result.isFailure)
        }
        coVerify(exactly = 0) { repository.updateBodyWeightPercent(any(), any()) }
    }

    @Test
    fun `given a repository failure, when saved, then the failure is returned`() = runTest {
        // Given
        coEvery { repository.updateBodyWeightPercent(any(), any()) } returns
            Result.failure(IllegalStateException("disk"))

        // When
        val result = useCase(22L, "80")

        // Then
        assertTrue(result.isFailure)
    }
}
