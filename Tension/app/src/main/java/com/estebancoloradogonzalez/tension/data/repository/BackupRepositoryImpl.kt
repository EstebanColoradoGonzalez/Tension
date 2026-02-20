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
        const val SCHEMA_VERSION = 7
        const val APP_VERSION = "1.0"

        // INSERT order: parents first (FK dependencies satisfied)
        val TABLE_ORDER_INSERT = listOf(
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

        if (schemaVersion != SCHEMA_VERSION) {
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
        val dataJson = JSONObject(json).getJSONObject("data")
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

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
