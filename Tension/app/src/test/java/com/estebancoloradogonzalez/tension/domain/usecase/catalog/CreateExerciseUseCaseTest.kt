package com.estebancoloradogonzalez.tension.domain.usecase.catalog

import com.estebancoloradogonzalez.tension.domain.model.ProgressionDifficulty
import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CreateExerciseUseCaseTest {

    private val repository: ExerciseRepository = mockk()
    private val useCase = CreateExerciseUseCase(repository)

    private suspend fun create(
        name: String = "Elevación Lateral",
        equipmentTypeIds: List<Long> = listOf(6L, 3L),
        primaryMuscleZoneIds: List<Long> = listOf(23L),
        secondaryMuscleZoneIds: List<Long> = emptyList(),
    ) = useCase(
        name = name,
        equipmentTypeIds = equipmentTypeIds,
        primaryMuscleZoneIds = primaryMuscleZoneIds,
        secondaryMuscleZoneIds = secondaryMuscleZoneIds,
        isBodyweight = false,
        isIsometric = false,
        isToTechnicalFailure = false,
        mediaResource = null,
        progressionDifficulty = ProgressionDifficulty.HIGH,
    )

    @Test
    fun `given several implements, when creating, then all of them are persisted`() = runTest {
        coEvery { repository.exerciseExistsByName("Elevación Lateral") } returns false
        coEvery {
            repository.createExercise(
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
            )
        } returns 42L

        val id = create()

        assertEquals(42L, id)
        coVerify {
            repository.createExercise(
                name = "Elevación Lateral",
                equipmentTypeIds = listOf(6L, 3L),
                primaryMuscleZoneIds = listOf(23L),
                secondaryMuscleZoneIds = emptyList(),
                isBodyweight = false,
                isIsometric = false,
                isToTechnicalFailure = false,
                mediaResource = null,
                progressionDifficulty = ProgressionDifficulty.HIGH,
            )
        }
    }

    // CA-39.09 — el equipamiento es obligatorio

    @Test(expected = IllegalArgumentException::class)
    fun `given no equipment, when creating, then it is rejected`() = runTest {
        coEvery { repository.exerciseExistsByName(any()) } returns false

        create(equipmentTypeIds = emptyList())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `given no muscle zone, when creating, then it is rejected`() = runTest {
        coEvery { repository.exerciseExistsByName(any()) } returns false

        create(primaryMuscleZoneIds = emptyList())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `given a blank name, when creating, then it is rejected`() = runTest {
        create(name = "   ")
    }

    // CA-39.02 — el nombre es único por sí solo

    @Test(expected = IllegalArgumentException::class)
    fun `given an existing name, when creating with other implements, then it is rejected`() =
        runTest {
            coEvery { repository.exerciseExistsByName("Elevación Lateral") } returns true

            create(equipmentTypeIds = listOf(1L))
        }

    @Test
    fun `given repeated implements, when creating, then duplicates are dropped`() = runTest {
        coEvery { repository.exerciseExistsByName(any()) } returns false
        coEvery {
            repository.createExercise(
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
            )
        } returns 1L

        create(equipmentTypeIds = listOf(6L, 3L, 6L))

        coVerify {
            repository.createExercise(
                name = any(),
                equipmentTypeIds = listOf(6L, 3L),
                primaryMuscleZoneIds = any(),
                secondaryMuscleZoneIds = any(),
                isBodyweight = any(),
                isIsometric = any(),
                isToTechnicalFailure = any(),
                mediaResource = any(),
                progressionDifficulty = any(),
            )
        }
    }

    @Test
    fun `given a padded name, when creating, then it is trimmed`() = runTest {
        coEvery { repository.exerciseExistsByName("Aperturas") } returns false
        coEvery {
            repository.createExercise(
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
            )
        } returns 1L

        create(name = "  Aperturas  ")

        coVerify { repository.exerciseExistsByName("Aperturas") }
    }
}
