package com.estebancoloradogonzalez.tension.domain.usecase.routine

import com.estebancoloradogonzalez.tension.domain.model.MovementCategory
import com.estebancoloradogonzalez.tension.domain.repository.RoutineRepository
import com.estebancoloradogonzalez.tension.domain.rules.CategorizedName
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** HU-46, CA-46.14 — el cambio de categoría de una rutina no puede contradecir sus ejercicios. */
class UpdateRoutineMovementCategoryUseCaseTest {

    private val repository: RoutineRepository = mockk()
    private val useCase = UpdateRoutineMovementCategoryUseCase(repository)

    @Test
    fun `given a plan conflict, when changing category, then exercises are named`() = runTest {
        // QA-24: «Lower — Foco Cuádriceps» a Empuje
        coEvery { repository.getExercisesAssignedToRoutine(3L) } returns listOf(
            CategorizedName("Aductores", MovementCategory.LEGS),
            CategorizedName("Extensión de Cuádriceps", MovementCategory.LEGS),
        )

        val result = useCase(3L, MovementCategory.PUSH)

        assertEquals(
            UpdateRoutineMovementCategoryUseCase.Result.ConflictsWithPlan(
                listOf("Aductores", "Extensión de Cuádriceps"),
            ),
            result,
        )
        coVerify(exactly = 0) { repository.updateMovementCategory(any(), any()) }
    }

    @Test
    fun `given no exercises, when changing category, then it is updated`() = runTest {
        coEvery { repository.getExercisesAssignedToRoutine(7L) } returns emptyList()
        coEvery { repository.updateMovementCategory(7L, MovementCategory.PUSH) } just runs

        val result = useCase(7L, MovementCategory.PUSH)

        assertEquals(UpdateRoutineMovementCategoryUseCase.Result.Updated, result)
    }
}
