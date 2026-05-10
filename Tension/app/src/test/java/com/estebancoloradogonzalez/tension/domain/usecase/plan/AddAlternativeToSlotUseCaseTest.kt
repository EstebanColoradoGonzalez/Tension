package com.estebancoloradogonzalez.tension.domain.usecase.plan

import com.estebancoloradogonzalez.tension.domain.repository.PlanRepository
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AddAlternativeToSlotUseCaseTest {

    private val planRepository: PlanRepository = mockk()
    private val sessionRepository: SessionRepository = mockk()
    private val useCase = AddAlternativeToSlotUseCase(planRepository, sessionRepository)

    @Test
    fun `invoke delegates to planRepository when no active deload`() = runTest {
        coEvery { sessionRepository.hasActiveDeload() } returns false
        coEvery { sessionRepository.hasActiveSessionForVersion(1L) } returns false
        coEvery { planRepository.addAlternativeToSlot(1L, 2, 10L) } just runs

        useCase(1L, 2, 10L)

        coVerify { planRepository.addAlternativeToSlot(1L, 2, 10L) }
    }

    @Test
    fun `invoke throws when active deload exists`() = runTest {
        coEvery { sessionRepository.hasActiveDeload() } returns true

        try {
            useCase(1L, 2, 10L)
            assert(false) { "Expected exception was not thrown" }
        } catch (e: IllegalArgumentException) {
            // expected
        }

        coVerify(exactly = 0) { planRepository.addAlternativeToSlot(any(), any(), any()) }
    }

    @Test
    fun `invoke throws when active session exists for version`() = runTest {
        coEvery { sessionRepository.hasActiveDeload() } returns false
        coEvery { sessionRepository.hasActiveSessionForVersion(1L) } returns true

        try {
            useCase(1L, 2, 10L)
            assert(false) { "Expected exception was not thrown" }
        } catch (e: IllegalArgumentException) {
            // expected
        }

        coVerify(exactly = 0) { planRepository.addAlternativeToSlot(any(), any(), any()) }
    }
}
