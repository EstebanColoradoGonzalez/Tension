package com.estebancoloradogonzalez.tension.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.estebancoloradogonzalez.tension.data.local.database.TensionDatabase
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackupRepositoryImplTest {

    private lateinit var database: TensionDatabase
    private lateinit var context: Context
    private lateinit var db: SupportSQLiteDatabase
    private lateinit var openHelper: SupportSQLiteOpenHelper
    private lateinit var repository: BackupRepositoryImpl

    @Before
    fun setup() {
        database = mockk(relaxed = true)
        context = mockk(relaxed = true)
        db = mockk(relaxed = true)
        openHelper = mockk(relaxed = true)

        every { database.openHelper } returns openHelper
        every { openHelper.readableDatabase } returns db
        every { openHelper.writableDatabase } returns db

        every { context.getString(any()) } returns "Error message"
        every { context.getString(any(), any()) } returns "Error message"
        every { context.getString(any(), any(), any()) } returns "Error message"

        repository = BackupRepositoryImpl(database, context)
    }

    private fun createEmptyCursor(): Cursor {
        val cursor = mockk<Cursor>()
        every { cursor.moveToNext() } returns false
        every { cursor.close() } returns Unit
        every { cursor.columnCount } returns 1
        every { cursor.getColumnName(0) } returns "id"
        return cursor
    }

    private fun createProfileCursor(): Cursor {
        val cursor = mockk<Cursor>()
        var callCount = 0
        every { cursor.moveToNext() } answers {
            callCount++
            callCount <= 1
        }
        every { cursor.close() } returns Unit
        every { cursor.columnCount } returns 3
        every { cursor.getColumnName(0) } returns "id"
        every { cursor.getColumnName(1) } returns "height_m"
        every { cursor.getColumnName(2) } returns "weekly_frequency"
        every { cursor.getType(0) } returns Cursor.FIELD_TYPE_INTEGER
        every { cursor.getType(1) } returns Cursor.FIELD_TYPE_FLOAT
        every { cursor.getType(2) } returns Cursor.FIELD_TYPE_INTEGER
        every { cursor.getLong(0) } returns 1L
        every { cursor.getDouble(1) } returns 1.75
        every { cursor.getLong(2) } returns 6L
        return cursor
    }

    @Test
    fun `exportToJson produces valid JSON with metadata and all 16 tables`() = runTest {
        // Return profile data for profile table, empty for everything else
        every { db.query("SELECT * FROM profile") } returns createProfileCursor()
        BackupRepositoryImpl.TABLE_ORDER_INSERT
            .filter { it != "profile" }
            .forEach { table ->
                every { db.query("SELECT * FROM $table") } returns createEmptyCursor()
            }

        val json = repository.exportToJson()
        val parsed = JSONObject(json)

        assertTrue(parsed.has("metadata"))
        assertTrue(parsed.has("data"))

        val metadata = parsed.getJSONObject("metadata")
        assertEquals(7, metadata.getInt("schemaVersion"))
        assertEquals("1.0", metadata.getString("appVersion"))
        assertTrue(metadata.has("exportDate"))
        assertTrue(metadata.has("recordCount"))

        val data = parsed.getJSONObject("data")
        for (table in BackupRepositoryImpl.TABLE_ORDER_INSERT) {
            assertTrue("Missing table: $table", data.has(table))
        }
    }

    @Test
    fun `exportToJson includes correct recordCount in metadata`() = runTest {
        // Profile with 1 row, everything else empty
        every { db.query("SELECT * FROM profile") } returns createProfileCursor()
        BackupRepositoryImpl.TABLE_ORDER_INSERT
            .filter { it != "profile" }
            .forEach { table ->
                every { db.query("SELECT * FROM $table") } returns createEmptyCursor()
            }

        val json = repository.exportToJson()
        val parsed = JSONObject(json)
        val metadata = parsed.getJSONObject("metadata")
        assertEquals(1, metadata.getInt("recordCount"))
    }

    @Test
    fun `validateBackup returns valid for correct JSON`() {
        val json = buildValidBackupJson(sessionCount = 5)
        val result = repository.validateBackup(json)

        assertTrue(result.isValid)
        assertNotNull(result.metadata)
        assertEquals(7, result.metadata?.schemaVersion)
        assertEquals(5, result.sessionCount)
        assertNull(result.errorMessage)
    }

    @Test
    fun `validateBackup returns invalid for malformed JSON`() {
        val result = repository.validateBackup("not a json string")

        assertFalse(result.isValid)
        assertNull(result.metadata)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun `validateBackup returns invalid for wrong schemaVersion`() {
        val json = buildBackupJsonWithSchemaVersion(99)
        val result = repository.validateBackup(json)

        assertFalse(result.isValid)
        assertNull(result.metadata)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun `validateBackup returns invalid for missing data section`() {
        val json = JSONObject().apply {
            put("metadata", JSONObject().apply {
                put("appVersion", "1.0")
                put("schemaVersion", 7)
                put("exportDate", "2026-02-20T14:00:00")
                put("recordCount", 0)
            })
        }.toString()

        val result = repository.validateBackup(json)

        assertFalse(result.isValid)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun `validateBackup returns invalid for missing tables`() {
        val json = JSONObject().apply {
            put("metadata", JSONObject().apply {
                put("appVersion", "1.0")
                put("schemaVersion", 7)
                put("exportDate", "2026-02-20T14:00:00")
                put("recordCount", 0)
            })
            put("data", JSONObject().apply {
                put("profile", org.json.JSONArray())
                // Missing other 15 tables
            })
        }.toString()

        val result = repository.validateBackup(json)

        assertFalse(result.isValid)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun `importFromJson calls beginTransaction and setTransactionSuccessful`() = runTest {
        val json = buildValidBackupJson()

        repository.importFromJson(json)

        verify { db.beginTransaction() }
        verify { db.setTransactionSuccessful() }
        verify { db.endTransaction() }
    }

    @Test
    fun `importFromJson deletes tables in children-first order`() = runTest {
        val json = buildValidBackupJson()
        val deleteCalls = mutableListOf<String>()

        every { db.execSQL(any()) } answers {
            val sql = firstArg<String>()
            if (sql.startsWith("DELETE FROM")) {
                deleteCalls.add(sql.removePrefix("DELETE FROM "))
            }
        }

        repository.importFromJson(json)

        assertEquals(BackupRepositoryImpl.TABLE_ORDER_DELETE, deleteCalls)
    }

    @Test
    fun `importFromJson inserts tables in parents-first order`() = runTest {
        // Build JSON with 1 row per table to verify insert order
        val jsonObj = JSONObject()
        jsonObj.put("metadata", JSONObject().apply {
            put("appVersion", "1.0")
            put("schemaVersion", 7)
            put("exportDate", "2026-02-20T14:00:00")
            put("recordCount", 16)
        })
        val data = JSONObject()
        for (table in BackupRepositoryImpl.TABLE_ORDER_INSERT) {
            val rows = org.json.JSONArray()
            rows.put(JSONObject().apply { put("id", 1) })
            data.put(table, rows)
        }
        jsonObj.put("data", data)

        val insertTableOrder = mutableListOf<String>()
        every { db.insert(any(), any(), any()) } answers {
            insertTableOrder.add(firstArg())
            1L
        }

        repository.importFromJson(jsonObj.toString())

        assertEquals(BackupRepositoryImpl.TABLE_ORDER_INSERT, insertTableOrder)
    }

    @Test
    fun `importFromJson rolls back on exception`() = runTest {
        val json = buildValidBackupJson()
        var transactionSuccessfulCalled = false

        every { db.setTransactionSuccessful() } answers {
            transactionSuccessfulCalled = true
        }
        // Throw exception during DELETE
        every { db.execSQL(any()) } throws RuntimeException("DB error")

        try {
            repository.importFromJson(json)
        } catch (_: RuntimeException) {
            // Expected
        }

        assertFalse(transactionSuccessfulCalled)
        verify { db.endTransaction() }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private fun buildValidBackupJson(sessionCount: Int = 0): String {
        val json = JSONObject()
        json.put("metadata", JSONObject().apply {
            put("appVersion", "1.0")
            put("schemaVersion", 7)
            put("exportDate", "2026-02-20T14:00:00")
            put("recordCount", sessionCount)
        })
        val data = JSONObject()
        for (table in BackupRepositoryImpl.TABLE_ORDER_INSERT) {
            val rows = org.json.JSONArray()
            if (table == "session") {
                repeat(sessionCount) { i ->
                    rows.put(JSONObject().apply { put("id", i + 1) })
                }
            }
            data.put(table, rows)
        }
        json.put("data", data)
        return json.toString()
    }

    private fun buildBackupJsonWithSchemaVersion(version: Int): String {
        val json = JSONObject()
        json.put("metadata", JSONObject().apply {
            put("appVersion", "1.0")
            put("schemaVersion", version)
            put("exportDate", "2026-02-20T14:00:00")
            put("recordCount", 0)
        })
        val data = JSONObject()
        for (table in BackupRepositoryImpl.TABLE_ORDER_INSERT) {
            data.put(table, org.json.JSONArray())
        }
        json.put("data", data)
        return json.toString()
    }
}
