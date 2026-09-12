package com.estebancoloradogonzalez.tension.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Base64
import androidx.sqlite.db.SupportSQLiteDatabase
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.data.local.database.TensionDatabase
import com.estebancoloradogonzalez.tension.domain.model.BackupMetadata
import com.estebancoloradogonzalez.tension.domain.model.BackupValidationResult
import com.estebancoloradogonzalez.tension.domain.repository.BackupRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val database: TensionDatabase,
    @ApplicationContext private val context: Context,
) : BackupRepository {

    companion object {
        const val SCHEMA_VERSION = 17

        const val APP_VERSION = "1.0"

        /**
         * **Solo el formato vigente.** Los anteriores se rechazan por completo (CA-39.12).
         *
         * El argumento que salvo al formato sin `tree_state` no aplica aqui: el arbol era
         * enteramente derivable del historial, asi que restaurar sin el nunca dejaba un
         * estado invalido. El **equipamiento de la serie no se deriva de nada** — un
         * respaldo anterior no dice con que implemento se hizo cada serie, y
         * `exercise_set.equipment_type_id` es `NOT NULL`. Aceptarlo exigiria inventar un
         * valor por serie, que es exactamente la importacion parcial que la historia
         * prohibe. Con ello se retiraron tambien los caminos de v8 y v11: codigo de
         * importacion inalcanzable es una promesa que la aplicacion ya no cumple.
         *
         * El formato 13 se rechaza por la misma razon (CA-40.09): su `exercise_progression`
         * no lleva `equipment_type_id`, que ahora es `NOT NULL` y encabeza la clave
         * primaria junto a `exercise_id`. Aceptarlo obligaria a inventar un implemento por
         * fila, y la CA exige que la restauracion reproduzca el estado del motor par por
         * par **sin recalcularlo**.
         *
         * El 14 cae por lo mismo (CA-41.10). Le faltan tres columnas `NOT NULL` que HU-41
         * anadio: `exercise_muscle_zone.is_primary`, `plan_assignment.suggested_equipment_type_id`
         * y `muscle_zone.sort_order`. Las dos primeras no se derivan de nada —la jerarquia
         * y la sugerencia son decisiones, no calculos— y la CA exige que la restauracion
         * reproduzca el catalogo y el plan **sin recalcularlos**. Ninguna tabla nueva entra
         * en el orden: el mecanismo vuelca columnas por cursor y las tres viajan solas.
         *
         * El 15 cae por el mismo argumento, y esta vez sobre una tabla entera (CA-42.08):
         * no lleva `exercise_one_rm`. El 1RM es un **record acumulado**, no un derivado
         * reconstruible: recalcularlo desde las series podria perder un maximo alcanzado en
         * una serie ya purgada o restaurada parcialmente, y la CA exige que el respaldo lo
         * reproduzca **sin recalcularlo**. Aceptar un v15 dejaria la vista de 1RM vacia
         * sobre un historial lleno de series que si calificaron.
         *
         * El 16 cae por partida doble (HU-43). No lleva `session_withdrawal`, y `validateBackup`
         * recorre [TABLE_ORDER_INSERT] rechazando por incompleto cualquier respaldo al que le
         * falte una tabla de la lista: en cuanto la tabla entra en el orden, ningun v16 pasa.
         * Y el motivo de fondo se sostiene solo: **el ajuste de la sesion no se deriva de
         * nada**. Un v16 no dice que ejercicio se anadio ni cual se retiro, asi que el
         * historial restaurado presentaria sesiones cuya composicion no coincide con la de su
         * plan sin poder explicar por que.
         */
        private val ACCEPTED_SCHEMA_VERSIONS = setOf(SCHEMA_VERSION)

        /** El formato inmediatamente anterior, el unico con un motivo de rechazo propio. */
        private const val PREVIOUS_SCHEMA_VERSION = SCHEMA_VERSION - 1

        // INSERT order: parents first (FK dependencies satisfied)
        val TABLE_ORDER_INSERT = listOf(
            "profile",
            "rotation_state",
            // tree_state no lleva FK: su lugar en el orden es indiferente para la
            // integridad, y va junto a rotation_state por ser la otra tabla de fila unica.
            "tree_state",
            "weight_record",
            "routine",
            // week_day y daily_routine_override llevan FK a routine y deben insertarse
            // despues de ella. Omitirlas perderia la relacion dia -> rutina en cada
            // restauracion: el borrado de routine dispara su ON DELETE SET NULL / CASCADE
            // y nada la repondria.
            "week_day",
            "daily_routine_override",
            "day_skip",
            "muscle_zone",
            "equipment_type",
            "deload",
            "exercise",
            "exercise_muscle_zone",
            // Los implementos admitidos del ejercicio. Lleva FK a exercise y a
            // equipment_type, asi que va detras de las dos, junto a su gemela.
            "exercise_equipment",
            // El 1RM estimado por par. Lleva FK a exercise y a equipment_type, como
            // exercise_equipment, y va junto a ella por la misma razon.
            "exercise_one_rm",
            "routine_version",
            "routine_current_version",
            "deload_frozen_version",
            "plan_assignment",
            "session",
            "session_exercise",
            // El retiro temporal de un ejercicio del plan. Lleva FK a session y a exercise,
            // asi que va detras de las dos (HU-43).
            "session_withdrawal",
            "exercise_set",
            // La clasificacion por implemento de cada sesion. Lleva FK a session_exercise y
            // a equipment_type, asi que va detras de las dos.
            "session_exercise_progression",
            "exercise_progression",
            "alert",
        )

        // DELETE order: children first (reverse of insert)
        val TABLE_ORDER_DELETE = TABLE_ORDER_INSERT.reversed()
    }

    override suspend fun exportToJson(): String {
        val db = database.openHelper.readableDatabase
        val root = JSONObject()
        val data = JSONObject()
        var totalRecordCount = 0

        for (table in TABLE_ORDER_INSERT) {
            val cursor = db.query("SELECT * FROM $table")
            val rows = JSONArray()
            cursor.use {
                while (it.moveToNext()) {
                    val row = JSONObject()
                    for (i in 0 until it.columnCount) {
                        val name = it.getColumnName(i)
                        when (it.getType(i)) {
                            Cursor.FIELD_TYPE_NULL -> row.put(name, JSONObject.NULL)
                            Cursor.FIELD_TYPE_INTEGER -> row.put(name, it.getLong(i))
                            Cursor.FIELD_TYPE_FLOAT -> row.put(name, it.getDouble(i))
                            Cursor.FIELD_TYPE_STRING -> row.put(name, it.getString(i))
                            Cursor.FIELD_TYPE_BLOB -> row.put(
                                name,
                                Base64.encodeToString(it.getBlob(i), Base64.NO_WRAP),
                            )
                        }
                    }
                    rows.put(row)
                }
            }
            data.put(table, rows)
            totalRecordCount += rows.length()
        }

        val metadata = JSONObject().apply {
            put("appVersion", APP_VERSION)
            put("schemaVersion", SCHEMA_VERSION)
            put(
                "exportDate",
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            )
            put("recordCount", totalRecordCount)
        }

        root.put("metadata", metadata)
        root.put("data", data)

        return root.toString(2)
    }

    override fun validateBackup(json: String): BackupValidationResult {
        val parsed: JSONObject
        try {
            parsed = JSONObject(json)
        } catch (_: JSONException) {
            return BackupValidationResult(
                isValid = false,
                metadata = null,
                sessionCount = 0,
                errorMessage = context.getString(R.string.import_backup_invalid_json),
            )
        }

        if (!parsed.has("metadata")) {
            return BackupValidationResult(
                isValid = false,
                metadata = null,
                sessionCount = 0,
                errorMessage = context.getString(R.string.import_backup_invalid_format),
            )
        }

        val metaJson = parsed.getJSONObject("metadata")
        val schemaVersion = try {
            metaJson.getInt("schemaVersion")
        } catch (_: JSONException) {
            return BackupValidationResult(
                isValid = false,
                metadata = null,
                sessionCount = 0,
                errorMessage = context.getString(R.string.import_backup_invalid_format),
            )
        }

        if (schemaVersion !in ACCEPTED_SCHEMA_VERSIONS) {
            // Un formato anterior se rechaza nombrando la causa y no los numeros de
            // version: lo que le falta al archivo es el equipamiento de las series
            // (CA-39.12). Un formato *posterior* —un respaldo de un build mas nuevo— no
            // tiene esa causa y se rechaza por version, que es lo unico que se sabe de el.
            val message = when {
                schemaVersion > SCHEMA_VERSION -> context.getString(
                    R.string.import_backup_incompatible_version,
                    SCHEMA_VERSION,
                    schemaVersion,
                )
                // El formato inmediatamente anterior tiene una causa propia y concreta: no
                // registra el ajuste de la sesion, que no se deduce del plan. Los mas
                // antiguos siguen cayendo por la ausencia del equipamiento de las series,
                // que es lo primero que les falta.
                schemaVersion == PREVIOUS_SCHEMA_VERSION ->
                    context.getString(R.string.import_backup_no_session_adjustment)
                else -> context.getString(R.string.import_backup_no_equipment)
            }
            return BackupValidationResult(
                isValid = false,
                metadata = null,
                sessionCount = 0,
                errorMessage = message,
            )
        }

        if (!parsed.has("data")) {
            return BackupValidationResult(
                isValid = false,
                metadata = null,
                sessionCount = 0,
                errorMessage = context.getString(R.string.import_backup_invalid_format),
            )
        }

        val dataJson = parsed.getJSONObject("data")
        for (table in TABLE_ORDER_INSERT) {
            if (!dataJson.has(table)) {
                return BackupValidationResult(
                    isValid = false,
                    metadata = null,
                    sessionCount = 0,
                    errorMessage = context.getString(R.string.import_backup_incomplete),
                )
            }
        }

        val metadata = BackupMetadata(
            appVersion = metaJson.optString("appVersion", ""),
            schemaVersion = schemaVersion,
            exportDate = metaJson.optString("exportDate", ""),
            recordCount = metaJson.optInt("recordCount", 0),
        )

        val sessionCount = dataJson.getJSONArray("session").length()

        return BackupValidationResult(
            isValid = true,
            metadata = metadata,
            sessionCount = sessionCount,
            errorMessage = null,
        )
    }

    override suspend fun importFromJson(json: String) {
        val parsed = JSONObject(json)
        val dataJson = parsed.getJSONObject("data")
        val db = database.openHelper.writableDatabase

        db.beginTransaction()
        try {
            for (table in TABLE_ORDER_DELETE) {
                db.execSQL("DELETE FROM $table")
            }

            for (table in TABLE_ORDER_INSERT) {
                // `optJSONArray` y no `getJSONArray`: una tabla ausente no debe abortar la
                // restauracion entera. La validacion previa ya exige que esten todas.
                val rows = dataJson.optJSONArray(table) ?: JSONArray()
                // A backup file carries the columns of the schema that produced it. Keys
                // that no longer exist are dropped instead of handed to insert(), which
                // would fail the whole restore with "no column named ...". The only such
                // key today is session_exercise.original_exercise_id, retired in v16
                // (HU-34), and its value was always NULL.
                val columns = columnsOf(db, table)
                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    val cv = ContentValues()
                    val keys = row.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        if (columns.isNotEmpty() && key !in columns) {
                            continue
                        }
                        if (row.isNull(key)) {
                            cv.putNull(key)
                        } else {
                            when (val value = row.get(key)) {
                                is Long -> cv.put(key, value)
                                is Int -> cv.put(key, value.toLong())
                                is Double -> cv.put(key, value)
                                is String -> cv.put(key, value)
                                is Boolean -> cv.put(key, if (value) 1L else 0L)
                                else -> cv.put(key, value.toString())
                            }
                        }
                    }
                    db.insert(table, SQLiteDatabase.CONFLICT_REPLACE, cv)
                }
            }

            db.execSQL(
                """
                UPDATE rotation_state SET microcycle_position = (SELECT COUNT(*) FROM routine)
                WHERE microcycle_position > (SELECT COUNT(*) FROM routine)
                AND (SELECT COUNT(*) FROM routine) > 0
                """.trimIndent(),
            )
            db.execSQL(
                """
                UPDATE rotation_state SET microcycle_position = 1
                WHERE microcycle_position < 1
                AND (SELECT COUNT(*) FROM routine) > 0
                """.trimIndent(),
            )

            // Fix slot values for plan_assignment rows imported from backups
            // that predate the slot column (all rows default to slot=0).
            db.execSQL("UPDATE plan_assignment SET slot = sort_order WHERE slot = 0")

            // Fix session_exercise.slot for sessions created when plan_assignment had slot=0.
            db.execSQL(
                """
                UPDATE session_exercise
                SET slot = COALESCE(
                    (SELECT pa.slot
                     FROM plan_assignment pa
                     INNER JOIN session s ON s.id = session_exercise.session_id
                     WHERE pa.routine_version_id = s.routine_version_id
                       AND pa.exercise_id = session_exercise.exercise_id
                     LIMIT 1),
                    slot
                )
                WHERE slot = 0
                """.trimIndent(),
            )

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Column names the table actually has in the current schema. Used to discard keys
     * carried by backups exported from an older schema. An empty result means the shape
     * could not be read, and the caller then imports every key rather than dropping all
     * of them.
     */
    private fun columnsOf(db: SupportSQLiteDatabase, table: String): Set<String> {
        val columns = mutableSetOf<String>()
        db.query("PRAGMA table_info($table)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            if (nameIndex < 0) return emptySet()
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(nameIndex))
            }
        }
        return columns
    }
}
