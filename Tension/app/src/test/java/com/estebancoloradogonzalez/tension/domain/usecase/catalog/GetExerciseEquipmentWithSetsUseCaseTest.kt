package com.estebancoloradogonzalez.tension.domain.usecase.catalog

import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private const val EXERCISE_ID = 10L

class GetExerciseEquipmentWithSetsUseCaseTest {

    private val repository: ExerciseRepository = mockk()
    private val useCase = GetExerciseEquipmentWithSetsUseCase(repository)

    @Test
    fun `given sets on some implements, when asked, then only those come back`() = runTest {
        coEvery { repository.countSetsWithEquipment(EXERCISE_ID, 6L) } returns 8
        coEvery { repository.countSetsWithEquipment(EXERCISE_ID, 3L) } returns 0
        coEvery { repository.countSetsWithEquipment(EXERCISE_ID, 1L) } returns 2

        val result = useCase(EXERCISE_ID, listOf(6L, 3L, 1L))

        assertEquals(setOf(6L, 1L), result)
    }

    @Test
    fun `given no sets at all, when asked, then the result is empty`() = runTest {
        coEvery { repository.countSetsWithEquipment(EXERCISE_ID, any()) } returns 0

        val result = useCase(EXERCISE_ID, listOf(6L, 3L))

        assertEquals(emptySet<Long>(), result)
    }

    /** Se pregunta solo por lo que el ejercicio admite, no por los quince tipos. */
    @Test
    fun `given the admitted list, when asked, then nothing outside it is queried`() = runTest {
        coEvery { repository.countSetsWithEquipment(EXERCISE_ID, any()) } returns 0

        useCase(EXERCISE_ID, listOf(6L))

        coVerify(exactly = 1) { repository.countSetsWithEquipment(EXERCISE_ID, any()) }
    }
}
