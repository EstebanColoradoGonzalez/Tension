package com.estebancoloradogonzalez.tension

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.estebancoloradogonzalez.tension.data.local.database.TensionDatabase
import com.estebancoloradogonzalez.tension.data.local.seed.EquipmentCatalog
import com.estebancoloradogonzalez.tension.data.local.seed.PrepopulateCallback
import com.estebancoloradogonzalez.tension.data.repository.SessionRepositoryImpl
import com.estebancoloradogonzalez.tension.domain.model.WeightUnit
import com.estebancoloradogonzalez.tension.domain.util.CurrentDateProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * El ajuste temporal de la sesión, ejercitado sobre una base real (HU-43).
 *
 * La aritmética del presupuesto y la invariante viven en `SessionAdjustmentRule` y están
 * cubiertas en JVM. Lo que esta prueba vigila es lo que ninguna regla pura alcanza: el SQL
 * —las guardas de `slot`, el orden, la prescripción propia—, la atomicidad de las dos
 * escrituras del retiro, y el efecto del ajuste sobre el cierre y sobre el plan.
 *
 * El defecto que más vigila es **silencioso**: los puestos del plan empiezan en `0`, igual
 * que el valor por defecto de la columna, así que sin las guardas un ejercicio añadido
 * hereda por el `LEFT JOIN` las series del puesto 0 y por la subconsulta sus alternativas.
 * No lanza excepción: muestra `4 series` donde deben ir `3`.
 */
@RunWith(AndroidJUnit4::class)
class AjustarSesionDelDiaTest {

    private lateinit var db: TensionDatabase
    private lateinit var repository: SessionRepositoryImpl

    private var routineVersionId: Long = 0
    private var sessionId: Long = 0

    @Before
    fun setUp() = runBlocking {
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

        // `rotation_state` no lo siembra PrepopulateCallback: lo crea el alta de perfil
        // (ProfileRepositoryImpl), que en una base de prueba no ocurre. Sin esta fila
        // `closeSession` no llega a avanzar la rotacion.
        db.openHelper.writableDatabase.execSQL(
            "INSERT OR IGNORE INTO rotation_state (id, microcycle_position, microcycle_count) " +
                "VALUES (1, 1, 0)",
        )

        routineVersionId = leerLong("SELECT id FROM routine_version ORDER BY id LIMIT 1")!!
        sessionId = repository.startSession(routineVersionId)
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ----- CA-43.01 y CA-43.03: el añadido entra al final, sin puesto y con su prescripción -----

    @Test
    fun el_anadido_entra_al_final_con_tres_series_de_ocho_a_doce() = runBlocking {
        val nuevo = ejercicioFueraDeLaSesion()

        repository.addExerciseToSession(sessionId, nuevo)

        val lista = repository.getSessionExercises(sessionId).first()
        val anadido = lista.last()
        assertEquals(nuevo, anadido.exerciseId)
        assertTrue(anadido.isExtra)
        assertEquals(3, anadido.sets)
        assertEquals("8-12", anadido.reps)
        assertFalse(anadido.hasAlternatives)
        // Todos los demás siguen siendo del plan, y ninguno se movió.
        assertTrue(lista.dropLast(1).none { it.isExtra })
    }

    @Test
    fun pueden_anadirse_varios_y_conservan_el_orden_de_llegada() = runBlocking {
        val primero = ejercicioFueraDeLaSesion()
        repository.addExerciseToSession(sessionId, primero)
        val segundo = ejercicioFueraDeLaSesion()
        repository.addExerciseToSession(sessionId, segundo)

        val lista = repository.getSessionExercises(sessionId).first()
        assertEquals(listOf(primero, segundo), lista.takeLast(2).map { it.exerciseId })
    }

    @Test
    fun un_isometrico_anadido_se_prescribe_en_segundos() = runBlocking {
        val isometrico = crearEjercicio("Plancha de prueba", isIsometric = 1)

        repository.addExerciseToSession(sessionId, isometrico)

        val anadido = repository.getSessionExercises(sessionId).first().last()
        assertEquals("30-45_SEC", anadido.reps)
        assertEquals(3, anadido.sets)
    }

    @Test
    fun uno_de_fallo_tecnico_anadido_se_prescribe_sin_limite_superior() = runBlocking {
        val alFallo = crearEjercicio("Al fallo de prueba", isToTechnicalFailure = 1)

        repository.addExerciseToSession(sessionId, alFallo)

        assertEquals(
            "TO_TECHNICAL_FAILURE",
            repository.getSessionExercises(sessionId).first().last().reps,
        )
    }

    /**
     * La guarda de `slot`, sobre el caso exacto que la hace falta.
     *
     * Se siembra un puesto `0` con dos alternativas y una prescripción distinta de la por
     * defecto. Sin `AND se.is_extra = 0` en el `LEFT JOIN` y sin el `CASE` de
     * `alternativesInSlot`, el añadido —que lleva `slot = 0`— saldría con 7 series, al fallo
     * técnico y con botón de intercambio.
     */
    @Test
    fun el_anadido_no_hereda_nada_del_puesto_cero() = runBlocking {
        val alternativaA = ejercicioFueraDeLaSesion()
        val alternativaB = ejercicioFueraDeLaSesion(excepto = setOf(alternativaA))
        asignarAlPlan(alternativaA, slot = 0, sets = 7, reps = "TO_TECHNICAL_FAILURE")
        asignarAlPlan(alternativaB, slot = 0, sets = 7, reps = "TO_TECHNICAL_FAILURE")

        repository.addExerciseToSession(sessionId, alternativaA)

        val anadido = repository.getSessionExercises(sessionId).first().last()
        assertEquals(alternativaA, anadido.exerciseId)
        assertEquals(3, anadido.sets)
        assertEquals("8-12", anadido.reps)
        assertFalse(anadido.hasAlternatives)
    }

    @Test
    fun un_ejercicio_que_ya_esta_en_la_sesion_no_se_anade_otra_vez() = runBlocking {
        val presente = repository.getSessionExercises(sessionId).first().first().exerciseId!!

        try {
            repository.addExerciseToSession(sessionId, presente)
            fail("se esperaba el rechazo del duplicado")
        } catch (_: IllegalStateException) {
            // esperado
        }

        assertEquals(0, contar("SELECT COUNT(*) FROM session_exercise WHERE is_extra = 1"))
    }

    // ----- CA-43.04: el presupuesto -----

    @Test
    fun sin_anadidos_no_puede_retirarse_ningun_ejercicio_del_plan() = runBlocking {
        val delPlan = repository.getSessionExercises(sessionId).first().first()

        try {
            repository.withdrawFromSession(delPlan.sessionExerciseId)
            fail("se esperaba el rechazo por presupuesto agotado")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("BudgetExhausted"))
        }

        assertEquals(0, contar("SELECT COUNT(*) FROM session_withdrawal"))
    }

    @Test
    fun con_un_anadido_puede_retirarse_uno_del_plan_y_solo_uno() = runBlocking {
        repository.addExerciseToSession(sessionId, ejercicioFueraDeLaSesion())
        val delPlan = repository.getSessionExercises(sessionId).first().filter { !it.isExtra }

        repository.withdrawFromSession(delPlan[0].sessionExerciseId)

        val ajuste = repository.getSessionAdjustment(sessionId).first()
        assertEquals(1, ajuste.addedCount)
        assertEquals(1, ajuste.withdrawnCount)
        assertEquals(0, ajuste.budget)

        try {
            repository.withdrawFromSession(delPlan[1].sessionExerciseId)
            fail("el presupuesto ya estaba agotado")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("BudgetExhausted"))
        }
    }

    @Test
    fun el_retirado_desaparece_de_la_sesion_y_queda_registrado_con_nombre() = runBlocking {
        repository.addExerciseToSession(sessionId, ejercicioFueraDeLaSesion())
        val delPlan = repository.getSessionExercises(sessionId).first().first { !it.isExtra }

        repository.withdrawFromSession(delPlan.sessionExerciseId)

        val lista = repository.getSessionExercises(sessionId).first()
        assertTrue(lista.none { it.sessionExerciseId == delPlan.sessionExerciseId })
        val ajuste = repository.getSessionAdjustment(sessionId).first()
        assertEquals(listOf(delPlan.name), ajuste.pendingWithdrawals.map { it.name })
    }

    // ----- CA-43.05: la invariante y la reposición -----

    @Test
    fun la_secuencia_completa_del_ajuste_conserva_la_invariante() = runBlocking {
        // 1. añadir: añadidos 1 · retirados 0 · presupuesto 1
        val anadido = ejercicioFueraDeLaSesion()
        repository.addExerciseToSession(sessionId, anadido)
        assertEquals(1, repository.getSessionAdjustment(sessionId).first().budget)

        // 2. retirar uno del plan: añadidos 1 · retirados 1 · presupuesto 0
        val delPlan = repository.getSessionExercises(sessionId).first().first { !it.isExtra }
        repository.withdrawFromSession(delPlan.sessionExerciseId)
        assertEquals(0, repository.getSessionAdjustment(sessionId).first().budget)

        // 3. deshacer el añadido rompería la invariante
        val filaAnadida = repository.getSessionExercises(sessionId).first().first { it.isExtra }
        try {
            repository.withdrawFromSession(filaAnadida.sessionExerciseId)
            fail("deshacer con un retiro vigente romperia la invariante")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("WouldBreakInvariant"))
        }

        // 4. reponer con la MISMA acción de añadir: añadidos 2 · retirados 1
        repository.addExerciseToSession(sessionId, delPlan.exerciseId!!)
        val trasReponer = repository.getSessionAdjustment(sessionId).first()
        assertEquals(2, trasReponer.addedCount)
        assertEquals(1, trasReponer.withdrawnCount)
        // El retiro deja de estar pendiente, pero sigue contado.
        assertTrue(trasReponer.pendingWithdrawals.isEmpty())

        // 5. el repuesto vuelve al final, no a su puesto original
        val lista = repository.getSessionExercises(sessionId).first()
        assertEquals(delPlan.exerciseId, lista.last().exerciseId)
        assertTrue(lista.last().isExtra)
        assertEquals(3, lista.last().sets)

        // 6. ahora sí puede deshacerse el añadido original
        repository.withdrawFromSession(filaAnadida.sessionExerciseId)
        val cierre = repository.getSessionAdjustment(sessionId).first()
        assertEquals(1, cierre.addedCount)
        assertEquals(1, cierre.withdrawnCount)
        assertTrue(cierre.addedCount >= cierre.withdrawnCount)
    }

    @Test
    fun deshacer_un_anadido_no_genera_un_retiro() = runBlocking {
        val anadido = ejercicioFueraDeLaSesion()
        repository.addExerciseToSession(sessionId, anadido)
        val fila = repository.getSessionExercises(sessionId).first().first { it.isExtra }

        repository.withdrawFromSession(fila.sessionExerciseId)

        val ajuste = repository.getSessionAdjustment(sessionId).first()
        assertEquals(0, ajuste.addedCount)
        assertEquals(0, ajuste.withdrawnCount)
        assertEquals(0, contar("SELECT COUNT(*) FROM session_withdrawal"))
    }

    // ----- CA-43.06: nada con series registradas se retira -----

    @Test
    fun un_ejercicio_del_plan_con_series_no_se_retira() = runBlocking {
        repository.addExerciseToSession(sessionId, ejercicioFueraDeLaSesion())
        val delPlan = repository.getSessionExercises(sessionId).first().first { !it.isExtra }
        registrarSerie(delPlan.sessionExerciseId)

        try {
            repository.withdrawFromSession(delPlan.sessionExerciseId)
            fail("un ejercicio con series no se retira")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("HasSets"))
        }

        assertTrue(
            repository.getSessionExercises(sessionId).first()
                .any { it.sessionExerciseId == delPlan.sessionExerciseId },
        )
    }

    @Test
    fun un_anadido_con_series_tampoco_se_retira() = runBlocking {
        repository.addExerciseToSession(sessionId, ejercicioFueraDeLaSesion())
        val anadido = repository.getSessionExercises(sessionId).first().first { it.isExtra }
        registrarSerie(anadido.sessionExerciseId)

        try {
            repository.withdrawFromSession(anadido.sessionExerciseId)
            fail("un anadido con series no se retira")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("HasSets"))
        }
    }

    // ----- CA-43.07: haberlo añadido es haberlo comprometido -----

    @Test
    fun un_anadido_sin_series_cierra_la_sesion_como_incompleta() = runBlocking {
        // Todos los del plan, completos; el añadido, en 0 series.
        repository.getSessionExercises(sessionId).first().forEach {
            registrarSerie(it.sessionExerciseId)
            repository.finalizeExercise(it.sessionExerciseId)
        }
        repository.addExerciseToSession(sessionId, ejercicioFueraDeLaSesion())

        repository.closeSession(sessionId)

        assertEquals("INCOMPLETE", leerTexto("SELECT status FROM session WHERE id = $sessionId"))
    }

    @Test
    fun retirar_el_anadido_antes_de_cerrar_devuelve_la_sesion_a_completa() = runBlocking {
        repository.getSessionExercises(sessionId).first().forEach {
            registrarSerie(it.sessionExerciseId)
            repository.finalizeExercise(it.sessionExerciseId)
        }
        repository.addExerciseToSession(sessionId, ejercicioFueraDeLaSesion())
        val anadido = repository.getSessionExercises(sessionId).first().first { it.isExtra }
        repository.withdrawFromSession(anadido.sessionExerciseId)

        repository.closeSession(sessionId)

        assertEquals("COMPLETED", leerTexto("SELECT status FROM session WHERE id = $sessionId"))
    }

    // ----- CA-43.08: el alcance temporal del ajuste -----

    @Test
    fun el_plan_queda_intacto_tras_una_sesion_ajustada() = runBlocking {
        val antes = volcarPlan()

        repository.addExerciseToSession(sessionId, ejercicioFueraDeLaSesion())
        val delPlan = repository.getSessionExercises(sessionId).first().first { !it.isExtra }
        repository.withdrawFromSession(delPlan.sessionExerciseId)
        repository.getSessionExercises(sessionId).first().forEach {
            registrarSerie(it.sessionExerciseId)
            repository.finalizeExercise(it.sessionExerciseId)
        }
        repository.closeSession(sessionId)

        assertEquals(antes, volcarPlan())
    }

    @Test
    fun la_siguiente_sesion_reproduce_la_composicion_original() = runBlocking {
        val composicionOriginal = repository.getSessionExercises(sessionId).first()
            .map { it.exerciseId to it.slot }

        repository.addExerciseToSession(sessionId, ejercicioFueraDeLaSesion())
        val delPlan = repository.getSessionExercises(sessionId).first().first { !it.isExtra }
        repository.withdrawFromSession(delPlan.sessionExerciseId)
        repository.getSessionExercises(sessionId).first().forEach {
            registrarSerie(it.sessionExerciseId)
            repository.finalizeExercise(it.sessionExerciseId)
        }
        repository.closeSession(sessionId)

        val siguiente = repository.startSession(routineVersionId)
        assertEquals(
            composicionOriginal,
            repository.getSessionExercises(siguiente).first().map { it.exerciseId to it.slot },
        )
        assertEquals(0, contar("SELECT COUNT(*) FROM session_exercise WHERE session_id = $siguiente AND is_extra = 1"))
    }

    @Test
    fun el_detalle_de_la_sesion_pasada_nombra_el_retirado_y_marca_el_anadido() = runBlocking {
        val anadido = ejercicioFueraDeLaSesion()
        repository.addExerciseToSession(sessionId, anadido)
        val delPlan = repository.getSessionExercises(sessionId).first().first { !it.isExtra }
        repository.withdrawFromSession(delPlan.sessionExerciseId)
        repository.getSessionExercises(sessionId).first().forEach {
            registrarSerie(it.sessionExerciseId)
            repository.finalizeExercise(it.sessionExerciseId)
        }
        repository.closeSession(sessionId)

        val detalle = repository.getSessionDetail(sessionId)
        assertEquals(listOf(delPlan.name), detalle.withdrawnExerciseNames)
        assertTrue(detalle.exercises.last().isExtra)
        assertTrue(detalle.exercises.dropLast(1).none { it.isExtra })
    }

    @Test
    fun el_ejercicio_creado_desde_la_sesion_permanece_en_el_diccionario_y_no_en_el_plan() =
        runBlocking {
            val creado = crearEjercicio("Press Pallof de prueba")
            repository.addExerciseToSession(sessionId, creado)
            val fila = repository.getSessionExercises(sessionId).first().first { it.isExtra }
            repository.withdrawFromSession(fila.sessionExerciseId)

            assertEquals(1, contar("SELECT COUNT(*) FROM exercise WHERE id = $creado"))
            assertEquals(
                0,
                contar("SELECT COUNT(*) FROM plan_assignment WHERE exercise_id = $creado"),
            )
        }

    // ----- Utilidades de fixture -----

    /** Un ejercicio del Diccionario que no está hoy en la sesión. */
    private fun ejercicioFueraDeLaSesion(excepto: Set<Long> = emptySet()): Long {
        val excluidos = if (excepto.isEmpty()) "" else " AND id NOT IN (${excepto.joinToString(",")})"
        return leerLong(
            "SELECT id FROM exercise WHERE id NOT IN " +
                "(SELECT exercise_id FROM session_exercise WHERE session_id = $sessionId " +
                "AND exercise_id IS NOT NULL)$excluidos ORDER BY id LIMIT 1",
        )!!
    }

    private fun crearEjercicio(
        nombre: String,
        isIsometric: Int = 0,
        isToTechnicalFailure: Int = 0,
    ): Long {
        val wdb = db.openHelper.writableDatabase
        wdb.execSQL(
            "INSERT INTO exercise (name, media_resource, is_bodyweight, is_isometric, " +
                "is_to_technical_failure, progression_difficulty, is_custom) " +
                "VALUES ('$nombre', 'x', 0, $isIsometric, $isToTechnicalFailure, 'MEDIUM', 1)",
        )
        val id = leerLong("SELECT id FROM exercise WHERE name = '$nombre'")!!
        wdb.execSQL(
            "INSERT INTO exercise_muscle_zone (exercise_id, muscle_zone_id, is_primary) " +
                "SELECT $id, id, 1 FROM muscle_zone LIMIT 1",
        )
        wdb.execSQL(
            "INSERT INTO exercise_equipment (exercise_id, equipment_type_id) " +
                "VALUES ($id, ${EquipmentCatalog.MANCUERNA})",
        )
        return id
    }

    private fun asignarAlPlan(exerciseId: Long, slot: Int, sets: Int, reps: String) {
        val equipamiento = leerLong(
            "SELECT equipment_type_id FROM exercise_equipment WHERE exercise_id = $exerciseId LIMIT 1",
        )!!
        db.openHelper.writableDatabase.execSQL(
            "INSERT INTO plan_assignment (routine_version_id, exercise_id, sets, reps, " +
                "sort_order, slot, suggested_equipment_type_id) " +
                "VALUES ($routineVersionId, $exerciseId, $sets, '$reps', 99, $slot, $equipamiento)",
        )
    }

    private suspend fun registrarSerie(sessionExerciseId: Long) {
        val equipamiento = leerLong(
            "SELECT ee.equipment_type_id FROM exercise_equipment ee " +
                "INNER JOIN session_exercise se ON se.exercise_id = ee.exercise_id " +
                "WHERE se.id = $sessionExerciseId LIMIT 1",
        )!!
        repository.registerSet(
            sessionExerciseId = sessionExerciseId,
            weightKg = 20.0,
            reps = 8,
            rir = 2,
            captureUnit = WeightUnit.KG,
            equipmentTypeId = equipamiento,
        )
    }

    /** El plan entero, fila a fila, para compararlo consigo mismo tras el ajuste. */
    private fun volcarPlan(): List<String> {
        val filas = mutableListOf<String>()
        db.query(
            "SELECT routine_version_id, exercise_id, sets, reps, sort_order, slot, " +
                "suggested_equipment_type_id FROM plan_assignment " +
                "ORDER BY routine_version_id, slot, sort_order, exercise_id",
            null,
        ).use { c ->
            while (c.moveToNext()) {
                filas += (0 until c.columnCount).joinToString("|") { c.getString(it) ?: "null" }
            }
        }
        return filas
    }

    private fun contar(sql: String): Int = db.query(sql, null).use {
        if (it.moveToFirst()) it.getInt(0) else 0
    }

    private fun leerLong(sql: String): Long? = db.query(sql, null).use {
        if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else null
    }

    private fun leerTexto(sql: String): String? = db.query(sql, null).use {
        if (it.moveToFirst() && !it.isNull(0)) it.getString(0) else null
    }
}
