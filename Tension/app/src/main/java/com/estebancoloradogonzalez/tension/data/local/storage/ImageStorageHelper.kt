package com.estebancoloradogonzalez.tension.data.local.storage

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageStorageHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun saveImageToInternal(uri: Uri): String? {
        return try {
            val fileName = "exercise_${UUID.randomUUID()}.jpg"
            val dir = File(context.filesDir, EXERCISE_IMAGES_DIR)
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, fileName)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                file.outputStream().use { output ->
                    inputStream.copyTo(output)
                }
            } ?: return null
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun deleteImageIfInternal(mediaResource: String?) {
        if (mediaResource == null) return
        try {
            val file = File(mediaResource)
            if (file.exists() && file.absolutePath.contains(EXERCISE_IMAGES_DIR)) {
                file.delete()
            }
        } catch (_: Exception) {
            // Silently ignore — old file cleanup is best-effort
        }
    }

    private companion object {
        const val EXERCISE_IMAGES_DIR = "exercise_images"
    }
}
