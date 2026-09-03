package com.estebancoloradogonzalez.tension

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.estebancoloradogonzalez.tension.data.local.database.TensionDatabase
import com.estebancoloradogonzalez.tension.data.local.seed.PrepopulateCallback
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integridad del catálogo que `PrepopulateFacade` siembra en una instalación limpia.
 *
 * Sustituye a `MigrationV6ToV7Test` y `MigrationV7ToV8Test`, que llevaban rotas desde que la
 * migración `8→9` renombró el vocabulario del dominio: consultaban `module_code`,
 * `module_version_id` y la tabla `module_version`, que dejaron de existir, y fallaban con
 * `no such column`. Su nombre además prometía algo que no hacían — construían una base en
 * memoria **en la versión actual**, donde `addMigrations` no llega a ejecutarse nunca, así que
 * jamás ejercitaron una migración. Lo que sí comprobaban, y aquí se conserva, es que el
 * sembrado deja un catálogo coherente. Las migraciones de verdad las cubre
 * [MigrationV16ToV19Test].
 *
 * Las afirmaciones son **invariantes, no cifras fijadas a mano**. Las antiguas clavaban
 * conteos concretos —14 ejercicios en el módulo A, 93 asignaciones— y el catálogo cambió sin
 * que nadie tocara la prueba: aserciones así envejecen mal y acaban desactivadas o ignoradas.
 * Lo que no puede cambiar sin que algo esté roto es que el plan esté completo y sin referencias
 * colgando.
 */
@RunWith(AndroidJUnit4::class)
class CatalogoSembradoTest {

    private lateinit var db: TensionDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, TensionDatabase::class.java)
            .addCallback(PrepopulateCallback())
            .build()
        // Fuerza la creación y con ella el sembrado.
        db.openHelper.writableDatabase
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun cada_rutina_tiene_version_inicial_y_version_vigente() {
        val rutinas = contar("SELECT COUNT(*) FROM routine")
        assertTrue("El plan por defecto no puede estar vacío", rutinas > 0)
        assertEquals(
            "Cada rutina necesita su versión inicial",
            rutinas,
            contar("SELECT COUNT(*) FROM routine_version"),
        )
        assertEquals(
            "Cada rutina necesita una versión marcada como vigente",
            rutinas,
            contar("SELECT COUNT(*) FROM routine_current_version"),
        )
    }

    @Test
    fun ninguna_version_de_rutina_queda_sin_ejercicios() {
        val vacias = contar(
            """
            SELECT COUNT(*) FROM routine_version rv
            WHERE NOT EXISTS (
                SELECT 1 FROM plan_assignment pa WHERE pa.routine_version_id = rv.id
            )
            """.trimIndent(),
        )
        assertEquals("Hay versiones de rutina sin un solo ejercicio asignado", 0, vacias)
    }

    /**
     * El orden de los ejercicios dentro de una rutina es lo que la pantalla de sesión recorre,
     * así que un hueco o un duplicado en `sort_order` se ve directamente como un ejercicio que
     * se salta o se repite.
     */
    @Test
    fun el_orden_dentro_de_cada_rutina_es_una_secuencia_desde_uno() {
        val desordenadas = contar(
            """
            SELECT COUNT(*) FROM (
                SELECT routine_version_id
                FROM plan_assignment
                GROUP BY routine_version_id
                HAVING MIN(sort_order) != 1
                    OR MAX(sort_order) != COUNT(*)
                    OR COUNT(DISTINCT sort_order) != COUNT(*)
            )
            """.trimIndent(),
        )
        assertEquals("Hay rutinas con el orden de ejercicios roto", 0, desordenadas)
    }

    @Test
    fun ninguna_asignacion_apunta_a_algo_inexistente() {
        assertEquals(
            "Asignaciones que apuntan a un ejercicio que no existe",
            0,
            contar(
                "SELECT COUNT(*) FROM plan_assignment pa " +
                    "LEFT JOIN exercise e ON e.id = pa.exercise_id WHERE e.id IS NULL",
            ),
        )
        assertEquals(
            "Asignaciones que apuntan a una versión de rutina que no existe",
            0,
            contar(
                "SELECT COUNT(*) FROM plan_assignment pa " +
                    "LEFT JOIN routine_version rv ON rv.id = pa.routine_version_id " +
                    "WHERE rv.id IS NULL",
            ),
        )
    }

    /**
     * Sin zona muscular un ejercicio no entra en la distribución de volumen ni en el análisis
     * de fatiga por grupo: existe en el catálogo pero es invisible para las métricas.
     */
    @Test
    fun todo_ejercicio_tiene_al_menos_una_zona_muscular() {
        val huerfanos = contar(
            "SELECT COUNT(*) FROM exercise e " +
                "LEFT JOIN exercise_muscle_zone z ON z.exercise_id = e.id " +
                "WHERE z.exercise_id IS NULL",
        )
        assertEquals("Hay ejercicios sin ninguna zona muscular asociada", 0, huerfanos)
    }

    /**
     * Los 7 días existen siempre y el domingo queda sin rutina, que es el modo en que el
     * descanso se vuelve un concepto visible en lugar de una fila ausente (HU-36).
     */
    @Test
    fun la_semana_esta_completa_con_el_domingo_en_descanso() {
        assertEquals("Deben existir los 7 días", 7, contar("SELECT COUNT(*) FROM week_day"))

        val domingo = db.openHelper.readableDatabase.query(
            "SELECT code, routine_id FROM week_day WHERE id = 7",
        ).use { cursor ->
            assertTrue("Falta el domingo", cursor.moveToNext())
            cursor.getString(0) to if (cursor.isNull(1)) null else cursor.getLong(1)
        }
        assertEquals("SUNDAY", domingo.first)
        assertNull("El domingo no lleva rutina asignada", domingo.second)

        assertEquals(
            "Los días de lunes a sábado deben tener rutina",
            0,
            contar("SELECT COUNT(*) FROM week_day WHERE id < 7 AND routine_id IS NULL"),
        )
    }

    /**
     * Las tablas de historial nacen vacías pero accesibles. Una instalación limpia que no
     * pudiera consultarlas reventaría en la primera pantalla que las lee.
     */
    @Test
    fun las_tablas_de_historial_existen_y_se_pueden_consultar() {
        listOf(
            "session",
            "session_exercise",
            "exercise_set",
            "exercise_progression",
            "alert",
            "rotation_state",
            "deload",
            "day_skip",
            "daily_routine_override",
            "tree_state",
        ).forEach { tabla ->
            db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $tabla").use { cursor ->
                assertTrue("La tabla $tabla no es consultable", cursor.moveToNext())
            }
        }
    }

    private fun contar(sql: String): Int = contar(db.openHelper.readableDatabase, sql)

    private fun contar(db: SupportSQLiteDatabase, sql: String): Int =
        db.query(sql).use { if (it.moveToNext()) it.getInt(0) else -1 }
}
