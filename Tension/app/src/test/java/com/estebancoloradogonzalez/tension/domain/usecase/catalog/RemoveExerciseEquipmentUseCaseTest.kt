package com.estebancoloradogonzalez.tension.domain.usecase.catalog

import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private const val EXERCISE_ID = 10L
private const val POLEA = 3L

class RemoveExerciseEquipmentUseCaseTest {

    private val repository: ExerciseRepository = mockk()
    private val useCase = RemoveExerciseEquipmentUseCase(repository)

    // CA-39.09 — no se puede quitar la última opción

    @Test
    fun `given a single admitted implement, when removing it, then it is rejected`() = runTest {
        coEvery { repository.countEquipmentOfExercise(EXERCISE_ID) } returns 1

        val result = useCase(EXERCISE_ID, POLEA)

        assertEquals(RemoveExerciseEquipmentUseCase.Result.LastOption, result)
        coVerify(exactly = 0) { repository.removeEquipmentFromExercise(any(), any()) }
    }

    // CA-39.10 — no se puede quitar un implemento con series registradas

    @Test
    fun `given registered sets with the implement, when removing it, then it is rejected`() =
        runTest {
            coEvery { repository.countEquipmentOfExercise(EXERCISE_ID) } returns 3
            coEvery { repository.countSetsWithEquipment(EXERCISE_ID, POLEA) } returns 4

            val result = useCase(EXERCISE_ID, POLEA)

            assertEquals(RemoveExerciseEquipmentUseCase.Result.HasRegisteredSets, result)
            coVerify(exactly = 0) { repository.removeEquipmentFromExercise(any(), any()) }
        }

    @Test
    fun `given no registered sets and more than one option, when removing it, then it is removed`() =
        runTest {
            coEvery { repository.countEquipmentOfExercise(EXERCISE_ID) } returns 3
            coEvery { repository.countSetsWithEquipment(EXERCISE_ID, POLEA) } returns 0
            coEvery { repository.removeEquipmentFromExercise(EXERCISE_ID, POLEA) } just runs

            val result = useCase(EXERCISE_ID, POLEA)

            assertEquals(RemoveExerciseEquipmentUseCase.Result.Removed, result)
            coVerify { repository.removeEquipmentFromExercise(EXERCISE_ID, POLEA) }
        }

    /**
     * La comprobación de la última opción va **antes** que la del historial: con una sola
     * opción el motivo es que quedaría vacío, y decirle al ejecutante que hay series
     * registradas cuando el problema es otro le haría buscar donde no está.
     */
    @Test
    fun `given the last option with registered sets, when removing it, then the reason is the last option`() =
        runTest {
            coEvery { repository.countEquipmentOfExercise(EXERCISE_ID) } returns 1

            val result = useCase(EXERCISE_ID, POLEA)

            assertEquals(RemoveExerciseEquipmentUseCase.Result.LastOption, result)
            coVerify(exactly = 0) { repository.countSetsWithEquipment(any(), any()) }
        }
}
