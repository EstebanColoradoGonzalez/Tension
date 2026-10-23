package com.estebancoloradogonzalez.tension.domain.usecase.metrics

import com.estebancoloradogonzalez.tension.domain.model.Profile
import com.estebancoloradogonzalez.tension.domain.model.SessionHistoryItem
import com.estebancoloradogonzalez.tension.domain.model.WeeklyTonnage
import com.estebancoloradogonzalez.tension.domain.model.WeightRecord
import com.estebancoloradogonzalez.tension.domain.repository.ProfileRepository
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import com.estebancoloradogonzalez.tension.domain.util.CurrentDateProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class GetBodyWeightSummaryUseCaseTest {

    private val profileRepository: ProfileRepository = mockk()
    private val sessionRepository: SessionRepository = mockk()
    private val currentDateProvider: CurrentDateProvider = mockk()
    private val useCase = GetBodyWeightSummaryUseCase(profileRepository, sessionRepository, currentDateProvider)

    private val today = LocalDate.of(2026, 10, 22)

    private val profile = Profile(
        currentWeightKg = 78.5,
        heightM = 1.78,
        weeklyFrequency = 6,
        plateauBaseThreshold = 10,
        createdAt = LocalDate.of(2026, 9, 2),
    )

    private val historyB1 = listOf(
        WeightRecord(id = 3, weightKg = 78.5, date = LocalDate.of(2026, 10, 20)),
        WeightRecord(id = 2, weightKg = 77.9, date = LocalDate.of(2026, 10, 1)),
        WeightRecord(id = 1, weightKg = 77.2, date = LocalDate.of(2026, 9, 2)),
    )

    @Before
    fun setUp() {
        every { currentDateProvider.today() } returns today
        every { profileRepository.getProfile() } returns flowOf(profile)
        every { profileRepository.getAllWeightRecords() } returns flowOf(historyB1)
        coEvery { sessionRepository.getSessionHistory() } returns listOf(
            session(1, "2026-10-21", 1300.0),
            session(2, "2026-10-15", 1500.0),
            session(3, "2026-10-13", 1200.0),
            session(4, "2026-10-08", 1400.0),
        )
    }

    @Test
    fun `given history B1 and the qa sessions, when reading 4 weeks, then every datum of the section is computed`() =
        runTest {
            // When
            val summary = useCase(4).getOrThrow()!!

            // Then
            assertEquals(78.5, summary.currentWeightKg, 0.0)
            assertEquals(LocalDate.of(2026, 10, 20), summary.recordedOn)
            assertEquals(2L, summary.daysSinceLastRecord)
            assertFalse(summary.isStale)
            assertEquals(1.3, summary.variationKg!!, 1e-9)
            assertEquals(78.5 / (1.78 * 1.78), summary.bmi, 1e-9)
            assertEquals(LocalDate.of(2026, 9, 24), summary.periodStart)
            assertEquals(today, summary.periodEnd)
            assertEquals(listOf(77.9, 78.5), summary.weightPoints.map { it.weightKg })
            assertEquals(
                listOf(
                    WeeklyTonnage(LocalDate.of(2026, 10, 5), 1400.0),
                    WeeklyTonnage(LocalDate.of(2026, 10, 12), 2700.0),
                    WeeklyTonnage(LocalDate.of(2026, 10, 19), 1300.0),
                ),
                summary.weeklyTonnage,
            )
        }

    @Test
    fun `given history B1, when reading 8 weeks, then the variation is not enough data and the series widen`() =
        runTest {
            // When
            val summary = useCase(8).getOrThrow()!!

            // Then
            assertNull(summary.variationKg)
            assertEquals(LocalDate.of(2026, 8, 27), summary.periodStart)
            assertEquals(listOf(77.2, 77.9, 78.5), summary.weightPoints.map { it.weightKg })
        }

    @Test
    fun `given a fresh install with one entry and no sessions, when reading, then nothing is shown as zero`() =
        runTest {
            // Given — history B2 (QA-13)
            every { profileRepository.getAllWeightRecords() } returns
                flowOf(listOf(WeightRecord(id = 1, weightKg = 78.5, date = today)))
            coEvery { sessionRepository.getSessionHistory() } returns emptyList()

            // When
            val summary = useCase(4).getOrThrow()!!

            // Then
            assertNull(summary.variationKg)
            assertEquals(1, summary.weightPoints.size)
            assertTrue(summary.weeklyTonnage.isEmpty())
        }

    @Test
    fun `given a last entry 42 days old, when reading, then the section flags it as stale`() = runTest {
        // Given — QA-12
        every { profileRepository.getAllWeightRecords() } returns
            flowOf(listOf(WeightRecord(id = 1, weightKg = 78.5, date = LocalDate.of(2026, 9, 10))))

        // When
        val summary = useCase(4).getOrThrow()!!

        // Then
        assertEquals(42L, summary.daysSinceLastRecord)
        assertTrue(summary.isStale)
    }

    @Test
    fun `given no profile yet, when reading, then there is no section`() = runTest {
        // Given
        every { profileRepository.getProfile() } returns flowOf(null)

        // When / Then
        assertNull(useCase(4).getOrThrow())
    }

    private fun session(
        id: Long,
        date: String,
        tonnage: Double,
    ) = SessionHistoryItem(
        sessionId = id,
        date = date,
        routineName = "Push",
        versionNumber = 1,
        status = "COMPLETED",
        totalTonnageKg = tonnage,
    )
}
