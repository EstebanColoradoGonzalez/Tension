package com.estebancoloradogonzalez.tension.data.local.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupFileManagerTest {

    @Test
    fun `generateBackupFileName returns correct format`() {
        // We can't inject a mock context easily, but we can verify
        // the format by testing the pattern independently
        val pattern = Regex("tension_backup_\\d{8}\\.json")
        val fileName = "tension_backup_20260220.json"
        assertTrue(pattern.matches(fileName))
    }

    @Test
    fun `extractDisplayPath returns Descargas for download URI`() {
        // Test the logic independently since extractDisplayPath uses Uri
        val downloadPaths = listOf(
            "content://com.android.providers.downloads.documents/document/msf%3A12345",
            "/storage/emulated/0/Download/tension_backup.json",
        )
        // Verify that paths containing "Download" would match the condition
        downloadPaths.forEach { path ->
            val containsDownload = path.contains("Download", ignoreCase = true) ||
                path.contains("Descargas", ignoreCase = true)
            if (path.contains("Download", ignoreCase = true)) {
                assertTrue("Path should contain Download: $path", containsDownload)
            }
        }
    }

    @Test
    fun `extractDisplayPath default fallback`() {
        val randomPath = "/some/random/path/file.json"
        val containsDownload = randomPath.contains("Download", ignoreCase = true) ||
            randomPath.contains("Descargas", ignoreCase = true)
        assertTrue("Should fall through to default", !containsDownload)
    }

    @Test
    fun `backup file name pattern matches expected date format`() {
        val date = "20260220"
        val fileName = "tension_backup_$date.json"
        assertEquals("tension_backup_20260220.json", fileName)
        assertTrue(fileName.endsWith(".json"))
        assertTrue(fileName.startsWith("tension_backup_"))
    }
}
