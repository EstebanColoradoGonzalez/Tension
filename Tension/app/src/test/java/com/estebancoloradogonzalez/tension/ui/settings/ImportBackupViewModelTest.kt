package com.estebancoloradogonzalez.tension.ui.settings

import android.net.Uri
import com.estebancoloradogonzalez.tension.data.local.storage.BackupFileManager
import com.estebancoloradogonzalez.tension.domain.model.BackupMetadata
import com.estebancoloradogonzalez.tension.domain.model.BackupValidationResult
import com.estebancoloradogonzalez.tension.domain.usecase.backup.ImportBackupUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.backup.ValidateBackupUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
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
class ImportBackupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val validateBackupUseCase: ValidateBackupUseCase = mockk()
    private val importBackupUseCase: ImportBackupUseCase = mockk()
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
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value is ImportBackupUiState.Idle)
    }

    @Test
    fun `selectFile with valid JSON transitions to Validated`() = runTest {
        val uri = mockk<Uri>()
        val json = """{"metadata":{},"data":{}}"""
        val validResult = BackupValidationResult(
            isValid = true,
            metadata = BackupMetadata("1.0", 7, "2026-02-20T14:00:00", 100),
            sessionCount = 10,
            errorMessage = null,
        )
        every { backupFileManager.readFromUri(uri) } returns json
        every { validateBackupUseCase(json) } returns validResult

        val viewModel = createViewModel()
        viewModel.selectFile(uri)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ImportBackupUiState.Validated)
        assertEquals(validResult, (state as ImportBackupUiState.Validated).result)
    }

    @Test
    fun `selectFile with invalid JSON transitions to Invalid`() = runTest {
        val uri = mockk<Uri>()
        val json = "invalid"
        val invalidResult = BackupValidationResult(
            isValid = false,
            metadata = null,
            sessionCount = 0,
            errorMessage = "Invalid JSON",
        )
        every { backupFileManager.readFromUri(uri) } returns json
        every { validateBackupUseCase(json) } returns invalidResult

        val viewModel = createViewModel()
        viewModel.selectFile(uri)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ImportBackupUiState.Invalid)
        assertEquals("Invalid JSON", (state as ImportBackupUiState.Invalid).errorMessage)
    }

    @Test
    fun `selectFile with read error transitions to Invalid`() = runTest {
        val uri = mockk<Uri>()
        every { backupFileManager.readFromUri(uri) } throws IOException("Read error")

        val viewModel = createViewModel()
        viewModel.selectFile(uri)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ImportBackupUiState.Invalid)
        assertEquals("Read error", (state as ImportBackupUiState.Invalid).errorMessage)
    }

    @Test
    fun `confirmImport transitions to Success`() = runTest {
        val uri = mockk<Uri>()
        val json = """{"metadata":{},"data":{}}"""
        val validResult = BackupValidationResult(
            isValid = true,
            metadata = BackupMetadata("1.0", 7, "2026-02-20T14:00:00", 100),
            sessionCount = 10,
            errorMessage = null,
        )
        every { backupFileManager.readFromUri(uri) } returns json
        every { validateBackupUseCase(json) } returns validResult
        coEvery { importBackupUseCase(json) } just runs

        val viewModel = createViewModel()
        viewModel.selectFile(uri)
        advanceUntilIdle()

        viewModel.confirmImport()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is ImportBackupUiState.Success)
        coVerify { importBackupUseCase(json) }
    }

    @Test
    fun `confirmImport transitions to Error on exception with rollback message`() = runTest {
        val uri = mockk<Uri>()
        val json = """{"metadata":{},"data":{}}"""
        val validResult = BackupValidationResult(
            isValid = true,
            metadata = BackupMetadata("1.0", 7, "2026-02-20T14:00:00", 100),
            sessionCount = 10,
            errorMessage = null,
        )
        every { backupFileManager.readFromUri(uri) } returns json
        every { validateBackupUseCase(json) } returns validResult
        coEvery { importBackupUseCase(json) } throws RuntimeException("DB error")

        val viewModel = createViewModel()
        viewModel.selectFile(uri)
        advanceUntilIdle()

        viewModel.confirmImport()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ImportBackupUiState.Error)
        assertTrue(
            (state as ImportBackupUiState.Error).message
                .contains("importación falló"),
        )
    }

    @Test
    fun `cancel resets to Idle`() = runTest {
        val uri = mockk<Uri>()
        val json = """{"metadata":{},"data":{}}"""
        val validResult = BackupValidationResult(
            isValid = true,
            metadata = BackupMetadata("1.0", 7, "2026-02-20T14:00:00", 100),
            sessionCount = 10,
            errorMessage = null,
        )
        every { backupFileManager.readFromUri(uri) } returns json
        every { validateBackupUseCase(json) } returns validResult

        val viewModel = createViewModel()
        viewModel.selectFile(uri)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is ImportBackupUiState.Validated)

        viewModel.cancel()
        assertTrue(viewModel.uiState.value is ImportBackupUiState.Idle)
    }

    private fun createViewModel(): ImportBackupViewModel {
        return ImportBackupViewModel(
            validateBackupUseCase,
            importBackupUseCase,
            backupFileManager,
        ).also {
            it.ioDispatcher = testDispatcher
        }
    }
}
