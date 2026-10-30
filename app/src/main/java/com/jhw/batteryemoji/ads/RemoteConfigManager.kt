package com.jhw.batteryemoji.ads

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.universal.remote.ads.AdsController
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

/**
 * Wraps Firebase Remote Config for the app's single ads JSON parameter ([com.universal.remote.ads.AdsSetup.remoteConfigKey]),
 * with [com.universal.remote.ads.AdsSetup.defaultJson] as its default, so ad placements stay off (fail closed) until a fetch
 * succeeds. Both come from [com.universal.remote.ads.AdsController.init]; nothing in this file is app-specific.
 *
 * Every ad unit id in the default JSON is deliberately blank and every ad flag is 0. A blank id makes
 * the loaders log DISABLED and collapse the slot, so a release build that never reaches Remote
 * Config shows no ads rather than requesting against an id that does not exist. Debug builds are
 * unaffected: [com.universal.remote.ads.AdUnitIds] swaps in Google's test ids before the id reaches a loader — but the
 * flags are still 0, so the slots stay collapsed there too.
 *
 * **Failure is always all-off, never half-on.** [parse] returns a whole [AdsConfig] or none: a
 * malformed or wrongly typed payload is caught and replaced by `AdsConfig()`, whose every field is
 * the same "off" default as the default JSON. There is no path that applies some keys from a broken
 * payload and leaves the rest at whatever was there before.
 */
object RemoteConfigManager {

    private const val TAG = "RemoteConfigManager"
    private const val FETCH_TIMEOUT_SECONDS = 10L
    private const val MIN_FETCH_INTERVAL_SECONDS_RELEASE = 3600L

    // In debug builds, never serve a throttled/cached fetch — always hit the network so console
    // edits show up on the next app launch instead of waiting out the release throttle window.
    private const val MIN_FETCH_INTERVAL_SECONDS_DEBUG = 0L

    /** The app's Remote Config parameter and its default JSON, from [com.universal.remote.ads.AdsController.init]. */
    private val configKey: String get() = AdsController.setup.remoteConfigKey
    private val defaultJson: String get() = AdsController.setup.defaultJson

    /** Keys of the app's default JSON: the known keys, for [AdsConfig]'s debug typo check. */
    private val knownKeys: Set<String> by lazy {
        runCatching { json.parseToJsonElement(defaultJson).jsonObject.keys }.getOrDefault(emptySet())
    }

    fun isKnownKey(key: String): Boolean = key in knownKeys

    /**
     * The package's own [Json], deliberately not the app's shared `SerializingHelper` instance —
     * this package depends on no class of the app it sits in.
     *
     * `ignoreUnknownKeys` because the console will grow keys this build does not know about, and a
     * new key must never fail the parse for an old build. `coerceInputValues` so an explicit null
     * or an out-of-range enum falls back to the field's default instead of throwing.
     */
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    /** Every flag 0 and every unit blank: what [current] returns while [AppAds.ADS_ENABLED] is false. */
    private val ADS_OFF = AdsConfig()

    @Volatile
    private var cachedConfig: AdsConfig? = null

    /**
     * Last fetched config. If nothing was fetched in this process (e.g. the app was restored after
     * process death and splash didn't run), falls back to the values Firebase activated in an
     * earlier session, and only then to the default JSON.
     */
    fun current(): AdsConfig = if (!AppAds.ADS_ENABLED) ADS_OFF else cachedConfig ?: run {
        val payload = withRemoteConfig { it.getString(configKey) }.orEmpty()
        parse(payload.ifBlank { defaultJson })
    }.also { cachedConfig = it }

    /**
     * Fetches and activates the remote config, then invokes [onComplete] with the resolved
     * config on the main thread. **[onComplete] always fires exactly once**, whatever happens —
     * that is what lets [com.universal.remote.ads.AdsBootstrap] treat this as a step it can wait on.
     *
     * Cold start with no network: `setDefaultsAsync` has already made the default JSON the value
     * `getString` returns, the fetch fails on its [FETCH_TIMEOUT_SECONDS] deadline, and the
     * listener still runs — so the resolved config is the default JSON (every flag 0, every unit id
     * blank) and every slot collapses. A later launch with a network picks up the console's values;
     * a launch after that, even offline, gets the values Firebase activated and persisted last time.
     */
    fun fetchAndActivate(onComplete: (AdsConfig) -> Unit) {
        if (!AppAds.ADS_ENABLED) {
            onComplete(ADS_OFF)
            return
        }
        val remoteConfig = withRemoteConfig { it }
        if (remoteConfig == null) {
            val config = parse(defaultJson)
            cachedConfig = config
            onComplete(config)
            return
        }
        val minFetchIntervalSeconds =
            if (AdsController.isDebug) MIN_FETCH_INTERVAL_SECONDS_DEBUG else MIN_FETCH_INTERVAL_SECONDS_RELEASE
        remoteConfig.setConfigSettingsAsync(
            remoteConfigSettings {
                minimumFetchIntervalInSeconds = minFetchIntervalSeconds
                fetchTimeoutInSeconds = FETCH_TIMEOUT_SECONDS
            }
        )
        remoteConfig.setDefaultsAsync(mapOf(configKey to defaultJson))
            .addOnCompleteListener {
                remoteConfig.fetchAndActivate()
                    .addOnCompleteListener { task ->
                        val payload = remoteConfig.getString(configKey)
                        val config = if (payload.isNotBlank()) parse(payload) else current()
                        cachedConfig = config
                        if (!task.isSuccessful) {
                            Log.w(TAG, "Remote config fetch failed, using cached/default values", task.exception)
                        }
                        onComplete(config)
                    }
            }
    }

    /**
     * Reaches Firebase Remote Config, or returns null if there is no Firebase to reach.
     *
     * `Firebase.remoteConfig` throws `IllegalStateException` when no `FirebaseApp` was initialised in
     * the process, and a `FirebaseApp` only exists when the host applied the `google-services` plugin
     * and shipped a `google-services.json`. Step 4 of *Adopting it in a new project* says to do both,
     * and until someone does — or in any build where that file is deliberately absent — every call
     * here used to throw from inside an AdMob SDK callback, on the main thread, where no caller could
     * catch it: the app died on the launch after consent was settled.
     *
     * That is the one failure this package must not have. Its rule is that a build which never
     * reaches Remote Config shows no ads, not that it crashes; "no Firebase at all" is the extreme of
     * "never reaches Remote Config", so it takes the same path as a failed fetch and resolves to
     * the default JSON — every flag 0, every unit id blank.
     */
    private fun <T> withRemoteConfig(block: (FirebaseRemoteConfig) -> T): T? = try {
        block(Firebase.remoteConfig)
    } catch (e: IllegalStateException) {
        Log.e(TAG, "No FirebaseApp in this process, so there is no remote config; every slot stays off", e)
        null
    }

    /**
     * All or nothing: on any failure every slot is off, rather than some keys applied and the rest
     * left over from an earlier value. Note this is stricter than the Gson version it replaced —
     * Gson quietly coerced `"1"` (a quoted number) into `1`, kotlinx.serialization treats it as a
     * type error and the whole payload falls back to defaults. That is the safer direction, but it
     * means the console's numbers must be written unquoted.
     */
    private fun parse(raw: String): AdsConfig = try {
        val defaultMap = runCatching {
            json.parseToJsonElement(defaultJson).jsonObject.toMutableMap()
        }.getOrDefault(mutableMapOf())
        val remoteMap = json.parseToJsonElement(raw).jsonObject
        defaultMap.putAll(remoteMap)
        val merged = JsonObject(defaultMap)
        json.decodeFromJsonElement<AdsConfig>(merged).copy(raw = merged)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to parse ads config JSON, every slot stays off", e)
        AdsConfig()
    }
}
