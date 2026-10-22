package com.asim.jhbatteryapp.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Root object of the remote manifest.json hosted on Dropbox.
 * Backward-compatible: unknown fields are silently ignored by kotlinx.serialization.
 */
@Serializable
data class AssetManifest(
    @SerialName("manifestVersion") val manifestVersion: Int = 1,
    @SerialName("generatedAt") val generatedAt: String = "",
    @SerialName("minAppVersion") val minAppVersion: Int = 1,
    @SerialName("categories") val categories: List<RemoteCategory> = emptyList(),
    @SerialName("assets") val assets: List<RemoteAssetItem> = emptyList()
) {
    /** Filter assets by type (emoji, sticker, animation) and optional category. */
    fun getByType(type: String, categoryId: String? = null): List<RemoteAssetItem> {
        val typeFiltered = assets.filter { it.type.equals(type, ignoreCase = true) }
        return if (categoryId.isNullOrEmpty() || categoryId.equals("hot", ignoreCase = true)) {
            typeFiltered
        } else {
            typeFiltered.filter { it.category.equals(categoryId, ignoreCase = true) }
        }
    }

    /** All unique category IDs present in the manifest assets. */
    fun distinctCategoryIds(): List<String> = assets.map { it.category }.distinct()
}
