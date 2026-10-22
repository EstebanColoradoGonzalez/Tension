package com.estebancoloradogonzalez.tension

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.estebancoloradogonzalez.tension.data.local.database.TensionDatabase
import com.estebancoloradogonzalez.tension.data.local.entity.SessionEntity
import com.estebancoloradogonzalez.tension.data.local.entity.SessionExerciseEntity
import com.estebancoloradogonzalez.tension.data.local.seed.EquipmentCatalog
import com.estebancoloradogonzalez.tension.data.local.seed.PrepopulateCallback
import com.estebancoloradogonzalez.tension.data.repository.SessionRepositoryImpl
import com.estebancoloradogonzalez.tension.domain.model.WeightUnit
import com.estebancoloradogonzalez.tension.domain.util.CurrentDateProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Correcting the last registered set, **through the real write path** (HU-47).
 *
 * The window is not a stored state: it is a query over `exercise_set` and `session.status`.
 * What this test guards is that derivation — on a single exercise, moving when a set is
 * registered, dying with the close — and the transaction that corrects and rebuilds the 1RM.
 * That is SQL and statement order, not pure functions, hence a real database, as in
 * `MiUnRmEstimadoTest` and `AjustarSesionDelDiaTest`.
 *
 * Surviving an unexpected shutdown (CA-47.10) follows from the same fact: the window is read
 * from the database every time, so a resumed session answers exactly as before closing.
 *
 * Test names use `given_…_when_…_then_…` instead of backticked sentences: DEX before version
 * 040 (minSdk 26) rejects spaces in method names, so the format of §5.3 is kept in snake case.
 */
@RunWith(AndroidJUnit4::class)
class CorrectLastSetTest {

    private lateinit var db: TensionDatabase
    private lateinit var repository: SessionRepositoryImpl

    /** `Elevación Lateral`: admits dumbbell, cable and machine. */
    private val lateralRaise = 10L

    /** `Press de Banca Plano`: admits barbell, dumbbell, machine and Smith. */
    private val benchPress = 19L

    /** `Dominadas`: the only bodyweight exercise of the seed catalog. */
    private val pullUps = 35L

    private val dumbbell = EquipmentCatalog.MANCUERNA
    private val barbell = EquipmentCatalog.BARRA
    private val pullUpBar = EquipmentCatalog.BARRA_FIJA

    private var routineVersionId: Long = 0

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, TensionDatabase::class.java)
            .addCallback(PrepopulateCallback())
            .build()
        db.openHelper.writableDatabase

        repository = SessionRepositoryImpl(
            sessionDao = db.sessionDao(),
            sessionExerciseDao = db.sessionExerciseDao(),
            planAssignmentDao = db.planAssignmentDao(),
            rotationStateDao = db.rotationStateDao(),
            routineDao = db.routineDao(),
            routineVersionDao = db.routineVersionDao(),
            routineCurrentVersionDao = db.routineCurrentVersionDao(),
            weekDayDao = db.weekDayDao(),
            dailyRoutineOverrideDao = db.dailyRoutineOverrideDao(),
            daySkipDao = db.daySkipDao(),
            deloadFrozenVersionDao = db.deloadFrozenVersionDao(),
            exerciseSetDao = db.exerciseSetDao(),
            exerciseProgressionDao = db.exerciseProgressionDao(),
            exerciseOneRmDao = db.exerciseOneRmDao(),
            sessionExerciseProgressionDao = db.sessionExerciseProgressionDao(),
            sessionWithdrawalDao = db.sessionWithdrawalDao(),
            alertDao = db.alertDao(),
            database = db,
            deloadDao = db.deloadDao(),
            exerciseDao = db.exerciseDao(),
            equipmentTypeDao = db.equipmentTypeDao(),
            profileDao = db.profileDao(),
            currentDateProvider = CurrentDateProvider(),
        )

        routineVersionId = readLong("SELECT id FROM routine_version LIMIT 1")!!
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ----- CA-47.01 / CA-47.02 / CA-47.07: one global window that moves -----

    @Test
    fun given_two_sets_of_one_exercise_when_listing_then_only_that_exercise_owns_the_window() = runBlocking {
        // Given
        val session = openSession(lateralRaise, benchPress)
        val (raise, bench) = session.exercises
        registerSet(raise, dumbbell, 10.0, 12, 1)
        registerSet(raise, dumbbell, 18.0, 18, 1)

        // When / Then
        assertEquals(listOf(raise), withWindow(session.id))
        assertEquals(2, repository.getCorrectableSet(raise)!!.setNumber)
        assertNull(repository.getCorrectableSet(bench))
    }

    @Test
    fun given_a_window_when_another_exercise_registers_then_the_window_moves_and_closes() = runBlocking {
        // Given
        val session = openSession(lateralRaise, benchPress)
        val (raise, bench) = session.exercises
        registerSet(raise, dumbbell, 18.0, 18, 1)
        val previous = repository.getCorrectableSet(raise)!!

        // When
        registerSet(bench, barbell, 60.0, 10, 1)

        // Then
        assertEquals(listOf(bench), withWindow(session.id))
        assertNull(repository.getCorrectableSet(raise))
        assertCorrectionRejected(raise, previous.setId)
    }

    @Test
    fun given_a_window_when_the_same_exercise_registers_then_the_window_passes_to_the_new_set() = runBlocking {
        // Given
        val session = openSession(benchPress)
        val bench = session.exercises.single()
        registerSet(bench, barbell, 60.0, 10, 1)
        val first = repository.getCorrectableSet(bench)!!

        // When
        registerSet(bench, barbell, 62.5, 9, 1)

        // Then
        val window = repository.getCorrectableSet(bench)!!
        assertEquals(2, window.setNumber)
        assertEquals(62.5, window.weightKg, 0.0001)
        assertCorrectionRejected(bench, first.setId)
    }

    // ----- CA-47.04 / CA-47.05: correcting updates the set and nothing else -----

    @Test
    fun given_the_last_set_when_correcting_then_only_the_typed_values_change() = runBlocking {
        // Given
        val session = openSession(lateralRaise)
        val raise = session.exercises.single()
        registerSet(raise, dumbbell, 10.0, 12, 1)
        registerSet(raise, dumbbell, 18.0, 18, 1)
        val window = repository.getCorrectableSet(raise)!!

        // When
        repository.correctSet(raise, window.setId, 10.0, 10, 1, WeightUnit.KG)

        // Then
        val corrected = repository.getCorrectableSet(raise)!!
        assertEquals(window.setId, corrected.setId)
        assertEquals(2, corrected.setNumber)
        assertEquals(10.0, corrected.weightKg, 0.0001)
        assertEquals(10, corrected.reps)
        assertEquals(1, corrected.rir)
        assertEquals(dumbbell, corrected.equipmentTypeId)
        // CA-47.05: no set is added nor removed.
        assertEquals(2L, readLong("SELECT COUNT(*) FROM exercise_set WHERE session_exercise_id = $raise"))
    }

    @Test
    fun given_a_corrected_set_when_correcting_again_then_the_window_stays_on_it() = runBlocking {
        // Given
        val session = openSession(lateralRaise)
        val raise = session.exercises.single()
        registerSet(raise, dumbbell, 18.0, 18, 1)
        val setId = repository.getCorrectableSet(raise)!!.setId

        // When
        repository.correctSet(raise, setId, 10.0, 10, 1, WeightUnit.KG)
        repository.correctSet(raise, setId, 10.0, 11, 1, WeightUnit.KG)

        // Then
        assertEquals(11, repository.getCorrectableSet(raise)!!.reps)
        assertEquals(listOf(raise), withWindow(session.id))
    }

    @Test
    fun given_a_weight_in_pounds_when_correcting_then_kilograms_and_the_unit_are_stored() = runBlocking {
        // Given
        val session = openSession(benchPress)
        val bench = session.exercises.single()
        repository.registerSet(bench, 59.87, 10, 1, WeightUnit.LB, barbell)
        val setId = repository.getCorrectableSet(bench)!!.setId

        // When
        repository.correctSet(bench, setId, 49.9, 10, 1, WeightUnit.LB)

        // Then
        val corrected = repository.getCorrectableSet(bench)!!
        assertEquals(49.9, corrected.weightKg, 0.0001)
        assertEquals(WeightUnit.LB, corrected.captureUnit)
    }

    @Test
    fun given_no_external_load_when_correcting_then_the_set_stays_at_zero_kilograms() = runBlocking {
        // Given
        val session = openSession(pullUps)
        val pullUp = session.exercises.single()
        registerSet(pullUp, pullUpBar, 0.0, 8, 2)
        val setId = repository.getCorrectableSet(pullUp)!!.setId

        // When
        repository.correctSet(pullUp, setId, 20.0, 6, 2, WeightUnit.LB)

        // Then
        val corrected = repository.getCorrectableSet(pullUp)!!
        assertEquals(0.0, corrected.weightKg, 0.0)
        assertEquals(WeightUnit.KG, corrected.captureUnit)
        assertEquals(6, corrected.reps)
    }

    // ----- CA-47.08: closing the session closes the window -----

    @Test
    fun given_a_completed_session_when_correcting_then_there_is_no_window_and_nothing_changes() = runBlocking {
        // Given
        val session = openSession(benchPress)
        val bench = session.exercises.single()
        registerSet(bench, barbell, 60.0, 10, 1)
        val setId = repository.getCorrectableSet(bench)!!.setId

        // When
        close(session.id, "COMPLETED")

        // Then
        assertNull(repository.getCorrectableSet(bench))
        assertTrue(withWindow(session.id).isEmpty())
        assertCorrectionRejected(bench, setId)
        assertEquals(60.0, readDouble("SELECT weight_kg FROM exercise_set WHERE id = $setId")!!, 0.0)
    }

    @Test
    fun given_an_incomplete_session_when_reading_the_window_then_there_is_none() = runBlocking {
        // Given
        val session = openSession(benchPress)
        val bench = session.exercises.single()
        registerSet(bench, barbell, 60.0, 10, 1)

        // When
        close(session.id, "INCOMPLETE")

        // Then
        assertNull(repository.getCorrectableSet(bench))
    }

    // ----- CA-47.11: the 1RM is rebuilt in both directions -----

    @Test
    fun given_a_set_that_inflated_the_1rm_when_corrected_then_the_record_goes_down() = runBlocking {
        // Given — QA-17: 12 kg in a previous session; the mistyped S3 (18 kg) raised it
        val previous = openSession(lateralRaise)
        registerSet(previous.exercises.single(), dumbbell, 12.0, 10, 1)
        close(previous.id, "COMPLETED")
        val session = openSession(lateralRaise)
        val raise = session.exercises.single()
        registerSet(raise, dumbbell, 18.0, 10, 1)
        assertEquals(18.0 * 1.3337, oneRm(lateralRaise, dumbbell)!!, 0.05)

        // When
        val setId = repository.getCorrectableSet(raise)!!.setId
        repository.correctSet(raise, setId, 10.0, 10, 1, WeightUnit.KG)

        // Then — the record GOES DOWN
        assertEquals(12.0 * 1.3337, oneRm(lateralRaise, dumbbell)!!, 0.05)
    }

    @Test
    fun given_a_corrected_value_that_qualifies_as_maximum_when_corrected_then_the_record_goes_up() = runBlocking {
        // Given — QA-18: record of 12 kg; the registered S3, 8 × 15, did not qualify
        val previous = openSession(lateralRaise)
        registerSet(previous.exercises.single(), dumbbell, 12.0, 10, 1)
        close(previous.id, "COMPLETED")
        val session = openSession(lateralRaise)
        val raise = session.exercises.single()
        registerSet(raise, dumbbell, 8.0, 15, 1)

        // When
        val setId = repository.getCorrectableSet(raise)!!.setId
        repository.correctSet(raise, setId, 14.0, 10, 1, WeightUnit.KG)

        // Then
        assertEquals(14.0 * 1.3337, oneRm(lateralRaise, dumbbell)!!, 0.05)
    }

    @Test
    fun given_no_qualifying_set_left_when_corrected_then_the_pair_loses_its_1rm() = runBlocking {
        // Given
        val session = openSession(lateralRaise)
        val raise = session.exercises.single()
        registerSet(raise, dumbbell, 18.0, 10, 1)
        assertNotNull(oneRm(lateralRaise, dumbbell))

        // When
        val setId = repository.getCorrectableSet(raise)!!.setId
        repository.correctSet(raise, setId, 18.0, 9, 1, WeightUnit.KG)

        // Then — CA-42.07: without a real value the pair is not shown, neither zero nor dash
        assertNull(oneRm(lateralRaise, dumbbell))
    }

    @Test
    fun given_another_pair_with_a_record_when_correcting_then_its_record_is_untouched() = runBlocking {
        // Given
        val previous = openSession(benchPress)
        registerSet(previous.exercises.single(), barbell, 60.0, 10, 1)
        close(previous.id, "COMPLETED")
        val session = openSession(lateralRaise)
        val raise = session.exercises.single()
        registerSet(raise, dumbbell, 18.0, 10, 1)

        // When
        val setId = repository.getCorrectableSet(raise)!!.setId
        repository.correctSet(raise, setId, 10.0, 9, 1, WeightUnit.KG)

        // Then
        assertEquals(60.0 * 1.3337, oneRm(benchPress, barbell)!!, 0.05)
    }

    // ----- CA-47.12: the history sees the corrected value -----

    @Test
    fun given_a_corrected_set_when_the_session_is_closed_then_its_detail_shows_the_corrected_value() =
        runBlocking {
            // Given
            val session = openSession(lateralRaise)
            val raise = session.exercises.single()
            registerSet(raise, dumbbell, 18.0, 18, 1)
            val setId = repository.getCorrectableSet(raise)!!.setId
            repository.correctSet(raise, setId, 10.0, 10, 1, WeightUnit.KG)

            // When
            close(session.id, "COMPLETED")

            // Then
            val sets = db.exerciseSetDao().getSetsForSessionExercise(raise)
            assertEquals(1, sets.size)
            assertEquals(10.0, sets.single().weightKg, 0.0)
            assertEquals(10, sets.single().reps)
        }

    // ----- Fixture helpers -----

    private data class OpenSession(val id: Long, val exercises: List<Long>)

    /** Opens a session in progress with the given exercises, in that order. */
    private suspend fun openSession(vararg exerciseIds: Long): OpenSession {
        val sessionId = db.sessionDao().insert(
            SessionEntity(routineVersionId = routineVersionId, date = "2026-10-22"),
        )
        db.sessionExerciseDao().insertAll(
            exerciseIds.mapIndexed { index, exerciseId ->
                SessionExerciseEntity(sessionId = sessionId, exerciseId = exerciseId, slot = index + 1)
            },
        )
        val ids = exerciseIds.map { exerciseId ->
            readLong(
                "SELECT id FROM session_exercise " +
                    "WHERE session_id = $sessionId AND exercise_id = $exerciseId",
            )!!
        }
        return OpenSession(sessionId, ids)
    }

    private suspend fun registerSet(
        sessionExerciseId: Long,
        equipmentTypeId: Long,
        weightKg: Double,
        reps: Int,
        rir: Int,
    ) {
        repository.registerSet(
            sessionExerciseId = sessionExerciseId,
            weightKg = weightKg,
            reps = reps,
            rir = rir,
            captureUnit = WeightUnit.KG,
            equipmentTypeId = equipmentTypeId,
        )
    }

    /** What the active session screen reads to draw «Corregir». */
    private suspend fun withWindow(sessionId: Long): List<Long> =
        repository.getSessionExercises(sessionId).first()
            .filter { it.canCorrectLastSet }
            .map { it.sessionExerciseId }

    /**
     * Closes the session without going through `closeSession`: what is tested is that the
     * closed status turns the window off, not the close itself, which has its own tests.
     */
    private fun close(sessionId: Long, status: String) {
        db.openHelper.writableDatabase.execSQL(
            "UPDATE session SET status = '$status' WHERE id = $sessionId",
        )
    }

    private suspend fun assertCorrectionRejected(sessionExerciseId: Long, setId: Long) {
        try {
            repository.correctSet(sessionExerciseId, setId, 1.0, 1, 0, WeightUnit.KG)
            fail("A set outside the correction window must not be corrected")
        } catch (_: IllegalStateException) {
            // Expected: the window is evaluated inside the transaction.
        }
    }

    private fun oneRm(exerciseId: Long, equipmentTypeId: Long): Double? = readDouble(
        "SELECT one_rm_kg FROM exercise_one_rm " +
            "WHERE exercise_id = $exerciseId AND equipment_type_id = $equipmentTypeId",
    )

    private fun readLong(sql: String): Long? = db.query(sql, null).use {
        if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else null
    }

    private fun readDouble(sql: String): Double? = db.query(sql, null).use {
        if (it.moveToFirst() && !it.isNull(0)) it.getDouble(0) else null
    }
}
