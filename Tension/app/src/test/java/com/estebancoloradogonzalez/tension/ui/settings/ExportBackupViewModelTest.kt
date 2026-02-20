package com.estebancoloradogonzalez.tension.ui.settings

import android.net.Uri
import com.estebancoloradogonzalez.tension.data.local.storage.BackupFileManager
import com.estebancoloradogonzalez.tension.domain.usecase.backup.ExportBackupUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ExportBackupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val exportBackupUseCase: ExportBackupUseCase = mockk()
    private val backupFileManager: BackupFileManager = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() {
        val viewModel = ExportBackupViewModel(exportBackupUseCase, backupFileManager)
        viewModel.ioDispatcher = testDispatcher
        assertTrue(viewModel.uiState.value is ExportBackupUiState.Idle)
    }

    @Test
    fun `export transitions from Idle to Exporting to Success`() = runTest {
        val uri = mockk<Uri>()
        val json = """{"metadata":{},"data":{}}"""
        coEvery { exportBackupUseCase() } returns json
        every { backupFileManager.writeToUri(json, uri) } returns Unit
        every { backupFileManager.generateBackupFileName() } returns "tension_backup_20260220.json"
        every { backupFileManager.extractDisplayPath(uri) } returns "Descargas/"

        val viewModel = ExportBackupViewModel(exportBackupUseCase, backupFileManager)
        viewModel.ioDispatcher = testDispatcher
        viewModel.export(uri)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ExportBackupUiState.Success)
        assertEquals("tension_backup_20260220.json", (state as ExportBackupUiState.Success).fileName)
        assertEquals("Descargas/", state.displayPath)
    }

    @Test
    fun `export transitions to Error on exception`() = runTest {
        val uri = mockk<Uri>()
        coEvery { exportBackupUseCase() } throws IOException("Disk full")

        val viewModel = ExportBackupViewModel(exportBackupUseCase, backupFileManager)
        viewModel.ioDispatcher = testDispatcher
        viewModel.export(uri)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ExportBackupUiState.Error)
        assertEquals("Disk full", (state as ExportBackupUiState.Error).message)
    }

    @Test
    fun `onExportPickerCancelled resets to Idle`() {
        val viewModel = ExportBackupViewModel(exportBackupUseCase, backupFileManager)
        viewModel.ioDispatcher = testDispatcher
        viewModel.onExportPickerCancelled()
        assertTrue(viewModel.uiState.value is ExportBackupUiState.Idle)
    }

    @Test
    fun `generateBackupFileName delegates to BackupFileManager`() {
        every { backupFileManager.generateBackupFileName() } returns "tension_backup_20260220.json"

        val viewModel = ExportBackupViewModel(exportBackupUseCase, backupFileManager)
        viewModel.ioDispatcher = testDispatcher
        val fileName = viewModel.generateBackupFileName()

        assertEquals("tension_backup_20260220.json", fileName)
        verify { backupFileManager.generateBackupFileName() }
    }
}
