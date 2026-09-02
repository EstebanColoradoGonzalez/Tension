package com.estebancoloradogonzalez.tension.domain.usecase.backup

import com.estebancoloradogonzalez.tension.domain.repository.BackupRepository
import com.estebancoloradogonzalez.tension.domain.repository.TreeRepository
import com.estebancoloradogonzalez.tension.domain.usecase.tree.RecalculateTreeStateUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ImportBackupUseCaseTest {

    private val backupRepository: BackupRepository = mockk()
    private val treeRepository: TreeRepository = mockk()
    private val useCase = ImportBackupUseCase(
        backupRepository,
        RecalculateTreeStateUseCase(treeRepository),
    )

    @Test
    fun `invoke delegates to repository importFromJson`() = runTest {
        val json = """{"metadata":{},"data":{}}"""
        coEvery { backupRepository.importFromJson(json) } just runs
        coEvery { treeRepository.recalculate() } just runs

        useCase(json)

        coVerify { backupRepository.importFromJson(json) }
    }

    // Un respaldo del formato anterior no trae el árbol, y uno del actual lo trae calculado en
    // otra fecha. Derivarlo del historial restaurado resuelve los dos casos.

    @Test
    fun `invoke rebuilds the tree after restoring`() = runTest {
        val json = """{"metadata":{},"data":{}}"""
        coEvery { backupRepository.importFromJson(json) } just runs
        coEvery { treeRepository.recalculate() } just runs

        useCase(json)

        coVerifyOrder {
            backupRepository.importFromJson(json)
            treeRepository.recalculate()
        }
    }

    // Con la restauración fallida no hay historial nuevo del que derivar nada.

    @Test
    fun `invoke does not rebuild the tree when the restore fails`() = runTest {
        val json = """{"metadata":{},"data":{}}"""
        coEvery { backupRepository.importFromJson(json) } throws IllegalStateException("bad json")

        runCatching { useCase(json) }

        coVerify(exactly = 0) { treeRepository.recalculate() }
    }
}
