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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationV6ToV7Test {

    private lateinit var db: TensionDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, TensionDatabase::class.java)
            .addCallback(PrepopulateCallback())
            .addMigrations(Migrations.MIGRATION_6_7)
            .build()
        // Trigger database creation and seeding
        db.openHelper.writableDatabase
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun exerciseCountByModule_isCorrect() {
        val cursor: Cursor = db.openHelper.readableDatabase.query(
            "SELECT module_code, COUNT(*) AS cnt FROM exercise GROUP BY module_code ORDER BY module_code",
        )
        val results = mutableMapOf<String, Int>()
        while (cursor.moveToNext()) {
            results[cursor.getString(0)] = cursor.getInt(1)
        }
        cursor.close()
        assertEquals(14, results["A"])
        assertEquals(15, results["B"])
        assertEquals(14, results["C"])
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
        for (versionId in 1..6) {
            assertEquals("module_version_id=$versionId should have 11", 11, results[versionId])
        }
        for (versionId in 7..9) {
            assertEquals("module_version_id=$versionId should have 9", 9, results[versionId])
        }
        assertEquals(93, results.values.sum())
    }

    @Test
    fun exercise26MuscleZone_isEspaldaMedia() {
        val cursor: Cursor = db.openHelper.readableDatabase.query(
            "SELECT mz.name FROM exercise_muscle_zone emz " +
                "JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id " +
                "WHERE emz.exercise_id = 26",
        )
        assertTrue(cursor.moveToFirst())
        assertEquals("Espalda Media", cursor.getString(0))
        cursor.close()
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
}
