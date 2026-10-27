package com.jhw.batteryemoji.core.Utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.util.LruCache
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.jhw.batteryemoji.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object ImageLoaderUtils {

    private val loaderScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Allocate 1/8th of available runtime memory for downsampled emoji bitmap cache
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 8).coerceAtLeast(1024 * 16) // at least 16 MB

    private val memoryCache = object : LruCache<Int, Bitmap>(cacheSize) {
        override fun sizeOf(key: Int, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    /**
     * Loads a downsampled & cached bitmap into the given ImageView.
     * If the bitmap is already in memory cache, it is set immediately (0ms UI thread time).
     * If not, it is decoded in the background with inSampleSize to prevent any UI stutter.
     */
    fun loadOptimizedEmoji(
        imageView: ImageView,
        @DrawableRes drawableRes: Int,
        targetWidthDp: Int = 100,
        targetHeightDp: Int = 100
    ) {
        if (drawableRes == 0) {
            imageView.setImageDrawable(null)
            imageView.setTag(R.id.ivEmojiMascot, null)
            return
        }

        // 1. Instant Cache Hit (0ms on main thread)
        val cached = memoryCache.get(drawableRes)
        if (cached != null && !cached.isRecycled) {
            imageView.setImageBitmap(cached)
            imageView.setTag(R.id.ivEmojiMascot, drawableRes)
            return
        }

        // 2. Tag view with target resource to prevent recycled view flickers
        imageView.setTag(R.id.ivEmojiMascot, drawableRes)
        imageView.setImageDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))

        val context = imageView.context.applicationContext
        val displayMetrics = context.resources.displayMetrics
        val reqWidth = ((targetWidthDp * displayMetrics.density).toInt()).coerceAtLeast(120)
        val reqHeight = ((targetHeightDp * displayMetrics.density).toInt()).coerceAtLeast(120)

        loaderScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                decodeSampledBitmap(context, drawableRes, reqWidth, reqHeight)
            }

            if (bitmap != null) {
                memoryCache.put(drawableRes, bitmap)
                if (imageView.getTag(R.id.ivEmojiMascot) == drawableRes) {
                    imageView.setImageBitmap(bitmap)
                }
            } else {
                if (imageView.getTag(R.id.ivEmojiMascot) == drawableRes) {
                    imageView.setImageResource(drawableRes)
                }
            }
        }
    }

    /**
     * Asynchronously pre-warms the cache in the background so all list items
     * are decoded and in memory before the user scrolls.
     */
    fun preload(
        context: Context,
        drawableResList: List<Int>,
        targetWidthDp: Int = 100,
        targetHeightDp: Int = 100
    ) {
        val appContext = context.applicationContext
        val displayMetrics = appContext.resources.displayMetrics
        val reqWidth = ((targetWidthDp * displayMetrics.density).toInt()).coerceAtLeast(120)
        val reqHeight = ((targetHeightDp * displayMetrics.density).toInt()).coerceAtLeast(120)

        loaderScope.launch(Dispatchers.IO) {
            for (resId in drawableResList) {
                if (resId == 0) continue
                if (memoryCache.get(resId) != null) continue
                val bmp = decodeSampledBitmap(appContext, resId, reqWidth, reqHeight)
                if (bmp != null) {
                    memoryCache.put(resId, bmp)
                }
            }
        }
    }

    private fun decodeSampledBitmap(
        context: Context,
        resId: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        try {
            // First decode bounds
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeResource(context.resources, resId, options)

            if (options.outWidth > 0 && options.outHeight > 0) {
                // Raster image (WebP, PNG, JPG)
                options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
                options.inJustDecodeBounds = false
                options.inPreferredConfig = Bitmap.Config.ARGB_8888

                return BitmapFactory.decodeResource(context.resources, resId, options)
            } else {
                // Vector / XML Drawable
                val drawable = ContextCompat.getDrawable(context, resId) ?: return null
                val w = if (drawable.intrinsicWidth > 0) Math.min(drawable.intrinsicWidth, reqWidth) else reqWidth
                val h = if (drawable.intrinsicHeight > 0) Math.min(drawable.intrinsicHeight, reqHeight) else reqHeight
                val bmp = Bitmap.createBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                return bmp
            }
        } catch (_: Throwable) {
            return null
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
