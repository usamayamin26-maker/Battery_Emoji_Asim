package com.jhw.batteryemoji.data.remote

import com.jhw.batteryemoji.domain.model.AssetManifest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

/**
 * Parses a raw JSON string into an [AssetManifest].
 * Uses kotlinx.serialization (already on the classpath).
 * ignoreUnknownKeys = true ensures backward compatibility when new fields are added.
 */
object AssetManifestParser {

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        // The hosted manifest is hand-edited: tolerate // and /* */ comments and trailing commas
        allowComments = true
        allowTrailingComma = true
    }

    /**
     * Parses the manifest JSON.
     * @return [AssetManifest] on success.
     * @throws kotlinx.serialization.SerializationException on malformed JSON.
     */
    fun parse(jsonString: String): AssetManifest {
        // Validate non-empty before attempting to parse
        if (jsonString.isBlank()) {
            throw IllegalArgumentException("Manifest JSON string is blank")
        }
        return json.decodeFromString<AssetManifest>(jsonString)
    }

    /**
     * Safe parse that returns null instead of throwing.
     */
    fun parseSafe(jsonString: String): AssetManifest? {
        return try {
            parse(jsonString)
        } catch (_: Throwable) {
            null
        }
    }
}
