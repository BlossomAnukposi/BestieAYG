package com.bayg.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Typed wrapper around [EncryptedSharedPreferences] for any auth-state or
 * security-sensitive flag we cache locally.
 *
 * Values are encrypted with AES-256-GCM, keys with AES-256-SIV, and the
 * master key lives in the Android Keystore. The on-disk file
 * (`bayg_secure_prefs.xml`) is an encrypted blob.
 *
 * Use for small auth-related state (last UID, biometric flag, settings
 * cache). Use the SQLCipher Room DB for larger data. Use plain
 * SharedPreferences for non-sensitive UI state.
 */
class SecurePrefs(context: Context) {

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun putString(key: Key, value: String) {
        prefs.edit().putString(key.raw, value).apply()
    }

    fun getString(key: Key): String? = prefs.getString(key.raw, null)

    fun putBoolean(key: Key, value: Boolean) {
        prefs.edit().putBoolean(key.raw, value).apply()
    }

    fun getBoolean(key: Key, default: Boolean = false): Boolean =
        prefs.getBoolean(key.raw, default)

    /** For per-user keys such as onboarding flags. Prefer [Key] for fixed entries. */
    fun putBoolean(rawKey: String, value: Boolean) {
        prefs.edit().putBoolean(rawKey, value).apply()
    }

    fun getBoolean(rawKey: String, default: Boolean = false): Boolean =
        prefs.getBoolean(rawKey, default)

    fun remove(rawKey: String) {
        prefs.edit().remove(rawKey).apply()
    }

    fun remove(key: Key) {
        prefs.edit().remove(key.raw).apply()
    }

    /** Call on sign-out. */
    fun clear() {
        prefs.edit().clear().apply()
    }

    fun registerOnChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterOnChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    /**
     * Single source of truth for everything we persist in encrypted prefs.
     * Add new entries here rather than passing raw strings around.
     */
    enum class Key(val raw: String) {
        LAST_SIGNED_IN_UID("last_signed_in_uid"),
        BIOMETRIC_UNLOCK_ENABLED("biometric_unlock_enabled"),
        REMEMBER_ME("remember_me"),
        SETTINGS_JSON_CACHE("settings_json_cache"),
    }

    private companion object {
        const val PREFS_FILE_NAME = "bayg_secure_prefs"
    }
}
