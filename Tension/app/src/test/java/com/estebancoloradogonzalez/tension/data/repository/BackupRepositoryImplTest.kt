package com.estebancoloradogonzalez.tension.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.estebancoloradogonzalez.tension.data.local.database.TensionDatabase
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.slot
import io.mockk.unmockkConstructor
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackupRepositoryImplTest {

    /** Espejo del valor privado del impl: el formato anterior, sin `tree_state`. */
    private val PREVIOUS_SCHEMA_VERSION = 11

    private lateinit var database: TensionDatabase
    private lateinit var context: Context
    private lateinit var db: SupportSQLiteDatabase
    private lateinit var openHelper: SupportSQLiteOpenHelper
    private lateinit var repository: BackupRepositoryImpl

    private val sessionExerciseV16Columns = listOf(
        "id",
        "session_id",
        "exercise_id",
        "progression_classification",
        "is_finalized",
        "pending_selection",
        "slot",
    )

    @After
    fun tearDown() {
        unmockkConstructor(ContentValues::class)
    }

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
    fun `exportToJson produces valid JSON with metadata and all tables`() = runTest {
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
        assertEquals(BackupRepositoryImpl.SCHEMA_VERSION, metadata.getInt("schemaVersion"))
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
        assertEquals(BackupRepositoryImpl.SCHEMA_VERSION, result.metadata?.schemaVersion)
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
                // Missing every other table
            })
        }.toString()

        val result = repository.validateBackup(json)

        assertFalse(result.isValid)
        assertNotNull(result.errorMessage)
    }

    // =========================================================================
    // Arbol de entrenamiento — compatibilidad de formato (HU-37, CA-37.09)
    // =========================================================================

    @Test
    fun `exportToJson carries the tree state table`() = runTest {
        BackupRepositoryImpl.TABLE_ORDER_INSERT.forEach { table ->
            every { db.query("SELECT * FROM $table") } returns createEmptyCursor()
        }

        val data = JSONObject(repository.exportToJson()).getJSONObject("data")

        assertTrue(data.has("tree_state"))
    }

    // El formato anterior no traia tree_state. Rechazarlo inutilizaria todo respaldo
    // exportado hasta ahora, y el arbol es enteramente derivable del historial restaurado.

    @Test
    fun `validateBackup accepts the previous format without the tree state table`() {
        val result = repository.validateBackup(buildPreviousFormatBackupJson())

        assertTrue(result.isValid)
        assertEquals(PREVIOUS_SCHEMA_VERSION, result.metadata?.schemaVersion)
        assertNull(result.errorMessage)
    }

    @Test
    fun `validateBackup still rejects the format before the previous one`() {
        val result = repository.validateBackup(buildBackupJsonWithSchemaVersion(10))

        assertFalse(result.isValid)
        assertNotNull(result.errorMessage)
    }

    // Un respaldo del formato actual sin la tabla si esta incompleto: la exporto y la perdio.

    @Test
    fun `validateBackup rejects a current backup missing the tree state table`() {
        val json = JSONObject(buildValidBackupJson())
        json.getJSONObject("data").remove("tree_state")

        val result = repository.validateBackup(json.toString())

        assertFalse(result.isValid)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun `importFromJson restores the previous format without failing`() = runTest {
        var transactionSuccessfulCalled = false
        every { db.setTransactionSuccessful() } answers { transactionSuccessfulCalled = true }

        repository.importFromJson(buildPreviousFormatBackupJson())

        assertTrue(transactionSuccessfulCalled)
        verify { db.endTransaction() }
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

    // region Column filtering on import (HU-34)

    private fun createColumnsCursor(names: List<String>): Cursor {
        val cursor = mockk<Cursor>()
        var index = -1
        every { cursor.getColumnIndex("name") } returns 0
        every { cursor.moveToNext() } answers {
            index++
            index < names.size
        }
        every { cursor.getString(0) } answers { names[index] }
        every { cursor.close() } returns Unit
        return cursor
    }

    private fun stubTableInfo(columnsByTable: Map<String, List<String>>) {
        every { db.query(match<String> { it.startsWith("PRAGMA table_info") }) } answers {
            val table = firstArg<String>()
                .substringAfter("PRAGMA table_info(")
                .substringBefore(")")
            createColumnsCursor(columnsByTable[table].orEmpty())
        }
    }

    private fun buildBackupWithSessionExerciseRow(row: Map<String, Int>): String {
        val json = JSONObject()
        json.put("metadata", JSONObject().apply {
            put("appVersion", "1.0")
            put("schemaVersion", BackupRepositoryImpl.SCHEMA_VERSION)
            put("exportDate", "2026-02-20T14:00:00")
            put("recordCount", 1)
        })
        val data = JSONObject()
        for (table in BackupRepositoryImpl.TABLE_ORDER_INSERT) {
            val rows = org.json.JSONArray()
            if (table == "session_exercise") {
                rows.put(JSONObject().apply { row.forEach { (k, v) -> put(k, v) } })
            }
            data.put(table, rows)
        }
        json.put("data", data)
        return json.toString()
    }

    private fun recordPutKeys(): MutableList<String> {
        val keys = mutableListOf<String>()
        mockkConstructor(ContentValues::class)
        every { anyConstructed<ContentValues>().put(any<String>(), any<Long>()) } answers {
            keys.add(firstArg())
            Unit
        }
        return keys
    }

    @Test
    fun `importFromJson drops keys absent from the current schema`() = runTest {
        stubTableInfo(mapOf("session_exercise" to sessionExerciseV16Columns))
        val json = buildBackupWithSessionExerciseRow(
            mapOf(
                "id" to 1,
                "session_id" to 1,
                "exercise_id" to 7,
                "original_exercise_id" to 3,
                "slot" to 2,
            ),
        )
        val putKeys = recordPutKeys()

        repository.importFromJson(json)

        assertFalse(
            "the retired column must not reach insert()",
            putKeys.contains("original_exercise_id"),
        )
        assertTrue(putKeys.contains("exercise_id"))
    }

    @Test
    fun `importFromJson keeps every key the schema still declares`() = runTest {
        stubTableInfo(mapOf("session_exercise" to sessionExerciseV16Columns))
        val json = buildBackupWithSessionExerciseRow(
            mapOf(
                "id" to 1,
                "session_id" to 1,
                "exercise_id" to 7,
                "original_exercise_id" to 3,
                "slot" to 2,
            ),
        )
        val putKeys = recordPutKeys()

        repository.importFromJson(json)

        assertEquals(
            listOf("id", "session_id", "exercise_id", "slot").sorted(),
            putKeys.sorted(),
        )
    }

    @Test
    fun `importFromJson restores a pre-v16 backup without failing`() = runTest {
        stubTableInfo(mapOf("session_exercise" to sessionExerciseV16Columns))
        val json = buildBackupWithSessionExerciseRow(
            mapOf("id" to 1, "session_id" to 1, "exercise_id" to 7, "original_exercise_id" to 3),
        )
        val insertedTables = mutableListOf<String>()
        every { db.insert(any(), any(), any()) } answers {
            insertedTables.add(firstArg())
            1L
        }

        repository.importFromJson(json)

        assertTrue(insertedTables.contains("session_exercise"))
        verify { db.setTransactionSuccessful() }
    }

    // endregion

    private fun buildValidBackupJson(sessionCount: Int = 0): String {
        val json = JSONObject()
        json.put("metadata", JSONObject().apply {
            put("appVersion", "1.0")
            put("schemaVersion", BackupRepositoryImpl.SCHEMA_VERSION)
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

    /** Respaldo del formato inmediatamente anterior: sin `tree_state`. */
    private fun buildPreviousFormatBackupJson(): String {
        val json = JSONObject()
        json.put("metadata", JSONObject().apply {
            put("appVersion", "1.0")
            put("schemaVersion", PREVIOUS_SCHEMA_VERSION)
            put("exportDate", "2026-02-20T14:00:00")
            put("recordCount", 0)
        })
        val data = JSONObject()
        BackupRepositoryImpl.TABLE_ORDER_INSERT
            .filter { it != "tree_state" }
            .forEach { data.put(it, org.json.JSONArray()) }
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
