package com.estebancoloradogonzalez.tension.domain.usecase.metrics

import com.estebancoloradogonzalez.tension.domain.model.ExercisePairSessionRange
import com.estebancoloradogonzalez.tension.domain.repository.MetricsRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetLoadVelocityUseCaseTest {

    private val repository: MetricsRepository = mockk()
    private val useCase = GetLoadVelocityUseCase(repository)

    private fun range(
        equipmentTypeId: Long,
        equipmentTypeName: String,
        sessionCount: Int,
        firstSessionId: Long,
        lastSessionId: Long,
        isBodyweight: Int = 0,
        isIsometric: Int = 0,
    ) = ExercisePairSessionRange(
        exerciseId = 10L,
        exerciseName = "Elevación Lateral",
        equipmentTypeId = equipmentTypeId,
        equipmentTypeName = equipmentTypeName,
        isBodyweight = isBodyweight,
        isIsometric = isIsometric,
        firstSessionId = firstSessionId,
        lastSessionId = lastSessionId,
        sessionCount = sessionCount,
    )

    // ----- CA-40.07 / D6: se calcula por par y se consolida por máximo -----

    @Test
    fun `given two implements, when computing velocity, then the faster one is reported`() =
        runTest {
            // Given — mancuerna sube 1 kg por sesión, polea sigue parada
            coEvery { repository.getPairSessionRanges(any()) } returns listOf(
                range(6L, "Mancuerna", sessionCount = 5, firstSessionId = 1L, lastSessionId = 9L),
                range(8L, "Polea", sessionCount = 5, firstSessionId = 2L, lastSessionId = 10L),
            )
            coEvery { repository.getAvgWeightForPairInSession(10L, 6L, 1L) } returns 10.0
            coEvery { repository.getAvgWeightForPairInSession(10L, 6L, 9L) } returns 14.0
            coEvery { repository.getAvgWeightForPairInSession(10L, 8L, 2L) } returns 8.0
            coEvery { repository.getAvgWeightForPairInSession(10L, 8L, 10L) } returns 8.0

            // When
            val result = useCase().single()

            // Then — (14 - 10) / 4 = 1.0, y no la media con el 0.0 de la polea
            assertEquals(1.0, result.velocity, 0.001)
            assertEquals(5, result.sessionCount)
        }

    @Test
    fun `given the pairs are not comparable, when computing, then no slope crosses implements`() =
        runTest {
            // El peso inicial de la polea es menor por razones mecánicas: si el cálculo
            // cruzara implementos, la pendiente saldría negativa sin que nadie retrocediera.
            coEvery { repository.getPairSessionRanges(any()) } returns listOf(
                range(6L, "Mancuerna", sessionCount = 3, firstSessionId = 1L, lastSessionId = 5L),
                range(8L, "Polea", sessionCount = 3, firstSessionId = 2L, lastSessionId = 6L),
            )
            coEvery { repository.getAvgWeightForPairInSession(10L, 6L, 1L) } returns 12.0
            coEvery { repository.getAvgWeightForPairInSession(10L, 6L, 5L) } returns 12.0
            coEvery { repository.getAvgWeightForPairInSession(10L, 8L, 2L) } returns 8.0
            coEvery { repository.getAvgWeightForPairInSession(10L, 8L, 6L) } returns 9.0

            val result = useCase().single()

            // (9 - 8) / 2 = 0.5 — la polea avanza, y eso es lo que se informa
            assertEquals(0.5, result.velocity, 0.001)
        }

    // ----- CA-40.08: con un solo implemento el número es el de siempre -----

    @Test
    fun `given a single implement, when computing velocity, then it matches the plain slope`() =
        runTest {
            coEvery { repository.getPairSessionRanges(any()) } returns listOf(
                range(4L, "Barra", sessionCount = 4, firstSessionId = 1L, lastSessionId = 7L),
            )
            coEvery { repository.getAvgWeightForPairInSession(10L, 4L, 1L) } returns 60.0
            coEvery { repository.getAvgWeightForPairInSession(10L, 4L, 7L) } returns 67.5

            val result = useCase().single()

            // (67.5 - 60) / 3 = 2.5
            assertEquals(2.5, result.velocity, 0.001)
            assertEquals(4, result.sessionCount)
        }

    @Test
    fun `given a bodyweight exercise, when computing velocity, then it is not applicable`() =
        runTest {
            coEvery { repository.getPairSessionRanges(any()) } returns listOf(
                range(
                    5L,
                    "Barra Fija",
                    sessionCount = 4,
                    firstSessionId = 1L,
                    lastSessionId = 7L,
                    isBodyweight = 1,
                ),
            )

            val result = useCase().single()

            assertTrue(result.isBodyweight)
            assertEquals(0.0, result.velocity, 0.001)
        }

    @Test
    fun `given a single session for the pair, when computing velocity, then there is no slope`() =
        runTest {
            coEvery { repository.getPairSessionRanges(any()) } returns listOf(
                range(4L, "Barra", sessionCount = 1, firstSessionId = 1L, lastSessionId = 1L),
            )
            coEvery { repository.getAvgWeightForPairInSession(10L, 4L, 1L) } returns 60.0

            val result = useCase().single()

            assertEquals(0.0, result.velocity, 0.001)
            assertEquals(1, result.sessionCount)
        }

    @Test
    fun `given no data in the window, when computing velocity, then the list is empty`() = runTest {
        coEvery { repository.getPairSessionRanges(any()) } returns emptyList()

        assertTrue(useCase().isEmpty())
    }
}
