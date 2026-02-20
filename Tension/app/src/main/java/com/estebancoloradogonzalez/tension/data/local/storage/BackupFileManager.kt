package com.estebancoloradogonzalez.tension.data.local.storage

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupFileManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun writeToUri(json: String, uri: Uri) {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(json.toByteArray(Charsets.UTF_8))
        } ?: throw IOException("Cannot open output stream for URI: $uri")
    }

    fun readFromUri(uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader(Charsets.UTF_8).readText()
        } ?: throw IOException("Cannot open input stream for URI: $uri")
    }

    fun writeToCacheForShare(json: String, fileName: String): File {
        val file = File(context.cacheDir, fileName)
        file.writeText(json, Charsets.UTF_8)
        return file
    }

    fun getShareableUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }

    fun extractDisplayPath(uri: Uri): String {
        val path = uri.path ?: uri.toString()
        return when {
            path.contains("Download", ignoreCase = true) ||
                path.contains("Descargas", ignoreCase = true) -> "Descargas/"
            else -> {
                val lastSegment = uri.lastPathSegment
                if (lastSegment != null && lastSegment.contains("/")) {
                    lastSegment.substringBeforeLast("/") + "/"
                } else {
                    "Almacenamiento del dispositivo"
                }
            }
        }
    }

    fun generateBackupFileName(): String {
        val date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        return "tension_backup_$date.json"
    }
}
