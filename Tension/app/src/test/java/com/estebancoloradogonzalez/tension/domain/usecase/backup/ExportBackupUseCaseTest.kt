package com.estebancoloradogonzalez.tension.domain.usecase.backup

import com.estebancoloradogonzalez.tension.domain.repository.BackupRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportBackupUseCaseTest {

    private val backupRepository: BackupRepository = mockk()
    private val useCase = ExportBackupUseCase(backupRepository)

    @Test
    fun `invoke delegates to repository exportToJson`() = runTest {
        val expectedJson = """{"metadata":{},"data":{}}"""
        coEvery { backupRepository.exportToJson() } returns expectedJson

        val result = useCase()

        assertEquals(expectedJson, result)
        coVerify { backupRepository.exportToJson() }
    }
}
