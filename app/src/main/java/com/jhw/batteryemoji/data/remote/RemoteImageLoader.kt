package com.jhw.batteryemoji.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import android.util.LruCache
import android.view.View
import android.widget.ImageView
import com.airbnb.lottie.LottieAnimationView
import com.caverock.androidsvg.SVG
import com.jhw.batteryemoji.JHBatteryApplication
import com.jhw.batteryemoji.R
import com.jhw.batteryemoji.core.Utils.LoadingShimmerDrawable
import com.jhw.batteryemoji.data.repository.PreferenceRepositoryImpl
import com.jhw.batteryemoji.domain.model.RemoteAssetItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.WeakHashMap

/**
 * Binds remote assets (images and Lottie JSON) to views.
 *
 * Rules:
 * - Files come from [RemoteAssetFileStore]: a cached file is reused without any network request;
 *   a missing one is downloaded once and kept in the disk cache.
 * - Decoded thumbnails are kept in a memory cache, so rebinding a recycled row is instant.
 * - Disk reads run on IO, SVG/bitmap decoding on Default; views are only touched on Main.
 *   A placeholder shows while loading, a fallback when the asset is unavailable (e.g. offline).
 * - Assets the user *applies* are copied to filesDir ([saveForApply], [saveSticker]) because
 *   the overlay services draw them long after the screen is gone, and cache eviction must never
 *   remove them.
 */
object RemoteImageLoader {

    private const val APPLIED_DIR = "applied_assets"
    private const val STICKER_DIR = "sticker_assets"
    private const val STICKER_PX = 512
    private const val THUMB_PX = 200
    private const val PREFETCH_AHEAD = 6

    private val loaderScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Pending load per view; cancelled when the view is bound to something else. Main thread only. */
    private val viewJobs = WeakHashMap<View, Job>()

    /** Decoded bitmaps, sized in KB; shared by grid thumbnails and the overlay's applied files. */
    private val bitmapCache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Loads a remote image asset into an [ImageView]: memory cache → disk cache → network.
     *
     * @param asset   The [RemoteAssetItem] to load.
     * @param view    Target [ImageView].
     * @param onError Optional callback invoked when loading fails.
     */
    fun loadImage(
        asset: RemoteAssetItem,
        view: ImageView,
        onError: (() -> Unit)? = null
    ) {
        // Tag the view so recycled views don't display wrong images
        view.tag = asset.id
        viewJobs.remove(view)?.cancel()

        val memoryKey = "asset:${RemoteAssetFileStore.diskCache.fileName(asset)}@$THUMB_PX"
        bitmapCache.get(memoryKey)?.let {
            view.setImageBitmap(it)
            return
        }
        showLoading(view)

        viewJobs[view] = loaderScope.launch {
            val file = RemoteAssetFileStore.getOrDownload(asset)
            val bitmap = file?.let { decodeFile(it, THUMB_PX, THUMB_PX) }
            // Only update if view tag still matches (not recycled)
            if (view.tag != asset.id) return@launch
            viewJobs.remove(view)
            if (bitmap != null) {
                bitmapCache.put(memoryKey, bitmap)
                view.setImageBitmap(bitmap)
            } else {
                view.setImageResource(R.drawable.ic_remote_unavailable)
                onError?.invoke()
            }
        }
    }

    /** Animated shimmer shown in [view] until its remote file is downloaded and decoded. */
    private fun showLoading(view: ImageView) {
        val radius = 10f * view.resources.displayMetrics.density
        view.setImageDrawable(LoadingShimmerDrawable(radius))
    }

    /**
     * Loads a remote Lottie JSON animation into a [LottieAnimationView] from the disk cache,
     * downloading it first when needed.
     *
     * @param asset The [RemoteAssetItem] with type "animation" and format "lottie".
     * @param view  Target [LottieAnimationView].
     */
    fun loadLottie(
        asset: RemoteAssetItem,
        view: LottieAnimationView,
        onError: (() -> Unit)? = null
    ) {
        view.tag = asset.id
        viewJobs.remove(view)?.cancel()
        view.cancelAnimation()
        showLoading(view)

        viewJobs[view] = loaderScope.launch {
            val file = RemoteAssetFileStore.getOrDownload(asset)
            if (view.tag != asset.id) return@launch
            if (file == null) {
                viewJobs.remove(view)
                view.setImageResource(R.drawable.ic_remote_unavailable)
                onError?.invoke()
                return@launch
            }
            val json = withContext(Dispatchers.IO) {
                if (AssetContent.sniff(file) == AssetKind.LOTTIE_JSON) readText(file) else null
            }
            if (json != null) {
                if (view.tag != asset.id) return@launch
                viewJobs.remove(view)
                // Lottie's default failure listener throws; report errors instead.
                // Keyed by the cache file name, so Lottie reuses the parsed composition on rebind.
                view.setFailureListener {
                    view.setImageResource(R.drawable.ic_remote_unavailable)
                    onError?.invoke()
                }
                view.setAnimationFromJson(json, file.name)
                view.playAnimation()
            } else {
                // Not Lottie JSON (e.g. an SVG/PNG listed as "lottie") — show it as a still image
                val bitmap = decodeFile(file, THUMB_PX, THUMB_PX)
                if (view.tag != asset.id) return@launch
                viewJobs.remove(view)
                if (bitmap != null) {
                    view.setImageBitmap(bitmap)
                } else {
                    view.setImageResource(R.drawable.ic_remote_unavailable)
                    onError?.invoke()
                }
            }
        }
    }

    /**
     * Warms the disk cache for the next few grid items so they appear instantly when scrolled
     * to. Cached items are skipped; nothing is decoded.
     */
    fun prefetch(assets: List<RemoteAssetItem>) {
        RemoteAssetFileStore.prefetch(assets)
    }

    /** Prefetches the [PREFETCH_AHEAD] items after [index] — call when binding item [index]. */
    fun prefetchAfter(assets: List<RemoteAssetItem>, index: Int) {
        val from = index + 1
        if (from >= assets.size) return
        prefetch(assets.subList(from, minOf(from + PREFETCH_AHEAD, assets.size)).toList())
    }

    // ── Applied assets (files the overlay needs) ───────────────────────────────

    /**
     * Copies [asset] (from the disk cache, downloading only if it isn't cached) to
     * <filesDir>/applied_assets so the overlay can draw it.
     * Files no longer referenced by any applied setting are deleted; the battery icon,
     * animation, left emoji, emotion and sticker currently in use are always kept, as is [alsoKeep]
     * (files saved earlier in the same Apply whose path isn't stored yet).
     * @return absolute path of the persisted file, or null on failure.
     */
    suspend fun saveForApply(
        asset: RemoteAssetItem,
        alsoKeep: Collection<String> = emptyList()
    ): String? {
        val target = copyToManaged(asset, APPLIED_DIR) ?: return null
        withContext(Dispatchers.IO) {
            val keep = appliedPaths() + alsoKeep + target.absolutePath
            target.parentFile?.listFiles()?.forEach {
                if (it.absolutePath !in keep) it.delete()
            }
        }
        return target.absolutePath
    }

    /**
     * Copies a remote sticker to <filesDir>/sticker_assets. Kept apart from [saveForApply] so
     * applying a sticker never touches the applied battery/animation file, and other stickers in
     * "My Sticker" stay usable. Offline, an earlier copy of the same sticker is reused.
     */
    suspend fun saveSticker(asset: RemoteAssetItem): String? {
        copyToManaged(asset, STICKER_DIR)?.let { return it.absolutePath }
        return withContext(Dispatchers.IO) {
            val dir = File(JHBatteryApplication.instance.filesDir, STICKER_DIR)
            val prefix = RemoteAssetFileStore.diskCache.fileName(asset).substringBefore('~')
            dir.listFiles()
                ?.firstOrNull { (it.name.startsWith("$prefix~") || it.nameWithoutExtension == asset.id) && it.length() > 0 }
                ?.absolutePath
        }
    }

    /** Shows a sticker from its downloaded file when [path] is set, otherwise from [drawableRes]. */
    fun bindSticker(view: ImageView, drawableRes: Int, path: String?) {
        if (!path.isNullOrEmpty()) {
            view.setImageBitmap(decodeFileForDisplay(path, STICKER_PX, STICKER_PX))
        } else if (drawableRes != 0) {
            view.setImageResource(drawableRes)
        } else {
            view.setImageDrawable(null)
        }
    }

    /**
     * Deletes what earlier builds cached on disk (the old browse cache and manifest).
     * Cheap to call on every start; does nothing once they are gone.
     */
    fun purgeLegacyDiskCache(context: Context) {
        val appContext = context.applicationContext
        loaderScope.launch(Dispatchers.IO) {
            File(appContext.cacheDir, "remote_assets").deleteRecursively()
            File(appContext.filesDir, "remote_assets").deleteRecursively()
            appContext.deleteSharedPreferences("jh_remote_asset_cache")
        }
    }

    /**
     * Synchronously decodes an applied asset file (SVG / PNG / WebP / GIF first frame) to fit
     * [reqW]x[reqH] px. Results are memory-cached per path and size.
     */
    fun decodeFileForDisplay(path: String, reqW: Int, reqH: Int): Bitmap? {
        val key = "file:$path@${reqW}x$reqH"
        bitmapCache.get(key)?.let { return it }
        val file = File(path)
        if (!file.exists()) return null
        val bytes = readBytes(file) ?: return null
        val bitmap = decodeAny(bytes, reqW, reqH) ?: return null
        bitmapCache.put(key, bitmap)
        return bitmap
    }

    /**
     * Binds an applied asset file to a [LottieAnimationView]: plays it when it's Lottie JSON,
     * otherwise shows it as a still image. Re-binding the same path only resumes playback.
     */
    fun bindAnimationFile(view: LottieAnimationView, path: String, reqW: Int, reqH: Int) {
        if (view.tag == path) {
            if (view.composition != null && !view.isAnimating) view.playAnimation()
            return
        }
        view.tag = path
        val file = File(path)
        if (!file.exists()) {
            view.cancelAnimation()
            view.setImageDrawable(null)
            return
        }
        if (AssetContent.sniff(file) == AssetKind.LOTTIE_JSON) {
            view.setFailureListener { view.setImageDrawable(null) }
            view.setAnimation(file.inputStream(), path)
            view.playAnimation()
        } else {
            view.cancelAnimation()
            view.setImageBitmap(decodeFileForDisplay(path, reqW, reqH))
        }
    }

    // ── Internal helpers ───────────────────────────────────────────────────────

    /**
     * Copies the cached file of [asset] into <filesDir>/[dirName], named after its content
     * fingerprint, so a changed asset gets a new path and never reuses a stale decoded bitmap.
     */
    private suspend fun copyToManaged(asset: RemoteAssetItem, dirName: String): File? {
        val source = RemoteAssetFileStore.getOrDownload(asset) ?: return null
        return withContext(Dispatchers.IO) {
            try {
                val dir = File(JHBatteryApplication.instance.filesDir, dirName).also { it.mkdirs() }
                val target = File(dir, source.name)
                if (target.isFile && target.length() == source.length()) return@withContext target
                val tmp = File(dir, "${source.name}.tmp")
                source.copyTo(tmp, overwrite = true)
                if (!tmp.renameTo(target)) {
                    target.delete()
                    if (!tmp.renameTo(target)) {
                        tmp.delete()
                        return@withContext null
                    }
                }
                target
            } catch (_: IOException) {
                // Low storage or I/O error
                null
            }
        }
    }

    /** Paths the overlay services may currently be drawing. */
    private fun appliedPaths(): Set<String> {
        val prefs = PreferenceRepositoryImpl()
        return setOf(
            prefs.getAppliedBatteryIconPath(),
            prefs.getAppliedAnimationPath(),
            prefs.getLeftStatusEmojiPath(),
            prefs.getEmotionPath(),
            prefs.getFloatingStickerImagePath(),
            prefs.getAppliedThemePath()
        ).filterTo(HashSet()) { it.isNotEmpty() }
    }

    /** Reads on IO and decodes on Default, so SVG parsing never runs on the main thread. */
    private suspend fun decodeFile(file: File, reqW: Int, reqH: Int): Bitmap? {
        val bytes = withContext(Dispatchers.IO) { readBytes(file) } ?: return null
        return withContext(Dispatchers.Default) { decodeAny(bytes, reqW, reqH) }
    }

    private fun readBytes(file: File): ByteArray? = try {
        file.readBytes()
    } catch (_: IOException) {
        null
    }

    private fun readText(file: File): String? = try {
        file.readText(Charsets.UTF_8)
    } catch (_: IOException) {
        null
    }

    private fun decodeAny(bytes: ByteArray, reqW: Int, reqH: Int): Bitmap? = when (AssetContent.sniff(bytes)) {
        AssetKind.SVG -> decodeSvg(bytes, reqW, reqH)
        AssetKind.BITMAP -> decodeSampled(bytes, reqW, reqH)
        else -> null
    }

    /** Renders an SVG into a bitmap that fits within [reqW]x[reqH], keeping aspect ratio. */
    private fun decodeSvg(bytes: ByteArray, reqW: Int, reqH: Int): Bitmap? {
        return try {
            val svg = bytes.inputStream().use { SVG.getFromInputStream(it) }
            if (svg.documentViewBox == null && svg.documentWidth > 0 && svg.documentHeight > 0) {
                svg.setDocumentViewBox(0f, 0f, svg.documentWidth, svg.documentHeight)
            }
            val box = svg.documentViewBox
            val srcW = box?.width() ?: reqW.toFloat()
            val srcH = box?.height() ?: reqH.toFloat()
            // Otherwise AndroidSVG draws at the declared width/height (e.g. 3503x4210) instead of
            // scaling the viewBox into the bitmap, and a large SVG shows only its empty top-left corner
            if (box != null) {
                svg.setDocumentWidth("100%")
                svg.setDocumentHeight("100%")
            }
            val scale = minOf(reqW / srcW, reqH / srcH)
            val w = (srcW * scale).toInt().coerceAtLeast(1)
            val h = (srcH * scale).toInt().coerceAtLeast(1)
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { bmp ->
                svg.renderToCanvas(Canvas(bmp), RectF(0f, 0f, w.toFloat(), h.toFloat()))
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun decodeSampled(bytes: ByteArray, reqW: Int, reqH: Int): Bitmap? {
        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            opts.inSampleSize = calculateSampleSize(opts, reqW, reqH)
            opts.inJustDecodeBounds = false
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        } catch (_: Throwable) {
            null
        }
    }

    private fun calculateSampleSize(opts: BitmapFactory.Options, reqW: Int, reqH: Int): Int {
        var size = 1
        if (opts.outHeight > reqH || opts.outWidth > reqW) {
            val halfH = opts.outHeight / 2
            val halfW = opts.outWidth / 2
            while (halfH / size >= reqH && halfW / size >= reqW) size *= 2
        }
        return size
    }
}
