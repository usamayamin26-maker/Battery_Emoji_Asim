package com.asim.jhbatteryapp.core.Utils

import com.asim.jhbatteryapp.BuildConfig

/**
 * Central configuration for the remote asset management system.
 * The manifest URL is injected via BuildConfig so it is never hardcoded
 * and can be changed per build variant without touching source code.
 */
object AssetConfig {

    /**
     * Direct-download URL for the Dropbox-hosted manifest.json.
     * Set via buildConfigField in app/build.gradle.kts.
     * Replace PLACEHOLDER_SHARE_ID with your actual Dropbox share ID.
     */
    val MANIFEST_URL: String ="https://www.dropbox.com/scl/fi/s1xirfllee8587cr57aft/manifest.json?rlkey=9afoz156dez2qvwn8lbjtxc67&st=gjlxnp8e&dl=0"

    /** Asset type constants — must match manifest.json "type" field values. */
    object Type {
        const val EMOJI = "emoji"
        const val STICKER = "sticker"
        const val ANIMATION = "animation"
        const val BATTERY = "battery"
        /** Small emoji shown beside the time (Custom → Customize Icon → left emojis). */
        const val STATUS_EMOJI = "status_emoji"
        /** Emotion icon style (Custom → Customize Icon → Emotion), next to the built-in three. */
        const val EMOTION = "emotion"
    }

    /** Asset format constants — must match manifest.json "format" field values. */
    object Format {
        const val PNG = "png"
        const val WEBP = "webp"
        const val SVG = "svg"
        const val GIF = "gif"
        const val LOTTIE = "lottie"
    }
}
