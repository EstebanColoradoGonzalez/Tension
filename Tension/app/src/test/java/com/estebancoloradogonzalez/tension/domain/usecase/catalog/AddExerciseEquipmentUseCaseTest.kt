package com.estebancoloradogonzalez.tension.domain.usecase.catalog

import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AddExerciseEquipmentUseCaseTest {

    private val repository: ExerciseRepository = mockk()
    private val useCase = AddExerciseEquipmentUseCase(repository)

    @Test
    fun `given an exercise, when adding an implement, then it is persisted right away`() =
        runTest {
            coEvery { repository.addEquipmentToExercise(10L, 2L) } just runs

            useCase(10L, 2L)

            coVerify { repository.addEquipmentToExercise(10L, 2L) }
        }
}
