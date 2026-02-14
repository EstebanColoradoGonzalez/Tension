package com.estebancoloradogonzalez.tension.domain.usecase.profile

import com.estebancoloradogonzalez.tension.domain.model.WeightRecord
import com.estebancoloradogonzalez.tension.domain.repository.ProfileRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class GetWeightHistoryUseCaseTest {

    private lateinit var profileRepository: ProfileRepository
    private lateinit var useCase: GetWeightHistoryUseCase

    @Before
    fun setUp() {
        profileRepository = mockk()
        useCase = GetWeightHistoryUseCase(profileRepository)
    }

    @Test
    fun `invoke returns weight records from repository`() = runTest {
        val records = listOf(
            WeightRecord(id = 3, weightKg = 76.0, date = LocalDate.of(2026, 2, 10)),
            WeightRecord(id = 2, weightKg = 78.0, date = LocalDate.of(2026, 1, 15)),
            WeightRecord(id = 1, weightKg = 80.0, date = LocalDate.of(2026, 1, 1)),
        )
        every { profileRepository.getAllWeightRecords() } returns flowOf(records)

        val result = useCase().first()

        assertEquals(3, result.size)
        assertEquals(76.0, result[0].weightKg, 0.001)
        assertEquals(78.0, result[1].weightKg, 0.001)
        assertEquals(80.0, result[2].weightKg, 0.001)
        verify(exactly = 1) { profileRepository.getAllWeightRecords() }
    }

    @Test
    fun `invoke returns single initial record`() = runTest {
        val records = listOf(
            WeightRecord(id = 1, weightKg = 80.0, date = LocalDate.of(2026, 1, 1)),
        )
        every { profileRepository.getAllWeightRecords() } returns flowOf(records)

        val result = useCase().first()

        assertEquals(1, result.size)
        assertEquals(80.0, result[0].weightKg, 0.001)
        assertEquals(LocalDate.of(2026, 1, 1), result[0].date)
    }

    @Test
    fun `invoke delegates directly to repository without transformation`() = runTest {
        val records = listOf(
            WeightRecord(id = 2, weightKg = 75.5, date = LocalDate.of(2026, 2, 5)),
            WeightRecord(id = 1, weightKg = 80.0, date = LocalDate.of(2026, 1, 1)),
        )
        every { profileRepository.getAllWeightRecords() } returns flowOf(records)

        val result = useCase().first()

        assertEquals(records, result)
    }
}
