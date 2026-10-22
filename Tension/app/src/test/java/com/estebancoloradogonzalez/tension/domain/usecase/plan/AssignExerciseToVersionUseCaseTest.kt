package com.estebancoloradogonzalez.tension.domain.usecase.plan

import com.estebancoloradogonzalez.tension.domain.model.Exercise
import com.estebancoloradogonzalez.tension.domain.model.MovementCategory
import com.estebancoloradogonzalez.tension.domain.model.ProgressionDifficulty
import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import com.estebancoloradogonzalez.tension.domain.repository.PlanRepository
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

private const val POLEA = 3L

/** HU-46, CA-46.12 — una rutina solo admite ejercicios de su misma categoría. */
class AssignExerciseToVersionUseCaseTest {

    private val planRepository: PlanRepository = mockk()
    private val sessionRepository: SessionRepository = mockk()
    private val exerciseRepository: ExerciseRepository = mockk()
    private val useCase =
        AssignExerciseToVersionUseCase(planRepository, sessionRepository, exerciseRepository)

    @Before
    fun setUp() {
        coEvery { sessionRepository.hasActiveDeload() } returns false
        coEvery { sessionRepository.hasActiveSessionForVersion(any()) } returns false
        every { exerciseRepository.getEquipmentIdsOfExercise(any()) } returns flowOf(listOf(POLEA))
        coEvery { planRepository.getMovementCategoryOfVersion(2L) } returns MovementCategory.PULL
        coEvery { planRepository.assignExercise(any(), any(), any(), any(), any()) } just runs
    }

    @Test
    fun `given an exercise of the routine category, when assigning, then it is assigned`() = runTest {
        every { exerciseRepository.getExerciseById(14L) } returns flowOf(exercise(MovementCategory.PULL))

        useCase(routineVersionId = 2L, exerciseId = 14L, sets = 3, reps = "8-12", suggestedEquipmentTypeId = POLEA)

        coVerify { planRepository.assignExercise(2L, 14L, 3, "8-12", POLEA) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `given an exercise of another category, when assigning, then it is rejected`() = runTest {
        // QA-19: Press Militar (Empuje) no entra en «Pull — Foco Dorsal Ancho».
        every { exerciseRepository.getExerciseById(34L) } returns flowOf(exercise(MovementCategory.PUSH))

        useCase(routineVersionId = 2L, exerciseId = 34L, sets = 3, reps = "8-12", suggestedEquipmentTypeId = POLEA)
    }

    private fun exercise(category: MovementCategory) = Exercise(
        id = 1L,
        name = "Ejercicio",
        equipmentOptions = emptyList(),
        primaryMuscleZones = listOf("Zona"),
        secondaryMuscleZones = emptyList(),
        muscleGroup = null,
        isBodyweight = false,
        isIsometric = false,
        isToTechnicalFailure = false,
        isCustom = false,
        mediaResource = null,
        progressionDifficulty = ProgressionDifficulty.MEDIUM,
        movementCategory = category,
    )
}
