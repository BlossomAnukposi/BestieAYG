package com.bayg.data.settings

import android.content.SharedPreferences
import com.bayg.security.SecurePrefs
import com.google.gson.Gson
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Local-only [SettingsRepository] backed by [SecurePrefs].
 *
 * Lets the settings UI ship and be exercised before Firebase Auth is
 * wired in. After Firestore is added, this stays in place as the local
 * read cache for offline use.
 *
 * Storage format is a single Gson JSON blob, encrypted at rest by
 * EncryptedSharedPreferences. A single blob keeps schema changes cheap
 * (no per-field migrations).
 */
class LocalSettingsRepository(
    private val securePrefs: SecurePrefs,
    private val gson: Gson = Gson(),
) : SettingsRepository {

    override fun observeSettings(): Flow<AppSettings> = callbackFlow {
        trySend(readOrDefault())

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == SecurePrefs.Key.SETTINGS_JSON_CACHE.raw) {
                trySend(readOrDefault())
            }
        }
        securePrefs.registerOnChangeListener(listener)
        awaitClose { securePrefs.unregisterOnChangeListener(listener) }
    }.distinctUntilChanged()

    override suspend fun getSettings(): AppSettings = readOrDefault()

    override suspend fun updateSettings(settings: AppSettings) {
        securePrefs.putString(SecurePrefs.Key.SETTINGS_JSON_CACHE, gson.toJson(settings))
    }

    private fun readOrDefault(): AppSettings {
        val json = securePrefs.getString(SecurePrefs.Key.SETTINGS_JSON_CACHE) ?: return AppSettings()
        return runCatching { gson.fromJson(json, AppSettings::class.java) }
            .getOrDefault(AppSettings())
            ?: AppSettings()
    }
}
