package com.jhw.batteryemoji.data.datasource

import android.content.Context
import com.jhw.batteryemoji.JHBatteryApplication
import java.io.File
import java.io.IOException

/**
 * Keeps the last successfully parsed manifest JSON on disk so the next launch can show remote
 * content before (or without) any network request.
 *
 * Stored in filesDir: it is tiny, and losing it to cache eviction would make the app look empty
 * while offline.
 */
class ManifestLocalDataSource(
    context: Context = JHBatteryApplication.instance
) {

    private val file = File(File(context.filesDir, DIR_NAME), FILE_NAME)

    /** Raw manifest JSON saved by [write], or null when nothing has been saved yet. */
    fun read(): String? = try {
        if (file.isFile) file.readText(Charsets.UTF_8).takeIf { it.isNotBlank() } else null
    } catch (_: IOException) {
        null
    }

    /** Saves [json] atomically: a crash mid-write leaves the previous copy intact. */
    fun write(json: String) {
        val tmp = File(file.parentFile, "$FILE_NAME.tmp")
        try {
            file.parentFile?.mkdirs()
            tmp.writeText(json, Charsets.UTF_8)
            if (!tmp.renameTo(file)) {
                file.delete()
                if (!tmp.renameTo(file)) tmp.delete()
            }
        } catch (_: IOException) {
            // Low storage or I/O error: keep the previous copy, the in-memory manifest still works
            tmp.delete()
        }
    }

    companion object {
        private const val DIR_NAME = "remote_manifest"
        private const val FILE_NAME = "manifest.json"
    }
}
