package com.estebancoloradogonzalez.tension

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.estebancoloradogonzalez.tension.data.local.database.Migrations
import com.estebancoloradogonzalez.tension.data.local.database.TensionDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migración de la versión 16 a la 19 del esquema.
 *
 * Existe por un fallo concreto: el esquema llegó a la versión 19 con `DatabaseModule`
 * registrando migraciones solo hasta la `15→16`. Faltaban las tres siguientes, así que
 * cualquier instalación existente quedaba **incapaz de abrir su propia base de datos** —
 * `IllegalStateException: A migration from 18 to 19 was required but not found`. Solo se
 * detectaba instalando sobre una versión anterior, que es justo lo que no ocurre al desarrollar
 * con desinstalación previa.
 *
 * A diferencia de las pruebas de migración anteriores, esta usa [MigrationTestHelper]: crea la
 * base en la versión 16 a partir del esquema exportado y ejecuta las migraciones de verdad.
 * Construir una base en memoria en la versión actual, como hacen `MigrationV6ToV7Test` y
 * `MigrationV7ToV8Test`, no ejercita ninguna migración: comprueba el sembrado.
 */
@RunWith(AndroidJUnit4::class)
class MigrationV16ToV19Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TensionDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    /**
     * El caso que reproducía el fallo: abrir en la versión actual una base creada en la 16.
     *
     * `validateDroppedTables = true` hace que Room compare el esquema resultante contra el
     * exportado para la versión 19. Si una migración creara una tabla con una columna, un
     * índice o una clave foránea distinta de la declarada, la validación falla aquí en lugar de
     * en el dispositivo del ejecutante.
     */
    @Test
    fun migra_de_16_a_19_y_valida_el_esquema() {
        helper.createDatabase(TEST_DB, 16).close()

        val db = helper.runMigrationsAndValidate(TEST_DB, 19, true, *Migrations.ALL)

        assertTrue("Falta week_day", existeTabla(db, "week_day"))
        assertTrue("Falta daily_routine_override", existeTabla(db, "daily_routine_override"))
        assertTrue("Falta day_skip", existeTabla(db, "day_skip"))
        assertTrue("Falta tree_state", existeTabla(db, "tree_state"))
        db.close()
    }

    /**
     * `week_day` no puede quedar vacía: la determinación de la sesión del día resuelve «hoy»
     * leyendo su fila, y sin filas el ejecutante se queda sin rutina todos los días.
     *
     * La base creada por [MigrationTestHelper] no lleva el sembrado —el esquema exportado
     * describe tablas, no datos—, así que las rutinas se insertan aquí para poder comprobar
     * que la migración las empareja por `sort_order`.
     */
    @Test
    fun la_migracion_16_17_puebla_los_siete_dias() {
        val v16 = helper.createDatabase(TEST_DB, 16)
        // sort_order deliberadamente desordenado respecto al id: si la migracion emparejara
        // por id, el lunes acabaria en la rutina equivocada y esta prueba lo detectaria.
        listOf(
            10L to 1,
            20L to 2,
            30L to 3,
            40L to 4,
            50L to 5,
            60L to 6,
        ).forEach { (id, orden) ->
            v16.execSQL(
                "INSERT INTO routine (id, name, sort_order, created_at) VALUES (?, ?, ?, ?)",
                arrayOf(id, "Rutina $orden", orden, "2025-01-01"),
            )
        }
        v16.close()

        val db = helper.runMigrationsAndValidate(TEST_DB, 19, true, *Migrations.ALL)

        val dias = mutableMapOf<Int, Pair<String, Long?>>()
        db.query("SELECT id, code, routine_id FROM week_day ORDER BY id").use { cursor ->
            while (cursor.moveToNext()) {
                dias[cursor.getInt(0)] = cursor.getString(1) to
                    if (cursor.isNull(2)) null else cursor.getLong(2)
            }
        }

        assertEquals("Deben existir los 7 dias", 7, dias.size)
        assertEquals("MONDAY" to 10L, dias[1])
        assertEquals("TUESDAY" to 20L, dias[2])
        assertEquals("WEDNESDAY" to 30L, dias[3])
        assertEquals("THURSDAY" to 40L, dias[4])
        assertEquals("FRIDAY" to 50L, dias[5])
        assertEquals("SATURDAY" to 60L, dias[6])
        assertEquals("SUNDAY", dias[7]?.first)
        assertNull("El domingo no lleva rutina", dias[7]?.second)
        db.close()
    }

    /**
     * Con menos de seis rutinas los días sobrantes quedan sin asignar.
     *
     * Es el estado que ya produce `ON DELETE SET NULL` al borrar una rutina, así que un día sin
     * rutina es válido y no un error. Fallar la migración por esto dejaría sin actualizar a
     * quien hubiera reducido su plan.
     */
    @Test
    fun la_migracion_16_17_tolera_un_plan_incompleto() {
        val v16 = helper.createDatabase(TEST_DB, 16)
        v16.execSQL(
            "INSERT INTO routine (id, name, sort_order, created_at) VALUES (1, 'Unica', 1, '2025-01-01')",
        )
        v16.close()

        val db = helper.runMigrationsAndValidate(TEST_DB, 19, true, *Migrations.ALL)

        val sinRutina = contar(db, "SELECT COUNT(*) FROM week_day WHERE routine_id IS NULL")
        assertEquals("Deben existir los 7 dias", 7, contar(db, "SELECT COUNT(*) FROM week_day"))
        assertEquals("Solo el lunes queda asignado", 6, sinRutina)
        db.close()
    }

    /**
     * `tree_state` nace vacía a propósito: el árbol es derivable y el arranque lo recalcula.
     *
     * Rellenarla en la migración habría dado un valor rancio, porque la salud depende de la
     * fecha de hoy y la de la migración no es la del próximo arranque.
     */
    @Test
    fun la_migracion_18_19_deja_el_arbol_sin_fila() {
        helper.createDatabase(TEST_DB, 16).close()

        val db = helper.runMigrationsAndValidate(TEST_DB, 19, true, *Migrations.ALL)

        assertEquals(0, contar(db, "SELECT COUNT(*) FROM tree_state"))
        assertEquals(0, contar(db, "SELECT COUNT(*) FROM day_skip"))
        assertEquals(0, contar(db, "SELECT COUNT(*) FROM daily_routine_override"))
        db.close()
    }

    /** El historial que ya existía no se pierde por el camino. */
    @Test
    fun la_migracion_conserva_el_historial() {
        val v16 = helper.createDatabase(TEST_DB, 16)
        v16.execSQL(
            "INSERT INTO routine (id, name, sort_order, created_at) VALUES (1, 'Push', 1, '2025-01-01')",
        )
        v16.execSQL(
            "INSERT INTO routine_version (id, routine_id, version_number) VALUES (1, 1, 1)",
        )
        v16.execSQL(
            "INSERT INTO session (id, routine_version_id, date, status) " +
                "VALUES (1, 1, '2026-01-15', 'COMPLETED')",
        )
        v16.close()

        val db = helper.runMigrationsAndValidate(TEST_DB, 19, true, *Migrations.ALL)

        db.query("SELECT date, status FROM session WHERE id = 1").use { cursor ->
            assertTrue("La sesion se perdio en la migracion", cursor.moveToNext())
            assertEquals("2026-01-15", cursor.getString(0))
            assertEquals("COMPLETED", cursor.getString(1))
        }
        db.close()
    }

    /**
     * Ninguna migración registrada falta ni sobra respecto al recorrido 6 → 19.
     *
     * Es la comprobación que habría detectado el fallo original sin necesidad de un
     * dispositivo: la cadena tiene que ser continua y terminar en la versión del esquema.
     */
    @Test
    fun la_cadena_de_migraciones_no_tiene_huecos() {
        val saltos = Migrations.ALL
            .map { it.startVersion to it.endVersion }
            .sortedBy { it.first }

        saltos.zipWithNext().forEach { (previo, siguiente) ->
            assertEquals(
                "Hueco entre la version ${previo.second} y la ${siguiente.first}",
                previo.second,
                siguiente.first,
            )
        }
        assertEquals(
            "La ultima migracion debe llegar a la version del esquema",
            VERSION_ESQUEMA,
            saltos.last().second,
        )
        assertNotNull(saltos.firstOrNull())
    }

    private fun existeTabla(db: SupportSQLiteDatabase, tabla: String): Boolean =
        contar(
            db,
            "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = '$tabla'",
        ) > 0

    private fun contar(db: SupportSQLiteDatabase, sql: String): Int =
        db.query(sql).use { if (it.moveToNext()) it.getInt(0) else 0 }

    private companion object {
        const val TEST_DB = "migration-test-16-19"

        /** Debe seguir a `@Database(version = ...)` de `TensionDatabase`. */
        const val VERSION_ESQUEMA = 19
    }
}
