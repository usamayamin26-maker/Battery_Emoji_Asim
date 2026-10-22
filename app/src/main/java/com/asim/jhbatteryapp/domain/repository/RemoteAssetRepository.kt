package com.asim.jhbatteryapp.domain.repository

import com.asim.jhbatteryapp.domain.model.AssetManifest
import com.asim.jhbatteryapp.domain.model.RemoteAssetItem

/**
 * Repository contract for the remote asset manifest.
 * Implementations handle fetching, caching, and parsing.
 */
interface RemoteAssetRepository {
    /**
     * Returns the manifest already in memory or saved on disk by an earlier fetch, without any
     * network request. Null on first launch before a fetch ever succeeded.
     */
    suspend fun loadCachedManifest(): AssetManifest?

    /**
     * Refreshes the manifest from the network (throttled and shared across screens).
     * Returns a failure Result when the network request fails.
     */
    suspend fun fetchManifest(): Result<AssetManifest>

    /** Returns the manifest currently held in memory, or null. */
    fun getCachedManifest(): AssetManifest?

    /** Convenience: filter assets by type + optional category from the cache. */
    fun getAssetsByType(type: String, categoryId: String? = null): List<RemoteAssetItem>
}
