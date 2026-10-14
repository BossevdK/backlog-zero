package com.backlogzero.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Omslagafbeeldingen staan in de privé-opslag van de app. In de database staat alleen de
 * bestandsnaam. Een latere "cover automatisch ophalen"-functie kan via [import] werken.
 */
class CoverStorage(private val context: Context) {

    private val dir: File get() = File(context.filesDir, DIR_NAME).apply { mkdirs() }

    fun file(name: String): File = file(context, name)

    suspend fun import(uri: Uri): String = withContext(Dispatchers.IO) {
        save { context.contentResolver.openInputStream(uri) }
    }

    /** Haalt een omslag van een online zoekresultaat op. Alleen https van bekende afbeeldingsservers. */
    suspend fun importUrl(url: String): String = withContext(Dispatchers.IO) {
        val host = Uri.parse(url).host.orEmpty()
        if (!url.startsWith("https://") || host !in ALLOWED_HOSTS) throw IOException("Onverwachte omslagbron")
        save {
            (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 20_000
                setRequestProperty("User-Agent", "BacklogZero")
            }.let { conn -> if (conn.responseCode == 200) conn.inputStream else null }
        }
    }

    /** `open` wordt twee keer aangeroepen: eerst om de afmetingen te lezen, daarna om te decoderen. */
    private fun save(open: () -> InputStream?): String {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open()?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE) sample *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = open()?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IllegalStateException("De afbeelding kon niet worden gelezen.")

        val name = "${UUID.randomUUID()}.jpg"
        try {
            FileOutputStream(File(dir, name)).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        } finally {
            bitmap.recycle()
        }
        return name
    }

    fun delete(name: String?) {
        if (name == null) return
        runCatching { File(dir, name).delete() }
    }

    companion object {
        private const val DIR_NAME = "covers"
        private const val MAX_SIDE = 1600
        private val ALLOWED_HOSTS = setOf("covers.openlibrary.org", "image.tmdb.org", "media.rawg.io")

        fun file(context: Context, name: String): File = File(File(context.filesDir, DIR_NAME), name)
    }
}
