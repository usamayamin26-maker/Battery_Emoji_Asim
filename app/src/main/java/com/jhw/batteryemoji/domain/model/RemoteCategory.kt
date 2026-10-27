package com.jhw.batteryemoji.domain.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * A category entry from the remote asset manifest.
 * Maps to the `categories` array in manifest.json.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class RemoteCategory(
    @SerialName("id") val id: String,
    // Accepts "name" too, and defaults so one malformed category can't break the whole manifest
    @SerialName("title") @JsonNames("name") val title: String = "",
    @SerialName("sortOrder") val sortOrder: Int = 0
)
