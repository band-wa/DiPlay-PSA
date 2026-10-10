package com.shilapi.xcertplay.backup

import android.content.Context
import android.content.SharedPreferences.Editor
import org.json.JSONArray
import org.json.JSONObject

data class SettingsImportOutcome(val applied: Int, val skipped: Int, val invalid: Boolean)

/** Reads and writes the settings documents exchanged through the backup web page. */
object SettingsBackupCodec {
    const val FORMAT = "diplay-settings"
    const val VERSION = 1

    private val SENSITIVE_KEYS = setOf(
        "identity_private",
        "identity_public",
        "pairing_id",
        "pairing_ids",
        "lockdown_host_id",
        "lockdown_system_buid",
        "lockdown_wifi_mac",
        "lockdown_device_public",
        "lockdown_device_cert",
        "lockdown_host_private",
        "lockdown_host_cert",
        "lockdown_root_private",
        "lockdown_root_cert",
        "manual_hotspot_passphrase",
        "remote_mfi_server",
        "remote_mfi_token",
        "mfi_i2c_path",
    )
    private val DIPLAY_KEYS = setOf(
        "auto_connect",
        "connect_on_phone_bluetooth",
        "default_connection_mode",
        "settings_force_full_layout",
        "app_appearance_button",
        "byd_vehicle_advanced",
        "picture_controls",
        "settings_gesture_fingers",
    )
    private val FILES = mapOf(
        "xcertplay_airplay" to Policy.Deny(SENSITIVE_KEYS),
        "diplay" to Policy.Allow(DIPLAY_KEYS),
        "carplay_picture" to Policy.All,
        "diplay_car_hotspot" to Policy.All,
    )

    private sealed interface Policy {
        fun includes(key: String): Boolean

        data object All : Policy {
            override fun includes(key: String) = true
        }

        data class Allow(val keys: Set<String>) : Policy {
            override fun includes(key: String) = key in keys
        }

        data class Deny(val keys: Set<String>) : Policy {
            override fun includes(key: String) = key !in keys
        }
    }

    fun export(context: Context, appVersion: String): ByteArray {
        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("version", VERSION)
        root.put("app", appVersion)
        root.put("exportedAt", System.currentTimeMillis())
        val files = JSONObject()
        for ((name, policy) in FILES) {
            val values = context.getSharedPreferences(name, Context.MODE_PRIVATE).all
                .filterKeys { policy.includes(it) }
                .filterValues { it !is ByteArray }
                .mapValues { it.value!! }
            if (values.isEmpty()) continue
            val file = JSONObject()
            for ((key, value) in values) file.put(key, jsonValue(value))
            files.put(name, file)
        }
        root.put("settings", files)
        return root.toString(2).toByteArray(Charsets.UTF_8)
    }

    fun import(context: Context, document: ByteArray): SettingsImportOutcome {
        val root = runCatching { JSONObject(document.toString(Charsets.UTF_8)) }.getOrNull()
            ?: return SettingsImportOutcome(0, 0, invalid = true)
        if (root.optString("format") != FORMAT || root.optInt("version") > VERSION) {
            return SettingsImportOutcome(0, 0, invalid = true)
        }
        val files = root.optJSONObject("settings")
            ?: return SettingsImportOutcome(0, 0, invalid = true)
        var applied = 0
        var skipped = 0
        for (name in files.keys()) {
            val policy = FILES[name]
            val file = files.optJSONObject(name)
            if (policy == null || file == null) {
                skipped += files.optJSONObject(name)?.length() ?: 1
                continue
            }
            val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            val current = prefs.all
            val editor = prefs.edit()
            for (key in file.keys()) {
                val value = file.opt(key)
                if (!policy.includes(key) || value === JSONObject.NULL) {
                    skipped++
                    continue
                }
                val stored = when (val target = current[key]) {
                    is Boolean -> if (value is Boolean) { editor.putBoolean(key, value); true } else false
                    is Int -> number(value)?.let { editor.putInt(key, it.toInt()); true } ?: false
                    is Long -> number(value)?.let { editor.putLong(key, it.toLong()); true } ?: false
                    is Float -> number(value)?.let { editor.putFloat(key, it.toFloat()); true } ?: false
                    is String -> if (value is String) { editor.putString(key, value); true } else false
                    is Set<*> -> stringSet(value)?.let { editor.putStringSet(key, it); true } ?: false
                    null -> inferred(editor, key, value)
                    else -> false
                }
                if (stored) applied++ else skipped++
            }
            editor.apply()
        }
        return SettingsImportOutcome(applied, skipped, invalid = false)
    }

    private fun inferred(editor: Editor, key: String, value: Any?): Boolean = when (value) {
        is Boolean -> { editor.putBoolean(key, value); true }
        is String -> { editor.putString(key, value); true }
        is Int -> { editor.putInt(key, value); true }
        is Long -> { editor.putLong(key, value); true }
        is Double -> if (value % 1.0 == 0.0 && kotlin.math.abs(value) <= Int.MAX_VALUE) {
            editor.putInt(key, value.toInt()); true
        } else {
            editor.putFloat(key, value.toFloat()); true
        }
        else -> false
    }

    private fun number(value: Any?): Number? {
        val number = value as? Number ?: return null
        return if (number.toDouble() % 1.0 == 0.0) {
            if (number.toDouble() > Int.MAX_VALUE || number.toDouble() < Int.MIN_VALUE) number.toLong() else number.toInt()
        } else {
            number.toDouble()
        }
    }

    private fun stringSet(value: Any?): Set<String>? {
        val array = value as? JSONArray ?: return null
        val result = LinkedHashSet<String>()
        for (index in 0 until array.length()) {
            val element = array.opt(index) as? String ?: return null
            result.add(element)
        }
        return result
    }

    private fun jsonValue(value: Any): Any = when (value) {
        is Set<*> -> JSONArray().apply { value.forEach { put(it.toString()) } }
        is Float -> value.toDouble()
        else -> value
    }
}
