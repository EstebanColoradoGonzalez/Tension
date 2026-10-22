package com.estebancoloradogonzalez.tension

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.estebancoloradogonzalez.tension.data.local.database.TensionDatabase
import com.estebancoloradogonzalez.tension.data.local.seed.EquipmentCatalog
import com.estebancoloradogonzalez.tension.data.local.seed.PrepopulateCallback
import com.estebancoloradogonzalez.tension.data.repository.SessionRepositoryImpl
import com.estebancoloradogonzalez.tension.domain.model.WeightUnit
import com.estebancoloradogonzalez.tension.domain.rules.SetRangeNotice
import com.estebancoloradogonzalez.tension.domain.util.CurrentDateProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Progresión por rango 8–12 y sesión acotada a la categoría del día, sobre una base real
 * (HU-46).
 *
 * La regla vive en `HypertrophyRangeRule` y está cubierta en JVM. Lo que esta prueba vigila
 * es lo que ninguna regla pura alcanza:
 *
 * - que el cierre **persista** el conteo y la prescripción del par;
 * - que un ejercicio de **3 series** reciba la subida, que es el defecto silencioso que la
 *   historia corrige;
 * - que la descarga no toque el conteo;
 * - que las consultas filtren por categoría;
 * - que el perfil nuevo nazca con el umbral 10.
 */
@RunWith(AndroidJUnit4::class)
class ProgresionPorRangoYCategoriaTest {

    private lateinit var db: TensionDatabase
    private lateinit var repository: SessionRepositoryImpl

    /** Rutina 1 del plan por defecto: Push, categoría Empuje. */
    private val pushVersion = 1L
    private val pressBancaPlano = 19L
    private val jalonAlPecho = 25L
    private val mancuerna = EquipmentCatalog.MANCUERNA

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

        // El alta de perfil crea `rotation_state`; en una base de prueba no ocurre.
        db.openHelper.writableDatabase.execSQL(
            "INSERT OR IGNORE INTO rotation_state (id, microcycle_position, microcycle_count) " +
                "VALUES (1, 1, 0)",
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ----- CA-46.03, CA-46.06: 3 series, 4 sesiones en el tope, subida -----

    @Test
    fun un_ejercicio_de_tres_series_recibe_la_subida_en_la_cuarta_sesion_en_el_tope() = runBlocking {
        // QA-04: el plan prescribe 3 series al Press de Banca Plano
        repeat(3) { cerrarSesion(reps = listOf(12, 12, 10)) }
        assertEquals(3L, conteo())
        assertEquals(60.0, cargaPrescrita(), 0.001)

        cerrarSesion(reps = listOf(13, 12, 9))

        assertEquals(4L, conteo())
        // 60 kg + el incremento de siempre para el tren superior
        assertEquals(62.5, cargaPrescrita(), 0.001)
    }

    @Test
    fun si_no_sube_y_vuelve_a_cerrar_en_el_tope_la_subida_se_mantiene() = runBlocking {
        // QA-05
        repeat(5) { cerrarSesion(reps = listOf(12, 12, 13)) }

        assertEquals(5L, conteo())
        assertEquals(62.5, cargaPrescrita(), 0.001)
    }

    @Test
    fun una_sesion_por_debajo_del_rango_reinicia_el_conteo_y_no_propone_subir() = runBlocking {
        // QA-03, D2: la carga propuesta vuelve al peso de referencia
        repeat(4) { cerrarSesion(reps = listOf(12, 12, 12)) }
        cerrarSesion(reps = listOf(6, 7, 9))

        assertEquals(0L, conteo())
        assertEquals("BELOW", leerTexto(sqlPar("last_range_reading")))
        assertEquals(60.0, cargaPrescrita(), 0.001)
    }

    // ----- CA-46.18: la descarga no suma ni reinicia -----

    @Test
    fun la_descarga_congela_el_conteo() = runBlocking {
        // QA-32
        repeat(3) { cerrarSesion(reps = listOf(12, 12, 12)) }

        repository.activateDeload()
        cerrarSesion(reps = listOf(6, 12, 6))

        assertEquals(3L, conteo())
    }

    // ----- CA-46.05: aviso en directo -----

    @Test
    fun una_serie_por_debajo_de_ocho_devuelve_el_aviso_de_bajar_peso() = runBlocking {
        // QA-07
        val sessionId = repository.startSession(pushVersion)
        val notice = repository.registerSet(
            sessionExerciseId = sessionExerciseDe(sessionId),
            weightKg = 60.0,
            reps = 7,
            rir = 0,
            captureUnit = WeightUnit.KG,
            equipmentTypeId = mancuerna,
        )

        assertEquals(SetRangeNotice.LowerNextSet, notice)
    }

    // ----- CA-46.10: la sesión solo admite la categoría del día -----

    @Test
    fun los_anadibles_de_un_dia_de_empuje_son_solo_de_empuje() = runBlocking {
        // QA-16
        val sessionId = repository.startSession(pushVersion)

        val anadibles = repository.getAddableExercises(sessionId).first()

        assertTrue(anadibles.isNotEmpty())
        val ids = anadibles.joinToString(",") { it.exerciseId.toString() }
        assertEquals(
            0L,
            leerLong("SELECT COUNT(*) FROM exercise WHERE id IN ($ids) AND movement_category != 'PUSH'"),
        )
    }

    @Test
    fun anadir_un_ejercicio_de_otra_categoria_se_rechaza() = runBlocking {
        val sessionId = repository.startSession(pushVersion)

        try {
            repository.addExerciseToSession(sessionId, jalonAlPecho)
            fail("Jalón al Pecho no es de Empuje")
        } catch (_: IllegalStateException) {
            // esperado
        }
    }

    // ----- CA-46.07: el perfil nuevo nace con umbral 10 -----

    @Test
    fun un_perfil_nuevo_nace_con_umbral_base_diez() {
        db.openHelper.writableDatabase.execSQL(
            "INSERT INTO profile (id, height_m, experience_level, weekly_frequency, created_at) " +
                "VALUES (1, 1.8, 'BEGINNER', 6, '2026-10-22')",
        )

        assertEquals(10L, leerLong("SELECT plateau_base_threshold FROM profile WHERE id = 1"))
    }

    // ----- Utilidades -----

    /** Una sesión de Push con el Press de Banca Plano a 60 kg y las repeticiones dadas. */
    private suspend fun cerrarSesion(reps: List<Int>) {
        val sessionId = repository.startSession(
            leerLong(
                "SELECT rv.id FROM routine_version rv WHERE rv.routine_id = 1 " +
                    "AND rv.version_number = COALESCE((SELECT frozen_version_number " +
                    "FROM deload_frozen_version WHERE routine_id = 1), 1)",
            )!!,
        )
        val sessionExerciseId = sessionExerciseDe(sessionId)
        reps.forEach { rep ->
            repository.registerSet(sessionExerciseId, 60.0, rep, 1, WeightUnit.KG, mancuerna)
        }
        repository.closeSession(sessionId)
    }

    private fun sessionExerciseDe(sessionId: Long): Long = leerLong(
        "SELECT id FROM session_exercise WHERE session_id = $sessionId AND exercise_id = $pressBancaPlano",
    )!!

    private fun sqlPar(column: String) =
        "SELECT $column FROM exercise_progression " +
            "WHERE exercise_id = $pressBancaPlano AND equipment_type_id = $mancuerna"

    private fun conteo(): Long = leerLong(sqlPar("top_range_streak"))!!

    private fun cargaPrescrita(): Double = leerDouble(sqlPar("prescribed_load_kg"))!!

    private fun leerLong(sql: String): Long? =
        db.openHelper.readableDatabase.query(sql).use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null
        }

    private fun leerDouble(sql: String): Double? =
        db.openHelper.readableDatabase.query(sql).use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getDouble(0) else null
        }

    private fun leerTexto(sql: String): String? =
        db.openHelper.readableDatabase.query(sql).use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getString(0) else null
        }
}
