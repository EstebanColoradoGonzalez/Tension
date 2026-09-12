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
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * El 1RM estimado, ejercitado **por el camino de escritura real** (HU-42).
 *
 * Se prueba sobre una base de verdad porque ahí es donde vive: la decisión es de
 * `OneRmRule` y está cubierta en JVM, pero lo que esta prueba vigila es la fontanería que
 * la rodea —qué peso se le pasa, dentro de qué transacción, contra qué fila se compara— y
 * eso es SQL y orden de sentencias, no una función pura.
 *
 * El fallo que vigila **no lanza ninguna excepción**: produce un número plausible y, como
 * no hay retro-cálculo ni recálculo posible, permanentemente equivocado.
 */
@RunWith(AndroidJUnit4::class)
class MiUnRmEstimadoTest {

    private lateinit var db: TensionDatabase
    private lateinit var repository: SessionRepositoryImpl

    /** `Elevación Lateral`: admite mancuerna, polea y máquina. */
    private val elevacionLateral = 10L

    /** `Dominadas`: el único de peso corporal del catálogo semilla. */
    private val dominadas = 35L

    /** `Crunch Abdominal`: admite `Peso Corporal` sin estar marcado como peso corporal. */
    private val crunchAbdominal = 3L

    private val mancuerna = EquipmentCatalog.MANCUERNA
    private val polea = EquipmentCatalog.POLEA
    private val barraFija = EquipmentCatalog.BARRA_FIJA
    private val maquina = EquipmentCatalog.MAQUINA
    private val pesoAnadido = EquipmentCatalog.PESO_ANADIDO
    private val pesoCorporal = EquipmentCatalog.PESO_CORPORAL

    /** Ejercicio isométrico creado a mano: el catálogo semilla no trae ninguno. */
    private val isometrico = 900L

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

        routineVersionId = leerLong("SELECT id FROM routine_version LIMIT 1")!!
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ----- CA-42.03: solo la serie de referencia dispara el cálculo -----

    @Test
    fun la_serie_de_referencia_crea_el_record_del_par() = runBlocking {
        registrar(elevacionLateral, mancuerna, pesoKg = 12.0, reps = 10, rir = 1)

        assertEquals(12.0 * 1.3337, oneRm(elevacionLateral, mancuerna)!!, 0.05)
    }

    @Test
    fun una_serie_con_repeticiones_distintas_no_crea_record() = runBlocking {
        registrar(elevacionLateral, mancuerna, pesoKg = 20.0, reps = 9, rir = 1)
        registrar(elevacionLateral, mancuerna, pesoKg = 20.0, reps = 11, rir = 1)

        assertNull(oneRm(elevacionLateral, mancuerna))
    }

    @Test
    fun una_serie_con_rir_distinto_no_crea_record() = runBlocking {
        registrar(elevacionLateral, mancuerna, pesoKg = 20.0, reps = 10, rir = 0)
        registrar(elevacionLateral, mancuerna, pesoKg = 20.0, reps = 10, rir = 2)

        assertNull(oneRm(elevacionLateral, mancuerna))
    }

    /**
     * El caso que más se parece a un fallo silencioso: series **más pesadas** que las que
     * calificaron, que no deben mover el récord ni un gramo.
     */
    @Test
    fun una_serie_que_no_califica_no_altera_el_record_existente() = runBlocking {
        registrar(elevacionLateral, mancuerna, pesoKg = 12.0, reps = 10, rir = 1)
        val record = oneRm(elevacionLateral, mancuerna)!!

        registrar(elevacionLateral, mancuerna, pesoKg = 20.0, reps = 9, rir = 1)
        registrar(elevacionLateral, mancuerna, pesoKg = 20.0, reps = 10, rir = 0)

        assertEquals(record, oneRm(elevacionLateral, mancuerna)!!, 0.0001)
    }

    // ----- CA-42.04: un récord que nunca baja -----

    @Test
    fun una_serie_mayor_sube_el_record() = runBlocking {
        registrar(elevacionLateral, mancuerna, pesoKg = 11.0, reps = 10, rir = 1)
        registrar(elevacionLateral, mancuerna, pesoKg = 12.0, reps = 10, rir = 1)

        assertEquals(12.0 * 1.3337, oneRm(elevacionLateral, mancuerna)!!, 0.05)
    }

    @Test
    fun una_serie_menor_no_deteriora_el_record() = runBlocking {
        registrar(elevacionLateral, mancuerna, pesoKg = 12.0, reps = 10, rir = 1)
        registrar(elevacionLateral, mancuerna, pesoKg = 9.0, reps = 10, rir = 1)

        assertEquals(12.0 * 1.3337, oneRm(elevacionLateral, mancuerna)!!, 0.05)
    }

    @Test
    fun varias_series_que_califican_en_la_misma_sesion_dejan_la_mas_alta() = runBlocking {
        val sessionExerciseId = abrirEjercicio(elevacionLateral)
        serie(sessionExerciseId, mancuerna, pesoKg = 11.0, reps = 10, rir = 1)
        serie(sessionExerciseId, mancuerna, pesoKg = 12.0, reps = 10, rir = 1)
        serie(sessionExerciseId, mancuerna, pesoKg = 11.5, reps = 10, rir = 1)

        assertEquals(12.0 * 1.3337, oneRm(elevacionLateral, mancuerna)!!, 0.05)
    }

    /** El récord es del par: entrenar otro implemento no toca el del primero. */
    @Test
    fun cada_implemento_lleva_su_propio_record() = runBlocking {
        registrar(elevacionLateral, mancuerna, pesoKg = 12.0, reps = 10, rir = 1)
        registrar(elevacionLateral, polea, pesoKg = 17.0, reps = 10, rir = 1)

        assertEquals(12.0 * 1.3337, oneRm(elevacionLateral, mancuerna)!!, 0.05)
        assertEquals(17.0 * 1.3337, oneRm(elevacionLateral, polea)!!, 0.05)
    }

    // ----- CA-42.05: sin carga externa no hay 1RM -----

    /**
     * Las opciones de `Dominadas`. `Barra Fija` es la dominada estricta y `Máquina` la
     * asistida, cuyo contrapeso **resta** esfuerzo; solo `Peso Añadido` deja récord, y sobre
     * el lastre únicamente — el peso corporal del ejecutante no se suma.
     */
    @Test
    fun de_las_opciones_de_dominadas_solo_peso_anadido_deja_record() = runBlocking {
        registrar(dominadas, barraFija, pesoKg = 0.0, reps = 10, rir = 1)
        registrar(dominadas, maquina, pesoKg = 0.0, reps = 10, rir = 1)
        registrar(dominadas, pesoAnadido, pesoKg = 10.0, reps = 10, rir = 1)

        assertNull(oneRm(dominadas, barraFija))
        assertNull(oneRm(dominadas, maquina))
        assertEquals(10.0 * 1.3337, oneRm(dominadas, pesoAnadido)!!, 0.05)
        assertEquals(1L, leerLong("SELECT COUNT(*) FROM exercise_one_rm WHERE exercise_id = $dominadas"))
    }

    /**
     * `Peso Corporal` sobre un ejercicio que no está marcado como tal — el caso que motivó
     * `ExternalLoadRule`. La serie se registra en cero y no hay fuerza que medir.
     */
    @Test
    fun peso_corporal_no_deja_record_aunque_el_ejercicio_no_sea_de_peso_corporal() = runBlocking {
        registrar(crunchAbdominal, pesoCorporal, pesoKg = 0.0, reps = 10, rir = 1)

        assertNull(oneRm(crunchAbdominal, pesoCorporal))
    }

    @Test
    fun un_ejercicio_isometrico_queda_excluido() = runBlocking {
        crearEjercicioIsometrico()

        registrar(isometrico, mancuerna, pesoKg = 20.0, reps = 10, rir = 1)

        assertNull(oneRm(isometrico, mancuerna))
    }

    // ----- El aislamiento: la tabla no alimenta nada -----

    /**
     * La escritura del récord es un efecto colateral que no puede alterar lo que el motor
     * decide. Aquí se comprueba lo mínimo verificable en ejecución: registrar la serie de
     * referencia deja el estado de progresión del par exactamente como lo dejaría sin la
     * historia.
     */
    @Test
    fun el_record_no_altera_el_estado_de_progresion_del_par() = runBlocking {
        registrar(elevacionLateral, mancuerna, pesoKg = 12.0, reps = 10, rir = 1)

        assertTrue(oneRm(elevacionLateral, mancuerna)!! > 0)
        assertEquals(
            "NO_HISTORY",
            leerTexto(
                "SELECT status FROM exercise_progression " +
                    "WHERE exercise_id = $elevacionLateral AND equipment_type_id = $mancuerna",
            ),
        )
    }

    // ----- Utilidades de fixture -----

    /** Abre una sesión con el ejercicio indicado y devuelve el `session_exercise`. */
    private suspend fun abrirEjercicio(exerciseId: Long): Long {
        val sessionId = db.sessionDao().insert(
            SessionEntity(routineVersionId = routineVersionId, date = "2026-09-11"),
        )
        db.sessionExerciseDao().insertAll(
            listOf(SessionExerciseEntity(sessionId = sessionId, exerciseId = exerciseId, slot = 1)),
        )
        return leerLong("SELECT id FROM session_exercise WHERE session_id = $sessionId LIMIT 1")!!
    }

    /** Una sesión de una sola serie del par indicado. */
    private suspend fun registrar(
        exerciseId: Long,
        equipamiento: Long,
        pesoKg: Double,
        reps: Int,
        rir: Int,
    ) {
        serie(abrirEjercicio(exerciseId), equipamiento, pesoKg, reps, rir)
    }

    private suspend fun serie(
        sessionExerciseId: Long,
        equipamiento: Long,
        pesoKg: Double,
        reps: Int,
        rir: Int,
    ) {
        repository.registerSet(
            sessionExerciseId = sessionExerciseId,
            weightKg = pesoKg,
            reps = reps,
            rir = rir,
            captureUnit = WeightUnit.KG,
            equipmentTypeId = equipamiento,
        )
    }

    /**
     * El catálogo semilla no trae ningún isométrico, así que la exclusión se prueba sobre uno
     * creado a mano. Solo necesita la marca y un implemento admitido.
     */
    private fun crearEjercicioIsometrico() {
        db.openHelper.writableDatabase.execSQL(
            "INSERT INTO exercise (id, name, media_resource, is_bodyweight, is_isometric, " +
                "is_to_technical_failure, progression_difficulty, is_custom) " +
                "VALUES ($isometrico, 'Isométrico de prueba', 'x', 0, 1, 0, 'MEDIUM', 1)",
        )
        db.openHelper.writableDatabase.execSQL(
            "INSERT INTO exercise_muscle_zone (exercise_id, muscle_zone_id, is_primary) " +
                "SELECT $isometrico, id, 1 FROM muscle_zone LIMIT 1",
        )
        db.openHelper.writableDatabase.execSQL(
            "INSERT INTO exercise_equipment (exercise_id, equipment_type_id) " +
                "VALUES ($isometrico, $mancuerna)",
        )
    }

    private fun oneRm(exerciseId: Long, equipmentTypeId: Long): Double? = leerDouble(
        "SELECT one_rm_kg FROM exercise_one_rm " +
            "WHERE exercise_id = $exerciseId AND equipment_type_id = $equipmentTypeId",
    )

    private fun leerLong(sql: String): Long? = db.query(sql, null).use {
        if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else null
    }

    private fun leerDouble(sql: String): Double? = db.query(sql, null).use {
        if (it.moveToFirst() && !it.isNull(0)) it.getDouble(0) else null
    }

    private fun leerTexto(sql: String): String? = db.query(sql, null).use {
        if (it.moveToFirst() && !it.isNull(0)) it.getString(0) else null
    }
}
