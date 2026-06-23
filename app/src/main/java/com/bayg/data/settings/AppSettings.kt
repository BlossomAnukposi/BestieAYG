package com.bayg.data.settings

/**
 * User preferences synced across devices via Firestore.
 *
 * Firestore path: `users/{uid}/settings/main`.
 *
 * Only small, non-sensitive preferences belong here. Per-app usage
 * timeline, calendar cache and crashout history live in the encrypted
 * Room DB instead.
 */
data class AppSettings(
    val theme: String = THEME_SYSTEM,
    val language: String = "en",
    val notificationsEnabled: Boolean = true,
    val weatherNudgesEnabled: Boolean = true,
    val parkNudgesEnabled: Boolean = true,
    val monitoredPackages: List<String> = emptyList(),
    val dailyCapMinutes: Map<String, Int> = emptyMap(),
    val examModeActive: Boolean = false,
) {
    companion object {
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"

        const val COLLECTION = "settings"
        const val DOCUMENT_ID = "main"
    }
}
