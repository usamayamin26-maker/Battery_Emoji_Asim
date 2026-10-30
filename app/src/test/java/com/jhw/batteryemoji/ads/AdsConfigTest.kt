package com.jhw.batteryemoji.ads

import com.universal.remote.ads.AdSlots
import com.universal.remote.ads.InterstitialClickGates
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdsConfigTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun `isOn returns false when all_ads_on is 0 regardless of flag value`() {
        val payload = """
            {
              "all_ads_on": 0,
              "is_show_splash_inter": 1,
              "home_top_banner": 1
            }
        """.trimIndent()
        
        val raw = json.parseToJsonElement(payload) as JsonObject
        val config = json.decodeFromJsonElement<AdsConfig>(raw).copy(raw = raw)
        
        assertFalse("Splash inter should be off when all_ads_on is 0", config.isOn("is_show_splash_inter"))
        assertFalse("Home banner should be off when all_ads_on is 0", config.isOn("home_top_banner"))
    }

    @Test
    fun `isOn respects flag value when all_ads_on is 1`() {
        val payload = """
            {
              "all_ads_on": 1,
              "is_show_splash_inter": 1,
              "home_top_banner": 0
            }
        """.trimIndent()
        
        val raw = json.parseToJsonElement(payload) as JsonObject
        val config = json.decodeFromJsonElement<AdsConfig>(raw).copy(raw = raw)
        
        assertTrue("Splash inter should be on when flag is 1 and all_ads_on is 1", config.isOn("is_show_splash_inter"))
        assertFalse("Home banner should be off when flag is 0 and all_ads_on is 1", config.isOn("home_top_banner"))
    }

    @Test
    fun `isOn returns false for unknown flag even if all_ads_on is 1`() {
        val payload = """
            {
              "all_ads_on": 1
            }
        """.trimIndent()
        
        val raw = json.parseToJsonElement(payload) as JsonObject
        val config = json.decodeFromJsonElement<AdsConfig>(raw).copy(raw = raw)
        
        assertFalse("Unknown flag should be off", config.isOn("unknown_flag"))
    }

    @Test
    fun `mayShowAnAd returns false when all_ads_on is 0`() {
        val payload = """
            {
              "all_ads_on": 0,
              "home_top_banner": 1,
              "admob_app_banner_id": "ca-app-pub-test"
            }
        """.trimIndent()

        val raw = json.parseToJsonElement(payload) as JsonObject
        val config = json.decodeFromJsonElement<AdsConfig>(raw).copy(raw = raw)

        assertFalse("mayShowAnAd must return false when all_ads_on is 0", AdSlots.mayShowAnAd(config, "home_top", null))
    }

    @Test
    fun `InterstitialClickGates returns false when all_ads_on is 0`() {
        val payload = """
            {
              "all_ads_on": 0,
              "counter_screen_buttons": 1
            }
        """.trimIndent()

        val raw = json.parseToJsonElement(payload) as JsonObject
        val config = json.decodeFromJsonElement<AdsConfig>(raw).copy(raw = raw)

        assertFalse(
            "InterstitialClickGates must return false when all_ads_on is 0",
            InterstitialClickGates.shouldShow(config, "counter_screen_buttons")
        )
    }
}
