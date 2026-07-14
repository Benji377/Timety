package io.github.benji377.timety.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException


/**
 * Copies a picked profile image into app-internal storage so it stays readable across app
 * restarts. Photo-picker `content://` URIs only carry a session-scoped read grant, so persisting
 * the URI itself would leave the avatar unloadable after the process is recreated.
 */
object ProfileImageStore {

    private const val DIRECTORY = "profile"

    /**
     * Copies the image at [uri] into internal storage and returns the new file's absolute path,
     * or null if the URI could not be read. Any previously stored profile image is deleted; the
     * filename is timestamped so a re-picked image never collides with Coil's cache entry for
     * the old one.
     */
    fun persist(context: Context, uri: Uri): String? {
        val dir = File(context.filesDir, DIRECTORY)
        if (!dir.isDirectory && !dir.mkdirs()) return null

        val target = File(dir, "avatar_${System.currentTimeMillis()}.img")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
        } catch (e: IOException) {
            target.delete()
            return null
        } catch (e: SecurityException) {
            target.delete()
            return null
        }

        dir.listFiles()?.forEach { file ->
            if (file != target) file.delete()
        }
        return target.absolutePath
    }

    /**
     * Returns [path] if it points to a readable image file, or null otherwise. Filters out
     * stale entries such as backup-restored paths from another device or pre-2.1.0 `content://`
     * URIs whose read grant has expired.
     */
    fun validOrNull(path: String?): String? =
        path?.takeIf { it.startsWith("/") && File(it).canRead() }
}
