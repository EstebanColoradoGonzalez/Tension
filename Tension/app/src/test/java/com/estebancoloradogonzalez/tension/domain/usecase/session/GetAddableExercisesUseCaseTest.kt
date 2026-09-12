package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.model.AddableExercise
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetAddableExercisesUseCaseTest {

    private val repository: SessionRepository = mockk()
    private val useCase = GetAddableExercisesUseCase(repository)

    private fun exercise(id: Long, name: String, inSession: Boolean) = AddableExercise(
        exerciseId = id,
        name = name,
        equipmentSummary = "Polea",
        muscleZonesSummary = "Dorsal Ancho",
        isAlreadyInSession = inSession,
    )

    @Test
    fun `los que ya estan en la sesion viajan marcados y no filtrados`() = runTest {
        every { repository.getAddableExercises(3L) } returns flowOf(
            listOf(
                exercise(1L, "Jalón al Pecho", inSession = true),
                exercise(2L, "Press Pallof", inSession = false),
            ),
        )

        val result = useCase(3L).first()

        assertEquals(2, result.size)
        assertTrue(result.first { it.exerciseId == 1L }.isAlreadyInSession)
        assertTrue(!result.first { it.exerciseId == 2L }.isAlreadyInSession)
    }

    @Test
    fun `un diccionario vacio entra y sale vacio`() = runTest {
        every { repository.getAddableExercises(3L) } returns flowOf(emptyList())

        assertTrue(useCase(3L).first().isEmpty())
    }
}
