package com.asim.jhbatteryapp.presentation.remote

import com.asim.jhbatteryapp.domain.model.RemoteAssetItem

/**
 * UI state for screens that display remote assets.
 * Consumed by ViewModels and observed in Fragments.
 *
 * Loading  → show shimmer / progress indicator
 * Success  → remote items fetched successfully
 * Empty    → no assets returned for this type/category
 * Error    → network or parse failure with no cache available
 * Offline  → serving from disk cache; network was unavailable
 */
sealed class RemoteAssetUiState {
    object Loading : RemoteAssetUiState()

    data class Success(
        val items: List<RemoteAssetItem>,
        val isFromCache: Boolean = false
    ) : RemoteAssetUiState()

    object Empty : RemoteAssetUiState()

    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : RemoteAssetUiState()

    data class Offline(
        val cachedItems: List<RemoteAssetItem>
    ) : RemoteAssetUiState()
}
