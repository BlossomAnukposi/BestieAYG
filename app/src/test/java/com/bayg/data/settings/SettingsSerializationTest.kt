package com.bayg.data.settings

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Guards the contract that [LocalSettingsRepository] relies on. Settings
 * and profile data classes must survive a Gson round-trip and an empty
 * JSON blob must deserialise to the documented defaults.
 */
class SettingsSerializationTest {

    private val gson = Gson()

    @Test
    fun `AppSettings default round-trip`() {
        val original = AppSettings()
        val roundTripped = gson.fromJson(gson.toJson(original), AppSettings::class.java)
        assertEquals(original, roundTripped)
    }

    @Test
    fun `AppSettings custom values round-trip`() {
        val original = AppSettings(
            theme = AppSettings.THEME_DARK,
            language = "nl",
            notificationsEnabled = false,
            weatherNudgesEnabled = false,
            parkNudgesEnabled = true,
            monitoredPackages = listOf("com.zhiliaoapp.musically", "com.instagram.android"),
            dailyCapMinutes = mapOf(
                "com.zhiliaoapp.musically" to 45,
                "com.instagram.android" to 30,
            ),
            examModeActive = true,
        )
        val roundTripped = gson.fromJson(gson.toJson(original), AppSettings::class.java)
        assertEquals(original, roundTripped)
    }

    @Test
    fun `AppSettings empty JSON deserialises to defaults`() {
        val parsed = gson.fromJson("{}", AppSettings::class.java)
        assertNotNull(parsed)
        assertEquals(AppSettings(), parsed)
    }

    @Test
    fun `UserProfile default round-trip`() {
        val original = UserProfile()
        val roundTripped = gson.fromJson(gson.toJson(original), UserProfile::class.java)
        assertEquals(original, roundTripped)
    }

    @Test
    fun `UserProfile with consent round-trip`() {
        val original = UserProfile(
            displayName = "Trifa",
            email = "trifa@example.com",
            createdAt = 1717400000L,
            consent = ConsentRecord(
                privacyPolicyVersion = "1.0",
                acceptedAt = 1717400000L,
                analyticsOptIn = false,
            ),
        )
        val roundTripped = gson.fromJson(gson.toJson(original), UserProfile::class.java)
        assertEquals(original, roundTripped)
    }
}
