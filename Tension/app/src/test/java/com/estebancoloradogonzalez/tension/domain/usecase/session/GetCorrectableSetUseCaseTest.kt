package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.model.CorrectableSet
import com.estebancoloradogonzalez.tension.domain.model.WeightUnit
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetCorrectableSetUseCaseTest {

    private val repository: SessionRepository = mockk()
    private val useCase = GetCorrectableSetUseCase(repository)

    @Test
    fun `given the exercise owns the window, when reading, then the registered set is returned`() =
        runTest {
            // Given
            val set = CorrectableSet(
                setId = 77L,
                sessionExerciseId = 5L,
                setNumber = 3,
                totalSets = 3,
                exerciseName = "Elevación Lateral",
                isBodyweight = false,
                isIsometric = false,
                weightKg = 18.0,
                reps = 18,
                rir = 1,
                captureUnit = WeightUnit.KG,
                equipmentTypeId = 6L,
                equipmentTypeName = "Mancuerna",
            )
            coEvery { repository.getCorrectableSet(5L) } returns set

            // When / Then
            assertEquals(set, useCase(5L))
        }

    @Test
    fun `given another exercise owns the window, when reading, then there is nothing to correct`() =
        runTest {
            coEvery { repository.getCorrectableSet(5L) } returns null

            assertNull(useCase(5L))
        }
}
