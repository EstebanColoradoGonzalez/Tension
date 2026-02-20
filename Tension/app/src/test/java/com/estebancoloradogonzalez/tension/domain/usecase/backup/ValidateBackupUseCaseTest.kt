package com.estebancoloradogonzalez.tension.domain.usecase.backup

import com.estebancoloradogonzalez.tension.domain.model.BackupMetadata
import com.estebancoloradogonzalez.tension.domain.model.BackupValidationResult
import com.estebancoloradogonzalez.tension.domain.repository.BackupRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateBackupUseCaseTest {

    private val backupRepository: BackupRepository = mockk()
    private val useCase = ValidateBackupUseCase(backupRepository)

    @Test
    fun `invoke delegates to repository validateBackup`() {
        val json = """{"metadata":{},"data":{}}"""
        val expectedResult = BackupValidationResult(
            isValid = true,
            metadata = BackupMetadata("1.0", 7, "2026-02-20", 100),
            sessionCount = 10,
            errorMessage = null,
        )
        every { backupRepository.validateBackup(json) } returns expectedResult

        val result = useCase(json)

        assertEquals(expectedResult, result)
        assertTrue(result.isValid)
        verify { backupRepository.validateBackup(json) }
    }
}
