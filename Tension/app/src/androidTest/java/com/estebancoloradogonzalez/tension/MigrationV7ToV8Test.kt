package com.estebancoloradogonzalez.tension

import android.database.Cursor
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.estebancoloradogonzalez.tension.data.local.database.Migrations
import com.estebancoloradogonzalez.tension.data.local.database.TensionDatabase
import com.estebancoloradogonzalez.tension.data.local.seed.PrepopulateCallback
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationV7ToV8Test {

    private lateinit var db: TensionDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, TensionDatabase::class.java)
            .addCallback(PrepopulateCallback())
            .addMigrations(Migrations.MIGRATION_6_7, Migrations.MIGRATION_7_8)
            .build()
        // Trigger database creation and seeding
        db.openHelper.writableDatabase
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun assignmentCountByVersion_isCorrect() {
        val cursor: Cursor = db.openHelper.readableDatabase.query(
            "SELECT module_version_id, COUNT(*) AS cnt FROM plan_assignment GROUP BY module_version_id ORDER BY module_version_id",
        )
        val results = mutableMapOf<Int, Int>()
        while (cursor.moveToNext()) {
            results[cursor.getInt(0)] = cursor.getInt(1)
        }
        cursor.close()

        assertEquals("A-V1 should have 12", 12, results[1])
        assertEquals("A-V2 should have 11", 11, results[2])
        assertEquals("A-V3 should have 11", 11, results[3])
        for (versionId in 4..9) {
            assertEquals("module_version_id=$versionId should have 8", 8, results[versionId])
        }
        assertEquals(82, results.values.sum())
    }

    @Test
    fun sortOrderIsSequential_perVersion() {
        for (mvId in 1..9) {
            val cursor: Cursor = db.openHelper.readableDatabase.query(
                "SELECT sort_order FROM plan_assignment WHERE module_version_id = $mvId ORDER BY sort_order ASC",
            )
            val sortOrders = mutableListOf<Int>()
            while (cursor.moveToNext()) {
                sortOrders.add(cursor.getInt(0))
            }
            cursor.close()

            val expected = (1..sortOrders.size).toList()
            assertEquals(
                "sort_order for module_version_id=$mvId should be sequential from 1 to ${sortOrders.size}",
                expected,
                sortOrders,
            )
        }
    }

    @Test
    fun sortOrderColumn_exists() {
        val cursor: Cursor = db.openHelper.readableDatabase.query(
            "PRAGMA table_info(plan_assignment)",
        )
        val columns = mutableListOf<String>()
        while (cursor.moveToNext()) {
            columns.add(cursor.getString(1)) // column name is at index 1
        }
        cursor.close()
        assertTrue("sort_order column should exist", columns.contains("sort_order"))
    }

    @Test
    fun planAssignmentReferentialIntegrity_isValid() {
        val cursor: Cursor = db.openHelper.readableDatabase.query(
            "SELECT pa.module_version_id, pa.exercise_id " +
                "FROM plan_assignment pa " +
                "JOIN module_version mv ON pa.module_version_id = mv.id " +
                "JOIN exercise e ON pa.exercise_id = e.id " +
                "WHERE mv.module_code != e.module_code",
        )
        assertEquals("Exercises assigned to wrong module version", 0, cursor.count)
        cursor.close()
    }

    @Test
    fun historicalTablesExist_andAreAccessible() {
        val tables = listOf("session", "session_exercise", "exercise_set", "alert", "rotation_state")
        for (table in tables) {
            val cursor: Cursor = db.openHelper.readableDatabase.query(
                "SELECT COUNT(*) FROM $table",
            )
            assertTrue("Table $table should be accessible", cursor.moveToFirst())
            cursor.close()
        }
    }

    @Test
    fun exercise26InAllModuleAVersions() {
        for (mvId in 1..3) {
            val cursor: Cursor = db.openHelper.readableDatabase.query(
                "SELECT COUNT(*) FROM plan_assignment WHERE module_version_id = $mvId AND exercise_id = 26",
            )
            assertTrue(cursor.moveToFirst())
            assertEquals(
                "exercise_id=26 should be in module_version_id=$mvId",
                1,
                cursor.getInt(0),
            )
            cursor.close()
        }
    }

    @Test
    fun exercise42RemovedFromAllModuleCVersions() {
        for (mvId in 7..9) {
            val cursor: Cursor = db.openHelper.readableDatabase.query(
                "SELECT COUNT(*) FROM plan_assignment WHERE module_version_id = $mvId AND exercise_id = 42",
            )
            assertTrue(cursor.moveToFirst())
            assertEquals(
                "exercise_id=42 should NOT be in module_version_id=$mvId",
                0,
                cursor.getInt(0),
            )
            cursor.close()
        }
    }
}
