package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.model.PrefilledLoad
import com.estebancoloradogonzalez.tension.domain.model.WeightUnit
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetPrefilledLoadForEquipmentUseCaseTest {

    private val repository: SessionRepository = mockk()
    private val useCase = GetPrefilledLoadForEquipmentUseCase(repository)

    private val sessionExerciseId = 42L
    private val mancuerna = 6L
    private val polea = 8L

    // ----- CA-40.03: la precarga se resuelve sobre el par -----

    @Test
    fun `given the pair has history, when resolving, then it returns that pair's weight and unit`() =
        runTest {
            // Given — el par (Elevación Lateral, Mancuerna) va por 12 kg
            coEvery { repository.getPrefilledLoadForPair(sessionExerciseId, mancuerna) } returns
                PrefilledLoad(weightKg = 12.0, captureUnit = WeightUnit.KG)

            // When
            val result = useCase(sessionExerciseId, mancuerna)

            // Then
            assertEquals(12.0, result?.weightKg!!, 0.001)
            assertEquals(WeightUnit.KG, result.captureUnit)
        }

    @Test
    fun `given the pair has no history, when resolving, then the weight is null`() = runTest {
        // Given — nunca se ha entrenado con polea
        coEvery { repository.getPrefilledLoadForPair(sessionExerciseId, polea) } returns
            PrefilledLoad(weightKg = null, captureUnit = WeightUnit.KG)

        // When
        val result = useCase(sessionExerciseId, polea)

        // Then — el campo queda vacío: nunca se hereda el peso de otro implemento
        assertNull(result?.weightKg)
    }

    @Test
    fun `given the pair was captured in lb, when resolving, then the unit is that pair's`() =
        runTest {
            // La unidad es la etiqueta de la máquina, y dos implementos son dos máquinas.
            coEvery { repository.getPrefilledLoadForPair(sessionExerciseId, polea) } returns
                PrefilledLoad(weightKg = 20.41, captureUnit = WeightUnit.LB)

            val result = useCase(sessionExerciseId, polea)

            assertEquals(WeightUnit.LB, result?.captureUnit)
        }

    @Test
    fun `given two implements, when resolving each, then each is asked for separately`() = runTest {
        coEvery { repository.getPrefilledLoadForPair(sessionExerciseId, mancuerna) } returns
            PrefilledLoad(weightKg = 12.0, captureUnit = WeightUnit.KG)
        coEvery { repository.getPrefilledLoadForPair(sessionExerciseId, polea) } returns
            PrefilledLoad(weightKg = 8.0, captureUnit = WeightUnit.KG)

        val withDumbbell = useCase(sessionExerciseId, mancuerna)
        val withCable = useCase(sessionExerciseId, polea)

        assertEquals(12.0, withDumbbell?.weightKg!!, 0.001)
        assertEquals(8.0, withCable?.weightKg!!, 0.001)
        coVerify(exactly = 1) { repository.getPrefilledLoadForPair(sessionExerciseId, mancuerna) }
        coVerify(exactly = 1) { repository.getPrefilledLoadForPair(sessionExerciseId, polea) }
    }

    @Test
    fun `given the session exercise does not exist, when resolving, then it returns null`() =
        runTest {
            coEvery { repository.getPrefilledLoadForPair(any(), any()) } returns null

            assertNull(useCase(999L, mancuerna))
        }
}
