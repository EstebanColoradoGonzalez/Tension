package com.estebancoloradogonzalez.tension

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.estebancoloradogonzalez.tension.data.local.database.TensionDatabase
import com.estebancoloradogonzalez.tension.data.local.seed.PrepopulateCallback
import com.estebancoloradogonzalez.tension.data.repository.BackupRepositoryImpl
import com.estebancoloradogonzalez.tension.data.repository.ProfileRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Profile without experience level and quick weight entry, **over a real database** (HU-48).
 *
 * What is guarded here is SQL, not pure functions: the column is gone from the schema, the
 * last entry of the same day wins by `id`, and the backup round trip restores profile and
 * history whole without the level. Hence a real database, as in `CorrectLastSetTest`.
 *
 * Test names use `snake_case` instead of backticked sentences: DEX before version 040
 * (minSdk 26) rejects spaces in method names.
 */
@RunWith(AndroidJUnit4::class)
class PesoCorporalEnMetricasTest {

    private lateinit var db: TensionDatabase
    private lateinit var profileRepository: ProfileRepositoryImpl
    private lateinit var backupRepository: BackupRepositoryImpl

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        db = newDatabase()
        profileRepository = profileRepositoryOf(db)
        backupRepository = BackupRepositoryImpl(db, context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ----- CA-48.01 / QA-01: the profile is weight and height -----

    @Test
    fun given_weight_and_height_when_the_profile_is_created_then_no_level_exists_and_the_weight_opens_the_history() =
        runBlocking {
            // When
            profileRepository.createProfile(weightKg = 78.5, heightM = 1.78)

            // Then
            assertFalse(profileColumns(db).contains("experience_level"))
            val profile = profileRepository.getProfile().first()!!
            assertEquals(78.5, profile.currentWeightKg, 0.0)
            assertEquals(1.78, profile.heightM, 0.0)
            val history = profileRepository.getAllWeightRecords().first()
            assertEquals(1, history.size)
            assertEquals(LocalDate.now(), history.single().date)
        }

    // ----- CA-48.04 / QA-08, QA-11: the quick entry is an entry of the same history -----

    @Test
    fun given_two_entries_the_same_day_when_reading_then_the_one_saved_last_is_the_current_weight() =
        runBlocking {
            // Given
            profileRepository.createProfile(weightKg = 78.5, heightM = 1.78)

            // When
            profileRepository.updateWeight(79.0)
            profileRepository.updateWeight(78.8)

            // Then
            assertEquals(78.8, profileRepository.getProfile().first()!!.currentWeightKg, 0.0)
            val history = profileRepository.getAllWeightRecords().first()
            assertEquals(listOf(78.8, 79.0, 78.5), history.map { it.weightKg })
        }

    // ----- CA-48.08 / QA-14: the backup carries no level and restores whole -----

    @Test
    fun given_an_export_when_imported_on_a_fresh_install_then_profile_and_history_restore_without_level() =
        runBlocking {
            // Given
            profileRepository.createProfile(weightKg = 78.5, heightM = 1.78)
            profileRepository.updateWeight(79.0)
            val json = backupRepository.exportToJson()

            // When
            val freshDb = newDatabase()
            try {
                BackupRepositoryImpl(freshDb, context).importFromJson(json)

                // Then
                assertFalse(json.contains("experience_level"))
                val restored = profileRepositoryOf(freshDb)
                val profile = restored.getProfile().first()!!
                assertEquals(79.0, profile.currentWeightKg, 0.0)
                assertEquals(1.78, profile.heightM, 0.0)
                assertEquals(listOf(79.0, 78.5), restored.getAllWeightRecords().first().map { it.weightKg })
            } finally {
                freshDb.close()
            }
        }

    private fun newDatabase(): TensionDatabase =
        Room.inMemoryDatabaseBuilder(context, TensionDatabase::class.java)
            .addCallback(PrepopulateCallback())
            .build()
            .also { it.openHelper.writableDatabase }

    private fun profileRepositoryOf(database: TensionDatabase) = ProfileRepositoryImpl(
        profileDao = database.profileDao(),
        weightRecordDao = database.weightRecordDao(),
        rotationStateDao = database.rotationStateDao(),
        database = database,
    )

    private fun profileColumns(database: TensionDatabase): Set<String> {
        val columns = mutableSetOf<String>()
        database.openHelper.readableDatabase.query("PRAGMA table_info(profile)").use { cursor ->
            val name = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) columns.add(cursor.getString(name))
        }
        return columns
    }
}
