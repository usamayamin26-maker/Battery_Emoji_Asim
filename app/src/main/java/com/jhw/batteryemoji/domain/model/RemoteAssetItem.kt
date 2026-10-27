package com.jhw.batteryemoji.domain.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * A single remote asset entry from the manifest.
 * type  = "emoji" | "sticker" | "animation"
 * format = "png" | "webp" | "svg" | "gif" | "lottie"
 *
 * Does NOT modify any existing domain model.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class RemoteAssetItem(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("category") val category: String,
    @SerialName("type") val type: String,           // emoji | sticker | animation
    @SerialName("format") val format: String,        // png | webp | svg | gif | lottie
    @SerialName("downloadUrl") val downloadUrl: String,
    @SerialName("previewUrl") val previewUrl: String = "",
    @SerialName("fileSizeBytes") val fileSizeBytes: Long = 0L,
    @SerialName("isPremium") val isPremium: Boolean = false,
    @SerialName("isHot") val isHot: Boolean = false,
    @SerialName("isRecent") val isRecent: Boolean = false,
    @SerialName("isTopPick") val isTopPick: Boolean = false,
    @SerialName("backgroundColor") val backgroundColor: String = "#FFECE8F5",
    @SerialName("batteryColor") val batteryColor: String = "#FF9333EA",
    @SerialName("tags") val tags: List<String> = emptyList(),
    // Optional per-asset change markers. Normally not needed: bumping the manifest's
    // manifestVersion re-downloads every cached file. These re-download just this one asset.
    // sha256 (hex) is also checked against the downloaded bytes.
    @SerialName("version") val version: Int = 0,
    @SerialName("updatedAt") val updatedAt: String = "",
    @SerialName("sha256") @JsonNames("hash", "contentHash") val sha256: String = ""
) {
    /** True when this asset is a Lottie JSON animation. */
    val isLottie: Boolean get() = format.equals("lottie", ignoreCase = true)

    /** True when this asset is a raster/vector image (not animation). */
    val isImage: Boolean get() = !isLottie
}
