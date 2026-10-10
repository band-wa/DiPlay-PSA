package com.shilapi.xcertplay.backup

import android.content.Context
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class SettingsBackupCodecTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    private fun prefs(name: String) = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    @Test
    fun exportOmitsSensitiveAndNavigationKeys() {
        prefs("xcertplay_airplay").edit()
            .putInt("display_fps", 60)
            .putString("identity_private", "secret")
            .putString("manual_hotspot_passphrase", "wifi-pass")
            .putStringSet("pairing_ids", setOf("pair-1"))
            .putString("manual_hotspot_ssid", "car-ap")
            .apply()
        prefs("diplay").edit()
            .putBoolean("auto_connect", true)
            .putString("page", "settings")
            .putString("phone_name", "iphone-of-owner")
            .apply()
        prefs("carplay_picture").edit().putInt("brightness", 80).apply()
        val document = JSONObject(String(SettingsBackupCodec.export(context, "0.2.17")))
        assertEquals(SettingsBackupCodec.FORMAT, document.getString("format"))
        assertEquals(SettingsBackupCodec.VERSION, document.getInt("version"))
        val airplay = document.getJSONObject("settings").getJSONObject("xcertplay_airplay")
        assertEquals(60, airplay.getInt("display_fps"))
        assertEquals("car-ap", airplay.getString("manual_hotspot_ssid"))
        assertFalse(airplay.has("identity_private"))
        assertFalse(airplay.has("manual_hotspot_passphrase"))
        assertFalse(airplay.has("pairing_ids"))
        val diplay = document.getJSONObject("settings").getJSONObject("diplay")
        assertTrue(diplay.getBoolean("auto_connect"))
        assertFalse(diplay.has("page"))
        assertFalse(diplay.has("phone_name"))
        assertEquals(80, document.getJSONObject("settings").getJSONObject("carplay_picture").getInt("brightness"))
        assertFalse(document.getJSONObject("settings").has("diplay_car_hotspot"))
    }

    @Test
    fun importAppliesKnownKeysAndSkipsUnknownOrForbidden() {
        val document = """
            {"format": "diplay-settings", "version": 1, "settings": {
              "xcertplay_airplay": {"display_fps": 48, "brand_new_key": 7, "manual_hotspot_passphrase": "x"},
              "diplay": {"auto_connect": true, "page": "about"},
              "carplay_picture": {"brightness": 66}
            }}
        """.trimIndent()
        val outcome = SettingsBackupCodec.import(context, document.toByteArray())
        assertEquals(false, outcome.invalid)
        assertEquals(4, outcome.applied)
        assertEquals(2, outcome.skipped)
        assertEquals(48, prefs("xcertplay_airplay").getInt("display_fps", 0))
        assertFalse(prefs("xcertplay_airplay").contains("manual_hotspot_passphrase"))
        assertEquals(7, prefs("xcertplay_airplay").getInt("brand_new_key", 0))
        assertEquals(true, prefs("diplay").getBoolean("auto_connect", false))
        assertFalse(prefs("diplay").contains("page"))
        assertEquals(66, prefs("carplay_picture").getInt("brightness", 0))
    }

    @Test
    fun importRejectsForeignOrNewerDocuments() {
        assertTrue(SettingsBackupCodec.import(context, "{\"format\":\"other\",\"version\":1,\"settings\":{}}".toByteArray()).invalid)
        assertTrue(SettingsBackupCodec.import(context, "{\"format\":\"diplay-settings\",\"version\":99,\"settings\":{}}".toByteArray()).invalid)
        assertTrue(SettingsBackupCodec.import(context, "not json".toByteArray()).invalid)
        assertTrue(SettingsBackupCodec.import(context, "{}".toByteArray()).invalid)
    }

    @Test
    fun importMatchesTypesAgainstStoredValues() {
        prefs("xcertplay_airplay").edit().putInt("display_fps", 60).putBoolean("hevc_enabled", false).apply()
        val document = """
            {"format": "diplay-settings", "version": 1, "settings": {
              "xcertplay_airplay": {"display_fps": "48", "hevc_enabled": true, "display_scale_tenths": 85}
            }}
        """.trimIndent()
        val outcome = SettingsBackupCodec.import(context, document.toByteArray())
        assertEquals(2, outcome.applied)
        assertEquals(1, outcome.skipped)
        assertEquals(60, prefs("xcertplay_airplay").getInt("display_fps", 0))
        assertEquals(true, prefs("xcertplay_airplay").getBoolean("hevc_enabled", true))
        assertEquals(85, prefs("xcertplay_airplay").getInt("display_scale_tenths", 0))
    }

    @Test
    fun exportImportRoundTripRestoresValues() {
        prefs("xcertplay_airplay").edit()
            .putInt("display_fps", 60)
            .putBoolean("hevc_enabled", true)
            .putString("manual_hotspot_ssid", "car-ap")
            .putFloat("ui_scale_percent", 100f)
            .apply()
        prefs("diplay_car_hotspot").edit().putBoolean("auto_enable", true).apply()
        val document = SettingsBackupCodec.export(context, "0.2.17")
        prefs("xcertplay_airplay").edit()
            .putInt("display_fps", 30)
            .putBoolean("hevc_enabled", false)
            .putString("manual_hotspot_ssid", "changed")
            .putFloat("ui_scale_percent", 120f)
            .apply()
        prefs("diplay_car_hotspot").edit().putBoolean("auto_enable", false).apply()
        val outcome = SettingsBackupCodec.import(context, document)
        assertEquals(false, outcome.invalid)
        val airplay = prefs("xcertplay_airplay")
        assertEquals(60, airplay.getInt("display_fps", 0))
        assertEquals(true, airplay.getBoolean("hevc_enabled", false))
        assertEquals("car-ap", airplay.getString("manual_hotspot_ssid", ""))
        assertEquals(100f, airplay.getFloat("ui_scale_percent", 0f))
        assertEquals(true, prefs("diplay_car_hotspot").getBoolean("auto_enable", false))
        assertFalse(airplay.contains("identity_private"))
    }
}
