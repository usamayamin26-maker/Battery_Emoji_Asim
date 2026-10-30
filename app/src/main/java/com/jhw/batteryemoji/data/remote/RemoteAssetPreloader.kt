package com.jhw.batteryemoji.data.remote

import com.jhw.batteryemoji.core.Utils.AssetConfig
import com.jhw.batteryemoji.data.repository.RemoteAssetRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Starts the remote manifest fetch from the splash screen, so by the time Home (or any other
 * screen) asks for it the request is already done or in flight.
 *
 * Screens keep calling [RemoteAssetRepositoryImpl.fetchManifest] as before: the repository shares
 * one request between callers and serves the fresh copy, so nothing is downloaded twice. Runs in
 * its own scope so finishing the splash screen doesn't cancel it.
 */
object RemoteAssetPreloader {

    /** Home's first rows: their thumbnails are downloaded to disk ahead of time. */
    private const val HOME_THUMBNAILS = 12

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            val repository = RemoteAssetRepositoryImpl()
            // Saved copy first, so Home can render from it even if the network is slow
            repository.loadCachedManifest()
            repository.fetchManifest().onSuccess { manifest ->
                RemoteImageLoader.prefetch(manifest.getByType(AssetConfig.Type.EMOJI).take(HOME_THUMBNAILS))
                RemoteImageLoader.prefetch(manifest.getByType(AssetConfig.Type.ANIMATION).take(6))
                val themeItems = manifest.assets.filter {
                    it.type.equals(AssetConfig.Type.THEME, ignoreCase = true) ||
                    it.type.equals(AssetConfig.Type.GRADIENT, ignoreCase = true) ||
                    it.type.equals(AssetConfig.Type.BACKGROUND, ignoreCase = true) ||
                    it.category.equals(AssetConfig.Type.THEME, ignoreCase = true) ||
                    it.category.equals(AssetConfig.Type.GRADIENT, ignoreCase = true) ||
                    it.category.equals(AssetConfig.Type.BACKGROUND, ignoreCase = true)
                }
                RemoteImageLoader.prefetch(themeItems.take(6))
            }
            // A failed fetch is retried by the screens themselves, exactly as before
            started = false
        }
    }
}
