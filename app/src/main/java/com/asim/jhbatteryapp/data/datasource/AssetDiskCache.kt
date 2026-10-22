package com.asim.jhbatteryapp.data.datasource

import com.asim.jhbatteryapp.domain.model.RemoteAssetItem
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/**
 * Disk cache for browsed remote assets (SVG / PNG / WebP / GIF / Lottie JSON).
 *
 * - Lives in cacheDir: every file here can be downloaded again, so the system may evict it under
 *   storage pressure. Files the overlay depends on (applied battery icon, animation, sticker) are
 *   copied to filesDir by RemoteImageLoader and are never stored or deleted here.
 * - One file per asset: "<safe id>-<id hash>~<fingerprint>.<ext>". The fingerprint hashes the
 *   manifest fields that describe the file's content plus [generation] (the manifestVersion), so
 *   an edited entry or a new manifestVersion maps to a new name: the stale file is never read
 *   again and is deleted by [put] or [retainOnly].
 * - Writes go to a temp file that is renamed into place, so a crash or failed download never
 *   leaves a half-written file that looks valid.
 * - Size is capped at [maxBytes]; least recently used files are dropped first.
 */
class AssetDiskCache(
    private val dir: File,
    private val maxBytes: Long = DEFAULT_MAX_BYTES,
    private val generation: () -> Int = { 0 }
) {

    private val lock = Any()

    /** The cached file for [asset], or null when it isn't cached for the asset's current fingerprint. */
    fun get(asset: RemoteAssetItem): File? {
        val file = File(dir, fileName(asset))
        if (!file.isFile || file.length() == 0L) return null
        // LRU bookkeeping for trim(); failure only makes eviction order less accurate
        file.setLastModified(System.currentTimeMillis())
        return file
    }

    /**
     * Stores already-validated [bytes] for [asset] and deletes its older versions.
     * @return the cached file, or null when storage is low or the write failed.
     */
    fun put(asset: RemoteAssetItem, bytes: ByteArray): File? {
        if (!dir.isDirectory && !dir.mkdirs()) return null
        if (dir.usableSpace < bytes.size + MIN_FREE_BYTES) {
            // Make room from our own cache before giving up
            trim(maxBytes / 2)
            if (dir.usableSpace < bytes.size + MIN_FREE_BYTES) return null
        }
        val target = File(dir, fileName(asset))
        val tmp = File(dir, target.name + TMP_SUFFIX)
        try {
            tmp.writeBytes(bytes)
            if (!tmp.renameTo(target)) {
                target.delete()
                if (!tmp.renameTo(target)) {
                    tmp.delete()
                    return null
                }
            }
        } catch (_: IOException) {
            tmp.delete()
            return null
        }
        val prefix = idPrefix(asset)
        dir.listFiles()?.forEach {
            if (it != target && it.name.startsWith(prefix) && !it.name.endsWith(TMP_SUFFIX)) it.delete()
        }
        trim(maxBytes)
        return target
    }

    /**
     * Deletes cached files that no longer match any asset in [assets] — removed entries and
     * entries whose content fingerprint changed. Files of unchanged assets are kept.
     */
    fun retainOnly(assets: Collection<RemoteAssetItem>) {
        val keep = assets.mapTo(HashSet()) { fileName(it) }
        val staleTmpBefore = System.currentTimeMillis() - STALE_TMP_MS
        synchronized(lock) {
            dir.listFiles()?.forEach { file ->
                val isTmp = file.name.endsWith(TMP_SUFFIX)
                // A recent .tmp belongs to a download in progress
                if (isTmp && file.lastModified() > staleTmpBefore) return@forEach
                if (isTmp || file.name !in keep) file.delete()
            }
        }
    }

    /** Stable, filesystem-safe name for the asset's current content. */
    fun fileName(asset: RemoteAssetItem): String {
        val fingerprint = sha256Hex(
            listOf(
                asset.downloadUrl.trim(),
                asset.format.lowercase(),
                asset.version.toString(),
                asset.updatedAt.trim(),
                asset.sha256.trim().lowercase(),
                asset.fileSizeBytes.toString(),
                generation().toString()
            ).joinToString("|")
        ).take(16)
        return "${idPrefix(asset)}$fingerprint.${extension(asset)}"
    }

    /** Shared by every version of one asset; the id hash keeps sanitized ids from colliding. */
    private fun idPrefix(asset: RemoteAssetItem): String {
        val safeId = asset.id.replace(UNSAFE_CHARS, "_").take(48)
        return "$safeId-${sha256Hex(asset.id).take(8)}~"
    }

    private fun extension(asset: RemoteAssetItem): String {
        val format = asset.format.lowercase()
        return when {
            asset.isLottie -> "json"
            format.matches(EXTENSION) -> format
            else -> "bin"
        }
    }

    private fun trim(limit: Long) {
        synchronized(lock) {
            val files = dir.listFiles()?.filter { !it.name.endsWith(TMP_SUFFIX) } ?: return
            var total = files.sumOf { it.length() }
            if (total <= limit) return
            for (file in files.sortedBy { it.lastModified() }) {
                if (total <= limit) break
                val size = file.length()
                if (file.delete()) total -= size
            }
        }
    }

    companion object {
        const val DEFAULT_MAX_BYTES = 60L * 1024 * 1024
        private const val MIN_FREE_BYTES = 8L * 1024 * 1024
        private const val STALE_TMP_MS = 10 * 60 * 1000L
        private const val TMP_SUFFIX = ".tmp"
        private val UNSAFE_CHARS = Regex("[^A-Za-z0-9_.-]")
        private val EXTENSION = Regex("[a-z0-9]{1,5}")

        fun sha256Hex(value: String): String = sha256Hex(value.toByteArray(Charsets.UTF_8))

        fun sha256Hex(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it) }
    }
}
