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
 * El caso que da nombre a la historia: **estrenar un implemento no es retroceder** (CA-40.02).
 *
 * Se ejercita el motor de decisión completo sobre una base real porque ahí es donde vive: la
 * clasificación por par depende de qué sesión elige la subconsulta que busca el histórico del
 * par, y eso es SQL, no una regla pura. El fallo que esta prueba vigila —poner el filtro por
 * implemento fuera de esa subconsulta— **se parece al comportamiento correcto**: devuelve cero
 * series, que el motor lee como «sin historial», así que ninguna aserción sobre la clasificación
 * del par nuevo lo detectaría. Lo que lo delata es el par **antiguo**, que a partir de ese
 * momento tampoco vuelve a tener con qué compararse.
 */
@RunWith(AndroidJUnit4::class)
class ProgresionPorEquipamientoTest {

    private lateinit var db: TensionDatabase
    private lateinit var repository: SessionRepositoryImpl

    /** `Elevación Lateral`: admite mancuerna, polea y máquina, y su dificultad es alta. */
    private val elevacionLateral = 10L
    private val mancuerna = EquipmentCatalog.MANCUERNA
    private val polea = EquipmentCatalog.POLEA

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
            alertDao = db.alertDao(),
            database = db,
            deloadDao = db.deloadDao(),
            exerciseDao = db.exerciseDao(),
            equipmentTypeDao = db.equipmentTypeDao(),
            profileDao = db.profileDao(),
            currentDateProvider = CurrentDateProvider(),
        )

        routineVersionId = leerLong(
            "SELECT routine_version_id FROM plan_assignment WHERE exercise_id = $elevacionLateral LIMIT 1",
        ) ?: leerLong("SELECT id FROM routine_version LIMIT 1")!!
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ----- CA-40.02: la primera serie con un implemento nuevo no es regresión -----

    @Test
    fun estrenar_un_implemento_con_menos_peso_no_produce_regresion() = runBlocking {
        // Given — dos sesiones con mancuerna, la segunda subiendo de 11.5 a 12.0 Kg
        entrenar(fecha = "2026-08-28", equipamiento = mancuerna, pesoKg = 11.5)
        entrenar(fecha = "2026-09-04", equipamiento = mancuerna, pesoKg = 12.0)

        val cargaMancuernaAntes = leerDouble(
            "SELECT prescribed_load_kg FROM exercise_progression " +
                "WHERE exercise_id = $elevacionLateral AND equipment_type_id = $mancuerna",
        )
        val estadoMancuernaAntes = leerTexto(
            "SELECT status FROM exercise_progression " +
                "WHERE exercise_id = $elevacionLateral AND equipment_type_id = $mancuerna",
        )
        val contadorMancuernaAntes = leerLong(
            "SELECT sessions_without_progression FROM exercise_progression " +
                "WHERE exercise_id = $elevacionLateral AND equipment_type_id = $mancuerna",
        )

        // When — se estrena la polea con un peso menor por razones puramente mecánicas
        val sesionPolea = entrenar(fecha = "2026-09-06", equipamiento = polea, pesoKg = 8.0)

        // Then — la clasificación del ejercicio es Sin Historial, jamás REGRESSION
        val clasificacionConsolidada = leerTexto(
            "SELECT progression_classification FROM session_exercise " +
                "WHERE session_id = $sesionPolea",
        )
        assertNull("Estrenar un implemento se leyó como un bache", clasificacionConsolidada)

        val clasificacionDelPar = leerTexto(
            "SELECT sep.progression_classification FROM session_exercise_progression sep " +
                "INNER JOIN session_exercise se ON sep.session_exercise_id = se.id " +
                "WHERE se.session_id = $sesionPolea AND sep.equipment_type_id = $polea",
        )
        assertNull("El par nuevo no tiene contra qué compararse", clasificacionDelPar)

        // Y el contador del par nuevo arranca en 0
        assertEquals(
            0L,
            leerLong(
                "SELECT sessions_without_progression FROM exercise_progression " +
                    "WHERE exercise_id = $elevacionLateral AND equipment_type_id = $polea",
            ),
        )

        // Y el par de la mancuerna no se altera por esa sesión
        assertEquals(
            estadoMancuernaAntes,
            leerTexto(
                "SELECT status FROM exercise_progression " +
                    "WHERE exercise_id = $elevacionLateral AND equipment_type_id = $mancuerna",
            ),
        )
        assertEquals(
            contadorMancuernaAntes,
            leerLong(
                "SELECT sessions_without_progression FROM exercise_progression " +
                    "WHERE exercise_id = $elevacionLateral AND equipment_type_id = $mancuerna",
            ),
        )
        // Y su carga prescrita no se reduce
        assertEquals(
            cargaMancuernaAntes!!,
            leerDouble(
                "SELECT prescribed_load_kg FROM exercise_progression " +
                    "WHERE exercise_id = $elevacionLateral AND equipment_type_id = $mancuerna",
            )!!,
            0.001,
        )
    }

    @Test
    fun un_ejercicio_entrenado_con_tres_implementos_mantiene_tres_estados() = runBlocking {
        // CA-40.01
        entrenar(fecha = "2026-08-28", equipamiento = mancuerna, pesoKg = 12.0)
        entrenar(fecha = "2026-08-30", equipamiento = polea, pesoKg = 8.0)
        entrenar(fecha = "2026-09-01", equipamiento = EquipmentCatalog.MAQUINA, pesoKg = 20.0)

        assertEquals(
            3L,
            leerLong(
                "SELECT COUNT(*) FROM exercise_progression WHERE exercise_id = $elevacionLateral",
            ),
        )
    }

    @Test
    fun el_par_antiguo_conserva_su_historico_tras_estrenar_otro_implemento() = runBlocking {
        // Es la aserción que delata el filtro por implemento puesto fuera de la subconsulta
        // que elige la sesión: si la última sesión del ejercicio fuera de polea y contase
        // como «la anterior» de la mancuerna, la mancuerna se quedaría sin comparación y
        // volvería a clasificar como Sin Historial para siempre.
        entrenar(fecha = "2026-08-28", equipamiento = mancuerna, pesoKg = 11.5)
        entrenar(fecha = "2026-09-04", equipamiento = polea, pesoKg = 8.0)

        // When — se vuelve a la mancuerna, subiendo carga
        val sesion = entrenar(fecha = "2026-09-06", equipamiento = mancuerna, pesoKg = 12.5)

        // Then — la mancuerna se compara con la mancuerna, no con lo último que se hizo
        assertEquals(
            "POSITIVE_PROGRESSION",
            leerTexto(
                "SELECT progression_classification FROM session_exercise " +
                    "WHERE session_id = $sesion",
            ),
        )
    }

    @Test
    fun dos_implementos_en_la_misma_sesion_se_clasifican_por_separado_y_se_consolidan() =
        runBlocking {
            // CA-40.02 y CA-40.04: la mancuerna progresa, la polea se estrena en la misma
            // sesión, y el ejercicio se lee como progresión.
            entrenar(fecha = "2026-08-28", equipamiento = mancuerna, pesoKg = 11.5)

            val sesion = crearSesion("2026-09-06")
            val sessionExerciseId = crearSessionExercise(sesion)
            registrarSeries(sessionExerciseId, mancuerna, 12.5)
            registrarSeries(sessionExerciseId, polea, 8.0)
            repository.closeSession(sesion)

            val pares = leerLong(
                "SELECT COUNT(*) FROM session_exercise_progression " +
                    "WHERE session_exercise_id = $sessionExerciseId",
            )
            assertEquals(2L, pares)

            assertEquals(
                "POSITIVE_PROGRESSION",
                leerTexto(
                    "SELECT progression_classification FROM session_exercise_progression " +
                        "WHERE session_exercise_id = $sessionExerciseId " +
                        "AND equipment_type_id = $mancuerna",
                ),
            )
            assertNull(
                leerTexto(
                    "SELECT progression_classification FROM session_exercise_progression " +
                        "WHERE session_exercise_id = $sessionExerciseId " +
                        "AND equipment_type_id = $polea",
                ),
            )
            // Alternar implementos no diluye la progresión del ejercicio
            assertEquals(
                "POSITIVE_PROGRESSION",
                leerTexto(
                    "SELECT progression_classification FROM session_exercise " +
                        "WHERE id = $sessionExerciseId",
                ),
            )
        }

    // ----- CA-40.05: la meseta necesita que todos los pares estén estancados -----

    @Test
    fun un_par_en_progresion_impide_declarar_meseta() = runBlocking {
        // `Elevación Lateral` es de dificultad alta: umbral efectivo 10 con la base por
        // defecto de 5. Diez sesiones de mancuerna sin avanzar la dejan estancada.
        entrenar(fecha = "2026-06-01", equipamiento = mancuerna, pesoKg = 12.0)
        repeat(11) { i ->
            entrenar(
                fecha = "2026-06-${(2 + i).toString().padStart(2, '0')}",
                equipamiento = mancuerna,
                pesoKg = 12.0,
                reps = 8,
            )
        }

        val contadorMancuerna = leerLong(
            "SELECT sessions_without_progression FROM exercise_progression " +
                "WHERE exercise_id = $elevacionLateral AND equipment_type_id = $mancuerna",
        )!!
        assertTrue("La mancuerna debería estar estancada", contadorMancuerna >= 10)

        // Se estrena la polea y en la siguiente sesión progresa
        entrenar(fecha = "2026-07-01", equipamiento = polea, pesoKg = 8.0)
        entrenar(fecha = "2026-07-03", equipamiento = polea, pesoKg = 9.0)

        // La conjunción no se cumple: no hay meseta ni alerta
        assertEquals(
            0L,
            leerLong(
                "SELECT COUNT(*) FROM alert WHERE type = 'PLATEAU' AND is_active = 1 " +
                    "AND exercise_id = $elevacionLateral",
            ),
        )
    }

    // ----- Utilidades de fixture -----

    /** Abre una sesión, registra cuatro series del par indicado y la cierra. */
    private suspend fun entrenar(
        fecha: String,
        equipamiento: Long,
        pesoKg: Double,
        reps: Int = 10,
    ): Long {
        val sessionId = crearSesion(fecha)
        val sessionExerciseId = crearSessionExercise(sessionId)
        registrarSeries(sessionExerciseId, equipamiento, pesoKg, reps)
        repository.closeSession(sessionId)
        return sessionId
    }

    private suspend fun crearSesion(fecha: String): Long = db.sessionDao().insert(
        SessionEntity(routineVersionId = routineVersionId, date = fecha),
    )

    private suspend fun crearSessionExercise(sessionId: Long): Long {
        db.sessionExerciseDao().insertAll(
            listOf(
                SessionExerciseEntity(
                    sessionId = sessionId,
                    exerciseId = elevacionLateral,
                    slot = 1,
                ),
            ),
        )
        return leerLong(
            "SELECT id FROM session_exercise WHERE session_id = $sessionId LIMIT 1",
        )!!
    }

    private suspend fun registrarSeries(
        sessionExerciseId: Long,
        equipamiento: Long,
        pesoKg: Double,
        reps: Int = 10,
    ) {
        repeat(4) {
            repository.registerSet(
                sessionExerciseId = sessionExerciseId,
                weightKg = pesoKg,
                reps = reps,
                rir = 1,
                captureUnit = WeightUnit.KG,
                equipmentTypeId = equipamiento,
            )
        }
    }

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
