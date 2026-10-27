package com.jhw.batteryemoji.data.repository

import android.util.Log
import com.jhw.batteryemoji.data.datasource.ManifestLocalDataSource
import com.jhw.batteryemoji.data.remote.AssetManifestParser
import com.jhw.batteryemoji.data.remote.RemoteAssetDataSource
import com.jhw.batteryemoji.data.remote.RemoteAssetFileStore
import com.jhw.batteryemoji.domain.model.AssetManifest
import com.jhw.batteryemoji.domain.model.RemoteAssetItem
import com.jhw.batteryemoji.domain.repository.RemoteAssetRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Concrete implementation of [RemoteAssetRepository].
 *
 * Fetch strategy (state is shared by every instance, so each ViewModel's own instance reuses it):
 * 1. [loadCachedManifest] returns the in-memory manifest, or the copy saved on disk by the last
 *    successful fetch — screens show it immediately, even offline.
 * 2. [fetchManifest] refreshes from Dropbox at most once per [REFRESH_INTERVAL_MS]; concurrent
 *    callers share one request. A new manifest is saved to disk and cached asset files whose
 *    entry was removed or changed are invalidated. A new manifestVersion invalidates all of them,
 *    so replacing a file behind the same link only needs a manifestVersion bump.
 * 3. If the network fails → failure Result; the cached manifest stays available.
 *
 * All I/O runs on Dispatchers.IO.
 */
class RemoteAssetRepositoryImpl(
    private val remoteDataSource: RemoteAssetDataSource = RemoteAssetDataSource(),
    private val localDataSource: ManifestLocalDataSource = ManifestLocalDataSource(),
    private val assetFileStore: RemoteAssetFileStore = RemoteAssetFileStore
) : RemoteAssetRepository {

    override suspend fun loadCachedManifest(): AssetManifest? {
        memoryManifest?.let { return it }
        return withContext(Dispatchers.IO) {
            diskLock.withLock {
                memoryManifest ?: localDataSource.read()
                    ?.let { AssetManifestParser.parseSafe(it) }
                    ?.let(::deduplicate)
                    ?.also {
                        if (memoryManifest == null) {
                            assetFileStore.manifestVersion = it.manifestVersion
                            memoryManifest = it
                        }
                    }
            }
        }
    }

    override suspend fun fetchManifest(): Result<AssetManifest> = withContext(Dispatchers.IO) {
        refreshLock.withLock {
            val current = memoryManifest
            if (current != null && isFresh()) return@withLock Result.success(current)

            try {
                Log.d(TAG, "Fetching manifest from network")
                val rawJson = remoteDataSource.fetchManifestJson()
                val manifest = deduplicate(AssetManifestParser.parse(rawJson))

                if (manifest != current) {
                    localDataSource.write(rawJson)
                    // A new manifestVersion invalidates every cached file (see RemoteAssetFileStore)
                    assetFileStore.manifestVersion = manifest.manifestVersion
                    // An empty list is more likely a broken upload than a real wipe: keep files
                    if (manifest.assets.isNotEmpty()) assetFileStore.retainOnly(manifest.assets)
                }
                memoryManifest = manifest
                lastNetworkRefreshAt = System.currentTimeMillis()
                Result.success(manifest)
            } catch (e: CancellationException) {
                throw e
            } catch (networkError: Throwable) {
                Result.failure(networkError)
            }
        }
    }

    override fun getCachedManifest(): AssetManifest? = memoryManifest

    override fun getAssetsByType(type: String, categoryId: String?): List<RemoteAssetItem> {
        return getCachedManifest()?.getByType(type, categoryId) ?: emptyList()
    }

    /** Deduplicate assets by ID — last occurrence wins. */
    private fun deduplicate(manifest: AssetManifest): AssetManifest = manifest.copy(
        assets = manifest.assets
            .groupBy { it.id }
            .mapValues { (_, v) -> v.last() }
            .values
            .toList()
    )

    private fun isFresh(): Boolean =
        lastNetworkRefreshAt > 0 &&
            System.currentTimeMillis() - lastNetworkRefreshAt < REFRESH_INTERVAL_MS

    private companion object {
        const val TAG = "RemoteAssetRepository"
        const val REFRESH_INTERVAL_MS = 15 * 60 * 1000L

        @Volatile
        var memoryManifest: AssetManifest? = null

        @Volatile
        var lastNetworkRefreshAt = 0L

        val refreshLock = Mutex()
        val diskLock = Mutex()
    }
}
