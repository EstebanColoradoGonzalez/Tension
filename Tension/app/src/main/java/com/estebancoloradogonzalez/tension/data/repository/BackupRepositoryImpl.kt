package com.estebancoloradogonzalez.tension.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Base64
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
        const val SCHEMA_VERSION = 9
        private const val LEGACY_SCHEMA_VERSION = 8
        const val APP_VERSION = "1.0"

        private val MODULE_CODE_TO_ROUTINE_ID = mapOf("A" to 1L, "B" to 2L, "C" to 3L)

        // INSERT order: parents first (FK dependencies satisfied)
        val TABLE_ORDER_INSERT = listOf(
            "profile",
            "rotation_state",
            "weight_record",
            "routine",
            "muscle_zone",
            "equipment_type",
            "deload",
            "exercise",
            "exercise_muscle_zone",
            "routine_version",
            "routine_current_version",
            "deload_frozen_version",
            "plan_assignment",
            "session",
            "session_exercise",
            "exercise_set",
            "exercise_progression",
            "alert",
        )

        // DELETE order: children first (reverse of insert)
        val TABLE_ORDER_DELETE = TABLE_ORDER_INSERT.reversed()

        val LEGACY_TABLE_ORDER = listOf(
            "profile",
            "rotation_state",
            "weight_record",
            "module",
            "muscle_zone",
            "equipment_type",
            "deload",
            "exercise",
            "exercise_muscle_zone",
            "module_version",
            "plan_assignment",
            "session",
            "session_exercise",
            "exercise_set",
            "exercise_progression",
            "alert",
        )
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

        if (schemaVersion != SCHEMA_VERSION && schemaVersion != LEGACY_SCHEMA_VERSION) {
            return BackupValidationResult(
                isValid = false,
                metadata = null,
                sessionCount = 0,
                errorMessage = context.getString(
                    R.string.import_backup_incompatible_version,
                    SCHEMA_VERSION,
                    schemaVersion,
                ),
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
        val requiredTables = if (schemaVersion == LEGACY_SCHEMA_VERSION) {
            LEGACY_TABLE_ORDER
        } else {
            TABLE_ORDER_INSERT
        }
        for (table in requiredTables) {
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
        val schemaVersion = parsed.getJSONObject("metadata").getInt("schemaVersion")
        val dataJson = if (schemaVersion == LEGACY_SCHEMA_VERSION) {
            transformV8ToV9(parsed.getJSONObject("data"))
        } else {
            parsed.getJSONObject("data")
        }
        val db = database.openHelper.writableDatabase

        db.beginTransaction()
        try {
            for (table in TABLE_ORDER_DELETE) {
                db.execSQL("DELETE FROM $table")
            }

            for (table in TABLE_ORDER_INSERT) {
                val rows = dataJson.getJSONArray(table)
                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    val cv = ContentValues()
                    val keys = row.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
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
                       AND pa.exercise_id = COALESCE(session_exercise.original_exercise_id, session_exercise.exercise_id)
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

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun transformV8ToV9(data: JSONObject): JSONObject {
        val result = JSONObject()

        // module → routine (v8 module table: code=String PK, name, group_description, load_increment_kg)
        val modules = data.optJSONArray("module") ?: JSONArray()
        val routines = JSONArray()
        for (i in 0 until modules.length()) {
            val m = modules.getJSONObject(i)
            val code = m.getString("code")
            val routineId = MODULE_CODE_TO_ROUTINE_ID[code] ?: continue
            val routine = JSONObject()
            routine.put("id", routineId)
            routine.put("name", m.getString("name"))
            routine.put("sort_order", routineId.toInt())
            routine.put("created_at", "2025-01-01")
            routines.put(routine)
        }
        result.put("routine", routines)

        // module_version → routine_version (v8: module_code=String FK)
        val moduleVersions = data.optJSONArray("module_version") ?: JSONArray()
        val routineVersions = JSONArray()
        for (i in 0 until moduleVersions.length()) {
            val mv = moduleVersions.getJSONObject(i)
            val rv = JSONObject()
            rv.put("id", mv.get("id"))
            val moduleCode = mv.getString("module_code")
            rv.put("routine_id", MODULE_CODE_TO_ROUTINE_ID[moduleCode] ?: continue)
            rv.put("version_number", mv.get("version_number"))
            routineVersions.put(rv)
        }
        result.put("routine_version", routineVersions)

        // rotation_state → extract routine_current_version
        val rotationStates = data.optJSONArray("rotation_state") ?: JSONArray()
        val routineCurrentVersions = JSONArray()
        val cleanedRotationStates = JSONArray()
        for (i in 0 until rotationStates.length()) {
            val rs = rotationStates.getJSONObject(i)
            for ((code, routineId) in MODULE_CODE_TO_ROUTINE_ID) {
                val colName = "current_version_module_${code.lowercase()}"
                if (rs.has(colName) && !rs.isNull(colName)) {
                    val rcv = JSONObject()
                    rcv.put("routine_id", routineId)
                    rcv.put("current_version_number", rs.get(colName))
                    routineCurrentVersions.put(rcv)
                }
            }
            val cleaned = JSONObject()
            cleaned.put("id", rs.get("id"))
            cleaned.put("microcycle_position", rs.get("microcycle_position"))
            cleaned.put("microcycle_count", rs.get("microcycle_count"))
            cleanedRotationStates.put(cleaned)
        }
        result.put("routine_current_version", routineCurrentVersions)
        result.put("rotation_state", cleanedRotationStates)

        // deload → extract deload_frozen_version
        val deloads = data.optJSONArray("deload") ?: JSONArray()
        val deloadFrozenVersions = JSONArray()
        val cleanedDeloads = JSONArray()
        for (i in 0 until deloads.length()) {
            val d = deloads.getJSONObject(i)
            val deloadId = d.get("id")
            for ((code, routineId) in MODULE_CODE_TO_ROUTINE_ID) {
                val colName = "frozen_version_module_${code.lowercase()}"
                if (d.has(colName) && !d.isNull(colName)) {
                    val dfv = JSONObject()
                    dfv.put("deload_id", deloadId)
                    dfv.put("routine_id", routineId)
                    dfv.put("frozen_version_number", d.get(colName))
                    deloadFrozenVersions.put(dfv)
                }
            }
            val cleaned = JSONObject()
            cleaned.put("id", d.get("id"))
            cleaned.put("status", d.getString("status"))
            cleaned.put("activation_date", d.getString("activation_date"))
            if (d.has("completion_date") && !d.isNull("completion_date")) {
                cleaned.put("completion_date", d.getString("completion_date"))
            } else {
                cleaned.put("completion_date", JSONObject.NULL)
            }
            cleanedDeloads.put(cleaned)
        }
        result.put("deload_frozen_version", deloadFrozenVersions)
        result.put("deload", cleanedDeloads)

        // exercise: drop module_code
        val exercises = data.optJSONArray("exercise") ?: JSONArray()
        val cleanedExercises = JSONArray()
        for (i in 0 until exercises.length()) {
            val e = exercises.getJSONObject(i)
            val cleaned = JSONObject()
            val keys = e.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key != "module_code") {
                    cleaned.put(key, e.get(key))
                }
            }
            cleanedExercises.put(cleaned)
        }
        result.put("exercise", cleanedExercises)

        // session: module_version_id → routine_version_id
        renameColumn(data, result, "session", "module_version_id", "routine_version_id")

        // plan_assignment: module_version_id → routine_version_id
        renameColumn(data, result, "plan_assignment", "module_version_id", "routine_version_id")

        // alert: module_code → routine_id
        val alerts = data.optJSONArray("alert") ?: JSONArray()
        val cleanedAlerts = JSONArray()
        for (i in 0 until alerts.length()) {
            val a = alerts.getJSONObject(i)
            val cleaned = JSONObject()
            val keys = a.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key == "module_code") {
                    if (!a.isNull(key)) {
                        val code = a.getString(key)
                        cleaned.put("routine_id", MODULE_CODE_TO_ROUTINE_ID[code] ?: JSONObject.NULL)
                    } else {
                        cleaned.put("routine_id", JSONObject.NULL)
                    }
                } else if (key == "type") {
                    val type = a.getString(key)
                    cleaned.put(
                        key,
                        if (type == "MODULE_REQUIRES_DELOAD") "ROUTINE_REQUIRES_DELOAD" else type,
                    )
                } else {
                    cleaned.put(key, a.get(key))
                }
            }
            cleanedAlerts.put(cleaned)
        }
        result.put("alert", cleanedAlerts)

        // Pass through unchanged tables
        val unchangedTables = listOf(
            "profile", "weight_record", "muscle_zone", "equipment_type",
            "exercise_muscle_zone", "session_exercise", "exercise_set",
            "exercise_progression",
        )
        for (table in unchangedTables) {
            result.put(table, data.optJSONArray(table) ?: JSONArray())
        }

        return result
    }

    private fun renameColumn(
        source: JSONObject,
        dest: JSONObject,
        table: String,
        oldCol: String,
        newCol: String,
    ) {
        val rows = source.optJSONArray(table) ?: JSONArray()
        val cleaned = JSONArray()
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            val newRow = JSONObject()
            val keys = row.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key == oldCol) {
                    newRow.put(newCol, row.get(key))
                } else {
                    newRow.put(key, row.get(key))
                }
            }
            cleaned.put(newRow)
        }
        dest.put(table, cleaned)
    }
}
