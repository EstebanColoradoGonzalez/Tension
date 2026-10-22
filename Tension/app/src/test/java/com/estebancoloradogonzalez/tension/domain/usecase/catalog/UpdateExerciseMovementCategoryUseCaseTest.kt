package com.estebancoloradogonzalez.tension.domain.usecase.catalog

import com.estebancoloradogonzalez.tension.domain.model.MovementCategory
import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import com.estebancoloradogonzalez.tension.domain.rules.CategorizedName
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** HU-46, CA-46.14 — el cambio de categoría de un ejercicio no puede contradecir el plan. */
class UpdateExerciseMovementCategoryUseCaseTest {

    private val repository: ExerciseRepository = mockk()
    private val useCase = UpdateExerciseMovementCategoryUseCase(repository)

    @Test
    fun `given a plan conflict, when changing category, then routines are named`() = runTest {
        // QA-22: Aperturas, de Empuje, a Jalón
        coEvery { repository.getRoutinesAssigningExercise(28L) } returns listOf(
            CategorizedName("Push — Foco Deltoides Lateral y Medio", MovementCategory.PUSH),
            CategorizedName("Push — Foco Tríceps", MovementCategory.PUSH),
        )

        val result = useCase(28L, MovementCategory.PULL)

        assertEquals(
            UpdateExerciseMovementCategoryUseCase.Result.ConflictsWithPlan(
                listOf("Push — Foco Deltoides Lateral y Medio", "Push — Foco Tríceps"),
            ),
            result,
        )
        coVerify(exactly = 0) { repository.updateMovementCategory(any(), any()) }
    }

    @Test
    fun `given no assignments, when changing category, then it is updated`() = runTest {
        // QA-23: Cruce de Polea Alta, fuera del plan
        coEvery { repository.getRoutinesAssigningExercise(2L) } returns emptyList()
        coEvery { repository.updateMovementCategory(2L, MovementCategory.PULL) } just runs

        val result = useCase(2L, MovementCategory.PULL)

        assertEquals(UpdateExerciseMovementCategoryUseCase.Result.Updated, result)
        coVerify { repository.updateMovementCategory(2L, MovementCategory.PULL) }
    }
}
