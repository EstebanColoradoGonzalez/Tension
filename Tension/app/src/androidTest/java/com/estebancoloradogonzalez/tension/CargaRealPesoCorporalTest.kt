package com.estebancoloradogonzalez.tension

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.estebancoloradogonzalez.tension.data.local.database.TensionDatabase
import com.estebancoloradogonzalez.tension.data.local.entity.SessionEntity
import com.estebancoloradogonzalez.tension.data.local.entity.SessionExerciseEntity
import com.estebancoloradogonzalez.tension.data.local.seed.EquipmentCatalog
import com.estebancoloradogonzalez.tension.data.local.seed.PrepopulateCallback
import com.estebancoloradogonzalez.tension.data.repository.BackupRepositoryImpl
import com.estebancoloradogonzalez.tension.data.repository.ExerciseRepositoryImpl
import com.estebancoloradogonzalez.tension.data.repository.ProfileRepositoryImpl
import com.estebancoloradogonzalez.tension.data.repository.SessionRepositoryImpl
import com.estebancoloradogonzalez.tension.domain.model.WeightUnit
import com.estebancoloradogonzalez.tension.domain.util.CurrentDateProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Real load with body weight in the tonnage and the 1RM, **over a real database** (HU-49).
 *
 * What is guarded here is SQL as much as rules: the body weight in force by date is a
 * correlated subquery, the session totals moved from a SQL `SUM` to Kotlin, the 1RM is rebuilt
 * from the reference sets and the backup carries two new columns. Hence a real database, as in
 * `MiUnRmEstimadoTest` and `PesoCorporalEnMetricasTest`.
 *
 * The body-weight history is the one of `qa.md`: 78.0 kg on 2026-09-02 and 80.0 kg on
 * 2026-10-10.
 *
 * Test names use `snake_case` instead of backticked sentences: DEX before version 040
 * (minSdk 26) rejects spaces in method names.
 */
@RunWith(AndroidJUnit4::class)
class CargaRealPesoCorporalTest {

    private lateinit var db: TensionDatabase
    private lateinit var sessionRepository: SessionRepositoryImpl
    private lateinit var exerciseRepository: ExerciseRepositoryImpl

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    /** `Dominadas`, 100 %. */
    private val dominadas = 35L

    /** `Sentadilla Búlgara`, 85 %. */
    private val bulgara = 22L

    /** `Elevación Lateral`, 0 %. */
    private val elevacionLateral = 10L

    private val barraFija = EquipmentCatalog.BARRA_FIJA
    private val maquina = EquipmentCatalog.MAQUINA
    private val pesoAnadido = EquipmentCatalog.PESO_ANADIDO
    private val mancuerna = EquipmentCatalog.MANCUERNA

    private var routineVersionId: Long = 0

    @Before
    fun setUp() {
        db = newDatabase()
        sessionRepository = sessionRepositoryOf(db)
        exerciseRepository = ExerciseRepositoryImpl(
            exerciseDao = db.exerciseDao(),
            equipmentTypeDao = db.equipmentTypeDao(),
            muscleZoneDao = db.muscleZoneDao(),
            exerciseSetDao = db.exerciseSetDao(),
            exerciseOneRmDao = db.exerciseOneRmDao(),
            database = db,
        )
        routineVersionId = readLong(db, "SELECT id FROM routine_version LIMIT 1")!!
        insertWeight("2026-09-02", 78.0)
        insertWeight("2026-10-10", 80.0)
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ----- CA-49.01 / QA-01, QA-04, QA-14: the session tonnage counts the body -----

    @Test
    fun given_bodyweight_sets_when_the_session_is_read_then_its_tonnage_is_the_real_load() = runBlocking {
        // Given — 80 kg on 2026-10-14
        val sessionId = openSession("2026-10-14")
        registerSet(exerciseIn(sessionId, dominadas, slot = 1), barraFija, 0.0, 8, 2)
        registerSet(exerciseIn(sessionId, dominadas, slot = 2), pesoAnadido, 10.0, 6, 2)
        registerSet(exerciseIn(sessionId, bulgara, slot = 3), mancuerna, 20.0, 10, 2)
        registerSet(exerciseIn(sessionId, elevacionLateral, slot = 4), mancuerna, 12.0, 10, 2)
        close(sessionId)

        // When
        val detail = sessionRepository.getSessionDetail(sessionId)
        val history = sessionRepository.getSessionHistory().single { it.sessionId == sessionId }

        // Then — 640 + 540 + 880 + 120
        assertEquals(2_180.0, detail.totalTonnageKg, 0.001)
        assertEquals(2_180.0, history.totalTonnageKg, 0.001)
        // The set keeps what was typed: 0 kg on the bar, the 10 kg ballast (QA-14)
        val typed = detail.exercises.flatMap { it.sets }.map { it.weightKg }
        assertEquals(listOf(0.0, 10.0, 20.0, 12.0), typed)
    }

    // ----- CA-49.06 / QA-06: the body weight of the date, not today's -----

    @Test
    fun given_sessions_on_several_dates_when_a_new_weight_is_recorded_then_past_tonnage_does_not_change() =
        runBlocking {
            // Given — 8 pull-ups on the bar per session
            val before = pullUpSession("2026-10-05")
            val sameDay = pullUpSession("2026-10-10")
            val after = pullUpSession("2026-10-14")

            // When
            insertWeight("2026-10-22", 82.0)

            // Then
            assertEquals(624.0, tonnage(before), 0.001)
            assertEquals(640.0, tonnage(sameDay), 0.001)
            assertEquals(640.0, tonnage(after), 0.001)
        }

    @Test
    fun given_two_weights_the_same_day_when_the_session_is_read_then_the_last_saved_applies() = runBlocking {
        // Given — 79.0 and then 80.5 on 2026-10-12
        insertWeight("2026-10-12", 79.0)
        insertWeight("2026-10-12", 80.5)

        // When
        val sessionId = pullUpSession("2026-10-12")

        // Then — 80.5 × 8
        assertEquals(644.0, tonnage(sessionId), 0.001)
    }

    // ----- CA-49.02 / QA-07, QA-08: the 1RM over the real load, with its date -----

    @Test
    fun given_reference_sets_on_the_bar_when_registered_then_the_record_is_the_maximum_with_its_date() =
        runBlocking {
            // Given — 78 kg on 05/10, 80 kg on 14/10
            referencePullUp("2026-10-05", barraFija, 0.0)
            referencePullUp("2026-10-14", barraFija, 0.0)

            // When — 22/10 with 77 kg: lower, the record holds
            insertWeight("2026-10-22", 77.0)
            referencePullUp("2026-10-22", barraFija, 0.0)

            // Then
            assertEquals(80.0 * ONE_RM_FACTOR, oneRm(dominadas, barraFija)!!, 0.05)
            assertEquals("2026-10-14", achievedOn(dominadas, barraFija))
        }

    @Test
    fun given_weighted_pull_ups_when_registered_then_the_record_counts_body_plus_ballast() = runBlocking {
        // When
        referencePullUp("2026-10-14", pesoAnadido, 10.0)

        // Then — (80 + 10) × 1.3337 = 120.0
        assertEquals(90.0 * ONE_RM_FACTOR, oneRm(dominadas, pesoAnadido)!!, 0.05)
    }

    // ----- CA-49.07 / QA-02: the assisted pull-up does not change -----

    @Test
    fun given_an_assisted_pull_up_when_registered_then_it_weighs_zero_and_has_no_record() = runBlocking {
        // Given
        val sessionId = openSession("2026-10-14")
        registerSet(exerciseIn(sessionId, dominadas), maquina, 0.0, 10, 1)
        close(sessionId)

        // Then
        assertEquals(0.0, tonnage(sessionId), 0.001)
        assertNull(oneRm(dominadas, maquina))
    }

    // ----- CA-49.08 / QA-12: changing the percentage rewrites the history -----

    @Test
    fun given_a_lower_percentage_when_saved_then_tonnage_and_record_are_recalculated() = runBlocking {
        // Given — Búlgara with 20 kg dumbbells at 85 %
        val first = referenceSquat("2026-10-05")
        val second = referenceSquat("2026-10-14")
        assertEquals(88.0 * ONE_RM_FACTOR, oneRm(bulgara, mancuerna)!!, 0.05)

        // When
        exerciseRepository.updateBodyWeightPercent(bulgara, 80)

        // Then — (62.4 + 20) × 10 = 824 and (64 + 20) × 10 = 840; 84 × 1.3337 = 112.0
        assertEquals(824.0, tonnage(first), 0.001)
        assertEquals(840.0, tonnage(second), 0.001)
        assertEquals(84.0 * ONE_RM_FACTOR, oneRm(bulgara, mancuerna)!!, 0.05)
        assertEquals("2026-10-14", achievedOn(bulgara, mancuerna))
    }

    @Test
    fun given_pull_ups_at_zero_percent_when_saved_then_the_bar_loses_its_record() = runBlocking {
        // Given
        referencePullUp("2026-10-14", barraFija, 0.0)

        // When
        exerciseRepository.updateBodyWeightPercent(dominadas, 0)

        // Then — without body weight the bar carries no load: no record (CA-42.07)
        assertNull(oneRm(dominadas, barraFija))
    }

    // ----- CA-49.09 / QA-13: the prescribed and prefilled loads stay external -----

    @Test
    fun given_a_closed_squat_session_when_the_next_one_prefills_then_the_load_is_the_external_one() =
        runBlocking {
            // Given
            referenceSquat("2026-10-14")
            val next = openSession("2026-10-16")
            val sessionExerciseId = exerciseIn(next, bulgara)

            // When
            val prefilled = sessionRepository.getPrefilledLoadForPair(sessionExerciseId, mancuerna)

            // Then — 20 kg, not 88 kg
            assertEquals(20.0, prefilled!!.weightKg!!, 0.0)
        }

    // ----- CA-49.06: a weight recorded today rebuilds today's records -----

    @Test
    fun given_a_reference_set_today_when_a_new_weight_is_recorded_then_its_record_uses_it() = runBlocking {
        // Given — today the body weight in force is 80 kg
        val today = LocalDate.now().toString()
        referencePullUp(today, barraFija, 0.0)
        assertEquals(80.0 * ONE_RM_FACTOR, oneRm(dominadas, barraFija)!!, 0.05)

        // When
        profileRepositoryOf(db).updateWeight(82.0)

        // Then
        assertEquals(82.0 * ONE_RM_FACTOR, oneRm(dominadas, barraFija)!!, 0.05)
    }

    // ----- CA-49.10 / QA-16: the backup carries the percentage and the date -----

    @Test
    fun given_an_export_when_imported_on_a_fresh_install_then_percentage_record_and_tonnage_are_identical() =
        runBlocking {
            // Given
            val first = referenceSquat("2026-10-05")
            referenceSquat("2026-10-14")
            exerciseRepository.updateBodyWeightPercent(bulgara, 80)
            val json = BackupRepositoryImpl(db, context).exportToJson()

            // When
            val freshDb = newDatabase()
            try {
                BackupRepositoryImpl(freshDb, context).importFromJson(json)

                // Then
                assertEquals(80L, readLong(freshDb, "SELECT body_weight_percent FROM exercise WHERE id = $bulgara"))
                assertEquals(
                    84.0 * ONE_RM_FACTOR,
                    readDouble(freshDb, oneRmSql(bulgara, mancuerna, "one_rm_kg"))!!,
                    0.05,
                )
                assertEquals("2026-10-14", readText(freshDb, oneRmSql(bulgara, mancuerna, "achieved_on")))
                assertEquals(824.0, sessionRepositoryOf(freshDb).getSessionDetail(first).totalTonnageKg, 0.001)
            } finally {
                freshDb.close()
            }
        }

    // ----- Fixtures -----

    private suspend fun pullUpSession(date: String): Long {
        val sessionId = openSession(date)
        registerSet(exerciseIn(sessionId, dominadas), barraFija, 0.0, 8, 2)
        close(sessionId)
        return sessionId
    }

    private suspend fun referencePullUp(date: String, equipmentTypeId: Long, weightKg: Double): Long {
        val sessionId = openSession(date)
        registerSet(exerciseIn(sessionId, dominadas), equipmentTypeId, weightKg, 10, 1)
        close(sessionId)
        return sessionId
    }

    private suspend fun referenceSquat(date: String): Long {
        val sessionId = openSession(date)
        registerSet(exerciseIn(sessionId, bulgara), mancuerna, 20.0, 10, 1)
        close(sessionId)
        return sessionId
    }

    private suspend fun openSession(date: String): Long =
        db.sessionDao().insert(SessionEntity(routineVersionId = routineVersionId, date = date))

    private suspend fun exerciseIn(sessionId: Long, exerciseId: Long, slot: Int = 1): Long {
        db.sessionExerciseDao().insertAll(
            listOf(SessionExerciseEntity(sessionId = sessionId, exerciseId = exerciseId, slot = slot)),
        )
        return readLong(
            db,
            "SELECT id FROM session_exercise WHERE session_id = $sessionId AND slot = $slot",
        )!!
    }

    private suspend fun registerSet(
        sessionExerciseId: Long,
        equipmentTypeId: Long,
        weightKg: Double,
        reps: Int,
        rir: Int,
    ) {
        sessionRepository.registerSet(
            sessionExerciseId = sessionExerciseId,
            weightKg = weightKg,
            reps = reps,
            rir = rir,
            captureUnit = WeightUnit.KG,
            equipmentTypeId = equipmentTypeId,
        )
    }

    private suspend fun close(sessionId: Long) {
        db.sessionDao().updateStatus(sessionId, "COMPLETED")
    }

    private suspend fun tonnage(sessionId: Long): Double =
        sessionRepository.getSessionDetail(sessionId).totalTonnageKg

    private fun insertWeight(date: String, weightKg: Double) {
        db.openHelper.writableDatabase.execSQL(
            "INSERT INTO weight_record (weight_kg, date) VALUES ($weightKg, '$date')",
        )
    }

    private fun oneRmSql(exerciseId: Long, equipmentTypeId: Long, column: String) =
        "SELECT $column FROM exercise_one_rm " +
            "WHERE exercise_id = $exerciseId AND equipment_type_id = $equipmentTypeId"

    private fun oneRm(exerciseId: Long, equipmentTypeId: Long): Double? =
        readDouble(db, oneRmSql(exerciseId, equipmentTypeId, "one_rm_kg"))

    private fun achievedOn(exerciseId: Long, equipmentTypeId: Long): String? =
        readText(db, oneRmSql(exerciseId, equipmentTypeId, "achieved_on"))

    private fun readLong(database: TensionDatabase, sql: String): Long? = database.query(sql, null).use {
        if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else null
    }

    private fun readDouble(database: TensionDatabase, sql: String): Double? = database.query(sql, null).use {
        if (it.moveToFirst() && !it.isNull(0)) it.getDouble(0) else null
    }

    private fun readText(database: TensionDatabase, sql: String): String? = database.query(sql, null).use {
        if (it.moveToFirst() && !it.isNull(0)) it.getString(0) else null
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
        exerciseSetDao = database.exerciseSetDao(),
        exerciseOneRmDao = database.exerciseOneRmDao(),
    )

    private fun sessionRepositoryOf(database: TensionDatabase) = SessionRepositoryImpl(
        sessionDao = database.sessionDao(),
        sessionExerciseDao = database.sessionExerciseDao(),
        planAssignmentDao = database.planAssignmentDao(),
        rotationStateDao = database.rotationStateDao(),
        routineDao = database.routineDao(),
        routineVersionDao = database.routineVersionDao(),
        routineCurrentVersionDao = database.routineCurrentVersionDao(),
        weekDayDao = database.weekDayDao(),
        dailyRoutineOverrideDao = database.dailyRoutineOverrideDao(),
        daySkipDao = database.daySkipDao(),
        deloadFrozenVersionDao = database.deloadFrozenVersionDao(),
        exerciseSetDao = database.exerciseSetDao(),
        exerciseProgressionDao = database.exerciseProgressionDao(),
        exerciseOneRmDao = database.exerciseOneRmDao(),
        sessionExerciseProgressionDao = database.sessionExerciseProgressionDao(),
        sessionWithdrawalDao = database.sessionWithdrawalDao(),
        alertDao = database.alertDao(),
        database = database,
        deloadDao = database.deloadDao(),
        exerciseDao = database.exerciseDao(),
        equipmentTypeDao = database.equipmentTypeDao(),
        profileDao = database.profileDao(),
        currentDateProvider = CurrentDateProvider(),
    )

    private companion object {
        /** `1 / (1.0278 − 0.0278 × 10)`, the 1RM factor at the reference repetitions. */
        const val ONE_RM_FACTOR = 1.33369
    }
}
