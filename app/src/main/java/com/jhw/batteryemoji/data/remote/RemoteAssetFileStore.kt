package com.jhw.batteryemoji.data.remote

import android.graphics.BitmapFactory
import android.util.Log
import com.jhw.batteryemoji.JHBatteryApplication
import com.jhw.batteryemoji.data.datasource.AssetDiskCache
import com.jhw.batteryemoji.domain.model.RemoteAssetItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** What a downloaded file really is — manifest "format" values aren't reliable. */
internal enum class AssetKind { SVG, LOTTIE_JSON, HTML, BITMAP }

internal object AssetContent {

    fun sniff(bytes: ByteArray): AssetKind {
        val head = String(bytes, 0, minOf(bytes.size, 512), Charsets.UTF_8)
            .removePrefix("﻿").trimStart().lowercase()
        return when {
            head.startsWith("<!doctype html") || head.startsWith("<html") -> AssetKind.HTML
            head.contains("<svg") -> AssetKind.SVG
            head.startsWith("{") -> AssetKind.LOTTIE_JSON
            else -> AssetKind.BITMAP
        }
    }

    fun sniff(file: File): AssetKind {
        val head = try {
            file.inputStream().use { input ->
                val buf = ByteArray(512)
                val n = input.read(buf)
                if (n <= 0) ByteArray(0) else buf.copyOf(n)
            }
        } catch (_: Exception) {
            ByteArray(0)
        }
        return sniff(head)
    }

    /**
     * Cheap integrity check run before anything is cached: rejects HTML error pages, truncated
     * JSON, undecodable bitmaps and hash mismatches, so a corrupted download is never reused.
     */
    fun isValid(asset: RemoteAssetItem, bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        val expectedHash = asset.sha256.trim().lowercase()
        if (expectedHash.isNotEmpty() && AssetDiskCache.sha256Hex(bytes) != expectedHash) return false
        return when (sniff(bytes)) {
            AssetKind.HTML -> false
            AssetKind.SVG -> String(bytes, Charsets.UTF_8).contains("</svg", ignoreCase = true)
            AssetKind.LOTTIE_JSON -> String(bytes, Charsets.UTF_8).trimEnd().endsWith("}")
            AssetKind.BITMAP -> {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                opts.outWidth > 0 && opts.outHeight > 0
            }
        }
    }
}

/**
 * Single entry point for remote asset files: returns the cached copy when it is valid for the
 * manifest entry, otherwise downloads it once.
 *
 * - Cache hit → no network request at all.
 * - Concurrent requests for the same asset share one download.
 * - Downloads run in their own scope, so a recycled view or closed screen doesn't waste a
 *   download that is almost done; the file still lands in the cache for next time.
 * - At most [MAX_PARALLEL_DOWNLOADS] downloads run at once; a failed asset isn't retried for
 *   [FAILURE_BACKOFF_MS], so an offline grid doesn't hammer the network on every scroll.
 */
object RemoteAssetFileStore {

    private const val TAG = "RemoteAssetFileStore"
    private const val CACHE_DIR = "remote_asset_cache"
    private const val MAX_ASSET_BYTES = 15L * 1024 * 1024
    private const val MAX_PARALLEL_DOWNLOADS = 4
    private const val FAILURE_BACKOFF_MS = 15_000L

    @OptIn(ExperimentalCoroutinesApi::class)
    private val downloadDispatcher = Dispatchers.IO.limitedParallelism(MAX_PARALLEL_DOWNLOADS)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val inFlight = ConcurrentHashMap<String, Deferred<File?>>()
    private val failedAt = ConcurrentHashMap<String, Long>()

    private val client: OkHttpClient by lazy {
        RemoteAssetDataSource.defaultClient.newBuilder()
            .callTimeout(45, TimeUnit.SECONDS)
            .build()
    }

    /**
     * manifestVersion of the manifest in use. It is part of every cache file name, so bumping
     * manifestVersion makes all cached files stale: each asset is downloaded again the next time it
     * is shown (not all at once), and the old files are deleted by [retainOnly].
     */
    @Volatile
    var manifestVersion: Int = 0

    val diskCache: AssetDiskCache by lazy {
        AssetDiskCache(
            File(JHBatteryApplication.instance.cacheDir, CACHE_DIR),
            generation = { manifestVersion }
        )
    }

    /** Cached file for [asset] without touching the network; null when not cached. */
    suspend fun getCached(asset: RemoteAssetItem): File? =
        withContext(Dispatchers.IO) { diskCache.get(asset) }

    /** Cached file for [asset], downloading it first when needed; null when unavailable. */
    suspend fun getOrDownload(asset: RemoteAssetItem): File? {
        getCached(asset)?.let { return it }
        val key = diskCache.fileName(asset)
        if (isBackingOff(key)) return null
        return downloadOnce(key, asset).await()
    }

    /** Warms the disk cache for [assets] in the background; already cached ones are skipped. */
    fun prefetch(assets: List<RemoteAssetItem>) {
        if (assets.isEmpty()) return
        scope.launch {
            for (asset in assets) {
                val key = diskCache.fileName(asset)
                if (inFlight.containsKey(key) || isBackingOff(key)) continue
                if (diskCache.get(asset) == null) downloadOnce(key, asset)
            }
        }
    }

    /** Invalidates cached files of assets that were removed or changed in [assets]. */
    fun retainOnly(assets: Collection<RemoteAssetItem>) {
        failedAt.clear()
        scope.launch { diskCache.retainOnly(assets) }
    }

    private fun isBackingOff(key: String): Boolean {
        val lastFailure = failedAt[key] ?: return false
        return System.currentTimeMillis() - lastFailure < FAILURE_BACKOFF_MS
    }

    private fun downloadOnce(key: String, asset: RemoteAssetItem): Deferred<File?> {
        inFlight[key]?.let { return it }
        val job = scope.async(downloadDispatcher, start = CoroutineStart.LAZY) {
            // Another caller may have finished it between the cache check and now
            val file = diskCache.get(asset) ?: download(asset)?.let { diskCache.put(asset, it) }
            if (file == null) failedAt[key] = System.currentTimeMillis() else failedAt.remove(key)
            file
        }
        val existing = inFlight.putIfAbsent(key, job)
        if (existing != null) return existing
        job.invokeOnCompletion { inFlight.remove(key, job) }
        job.start()
        return job
    }

    /** Downloads and validates [asset]; null on HTTP/network errors, oversize or corrupt content. */
    private fun download(asset: RemoteAssetItem): ByteArray? {
        Log.d(TAG, "Downloading asset ${asset.id}")
        return try {
            val request = Request.Builder()
                .url(toDirectDownloadUrl(asset.downloadUrl))
                .build()
            val bytes = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "HTTP ${response.code} for asset ${asset.id}")
                    return null
                }
                val body = response.body ?: return null
                if (body.contentLength() > MAX_ASSET_BYTES) return null
                readCapped(body.byteStream()) ?: return null
            }
            if (AssetContent.isValid(asset, bytes)) bytes else {
                Log.w(TAG, "Rejected invalid download for asset ${asset.id}")
                null
            }
        } catch (e: Exception) {
            // IOException for network errors/timeouts, IllegalArgumentException for a malformed URL
            Log.w(TAG, "Download failed for asset ${asset.id}: ${e.message}")
            null
        }
    }

    private fun readCapped(input: java.io.InputStream): ByteArray? {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(16 * 1024)
        var total = 0L
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            total += n
            if (total > MAX_ASSET_BYTES) return null
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    /** Dropbox share links with dl=0 return an HTML preview page; force a direct download. */
    fun toDirectDownloadUrl(url: String): String {
        if (!url.contains("dropbox.com")) return url
        return when {
            url.contains("dl=0") -> url.replace("dl=0", "dl=1")
            url.contains("dl=1") || url.contains("raw=1") -> url
            url.contains("?") -> "$url&dl=1"
            else -> "$url?dl=1"
        }
    }
}
