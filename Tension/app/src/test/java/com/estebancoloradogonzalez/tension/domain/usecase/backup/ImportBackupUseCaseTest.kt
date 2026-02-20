package com.estebancoloradogonzalez.tension.domain.usecase.backup

import com.estebancoloradogonzalez.tension.domain.repository.BackupRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ImportBackupUseCaseTest {

    private val backupRepository: BackupRepository = mockk()
    private val useCase = ImportBackupUseCase(backupRepository)

    @Test
    fun `invoke delegates to repository importFromJson`() = runTest {
        val json = """{"metadata":{},"data":{}}"""
        coEvery { backupRepository.importFromJson(json) } just runs

        useCase(json)

        coVerify { backupRepository.importFromJson(json) }
    }
}
