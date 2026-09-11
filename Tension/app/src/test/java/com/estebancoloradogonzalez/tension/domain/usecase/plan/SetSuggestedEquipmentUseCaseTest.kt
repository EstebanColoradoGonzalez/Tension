package com.estebancoloradogonzalez.tension.domain.usecase.plan

import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import com.estebancoloradogonzalez.tension.domain.repository.PlanRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

private const val ROUTINE_VERSION_ID = 5L

/** `Remo al Mentón`: admite Barra, Polea y Mancuerna. */
private const val EXERCISE_ID = 27L
private const val MAQUINA = 1L
private const val POLEA = 3L
private const val BARRA = 4L
private const val MANCUERNA = 6L

/** CA-41.08 — la sugerencia del plan solo puede ser una opción que el ejercicio admita. */
class SetSuggestedEquipmentUseCaseTest {

    private val planRepository: PlanRepository = mockk(relaxUnitFun = true)
    private val exerciseRepository: ExerciseRepository = mockk()
    private val useCase = SetSuggestedEquipmentUseCase(planRepository, exerciseRepository)

    private fun admits(vararg ids: Long) {
        every { exerciseRepository.getEquipmentIdsOfExercise(EXERCISE_ID) } returns flowOf(ids.toList())
    }

    @Test
    fun `given an admitted implement, when suggesting it, then it is persisted`() = runTest {
        admits(BARRA, POLEA, MANCUERNA)

        useCase(ROUTINE_VERSION_ID, EXERCISE_ID, POLEA)

        coVerify { planRepository.setSuggestedEquipment(ROUTINE_VERSION_ID, EXERCISE_ID, POLEA) }
    }

    @Test
    fun `given an implement the exercise does not admit, when suggesting it, then it is rejected`() =
        runTest {
            admits(BARRA, POLEA, MANCUERNA)

            val thrown = runCatching {
                useCase(ROUTINE_VERSION_ID, EXERCISE_ID, MAQUINA)
            }.exceptionOrNull()

            assertTrue(thrown is IllegalArgumentException)
            coVerify(exactly = 0) { planRepository.setSuggestedEquipment(any(), any(), any()) }
        }

    @Test
    fun `the check reads the options of now, not those the screen was painted with`() = runTest {
        // El ejercicio perdió `Barra` desde que el diálogo se abrió. La comprobación
        // consulta el estado de ahora, que es la razón de que exista además del selector.
        admits(POLEA, MANCUERNA)

        val thrown = runCatching {
            useCase(ROUTINE_VERSION_ID, EXERCISE_ID, BARRA)
        }.exceptionOrNull()

        assertTrue(thrown is IllegalArgumentException)
    }
}
