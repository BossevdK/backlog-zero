package com.backlogzero.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Omslagafbeeldingen staan in de privé-opslag van de app. In de database staat alleen de
 * bestandsnaam. Een latere "cover automatisch ophalen"-functie kan via [import] werken.
 */
class CoverStorage(private val context: Context) {

    private val dir: File get() = File(context.filesDir, DIR_NAME).apply { mkdirs() }

    fun file(name: String): File = file(context, name)

    suspend fun import(uri: Uri): String = withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE) sample *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IllegalStateException("De afbeelding kon niet worden gelezen.")

        val name = "${UUID.randomUUID()}.jpg"
        try {
            FileOutputStream(File(dir, name)).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        } finally {
            bitmap.recycle()
        }
        name
    }

    fun delete(name: String?) {
        if (name == null) return
        runCatching { File(dir, name).delete() }
    }

    companion object {
        private const val DIR_NAME = "covers"
        private const val MAX_SIDE = 1600

        fun file(context: Context, name: String): File = File(File(context.filesDir, DIR_NAME), name)
    }
}
